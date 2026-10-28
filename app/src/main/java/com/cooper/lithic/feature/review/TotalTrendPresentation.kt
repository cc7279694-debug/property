package com.cooper.lithic.feature.review

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
