package com.cooper.lithic.feature.settings

import com.cooper.lithic.core.design.UiMessageTone
import com.cooper.lithic.data.backup.BackupInspectionResult
import com.cooper.lithic.data.backup.BackupInspectionStatus
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class BackupInspectionPhase { IDLE, CHOOSING, CHECKING }

internal data class BackupInspectionUiState(
    val phase: BackupInspectionPhase = BackupInspectionPhase.IDLE,
    val requestId: String? = null,
    val result: BackupInspectionResult? = null,
    val message: String? = null,
    val cancelling: Boolean = false,
) {
    val busy: Boolean get() = phase != BackupInspectionPhase.IDLE
    val messageTone: UiMessageTone get() = when (result?.status) {
        BackupInspectionStatus.INVALID -> UiMessageTone.ERROR
        BackupInspectionStatus.UNKNOWN, BackupInspectionStatus.UNSUPPORTED -> UiMessageTone.WARNING
        else -> UiMessageTone.INFORMATION
    }
}

internal fun BackupInspectionUiState.requiresRecreationWarning(): Boolean =
    busy || result?.status == BackupInspectionStatus.UNKNOWN

/** Read-only request controller; no restore capability, URI or file path belongs to this state. */
internal class BackupInspectionController(
    interruptedOnRecreation: Boolean = false,
    private val currentOwner: () -> Any?,
    private val inspect: suspend (() -> InputStream, () -> Boolean) -> BackupInspectionResult,
) {
    private val monitor = Any()
    private var selectedOwner: Any? = null
    private var cancelled = false
    private val mutableState = MutableStateFlow(if (interruptedOnRecreation) finished(unknown(),
        "上次检查已中断，文件完整性尚未确认。当前记录不会因检查而改变。") else BackupInspectionUiState())
    val state: StateFlow<BackupInspectionUiState> = mutableState.asStateFlow()

    fun beginSelection(): String? = synchronized(monitor) {
        if (state.value.busy) return@synchronized null
        val owner = ownerNow()
        if (owner == null) {
            mutableState.value = finished(unknown(), "当前记录尚未就绪，暂不能开始检查。")
            return@synchronized null
        }
        val request = UUID.randomUUID().toString()
        selectedOwner = owner
        cancelled = false
        mutableState.value = BackupInspectionUiState(BackupInspectionPhase.CHOOSING, request)
        request
    }

    fun selectionCancelled(request: String) = finishSelection(request, null, "已取消选择，当前记录未改变。")
    fun selectionLaunchFailed(request: String) = finishSelection(request, unknown(), "未能选择文件，尚未取得检查结果。当前记录未改变。")
    fun previousSelectionPending() = synchronized(monitor) {
        if (!state.value.busy) mutableState.value = finished(unknown(),
            "上次系统文件选择尚未返回，请先完成或退出系统选择器。当前记录未改变。")
    }
    /** Restore only a drain obligation, never permission to open an old document. */
    fun retainPendingSelection(request: String) = synchronized(monitor) {
        if (!state.value.busy) {
            selectedOwner = null
            cancelled = true
            mutableState.value = BackupInspectionUiState(BackupInspectionPhase.CHOOSING, request,
                unknown(), "等待上次系统文件选择结束；旧结果不会用于检查。", cancelling = true)
        }
    }

    private fun finishSelection(request: String, result: BackupInspectionResult?, message: String) = synchronized(monitor) {
        if (state.value.phase == BackupInspectionPhase.CHOOSING && state.value.requestId == request) {
            val invalidated = cancelled || selectedOwner == null || selectedOwner !== ownerNow()
            selectedOwner = null
            mutableState.value = if (invalidated) finished(unknown(), OWNER_CHANGED)
                else BackupInspectionUiState(result = result, message = message)
        }
    }

    suspend fun selected(request: String, openInput: () -> InputStream) {
        val accepted = synchronized(monitor) {
            if (state.value.phase != BackupInspectionPhase.CHOOSING || state.value.requestId != request) false
            else if (cancelled || selectedOwner !== ownerNow() || selectedOwner == null) {
                selectedOwner = null
                mutableState.value = finished(unknown(), OWNER_CHANGED)
                false
            } else {
                mutableState.value = state.value.copy(phase = BackupInspectionPhase.CHECKING)
                true
            }
        }
        if (!accepted) return
        // Dispatch/cancellation may prevent delivery of the service's residue receipt.
        var outcome = unknown().copy(temporaryFilesRetained = true)
        try {
            val result = inspect(openInput) { validRequest(request) }
            currentCoroutineContext().ensureActive()
            outcome = result
        } catch (stop: CancellationException) {
            throw stop
        } catch (_: Exception) {
            // A provider error never establishes that the selected file is invalid.
            outcome = unknown().copy(temporaryFilesRetained = true)
        } finally {
            synchronized(monitor) {
                if (state.value.requestId == request && state.value.phase == BackupInspectionPhase.CHECKING) {
                    val trusted = !cancelled && selectedOwner != null && selectedOwner === ownerNow()
                    val result = if (!trusted || (outcome.status == BackupInspectionStatus.VERIFIED && outcome.summary == null))
                        unknown().copy(temporaryFilesRetained = outcome.temporaryFilesRetained) else outcome
                    selectedOwner = null
                    mutableState.value = finished(result)
                }
            }
        }
    }

    /** Remain busy until the actual reader unwinds; cancellation is not an FD-close receipt. */
    fun requestCancellation(): Boolean = synchronized(monitor) {
        if (state.value.phase != BackupInspectionPhase.CHECKING) false
        else {
            cancelled = true
            mutableState.value = state.value.copy(result = unknown(), cancelling = true,
                message = "正在结束检查，文件完整性尚未确认。当前记录未改变。")
            true
        }
    }

    fun ownerChanged(): Boolean = synchronized(monitor) {
        if (!state.value.busy || (selectedOwner != null && selectedOwner === ownerNow())) false
        else if (state.value.phase == BackupInspectionPhase.CHOOSING) {
            cancelled = true
            mutableState.value = state.value.copy(result = unknown(), message = OWNER_CHANGED, cancelling = true)
            false
        } else {
            cancelled = true
            mutableState.value = state.value.copy(result = unknown(), cancelling = true, message = OWNER_CHANGED)
            true
        }
    }

    private fun validRequest(request: String): Boolean = synchronized(monitor) {
        state.value.phase == BackupInspectionPhase.CHECKING && state.value.requestId == request &&
            !cancelled && selectedOwner != null && selectedOwner === ownerNow()
    }

    private fun ownerNow(): Any? = try { currentOwner() } catch (_: Exception) { null }

    companion object {
        private const val OWNER_CHANGED = "本地记录已切换，本次检查结果不再显示。当前记录未因检查改变。"
        private fun unknown() = BackupInspectionResult(BackupInspectionStatus.UNKNOWN)
        private fun finished(result: BackupInspectionResult, message: String? = null): BackupInspectionUiState {
            val text = message ?: when (result.status) {
                BackupInspectionStatus.VERIFIED -> "VERIFIED · 文件符合当前完整性规则。"
                BackupInspectionStatus.INVALID -> "INVALID · 文件未通过完整性检查，请勿用它覆盖当前记录。"
                BackupInspectionStatus.UNSUPPORTED -> "UNSUPPORTED · 当前版本不支持此备份格式或版本。"
                BackupInspectionStatus.UNKNOWN -> "UNKNOWN · 检查未能完整结束，文件完整性尚未确认。当前记录未改变。"
            }
            return BackupInspectionUiState(result = result, message = text +
                if (result.temporaryFilesRetained) " 本次内部检查临时文件可能仍保留，不会自动清理未知文件。" else "")
        }
    }
}
