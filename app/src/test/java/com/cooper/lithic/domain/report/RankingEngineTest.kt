package com.cooper.lithic.domain.report

import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingEngineTest {
    private val today = LocalDate.of(2026, 10, 21)

    @Test fun review_includes_still_active() {
        val active = project("eight-years", today.minusYears(8), cents = 800_000)
        val ended = project("ended", today.minusYears(2), today.minusYears(1), 100_000)

        val result = RankingEngine.projectRanks(listOf(ended, active), today)

        assertEquals(listOf("eight-years", "ended"), result.map { it.id })
        assertFalse(result.first().isHistory)
        assertTrue(result.first().cost.contributesToCurrentTotal)
        assertEquals(ProjectCostEngine.evaluate(active.snapshot, today), result.first().cost)
    }

    @Test fun types_rank_separately() {
        val projects = listOf(project("same-id", today.minusDays(100), cents = 10_000))
        val subscriptions = listOf(subscription("same-id", listOf(annual())))

        val projectRanks = RankingEngine.projectRanks(projects, today)
        val subscriptionRanks = RankingEngine.subscriptionRanks(subscriptions, today)

        assertEquals(1, projectRanks.size)
        assertEquals(1, subscriptionRanks.size)
        assertEquals("project-category", projectRanks.single().categoryId)
        assertEquals("subscription-category", subscriptionRanks.single().categoryId)
        assertEquals(101L, projectRanks.single().cost.heldDays)
        assertEquals(365L, subscriptionRanks.single().cost.standardPurchasedDays)
    }

    @Test fun current_final_labels() {
        val current = project("current", today.minusDays(9))
        val ended = project("ended", today.minusDays(9), today)
        val futureEnd = project("future-end", today.minusDays(9), today.plusDays(1))

        val result = RankingEngine.projectRanks(listOf(current, ended, futureEnd), today).associateBy { it.id }

        assertFalse(result.getValue("current").isHistory)
        assertTrue(result.getValue("ended").isHistory)
        assertFalse(result.getValue("future-end").isHistory)
        assertFalse(result.getValue("ended").cost.contributesToCurrentTotal)
        assertTrue(result.getValue("future-end").cost.contributesToCurrentTotal)
    }

    @Test fun zero_negative_are_honest() {
        val negative = project("negative", today, cents = 100, recoveries = listOf(event(200, today, MoneyDirection.RECOVERY)))
        val zero = project("zero", today, cents = 0)
        val positive = project("positive", today, cents = 100)

        val result = RankingEngine.projectRanks(listOf(positive, zero, negative), today, ProjectRankOrder.DAILY_RATE)

        assertEquals(listOf("negative", "zero", "positive"), result.map { it.id })
        assertTrue(result.all { it.eligible })
        decimal("-1", result[0].cost.dailyRate)
        decimal("0", result[1].cost.dailyRate)
        decimal("1", result[2].cost.dailyRate)
        assertEquals(-100L, result[0].cost.netCents)
    }

    @Test fun subscription_time_label() {
        val input = subscription("annual", listOf(annual()))

        val result = RankingEngine.subscriptionRanks(listOf(input), today).single()

        assertEquals(365L, result.cost.standardPurchasedDays)
        assertEquals(1L, result.cost.elapsedPaidDays)
        decimal("10", result.cost.historicalAverage)
        assertEquals(SubscriptionCostEngine.evaluate(input.snapshot, today), result.cost)
    }

    @Test fun ended_settlement_changes_cost_without_extending_time_or_reactivating() {
        val end = today.minusDays(3)
        val beforeSettlement = project("ended", end.minusDays(9), end, 10_000)
        val afterSettlement = beforeSettlement.copy(snapshot = beforeSettlement.snapshot.copy(
            moneyEvents = beforeSettlement.snapshot.moneyEvents + event(4_000, today, MoneyDirection.RECOVERY),
        ))

        val oldObservation = RankingEngine.projectRanks(listOf(afterSettlement), end).single()
        val newObservation = RankingEngine.projectRanks(listOf(afterSettlement), today).single()

        assertEquals(10L, oldObservation.cost.heldDays)
        assertEquals(10L, newObservation.cost.heldDays)
        assertEquals(10_000L, oldObservation.cost.netCents)
        assertEquals(6_000L, newObservation.cost.netCents)
        assertTrue(newObservation.isHistory)
        assertFalse(newObservation.cost.contributesToCurrentTotal)
        decimal("6", newObservation.cost.dailyRate)
    }

    @Test fun daily_rate_order_preserves_unrounded_engine_precision() {
        val low = project("z-lower", today.minusDays(999), cents = 100_001)
        val high = project("a-higher", today.minusDays(999), cents = 100_002)

        val result = RankingEngine.projectRanks(listOf(high, low), today, ProjectRankOrder.DAILY_RATE)

        assertEquals(listOf("z-lower", "a-higher"), result.map { it.id })
        assertEquals("1.00", result[0].cost.dailyRate!!.setScale(2, RoundingMode.HALF_UP).toPlainString())
        assertEquals("1.00", result[1].cost.dailyRate!!.setScale(2, RoundingMode.HALF_UP).toPlainString())
        assertTrue(result[0].cost.dailyRate!! < result[1].cost.dailyRate)
    }

    @Test fun not_started_projects_are_explicitly_ineligible_and_follow_valid_zero_in_both_orders() {
        val future = project("a-future", today.plusDays(1), cents = 500)
        val zero = project("z-zero", today, cents = 0)

        for (order in ProjectRankOrder.entries) {
            val result = RankingEngine.projectRanks(listOf(future, zero), today, order)
            assertEquals(listOf("z-zero", "a-future"), result.map { it.id })
            assertTrue(result.first().eligible)
            assertFalse(result.last().eligible)
            assertNull(result.last().cost.dailyRate)
            assertEquals(0L, result.last().cost.heldDays)
        }
    }

    @Test fun equal_values_use_stable_id_tiebreak_in_every_order() {
        val projects = listOf("c", "a", "b").map { project(it, today, cents = 1_000) }
        val subscriptions = listOf("c", "a", "b").map { subscription(it, listOf(annual())) }

        for (order in ProjectRankOrder.entries) {
            assertEquals(listOf("a", "b", "c"), RankingEngine.projectRanks(projects, today, order).map { it.id })
            assertEquals(listOf("a", "b", "c"), RankingEngine.projectRanks(projects.reversed(), today, order).map { it.id })
        }
        assertEquals(listOf("a", "b", "c"), RankingEngine.subscriptionRanks(subscriptions, today).map { it.id })
        assertEquals(listOf("a", "b", "c"), RankingEngine.subscriptionRanks(subscriptions.reversed(), today).map { it.id })
    }

    @Test fun global_order_is_computed_before_fifty_item_display_windows() {
        val projects = (0..102).map { index -> project("id-%03d".format(index), today.minusDays(index.toLong())) }

        val all = RankingEngine.projectRanks(projects, today)
        val windows = all.chunked(50)

        assertEquals(listOf(50, 50, 3), windows.map { it.size })
        assertEquals(103, windows.flatten().map { it.id }.toSet().size)
        assertEquals((102 downTo 0).map { "id-%03d".format(it) }, windows.flatten().map { it.id })
    }

    @Test fun subscription_inactive_states_preserve_purchased_duration_and_historical_average() {
        val first = today.minusDays(10)
        val paid = annual(first)
        val paused = subscription("paused", listOf(paid), listOf(change(today, SubscriptionOperationState.PAUSED)))
        val stopped = subscription("stopped", listOf(paid), listOf(change(today, SubscriptionOperationState.STOPPED)))
        val expired = subscription("expired", listOf(paid.copy(paidUntilExclusive = today)))
        val futureStop = subscription("future-stop", listOf(paid), listOf(change(today.plusDays(1), SubscriptionOperationState.STOPPED)))

        val result = RankingEngine.subscriptionRanks(listOf(paused, stopped, expired, futureStop), today).associateBy { it.id }

        assertEquals(SubscriptionDisplayStatus.PAUSED, result.getValue("paused").cost.displayStatus)
        assertEquals(SubscriptionDisplayStatus.STOPPED, result.getValue("stopped").cost.displayStatus)
        assertEquals(SubscriptionDisplayStatus.EXPIRED, result.getValue("expired").cost.displayStatus)
        assertEquals(SubscriptionDisplayStatus.ACTIVE, result.getValue("future-stop").cost.displayStatus)
        for (item in result.values) {
            assertEquals(365L, item.cost.standardPurchasedDays)
            decimal("10", item.cost.historicalAverage)
        }
        decimal("0", result.getValue("paused").cost.currentRate)
    }

    @Test fun future_prepaid_duration_counts_without_early_current_price_change() {
        val monthly = Renewal(Money(15_000), BillingCycle.MONTHLY, today.minusDays(10), today.minusDays(10), today.plusDays(20))
        val prepaid = annual().copy(effectiveFrom = today.plusDays(20), paidUntilExclusive = today.plusDays(385))
        val input = subscription("prepaid", listOf(monthly, prepaid))

        val beforePayment = RankingEngine.subscriptionRanks(listOf(input), today.minusDays(1)).single().cost
        val onPayment = RankingEngine.subscriptionRanks(listOf(input), today).single().cost

        assertEquals(30L, beforePayment.standardPurchasedDays)
        assertEquals(395L, onPayment.standardPurchasedDays)
        assertEquals(15_000L, beforePayment.confirmedPaidCents)
        assertEquals(380_000L, onPayment.confirmedPaidCents)
        decimal("5", beforePayment.currentRate)
        decimal("5", onPayment.currentRate)
        assertEquals(11L, onPayment.elapsedPaidDays)
        assertEquals(SubscriptionCostEngine.evaluate(input.snapshot, today), onPayment)
    }

    @Test fun no_confirmed_subscription_payments_remain_distinct_from_zero_average() {
        val noPayment = subscription("none", emptyList())
        val futurePayment = subscription("future", listOf(annual(today.plusDays(1))))

        val result = RankingEngine.subscriptionRanks(listOf(noPayment, futurePayment), today)

        assertEquals(listOf("future", "none"), result.map { it.id })
        assertTrue(result.all { it.cost.standardPurchasedDays == 0L })
        assertTrue(result.all { it.cost.historicalAverage == null })
        assertTrue(result.all { it.cost.displayStatus == SubscriptionDisplayStatus.EXPIRED })
    }

    @Test fun ranking_is_pure_and_empty_inputs_remain_empty() {
        val input = listOf(project("project", today.minusDays(5), cents = 12_000))
        val original = input.toList()

        val first = RankingEngine.projectRanks(input, today)
        val second = RankingEngine.projectRanks(input, today)

        assertEquals(first, second)
        assertEquals(original, input)
        assertTrue(RankingEngine.projectRanks(emptyList(), today).isEmpty())
        assertTrue(RankingEngine.subscriptionRanks(emptyList(), today).isEmpty())
    }

    private fun project(
        id: String,
        start: LocalDate,
        end: LocalDate? = null,
        cents: Long = 0,
        recoveries: List<ProjectMoneyEvent> = emptyList(),
    ) = OverviewProject(id, "project-category", ProjectSnapshot(
        LongTermProject(start, end), listOf(event(cents, start)) + recoveries,
    ))

    private fun event(cents: Long, date: LocalDate, direction: MoneyDirection = MoneyDirection.OUTLAY) =
        ProjectMoneyEvent(direction, Money(cents), date)

    private fun annual(start: LocalDate = today) = Renewal(
        Money(365_000), BillingCycle.YEARLY, start, start, start.plusYears(1),
    )

    private fun subscription(id: String, renewals: List<Renewal>, changes: List<SubscriptionStateChange> = emptyList()) =
        OverviewSubscription(id, "subscription-category", SubscriptionSnapshot(Subscription(id), renewals, changes))

    private fun change(date: LocalDate, state: SubscriptionOperationState) = SubscriptionStateChange(date, state)

    private fun decimal(expected: String, actual: BigDecimal?) {
        assertEquals(0, BigDecimal(expected).compareTo(requireNotNull(actual)))
    }
}
