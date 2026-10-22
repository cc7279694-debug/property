package com.cooper.lithic.data.repository

import com.cooper.lithic.core.model.Money
import com.cooper.lithic.data.settings.ReminderPreferences
import com.cooper.lithic.domain.reminder.ReminderRenewal
import com.cooper.lithic.domain.reminder.RenewalReminderCandidate
import com.cooper.lithic.domain.reminder.RenewalReminderPlanner
import com.cooper.lithic.domain.reminder.RenewalReminderSource
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import com.cooper.lithic.platform.notification.ReminderPublisher
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M20ReminderDispatchTest {
    private val due = LocalDate.parse("2026-10-20")
    private fun source(owner: String = "service", row: String = "payment") = RenewalReminderSource(owner, "合成订阅", listOf(
        ReminderRenewal(row, Renewal(Money(15_000), BillingCycle.MONTHLY,
            LocalDate.parse("2026-09-20"), LocalDate.parse("2026-09-20"), due)),
    ))

    /** Only the platform and persistent receipt port are substituted. The dispatch,
     * revision, planner and generation mutex exercised below are actual production code. */
    private class Publisher(private val events: MutableList<String>) : ReminderPublisher {
        var allowed = true
        var returnsSuccess = true
        var failure: Exception? = null
        var onCheck: () -> Unit = {}
        var onPublish: (RenewalReminderCandidate) -> Unit = {}
        val attempts = mutableListOf<RenewalReminderCandidate>()
        override fun canPublish(): Boolean {
            events += "permission"
            onCheck()
            return allowed
        }
        override fun publish(candidate: RenewalReminderCandidate): Boolean {
            events += "publish"
            attempts += candidate
            onPublish(candidate)
            failure?.let { throw it }
            return returnsSuccess
        }
    }

    private inner class Harness(val original: RenewalReminderSource = source()) {
        val coordinator = LocalWriteCoordinator()
        var current = true
        var instant = Instant.parse("2026-10-17T01:00:00Z")
        val events = mutableListOf<String>()
        val publisher = Publisher(events)
        val receipts = mutableListOf<Pair<RenewalReminderCandidate, Instant>>()
        var reads = 0
        var input = ReminderDispatchInput(listOf(original), ReminderPreferences(), emptySet(), ZoneId.of("Asia/Shanghai"))
        var onRead: suspend () -> Unit = {}
        var onRecord: suspend (RenewalReminderCandidate, Instant) -> Unit = { _, _ -> }
        val key = RenewalReminderPlanner.logicalKey(original.ownerId, due)
        val renewalId = original.renewals.single().id
        val revision = ReminderTaskRevision.of(original)

        suspend fun execute(
            expectedRevision: String = revision,
            logicalKey: String = key,
            rowId: String = renewalId,
        ): ReminderDispatchResult = ReminderDispatch(
            coordinator, { current }, {
                events += "read"
                reads++
                onRead()
                input.copy(successfulKeys = input.successfulKeys + receipts.map { it.first.logicalKey })
            }, { candidate, at ->
                events += "record"
                onRecord(candidate, at)
                receipts += candidate to at
            }, publisher, { instant },
        ).execute(logicalKey, rowId, expectedRevision)

        fun replaceSource(changed: RenewalReminderSource) { input = input.copy(sources = listOf(changed)) }
        fun noPublicationOrReceipt() {
            assertTrue(publisher.attempts.isEmpty())
            assertTrue(receipts.isEmpty())
        }
    }

    @Test fun permission_denied_does_not_publish_or_record_false_success() = runBlocking {
        val h = Harness(); h.publisher.allowed = false
        assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute())
        h.noPublicationOrReceipt()
        assertEquals(listOf("read", "permission"), h.events)
    }

    @Test fun preference_off_suppresses_before_platform_permission_or_publication() = runBlocking {
        val h = Harness(); h.input = h.input.copy(preferences = ReminderPreferences(renewalEnabled = false))
        assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute())
        h.noPublicationOrReceipt()
        assertEquals(listOf("read"), h.events)
    }

    @Test fun permission_revoked_inside_publish_returns_false_without_receipt() = runBlocking {
        val h = Harness(); h.publisher.returnsSuccess = false
        assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute())
        assertEquals(1, h.publisher.attempts.size)
        assertTrue(h.receipts.isEmpty())
        assertFalse(h.events.contains("record"))
    }

    @Test fun publisher_exception_propagates_without_receipt_and_retry_rechecks_facts() = runBlocking {
        val h = Harness(); val problem = IOException("synthetic publisher failure")
        h.publisher.failure = problem
        assertEquals(problem, runCatching { h.execute() }.exceptionOrNull())
        assertTrue(h.receipts.isEmpty())
        h.publisher.failure = null
        assertEquals(ReminderDispatchResult.PUBLISHED, h.execute())
        assertEquals(2, h.reads)
        assertEquals(2, h.publisher.attempts.size)
        assertEquals(1, h.receipts.size)
    }

    @Test fun successful_api_call_precedes_receipt_and_timestamp_means_after_publication() = runBlocking {
        val h = Harness()
        val publishedAt = h.instant.plusSeconds(7)
        h.publisher.onPublish = {
            assertTrue(h.receipts.isEmpty())
            h.instant = publishedAt
        }
        assertEquals(ReminderDispatchResult.PUBLISHED, h.execute())
        assertEquals(listOf("read", "permission", "publish", "record"), h.events)
        assertEquals(h.publisher.attempts.single(), h.receipts.single().first)
        assertEquals(publishedAt, h.receipts.single().second)
    }

    @Test fun normal_retry_reads_receipt_and_does_not_publish_same_key_again() = runBlocking {
        val h = Harness()
        assertEquals(ReminderDispatchResult.PUBLISHED, h.execute())
        assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute())
        assertEquals(2, h.reads)
        assertEquals(1, h.publisher.attempts.size)
        assertEquals(1, h.receipts.size)
    }

    @Test fun concurrent_dispatch_instances_share_actual_coordinator_and_publish_once() = runBlocking {
        val h = Harness(); val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        h.onRead = { if (h.reads == 1) { entered.complete(Unit); release.await() } }
        val first = async { h.execute() }
        withTimeout(3_000) { entered.await() }
        val second = async { h.execute() }
        yield()
        assertEquals(1, h.reads)
        assertFalse(second.isCompleted)
        release.complete(Unit)
        assertEquals(ReminderDispatchResult.PUBLISHED, withTimeout(3_000) { first.await() })
        assertEquals(ReminderDispatchResult.SUPPRESSED, withTimeout(3_000) { second.await() })
        assertEquals(1, h.publisher.attempts.size)
        assertEquals(1, h.receipts.size)
    }

    @Test fun old_generation_rejected_without_reading_its_storage() = runBlocking {
        val h = Harness(); h.current = false
        assertEquals(ReminderDispatchResult.OLD_GENERATION, h.execute())
        assertEquals(0, h.reads)
        h.noPublicationOrReceipt()
    }

    @Test fun generation_changed_while_reading_cannot_publish_old_snapshot() = runBlocking {
        val h = Harness(); h.onRead = { h.current = false }
        assertEquals(ReminderDispatchResult.OLD_GENERATION, h.execute())
        h.noPublicationOrReceipt()
    }

    @Test fun generation_lost_during_permission_check_is_rechecked_before_publish() = runBlocking {
        val h = Harness(); h.publisher.onCheck = { h.current = false }
        assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute())
        h.noPublicationOrReceipt()
    }

    @Test fun already_saved_receipt_blocks_publication_even_if_queued_task_still_exists() = runBlocking {
        val h = Harness(); h.input = h.input.copy(successfulKeys = setOf(h.key))
        assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute())
        h.noPublicationOrReceipt()
    }

    @Test fun removed_owner_or_payment_and_wrong_original_key_are_suppressed() = runBlocking {
        for (missingOwner in listOf(true, false)) {
            val h = Harness()
            h.input = h.input.copy(sources = if (missingOwner) emptyList() else listOf(h.original.copy(renewals = emptyList())))
            assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute())
            h.noPublicationOrReceipt()
        }
        val h = Harness()
        assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute(logicalKey = "RENEWAL:another:2026-10-20"))
        assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute(rowId = "removed-payment"))
        h.noPublicationOrReceipt()
    }

    @Test fun same_payment_id_amount_or_cycle_correction_rejects_queued_revision() = runBlocking {
        val original = source()
        val oldRow = original.renewals.single()
        for (changed in listOf(oldRow.renewal.copy(amount = Money(18_000)),
            oldRow.renewal.copy(cycle = BillingCycle.QUARTERLY, standardDays = 90))) {
            val h = Harness(original)
            h.replaceSource(original.copy(renewals = listOf(oldRow.copy(renewal = changed))))
            assertNotEquals(h.revision, ReminderTaskRevision.of(h.input.sources.single()))
            assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute())
            h.noPublicationOrReceipt()
        }
    }

    @Test fun same_payment_id_correction_of_any_real_date_rejects_queued_revision() = runBlocking {
        val original = source(); val oldRow = original.renewals.single(); val payment = oldRow.renewal
        val correctedDates = listOf(
            payment.copy(paidOn = payment.paidOn.minusDays(1)),
            payment.copy(effectiveFrom = payment.effectiveFrom.plusDays(1)),
            payment.copy(paidUntilExclusive = payment.paidUntilExclusive.plusDays(1)),
            payment.copy(reminderDueOn = payment.reminderDueOn.plusDays(1)),
        )
        for (changed in correctedDates) {
            val h = Harness(original)
            h.replaceSource(original.copy(renewals = listOf(oldRow.copy(renewal = changed))))
            assertNotEquals(h.revision, ReminderTaskRevision.of(h.input.sources.single()))
            assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute())
            h.noPublicationOrReceipt()
        }
    }

    @Test fun state_correction_invalidates_old_revision_even_when_pause_still_allows_new_candidate() = runBlocking {
        val h = Harness()
        val changed = h.original.copy(stateChanges = listOf(SubscriptionStateChange(
            LocalDate.parse("2026-10-16"), SubscriptionOperationState.PAUSED)))
        h.replaceSource(changed)
        assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute())
        h.noPublicationOrReceipt()
        assertEquals(ReminderDispatchResult.PUBLISHED, h.execute(expectedRevision = ReminderTaskRevision.of(changed)))
        assertEquals(1, h.receipts.size)
    }

    @Test fun corrected_name_cannot_leak_old_notification_copy() = runBlocking {
        val h = Harness(); val changed = h.original.copy(name = "修正后的合成订阅")
        h.replaceSource(changed)
        assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute())
        assertEquals(ReminderDispatchResult.PUBLISHED, h.execute(expectedRevision = ReminderTaskRevision.of(changed)))
        assertTrue(h.publisher.attempts.single().title.contains(changed.name))
    }

    @Test fun revision_is_order_independent_but_includes_full_future_facts() {
        val original = source()
        val later = original.renewals.single().copy(id = "future-payment", renewal = original.renewals.single().renewal.copy(
            effectiveFrom = LocalDate.parse("2026-11-01"), paidUntilExclusive = LocalDate.parse("2026-12-01"),
            reminderDueOn = LocalDate.parse("2026-12-01")))
        val states = listOf(SubscriptionStateChange(LocalDate.parse("2026-10-14"), SubscriptionOperationState.PAUSED),
            SubscriptionStateChange(LocalDate.parse("2026-10-15"), SubscriptionOperationState.ENABLED))
        val complete = original.copy(renewals = original.renewals + later, stateChanges = states)
        assertEquals(ReminderTaskRevision.of(complete), ReminderTaskRevision.of(complete.copy(
            renewals = complete.renewals.reversed(), stateChanges = states.reversed())))
        assertNotEquals(ReminderTaskRevision.of(original), ReminderTaskRevision.of(complete))
        assertTrue(ReminderTaskRevision.of(complete).matches(Regex("[a-f0-9]{64}")))
    }

    @Test fun restore_cutoff_is_rechecked_and_does_not_fabricate_historical_receipt() = runBlocking {
        val h = Harness()
        h.input = h.input.copy(preferences = ReminderPreferences(importCutoff = LocalDate.parse("2026-10-17")))
        assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute())
        h.noPublicationOrReceipt()
        h.input = h.input.copy(preferences = ReminderPreferences(importCutoff = LocalDate.parse("2026-10-16")))
        assertEquals(ReminderDispatchResult.PUBLISHED, h.execute())
        assertEquals(1, h.receipts.size)
    }

    @Test fun dispatch_uses_fresh_record_zone_day_for_late_copy_and_expiry() = runBlocking {
        val h = Harness(); h.instant = Instant.parse("2026-10-18T01:00:00Z")
        assertEquals(ReminderDispatchResult.PUBLISHED, h.execute())
        assertEquals(2L, h.publisher.attempts.single().remainingDays)
        assertTrue(h.publisher.attempts.single().text.contains("2天"))
        val expired = Harness(); expired.instant = Instant.parse("2026-10-20T01:00:00Z")
        assertEquals(ReminderDispatchResult.SUPPRESSED, expired.execute())
        expired.noPublicationOrReceipt()
    }

    @Test fun delayed_execution_before_nine_retains_retry_eligibility_without_receipt() = runBlocking {
        val h = Harness(); h.instant = Instant.parse("2026-10-19T00:00:00Z")
        assertEquals("WAITING_FOR_NINE", h.execute().name)
        h.noPublicationOrReceipt()
    }

    @Test fun permission_check_crossing_due_boundary_cannot_publish_stale_candidate() = runBlocking {
        val h = Harness()
        h.publisher.onCheck = { h.instant = Instant.parse("2026-10-20T01:00:00Z") }
        assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute())
        h.noPublicationOrReceipt()
    }

    @Test fun permission_check_crossing_business_day_uses_fresh_remaining_days() = runBlocking {
        val h = Harness()
        h.publisher.onCheck = { h.instant = Instant.parse("2026-10-18T01:00:00Z") }
        assertEquals(ReminderDispatchResult.PUBLISHED, h.execute())
        assertEquals(2L, h.publisher.attempts.single().remainingDays)
        assertTrue(h.publisher.attempts.single().text.contains("还剩2天"))
    }

    @Test fun cancellation_while_waiting_for_coordinator_does_not_read_or_publish() = runBlocking {
        val h = Harness(); val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val holder = launch { h.coordinator.withWrite { entered.complete(Unit); release.await() } }
        withTimeout(3_000) { entered.await() }
        val worker = launch { h.execute() }
        yield()
        worker.cancelAndJoin()
        release.complete(Unit)
        holder.join()
        assertEquals(0, h.reads)
        h.noPublicationOrReceipt()
    }

    @Test fun cancellation_known_after_read_must_not_publish_or_record() = runBlocking {
        val h = Harness()
        h.onRead = { currentCoroutineContext()[Job]!!.cancel() }
        val worker = launch { h.execute() }
        withTimeout(3_000) { worker.join() }
        assertTrue(worker.isCancelled)
        h.noPublicationOrReceipt()
    }

    @Test fun cancellation_during_permission_check_must_be_observed_before_publish() = runBlocking {
        val h = Harness(); var executingJob: Job? = null
        h.onRead = { executingJob = currentCoroutineContext()[Job]!! }
        h.publisher.onCheck = { executingJob!!.cancel() }
        val worker = launch { h.execute() }
        withTimeout(3_000) { worker.join() }
        assertTrue(worker.isCancelled)
        h.noPublicationOrReceipt()
    }

    @Test fun cancellation_after_successful_publish_keeps_receipt_noncancellable_and_lease_held() = runBlocking {
        val h = Harness(); var executingJob: Job? = null
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        h.onRead = { executingJob = currentCoroutineContext()[Job]!! }
        h.publisher.onPublish = { executingJob!!.cancel() }
        h.onRecord = { _, _ ->
            assertTrue(currentCoroutineContext()[Job]!!.isActive)
            entered.complete(Unit)
            release.await()
        }
        val worker = launch { h.execute() }
        withTimeout(3_000) { entered.await() }
        val nextGenerationBoundary = async { h.coordinator.withWrite { "lease finished" } }
        yield()
        assertFalse(nextGenerationBoundary.isCompleted)
        assertEquals(1, h.publisher.attempts.size)
        assertTrue(h.receipts.isEmpty())
        release.complete(Unit)
        withTimeout(3_000) { worker.join() }
        assertTrue(worker.isCancelled)
        assertEquals(1, h.receipts.size)
        assertEquals("lease finished", withTimeout(3_000) { nextGenerationBoundary.await() })
    }

    @Test fun receipt_write_failure_exposes_limited_retry_window_with_same_key_and_system_id() = runBlocking {
        val h = Harness(); val failure = IOException("synthetic receipt write failed")
        h.onRecord = { _, _ -> throw failure }
        val reported = checkNotNull(runCatching { h.execute() }.exceptionOrNull())
        assertTrue(reported is IOException)
        assertEquals(failure.message, reported.message)
        // Coroutine stacktrace recovery may copy an IOException and retain the original
        // as its cause. Preserve failure semantics without requiring object identity.
        val rootCause = generateSequence(reported) { it.cause }.last()
        assertTrue(rootCause is IOException)
        assertEquals(failure.message, rootCause.message)
        assertEquals(1, h.publisher.attempts.size)
        assertTrue(h.receipts.isEmpty())
        h.onRecord = { _, _ -> }
        assertEquals(ReminderDispatchResult.PUBLISHED, h.execute())
        assertEquals(2, h.publisher.attempts.size)
        assertEquals(h.publisher.attempts[0].logicalKey, h.publisher.attempts[1].logicalKey)
        assertEquals(h.publisher.attempts[0].notificationId, h.publisher.attempts[1].notificationId)
        assertEquals(1, h.receipts.size)
        assertEquals(ReminderDispatchResult.SUPPRESSED, h.execute())
        assertEquals(2, h.publisher.attempts.size)
    }

    @Test fun distinct_logical_tags_survive_a_deterministic_integer_id_collision() = runBlocking {
        val a = source("Aa", "a-payment"); val b = source("BB", "b-payment")
        val h = Harness(a); h.input = h.input.copy(sources = listOf(a, b))
        val keyA = RenewalReminderPlanner.logicalKey(a.ownerId, due)
        val keyB = RenewalReminderPlanner.logicalKey(b.ownerId, due)
        assertNotEquals(keyA, keyB)
        assertEquals(RenewalReminderPlanner.notificationId(keyA), RenewalReminderPlanner.notificationId(keyB))
        assertEquals(ReminderDispatchResult.PUBLISHED, h.execute())
        assertEquals(ReminderDispatchResult.PUBLISHED, h.execute(ReminderTaskRevision.of(b), keyB, "b-payment"))
        assertEquals(setOf(keyA, keyB), h.publisher.attempts.map { it.logicalKey }.toSet())
        assertEquals(2, h.receipts.size)
    }
}
