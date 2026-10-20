package com.cooper.lithic.data.backup

import android.content.Context
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.app.StorageInitializationState
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Service integration uses two explicit synthetic owners, never the application's normal ledger. */
@RunWith(AndroidJUnit4::class)
class RestoreCoordinatorTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var root: File
    private lateinit var currentRoot: File
    private lateinit var owner: AppContainer
    private lateinit var sourceOwner: AppContainer
    private lateinit var coordinator: RestoreCoordinator
    private lateinit var archive: File
    private lateinit var sourceFacts: BackupSnapshot
    private lateinit var sourcePictures: Map<String, ByteArray>
    private lateinit var oldPictures: Map<String, ByteArray>
    private lateinit var oldId: String
    private lateinit var oldFacts: BackupSnapshot
    private lateinit var oldPointer: ByteArray
    private lateinit var oldCategory: CategoryEntity
    private val testCacheOperations = linkedSetOf<Path>()

    @Before fun before(): Unit = runBlocking {
        root = File(context.noBackupFilesDir, "m12-service-${UUID.randomUUID()}").canonicalFile
        check(root.mkdir())
        currentRoot = File(root, "current")
        owner = AppContainer(DataGenerationStore(context, currentRoot), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        sourceOwner = AppContainer(DataGenerationStore(context, File(root, "source")), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        owner.start(ZoneId.of("UTC"))
        sourceOwner.start(ZoneId.of("Asia/Shanghai"))
        val old = owner.openStorage()
        oldPictures = RestoreSyntheticFixture.insert(old)
        oldCategory = CategoryEntity(UUID.randomUUID().toString(), CategoryType.PROJECT, "旧数据专属", "旧数据专属", false)
        old.coordinator.withWrite { old.database.withTransaction { old.database.categoryDao().insert(oldCategory) } }
        oldId = old.id
        oldFacts = old.backupSnapshotSource().capture()
        oldPointer = pointerBytes()
        val source = sourceOwner.openStorage()
        sourcePictures = RestoreSyntheticFixture.insert(source)
        sourceFacts = source.backupSnapshotSource().capture()
        archive = File(root, "source.lithicbackup")
        val exported = BackupExportService(context, source).export(RestoreSyntheticFixture.target(archive))
        assertTrue("Synthetic fixture must form a real complete M11 archive", exported is BackupExportResult.Success)
        assertFalse((exported as BackupExportResult.Success).temporaryFilesRemaining)
        coordinator = RestoreCoordinator(context, owner)
    }

    @After fun after(): Unit = runBlocking {
        if (::owner.isInitialized) owner.close()
        if (::sourceOwner.isInitialized) sourceOwner.close()
        // Failed-import residue belongs only to these synthetic calls. The service's
        // conservative retention is asserted before this explicitly scoped test teardown.
        if (testCacheOperations.isNotEmpty()) {
            val cache = ownedBackupCacheDirectory(context)
            testCacheOperations.forEach { path ->
                if (Files.exists(path, NOFOLLOW_LINKS)) {
                    check(path.parent == cache && path.fileName.toString().startsWith("lithic-restore-"))
                    check(path.toRealPath() == path && !Files.isSymbolicLink(path))
                    Files.walk(path).use { paths -> check(paths.noneMatch { Files.isSymbolicLink(it) }) }
                    check(path.toFile().deleteRecursively())
                }
            }
        }
        if (::root.isInitialized && root.exists()) {
            check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.name.startsWith("m12-service-"))
            check(root.deleteRecursively())
        }
    }

    @Test fun preview_keeps_old_generation_and_discard_removes_only_its_known_complete_operation(): Unit = runBlocking {
        val unrelated = Files.createDirectory(ownedBackupCacheDirectory(context).resolve("lithic-restore-unrelated-${UUID.randomUUID()}"))
        testCacheOperations.add(unrelated)
        Files.write(unrelated.resolve("keep"), byteArrayOf(9, 8, 7))
        val before = cacheOperations()
        val preview = prepare()
        val operation = (cacheOperations() - before).single()
        assertTrue(Files.isRegularFile(operation.resolve("payload/manifest.json"), NOFOLLOW_LINKS))
        assertEquals(sourceFacts.data.recordCounts(), preview.backupCounts)
        assertEquals(oldFacts.data.recordCounts(), preview.currentCounts)
        assertEquals("Asia/Shanghai", preview.recordZoneId)
        assertCurrent(oldId, oldFacts, oldPointer)
        coordinator.discard(preview.token)
        assertFalse(Files.exists(operation, NOFOLLOW_LINKS))
        assertEquals(before, cacheOperations())
        assertArrayEquals(byteArrayOf(9, 8, 7), Files.readAllBytes(unrelated.resolve("keep")))
        assertCurrent(oldId, oldFacts, oldPointer)
        val replay = coordinator.confirm(preview.token)
        assertTrue(replay is RestoreResult.Failed && replay.currentPreserved)
    }

    @Test fun confirmation_replaces_complete_bundle_once_and_replayed_token_cannot_change_new_generation(): Unit = runBlocking {
        val before = cacheOperations()
        val preview = prepare()
        val operation = (cacheOperations() - before).single()
        assertEquals(RestoreResult.Success, coordinator.confirm(preview.token))
        val restored = owner.openStorage()
        val restoredId = restored.id
        val committedPointer = pointerBytes()
        assertNotEquals(oldId, restoredId)
        assertEquals(sourceFacts, restored.backupSnapshotSource().capture())
        assertEquals(ZoneId.of("Asia/Shanghai"), restored.settings.recordZone())
        assertNull(restored.database.categoryDao().find(oldCategory.id))
        sourcePictures.forEach { (path, bytes) -> assertArrayEquals(bytes, File(restored.directory, path).readBytes()) }
        RestoredGenerationWriter().verify(restored, sourceFacts)
        assertFalse(Files.exists(operation, NOFOLLOW_LINKS))
        val replay = coordinator.confirm(preview.token)
        assertTrue(replay is RestoreResult.Failed && replay.currentPreserved)
        assertCurrent(restoredId, sourceFacts, committedPointer)
        assertEquals(sourceFacts, sourceOwner.openStorage().backupSnapshotSource().capture())
    }

    @Test fun changed_old_facts_after_preview_reject_confirmation_without_losing_that_new_write(): Unit = runBlocking {
        val preview = prepare()
        val old = owner.openStorage()
        val afterPreview = CategoryEntity(UUID.randomUUID().toString(), CategoryType.SUBSCRIPTION, "预览后新增", "预览后新增", false)
        old.coordinator.withWrite { old.database.withTransaction { old.database.categoryDao().insert(afterPreview) } }
        val changedFacts = old.backupSnapshotSource().capture()
        assertNotEquals(oldFacts, changedFacts)
        val result = coordinator.confirm(preview.token)
        assertTrue(result is RestoreResult.Failed && result.currentPreserved)
        assertCurrent(oldId, changedFacts, oldPointer)
        assertEquals(afterPreview, owner.openStorage().database.categoryDao().find(afterPreview.id))
        assertEquals(oldCategory, owner.openStorage().database.categoryDao().find(oldCategory.id))
        assertEquals(sourceFacts, sourceOwner.openStorage().backupSnapshotSource().capture())
    }

    @Test fun invalid_archive_and_provider_read_failure_preserve_current_and_leave_no_pending_confirmation(): Unit = runBlocking {
        try {
            prepare { byteArrayOf(1, 2, 3).inputStream() }
            fail("An invalid archive must not produce a preview")
        } catch (_: IllegalArgumentException) { }
        assertCurrent(oldId, oldFacts, oldPointer)
        try {
            prepare {
                object : FilterInputStream(archive.inputStream()) {
                    private var readOnce = false
                    override fun read(bytes: ByteArray, offset: Int, count: Int): Int {
                        if (readOnce) throw IOException("Synthetic provider read failed")
                        readOnce = true
                        return super.read(bytes, offset, count)
                    }
                }
            }
            fail("A provider read failure must not produce a preview")
        } catch (expected: IOException) { assertEquals("Synthetic provider read failed", expected.message) }
        assertCurrent(oldId, oldFacts, oldPointer)
        val retainedFailures = cacheOperations()
        // A new real preview proves failed prepare never left an invisible confirmation capability.
        val valid = prepare()
        coordinator.discard(valid.token)
        assertEquals(retainedFailures, cacheOperations())
        assertCurrent(oldId, oldFacts, oldPointer)
    }

    @Test fun modified_extracted_payload_rejects_whole_restore_and_retains_all_uncertain_private_files(): Unit = runBlocking {
        val before = cacheOperations()
        val preview = prepare()
        val operation = (cacheOperations() - before).single()
        val manifest = Files.readAllBytes(operation.resolve("payload/manifest.json"))
        val allPaths = relativePaths(operation)
        val changedBytes = byteArrayOf(4, 3, 2, 1)
        Files.write(operation.resolve("payload/data.json"), changedBytes)
        val result = coordinator.confirm(preview.token)
        assertTrue(result is RestoreResult.Failed && result.currentPreserved)
        assertCurrent(oldId, oldFacts, oldPointer)
        assertTrue(Files.isDirectory(operation, NOFOLLOW_LINKS))
        assertEquals(allPaths, relativePaths(operation))
        assertArrayEquals(changedBytes, Files.readAllBytes(operation.resolve("payload/data.json")))
        assertArrayEquals(manifest, Files.readAllBytes(operation.resolve("payload/manifest.json")))
        val replay = coordinator.confirm(preview.token)
        assertTrue(replay is RestoreResult.Failed && replay.currentPreserved)
        assertCurrent(oldId, oldFacts, oldPointer)
    }

    private suspend fun prepare(open: () -> InputStream = { archive.inputStream() }): RestorePreview {
        val before = cacheOperations()
        try { return coordinator.prepare(open) }
        finally { testCacheOperations.addAll(cacheOperations() - before) }
    }

    private suspend fun assertCurrent(id: String, facts: BackupSnapshot, pointer: ByteArray) {
        assertTrue(owner.storageState.value is StorageInitializationState.Ready)
        val observed = owner.openStorage()
        assertEquals(id, observed.id)
        assertEquals(facts, observed.backupSnapshotSource().capture())
        val pictures = if (id == oldId) oldPictures else sourcePictures
        pictures.forEach { (path, bytes) -> assertArrayEquals(bytes, File(observed.directory, path).readBytes()) }
        assertArrayEquals(pointer, pointerBytes())
    }

    private fun pointerBytes(): ByteArray = File(currentRoot, "active-generation").readBytes()
    private fun cacheOperations(): Set<Path> = Files.list(ownedBackupCacheDirectory(context)).use { paths ->
        paths.iterator().asSequence().filter { Files.isDirectory(it, NOFOLLOW_LINKS) && it.fileName.toString().startsWith("lithic-restore-") }.toSet()
    }
    private fun relativePaths(root: Path): Set<String> = Files.walk(root).use { paths ->
        paths.iterator().asSequence().filter { it != root }.map { root.relativize(it).joinToString("/") }.toSet()
    }
}
