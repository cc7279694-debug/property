package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.cooper.lithic.data.repository.ProjectDetail

@Composable
internal fun ProjectListScreen(
    state: ProjectListUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpenProject: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().testTag("project-list-screen")) {
        LazyColumn(Modifier.fillMaxSize().testTag("project-list-content"),
            verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                    C2PageHeader("长期项目", onBack, "project-list-back", backLabel = "返回现在")
                    Text("尚未结束的项目；已结束记录可在回看中的历史档案查看。",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onCreate, Modifier.heightIn(min = 48.dp).testTag("project-list-create")) {
                        C2Icon(C2Glyph.Add)
                        Text("新增长期项目", style = MaterialTheme.typography.bodyLarge)
                    }
                    state.totalCount?.let { Text("当前列表 $it 条长期项目", style = MaterialTheme.typography.bodyMedium) }
                    if (state.totalCount != null && state.items.isEmpty() &&
                        (state.loading || state.loadingMore || state.error != null)) {
                        Text("数量为上次读取结果，尚不能确认当前列表为空。", Modifier.testTag("project-list-count-snapshot"),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    state.error?.let {
                        Text(it, Modifier.testTag("project-list-error"), style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error)
                        if (state.items.isNotEmpty()) {
                            Text("暂时无法刷新，以下为上次读取记录。", Modifier.testTag("project-list-stale"),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("project-list-retry"),
                            enabled = !state.loading && !state.loadingMore) {
                            Text("重新读取", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            if (state.loading) item { Text("正在读取长期项目", Modifier.padding(LithicSpacing.medium)) }
            if (!state.loading && state.error == null && state.totalCount == null && state.items.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(LithicSpacing.medium)) {
                    Text("尚未取得长期项目记录，请重新读取；这不表示没有记录。",
                        Modifier.testTag("project-list-unavailable"), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("project-list-retry"),
                        enabled = !state.loadingMore) { Text("重新读取", style = MaterialTheme.typography.bodyLarge) }
                }
            }
            if (state.error == null && (state.loading || state.loadingMore) && state.items.isNotEmpty()) item {
                Text("正在读取；数量和下方记录仍为上次读取结果。",
                    Modifier.padding(LithicSpacing.medium).testTag("project-list-refreshing-snapshot"),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!state.loading && !state.loadingMore && state.error == null && state.totalCount == 0L && state.items.isEmpty()) {
                item { Text("暂无长期项目", Modifier.padding(LithicSpacing.medium).testTag("project-list-empty"), style = MaterialTheme.typography.bodyLarge) }
            }
            items(state.items, key = { it.project.id }) { detail ->
                val dates = "开始：${detail.project.startedOn}" +
                    (detail.project.endedOn?.let { "\n结束：$it" } ?: "") +
                    if (detail.cost.unavailableReason == null) "\n已持有${detail.cost.heldDays}天" else ""
                EntryCard(detail.project.name, projectStatusLabel(detail), dates, detail.cost.dailyRate,
                    Modifier.padding(horizontal = LithicSpacing.medium).testTag("project-row-${detail.project.id}"),
                    onClick = { onOpenProject(detail.project.id) }, category = detail.category.name,
                    hasCoverReference = detail.project.coverAssetId != null, metricLabel = "当前日均成本")
            }
            if (!state.loading && state.error == null && state.totalCount != null && state.items.isNotEmpty()) item {
                Text(when {
                    state.loadingMore -> "正在读取下一页；已显示 ${state.items.size} / ${state.totalCount} 条"
                    state.hasMore -> "已显示 ${state.items.size} / ${state.totalCount} 条，可继续加载"
                    else -> "已显示 ${state.items.size} 条，已到列表末尾"
                }, Modifier.padding(horizontal = LithicSpacing.medium).testTag("project-list-page-status"),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.hasMore) item {
                OutlinedButton(onLoadMore, Modifier.fillMaxWidth().padding(LithicSpacing.medium)
                    .heightIn(min = 48.dp).testTag("project-list-load-more"), enabled = !state.loadingMore && !state.loading) {
                    Text(if (state.loadingMore) "正在读取下一页" else "继续加载", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

internal fun projectStatusLabel(detail: ProjectDetail): String = when {
    detail.cost.unavailableReason != null -> "尚未开始"
    detail.project.endedOn != null && !detail.cost.contributesToCurrentTotal -> "已结束"
    detail.project.endedOn != null -> "已记录结束日期"
    else -> "持有中"
}
