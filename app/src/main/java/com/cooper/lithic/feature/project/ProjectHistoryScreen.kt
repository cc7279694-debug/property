package com.cooper.lithic.feature.project

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
import com.cooper.lithic.core.design.C5InformationButton
import com.cooper.lithic.core.design.C2PageHeader
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
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
                C2PageHeader("历史档案", onBack, "project-history-back", backLabel = "返回回看",
                    actions = { C5InformationButton("历史档案说明") { Text("已结束的长期项目。结果包含已记录的真实尾款与退款。") } })
                state.totalCount?.let { Text("已结束 $it 条长期项目", style = MaterialTheme.typography.bodyMedium) }
                if (state.totalCount != null && state.items.isEmpty() &&
                    (state.loading || state.loadingMore || state.error != null)) {
                    Text("数量为上次读取结果，尚不能确认当前列表为空。", Modifier.testTag("project-history-count-snapshot"),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.error?.let {
                    Text(it, Modifier.testTag("project-history-error"), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error)
                    if (state.items.isNotEmpty()) {
                        Text("暂时无法刷新，以下为上次读取记录。", Modifier.testTag("project-history-stale"),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("project-history-retry"),
                        enabled = !state.loading && !state.loadingMore) {
                        Text("重新读取", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        if (state.loading) item { Text("正在读取历史档案", Modifier.padding(LithicSpacing.medium)) }
        if (!state.loading && state.error == null && state.totalCount == null && state.items.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(LithicSpacing.medium)) {
                Text("尚未取得历史档案记录，请重新读取；这不表示没有记录。",
                    Modifier.testTag("project-history-unavailable"), style = MaterialTheme.typography.bodyLarge)
                TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("project-history-retry"),
                    enabled = !state.loadingMore) { Text("重新读取", style = MaterialTheme.typography.bodyLarge) }
            }
        }
        if (state.error == null && (state.loading || state.loadingMore) && state.items.isNotEmpty()) item {
            Text("正在读取；数量和下方记录仍为上次读取结果。",
                Modifier.padding(LithicSpacing.medium).testTag("project-history-refreshing-snapshot"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!state.loading && !state.loadingMore && state.error == null && state.totalCount == 0L && state.items.isEmpty()) {
            item { Text("暂无已结束项目", Modifier.padding(LithicSpacing.medium).testTag("project-history-empty"), style = MaterialTheme.typography.bodyLarge) }
        }
        items(state.items, key = { it.project.id }) { detail ->
            val dates = "持有 ${detail.cost.heldDays}天 · ${detail.project.startedOn}—${detail.project.endedOn}"
            EntryCard(detail.project.name, "已结束", dates, detail.cost.dailyRate,
                Modifier.padding(horizontal = LithicSpacing.medium).testTag("project-history-row-${detail.project.id}"),
                onClick = { onOpenProject(detail.project.id) }, category = detail.category.name,
                cover = { RecordCover(CoverReference(CoverOwnerType.PROJECT, detail.project.id, detail.project.coverAssetId),
                        Modifier.size(64.dp)) { C2RecordIdentity(detail.project.name, detail.category.name, C2Glyph.Project) } },
                    hasCoverReference = detail.project.coverAssetId != null, metricLabel = "结果截至 ${detail.asOf}")
        }
        if (!state.loading && state.error == null && state.totalCount != null && state.items.isNotEmpty()) item {
            Text(when {
                state.loadingMore -> "正在读取下一页；已显示 ${state.items.size} / ${state.totalCount} 条"
                state.hasMore -> "已显示 ${state.items.size} / ${state.totalCount} 条，可继续加载"
                else -> "已显示 ${state.items.size} 条，已到列表末尾"
            }, Modifier.padding(horizontal = LithicSpacing.medium).testTag("project-history-page-status"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.hasMore) item {
            OutlinedButton(onLoadMore, Modifier.fillMaxWidth().padding(LithicSpacing.medium)
                .heightIn(min = 48.dp).testTag("project-history-load-more"), enabled = !state.loading && !state.loadingMore) {
                Text(if (state.loadingMore) "正在读取下一页" else "继续加载", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    }
}
