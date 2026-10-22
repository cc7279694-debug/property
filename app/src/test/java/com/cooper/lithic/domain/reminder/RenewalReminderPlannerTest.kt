package com.cooper.lithic.domain.reminder

import com.cooper.lithic.core.model.Money
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RenewalReminderPlannerTest {
    @Test fun missed_candidate_in_early_morning_waits_for_today_nine_instead_of_being_consumed() {
        val early = ZonedDateTime.parse("2026-10-19T08:00+08:00[Asia/Shanghai]")
        assertEquals(early.withHour(9), RenewalReminderPlanner.nextAttemptAt(LocalDate.parse("2026-10-20"), early))
    }
    @Test fun future_candidate_keeps_its_original_date_and_late_daytime_is_immediate() {
        val early = ZonedDateTime.parse("2026-10-16T08:00+08:00[Asia/Shanghai]")
        assertEquals(early.plusDays(1).withHour(9), RenewalReminderPlanner.nextAttemptAt(LocalDate.parse("2026-10-20"), early))
        val late = early.plusDays(2).withHour(15)
        assertEquals(late, RenewalReminderPlanner.nextAttemptAt(LocalDate.parse("2026-10-20"), late))
    }
    private val recordZone = ZoneId.of("Asia/Shanghai")
    private val due = date("2026-10-20")
    private fun date(value: String) = LocalDate.parse(value)
    private fun now(day: String = "2026-10-17", time: String = "09:00:00") =
        ZonedDateTime.parse("${day}T${time}+08:00[Asia/Shanghai]")

    private fun renewal(
        id: String = "payment",
        paid: String = "2026-09-20",
        from: String = "2026-09-20",
        until: String = "2026-10-20",
        reminder: String = until,
    ) = ReminderRenewal(id, Renewal(Money(15_000), BillingCycle.MONTHLY, date(paid), date(from), date(until), date(reminder)))

    private fun state(day: String, value: SubscriptionOperationState) = SubscriptionStateChange(date(day), value)
    private fun source(
        owner: String = "service",
        name: String = "合成订阅",
        rows: List<ReminderRenewal> = listOf(renewal()),
        changes: List<SubscriptionStateChange> = emptyList(),
    ) = RenewalReminderSource(owner, name, rows, changes)

    private fun evaluate(
        sources: List<RenewalReminderSource> = listOf(source()),
        at: ZonedDateTime = now(),
        enabled: Boolean = true,
        cutoff: LocalDate? = null,
        keys: Set<String> = emptySet(),
    ) = RenewalReminderPlanner.evaluate(sources, at, enabled, cutoff, keys)

    @Test fun exactly_three_business_days_before_has_true_boundary_identity_and_copy() {
        val candidate = evaluate().single()
        assertEquals("service", candidate.ownerId)
        assertEquals("payment", candidate.renewalId)
        assertEquals(due, candidate.boundary)
        assertEquals(date("2026-10-17"), candidate.triggerOn)
        assertEquals(3L, candidate.remainingDays)
        assertEquals("RENEWAL:service:2026-10-20", candidate.logicalKey)
        assertTrue(candidate.title.contains("合成订阅"))
        assertTrue(candidate.text.contains("砺石记录中的到期日"))
        assertTrue(candidate.text.contains("2026-10-20"))
        assertTrue(candidate.text.contains("3天"))
        assertTrue(candidate.text.contains("服务商"))
        assertTrue(candidate.text.contains("续费或取消"))
    }

    @Test fun before_candidate_day_or_before_nine_does_not_publish() {
        assertTrue(evaluate(at = now("2026-10-16", "23:59:59")).isEmpty())
        assertTrue(evaluate(at = now(time = "08:59:59")).isEmpty())
        assertEquals(1, evaluate(at = now(time = "09:00:00")).size)
    }

    @Test fun late_first_execution_has_two_real_days_and_next_day_has_one() {
        val twoDays = evaluate(at = now("2026-10-18")).single()
        assertEquals(2L, twoDays.remainingDays)
        assertTrue(twoDays.text.contains("2天"))
        assertFalse(twoDays.text.contains("3天"))
        val oneDay = evaluate(at = now("2026-10-19")).single()
        assertEquals(1L, oneDay.remainingDays)
        assertTrue(oneDay.text.contains("1天"))
        assertTrue(evaluate(at = now("2026-10-18", "08:59:59")).isEmpty())
    }

    @Test fun due_boundary_and_later_never_emit_future_tense_reminder() {
        assertTrue(evaluate(at = now("2026-10-20")).isEmpty())
        assertTrue(evaluate(at = now("2026-10-21")).isEmpty())
    }

    @Test fun recorded_zone_owns_day_and_nine_even_when_travel_zone_differs() {
        val instant = Instant.parse("2026-10-17T01:00:00Z")
        assertEquals(1, evaluate(at = instant.atZone(recordZone)).size)
        assertTrue(evaluate(at = instant.atZone(ZoneId.of("America/Los_Angeles"))).isEmpty())
        assertTrue(evaluate(at = instant.atZone(ZoneOffset.UTC)).isEmpty())
    }

    @Test fun schedule_helper_uses_recorded_local_nine_through_daylight_saving_change() {
        val zone = ZoneId.of("America/New_York")
        val before = RenewalReminderPlanner.scheduledAt(date("2026-11-03"), zone)
        val after = RenewalReminderPlanner.scheduledAt(date("2026-11-04"), zone)
        assertEquals(ZonedDateTime.parse("2026-10-31T09:00-04:00[America/New_York]"), before)
        assertEquals(ZonedDateTime.parse("2026-11-01T09:00-05:00[America/New_York]"), after)
    }

    @Test fun restored_cutoff_excludes_equal_and_past_triggers_but_keeps_future() {
        assertTrue(evaluate(cutoff = date("2026-10-17")).isEmpty())
        assertTrue(evaluate(cutoff = date("2026-10-18")).isEmpty())
        assertEquals(1, evaluate(cutoff = date("2026-10-16")).size)
        assertTrue(evaluate(at = now("2026-10-18"), cutoff = date("2026-10-17")).isEmpty())
    }

    @Test fun renewal_preference_off_suppresses_and_reenable_uses_current_legal_window() {
        assertTrue(evaluate(enabled = false).isEmpty())
        assertEquals(1, evaluate(enabled = true, at = now("2026-10-18")).size)
        assertTrue(evaluate(enabled = true, at = now("2026-10-20")).isEmpty())
    }

    @Test fun successful_logical_receipt_suppresses_exact_boundary_and_other_keys_do_not() {
        assertTrue(evaluate(keys = setOf("RENEWAL:service:2026-10-20")).isEmpty())
        assertEquals(1, evaluate(keys = setOf("RENEWAL:another:2026-10-20", "RENEWAL:service:2026-10-19")).size)
    }

    @Test fun stable_key_and_notification_id_survive_name_and_payment_identity_change() {
        val original = evaluate().single()
        val corrected = evaluate(listOf(source(name = "修正后的名称", rows = listOf(renewal(id = "corrected-payment"))))).single()
        assertEquals(original.logicalKey, corrected.logicalKey)
        assertEquals(original.notificationId, corrected.notificationId)
        assertEquals(820111720, original.notificationId)
        assertEquals("corrected-payment", corrected.renewalId)
        assertTrue(corrected.title.contains("修正后的名称"))
    }

    @Test fun corrected_due_date_removes_old_candidate_and_later_uses_new_boundary() {
        val changed = source(rows = listOf(renewal(reminder = "2026-10-21")))
        assertTrue(evaluate(listOf(changed)).isEmpty())
        val candidate = evaluate(listOf(changed), now("2026-10-18"), keys = setOf("RENEWAL:service:2026-10-20")).single()
        assertEquals("RENEWAL:service:2026-10-21", candidate.logicalKey)
        assertEquals(date("2026-10-21"), candidate.boundary)
        assertEquals("payment", candidate.renewalId)
    }

    @Test fun removed_owner_or_original_payment_is_not_a_candidate() {
        assertTrue(evaluate(emptyList()).isEmpty())
        assertTrue(evaluate(listOf(source(rows = emptyList()))).isEmpty())
        val onlyLater = source(rows = listOf(renewal(id = "later", from = "2026-10-20", until = "2026-11-20")))
        assertTrue(evaluate(listOf(onlyLater)).isEmpty())
    }

    @Test fun confirmed_adjacent_prepaid_successor_suppresses_original_boundary() {
        val input = source(rows = listOf(renewal(), renewal("prepaid", paid = "2026-10-10", from = "2026-10-20", until = "2026-11-20")))
        assertTrue(evaluate(listOf(input)).isEmpty())
    }

    @Test fun q04_successor_starting_in_unpaid_gap_before_reminder_suppresses_old_boundary() {
        val original = renewal(from = "2026-09-15", until = "2026-10-15", reminder = "2026-10-20")
        for (start in listOf("2026-10-15", "2026-10-18", "2026-10-20")) {
            val next = renewal("next", paid = "2026-10-10", from = start, until = "2026-11-20")
            assertTrue("Confirmed successor effective $start", evaluate(listOf(source(rows = listOf(original, next)))).isEmpty())
        }
    }

    @Test fun disconnected_future_successor_after_reminder_does_not_hide_old_boundary() {
        val input = source(rows = listOf(renewal(), renewal("later", paid = "2026-10-10", from = "2026-10-21", until = "2026-11-21")))
        assertEquals("payment", evaluate(listOf(input)).single().renewalId)
    }

    @Test fun payment_not_yet_confirmed_as_of_today_neither_reminds_nor_suppresses() {
        val futureOriginal = source(rows = listOf(renewal(paid = "2026-10-18")))
        assertTrue(evaluate(listOf(futureOriginal)).isEmpty())
        val futureNext = renewal("later", paid = "2026-10-18", from = "2026-10-20", until = "2026-11-20")
        assertEquals("payment", evaluate(listOf(source(rows = listOf(renewal(), futureNext)))).single().renewalId)
    }

    @Test fun already_stopped_suppresses_even_with_paid_coverage() {
        val input = source(changes = listOf(state("2026-10-17", SubscriptionOperationState.STOPPED)))
        assertTrue(evaluate(listOf(input)).isEmpty())
    }

    @Test fun scheduled_stop_before_or_on_due_suppresses_without_changing_effective_state() {
        for (day in listOf("2026-10-18", "2026-10-20")) {
            val input = source(changes = listOf(state(day, SubscriptionOperationState.STOPPED)))
            assertTrue(evaluate(listOf(input)).isEmpty())
            val snapshot = SubscriptionSnapshot(Subscription(input.ownerId), input.renewals.map { it.renewal }, input.stateChanges)
            assertEquals(SubscriptionDisplayStatus.ACTIVE, SubscriptionCostEngine.evaluate(snapshot, date("2026-10-17")).displayStatus)
        }
    }

    @Test fun scheduled_stop_after_due_does_not_suppress_prematurely() {
        val input = source(changes = listOf(state("2026-10-21", SubscriptionOperationState.STOPPED)))
        assertEquals(1, evaluate(listOf(input)).size)
    }

    @Test fun pause_is_not_stop_and_does_not_fabricate_payment() {
        val input = source(changes = listOf(state("2026-10-16", SubscriptionOperationState.PAUSED)))
        val before = input.copy(renewals = input.renewals.toList(), stateChanges = input.stateChanges.toList())
        assertEquals(1, evaluate(listOf(input)).size)
        assertEquals(before, input)
        assertEquals(1, input.renewals.size)
    }

    @Test fun same_identity_reopened_after_historical_stop_is_eligible() {
        val input = source(changes = listOf(
            state("2026-10-10", SubscriptionOperationState.STOPPED),
            state("2026-10-16", SubscriptionOperationState.ENABLED),
        ))
        assertEquals("service", evaluate(listOf(input)).single().ownerId)
    }

    @Test fun stop_superseded_by_confirmed_reopen_before_due_is_not_permanent_stop() {
        val input = source(changes = listOf(
            state("2026-10-18", SubscriptionOperationState.STOPPED),
            state("2026-10-19", SubscriptionOperationState.ENABLED),
        ))
        assertEquals(1, evaluate(listOf(input)).size)
        assertTrue(evaluate(listOf(input), now("2026-10-18")).isEmpty())
        assertEquals(1, evaluate(listOf(input), now("2026-10-19")).size)
    }

    @Test fun q04_paid_coverage_expired_still_has_real_future_reminder_and_unchanged_cost() {
        val input = source(rows = listOf(renewal(paid = "2026-09-15", from = "2026-09-15", until = "2026-10-15", reminder = "2026-10-20")))
        val snapshot = SubscriptionSnapshot(Subscription(input.ownerId), input.renewals.map { it.renewal }, input.stateChanges)
        val before = SubscriptionCostEngine.evaluate(snapshot, date("2026-10-17"))
        assertEquals(SubscriptionDisplayStatus.EXPIRED, before.displayStatus)
        assertEquals(0, before.currentRate.signum())
        assertEquals(30L, before.elapsedPaidDays)
        assertEquals(15_000L, before.confirmedPaidCents)
        assertEquals(3L, evaluate(listOf(input)).single().remainingDays)
        assertEquals(2L, evaluate(listOf(input), now("2026-10-18")).single().remainingDays)
        assertTrue(evaluate(listOf(input), now("2026-10-20")).isEmpty())
        assertEquals(before, SubscriptionCostEngine.evaluate(snapshot, date("2026-10-18")))
    }

    @Test fun full_paid_facts_are_validated_even_if_overlapping_row_is_outside_candidate_window() {
        val overlapping = source(rows = listOf(renewal(), renewal("overlap", from = "2026-10-19", until = "2026-11-20")))
        assertThrows(IllegalArgumentException::class.java) { evaluate(listOf(overlapping)) }
    }

    @Test fun duplicate_state_day_is_rejected_instead_of_arbitrary_last_state() {
        val invalid = source(changes = listOf(
            state("2026-10-16", SubscriptionOperationState.PAUSED),
            state("2026-10-16", SubscriptionOperationState.ENABLED),
        ))
        assertThrows(IllegalArgumentException::class.java) { evaluate(listOf(invalid)) }
    }

    @Test fun duplicate_owner_or_payment_identity_is_rejected_instead_of_silent_dedup() {
        assertThrows(IllegalArgumentException::class.java) { evaluate(listOf(source(), source())) }
        assertThrows(IllegalArgumentException::class.java) { evaluate(listOf(source(), source(owner = "second"))) }
        val invalid = source(rows = listOf(renewal(), renewal(from = "2026-10-20", until = "2026-11-20")))
        assertThrows(IllegalArgumentException::class.java) { evaluate(listOf(invalid)) }
    }

    @Test fun blank_owner_name_or_source_row_identity_cannot_be_published() {
        for (invalid in listOf(source(owner = " "), source(name = " "), source(rows = listOf(renewal(id = " "))))) {
            assertThrows(IllegalArgumentException::class.java) { evaluate(listOf(invalid)) }
        }
        assertThrows(IllegalArgumentException::class.java) { RenewalReminderPlanner.logicalKey("", due) }
        assertThrows(IllegalArgumentException::class.java) { RenewalReminderPlanner.notificationId(" ") }
    }

    @Test fun calendar_underflow_rejected_without_wrapping_or_scheduling_wrong_date() {
        assertThrows(DateTimeException::class.java) { RenewalReminderPlanner.scheduledAt(LocalDate.MIN, recordZone) }
        val invalid = source(rows = listOf(renewal().copy(renewal = renewal().renewal.copy(reminderDueOn = LocalDate.MIN))))
        assertThrows(DateTimeException::class.java) { evaluate(listOf(invalid)) }
    }

    @Test fun stable_id_handles_minimum_signed_hash_without_negative_overflow() {
        assertEquals(Int.MIN_VALUE, "polygenelubricants".hashCode())
        assertEquals(0, RenewalReminderPlanner.notificationId("polygenelubricants"))
        assertEquals(820111720, RenewalReminderPlanner.notificationId("RENEWAL:service:2026-10-20"))
    }

    @Test fun independent_owners_keep_distinct_keys_and_deterministic_candidate_order() {
        val a = source(owner = "a", rows = listOf(renewal(id = "a-payment")))
        val b = source(owner = "b", rows = listOf(renewal(id = "b-payment")))
        val first = evaluate(listOf(a, b))
        assertEquals(first, evaluate(listOf(b, a)))
        assertEquals(listOf("RENEWAL:a:2026-10-20", "RENEWAL:b:2026-10-20"), first.map { it.logicalKey })
        assertEquals(2, first.map { it.notificationId }.distinct().size)
    }
}
