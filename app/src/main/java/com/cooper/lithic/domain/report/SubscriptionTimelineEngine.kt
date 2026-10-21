package com.cooper.lithic.domain.report

import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object SubscriptionTimelineEngine {
    /** Retain payment, service coverage and state boundaries; effective dates are not payment dates. */
    fun build(input: SubscriptionSnapshot, asOf: LocalDate, maxDisplayPoints: Int = 240): SubscriptionTimeline {
        requireItemDisplayPointLimit(maxDisplayPoints)
        // Also performs the frozen structural validation when no historical points are available.
        val atCutoff = point(input, asOf, asOf)
        val knownPayments = input.renewals.filter { !it.paidOn.isAfter(asOf) }
        val eventDays = (knownPayments.asSequence().flatMap {
            sequenceOf(it.paidOn, it.effectiveFrom, it.paidUntilExclusive)
        } + input.stateChanges.asSequence().map { it.effectiveOn })
            .filter { !it.isAfter(asOf) }.distinct().sorted().toList()
        val first = eventDays.firstOrNull()
            ?: return SubscriptionTimeline(asOf, emptyList(), emptyList(), maxDisplayPoints, sampled = false)
        val points = itemChartDates(first, asOf, eventDays, maxDisplayPoints).map { day ->
            if (day == asOf) atCutoff else point(input, asOf, day)
        }
        return SubscriptionTimeline(
            asOf = asOf,
            points = points,
            eventDays = eventDays,
            displayPointLimit = maxDisplayPoints,
            sampled = ChronoUnit.DAYS.between(first, asOf) >= maxDisplayPoints.toLong(),
        )
    }

    /** Do not derive selected money from screen pixels or interpolate between two observations. */
    fun point(input: SubscriptionSnapshot, asOf: LocalDate, date: LocalDate): SubscriptionTimelinePoint {
        require(!date.isAfter(asOf)) { "An item history cannot select a future date" }
        return SubscriptionTimelinePoint(date, SubscriptionCostEngine.evaluate(input, date))
    }
}
