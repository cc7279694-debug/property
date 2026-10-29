package com.cooper.lithic.platform.notification

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ReminderRequestObservationTest {
    @Test fun initial_request_failure_does_not_end_observation() = runBlocking {
        val requestFailure = IOException("Synthetic initial scheduling failure")
        val failures = mutableListOf<Exception>()
        var requests = 0
        var changesObserved = 0
        val failure = runCatching {
            observeReminderRequests(
                changes = flow {
                    repeat(2) {
                        changesObserved++
                        emit(Unit)
                    }
                },
                request = {
                    requests++
                    if (requests == 1) throw requestFailure
                },
                onRequestFailed = { failures += it },
            )
        }.exceptionOrNull()

        assertEquals(3, requests)
        assertEquals(2, changesObserved)
        assertNull(failure)
        assertEquals(listOf(requestFailure), failures)
    }

    @Test fun change_request_failure_keeps_observing_later_changes() = runBlocking {
        val requestFailure = IOException("Synthetic change scheduling failure")
        val failures = mutableListOf<Exception>()
        var requests = 0
        var changesObserved = 0
        val failure = runCatching {
            observeReminderRequests(
                changes = flow {
                    repeat(2) {
                        changesObserved++
                        emit(Unit)
                    }
                },
                request = {
                    requests++
                    if (requests == 2) throw requestFailure
                },
                onRequestFailed = { failures += it },
            )
        }.exceptionOrNull()

        assertEquals(3, requests)
        assertEquals(2, changesObserved)
        assertNull(failure)
        assertEquals(listOf(requestFailure), failures)
    }

    @Test fun initial_request_precedes_change_subscription_and_each_change_requests_once() = runBlocking {
        val events = mutableListOf<String>()
        var failedRequests = 0

        observeReminderRequests(
            changes = flow {
                events += "source-start"
                emit(Unit)
                events += "between-changes"
                emit(Unit)
                events += "source-complete"
            },
            request = { events += "request" },
            onRequestFailed = { failedRequests++ },
        )

        assertEquals(listOf("request", "source-start", "request", "between-changes", "request", "source-complete"), events)
        assertEquals(0, failedRequests)
    }

    @Test fun cancellation_from_request_propagates_without_observing_later_changes() = runBlocking {
        val cancellation = CancellationException("Synthetic generation retired")
        var failedRequests = 0
        var requests = 0
        var changesObserved = 0
        val failure = runCatching {
            observeReminderRequests(
                changes = flow {
                    repeat(2) {
                        changesObserved++
                        emit(Unit)
                    }
                },
                request = {
                    requests++
                    if (requests == 2) throw cancellation
                },
                onRequestFailed = { failedRequests++ },
            )
        }.exceptionOrNull()

        assertSame(cancellation, failure)
        assertEquals(2, requests)
        assertEquals(1, changesObserved)
        assertEquals(0, failedRequests)
    }

    @Test fun upstream_observation_failure_propagates_without_scheduling_extra_requests() = runBlocking {
        val sourceFailure = IOException("Synthetic observation failure")
        var failedRequests = 0
        var requests = 0
        val failure = runCatching {
            observeReminderRequests(
                changes = flow {
                    emit(Unit)
                    throw sourceFailure
                },
                request = { requests++ },
                onRequestFailed = { failedRequests++ },
            )
        }.exceptionOrNull()

        assertSame(sourceFailure, failure)
        assertEquals(2, requests)
        assertEquals(0, failedRequests)
    }
}
