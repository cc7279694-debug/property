package com.cooper.lithic.feature.project

import com.cooper.lithic.core.design.lithicCheckboxColors
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import com.cooper.lithic.core.design.LithicButton as Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.UiFeedbackText
import com.cooper.lithic.core.design.UiMessageTone
import com.cooper.lithic.core.design.AmountField
import com.cooper.lithic.core.design.C2FormScaffold
import com.cooper.lithic.core.design.C2Section

/** A scrollable full-screen form; route ownership supplies the shared leave guard. */
@Composable
internal fun EndProjectDialog(
    state: EndProjectUiState,
    onBack: () -> Unit,
    onEndedOnChange: (String) -> Unit,
    onEndReasonChange: (String) -> Unit,
    onRecordRecoveryChange: (Boolean) -> Unit,
    onRecoveryAmountChange: (String) -> Unit,
    onRecoveryOnChange: (String) -> Unit,
    onRecoveryDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetryRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    C2FormScaffold(if (state.correcting) "修正结束记录" else "结束项目", onBack, "end-back",
        modifier = modifier.testTag("end-editor-screen"), footer = {
            LifecycleFormActions(state.phase, state.loading, state.conflict, state.needsInitialRead,
                state.message, "end", if (state.correcting) "保存结束修正" else "保存结束记录", onSave, onRetryRead, messageTone = state.messageTone)
        }) {
        C2Section("项目") { Text(state.projectName, style = MaterialTheme.typography.titleLarge) }
        Text("结束不是删除；从结束日期起退出当前项目列表。",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.loading) Text("正在读取结束资料", style = MaterialTheme.typography.bodyLarge)
        C2Section("结束资料") {
            state.today?.let { today ->
                ProjectDateInput(state.endedOn, onEndedOnChange, today, "结束日期", "end-ended-on",
                    error = state.errors["endedOn"], enabled = state.canEdit && !state.tradeInDateProtected, labelOutside = true)
            }
            if (state.tradeInProtected) Text("以旧换新的日期和折抵金额不能单独修改；这里可修改结束原因和关联说明。")
            else if (state.tradeInDateProtected) Text("此项目参与以旧换新，既有结束日期不能单独修改；普通现金结算和说明仍可纠正。")
            ProjectNoteInput(state.endReason, onEndReasonChange, "结束原因", "end-reason", state.canEdit,
                state.errors["endReason"], labelOutside = true)
        }
        if (!state.correcting) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("end-record-recovery")
                .toggleable(state.recordRecovery, enabled = state.canEdit, role = Role.Checkbox,
                    onValueChange = onRecordRecoveryChange), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(state.recordRecovery, onCheckedChange = null, enabled = state.canEdit, colors = lithicCheckboxColors())
                Text("同时记录实际收回", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            }
            Text("只有实际收到款项时才勾选；0元也可记录，不预填估价。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(if (state.hasSettlement) "更新原有关联收回记录，不新增或删除金额记录。"
                else "本次只修正结束记录；真实退款请在详情新增收回。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if ((state.correcting && state.hasSettlement) || (!state.correcting && state.recordRecovery)) {
            C2Section(if (state.tradeInProtected) "非现金折抵" else "实际收回") {
                AmountField(state.recoveryAmount, onRecoveryAmountChange, Modifier.testTag("end-recovery-amount"),
                    label = if (state.tradeInProtected) "非现金折抵金额（元）" else "实际收回金额（元）", error = state.errors["recoveryAmount"], enabled = state.canEdit && !state.tradeInProtected, labelOutside = true)
                state.today?.let { today ->
                    ProjectDateInput(state.recoveryOn, onRecoveryOnChange, today, "真实收回日期", "end-recovery-on",
                        error = state.errors["recoveryOn"], enabled = state.canEdit && !state.tradeInProtected, labelOutside = true)
                }
                Text("请按真实到账日填写，不记录未来到账。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ProjectNoteInput(state.recoveryDescription, onRecoveryDescriptionChange, "收回说明（可选）",
                    "end-recovery-description", state.canEdit, state.errors["recoveryDescription"], labelOutside = true)
            }
        }
    }
}

/** Shared presentation only: the ViewModel owns submission identity, CAS and read recovery. */
@Composable
internal fun LifecycleFormActions(
    phase: SubmissionPhase,
    loading: Boolean,
    conflict: Boolean,
    needsInitialRead: Boolean,
    message: String?,
    tag: String,
    saveLabel: String,
    onSave: () -> Unit,
    onRetryRead: () -> Unit,
    messageTone: UiMessageTone = UiMessageTone.ERROR,
) {
    message?.let {
        UiFeedbackText(it, messageTone, "$tag-form-error")
    }
    ProjectSubmissionNotice(phase, tag)
    when {
        phase == SubmissionPhase.COMMITTED -> Button(onRetryRead,
            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$tag-retry-read"), enabled = !loading) {
            Text("重新读取已保存项目", style = MaterialTheme.typography.bodyLarge)
        }
        conflict -> {
            Text("记录已改变。重新读取会替换当前输入，请核对后重新编辑。", style = MaterialTheme.typography.bodyLarge)
            Button(onRetryRead, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$tag-resolve-conflict"),
                enabled = !loading) { Text("读取当前记录重新编辑", style = MaterialTheme.typography.bodyLarge) }
        }
        else -> {
            if (needsInitialRead && !loading) Button(onRetryRead,
                Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$tag-initial-read-retry")) {
                Text("重新读取原记录", style = MaterialTheme.typography.bodyLarge)
            }
            Button(onSave, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$tag-save"),
                enabled = !loading && phase != SubmissionPhase.IN_FLIGHT && !needsInitialRead) {
                Text(if (phase == SubmissionPhase.UNCONFIRMED) "核对并重试" else saveLabel,
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
