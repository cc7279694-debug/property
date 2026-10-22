package com.cooper.lithic.data.backup

import com.cooper.lithic.data.settings.ReminderPreferences
import java.time.Instant
import java.time.ZoneId

/** Imported triggers on or before this business day are never rebuilt as new notifications. */
internal object RestoreReminderProtection {
    fun preferences(value: ReminderPreferences, zone: ZoneId, restoredAt: Instant): ReminderPreferences {
        val today = restoredAt.atZone(zone).toLocalDate()
        return value.copy(importCutoff = maxOf(value.importCutoff ?: today, today))
    }
}
