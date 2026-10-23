package com.cooper.lithic.domain.report

import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.time.LocalDate
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

/** Prepared data is local to a single captured ledger; OverviewEngine remains the reference. */
class M22TimelinePreparationTest {
    private val first = LocalDate.of(2006, 10, 23)

    @Test fun cancellation_is_observed_inside_a_single_large_observation_day() {
        val input = ledger(projects = (0 until 1_000).map { project("p-$it", first) })
        var checks = 0
        assertThrows(CancellationException::class.java) {
            TimelineSampler.sample(input, first, 12) {
                if (++checks == 128) throw CancellationException("M22 synthetic in-day cancellation")
            }
        }
        assertEquals(128, checks)
    }

    @Test fun prepared_daily_scan_matches_frozen_overview_across_payment_and_state_boundaries() {
        val input = ledger(
            projects = listOf(
                project("active", first, facts = listOf(outlay(1, first), recovery(5_000, first.plusDays(8)))),
                project("ended", first, first.plusDays(20), listOf(outlay(20_000, first), recovery(500, first.plusDays(30)))),
                project("zero", first.plusDays(10), facts = listOf(outlay(0, first.plusDays(10))))),
            subscriptions = listOf(subscription("s", listOf(
                renewal(3_000, first, first.plusDays(2), first.plusDays(12)),
                renewal(1, first.plusDays(3), first.plusDays(12), first.plusDays(22)),
                renewal(9_000, first.plusDays(25), first.plusDays(28), first.plusDays(58))),
                listOf(change(first.plusDays(4), SubscriptionOperationState.PAUSED),
                    change(first.plusDays(7), SubscriptionOperationState.ENABLED),
                    change(first.plusDays(40), SubscriptionOperationState.STOPPED),
                    change(first.plusDays(45), SubscriptionOperationState.ENABLED)))))
        val dates = (-2L..65L).map(first::plusDays)
        val observed = CostTimelineEngine.evaluate(input, dates + dates.reversed())
        (dates + dates.reversed()).zip(observed).forEach { (date, point) ->
            val expected = OverviewEngine.evaluate(input.overview, date)
            assertEquals("$date project", expected.projectRate, point.projectRate)
            assertEquals("$date subscription", expected.subscriptionRate, point.subscriptionRate)
            assertEquals("$date total", expected.totalRate, point.totalRate)
            assertEquals(expected.activeProjectCount, point.activeProjectCount)
            assertEquals(expected.activeSubscriptionCount, point.activeSubscriptionCount)
        }
    }

    @Test fun prepared_state_does_not_leak_across_same_generation_fact_revisions() {
        val original = ledger(subscriptions = listOf(subscription("s", listOf(renewal(3_000, first, first, first.plusDays(30))))))
        val changed = ledger(subscriptions = listOf(subscription("s", listOf(renewal(9_000, first, first, first.plusDays(30))))))
        val dates = listOf(first.plusDays(3), first.plusDays(7))
        val before = CostTimelineEngine.evaluate(original, dates)
        val after = CostTimelineEngine.evaluate(changed, dates)
        assertNotEquals(before.first().subscriptionRate, after.first().subscriptionRate)
        assertEquals(before, CostTimelineEngine.evaluate(original, dates))
        dates.zip(after).forEach { (date, point) -> assertEquals(OverviewEngine.evaluate(changed.overview, date).totalRate, point.totalRate) }
    }

    @Test fun exact_query_cancellation_reaches_large_snapshot_preparation() {
        val input = ledger(projects = (0 until 1_000).map { project("p-$it", first) })
        var checks = 0
        assertThrows(CancellationException::class.java) {
            CostTimelineEngine.point(input, first) {
                if (++checks == 128) throw CancellationException("M22 synthetic preparation cancellation")
            }
        }
        assertEquals(128, checks)
    }

    @Test fun prepared_one_day_evaluation_checks_cancellation_between_records() {
        val input = ledger(projects = (0 until 1_000).map { project("p-$it", first) })
        val prepared = CostTimelineEngine.prepare(input)
        var checks = 0
        assertThrows(CancellationException::class.java) {
            prepared.point(first) {
                if (++checks == 128) throw CancellationException("M22 synthetic prepared-day cancellation")
            }
        }
        assertEquals(128, checks)
    }
}
