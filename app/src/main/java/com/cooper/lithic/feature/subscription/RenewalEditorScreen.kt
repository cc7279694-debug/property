package com.cooper.lithic.feature.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.cooper.lithic.core.design.FormHeader
import com.cooper.lithic.core.design.LithicCard
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
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
        .padding(LithicSpacing.medium).testTag("renewal-editor-screen"),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        val correction = state.kind == SubscriptionEditorKind.CORRECT_RENEWAL
        val reenable = state.kind == SubscriptionEditorKind.REENABLE
        FormHeader(if (correction) "修正真实付款" else if (reenable) "确认真实付款并启用原订阅" else "记录真实续费",
            onBack, "renewal-editor-back", backLabel = "返回订阅详情")
        LithicCard {
            Text(state.serviceName, Modifier.testTag("renewal-service-name"), style = MaterialTheme.typography.titleLarge)
            if (state.loading) Text("正在读取订阅资料", style = MaterialTheme.typography.bodyLarge)
            Text(if (correction) "修改原付款事实并重算历史，不新增付款或退款；原记录身份保持。"
                else if (reenable) "当前和未来均无已确认覆盖，请主动填写实际付款金额和付款日期。只有明确确认后，真实付款和启用状态才会一起保存。"
                else "前次金额和周期仅供参考，请核对本次实际付款及四个日期。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.fixedStopOn?.let { Text("已固定的计划停止日：$it。付款纠错不会自动移动它；扩张停止日后的覆盖前，请明确修正或取消计划。",
                Modifier.testTag("renewal-fixed-stop"), style = MaterialTheme.typography.bodyMedium) }
        }
        SubscriptionPaymentFields(state, "renewal", onAmountChange, onCycleChange, onPaidOnChange,
            onEffectiveFromChange, onPaidUntilChange, onReminderDueChange)
        SubscriptionPaymentActions(state, "renewal", if (correction) "确认修正原付款" else if (reenable) "确认真实付款并启用原订阅" else "确认这笔真实付款",
            onConfirmPayment, onRetryRead)
    }
}
