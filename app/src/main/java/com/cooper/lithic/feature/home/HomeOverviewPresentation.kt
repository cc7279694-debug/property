package com.cooper.lithic.feature.home

import com.cooper.lithic.domain.report.OverviewResult

/** Presentation of the supplied cost snapshot, not a second cost or record-count calculation. */
internal enum class HomeOverviewStatus { UNAVAILABLE, EMPTY, NO_CURRENT, CURRENT }

internal fun homeOverviewStatus(overview: OverviewResult?): HomeOverviewStatus = when {
    overview == null -> HomeOverviewStatus.UNAVAILABLE
    !overview.hasRecords -> HomeOverviewStatus.EMPTY
    overview.activeProjectCount == 0L && overview.activeSubscriptionCount == 0L -> HomeOverviewStatus.NO_CURRENT
    else -> HomeOverviewStatus.CURRENT
}
