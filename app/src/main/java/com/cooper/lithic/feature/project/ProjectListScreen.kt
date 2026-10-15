package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.EmptyState
import com.cooper.lithic.core.design.LithicIcon
import com.cooper.lithic.core.design.PageHeader
import com.cooper.lithic.core.design.EntryCard
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.feature.media.CoverThumbnail
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
    mediaRepository: MediaRepository? = null,
) {
    Column(modifier.fillMaxSize().testTag("project-list-screen")) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("project-list-content"),
        contentPadding = PaddingValues(horizontal = LithicSpacing.medium, vertical = LithicSpacing.xSmall),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                TextButton(onBack, Modifier.heightIn(min = 48.dp).testTag("project-list-back")) {
                    LithicIcon(LithicIcon.Back, null)
                    Text("返回现在", style = MaterialTheme.typography.bodyLarge)
                }
                PageHeader("长期项目", subtitle = "记录拥有，让时间说明价值。")
                Text("尚未结束的项目；已结束记录可在回看中的历史档案查看。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onCreate, Modifier.heightIn(min = 48.dp).testTag("project-list-create")) {
                    LithicIcon(LithicIcon.Add, null, tint = MaterialTheme.colorScheme.onPrimary)
                    Text("新增长期项目", style = MaterialTheme.typography.bodyLarge)
                }
                state.totalCount?.let { Text("当前列表 $it 条长期项目", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                state.error?.let {
                    Text(it, Modifier.testTag("project-list-error"), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("project-list-retry")) {
                        Text("重新读取", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        if (state.loading) item { Text("正在读取长期项目", style = MaterialTheme.typography.bodyLarge) }
        if (!state.loading && state.error == null && state.totalCount == 0L && state.items.isEmpty()) {
            item { EmptyState("暂无长期项目", "想看看什么能陪伴你更久？从一件熟悉的物品开始。") }
        }
        items(state.items, key = { it.project.id }) { detail ->
            val dates = "开始：${detail.project.startedOn}" +
                (detail.project.endedOn?.let { "\n结束：$it" } ?: "") +
                if (detail.cost.unavailableReason == null) "\n已持有${detail.cost.heldDays}天" else ""
            val cover: (@Composable () -> Unit)? = if (detail.project.coverAssetId == null) null else {
                { CoverThumbnail(detail.project.coverAssetId, mediaRepository, "project-${detail.project.id}") }
            }
            EntryCard(detail.project.name, projectStatusLabel(detail), dates, detail.cost.dailyRate,
                Modifier.testTag("project-row-${detail.project.id}"), cover = cover,
                onClick = { onOpenProject(detail.project.id) })
        }
        if (state.hasMore) item {
            Button(onLoadMore, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-list-load-more"),
                enabled = !state.loadingMore && !state.loading) {
                Text(if (state.loadingMore) "正在读取下一页" else "继续加载", style = MaterialTheme.typography.bodyLarge)
            }
        }
        item { Spacer(Modifier.height(LithicSpacing.medium)) }
    }
    }
}

internal fun projectStatusLabel(detail: ProjectDetail): String = when {
    detail.cost.unavailableReason != null -> "尚未开始"
    detail.project.endedOn != null && !detail.cost.contributesToCurrentTotal -> "已结束"
    detail.project.endedOn != null -> "已记录结束日期"
    else -> "持有中"
}
