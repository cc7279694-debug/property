package com.cooper.lithic.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class M20ReminderSettingsTest {
    @Test fun defaultsToggleWatermarkAndReopenUseOneSettingsOwner() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.noBackupFilesDir, "m20-settings-${UUID.randomUUID()}").apply { check(mkdir()) }
        val file = File(root, "settings.preferences_pb")
        val gate = LocalWriteCoordinator()
        val settings = SettingsStore.open(file, gate, ZoneId.of("Asia/Shanghai"))
        try {
            assertEquals(ReminderPreferences(), settings.reminderPreferences())
            assertEquals(ReminderPreferences(false, true), settings.updateReminderPreferences(renewalEnabled = false))
            gate.withWrite {
                settings.restoreRemindersWithinWrite(gate, ReminderPreferences(false, false, LocalDate.parse("2026-10-22")))
            }
        } finally { settings.close() }
        val reopened = SettingsStore.open(file, LocalWriteCoordinator(), requireExisting = true)
        try {
            assertEquals(ReminderPreferences(false, false, LocalDate.parse("2026-10-22")), reopened.reminderPreferences())
            assertEquals(ReminderPreferences(true, false, LocalDate.parse("2026-10-22")), reopened.updateReminderPreferences(renewalEnabled = true))
        } finally { reopened.close() }
    }
}
