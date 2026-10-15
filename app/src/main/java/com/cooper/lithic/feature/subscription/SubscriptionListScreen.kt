package com.cooper.lithic.feature.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.EntryCard
import com.cooper.lithic.core.design.EmptyState
import com.cooper.lithic.core.design.LithicIcon
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.feature.media.CoverThumbnail

@Composable
internal fun SubscriptionListScreen(
    state: SubscriptionListUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpenSubscription: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    mediaRepository: MediaRepository? = null,
) {
    Column(modifier.fillMaxSize().testTag("subscription-list-screen")) {
        LazyColumn(Modifier.fillMaxSize().testTag("subscription-list-content"),
            contentPadding = PaddingValues(horizontal = LithicSpacing.medium, vertical = LithicSpacing.xSmall),
            verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
                        IconButton(onBack, Modifier.size(48.dp).testTag("subscription-list-back")
                            .semantics { contentDescription = "返回现在" }) {
                            LithicIcon(LithicIcon.Back, null)
                        }
                        Text("订阅服务", Modifier.weight(1f).semantics { heading() },
                            style = MaterialTheme.typography.headlineMedium, softWrap = true)
                        IconButton(onCreate, Modifier.size(48.dp).testTag("subscription-list-create")
                            .semantics { contentDescription = "新增订阅" }) {
                            LithicIcon(LithicIcon.Add, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    state.totalCount?.let { Text("已录入 $it 条订阅服务", Modifier.testTag("subscription-list-count"),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text("最近实际付款在前；已停止订阅在回看中。", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.loading) item { Text("正在读取订阅服务", style = MaterialTheme.typography.bodyLarge) }
            state.error?.let { message -> item {
                Column(Modifier.fillMaxWidth()) {
                    Text(message, Modifier.testTag("subscription-list-error"), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error)
                    if (state.items.isNotEmpty()) Text("暂时无法刷新，以下为上次读取记录。", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-list-retry")) { Text("重新读取") }
                }
            } }
            if (!state.loading && state.error == null && state.totalCount == 0L && state.items.isEmpty()) item {
                EmptyState("暂无订阅服务", "记录一项已付款的服务，慢慢看清长期成本。", icon = LithicIcon.Subscription)
            }
            for (status in listOf(SubscriptionDisplayStatus.ACTIVE, SubscriptionDisplayStatus.PAUSED, SubscriptionDisplayStatus.EXPIRED)) {
                val group = state.items.filter { it.cost.displayStatus == status }
                if (group.isNotEmpty()) item {
                    Text(if (status == SubscriptionDisplayStatus.PAUSED) "暂停中" else subscriptionStatusLabel(status),
                        Modifier.testTag("subscription-group-${status.name}").semantics { heading() },
                        style = MaterialTheme.typography.titleMedium)
                }
                items(group, key = { it.subscription.id }) { detail ->
                    SubscriptionEntryCard(detail, mediaRepository, "subscription-row-${detail.subscription.id}") {
                        onOpenSubscription(detail.subscription.id)
                    }
                }
            }
            if (state.hasMore) item {
                Button(onLoadMore, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .testTag("subscription-list-load-more"), enabled = !state.loading && !state.loadingMore) {
                    Text(if (state.loadingMore) "正在读取下一页" else "继续加载", style = MaterialTheme.typography.bodyLarge)
                }
            }
            item { Spacer(Modifier.height(LithicSpacing.medium)) }
        }
    }
}

/** Presentation of existing facts only; ordering/pagination remain owned by the repository and caller. */
@Composable
internal fun SubscriptionEntryCard(detail: SubscriptionDetail, mediaRepository: MediaRepository?, tag: String,
    onClick: () -> Unit) {
    val lastPaidOn = detail.renewals.maxOfOrNull { it.paidOn }
    val dates = (lastPaidOn?.let { "最近实际付款：$it\n" } ?: "暂无付费记录\n") +
        "折算付费时长：${detail.cost.standardPurchasedDays}天\n历史平均日成本"
    val cover: (@Composable () -> Unit)? = if (detail.subscription.coverAssetId == null) null else {
        { CoverThumbnail(detail.subscription.coverAssetId, mediaRepository, "subscription-${detail.subscription.id}") }
    }
    EntryCard(detail.subscription.name, subscriptionStatusLabel(detail.cost.displayStatus), dates,
        detail.cost.historicalAverage, Modifier.testTag(tag), onClick = onClick, cover = cover,
        placeholderIcon = LithicIcon.Subscription)
}

internal fun subscriptionStatusLabel(status: SubscriptionDisplayStatus): String = when (status) {
    SubscriptionDisplayStatus.ACTIVE -> "订阅中"
    SubscriptionDisplayStatus.EXPIRED -> "已到期"
    SubscriptionDisplayStatus.PAUSED -> "已暂停"
    SubscriptionDisplayStatus.STOPPED -> "已停止"
}
