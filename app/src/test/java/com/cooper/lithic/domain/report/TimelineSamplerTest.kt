package com.cooper.lithic.domain.report

import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class TimelineSamplerTest {
    private val start = LocalDate.of(2006, 1, 1)
    private val until = start.plusYears(20)

    @Test fun empty_ledger_sampling_cannot_hide_corrupt_categories_as_no_records() {
        val good = ledger()
        val blankName = good.copy(overview = good.overview.copy(categories = listOf(
            OverviewCategory("pc", "", OverviewKind.PROJECT))))
        val duplicate = good.copy(overview = good.overview.copy(categories = listOf(
            OverviewCategory("pc", "项目", OverviewKind.PROJECT), OverviewCategory("pc", "重复", OverviewKind.PROJECT))))
        listOf(blankName, duplicate).forEach { snapshot ->
            assertThrows(IllegalArgumentException::class.java) { TimelineSampler.sample(snapshot, until) }
        }
        assertTrue(TimelineSampler.sample(good, until).points.isEmpty())
    }

    @Test fun sampling_keeps_spikes_twenty_year_reference_and_all_three_bucket_extrema() {
        val snapshot = twentyYearLedger()
        val full = (0L..ChronoUnit.DAYS.between(start, until)).map { CostTimelineEngine.point(snapshot, start.plusDays(it)) }
        val timeline = TimelineSampler.sample(snapshot, until, 60)
        val kept = timeline.points.map { it.date }.toSet()
        assertEquals(start, timeline.from)
        assertEquals(start, timeline.points.first().date)
        assertEquals(until, timeline.points.last().date)
        assertEquals(full.size.toLong(), timeline.evaluatedDayCount)
        assertTrue(timeline.sampled)
        val count = full.size.toLong()
        val buckets = 60 / 6
        full.groupBy { ((it.date.toEpochDay() - start.toEpochDay()) * buckets / count).toInt() }.values.forEach { points ->
            listOf<(CostTimelinePoint) -> BigDecimal>({ it.totalRate }, { it.projectRate }, { it.subscriptionRate }).forEach { value ->
                assertTrue(kept.contains(points.minBy { value(it) }.date))
                assertTrue(kept.contains(points.maxBy { value(it) }.date))
            }
        }
        val peak = start.plusDays(1_173)
        assertFalse((0 until 60).map { start.plusDays(ChronoUnit.DAYS.between(start, until) * it / 59) }.contains(peak))
        assertTrue(kept.contains(peak))
        assertTrue(kept.contains(start.plusDays(2_197)))
        assertTrue(kept.contains(start.plusDays(3_411)))
        assertTrue(timeline.points.any { it.projectRate.signum() < 0 })
    }

    @Test fun key_dates_and_neighbours_are_retained_beyond_budget() {
        val dates = (0 until 60).map { start.plusDays(it * 4L) }
        val p = project("p", start, start.plusDays(245), dates.map { outlay(100, it) })
        val events = dates.mapIndexed { index, date -> event("e$index", date, "p") }
        val result = TimelineSampler.sample(ledger(listOf(p), events = events), start.plusDays(250), 12)
        val kept = result.points.map { it.date }.toSet()
        dates.forEach { date ->
            assertTrue(kept.contains(date))
            if (date != start) assertTrue(kept.contains(date.minusDays(1)))
            assertTrue(kept.contains(date.plusDays(1)))
        }
        assertTrue(result.points.size > result.basePointBudget)
        assertTrue(kept.contains(start.plusDays(245)))
    }

    @Test fun component_cancellation_does_not_erase_subscription_spike_or_negative_valley() {
        val snapshot = twentyYearLedger()
        val result = TimelineSampler.sample(snapshot, until, 12)
        val peak = result.points.single { it.date == start.plusDays(3_411) }
        assertTrue(peak.projectRate.signum() < 0)
        assertTrue(peak.subscriptionRate.signum() > 0)
        assertTrue(peak.isCancellationZero)
        assertEquals(peak.projectRate.add(peak.subscriptionRate), peak.totalRate)
        assertTrue(result.points.any { it.date == start.plusDays(3_412) && it.subscriptionRate.signum() == 0 })
    }

    @Test fun exact_non_display_date_returns_original_value() {
        val snapshot = twentyYearLedger()
        val timeline = TimelineSampler.sample(snapshot, until, 12)
        val kept = timeline.points.map { it.date }.toSet()
        val hidden = (0L..ChronoUnit.DAYS.between(start, until)).asSequence().map { start.plusDays(it) }.first { it !in kept }
        val exact = CostTimelineEngine.point(snapshot, hidden)
        val homeReference = OverviewEngine.evaluate(snapshot.overview, hidden)
        assertEquals(homeReference.totalRate, exact.totalRate)
        assertEquals(homeReference.projectRate, exact.projectRate)
        assertEquals(homeReference.subscriptionRate, exact.subscriptionRate)
    }

    @Test fun paid_coverage_and_status_boundaries_survive_even_without_event_metadata() {
        val s = subscription("s", listOf(renewal(15_000, start.plusDays(2), start.plusDays(5), start.plusDays(15))),
            listOf(change(start.plusDays(7), SubscriptionOperationState.PAUSED), change(start.plusDays(9), SubscriptionOperationState.ENABLED),
                change(start.plusDays(13), SubscriptionOperationState.STOPPED)))
        val timeline = TimelineSampler.sample(ledger(subscriptions = listOf(s)), start.plusDays(100), 12)
        val kept = timeline.points.map { it.date }.toSet()
        listOf(2L, 5L, 7L, 9L, 13L, 15L).forEach { day ->
            for (offset in -1L..1L) if (day + offset >= 2) assertTrue(kept.contains(start.plusDays(day + offset)))
        }
    }

    @Test fun empty_ledger_is_not_a_fabricated_zero_history() {
        val timeline = TimelineSampler.sample(ledger(), until)
        assertNull(timeline.from)
        assertTrue(timeline.points.isEmpty())
        assertEquals(0L, timeline.evaluatedDayCount)
    }

    @Test fun future_only_facts_do_not_forecast_and_current_cutoff_is_kept() {
        val future = until.plusDays(7)
        val timeline = TimelineSampler.sample(ledger(listOf(project("future", future)), events = listOf(event("future", future, "future"))), until)
        assertEquals(listOf(until), timeline.points.map { it.date })
        assertEquals(1L, timeline.evaluatedDayCount)
        equalDecimal(BigDecimal.ZERO, timeline.points.single().totalRate)
        assertTrue(timeline.events.isEmpty())
    }

    @Test fun cancellation_is_checked_while_scanning_and_publishes_no_partial_timeline() {
        var checks = 0
        assertThrows(CancellationException::class.java) {
            TimelineSampler.sample(twentyYearLedger(), until, 60) {
                if (++checks == 100) throw CancellationException("synthetic cancellation")
            }
        }
        assertEquals(100, checks)
    }

    @Test fun extreme_localdate_endpoint_does_not_overflow_neighbour_math() {
        val snapshot = ledger(listOf(project("max", LocalDate.MAX, facts = listOf(outlay(100, LocalDate.MAX)))))
        val result = TimelineSampler.sample(snapshot, LocalDate.MAX, 12)
        assertEquals(listOf(LocalDate.MAX), result.points.map { it.date })
        equalDecimal(BigDecimal.ONE, result.points.single().totalRate)
    }

    @Test fun invalid_budget_is_rejected_instead_of_silently_changing_history() {
        listOf(0, 1, 11, 1_001).forEach { budget ->
            assertThrows(IllegalArgumentException::class.java) { TimelineSampler.sample(twentyYearLedger(), until, budget) }
        }
    }

    private fun twentyYearLedger(): LedgerSnapshot {
        val peak = start.plusDays(1_173)
        val valley = start.plusDays(2_197)
        val cancel = start.plusDays(3_411)
        val projects = listOf(
            project("base", start, start.plusDays(1_000), listOf(outlay(10_000, start))),
            project("spike", peak, peak.plusDays(1), listOf(outlay(90_000_000, peak))),
            project("negative", valley, valley.plusDays(1), listOf(outlay(0, valley), recovery(30_000_000, valley))),
            project("cancel", cancel, cancel.plusDays(1), listOf(outlay(0, cancel), recovery(100_000, cancel))))
        val sub = subscription("service", listOf(renewal(3_000_000, cancel, cancel, cancel.plusDays(4))), listOf(
            change(cancel.plusDays(1), SubscriptionOperationState.PAUSED), change(cancel.plusDays(2), SubscriptionOperationState.ENABLED)))
        val events = listOf(event("spike-money", peak, "spike"), event("negative-money", valley, "negative"),
            event("cancel-money", cancel, "cancel"), event("prepayment", cancel, "service", OverviewKind.SUBSCRIPTION))
        return ledger(projects, listOf(sub), events)
    }
}
