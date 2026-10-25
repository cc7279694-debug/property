package com.cooper.lithic.feature.project

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

@Composable
internal fun ProjectHistoryScreen(
    state: ProjectListUiState,
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().testTag("project-history-screen")) {
    LazyColumn(Modifier.fillMaxSize().testTag("project-history-content"),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                TextButton(onBack, Modifier.heightIn(min = 48.dp).testTag("project-history-back")) {
                    Text("返回回看", style = MaterialTheme.typography.bodyLarge)
                }
                Text("历史档案", style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() })
                Text("已结束的长期项目。结果包含已记录的真实尾款与退款。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.totalCount?.let { Text("已结束 $it 条长期项目", style = MaterialTheme.typography.bodyMedium) }
                state.error?.let {
                    Text(it, Modifier.testTag("project-history-error"), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error)
                    if (state.items.isNotEmpty()) {
                        Text("暂时无法刷新，以下为上次读取记录。", Modifier.testTag("project-history-stale"),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("project-history-retry")) {
                        Text("重新读取", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        if (state.loading) item { Text("正在读取历史档案", Modifier.padding(LithicSpacing.medium)) }
        if (!state.loading && state.error == null && state.items.isEmpty()) {
            item { Text("暂无已结束项目", Modifier.padding(LithicSpacing.medium), style = MaterialTheme.typography.bodyLarge) }
        }
        items(state.items, key = { it.project.id }) { detail ->
            val dates = "开始：${detail.project.startedOn}" +
                (detail.project.endedOn?.let { "\n结束：$it" } ?: "") +
                "\n已持有${detail.cost.heldDays}天\n结果截至 ${detail.asOf}"
            EntryCard(detail.project.name, "已结束", dates, detail.cost.dailyRate,
                Modifier.padding(horizontal = LithicSpacing.medium).testTag("project-history-row-${detail.project.id}"),
                onClick = { onOpenProject(detail.project.id) })
        }
        if (state.hasMore) item {
            Button(onLoadMore, Modifier.fillMaxWidth().padding(LithicSpacing.medium)
                .heightIn(min = 48.dp).testTag("project-history-load-more"), enabled = !state.loading && !state.loadingMore) {
                Text(if (state.loadingMore) "正在读取下一页" else "继续加载", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    }
}
