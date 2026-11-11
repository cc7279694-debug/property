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

internal enum class HomeOverviewReadStatus { CURRENT, WAITING, UNCONFIRMED, RETAINED, FAILED }

// Ready.readError also carries recovery warnings. Its producer appends the refresh failure
// sentence after any warning. The short form is the explicit failure in the no-snapshot fixture.
private val overviewRefreshError = Regex("""(?:^| )本地记录暂时无法刷新(?:，当前显示上次读取结果)?。$""")

internal fun homeOverviewReadStatus(
    overview: OverviewResult?,
    hasSnapshotNotice: Boolean,
    readError: String?,
): HomeOverviewReadStatus {
    val refreshFailed = readError?.let(overviewRefreshError::containsMatchIn) == true
    return when {
        refreshFailed && overview == null -> HomeOverviewReadStatus.FAILED
        refreshFailed -> HomeOverviewReadStatus.RETAINED
        overview == null -> HomeOverviewReadStatus.WAITING
        hasSnapshotNotice -> HomeOverviewReadStatus.UNCONFIRMED
        else -> HomeOverviewReadStatus.CURRENT
    }
}
