package com.cooper.lithic.platform.notification

import kotlinx.coroutines.CancellationException

/** Only failures before the platform submission call can prove this request was not submitted. */
internal inline fun <P : Any, R : Any> prepareAndSubmitEnqueue(
    prepare: () -> P,
    submit: (P) -> R,
): R {
    val prepared = try {
        prepare()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        throw EnqueueNotSubmittedException("Reminder enqueue preparation failed", failure)
    }
    // Do not catch this call: even a synchronous exception can follow a platform side effect.
    return submit(prepared)
}
