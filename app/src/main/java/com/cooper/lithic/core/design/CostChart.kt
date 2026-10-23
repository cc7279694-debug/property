package com.cooper.lithic.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.time.LocalDate

/** Read-only presentation of one stored fact or explicitly named effective boundary. */
data class ChartEvent(
    val id: String,
    val date: LocalDate,
    val title: String,
    val detail: String,
    val amountCents: Long? = null,
    val nonCash: Boolean = false,
)

/** Display coordinates only. A gesture selects a calendar date, never an interpolated amount. */
@Composable
internal fun CostChart(
    points: List<CostChartPoint>,
    selected: CostChartPoint?,
    onSelectDate: (LocalDate) -> Unit,
    step: Boolean,
    eventDates: Set<LocalDate>,
    modifier: Modifier = Modifier,
    requestedDate: LocalDate? = selected?.date,
) {
    val ordered = remember(points) { points.sortedBy { it.date } }
    val selectedInRange = selected?.takeIf { point ->
        ordered.isNotEmpty() && point.date >= ordered.first().date && point.date <= ordered.last().date
    }
    val baseProjection = remember(ordered) { ChartGeometry.forPoints(ordered) }
    val projection = remember(baseProjection, selectedInRange?.value) {
        if (baseProjection != null && (selectedInRange == null ||
                selectedInRange.value >= baseProjection.minimum && selectedInRange.value <= baseProjection.maximum)) baseProjection
        else ChartGeometry.forPoints(ordered + listOfNotNull(selectedInRange))
    }
    if (projection == null) {
        Text("没有可绘制的历史日期；不会以0代替未开始或缺失数据。", modifier)
        return
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val selectedColor = MaterialTheme.colorScheme.onSurface
    val currentSelect by rememberUpdatedState(onSelectDate)
    val focusDate = requestedDate ?: selectedInRange?.date ?: ordered.first().date
    val coordinates = remember(ordered, projection) {
        ordered.map { Offset(projection.x(it.date), projection.y(it.value)) }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (ordered.all { it.value.signum() == 0 }) "全部显示点为¥0.00元/天；零线位于图中间。"
            else "纵轴：${AmountFormatter.formatDailyRate(projection.minimum)} 至 ${AmountFormatter.formatDailyRate(projection.maximum)} 元/天",
            style = MaterialTheme.typography.bodySmall)
        Canvas(Modifier.fillMaxWidth().height(220.dp).testTag("cost-chart-canvas")
            .semantics {
                contentDescription = if (step) "订阅标准日均价格阶梯图。暂停、到期和停止的零值保留。可在下方输入日期或查看显示点。"
                    else "项目日均成本历史图。负值位于零线下方。可在下方输入日期或查看显示点。"
                stateDescription = selectedInRange?.takeIf { it.date == focusDate }?.let { "${it.date}，${AmountFormatter.formatDailyRate(it.value)} 元/天" }
                    ?: requestedDate?.let { "$it，尚未取得该日期的图中精确结果" } ?: "尚未选择日期"
                customActions = listOf(
                    CustomAccessibilityAction("前一天") {
                        if (focusDate > projection.start && focusDate <= projection.end) {
                            currentSelect(projection.adjacentDay(focusDate, -1)); true
                        } else false
                    },
                    CustomAccessibilityAction("后一天") {
                        if (focusDate >= projection.start && focusDate < projection.end) {
                            currentSelect(projection.adjacentDay(focusDate, 1)); true
                        } else false
                    },
                )
            }
            .chartDateSelection(projection) { currentSelect(it) }) {
            val inset = 12.dp.toPx()
            val width = (size.width - inset * 2).coerceAtLeast(1f)
            val height = (size.height - inset * 2).coerceAtLeast(1f)
            fun position(point: CostChartPoint) = Offset(inset + projection.x(point.date) * width,
                inset + projection.y(point.value) * height)
            fun position(index: Int) = Offset(inset + coordinates[index].x * width, inset + coordinates[index].y * height)
            val zeroY = inset + projection.zero * height
            drawLine(axisColor, Offset(inset, zeroY), Offset(inset + width, zeroY),
                strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx())))
            val path = Path()
            ordered.forEachIndexed { index, point ->
                val coordinate = position(index)
                if (index == 0) path.moveTo(coordinate.x, coordinate.y)
                else {
                    if (step) path.lineTo(coordinate.x, position(index - 1).y)
                    path.lineTo(coordinate.x, coordinate.y)
                }
            }
            drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx()))
            ordered.forEachIndexed { index, point ->
                if (ordered.size == 1 || point.date in eventDates) drawCircle(lineColor, 3.dp.toPx(), position(index))
            }
            selectedInRange?.let { drawCircle(selectedColor, 5.dp.toPx(), position(it)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(projection.start.toString(), style = MaterialTheme.typography.bodySmall)
            if (projection.start != projection.end) Text(projection.end.toString(), style = MaterialTheme.typography.bodySmall)
        }
        Text(if (step) "按真实自然日期排列；价格在边界处阶梯变化，不平滑连接零值区间。横向拖动选日，纵向滑动仍可浏览页面。"
            else "按真实自然日期排列；横向拖动、前后日期和输入查询均读取真实日期结果，不从连线估算金额。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Horizontal touch slop keeps vertical scrolling owned by the surrounding page. */
internal fun Modifier.chartDateSelection(projection: ChartGeometry, onSelectDate: (LocalDate) -> Unit): Modifier =
    pointerInput(projection.start, projection.end) {
        detectTapGestures { tap ->
            val inset = 12.dp.toPx()
            onSelectDate(projection.dateAt((tap.x - inset) / (size.width - 2 * inset).coerceAtLeast(1f)))
        }
    }.pointerInput(projection.start, projection.end) {
        detectHorizontalDragGestures { change, _ ->
            val inset = 12.dp.toPx()
            onSelectDate(projection.dateAt((change.position.x - inset) / (size.width - 2 * inset).coerceAtLeast(1f)))
        }
    }

internal data class CostChartAdjustment(val date: LocalDate, val value: BigDecimal?, val heldDays: Long)
