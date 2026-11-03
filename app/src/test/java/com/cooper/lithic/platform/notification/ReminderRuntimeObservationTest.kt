package com.cooper.lithic.platform.notification

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderRuntimeObservationTest {
    @Test fun initial_room_and_settings_observations_request_full_refresh() = runBlocking {
        var count = 0
        observeReminderSignals(flowOf(Unit), flowOf(Unit)) { count++ }
        assertEquals(2, count)
    }

    @Test fun room_change_during_delayed_settings_subscription_is_not_lost() = runBlocking {
        val room = MutableSharedFlow<Unit>()
        val roomSubscribed = CompletableDeferred<Unit>()
        val settingsStarted = CompletableDeferred<Unit>()
        val allowSettings = CompletableDeferred<Unit>()
        val received = Channel<Unit>(Channel.UNLIMITED)
        val settings = flow {
            settingsStarted.complete(Unit)
            allowSettings.await()
            emit(Unit) // DataStore's first value represents its current preferences.
        }
        val listener = launch {
            observeReminderSignals(room.onSubscription { roomSubscribed.complete(Unit); emit(Unit) }, settings) {
                received.trySend(Unit).getOrThrow()
            }
        }
        withTimeout(2_000) { roomSubscribed.await(); settingsStarted.await(); received.receive() }
        room.emit(Unit)
        withTimeout(2_000) { received.receive() }
        allowSettings.complete(Unit)
        withTimeout(2_000) { received.receive() }
        listener.cancelAndJoin()
    }

    @Test fun channel_failure_is_reported_without_blocking_an_independent_request() {
        var failures = 0
        var requests = 0
        ensureReminderChannelSafely({ throw IllegalStateException("channel") }, { failures++ })
        requests++ // the independent scheduling path remains reachable
        assertEquals(1, failures)
        assertEquals(1, requests)
    }

    @Test fun channel_cancellation_is_not_hidden() {
        val cancellation = kotlinx.coroutines.CancellationException("cancelled")
        val actual = runCatching { ensureReminderChannelSafely({ throw cancellation }, {}) }.exceptionOrNull()
        org.junit.Assert.assertSame(cancellation, actual)
    }

}
