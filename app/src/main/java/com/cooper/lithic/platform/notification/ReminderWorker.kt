package com.cooper.lithic.platform.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.app.StorageInitializationState
import com.cooper.lithic.data.repository.ReminderRepository
import com.cooper.lithic.data.repository.ReminderDispatchResult
import kotlinx.coroutines.CancellationException
import java.time.Clock

class ReminderWorker(context: Context, parameters: WorkerParameters, private val container: AppContainer,
    private val publisher: ReminderPublisher = NotificationPublisher(context),
    private val clock: Clock = Clock.systemUTC()) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        return try {
            val generation = container.openStorage()
            fun current() = (container.storageState.value as? StorageInitializationState.Ready)?.generation === generation
            when (inputData.getString(MODE)) {
                RECONCILE -> ReminderScheduling.reconcile(applicationContext, generation, ::current, clock)
                PUBLISH -> {
                    if (inputData.getString(GENERATION) != generation.id || !current()) return Result.success()
                    val key = inputData.getString(KEY) ?: return Result.failure()
                    val row = inputData.getString(RENEWAL) ?: return Result.failure()
                    val revision = inputData.getString(REVISION) ?: return Result.failure()
                    if (ReminderRepository(generation, ::current, publisher, clock).publish(key, row, revision) ==
                        ReminderDispatchResult.WAITING_FOR_NINE) return Result.retry()
                }
                else -> return Result.failure()
            }
            Result.success()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { if (runAttemptCount < 5) Result.retry() else Result.failure() }
    }
    companion object {
        const val MODE="mode"; const val RECONCILE="reconcile"; const val PUBLISH="publish"
        const val GENERATION="generation"; const val KEY="logical_key"; const val RENEWAL="renewal_id"; const val REVISION="revision"
    }
}

class ReminderWorkerFactory(private val container: AppContainer) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
        if (workerClassName == ReminderWorker::class.java.name) ReminderWorker(appContext, workerParameters, container) else null
}
