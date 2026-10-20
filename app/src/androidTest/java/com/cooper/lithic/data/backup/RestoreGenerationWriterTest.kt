package com.cooper.lithic.data.backup

import android.content.Context
import android.system.Os
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Failure candidates are isolated generations. Every assertion preserves the original pointer. */
@RunWith(AndroidJUnit4::class)
class RestoreGenerationWriterTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var root: File
    private lateinit var store: DataGenerationStore
    private lateinit var source: ManagedGeneration

    @Before fun before(): Unit = runBlocking {
        root = File(context.noBackupFilesDir, "m12-writer-${UUID.randomUUID()}").canonicalFile
        check(root.mkdir())
        store = DataGenerationStore(context, root)
        source = store.open(ZoneId.of("Asia/Shanghai"))
        RestoreSyntheticFixture.insert(source)
    }

    @After fun after(): Unit = runBlocking { source.close(); check(root.deleteRecursively()) }

    @Test fun restored_candidate_has_all_seven_tables_settings_and_exact_original_media(): Unit = runBlocking {
        val backup = detached()
        val pointer = File(root, "active-generation").readBytes()
        val candidate = store.prepareRestore(ZoneId.of(backup.snapshot.settings.recordZoneId)) { fresh ->
            assertTrue(fresh.database.categoryDao().list().isEmpty())
            RestoredGenerationWriter().populate(fresh, backup)
            assertEquals(backup.snapshot, fresh.backupSnapshotSource().capture())
            RestoredGenerationWriter().verify(fresh, backup.snapshot)
        }
        assertArrayEquals(pointer, File(root, "active-generation").readBytes())
        backup.snapshot.data.mediaAssets.forEach { asset ->
            assertArrayEquals(File(source.directory, asset.relativePath).readBytes(), File(candidate.directory, asset.relativePath).readBytes())
        }
        assertEquals(3, backup.snapshot.data.mediaAssets.size)
    }

    @Test fun missing_and_changed_staged_media_fail_without_publishing_a_new_generation(): Unit = runBlocking {
        for (missing in listOf(true, false)) {
            val backup = detached()
            val file = backup.directory.resolve(backup.snapshot.data.mediaAssets.first().relativePath).toFile()
            if (missing) check(file.delete()) else file.writeBytes(byteArrayOf(1, 2, 3))
            rejected(backup)
        }
    }

    @Test fun dimensions_and_format_are_checked_even_when_length_and_sha_match(): Unit = runBlocking {
        val backup = detached()
        val first = backup.snapshot.data.mediaAssets.first()
        val wrong = backup.snapshot.copy(data = backup.snapshot.data.copy(mediaAssets = backup.snapshot.data.mediaAssets.map {
            if (it.id == first.id) it.copy(width = it.width + 1) else it
        }))
        rejected(backup.copy(snapshot = wrong))
    }

    @Test fun linked_staged_media_is_rejected_and_external_target_remains_unchanged(): Unit = runBlocking {
        val backup = detached()
        val file = backup.directory.resolve(backup.snapshot.data.mediaAssets.first().relativePath).toFile()
        val outside = File(root, "synthetic-outside.png").apply { writeBytes(file.readBytes()) }
        val original = outside.readBytes()
        check(file.delete()); Os.symlink(outside.path, file.path)
        rejected(backup)
        assertArrayEquals(original, outside.readBytes())
    }

    @Test fun preexisting_destination_is_never_overwritten_and_pointer_stays_original(): Unit = runBlocking {
        val backup = detached()
        val pointer = File(root, "active-generation").readBytes()
        val original = source.backupSnapshotSource().capture()
        var collided: File? = null
        expectFailure {
            store.prepareRestore(ZoneId.of(backup.snapshot.settings.recordZoneId)) { fresh ->
                val file = File(fresh.directory, backup.snapshot.data.mediaAssets.first().relativePath)
                check(file.parentFile?.isDirectory == true || file.parentFile?.mkdirs() == true)
                file.writeText("synthetic collision")
                collided = file
                RestoredGenerationWriter().populate(fresh, backup)
            }
        }
        assertEquals("synthetic collision", requireNotNull(collided).readText())
        assertArrayEquals(pointer, File(root, "active-generation").readBytes())
        assertEquals(original, source.backupSnapshotSource().capture())
    }

    @Test fun partial_candidate_io_failure_does_not_change_original_facts_or_active_pointer(): Unit = runBlocking {
        val backup = detached()
        val pointer = File(root, "active-generation").readBytes()
        val original = source.backupSnapshotSource().capture()
        var populated: File? = null
        expectFailure {
            store.prepareRestore(ZoneId.of(backup.snapshot.settings.recordZoneId)) { fresh ->
                RestoredGenerationWriter().populate(fresh, backup)
                populated = fresh.directory
                throw IOException("Synthetic interruption before readiness/activation")
            }
        }
        assertNotNull(populated)
        assertFalse(File(requireNotNull(populated), "ready.json").exists())
        assertArrayEquals(pointer, File(root, "active-generation").readBytes())
        assertEquals(original, source.backupSnapshotSource().capture())
    }

    @Test fun destination_directory_collision_does_not_commit_business_rows_or_publish(): Unit = runBlocking {
        val backup = detached()
        val pointer = File(root, "active-generation").readBytes()
        val original = source.backupSnapshotSource().capture()
        var freshDirectory: File? = null
        expectFailure {
            store.prepareRestore(ZoneId.of(backup.snapshot.settings.recordZoneId)) { fresh ->
                freshDirectory = fresh.directory
                // A regular payload must never adopt or replace an existing directory.
                val obstacle = File(fresh.directory, backup.snapshot.data.mediaAssets[1].relativePath)
                check(obstacle.mkdirs())
                try { RestoredGenerationWriter().populate(fresh, backup) }
                finally {
                    assertTrue(fresh.database.categoryDao().list().isEmpty())
                    assertTrue(fresh.database.mediaDao().list().isEmpty())
                }
            }
        }
        assertNotNull(freshDirectory)
        assertFalse(File(requireNotNull(freshDirectory), "ready.json").exists())
        assertArrayEquals(pointer, File(root, "active-generation").readBytes())
        assertEquals(original, source.backupSnapshotSource().capture())
    }

    @Test fun verify_rejects_unknown_media_and_changed_restored_bytes_before_activation(): Unit = runBlocking {
        val backup = detached()
        val pointer = File(root, "active-generation").readBytes()
        expectFailure {
            store.prepareRestore(ZoneId.of(backup.snapshot.settings.recordZoneId)) { fresh ->
                val writer = RestoredGenerationWriter()
                writer.populate(fresh, backup)
                val unknown = File(fresh.directory, "media/unknown.bin").apply { writeText("unregistered") }
                expectFailure { writer.verify(fresh, backup.snapshot) }
                check(unknown.delete())
                val restored = File(fresh.directory, backup.snapshot.data.mediaAssets.first().relativePath)
                restored.writeBytes(byteArrayOf(9, 8, 7))
                expectFailure { writer.verify(fresh, backup.snapshot) }
                // Refuse the whole preparation after the intentionally corrupted candidate checks.
                throw IOException("Synthetic candidate deliberately invalidated")
            }
        }
        assertArrayEquals(pointer, File(root, "active-generation").readBytes())
    }

    @Test fun a_failed_late_insert_rolls_back_all_seven_table_rows(): Unit = runBlocking {
        val backup = detached()
        val events = backup.snapshot.data.projectMoneyEvents
        val invalid = backup.copy(snapshot = backup.snapshot.copy(data = backup.snapshot.data.copy(
            projectMoneyEvents = events.mapIndexed { index, event ->
                if (index == 1) event.copy(operationId = events.first().operationId) else event
            },
        )))
        val pointer = File(root, "active-generation").readBytes()
        expectFailure {
            store.prepareRestore(ZoneId.of(backup.snapshot.settings.recordZoneId)) { fresh ->
                try { RestoredGenerationWriter().populate(fresh, invalid) }
                finally {
                    val data = fresh.backupSnapshotSource().capture().data
                    assertTrue(data.categories.isEmpty())
                    assertTrue(data.mediaAssets.isEmpty())
                    assertTrue(data.projects.isEmpty())
                    assertTrue(data.projectMoneyEvents.isEmpty())
                    assertTrue(data.subscriptions.isEmpty())
                    assertTrue(data.subscriptionRenewals.isEmpty())
                    assertTrue(data.subscriptionStateChanges.isEmpty())
                }
            }
        }
        assertArrayEquals(pointer, File(root, "active-generation").readBytes())
    }

    @Test fun a_candidate_initialized_with_another_record_zone_is_rejected(): Unit = runBlocking {
        val backup = detached()
        val pointer = File(root, "active-generation").readBytes()
        expectFailure {
            store.prepareRestore(ZoneId.of("Europe/Paris")) { fresh ->
                RestoredGenerationWriter().populate(fresh, backup)
            }
        }
        assertArrayEquals(pointer, File(root, "active-generation").readBytes())
    }

    private suspend fun detached(): ValidatedBackup {
        val snapshot = source.backupSnapshotSource().capture()
        val staging = Files.createTempDirectory(root.toPath(), "validated-")
        val core = RoomBackupSnapshotSource(source).captureInto {
            BackupMediaSnapshot(source).copy(it, BackupCoreStager().prepare(it, staging))
        }
        return ValidatedBackup(snapshot, BackupManifestV1.complete(core, "synthetic-m12", Instant.parse("2026-01-20T00:00:00Z")),
            core.directory, "0".repeat(64))
    }

    private suspend fun rejected(backup: ValidatedBackup) {
        val pointer = File(root, "active-generation").readBytes()
        val original = source.backupSnapshotSource().capture()
        expectFailure { store.prepareRestore(ZoneId.of(backup.snapshot.settings.recordZoneId)) { RestoredGenerationWriter().populate(it, backup) } }
        assertArrayEquals(pointer, File(root, "active-generation").readBytes())
        assertEquals(original, source.backupSnapshotSource().capture())
    }

    private suspend fun expectFailure(block: suspend () -> Unit) {
        try { block(); fail("Expected restore preparation to reject an unsafe candidate") }
        catch (_: Exception) { /* The source and sole pointer are checked by each caller. */ }
    }
}
