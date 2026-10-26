package com.cooper.lithic.core.design

import com.cooper.lithic.core.design.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Pure read-only page shared by the two item types. All monetary results come from the caller. */
@Composable
internal fun CostChartPage(
    title: String,
    screenTag: String,
    metricTitle: String,
    explanation: String,
    asOf: LocalDate?,
    points: List<CostChartPoint>,
    selectedDate: LocalDate?,
    selectedValue: BigDecimal?,
    selectedStatus: String?,
    events: List<ChartEvent>,
    adjustments: List<CostChartAdjustment>,
    sampled: Boolean,
    step: Boolean,
    loading: Boolean,
    error: String?,
    missing: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    requestedDate: LocalDate? = selectedDate,
) {
    var dateRaw by rememberSaveable(title) { mutableStateOf("") }
    var dateError by rememberSaveable(title) { mutableStateOf<String?>(null) }
    var pointsExpanded by rememberSaveable(title) { mutableStateOf(false) }
    var pointLimit by rememberSaveable(title) { mutableIntStateOf(50) }
    var eventLimit by rememberSaveable(title) { mutableIntStateOf(50) }
    var eventFilter by rememberSaveable(title) { mutableStateOf<String?>(null) }
    var expandedDay by rememberSaveable(title) { mutableStateOf<String?>(null) }
    val orderedEvents = remember(events) { events.sortedWith(compareByDescending<ChartEvent> { it.date }.thenBy { it.id }) }
    val filteredEvents = remember(orderedEvents, eventFilter) {
        orderedEvents.filter { eventFilter == null || it.date.toString() == eventFilter }
    }
    val visibleEvents = filteredEvents.take(eventLimit)
    val groups = visibleEvents.groupBy { it.date }
    val hasData = asOf != null && !missing
    val selectionFocus = requestedDate ?: selectedDate
    val selectionStart = remember(points) { points.minOfOrNull { it.date } }
    val selectionEnd = remember(points) { points.maxOfOrNull { it.date } }
    fun applyDate() {
        val parsed = try { LocalDate.parse(dateRaw.trim()) } catch (_: DateTimeParseException) { null }
        when {
            parsed == null -> dateError = "请输入有效日期，格式为年-月-日，例如2026-01-20。"
            asOf == null -> dateError = "历史数据尚未可用，请先重试读取。"
            parsed.isAfter(asOf) -> dateError = "不能查看业务今天之后的历史日期。"
            else -> { dateError = null; onSelectDate(parsed) }
        }
    }
    LazyColumn(modifier.fillMaxSize().imePadding().testTag(screenTag), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item("heading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                C2PageHeader(title, onBack, "chart-back", backLabel = "返回详情")
                Text(metricTitle, style = MaterialTheme.typography.titleLarge)
                Text(explanation, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (loading) item("loading") { Text(if (hasData) "正在重新读取；下方保留上次结果。" else "正在读取历史数据。") }
        if (error != null) item("error") {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().testTag("chart-error")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (hasData) "读取失败；下方是上次结果，尚未刷新。" else "读取失败，不能将错误作为空数据。")
                    Text(error)
                    OutlinedButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("chart-retry")) { Text("重试") }
                }
            }
        }
        if (missing) item("missing") { Text("该记录当前不存在，不能继续显示其历史。", Modifier.testTag("chart-missing")) }
        if (hasData) {
            val cutoff = requireNotNull(asOf)
            item("selection") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("结果截至 $asOf", style = MaterialTheme.typography.bodyMedium)
                        if (selectionFocus != null) {
                            Text("所选日期：$selectionFocus", Modifier.testTag("chart-selected-date"), style = MaterialTheme.typography.titleMedium)
                            if (selectedDate == selectionFocus) {
                                AmountText(selectedValue, Modifier.testTag("chart-selected-value"), style = MaterialTheme.typography.headlineMedium)
                                Text(selectedStatus.orEmpty(), Modifier.testTag("chart-selected-status"))
                            } else Text("正在查询所选日期；不使用旧日期金额或0代替结果。")
                        } else Text("选择一个日期，查看冻结引擎返回的精确历史结果。")
                    }
                }
            }
            item("canvas") {
                CostChart(points, if (selectedDate != null && selectedValue != null) CostChartPoint(selectedDate, selectedValue) else null,
                    onSelectDate, step, events.asSequence().filter { it.date <= cutoff }.map { it.date }.toSet(), requestedDate = selectionFocus)
                if (sampled) Text("长跨度按显示点采样；事实边界保留。日期查询与事件清单不受显示点数量限制。",
                    style = MaterialTheme.typography.bodySmall)
            }
            item("date") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectionFocus?.let { date ->
                        OutlinedButton({ onSelectDate(date.minusDays(1)) },
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("chart-previous-day"),
                            enabled = selectionStart != null && selectionEnd != null && date > selectionStart && date <= selectionEnd) { Text("前一天") }
                        OutlinedButton({ onSelectDate(date.plusDays(1)) },
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("chart-next-day"),
                            enabled = selectionStart != null && selectionEnd != null && date >= selectionStart && date < selectionEnd) { Text("后一天") }
                        Text("前后一天限图表日期范围；其他已过去日期可输入查询。", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(dateRaw, { dateRaw = it; dateError = null }, Modifier.fillMaxWidth().testTag("chart-date-input"),
                        label = { Text("查询日期（年-月-日）") }, placeholder = { Text(selectedDate?.toString() ?: asOf.toString()) },
                        isError = dateError != null, singleLine = true,
                        supportingText = { Text(dateError ?: "可输入任意已过去的日期，不必拖动图表。") })
                    Button(::applyDate, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("chart-date-apply")) { Text("查看该日") }
                }
            }
            item("point-heading") {
                OutlinedButton({ pointsExpanded = !pointsExpanded }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("chart-points-toggle")) {
                    Text(if (pointsExpanded) "收起显示点文字清单" else "查看显示点文字清单（${points.size}个）")
                }
            }
            if (pointsExpanded) {
                items(points.take(pointLimit), key = { "point-${it.date}" }) { point ->
                    OutlinedButton({ onSelectDate(point.date) }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("chart-point-${point.date}")) {
                        Text("${point.date}　${AmountFormatter.formatDailyRate(point.value)} 元/天")
                    }
                }
                if (points.size > pointLimit) item("more-points") {
                    TextButton({ pointLimit += 50 }, Modifier.heightIn(min = 48.dp).testTag("chart-points-more")) { Text("再显示50个日期") }
                }
            }
            if (adjustments.isNotEmpty()) {
                item("adjustment-heading") {
                    Text("结束后结果调整", style = MaterialTheme.typography.titleLarge)
                    Text("仅列实际结算或纠错日期，不与持有期曲线连接；持有天数不再增长。")
                }
                items(adjustments, key = { "adjustment-${it.date}" }) { point ->
                    OutlinedButton({ onSelectDate(point.date) }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("chart-adjustment-${point.date}")) {
                        Text("${point.date}　${AmountFormatter.formatDailyRate(point.value)} 元/天；持有${point.heldDays}天")
                    }
                }
            }
            item("events-heading") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("该日相关事件", style = MaterialTheme.typography.titleLarge)
                    Text("同日记录完整列出；没有小时事实时，不推断同日先后顺序。")
                    if (selectedDate != null) OutlinedButton({ eventFilter = selectedDate.toString(); eventLimit = 50; expandedDay = selectedDate.toString() },
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("chart-show-selected-events")) { Text("查看所选日期的全部事件") }
                    if (eventFilter != null) TextButton({ eventFilter = null; eventLimit = 50 },
                        Modifier.heightIn(min = 48.dp).testTag("chart-show-all-events")) { Text("返回全部日期事件") }
                    if (filteredEvents.isEmpty()) Text(if (eventFilter != null) "所选日期没有事件；历史金额仍按领域引擎计算。" else "没有已登记的事件。")
                }
            }
            groups.forEach { (date, dayEvents) ->
                item("event-day-$date") {
                    val total = filteredEvents.count { it.date == date }
                    OutlinedButton({ expandedDay = if (expandedDay == date.toString()) null else date.toString() },
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("chart-event-day-$date")) {
                        Text("$date　${total}条事件${if (expandedDay == date.toString()) "，收起" else "，展开"}")
                    }
                }
                if (expandedDay == date.toString()) {
                    if (date > cutoff) item("future-$date") { Text("尚未生效的计划，不属于历史曲线。") }
                    items(dayEvents, key = { "event-${it.id}" }) { event ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().testTag("chart-event-${event.id}")) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(event.title, style = MaterialTheme.typography.titleMedium)
                                Text(event.detail)
                                event.amountCents?.let { Text(AmountFormatter.formatCny(BigDecimal.valueOf(it, 2))) }
                                if (event.nonCash) Text("非现金折抵（不是现金扣款）")
                                if (date <= cutoff) TextButton({ onSelectDate(date) }, Modifier.heightIn(min = 48.dp)) { Text("查看该日结果") }
                            }
                        }
                    }
                }
            }
            if (filteredEvents.size > eventLimit) item("more-events") {
                OutlinedButton({ eventLimit += 50 }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("chart-events-more")) { Text("再显示50条事件（已显示${visibleEvents.size}/${filteredEvents.size}）") }
            }
        }
    }
}
