package com.cooper.lithic.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.data.local.NotificationReceiptEntity
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import com.cooper.lithic.data.settings.ReminderPreferences
import com.cooper.lithic.data.settings.SettingsStore
import java.io.File
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.util.AtomicFile
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** New M20 synthetic roots only. This source does not authorize a device run. */
@RunWith(AndroidJUnit4::class)
class M20BackupRestoreTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun formatThreePreservesAllFactsReceiptSwitchesMediaBytesAndPrivateCutoffBeforeActivation() = runBlocking {
        val root = newRoot("v3")
        val store = DataGenerationStore(context, root, restoreClock = CLOCK)
        var active: ManagedGeneration? = store.open(ZONE)
        try {
            val current = requireNotNull(active)
            val media = RestoreSyntheticFixture.insert(current)
            val subscription = current.database.subscriptionDao().validationSubscriptions(null, 100).single()
            val receipt = NotificationReceiptEntity("RENEWAL:${subscription.id}:2026-03-01", subscription.id,
                "RENEWAL", "2026-03-01", Instant.parse("2026-02-26T01:00:00Z"))
            current.coordinator.withWrite {
                current.database.notificationReceiptDao().insert(receipt)
                current.settings.restoreRemindersWithinWrite(current.coordinator,
                    ReminderPreferences(false, false, LocalDate.parse("2026-01-01")))
            }
            val before = current.backupSnapshotSource().capture()
            val archive = File(root, "roundtrip.lithicbackup")
            assertTrue(BackupExportService(context, current).export(RestoreSyntheticFixture.target(archive)) is BackupExportResult.Success)
            val backup = BackupValidator().validate(archive.toPath(), Files.createDirectory(root.toPath().resolve("payload")))
            assertEquals(3, backup.manifest.formatVersion)
            assertEquals(4, backup.manifest.sourceDatabaseSchemaVersion)
            assertEquals(before, backup.snapshot)
            val candidate = store.prepareRestore(ZONE) { RestoredGenerationWriter().populate(it, backup) }
            assertEquals(3, JSONObject(File(candidate.directory, "restore-proof.json").readText()).getInt("dataFormatVersion"))
            val candidateSettings = SettingsStore.open(File(candidate.directory, "settings.preferences_pb"), LocalWriteCoordinator(), requireExisting = true)
            try { assertEquals(LocalDate.parse("2026-10-22"), candidateSettings.reminderPreferences().importCutoff) }
            finally { candidateSettings.close() }
            val expected = before.copy(reminderPreferences = before.reminderPreferences.copy(importCutoff = LocalDate.parse("2026-10-22")))
            val restored = store.activateRestore(current.id, candidate, before)
            active = restored
            assertEquals(expected, restored.backupSnapshotSource().capture())
            assertEquals(receipt, restored.database.notificationReceiptDao().find(receipt.logicalKey))
            media.forEach { (path, bytes) -> assertArrayEquals(bytes, File(restored.directory, path).readBytes()) }
            val id = restored.id
            restored.close(); active = null
            val reopened = DataGenerationStore(context, root, restoreClock = CLOCK).open()
            active = reopened
            assertEquals(id, reopened.id)
            assertEquals(expected, reopened.backupSnapshotSource().capture())
            assertEquals(4, reopened.database.openHelper.readableDatabase.version)
        } finally { active?.close() }
    }

    @Test fun authenticLegacyFormatOneRestoresNoInventedReceiptsAndEstablishesCutoffBeforeActivation() = runBlocking {
        val root = newRoot("v1")
        val store = DataGenerationStore(context, root, restoreClock = CLOCK)
        var active: ManagedGeneration? = store.open(ZONE)
        try {
            val current = requireNotNull(active)
            val archive = File(root, "legacy-v1.lithicbackup")
            InstrumentationRegistry.getInstrumentation().context.assets.open("m16/legacy-v1.lithicbackup").use { input ->
                archive.outputStream().use { input.copyTo(it) }
            }
            val backup = BackupValidator().validate(archive.toPath(), Files.createDirectory(root.toPath().resolve("payload")))
            val candidate = store.prepareRestore(ZONE) { RestoredGenerationWriter().populate(it, backup) }
            val restored = store.activateRestore(current.id, candidate)
            active = restored
            assertEquals(backup.snapshot.data, restored.backupSnapshotSource().capture().data)
            assertTrue(restored.database.notificationReceiptDao().list().isEmpty())
            assertEquals(ReminderPreferences(true, true, LocalDate.parse("2026-10-22")), restored.settings.reminderPreferences())
            val id = restored.id
            restored.close(); active = null
            active = DataGenerationStore(context, root, restoreClock = CLOCK).open()
            assertEquals(id, requireNotNull(active).id)
            assertEquals(LocalDate.parse("2026-10-22"), requireNotNull(active).settings.reminderPreferences().importCutoff)
        } finally { active?.close() }
    }

    @Test fun authenticFormatTwoRestoresOldEightTablesAndMediaWithDefaultPreferencesAndNoHistoricalReceipt() = runBlocking {
        val root = newRoot("v2")
        val store = DataGenerationStore(context, root, restoreClock = CLOCK)
        var active: ManagedGeneration? = store.open(ZONE)
        try {
            val current = requireNotNull(active)
            val media = RestoreSyntheticFixture.insert(current)
            val before = current.backupSnapshotSource().capture()
            val payload = linkedMapOf("data.json" to BackupJson.encodeData(before.data), "settings.json" to BackupJson.encodeSettings(before.settings))
            payload.putAll(media)
            val manifest = BackupManifestV1(2, 2, "m20-legacy2-synthetic", 3, CLOCK.instant().toString(), ZONE.id,
                before.data.recordCounts(), payload.map { (path, bytes) -> BackupFileEntry.fromBytes(path, bytes) })
            val archive = File(root, "legacy-v2.lithicbackup")
            ZipOutputStream(archive.outputStream()).use { zip ->
                for ((path, bytes) in linkedMapOf("manifest.json" to manifest.encode()) + payload) {
                    zip.putNextEntry(ZipEntry(path)); zip.write(bytes); zip.closeEntry()
                }
            }
            val backup = BackupValidator().validate(archive.toPath(), Files.createDirectory(root.toPath().resolve("payload")))
            assertEquals(2, backup.manifest.formatVersion)
            val candidate = store.prepareRestore(ZONE) { RestoredGenerationWriter().populate(it, backup) }
            val restored = store.activateRestore(current.id, candidate, before)
            active = restored
            assertEquals(before.data, restored.backupSnapshotSource().capture().data)
            assertEquals(ReminderPreferences(true, true, LocalDate.parse("2026-10-22")), restored.settings.reminderPreferences())
            assertTrue(restored.database.notificationReceiptDao().list().isEmpty())
            media.forEach { (path, bytes) -> assertArrayEquals(bytes, File(restored.directory, path).readBytes()) }
        } finally { active?.close() }
    }

    @Test fun interruptedLegacyProofOneAndTwoAuthenticateThenPersistCutoffAndProofThreeBeforeStable() = runBlocking {
        for (format in 1..2) {
            val root = newRoot("legacy-proof-$format")
            val store = DataGenerationStore(context, root, restoreClock = CLOCK)
            var active: ManagedGeneration? = store.open(ZONE)
            try {
                val old = requireNotNull(active)
                val archive = File(root, "legacy-v1.lithicbackup")
                InstrumentationRegistry.getInstrumentation().context.assets.open("m16/legacy-v1.lithicbackup").use { input ->
                    archive.outputStream().use { input.copyTo(it) }
                }
                val backup = BackupValidator().validate(archive.toPath(), Files.createDirectory(root.toPath().resolve("payload")))
                val candidate = store.prepareRestore(ZONE) { RestoredGenerationWriter().populate(it, backup) }
                val gate = LocalWriteCoordinator()
                val settings = SettingsStore.open(File(candidate.directory, "settings.preferences_pb"), gate, requireExisting = true)
                try { gate.withWrite { settings.restoreRemindersWithinWrite(gate, ReminderPreferences()) } }
                finally { settings.close() }
                val dataBytes = if (format == 1) BackupJson.encodeData(backup.snapshot.data.legacyProjection()) else BackupJson.encodeData(backup.snapshot.data)
                val proof = JSONObject().put("formatVersion", 1).put("generationId", candidate.id).put("nonce", candidate.nonce)
                    .put("dataSha256", sha(dataBytes)).put("settingsSha256", sha(BackupJson.encodeSettings(backup.snapshot.settings)))
                if (format == 2) proof.put("dataFormatVersion", 2)
                File(candidate.directory, "restore-proof.json").writeText(proof.toString())
                old.close(); active = null
                val pointer = AtomicFile(File(root, "active-generation"))
                val stream = pointer.startWrite()
                try {
                    stream.write(JSONObject().put("formatVersion", 1).put("activeId", candidate.id).put("previousId", old.id)
                        .put("stage", "SWITCHING").toString().toByteArray())
                    pointer.finishWrite(stream)
                } catch (failure: Throwable) { pointer.failWrite(stream); throw failure }
                val resumedStore = DataGenerationStore(context, root, restoreClock = CLOCK)
                val resumed = resumedStore.open()
                active = resumed
                assertEquals(candidate.id, resumed.id)
                assertEquals(backup.snapshot.data, resumed.backupSnapshotSource().capture().data)
                assertEquals(ReminderPreferences(true, true, LocalDate.parse("2026-10-22")), resumed.settings.reminderPreferences())
                assertEquals(3, JSONObject(File(candidate.directory, "restore-proof.json").readText()).getInt("dataFormatVersion"))
                assertEquals("STABLE", JSONObject(File(root, "active-generation").readText()).getString("stage"))
                assertNull(resumedStore.lastRecoveryWarning)
            } finally { active?.close() }
        }
    }

    private fun newRoot(kind: String) = File(context.noBackupFilesDir, "m20-backup-$kind-${UUID.randomUUID()}").apply { check(mkdir()) }
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    private companion object {
        val ZONE: ZoneId = ZoneId.of("Asia/Shanghai")
        val CLOCK: Clock = Clock.fixed(Instant.parse("2026-10-22T02:00:00Z"), ZoneOffset.UTC)
    }
}
