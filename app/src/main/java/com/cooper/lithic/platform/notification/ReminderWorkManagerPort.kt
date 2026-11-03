package com.cooper.lithic.platform.notification

import android.content.Context
import android.util.Log
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

/** Observe the Operation terminal result without cancelling its underlying Future. */
internal class ReminderWorkManagerPort(private val context: Context) : EnqueuePort {
    override fun enqueue(kind: EnqueueKind): EnqueueOperation {
        val operation = ReminderScheduling.enqueue(context.applicationContext, kind)
        return EnqueueOperation { listener ->
            val future = operation.result
            future.addListener({
                val outcome = try {
                    future.get()
                    Result.success(Unit)
                } catch (cancelled: CancellationException) {
                    Result.failure(cancelled)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Result.failure(CancellationException("Operation observation interrupted"))
                } catch (failure: ExecutionException) {
                    Result.failure(failure.cause ?: failure)
                } catch (failure: Exception) {
                    // An observation failure is not proof that the platform rejected work.
                    Result.failure(CancellationException("Operation observation unavailable"))
                }
                if (outcome.isFailure) Log.w("LITHIC_REMINDER", "Reminder enqueue terminal result unavailable or failed")
                listener(outcome)
            }, Executor { it.run() })
        }
    }
}
