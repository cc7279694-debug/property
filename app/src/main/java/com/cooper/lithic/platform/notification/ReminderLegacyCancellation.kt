package com.cooper.lithic.platform.notification

import android.content.Context

/** Compatibility entry: V1 never mutates the legacy WorkManager task graph. */
internal object ReminderLegacyCancellation {
    @Suppress("UNUSED_PARAMETER")
    fun start(context: Context) = Unit
}
