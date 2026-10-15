package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.LithicIcon
import com.cooper.lithic.core.design.EmptyState
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.feature.media.CoverThumbnail

@Composable
internal fun ProjectHistoryScreen(
    state: ProjectListUiState,
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    mediaRepository: MediaRepository? = null,
) {
    Column(modifier.fillMaxSize().testTag("project-history-screen")) {
    LazyColumn(Modifier.fillMaxSize().testTag("project-history-content"),
        contentPadding = PaddingValues(horizontal = LithicSpacing.medium, vertical = LithicSpacing.xSmall),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        item {
            Column(Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onBack, Modifier.size(48.dp).testTag("project-history-back")
                        .semantics { contentDescription = "返回回看" }) { LithicIcon(LithicIcon.Back, null) }
                    Text("历史档案", style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f).semantics { heading() })
                }
                state.totalCount?.let { Text("已结束 $it 条长期项目", style = MaterialTheme.typography.bodyMedium) }
                state.error?.let {
                    Text(it, Modifier.testTag("project-history-error"), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error)
                    if (state.items.isNotEmpty()) Text("以下为上次读取的历史档案。", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("project-history-retry")) {
                        Text("重新读取", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        if (state.loading) item { Text("正在读取历史档案", Modifier.padding(LithicSpacing.medium)) }
        if (!state.loading && state.error == null && state.items.isEmpty()) {
            item { EmptyState("暂无已结束项目", "项目结束后，可在这里回看陪伴时间和真实净投入。") }
        }
        items(state.items, key = { it.project.id }) { detail ->
            val dates = "开始：${detail.project.startedOn}" +
                (detail.project.endedOn?.let { "\n结束：$it" } ?: "") +
                "\n已持有${detail.cost.heldDays}天\n结果截至 ${detail.asOf}"
            val cover: (@Composable () -> Unit)? = detail.project.coverAssetId?.let { assetId ->
                { CoverThumbnail(assetId, mediaRepository, "project-history-${detail.project.id}") }
            }
            EntryCard(detail.project.name, "已结束", dates, detail.cost.dailyRate,
                Modifier.testTag("project-history-row-${detail.project.id}"), cover = cover,
                onClick = { onOpenProject(detail.project.id) })
        }
        if (state.hasMore) item {
            Button(onLoadMore, Modifier.fillMaxWidth()
                .heightIn(min = 48.dp).testTag("project-history-load-more"), enabled = !state.loading && !state.loadingMore) {
                Text(if (state.loadingMore) "正在读取下一页" else "继续加载", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    }
}
