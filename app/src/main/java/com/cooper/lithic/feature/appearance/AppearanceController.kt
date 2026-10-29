package com.cooper.lithic.feature.appearance

import com.cooper.lithic.app.AppProcessRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

enum class LithicAppearance(val storageId: String, val label: String) {
    CHARCOAL("charcoal", "深色黑灰"),
    PURE_WHITE("pure_white", "纯白"),
    STONE_MIST("stone_mist", "岩白雾蓝");

    companion object {
        fun fromStorage(value: String): LithicAppearance? = entries.firstOrNull { it.storageId == value }
    }
}

internal data class AppearanceUiState(
    val selected: LithicAppearance = LithicAppearance.STONE_MIST,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val readable: Boolean = false,
)

internal interface AppearanceStorage {
    suspend fun read(): String?
    suspend fun write(storageId: String)
}

/** Local presentation preference only. No ledger, generation, form or business owner is involved. */
internal class AppearanceController(private val scope: CoroutineScope, private val storage: AppearanceStorage) {
    private val mutableState = MutableStateFlow(AppearanceUiState())
    val state: StateFlow<AppearanceUiState> = mutableState
    private val requests = Any()
    private val storageOperation = Mutex()
    private var sequence = 0L

    init { retryRead() }

    fun select(theme: LithicAppearance) {
        val ticket = synchronized(requests) {
            if (!scope.isActive) {
                mutableState.value = mutableState.value.copy(saving = false, error = SAVE_ERROR)
                return
            }
            sequence = Math.addExact(sequence, 1)
            mutableState.value = mutableState.value.copy(loading = false, saving = true, error = null)
            sequence
        }
        scope.launch {
            storageOperation.withLock {
                // A queued choice superseded before its write started need not touch the file.
                if (!isLatest(ticket)) return@withLock
                try {
                    storage.write(theme.storageId)
                    coroutineContext.ensureActive()
                    synchronized(requests) {
                        // An earlier in-flight write really did persist. Retain it if the later
                        // requested write fails; never label an unpersisted selection as saved.
                        mutableState.value = mutableState.value.copy(selected = theme, readable = true,
                            saving = ticket != sequence, error = if (ticket == sequence) null else mutableState.value.error)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    synchronized(requests) {
                        if (ticket == sequence) mutableState.value = mutableState.value.copy(saving = false, error = SAVE_ERROR)
                    }
                }
            }
        }
    }

    fun retryRead() {
        val ticket = synchronized(requests) {
            // A retry action must not cancel or replace the user's in-flight theme choice.
            if (mutableState.value.saving) return
            if (!scope.isActive) {
                mutableState.value = mutableState.value.copy(loading = false, error = READ_ERROR)
                return
            }
            sequence = Math.addExact(sequence, 1)
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            sequence
        }
        scope.launch {
            storageOperation.withLock {
                if (!isLatest(ticket)) return@withLock
                try {
                    val value = storage.read()
                    coroutineContext.ensureActive()
                    val theme = value?.let(LithicAppearance::fromStorage)
                    synchronized(requests) {
                        if (ticket != sequence) return@synchronized
                        mutableState.value = if (value != null && theme == null) {
                            mutableState.value.copy(loading = false, readable = false,
                                error = "已保存的外观暂时无法识别，未重置原选择；可以重试读取或明确选择一种外观。")
                        } else {
                            mutableState.value.copy(selected = theme ?: LithicAppearance.STONE_MIST,
                                loading = false, readable = true, error = null)
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    synchronized(requests) {
                        if (ticket == sequence) mutableState.value = mutableState.value.copy(loading = false, error = READ_ERROR)
                    }
                }
            }
        }
    }

    private fun isLatest(ticket: Long): Boolean = synchronized(requests) { ticket == sequence }

    companion object {
        private const val READ_ERROR = "外观偏好暂时无法读取，未更改已保存选择；当前外观仅用于显示，请重试。"
        private const val SAVE_ERROR = "外观未能保存，仍保留原已保存选择，请重试。"
    }
}

internal object AppearanceInitialization {
    fun <T> mainOnly(role: AppProcessRole, create: () -> T): T {
        check(role == AppProcessRole.MAIN) { "Appearance preferences are unavailable in this process" }
        return create()
    }
}
