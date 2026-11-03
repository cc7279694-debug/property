package com.cooper.lithic.platform.notification

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderEnqueuePreparationTest {
    private val lease = object : EnqueueLease {
        override fun isActive() = true
        override fun <T : Any> withActive(block: () -> T): T = block()
    }

    @Test fun preparation_failure_preserves_cause_and_never_calls_submit() {
        val original = IOException("manager unavailable")
        var submits = 0
        val actual = runCatching {
            prepareAndSubmitEnqueue<String, String>(
                prepare = { throw original },
                submit = { submits++; it },
            )
        }.exceptionOrNull()
        assertTrue(actual is EnqueueNotSubmittedException)
        assertSame(original, actual?.cause)
        assertEquals(0, submits)
    }

    @Test fun preparation_cancellation_propagates_without_platform_submission() {
        val original = CancellationException("caller cancelled")
        var submits = 0
        val actual = runCatching {
            prepareAndSubmitEnqueue<String, String>(
                prepare = { throw original },
                submit = { submits++; it },
            )
        }.exceptionOrNull()
        assertSame(original, actual)
        assertEquals(0, submits)
    }

    @Test fun platform_call_failure_after_possible_side_effect_is_not_reclassified() {
        val original = IOException("possible acceptance")
        var effects = 0
        val actual = runCatching {
            prepareAndSubmitEnqueue<String, String>(
                prepare = { "request" },
                submit = { effects++; throw original },
            )
        }.exceptionOrNull()
        assertSame(original, actual)
        assertEquals(1, effects)
    }

    @Test fun successful_preparation_and_submission_preserve_result() {
        val result = Any()
        assertSame(result, prepareAndSubmitEnqueue(prepare = { "request" }, submit = { result }))
    }

    @Test fun actual_helper_pre_submission_failure_becomes_coordinator_failed() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var submits = 0
        try {
            val coordinator = ReminderEnqueueCoordinator(scope, EnqueuePort {
                prepareAndSubmitEnqueue<String, EnqueueOperation>(
                    prepare = { throw IOException("builder unavailable") },
                    submit = { submits++; EnqueueOperation {} },
                )
            }, 5_000, Dispatchers.Unconfined)
            assertEquals(EnqueueStatus.FAILED, coordinator.request(EnqueueKind.PERIODIC, lease).value)
            assertEquals(0, submits)
        } finally { scope.cancel() }
    }

    @Test fun actual_helper_submit_failure_remains_unknown_and_cannot_replace() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var effects = 0
        try {
            val coordinator = ReminderEnqueueCoordinator(scope, EnqueuePort {
                prepareAndSubmitEnqueue<String, EnqueueOperation>(
                    prepare = { "request" },
                    submit = { effects++; throw IOException("possibly accepted") },
                )
            }, 5_000, Dispatchers.Unconfined)
            assertEquals(EnqueueStatus.UNKNOWN, coordinator.request(EnqueueKind.RECONCILE, lease).value)
            assertEquals(EnqueueStatus.UNKNOWN, coordinator.request(EnqueueKind.RECONCILE, lease).value)
            assertEquals(1, effects)
        } finally { scope.cancel() }
    }
}
