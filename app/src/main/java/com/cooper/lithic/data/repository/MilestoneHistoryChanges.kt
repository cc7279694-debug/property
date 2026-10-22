package com.cooper.lithic.data.repository

import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.time.LocalDate

/** Only changes to elapsed-time facts may advance a milestone suppression baseline. */
internal object MilestoneHistoryChanges {
    fun project(
        oldStartedOn: LocalDate,
        oldEndedOn: LocalDate?,
        startedOn: LocalDate,
        endedOn: LocalDate?,
        today: LocalDate,
    ): Boolean = oldStartedOn != startedOn || (oldEndedOn != endedOn &&
        (oldEndedOn?.let { !it.isAfter(today) } == true || endedOn?.isBefore(today) == true))

    fun renewal(before: Renewal?, after: Renewal?, today: LocalDate): Boolean =
        pastCoverage(before, today) != pastCoverage(after, today)

    fun state(before: SubscriptionStateChange?, after: SubscriptionStateChange?, today: LocalDate): Boolean =
        before != after && if (before == null) after != null && after.effectiveOn.isBefore(today)
        else listOfNotNull(before, after).any { !it.effectiveOn.isAfter(today) }

    /** Classification of changed past facts only, not another elapsed-day formula.
     * Today is the exclusive past boundary; future-only extensions remain ordinary. */
    private fun pastCoverage(renewal: Renewal?, today: LocalDate): Triple<LocalDate, LocalDate, LocalDate>? =
        renewal?.takeIf { !it.paidOn.isAfter(today) && it.effectiveFrom.isBefore(today) }?.let {
            Triple(it.paidOn, it.effectiveFrom, minOf(it.paidUntilExclusive, today))
        }
}
