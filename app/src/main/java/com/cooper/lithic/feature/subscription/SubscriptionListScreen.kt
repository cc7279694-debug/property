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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.EntryCard
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus

@Composable
internal fun SubscriptionListScreen(
    state: SubscriptionListUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpenSubscription: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().testTag("subscription-list-screen")) {
        LazyColumn(Modifier.fillMaxSize().testTag("subscription-list-content"),
            verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                    TextButton(onBack, Modifier.heightIn(min = 48.dp).testTag("subscription-list-back")) { Text("返回现在") }
                    Text("订阅服务", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                    Text("最近实际付款在前；已到期的服务仍保留。", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onCreate, Modifier.heightIn(min = 48.dp).testTag("subscription-list-create")) { Text("新增订阅") }
                    state.totalCount?.let { Text("已录入 $it 条订阅服务", Modifier.testTag("subscription-list-count"),
                        style = MaterialTheme.typography.bodyMedium) }
                }
            }
            if (state.loading) item { Text("正在读取订阅服务", Modifier.padding(LithicSpacing.medium)) }
            state.error?.let { message -> item {
                Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium)) {
                    Text(message, Modifier.testTag("subscription-list-error"), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error)
                    if (state.items.isNotEmpty()) Text("暂时无法刷新，以下为上次读取记录。", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-list-retry")) { Text("重新读取") }
                }
            } }
            if (!state.loading && state.error == null && state.totalCount == 0L && state.items.isEmpty()) item {
                Text("暂无订阅服务", Modifier.padding(LithicSpacing.medium), style = MaterialTheme.typography.bodyLarge)
            }
            for (status in listOf(SubscriptionDisplayStatus.ACTIVE, SubscriptionDisplayStatus.PAUSED, SubscriptionDisplayStatus.EXPIRED)) {
                val group = state.items.filter { it.cost.displayStatus == status }
                if (group.isNotEmpty()) item {
                    Text(if (status == SubscriptionDisplayStatus.PAUSED) "暂停中" else subscriptionStatusLabel(status),
                        Modifier.padding(horizontal = LithicSpacing.medium).testTag("subscription-group-${status.name}").semantics { heading() },
                        style = MaterialTheme.typography.titleLarge)
                }
                items(group, key = { it.subscription.id }) { detail ->
                val lastPaidOn = detail.renewals.maxOfOrNull { it.paidOn }
                val timeLabel = (lastPaidOn?.let { "最近实际付款：$it\n" } ?: "暂无付费记录\n") +
                    "折算付费时长：${detail.cost.standardPurchasedDays}天\n历史平均日成本"
                EntryCard(detail.subscription.name, subscriptionStatusLabel(detail.cost.displayStatus), timeLabel,
                    detail.cost.historicalAverage,
                    Modifier.padding(horizontal = LithicSpacing.medium).testTag("subscription-row-${detail.subscription.id}"),
                    onClick = { onOpenSubscription(detail.subscription.id) })
                }
            }
            if (state.hasMore) item {
                Button(onLoadMore, Modifier.fillMaxWidth().padding(LithicSpacing.medium).heightIn(min = 48.dp)
                    .testTag("subscription-list-load-more"), enabled = !state.loading && !state.loadingMore) {
                    Text(if (state.loadingMore) "正在读取下一页" else "继续加载", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

internal fun subscriptionStatusLabel(status: SubscriptionDisplayStatus): String = when (status) {
    SubscriptionDisplayStatus.ACTIVE -> "订阅中"
    SubscriptionDisplayStatus.EXPIRED -> "已到期"
    SubscriptionDisplayStatus.PAUSED -> "已暂停"
    SubscriptionDisplayStatus.STOPPED -> "已停止"
}
