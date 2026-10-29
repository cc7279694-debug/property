package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import com.cooper.lithic.core.design.LithicButton as Button
import androidx.compose.material3.MaterialTheme
import com.cooper.lithic.core.design.LithicOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.UiFeedbackText
import com.cooper.lithic.core.design.UiMessageTone
import com.cooper.lithic.core.design.AmountField
import com.cooper.lithic.core.design.C2FormScaffold
import com.cooper.lithic.core.design.C2Section
import com.cooper.lithic.core.design.C5InformationButton
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.model.MoneyDirection

@Composable
internal fun MoneyEventEditor(
    state: MoneyEventEditorUiState,
    onBack: () -> Unit,
    onDirectionChange: (MoneyDirection) -> Unit,
    onAmountChange: (String) -> Unit,
    onOccurredOnChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetryRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    C2FormScaffold("记录投入或收回", onBack, "money-editor-back",
        modifier = modifier.testTag("money-editor-screen"), footer = {
            state.message?.let {
                UiFeedbackText(it, UiMessageTone.ERROR, "money-form-error")
            }
            ProjectSubmissionNotice(state.phase, "money")
            if (state.phase == SubmissionPhase.COMMITTED) {
                Button(onRetryRead, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("money-retry-read")) {
                    Text("重新读取已保存项目", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Button(onSave, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("money-save"), enabled = !state.busy) {
                    Text(if (state.phase == SubmissionPhase.UNCONFIRMED) "核对并重试" else "保存金额记录",
                        style = MaterialTheme.typography.bodyLarge)
                }
            }
        }) {
        C2Section("记录对象") { Text(state.projectName, style = MaterialTheme.typography.titleLarge) }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
            Text("本次记录：${if (state.direction == MoneyDirection.OUTLAY) "投入" else "收回"}",
                Modifier.testTag("money-selected-direction"), style = MaterialTheme.typography.titleMedium)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(MoneyDirection.OUTLAY to "投入", MoneyDirection.RECOVERY to "收回").forEach { (direction, label) ->
                    val tag = if (direction == MoneyDirection.OUTLAY) "money-direction-outlay" else "money-direction-recovery"
                    OutlinedButton(onClick = { onDirectionChange(direction) }, enabled = state.canEdit,
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            containerColor = if (state.direction == direction) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = if (state.direction == direction) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.heightIn(min = 48.dp).testTag(tag)
                            .semantics { selected = state.direction == direction }) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
            AmountField(state.amount, onAmountChange, Modifier.testTag("money-amount"),
                label = "实际金额（元）", error = state.errors["amount"], enabled = state.canEdit, labelOutside = true)
            state.today?.let { today ->
                ProjectDateInput(state.occurredOn, onOccurredOnChange, today, "真实发生日期", "money-occurred-on",
                    error = state.errors["occurredOn"], enabled = state.canEdit, labelOutside = true)
            }
        }
        ProjectNoteInput(state.description, onDescriptionChange, "说明（可选）", "money-description", state.canEdit, labelOutside = true)
        C5InformationButton("金额记录说明", Modifier.testTag("money-editor-information"), buttonLabel = "金额记录说明") {
            Text("填实际发生的正数或0，收回通过上方方向记录。真实发生日期按实际付款或到账日填写。")
        }
    }
}
