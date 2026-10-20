package com.cooper.lithic.app

import android.content.Context
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock

sealed interface StorageInitializationState {
    data object Initializing : StorageInitializationState
    data class Ready(val generation: ManagedGeneration,val recoveryWarning:String?=null) : StorageInitializationState
    data class Failed(val reason: Throwable) : StorageInitializationState
}

/** One storage owner for this application. The supplied scope belongs to this container. */
class AppContainer private constructor(
    generationFactory: () -> DataGenerationStore,
    private val scope: CoroutineScope,
) {
    constructor(context: Context) : this(
        { DataGenerationStore(context.applicationContext) },
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    internal constructor(generations: DataGenerationStore, scope: CoroutineScope) : this({ generations }, scope)

    private val generations by lazy(generationFactory)
    private val restoreLifecycle=kotlinx.coroutines.sync.Mutex()
    private val ownerJob = requireNotNull(scope.coroutineContext[Job]) { "Storage requires a lifecycle owner" }
    private val lifecycle = Any()
    private var initialization: Deferred<ManagedGeneration>? = null
    private var closed = false
    private val mutableState = MutableStateFlow<StorageInitializationState>(StorageInitializationState.Initializing)
    val storageState: StateFlow<StorageInitializationState> = mutableState.asStateFlow()

    fun start(recordZone: ZoneId) {
        synchronized(lifecycle) {
            check(!closed) { "Storage owner is closed" }
            if (initialization == null) {
                initialization = scope.async(Dispatchers.IO) { initializeStorage(recordZone) }
            }
        }
    }

    suspend fun openStorage(): ManagedGeneration {
        val pending = synchronized(lifecycle) {
            check(!closed) { "Storage owner is closed" }
            checkNotNull(initialization) { "Storage initialization has not started" }
        }
        pending.await()
        return when(val observed=storageState.value) {
            is StorageInitializationState.Ready -> observed.generation
            is StorageInitializationState.Failed -> throw observed.reason
            StorageInitializationState.Initializing -> throw IllegalStateException("Storage is switching generations")
        }
    }

    private suspend fun initializeStorage(recordZone: ZoneId): ManagedGeneration {
        var acquired: ManagedGeneration? = null
        try {
            val generation = generations.open(recordZone)
            acquired = generation
            currentCoroutineContext().ensureActive()
            mutableState.value = StorageInitializationState.Ready(generation,generations.lastRecoveryWarning)
            return generation
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable + Dispatchers.IO) {
                try {
                    acquired?.close()
                } catch (closingFailure: Throwable) {
                    cancelled.addSuppressed(closingFailure)
                }
            }
            throw cancelled
        } catch (failure: Throwable) {
            mutableState.value = StorageInitializationState.Failed(failure)
            throw failure
        }
    }

    internal suspend fun prepareRestoreGeneration(zone:ZoneId,populate:suspend(ManagedGeneration)->Unit):com.cooper.lithic.data.backup.PreparedGeneration {
        return restoreLifecycle.withLock {
            openStorage()
            generations.prepareRestore(zone,populate)
        }
    }

    internal suspend fun activateRestoreGeneration(expectedId:String,candidate:com.cooper.lithic.data.backup.PreparedGeneration,expectedSnapshot:com.cooper.lithic.data.backup.BackupSnapshot):ManagedGeneration {
        restoreLifecycle.lock()
        try {
            check(openStorage().id==expectedId) { "Current generation changed" }
            mutableState.value=StorageInitializationState.Initializing
            return withContext(NonCancellable+Dispatchers.IO) {
                try {
                    val restored=generations.activateRestore(expectedId,candidate,expectedSnapshot)
                    mutableState.value=StorageInitializationState.Ready(restored)
                    restored
                } catch(failure:Throwable) {
                    try {
                        val fallback=generations.open()
                        mutableState.value=StorageInitializationState.Ready(fallback,generations.lastRecoveryWarning)
                    } catch(reopenFailure:Throwable) {
                        failure.addSuppressed(reopenFailure)
                        mutableState.value=StorageInitializationState.Failed(failure)
                    }
                    throw failure
                }
            }
        } finally { restoreLifecycle.unlock() }
    }

    suspend fun close() = withContext(NonCancellable + Dispatchers.IO) {
        synchronized(lifecycle) { closed = true }
        scope.cancel()
        ownerJob.join()
        // An activation already in progress must finish (or restore its old owner) before
        // shutdown chooses which resources to close. Initializing is not proof of no owner.
        restoreLifecycle.withLock {
            (mutableState.value as? StorageInitializationState.Ready)?.generation?.close()
        }
    }
}
