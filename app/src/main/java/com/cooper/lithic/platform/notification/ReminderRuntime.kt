package com.cooper.lithic.platform.notification

import android.content.Context
import android.util.Log
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.app.ReminderOwnerLease
import com.cooper.lithic.data.backup.ManagedGeneration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Process observation only; persistent execution remains WorkManager's responsibility. */
class ReminderRuntime(private val context: Context, private val container: AppContainer) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val coordinator = ReminderEnqueueCoordinator(scope, ReminderWorkManagerPort(context), 15_000,
        onOutcome = { kind, status -> Log.w("LITHIC_REMINDER", "Reminder enqueue $kind: $status") })

    internal fun request(lease: ReminderOwnerLease<ManagedGeneration>): StateFlow<EnqueueStatus> =
        coordinator.request(EnqueueKind.RECONCILE, lease)

    fun start() {
        // Channel failures do not prevent the two independent scheduling lanes.
        scope.launch {
            ensureReminderChannelSafely(
                { NotificationPublisher(context).ensureChannel() },
                { Log.e("LITHIC_REMINDER", "Notification channel unavailable") },
            )
        }
        scope.launch {
            container.reminderOwnerLease.collectLatest { lease ->
                if (lease != null && lease.isActive()) {
                    try {
                        coordinator.request(EnqueueKind.PERIODIC, lease)
                        val generation = lease.owner
                        // Both sources emit current facts on subscription. This gives each
                        // late subscriber a full refresh without waiting on an Operation.
                        observeReminderSignals(
                            generation.database.invalidationTracker.createFlow(
                                "long_term_projects", "subscriptions", "subscription_renewals",
                                "subscription_state_changes", emitInitialState = true).map { Unit },
                            generation.settings.reminderChanges,
                        ) { request(lease) }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { Log.e("LITHIC_REMINDER", "Reminder observation unavailable; business storage unchanged") }
                    finally { coordinator.retire(lease) }
                }
            }
        }
    }
}
