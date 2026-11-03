package com.cooper.lithic.feature.settings

import com.cooper.lithic.data.settings.ReminderPreferences
import com.cooper.lithic.platform.notification.EnqueueStatus
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

/** UI tickets and owner checks are tested with a bounded fake coordinator, not Android WorkManager. */
class NotificationSchedulingFeedbackTest {
    private suspend fun ready(controller: NotificationSettingsController) =
        withTimeout(3_000) { controller.state.first { !it.loading && it.preferences != null } }

    @Test fun saved_preference_is_kept_when_pending_enqueue_fails() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val events = MutableStateFlow(EnqueueStatus.PENDING)
        var saved = ReminderPreferences()
        var submissions = 0
        try {
            val controller = NotificationSettingsController(scope, { saved }, { renewal, milestone ->
                saved.copy(renewalEnabled = renewal ?: saved.renewalEnabled,
                    milestoneEnabled = milestone ?: saved.milestoneEnabled).also { saved = it }
            }, schedulePort = { submissions++; events })
            ready(controller)
            controller.setRenewalEnabled(false)
            val pending = withTimeout(3_000) { controller.state.first {
                !it.saving && it.preferences?.renewalEnabled == false && it.scheduleStatus == EnqueueStatus.PENDING
            } }
            assertFalse(pending.preferences!!.renewalEnabled)
            assertEquals(1, submissions)
            events.value = EnqueueStatus.FAILED
            val failed = withTimeout(3_000) { controller.state.first { it.scheduleStatus == EnqueueStatus.FAILED } }
            assertFalse(failed.preferences!!.renewalEnabled)
            assertFalse(saved.renewalEnabled)
            assertEquals(1, submissions)
        } finally { scope.cancel() }
    }

    @Test fun unknown_and_cancelled_are_not_enqueued_and_do_not_trigger_retry() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val events = MutableStateFlow(EnqueueStatus.PENDING)
        var submissions = 0
        try {
            val controller = NotificationSettingsController(scope, { ReminderPreferences() },
                { _, _ -> ReminderPreferences() }, schedulePort = { submissions++; events })
            ready(controller); controller.requestScheduleOnResume(); yield()
            events.value = EnqueueStatus.UNKNOWN
            withTimeout(3_000) { controller.state.first { it.scheduleStatus == EnqueueStatus.UNKNOWN } }
            assertEquals(1, submissions)
            events.value = EnqueueStatus.CANCELLED
            withTimeout(3_000) { controller.state.first { it.scheduleStatus == EnqueueStatus.CANCELLED } }
            assertEquals(1, submissions)
            assertTrue(controller.state.value.preferences!!.renewalEnabled)
        } finally { scope.cancel() }
    }

    @Test fun new_ui_ticket_blocks_older_result_after_repeated_triggers() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val old = MutableStateFlow(EnqueueStatus.PENDING)
        val current = MutableStateFlow(EnqueueStatus.PENDING)
        var submissions = 0
        try {
            val controller = NotificationSettingsController(scope, { ReminderPreferences() },
                { _, _ -> ReminderPreferences() }, schedulePort = { if (submissions++ == 0) old else current })
            ready(controller); controller.requestScheduleOnResume(); yield()
            controller.requestScheduleOnResume(); yield()
            old.value = EnqueueStatus.FAILED; yield()
            assertEquals(EnqueueStatus.PENDING, controller.state.value.scheduleStatus)
            current.value = EnqueueStatus.ENQUEUED
            withTimeout(3_000) { controller.state.first { it.scheduleStatus == EnqueueStatus.ENQUEUED } }
            assertEquals(2, submissions)
        } finally { scope.cancel() }
    }

    @Test fun later_saved_setting_cannot_be_overwritten_by_earlier_enqueue_result(): Unit = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val old = MutableStateFlow(EnqueueStatus.PENDING)
        val current = MutableStateFlow(EnqueueStatus.PENDING)
        var saved = ReminderPreferences(); var submissions = 0
        try {
            val controller = NotificationSettingsController(scope, { saved }, { renewal, milestone ->
                saved.copy(renewalEnabled = renewal ?: saved.renewalEnabled,
                    milestoneEnabled = milestone ?: saved.milestoneEnabled).also { saved = it }
            }, schedulePort = { if (submissions++ == 0) old else current })
            ready(controller)
            controller.setRenewalEnabled(false)
            withTimeout(3_000) { controller.state.first { !it.saving && it.scheduleStatus == EnqueueStatus.PENDING } }
            controller.setRenewalEnabled(true)
            withTimeout(3_000) { controller.state.first { !it.saving && it.preferences?.renewalEnabled == true && submissions == 2 } }
            old.value = EnqueueStatus.FAILED; yield()
            assertEquals(EnqueueStatus.PENDING, controller.state.value.scheduleStatus)
            assertTrue(controller.state.value.preferences!!.renewalEnabled)
            current.value = EnqueueStatus.ENQUEUED
            withTimeout(3_000) { controller.state.first { it.scheduleStatus == EnqueueStatus.ENQUEUED } }
        } finally { scope.cancel() }
    }

    @Test fun retired_owner_ignores_late_status_and_reopened_owner_is_independent(): Unit = runBlocking {
        val firstScope = CoroutineScope(coroutineContext + SupervisorJob())
        val nextScope = CoroutineScope(coroutineContext + SupervisorJob())
        val old = MutableStateFlow(EnqueueStatus.PENDING)
        val current = MutableStateFlow(EnqueueStatus.PENDING)
        var firstOwnerActive = true
        try {
            val first = NotificationSettingsController(firstScope, { ReminderPreferences() },
                { _, _ -> ReminderPreferences() }, schedulePort = { old }, isOwnerActive = { firstOwnerActive })
            ready(first); first.requestScheduleOnResume(); yield()
            firstOwnerActive = false
            val next = NotificationSettingsController(nextScope, { ReminderPreferences() },
                { _, _ -> ReminderPreferences() }, schedulePort = { current })
            ready(next); next.requestScheduleOnResume(); yield()
            old.value = EnqueueStatus.FAILED; yield()
            assertEquals(EnqueueStatus.PENDING, first.state.value.scheduleStatus)
            assertEquals(EnqueueStatus.PENDING, next.state.value.scheduleStatus)
            current.value = EnqueueStatus.ENQUEUED
            withTimeout(3_000) { next.state.first { it.scheduleStatus == EnqueueStatus.ENQUEUED } }
        } finally { firstScope.cancel(); nextScope.cancel() }
    }

    @Test fun refresh_preserves_unknown_ticket_and_does_not_enqueue_again() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val operation = MutableStateFlow(EnqueueStatus.PENDING)
        var submissions = 0
        try {
            val controller = NotificationSettingsController(scope, { ReminderPreferences() },
                { _, _ -> ReminderPreferences() }, schedulePort = { submissions++; operation })
            ready(controller); controller.requestScheduleOnResume(); yield()
            operation.value = EnqueueStatus.UNKNOWN
            withTimeout(3_000) { controller.state.first { it.scheduleStatus == EnqueueStatus.UNKNOWN } }
            controller.refresh(); ready(controller)
            assertEquals(EnqueueStatus.UNKNOWN, controller.state.value.scheduleStatus)
            assertEquals(1, submissions)
            operation.value = EnqueueStatus.FAILED
            withTimeout(3_000) { controller.state.first { it.scheduleStatus == EnqueueStatus.FAILED } }
            assertEquals(1, submissions)
        } finally { scope.cancel() }
    }

    @Test fun rebind_same_lease_is_noop_and_does_not_request_or_invalidate_existing_ticket() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val lease = Any()
        val updates = MutableStateFlow(EnqueueStatus.PENDING)
        var submissions = 0; var replacementInvoked = false
        try {
            val controller = NotificationSettingsController(scope, { ReminderPreferences() },
                { _, _ -> ReminderPreferences() }, schedulePort = { submissions++; updates },
                bindingIdentity = lease)
            ready(controller); controller.requestScheduleOnResume(); yield()
            assertFalse(controller.rebindSchedule(lease, {
                replacementInvoked = true; MutableStateFlow(EnqueueStatus.FAILED)
            }, { true }))
            assertEquals(1, submissions)
            assertFalse(replacementInvoked)
            updates.value = EnqueueStatus.ENQUEUED
            withTimeout(3_000) { controller.state.first { it.scheduleStatus == EnqueueStatus.ENQUEUED } }
            assertEquals(1, submissions)
        } finally { scope.cancel() }
    }

    @Test fun retained_controller_rebind_rejects_old_lease_results_even_for_same_store() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val old = MutableStateFlow(EnqueueStatus.PENDING)
        val current = MutableStateFlow(EnqueueStatus.PENDING)
        var oldActive = true
        var currentActive = true
        try {
            val controller = NotificationSettingsController(scope, { ReminderPreferences() },
                { _, _ -> ReminderPreferences() }, schedulePort = { old }, isOwnerActive = { oldActive })
            ready(controller); controller.requestScheduleOnResume(); yield()
            oldActive = false
            controller.rebindSchedule(Any(), { current }, { currentActive })
            ready(controller); controller.requestScheduleOnResume(); yield()
            old.value = EnqueueStatus.FAILED; yield()
            assertEquals(EnqueueStatus.PENDING, controller.state.value.scheduleStatus)
            current.value = EnqueueStatus.ENQUEUED
            withTimeout(3_000) { controller.state.first { it.scheduleStatus == EnqueueStatus.ENQUEUED } }
            currentActive = false
            current.value = EnqueueStatus.FAILED; yield()
            assertEquals(EnqueueStatus.ENQUEUED, controller.state.value.scheduleStatus)
        } finally { scope.cancel() }
    }

    @Test fun rebind_during_save_blocks_duplicate_write_until_old_save_terminates_and_rereads() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val started = kotlinx.coroutines.CompletableDeferred<Unit>()
        val finish = kotlinx.coroutines.CompletableDeferred<Unit>()
        val old = MutableStateFlow(EnqueueStatus.PENDING)
        val current = MutableStateFlow(EnqueueStatus.PENDING)
        var saved = ReminderPreferences(); var writes = 0; var oldActive = true; var currentSubmissions = 0
        try {
            val controller = NotificationSettingsController(scope, { saved }, { renewal, _ ->
                writes++; started.complete(Unit); finish.await()
                saved.copy(renewalEnabled = renewal ?: saved.renewalEnabled).also { saved = it }
            }, schedulePort = { old }, isOwnerActive = { oldActive })
            ready(controller); controller.setRenewalEnabled(false); started.await()
            oldActive = false
            controller.rebindSchedule(Any(), { currentSubmissions++; current }, { true })
            assertTrue(controller.state.value.saving)
            controller.setMilestoneEnabled(false)
            assertEquals(1, writes)
            finish.complete(Unit)
            val reread = withTimeout(3_000) { controller.state.first {
                !it.loading && !it.saving && it.preferences?.renewalEnabled == false
            } }
            assertFalse(reread.preferences!!.renewalEnabled)
            assertEquals(1, writes)
            assertEquals(1, currentSubmissions)
            old.value = EnqueueStatus.FAILED; yield()
            assertEquals(EnqueueStatus.PENDING, controller.state.value.scheduleStatus)
        } finally { finish.complete(Unit); scope.cancel() }
    }

    @Test fun rebind_during_cancelled_save_keeps_outcome_unknown_without_retry() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val started = kotlinx.coroutines.CompletableDeferred<Unit>()
        val operation = kotlinx.coroutines.CompletableDeferred<Unit>()
        val old = MutableStateFlow(EnqueueStatus.PENDING)
        val current = MutableStateFlow(EnqueueStatus.PENDING)
        var oldActive = true; var writes = 0; var currentSubmissions = 0
        try {
            val controller = NotificationSettingsController(scope, { ReminderPreferences() }, { _, _ ->
                writes++; started.complete(Unit); operation.await(); ReminderPreferences()
            }, schedulePort = { old }, isOwnerActive = { oldActive })
            ready(controller); controller.setRenewalEnabled(false); started.await()
            oldActive = false
            controller.rebindSchedule(Any(), { currentSubmissions++; current }, { true })
            assertTrue(controller.state.value.saving)
            operation.completeExceptionally(CancellationException("uncertain save"))
            val unknown = withTimeout(3_000) { controller.state.first { it.scheduleStatus == EnqueueStatus.UNKNOWN } }
            assertNull(unknown.preferences)
            assertTrue(unknown.error!!.contains("无法确认"))
            assertEquals(1, writes)
            assertEquals(0, currentSubmissions)
        } finally { scope.cancel() }
    }

    @Test fun retired_owner_save_finishes_before_rebind_then_new_binding_rereads() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val entered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val finish = kotlinx.coroutines.CompletableDeferred<Unit>()
        val returned = kotlinx.coroutines.CompletableDeferred<Unit>()
        val old = MutableStateFlow(EnqueueStatus.PENDING)
        val current = MutableStateFlow(EnqueueStatus.PENDING)
        var saved = ReminderPreferences(); var oldActive = true; var writes = 0; var submissions = 0
        try {
            val controller = NotificationSettingsController(scope, { saved }, { renewal, _ ->
                writes++; entered.complete(Unit); finish.await()
                saved.copy(renewalEnabled = renewal ?: saved.renewalEnabled).also {
                    saved = it; returned.complete(Unit)
                }
            }, schedulePort = { old }, isOwnerActive = { oldActive })
            ready(controller); controller.setRenewalEnabled(false); entered.await()
            oldActive = false
            finish.complete(Unit); returned.await(); yield()
            assertTrue(controller.state.value.saving)
            controller.rebindSchedule(Any(), { submissions++; current }, { true })
            val reread = withTimeout(3_000) { controller.state.first {
                !it.loading && !it.saving && it.preferences?.renewalEnabled == false
            } }
            assertFalse(reread.preferences!!.renewalEnabled)
            assertEquals(1, writes)
            assertEquals(0, submissions)
            controller.requestScheduleOnResume(); yield()
            assertEquals(1, submissions)
        } finally { finish.complete(Unit); scope.cancel() }
    }

    @Test fun retired_owner_save_cancels_before_rebind_stays_unknown_without_enqueue() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val entered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val operation = kotlinx.coroutines.CompletableDeferred<Unit>()
        val ended = kotlinx.coroutines.CompletableDeferred<Unit>()
        val old = MutableStateFlow(EnqueueStatus.PENDING)
        val current = MutableStateFlow(EnqueueStatus.PENDING)
        var oldActive = true; var writes = 0; var submissions = 0
        try {
            val controller = NotificationSettingsController(scope, { ReminderPreferences() }, { _, _ ->
                writes++; entered.complete(Unit)
                try { operation.await(); ReminderPreferences() } finally { ended.complete(Unit) }
            }, schedulePort = { old }, isOwnerActive = { oldActive })
            ready(controller); controller.setRenewalEnabled(false); entered.await()
            oldActive = false
            operation.completeExceptionally(CancellationException("uncertain save")); ended.await(); yield()
            assertTrue(controller.state.value.saving)
            controller.rebindSchedule(Any(), { submissions++; current }, { true })
            assertFalse(controller.state.value.saving)
            assertTrue(controller.state.value.saveOutcomeUnknown)
            assertNull(controller.state.value.preferences)
            assertEquals(EnqueueStatus.UNKNOWN, controller.state.value.scheduleStatus)
            controller.requestScheduleOnResume(); controller.refresh(); yield()
            assertEquals(0, submissions)
            assertEquals(1, writes)
            assertEquals(EnqueueStatus.UNKNOWN, controller.state.value.scheduleStatus)
        } finally { scope.cancel() }
    }

    @Test fun cancellation_on_current_binding_does_not_leave_saving_or_auto_enqueue() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val started = kotlinx.coroutines.CompletableDeferred<Unit>()
        val operation = kotlinx.coroutines.CompletableDeferred<Unit>()
        val updates = MutableStateFlow(EnqueueStatus.PENDING)
        var submissions = 0; var writes = 0
        try {
            val controller = NotificationSettingsController(scope, { ReminderPreferences() }, { _, _ ->
                writes++; started.complete(Unit); operation.await(); ReminderPreferences()
            }, schedulePort = { submissions++; updates })
            ready(controller); controller.setRenewalEnabled(false); started.await()
            repeat(3) { controller.requestScheduleOnResume() }
            assertEquals(0, submissions)
            operation.completeExceptionally(CancellationException("uncertain save"))
            val result = withTimeout(3_000) { controller.state.first { it.saveOutcomeUnknown } }
            assertFalse(result.saving)
            assertNull(result.preferences)
            assertEquals(EnqueueStatus.UNKNOWN, result.scheduleStatus)
            controller.requestScheduleOnResume(); controller.refresh(); yield()
            assertEquals(0, submissions)
            assertEquals(1, writes)
            assertEquals(EnqueueStatus.UNKNOWN, controller.state.value.scheduleStatus)
        } finally { scope.cancel() }
    }

    @Test fun failed_save_retains_coalesced_foreground_request_as_unknown_without_enqueue() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val started = kotlinx.coroutines.CompletableDeferred<Unit>()
        val operation = kotlinx.coroutines.CompletableDeferred<Unit>()
        val updates = MutableStateFlow(EnqueueStatus.PENDING)
        var submissions = 0; var writes = 0
        try {
            val controller = NotificationSettingsController(scope, { ReminderPreferences() }, { _, _ ->
                writes++; started.complete(Unit); operation.await(); ReminderPreferences()
            }, schedulePort = { submissions++; updates })
            ready(controller); controller.setRenewalEnabled(false); started.await()
            repeat(3) { controller.requestScheduleOnResume() }
            assertEquals(0, submissions)
            operation.completeExceptionally(IOException("synthetic write failure"))
            val failure = withTimeout(3_000) { controller.state.first { !it.saving && it.error != null } }
            assertEquals(EnqueueStatus.UNKNOWN, failure.scheduleStatus)
            assertTrue(failure.preferences!!.renewalEnabled)
            assertTrue(failure.error!!.contains("未能保存"))
            assertEquals(0, submissions)
            assertEquals(1, writes)
        } finally { scope.cancel() }
    }

    @Test fun repeated_resume_during_save_is_coalesced_until_success() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val started = kotlinx.coroutines.CompletableDeferred<Unit>()
        val finish = kotlinx.coroutines.CompletableDeferred<Unit>()
        val updates = MutableStateFlow(EnqueueStatus.PENDING)
        var submissions = 0
        try {
            val controller = NotificationSettingsController(scope, { ReminderPreferences() }, { renewal, _ ->
                started.complete(Unit); finish.await(); ReminderPreferences(renewalEnabled = renewal ?: true)
            }, schedulePort = { submissions++; updates })
            ready(controller); controller.setRenewalEnabled(false); started.await()
            repeat(3) { controller.requestScheduleOnResume() }
            assertEquals(0, submissions)
            assertTrue(controller.state.value.saving)
            finish.complete(Unit)
            withTimeout(3_000) { controller.state.first { !it.saving && it.scheduleStatus == EnqueueStatus.PENDING } }
            assertEquals(1, submissions)
        } finally { finish.complete(Unit); scope.cancel() }
    }

    @Test fun resume_request_is_not_erased_when_slow_preference_refresh_finishes(): Unit = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val entered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val finish = kotlinx.coroutines.CompletableDeferred<Unit>()
        val updates = MutableStateFlow(EnqueueStatus.PENDING)
        var reads = 0
        try {
            val controller = NotificationSettingsController(scope, {
                reads++
                if (reads == 2) { entered.complete(Unit); finish.await() }
                ReminderPreferences()
            }, { _, _ -> ReminderPreferences() }, schedulePort = { updates })
            ready(controller); controller.refresh(); entered.await()
            controller.requestScheduleOnResume(); yield()
            assertEquals(EnqueueStatus.PENDING, controller.state.value.scheduleStatus)
            finish.complete(Unit)
            ready(controller)
            assertEquals(EnqueueStatus.PENDING, controller.state.value.scheduleStatus)
            updates.value = EnqueueStatus.UNKNOWN
            withTimeout(3_000) { controller.state.first { it.scheduleStatus == EnqueueStatus.UNKNOWN } }
        } finally { finish.complete(Unit); scope.cancel() }
    }

    @Test fun leaving_page_invalidates_ticket_and_reentering_uses_a_new_ticket(): Unit = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val old = MutableStateFlow(EnqueueStatus.PENDING)
        val next = MutableStateFlow(EnqueueStatus.PENDING)
        var submissions = 0
        try {
            val controller = NotificationSettingsController(scope, { ReminderPreferences() },
                { _, _ -> ReminderPreferences() }, schedulePort = { if (submissions++ == 0) old else next })
            ready(controller); controller.requestScheduleOnResume(); yield()
            controller.stopScheduleObservation()
            old.value = EnqueueStatus.FAILED; yield()
            assertNull(controller.state.value.scheduleStatus)
            controller.requestScheduleOnResume(); yield()
            old.value = EnqueueStatus.ENQUEUED; yield()
            assertEquals(EnqueueStatus.PENDING, controller.state.value.scheduleStatus)
            next.value = EnqueueStatus.ENQUEUED
            withTimeout(3_000) { controller.state.first { it.scheduleStatus == EnqueueStatus.ENQUEUED } }
        } finally { scope.cancel() }
    }

    @Test fun save_completing_after_page_exit_still_requests_but_does_not_publish_old_page_status() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val started = kotlinx.coroutines.CompletableDeferred<Unit>()
        val finish = kotlinx.coroutines.CompletableDeferred<Unit>()
        val updates = MutableStateFlow(EnqueueStatus.PENDING)
        var saved = ReminderPreferences(); var submissions = 0
        try {
            val controller = NotificationSettingsController(scope, { saved }, { renewal, _ ->
                started.complete(Unit); finish.await()
                saved.copy(renewalEnabled = renewal ?: saved.renewalEnabled).also { saved = it }
            }, schedulePort = { submissions++; updates })
            ready(controller); controller.setRenewalEnabled(false); started.await()
            controller.stopScheduleObservation(); finish.complete(Unit)
            withTimeout(3_000) { while (submissions == 0) yield() }
            assertFalse(saved.renewalEnabled)
            assertNull(controller.state.value.scheduleStatus)
            updates.value = EnqueueStatus.FAILED; yield()
            assertNull(controller.state.value.scheduleStatus)
            assertEquals(1, submissions)
        } finally { finish.complete(Unit); scope.cancel() }
    }

    @Test fun missing_ticket_and_unclassified_adapter_failure_are_unknown() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val unknown = NotificationSettingsController(scope, { ReminderPreferences() },
                { _, _ -> ReminderPreferences() }, schedulePort = { null })
            ready(unknown); unknown.requestScheduleOnResume()
            assertEquals(EnqueueStatus.UNKNOWN, unknown.state.value.scheduleStatus)
            val failed = NotificationSettingsController(scope, { ReminderPreferences() },
                { _, _ -> ReminderPreferences() }, schedulePort = { throw IOException("synthetic") })
            ready(failed); failed.requestScheduleOnResume()
            assertEquals(EnqueueStatus.UNKNOWN, failed.state.value.scheduleStatus)
        } finally { scope.cancel() }
    }

    @Test fun synchronous_cancellation_is_propagated_without_fake_success() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val cancellation = CancellationException("synthetic")
        try {
            val controller = NotificationSettingsController(scope, { ReminderPreferences() },
                { _, _ -> ReminderPreferences() }, schedulePort = { throw cancellation })
            ready(controller)
            assertSame(cancellation, runCatching { controller.requestScheduleOnResume() }.exceptionOrNull())
            assertNotEquals(EnqueueStatus.ENQUEUED, controller.state.value.scheduleStatus)
        } finally { scope.cancel() }
    }
}
