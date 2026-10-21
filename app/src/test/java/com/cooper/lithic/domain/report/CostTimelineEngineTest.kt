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
import java.time.LocalDate
import java.util.Random
import org.junit.Assert.*
import org.junit.Test

class CostTimelineEngineTest {
    private val start = LocalDate.of(2026, 1, 1)

    @Test fun empty_observation_list_still_rejects_corrupt_empty_ledger_categories() {
        val good = ledger()
        val blankName = good.copy(overview = good.overview.copy(categories = listOf(
            OverviewCategory("pc", " ", OverviewKind.PROJECT))))
        val duplicate = good.copy(overview = good.overview.copy(categories = listOf(
            OverviewCategory("pc", "项目", OverviewKind.PROJECT), OverviewCategory("pc", "重复", OverviewKind.PROJECT))))
        listOf(blankName, duplicate).forEach { snapshot ->
            assertThrows(IllegalArgumentException::class.java) { CostTimelineEngine.evaluate(snapshot, emptyList()) }
        }
        assertTrue(CostTimelineEngine.evaluate(good, emptyList()).isEmpty())
    }

    @Test fun every_point_matches_reference_seeded_random_ledger() {
        val random = Random(18012026L)
        val projects = (0 until 17).map { index ->
            val from = start.plusDays(random.nextInt(10).toLong())
            val end = if (index % 3 == 0) from.plusDays(7) else null
            val facts = listOf(outlay(random.nextInt(90_000).toLong(), from),
                recovery(random.nextInt(30_000).toLong(), from.plusDays(4)),
                outlay(random.nextInt(10_000).toLong(), from.plusDays(17)))
            project("p$index", from, end, facts)
        }
        val subscriptions = (0 until 11).map { index ->
            val from = start.plusDays(index.toLong())
            subscription("s$index", listOf(renewal(15_001L + index, from, from, from.plusDays(12))),
                if (index % 2 == 0) listOf(change(from.plusDays(4), SubscriptionOperationState.PAUSED),
                    change(from.plusDays(6), SubscriptionOperationState.ENABLED)) else emptyList())
        }
        val snapshot = ledger(projects, subscriptions)
        val dates = (0 until 100).map { start.minusDays(3).plusDays(random.nextInt(37).toLong()) }
        val actual = CostTimelineEngine.evaluate(snapshot, dates)
        assertEquals(dates, actual.map { it.date })
        actual.forEach { point ->
            val projectRate = projects.fold(BigDecimal.ZERO) { sum, p ->
                val result = ProjectCostEngine.evaluate(p.snapshot, point.date)
                sum.add(if (result.contributesToCurrentTotal) requireNotNull(result.dailyRate) else BigDecimal.ZERO)
            }
            val subscriptionRate = subscriptions.fold(BigDecimal.ZERO) { sum, s ->
                val result = SubscriptionCostEngine.evaluate(s.snapshot, point.date)
                sum.add(if (result.displayStatus == SubscriptionDisplayStatus.ACTIVE) result.currentRate else BigDecimal.ZERO)
            }
            equalDecimal(projectRate, point.projectRate)
            equalDecimal(subscriptionRate, point.subscriptionRate)
            equalDecimal(projectRate.add(subscriptionRate), point.totalRate)
            assertEquals(point.projectRate.add(point.subscriptionRate), point.totalRate)
        }
    }

    @Test fun closed_or_expired_removed_q02_and_post_end_settlement() {
        val p = project("q02", start, start.plusDays(9), listOf(outlay(100_000, start), recovery(20_000, start.plusDays(19))))
        val snapshot = ledger(listOf(p))
        equalDecimal(ProjectCostEngine.evaluate(p.snapshot, start.plusDays(8)).dailyRate!!,
            CostTimelineEngine.point(snapshot, start.plusDays(8)).projectRate)
        listOf(9L, 18L, 19L, 30L).forEach { day ->
            equalDecimal(BigDecimal.ZERO, CostTimelineEngine.point(snapshot, start.plusDays(day)).totalRate)
        }
        equalDecimal(BigDecimal("80"), ProjectCostEngine.evaluate(p.snapshot, start.plusDays(19)).dailyRate!!)
        assertEquals(10L, ProjectCostEngine.evaluate(p.snapshot, start.plusDays(19)).heldDays)
    }

    @Test fun same_day_start_and_end_never_manufactures_a_total_spike() {
        val snapshot = ledger(listOf(project("same", start, start, listOf(outlay(99_999, start)))))
        val point = CostTimelineEngine.point(snapshot, start)
        equalDecimal(BigDecimal.ZERO, point.totalRate)
        assertEquals(1L, point.projectCount)
        assertEquals(0L, point.activeProjectCount)
    }

    @Test fun subscription_boundaries_pause_gap_stop_reopen_and_future_stop() {
        val sub = subscription("service", listOf(renewal(15_000, start, start, start.plusDays(10)),
            renewal(18_000, start.plusDays(5), start.plusDays(12), start.plusDays(25))), listOf(
                change(start.plusDays(2), SubscriptionOperationState.PAUSED),
                change(start.plusDays(4), SubscriptionOperationState.ENABLED),
                change(start.plusDays(7), SubscriptionOperationState.STOPPED),
                change(start.plusDays(13), SubscriptionOperationState.ENABLED),
                change(start.plusDays(20), SubscriptionOperationState.STOPPED)))
        val expected = mapOf(0L to "5", 2L to "0", 4L to "5", 5L to "5", 7L to "0", 10L to "0",
            12L to "0", 13L to "6", 19L to "6", 20L to "0")
        expected.forEach { (day, rate) -> equalDecimal(BigDecimal(rate),
            CostTimelineEngine.point(ledger(subscriptions = listOf(sub)), start.plusDays(day)).subscriptionRate) }
    }

    @Test fun prepayment_is_not_current_price_and_paid_until_is_exclusive() {
        val sub = subscription("prepaid", listOf(renewal(15_000, start, start, start.plusDays(31)),
            renewal(18_000, start.plusDays(19), start.plusDays(31), start.plusDays(59))))
        val snapshot = ledger(subscriptions = listOf(sub))
        equalDecimal(BigDecimal("5"), CostTimelineEngine.point(snapshot, start.plusDays(19)).subscriptionRate)
        equalDecimal(BigDecimal("6"), CostTimelineEngine.point(snapshot, start.plusDays(31)).subscriptionRate)
        equalDecimal(BigDecimal.ZERO, CostTimelineEngine.point(snapshot, start.plusDays(59)).subscriptionRate)
    }

    @Test fun today_equals_home_without_display_rounding() {
        val snapshot = ledger(listOf(project("tiny", start, facts = listOf(outlay(1, start))),
            project("large", start.plusDays(2), facts = listOf(outlay(Money.MAX_CENTS, start.plusDays(2))))),
            listOf(subscription("sub", listOf(renewal(1, start, start, start.plusDays(31))))))
        val date = start.plusDays(2)
        val home = OverviewEngine.evaluate(snapshot.overview, date)
        val point = CostTimelineEngine.point(snapshot, date)
        assertEquals(home.totalRate, point.totalRate)
        assertEquals(home.projectRate, point.projectRate)
        assertEquals(home.subscriptionRate, point.subscriptionRate)
        assertTrue(point.totalRate.scale() > 2)
    }

    @Test fun empty_inactive_zero_and_cancellation_are_different() {
        val empty = CostTimelineEngine.point(ledger(), start)
        assertFalse(empty.hasRecords)
        val inactive = CostTimelineEngine.point(ledger(listOf(project("future", start.plusDays(1)))), start)
        assertTrue(inactive.hasRecords)
        assertFalse(inactive.hasActiveContributions)
        val zero = CostTimelineEngine.point(ledger(listOf(project("free", start, facts = listOf(outlay(0, start))))), start)
        assertTrue(zero.hasActiveContributions)
        assertFalse(zero.isCancellationZero)
        val cancel = CostTimelineEngine.point(ledger(listOf(
            project("positive", start, facts = listOf(outlay(100, start))),
            project("negative", start, facts = listOf(outlay(0, start), recovery(100, start))))), start)
        assertTrue(cancel.isCancellationZero)
        equalDecimal(BigDecimal.ZERO, cancel.totalRate)
    }

    @Test fun negative_rate_and_positive_subscription_can_cancel_without_losing_components() {
        val snapshot = ledger(listOf(project("negative", start, facts = listOf(outlay(0, start), recovery(500, start)))),
            listOf(subscription("positive", listOf(renewal(15_000, start, start, start.plusDays(31))))))
        val point = CostTimelineEngine.point(snapshot, start)
        equalDecimal(BigDecimal("-5"), point.projectRate)
        equalDecimal(BigDecimal("5"), point.subscriptionRate)
        assertTrue(point.isCancellationZero)
    }

    @Test fun multi_event_day_not_false_causality_and_prepayment_without_jump() {
        val sub = subscription("s", listOf(renewal(15_000, start, start, start.plusDays(31)),
            renewal(18_000, start.plusDays(19), start.plusDays(31), start.plusDays(59))))
        val day = start.plusDays(19)
        val events = (0 until 73).map { event("event$it", day, "s", OverviewKind.SUBSCRIPTION) }
        val snapshot = ledger(subscriptions = listOf(sub), events = events.reversed())
        val point = CostTimelineEngine.point(snapshot, day)
        assertEquals(events.map { it.id }.sorted(), point.relatedEventIds)
        equalDecimal(CostTimelineEngine.point(snapshot, day.minusDays(1)).totalRate, point.totalRate)
        assertEquals(73, point.relatedEventIds.size)
    }

    @Test fun trade_in_reads_three_stored_facts_without_repeating_purchase_total() {
        val tradedOn = start.plusDays(9)
        val old = project("old", start, tradedOn, listOf(outlay(1_000_000, start), recovery(300_000, tradedOn)))
        val next = project("new", tradedOn, facts = listOf(outlay(500_000, tradedOn), outlay(300_000, tradedOn)))
        val snapshot = ledger(listOf(old, next), events = listOf(event("credit", tradedOn, "new").copy(nonCash = true, amountCents = 300_000)))
        val point = CostTimelineEngine.point(snapshot, tradedOn)
        equalDecimal(BigDecimal("8000"), point.totalRate)
        assertEquals(listOf("credit"), point.relatedEventIds)
        assertTrue(snapshot.events.single().nonCash)
    }

    @Test fun duplicate_or_wrong_record_event_identity_is_rejected() {
        val p = project("p", start)
        val e = event("e", start, "p")
        assertThrows(IllegalArgumentException::class.java) { CostTimelineEngine.point(ledger(listOf(p), events = listOf(e, e)), start) }
        assertThrows(IllegalArgumentException::class.java) { CostTimelineEngine.point(ledger(listOf(p), events = listOf(e.copy(recordId = "missing"))), start) }
        assertThrows(IllegalArgumentException::class.java) { CostTimelineEngine.point(ledger(listOf(p), events = listOf(e.copy(kind = OverviewKind.SUBSCRIPTION))), start) }
    }

    @Test fun dates_preserve_callers_order_and_duplicate_observations() {
        val dates = listOf(start.plusDays(2), start, start.plusDays(2))
        val points = CostTimelineEngine.evaluate(ledger(listOf(project("p", start))), dates)
        assertEquals(dates, points.map { it.date })
        assertEquals(points.first(), points.last())
        assertTrue(points.all { it.hasRecords })
    }
}

internal fun ledger(projects: List<OverviewProject> = emptyList(), subscriptions: List<OverviewSubscription> = emptyList(),
    events: List<TotalCostEvent> = emptyList()): LedgerSnapshot = LedgerSnapshot(OverviewSnapshot(listOf(
        OverviewCategory("pc", "项目", OverviewKind.PROJECT), OverviewCategory("sc", "订阅", OverviewKind.SUBSCRIPTION)), projects, subscriptions), events)
internal fun project(id: String, start: LocalDate, end: LocalDate? = null,
    facts: List<ProjectMoneyEvent> = listOf(outlay(10_000, start))): OverviewProject =
    OverviewProject(id, "pc", ProjectSnapshot(LongTermProject(start, end), facts))
internal fun subscription(id: String, paid: List<Renewal>, changes: List<SubscriptionStateChange> = emptyList()): OverviewSubscription =
    OverviewSubscription(id, "sc", SubscriptionSnapshot(Subscription(id), paid, changes))
internal fun outlay(cents: Long, date: LocalDate) = ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(cents), date)
internal fun recovery(cents: Long, date: LocalDate) = ProjectMoneyEvent(MoneyDirection.RECOVERY, Money(cents), date)
internal fun renewal(cents: Long, paid: LocalDate, from: LocalDate, until: LocalDate) =
    Renewal(Money(cents), BillingCycle.MONTHLY, paid, from, until)
internal fun change(date: LocalDate, state: SubscriptionOperationState) = SubscriptionStateChange(date, state)
internal fun event(id: String, date: LocalDate, record: String, kind: OverviewKind = OverviewKind.PROJECT) =
    TotalCostEvent(id, date, kind, record, "合成记录$record", "该日相关事件", "真实业务日；不指定唯一因果。")
internal fun equalDecimal(expected: BigDecimal, actual: BigDecimal) = assertEquals("$expected != $actual", 0, expected.compareTo(actual))
