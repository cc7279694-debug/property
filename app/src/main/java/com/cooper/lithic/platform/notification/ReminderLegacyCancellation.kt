package com.cooper.lithic.platform.notification

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** No Worker, periodic retry, ledger access or owner lock is created for cleanup. */
internal object ReminderLegacyCancellation {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val attempted = AtomicBoolean(false)

    fun start(context: Context) {
        if (ReminderCapability.AUTOMATIC_NOTIFICATIONS || !attempted.compareAndSet(false, true)) return
        // The caller is the Application, never an Activity. Even resolving its context or
        // constructing the runner happens on IO, outside the normal UI startup path.
        scope.launch {
            try {
                val application = context.applicationContext
                val instance = LegacyReminderCancellationRunner(
                    dispatch = { block -> scope.launch { block() }; Unit },
                    cancel = { target ->
                        val manager = WorkManager.getInstance(application)
                        val operation = when (target) {
                            is LegacyReminderTarget.Unique -> manager.cancelUniqueWork(target.name)
                            is LegacyReminderTarget.Tag -> manager.cancelAllWorkByTag(target.tag)
                        }
                        LegacyCancellationOperation { listener ->
                            val future = operation.result
                            future.addListener({
                                val outcome = try {
                                    future.get()
                                    LegacyCancellationOutcome.CONFIRMED
                                } catch (_: ExecutionException) {
                                    LegacyCancellationOutcome.FAILED
                                } catch (_: CancellationException) {
                                    LegacyCancellationOutcome.UNKNOWN
                                } catch (_: InterruptedException) {
                                    Thread.currentThread().interrupt()
                                    LegacyCancellationOutcome.UNKNOWN
                                } catch (_: Exception) {
                                    LegacyCancellationOutcome.UNKNOWN
                                }
                                listener(outcome)
                            }, Executor { command -> command.run() })
                        }
                    },
                    record = { target, outcome -> Log.i("LITHIC_REMINDER", "Legacy cancellation $target: $outcome") },
                )
                instance.start()
            } catch (cancelled: CancellationException) {
                recordUnknown()
                throw cancelled
            } catch (_: Exception) {
                recordUnknown()
            }
        }
    }

    private fun recordUnknown() {
        try { Log.e("LITHIC_REMINDER", "Legacy cancellation preparation outcome unknown") }
        catch (_: Exception) { /* A diagnostic failure does not change the unknown result. */ }
    }
}
