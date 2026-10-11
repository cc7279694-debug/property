package com.cooper.lithic.domain.subscription

import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.time.FixedBusinessClock
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class SubscriptionCostEngineTest {
    private fun date(text: String) = LocalDate.parse(text)
    private val jan1 = date("2026-01-01")
    private val feb1 = date("2026-02-01")
    private fun renewal(
        cents: Long = 15_000,
        cycle: BillingCycle = BillingCycle.MONTHLY,
        paid: LocalDate = jan1,
        from: LocalDate = jan1,
        until: LocalDate = feb1,
        reminder: LocalDate = until,
        days: Int = cycle.standardDays,
    ) = Renewal(Money(cents), cycle, paid, from, until, reminder, days)
    private fun annual() = renewal(365_000, BillingCycle.YEARLY, until = date("2027-01-01"))
    private fun state(day: String, value: SubscriptionOperationState) = SubscriptionStateChange(date(day), value)
    private fun snapshot(
        renewals: List<Renewal> = listOf(renewal()),
        changes: List<SubscriptionStateChange> = emptyList(),
    ) = SubscriptionSnapshot(Subscription("service"), renewals, changes)
    private fun evaluate(input: SubscriptionSnapshot = snapshot(), day: String = "2026-01-01") =
        SubscriptionCostEngine.evaluate(input, date(day))
    private fun decimal(expected: String, actual: BigDecimal?) {
        assertNotNull("Expected a meaningful decimal", actual)
        assertEquals(0, BigDecimal(expected).compareTo(actual))
    }
    private fun inactive(status: SubscriptionDisplayStatus, result: SubscriptionCostResult) {
        assertEquals(status, result.displayStatus)
        decimal("0", result.currentRate)
        assertNull(result.currentPrice)
    }
    private fun invalid(block: () -> Unit) {
        assertThrows(IllegalArgumentException::class.java) { block() }
    }

    @Test fun monthly_standard_rate() {
        val result = evaluate(day = "2026-01-31")
        assertEquals(SubscriptionDisplayStatus.ACTIVE, result.displayStatus)
        decimal("5", result.currentRate)
        assertEquals(Money(15_000), result.currentPrice)
        assertEquals(30L, result.standardPurchasedDays)
        assertEquals(31L, result.elapsedPaidDays)
        decimal("5", result.historicalAverage)
    }

    @Test fun annual_purchase_not_elapsed_year() {
        val input = snapshot(listOf(annual()))
        for ((day, elapsed) in listOf("2026-01-01" to 1L, "2026-04-10" to 100L, "2026-12-31" to 365L)) {
            val result = evaluate(input, day)
            decimal("10", result.currentRate)
            decimal("10", result.historicalAverage)
            assertEquals(365L, result.standardPurchasedDays)
            assertEquals(elapsed, result.elapsedPaidDays)
        }
        val expired = evaluate(input, "2027-01-01")
        inactive(SubscriptionDisplayStatus.EXPIRED, expired)
        assertEquals(365L, expired.elapsedPaidDays)
    }

    @Test fun prepaid_next_rate_does_not_start_early() {
        val input = snapshot(listOf(renewal(), renewal(18_000, paid = date("2026-01-20"), from = feb1, until = date("2026-03-01"))))
        val before = evaluate(input, "2026-01-19")
        decimal("5", before.currentRate)
        assertEquals(15_000L, before.confirmedPaidCents)
        assertEquals(30L, before.standardPurchasedDays)
        val prepaid = evaluate(input, "2026-01-20")
        decimal("5", prepaid.currentRate)
        assertEquals(33_000L, prepaid.confirmedPaidCents)
        assertEquals(60L, prepaid.standardPurchasedDays)
        decimal("5.5", prepaid.historicalAverage)
        assertEquals(20L, prepaid.elapsedPaidDays)
        val next = evaluate(input, "2026-02-01")
        decimal("6", next.currentRate)
        assertEquals(Money(18_000), next.currentPrice)
        assertEquals(32L, next.elapsedPaidDays)
    }

    @Test fun expired_has_zero_current_rate() {
        val expired = evaluate(day = "2026-02-01")
        inactive(SubscriptionDisplayStatus.EXPIRED, expired)
        assertEquals(31L, expired.elapsedPaidDays)
        assertEquals(expired, evaluate(day = "2026-04-01"))
        decimal("5", expired.historicalAverage)
    }

    @Test fun pause_does_not_erase_paid_history() {
        val input = snapshot(changes = listOf(state("2026-01-10", SubscriptionOperationState.PAUSED), state("2026-01-20", SubscriptionOperationState.ENABLED)))
        val paused = evaluate(input, "2026-01-15")
        inactive(SubscriptionDisplayStatus.PAUSED, paused)
        assertEquals(9L, paused.elapsedPaidDays)
        assertEquals(15_000L, paused.confirmedPaidCents)
        assertEquals(30L, paused.standardPurchasedDays)
        decimal("5", paused.historicalAverage)
        val resumed = evaluate(input, "2026-01-20")
        decimal("5", resumed.currentRate)
        assertEquals(10L, resumed.elapsedPaidDays)
        assertEquals(21L, evaluate(input, "2026-01-31").elapsedPaidDays)
    }

    @Test fun overlaps_rejected() {
        val overlap = snapshot(listOf(renewal(), renewal(from = date("2026-01-31"), until = date("2026-03-01"))))
        invalid { SubscriptionIntervalRules.validateStructure(overlap) }
        invalid { evaluate(overlap) }
        val adjacent = snapshot(listOf(renewal(), renewal(paid = jan1, from = feb1, until = date("2026-03-01"))))
        SubscriptionIntervalRules.validateForWrite(adjacent, FixedBusinessClock(jan1))
        assertEquals(32L, evaluate(adjacent, "2026-02-01").elapsedPaidDays)
        invalid { SubscriptionIntervalRules.validateForWrite(snapshot(listOf(renewal(paid = jan1.plusDays(1)))), FixedBusinessClock(jan1)) }
    }

    @Test fun quarterly_standard_rate() {
        val input = snapshot(listOf(renewal(27_000, BillingCycle.QUARTERLY, until = date("2026-04-01"))))
        val result = evaluate(input, "2026-03-31")
        decimal("3", result.currentRate)
        assertEquals(90L, result.standardPurchasedDays)
        assertEquals(90L, result.elapsedPaidDays)
    }

    @Test fun prepaid_future_without_current_coverage_is_expired() {
        val input = snapshot(listOf(renewal(from = feb1, until = date("2026-03-01"))))
        val result = evaluate(input)
        inactive(SubscriptionDisplayStatus.EXPIRED, result)
        assertEquals(15_000L, result.confirmedPaidCents)
        assertEquals(30L, result.standardPurchasedDays)
        assertEquals(0L, result.elapsedPaidDays)
        decimal("5", result.historicalAverage)
        assertNull(result.historicalUnavailableReason)
    }

    @Test fun later_real_payment_is_legal_input_but_unknown_to_earlier_observation() {
        val input = snapshot(listOf(renewal(paid = date("2026-01-20"))))
        SubscriptionIntervalRules.validateForWrite(input, FixedBusinessClock(date("2026-01-20")))
        val earlier = evaluate(input, "2026-01-19")
        inactive(SubscriptionDisplayStatus.EXPIRED, earlier)
        assertEquals(0L, earlier.confirmedPaidCents)
        assertEquals(0L, earlier.standardPurchasedDays)
        assertEquals(0L, earlier.elapsedPaidDays)
        assertNull(earlier.historicalAverage)
        assertEquals(SubscriptionCostUnavailableReason.NO_CONFIRMED_PAYMENTS, earlier.historicalUnavailableReason)
        val known = evaluate(input, "2026-01-20")
        decimal("5", known.currentRate)
        assertEquals(20L, known.elapsedPaidDays)
    }

    @Test fun empty_history_has_explicit_no_record_reason() {
        val result = evaluate(snapshot(emptyList()))
        inactive(SubscriptionDisplayStatus.EXPIRED, result)
        assertNull(result.historicalAverage)
        assertEquals(SubscriptionCostUnavailableReason.NO_CONFIRMED_PAYMENTS, result.historicalUnavailableReason)
        assertEquals(0L, result.standardPurchasedDays)
    }

    @Test fun writing_rejects_future_payment_using_business_today() {
        invalid { SubscriptionIntervalRules.validateForWrite(snapshot(listOf(renewal(paid = feb1))), FixedBusinessClock(jan1)) }
    }

    @Test fun payment_today_and_future_service_are_valid() {
        val input = snapshot(listOf(renewal(from = feb1, until = date("2026-03-01"))))
        SubscriptionIntervalRules.validateForWrite(input, FixedBusinessClock(jan1))
        assertEquals(30L, evaluate(input).standardPurchasedDays)
    }

    @Test fun zero_amount_is_not_a_real_renewal() { invalid { renewal(0) } }
    @Test fun empty_coverage_is_rejected() { invalid { renewal(until = jan1) } }
    @Test fun reversed_coverage_is_rejected() { invalid { renewal(until = jan1.minusDays(1)) } }
    @Test fun stored_standard_days_must_match_fixed_cycle() { invalid { renewal(days = 31) } }

    @Test fun only_three_fixed_cycles_exist() {
        assertEquals(listOf(30, 90, 365), BillingCycle.entries.map { it.standardDays })
        assertEquals(4, SubscriptionDisplayStatus.entries.size)
    }

    @Test fun duplicate_same_day_states_are_rejected_in_both_orders() {
        val changes = listOf(state("2026-01-10", SubscriptionOperationState.PAUSED), state("2026-01-10", SubscriptionOperationState.ENABLED))
        for (ordered in listOf(changes, changes.reversed())) {
            invalid { evaluate(snapshot(changes = ordered), "2026-01-20") }
        }
    }

    @Test fun even_identical_same_day_states_require_correction() {
        val change = state("2026-01-10", SubscriptionOperationState.PAUSED)
        invalid { SubscriptionIntervalRules.validateStructure(snapshot(changes = listOf(change, change))) }
    }

    @Test fun unordered_records_produce_identical_history() {
        val renewals = listOf(renewal(), renewal(18_000, paid = date("2026-01-20"), from = feb1, until = date("2026-03-01")))
        val changes = listOf(state("2026-01-10", SubscriptionOperationState.PAUSED), state("2026-01-20", SubscriptionOperationState.ENABLED), state("2026-02-15", SubscriptionOperationState.STOPPED))
        for (day in listOf("2025-12-31", "2026-01-01", "2026-01-15", "2026-01-20", "2026-02-01", "2026-02-15")) {
            assertEquals(evaluate(snapshot(renewals, changes), day), evaluate(snapshot(renewals.reversed(), changes.reversed()), day))
        }
        assertEquals(SubscriptionDisplayStatus.ACTIVE, evaluate(snapshot(renewals, changes)).displayStatus)
    }

    @Test fun future_pause_and_stop_do_not_change_the_past() {
        val input = snapshot(changes = listOf(state("2026-01-10", SubscriptionOperationState.PAUSED), state("2026-02-01", SubscriptionOperationState.STOPPED)))
        val past = evaluate(input, "2026-01-09")
        assertEquals(SubscriptionDisplayStatus.ACTIVE, past.displayStatus)
        decimal("5", past.currentRate)
        assertEquals(9L, past.elapsedPaidDays)
    }

    @Test fun paused_without_coverage_remains_paused() {
        val input = snapshot(changes = listOf(state("2026-01-10", SubscriptionOperationState.PAUSED)))
        inactive(SubscriptionDisplayStatus.PAUSED, evaluate(input, "2026-02-10"))
        assertEquals(9L, evaluate(input, "2026-02-10").elapsedPaidDays)
    }

    @Test fun resume_after_expiry_does_not_create_payment() {
        val input = snapshot(changes = listOf(state("2026-01-10", SubscriptionOperationState.PAUSED), state("2026-02-10", SubscriptionOperationState.ENABLED)))
        val result = evaluate(input, "2026-02-10")
        inactive(SubscriptionDisplayStatus.EXPIRED, result)
        assertEquals(9L, result.elapsedPaidDays)
        assertEquals(15_000L, result.confirmedPaidCents)
    }

    @Test fun prepaid_cycle_change_starts_at_its_effective_boundary() {
        val input = snapshot(listOf(renewal(), renewal(27_000, BillingCycle.QUARTERLY, date("2026-01-20"), feb1, date("2026-05-01"))))
        val early = evaluate(input, "2026-01-20")
        decimal("5", early.currentRate)
        assertEquals(120L, early.standardPurchasedDays)
        decimal("3.5", early.historicalAverage)
        decimal("3", evaluate(input, "2026-02-01").currentRate)
    }

    @Test fun default_stop_uses_paid_end_and_preserves_preboundary_service() {
        val change = SubscriptionIntervalRules.stopAtPaidBoundary(renewal())
        assertEquals(SubscriptionStateChange(feb1, SubscriptionOperationState.STOPPED), change)
        val input = snapshot(changes = listOf(change))
        decimal("5", evaluate(input, "2026-01-31").currentRate)
        val result = evaluate(input, "2026-02-01")
        inactive(SubscriptionDisplayStatus.STOPPED, result)
        assertEquals(31L, result.elapsedPaidDays)
    }

    @Test fun explicit_early_stop_excludes_its_first_stopped_day() {
        val input = snapshot(changes = listOf(state("2026-01-10", SubscriptionOperationState.STOPPED)))
        val result = evaluate(input, "2026-01-10")
        inactive(SubscriptionDisplayStatus.STOPPED, result)
        assertEquals(9L, result.elapsedPaidDays)
        assertEquals(15_000L, result.confirmedPaidCents)
        assertEquals(30L, result.standardPurchasedDays)
        decimal("5", result.historicalAverage)
        assertEquals(result, evaluate(input, "2026-03-01"))
    }

    @Test fun annual_pause_excludes_ten_days_and_next_real_period_reaches_365() {
        val next = renewal(365_000, BillingCycle.YEARLY, date("2026-12-20"), date("2027-01-01"), date("2028-01-01"))
        val input = snapshot(listOf(annual(), next), listOf(state("2026-01-10", SubscriptionOperationState.PAUSED), state("2026-01-20", SubscriptionOperationState.ENABLED)))
        assertEquals(355L, evaluate(input, "2026-12-31").elapsedPaidDays)
        assertEquals(365L, evaluate(input, "2027-01-10").elapsedPaidDays)
        assertEquals(730L, evaluate(input, "2026-12-31").standardPurchasedDays)
    }

    @Test fun coverage_gap_is_not_counted_or_active() {
        val input = snapshot(listOf(renewal(), renewal(paid = jan1, from = date("2026-02-10"), until = date("2026-03-10"))))
        val gap = evaluate(input, "2026-02-09")
        inactive(SubscriptionDisplayStatus.EXPIRED, gap)
        assertEquals(31L, gap.elapsedPaidDays)
        assertEquals(32L, evaluate(input, "2026-02-10").elapsedPaidDays)
    }

    @Test fun nested_and_duplicate_coverage_are_rejected() {
        invalid { evaluate(snapshot(listOf(annual(), renewal()))) }
        invalid { evaluate(snapshot(listOf(renewal(), renewal()))) }
    }

    @Test fun reminder_boundary_does_not_extend_paid_coverage() {
        val input = snapshot(listOf(renewal(paid = date("2026-09-15"), from = date("2026-09-15"), until = date("2026-10-15"), reminder = date("2026-10-20"))))
        val expired = evaluate(input, "2026-10-15")
        inactive(SubscriptionDisplayStatus.EXPIRED, expired)
        assertEquals(30L, expired.elapsedPaidDays)
        assertEquals(expired, evaluate(input, "2026-10-20"))
    }

    @Test fun leap_year_natural_366_is_distinct_from_standard_365() {
        val input = snapshot(listOf(renewal(365_000, BillingCycle.YEARLY, date("2024-01-01"), date("2024-01-01"), date("2025-01-01"))))
        val result = evaluate(input, "2024-12-31")
        assertEquals(366L, result.elapsedPaidDays)
        assertEquals(365L, result.standardPurchasedDays)
        decimal("10", result.currentRate)
    }

    @Test fun leap_day_start_reaches_365_on_february_27() {
        val input = snapshot(listOf(renewal(365_000, BillingCycle.YEARLY, date("2024-02-29"), date("2024-02-29"), date("2025-02-28"))))
        assertEquals(1L, evaluate(input, "2024-02-29").elapsedPaidDays)
        assertEquals(365L, evaluate(input, "2025-02-27").elapsedPaidDays)
        assertEquals(365L, evaluate(input, "2025-02-28").elapsedPaidDays)
    }

    @Test fun fractional_rate_keeps_decimal128_precision_and_nonzero_value() {
        val result = evaluate(snapshot(listOf(renewal(1))))
        decimal("0.0003333333333333333333333333333333333", result.currentRate)
        decimal("0.0003333333333333333333333333333333333", result.historicalAverage)
        assertTrue(result.currentRate.signum() > 0)
    }

    @Test fun observation_before_all_facts_has_no_cost_or_elapsed_days() {
        val result = evaluate(day = "2025-12-31")
        inactive(SubscriptionDisplayStatus.EXPIRED, result)
        assertEquals(0L, result.confirmedPaidCents)
        assertEquals(0L, result.elapsedPaidDays)
        assertNull(result.historicalAverage)
    }

    @Test fun coverage_at_max_date_expires_without_plus_days_overflow() {
        val input = snapshot(listOf(renewal(paid = LocalDate.MAX.minusDays(1), from = LocalDate.MAX.minusDays(1), until = LocalDate.MAX)))
        val result = SubscriptionCostEngine.evaluate(input, LocalDate.MAX)
        inactive(SubscriptionDisplayStatus.EXPIRED, result)
        assertEquals(1L, result.elapsedPaidDays)
    }

    @Test fun interval_days_use_long_and_do_not_enumerate_dates() {
        val input = snapshot(listOf(renewal(paid = LocalDate.MIN, from = LocalDate.MIN, until = LocalDate.MAX)))
        val result = SubscriptionCostEngine.evaluate(input, LocalDate.MAX.minusDays(1))
        assertEquals(LocalDate.MAX.toEpochDay() - LocalDate.MIN.toEpochDay(), result.elapsedPaidDays)
        assertTrue(result.elapsedPaidDays > Int.MAX_VALUE)
        decimal("5", result.currentRate)
    }

    @Test fun interval_start_state_boundary_counts_enabled_date_only_once() {
        val input = snapshot(changes = listOf(state("2025-12-31", SubscriptionOperationState.PAUSED), state("2026-01-01", SubscriptionOperationState.ENABLED)))
        val result = evaluate(input)
        assertEquals(1L, result.elapsedPaidDays)
        decimal("5", result.currentRate)
    }
}
