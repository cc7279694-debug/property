package com.cooper.lithic.feature.review

import com.cooper.lithic.core.design.lithicInputColors
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.cooper.lithic.core.design.LithicButton as Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import com.cooper.lithic.core.design.LithicOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountUnit
import com.cooper.lithic.core.design.C2PageHeader
import com.cooper.lithic.core.design.MoneyMetric
import com.cooper.lithic.core.design.LocalLithicVisuals
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.MoneyMetricLevel
import com.cooper.lithic.core.design.moneyMetricDisplay
import com.cooper.lithic.core.design.moneyMetricDescription
import com.cooper.lithic.core.design.moneyMetricFitsAtPreferredSize
import com.cooper.lithic.core.design.ChartGeometry
import com.cooper.lithic.core.design.CostChartPoint
import com.cooper.lithic.core.design.chartDateSelection
import com.cooper.lithic.domain.report.CostTimelinePoint
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.TotalCostEvent
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Read-only view: all amounts, status counts and event identities are supplied by the snapshot. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TotalCostTrendScreen(
    state: TotalCostTrendUiState,
    onSelectDate: (LocalDate) -> Unit,
    onToggleBreakdown: () -> Unit,
    onRetry: () -> Unit,
    onOpenRecord: (OverviewKind, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    reviewHeader: (@Composable () -> Unit)? = null,
    reviewFooter: (@Composable () -> Unit)? = null,
    summaryOnly: Boolean = false,
    hasExternalReadNotice: Boolean = false,
    expectedCounts: TrendRecordCounts? = null,
) {
    var dateRaw by rememberSaveable { mutableStateOf("") }
    var dateError by rememberSaveable { mutableStateOf<String?>(null) }
    var pointsExpanded by rememberSaveable { mutableStateOf(false) }
    var pointLimit by rememberSaveable { mutableIntStateOf(50) }
    var allEvents by rememberSaveable { mutableStateOf(false) }
    var eventLimit by rememberSaveable { mutableIntStateOf(50) }
    var reviewDateExpanded by rememberSaveable { mutableStateOf(false) }
    var eventsExpanded by rememberSaveable { mutableStateOf(false) }
    var toolsExpanded by rememberSaveable { mutableStateOf(false) }
    var informationOpen by rememberSaveable { mutableStateOf(false) }
    val presentation = totalTrendPresentation(state, expectedCounts)
    val timeline = state.timeline?.takeIf { presentation.hasCompleteSnapshot }
    val hasRecords = presentation.hasRecords
    val selected = presentation.selected
    val events = trendEventsForDisplay(state, allEvents)
    val orderedEvents = remember(events) { events.orEmpty().sortedWith(compareByDescending<TotalCostEvent> { it.date }.thenBy { it.id }) }
    fun applyDate() {
        val parsed = try { LocalDate.parse(dateRaw.trim()) } catch (_: DateTimeParseException) { null }
        when {
            parsed == null -> dateError = "请输入有效日期，例如2026-01-20。"
            timeline == null -> dateError = "历史尚未读取成功，请先重试。"
            parsed > timeline.asOf -> dateError = "不能查看业务今天之后的历史日期。"
            else -> { dateError = null; allEvents = false; eventLimit = 50; onSelectDate(parsed) }
        }
    }
    val metricInformation: @Composable ColumnScope.() -> Unit = {
        selected?.let { point ->
            Text(reviewSelectionLabel(point), Modifier.testTag("total-trend-selected-status"))
            Text(selectionMeaning(point), Modifier.testTag("total-trend-selection-meaning"))
        }
        timeline?.let {
            Text("结果截至 ${it.asOf}。按已录入、已纠正事实重建，不代表全部生活消费，也不是当天扣款。真实0、正负相抵与没有有效贡献不同；金额只在展示时舍入。")
        }
        Text("图表按真实自然日间距绘制，保留负值和重要变化，不做平滑预测。横向拖动选择日期后查询精确金额；纵向滑动浏览页面。")
        Text("长跨度精简显示；显示点清单与所有事件仍可继续查看。项目结束日退出总额，后续结算单独作为事件；订阅零区间保留阶梯。")
        Text("同日事件共同列出，不把某一笔当作全部跳变的唯一原因。付款、覆盖边界和状态变化是不同事实；有事件也可能没有总额变化。")
        Text("前后一天限图表日期范围；其他已过去日期可输入查询。")
    }
    LazyColumn(modifier.fillMaxSize().imePadding().testTag("total-cost-trend-screen"),
        contentPadding = PaddingValues(horizontal = LithicSpacing.page, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item("heading") {
            if (reviewHeader != null) reviewHeader()
            else C2PageHeader("成本趋势", onBack, "total-trend-back", backLabel = "返回回看")
        }
        if (presentation.showEmpty && !hasExternalReadNotice) item("empty") {
            Text("记下一件物品后，在这里看见成本变化", Modifier.testTag("total-trend-empty"),
                style = MaterialTheme.typography.bodyLarge)
        } else {
            item("overview") {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("总日均成本", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                                Text("所选日期", Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("total-trend-selection-label"), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(state.selectedDate?.toString() ?: "日期待读取",
                                Modifier.padding(vertical = 4.dp)
                                    .testTag(if (state.selectedDate == null) "total-trend-date-pending" else "total-trend-selected-date"),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                        MoneyMetric(selected?.totalRate, Modifier.fillMaxWidth()
                            .testTag(if (selected == null) "total-trend-total-pending" else "total-trend-total")
                            .semantics { if (presentation.hasRecords && selected == null) stateDescription = "所选日期的精确结果尚未取得" },
                            level = if (summaryOnly) MoneyMetricLevel.HERO else MoneyMetricLevel.DETAIL,
                            color = MaterialTheme.colorScheme.onSurface, label = "总日均成本")
                    }
                    val plot: @Composable () -> Unit = {
                        if (timeline != null && hasRecords) {
                            TotalCostChart(timeline.points, selected, state.selectedDate, state.showBreakdown, onSelectDate,
                                concise = summaryOnly, cutoff = timeline.asOf,
                                heading = { TrendPlotHeading(presentation, onRetry) },
                                informationOpen = informationOpen, onCloseInformation = { informationOpen = false },
                                information = metricInformation)
                        } else TrendPlotPlaceholder(presentation, summaryOnly, onRetry, hasExternalReadNotice, state.showBreakdown)
                    }
                    if (summaryOnly) Surface(Modifier.fillMaxWidth().testTag("review-trend-plot-surface"),
                        shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.background) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            .testTag("total-trend-plot-frame")) { plot() }
                    } else Column(Modifier.fillMaxWidth().testTag("total-trend-plot-frame")) { plot() }
                    DailyContributions(selected)
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val date = state.selectedDate
                        TextButton({ date?.let { onSelectDate(it.minusDays(1)) } }, Modifier.heightIn(min = 48.dp)
                            .testTag("total-trend-previous-day"),
                            enabled = hasRecords && timeline?.from != null && date != null && date > timeline.from && date <= timeline.asOf) { Text("前一天") }
                        TextButton({ date?.let { onSelectDate(it.plusDays(1)) } }, Modifier.heightIn(min = 48.dp)
                            .testTag("total-trend-next-day"),
                            enabled = hasRecords && timeline?.from != null && date != null && date >= timeline.from && date < timeline.asOf) { Text("后一天") }
                        TextButton({ toolsExpanded = !toolsExpanded }, Modifier.heightIn(min = 48.dp).testTag("total-trend-tools-toggle"),
                            enabled = timeline != null && hasRecords) { Text(if (toolsExpanded) "收起工具" else "更多工具") }
                    }
                    if (toolsExpanded && timeline != null && hasRecords) {
                        FlowRow(Modifier.fillMaxWidth().testTag("total-trend-tools"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton({ reviewDateExpanded = !reviewDateExpanded }, Modifier.heightIn(min = 48.dp)
                                .testTag(if (summaryOnly) "review-trend-date-toggle" else "total-trend-date-toggle")) {
                                Text(if (reviewDateExpanded) "收起日期查询" else "指定日期")
                            }
                            TextButton(onToggleBreakdown, Modifier.heightIn(min = 48.dp).testTag("total-trend-breakdown")) {
                                Text(if (state.showBreakdown) "收起分项曲线" else "显示分项曲线")
                            }
                            TextButton({ informationOpen = true }, Modifier.heightIn(min = 48.dp)
                                .testTag(if (summaryOnly) "review-trend-details-toggle" else "total-trend-details-toggle")) { Text("趋势说明") }
                        }
                    }
                }
            }
        }
        if (presentation.recordCountsMismatch) item("record-count-notice") {
            Text("趋势事实尚需核对：当前记录数量与趋势快照不一致。" +
                (presentation.cutoff?.let { "现有趋势仍截至 $it，请刷新核对。" } ?: "请刷新核对。"),
                Modifier.testTag("total-trend-record-count-notice"), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error)
        }
        // Safety notices are persistent, outside the collapsible tools. No loading row is inserted above the plot.
        state.error?.let { error -> item("error") {
            Card(Modifier.fillMaxWidth().testTag("total-trend-error")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (!presentation.hasCompleteSnapshot) "读取失败，不能将错误作为空账本。" else "读取失败；仍保留上次结果，尚未刷新。")
                    presentation.cutoff?.let { Text("上次结果截至 $it") }
                    Text(error)
                    OutlinedButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("total-trend-retry")) { Text("重试") }
                }
            }
        } }
        state.notice?.let { notice -> item("notice") {
            Text(notice, Modifier.testTag("total-trend-notice"), style = MaterialTheme.typography.bodyMedium)
        } }
        if (timeline != null && hasRecords) {
            if (toolsExpanded && reviewDateExpanded) {
                item("date") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(dateRaw, { dateRaw = it; dateError = null }, Modifier.fillMaxWidth().testTag("total-trend-date-input"),
                            label = { Text("查询日期（年-月-日）") }, placeholder = { Text(state.selectedDate?.toString() ?: timeline.asOf.toString()) },
                            colors = lithicInputColors(), singleLine = true, isError = dateError != null,
                            supportingText = { Text(dateError ?: "可查询任意已过去的日期，不必拖动图表。") })
                        Button(::applyDate, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("total-trend-date-apply")) { Text("查看该日") }
                    }
                }
            }
            // Full point/event pages remain on the original dedicated route, reached
            // through Review's guarded callback; no second event implementation is added.
            if (!summaryOnly && toolsExpanded) {
                item("point-heading") {
                    OutlinedButton({ pointsExpanded = !pointsExpanded }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("total-trend-points-toggle")) {
                        Text(if (pointsExpanded) "收起显示点文字清单" else "查看显示点文字清单（${timeline.points.size}个）")
                    }
                }
                if (pointsExpanded) {
                    items(timeline.points.take(pointLimit), key = { "point-${it.date}" }) { point ->
                        OutlinedButton({ allEvents = false; eventLimit = 50; onSelectDate(point.date) },
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("total-trend-point-${point.date}")) {
                            Column {
                                Text(point.date.toString())
                                DailyValue("总日均成本", point.totalRate, "total-trend-point-total-${point.date}")
                                DailyValue("长期项目贡献", point.projectRate, "total-trend-point-project-${point.date}")
                                DailyValue("订阅贡献", point.subscriptionRate, "total-trend-point-subscription-${point.date}")
                            }
                        }
                    }
                    if (timeline.points.size > pointLimit) item("more-points") {
                        TextButton({ pointLimit += 50 }, Modifier.heightIn(min = 48.dp).testTag("total-trend-points-more")) { Text("再显示50个日期") }
                    }
                }
                item("event-heading") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton({ eventsExpanded = !eventsExpanded }, Modifier.heightIn(min = 48.dp).testTag("total-trend-events-toggle")) {
                            Text(if (eventsExpanded) "收起相关事件" else "查看相关事件")
                        }
                    }
                }
                if (eventsExpanded) {
                    item("event-scope") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton({ allEvents = !allEvents; eventLimit = 50 },
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("total-trend-events-scope")) {
                                Text(if (allEvents) "只看所选日期事件" else "查看全部日期事件")
                            }
                            if (events == null) Text("所选日期的事件尚未取得。", Modifier.testTag("total-trend-event-pending"))
                            else {
                                Text(if (allEvents) "全部已记录历史事件：${orderedEvents.size}条" else "所选日期相关事件：${orderedEvents.size}条",
                                    Modifier.testTag("total-trend-event-count"))
                                if (orderedEvents.isEmpty()) Text(if (allEvents) "已读取的历史中没有相关事件。" else
                                    "该日没有相关事件；历史成本仍按已录入事实计算，不推断未录入的支出。")
                            }
                        }
                    }
                    items(orderedEvents.take(eventLimit), key = { "event-${it.id}" }) { event ->
                        Column(Modifier.fillMaxWidth().testTag("total-trend-event-${event.id}")) {
                            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${event.date} · ${event.title}", style = MaterialTheme.typography.titleMedium)
                                Text("${if (event.kind == OverviewKind.PROJECT) "长期项目" else "订阅"}：${event.recordName}")
                                Text(event.description)
                                event.amountCents?.let { MoneyMetric(BigDecimal.valueOf(it, 2), unit = AmountUnit.YUAN,
                                    level = MoneyMetricLevel.ROW, label = event.title) }
                                if (event.nonCash) Text("非现金折抵（不是现金扣款）")
                                TextButton({ onOpenRecord(event.kind, event.recordId) },
                                    Modifier.heightIn(min = 48.dp).testTag("total-trend-open-${event.id}")) { Text("查看所属记录") }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                    if (orderedEvents.size > eventLimit) item("more-events") {
                        OutlinedButton({ eventLimit += 50 }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("total-trend-events-more")) {
                            Text("再显示50条事件（已显示${minOf(eventLimit, orderedEvents.size)}/${orderedEvents.size}）")
                        }
                    }
                }
            }
        }
        reviewFooter?.let { footer -> item("review-destinations") { footer() } }
    }
}

@Composable
private fun DailyContributions(point: CostTimelinePoint?) {
    Column(Modifier.fillMaxWidth().testTag("total-trend-contributions").padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("所选日期贡献", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        DailyValue("长期项目贡献", point?.projectRate, if (point == null) "total-trend-project-pending" else "total-trend-project")
        DailyValue("订阅贡献", point?.subscriptionRate, if (point == null) "total-trend-subscription-pending" else "total-trend-subscription")
    }
}

@Composable
private fun DailyValue(label: String, value: BigDecimal?, tag: String) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val rowAmountWidth = (maxWidth - 8.dp).coerceAtLeast(0.dp) / 2f
        val amountFitsRow = moneyMetricFitsAtPreferredSize(value, rowAmountWidth, level = MoneyMetricLevel.ROW)
        if (maxWidth < 280.dp || fontScale > 1.3f || !amountFitsRow) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoneyMetric(value, Modifier.fillMaxWidth().testTag(tag), level = MoneyMetricLevel.ROW, label = label)
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoneyMetric(value, Modifier.weight(1f).testTag(tag), level = MoneyMetricLevel.ROW, label = label)
        }
    }
}

private fun selectionMeaning(point: CostTimelinePoint): String = when {
    point.projectCount == 0L && point.subscriptionCount == 0L -> "该日尚未有已录入记录生效；不是当年没有花钱的证明。"
    point.activeProjectCount == 0L && point.activeSubscriptionCount == 0L -> "该日没有有效成本贡献；项目结束日退出，暂停、到期、停止或无付费覆盖的订阅贡献为0。"
    point.totalRate.signum() == 0 && point.hasPositiveContribution && point.hasNegativeContribution -> "该日正负贡献相互抵消为0；两类小计和真实负值均保留。"
    point.totalRate.signum() == 0 -> "该日存在有效记录，按已录入事实计算的真实成本为0。"
    else -> "该日有效贡献：${point.activeProjectCount}个长期项目、${point.activeSubscriptionCount}个订阅。已结束项目后续结算只作相关事件，不重新加入总额。"
}

/** One date axis and numeric scale. Line styles and readable values supplement colour. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TotalCostChart(points: List<CostTimelinePoint>, selected: CostTimelinePoint?, requestedDate: LocalDate?, breakdown: Boolean,
    onSelectDate: (LocalDate) -> Unit, concise: Boolean = false, cutoff: LocalDate,
    heading: @Composable () -> Unit, informationOpen: Boolean, onCloseInformation: () -> Unit,
    information: @Composable ColumnScope.() -> Unit) {
    if (points.isEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            heading()
            TrendLegend(breakdown)
            TrendAxisUnit()
            Box(Modifier.fillMaxWidth().height(if (concise) 172.dp else 240.dp), contentAlignment = Alignment.Center) {
                Text("截至本次业务日尚无可绘制的记录历史。", Modifier.testTag("total-trend-no-history"))
            }
            TrendAxisRow("尚无历史范围", "截至 $cutoff")
        }
        if (informationOpen) TrendInformationDialog(onCloseInformation) {
            Text("尚无可绘制的坐标范围。", Modifier.testTag("total-trend-axis"))
            information()
        }
        return
    }
    val ordered = remember(points) { points.sortedBy { it.date } }
    val inRange = selected?.takeIf { it.date >= ordered.first().date && it.date <= ordered.last().date }
    val baseProjection = remember(ordered, breakdown) {
        ChartGeometry.forPoints(ordered.flatMap { p ->
            if (breakdown) listOf(CostChartPoint(p.date, p.totalRate), CostChartPoint(p.date, p.projectRate), CostChartPoint(p.date, p.subscriptionRate))
            else listOf(CostChartPoint(p.date, p.totalRate))
        })!!
    }
    val projection = remember(baseProjection, inRange, breakdown) {
        val selectedValues = inRange?.let { if (breakdown) listOf(it.totalRate, it.projectRate, it.subscriptionRate) else listOf(it.totalRate) }.orEmpty()
        if (selectedValues.all { it >= baseProjection.minimum && it <= baseProjection.maximum }) baseProjection
        else ChartGeometry.forPoints(listOf(CostChartPoint(baseProjection.start, baseProjection.minimum),
            CostChartPoint(baseProjection.end, baseProjection.maximum)) + selectedValues.map { CostChartPoint(requireNotNull(inRange).date, it) })!!
    }
    val coordinates = remember(ordered, projection, breakdown) {
        ordered.map { point ->
            floatArrayOf(projection.x(point.date), projection.y(point.totalRate),
                if (breakdown) projection.y(point.projectRate) else 0f,
                if (breakdown) projection.y(point.subscriptionRate) else 0f)
        }
    }
    val visuals = LocalLithicVisuals.current
    val totalColor = visuals.chartTotal
    val projectColor = visuals.chartProject
    val subscriptionColor = visuals.chartSubscription
    val axisColor = visuals.chartGrid
    val selectedColor = MaterialTheme.colorScheme.onSurface
    val currentSelect by rememberUpdatedState(onSelectDate)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        heading()
        TrendLegend(breakdown)
        TrendAxisUnit()
        Canvas(Modifier.fillMaxWidth().height(if (concise) 172.dp else 240.dp).testTag("total-trend-canvas")
            .semantics {
                contentDescription = (if (breakdown) "总日均成本实线、长期项目长虚线、订阅短虚线阶梯，共同日期和金额尺度。负值保留。下方可输入日期和查看文字清单。"
                    else "总日均成本历史实线，负值保留。可展开两类贡献，或在下方输入日期和查看文字清单。") +
                    "纵轴：${moneyMetricDescription(projection.minimum)} 至 ${moneyMetricDescription(projection.maximum)}；按自然日绘制。"
                stateDescription = inRange?.let { "${it.date}，${moneyMetricDescription(it.totalRate, label = "总日均成本")}，${moneyMetricDescription(it.projectRate, label = "长期项目贡献")}，${moneyMetricDescription(it.subscriptionRate, label = "订阅贡献")}" }
                    ?: requestedDate?.let { "$it，尚未取得所选日期的精确结果" } ?: "尚未取得所选日期的精确结果"
                val focus = requestedDate ?: inRange?.date ?: ordered.first().date
                customActions = listOf(CustomAccessibilityAction("前一天") {
                    if (focus > projection.start && focus <= projection.end) {
                        currentSelect(projection.adjacentDay(focus, -1)); true
                    } else false
                }, CustomAccessibilityAction("后一天") {
                    if (focus >= projection.start && focus < projection.end) {
                        currentSelect(projection.adjacentDay(focus, 1)); true
                    } else false
                })
            }
            .chartDateSelection(projection) { currentSelect(it) }) {
            val inset = 12.dp.toPx()
            val w = (size.width - 2 * inset).coerceAtLeast(1f)
            val h = (size.height - 2 * inset).coerceAtLeast(1f)
            fun pos(date: LocalDate, value: BigDecimal) = Offset(inset + projection.x(date) * w, inset + projection.y(value) * h)
            val zero = inset + projection.zero * h
            drawLine(axisColor, Offset(inset, zero), Offset(inset + w, zero), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
            fun series(color: Color, step: Boolean, dash: FloatArray?, seriesIndex: Int, value: (CostTimelinePoint) -> BigDecimal) {
                val path = Path()
                ordered.forEachIndexed { index, p ->
                    val normalized = coordinates[index]
                    val coordinate = Offset(inset + normalized[0] * w, inset + normalized[seriesIndex] * h)
                    if (index == 0) path.moveTo(coordinate.x, coordinate.y)
                    else {
                        if (step) path.lineTo(coordinate.x, inset + coordinates[index - 1][seriesIndex] * h)
                        path.lineTo(coordinate.x, coordinate.y)
                    }
                }
                drawPath(path, color, style = Stroke(2.dp.toPx(), pathEffect = dash?.let { PathEffect.dashPathEffect(it) }))
                if (ordered.size == 1) drawCircle(color, 3.dp.toPx(), pos(ordered[0].date, value(ordered[0])))
                inRange?.let { drawCircle(color, 4.dp.toPx(), pos(it.date, value(it))) }
            }
            if (breakdown) {
                series(projectColor, false, floatArrayOf(10.dp.toPx(), 5.dp.toPx()), 2) { it.projectRate }
                series(subscriptionColor, true, floatArrayOf(3.dp.toPx(), 4.dp.toPx()), 3) { it.subscriptionRate }
            }
            series(totalColor, false, null, 1) { it.totalRate }
            inRange?.let { p -> val x = pos(p.date, p.totalRate).x
                drawLine(selectedColor, Offset(x, inset), Offset(x, inset + h), 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx())))
                drawCircle(selectedColor, 7.dp.toPx(), pos(p.date, p.totalRate), style = Stroke(1.5.dp.toPx()))
            }
        }
        TrendAxisRow(if (projection.start == projection.end) "单日记录" else projection.start.toString(), "截至 $cutoff")
    }
    if (informationOpen) TrendInformationDialog(onCloseInformation) {
        Text("纵轴：${moneyMetricDisplay(projection.minimum)} 至 ${moneyMetricDisplay(projection.maximum)}",
            Modifier.testTag("total-trend-axis"))
        Text("图表日期：${projection.start} 至 ${projection.end}。")
        Text("三条序列共用日期轴与金额尺度，零线区分正负，不使用百分比堆叠。真实自然日间距、单点、负值和订阅零区间均保留。")
        Text("总日均为实线；长期项目为长虚线；订阅为短虚线阶梯。横向拖动只选择日期，再查询真实结果，不从连线估算金额。")
        information()
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrendLegend(breakdown: Boolean) {
    val visuals = LocalLithicVisuals.current
    FlowRow(Modifier.fillMaxWidth().testTag("total-trend-legend"),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TrendLegendItem("总日均成本 · 实线", visuals.chartTotal, 0, "total-trend-legend-total")
        if (breakdown) {
            TrendLegendItem("长期项目 · 长虚线", visuals.chartProject, 1, "total-trend-legend-project")
            TrendLegendItem("订阅 · 短虚线阶梯", visuals.chartSubscription, 2, "total-trend-legend-subscription")
        }
    }
}

/** Labels and distinct stroke patterns remain readable without colour discrimination. */
@Composable
private fun TrendLegendItem(label: String, color: Color, pattern: Int, tag: String) {
    Row(Modifier.testTag(tag), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(width = 24.dp, height = 12.dp)) {
            val dash = when (pattern) {
                1 -> floatArrayOf(8.dp.toPx(), 4.dp.toPx())
                2 -> floatArrayOf(2.dp.toPx(), 3.dp.toPx())
                else -> null
            }
            if (pattern == 2) {
                val path = Path().apply {
                    moveTo(0f, size.height * .75f)
                    lineTo(size.width / 2, size.height * .75f)
                    lineTo(size.width / 2, size.height * .25f)
                    lineTo(size.width, size.height * .25f)
                }
                drawPath(path, color, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(requireNotNull(dash))))
            } else drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                2.dp.toPx(), pathEffect = dash?.let { PathEffect.dashPathEffect(it) })
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrendPlotHeading(presentation: TotalTrendPresentation, onRetry: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("成本趋势", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface)
        Box(Modifier.width(128.dp).heightIn(min = 48.dp), contentAlignment = Alignment.CenterEnd) {
            if (presentation.phase == TrendReadPhase.FAILED) {
                TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("total-trend-compact-retry")) {
                    Text("失败 · 重试", color = MaterialTheme.colorScheme.error)
                }
            } else if (presentation.recordCountsMismatch || presentation.phase != TrendReadPhase.READY || (presentation.hasRecords && presentation.selected == null)) {
                val label = if (presentation.recordCountsMismatch) "待核对" else when (presentation.phase) {
                    TrendReadPhase.REFRESHING -> "刷新中"
                    TrendReadPhase.READING -> "读取中"
                    TrendReadPhase.READY -> "计算中"
                    else -> "待载入"
                }
                Text(label, Modifier.testTag("total-trend-refresh-status").semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = if (presentation.recordCountsMismatch) {
                        "趋势事实尚需核对；保留截至 ${presentation.cutoff} 的趋势快照"
                    } else if (presentation.phase == TrendReadPhase.READY) {
                        "所选日期计算中，尚未取得精确金额；图表截至 ${presentation.cutoff}"
                    } else label + (presentation.cutoff?.let { "，保留截至 $it 的上次结果" } ?: "，尚未取得可信历史")
                }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TrendPlotPlaceholder(presentation: TotalTrendPresentation, summaryOnly: Boolean, onRetry: () -> Unit, hasExternalReadNotice: Boolean, breakdown: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TrendPlotHeading(presentation, onRetry)
        TrendLegend(breakdown)
        TrendAxisUnit()
        Box(Modifier.fillMaxWidth().height(if (summaryOnly) 172.dp else 240.dp)
            .testTag("total-trend-loading-placeholder"), contentAlignment = Alignment.Center) {
            Text(if (presentation.recordCountsMismatch) "记录已变化，趋势尚待核对" else
                if (hasExternalReadNotice && !presentation.hasRecords) "当前记录快照需要核对" else when (presentation.phase) {
                TrendReadPhase.FAILED -> "历史未能读取，请重试"
                TrendReadPhase.WAITING -> "成本趋势尚未读取"
                TrendReadPhase.READY -> "尚无可绘制的记录历史"
                else -> "正在读取完整历史"
            }, Modifier.testTag(if (presentation.phase == TrendReadPhase.WAITING && summaryOnly) "review-trend-unavailable" else "total-trend-pending-message"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TrendAxisRow("起始日期待读取", presentation.cutoff?.let { "截至 $it" } ?: "截止日期待读取")
    }
}

/** Identical in waiting and real plot frames; ranges remain fully readable in the existing explanation. */
@Composable
private fun TrendAxisUnit() {
    Text("纵轴 · 元/天", Modifier.fillMaxWidth().testTag("total-trend-axis-unit"),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TrendAxisRow(start: String, end: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(start, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(end, Modifier.weight(1f).testTag("total-trend-cutoff"), textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrendInformationDialog(onClose: () -> Unit, information: @Composable ColumnScope.() -> Unit) {
    AlertDialog(onDismissRequest = onClose, title = { Text("趋势说明") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()).testTag("total-trend-information"),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = information)
    }, confirmButton = { TextButton(onClose, Modifier.heightIn(min = 48.dp)) { Text("知道了") } })
}
