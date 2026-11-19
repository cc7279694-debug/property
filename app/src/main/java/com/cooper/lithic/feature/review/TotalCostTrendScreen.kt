package com.cooper.lithic.feature.review

import com.cooper.lithic.core.design.lithicInputColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.lazy.itemsIndexed
import com.cooper.lithic.core.design.LithicButton as Button
import androidx.compose.material3.MaterialTheme
import com.cooper.lithic.core.design.LithicOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import com.cooper.lithic.core.design.LithicIconButton as IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountUnit
import com.cooper.lithic.core.design.C2PageHeader
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.MoneyMetric
import com.cooper.lithic.core.design.LocalLithicVisuals
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

/** Read-only view: amounts, dates and event identities come from the existing snapshot. */
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
) {
    var dateRaw by rememberSaveable { mutableStateOf("") }
    var dateError by rememberSaveable { mutableStateOf<String?>(null) }
    var pointsExpanded by rememberSaveable { mutableStateOf(false) }
    var pointLimit by rememberSaveable { mutableIntStateOf(50) }
    var allEvents by rememberSaveable { mutableStateOf(false) }
    var eventLimit by rememberSaveable { mutableIntStateOf(50) }
    var dateExpanded by rememberSaveable { mutableStateOf(false) }
    var eventsExpanded by rememberSaveable { mutableStateOf(false) }
    var toolsExpanded by rememberSaveable { mutableStateOf(false) }
    var informationOpen by rememberSaveable { mutableStateOf(false) }
    val presentation = totalTrendPresentation(state)
    val timeline = state.timeline?.takeIf { presentation.hasCompleteSnapshot }
    val hasRecords = presentation.hasRecords
    val selected = presentation.selected
    val observedSingleDate = timeline?.takeIf { hasRecords && trendHasSingleDate(it) }?.asOf
    val singleDate = observedSingleDate != null
    val chart = if (timeline != null && hasRecords && !singleDate) {
        rememberTrendChart(timeline.points, selected, state.showBreakdown)
    } else null
    val selectedEvents = trendEventsForDisplay(state, allDates = false)
    val displayedEvents = if (!eventsExpanded) {
        remember(timeline) { trendRecentEvents(timeline?.events.orEmpty()) }
    } else {
        val events = if (allEvents) trendEventsForDisplay(state, allDates = true) else selectedEvents
        remember(events) { events.orEmpty().sortedWith(trendEventOrder) }
    }

    fun applyDate() {
        val parsed = try { LocalDate.parse(dateRaw.trim()) } catch (_: DateTimeParseException) { null }
        when {
            parsed == null -> dateError = "请输入有效日期，例如2026-01-20。"
            timeline == null -> dateError = "历史尚未读取成功，请先重试。"
            parsed > timeline.asOf -> dateError = "不能查看业务今天之后的历史日期。"
            else -> {
                dateError = null
                allEvents = false
                eventLimit = 50
                onSelectDate(parsed)
            }
        }
    }

    val metricInformation: @Composable ColumnScope.() -> Unit = {
        selected?.let { point ->
            Text(reviewSelectionLabel(point), Modifier.testTag("total-trend-selected-status"))
            Text(trendSelectionMeaning(point), Modifier.testTag("total-trend-selection-meaning"))
        }
        timeline?.let {
            Text("截至 ${it.asOf}。趋势按已录入及更正的记录重建，非当天扣款或全部生活消费。区分真实零值、相抵和无有效贡献，仅展示时舍入。")
        }
        Text("按自然日保留负值和重要变化，不预测。项目结束日退出总额，后续结算另列，订阅保留零值台阶。")
        Text("长跨度会精简显示，日期和事件仍可查看。事件可能不改变总额，付款、覆盖边界与状态变化按实际记录分别展示。")
        Text("前后切换限图表范围。其他已过去日期可输入查询。")
    }

    Box(modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 560.dp).fillMaxSize().testTag("total-cost-trend-screen"),
            contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
        ) {
            item("heading") {
                C2PageHeader("成本趋势", onBack, "total-trend-back", backLabel = "返回回看") {
                    IconButton({ informationOpen = true }, Modifier.size(48.dp).testTag("total-trend-details-toggle")) {
                        C2Icon(C2Glyph.Info, description = "趋势说明")
                    }
                }
            }
            if (presentation.showEmpty) {
                item("empty") {
                    Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TrendSectionTitle("暂无记录")
                        Text("记下一件物品后，在这里看见成本变化", Modifier.testTag("total-trend-empty"),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item("overview") {
                    TrendSurface(Modifier.padding(top = 8.dp).testTag("total-trend-plot-frame")) {
                        Column(
                            if (singleDate) Modifier.testTag("trend-single-date-facts") else Modifier,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TrendReadout(state.selectedDate, selected, hasRecords)
                            TrendReadStatus(presentation, onRetry)
                            when {
                                observedSingleDate != null -> {
                                    Text("趋势范围只有一天",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TrendCutoff(observedSingleDate)
                                }
                                timeline != null && chart != null -> TotalCostChart(
                                    chart, selected, state.selectedDate, state.showBreakdown, onSelectDate,
                                    start = timeline.from, cutoff = timeline.asOf,
                                )
                                timeline != null && hasRecords -> {
                                    TrendLegend(state.showBreakdown)
                                    TrendAxisUnit()
                                    Text("截至本次业务日尚无可绘制的记录历史。", Modifier.testTag("total-trend-no-history"))
                                    TrendAxisRow("尚无历史范围", "截至 ${timeline.asOf}")
                                }
                                else -> TrendPlotPlaceholder(presentation)
                            }
                            if (hasRecords) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                DailyContributions(selected)
                            }
                            if (singleDate) {
                                Text(selected?.let(::reviewSelectionLabel) ?: "尚未取得所选日期的精确结果。",
                                    Modifier.testTag("total-trend-single-status"), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            state.error?.let { error ->
                item("error") {
                    TrendSurface(Modifier.padding(top = 12.dp).testTag("total-trend-error")) {
                        Text(if (presentation.hasCompleteSnapshot) "读取失败；仍保留上次结果，尚未刷新。"
                            else "读取失败，不能将错误作为空账本。", color = MaterialTheme.colorScheme.error)
                        presentation.cutoff?.let { Text("上次结果截至 $it") }
                        Text(error)
                        OutlinedButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("total-trend-retry")) { Text("重试") }
                    }
                }
            }
            state.notice?.let { notice ->
                item("notice") {
                    Text(notice, Modifier.padding(top = 12.dp).testTag("total-trend-notice"),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (timeline != null && hasRecords) {
                item("date-tools") {
                    Column(Modifier.padding(top = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val date = state.selectedDate
                            TextButton({ date?.let { onSelectDate(it.minusDays(1)) } },
                                Modifier.heightIn(min = 48.dp).testTag("total-trend-previous-day"),
                                enabled = timeline.from != null && date != null && date > timeline.from && date <= timeline.asOf) {
                                Text("前一天")
                            }
                            TextButton({ date?.let { onSelectDate(it.plusDays(1)) } },
                                Modifier.heightIn(min = 48.dp).testTag("total-trend-next-day"),
                                enabled = timeline.from != null && date != null && date >= timeline.from && date < timeline.asOf) {
                                Text("后一天")
                            }
                            TextButton({ toolsExpanded = !toolsExpanded },
                                Modifier.heightIn(min = 48.dp).testTag("total-trend-tools-toggle")) {
                                Text(if (toolsExpanded) "收起工具" else "更多工具")
                                C2Icon(C2Glyph.Chevron, Modifier.size(16.dp))
                            }
                        }
                        if (toolsExpanded) {
                            FlowRow(Modifier.fillMaxWidth().testTag("total-trend-tools"),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton({ dateExpanded = !dateExpanded },
                                    Modifier.heightIn(min = 48.dp).testTag("total-trend-date-toggle")) {
                                    Text(if (dateExpanded) "收起日期查询" else "指定日期")
                                }
                                if (!singleDate) TextButton(onToggleBreakdown,
                                    Modifier.heightIn(min = 48.dp).testTag("total-trend-breakdown")) {
                                    Text(if (state.showBreakdown) "收起分项曲线" else "显示分项曲线")
                                }
                            }
                        }
                    }
                }
                if (toolsExpanded && dateExpanded) {
                    item("date") {
                        Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(dateRaw, { dateRaw = it; dateError = null },
                                Modifier.fillMaxWidth().testTag("total-trend-date-input"),
                                label = { Text("查询日期（年-月-日）") },
                                placeholder = { Text(state.selectedDate?.toString() ?: timeline.asOf.toString()) },
                                colors = lithicInputColors(), shape = RoundedCornerShape(14.dp), singleLine = true,
                                isError = dateError != null,
                                supportingText = { Text(dateError ?: "可查询任意已过去的日期，不必拖动图表。") })
                            Button(::applyDate, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("total-trend-date-apply")) {
                                Text("查看该日")
                            }
                        }
                    }
                }
                item("selected-events") {
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TrendSectionTitle("所选日期事件")
                        TrendSurface(Modifier.testTag("total-trend-selected-events")) {
                            Text(state.selectedDate?.toString() ?: "日期待读取", style = MaterialTheme.typography.titleSmall)
                            Text(when {
                                selectedEvents == null -> "所选日期的事件尚未取得"
                                selectedEvents.isEmpty() -> "当日无已录入事件"
                                else -> "当日有 ${selectedEvents.size} 条已录入事件"
                            }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item("event-heading") {
                    Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TrendSectionTitle("日期与事件")
                        TextButton({ eventsExpanded = !eventsExpanded },
                            Modifier.heightIn(min = 48.dp).testTag("total-trend-events-toggle")) {
                            Text(if (eventsExpanded) "收起相关事件" else "查看相关事件")
                            C2Icon(C2Glyph.Chevron, Modifier.size(16.dp))
                        }
                    }
                }
                if (eventsExpanded) {
                    item("event-scope") {
                        Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton({ allEvents = !allEvents; eventLimit = 50 },
                                Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("total-trend-events-scope")) {
                                Text(if (allEvents) "只看所选日期事件" else "查看全部日期事件")
                            }
                            if (!allEvents && selectedEvents == null) {
                                Text("所选日期的事件尚未取得。", Modifier.testTag("total-trend-event-pending"))
                            } else {
                                Text(if (allEvents) "全部已记录历史事件：${displayedEvents.size}条"
                                    else "所选日期相关事件：${displayedEvents.size}条",
                                    Modifier.testTag("total-trend-event-count"))
                                if (displayedEvents.isEmpty()) Text(if (allEvents) "已读取的历史中没有相关事件。" else
                                    "该日没有相关事件；历史成本仍按已录入事实计算，不推断未录入的支出。")
                            }
                        }
                    }
                }
                val visibleEvents = displayedEvents.take(eventLimit)
                itemsIndexed(visibleEvents, key = { _, event -> "event-${event.id}" }) { index, event ->
                    TrendEventRow(event, eventsExpanded, index == 0, index == visibleEvents.lastIndex) {
                        onOpenRecord(event.kind, event.recordId)
                    }
                }
                if (displayedEvents.size > eventLimit) {
                    item("more-events") {
                        OutlinedButton({ eventLimit += 50 },
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("total-trend-events-more")) {
                            Text("再显示50条事件（已显示${minOf(eventLimit, displayedEvents.size)}/${displayedEvents.size}）")
                        }
                    }
                }
                item("point-heading") {
                    TextButton({ pointsExpanded = !pointsExpanded },
                        Modifier.padding(top = 12.dp).heightIn(min = 48.dp).testTag("total-trend-points-toggle")) {
                        Text(if (pointsExpanded) "收起显示点文字清单" else "查看显示点文字清单（${timeline.points.size}个）")
                        C2Icon(C2Glyph.Chevron, Modifier.size(16.dp))
                    }
                }
                if (pointsExpanded) {
                    items(timeline.points.take(pointLimit), key = { "point-${it.date}" }) { point ->
                        OutlinedButton({ allEvents = false; eventLimit = 50; onSelectDate(point.date) },
                            Modifier.padding(bottom = 8.dp).fillMaxWidth().heightIn(min = 48.dp).testTag("total-trend-point-${point.date}")) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(point.date.toString())
                                DailyValue("总日均成本", point.totalRate, "total-trend-point-total-${point.date}")
                                DailyValue("长期项目贡献", point.projectRate, "total-trend-point-project-${point.date}")
                                DailyValue("订阅贡献", point.subscriptionRate, "total-trend-point-subscription-${point.date}")
                            }
                        }
                    }
                    if (timeline.points.size > pointLimit) {
                        item("more-points") {
                            TextButton({ pointLimit += 50 }, Modifier.heightIn(min = 48.dp).testTag("total-trend-points-more")) {
                                Text("再显示50个日期")
                            }
                        }
                    }
                }
            }
        }
    }
    if (informationOpen) {
        TrendInformationDialog({ informationOpen = false }) {
            val projection = chart?.projection
            if (projection != null) {
                Text("纵轴：${moneyMetricDisplay(projection.minimum)} 至 ${moneyMetricDisplay(projection.maximum)}",
                    Modifier.testTag("total-trend-axis"))
                Text("图表日期：${timeline?.from ?: projection.start} 至 ${timeline?.asOf ?: projection.end}。")
                Text("三条序列共用日期轴与金额尺度，零线区分正负。总日均为实线，长期项目为长虚线，订阅为短虚线阶梯。")
                Text("横向拖动选择日期，再查询精确结果。纵向滑动浏览页面。")
            } else {
                Text(if (singleDate) "已取得的观察范围只有一个自然日，显示事实金额及两类贡献。"
                    else "尚无可绘制的坐标范围。", Modifier.testTag("total-trend-axis"))
            }
            metricInformation()
        }
    }
}

@Composable
private fun TrendSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun TrendSectionTitle(title: String) {
    Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrendReadout(date: LocalDate?, selected: CostTimelinePoint?, hasRecords: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("总日均成本", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("所选日期", Modifier.testTag("total-trend-selection-label"),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(date?.toString() ?: "日期待读取",
                    Modifier.testTag(if (date == null) "total-trend-date-pending" else "total-trend-selected-date"),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        MoneyMetric(selected?.totalRate, Modifier.fillMaxWidth()
            .testTag(if (selected == null) "total-trend-total-pending" else "total-trend-total")
            .semantics { if (hasRecords && selected == null) stateDescription = "所选日期的精确结果尚未取得" },
            level = MoneyMetricLevel.DETAIL, label = "总日均成本")
    }
}

@Composable
private fun TrendEventRow(event: TotalCostEvent, expanded: Boolean, first: Boolean, last: Boolean, onOpen: () -> Unit) {
    val top = if (first) 14.dp else 0.dp
    val bottom = if (last) 14.dp else 0.dp
    val shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    Column(Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surface)
        .testTag("total-trend-event-${event.id}")) {
        if (!first) HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
        val body = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        if (expanded) {
            Column(body, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(event.title, style = MaterialTheme.typography.titleSmall)
                Text("${event.date} · ${if (event.kind == OverviewKind.PROJECT) "长期项目" else "订阅"}：${event.recordName}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(event.description, style = MaterialTheme.typography.bodyMedium)
                event.amountCents?.let { MoneyMetric(BigDecimal.valueOf(it, 2), unit = AmountUnit.YUAN,
                    level = MoneyMetricLevel.ROW, label = event.title) }
                if (event.nonCash) Text("非现金折抵（不是现金扣款）", style = MaterialTheme.typography.bodyMedium)
                TextButton(onOpen, Modifier.heightIn(min = 48.dp).testTag("total-trend-open-${event.id}")) {
                    Text("查看所属记录")
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen)
                .heightIn(min = 50.dp).then(body), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(event.title, style = MaterialTheme.typography.titleSmall)
                    Text("${event.date} · ${event.recordName}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    event.amountCents?.let { MoneyMetric(BigDecimal.valueOf(it, 2), unit = AmountUnit.YUAN,
                        level = MoneyMetricLevel.ROW, label = event.title) }
                    if (event.nonCash) Text("非现金折抵", style = MaterialTheme.typography.bodySmall)
                }
                C2Icon(C2Glyph.Chevron, Modifier.size(18.dp))
            }
        }
    }
}
@Composable
private fun DailyContributions(point: CostTimelinePoint?) {
    Column(Modifier.fillMaxWidth().testTag("total-trend-contributions"),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

private data class TrendChartFrame(
    val ordered: List<CostTimelinePoint>,
    val inRange: CostTimelinePoint?,
    val projection: ChartGeometry,
)

/** One unchanged projection is shared by the chart and the information dialog. */
@Composable
private fun rememberTrendChart(points: List<CostTimelinePoint>, selected: CostTimelinePoint?, breakdown: Boolean): TrendChartFrame? {
    if (points.isEmpty()) return null
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
    return TrendChartFrame(ordered, inRange, projection)
}

/** One date axis and numeric scale. Line styles and readable values supplement colour. */
@Composable
private fun TotalCostChart(chart: TrendChartFrame, selected: CostTimelinePoint?, requestedDate: LocalDate?, breakdown: Boolean,
    onSelectDate: (LocalDate) -> Unit, start: LocalDate?, cutoff: LocalDate) {
    val (ordered, inRange, projection) = chart
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
        TrendLegend(breakdown)
        TrendAxisScale(projection)
        Canvas(Modifier.fillMaxWidth().height(240.dp).testTag("total-trend-canvas")
            .semantics {
                contentDescription = (if (breakdown) "总日均成本实线、长期项目长虚线、订阅短虚线阶梯，共同日期和金额尺度。负值保留。下方可输入日期和查看文字清单。"
                    else "总日均成本历史实线，负值保留。可展开两类贡献，或在下方输入日期和查看文字清单。") +
                    "纵轴：${moneyMetricDescription(projection.minimum)} 至 ${moneyMetricDescription(projection.maximum)}；按自然日绘制。"
                stateDescription = selected?.let {
                    "${it.date}，${moneyMetricDescription(it.totalRate, label = "总日均成本")}，" +
                        "${moneyMetricDescription(it.projectRate, label = "长期项目贡献")}，" +
                        moneyMetricDescription(it.subscriptionRate, label = "订阅贡献") +
                        (if (inRange == null) "，该日期不在绘图范围内" else "")
                }
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
            drawLine(axisColor, Offset(inset, inset), Offset(inset + w, inset), 1.dp.toPx())
            drawLine(axisColor, Offset(inset, inset + h), Offset(inset + w, inset + h), 1.dp.toPx())
            val zero = inset + projection.zero * h
            drawLine(axisColor, Offset(inset, zero), Offset(inset + w, zero), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
            fun series(color: Color, step: Boolean, dash: FloatArray?, seriesIndex: Int, value: (CostTimelinePoint) -> BigDecimal) {
                val path = Path()
                for (index in ordered.indices) {
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
        TrendAxisRow((start ?: projection.start).toString(), "截至 $cutoff")
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
private fun TrendReadStatus(presentation: TotalTrendPresentation, onRetry: () -> Unit) {
    if (presentation.phase == TrendReadPhase.FAILED) {
        TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("total-trend-compact-retry")) {
            Text("失败 · 重试", color = MaterialTheme.colorScheme.error)
        }
    } else if (presentation.phase != TrendReadPhase.READY || (presentation.hasRecords && presentation.selected == null)) {
        val label = when (presentation.phase) {
            TrendReadPhase.REFRESHING -> "刷新中"
            TrendReadPhase.READING -> "读取中"
            TrendReadPhase.READY -> "计算中"
            else -> "待载入"
        }
        Text(label, Modifier.testTag("total-trend-refresh-status").semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = if (presentation.phase == TrendReadPhase.READY) {
                "所选日期计算中，尚未取得精确金额；历史截至 ${presentation.cutoff}"
            } else label + (presentation.cutoff?.let { "，保留截至 $it 的上次结果" } ?: "，尚未取得可信历史")
        }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrendPlotPlaceholder(presentation: TotalTrendPresentation) {
    Column(Modifier.fillMaxWidth().testTag("total-trend-loading-placeholder"),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(when (presentation.phase) {
            TrendReadPhase.FAILED -> "历史未能读取，请重试"
            TrendReadPhase.WAITING -> "成本趋势尚未读取"
            TrendReadPhase.READY -> "尚无可绘制的记录历史"
            else -> "正在读取完整历史"
        }, Modifier.testTag("total-trend-pending-message"),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TrendCutoff(presentation.cutoff)
    }
}

@Composable
private fun TrendCutoff(cutoff: LocalDate?) {
    Text(cutoff?.let { "截至 $it" } ?: "截止日期待读取", Modifier.testTag("total-trend-cutoff"),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Measured ranges remain fully readable in the explanation. */
@Composable
private fun TrendAxisUnit() {
    Text("纵轴 · 元/天", Modifier.testTag("total-trend-axis-unit"),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrendAxisScale(projection: ChartGeometry) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TrendAxisUnit()
        Text("${moneyMetricDisplay(projection.minimum, AmountUnit.YUAN)} 至 ${moneyMetricDisplay(projection.maximum, AmountUnit.YUAN)}",
            Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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
