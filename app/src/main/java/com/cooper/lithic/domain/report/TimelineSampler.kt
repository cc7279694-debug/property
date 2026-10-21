package com.cooper.lithic.domain.report

import java.time.LocalDate
import java.util.TreeMap

object TimelineSampler {
    /**
     * Full inclusive day scan on the caller's background dispatcher. At most floor(budget/6)
     * buckets retain min/max for each of the three sequences. Real fact dates and adjacent
     * days are additional, never silently truncated to fit the display budget.
     *
     * Work is O(days × frozen evaluation cost); retained memory is O(budget + fact dates).
     * A 240-point display budget is neither a CPU bound nor an unlimited-history guarantee.
     */
    fun sample(
        snapshot: LedgerSnapshot,
        asOf: LocalDate,
        basePointBudget: Int = 240,
        cancellationCheck: () -> Unit = {},
    ): CostTimeline {
        require(basePointBudget in 12..1000) { "Total-history extrema budget must be between 12 and 1000" }
        cancellationCheck()
        val prepared = CostTimelineEngine.prepare(snapshot)
        if (snapshot.overview.projects.isEmpty() && snapshot.overview.subscriptions.isEmpty()) {
            return CostTimeline(asOf, null, emptyList(), emptyList(), basePointBudget, 0L, false)
        }

        val boundaries = factDates(snapshot).map(LocalDate::toEpochDay).toSortedSet()
        val last = asOf.toEpochDay()
        val first = minOf(boundaries.firstOrNull() ?: last, last)
        val dayCount = Math.addExact(Math.subtractExact(last, first), 1L)
        val critical = sortedSetOf(first, last)
        boundaries.forEach { day ->
            cancellationCheck()
            for (offset in -1L..1L) {
                val neighbour = Math.addExact(day, offset)
                if (neighbour in first..last) critical += neighbour
            }
        }

        val retained = TreeMap<LocalDate, CostTimelinePoint>()
        val bucketCount = basePointBudget / 6
        var bucket = -1
        val minimum = arrayOfNulls<CostTimelinePoint>(3)
        val maximum = arrayOfNulls<CostTimelinePoint>(3)
        fun keepExtrema() {
            minimum.forEach { point -> if (point != null) retained[point.date] = point }
            maximum.forEach { point -> if (point != null) retained[point.date] = point }
            minimum.fill(null)
            maximum.fill(null)
        }

        var epochDay = first
        while (epochDay <= last) {
            cancellationCheck()
            val offset = Math.subtractExact(epochDay, first)
            val nextBucket = (Math.multiplyExact(offset, bucketCount.toLong()) / dayCount).toInt()
            if (nextBucket != bucket) {
                keepExtrema()
                bucket = nextBucket
            }
            val point = prepared.point(LocalDate.ofEpochDay(epochDay))
            if (epochDay in critical) retained[point.date] = point
            for (series in 0..2) {
                val low = minimum[series]
                val high = maximum[series]
                // Strict comparisons preserve the earliest day in ties, deterministically.
                if (low == null || value(point, series) < value(low, series)) minimum[series] = point
                if (high == null || value(point, series) > value(high, series)) maximum[series] = point
            }
            epochDay = Math.incrementExact(epochDay)
        }
        keepExtrema()
        cancellationCheck()
        val events = snapshot.events.filter { !it.date.isAfter(asOf) }
            .sortedWith(compareBy<TotalCostEvent> { it.date }.thenBy { it.id })
        return CostTimeline(asOf, LocalDate.ofEpochDay(first), retained.values.toList(), events,
            basePointBudget, dayCount, retained.size.toLong() < dayCount)
    }

    private fun value(point: CostTimelinePoint, index: Int) = when (index) {
        0 -> point.totalRate
        1 -> point.projectRate
        else -> point.subscriptionRate
    }

    /** Includes all original facts even if event presentation metadata is absent. */
    private fun factDates(snapshot: LedgerSnapshot): Sequence<LocalDate> = sequence {
        snapshot.overview.projects.forEach { entry ->
            yield(entry.snapshot.project.startedOn)
            entry.snapshot.project.endedOn?.let { yield(it) }
            entry.snapshot.moneyEvents.forEach { yield(it.occurredOn) }
        }
        snapshot.overview.subscriptions.forEach { entry ->
            entry.snapshot.renewals.forEach { renewal ->
                yield(renewal.paidOn)
                yield(renewal.effectiveFrom)
                yield(renewal.paidUntilExclusive)
                yield(renewal.reminderDueOn)
            }
            entry.snapshot.stateChanges.forEach { yield(it.effectiveOn) }
        }
        snapshot.events.forEach { yield(it.date) }
    }
}
