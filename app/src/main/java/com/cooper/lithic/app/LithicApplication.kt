package com.cooper.lithic.app

import android.app.Application
import java.time.ZoneId
import com.cooper.lithic.data.media.jpegworker.JpegProcessRole
import com.cooper.lithic.data.media.jpegworker.JpegWorkerRuntime

class LithicApplication : Application() {
    val appContainer: AppContainer by lazy {
        check(JpegProcessRole.role(packageName) == JpegProcessRole.Role.MAIN) { "Storage owner is unavailable outside the verified main process" }
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()
        if (JpegProcessRole.role(packageName) != JpegProcessRole.Role.MAIN) return
        JpegWorkerRuntime.installHost(this)
        val recordZone = ZoneId.systemDefault()
        appContainer.start(recordZone)
    }
}
