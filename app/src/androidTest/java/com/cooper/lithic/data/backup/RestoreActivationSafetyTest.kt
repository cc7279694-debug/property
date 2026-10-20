package com.cooper.lithic.data.backup

import android.content.Context
import android.util.AtomicFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exception/checkpoint recovery only; never kills a process or opens a non-synthetic ledger. */
@RunWith(AndroidJUnit4::class)
class RestoreActivationSafetyTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var root: File
    private lateinit var store: DataGenerationStore
    private lateinit var active: ManagedGeneration
    private var failAt: String? = null

    @Before fun before(): Unit = runBlocking {
        root = File(context.noBackupFilesDir, "m12-activation-${UUID.randomUUID()}").canonicalFile
        check(root.mkdir())
        store = DataGenerationStore(context, root, restoreProbe = { step ->
            if (step == failAt) throw IOException("Synthetic restore failure at $step")
        })
        active = store.open(ZoneId.of("Asia/Shanghai"))
        RestoreSyntheticFixture.insert(active)
    }

    @After fun after(): Unit = runBlocking { active.close(); check(root.deleteRecursively()) }

    @Test fun exception_before_switch_keeps_the_original_pointer_and_complete_readable_dataset(): Unit = runBlocking {
        failAndRequireOriginal("BEFORE_SWITCH", pointerMustMatch = true)
    }

    @Test fun exception_after_switch_restores_the_complete_original_dataset_and_records_a_stable_pointer(): Unit = runBlocking {
        failAndRequireOriginal("AFTER_SWITCH", pointerMustMatch = false)
    }

    @Test fun exception_on_first_new_open_restores_the_complete_original_dataset(): Unit = runBlocking {
        failAndRequireOriginal("FIRST_OPEN", pointerMustMatch = false)
    }

    @Test fun persisted_switching_reopens_complete_new_or_rolls_back_when_new_media_is_damaged(): Unit = runBlocking {
        for (damaged in listOf(false, true)) {
            val scenarioRoot = File(root, "scenario-$damaged").apply { check(mkdir()) }
            var owner = DataGenerationStore(context, scenarioRoot)
            var old = owner.open(ZoneId.of("Asia/Shanghai"))
            try {
                RestoreSyntheticFixture.insert(old)
                val backup = detached(old, scenarioRoot)
                val candidate = owner.prepareRestore(ZoneId.of(backup.snapshot.settings.recordZoneId)) {
                    RestoredGenerationWriter().populate(it, backup)
                }
                addOldOnlyCategory(old)
                val before = old.backupSnapshotSource().capture()
                val previousId = old.id
                old.close()
                if (damaged) File(candidate.directory, backup.snapshot.data.mediaAssets.first().relativePath).writeBytes(byteArrayOf(3, 2, 1))
                val pointer = AtomicFile(File(scenarioRoot, "active-generation"))
                val bytes = JSONObject().put("formatVersion", 1).put("activeId", candidate.id)
                    .put("previousId", previousId).put("stage", "SWITCHING").toString().toByteArray(Charsets.UTF_8)
                val output = pointer.startWrite()
                try { output.write(bytes); pointer.finishWrite(output) }
                catch (failure: Throwable) { pointer.failWrite(output); throw failure }
                owner = DataGenerationStore(context, scenarioRoot)
                old = owner.open()
                if (damaged) {
                    assertEquals(previousId, old.id)
                    assertEquals(before, old.backupSnapshotSource().capture())
                    assertNotNull(owner.lastRecoveryWarning)
                } else {
                    assertEquals(candidate.id, old.id)
                    RestoredGenerationWriter().verify(old, backup.snapshot)
                    assertEquals(backup.snapshot, old.backupSnapshotSource().capture())
                }
                val stable = JSONObject(File(scenarioRoot, "active-generation").readText())
                assertEquals(if (damaged) "ROLLED_BACK" else "STABLE", stable.getString("stage"))
                assertEquals(old.id, stable.getString("activeId"))
                if (damaged) {
                    old.close()
                    owner = DataGenerationStore(context, scenarioRoot)
                    old = owner.open()
                    assertEquals(previousId, old.id)
                    assertEquals(before, old.backupSnapshotSource().capture())
                    assertNotNull(owner.lastRecoveryWarning)
                    assertEquals("ROLLED_BACK", JSONObject(File(scenarioRoot, "active-generation").readText()).getString("stage"))
                }
            } finally { old.close() }
        }
    }

    @Test fun old_generation_write_is_rejected_after_the_successful_restore_fence(): Unit = runBlocking {
        val old = active
        val backup = detached(old, root)
        val candidate = store.prepareRestore(ZoneId.of(backup.snapshot.settings.recordZoneId)) {
            RestoredGenerationWriter().populate(it, backup)
        }
        active = store.activateRestore(old.id, candidate)
        var wrote = false
        var rejected = false
        try {
            old.coordinator.withWrite { wrote = true }
        } catch (_: IllegalStateException) {
            rejected = true
        }
        assertTrue(rejected)
        assertFalse(wrote)
        assertNotEquals(old.id, active.id)
        RestoredGenerationWriter().verify(active, backup.snapshot)
    }

    @Test fun changed_preview_snapshot_refuses_switch_and_retains_the_new_original_facts(): Unit = runBlocking {
        val backup = detached(active, root)
        val candidate = store.prepareRestore(ZoneId.of(backup.snapshot.settings.recordZoneId)) {
            RestoredGenerationWriter().populate(it, backup)
        }
        val pointer = File(root, "active-generation").readBytes()
        val oldId = active.id
        addOldOnlyCategory(active)
        val changed = active.backupSnapshotSource().capture()
        expectFailure { store.activateRestore(oldId, candidate, backup.snapshot) }
        active = store.open()
        assertEquals(oldId, active.id)
        assertArrayEquals(pointer, File(root, "active-generation").readBytes())
        assertEquals(changed, active.backupSnapshotSource().capture())
        assertNotEquals(backup.snapshot, changed)
    }

    @Test fun later_stable_open_cleans_only_the_known_previous_generation_and_preserves_unknown_old_residue(): Unit = runBlocking {
        for (unknown in listOf(false, true)) {
            val scenarioRoot = File(root, "cleanup-$unknown").apply { check(mkdir()) }
            var owner = DataGenerationStore(context, scenarioRoot)
            var current = owner.open(ZoneId.of("Asia/Shanghai"))
            try {
                RestoreSyntheticFixture.insert(current)
                val backup = detached(current, scenarioRoot)
                val oldDirectory = current.directory
                val oldId = current.id
                val candidate = owner.prepareRestore(ZoneId.of(backup.snapshot.settings.recordZoneId)) {
                    RestoredGenerationWriter().populate(it, backup)
                }
                addOldOnlyCategory(current)
                current = owner.activateRestore(oldId, candidate)
                assertTrue(oldDirectory.isDirectory)
                if (unknown) File(oldDirectory, "unknown-user-file.txt").writeText("preserve the whole old generation")
                val before = Files.walk(oldDirectory.toPath()).use { paths ->
                    paths.iterator().asSequence().filter { Files.isRegularFile(it) }
                        .associate { oldDirectory.toPath().relativize(it).toString() to Files.readAllBytes(it) }
                }
                val unrelated = File(scenarioRoot, "generations/${UUID.randomUUID()}").apply { check(mkdir()) }
                val sentinel = File(unrelated, "untouched.txt").apply { writeText("unrelated generation") }
                current.close()
                owner = DataGenerationStore(context, scenarioRoot)
                current = owner.open()
                assertEquals(candidate.id, current.id)
                RestoredGenerationWriter().verify(current, backup.snapshot)
                assertEquals("unrelated generation", sentinel.readText())
                if (unknown) {
                    assertTrue(oldDirectory.isDirectory)
                    val after = Files.walk(oldDirectory.toPath()).use { paths ->
                        paths.iterator().asSequence().filter { Files.isRegularFile(it) }
                            .associate { oldDirectory.toPath().relativize(it).toString() to Files.readAllBytes(it) }
                    }
                    assertEquals(before.keys, after.keys)
                    before.forEach { (path, bytes) -> assertArrayEquals(bytes, after.getValue(path)) }
                } else {
                    assertFalse(oldDirectory.exists())
                    assertTrue(JSONObject(File(scenarioRoot, "active-generation").readText()).isNull("previousId"))
                }
            } finally { current.close() }
        }
    }

    private suspend fun failAndRequireOriginal(step: String, pointerMustMatch: Boolean) {
        val backup = detached(active, root)
        val candidate = store.prepareRestore(ZoneId.of(backup.snapshot.settings.recordZoneId)) {
            RestoredGenerationWriter().populate(it, backup)
        }
        addOldOnlyCategory(active)
        val original = active.backupSnapshotSource().capture()
        val id = active.id
        val pointer = File(root, "active-generation").readBytes()
        failAt = step
        expectFailure { store.activateRestore(id, candidate) }
        failAt = null
        active = store.open()
        assertEquals(id, active.id)
        assertEquals(original, active.backupSnapshotSource().capture())
        RestoredGenerationWriter().verify(active, original)
        if (pointerMustMatch) assertArrayEquals(pointer, File(root, "active-generation").readBytes())
        val stable = JSONObject(File(root, "active-generation").readText())
        assertEquals(if (pointerMustMatch) "STABLE" else "ROLLED_BACK", stable.getString("stage"))
        assertEquals(id, stable.getString("activeId"))
    }

    private suspend fun detached(generation: ManagedGeneration, parent: File): ValidatedBackup {
        val staging = Files.createTempDirectory(parent.toPath(), "validated-")
        lateinit var snapshot: BackupSnapshot
        val core = RoomBackupSnapshotSource(generation).captureInto {
            snapshot = it
            BackupMediaSnapshot(generation).copy(it, BackupCoreStager().prepare(it, staging))
        }
        return ValidatedBackup(snapshot, BackupManifestV1.complete(core, "synthetic-m12", Instant.parse("2026-01-20T00:00:00Z")),
            core.directory, "0".repeat(64))
    }

    private suspend fun addOldOnlyCategory(generation: ManagedGeneration) = generation.coordinator.withWrite {
        generation.database.categoryDao().insert(CategoryEntity(UUID.randomUUID().toString(), CategoryType.PROJECT,
            "仅旧代新增", "仅旧代新增", false))
    }

    private suspend fun expectFailure(block: suspend () -> Unit) {
        try { block(); fail("Expected synthetic restoration failure") }
        catch (_: Exception) { /* Assertions below verify the old generation and sole pointer. */ }
    }
}
