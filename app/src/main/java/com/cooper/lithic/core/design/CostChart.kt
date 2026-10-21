package com.cooper.lithic.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.abs

/** Read-only presentation of one stored fact or explicitly named effective boundary. */
data class ChartEvent(
    val id: String,
    val date: LocalDate,
    val title: String,
    val detail: String,
    val amountCents: Long? = null,
    val nonCash: Boolean = false,
)

/** Display coordinates only. A tap selects an existing date, never an interpolated amount. */
@Composable
internal fun CostChart(
    points: List<CostChartPoint>,
    selected: CostChartPoint?,
    onSelectDate: (LocalDate) -> Unit,
    step: Boolean,
    eventDates: Set<LocalDate>,
    modifier: Modifier = Modifier,
) {
    val ordered = remember(points) { points.sortedBy { it.date } }
    val selectedInRange = selected?.takeIf { point ->
        ordered.isNotEmpty() && point.date >= ordered.first().date && point.date <= ordered.last().date
    }
    val projection = remember(ordered, selectedInRange) {
        ChartGeometry.forPoints(ordered + listOfNotNull(selectedInRange))
    }
    if (projection == null) {
        Text("没有可绘制的历史日期；不会以0代替未开始或缺失数据。", modifier)
        return
    }
    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val selectedColor = MaterialTheme.colorScheme.onSurface
    val currentSelect by rememberUpdatedState(onSelectDate)
    val focusDate = selectedInRange?.date ?: ordered.first().date
    val previousIndex = ordered.indexOfLast { it.date < focusDate }.coerceAtLeast(0)
    val nextIndex = ordered.indexOfFirst { it.date > focusDate }.takeIf { it >= 0 } ?: ordered.lastIndex
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (ordered.all { it.value.signum() == 0 }) "全部显示点为¥0.00元/天；零线位于图中间。"
            else "纵轴：${AmountFormatter.formatDailyRate(projection.minimum)} 至 ${AmountFormatter.formatDailyRate(projection.maximum)} 元/天",
            style = MaterialTheme.typography.bodySmall)
        Canvas(Modifier.fillMaxWidth().height(220.dp).testTag("cost-chart-canvas")
            .semantics {
                contentDescription = if (step) "订阅标准日均价格阶梯图。暂停、到期和停止的零值保留。可在下方输入日期或查看显示点。"
                    else "项目日均成本历史图。负值位于零线下方。可在下方输入日期或查看显示点。"
                customActions = listOf(
                    CustomAccessibilityAction("上一显示日期") {
                        currentSelect(ordered[previousIndex].date); true
                    },
                    CustomAccessibilityAction("下一显示日期") {
                        currentSelect(ordered[nextIndex].date); true
                    },
                )
            }
            .pointerInput(ordered, projection) {
                detectTapGestures { tap ->
                    val inset = 12.dp.toPx()
                    val width = (size.width - inset * 2f).coerceAtLeast(1f)
                    val fraction = ((tap.x - inset) / width).coerceIn(0f, 1f)
                    ordered.minByOrNull { abs(projection.x(it.date) - fraction) }?.let { currentSelect(it.date) }
                }
            }) {
            val inset = 12.dp.toPx()
            val width = (size.width - inset * 2).coerceAtLeast(1f)
            val height = (size.height - inset * 2).coerceAtLeast(1f)
            fun position(point: CostChartPoint) = Offset(inset + projection.x(point.date) * width,
                inset + projection.y(point.value) * height)
            val zeroY = inset + projection.zero * height
            drawLine(axisColor, Offset(inset, zeroY), Offset(inset + width, zeroY),
                strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx())))
            val path = Path()
            ordered.forEachIndexed { index, point ->
                val coordinate = position(point)
                if (index == 0) path.moveTo(coordinate.x, coordinate.y)
                else {
                    if (step) path.lineTo(coordinate.x, position(ordered[index - 1]).y)
                    path.lineTo(coordinate.x, coordinate.y)
                }
            }
            drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx()))
            ordered.forEach { point ->
                if (ordered.size == 1 || point.date in eventDates) drawCircle(lineColor, 3.dp.toPx(), position(point))
            }
            selectedInRange?.let { drawCircle(selectedColor, 5.dp.toPx(), position(it)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(projection.start.toString(), style = MaterialTheme.typography.bodySmall)
            if (projection.start != projection.end) Text(projection.end.toString(), style = MaterialTheme.typography.bodySmall)
        }
        Text(if (step) "按真实自然日期排列；价格在边界处阶梯变化，不平滑连接零值区间。"
            else "按真实自然日期排列；连线用于观察走势，任意日期的精确值请使用下方日期查询。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal data class CostChartAdjustment(val date: LocalDate, val value: BigDecimal?, val heldDays: Long)
