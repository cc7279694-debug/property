package com.cooper.lithic.data.repository

import com.cooper.lithic.core.model.Money
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MilestoneHistoryChangesTest {
    private val today = LocalDate.of(2026, 4, 10)
    private val start = LocalDate.of(2026, 1, 1)

    @Test fun changed_project_start_is_historical_even_when_moved_forward() {
        assertTrue(MilestoneHistoryChanges.project(start, null, start.plusDays(1), null, today))
        assertTrue(MilestoneHistoryChanges.project(today.plusDays(5), null, today.plusDays(4), null, today))
    }

    @Test fun ordinary_end_today_does_not_swallow_an_end_day_milestone() {
        assertFalse(MilestoneHistoryChanges.project(start, null, start, today, today))
    }

    @Test fun backdated_end_and_past_end_correction_require_suppression() {
        assertTrue(MilestoneHistoryChanges.project(start, null, start, today.minusDays(1), today))
        assertTrue(MilestoneHistoryChanges.project(start, today.minusDays(1), start, today, today))
    }

    @Test fun metadata_and_future_end_changes_are_not_history() {
        assertFalse(MilestoneHistoryChanges.project(start, null, start, null, today))
        assertFalse(MilestoneHistoryChanges.project(start, today.plusDays(2), start, today.plusDays(4), today))
    }

    @Test fun existing_end_today_correction_or_withdrawal_is_history() {
        assertTrue(MilestoneHistoryChanges.project(start, today, start, today.plusDays(1), today))
        assertTrue(MilestoneHistoryChanges.project(start, today, start, null, today))
        assertFalse(MilestoneHistoryChanges.project(start, today.plusDays(1), start, today, today))
    }

    @Test fun backfilled_coverage_is_history_but_today_and_future_renewal_are_not() {
        assertTrue(MilestoneHistoryChanges.renewal(null, payment(start), today))
        assertFalse(MilestoneHistoryChanges.renewal(null, payment(today), today))
        assertFalse(MilestoneHistoryChanges.renewal(null, payment(today.plusDays(1)), today))
    }

    @Test fun amount_cycle_and_reminder_only_corrections_preserve_legal_candidate() {
        val before = payment(start)
        assertFalse(MilestoneHistoryChanges.renewal(before, before.copy(amount = Money(200)), today))
        assertFalse(MilestoneHistoryChanges.renewal(before, before.copy(cycle = BillingCycle.YEARLY, standardDays = 365), today))
        assertFalse(MilestoneHistoryChanges.renewal(before, before.copy(reminderDueOn = today.plusDays(2)), today))
    }

    @Test fun historical_coverage_or_payment_day_correction_is_history() {
        val before = payment(start)
        assertTrue(MilestoneHistoryChanges.renewal(before, before.copy(effectiveFrom = start.plusDays(1)), today))
        assertTrue(MilestoneHistoryChanges.renewal(before, before.copy(paidUntilExclusive = today.plusDays(1)), today))
        assertTrue(MilestoneHistoryChanges.renewal(before, before.copy(paidOn = today.minusDays(1)), today))
    }

    @Test fun removing_historical_coverage_is_history_but_removing_future_is_not() {
        assertTrue(MilestoneHistoryChanges.renewal(payment(start), null, today))
        assertFalse(MilestoneHistoryChanges.renewal(payment(today.plusDays(1)), null, today))
    }

    @Test fun current_and_future_coverage_corrections_do_not_rewrite_past_days() {
        val before = payment(today)
        assertFalse(MilestoneHistoryChanges.renewal(before, before.copy(paidUntilExclusive = today.plusDays(45)), today))
        assertFalse(MilestoneHistoryChanges.renewal(before, before, today))
    }

    @Test fun extending_only_future_end_of_existing_coverage_preserves_today_candidate() {
        val before = payment(start).copy(paidUntilExclusive = LocalDate.of(2027, 1, 1))
        val after = before.copy(paidUntilExclusive = LocalDate.of(2027, 2, 1))
        assertFalse(MilestoneHistoryChanges.renewal(before, after, today))
    }

    @Test fun not_yet_confirmed_future_payment_changes_do_not_rewrite_visible_past_days() {
        val before = payment(start).copy(paidOn = today.plusDays(1), paidUntilExclusive = LocalDate.of(2027, 1, 1))
        val after = before.copy(paidOn = today.plusDays(2), effectiveFrom = start.plusDays(1),
            paidUntilExclusive = LocalDate.of(2027, 2, 1))
        assertFalse(MilestoneHistoryChanges.renewal(before, after, today))
    }

    @Test fun ordinary_today_state_change_does_not_advance_watermark() {
        assertFalse(MilestoneHistoryChanges.state(null, state(today), today))
        assertFalse(MilestoneHistoryChanges.state(null, state(today.plusDays(1)), today))
        assertTrue(MilestoneHistoryChanges.state(null, state(today.minusDays(1)), today))
    }

    @Test fun correction_to_existing_effective_state_is_history() {
        val before = state(today)
        assertTrue(MilestoneHistoryChanges.state(before, before.copy(state = SubscriptionOperationState.ENABLED), today))
        assertTrue(MilestoneHistoryChanges.state(state(today.minusDays(1)), state(today.plusDays(1)), today))
    }

    @Test fun future_state_plan_or_unchanged_state_does_not_advance_watermark() {
        val before = state(today.plusDays(1))
        assertFalse(MilestoneHistoryChanges.state(before, state(today.plusDays(2)), today))
        assertFalse(MilestoneHistoryChanges.state(before, null, today))
        assertFalse(MilestoneHistoryChanges.state(state(today), state(today), today))
    }

    private fun payment(from: LocalDate) = Renewal(
        Money(100), BillingCycle.MONTHLY, today, from, from.plusDays(30),
    )

    private fun state(on: LocalDate) = SubscriptionStateChange(on, SubscriptionOperationState.PAUSED)
}
