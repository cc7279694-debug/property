package com.cooper.lithic.platform.notification

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** No Worker, periodic retry, ledger access or owner lock is created for cleanup. */
internal object ReminderLegacyCancellation {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var runner: LegacyReminderCancellationRunner? = null

    fun start(context: Context) {
        if (ReminderCapability.AUTOMATIC_NOTIFICATIONS) return
        val application = context.applicationContext
        val instance = synchronized(lock) {
            runner ?: LegacyReminderCancellationRunner(
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
            ).also { runner = it }
        }
        instance.start()
    }
}
