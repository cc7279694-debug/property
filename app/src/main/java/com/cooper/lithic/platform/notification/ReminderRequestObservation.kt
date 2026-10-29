package com.cooper.lithic.platform.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CancellationException

/** A failed platform request must not discard later changes from the same generation.
 * Cancellation and upstream observation failures still belong to the caller. */
internal suspend fun observeReminderRequests(
    changes: Flow<Unit>,
    request: () -> Unit,
    onRequestFailed: (Exception) -> Unit,
) {
    fun requestIndependently() {
        try { request() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { onRequestFailed(failure) }
    }

    requestIndependently()
    changes.collect { requestIndependently() }
}
