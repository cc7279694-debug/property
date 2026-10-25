package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountField
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.ProjectMoneyEventRole

@Composable
internal fun MoneyEventCorrectionScreen(
    state: MoneyCorrectionUiState,
    onBack: () -> Unit,
    onAmountChange: (String) -> Unit,
    onOccurredOnChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetryRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
        .padding(LithicSpacing.medium).testTag("money-correction-screen"),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        TextButton(onBack, Modifier.heightIn(min = 48.dp).testTag("correction-back")) {
            Text("返回", style = MaterialTheme.typography.bodyLarge)
        }
        Text("纠正金额记录", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() })
        Text(state.projectName, style = MaterialTheme.typography.bodyLarge)
        Text(if (state.tradeInProtected) "以旧换新关联记录只修改说明，不单独修改金额或真实日期。" else "纠正原记录的金额、真实日期或说明；真实退款请新增收回记录。",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.loading) Text("正在读取原金额记录", style = MaterialTheme.typography.bodyLarge)
        Text("方向：${if (state.direction == MoneyDirection.OUTLAY) "投入" else "收回"}",
            Modifier.testTag("correction-direction"), style = MaterialTheme.typography.bodyLarge)
        Text("记录类型：${when (state.role) {
            ProjectMoneyEventRole.INITIAL -> "初始投入"
            ProjectMoneyEventRole.REGULAR -> "普通金额记录"
            ProjectMoneyEventRole.END_SETTLEMENT -> "结束关联收回"
        }}", Modifier.testTag("correction-role"), style = MaterialTheme.typography.bodyMedium)
        AmountField(state.amount, onAmountChange, Modifier.testTag("correction-amount"), label = "实际金额（元）",
            error = state.errors["amount"], enabled = state.canEdit && !state.tradeInProtected)
        state.today?.let { today ->
            ProjectDateInput(state.occurredOn, onOccurredOnChange, today, "真实发生日期", "correction-occurred-on",
                error = state.errors["occurredOn"], enabled = state.canEdit && !state.tradeInProtected)
        }
        ProjectNoteInput(state.description, onDescriptionChange, "说明（可选）", "correction-description",
            state.canEdit, state.errors["description"])
        LifecycleFormActions(state.phase, state.loading, state.conflict, state.needsInitialRead,
            state.message, "correction", "保存原记录修正", onSave, onRetryRead, messageTone = state.messageTone)
    }
}
