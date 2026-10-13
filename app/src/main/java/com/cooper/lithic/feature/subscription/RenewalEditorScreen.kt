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
        TextButton(onBack, Modifier.heightIn(min = 48.dp).testTag("renewal-editor-back")) { Text("返回订阅详情") }
        Text("记录真实续费", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() })
        Text(state.serviceName, Modifier.testTag("renewal-service-name"), style = MaterialTheme.typography.titleLarge)
        if (state.loading) Text("正在读取订阅资料", style = MaterialTheme.typography.bodyLarge)
        Text("前次金额和周期仅供参考，请核对本次实际付款及四个日期。",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SubscriptionPaymentFields(state, "renewal", onAmountChange, onCycleChange, onPaidOnChange,
            onEffectiveFromChange, onPaidUntilChange, onReminderDueChange)
        SubscriptionPaymentActions(state, "renewal", "确认这笔真实付款", onConfirmPayment, onRetryRead)
    }
}
