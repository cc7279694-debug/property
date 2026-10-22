package com.cooper.lithic.feature.settings

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cooper.lithic.app.LithicApplication
import com.cooper.lithic.platform.notification.NotificationPublisher
import com.cooper.lithic.platform.notification.ReminderScheduling

/** Permission prompts happen only after an explicit button press. Isolated hosts never
 * implicitly initialize a production AppContainer or persistent WorkManager. */
@Composable
internal fun NotificationSettingsEntry(viewModel: NotificationSettingsViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val publisher = remember(context) { NotificationPublisher(context) }
    fun access(): NotificationPermissionState = publisher.access().let {
        NotificationPermissionState(it.requiresRuntimePermission, it.runtimeGranted, it.appEnabled, it.channelEnabled)
    }
    var permission by remember(context) { mutableStateOf(access()) }
    fun refresh() {
        permission = access()
        viewModel.refresh()
        if (context.applicationContext is LithicApplication) ReminderScheduling.request(context)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel, context) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    NotificationSettingsScreen(viewModel, permission,
        onRequestPermission = {
            if (permission.requiresRuntimePermission) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onOpenSystemSettings = {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }, onBack = onBack)
}
