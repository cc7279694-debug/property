package com.cooper.lithic.platform.notification

import java.util.concurrent.atomic.AtomicBoolean

/** These are the only WorkManager namespaces created by the legacy reminder feature. */
internal sealed interface LegacyReminderTarget {
    data class Unique(val name: String) : LegacyReminderTarget
    data class Tag(val tag: String) : LegacyReminderTarget
}

internal enum class LegacyCancellationOutcome { CONFIRMED, FAILED, UNKNOWN }
internal fun interface LegacyCancellationOperation {
    fun observe(listener: (LegacyCancellationOutcome) -> Unit)
}

/**
 * Five independent, one-shot requests per process. A missing callback, cancellation of
 * observation, or an exception cannot prove that WorkManager applied cancellation.
 * A duplicate or late callback cannot turn an already confirmed result into another one.
 */
internal class LegacyReminderCancellationRunner(
    private val dispatch: (() -> Unit) -> Unit,
    private val cancel: (LegacyReminderTarget) -> LegacyCancellationOperation,
    private val record: (LegacyReminderTarget, LegacyCancellationOutcome) -> Unit,
) {
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        TARGETS.forEach { target ->
            safeRecord(target, LegacyCancellationOutcome.UNKNOWN)
            try {
                dispatch {
                    val terminal = AtomicBoolean(false)
                    try {
                        cancel(target).observe { outcome ->
                            if (outcome != LegacyCancellationOutcome.UNKNOWN && terminal.compareAndSet(false, true)) {
                                safeRecord(target, outcome)
                            }
                        }
                    } catch (_: Exception) {
                        // Even a synchronous failure may follow a platform side effect.
                        // UNKNOWN already recorded; never retry or infer cancellation.
                    }
                }
            } catch (_: Exception) {
                // A rejected dispatch leaves only this target UNKNOWN.
            }
        }
    }

    private fun safeRecord(target: LegacyReminderTarget, outcome: LegacyCancellationOutcome) {
        try { record(target, outcome) } catch (_: Exception) { /* Logging is not a cancellation result. */ }
    }

    companion object {
        val TARGETS: List<LegacyReminderTarget> = listOf(
            LegacyReminderTarget.Unique("lithic-renewal-daily-v1"),
            LegacyReminderTarget.Unique("lithic-renewal-reconcile-v1"),
            LegacyReminderTarget.Unique("lithic-milestone-reconcile-v1"),
            LegacyReminderTarget.Tag("lithic-renewal-candidate-v1"),
            LegacyReminderTarget.Tag("lithic-milestone-candidate-v1"),
        )
    }
}
