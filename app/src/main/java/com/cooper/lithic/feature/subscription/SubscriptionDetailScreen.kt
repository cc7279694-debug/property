package com.cooper.lithic.feature.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountText
import com.cooper.lithic.core.design.AmountUnit
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import java.math.BigDecimal
import java.time.LocalDate

@Composable
internal fun SubscriptionDetailScreen(
    state: SubscriptionDetailUiState,
    onBack: () -> Unit,
    onRenewal: () -> Unit,
    onLoadMoreHistory: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().testTag("subscription-detail-screen")) {
        LazyColumn(Modifier.fillMaxSize().testTag("subscription-detail-content"),
            verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
            item { TextButton(onBack, Modifier.padding(horizontal = LithicSpacing.medium).heightIn(min = 48.dp)
                .testTag("subscription-detail-back")) { Text("返回订阅列表") } }
            if (state.loading) item { Text("正在读取订阅", Modifier.padding(LithicSpacing.medium)) }
            state.error?.let { message -> item {
                Column(Modifier.padding(horizontal = LithicSpacing.medium)) {
                    Text(message, Modifier.testTag("subscription-detail-error"), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge)
                    if (state.detail != null) Text("以下为上次读取记录。", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-detail-retry")) { Text("重新读取") }
                }
            } }
            if (state.missing) item { Text("订阅不存在，返回列表查看当前记录。", Modifier.padding(LithicSpacing.medium)
                .testTag("subscription-detail-missing"), style = MaterialTheme.typography.bodyLarge) }
            state.detail?.takeUnless { state.missing }?.let { detail ->
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
                        verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                        Text(detail.subscription.name, Modifier.testTag("subscription-detail-name").semantics { heading() },
                            style = MaterialTheme.typography.headlineLarge)
                        Text(subscriptionStatusLabel(detail.cost.displayStatus), Modifier.testTag("subscription-detail-status"),
                            style = MaterialTheme.typography.bodyLarge)
                        Text("结果截至 ${detail.asOf}", style = MaterialTheme.typography.bodyMedium)
                        Text("历史平均日成本", style = MaterialTheme.typography.bodyMedium)
                        AmountText(detail.cost.historicalAverage, Modifier.testTag("subscription-detail-historical-average"))
                        Text("折算付费时长：${detail.cost.standardPurchasedDays}天",
                            Modifier.testTag("subscription-detail-purchased-days"), style = MaterialTheme.typography.bodyLarge)
                        Text("按月30天、季90天、年365天折算；包含已确认付款的未来周期，不代表实际已使用时长。",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (detail.cost.historicalAverage == null) Text("暂无已确认付款，历史平均日成本暂无数据。",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium)
                        .testTag("subscription-detail-current-plan"), verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                        HorizontalDivider()
                        Text("当前套餐", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        val current = detail.currentRenewal
                        if (current != null && detail.cost.currentPrice != null) {
                            Text(billingCycleLabel(current.cycle), style = MaterialTheme.typography.bodyLarge)
                            AmountText(BigDecimal.valueOf(detail.cost.currentPrice.cents, 2), unit = AmountUnit.YUAN,
                                style = MaterialTheme.typography.titleLarge)
                            Text("当前标准日均价", style = MaterialTheme.typography.bodyMedium)
                            AmountText(detail.cost.currentRate, Modifier.testTag("subscription-detail-current-rate"),
                                style = MaterialTheme.typography.titleLarge)
                            Text("实际生效日期：${current.effectiveFrom}", style = MaterialTheme.typography.bodyMedium)
                            Text("付费到期边界：${current.paidUntilExclusive}（当天不包含）", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("当前无有效套餐", Modifier.testTag("subscription-detail-no-current-plan"),
                                style = MaterialTheme.typography.bodyLarge)
                            Text("当前成本贡献为0元/天", style = MaterialTheme.typography.bodyMedium)
                            if (detail.renewals.any { !it.paidOn.isAfter(detail.asOf) && it.effectiveFrom.isAfter(detail.asOf) })
                                Text("已付下一期尚未生效。", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
                        verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                        Text("累计已确认实际付款", style = MaterialTheme.typography.bodyMedium)
                        AmountText(BigDecimal.valueOf(detail.cost.confirmedPaidCents, 2),
                            Modifier.testTag("subscription-detail-total-paid"), AmountUnit.YUAN,
                            style = MaterialTheme.typography.titleLarge)
                        Text("类别：${detail.category.name}", style = MaterialTheme.typography.bodyLarge)
                        detail.subscription.note?.takeIf { it.isNotBlank() }?.let {
                            Text("备注", style = MaterialTheme.typography.bodyMedium)
                            Text(it, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text("实际已过去的付费有效天数：${detail.cost.elapsedPaidDays}天",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    OutlinedButton(onRenewal, Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium)
                        .heightIn(min = 48.dp).testTag("subscription-detail-renewal"), enabled = !state.loading && state.error == null) {
                        Text("记录真实续费", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = LithicSpacing.medium), verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                        HorizontalDivider()
                        Text("真实付款历史", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        Text("按真实付款日期排序；付费到期边界当天不包含在前一期。", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (state.history.loading) item { Text("正在读取付款历史", Modifier.padding(LithicSpacing.medium)) }
                state.history.error?.let { message -> item {
                    Column(Modifier.padding(horizontal = LithicSpacing.medium)) {
                        Text(message, Modifier.testTag("subscription-detail-history-error"), color = MaterialTheme.colorScheme.error)
                        if (state.history.items.isNotEmpty()) Text("以下为上次读取的付款历史。", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-detail-history-retry")) { Text("重新读取付款历史") }
                    }
                } }
                if (!state.history.loading && state.history.error == null && state.history.totalCount == 0L && state.history.items.isEmpty()) item {
                    Text("暂无付费记录", Modifier.padding(LithicSpacing.medium), style = MaterialTheme.typography.bodyLarge)
                }
                items(state.history.items, key = { it.id }) { renewal -> RenewalHistoryRow(renewal, detail.asOf) }
                if (state.history.hasMore) item {
                    Button(onLoadMoreHistory, Modifier.fillMaxWidth().padding(LithicSpacing.medium).heightIn(min = 48.dp)
                        .testTag("subscription-detail-load-more-history"), enabled = !state.history.loading && !state.history.loadingMore) {
                        Text(if (state.history.loadingMore) "正在读取下一页" else "继续读取付款历史", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun RenewalHistoryRow(renewal: SubscriptionRenewalEntity, asOf: LocalDate) {
    Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium).testTag("subscription-renewal-${renewal.id}"),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
        Text("真实付款日期：${renewal.paidOn}", style = MaterialTheme.typography.bodyLarge)
        AmountText(BigDecimal.valueOf(renewal.amountCents, 2), unit = AmountUnit.YUAN, style = MaterialTheme.typography.titleLarge)
        Text("${billingCycleLabel(renewal.cycle)} · 折算${renewal.standardDays}天", style = MaterialTheme.typography.bodyLarge)
        if (renewal.effectiveFrom.isAfter(asOf)) Text("尚未生效", Modifier.testTag("subscription-renewal-future-${renewal.id}"),
            style = MaterialTheme.typography.bodyLarge)
        Text("实际生效日期：${renewal.effectiveFrom}", style = MaterialTheme.typography.bodyMedium)
        Text("付费到期边界：${renewal.paidUntilExclusive}（当天不包含）", style = MaterialTheme.typography.bodyMedium)
        Text("提醒到期日：${renewal.reminderDueOn}", style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider()
    }
}
