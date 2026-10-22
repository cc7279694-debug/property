package com.cooper.lithic.platform.notification

import android.content.Context
import android.util.Log
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.app.StorageInitializationState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Process observation only; persistent execution remains WorkManager's responsibility. */
class ReminderRuntime(private val context: Context, private val container: AppContainer) {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    fun start() {
        try { NotificationPublisher(context).ensureChannel(); ReminderScheduling.start(context) }
        catch (_: Exception) { Log.e("LITHIC_REMINDER","Reminder scheduling not initialized") }
        scope.launch {
            container.storageState.collectLatest { ready ->
                if (ready is StorageInitializationState.Ready) {
                    try {
                        ReminderScheduling.request(context)
                        merge(ready.generation.settings.reminderChanges,
                            ready.generation.database.invalidationTracker.createFlow("long_term_projects","subscriptions","subscription_renewals","subscription_state_changes",emitInitialState=false).map { Unit })
                            .collect { ReminderScheduling.request(context) }
                    } catch (cancelled:CancellationException) { throw cancelled }
                    catch (_:Exception) { Log.e("LITHIC_REMINDER","Reminder coordination unavailable; business storage unchanged") }
                }
            }
        }
    }
}
