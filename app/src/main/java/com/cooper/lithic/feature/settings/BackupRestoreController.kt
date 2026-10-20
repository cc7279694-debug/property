package com.cooper.lithic.feature.settings

import com.cooper.lithic.data.backup.RestorePreview
import com.cooper.lithic.data.backup.RestoreResult
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class RestoreUiPhase { IDLE, CHOOSING, VALIDATING, PREVIEW, DISCARDING, RESTORING }

internal data class BackupRestoreUiState(
    val phase: RestoreUiPhase = RestoreUiPhase.IDLE,
    val preview: RestorePreview? = null,
    val message: String? = null,
)

/** Only an explicit confirmation of this live preview can pass its private capability to storage. */
internal class BackupRestoreController(
    interruptedOnRecreation: Boolean = false,
    private val prepare: suspend (() -> InputStream) -> RestorePreview,
    private val confirm: suspend (String) -> RestoreResult,
    private val discard: suspend (String) -> Unit,
) {
    private val monitor = Any()
    private val mutableState = MutableStateFlow(BackupRestoreUiState(message = if (interruptedOnRecreation)
        "上次恢复流程已中断。请重新选择备份并核对；应用不会自动覆盖记录。" else null))
    val state: StateFlow<BackupRestoreUiState> = mutableState.asStateFlow()

    fun beginSelection(): Boolean = synchronized(monitor) {
        if (state.value.phase != RestoreUiPhase.IDLE) false
        else {
            mutableState.value = BackupRestoreUiState(RestoreUiPhase.CHOOSING)
            true
        }
    }

    fun selectionCancelled() = finishSelection("已取消选择备份，当前记录未改变。")
    fun selectionLaunchFailed() = finishSelection("无法打开系统文件选择器。当前记录未改变，请重新选择。")

    private fun finishSelection(message: String) = synchronized(monitor) {
        if (state.value.phase == RestoreUiPhase.CHOOSING) mutableState.value = BackupRestoreUiState(message = message)
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
            mutableState.value = BackupRestoreUiState(message = "已取消备份校验，当前记录未改变。")
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = BackupRestoreUiState(message =
                "备份无法完整读取或未通过校验，恢复未开始。当前记录未改变，请检查备份文件和可用空间。")
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
            mutableState.value = BackupRestoreUiState(message = "已取消恢复，当前记录未改变。")
        } catch (cancelled: CancellationException) {
            mutableState.value = BackupRestoreUiState(message = "恢复已取消，当前记录未改变；内部临时文件可能仍保留。")
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = BackupRestoreUiState(message = "已取消恢复，当前记录未改变；部分内部临时文件暂未清理。")
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
                RestoreResult.Success -> BackupRestoreUiState(message = "恢复成功，已重新读取恢复后的本地记录。")
                is RestoreResult.Failed -> BackupRestoreUiState(message = result.message +
                    if (result.currentPreserved) "当前记录已保留。请重新选择备份并核对。"
                    else "恢复结果暂无法确认。请重新打开应用核对本地记录，请勿清除应用数据。")
            }
        } catch (cancelled: CancellationException) {
            mutableState.value = BackupRestoreUiState(message = UNKNOWN_OUTCOME)
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = BackupRestoreUiState(message = UNKNOWN_OUTCOME)
        }
    }

    companion object {
        private const val UNKNOWN_OUTCOME = "恢复结果暂无法确认。请重新打开应用核对本地记录，请勿清除应用数据。"
    }
}
