package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.UiFeedbackText
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Section
import com.cooper.lithic.core.design.LithicSpacing

@Composable
internal fun DeleteProjectDialog(
    state: DeleteProjectUiState,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onRetryRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canCancel = !state.loading && state.phase == SubmissionPhase.IDLE
    val canConfirm = !state.loading && !state.busy && !state.needsInitialRead && !state.conflict
    val needsRead = state.needsInitialRead || state.conflict || state.phase == SubmissionPhase.COMMITTED
    AlertDialog(onDismissRequest = { if (canCancel) onCancel() },
        modifier = modifier.testTag("project-delete-dialog"),
        shape = MaterialTheme.shapes.large,
        icon = { C2Icon(C2Glyph.Info) },
        title = { Text("永久删除项目？", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
                C2Section("确认永久移除") {
                    Text(state.projectName, style = MaterialTheme.typography.titleLarge)
                    Text("将删除项目及金额记录，历史统计也会重算；无法在App内撤回。",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                }
                if (state.loading) Text("正在核对项目与金额记录", style = MaterialTheme.typography.bodyLarge)
                if (state.busy) Text("正在删除本地记录", style = MaterialTheme.typography.bodyLarge)
                state.message?.let {
                    // Readback can fail after a confirmed result; only explicit metadata controls severity.
                    UiFeedbackText(it, state.messageTone, "project-delete-message")
                }
                if (state.phase == SubmissionPhase.UNCONFIRMED && !state.conflict) {
                    Text("删除结果尚未确认。请核对原提交，期间不能改选项目或重复创建删除操作。",
                        style = MaterialTheme.typography.bodyMedium)
                }
                if (state.conflict) Text("原确认未执行删除，请重新读取后核对。",
                    style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {
            if (needsRead) TextButton(onRetryRead, enabled = !state.loading && !state.busy,
                modifier = Modifier.heightIn(min = 48.dp).testTag("project-delete-retry-read")) {
                Text(if (state.phase == SubmissionPhase.COMMITTED) "重新读取删除结果" else "重新读取并核对",
                    style = MaterialTheme.typography.bodyLarge)
            } else TextButton(onConfirm, enabled = canConfirm,
                modifier = Modifier.heightIn(min = 48.dp).testTag("project-delete-confirm"),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(if (state.phase == SubmissionPhase.UNCONFIRMED) "核对原删除提交" else "确认永久删除",
                    style = MaterialTheme.typography.bodyLarge)
            }
        },
        dismissButton = {
            TextButton(onCancel, enabled = canCancel,
                modifier = Modifier.heightIn(min = 48.dp).testTag("project-delete-cancel")) {
                Text("取消", style = MaterialTheme.typography.bodyLarge)
            }
        })
}
