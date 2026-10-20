package com.cooper.lithic.feature.settings

import com.cooper.lithic.data.backup.BackupExportResult
import com.cooper.lithic.data.backup.BackupFailureReason
import com.cooper.lithic.data.backup.BackupTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal enum class BackupUiPhase { IDLE, CHOOSING, EXPORTING }

internal data class BackupExportUiState(
    val phase: BackupUiPhase = BackupUiPhase.IDLE,
    val message: String? = null,
    val exportedBytes: Long? = null,
)

/** State lives above the settings composition; only a live, accepted picker result can start IO. */
internal class BackupExportController(
    private var interruptedOnRecreation: Boolean = false,
    private val export: suspend (BackupTarget) -> BackupExportResult,
) {
    private val monitor = Any()
    private val mutableState = MutableStateFlow(BackupExportUiState(message =
        if (interruptedOnRecreation) interruptedMessage(remaining = true) else null))
    val state: StateFlow<BackupExportUiState> = mutableState.asStateFlow()

    fun beginSelection(): Boolean = synchronized(monitor) {
        if (state.value.phase != BackupUiPhase.IDLE) false
        else {
            interruptedOnRecreation = false
            mutableState.value = BackupExportUiState(phase = BackupUiPhase.CHOOSING)
            true
        }
    }

    fun selectionCancelled() = synchronized(monitor) {
        if (state.value.phase == BackupUiPhase.CHOOSING) {
            mutableState.value = BackupExportUiState(message = "已取消选择保存位置，未导出备份。")
        }
    }

    fun selectionLaunchFailed(possibleResidual: Boolean = false) = synchronized(monitor) {
        if (state.value.phase == BackupUiPhase.CHOOSING) {
            mutableState.value = BackupExportUiState(message = "无法打开系统保存位置。备份未完成，请稍后重试。" + residue(possibleResidual))
        }
    }

    suspend fun selected(target: BackupTarget) {
        val accepted = synchronized(monitor) {
            if (state.value.phase == BackupUiPhase.CHOOSING) {
                mutableState.value = BackupExportUiState(phase = BackupUiPhase.EXPORTING)
                true
            } else {
                // A restarted process cannot prove a previous export's outcome or target ownership.
                // In particular, a duplicate callback must never delete a completed backup.
                if (interruptedOnRecreation) mutableState.value = BackupExportUiState(message = interruptedMessage(remaining = true))
                false
            }
        }
        if (!accepted) return
        val result = try {
            export(target)
        } catch (cancelled: CancellationException) {
            // Publish the cleanup outcome before returning to the cancelled caller context.
            withContext(NonCancellable) {
                val remaining = !deleteSelectedTarget(target)
                publish(BackupExportResult.Cancelled(remaining))
            }
            throw cancelled
        } catch (_: Exception) {
            BackupExportResult.Failed(BackupFailureReason.IO, !deleteSelectedTarget(target))
        }
        publish(result)
    }

    private suspend fun deleteSelectedTarget(target: BackupTarget): Boolean = withContext(NonCancellable + Dispatchers.IO) {
        try { target.deletePartial() } catch (_: Exception) { false }
    }

    private fun publish(result: BackupExportResult) = synchronized(monitor) {
        mutableState.value = when (result) {
            is BackupExportResult.Success -> BackupExportUiState(message = "完整备份已导出并通过校验。" +
                if (result.temporaryFilesRemaining) "内部临时文件暂未清理，已保留；已校验的备份文件仍可使用。" else "", exportedBytes = result.byteLength)
            is BackupExportResult.Cancelled -> BackupExportUiState(message = "导出已取消，未完成备份。" + residue(result.partialTargetRemaining))
            is BackupExportResult.Failed -> BackupExportUiState(message = when (result.reason) {
                BackupFailureReason.INVALID_MEDIA -> "已登记图片缺失或损坏，备份未完成。原记录和图片未改变。"
                BackupFailureReason.MEDIA_NOT_CERTIFIED -> "无法确认已登记图片的完整性，未生成完整备份。原记录和图片未改变。"
                BackupFailureReason.SOURCE_CHANGED -> "备份来源未通过一致性或安全检查，备份未完成。原记录和图片未改变。"
                BackupFailureReason.INCOMPLETE_TARGET -> "保存文件未通过完整校验，备份未完成。"
                BackupFailureReason.IO -> "无法完成文件读写，备份未完成。请检查保存位置和可用空间。"
                BackupFailureReason.INTERNAL -> "备份未完成。原记录和图片未改变，请稍后重试。"
            } + residue(result.partialTargetRemaining))
        }
    }

    private fun residue(remaining: Boolean) = if (remaining) "保存位置可能有残留文件，请勿将其用于恢复。" else ""
    private fun interruptedMessage(remaining: Boolean) = "上次备份已中断，未自动重新导出。" + residue(remaining)
}
