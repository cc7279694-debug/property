package com.cooper.lithic.domain.subscription

import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

object SubscriptionCostEngine {
    /** Pure observation: later real payments are unknown on asOf, not invalid because asOf is in the past. */
    fun evaluate(input: SubscriptionSnapshot, asOf: LocalDate): SubscriptionCostResult {
        SubscriptionIntervalRules.validateStructure(input)
        val paid = input.renewals.filter { !it.paidOn.isAfter(asOf) }.sortedBy { it.effectiveFrom }
        val changes = input.stateChanges.sortedBy { it.effectiveOn }
        var paidCents = 0L
        var standardDays = 0L
        for (renewal in paid) {
            paidCents = Math.addExact(paidCents, renewal.amount.cents)
            standardDays = Math.addExact(standardDays, renewal.standardDays.toLong())
        }

        val operationState = changes.lastOrNull { !it.effectiveOn.isAfter(asOf) }?.state
            ?: SubscriptionOperationState.ENABLED
        val covering = paid.firstOrNull { !asOf.isBefore(it.effectiveFrom) && asOf.isBefore(it.paidUntilExclusive) }
        val displayStatus = when (operationState) {
            SubscriptionOperationState.PAUSED -> SubscriptionDisplayStatus.PAUSED
            SubscriptionOperationState.STOPPED -> SubscriptionDisplayStatus.STOPPED
            SubscriptionOperationState.ENABLED -> if (covering != null) SubscriptionDisplayStatus.ACTIVE else SubscriptionDisplayStatus.EXPIRED
        }
        val current = covering.takeIf { displayStatus == SubscriptionDisplayStatus.ACTIVE }
        return SubscriptionCostResult(
            displayStatus = displayStatus,
            currentRate = current?.let { rate(it.amount.cents, it.standardDays.toLong()) } ?: BigDecimal.ZERO,
            currentPrice = current?.amount,
            confirmedPaidCents = paidCents,
            standardPurchasedDays = standardDays,
            historicalAverage = if (standardDays > 0) rate(paidCents, standardDays) else null,
            elapsedPaidDays = elapsedPaidDays(paid, changes, asOf),
            historicalUnavailableReason = if (standardDays == 0L) SubscriptionCostUnavailableReason.NO_CONFIRMED_PAYMENTS else null,
        )
    }

    private fun rate(cents: Long, days: Long): BigDecimal =
        BigDecimal.valueOf(cents, 2).divide(BigDecimal.valueOf(days), MathContext.DECIMAL128)

    private data class Interval(val from: Long, val until: Long)

    private fun elapsedPaidDays(paid: List<Renewal>, changes: List<SubscriptionStateChange>, asOf: LocalDate): Long {
        // epochDay + 1 remains representable even when LocalDate.plusDays(1) would overflow at MAX.
        val cutoff = Math.addExact(asOf.toEpochDay(), 1L)
        val coverage = paid.mapNotNull {
            val from = it.effectiveFrom.toEpochDay()
            val until = minOf(it.paidUntilExclusive.toEpochDay(), cutoff)
            if (from < until) Interval(from, until) else null
        }
        val enabled = mutableListOf<Interval>()
        var cursor = LocalDate.MIN.toEpochDay()
        var state = SubscriptionOperationState.ENABLED
        for (change in changes) {
            val boundary = change.effectiveOn.toEpochDay()
            if (boundary >= cutoff) break
            if (state == SubscriptionOperationState.ENABLED && cursor < boundary) {
                enabled += Interval(cursor, boundary)
            }
            cursor = boundary
            state = change.state
        }
        if (state == SubscriptionOperationState.ENABLED && cursor < cutoff) enabled += Interval(cursor, cutoff)

        // Intersect sorted coverage with ENABLED segments, then count their union without enumerating dates.
        var coverageIndex = 0
        var stateIndex = 0
        var unionFrom: Long? = null
        var unionUntil = 0L
        var total = 0L
        while (coverageIndex < coverage.size && stateIndex < enabled.size) {
            val cover = coverage[coverageIndex]
            val active = enabled[stateIndex]
            val from = maxOf(cover.from, active.from)
            val until = minOf(cover.until, active.until)
            if (from < until) {
                val previousFrom = unionFrom
                if (previousFrom == null) {
                    unionFrom = from
                    unionUntil = until
                } else if (from <= unionUntil) {
                    unionUntil = maxOf(unionUntil, until)
                } else {
                    total = Math.addExact(total, Math.subtractExact(unionUntil, previousFrom))
                    unionFrom = from
                    unionUntil = until
                }
            }
            if (cover.until <= active.until) coverageIndex++ else stateIndex++
        }
        return unionFrom?.let { Math.addExact(total, Math.subtractExact(unionUntil, it)) } ?: total
    }
}
