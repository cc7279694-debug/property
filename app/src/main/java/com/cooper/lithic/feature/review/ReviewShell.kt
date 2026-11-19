package com.cooper.lithic.feature.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.C5InformationButton
import com.cooper.lithic.core.design.ChartGeometry
import com.cooper.lithic.core.design.CostChartPoint
import com.cooper.lithic.core.design.LithicButton
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.LithicTextButton
import com.cooper.lithic.core.design.LocalLithicVisuals
import com.cooper.lithic.core.design.MoneyMetric
import com.cooper.lithic.core.design.MoneyMetricLevel
import com.cooper.lithic.core.design.moneyMetricDescription
import com.cooper.lithic.core.design.moneyMetricDisplay
import com.cooper.lithic.domain.report.CostTimelinePoint
import com.cooper.lithic.domain.report.OverviewKind
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** The protected navigation entry still supplies its callbacks. Exact date tools and event actions
 * live on the existing full trend route. The root reads the same state without changing selection. */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun ReviewShell(
    projectCount: Long,
    modifier: Modifier = Modifier,
    onProjectHistory: (() -> Unit)? = null,
    onSubscriptionHistory: (() -> Unit)? = null,
    onLongTermReview: (() -> Unit)? = null,
    onTotalCostTrend: (() -> Unit)? = null,
    onCategoryAnalysis: (() -> Unit)? = null,
    onAnnualReview: (() -> Unit)? = null,
    subscriptionCount: Long = 0L,
    hasReadNotice: Boolean = false,
    trendState: TotalCostTrendUiState? = null,
    onSelectTrendDate: (LocalDate) -> Unit = {},
    onToggleTrendBreakdown: () -> Unit = {},
    onRetryTrend: () -> Unit = {},
    onOpenTrendRecord: (OverviewKind, String) -> Unit = { _, _ -> },
    onAddRecord: (() -> Unit)? = null,
) {
    val state = trendState ?: TotalCostTrendUiState()
    val presentation = totalTrendPresentation(state, TrendRecordCounts(projectCount, subscriptionCount))
    val trendHasRecords = state.snapshot?.overview?.let {
        it.projects.isNotEmpty() || it.subscriptions.isNotEmpty()
    } == true || state.timeline?.from != null
    val showEmpty = reviewShowsEmptyLedger(
        projectCount, subscriptionCount, hasReadNotice,
        trendLoading = state.loading,
        trendHasNotice = state.error != null || state.notice != null,
        trendHasRecords = trendHasRecords,
    )
    val countsNeedNotice = hasReadNotice || (projectCount == 0L && subscriptionCount == 0L && trendHasRecords)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 560.dp).fillMaxSize().testTag("review-screen"),
            contentPadding = PaddingValues(horizontal = LithicSpacing.page, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item("heading") { ReviewHeading(projectCount, subscriptionCount, countsNeedNotice, showCounts = !showEmpty) }
            if (showEmpty) {
                item("empty") { ReviewEmptyState(onAddRecord) }
            } else {
                item("summary") { ReviewTrendSummary(state, presentation, hasReadNotice, onRetryTrend) }
                item("review-destinations") {
                    ReviewDestinations(
                        onProjectHistory, onSubscriptionHistory, onLongTermReview,
                        onTotalCostTrend, onCategoryAnalysis, onAnnualReview,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewEmptyState(onAddRecord: (() -> Unit)?) {
    Column(
        Modifier.fillMaxWidth().padding(top = 20.dp).testTag("review-empty-state"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.testTag("review-empty-identity"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ReviewSectionTitle("暂无记录")
            Text(
                "记下一件物品后，在这里看见成本变化", Modifier.testTag("review-record-summary"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        onAddRecord?.let { add ->
            LithicButton(add, Modifier.fillMaxWidth().testTag("review-add-record"), shape = RoundedCornerShape(50)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("去首页记录")
                    C2Icon(C2Glyph.Chevron, Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewHeading(projectCount: Long, subscriptionCount: Long, hasReadNotice: Boolean, showCounts: Boolean) {
    val colors = MaterialTheme.colorScheme
    val auxiliary = MaterialTheme.typography.bodySmall
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "回看", Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            C5InformationButton("回看的口径", Modifier.testTag("review-information")) {
                Column(Modifier.testTag("review-information-content"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReviewCounts(projectCount, subscriptionCount)
                    Text("没有录入不等于没有花钱。趋势按已录入及更正的记录重建，非当天扣款。年度回顾按资金实际发生年归属。历史档案仅含已结束项目和已停止订阅。")
                }
            }
        }
        if (showCounts) FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("$projectCount 条长期项目", Modifier.testTag("review-summary-project-count"),
                style = auxiliary, color = colors.onSurfaceVariant)
            Text("$subscriptionCount 条订阅服务", Modifier.testTag("review-summary-subscription-count"),
                style = auxiliary, color = colors.onSurfaceVariant)
            Text("含历史", Modifier.testTag("review-summary-scope"), style = auxiliary, color = colors.onSurfaceVariant)
        }
        if (reviewRecordSummary(projectCount, subscriptionCount, hasReadNotice) == ReviewRecordSummary.SNAPSHOT_NOTICE) {
            Text("以下为已取得的记录快照", Modifier.testTag("review-record-summary"),
                style = auxiliary, color = colors.onSurfaceVariant)
            Text("当前有读取或恢复提示；这里只展示已取得的快照，最新记录需重新核对。",
                Modifier.testTag("review-record-notice").semantics { liveRegion = LiveRegionMode.Polite },
                style = auxiliary, color = colors.error)
        }
    }
}

@Composable
private fun ReviewCounts(projectCount: Long, subscriptionCount: Long) {
    Column(Modifier.testTag("review-counts-compact"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$projectCount 条长期项目", Modifier.testTag("review-project-count"))
        Text("$subscriptionCount 条订阅服务", Modifier.testTag("review-subscription-count"))
        Text("含当前和历史。读取异常时显示已取得的数量。", Modifier.testTag("review-count-scope"),
            style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewTrendSummary(
    state: TotalCostTrendUiState,
    presentation: TotalTrendPresentation,
    hasReadNotice: Boolean,
    onRetry: () -> Unit,
) {
    val timeline = state.timeline?.takeIf { presentation.hasCompleteSnapshot && presentation.hasRecords }
    val selected = presentation.selected
    val selectedDate = state.selectedDate
    val colors = MaterialTheme.colorScheme
    val cutoff = presentation.cutoff
    val singleDate = timeline?.let(::trendHasSingleDate) == true
    val dateLabel = if (selectedDate != null && selectedDate == cutoff) "观察日" else "所选日期"
    ReviewGroup("review-trend-plot-surface") {
        Column(
            Modifier.fillMaxWidth().padding(12.dp).testTag("total-trend-plot-frame"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("总日均成本", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (selectedDate != null) ReviewAuxiliaryText(dateLabel, "total-trend-selection-label")
                    if (selectedDate == null) ReviewAuxiliaryText("日期待读取", "total-trend-date-pending")
                    else ReviewAuxiliaryText(selectedDate.toString(), "total-trend-selected-date")
                }
            }
            MoneyMetric(
                selected?.totalRate, Modifier.fillMaxWidth()
                    .testTag(if (selected == null) "total-trend-total-pending" else "total-trend-total")
                    .semantics {
                        if (presentation.hasRecords && selected == null) {
                            stateDescription = "所选日期的精确结果尚未取得"
                        }
                    },
                level = MoneyMetricLevel.DETAIL, label = "总日均成本", keepPreferredSize = true,
            )
            if (state.loading && presentation.hasCompleteSnapshot) {
                ReviewAuxiliaryText("正在刷新，暂保留上次结果", "total-trend-refresh-status", announce = true)
            } else if (selectedDate != null && presentation.hasRecords && selected == null && state.error == null) {
                ReviewAuxiliaryText("正在计算所选日期", "total-trend-refresh-status", announce = true)
            }
            if (singleDate) {
                ReviewSingleDate(requireNotNull(timeline).asOf, selected)
            } else if (timeline != null && timeline.points.isNotEmpty()) {
                ReviewCostPreview(timeline.points, selected)
            } else if (state.error == null) {
                ReviewPendingTrend(presentation, hasReadNotice)
            }
            if (cutoff != null && (cutoff != selectedDate || state.error != null ||
                    state.loading || presentation.recordCountsMismatch || hasReadNotice)) {
                ReviewAuxiliaryText("截至 $cutoff", "total-trend-cutoff")
            }
            if (selected != null && (singleDate || selected.totalRate.signum() == 0)) {
                ReviewAuxiliaryText(reviewSelectionLabel(selected), "total-trend-selected-status")
            }
            if (presentation.recordCountsMismatch) {
                Text(
                    "趋势事实尚需核对：当前记录数量与趋势快照不一致。请刷新核对。",
                    Modifier.testTag("total-trend-record-count-notice").semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodySmall, color = colors.error,
                )
            }
            state.error?.let { error ->
                Column(
                    Modifier.testTag("total-trend-error").semantics { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        if (presentation.hasCompleteSnapshot) "读取失败；仍保留上次结果，尚未刷新。" else "历史未能读取，请重试。",
                        style = MaterialTheme.typography.bodyMedium, color = colors.error,
                    )
                    Text(error, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    LithicTextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("total-trend-retry")) { Text("重试") }
                }
            }
            state.notice?.let { ReviewAuxiliaryText(it, "total-trend-notice", announce = true) }
        }
    }
}

@Composable
private fun ReviewPendingTrend(presentation: TotalTrendPresentation, hasReadNotice: Boolean) {
    val message = when {
        presentation.recordCountsMismatch -> "记录已变化，趋势尚待核对"
        hasReadNotice -> "当前记录快照需要核对"
        presentation.phase == TrendReadPhase.WAITING -> "成本趋势尚未读取"
        presentation.hasCompleteSnapshot -> "截至本次业务日尚无可绘制的记录历史"
        else -> "正在读取完整历史"
    }
    val tag = if (presentation.phase == TrendReadPhase.WAITING) "review-trend-unavailable" else "total-trend-pending-message"
    Box(Modifier.fillMaxWidth().heightIn(min = 104.dp).testTag("total-trend-loading-placeholder"),
        contentAlignment = Alignment.CenterStart) {
        ReviewAuxiliaryText(message, tag, announce = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewSingleDate(observed: LocalDate, selected: CostTimelinePoint?) {
    Column(Modifier.fillMaxWidth().testTag("review-single-date"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ReviewAuxiliaryText(
            if (selected?.date == observed) "唯一已取得日期" else "唯一观察日期 · $observed",
            "review-single-date-caption",
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                Text("长期项目贡献", style = MaterialTheme.typography.bodySmall)
                MoneyMetric(selected?.projectRate, Modifier.testTag(if (selected == null) "total-trend-project-pending" else "total-trend-project"),
                    label = "长期项目贡献", keepPreferredSize = true)
            }
            Column {
                Text("订阅贡献", style = MaterialTheme.typography.bodySmall)
                MoneyMetric(selected?.subscriptionRate, Modifier.testTag(if (selected == null) "total-trend-subscription-pending" else "total-trend-subscription"),
                    label = "订阅贡献", keepPreferredSize = true)
            }
        }
    }
}

private val ReviewPreviewDateFormat = DateTimeFormatter.ofPattern("MM-dd")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewCostPreview(points: List<CostTimelinePoint>, selected: CostTimelinePoint?) {
    val series = remember(points) { points.sortedBy { it.date }.map { CostChartPoint(it.date, it.totalRate) } }
    val focus = selected?.takeIf { it.date in series.first().date..series.last().date }
    val geometry = remember(series, focus) {
        requireNotNull(ChartGeometry.forPoints(if (focus == null) series else series + CostChartPoint(focus.date, focus.totalRate)))
    }
    val samples = remember(series, geometry) { series.map { Offset(geometry.x(it.date), geometry.y(it.value)) } }
    val visuals = LocalLithicVisuals.current
    val top = moneyMetricDisplay(geometry.maximum).removeSuffix("/天")
    val bottom = moneyMetricDisplay(geometry.minimum).removeSuffix("/天")
    val showScaleText = LocalDensity.current.fontScale <= 1.3f && maxOf(top.length, bottom.length) <= 12
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth().height(84.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showScaleText) Column(
                Modifier.fillMaxHeight().clearAndSetSemantics {},
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(top, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(bottom, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Canvas(Modifier.weight(1f).fillMaxHeight().testTag("total-trend-canvas").semantics {
                contentDescription = "总日均成本 · 实线，${geometry.start} 至 ${geometry.end}。" +
                    "纵轴：${moneyMetricDescription(geometry.minimum)} 至 ${moneyMetricDescription(geometry.maximum)}。完整图表可在全部历史与事件中查看。"
            }) {
                val inset = 4.dp.toPx()
                val width = (size.width - inset * 2).coerceAtLeast(0f)
                val height = (size.height - inset * 2).coerceAtLeast(0f)
                fun position(offset: Offset) = Offset(inset + offset.x * width, inset + offset.y * height)
                for (line in listOf(0f, 0.5f, geometry.zero, 1f).distinct()) {
                    drawLine(visuals.chartGrid, position(Offset(0f, line)), position(Offset(1f, line)), 1.dp.toPx())
                }
                val path = Path()
                samples.forEachIndexed { index, sample ->
                    val point = position(sample)
                    if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(path, visuals.chartTotal, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                if (focus != null) {
                    drawCircle(visuals.chartTotal, 3.dp.toPx(), position(Offset(geometry.x(focus.date), geometry.y(focus.totalRate))))
                } else if (samples.size == 1) {
                    drawCircle(visuals.chartTotal, 3.dp.toPx(), position(samples.single()))
                }
            }
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ReviewAuxiliaryText(geometry.start.format(ReviewPreviewDateFormat), "review-trend-start")
            ReviewAuxiliaryText(geometry.end.format(ReviewPreviewDateFormat), "review-trend-end")
        }
    }
}

@Composable
private fun ReviewAuxiliaryText(text: String, tag: String, announce: Boolean = false) {
    Text(
        text, Modifier.testTag(tag).semantics { if (announce) liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ReviewDestinations(
    onProjectHistory: (() -> Unit)?, onSubscriptionHistory: (() -> Unit)?,
    onLongTermReview: (() -> Unit)?, onTotalCostTrend: (() -> Unit)?,
    onCategoryAnalysis: (() -> Unit)?, onAnnualReview: (() -> Unit)?,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        onTotalCostTrend?.let {
            Column(Modifier.testTag("review-full-history-group"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewLink("全部历史与事件", "日期与记录来源", C2Glyph.Review, "review-total-cost-trend", it, horizontalInset = 0)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        if (onCategoryAnalysis != null || onAnnualReview != null || onLongTermReview != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewSectionTitle("分析")
                ReviewGroup("review-analysis-group") {
                    onCategoryAnalysis?.let {
                        ReviewLink("类别分析", "项目与订阅分列", C2Glyph.Project, "review-category-analysis", it)
                    }
                    if (onCategoryAnalysis != null && (onAnnualReview != null || onLongTermReview != null)) ReviewRowDivider()
                    onAnnualReview?.let {
                        ReviewLink("年度回顾", "按资金实际发生年份", C2Glyph.Review, "review-annual", it)
                    }
                    if (onAnnualReview != null && onLongTermReview != null) ReviewRowDivider()
                    onLongTermReview?.let {
                        ReviewLink("长期复盘", "持有天数与折算付费时长", C2Glyph.Review, "review-long-term", it)
                    }
                }
            }
        }
        if (onProjectHistory != null || onSubscriptionHistory != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewSectionTitle("历史档案")
                ReviewGroup("review-archive-group") {
                    onProjectHistory?.let {
                        ReviewLink("已结束长期项目", "含后续结算", C2Glyph.Project, "review-project-history", it)
                    }
                    if (onProjectHistory != null && onSubscriptionHistory != null) ReviewRowDivider()
                    onSubscriptionHistory?.let {
                        ReviewLink(
                            "已停止订阅", "暂停及到期未停止的订阅仍在当前列表", C2Glyph.Subscription,
                            "review-subscription-history", it, visibleDescription = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewSectionTitle(title: String) {
    Text(title, Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
}

@Composable
private fun ReviewGroup(tag: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth().testTag(tag),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(content = content)
    }
}

@Composable
private fun ReviewRowDivider() {
    HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ReviewLink(
    title: String,
    description: String,
    glyph: C2Glyph,
    tag: String,
    onClick: () -> Unit,
    horizontalInset: Int = 12,
    visibleDescription: Boolean = true,
) {
    val supporting = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag(tag)
            .semantics(mergeDescendants = true) { contentDescription = "$title，$description" }
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = horizontalInset.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        C2Icon(glyph, Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            if (visibleDescription) Text(description, style = MaterialTheme.typography.bodySmall, color = supporting)
        }
        C2Icon(C2Glyph.Chevron, Modifier.size(18.dp))
    }
}
