package com.cooper.lithic.domain.subscription

import com.cooper.lithic.core.time.BusinessClock

object SubscriptionIntervalRules {
    /** Adjacency and gaps are valid; all coverage, including prepaid future periods, must be disjoint. */
    fun validateStructure(input: SubscriptionSnapshot) {
        val sorted = input.renewals.sortedBy { it.effectiveFrom }
        for (index in 1 until sorted.size) {
            require(!sorted[index].effectiveFrom.isBefore(sorted[index - 1].paidUntilExclusive)) {
                "Paid coverage overlaps another renewal"
            }
        }
        require(input.stateChanges.map { it.effectiveOn }.distinct().size == input.stateChanges.size) {
            "Only one state boundary per service per day is allowed; replace the old entry to correct it"
        }
    }

    /** Write-time validation is intentionally separate from historical observation. */
    fun validateForWrite(input: SubscriptionSnapshot, clock: BusinessClock) {
        validateStructure(input)
        val today = clock.today()
        require(input.renewals.none { it.paidOn.isAfter(today) }) { "Payment cannot be in the business future" }
    }

    /** The caller selects the real paid period; this does not cancel provider auto-renewal. */
    fun stopAtPaidBoundary(renewal: Renewal) =
        SubscriptionStateChange(renewal.paidUntilExclusive, SubscriptionOperationState.STOPPED)
}
