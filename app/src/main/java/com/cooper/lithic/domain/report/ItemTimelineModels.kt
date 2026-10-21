package com.cooper.lithic.domain.report

import com.cooper.lithic.core.model.ProjectCostResult
import com.cooper.lithic.domain.subscription.SubscriptionCostResult
import java.time.LocalDate

data class ProjectTimelinePoint(val date: LocalDate, val cost: ProjectCostResult)

data class ProjectTimeline(
    val asOf: LocalDate,
    val holdingPoints: List<ProjectTimelinePoint>,
    /** Actual post-end settlement days, never a continuation of the holding curve. */
    val postEndAdjustments: List<ProjectTimelinePoint>,
    val eventDays: List<LocalDate>,
    /** Limit of the uniform base points only; all fact boundaries are retained additionally. */
    val displayPointLimit: Int,
    val sampled: Boolean,
)

data class SubscriptionTimelinePoint(val date: LocalDate, val cost: SubscriptionCostResult)

data class SubscriptionTimeline(
    val asOf: LocalDate,
    val points: List<SubscriptionTimelinePoint>,
    val eventDays: List<LocalDate>,
    val displayPointLimit: Int,
    val sampled: Boolean,
)

internal fun requireItemDisplayPointLimit(limit: Int) {
    require(limit in 2..1000) { "An item chart needs 2 to 1000 uniform display points" }
}

/** M17 item-chart date selection only, not a persisted or total-cost timeline.
 * The uniform portion is bounded; real boundaries and their adjacent dates are never discarded.
 * Date selection does not enumerate the duration. Frozen evaluation still costs facts × retained points. */
internal fun itemChartDates(
    from: LocalDate,
    until: LocalDate,
    factDates: List<LocalDate>,
    limit: Int,
): List<LocalDate> {
    requireItemDisplayPointLimit(limit)
    if (from.isAfter(until)) return emptyList()
    val first = from.toEpochDay()
    val last = until.toEpochDay()
    val span = Math.subtractExact(last, first)
    val dates = sortedSetOf<Long>()
    if (span < limit.toLong()) {
        for (offset in 0L..span) dates += Math.addExact(first, offset)
    } else {
        for (index in 0 until limit) {
            val offset = Math.multiplyExact(span, index.toLong()) / (limit - 1L)
            dates += Math.addExact(first, offset)
        }
    }
    // Epoch arithmetic remains safe at LocalDate.MIN/MAX; convert only after range filtering.
    for (boundary in factDates.asSequence().map(LocalDate::toEpochDay) + sequenceOf(first, last)) {
        for (offset in -1L..1L) {
            val date = Math.addExact(boundary, offset)
            if (date in first..last) dates += date
        }
    }
    return dates.map(LocalDate::ofEpochDay)
}
