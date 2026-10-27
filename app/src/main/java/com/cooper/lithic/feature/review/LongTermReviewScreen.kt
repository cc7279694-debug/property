package com.cooper.lithic.feature.review

import com.cooper.lithic.core.design.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.ReviewRankItem
import com.cooper.lithic.data.repository.ReviewRanking

@Composable
internal fun LongTermReviewScreen(
    state: ReviewUiState,
    onBack: () -> Unit,
    onRankingChange: (ReviewRanking) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenSubscription: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().testTag("review-ranks-screen"),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                C2PageHeader("长期复盘", onBack, "review-ranks-back", backLabel = "返回回看")
                C5InformationButton("长期复盘口径", Modifier.testTag("review-ranks-information")) {
                    Text("当前与历史一起回看，两类记录分别排序。陪伴榜按持有天数；低成本榜按未舍入的日均；订阅时间榜按折算付费时长。")
                    Text("月30天、季90天、年365天；折算付费时长不等于实际陪伴时间。",
                        Modifier.testTag("review-standard-days-explanation"))
                    Text("尚未开始或没有有效分母的记录不参与排名；真实0和负值保留。已结束项目的持有天数冻结，后续真实结算仍影响截至观察日的结果。")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                    FilterChip(selected = state.ranking != ReviewRanking.SUBSCRIPTION_TIME,
                        onClick = { if (state.ranking == ReviewRanking.SUBSCRIPTION_TIME) onRankingChange(ReviewRanking.PROJECT_TIME) },
                        label = { Text("长期项目") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("review-type-project"))
                    FilterChip(selected = state.ranking == ReviewRanking.SUBSCRIPTION_TIME,
                        onClick = { onRankingChange(ReviewRanking.SUBSCRIPTION_TIME) }, label = { Text("订阅服务") },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("review-rank-SUBSCRIPTION_TIME"))
                }
                if (state.ranking != ReviewRanking.SUBSCRIPTION_TIME) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                        FilterChip(selected = state.ranking == ReviewRanking.PROJECT_TIME,
                            onClick = { onRankingChange(ReviewRanking.PROJECT_TIME) }, label = { Text("陪伴时间最长") },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("review-rank-PROJECT_TIME"))
                        FilterChip(selected = state.ranking == ReviewRanking.PROJECT_COST,
                            onClick = { onRankingChange(ReviewRanking.PROJECT_COST) }, label = { Text("日均成本最低") },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("review-rank-PROJECT_COST"))
                    }
                } else {
                    Text("折算付费时长", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("review-subscription-time-heading").semantics { heading() })
                }
                state.asOf?.let { Text("观察日：$it", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                state.totalCount?.let { total ->
                    Text("共 $total 条记录 · ${state.rankableCount ?: 0} 条可排名", style = MaterialTheme.typography.bodyMedium)
                }
                state.error?.let { error ->
                    Text(error, Modifier.testTag("review-ranks-error"), color = MaterialTheme.colorScheme.error)
                    if (state.items.isNotEmpty()) Text("保留上次读取的结果，尚未确认最新状态。",
                        Modifier.testTag("review-ranks-stale"), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("review-ranks-retry")) { Text("重新读取") }
                }
                if (state.loading) Text(if (state.items.isEmpty()) "正在读取复盘" else "正在更新复盘")
                if (!state.loading && state.error == null && state.totalCount == 0L) {
                    Text(if (state.ranking == ReviewRanking.SUBSCRIPTION_TIME) "暂无订阅记录" else "暂无长期项目记录",
                        Modifier.testTag("review-ranks-empty"), style = MaterialTheme.typography.bodyLarge)
                } else if (!state.loading && state.error == null && state.totalCount != null && state.rankableCount == 0L) {
                    Text("暂无可排名记录，以下记录尚无有效指标。", Modifier.testTag("review-ranks-unavailable"),
                        style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        itemsIndexed(state.items, key = { _, item -> "${item.type.name}:${item.id}" }) { index, item ->
            val rank = if (index.toLong() < (state.rankableCount ?: 0L)) index + 1 else null
            ReviewRankCard(item, rank, state.ranking, onClick = {
                if (item.type == CategoryType.PROJECT) onOpenProject(item.id) else onOpenSubscription(item.id)
            })
        }
        if (state.hasMore) item {
            Button(onLoadMore, Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium)
                .heightIn(min = 48.dp).testTag("review-ranks-load-more"), enabled = !state.loading) { Text("继续加载") }
        }
        item { Spacer(Modifier.height(LithicSpacing.medium)) }
    }
}

@Composable
private fun ReviewRankCard(item: ReviewRankItem, rank: Int?, ranking: ReviewRanking, onClick: () -> Unit) {
    Card(onClick, Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium).testTag("review-row-${item.id}")) {
        Column(Modifier.padding(LithicSpacing.medium), verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                C2RecordIdentity(item.name, item.categoryName, if (item.type == CategoryType.PROJECT) C2Glyph.Project else C2Glyph.Subscription)
                Text(if (rank == null) "未排名 · ${item.name}" else "$rank. ${item.name}",
                    Modifier.weight(1f).testTag("review-name-${item.id}"), style = MaterialTheme.typography.titleMedium)
            }
            Text(item.categoryName, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            item.project?.let { project ->
                val unavailable = project.cost.unavailableReason != null
                C2StatusChip(when { unavailable -> "尚未开始"; project.isHistory -> "已结束"; else -> "持有中" },
                    Modifier.testTag("review-status-${item.id}"))
                val duration: @Composable () -> Unit = { Text(if (unavailable) "尚无已过去的持有日期，不参与排名" else "持有 ${project.cost.heldDays} 天",
                    Modifier.testTag("review-time-${item.id}"),
                    style = if (ranking == ReviewRanking.PROJECT_TIME) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium) }
                val daily: @Composable () -> Unit = {
                    Text(when { unavailable -> "暂无可计算日均成本"; project.isHistory -> "已结束；结果截至今天"; else -> "当前日均成本" },
                        Modifier.testTag("review-cost-label-${item.id}"), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    MoneyMetric(project.cost.dailyRate, Modifier.fillMaxWidth().testTag("review-rate-${item.id}"),
                        level = MoneyMetricLevel.ROW,
                        color = if (ranking == ReviewRanking.PROJECT_COST) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = when { unavailable -> "暂无可计算日均成本"; project.isHistory -> "已结束；结果截至今天"; else -> "当前日均成本" })
                }
                if (ranking == ReviewRanking.PROJECT_COST) { daily(); duration() } else { duration(); daily() }
            }
            item.subscription?.let { subscription ->
                C2StatusChip(reviewSubscriptionStatus(subscription.cost.displayStatus), Modifier.testTag("review-status-${item.id}"))
                Text("折算付费时长：${subscription.cost.standardPurchasedDays} 天", Modifier.testTag("review-time-${item.id}"),
                    style = MaterialTheme.typography.titleLarge)
                Text("历史平均日成本", Modifier.testTag("review-cost-label-${item.id}"),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyMetric(subscription.cost.historicalAverage, Modifier.fillMaxWidth().testTag("review-rate-${item.id}"),
                    level = MoneyMetricLevel.ROW, color = MaterialTheme.colorScheme.onSurfaceVariant, label = "历史平均日成本")
                if (subscription.cost.historicalAverage == null) Text("尚无已确认付款，暂无有效分母。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
