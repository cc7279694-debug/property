package com.cooper.lithic.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.NameField
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.SearchItem

@Composable
internal fun SearchScreen(
    state: SearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpen: (SearchItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().imePadding().testTag("global-search-screen"),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                TextButton(onBack, Modifier.heightIn(min = 48.dp).testTag("search-back")) { Text("返回现在") }
                Text("搜索名称", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineLarge)
                NameField(state.query, onQueryChange, Modifier.testTag("search-query"), label = "项目或订阅名称",
                    imeAction = ImeAction.Search, onImeAction = onRetry)
                Text("同时查找当前与历史记录。仅匹配名称。", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.loading) Text("正在查找")
                if (state.query.isBlank()) Text("输入名称开始搜索。", Modifier.testTag("search-empty-query"))
                else if (!state.loading && state.error == null) {
                    Text("找到 ${state.totalCount} 条记录", Modifier.testTag("search-result-count"))
                }
                state.error?.let {
                    Text(it, Modifier.testTag("search-error"), color = MaterialTheme.colorScheme.error)
                    if (state.items.isNotEmpty()) Text("以下为上次读取结果，状态可能已变化。", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("search-retry")) { Text("重新查找") }
                }
            }
        }
        items(state.items, key = { "${it.type.name}:${it.id}" }) { item ->
            Card(onClick = { onOpen(item) }, Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium)
                .testTag("search-row-${item.id}")) {
                Column(Modifier.fillMaxWidth().padding(LithicSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                    Text(item.name, style = MaterialTheme.typography.titleLarge)
                    Text((if (item.type == CategoryType.PROJECT) "长期项目" else "订阅服务") + " · ${item.categoryName}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text(searchStatusLabel(item.status) + if (item.isHistory) " · 历史记录" else " · 当前管理",
                        Modifier.testTag("search-status-${item.id}"), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (state.hasMore) item {
            Button(onLoadMore, Modifier.fillMaxWidth().padding(LithicSpacing.medium).heightIn(min = 48.dp)
                .testTag("search-load-more"), enabled = !state.loading) { Text("继续加载") }
        }
    }
}
