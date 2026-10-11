package com.cooper.lithic.domain.subscription

import com.cooper.lithic.core.model.Money
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class SubscriptionAccumulationTest {
    private val jan1 = LocalDate.of(2026, 1, 1)
    private val feb1 = LocalDate.of(2026, 2, 1)
    private fun renewal(cents: Long = 1, cycle: BillingCycle = BillingCycle.MONTHLY) =
        Renewal(Money(cents), cycle, jan1, jan1, when (cycle) {
            BillingCycle.MONTHLY -> feb1
            BillingCycle.QUARTERLY -> jan1.plusMonths(3)
            BillingCycle.YEARLY -> jan1.plusYears(1)
        })

    @Test fun confirmed_paid_cents_reaches_long_max_exactly() {
        val seed = SubscriptionPaidTotals(Long.MAX_VALUE - Money.MAX_CENTS, 0)
        val result = SubscriptionCostEngine.accumulateConfirmedPayment(seed, renewal(Money.MAX_CENTS))
        assertEquals(Long.MAX_VALUE, result.confirmedPaidCents)
        assertEquals(30L, result.standardPurchasedDays)
        assertEquals(Long.MAX_VALUE - Money.MAX_CENTS, seed.confirmedPaidCents)
    }

    @Test fun confirmed_paid_cents_overflow_fails_without_wrapping() {
        val seed = SubscriptionPaidTotals(Long.MAX_VALUE - Money.MAX_CENTS + 1L, 0)
        repeat(2) {
            assertThrows(ArithmeticException::class.java) {
                SubscriptionCostEngine.accumulateConfirmedPayment(seed, renewal(Money.MAX_CENTS))
            }
        }
        assertEquals(Long.MAX_VALUE - Money.MAX_CENTS + 1L, seed.confirmedPaidCents)
        assertEquals(0L, seed.standardPurchasedDays)
    }

    @Test fun standard_purchased_days_reaches_long_max_at_arithmetic_boundary() {
        // Synthetic aggregate seed: valid fixed cycles cannot form this complete-history total.
        val seed = SubscriptionPaidTotals(0, Long.MAX_VALUE - 365L)
        val result = SubscriptionCostEngine.accumulateConfirmedPayment(seed, renewal(cycle = BillingCycle.YEARLY))
        assertEquals(1L, result.confirmedPaidCents)
        assertEquals(Long.MAX_VALUE, result.standardPurchasedDays)
        assertEquals(Long.MAX_VALUE - 365L, seed.standardPurchasedDays)
    }

    @Test fun standard_purchased_days_overflow_fails_without_wrapping() {
        // Same production arithmetic, with a synthetic near-limit aggregate instead of a huge list.
        val seed = SubscriptionPaidTotals(0, Long.MAX_VALUE - 365L + 1L)
        repeat(2) {
            assertThrows(ArithmeticException::class.java) {
                SubscriptionCostEngine.accumulateConfirmedPayment(seed, renewal(cycle = BillingCycle.YEARLY))
            }
        }
        assertEquals(0L, seed.confirmedPaidCents)
        assertEquals(Long.MAX_VALUE - 365L + 1L, seed.standardPurchasedDays)
    }

    @Test fun largest_cycle_multiple_below_long_max_is_safe_then_next_cycle_fails() {
        val seed = SubscriptionPaidTotals(0, Long.MAX_VALUE - 367L)
        assertEquals(0L, seed.standardPurchasedDays % 5L)
        val result = SubscriptionCostEngine.accumulateConfirmedPayment(seed, renewal(cycle = BillingCycle.YEARLY))
        assertEquals(Long.MAX_VALUE - 2L, result.standardPurchasedDays)
        assertEquals(1L, result.confirmedPaidCents)
        assertThrows(ArithmeticException::class.java) {
            SubscriptionCostEngine.accumulateConfirmedPayment(result, renewal())
        }
        assertEquals(Long.MAX_VALUE - 2L, result.standardPurchasedDays)
        assertEquals(1L, result.confirmedPaidCents)
    }

    @Test fun two_real_payments_use_same_accumulation_in_engine() {
        val first = renewal(15_000)
        val next = Renewal(Money(27_000), BillingCycle.QUARTERLY, LocalDate.of(2026, 1, 20), feb1, LocalDate.of(2026, 5, 1))
        val totals = SubscriptionCostEngine.accumulateConfirmedPayment(
            SubscriptionCostEngine.accumulateConfirmedPayment(SubscriptionPaidTotals(), first), next)
        assertEquals(42_000L, totals.confirmedPaidCents)
        assertEquals(120L, totals.standardPurchasedDays)
        val result = SubscriptionCostEngine.evaluate(
            SubscriptionSnapshot(Subscription("service"), listOf(first, next)), LocalDate.of(2026, 1, 20))
        assertEquals(totals.confirmedPaidCents, result.confirmedPaidCents)
        assertEquals(totals.standardPurchasedDays, result.standardPurchasedDays)
    }
}
