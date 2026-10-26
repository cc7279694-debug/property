package com.cooper.lithic.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2PageHeader
import com.cooper.lithic.core.design.C2RecordIdentity
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
                C2PageHeader("搜索名称", onBack, "search-back", backLabel = "返回现在")
                NameField(state.query, onQueryChange, Modifier.testTag("search-query"), label = "项目或订阅名称",
                    imeAction = ImeAction.Search, onImeAction = onRetry, labelOutside = true)
                Text("同时查找当前与历史记录。仅匹配名称。", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.loading) Text("正在查找", Modifier.testTag("search-loading"))
                if (state.query.isBlank()) Text("输入名称开始搜索。", Modifier.testTag("search-empty-query"))
                else if (!state.loading && state.error == null && state.hasReadSnapshot) {
                    Text("找到 ${state.totalCount} 条记录", Modifier.testTag("search-result-count"))
                    if (state.isConfirmedEmptyResult) Text("没有匹配这个名称的项目或订阅，可换个名称再试。",
                        Modifier.testTag("search-no-results"), style = MaterialTheme.typography.bodyLarge)
                } else if (!state.loading && state.error == null) {
                    Text("尚未取得搜索结果，请重新查找。", Modifier.testTag("search-awaiting-read"))
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("search-retry")) { Text("查找") }
                }
                if (state.hasReadSnapshot && (state.loading || state.error != null)) Text(
                    if (state.loading) "正在更新；以下为同一搜索条件上次取得的结果，尚未确认是最新结果。"
                    else "以下为同一搜索条件上次取得的结果，状态可能已变化。",
                    Modifier.testTag("search-stale"), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.asOf?.let { date -> Text("这批结果的业务日期：$date。状态以该日判断。",
                    Modifier.testTag("search-as-of"), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                state.error?.let {
                    Text(it, Modifier.testTag("search-error"), color = MaterialTheme.colorScheme.error)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("search-retry"), enabled = !state.loading) { Text("重新查找") }
                }
            }
        }
        items(state.items, key = { "${it.type.name}:${it.id}" }) { item ->
            Card(onClick = { onOpen(item) }, modifier = Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium)
                .testTag("search-row-${item.id}"), shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    C2RecordIdentity(item.name, item.categoryName,
                        if (item.type == CategoryType.PROJECT) C2Glyph.Project else C2Glyph.Subscription)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.name, style = MaterialTheme.typography.bodyLarge)
                        Text((if (item.type == CategoryType.PROJECT) "长期项目" else "订阅服务") + " · ${item.categoryName}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(searchStatusLabel(item.status) + if (item.isHistory) " · 历史记录" else " · 当前管理",
                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp).testTag("search-status-${item.id}"),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        if (state.hasMore) item {
            OutlinedButton(onLoadMore, Modifier.fillMaxWidth().padding(LithicSpacing.medium).heightIn(min = 48.dp)
                .testTag("search-load-more"), enabled = !state.loading) { Text(if (state.loading) "正在读取" else "继续加载") }
        }
        else if (state.items.isNotEmpty() && !state.loading && state.error == null) item {
            Text("已到本次结果末尾", Modifier.padding(LithicSpacing.medium).testTag("search-end"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
