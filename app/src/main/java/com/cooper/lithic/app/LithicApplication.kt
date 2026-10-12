package com.cooper.lithic.app

import android.app.Application
import java.time.ZoneId

class LithicApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        val recordZone = ZoneId.systemDefault()
        appContainer.start(recordZone)
    }
}
