package com.cooper.lithic.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.AtomicFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.data.local.MilestoneWatermarkEntity
import com.cooper.lithic.data.local.NotificationReceiptEntity
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import com.cooper.lithic.data.settings.SettingsStore
import com.cooper.lithic.domain.reminder.MilestoneOwnerType
import com.cooper.lithic.testutil.M20TestRoots
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Compiled source only until a separately approved clean Android environment is available. */
@RunWith(AndroidJUnit4::class)
class M21BackupRestoreTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val roots = M20TestRoots()
    @After fun cleanupClosed() { roots.cleanupClosed() }

    @Test fun currentFormatFourRoundTripPreservesFactsMediaReceiptsAndMergesWatermarksBeforeProofAndActivation() = runBlocking {
        val root = newRoot("roundtrip")
        val store = DataGenerationStore(context, root, restoreClock = CLOCK)
        var active: ManagedGeneration? = store.open(ZONE)
        roots.generation(root, requireNotNull(active).id)
        try {
            val current = requireNotNull(active)
            val media = RestoreSyntheticFixture.insert(current)
            roots.media(root, media.keys, current.id)
            val snapshot = current.backupSnapshotSource().capture()
            val project = snapshot.data.projects.single { it.endedOn == null }
            val receipt = NotificationReceiptEntity("MILESTONE:PROJECT:${project.id}:D100", project.id,
                "PROJECT_MILESTONE", "D100", Instant.parse("2026-04-10T01:00:00Z"))
            current.coordinator.withWrite {
                current.database.notificationReceiptDao().insert(receipt)
                snapshot.data.projects.forEach { row -> current.database.milestoneWatermarkDao().upsert(
                    MilestoneWatermarkEntity(MilestoneOwnerType.PROJECT, row.id, LocalDate.parse("2026-01-01"), 0)) }
                snapshot.data.subscriptions.forEach { row -> current.database.milestoneWatermarkDao().upsert(
                    MilestoneWatermarkEntity(MilestoneOwnerType.SUBSCRIPTION, row.id, LocalDate.parse("2026-01-01"), 0)) }
            }
            val before = current.backupSnapshotSource().capture()
            val archive = File(root, "backup.lithicbackup")
            assertTrue(BackupExportService(context, current).export(RestoreSyntheticFixture.target(archive)) is BackupExportResult.Success)
            val validated = BackupValidator().validate(archive.toPath(), Files.createDirectory(root.toPath().resolve("payload")))
            roots.media(root, validated.snapshot.data.mediaAssets.map { it.relativePath })
            assertEquals(4, validated.manifest.formatVersion)
            assertEquals(4, validated.manifest.minReaderFormat)
            assertEquals(5, validated.manifest.sourceDatabaseSchemaVersion)
            assertEquals(10, validated.manifest.recordCounts.size)
            assertEquals(before, validated.snapshot)
            val candidate = store.prepareRestore(ZONE) { RestoredGenerationWriter().populate(it, validated) }
            roots.generation(root, candidate.id); roots.media(root, media.keys, candidate.id)
            val proof = JSONObject(File(candidate.directory, "restore-proof.json").readText())
            assertEquals(1, proof.getInt("formatVersion"))
            assertEquals(4, proof.getInt("dataFormatVersion"))
            val expected = RestoreReminderProtection.snapshot(before, CLOCK.instant())
            assertEquals(1, expected.milestoneWatermarks.single { it.ownerType == "PROJECT" && it.ownerId == project.id }.suppressedMask)
            assertEquals(0, expected.milestoneWatermarks.single { it.ownerType == "SUBSCRIPTION" }.suppressedMask)
            val restored = store.activateRestore(current.id, candidate, before)
            active = restored
            assertEquals(expected, restored.backupSnapshotSource().capture())
            assertEquals(receipt, restored.database.notificationReceiptDao().list().single())
            media.forEach { (path, bytes) -> assertArrayEquals(bytes, File(restored.directory, path).readBytes()) }
            val id = restored.id
            restored.close(); active = null
            active = DataGenerationStore(context, root, restoreClock = CLOCK).open()
            assertEquals(id, requireNotNull(active).id)
            assertEquals(expected, requireNotNull(active).backupSnapshotSource().capture())
        } finally { active?.let { it.close(); roots.ownersClosed(root) } }
    }

    @Test fun interruptedAuthenticProofThreeAndSchemaFourAreAuthenticatedBeforeUpgradeAndProtection() = runBlocking {
        val root = newRoot("legacy-proof3")
        val store = DataGenerationStore(context, root, restoreClock = CLOCK)
        var active: ManagedGeneration? = store.open(ZONE)
        roots.generation(root, requireNotNull(active).id)
        try {
            val old = requireNotNull(active)
            val media = RestoreSyntheticFixture.insert(old)
            roots.media(root, media.keys, old.id)
            val subscription = old.database.subscriptionDao().validationSubscriptions(null, 100).single()
            val receipt = NotificationReceiptEntity("RENEWAL:${subscription.id}:2026-03-01", subscription.id,
                "RENEWAL", "2026-03-01", Instant.parse("2026-02-26T01:00:00Z"))
            old.coordinator.withWrite { old.database.notificationReceiptDao().insert(receipt) }
            val original = old.backupSnapshotSource().capture()
            val archive = File(root, "backup.lithicbackup")
            assertTrue(BackupExportService(context, old).export(RestoreSyntheticFixture.target(archive)) is BackupExportResult.Success)
            val validated = BackupValidator().validate(archive.toPath(), Files.createDirectory(root.toPath().resolve("payload")))
            roots.media(root, validated.snapshot.data.mediaAssets.map { it.relativePath })
            val candidate = store.prepareRestore(ZONE) { RestoredGenerationWriter().populate(it, validated) }
            roots.generation(root, candidate.id); roots.media(root, media.keys, candidate.id)
            // Fixture setup only: reproduce the exact original v4 schema and v3 proof,
            // rather than concealing new watermarks behind an old proof during production.
            val schema4 = InstrumentationRegistry.getInstrumentation().context.assets
                .open("com.cooper.lithic.data.local.LithicDatabase/4.json")
                .use { JSONObject(it.bufferedReader().readText()).getJSONObject("database") }
            SQLiteDatabase.openDatabase(File(candidate.directory, "data.db").path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL("DROP TABLE milestone_watermarks")
                db.execSQL("UPDATE room_master_table SET identity_hash=? WHERE id=42", arrayOf(schema4.getString("identityHash")))
                db.version = 4
            }
            val gate = LocalWriteCoordinator()
            val settings = SettingsStore.open(File(candidate.directory, "settings.preferences_pb"), gate, requireExisting = true)
            try { gate.withWrite { settings.restoreRemindersWithinWrite(gate, original.reminderPreferences) } }
            finally { settings.close() }
            val originalData = BackupJson.encodeData(original.legacyDataV3())
            val originalSettings = BackupJson.encodeSettings(original.currentSettings())
            val proof = JSONObject().put("formatVersion", 1).put("generationId", candidate.id).put("nonce", candidate.nonce)
                .put("dataFormatVersion", 3).put("dataSha256", sha(originalData)).put("settingsSha256", sha(originalSettings))
            File(candidate.directory, "restore-proof.json").writeText(proof.toString())
            old.close(); active = null
            val pointer = AtomicFile(File(root, "active-generation")); val stream = pointer.startWrite()
            try {
                stream.write(JSONObject().put("formatVersion", 1).put("activeId", candidate.id).put("previousId", old.id)
                    .put("stage", "SWITCHING").toString().toByteArray())
                pointer.finishWrite(stream)
            } catch (failure: Throwable) { pointer.failWrite(stream); throw failure }
            val resumedStore = DataGenerationStore(context, root, restoreClock = CLOCK)
            val resumed = resumedStore.open(); active = resumed
            assertEquals(candidate.id, resumed.id)
            assertEquals(5, resumed.database.openHelper.readableDatabase.version)
            assertEquals(RestoreReminderProtection.snapshot(original, CLOCK.instant()), resumed.backupSnapshotSource().capture())
            assertEquals(receipt, resumed.database.notificationReceiptDao().list().single())
            assertEquals(4, JSONObject(File(candidate.directory, "restore-proof.json").readText()).getInt("dataFormatVersion"))
            assertEquals("STABLE", JSONObject(File(root, "active-generation").readText()).getString("stage"))
            assertNull(resumedStore.lastRecoveryWarning)
            media.forEach { (path, bytes) -> assertArrayEquals(bytes, File(resumed.directory, path).readBytes()) }
        } finally { active?.let { it.close(); roots.ownersClosed(root) } }
    }

    private fun newRoot(kind: String) = roots.create(context, "m21-backup-$kind").also { root ->
        roots.files(root, "backup.lithicbackup", "payload/manifest.json", "payload/data.json", "payload/settings.json")
    }
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    private companion object {
        val ZONE: ZoneId = ZoneId.of("Asia/Shanghai")
        val CLOCK: Clock = Clock.fixed(Instant.parse("2026-10-22T02:00:00Z"), ZoneOffset.UTC)
    }
}
