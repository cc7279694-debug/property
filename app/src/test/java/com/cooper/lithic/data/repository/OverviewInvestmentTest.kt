package com.cooper.lithic.data.repository

import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.report.OverviewProject
import com.cooper.lithic.domain.report.OverviewSnapshot
import com.cooper.lithic.domain.report.OverviewSubscription
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverviewInvestmentTest {
    private val asOf = LocalDate.of(2026, 5, 20)

    @Test fun sums_project_outlay_and_actual_payment_dates_across_batches() {
        val project = project("ended", listOf(
            event(MoneyDirection.OUTLAY, 10_000, asOf.minusDays(4)),
            event(MoneyDirection.OUTLAY, 2_500, asOf),
            event(MoneyDirection.RECOVERY, 4_000, asOf),
            event(MoneyDirection.OUTLAY, 9_000, asOf.plusDays(1)),
        ))
        val stopped = subscription("stopped", SubscriptionOperationState.STOPPED, listOf(
            payment(1_000, asOf.minusDays(50), asOf.minusDays(50)),
            payment(2_000, asOf.minusDays(35), asOf.minusDays(20)),
        ))
        val paused = subscription("paused", SubscriptionOperationState.PAUSED, listOf(
            payment(600, asOf, asOf.plusDays(3)),
            payment(800, asOf.plusDays(1), asOf.plusDays(34)),
        ))

        val projectBatch = OverviewSnapshot(emptyList(), listOf(project), emptyList())
        val subscriptionBatch = OverviewSnapshot(emptyList(), emptyList(), listOf(stopped, paused))
        val first = accumulateOverviewInvestmentCents(0L, projectBatch, asOf)
        assertEquals(12_500L, first)
        assertEquals(16_100L, accumulateOverviewInvestmentCents(first, subscriptionBatch, asOf))
    }

    @Test fun verified_empty_and_zero_remain_distinct_from_unknown_accumulation() {
        val empty = OverviewSnapshot(emptyList(), emptyList(), emptyList())
        val zero = empty.copy(projects = listOf(project("zero", listOf(event(MoneyDirection.OUTLAY, 0, asOf)))))
        assertEquals(0L, accumulateOverviewInvestmentCents(0L, empty, asOf))
        assertEquals(0L, accumulateOverviewInvestmentCents(0L, zero, asOf))
        assertNull(accumulateOverviewInvestmentCents(null, zero, asOf))
    }

    @Test fun aggregate_can_exceed_the_single_event_limit_and_never_wraps() {
        val batch = OverviewSnapshot(emptyList(), listOf(project("large", listOf(
            event(MoneyDirection.OUTLAY, Money.MAX_CENTS, asOf),
            event(MoneyDirection.OUTLAY, Money.MAX_CENTS, asOf),
        ))), emptyList())
        assertEquals(2 * Money.MAX_CENTS, accumulateOverviewInvestmentCents(0L, batch, asOf))
        assertNull(accumulateOverviewInvestmentCents(Long.MAX_VALUE - Money.MAX_CENTS, batch, asOf))
    }

    private fun project(id: String, events: List<ProjectMoneyEvent>) = OverviewProject(
        id, "project-category", ProjectSnapshot(LongTermProject(asOf.minusDays(10), asOf.minusDays(2)), events),
    )

    private fun event(direction: MoneyDirection, cents: Long, date: LocalDate) =
        ProjectMoneyEvent(direction, Money(cents), date)

    private fun payment(cents: Long, paidOn: LocalDate, from: LocalDate) =
        Renewal(Money(cents), BillingCycle.MONTHLY, paidOn, from, from.plusMonths(1))

    private fun subscription(id: String, state: SubscriptionOperationState, payments: List<Renewal>) =
        OverviewSubscription(id, "subscription-category", SubscriptionSnapshot(
            Subscription(id), payments, listOf(SubscriptionStateChange(asOf, state)),
        ))
}
