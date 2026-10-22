package com.cooper.lithic.app

import android.app.Application
import androidx.work.Configuration
import com.cooper.lithic.platform.notification.ReminderRuntime
import com.cooper.lithic.platform.notification.ReminderWorkerFactory
import java.time.ZoneId

class LithicApplication : Application(), Configuration.Provider {
    val appContainer: AppContainer by lazy { AppContainer(this) }
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(ReminderWorkerFactory(appContainer)).build()
    private lateinit var reminders: ReminderRuntime

    override fun onCreate() {
        super.onCreate()
        val recordZone = ZoneId.systemDefault()
        appContainer.start(recordZone)
        reminders = ReminderRuntime(this, appContainer).also { it.start() }
    }
}
