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
import java.math.MathContext
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ItemTimelineTest {
    private val start = LocalDate.of(2026, 1, 1)

    @Test fun past_event_boundary() {
        val repair = start.plusDays(9)
        val recovery = start.plusDays(19)
        val input = project(events = listOf(outlay(100_000, start), outlay(20_000, repair), recovered(30_000, recovery)))
        val timeline = ProjectTimelineEngine.build(input, start.plusDays(30))
        assertRate("111.1111111111111111111111111111111", timeline.holdingPoints.single { it.date == repair.minusDays(1) }.cost.dailyRate)
        assertRate("120", timeline.holdingPoints.single { it.date == repair }.cost.dailyRate)
        assertRate("45", timeline.holdingPoints.single { it.date == recovery }.cost.dailyRate)
        timeline.holdingPoints.forEach { assertEquals(ProjectCostEngine.evaluate(input, it.date), it.cost) }
        assertEquals(ProjectCostEngine.evaluate(project(events = listOf(outlay(100_000, start))), repair.minusDays(1)),
            ProjectTimelineEngine.point(input, start.plusDays(30), repair.minusDays(1)).cost)
    }

    @Test fun q02_ended_snapshot_observes_each_historical_day_then_freezes_duration() {
        val end = start.plusDays(9)
        val settlement = start.plusDays(19)
        val input = project(end = end, events = listOf(outlay(100_000, start), recovered(20_000, settlement)))
        val timeline = ProjectTimelineEngine.build(input, start.plusDays(30))
        assertRate("500", ProjectTimelineEngine.point(input, timeline.asOf, start.plusDays(1)).cost.dailyRate)
        assertEquals(BigDecimal("1000").divide(BigDecimal("9"), MathContext.DECIMAL128),
            ProjectTimelineEngine.point(input, timeline.asOf, start.plusDays(8)).cost.dailyRate)
        val ended = ProjectTimelineEngine.point(input, timeline.asOf, end).cost
        assertRate("100", ended.dailyRate)
        assertFalse(ended.contributesToCurrentTotal)
        assertEquals(10L, ended.heldDays)
        assertRate("100", ProjectTimelineEngine.point(input, timeline.asOf, settlement.minusDays(1)).cost.dailyRate)
        assertRate("80", ProjectTimelineEngine.point(input, timeline.asOf, settlement).cost.dailyRate)
        assertEquals(10L, ProjectTimelineEngine.point(input, timeline.asOf, settlement).cost.heldDays)
        assertEquals(end, timeline.holdingPoints.last().date)
        assertEquals(listOf(settlement), timeline.postEndAdjustments.map { it.date })
    }

    @Test fun before_start_prepayment_is_an_event_but_not_a_fabricated_zero_curve() {
        val prepaid = start.minusDays(10)
        val input = project(events = listOf(outlay(100_000, prepaid)))
        val before = ProjectTimelineEngine.build(input, start.minusDays(1))
        assertTrue(before.holdingPoints.isEmpty())
        assertEquals(listOf(prepaid), before.eventDays)
        assertNull(ProjectTimelineEngine.point(input, before.asOf, prepaid).cost.dailyRate)
        val started = ProjectTimelineEngine.build(input, start)
        assertEquals(listOf(start), started.holdingPoints.map { it.date })
        assertRate("1000", started.holdingPoints.single().cost.dailyRate)
    }

    @Test fun no_forecast() {
        val end = start.plusDays(10)
        val today = start.plusDays(30)
        val input = project(end = end, events = listOf(outlay(100_000, start), recovered(20_000, today.plusDays(1))))
        val timeline = ProjectTimelineEngine.build(input, today)
        assertEquals(end, timeline.holdingPoints.last().date)
        assertTrue(timeline.postEndAdjustments.isEmpty())
        assertTrue(timeline.eventDays.all { !it.isAfter(today) })
        assertThrows(IllegalArgumentException::class.java) { ProjectTimelineEngine.point(input, today, today.plusDays(1)) }
    }

    @Test fun a_future_end_does_not_truncate_today_or_plot_the_future() {
        val today = start.plusDays(4)
        val timeline = ProjectTimelineEngine.build(project(end = today.plusYears(1)), today)
        assertEquals(today, timeline.holdingPoints.last().date)
        assertTrue(timeline.holdingPoints.last().cost.contributesToCurrentTotal)
        assertTrue(timeline.postEndAdjustments.isEmpty())
    }

    @Test fun same_day_multiple_money_events_are_all_counted_and_the_day_is_not_duplicated() {
        val day = start.plusDays(1)
        val input = project(events = listOf(outlay(100_000, start), outlay(20_000, day), recovered(3_000, day), recovered(7_000, day)))
        val timeline = ProjectTimelineEngine.build(input, day)
        assertEquals(listOf(start, day), timeline.eventDays)
        assertEquals(110_000L, timeline.holdingPoints.last().cost.netCents)
        assertRate("550", timeline.holdingPoints.last().cost.dailyRate)
    }

    @Test fun negative_values_visible() {
        val day = start.plusDays(1)
        val input = project(events = listOf(outlay(100_000, start), recovered(200_000, day)))
        val timeline = ProjectTimelineEngine.build(input, day.plusDays(1))
        assertTrue(timeline.holdingPoints.first().cost.dailyRate!! > BigDecimal.ZERO)
        assertTrue(timeline.holdingPoints.last().cost.dailyRate!! < BigDecimal.ZERO)
        assertRate("-500", timeline.holdingPoints.single { it.date == day }.cost.dailyRate)
    }

    @Test fun all_zero_all_negative_tiny_nonzero_and_single_day_keep_exact_rates() {
        val zero = ProjectTimelineEngine.build(project(events = listOf(outlay(0, start))), start)
        assertEquals(1, zero.holdingPoints.size)
        assertRate("0", zero.holdingPoints.single().cost.dailyRate)
        val negative = ProjectTimelineEngine.build(project(events = listOf(recovered(1, start))), start.plusDays(10))
        assertTrue(negative.holdingPoints.all { it.cost.dailyRate!! < BigDecimal.ZERO })
        val tiny = ProjectTimelineEngine.build(project(events = listOf(outlay(1, start))), start.plusYears(10))
        assertTrue(tiny.holdingPoints.last().cost.dailyRate!! > BigDecimal.ZERO)
        assertTrue(tiny.holdingPoints.last().cost.dailyRate!! < BigDecimal("0.01"))
    }

    @Test fun long_span_preserves_all_event_neighbors_without_enumerating_all_dates() {
        val end = start.plusYears(50)
        val event = start.plusDays(1234)
        val input = project(events = listOf(outlay(100_000, start), recovered(1, event)))
        val timeline = ProjectTimelineEngine.build(input, end, maxDisplayPoints = 24)
        assertTrue(timeline.sampled)
        assertEquals(24, timeline.displayPointLimit)
        assertTrue(timeline.holdingPoints.size <= 32)
        assertTrue(timeline.holdingPoints.map { it.date }.containsAll(listOf(start, end, event.minusDays(1), event, event.plusDays(1))))
        assertEquals(timeline.holdingPoints.map { it.date }.sorted().distinct(), timeline.holdingPoints.map { it.date })
        assertEquals(ProjectCostEngine.evaluate(input, event.plusDays(17)), ProjectTimelineEngine.point(input, end, event.plusDays(17)).cost)
    }

    @Test fun all_fact_boundaries_are_retained_even_when_more_than_the_base_point_limit() {
        val events = (0..50).map { outlay(1, start.plusDays(it.toLong())) }
        val timeline = ProjectTimelineEngine.build(project(events = events), start.plusDays(100), maxDisplayPoints = 4)
        assertTrue(timeline.holdingPoints.size > timeline.displayPointLimit)
        assertTrue(timeline.holdingPoints.map { it.date }.containsAll(events.map { it.occurredOn }))
    }

    @Test fun local_date_extremes_use_bounded_sampling_without_date_overflow() {
        val input = ProjectSnapshot(LongTermProject(LocalDate.MIN), listOf(outlay(1, LocalDate.MIN)))
        val timeline = ProjectTimelineEngine.build(input, LocalDate.MAX, maxDisplayPoints = 8)
        assertEquals(LocalDate.MIN, timeline.holdingPoints.first().date)
        assertEquals(LocalDate.MAX, timeline.holdingPoints.last().date)
        assertTrue(timeline.holdingPoints.size <= 12)
        assertTrue(timeline.holdingPoints.last().cost.dailyRate!! > BigDecimal.ZERO)
    }

    @Test fun noncash_trade_in_money_is_not_counted_again_as_an_extra_initial_total() {
        val input = project(events = listOf(outlay(500_000, start), outlay(300_000, start)))
        val timeline = ProjectTimelineEngine.build(input, start)
        assertEquals(800_000L, timeline.holdingPoints.single().cost.grossOutlayCents)
        assertRate("8000", timeline.holdingPoints.single().cost.dailyRate)
    }

    @Test fun subscription_is_step_not_smooth() {
        val next = start.plusMonths(1)
        val input = subscription(listOf(renewal(15_000, start, start, next), renewal(18_000, start.plusDays(19), next, next.plusMonths(1))))
        val timeline = SubscriptionTimelineEngine.build(input, next.plusDays(3))
        assertRate("5", SubscriptionTimelineEngine.point(input, timeline.asOf, start.plusDays(18)).cost.currentRate)
        assertRate("5", SubscriptionTimelineEngine.point(input, timeline.asOf, start.plusDays(19)).cost.currentRate)
        assertRate("5", timeline.points.single { it.date == next.minusDays(1) }.cost.currentRate)
        assertRate("6", timeline.points.single { it.date == next }.cost.currentRate)
        timeline.points.forEach { assertEquals(SubscriptionCostEngine.evaluate(input, it.date), it.cost) }
    }

    @Test fun subscription_pause_resume_expiry_and_stopping_are_zero_with_their_real_status() {
        val input = subscription(listOf(renewal(15_000, start, start, start.plusDays(10))), listOf(
            state(2, SubscriptionOperationState.PAUSED), state(4, SubscriptionOperationState.ENABLED), state(12, SubscriptionOperationState.STOPPED),
        ))
        val timeline = SubscriptionTimelineEngine.build(input, start.plusDays(15))
        assertPoint(timeline, 1, "5", SubscriptionDisplayStatus.ACTIVE)
        assertPoint(timeline, 2, "0", SubscriptionDisplayStatus.PAUSED)
        assertPoint(timeline, 4, "5", SubscriptionDisplayStatus.ACTIVE)
        assertPoint(timeline, 10, "0", SubscriptionDisplayStatus.EXPIRED)
        assertPoint(timeline, 12, "0", SubscriptionDisplayStatus.STOPPED)
    }

    @Test fun subscription_late_payment_respects_paid_on_in_addition_to_coverage() {
        val payment = start.plusDays(9)
        val input = subscription(listOf(renewal(15_000, payment, start, start.plusMonths(1))))
        val timeline = SubscriptionTimelineEngine.build(input, payment.plusDays(1))
        assertRate("0", timeline.points.single { it.date == payment.minusDays(1) }.cost.currentRate)
        assertRate("5", timeline.points.single { it.date == payment }.cost.currentRate)
        assertEquals(SubscriptionDisplayStatus.EXPIRED, SubscriptionTimelineEngine.point(input, timeline.asOf, start).cost.displayStatus)
    }

    @Test fun subscription_gap_is_retained_instead_of_connecting_positive_periods() {
        val input = subscription(listOf(renewal(15_000, start, start, start.plusDays(5)),
            renewal(18_000, start.plusDays(9), start.plusDays(10), start.plusDays(20))))
        val timeline = SubscriptionTimelineEngine.build(input, start.plusDays(13), maxDisplayPoints = 3)
        assertPoint(timeline, 5, "0", SubscriptionDisplayStatus.EXPIRED)
        assertPoint(timeline, 9, "0", SubscriptionDisplayStatus.EXPIRED)
        assertPoint(timeline, 10, "6", SubscriptionDisplayStatus.ACTIVE)
        assertTrue(timeline.points.map { it.date }.contains(start.plusDays(6)))
    }

    @Test fun subscription_current_paused_or_stopped_state_does_not_overwrite_past_active_days() {
        val input = subscription(listOf(renewal(15_000, start, start, start.plusMonths(1))), listOf(state(5, SubscriptionOperationState.PAUSED)))
        val timeline = SubscriptionTimelineEngine.build(input, start.plusDays(6))
        assertRate("5", SubscriptionTimelineEngine.point(input, timeline.asOf, start.plusDays(4)).cost.currentRate)
        assertPoint(timeline, 5, "0", SubscriptionDisplayStatus.PAUSED)
    }

    @Test fun subscription_prepaid_future_period_and_future_stop_are_not_forecast() {
        val next = start.plusMonths(1)
        val input = subscription(listOf(renewal(15_000, start, start, next), renewal(18_000, start.plusDays(2), next, next.plusMonths(1))),
            listOf(SubscriptionStateChange(next.plusDays(5), SubscriptionOperationState.STOPPED)))
        val today = start.plusDays(5)
        val timeline = SubscriptionTimelineEngine.build(input, today)
        assertTrue(timeline.points.all { !it.date.isAfter(today) })
        assertTrue(timeline.eventDays.all { !it.isAfter(today) })
        assertRate("5", timeline.points.last().cost.currentRate)
        assertThrows(IllegalArgumentException::class.java) { SubscriptionTimelineEngine.point(input, today, next) }
    }

    @Test fun subscription_only_future_coverage_shows_zero_not_an_active_future_price() {
        val input = subscription(listOf(renewal(365_000, start, start.plusDays(10), start.plusYears(1), BillingCycle.YEARLY)))
        val timeline = SubscriptionTimelineEngine.build(input, start)
        assertEquals(listOf(start), timeline.points.map { it.date })
        assertRate("0", timeline.points.single().cost.currentRate)
        assertEquals(SubscriptionDisplayStatus.EXPIRED, timeline.points.single().cost.displayStatus)
    }

    @Test fun subscription_same_identity_reopen_and_fixed_standard_days_remain_frozen_engine_results() {
        val input = subscription(listOf(renewal(365_000, start, start, start.plusYears(1), BillingCycle.YEARLY)), listOf(
            state(2, SubscriptionOperationState.STOPPED), state(4, SubscriptionOperationState.ENABLED),
        ))
        val timeline = SubscriptionTimelineEngine.build(input, start.plusDays(7))
        assertPoint(timeline, 2, "0", SubscriptionDisplayStatus.STOPPED)
        assertPoint(timeline, 4, "10", SubscriptionDisplayStatus.ACTIVE)
        assertEquals(365L, timeline.points.last().cost.standardPurchasedDays)
        assertRate("10", timeline.points.last().cost.historicalAverage)
    }

    @Test fun subscription_long_span_keeps_every_state_and_coverage_neighbor() {
        val end = start.plusYears(20)
        val change = start.plusDays(2345)
        val input = subscription(listOf(renewal(365_000, start, start, end, BillingCycle.YEARLY)), listOf(SubscriptionStateChange(change, SubscriptionOperationState.PAUSED)))
        val timeline = SubscriptionTimelineEngine.build(input, end.plusDays(10), maxDisplayPoints = 12)
        assertTrue(timeline.sampled)
        assertTrue(timeline.points.size <= 25)
        assertTrue(timeline.points.map { it.date }.containsAll(listOf(start, change.minusDays(1), change, change.plusDays(1), end.minusDays(1), end, end.plusDays(1))))
        assertEquals(SubscriptionCostEngine.evaluate(input, change.minusDays(50)), SubscriptionTimelineEngine.point(input, timeline.asOf, change.minusDays(50)).cost)
    }

    @Test fun empty_future_only_and_invalid_display_limits_fail_or_remain_explicit() {
        val empty = SubscriptionTimelineEngine.build(subscription(emptyList()), start)
        assertTrue(empty.points.isEmpty())
        val unknown = SubscriptionTimelineEngine.build(subscription(listOf(renewal(100, start.plusDays(1), start.plusDays(2), start.plusDays(3)))), start)
        assertTrue(unknown.points.isEmpty())
        assertThrows(IllegalArgumentException::class.java) { ProjectTimelineEngine.build(project(), start, maxDisplayPoints = 0) }
        assertThrows(IllegalArgumentException::class.java) { SubscriptionTimelineEngine.build(subscription(emptyList()), start, maxDisplayPoints = 1001) }
    }

    private fun project(end: LocalDate? = null, events: List<ProjectMoneyEvent> = listOf(outlay(100_000, start))) =
        ProjectSnapshot(LongTermProject(start, end), events)
    private fun outlay(cents: Long, date: LocalDate) = ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(cents), date)
    private fun recovered(cents: Long, date: LocalDate) = ProjectMoneyEvent(MoneyDirection.RECOVERY, Money(cents), date)
    private fun subscription(renewals: List<Renewal>, states: List<SubscriptionStateChange> = emptyList()) = SubscriptionSnapshot(Subscription("service"), renewals, states)
    private fun renewal(cents: Long, paid: LocalDate, from: LocalDate, until: LocalDate, cycle: BillingCycle = BillingCycle.MONTHLY) = Renewal(Money(cents), cycle, paid, from, until)
    private fun state(offset: Long, state: SubscriptionOperationState) = SubscriptionStateChange(start.plusDays(offset), state)
    private fun assertRate(expected: String, actual: BigDecimal?) { assertNotNull(actual); assertEquals(0, BigDecimal(expected).compareTo(actual)) }
    private fun assertPoint(timeline: SubscriptionTimeline, offset: Long, value: String, status: SubscriptionDisplayStatus) {
        val point = timeline.points.single { it.date == start.plusDays(offset) }
        assertRate(value, point.cost.currentRate); assertEquals(status, point.cost.displayStatus)
    }
}
