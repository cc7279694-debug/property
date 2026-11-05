package com.cooper.lithic.platform.notification

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LegacyReminderCancellationRunnerTest {
    @Test fun core_capability_cannot_be_enabled_by_stored_preferences() {
        // This product decision is a compile-time capability, not a DataStore setting.
        assertFalse(ReminderCapability.AUTOMATIC_NOTIFICATIONS)
    }

    @Test fun only_five_explicit_reminder_targets_are_submitted_once() {
        val submitted = mutableListOf<LegacyReminderTarget>()
        val recorded = mutableListOf<Pair<LegacyReminderTarget, LegacyCancellationOutcome>>()
        val runner = LegacyReminderCancellationRunner(
            dispatch = { it() },
            cancel = { target ->
                submitted += target
                LegacyCancellationOperation { callback -> callback(LegacyCancellationOutcome.CONFIRMED) }
            },
            record = { target, outcome -> recorded += target to outcome },
        )

        runner.start()
        runner.start()

        assertEquals(listOf(
            LegacyReminderTarget.Unique("lithic-renewal-daily-v1"),
            LegacyReminderTarget.Unique("lithic-renewal-reconcile-v1"),
            LegacyReminderTarget.Unique("lithic-milestone-reconcile-v1"),
            LegacyReminderTarget.Tag("lithic-renewal-candidate-v1"),
            LegacyReminderTarget.Tag("lithic-milestone-candidate-v1"),
        ), submitted)
        assertEquals(5, recorded.count { it.second == LegacyCancellationOutcome.UNKNOWN })
        assertEquals(5, recorded.count { it.second == LegacyCancellationOutcome.CONFIRMED })
    }

    @Test fun missing_callback_synchronous_exception_and_failed_operation_do_not_claim_success() {
        val outcomes = mutableMapOf<LegacyReminderTarget, MutableList<LegacyCancellationOutcome>>()
        val submissions = mutableListOf<LegacyReminderTarget>()
        val targets = LegacyReminderCancellationRunner.TARGETS
        val runner = LegacyReminderCancellationRunner(
            dispatch = { it() },
            cancel = { target ->
                submissions += target
                when (target) {
                    targets[0] -> throw IOException("outcome unknown")
                    targets[1] -> LegacyCancellationOperation { }
                    targets[2] -> LegacyCancellationOperation { it(LegacyCancellationOutcome.UNKNOWN) }
                    targets[3] -> LegacyCancellationOperation { it(LegacyCancellationOutcome.FAILED) }
                    else -> LegacyCancellationOperation { it(LegacyCancellationOutcome.CONFIRMED) }
                }
            },
            record = { target, outcome -> outcomes.getOrPut(target) { mutableListOf() } += outcome },
        )

        runner.start()
        runner.start()

        assertEquals(5, submissions.size)
        assertEquals(listOf(LegacyCancellationOutcome.UNKNOWN), outcomes[targets[0]])
        assertEquals(listOf(LegacyCancellationOutcome.UNKNOWN), outcomes[targets[1]])
        assertEquals(listOf(LegacyCancellationOutcome.UNKNOWN), outcomes[targets[2]])
        assertEquals(listOf(LegacyCancellationOutcome.UNKNOWN, LegacyCancellationOutcome.FAILED), outcomes[targets[3]])
        assertEquals(listOf(LegacyCancellationOutcome.UNKNOWN, LegacyCancellationOutcome.CONFIRMED), outcomes[targets[4]])
    }

    @Test fun delayed_and_duplicate_results_cannot_reopen_or_overwrite_terminal_result() {
        val callbacks = mutableMapOf<LegacyReminderTarget, (LegacyCancellationOutcome) -> Unit>()
        val outcomes = mutableMapOf<LegacyReminderTarget, MutableList<LegacyCancellationOutcome>>()
        val runner = LegacyReminderCancellationRunner(
            dispatch = { it() },
            cancel = { target -> LegacyCancellationOperation { callbacks[target] = it } },
            record = { target, outcome -> outcomes.getOrPut(target) { mutableListOf() } += outcome },
        )
        runner.start()
        val target = LegacyReminderCancellationRunner.TARGETS.first()
        val callback = requireNotNull(callbacks[target])
        callback(LegacyCancellationOutcome.UNKNOWN)
        callback(LegacyCancellationOutcome.CONFIRMED)
        callback(LegacyCancellationOutcome.FAILED)
        runner.start()
        assertEquals(listOf(LegacyCancellationOutcome.UNKNOWN, LegacyCancellationOutcome.CONFIRMED), outcomes[target])
        assertEquals(5, callbacks.size)
    }

    @Test fun rejected_dispatch_does_not_skip_other_targets_or_retry() {
        var attempts = 0
        val submitted = mutableListOf<LegacyReminderTarget>()
        val outcomes = mutableMapOf<LegacyReminderTarget, MutableList<LegacyCancellationOutcome>>()
        val runner = LegacyReminderCancellationRunner(
            dispatch = { action ->
                attempts++
                if (attempts == 1) throw IOException("dispatcher unavailable")
                action()
            },
            cancel = { target ->
                submitted += target
                LegacyCancellationOperation { it(LegacyCancellationOutcome.CONFIRMED) }
            },
            record = { target, outcome -> outcomes.getOrPut(target) { mutableListOf() } += outcome },
        )
        runner.start()
        runner.start()
        assertEquals(5, attempts)
        assertEquals(LegacyReminderCancellationRunner.TARGETS.drop(1), submitted)
        assertEquals(listOf(LegacyCancellationOutcome.UNKNOWN), outcomes[LegacyReminderCancellationRunner.TARGETS.first()])
    }
}
