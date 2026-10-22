package com.cooper.lithic.data.backup

import java.io.OutputStream

/** The generation metadata protocol remains format 1; only its explicitly identified data digest changes. */
internal object RestoreProofData {
    fun write(snapshot:BackupSnapshot,dataFormatVersion:Int,output:OutputStream) {
        when(dataFormatVersion) {
            1 -> { requireLegacyReminders(snapshot); BackupJson.writeData(snapshot.data.legacyProjection(),output) }
            2 -> { requireLegacyReminders(snapshot); BackupJson.writeData(snapshot.data,output) }
            3 -> BackupJson.writeData(snapshot.currentData(),output)
            else -> throw IllegalArgumentException("Unknown restoration proof data format")
        }
    }

    fun writeSettings(snapshot: BackupSnapshot, dataFormatVersion: Int, output: OutputStream) {
        when (dataFormatVersion) {
            1, 2 -> { requireLegacyReminders(snapshot); BackupJson.writeSettings(snapshot.settings, output) }
            3 -> BackupJson.writeSettings(snapshot.currentSettings(), output)
            else -> throw IllegalArgumentException("Unknown restoration proof settings format")
        }
    }

    private fun requireLegacyReminders(snapshot: BackupSnapshot) {
        require(snapshot.notificationReceipts.isEmpty() && snapshot.reminderPreferences == com.cooper.lithic.data.settings.ReminderPreferences()) {
            "New reminder facts cannot disappear behind a legacy restoration proof"
        }
    }
}
