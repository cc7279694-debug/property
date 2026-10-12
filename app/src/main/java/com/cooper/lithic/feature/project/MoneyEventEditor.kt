package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountField
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
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
        .padding(LithicSpacing.medium).testTag("money-editor-screen"),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        TextButton(onBack, Modifier.heightIn(min = 48.dp).testTag("money-editor-back")) {
            Text("返回", style = MaterialTheme.typography.bodyLarge)
        }
        Text("记录投入或收回", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() })
        Text(state.projectName, style = MaterialTheme.typography.bodyLarge)
        listOf(MoneyDirection.OUTLAY to "投入", MoneyDirection.RECOVERY to "收回").forEach { (direction, label) ->
            val tag = if (direction == MoneyDirection.OUTLAY) "money-direction-outlay" else "money-direction-recovery"
            OutlinedButton(onClick = { onDirectionChange(direction) }, enabled = state.canEdit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag)
                    .semantics { selected = state.direction == direction }) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
        AmountField(state.amount, onAmountChange, Modifier.testTag("money-amount"),
            label = "实际金额（元）", error = state.errors["amount"], enabled = state.canEdit)
        Text("填实际发生的正数或0，收回通过上方方向记录。", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.today?.let { today ->
            ProjectDateInput(state.occurredOn, onOccurredOnChange, today, "真实发生日期", "money-occurred-on",
                error = state.errors["occurredOn"], enabled = state.canEdit)
        }
        ProjectNoteInput(state.description, onDescriptionChange, "说明（可选）", "money-description", state.canEdit)
        state.message?.let {
            Text(it, Modifier.fillMaxWidth().testTag("money-form-error"),
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        }
        if (state.busy) Text("正在保存到本地", style = MaterialTheme.typography.bodyLarge)
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
    }
}
