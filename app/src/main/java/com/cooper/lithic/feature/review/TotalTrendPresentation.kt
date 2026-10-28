package com.cooper.lithic.feature.review

import com.cooper.lithic.domain.report.CostTimelinePoint
import java.time.LocalDate

internal enum class TrendReadPhase { WAITING, READING, READY, REFRESHING, FAILED }

/** Frame-local presentation only. Generation isolation remains owned by the existing VM scope. */
internal data class TotalTrendPresentation(
    val phase: TrendReadPhase,
    val hasCompleteSnapshot: Boolean,
    val hasRecords: Boolean,
    val selected: CostTimelinePoint?,
    val cutoff: LocalDate?,
    val showEmpty: Boolean,
)

internal fun totalTrendPresentation(state: TotalCostTrendUiState): TotalTrendPresentation {
    val complete = state.snapshot != null && state.timeline != null
    val hasRecords = complete && state.snapshot!!.overview.let {
        it.projects.isNotEmpty() || it.subscriptions.isNotEmpty()
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
        showEmpty = complete && !hasRecords && !state.loading && state.error == null && state.notice == null,
    )
}
