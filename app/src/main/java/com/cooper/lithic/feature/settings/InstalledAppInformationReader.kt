package com.cooper.lithic.feature.settings

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

/** Queries only our installed package. No device identifiers, signing secrets or private paths. */
@Suppress("DEPRECATION")
internal fun readInstalledAppInformation(context: Context): InstalledAppInformation = try {
    val manager = context.packageManager
    val info = manager.getPackageInfo(context.packageName, 0)
    val application = context.applicationInfo
    InstalledAppInformation(
        applicationName = manager.getApplicationLabel(application).toString(),
        versionName = info.versionName,
        versionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(),
        packageName = context.packageName,
        debuggable = application.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
    )
} catch (_: PackageManager.NameNotFoundException) {
    InstalledAppInformation()
} catch (_: RuntimeException) {
    InstalledAppInformation()
}
