package com.cooper.lithic.feature.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.C2PageHeader
import com.cooper.lithic.core.design.C5InformationButton
import com.cooper.lithic.core.design.LithicButton
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.LithicTextButton
import com.cooper.lithic.core.design.MoneyMetric
import com.cooper.lithic.core.design.MoneyMetricLevel
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.ReviewRankItem
import com.cooper.lithic.data.repository.ReviewRanking
import com.cooper.lithic.domain.report.OverviewKind

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
    val subscriptions = state.ranking == ReviewRanking.SUBSCRIPTION_TIME
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
            val trailingDuration = maxWidth >= 360.dp && LocalDensity.current.fontScale <= 1.3f
            LazyColumn(
                Modifier.fillMaxSize().testTag("review-ranks-screen"),
                contentPadding = PaddingValues(start = LithicSpacing.page, end = LithicSpacing.page, bottom = 20.dp),
            ) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        C2PageHeader("长期复盘", onBack, "review-ranks-back", backLabel = "返回回看") {
                            C5InformationButton(
                                "长期复盘口径",
                                Modifier.testTag("review-ranks-information"),
                                iconOnly = true,
                            ) {
                                Text("当前与历史一起排序，项目与订阅分列。日均成本按未舍入值排名。")
                                Text(
                                    "月30天、季90天、年365天；折算付费时长不等于实际陪伴时间。",
                                    Modifier.testTag("review-standard-days-explanation"),
                                )
                                Text("未开始或无有效分母的记录不排名，保留零和负值。结束后持有天数固定，后续结算仍影响结果。")
                            }
                        }
                        ReportKindSegment(
                            subscriptions,
                            "review-type-project",
                            "review-rank-SUBSCRIPTION_TIME",
                            subscriptionLabel = "订阅服务",
                            onProjects = { if (subscriptions) onRankingChange(ReviewRanking.PROJECT_TIME) },
                            onSubscriptions = { onRankingChange(ReviewRanking.SUBSCRIPTION_TIME) },
                        )
                        RankSort(state.ranking, onRankingChange)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        RankSnapshot(state, onRetry)
                    }
                }
                itemsIndexed(state.items, key = { _, item -> "${item.type.name}:${item.id}" }) { index, item ->
                    // The repository supplies the globally sorted window with every eligible item first.
                    val rank = if (index.toLong() < (state.rankableCount ?: 0L)) index + 1 else null
                    ReviewRankRow(
                        item = item,
                        rank = rank,
                        ranking = state.ranking,
                        trailingDuration = trailingDuration,
                        first = index == 0,
                        last = index == state.items.lastIndex,
                        onClick = {
                            if (item.type == CategoryType.PROJECT) onOpenProject(item.id) else onOpenSubscription(item.id)
                        },
                    )
                }
                if (state.hasMore) item {
                    LithicButton(
                        onLoadMore,
                        Modifier.fillMaxWidth().padding(top = 16.dp).heightIn(min = 48.dp)
                            .testTag("review-ranks-load-more"),
                        enabled = !state.loading,
                    ) { Text("继续加载") }
                }
            }
        }
    }
}

@Composable
internal fun ReportKindSegment(selectedKind: OverviewKind, tagPrefix: String, onSelect: (OverviewKind) -> Unit) {
    ReportKindSegment(
        selectedKind == OverviewKind.SUBSCRIPTION,
        "$tagPrefix-project",
        "$tagPrefix-subscription",
        subscriptionLabel = "订阅",
        onProjects = { onSelect(OverviewKind.PROJECT) },
        onSubscriptions = { onSelect(OverviewKind.SUBSCRIPTION) },
    )
}

@Composable
private fun ReportKindSegment(
    subscriptions: Boolean,
    projectTag: String,
    subscriptionTag: String,
    subscriptionLabel: String,
    onProjects: () -> Unit,
    onSubscriptions: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val selectedSurface = if (colors.surface.luminance() < 0.5f) {
        colors.onSurface.copy(alpha = 0.09f).compositeOver(colors.surfaceContainer)
    } else colors.surface

    Box {
        Box(Modifier.matchParentSize().padding(vertical = 3.dp).background(colors.surfaceContainer, CircleShape))
        Row(Modifier.padding(horizontal = 3.dp).selectableGroup()) {
            ReportKindTab("长期项目", !subscriptions, selectedSurface, projectTag, onProjects)
            ReportKindTab(subscriptionLabel, subscriptions, selectedSurface, subscriptionTag, onSubscriptions)
        }
    }
}

@Composable
private fun ReportKindTab(label: String, selected: Boolean, selectedSurface: Color, tag: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interactions = remember { MutableInteractionSource() }
    Box(
        Modifier.widthIn(min = 68.dp).heightIn(min = 48.dp).clip(CircleShape)
            .selectable(selected, interactions, indication = null, role = Role.Tab, onClick = onClick)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        // The outer tab owns touch and selection. Only the inset capsule draws the press.
        Box(
            Modifier.matchParentSize().padding(vertical = 6.dp).clip(CircleShape)
                .background(if (selected) selectedSurface else Color.Transparent)
                .indication(interactions, ripple(bounded = false)),
        )
        Text(
            label,
            Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RankSort(ranking: ReviewRanking, onRankingChange: (ReviewRanking) -> Unit) {
    if (ranking == ReviewRanking.SUBSCRIPTION_TIME) {
        Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                "折算付费时长",
                Modifier.testTag("review-subscription-time-heading").semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    } else {
        FlowRow(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RankSortOption("陪伴时间最长", ReviewRanking.PROJECT_TIME, ranking, onRankingChange)
            RankSortOption("日均成本最低", ReviewRanking.PROJECT_COST, ranking, onRankingChange)
        }
    }
}

@Composable
private fun RankSortOption(
    label: String,
    option: ReviewRanking,
    ranking: ReviewRanking,
    onRankingChange: (ReviewRanking) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val selected = option == ranking
    Box(
        Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
            .selectable(selected, role = Role.RadioButton, onClick = { onRankingChange(option) })
            .testTag("review-rank-${option.name}"),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RankSnapshot(state: ReviewUiState, onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val hasSnapshot = state.asOf != null || state.totalCount != null || state.items.isNotEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.totalCount?.let { total ->
                Text(
                    "共 $total 条记录 · ${state.rankableCount ?: 0} 条可排名",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            state.asOf?.let {
                Text("观察日：$it", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        state.error?.let { error ->
            Text(error, Modifier.testTag("review-ranks-error"), color = colors.error)
            if (hasSnapshot) {
                Text(
                    "保留上次结果，最新状态未确认。",
                    Modifier.testTag("review-ranks-stale"),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            LithicTextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("review-ranks-retry")) {
                Text("重新读取")
            }
        }
        when {
            state.loading -> Text(if (hasSnapshot) "正在更新复盘" else "正在读取复盘")
            state.error != null -> Unit
            state.totalCount == 0L -> Text(
                if (state.ranking == ReviewRanking.SUBSCRIPTION_TIME) "暂无订阅记录" else "暂无长期项目记录",
                Modifier.testTag("review-ranks-empty"),
                style = MaterialTheme.typography.bodyLarge,
            )
            state.totalCount != null && state.rankableCount == 0L -> Text(
                "暂无可排名记录，以下记录尚无有效指标。",
                Modifier.testTag("review-ranks-unavailable"),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewRankRow(
    item: ReviewRankItem,
    rank: Int?,
    ranking: ReviewRanking,
    trailingDuration: Boolean,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val project = item.project
    val subscription = item.subscription
    val unavailable = project?.cost?.unavailableReason != null
    val costIsPrimary = ranking == ReviewRanking.PROJECT_COST
    val showDurationAtEnd = trailingDuration && project != null && rank != null && !costIsPrimary
    val status = when {
        unavailable -> "尚未开始"
        project?.isHistory == true -> "已结束"
        project != null -> "持有中"
        subscription != null -> reviewSubscriptionStatus(subscription.cost.displayStatus)
        else -> null
    }
    val duration = when {
        unavailable -> "尚无已过去的持有日期，不参与排名"
        project != null -> "持有 ${project.cost.heldDays} 天"
        subscription != null -> "折算付费时长：${subscription.cost.standardPurchasedDays} 天"
        else -> null
    }
    val shape = RoundedCornerShape(
        topStart = if (first) 14.dp else 0.dp,
        topEnd = if (first) 14.dp else 0.dp,
        bottomStart = if (last) 14.dp else 0.dp,
        bottomEnd = if (last) 14.dp else 0.dp,
    )
    Column(
        Modifier.fillMaxWidth().clip(shape).background(colors.surface)
            .rankRowOutline(colors.outlineVariant, first, last),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = 50.dp).testTag("review-row-${item.id}")
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            if (rank == null) "未排名 · ${item.name}" else "$rank. ${item.name}",
                            Modifier.testTag("review-name-${item.id}"),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            RankNote(item.categoryName)
                            status?.let { RankNote(it, Modifier.testTag("review-status-${item.id}")) }
                        }
                    }
                    if (showDurationAtEnd && duration != null) {
                        Text(
                            duration,
                            Modifier.widthIn(max = 112.dp).testTag("review-time-${item.id}"),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                    }
                }
                if (!showDurationAtEnd && !costIsPrimary && duration != null) {
                    RankDuration(duration, item.id, emphasized = rank != null)
                }
                if (project != null) {
                    val costLabel = when {
                        unavailable -> "暂无可计算日均成本"
                        project.isHistory -> "已结束；结果截至今天"
                        else -> "当前日均成本"
                    }
                    RankNote(costLabel, Modifier.testTag("review-cost-label-${item.id}"))
                    MoneyMetric(
                        project.cost.dailyRate,
                        Modifier.fillMaxWidth().testTag("review-rate-${item.id}"),
                        level = if (costIsPrimary) MoneyMetricLevel.CARD else MoneyMetricLevel.ROW,
                        color = if (costIsPrimary) colors.onSurface else colors.onSurfaceVariant,
                        label = costLabel,
                    )
                }
                if (subscription != null) {
                    val costLabel = "历史平均日成本"
                    RankNote(costLabel, Modifier.testTag("review-cost-label-${item.id}"))
                    MoneyMetric(
                        subscription.cost.historicalAverage,
                        Modifier.fillMaxWidth().testTag("review-rate-${item.id}"),
                        color = colors.onSurfaceVariant,
                        label = costLabel,
                    )
                    if (rank == null) RankNote("尚无已确认付款，暂无有效分母。")
                }
                if (costIsPrimary && duration != null) RankDuration(duration, item.id, emphasized = false)
            }
            C2Icon(C2Glyph.Chevron, Modifier.size(16.dp))
        }
        if (!last) HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = colors.outlineVariant)
    }
}

@Composable
private fun RankDuration(duration: String, id: String, emphasized: Boolean) {
    Text(
        duration,
        Modifier.testTag("review-time-$id"),
        style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        color = if (emphasized) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RankNote(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun Modifier.rankRowOutline(color: Color, first: Boolean, last: Boolean): Modifier = drawWithContent {
    drawContent()
    val stroke = 1.dp.toPx()
    val radius = 14.dp.toPx()
    // Extend open ends beyond each lazy item so the list has one outline and inset separators.
    val top = if (first) stroke / 2 else -radius - stroke
    val bottom = if (last) size.height - stroke / 2 else size.height + radius + stroke
    clipRect {
        drawRoundRect(
            color,
            topLeft = Offset(stroke / 2, top),
            size = Size(size.width - stroke, bottom - top),
            cornerRadius = CornerRadius(radius - stroke / 2),
            style = Stroke(stroke),
        )
    }
}
