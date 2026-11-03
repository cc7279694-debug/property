package com.cooper.lithic.platform.notification

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderEnqueueCoordinatorTest {
    private class Lease : EnqueueLease {
        private val lock = Any()
        @Volatile private var active = true
        override fun isActive(): Boolean = active
        override fun <T : Any> withActive(block: () -> T): T? = synchronized(lock) {
            if (active) block() else null
        }
        fun close() = synchronized(lock) { active = false }
    }

    private class Operation : EnqueueOperation {
        val registered = CountDownLatch(1)
        @Volatile private var callback: ((Result<Unit>) -> Unit)? = null
        override fun addTerminalListener(listener: (Result<Unit>) -> Unit) { callback = listener; registered.countDown() }
        fun finish(result: Result<Unit> = Result.success(Unit)) = requireNotNull(callback)(result)
    }

    private class Port : EnqueuePort {
        val calls = mutableListOf<Pair<EnqueueKind, Operation>>()
        var fail: Exception? = null
        override fun enqueue(kind: EnqueueKind): EnqueueOperation {
            fail?.let { throw it }
            return Operation().also { calls += kind to it }
        }
    }

    private class Harness(timeout: Long = 20) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val port = Port()
        val owner = Lease()
        val coordinator = ReminderEnqueueCoordinator(scope, port, timeout, Dispatchers.Unconfined)
        fun request(kind: EnqueueKind = EnqueueKind.RECONCILE, lease: Lease = owner) =
            coordinator.request(kind, lease)
        fun close() = scope.cancel()
    }

    @Test fun successful_operation_is_confirmed_only_on_terminal_callback() = runBlocking {
        val h = Harness()
        try {
            val state = h.request()
            assertEquals(EnqueueStatus.PENDING, state.value)
            assertEquals(1, h.port.calls.size)
            h.port.calls.single().second.finish()
            assertEquals(EnqueueStatus.ENQUEUED, state.value)
        } finally { h.close() }
    }

    @Test fun synchronous_and_asynchronous_failures_are_not_success_or_automatic_retry() = runBlocking {
        val h = Harness()
        try {
            h.port.fail = EnqueueNotSubmittedException("known pre-submission failure")
            val sync = h.request()
            assertEquals(EnqueueStatus.FAILED, sync.value)
            assertTrue(h.port.calls.isEmpty())
            h.port.fail = null
            val async = h.request()
            h.port.calls.single().second.finish(Result.failure(IOException("asynchronous")))
            assertEquals(EnqueueStatus.FAILED, async.value)
            assertEquals(1, h.port.calls.size)
        } finally { h.close() }
    }

    @Test fun timeout_is_unknown_does_not_retry_and_waits_for_actual_terminal() = runBlocking {
        val h = Harness()
        try {
            val first = h.request()
            withTimeout(1_000) { first.first { it == EnqueueStatus.UNKNOWN } }
            val next = h.request()
            assertEquals(1, h.port.calls.size)
            delay(40)
            assertEquals(1, h.port.calls.size)
            assertEquals(EnqueueStatus.UNKNOWN, next.value)
            h.port.calls[0].second.finish()
            assertEquals(2, h.port.calls.size)
            assertEquals(EnqueueStatus.PENDING, next.value)
            h.port.calls[1].second.finish()
            assertEquals(EnqueueStatus.ENQUEUED, next.value)
        } finally { h.close() }
    }

    @Test fun rapid_requests_merge_to_one_latest_pending_without_automatic_loop() = runBlocking {
        val h = Harness(timeout = 5_000)
        try {
            val first = h.request()
            val pending = (1..100).map { h.request() }
            assertEquals(1, h.port.calls.size)
            assertEquals(EnqueueStatus.CANCELLED, first.value)
            assertTrue(pending.dropLast(1).all { it.value == EnqueueStatus.CANCELLED })
            assertEquals(EnqueueStatus.PENDING, pending.last().value)
            h.port.calls[0].second.finish()
            assertEquals(2, h.port.calls.size)
            h.port.calls[1].second.finish()
            assertEquals(EnqueueStatus.ENQUEUED, pending.last().value)
            assertEquals(2, h.port.calls.size)
        } finally { h.close() }
    }

    @Test fun retired_owner_and_reopened_instance_cannot_share_old_results() = runBlocking {
        val h = Harness(timeout = 5_000)
        try {
            val old = h.request()
            h.owner.close()
            h.coordinator.retire(h.owner)
            assertEquals(EnqueueStatus.CANCELLED, old.value)
            val reopened = Lease() // same underlying generation ID does not make this the old instance
            val next = h.request(lease = reopened)
            assertEquals(1, h.port.calls.size)
            h.port.calls[0].second.finish()
            assertEquals(EnqueueStatus.CANCELLED, old.value)
            assertEquals(2, h.port.calls.size)
            assertEquals(EnqueueStatus.PENDING, next.value)
            h.port.calls[1].second.finish()
            assertEquals(EnqueueStatus.ENQUEUED, next.value)
        } finally { h.close() }
    }

    @Test fun retired_owner_hanging_operation_makes_new_owner_waiting_status_unknown() = runBlocking {
        val h = Harness()
        try {
            val old = h.request()
            h.owner.close()
            h.coordinator.retire(h.owner)
            val next = h.request(lease = Lease())
            assertEquals(EnqueueStatus.CANCELLED, old.value)
            withTimeout(1_000) { next.first { it == EnqueueStatus.UNKNOWN } }
            assertEquals(1, h.port.calls.size)
            h.port.calls[0].second.finish()
            assertEquals(2, h.port.calls.size)
            assertEquals(EnqueueStatus.CANCELLED, old.value)
            assertEquals(EnqueueStatus.PENDING, next.value)
        } finally { h.close() }
    }

    @Test fun cancellation_of_the_future_is_unknown_not_proof_that_os_work_ended() = runBlocking {
        val h = Harness(timeout = 5_000)
        try {
            val state = h.request()
            h.port.calls.single().second.finish(Result.failure(CancellationException("future cancelled")))
            assertEquals(EnqueueStatus.UNKNOWN, state.value)
            val next = h.request()
            assertEquals(EnqueueStatus.UNKNOWN, next.value)
            assertEquals(1, h.port.calls.size)
        } finally { h.close() }
    }

    @Test fun periodic_and_reconcile_are_independent_single_flight_lanes() = runBlocking {
        val h = Harness(timeout = 5_000)
        try {
            val periodic = h.request(EnqueueKind.PERIODIC)
            val reconcile = h.request()
            assertEquals(listOf(EnqueueKind.PERIODIC, EnqueueKind.RECONCILE), h.port.calls.map { it.first })
            h.port.calls[0].second.finish(Result.failure(IOException("periodic")))
            h.port.calls[1].second.finish()
            assertEquals(EnqueueStatus.FAILED, periodic.value)
            assertEquals(EnqueueStatus.ENQUEUED, reconcile.value)
        } finally { h.close() }
    }

    @Test fun listener_registration_failure_remains_unknown_and_blocks_followup() = runBlocking {
        val owner = Lease()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var calls = 0
        try {
            val coordinator = ReminderEnqueueCoordinator(scope, EnqueuePort {
                calls++
                EnqueueOperation { throw IOException("cannot observe result") }
            }, 20, Dispatchers.Unconfined)
            val first = coordinator.request(EnqueueKind.RECONCILE, owner)
            assertEquals(EnqueueStatus.UNKNOWN, first.value)
            val next = coordinator.request(EnqueueKind.RECONCILE, owner)
            delay(40)
            assertEquals(1, calls)
            assertEquals(EnqueueStatus.UNKNOWN, next.value)
        } finally { scope.cancel() }
    }

    @Test fun blocking_synchronous_submission_times_out_without_submitting_latest() = runBlocking {
        val executor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val owner = Lease()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val operation = Operation()
        var submissions = 0
        try {
            val coordinator = ReminderEnqueueCoordinator(scope, EnqueuePort {
                submissions++
                entered.countDown()
                assertTrue(release.await(2, TimeUnit.SECONDS))
                operation
            }, 30, executor)
            val first = coordinator.request(EnqueueKind.RECONCILE, owner)
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            withTimeout(1_000) { first.first { it == EnqueueStatus.UNKNOWN } }
            val pending = coordinator.request(EnqueueKind.RECONCILE, owner)
            assertEquals(EnqueueStatus.UNKNOWN, pending.value)
            assertEquals(1, submissions)
            release.countDown()
            assertTrue(operation.registered.await(2, TimeUnit.SECONDS))
            assertEquals(1, submissions) // returning a handle is not terminal completion
        } finally {
            release.countDown()
            scope.cancel()
            executor.close()
        }
    }

    @Test fun blocking_listener_registration_times_out_and_retains_the_lane() = runBlocking {
        val executor = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val owner = Lease()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        var submissions = 0
        try {
            val coordinator = ReminderEnqueueCoordinator(scope, EnqueuePort {
                submissions++
                EnqueueOperation {
                    entered.countDown()
                    assertTrue(release.await(2, TimeUnit.SECONDS))
                }
            }, 30, executor)
            val first = coordinator.request(EnqueueKind.RECONCILE, owner)
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            withTimeout(1_000) { first.first { it == EnqueueStatus.UNKNOWN } }
            val pending = coordinator.request(EnqueueKind.RECONCILE, owner)
            assertEquals(EnqueueStatus.UNKNOWN, pending.value)
            assertEquals(1, submissions)
        } finally {
            release.countDown()
            scope.cancel()
            executor.close()
        }
    }

    @Test fun new_coordinator_does_not_inherit_old_unknown_process_death_remains_blocked() = runBlocking {
        // This is an in-memory simulation, not Android process-death validation. It exposes
        // the unresolved safety limitation: no durable record transfers UNKNOWN to a new
        // process, so a new coordinator can submit again while the old effect is unknown.
        val old = Harness()
        val nextScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val oldStatus = old.request()
            withTimeout(1_000) { oldStatus.first { it == EnqueueStatus.UNKNOWN } }
            assertEquals(1, old.port.calls.size)
            old.close()
            val replacement = ReminderEnqueueCoordinator(nextScope, old.port, 5_000, Dispatchers.Unconfined)
            val newStatus = replacement.request(EnqueueKind.RECONCILE, Lease())
            assertEquals(2, old.port.calls.size)
            assertEquals(EnqueueStatus.PENDING, newStatus.value)
        } finally {
            old.close()
            nextScope.cancel()
        }
    }

    @Test fun cancelled_scope_and_retired_lease_do_not_enqueue() = runBlocking {
        val h = Harness(timeout = 5_000)
        h.owner.close()
        val invalid = h.request()
        assertEquals(EnqueueStatus.CANCELLED, invalid.value)
        assertTrue(h.port.calls.isEmpty())
        h.close()
        val closed = h.request(lease = Lease())
        assertEquals(EnqueueStatus.CANCELLED, closed.value)
        assertTrue(h.port.calls.isEmpty())
    }

    @Test fun duplicate_old_callback_cannot_finish_a_new_operation() = runBlocking {
        val h = Harness(timeout = 5_000)
        try {
            h.request()
            val latest = h.request()
            val original = h.port.calls[0].second
            original.finish()
            assertEquals(2, h.port.calls.size)
            original.finish(Result.failure(IOException("duplicate")))
            assertEquals(EnqueueStatus.PENDING, latest.value)
            h.port.calls[1].second.finish()
            assertEquals(EnqueueStatus.ENQUEUED, latest.value)
            assertFalse(h.port.calls.size > 2)
        } finally { h.close() }
    }

    @Test fun superseded_in_flight_timeout_marks_latest_unknown_until_late_real_completion() = runBlocking {
        val h = Harness()
        try {
            val old = h.request()
            val latest = h.request()
            assertEquals(EnqueueStatus.CANCELLED, old.value)
            withTimeout(1_000) { latest.first { it == EnqueueStatus.UNKNOWN } }
            assertEquals(1, h.port.calls.size)
            val newer = h.request()
            assertEquals(EnqueueStatus.CANCELLED, latest.value)
            assertEquals(EnqueueStatus.UNKNOWN, newer.value)
            assertEquals(1, h.port.calls.size)
            h.port.calls[0].second.finish()
            assertEquals(2, h.port.calls.size)
            assertEquals(EnqueueStatus.PENDING, newer.value)
            h.port.calls[1].second.finish()
            assertEquals(EnqueueStatus.ENQUEUED, newer.value)
        } finally { h.close() }
    }

    @Test fun synchronous_cancellation_leaves_enqueue_outcome_unknown_and_does_not_release_lane() = runBlocking {
        val h = Harness(timeout = 5_000)
        try {
            h.port.fail = CancellationException("synchronous cancellation")
            val first = h.request()
            assertEquals(EnqueueStatus.UNKNOWN, first.value)
            h.port.fail = null
            val next = h.request()
            assertEquals(EnqueueStatus.UNKNOWN, next.value)
            assertTrue(h.port.calls.isEmpty())
        } finally { h.close() }
    }

    @Test fun synchronous_exception_after_side_effect_cannot_trigger_automatic_replacement() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val owner = Lease()
        var sideEffects = 0
        try {
            val coordinator = ReminderEnqueueCoordinator(scope, EnqueuePort {
                sideEffects++ // the platform may already have accepted the request
                throw IOException("failure after possible acceptance")
            }, 20, Dispatchers.Unconfined)
            val first = coordinator.request(EnqueueKind.RECONCILE, owner)
            assertEquals(EnqueueStatus.UNKNOWN, first.value)
            val next = coordinator.request(EnqueueKind.RECONCILE, owner)
            assertEquals(EnqueueStatus.UNKNOWN, next.value)
            delay(40)
            assertEquals(1, sideEffects)
        } finally { scope.cancel() }
    }

    @Test fun owner_retirement_callback_does_not_deadlock_against_enqueue_admission() = runBlocking {
        val ownerLock = Any()
        val ownerHeld = CountDownLatch(1)
        val admissionAttempted = CountDownLatch(1)
        val retired = CountDownLatch(1)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val owner = object : EnqueueLease {
            override fun isActive() = true // as required by the real gate, a non-blocking read
            override fun <T : Any> withActive(block: () -> T): T? {
                admissionAttempted.countDown()
                return synchronized(ownerLock) { block() }
            }
        }
        val coordinator = ReminderEnqueueCoordinator(scope, EnqueuePort { Operation() }, 5_000)
        val ownerThread = thread(isDaemon = true) {
            synchronized(ownerLock) {
                ownerHeld.countDown()
                if (admissionAttempted.await(2, TimeUnit.SECONDS)) {
                    coordinator.retire(owner)
                    retired.countDown()
                }
            }
        }
        try {
            assertTrue(ownerHeld.await(2, TimeUnit.SECONDS))
            coordinator.request(EnqueueKind.RECONCILE, owner)
            assertTrue("retirement must not wait on coordinator holding the owner lock", retired.await(2, TimeUnit.SECONDS))
        } finally {
            scope.cancel()
            ownerThread.join(2_000)
        }
    }

    @Test fun diagnostics_failure_does_not_change_or_block_scheduling() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val reported = CountDownLatch(1)
        val operation = Operation()
        try {
            val coordinator = ReminderEnqueueCoordinator(scope, EnqueuePort { operation }, 5_000,
                Dispatchers.Unconfined) { _, _ -> reported.countDown(); throw IOException("logger") }
            val state = coordinator.request(EnqueueKind.RECONCILE, Lease())
            operation.finish(Result.failure(IOException("enqueue")))
            assertEquals(EnqueueStatus.FAILED, state.value)
            assertTrue(reported.await(2, TimeUnit.SECONDS))
        } finally { scope.cancel() }
    }
}
