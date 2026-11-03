package com.cooper.lithic.platform.notification

import kotlinx.coroutines.CancellationException

/** Reconcile renewals before waiting for milestone work to be enqueued. Ordinary failures
 * do not skip the other domain; the worker's bounded retry policy remains in force. */
internal suspend fun reconcileReminderDomains(
    requestMilestones: suspend () -> Unit,
    reconcileRenewals: suspend () -> Unit,
) {
    val renewalFailure = try {
        reconcileRenewals()
        null
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        failure
    }
    try {
        requestMilestones()
    } catch (cancelled: CancellationException) {
        if (renewalFailure != null && renewalFailure !== cancelled) cancelled.addSuppressed(renewalFailure)
        throw cancelled
    } catch (failure: Exception) {
        if (renewalFailure != null) {
            if (renewalFailure !== failure) renewalFailure.addSuppressed(failure)
            throw renewalFailure
        }
        throw failure
    }
    if (renewalFailure != null) throw renewalFailure
}
