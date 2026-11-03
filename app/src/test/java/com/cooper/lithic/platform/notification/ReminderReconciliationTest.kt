package com.cooper.lithic.platform.notification

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderReconciliationTest {
    @Test fun both_domains_run_in_order_on_success() = runBlocking {
        val calls = mutableListOf<String>()
        val failure = runCatching {
            reconcileReminderDomains({ calls += "milestone" }, { calls += "renewal" })
        }.exceptionOrNull()
        assertEquals(listOf("renewal", "milestone"), calls)
        assertNull(failure)
    }

    @Test fun milestone_failure_does_not_block_renewal_but_remains_a_failure() = runBlocking {
        val milestoneFailure = IOException("milestone")
        var renewalRuns = 0
        val failure = runCatching {
            reconcileReminderDomains({ throw milestoneFailure }, { renewalRuns++ })
        }.exceptionOrNull()
        assertEquals(1, renewalRuns)
        assertSame(milestoneFailure, failure)
    }

    @Test fun renewal_failure_is_not_swallowed() = runBlocking {
        val renewalFailure = IOException("renewal")
        var milestoneRuns = 0
        val failure = runCatching {
            reconcileReminderDomains({ milestoneRuns++ }, { throw renewalFailure })
        }.exceptionOrNull()
        assertSame(renewalFailure, failure)
        assertEquals(1, milestoneRuns)
    }

    @Test fun both_failures_remain_available_for_diagnosis() = runBlocking {
        val milestoneFailure = IOException("milestone")
        val renewalFailure = IOException("renewal")
        val failure = runCatching {
            reconcileReminderDomains({ throw milestoneFailure }, { throw renewalFailure })
        }.exceptionOrNull()
        assertSame(renewalFailure, failure)
        assertEquals(listOf(milestoneFailure), renewalFailure.suppressed.toList())
    }

    @Test fun renewal_cancellation_propagates_without_requesting_milestones() = runBlocking {
        val cancellation = CancellationException("cancelled")
        var milestoneRuns = 0
        val failure = runCatching {
            reconcileReminderDomains({ milestoneRuns++ }, { throw cancellation })
        }.exceptionOrNull()
        assertSame(cancellation, failure)
        assertEquals(0, milestoneRuns)
    }

    @Test fun milestone_cancellation_propagates_with_prior_failure_preserved() = runBlocking {
        val renewalFailure = IOException("renewal")
        val cancellation = CancellationException("cancelled")
        val failure = runCatching {
            reconcileReminderDomains({ throw cancellation }, { throw renewalFailure })
        }.exceptionOrNull()
        assertSame(cancellation, failure)
        assertTrue(cancellation.suppressed.contains(renewalFailure))
    }

    @Test fun milestone_enqueue_suspends_until_success_is_observed() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val operation = CompletableDeferred<Unit>()
        val result = async {
            runCatching {
                reconcileReminderDomains({ started.complete(Unit); operation.await() }, {})
            }.exceptionOrNull()
        }
        started.await()
        assertTrue(!result.isCompleted)
        operation.complete(Unit)
        assertNull(result.await())
    }

    @Test fun milestone_enqueue_suspends_and_reports_async_failure() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val operation = CompletableDeferred<Unit>()
        val enqueueFailure = IOException("enqueue failed")
        var boundaryFailure: Throwable? = null
        val result = async {
            runCatching {
                reconcileReminderDomains({
                    started.complete(Unit)
                    try { operation.await() } catch (failure: Throwable) {
                        boundaryFailure = failure
                        throw failure
                    }
                }, {})
            }.exceptionOrNull()
        }
        started.await()
        assertTrue(!result.isCompleted)
        operation.completeExceptionally(enqueueFailure)
        val actual = result.await()
        assertSame(boundaryFailure, actual)
        assertTrue(actual is IOException)
        assertEquals("enqueue failed", actual?.message)
        assertTrue(generateSequence(actual) { it.cause }.any { it === enqueueFailure })
    }

    @Test fun cancellation_during_suspended_enqueue_propagates() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val operation = CompletableDeferred<Unit>()
        val cancellation = CancellationException("enqueue cancelled")
        var boundaryFailure: Throwable? = null
        val result = async {
            runCatching {
                reconcileReminderDomains({
                    started.complete(Unit)
                    try { operation.await() } catch (failure: Throwable) {
                        boundaryFailure = failure
                        throw failure
                    }
                }, {})
            }.exceptionOrNull()
        }
        started.await()
        assertTrue(!result.isCompleted)
        operation.completeExceptionally(cancellation)
        val actual = result.await()
        assertSame(boundaryFailure, actual)
        assertTrue(actual is CancellationException)
        assertEquals("enqueue cancelled", actual?.message)
        assertTrue(generateSequence(actual) { it.cause }.any { it === cancellation })
    }
}
