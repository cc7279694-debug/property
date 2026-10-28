package com.cooper.lithic.feature.subscription

import com.cooper.lithic.data.media.CoverOwnerType
import com.cooper.lithic.data.media.CoverReference
import com.cooper.lithic.feature.media.RecordCover
import com.cooper.lithic.core.design.C2RecordIdentity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.C2PageHeader
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
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
    var informationOpen by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize().testTag("subscription-list-screen")) {
        LazyColumn(Modifier.fillMaxSize().testTag("subscription-list-content"),
            verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                    C2PageHeader("订阅服务", onBack, "subscription-list-back", backLabel = "返回现在",
                        actions = { IconButton({ informationOpen = true }, Modifier.size(48.dp).testTag("subscription-list-information")) {
                            C2Icon(C2Glyph.Info, description = "订阅列表说明")
                        } })
                    Button(onCreate, Modifier.heightIn(min = 48.dp).testTag("subscription-list-create")) { C2Icon(C2Glyph.Add); Text("新增订阅") }
                    state.totalCount?.let { Text("已录入 $it 条订阅服务", Modifier.testTag("subscription-list-count"),
                        style = MaterialTheme.typography.bodyMedium) }
                    if (state.totalCount != null && state.items.isEmpty() &&
                        (state.loading || state.loadingMore || state.error != null)) {
                        Text("数量为上次读取结果，尚不能确认当前列表为空。", Modifier.testTag("subscription-list-count-snapshot"),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state.loading) item { Text("正在读取订阅服务", Modifier.padding(LithicSpacing.medium)) }
            state.error?.let { message -> item {
                Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium)) {
                    Text(message, Modifier.testTag("subscription-list-error"), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error)
                    if (state.items.isNotEmpty()) Text("暂时无法刷新，以下为上次读取记录。",
                        Modifier.testTag("subscription-list-stale"), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-list-retry"),
                        enabled = !state.loading && !state.loadingMore) { Text("重新读取") }
                }
            } }
            if (!state.loading && state.error == null && state.totalCount == null && state.items.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(LithicSpacing.medium)) {
                    Text("尚未取得订阅服务记录，请重新读取；这不表示没有记录。",
                        Modifier.testTag("subscription-list-unavailable"), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-list-retry"),
                        enabled = !state.loadingMore) { Text("重新读取", style = MaterialTheme.typography.bodyLarge) }
                }
            }
            if (state.error == null && (state.loading || state.loadingMore) && state.items.isNotEmpty()) item {
                Text("正在读取；数量和下方记录仍为上次读取结果。",
                    Modifier.padding(LithicSpacing.medium).testTag("subscription-list-refreshing-snapshot"),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!state.loading && !state.loadingMore && state.error == null && state.totalCount == 0L && state.items.isEmpty()) item {
                Text("暂无订阅服务", Modifier.padding(LithicSpacing.medium).testTag("subscription-list-empty"), style = MaterialTheme.typography.bodyLarge)
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
                    "折算付费时长：${detail.cost.standardPurchasedDays}天"
                EntryCard(detail.subscription.name, subscriptionStatusLabel(detail.cost.displayStatus), timeLabel,
                    detail.cost.historicalAverage,
                    Modifier.padding(horizontal = LithicSpacing.medium).testTag("subscription-row-${detail.subscription.id}"),
                    onClick = { onOpenSubscription(detail.subscription.id) }, category = detail.category.name, kind = C2Glyph.Subscription,
                    cover = { RecordCover(CoverReference(CoverOwnerType.SUBSCRIPTION, detail.subscription.id, detail.subscription.coverAssetId),
                        Modifier.size(64.dp)) { C2RecordIdentity(detail.subscription.name, detail.category.name, C2Glyph.Subscription) } },
                    hasCoverReference = detail.subscription.coverAssetId != null, metricLabel = "历史平均日成本")
                }
            }
            if (!state.loading && state.error == null && state.totalCount != null && state.items.isNotEmpty()) item {
                Text(when {
                    state.loadingMore -> "正在读取下一页；已显示 ${state.items.size} / ${state.totalCount} 条"
                    state.hasMore -> "已显示 ${state.items.size} / ${state.totalCount} 条，可继续加载"
                    else -> "已显示 ${state.items.size} 条，已到列表末尾"
                }, Modifier.padding(horizontal = LithicSpacing.medium).testTag("subscription-list-page-status"),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.hasMore) item {
                OutlinedButton(onLoadMore, Modifier.fillMaxWidth().padding(LithicSpacing.medium).heightIn(min = 48.dp)
                    .testTag("subscription-list-load-more"), enabled = !state.loading && !state.loadingMore) {
                    Text(if (state.loadingMore) "正在读取下一页" else "继续加载", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
    if (informationOpen) AlertDialog(onDismissRequest = { informationOpen = false }, title = { Text("订阅列表口径") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("最近真实付款在前。", style = MaterialTheme.typography.bodyMedium)
            Text("暂停和已到期服务仍在这里管理；已停止服务可从“回看 → 历史档案”查看。",
                Modifier.testTag("subscription-list-history-explanation"), style = MaterialTheme.typography.bodyMedium)
            Text("已到期表示当前没有生效套餐，可能包含已预付但尚未开始的周期。",
                Modifier.testTag("subscription-list-expired-explanation"), style = MaterialTheme.typography.bodyMedium)
            Text("列表日均指标为历史平均日成本，不是当前套餐价格；折算付费时长不等于已经使用时长。",
                style = MaterialTheme.typography.bodyMedium)
        } }, confirmButton = { TextButton({ informationOpen = false }, Modifier.testTag("subscription-list-close-information")) { Text("知道了") } })

}

internal fun subscriptionStatusLabel(status: SubscriptionDisplayStatus): String = when (status) {
    SubscriptionDisplayStatus.ACTIVE -> "订阅中"
    SubscriptionDisplayStatus.EXPIRED -> "已到期"
    SubscriptionDisplayStatus.PAUSED -> "已暂停"
    SubscriptionDisplayStatus.STOPPED -> "已停止"
}
