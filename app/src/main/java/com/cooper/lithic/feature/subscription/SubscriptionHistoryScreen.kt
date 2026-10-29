package com.cooper.lithic.feature.subscription

import androidx.compose.foundation.layout.size
import com.cooper.lithic.data.media.CoverOwnerType
import com.cooper.lithic.data.media.CoverReference
import com.cooper.lithic.feature.media.RecordCover
import com.cooper.lithic.core.design.C2RecordIdentity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import com.cooper.lithic.core.design.LithicOutlinedButton as OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.C5InformationButton
import com.cooper.lithic.core.design.C2PageHeader
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.EntryCard
import com.cooper.lithic.core.design.LithicSpacing

@Composable
internal fun SubscriptionHistoryScreen(state: SubscriptionListUiState, onBack: () -> Unit,
    onOpenSubscription: (String) -> Unit, onLoadMore: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().testTag("subscription-history-screen")) {
    LazyColumn(Modifier.fillMaxSize().testTag("subscription-history-content"),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.page), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                C2PageHeader("已停止订阅", onBack, "subscription-history-back", backLabel = "返回回看",
                    actions = { C5InformationButton("已停止订阅说明") { Text("保留原服务的真实付款和状态历史。暂停及已到期服务仍在当前管理。") } })
                state.totalCount?.let { Text("已停止 $it 条订阅", Modifier.testTag("subscription-history-count")) }
                if (state.totalCount != null && state.items.isEmpty() &&
                    (state.loading || state.loadingMore || state.error != null)) {
                    Text("数量为上次读取结果，尚不能确认当前列表为空。", Modifier.testTag("subscription-history-count-snapshot"),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (state.loading) item { Text("正在读取已停止订阅", Modifier.padding(LithicSpacing.medium)) }
        state.error?.let { error -> item {
            Column(Modifier.padding(LithicSpacing.medium)) {
                Text(error, Modifier.testTag("subscription-history-error"), color = MaterialTheme.colorScheme.error)
                if (state.items.isNotEmpty()) Text("以下为上次读取记录。", Modifier.testTag("subscription-history-stale"),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-history-retry"),
                    enabled = !state.loading && !state.loadingMore) { Text("重新读取") }
            }
        } }
        if (!state.loading && state.error == null && state.totalCount == null && state.items.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(LithicSpacing.medium)) {
                Text("尚未取得已停止订阅记录，请重新读取；这不表示没有记录。",
                    Modifier.testTag("subscription-history-unavailable"), style = MaterialTheme.typography.bodyLarge)
                TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-history-retry"),
                    enabled = !state.loadingMore) { Text("重新读取", style = MaterialTheme.typography.bodyLarge) }
            }
        }
        if (state.error == null && (state.loading || state.loadingMore) && state.items.isNotEmpty()) item {
            Text("正在读取；数量和下方记录仍为上次读取结果。",
                Modifier.padding(LithicSpacing.medium).testTag("subscription-history-refreshing-snapshot"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!state.loading && !state.loadingMore && state.error == null && state.totalCount == 0L && state.items.isEmpty()) item {
            Text("暂无已停止订阅", Modifier.padding(LithicSpacing.medium).testTag("subscription-history-empty"))
        }
        items(state.items, key = { it.subscription.id }) { detail ->
            EntryCard(detail.subscription.name, "已停止", "折算付费时长：${detail.cost.standardPurchasedDays}天",
                detail.cost.historicalAverage, Modifier.padding(horizontal = LithicSpacing.page)
                    .testTag("subscription-history-row-${detail.subscription.id}"), onClick = { onOpenSubscription(detail.subscription.id) }, category = detail.category.name, kind = C2Glyph.Subscription,
                    cover = { RecordCover(CoverReference(CoverOwnerType.SUBSCRIPTION, detail.subscription.id, detail.subscription.coverAssetId),
                        Modifier.size(64.dp)) { C2RecordIdentity(detail.subscription.name, detail.category.name, C2Glyph.Subscription) } },
                    hasCoverReference = detail.subscription.coverAssetId != null, metricLabel = "历史平均日成本")
        }
        if (!state.loading && state.error == null && state.totalCount != null && state.items.isNotEmpty()) item {
            Text(when {
                state.loadingMore -> "正在读取下一页；已显示 ${state.items.size} / ${state.totalCount} 条"
                state.hasMore -> "已显示 ${state.items.size} / ${state.totalCount} 条，可继续加载"
                else -> "已显示 ${state.items.size} 条，已到列表末尾"
            }, Modifier.padding(horizontal = LithicSpacing.page).testTag("subscription-history-page-status"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.hasMore) item {
            OutlinedButton(onLoadMore, Modifier.fillMaxWidth().padding(LithicSpacing.medium).heightIn(min = 48.dp)
                .testTag("subscription-history-load-more"), enabled = !state.loading && !state.loadingMore) {
                Text(if (state.loadingMore) "正在读取下一页" else "继续读取已停止订阅")
            }
        }
    }
    }
}
