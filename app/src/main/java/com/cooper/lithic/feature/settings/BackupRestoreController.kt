package com.cooper.lithic.feature.settings

import com.cooper.lithic.data.backup.RestorePreview
import com.cooper.lithic.data.backup.RestoreResult
import com.cooper.lithic.core.design.UiMessageTone
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class RestoreUiPhase { IDLE, CHOOSING, VALIDATING, PREVIEW, DISCARDING, RESTORING }

/** Presentation only. A return to IDLE never establishes a restore outcome. */
internal enum class RestoreFeedbackKind {
    NONE, SUCCESS, SELECTION_CANCELLED, SELECTION_FAILED, VALIDATION_CANCELLED, VALIDATION_FAILED,
    PREVIEW_CANCELLED, PREVIEW_CANCELLED_WITH_RESIDUE, FAILED_CURRENT_PRESERVED, OUTCOME_UNKNOWN;

    val messageTone: UiMessageTone get() = when (this) {
        SUCCESS, SELECTION_CANCELLED, VALIDATION_CANCELLED, PREVIEW_CANCELLED -> UiMessageTone.INFORMATION
        PREVIEW_CANCELLED_WITH_RESIDUE, OUTCOME_UNKNOWN -> UiMessageTone.WARNING
        NONE, SELECTION_FAILED, VALIDATION_FAILED, FAILED_CURRENT_PRESERVED -> UiMessageTone.ERROR
    }
}

internal data class BackupRestoreUiState(
    val phase: RestoreUiPhase = RestoreUiPhase.IDLE,
    val preview: RestorePreview? = null,
    val message: String? = null,
    val feedback: RestoreFeedbackKind = RestoreFeedbackKind.NONE,
) {
    val messageTone: UiMessageTone get() = feedback.messageTone
}

/** Rebuild presentation after recreation; this marker never authorizes a restore. */
internal fun BackupRestoreUiState.requiresRecreationWarning(): Boolean =
    phase != RestoreUiPhase.IDLE || feedback == RestoreFeedbackKind.OUTCOME_UNKNOWN

/** Only an explicit confirmation of this live preview can pass its private capability to storage. */
internal class BackupRestoreController(
    interruptedOnRecreation: Boolean = false,
    private val prepare: suspend (() -> InputStream) -> RestorePreview,
    private val confirm: suspend (String) -> RestoreResult,
    private val discard: suspend (String) -> Unit,
) {
    private val monitor = Any()
    private val mutableState = MutableStateFlow(if (interruptedOnRecreation) BackupRestoreUiState(
        message = "上次恢复流程已中断。" + UNKNOWN_OUTCOME +
            "应用不会自动覆盖记录；核对当前本地记录后，如需恢复，请重新选择备份并确认。",
        feedback = RestoreFeedbackKind.OUTCOME_UNKNOWN,
    ) else BackupRestoreUiState())
    val state: StateFlow<BackupRestoreUiState> = mutableState.asStateFlow()

    fun beginSelection(): Boolean = synchronized(monitor) {
        if (state.value.phase != RestoreUiPhase.IDLE) false
        else {
            mutableState.value = BackupRestoreUiState(RestoreUiPhase.CHOOSING)
            true
        }
    }

    fun selectionCancelled() = finishSelection("已取消选择备份，当前记录未改变。", RestoreFeedbackKind.SELECTION_CANCELLED)
    fun selectionLaunchFailed() = finishSelection("无法打开系统文件选择器。当前记录未改变，请重新选择。", RestoreFeedbackKind.SELECTION_FAILED)

    private fun finishSelection(message: String, feedback: RestoreFeedbackKind) = synchronized(monitor) {
        if (state.value.phase == RestoreUiPhase.CHOOSING) mutableState.value = BackupRestoreUiState(message = message, feedback = feedback)
    }

    suspend fun selected(openInput: () -> InputStream) {
        val accepted = synchronized(monitor) {
            if (state.value.phase != RestoreUiPhase.CHOOSING) false
            else {
                mutableState.value = BackupRestoreUiState(RestoreUiPhase.VALIDATING)
                true
            }
        }
        if (!accepted) return
        try {
            val preview = prepare(openInput)
            mutableState.value = BackupRestoreUiState(RestoreUiPhase.PREVIEW, preview)
        } catch (cancelled: CancellationException) {
            mutableState.value = BackupRestoreUiState(message = "已取消备份校验，当前记录未改变。",
                feedback = RestoreFeedbackKind.VALIDATION_CANCELLED)
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = BackupRestoreUiState(message =
                "备份无法完整读取或未通过校验，恢复未开始。当前记录未改变，请检查备份文件和可用空间。",
                feedback = RestoreFeedbackKind.VALIDATION_FAILED)
        }
    }

    suspend fun discardPreview() {
        val token = synchronized(monitor) {
            val current = state.value
            if (current.phase != RestoreUiPhase.PREVIEW) null
            else current.preview?.token?.also { mutableState.value = BackupRestoreUiState(RestoreUiPhase.DISCARDING) }
        } ?: return
        try {
            discard(token)
            mutableState.value = BackupRestoreUiState(message = "已取消恢复，当前记录未改变。",
                feedback = RestoreFeedbackKind.PREVIEW_CANCELLED)
        } catch (cancelled: CancellationException) {
            mutableState.value = BackupRestoreUiState(message = "恢复已取消，当前记录未改变；内部临时文件可能仍保留。",
                feedback = RestoreFeedbackKind.PREVIEW_CANCELLED_WITH_RESIDUE)
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = BackupRestoreUiState(message = "已取消恢复，当前记录未改变；部分内部临时文件暂未清理。",
                feedback = RestoreFeedbackKind.PREVIEW_CANCELLED_WITH_RESIDUE)
        }
    }

    suspend fun confirmOverwrite() {
        val token = synchronized(monitor) {
            val current = state.value
            if (current.phase != RestoreUiPhase.PREVIEW) null
            else current.preview?.token?.also { mutableState.value = BackupRestoreUiState(RestoreUiPhase.RESTORING) }
        } ?: return
        try {
            mutableState.value = when (val result = confirm(token)) {
                RestoreResult.Success -> BackupRestoreUiState(message = "恢复成功，已重新读取恢复后的本地记录。",
                    feedback = RestoreFeedbackKind.SUCCESS)
                is RestoreResult.Failed -> BackupRestoreUiState(message = result.message +
                    if (result.currentPreserved) "当前记录已保留。请重新选择备份并核对。"
                    else UNKNOWN_OUTCOME,
                    feedback = if (result.currentPreserved) RestoreFeedbackKind.FAILED_CURRENT_PRESERVED else RestoreFeedbackKind.OUTCOME_UNKNOWN)
            }
        } catch (cancelled: CancellationException) {
            mutableState.value = BackupRestoreUiState(message = UNKNOWN_OUTCOME, feedback = RestoreFeedbackKind.OUTCOME_UNKNOWN)
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = BackupRestoreUiState(message = UNKNOWN_OUTCOME, feedback = RestoreFeedbackKind.OUTCOME_UNKNOWN)
        }
    }

    companion object {
        private const val UNKNOWN_OUTCOME = "恢复结果暂无法确认。请重新打开应用核对本地记录，请勿清除应用数据。"
    }
}
