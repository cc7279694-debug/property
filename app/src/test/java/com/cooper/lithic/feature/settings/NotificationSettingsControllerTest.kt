package com.cooper.lithic.feature.settings

import com.cooper.lithic.data.settings.ReminderPreferences
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class NotificationSettingsControllerTest {
    @Test fun reading_default_preferences_does_not_write_or_schedule() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        var writes = 0; var schedules = 0
        try {
            val controller = NotificationSettingsController(scope, { ReminderPreferences() },
                { _, _ -> writes++; ReminderPreferences() }, { schedules++ })
            val ready = withTimeout(3_000) { controller.state.first { !it.loading } }
            assertTrue(ready.preferences!!.renewalEnabled)
            assertTrue(ready.preferences.milestoneEnabled)
            assertEquals(0, writes)
            assertEquals(0, schedules)
        } finally { scope.cancel() }
    }

    @Test fun failed_read_never_uses_default_enabled_values_and_cannot_write() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        var fail = true; var writes = 0; var schedules = 0
        try {
            val controller = NotificationSettingsController(scope,
                { if (fail) throw IOException("Synthetic read failure") else ReminderPreferences(false, true) },
                { _, _ -> writes++; ReminderPreferences() }, { schedules++ })
            val failed = withTimeout(3_000) { controller.state.first { it.error != null } }
            assertNull(failed.preferences)
            controller.setRenewalEnabled(true); controller.setMilestoneEnabled(false)
            assertEquals(0, writes)
            assertEquals(0, schedules)
            fail = false; controller.refresh()
            val ready = withTimeout(3_000) { controller.state.first { !it.loading && it.preferences != null } }
            assertFalse(ready.preferences!!.renewalEnabled)
            assertTrue(ready.preferences.milestoneEnabled)
            assertNull(ready.error)
        } finally { scope.cancel() }
    }

    @Test fun switches_wait_for_persisted_values_and_only_then_request_schedule() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        val entered = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        val cutoff = LocalDate.of(2026, 10, 22)
        var saved = ReminderPreferences(importCutoff = cutoff)
        var schedules = 0; var writes = 0
        try {
            val controller = NotificationSettingsController(scope, { saved }, { renewal, milestone ->
                writes++; entered.complete(Unit); finish.await()
                saved.copy(renewalEnabled = renewal ?: saved.renewalEnabled,
                    milestoneEnabled = milestone ?: saved.milestoneEnabled).also { saved = it }
            }, { schedules++ })
            withTimeout(3_000) { controller.state.first { !it.loading } }
            controller.setRenewalEnabled(false)
            withTimeout(3_000) { entered.await() }
            assertTrue(controller.state.value.saving)
            assertTrue(controller.state.value.preferences!!.renewalEnabled)
            assertEquals(0, schedules)
            controller.setMilestoneEnabled(false)
            finish.complete(Unit)
            val ready = withTimeout(3_000) { controller.state.first { !it.saving } }
            assertFalse(ready.preferences!!.renewalEnabled)
            assertTrue(ready.preferences.milestoneEnabled)
            assertEquals(cutoff, ready.preferences.importCutoff)
            assertEquals(1, writes)
            assertEquals(1, schedules)
        } finally { finish.complete(Unit); scope.cancel() }
    }

    @Test fun write_failure_preserves_old_switches_and_is_retryable_without_fake_success() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        var fail = true; var schedules = 0
        var saved = ReminderPreferences()
        try {
            val controller = NotificationSettingsController(scope, { saved }, { renewal, milestone ->
                if (fail) throw IOException("Synthetic write failure")
                saved.copy(renewalEnabled = renewal ?: saved.renewalEnabled,
                    milestoneEnabled = milestone ?: saved.milestoneEnabled).also { saved = it }
            }, { schedules++ })
            withTimeout(3_000) { controller.state.first { !it.loading } }
            controller.setMilestoneEnabled(false)
            val failure = withTimeout(3_000) { controller.state.first { !it.saving && it.error != null } }
            assertTrue(failure.preferences!!.milestoneEnabled)
            assertEquals(0, schedules)
            fail = false; controller.setMilestoneEnabled(false)
            val ready = withTimeout(3_000) { controller.state.first { !it.saving && it.preferences?.milestoneEnabled == false } }
            assertNull(ready.error)
            assertTrue(ready.preferences!!.renewalEnabled)
            assertEquals(1, schedules)
        } finally { scope.cancel() }
    }

    @Test fun unchanged_switch_does_not_write_and_refresh_failure_disables_mutation() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        var fail = false; var writes = 0
        try {
            val controller = NotificationSettingsController(scope,
                { if (fail) throw IOException("Synthetic refresh failure") else ReminderPreferences() },
                { _, _ -> writes++; ReminderPreferences() })
            withTimeout(3_000) { controller.state.first { !it.loading } }
            controller.setRenewalEnabled(true); controller.setMilestoneEnabled(true)
            assertEquals(0, writes)
            fail = true; controller.refresh()
            withTimeout(3_000) { controller.state.first { it.error != null } }
            controller.setRenewalEnabled(false)
            assertNull(controller.state.value.preferences)
            assertEquals(0, writes)
        } finally { scope.cancel() }
    }

    @Test fun scheduling_failure_reports_saved_preferences_without_reverting_or_rewriting() = runBlocking {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        var saved = ReminderPreferences(); var writes = 0
        try {
            val controller = NotificationSettingsController(scope, { saved }, { renewal, milestone ->
                writes++
                saved.copy(renewalEnabled = renewal ?: saved.renewalEnabled,
                    milestoneEnabled = milestone ?: saved.milestoneEnabled).also { saved = it }
            }, { throw IOException("Synthetic scheduler failure") })
            withTimeout(3_000) { controller.state.first { !it.loading } }
            controller.setRenewalEnabled(false)
            val ready = withTimeout(3_000) { controller.state.first { !it.saving && it.notice != null } }
            assertFalse(ready.preferences!!.renewalEnabled)
            assertNull(ready.error)
            assertTrue(ready.notice!!.contains("已保存"))
            assertEquals(1, writes)
        } finally { scope.cancel() }
    }
}
