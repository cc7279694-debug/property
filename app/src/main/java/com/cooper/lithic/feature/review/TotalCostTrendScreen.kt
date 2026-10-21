package com.cooper.lithic.feature.review

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountFormatter
import com.cooper.lithic.core.design.AmountText
import com.cooper.lithic.core.design.ChartGeometry
import com.cooper.lithic.core.design.CostChartPoint
import com.cooper.lithic.domain.report.CostTimelinePoint
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.TotalCostEvent
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlin.math.abs

/** Read-only view: all amounts, status counts and event identities are supplied by the snapshot. */
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
    val timeline = state.timeline
    val snapshot = state.snapshot
    val hasRecords = snapshot?.overview?.let { it.projects.isNotEmpty() || it.subscriptions.isNotEmpty() } == true
    val selected = state.selectedPoint?.takeIf { it.date == state.selectedDate }
    val events = if (allEvents) timeline?.events.orEmpty() else state.events
    val orderedEvents = remember(events) { events.sortedWith(compareByDescending<TotalCostEvent> { it.date }.thenBy { it.id }) }
    fun applyDate() {
        val parsed = try { LocalDate.parse(dateRaw.trim()) } catch (_: DateTimeParseException) { null }
        when {
            parsed == null -> dateError = "请输入有效日期，例如2026-01-20。"
            timeline == null -> dateError = "历史尚未读取成功，请先重试。"
            parsed > timeline.asOf -> dateError = "不能查看业务今天之后的历史日期。"
            else -> { dateError = null; allEvents = false; eventLimit = 50; onSelectDate(parsed) }
        }
    }
    LazyColumn(modifier.fillMaxSize().imePadding().testTag("total-cost-trend-screen"),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item("heading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onBack, Modifier.heightIn(min = 48.dp).testTag("total-trend-back")) { Text("返回回看") }
                Text("成本趋势", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                Text("总日均成本", style = MaterialTheme.typography.titleLarge)
                Text("按现有已录入、已纠正事实重建的历史参考指标；不是当天实际扣款，也不代表完整生活支出或永远不变的历史账本快照。",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (state.loading) item("loading") { Text(if (timeline == null) "正在读取完整历史。" else "正在刷新；下方为上次结果，尚未刷新。") }
        state.error?.let { error -> item("error") {
            Card(Modifier.fillMaxWidth().testTag("total-trend-error")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (timeline == null) "读取失败，不能将错误作为空账本。" else "读取失败；下方保留上次结果，尚未刷新。")
                    Text(error)
                    OutlinedButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("total-trend-retry")) { Text("重试") }
                }
            }
        } }
        state.notice?.let { notice -> item("notice") {
            Text(notice, Modifier.testTag("total-trend-notice"), style = MaterialTheme.typography.bodyMedium)
        } }
        if (timeline != null && !hasRecords) item("empty") {
            Text("暂无记录。尚未录入相关记录，不能据此推断过去没有花钱。", Modifier.testTag("total-trend-empty"))
        }
        if (timeline != null && hasRecords) {
            item("selected") {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("结果截至 ${timeline.asOf}", style = MaterialTheme.typography.bodyMedium)
                        state.selectedDate?.let { Text("所选日期：$it", Modifier.testTag("total-trend-selected-date"),
                            style = MaterialTheme.typography.titleMedium) }
                        if (selected == null) Text("正在计算所选日期；不会用旧日期金额或0代替结果。")
                        else {
                            DailyValue("总日均成本", selected.totalRate, "total-trend-total")
                            DailyValue("长期项目贡献", selected.projectRate, "total-trend-project")
                            DailyValue("订阅贡献", selected.subscriptionRate, "total-trend-subscription")
                            Text(selectionMeaning(selected), Modifier.testTag("total-trend-selected-status"),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item("chart") {
                TotalCostChart(timeline.points, selected, state.showBreakdown, onSelectDate)
                Text("完整记录历史至截止日；长跨度图表精简显示，事件和重要变化保留。任意日期可查询精确值，全部事件都可继续查看。",
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onToggleBreakdown, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("total-trend-breakdown")) {
                    Text(if (state.showBreakdown) "收起两类贡献" else "展开长期项目与订阅贡献")
                }
            }
            item("date") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(dateRaw, { dateRaw = it; dateError = null }, Modifier.fillMaxWidth().testTag("total-trend-date-input"),
                        label = { Text("查询日期（年-月-日）") }, placeholder = { Text(state.selectedDate?.toString() ?: timeline.asOf.toString()) },
                        singleLine = true, isError = dateError != null,
                        supportingText = { Text(dateError ?: "可查询任意已过去的日期，不必拖动图表。") })
                    Button(::applyDate, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("total-trend-date-apply")) { Text("查看该日") }
                }
            }
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
                            Text("总额 ${AmountFormatter.formatDailyRate(point.totalRate)} 元/天")
                            Text("项目 ${AmountFormatter.formatDailyRate(point.projectRate)}；订阅 ${AmountFormatter.formatDailyRate(point.subscriptionRate)} 元/天")
                        }
                    }
                }
                if (timeline.points.size > pointLimit) item("more-points") {
                    TextButton({ pointLimit += 50 }, Modifier.heightIn(min = 48.dp).testTag("total-trend-points-more")) { Text("再显示50个日期") }
                }
            }
            item("event-heading") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("该日相关事件", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    Text("同日事件共同列出，不把某一笔说成全部跳变的唯一原因。付款、覆盖边界和状态变化是不同事实；有事件也可能没有总额变化。")
                    OutlinedButton({ allEvents = !allEvents; eventLimit = 50 },
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("total-trend-events-scope")) {
                        Text(if (allEvents) "只看所选日期事件" else "查看全部日期事件")
                    }
                    Text(if (allEvents) "全部已记录历史事件：${orderedEvents.size}条" else "所选日期相关事件：${orderedEvents.size}条",
                        Modifier.testTag("total-trend-event-count"))
                    if (orderedEvents.isEmpty()) Text("该日没有相关事件；历史成本仍按已录入事实计算，不推断未录入的支出。")
                }
            }
            items(orderedEvents.take(eventLimit), key = { "event-${it.id}" }) { event ->
                Card(Modifier.fillMaxWidth().testTag("total-trend-event-${event.id}")) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${event.date} · ${event.title}", style = MaterialTheme.typography.titleMedium)
                        Text("${if (event.kind == OverviewKind.PROJECT) "长期项目" else "订阅"}：${event.recordName}")
                        Text(event.description)
                        event.amountCents?.let { Text(AmountFormatter.formatCny(BigDecimal.valueOf(it, 2))) }
                        if (event.nonCash) Text("非现金折抵（不是现金扣款）")
                        TextButton({ onOpenRecord(event.kind, event.recordId) },
                            Modifier.heightIn(min = 48.dp).testTag("total-trend-open-${event.id}")) { Text("查看所属记录") }
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

@Composable
private fun DailyValue(label: String, value: BigDecimal, tag: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        AmountText(value, Modifier.testTag(tag), style = if (tag == "total-trend-total") MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge)
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
@Composable
private fun TotalCostChart(points: List<CostTimelinePoint>, selected: CostTimelinePoint?, breakdown: Boolean,
    onSelectDate: (LocalDate) -> Unit) {
    if (points.isEmpty()) { Text("截至本次业务日尚无可绘制的记录历史。", Modifier.testTag("total-trend-no-history")); return }
    val ordered = remember(points) { points.sortedBy { it.date } }
    val inRange = selected?.takeIf { it.date >= ordered.first().date && it.date <= ordered.last().date }
    val projection = remember(ordered, inRange, breakdown) {
        ChartGeometry.forPoints((ordered + listOfNotNull(inRange)).flatMap { p ->
            if (breakdown) listOf(CostChartPoint(p.date, p.totalRate), CostChartPoint(p.date, p.projectRate), CostChartPoint(p.date, p.subscriptionRate))
            else listOf(CostChartPoint(p.date, p.totalRate))
        })!!
    }
    val totalColor = MaterialTheme.colorScheme.primary
    val projectColor = MaterialTheme.colorScheme.tertiary
    val subscriptionColor = MaterialTheme.colorScheme.secondary
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val selectedColor = MaterialTheme.colorScheme.onSurface
    val currentSelect by rememberUpdatedState(onSelectDate)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("总日均成本 — 实线", style = MaterialTheme.typography.labelLarge, color = totalColor)
        if (breakdown) {
            Text("长期项目 ━ ━ 长虚线；订阅 ··· 短虚线阶梯", style = MaterialTheme.typography.bodySmall)
            Text("三条序列共用日期轴与数值尺度，不使用百分比堆叠。", style = MaterialTheme.typography.bodySmall)
        }
        Text("纵轴：${AmountFormatter.formatDailyRate(projection.minimum)} 至 ${AmountFormatter.formatDailyRate(projection.maximum)} 元/天",
            Modifier.testTag("total-trend-axis"), style = MaterialTheme.typography.bodySmall)
        Canvas(Modifier.fillMaxWidth().height(240.dp).testTag("total-trend-canvas")
            .semantics {
                contentDescription = if (breakdown) "总日均成本实线、长期项目长虚线、订阅短虚线阶梯，共同日期和金额尺度。负值保留。下方可输入日期和查看文字清单。"
                    else "总日均成本历史实线，负值保留。可展开两类贡献，或在下方输入日期和查看文字清单。"
                val focus = inRange?.date ?: ordered.first().date
                customActions = listOf(CustomAccessibilityAction("上一显示日期") {
                    currentSelect(ordered.lastOrNull { it.date < focus }?.date ?: ordered.first().date); true
                }, CustomAccessibilityAction("下一显示日期") {
                    currentSelect(ordered.firstOrNull { it.date > focus }?.date ?: ordered.last().date); true
                })
            }
            .pointerInput(ordered, projection) { detectTapGestures { tap ->
                val inset = 12.dp.toPx()
                val fraction = ((tap.x - inset) / (size.width - 2f * inset).coerceAtLeast(1f)).coerceIn(0f, 1f)
                ordered.minByOrNull { abs(projection.x(it.date) - fraction) }?.let { currentSelect(it.date) }
            } }) {
            val inset = 12.dp.toPx()
            val w = (size.width - 2 * inset).coerceAtLeast(1f)
            val h = (size.height - 2 * inset).coerceAtLeast(1f)
            fun pos(date: LocalDate, value: BigDecimal) = Offset(inset + projection.x(date) * w, inset + projection.y(value) * h)
            val zero = inset + projection.zero * h
            drawLine(axisColor, Offset(inset, zero), Offset(inset + w, zero), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
            fun series(color: Color, step: Boolean, dash: FloatArray?, value: (CostTimelinePoint) -> BigDecimal) {
                val path = Path()
                ordered.forEachIndexed { index, p ->
                    val coordinate = pos(p.date, value(p))
                    if (index == 0) path.moveTo(coordinate.x, coordinate.y)
                    else {
                        if (step) path.lineTo(coordinate.x, pos(p.date, value(ordered[index - 1])).y)
                        path.lineTo(coordinate.x, coordinate.y)
                    }
                }
                drawPath(path, color, style = Stroke(2.dp.toPx(), pathEffect = dash?.let { PathEffect.dashPathEffect(it) }))
                if (ordered.size == 1) drawCircle(color, 3.dp.toPx(), pos(ordered[0].date, value(ordered[0])))
                inRange?.let { drawCircle(color, 4.dp.toPx(), pos(it.date, value(it))) }
            }
            if (breakdown) {
                series(projectColor, false, floatArrayOf(10.dp.toPx(), 5.dp.toPx())) { it.projectRate }
                series(subscriptionColor, true, floatArrayOf(3.dp.toPx(), 4.dp.toPx())) { it.subscriptionRate }
            }
            series(totalColor, false, null) { it.totalRate }
            inRange?.let { p -> val x = pos(p.date, p.totalRate).x
                drawLine(selectedColor, Offset(x, inset), Offset(x, inset + h), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(projection.start.toString(), style = MaterialTheme.typography.bodySmall)
            if (projection.start != projection.end) Text(projection.end.toString(), style = MaterialTheme.typography.bodySmall)
        }
        Text("按真实自然日期间距绘制，不做平滑预测；订阅零区间保留阶梯。点击只选择真实显示日期，任意日期精确值请用日期查询。",
            style = MaterialTheme.typography.bodySmall)
    }
}
