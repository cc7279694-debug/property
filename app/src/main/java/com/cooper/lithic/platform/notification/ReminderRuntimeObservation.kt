package com.cooper.lithic.platform.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.merge

/** Both sources must emit their current fact when collection begins. Enqueue requests are
 * non-blocking: a slow WorkManager Operation must not suspend change observation. */
internal suspend fun observeReminderSignals(
    roomChanges: Flow<Unit>,
    settingsChanges: Flow<Unit>,
    onSignal: () -> Unit,
) {
    merge(roomChanges, settingsChanges).collect { onSignal() }
}

/** Channel setup failure is reported without coupling it to scheduling initialization. */
internal fun ensureReminderChannelSafely(ensure: () -> Unit, onFailure: (Exception) -> Unit) {
    try { ensure() }
    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
    catch (failure: Exception) { onFailure(failure) }
}
