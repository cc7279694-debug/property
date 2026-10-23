package com.cooper.lithic.feature.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.cooper.lithic.core.design.ChartEvent
import com.cooper.lithic.core.design.CostChartAdjustment
import com.cooper.lithic.core.design.CostChartPage
import com.cooper.lithic.core.design.CostChartPoint
import com.cooper.lithic.domain.report.ProjectTimeline
import com.cooper.lithic.domain.report.ProjectTimelinePoint
import java.time.LocalDate

@Composable
internal fun ProjectCostChart(
    title: String,
    timeline: ProjectTimeline?,
    selected: ProjectTimelinePoint?,
    events: List<ChartEvent>,
    loading: Boolean,
    error: String?,
    missing: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    requestedDate: LocalDate? = selected?.date,
) {
    val points = remember(timeline) {
        timeline?.holdingPoints.orEmpty().mapNotNull { point -> point.cost.dailyRate?.let { CostChartPoint(point.date, it) } }
    }
    val adjustments = remember(timeline) {
        timeline?.postEndAdjustments.orEmpty().map { CostChartAdjustment(it.date, it.cost.dailyRate, it.cost.heldDays) }
    }
    val selectedStatus = selected?.let {
        when {
            it.cost.dailyRate == null -> "尚未开始；没有有效持有天数，不以0代替日均成本。"
            it.cost.contributesToCurrentTotal -> "该日日末：仍在持有；持有${it.cost.heldDays}天，计入当前合计。"
            else -> "该日日末：已结束；持有${it.cost.heldDays}天，不计入当前合计。结束后结果截至所选日期。"
        }
    }
    CostChartPage(title, "project-cost-chart-screen", "项目日均成本历史",
        "按已登记投入与收回事实回看，不估值、不预测。持有期曲线在结束日截止；结束后的结算单独列出。图中的历史日均不等于首页当前贡献。",
        timeline?.asOf, points, selected?.date, selected?.cost?.dailyRate, selectedStatus,
        events, adjustments, timeline?.sampled == true, false, loading, error, missing,
        onSelectDate, onRetry, onBack, modifier, requestedDate)
}
