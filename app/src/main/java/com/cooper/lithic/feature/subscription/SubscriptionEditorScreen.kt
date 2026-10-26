package com.cooper.lithic.feature.subscription

import androidx.compose.foundation.layout.FlowRow
import com.cooper.lithic.core.design.C2FormScaffold
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.error as semanticsError
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.UiFeedbackText
import com.cooper.lithic.core.design.UiMessageTone
import com.cooper.lithic.core.design.AmountField
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.NameField
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.feature.project.ProjectDateInput
import com.cooper.lithic.feature.project.ProjectNoteInput
import com.cooper.lithic.feature.project.SubmissionPhase
import java.time.LocalDate

@Composable
internal fun SubscriptionEditorScreen(
    state: SubscriptionEditorUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCycleChange: (BillingCycle) -> Unit,
    onPaidOnChange: (String) -> Unit,
    onEffectiveFromChange: (String) -> Unit,
    onPaidUntilChange: (String) -> Unit,
    onReminderDueChange: (String) -> Unit,
    onConfirmPayment: () -> Unit,
    onRetryRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    C2FormScaffold("新增订阅服务", onBack, "subscription-editor-back", modifier.testTag("subscription-editor-screen"),
        footer = { SubscriptionPaymentActions(state, "subscription", "确认已付款并创建订阅", onConfirmPayment, onRetryRead) }) {
        if (state.loading) Text("正在读取订阅资料", style = MaterialTheme.typography.bodyLarge)
        NameField(state.serviceName, onNameChange, Modifier.testTag("subscription-name"),
            label = "服务名称", error = state.errors["serviceName"], enabled = state.canEdit, labelOutside = true)
        SubscriptionCategoryPicker(state.categories, state.categoryId, onCategoryChange,
            state.errors["categoryId"], state.canEdit)
        Text("首次真实付款", style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() })
        Text("请核对实际付款及四个日期；预填不代表付款已经确认。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SubscriptionPaymentFields(state, "subscription", onAmountChange, onCycleChange, onPaidOnChange,
            onEffectiveFromChange, onPaidUntilChange, onReminderDueChange)
        ProjectNoteInput(state.note, onNoteChange, "备注（可选）", "subscription-note", state.canEdit, state.errors["note"])
    }
}

@Composable
internal fun SubscriptionPaymentFields(
    state: SubscriptionEditorUiState,
    prefix: String,
    onAmountChange: (String) -> Unit,
    onCycleChange: (BillingCycle) -> Unit,
    onPaidOnChange: (String) -> Unit,
    onEffectiveFromChange: (String) -> Unit,
    onPaidUntilChange: (String) -> Unit,
    onReminderDueChange: (String) -> Unit,
) {
    AmountField(state.amount, onAmountChange, Modifier.testTag("$prefix-amount"),
        label = if (state.kind == SubscriptionEditorKind.CREATE) "首次实际付款金额（元）" else "本次实际付款金额（元）",
        error = state.errors["amount"], enabled = state.canEdit, labelOutside = true)
    Text("仅记录已实际付款，金额必须大于0元。免费期不生成付费记录。",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("固定付费周期：${billingCycleLabel(state.cycle)}（当前选择）", style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.testTag("$prefix-cycle-selection"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BillingCycle.entries.forEach { cycle ->
            val isSelected = state.cycle == cycle
            OutlinedButton(onClick = { onCycleChange(cycle) }, enabled = state.canEdit,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary),
                modifier = Modifier.heightIn(min = 48.dp).testTag("$prefix-cycle-${cycle.name}")
                    .semantics { selected = isSelected }) {
                Text("${if (isSelected) "已选 · " else ""}${billingCycleLabel(cycle)}", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    Text("30、90、365天是成本折算口径，不代表已经使用的天数；请按实际服务核对下面的覆盖日期。",
        Modifier.testTag("$prefix-cycle-explanation"), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    state.errors["cycle"]?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    PaymentDate(state.paidOn, onPaidOnChange, state.today, "真实付款日期", "$prefix-paid-on",
        state.errors["paidOn"], state.canEdit)
    Text("实际已完成付款的日期，不能晚于记录时区的今天。", style = MaterialTheme.typography.bodyMedium)
    PaymentDate(state.effectiveFrom, onEffectiveFromChange, state.today, "实际生效日期", "$prefix-effective-from",
        state.errors["effectiveFrom"], state.canEdit)
    Text("这笔付款对应的真实服务覆盖起点，可以晚于付款日期。预付未来周期要到生效日才影响当前套餐。",
        Modifier.testTag("$prefix-effective-explanation"), style = MaterialTheme.typography.bodyMedium)
    PaymentDate(state.paidUntilExclusive, onPaidUntilChange, state.today, "付费到期边界（当天不包含）", "$prefix-paid-until",
        state.errors["paidUntilExclusive"], state.canEdit)
    Text("付费到期边界当天不包含在前一期。", style = MaterialTheme.typography.bodyMedium)
    PaymentDate(state.reminderDueOn, onReminderDueChange, state.today, "提醒到期日", "$prefix-reminder-due",
        state.errors["reminderDueOn"], state.canEdit)
    Text("提醒到期日独立于付费覆盖，修改它不会延长服务。在“设置”开启续费提醒并允许系统通知后，应用会依据此日期尽力提醒，不保证准点；这不代表服务商已经扣款。",
        Modifier.testTag("$prefix-reminder-explanation"), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PaymentDate(
    value: String, onChange: (String) -> Unit, today: LocalDate?, label: String, prefix: String,
    error: String?, enabled: Boolean,
) {
    if (today != null) ProjectDateInput(value, onChange, today, label, prefix, error = error, enabled = enabled, labelOutside = true)
    else OutlinedTextField(value, onChange, label = { Text(label) }, enabled = false,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("$prefix-input"),
        textStyle = MaterialTheme.typography.bodyLarge)
}

@Composable
internal fun SubscriptionPaymentActions(
    state: SubscriptionEditorUiState, prefix: String, confirmLabel: String,
    onConfirmPayment: () -> Unit, onRetryRead: () -> Unit,
) {
    state.message?.let { UiFeedbackText(it, UiMessageTone.ERROR, "$prefix-form-error") }
    if (state.busy) Text("正在保存到本地", style = MaterialTheme.typography.bodyLarge)
    when {
        state.verifiedCannotWrite -> {
            UiFeedbackText("已核对当前记录，本次输入不能提交。原始输入已保留，请返回后重新开始。",
                UiMessageTone.WARNING, "$prefix-cannot-write")
        }
        state.phase == SubmissionPhase.COMMITTED -> {
            UiFeedbackText("付款已确认保存。下面只重新读取结果，不会再记录一笔付款。",
                UiMessageTone.INFORMATION, "$prefix-payment-saved")
            if (state.message == null) Text(if (state.loading) "正在读取当前详情；不会重复付款。"
                else "可重新读取当前详情；不会重复付款。", Modifier.testTag("$prefix-payment-read-status"),
                style = MaterialTheme.typography.bodyMedium)
            Button(onRetryRead, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$prefix-retry-read"),
                enabled = !state.loading && !state.busy) { Text(if (state.conflict) "核对当前事实并安全返回" else "重新读取已保存订阅", style = MaterialTheme.typography.bodyLarge) }
        }
        state.conflict -> {
            UiFeedbackText("请先核对已保存的付款事实；当前输入和原付款身份仍保留。",
                UiMessageTone.WARNING, "$prefix-conflict-notice")
            Button(onRetryRead, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$prefix-resolve-conflict"),
                enabled = !state.loading && !state.busy) { Text("核对已保存付款", style = MaterialTheme.typography.bodyLarge) }
        }
        state.needsInitialRead -> {
            Button(onRetryRead, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$prefix-retry-read"),
                enabled = !state.loading && !state.busy) { Text("重新读取订阅资料", style = MaterialTheme.typography.bodyLarge) }
        }
        else -> {
            if (state.phase == SubmissionPhase.UNCONFIRMED) UiFeedbackText(
                "付款记录是否保存尚未确认；核对并重试会重放同一笔记录。请勿另建订阅或再记一笔付款。",
                UiMessageTone.WARNING, "$prefix-payment-unconfirmed")
            Button(onConfirmPayment, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$prefix-confirm-payment"),
                enabled = !state.loading && !state.busy) {
                Text(if (state.phase == SubmissionPhase.UNCONFIRMED) "核对并重试同一付款" else confirmLabel,
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SubscriptionCategoryPicker(
    categories: List<CategoryEntity>, selectedId: String, onSelect: (String) -> Unit,
    error: String?, enabled: Boolean,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val options = categories.filter { it.type == CategoryType.SUBSCRIPTION }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
        OutlinedButton(onClick = { open = true }, enabled = enabled && options.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("subscription-category")
                .semantics { error?.let { semanticsError(it) } }) {
            Column(Modifier.fillMaxWidth()) {
                Text("订阅类别", style = MaterialTheme.typography.bodyMedium)
                Text(options.firstOrNull { it.id == selectedId }?.name ?: "请选择订阅类别", style = MaterialTheme.typography.bodyLarge)
            }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
    }
    if (open && enabled) AlertDialog(onDismissRequest = { open = false }, title = { Text("选择订阅类别") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            options.forEach { category ->
                TextButton(onClick = { onSelect(category.id); open = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("subscription-category-${category.id}")) {
                    Text(category.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } }, confirmButton = { TextButton({ open = false }, Modifier.heightIn(min = 48.dp)) { Text("取消") } })
}

internal fun billingCycleLabel(cycle: BillingCycle): String = when (cycle) {
    BillingCycle.MONTHLY -> "月付"
    BillingCycle.QUARTERLY -> "季付"
    BillingCycle.YEARLY -> "年付"
}
