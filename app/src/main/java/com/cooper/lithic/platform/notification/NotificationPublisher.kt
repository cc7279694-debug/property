package com.cooper.lithic.platform.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.cooper.lithic.R
import com.cooper.lithic.app.MainActivity
import com.cooper.lithic.domain.reminder.RenewalReminderCandidate

/** No permission request, network call, foreground service or ledger owner is created here. */
class NotificationPublisher(private val context: Context) : ReminderPublisher {
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    fun ensureChannel() {
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "续费提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "依据砺石已记录的到期日尽力提醒，不代表服务商扣款。"
        })
    }
    fun access(): NotificationAccess {
        val runtime = Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return NotificationAccess(Build.VERSION.SDK_INT >= 33, runtime, manager.areNotificationsEnabled(),
            manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)
    }
    override fun canPublish(): Boolean = access().allowed
    override fun publish(candidate: RenewalReminderCandidate): Boolean {
        ensureChannel()
        if (!canPublish()) return false
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_launcher_placeholder)
            .setContentTitle(candidate.title).setContentText(candidate.text)
            .setStyle(Notification.BigTextStyle().bigText(candidate.text)).setContentIntent(open)
            .setOnlyAlertOnce(true).setAutoCancel(true).build()
        try {
            manager.notify(candidate.logicalKey, candidate.notificationId, notification)
        } catch (_: SecurityException) { return false }
        return true
    }
    companion object { const val CHANNEL = "lithic-renewals-v1" }
}

data class NotificationAccess(val requiresRuntimePermission:Boolean, val runtimeGranted:Boolean,
    val appEnabled:Boolean, val channelEnabled:Boolean) {
    val allowed get() = runtimeGranted && appEnabled && channelEnabled
}
