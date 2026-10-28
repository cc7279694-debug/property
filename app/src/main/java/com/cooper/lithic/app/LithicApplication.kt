package com.cooper.lithic.app

import android.app.Application
import android.os.Build
import androidx.work.Configuration
import com.cooper.lithic.platform.notification.ReminderRuntime
import com.cooper.lithic.platform.notification.ReminderWorkerFactory
import java.io.FileInputStream
import java.time.ZoneId

class LithicApplication : Application(), Configuration.Provider {
    /** Resolved before any owner/configuration access, including access before onCreate. */
    val processRole: AppProcessRole by lazy { resolveCurrentProcessRole() }
    private val appContainerDelegate = lazy { AppContainer(this) }
    val appContainer: AppContainer
        get() {
            check(processRole == AppProcessRole.MAIN) { "Business storage is unavailable in this process" }
            return appContainerDelegate.value
        }
    internal val businessOwnerInitialized: Boolean get() = appContainerDelegate.isInitialized()

    override val workManagerConfiguration: Configuration
        get() {
            check(processRole == AppProcessRole.MAIN) { "Business scheduling is unavailable in this process" }
            return Configuration.Builder().setWorkerFactory(ReminderWorkerFactory(appContainer)).build()
        }
    private lateinit var reminders: ReminderRuntime

    override fun onCreate() {
        super.onCreate()
        if (processRole != AppProcessRole.MAIN) return
        val recordZone = ZoneId.systemDefault()
        appContainer.start(recordZone)
        reminders = ReminderRuntime(this, appContainer).also { it.start() }
    }

    private fun resolveCurrentProcessRole(): AppProcessRole = try {
        val actual = if (Build.VERSION.SDK_INT >= 28) Application.getProcessName() else ownProcName()
        AppProcessRoles.resolve(applicationInfo.processName, actual)
    } catch (_: Exception) {
        // Unknown identity never falls back to the main process or opens business storage.
        AppProcessRole.UNKNOWN
    }

    private fun ownProcName(): String? = FileInputStream("/proc/self/cmdline").use { input ->
        val bytes = ByteArray(AppProcessRoles.MAX_PROCESS_NAME_BYTES)
        var count = 0
        while (count < bytes.size) {
            val read = input.read(bytes, count, bytes.size - count)
            if (read <= 0) break
            count += read
            if (bytes.take(count).any { it == 0.toByte() }) break
        }
        AppProcessRoles.decodeOwnProcessName(bytes.copyOf(count))
    }
}
