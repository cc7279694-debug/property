package com.cooper.lithic.feature.subscription

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.cooper.lithic.core.design.ChartEvent
import com.cooper.lithic.core.design.CostChartPage
import com.cooper.lithic.core.design.CostChartPoint
import com.cooper.lithic.domain.report.SubscriptionTimeline
import com.cooper.lithic.domain.report.SubscriptionTimelinePoint
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import java.time.LocalDate

@Composable
internal fun SubscriptionPriceChart(
    title: String,
    timeline: SubscriptionTimeline?,
    selected: SubscriptionTimelinePoint?,
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
    val points = remember(timeline) { timeline?.points.orEmpty().map { CostChartPoint(it.date, it.cost.currentRate) } }
    val selectedStatus = selected?.let {
        when (it.cost.displayStatus) {
            SubscriptionDisplayStatus.ACTIVE -> "该日日末：订阅中；使用当时有效套餐的标准日均价。"
            SubscriptionDisplayStatus.PAUSED -> "该日日末：暂停；当前成本贡献为0，不代表免费套餐。"
            SubscriptionDisplayStatus.EXPIRED -> "该日日末：到期或没有有效覆盖；当前成本贡献为0，不代表免费套餐。"
            SubscriptionDisplayStatus.STOPPED -> "该日日末：停止；当前成本贡献为0，不代表免费套餐。"
        }
    }
    CostChartPage(title, "subscription-price-chart-screen", "标准日均价格历史",
        "展示当日有效套餐的标准日均价，月30天、季90天、年365天。不是历史平均日成本，也不是每天发生扣款；预付未来套餐不提前生效。",
        timeline?.asOf, points, selected?.date, selected?.cost?.currentRate, selectedStatus,
        events, emptyList(), timeline?.sampled == true, true, loading, error, missing,
        onSelectDate, onRetry, onBack, modifier, requestedDate)
}
