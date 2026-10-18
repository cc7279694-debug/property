package com.cooper.lithic.data.media.jpegworker

import android.app.Application

/** The verified main process installs only an application Context; it does not create another owner. */
internal object JpegWorkerRuntime {
    @Volatile private var host:Application?=null
    fun installHost(app:Application) {
        check(JpegProcessRole.role(app.packageName)==JpegProcessRole.Role.MAIN) {"JPEG Host context role unknown"}
        synchronized(this) {
            check(host==null||host===app) {"Conflicting JPEG Host context"}
            host=app
        }
    }
    fun requireHostContext():Application = host ?: throw IllegalStateException("Verified JPEG Host context is not installed")
}
