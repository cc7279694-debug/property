package com.cooper.lithic.feature.review

import com.cooper.lithic.domain.report.CostTimeline
import com.cooper.lithic.domain.report.TotalCostEvent
import com.cooper.lithic.domain.report.CostTimelinePoint
import java.time.LocalDate

internal enum class TrendReadPhase { WAITING, READING, READY, REFRESHING, FAILED }

internal data class TrendRecordCounts(val projects: Long, val subscriptions: Long)

/** Frame-local presentation only. Generation isolation remains owned by the existing VM scope. */
internal data class TotalTrendPresentation(
    val phase: TrendReadPhase,
    val hasCompleteSnapshot: Boolean,
    val hasRecords: Boolean,
    val selected: CostTimelinePoint?,
    val cutoff: LocalDate?,
    val showEmpty: Boolean,
    val recordCountsMismatch: Boolean,
)

internal fun totalTrendPresentation(state: TotalCostTrendUiState, expectedCounts: TrendRecordCounts? = null): TotalTrendPresentation {
    val complete = state.snapshot != null && state.timeline != null
    val hasRecords = complete && state.snapshot!!.overview.let {
        it.projects.isNotEmpty() || it.subscriptions.isNotEmpty()
    }
    val countsMismatch = complete && expectedCounts != null && state.snapshot!!.overview.let {
        expectedCounts.projects != it.projects.size.toLong() || expectedCounts.subscriptions != it.subscriptions.size.toLong()
    }
    return TotalTrendPresentation(
        phase = when {
            state.error != null -> TrendReadPhase.FAILED
            complete && state.loading -> TrendReadPhase.REFRESHING
            complete -> TrendReadPhase.READY
            state.loading -> TrendReadPhase.READING
            else -> TrendReadPhase.WAITING
        },
        hasCompleteSnapshot = complete,
        hasRecords = hasRecords,
        selected = state.selectedPoint?.takeIf { hasRecords && it.date == state.selectedDate },
        cutoff = state.timeline?.asOf?.takeIf { complete },
        showEmpty = complete && !hasRecords && !countsMismatch && !state.loading && state.error == null && state.notice == null,
        recordCountsMismatch = countsMismatch,
    )
}

/** A pending date has unknown events; it does not revoke the already captured full history. */
internal fun trendEventsForDisplay(state: TotalCostTrendUiState, allDates: Boolean): List<TotalCostEvent>? {
    val presentation = totalTrendPresentation(state)
    if (!presentation.hasRecords) return null
    return if (allDates) state.timeline!!.events else state.events.takeIf { presentation.selected != null }
}

internal val trendEventOrder = compareByDescending<TotalCostEvent> { it.date }.thenBy { it.id }

/** Retains the first three events in archive order without sorting the full timeline. */
internal fun trendRecentEvents(events: List<TotalCostEvent>): List<TotalCostEvent> {
    val recent = ArrayList<TotalCostEvent>(3)
    for (event in events) {
        val index = recent.indexOfFirst { trendEventOrder.compare(event, it) < 0 }
        if (index >= 0) recent.add(index, event)
        else if (recent.size < 3) recent.add(event)
        if (recent.size > 3) recent.removeAt(3)
    }
    return recent
}

/** Retained display points and recorded events do not determine the full observed range. */
internal fun trendHasSingleDate(timeline: CostTimeline): Boolean =
    timeline.evaluatedDayCount == 1L && timeline.from == timeline.asOf

internal fun trendSelectionMeaning(point: CostTimelinePoint): String = when {
    !point.hasRecords -> "该日尚未有已录入记录生效；不是当年没有花钱的证明。"
    !point.hasActiveContributions -> "该日没有有效成本贡献；项目结束日退出，暂停、到期、停止或无付费覆盖的订阅贡献为0。"
    point.isCancellationZero -> "该日正负贡献相互抵消为0；两类小计和真实负值均保留。"
    point.totalRate.signum() == 0 -> "该日存在有效记录，按已录入事实计算的真实成本为0。"
    else -> "该日有效贡献：${point.activeProjectCount}个长期项目、${point.activeSubscriptionCount}个订阅。已结束项目后续结算只作相关事件，不重新加入总额。"
}
