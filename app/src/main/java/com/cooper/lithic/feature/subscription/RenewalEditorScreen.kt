package com.cooper.lithic.feature.subscription

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
import com.cooper.lithic.core.design.C2FormScaffold
import com.cooper.lithic.core.design.C5InformationButton
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.domain.subscription.BillingCycle

@Composable
internal fun RenewalEditorScreen(
    state: SubscriptionEditorUiState,
    onBack: () -> Unit,
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
    val correction = state.kind == SubscriptionEditorKind.CORRECT_RENEWAL
    val reenable = state.kind == SubscriptionEditorKind.REENABLE
    C2FormScaffold(if (correction) "修正真实付款" else if (reenable) "确认真实付款并启用原订阅" else "记录真实续费",
        onBack, "renewal-editor-back", modifier.testTag("renewal-editor-screen"), backLabel = "返回订阅详情",
        footer = { SubscriptionPaymentActions(state, "renewal", if (correction) "确认修正原付款" else if (reenable) "确认真实付款并启用原订阅" else "确认这笔真实付款",
            onConfirmPayment, onRetryRead) }) {
        Text(state.serviceName, Modifier.testTag("renewal-service-name"), style = MaterialTheme.typography.titleLarge)
        if (state.loading) Text("正在读取订阅资料", style = MaterialTheme.typography.bodyLarge)
        if (correction || reenable) {
            Text(if (correction) "修改原付款事实并重算历史，不新增付款或退款；原记录身份保持。"
                else "当前和未来均无已确认覆盖，请主动填写实际付款金额和付款日期。只有明确确认后，真实付款和启用状态才会一起保存。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.fixedStopOn?.let { Text("已固定的计划停止日：$it。付款纠错不会自动移动它；扩张停止日后的覆盖前，请明确修正或取消计划。",
            Modifier.testTag("renewal-fixed-stop"), style = MaterialTheme.typography.bodyMedium) }
        SubscriptionPaymentFields(state, "renewal", onAmountChange, onCycleChange, onPaidOnChange,
            onEffectiveFromChange, onPaidUntilChange, onReminderDueChange)
        if (!correction && !reenable) C5InformationButton("续费录入说明", Modifier.testTag("renewal-reference-information"), buttonLabel = "续费录入说明") {
            Text("前次金额和周期仅供参考，请核对本次实际付款及四个日期。确认只保存这一笔真实付款。")
        }
    }
}
