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
    Column(modifier.fillMaxSize().testTag("project-list-screen"),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
            TextButton(onBack, Modifier.heightIn(min = 48.dp).testTag("project-list-back")) {
                Text("返回现在", style = MaterialTheme.typography.bodyLarge)
            }
            Text("长期项目", style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() })
            Button(onCreate, Modifier.heightIn(min = 48.dp).testTag("project-list-create")) {
                Text("新增长期项目", style = MaterialTheme.typography.bodyLarge)
            }
            state.totalCount?.let { Text("已录入 $it 条长期项目", style = MaterialTheme.typography.bodyMedium) }
            state.error?.let {
                Text(it, Modifier.testTag("project-list-error"), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error)
                TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("project-list-retry")) {
                    Text("重新读取", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("project-list-content"),
            verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
            if (state.loading) item { Text("正在读取长期项目", Modifier.padding(LithicSpacing.medium)) }
            if (!state.loading && state.error == null && state.totalCount == 0L && state.items.isEmpty()) {
                item { Text("暂无长期项目", Modifier.padding(LithicSpacing.medium), style = MaterialTheme.typography.bodyLarge) }
            }
            items(state.items, key = { it.project.id }) { detail ->
                val dates = "开始：${detail.project.startedOn}" +
                    (detail.project.endedOn?.let { "\n结束：$it" } ?: "") +
                    if (detail.cost.unavailableReason == null) "\n已持有${detail.cost.heldDays}天" else ""
                EntryCard(detail.project.name, projectStatusLabel(detail), dates, detail.cost.dailyRate,
                    Modifier.padding(horizontal = LithicSpacing.medium).testTag("project-row-${detail.project.id}"),
                    onClick = { onOpenProject(detail.project.id) })
            }
            if (state.hasMore) item {
                Button(onLoadMore, Modifier.fillMaxWidth().padding(LithicSpacing.medium)
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
