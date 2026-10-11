package com.cooper.lithic.domain.project

import com.cooper.lithic.core.model.*
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ProjectCostEngineTest {
    private val start = LocalDate.of(2026, 1, 1)
    private fun day(day: Int) = LocalDate.of(2026, 1, day)
    private fun event(cents: Long, date: LocalDate = start, direction: MoneyDirection = MoneyDirection.OUTLAY) =
        ProjectMoneyEvent(direction, Money(cents), date)
    private fun snapshot(end: LocalDate? = null, events: List<ProjectMoneyEvent> = listOf(event(100_000))) =
        ProjectSnapshot(LongTermProject(start, end), events)
    private fun rate(expected: String, result: ProjectCostResult) {
        assertNotNull("Expected an effective denominator", result.dailyRate)
        assertEquals(0, BigDecimal(expected).compareTo(result.dailyRate))
        assertNull(result.unavailableReason)
    }

    @Test fun same_day_is_one_day() {
        val result = ProjectCostEngine.evaluate(snapshot(), start)
        assertEquals(1L, result.heldDays)
        assertEquals(100_000L, result.grossOutlayCents)
        assertEquals(0L, result.recoveryCents)
        rate("1000", result)
        assertTrue(result.contributesToCurrentTotal)
    }

    @Test fun dated_event_does_not_rewrite_earlier_days() {
        val input = snapshot(events = listOf(event(100_000), event(20_000, day(11))))
        val earlier = ProjectCostEngine.evaluate(input, day(10))
        assertEquals(10L, earlier.heldDays)
        assertEquals(100_000L, earlier.netCents)
        rate("100", earlier)
        val later = ProjectCostEngine.evaluate(input, day(11))
        assertEquals(11L, later.heldDays)
        assertEquals(120_000L, later.netCents)
        rate("109.0909090909090909090909090909091", later)
    }

    @Test fun end_day_removes_current_contribution() {
        val result = ProjectCostEngine.evaluate(snapshot(day(10)), day(10))
        assertEquals(10L, result.heldDays)
        rate("100", result)
        assertFalse(result.contributesToCurrentTotal)
    }

    @Test fun post_end_refund_keeps_duration() {
        val input = snapshot(day(10), listOf(event(100_000), event(20_000, day(20), MoneyDirection.RECOVERY)))
        val result = ProjectCostEngine.evaluate(input, day(20))
        assertEquals(10L, result.heldDays)
        assertEquals(100_000L, result.grossOutlayCents)
        assertEquals(20_000L, result.recoveryCents)
        assertEquals(80_000L, result.netCents)
        rate("80", result)
        assertFalse(result.contributesToCurrentTotal)
    }

    @Test fun negative_cost_is_not_clamped() {
        val result = ProjectCostEngine.evaluate(snapshot(day(10), listOf(
            event(500_000), event(600_000, day(10), MoneyDirection.RECOVERY))), day(20))
        assertEquals(-100_000L, result.netCents)
        rate("-100", result)
        assertFalse(result.contributesToCurrentTotal)
    }

    @Test fun q02_v01_ended_snapshot_uses_historical_observation_day() {
        val input = snapshot(day(10))
        val before = ProjectCostEngine.evaluate(input, start.minusDays(1))
        assertEquals(0L, before.heldDays)
        assertNull(before.dailyRate)
        assertEquals(CostUnavailableReason.PROJECT_NOT_STARTED, before.unavailableReason)
        assertFalse(before.contributesToCurrentTotal)
        val second = ProjectCostEngine.evaluate(input, day(2))
        assertEquals(2L, second.heldDays)
        rate("500", second)
        assertTrue(second.contributesToCurrentTotal)
        val ninth = ProjectCostEngine.evaluate(input, day(9))
        assertEquals(9L, ninth.heldDays)
        rate("111.1111111111111111111111111111111", ninth)
        assertTrue(ninth.contributesToCurrentTotal)
    }

    @Test fun q02_v02_refund_changes_only_its_occurrence_day_and_after() {
        val input = snapshot(day(10), listOf(event(100_000), event(20_000, day(20), MoneyDirection.RECOVERY)))
        for ((date, expected) in listOf(day(10) to "100", day(19) to "100", day(20) to "80", day(31) to "80")) {
            val result = ProjectCostEngine.evaluate(input, date)
            assertEquals(10L, result.heldDays)
            rate(expected, result)
            assertFalse(result.contributesToCurrentTotal)
        }
    }

    @Test fun q02_v03_same_day_end_and_late_settlement_share_one_evaluation() {
        val input = snapshot(start, listOf(event(100_000), event(20_000, day(20), MoneyDirection.RECOVERY)))
        for ((date, expected) in listOf(start to "1000", day(19) to "1000", day(20) to "800")) {
            val result = ProjectCostEngine.evaluate(input, date)
            assertEquals(1L, result.heldDays)
            rate(expected, result)
            assertFalse(result.contributesToCurrentTotal)
        }
    }

    @Test fun prepaid_money_before_start_has_no_effective_daily_rate() {
        val input = snapshot(events = listOf(event(100_000, start.minusDays(2))))
        val before = ProjectCostEngine.evaluate(input, start.minusDays(1))
        assertEquals(100_000L, before.grossOutlayCents)
        assertEquals(0L, before.heldDays)
        assertNull(before.dailyRate)
        assertEquals(CostUnavailableReason.PROJECT_NOT_STARTED, before.unavailableReason)
        assertFalse(before.contributesToCurrentTotal)
        rate("1000", ProjectCostEngine.evaluate(input, start))
    }

    @Test fun rejects_end_date_before_start() {
        assertThrows(IllegalArgumentException::class.java) {
            ProjectCostEngine.evaluate(snapshot(start.minusDays(1)), start)
        }
    }

    @Test fun zero_cost_is_effective_and_contributes_while_active() {
        val result = ProjectCostEngine.evaluate(snapshot(events = listOf(event(0))), day(2))
        assertEquals(2L, result.heldDays)
        assertEquals(0L, result.netCents)
        rate("0", result)
        assertTrue(result.contributesToCurrentTotal)
    }

    @Test fun empty_event_list_is_valid_zero_cost() {
        val result = ProjectCostEngine.evaluate(snapshot(events = emptyList()), start)
        assertEquals(0L, result.grossOutlayCents)
        rate("0", result)
    }

    @Test fun installments_count_only_after_their_real_payment_day() {
        val input = snapshot(events = listOf(event(10_000), event(10_000, day(10)), event(10_000, day(20))))
        for ((date, cents, expected) in listOf(Triple(start, 10_000L, "100"),
            Triple(day(10), 20_000L, "20"), Triple(day(20), 30_000L, "15"))) {
            val result = ProjectCostEngine.evaluate(input, date)
            assertEquals(cents, result.grossOutlayCents)
            rate(expected, result)
        }
    }

    @Test fun post_end_installment_does_not_restart_or_extend_project() {
        val input = snapshot(day(10), listOf(event(100_000), event(20_000, day(20))))
        rate("100", ProjectCostEngine.evaluate(input, day(19)))
        val result = ProjectCostEngine.evaluate(input, day(20))
        assertEquals(10L, result.heldDays)
        assertEquals(120_000L, result.grossOutlayCents)
        rate("120", result)
        assertFalse(result.contributesToCurrentTotal)
    }

    @Test fun refund_is_separate_from_the_original_outlay() {
        val result = ProjectCostEngine.evaluate(snapshot(events = listOf(
            event(100_000), event(20_000, day(2), MoneyDirection.RECOVERY))), day(2))
        assertEquals(100_000L, result.grossOutlayCents)
        assertEquals(20_000L, result.recoveryCents)
        assertEquals(80_000L, result.netCents)
        rate("400", result)
    }

    @Test fun unordered_same_day_events_are_all_counted_once() {
        val input = snapshot(events = listOf(event(20_000, day(2), MoneyDirection.RECOVERY),
            event(10_000, day(2)), event(100_000), event(10_000, day(2))))
        val result = ProjectCostEngine.evaluate(input, day(2))
        assertEquals(120_000L, result.grossOutlayCents)
        assertEquals(20_000L, result.recoveryCents)
        rate("500", result)
        assertEquals(100_000L, ProjectCostEngine.evaluate(input, start).grossOutlayCents)
    }

    @Test fun aggregate_can_exceed_single_event_limit() {
        val result = ProjectCostEngine.evaluate(snapshot(events = listOf(
            event(999_999_999_999), event(999_999_999_999))), start)
        assertEquals(1_999_999_999_998L, result.grossOutlayCents)
        rate("19999999999.98", result)
    }

    @Test fun rejects_gross_outlay_overflow() = assertAggregateOverflow(MoneyDirection.OUTLAY)
    @Test fun rejects_recovery_overflow() = assertAggregateOverflow(MoneyDirection.RECOVERY)

    @Test fun tiny_positive_and_negative_daily_rates_are_not_rounded_to_zero() {
        for ((direction, expected) in listOf(MoneyDirection.OUTLAY to "0.005", MoneyDirection.RECOVERY to "-0.005")) {
            rate(expected, ProjectCostEngine.evaluate(snapshot(events = listOf(event(1, direction = direction))), day(2)))
        }
    }

    @Test fun leap_day_is_a_real_natural_day() {
        val date = LocalDate.of(2024, 2, 28)
        val input = ProjectSnapshot(LongTermProject(date), listOf(event(30_000, date)))
        val result = ProjectCostEngine.evaluate(input, LocalDate.of(2024, 3, 1))
        assertEquals(3L, result.heldDays)
        rate("100", result)
    }

    @Test fun date_span_is_not_narrowed_to_int_or_rounded_to_zero() {
        val input = ProjectSnapshot(LongTermProject(LocalDate.MIN), listOf(event(1, LocalDate.MIN)))
        val result = ProjectCostEngine.evaluate(input, LocalDate.MAX)
        assertEquals(730_484_999_634L, result.heldDays)
        assertTrue(result.dailyRate!!.signum() > 0)
        assertTrue(result.contributesToCurrentTotal)
    }

    private fun assertAggregateOverflow(direction: MoneyDirection) {
        val item = event(999_999_999_999, direction = direction)
        // Real legal single amounts; lazy list avoids millions of allocations.
        val events = object : AbstractList<ProjectMoneyEvent>() {
            override val size = 9_223_373
            override fun get(index: Int): ProjectMoneyEvent {
                require(index in indices)
                return item
            }
        }
        assertThrows(ArithmeticException::class.java) {
            ProjectCostEngine.evaluate(snapshot(events = events), start)
        }
    }
}
