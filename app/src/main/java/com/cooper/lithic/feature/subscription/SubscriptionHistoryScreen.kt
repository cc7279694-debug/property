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
import com.cooper.lithic.core.design.EmptyState
import com.cooper.lithic.core.design.LithicIcon
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.data.media.MediaRepository

@Composable
internal fun SubscriptionHistoryScreen(state: SubscriptionListUiState, onBack: () -> Unit,
    onOpenSubscription: (String) -> Unit, onLoadMore: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier,
    mediaRepository: MediaRepository? = null) {
    Column(modifier.fillMaxSize().testTag("subscription-history-screen")) {
    LazyColumn(Modifier.fillMaxSize().testTag("subscription-history-content"),
        contentPadding = PaddingValues(horizontal = LithicSpacing.medium, vertical = LithicSpacing.xSmall),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        item {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
                    IconButton(onBack, Modifier.size(48.dp).testTag("subscription-history-back")
                        .semantics { contentDescription = "返回回看" }) {
                        LithicIcon(LithicIcon.Back, null)
                    }
                    Text("已停止订阅", Modifier.weight(1f).semantics { heading() },
                        style = MaterialTheme.typography.headlineMedium, softWrap = true)
                }
                state.totalCount?.let { Text("已停止 $it 条订阅", Modifier.testTag("subscription-history-count"),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text("保留真实付款与状态历史；重新开通沿用原服务。", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.loading) item { Text("正在读取已停止订阅", style = MaterialTheme.typography.bodyLarge) }
        state.error?.let { error -> item {
            Column {
                Text(error, Modifier.testTag("subscription-history-error"), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge)
                if (state.items.isNotEmpty()) Text("以下为上次读取记录。", style = MaterialTheme.typography.bodyMedium)
                TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-history-retry")) {
                    Text("重新读取", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } }
        if (!state.loading && state.error == null && state.totalCount == 0L && state.items.isEmpty()) item {
            EmptyState("暂无已停止订阅", "停止服务记录后，真实付款与状态事实仍保存在这里。",
                Modifier.testTag("subscription-history-empty"), icon = LithicIcon.Subscription)
        }
        items(state.items, key = { it.subscription.id }) { detail ->
            SubscriptionEntryCard(detail, mediaRepository, "subscription-history-row-${detail.subscription.id}") {
                onOpenSubscription(detail.subscription.id)
            }
        }
        if (state.hasMore) item {
            Button(onLoadMore, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("subscription-history-load-more"), enabled = !state.loading && !state.loadingMore) {
                Text(if (state.loadingMore) "正在读取下一页" else "继续读取已停止订阅")
            }
        }
        item { Spacer(Modifier.height(LithicSpacing.medium)) }
    }
    }
}
