package com.cooper.lithic.domain.reminder

import com.cooper.lithic.core.model.Money
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.time.LocalDate
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class MilestonePlannerTest {
    private fun date(value: String) = LocalDate.parse(value)
    private fun now(day: String, time: String = "09:00:00") = ZonedDateTime.parse("${day}T${time}+08:00[Asia/Shanghai]")
    private fun project(start: String = "2026-01-01", end: String? = null, id: String = "project") =
        ProjectMilestoneSource(id, "合成长期项目", date(start), end?.let(::date))
    private fun renewal(from: String, until: String, paid: String = from, id: String = "payment") =
        ReminderRenewal(id, Renewal(Money(365_000), BillingCycle.YEARLY, date(paid), date(from), date(until)))
    private fun subscription(
        from: String = "2026-01-01", until: String = "2027-01-01", id: String = "subscription",
        states: List<SubscriptionStateChange> = emptyList(), rows: List<ReminderRenewal>? = null,
    ) = RenewalReminderSource(id, "合成订阅", rows ?: listOf(renewal(from, until)), states)
    private fun state(on: String, value: SubscriptionOperationState) = SubscriptionStateChange(date(on), value)
    private fun projectCandidates(source: ProjectMilestoneSource, day: String, watermark: MilestoneWatermark,
        keys: Set<String> = emptySet(), enabled: Boolean = true, time: String = "09:00:00") =
        MilestonePlanner.evaluate(listOf(source), emptyList(), now(day, time), listOf(watermark), keys, enabled)

    @Test fun project_day_nodes_count_the_first_day_and_calendar_years_are_distinct() {
        val source = project()
        assertTrue(MilestonePlanner.projectReached(source, date("2026-04-09")).isEmpty())
        assertEquals(date("2026-04-10"), MilestonePlanner.projectReached(source, date("2026-04-10")).single().reachedOn)
        val all = MilestonePlanner.projectReached(source, date("2036-01-01")).associateBy { it.nodeCode }
        assertEquals(date("2028-09-26"), all.getValue("D1000").reachedOn)
        assertEquals(date("2027-01-01"), all.getValue("Y1").reachedOn)
        assertEquals(date("2029-01-01"), all.getValue("Y3").reachedOn)
        assertEquals(date("2031-01-01"), all.getValue("Y5").reachedOn)
        assertEquals(date("2036-01-01"), all.getValue("Y10").reachedOn)
    }

    @Test fun leap_project_anniversary_uses_calendar_clamping() {
        val events = MilestonePlanner.projectReached(project("2024-02-29"), date("2025-02-28"))
        assertEquals(date("2025-02-28"), events.single { it.nodeCode == "Y1" }.reachedOn)
    }

    @Test fun project_end_before_node_excludes_it_and_end_on_node_keeps_honest_past_copy() {
        assertTrue(MilestonePlanner.projectReached(project(end = "2026-04-09"), date("2036-01-01")).isEmpty())
        val reached = MilestonePlanner.projectReached(project(end = "2026-04-10"), date("2026-04-12")).single()
        assertEquals("D100", reached.nodeCode)
        assertTrue(reached.text.contains("已结束"))
        assertFalse(reached.text.contains("仍在使用"))
        assertTrue(reached.text.contains("2026-04-10"))
    }

    @Test fun future_project_has_no_reached_node_and_invalid_end_is_rejected() {
        assertTrue(MilestonePlanner.projectReached(project("2027-01-01"), date("2026-01-01")).isEmpty())
        assertThrows(IllegalArgumentException::class.java) { MilestonePlanner.projectReached(project(end = "2025-12-31"), date("2026-04-10")) }
    }

    @Test fun annual_prepaid_first_day_has_one_elapsed_day_and_no_milestone() {
        val source = subscription()
        val result = SubscriptionCostEngine.evaluate(SubscriptionSnapshot(Subscription(source.ownerId), source.renewals.map { it.renewal }), date("2026-01-01"))
        assertEquals(365L, result.standardPurchasedDays)
        assertEquals(1L, result.elapsedPaidDays)
        assertTrue(MilestonePlanner.subscriptionReached(source, date("2026-01-01")).isEmpty())
    }

    @Test fun continuous_subscription_first_reaches_100_and_365_on_actual_dates() {
        val all = MilestonePlanner.subscriptionReached(subscription(), date("2026-12-31")).associateBy { it.nodeCode }
        assertEquals(setOf("D100", "D365"), all.keys)
        assertEquals(date("2026-04-10"), all.getValue("D100").reachedOn)
        assertEquals(date("2026-12-31"), all.getValue("D365").reachedOn)
        assertTrue(all.getValue("D365").text.contains("累计付费有效天数"))
        assertFalse(all.getValue("D365").text.contains("连续一年"))
    }

    @Test fun leap_subscription_365_effective_days_precede_project_anniversary() {
        val event = MilestonePlanner.subscriptionReached(subscription("2024-02-29", "2025-03-01"), date("2025-02-28")).single { it.nodeCode == "D365" }
        assertEquals(date("2025-02-27"), event.reachedOn)
    }

    @Test fun paused_dates_are_excluded_and_continued_real_coverage_reaches_365_later() {
        val source = subscription(until = "2027-01-11", states = listOf(state("2026-01-10", SubscriptionOperationState.PAUSED), state("2026-01-20", SubscriptionOperationState.ENABLED)))
        assertFalse(MilestonePlanner.subscriptionReached(source, date("2026-12-31")).any { it.nodeCode == "D365" })
        assertEquals(date("2027-01-10"), MilestonePlanner.subscriptionReached(source, date("2027-01-10")).single { it.nodeCode == "D365" }.reachedOn)
    }

    @Test fun stopped_free_and_uncovered_gaps_never_add_effective_time() {
        val rows = listOf(renewal("2026-01-01", "2026-02-01", id = "first"), renewal("2026-03-01", "2026-05-30", id = "second"))
        val source = subscription(rows = rows, states = listOf(state("2026-01-21", SubscriptionOperationState.STOPPED), state("2026-03-01", SubscriptionOperationState.ENABLED)))
        assertTrue(MilestonePlanner.subscriptionReached(source, date("2026-05-18")).isEmpty())
        assertEquals(date("2026-05-19"), MilestonePlanner.subscriptionReached(source, date("2026-05-20")).single().reachedOn)
    }

    @Test fun future_payment_cannot_unlock_a_threshold_in_the_past() {
        val source = subscription(rows = listOf(renewal("2026-01-01", "2027-01-01", paid = "2026-04-11")))
        assertTrue(MilestonePlanner.subscriptionReached(source, date("2026-04-10")).isEmpty())
        assertEquals(date("2026-04-11"), MilestonePlanner.subscriptionReached(source, date("2026-04-11")).single().reachedOn)
    }

    @Test fun six_subscription_thresholds_are_fixed_and_ordered_without_standard_duration() {
        val source = subscription("2026-01-01", "2037-01-01")
        val events = MilestonePlanner.subscriptionReached(source, date("2036-12-31"))
        assertEquals(listOf("D100", "D365", "D1000", "D1095", "D1825", "D3650"), events.map { it.nodeCode })
        assertEquals(listOf(100L, 365L, 1000L, 1095L, 1825L, 3650L), events.map { it.threshold })
        events.forEach { assertEquals(date("2026-01-01").plusDays(it.threshold - 1), it.reachedOn) }
    }

    @Test fun legal_window_includes_reached_day_and_plus_six_but_not_plus_seven() {
        val source = project(); val mark = MilestoneWatermark(MilestoneOwnerType.PROJECT, source.ownerId, date("2026-04-09"), 0)
        assertEquals(1, projectCandidates(source, "2026-04-10", mark).size)
        assertEquals(1, projectCandidates(source, "2026-04-16", mark).size)
        assertTrue(projectCandidates(source, "2026-04-17", mark).isEmpty())
    }

    @Test fun record_zone_nine_gate_never_consumes_early_morning_eligibility() {
        val source = project(); val mark = MilestoneWatermark(MilestoneOwnerType.PROJECT, source.ownerId, date("2026-04-09"), 0)
        assertTrue(projectCandidates(source, "2026-04-10", mark, time = "08:59:59").isEmpty())
        assertEquals(1, projectCandidates(source, "2026-04-10", mark, time = "09:00:00").size)
        val utc = now("2026-04-10").withZoneSameInstant(java.time.ZoneId.of("UTC"))
        assertTrue(MilestonePlanner.evaluate(listOf(source), emptyList(), utc, listOf(mark), emptySet()).isEmpty())
    }

    @Test fun initial_history_baseline_suppresses_today_and_every_earlier_node_without_receipts() {
        val source = project("2016-01-01"); val mark = MilestonePlanner.projectWatermark(source, date("2026-01-01"))
        assertEquals(63, mark.suppressedMask)
        assertEquals(date("2026-01-01"), mark.cutoffOn)
        assertTrue(projectCandidates(source, "2026-01-01", mark).isEmpty())
    }

    @Test fun initializing_exact_target_day_suppresses_that_day_but_future_node_still_works() {
        val source = project(); val mark = MilestonePlanner.projectWatermark(source, date("2026-04-10"))
        assertTrue(projectCandidates(source, "2026-04-10", mark).isEmpty())
        assertEquals(listOf("Y1"), projectCandidates(source, "2027-01-01", mark).map { it.nodeCode })
    }

    @Test fun correction_keeps_suppressed_bits_when_node_moves_into_the_future_and_cutoff_never_regresses() {
        val original = project(); val mark = MilestonePlanner.projectWatermark(original, date("2026-04-10"))
        val shifted = original.copy(startedOn = date("2026-02-01"))
        val merged = MilestonePlanner.projectWatermark(shifted, date("2026-04-09"), mark)
        assertEquals(mark.cutoffOn, merged.cutoffOn)
        assertEquals(mark.suppressedMask, merged.suppressedMask)
        assertTrue(projectCandidates(shifted, "2026-05-11", merged).isEmpty())
    }

    @Test fun correcting_history_unions_newly_crossed_nodes_and_subscription_watermark_is_typed() {
        val source = project(); val old = MilestoneWatermark(MilestoneOwnerType.PROJECT, source.ownerId, date("2026-04-09"), 0)
        val changed = source.copy(startedOn = date("2025-01-01"))
        val merged = MilestonePlanner.projectWatermark(changed, date("2026-04-10"), old)
        assertEquals(5, merged.suppressedMask)
        val sub = subscription(); val subMark = MilestonePlanner.subscriptionWatermark(sub, date("2026-12-31"))
        assertEquals(MilestoneOwnerType.SUBSCRIPTION, subMark.ownerType)
        assertEquals(3, subMark.suppressedMask)
        assertThrows(IllegalArgumentException::class.java) { MilestonePlanner.projectWatermark(source, date("2026-04-10"), subMark) }
    }

    @Test fun switch_and_receipts_suppress_without_mutating_watermark_or_candidate_identity() {
        val source = project(); val mark = MilestoneWatermark(MilestoneOwnerType.PROJECT, source.ownerId, date("2026-04-09"), 0)
        val event = projectCandidates(source, "2026-04-10", mark).single()
        assertTrue(projectCandidates(source, "2026-04-10", mark, enabled = false).isEmpty())
        assertTrue(projectCandidates(source, "2026-04-10", mark, keys = setOf(event.logicalKey)).isEmpty())
        assertEquals(event.logicalKey, projectCandidates(source, "2026-04-11", mark).single().logicalKey)
        assertEquals(0, mark.suppressedMask)
        assertEquals(date("2026-04-09"), mark.cutoffOn)
    }

    @Test fun same_uuid_cross_type_has_independent_watermark_and_stable_key() {
        val source = project(id = "same"); val sub = subscription(id = "same")
        val marks = listOf(MilestoneWatermark(MilestoneOwnerType.PROJECT, "same", date("2026-04-09"), 0), MilestoneWatermark(MilestoneOwnerType.SUBSCRIPTION, "same", date("2026-04-09"), 0))
        val events = MilestonePlanner.evaluate(listOf(source), listOf(sub), now("2026-04-10"), marks, emptySet())
        assertEquals(2, events.size)
        assertEquals(setOf("MILESTONE:PROJECT:same:D100", "MILESTONE:SUBSCRIPTION:same:D100"), events.map { it.logicalKey }.toSet())
    }

    @Test fun missing_duplicate_and_orphan_watermarks_fail_closed() {
        val source = project(); val mark = MilestoneWatermark(MilestoneOwnerType.PROJECT, source.ownerId, date("2026-04-09"), 0)
        assertThrows(IllegalArgumentException::class.java) { MilestonePlanner.evaluate(listOf(source), emptyList(), now("2026-04-10"), emptyList(), emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { MilestonePlanner.evaluate(listOf(source), emptyList(), now("2026-04-10"), listOf(mark, mark), emptySet()) }
        assertThrows(IllegalArgumentException::class.java) { MilestonePlanner.evaluate(emptyList(), emptyList(), now("2026-04-10"), listOf(mark), emptySet()) }
    }

    @Test fun node_positions_are_six_strict_bits_and_unknown_codes_are_rejected() {
        val projects = listOf("D100", "D1000", "Y1", "Y3", "Y5", "Y10")
        val subscriptions = listOf("D100", "D365", "D1000", "D1095", "D1825", "D3650")
        assertEquals(projects, MilestonePlanner.nodeCodes(MilestoneOwnerType.PROJECT))
        assertEquals(subscriptions, MilestonePlanner.nodeCodes(MilestoneOwnerType.SUBSCRIPTION))
        projects.forEachIndexed { index, code -> assertEquals(1 shl index, MilestonePlanner.nodeBit(MilestoneOwnerType.PROJECT, code)) }
        subscriptions.forEachIndexed { index, code -> assertEquals(1 shl index, MilestonePlanner.nodeBit(MilestoneOwnerType.SUBSCRIPTION, code)) }
        assertThrows(IllegalArgumentException::class.java) { MilestonePlanner.validateNode(MilestoneOwnerType.PROJECT, "D365") }
        assertThrows(IllegalArgumentException::class.java) { MilestonePlanner.validateNode(MilestoneOwnerType.SUBSCRIPTION, "Y1") }
        assertThrows(IllegalArgumentException::class.java) { MilestoneWatermark(MilestoneOwnerType.PROJECT, "id", date("2026-01-01"), 64) }
        assertThrows(IllegalArgumentException::class.java) { MilestoneWatermark(MilestoneOwnerType.PROJECT, "id", date("2026-01-01"), -1) }
    }

    @Test fun invalid_subscription_structure_and_duplicate_source_identity_are_not_silently_filtered() {
        val bad = subscription(rows = listOf(renewal("2026-01-01", "2027-01-01", id = "one"), renewal("2026-01-02", "2027-01-02", id = "two")))
        assertThrows(IllegalArgumentException::class.java) { MilestonePlanner.subscriptionReached(bad, date("2026-04-10")) }
        val source = project(); val mark = MilestoneWatermark(MilestoneOwnerType.PROJECT, source.ownerId, date("2026-04-09"), 0)
        assertThrows(IllegalArgumentException::class.java) { MilestonePlanner.evaluate(listOf(source, source), emptyList(), now("2026-04-10"), listOf(mark), emptySet()) }
    }

    @Test fun very_large_date_range_is_bounded_and_unrepresentable_future_project_nodes_are_skipped() {
        assertTrue(MilestonePlanner.projectReached(ProjectMilestoneSource("far", "边界", LocalDate.MAX.minusDays(20)), LocalDate.MAX).isEmpty())
        val row = ReminderRenewal("wide", Renewal(Money(1), BillingCycle.YEARLY, LocalDate.MIN, LocalDate.MIN, LocalDate.MAX))
        val events = MilestonePlanner.subscriptionReached(RenewalReminderSource("wide", "宽区间", listOf(row)), LocalDate.MAX)
        assertEquals(LocalDate.MIN.plusDays(3649), events.single { it.nodeCode == "D3650" }.reachedOn)
    }
}
