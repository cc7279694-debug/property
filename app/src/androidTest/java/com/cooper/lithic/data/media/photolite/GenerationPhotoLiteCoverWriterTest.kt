package com.cooper.lithic.data.media.photolite

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Process
import android.system.Os
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.backup.BackupExportResult
import com.cooper.lithic.data.backup.BackupExportService
import com.cooper.lithic.data.backup.BackupFailureReason
import com.cooper.lithic.data.backup.BackupTarget
import com.cooper.lithic.data.backup.BackupValidator
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.backup.RestoredGenerationWriter
import com.cooper.lithic.data.backup.RestoreReminderProtection
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.media.CoverOwnerType
import com.cooper.lithic.data.media.CoverReadFailure
import com.cooper.lithic.data.media.CoverReadResult
import com.cooper.lithic.data.media.CoverReference
import com.cooper.lithic.data.media.GenerationCoverReader
import com.cooper.lithic.testutil.M20TestRoots
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/** Synthetic fixtures only; NOT_RUN until this independent package receives device authority. */
@RunWith(AndroidJUnit4::class)
class GenerationPhotoLiteCoverWriterTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val roots = M20TestRoots()
    private lateinit var root: File
    private lateinit var store: DataGenerationStore
    private lateinit var generation: ManagedGeneration
    private lateinit var project: LongTermProjectEntity
    private lateinit var writer: GenerationPhotoLiteCoverWriter
    private val opened = mutableListOf<ManagedGeneration>()
    private var current: ManagedGeneration? = null
    private var setupComplete = false
    private val restoreClock = Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneOffset.UTC)

    @get:Rule val closeOwnedResources = TestRule { base, _ -> object : Statement() {
        override fun evaluate() {
            var failure: Throwable? = null
            fun retainFailure(caught: Throwable) {
                val original = failure
                if (original == null) failure = caught
                else if (original !== caught) original.addSuppressed(caught)
            }
            try { base.evaluate() } catch (caught: Throwable) { failure = caught }
            try {
                runBlocking { withContext(NonCancellable) {
                    for (owner in opened.asReversed()) {
                        try {
                            owner.close()
                            check(!owner.database.isOpen)
                        } catch (closing: Throwable) {
                            retainFailure(closing)
                        }
                    }
                } }
                if (failure == null && setupComplete) {
                    check(opened.all { !it.database.isOpen })
                    roots.ownersClosed(root)
                    // Unknown/partial new files were never granted cleanup capabilities. The
                    // helper preserves those roots. Failed restore may also own an unreturned
                    // rollback generation: never mark owners closed or clean after any failure.
                    roots.cleanupClosed()
                }
            } catch (closing: Throwable) {
                retainFailure(closing)
            }
            failure?.let { throw it }
        }
    } }

    @Before fun createIndependentFixture() = runBlocking {
        check(context.packageName == "com.cooper.lithic.photolite.debug")
        check(context.applicationContext.javaClass == Application::class.java)
        check(InstrumentationRegistry.getInstrumentation().javaClass.name == "com.cooper.lithic.app.M13IsolatedRunner")
        root = roots.create(context, "photo-lite-writer")
        store = DataGenerationStore(context, root, restoreClock = restoreClock)
        generation = store.open(ZoneId.of("Asia/Shanghai"))
        current = generation; opened += generation; roots.generation(root, generation.id)
        val category = CategoryEntity(uuid(), CategoryType.PROJECT, "合成物品", "合成物品", false)
        val at = Instant.parse("2026-10-01T01:02:03.123456789Z")
        project = LongTermProjectEntity(uuid(), "PHOTO-LITE 合成物品", category.id,
            LocalDate.parse("2026-10-01"), note = "测试夹具，不是用户记录", createdAt = at, updatedAt = at)
        generation.coordinator.withWrite {
            generation.database.categoryDao().insert(category)
            generation.database.projectDao().insertProject(project)
            generation.database.projectDao().insertEvent(ProjectMoneyEventEntity(
                uuid(), project.id, MoneyDirection.OUTLAY, 10000, project.startedOn,
                role = ProjectMoneyEventRole.INITIAL, operationId = uuid(),
            ))
        }
        writer = GenerationPhotoLiteCoverWriter(generation) { current === generation }
        setupComplete = true
    }

    @Test fun confirmed_png_is_registered_once_authenticated_and_read_after_reopen() = runBlocking {
        val before = generation.backupSnapshotSource().capture()
        val png = picture()
        val target = capture()
        val result = writer.commit(target, png) as PhotoLiteCommitResult.Success
        roots.media(root, listOf("media/${result.assetId}.png"), generation.id)
        assertEquals(target.nonce, result.assetId)
        val stored = generation.database.projectDao().findProject(project.id)!!
        assertEquals(project.copy(coverAssetId = result.assetId, updatedAt = stored.updatedAt), stored)
        assertTrue(stored.updatedAt > project.updatedAt)
        val after = generation.backupSnapshotSource().capture()
        assertEquals(before.data.projectMoneyEvents, after.data.projectMoneyEvents)
        assertEquals(before.data.subscriptions, after.data.subscriptions)
        val asset = generation.database.mediaDao().list().single()
        assertEquals("image/png", asset.mime)
        assertEquals(png.width.toInt(), asset.width); assertEquals(png.height.toInt(), asset.height)
        assertEquals(png.byteLength, asset.byteLength); assertEquals(png.sha256, asset.sha256)
        val file = File(generation.directory, asset.relativePath)
        assertEquals(Process.myUid(), Os.lstat(file.path).st_uid)
        assertEquals(0x180, Os.lstat(file.path).st_mode and 0x1ff)
        assertArrayEquals(png.copyBytes(), file.readBytes())
        val reader = GenerationCoverReader(generation) { current === generation }
        assertArrayEquals(png.copyBytes(), (reader.read(CoverReference(CoverOwnerType.PROJECT,
            project.id, asset.id)) as CoverReadResult.Ready).image.bytes)
        generation.close(); assertFalse(generation.database.isOpen)
        val reopened = DataGenerationStore(context, root).open()
        current = reopened; opened += reopened; roots.generation(root, reopened.id)
        assertEquals(generation.id, reopened.id); assertNotSame(generation, reopened)
        val reopenedReader = GenerationCoverReader(reopened) { current === reopened }
        assertArrayEquals(png.copyBytes(), (reopenedReader.read(CoverReference(CoverOwnerType.PROJECT,
            project.id, asset.id)) as CoverReadResult.Ready).image.bytes)
    }

    @Test fun capture_and_discard_create_neither_file_nor_media_or_cover_reference() = runBlocking {
        val before = generation.backupSnapshotSource().capture()
        capture() // Closing the picker or preview has no storage work to undo.
        assertEquals(before, generation.backupSnapshotSource().capture())
        assertTrue(File(generation.directory, "media").listFiles()!!.isEmpty())
    }

    @Test fun complete_old_row_is_compared_even_when_another_edit_reuses_the_timestamp() = runBlocking {
        val target = capture()
        generation.coordinator.withWrite {
            generation.database.openHelper.writableDatabase.execSQL(
                "UPDATE long_term_projects SET name=? WHERE id=?", arrayOf("已修改，时间未变", project.id),
            )
            val p = target.expected
            assertEquals(0, generation.database.projectDao().addPhotoLiteCoverIfUnchanged(
                p.id, p.name, p.categoryId, p.startedOn, p.endedOn, p.endReason, p.coverAssetId,
                p.note, p.createdAt, p.updatedAt, uuid(), Instant.now(),
            ))
        }
        assertEquals(PhotoLiteCommitResult.Rejected(PhotoLiteFailure.STALE_PROJECT), writer.commit(target, picture()))
        assertEquals("已修改，时间未变", generation.database.projectDao().findProject(project.id)!!.name)
        assertNull(generation.database.projectDao().findProject(project.id)!!.coverAssetId)
        assertTrue(generation.database.mediaDao().list().isEmpty())
        assertTrue(File(generation.directory, "media").listFiles()!!.isEmpty())
    }

    @Test fun duplicate_confirmation_and_second_old_capture_cannot_overwrite_first_cover() = runBlocking {
        val first = capture(); val second = capture(); val png = picture()
        val success = writer.commit(first, png) as PhotoLiteCommitResult.Success
        roots.media(root, listOf("media/${success.assetId}.png"), generation.id)
        assertEquals(PhotoLiteCommitResult.Rejected(PhotoLiteFailure.DUPLICATE), writer.commit(first, png))
        assertEquals(PhotoLiteCommitResult.Rejected(PhotoLiteFailure.STALE_PROJECT), writer.commit(second, png))
        assertEquals(PhotoLiteCaptureResult.Rejected(PhotoLiteFailure.ALREADY_HAS_COVER), writer.capture(project.id))
        assertEquals(success.assetId, generation.database.projectDao().findProject(project.id)!!.coverAssetId)
        assertEquals(1, generation.database.mediaDao().list().size)
    }

    @Test fun owner_instance_and_writer_capability_are_required_even_if_generation_id_matches() = runBlocking {
        val target = capture()
        val anotherWriter = GenerationPhotoLiteCoverWriter(generation) { current === generation }
        assertEquals(PhotoLiteCommitResult.Rejected(PhotoLiteFailure.OWNER_UNAVAILABLE), anotherWriter.commit(target, picture()))
        generation.close(); assertFalse(generation.database.isOpen)
        val reopened = DataGenerationStore(context, root).open()
        current = reopened; opened += reopened; roots.generation(root, reopened.id)
        assertEquals(generation.id, reopened.id)
        assertEquals(PhotoLiteCaptureResult.Rejected(PhotoLiteFailure.OWNER_UNAVAILABLE), writer.capture(project.id))
        assertEquals(PhotoLiteCommitResult.Rejected(PhotoLiteFailure.OWNER_UNAVAILABLE), writer.commit(target, picture()))
        assertFalse(generation.database.isOpen)
        assertNull(reopened.database.projectDao().findProject(project.id)!!.coverAssetId)
        assertTrue(reopened.database.mediaDao().list().isEmpty())
    }

    @Test fun preexisting_output_is_not_overwritten_or_registered_as_new_media() = runBlocking {
        val target = capture(); val relative = "media/${target.nonce}.png"
        val existing = File(generation.directory, relative)
        check(existing.createNewFile()); existing.writeBytes(byteArrayOf(7, 8, 9))
        roots.media(root, listOf(relative), generation.id) // Explicitly created by this fixture.
        assertEquals(PhotoLiteCommitResult.Rejected(PhotoLiteFailure.FILE_FAILED), writer.commit(target, picture()))
        assertArrayEquals(byteArrayOf(7, 8, 9), existing.readBytes())
        assertTrue(generation.database.mediaDao().list().isEmpty())
        assertNull(generation.database.projectDao().findProject(project.id)!!.coverAssetId)
    }

    @Test fun symlink_output_is_rejected_without_following_or_unlinking_it() = runBlocking {
        val target = capture()
        val sentinel = File(root, "sentinel.txt")
        check(sentinel.createNewFile()); sentinel.writeText("synthetic sentinel")
        roots.files(root, sentinel.name)
        val destination = File(generation.directory, "media/${target.nonce}.png")
        Os.symlink(sentinel.path, destination.path)
        assertEquals(PhotoLiteCommitResult.Rejected(PhotoLiteFailure.FILE_FAILED), writer.commit(target, picture()))
        assertEquals("synthetic sentinel", sentinel.readText())
        assertEquals(sentinel.path, Os.readlink(destination.path))
        assertTrue(generation.database.mediaDao().list().isEmpty())
        assertNull(generation.database.projectDao().findProject(project.id)!!.coverAssetId)
        // Leave the deliberately invalid node for inspection; never authorize generic cleanup.
    }

    @Test fun database_rejection_rolls_back_both_rows_and_preserves_the_new_authenticated_orphan() = runBlocking {
        val target = capture()
        generation.coordinator.withWrite {
            generation.database.openHelper.writableDatabase.execSQL("""
                CREATE TRIGGER photo_lite_fixture_abort BEFORE UPDATE OF coverAssetId ON long_term_projects
                BEGIN SELECT RAISE(ABORT, 'photo-lite synthetic abort'); END
            """.trimIndent())
        }
        try {
            assertEquals(PhotoLiteCommitResult.Unknown, writer.commit(target, picture()))
            assertNull(generation.database.projectDao().findProject(project.id)!!.coverAssetId)
            assertTrue(generation.database.mediaDao().list().isEmpty())
            val preserved = File(generation.directory, "media/${target.nonce}.png")
            assertTrue(preserved.isFile)
            PhotoLitePng.checked(preserved.readBytes())
            // No cleanup capability is assigned to this confirmation's unknown file.
            assertEquals(PhotoLiteCommitResult.Rejected(PhotoLiteFailure.DUPLICATE), writer.commit(target, picture()))
        } finally {
            generation.coordinator.withWrite {
                generation.database.openHelper.writableDatabase.execSQL("DROP TRIGGER photo_lite_fixture_abort")
            }
        }
    }

    @Test fun caller_cancelled_while_waiting_for_write_lock_cannot_publish_a_late_cover() = runBlocking {
        val target = capture(); val before = generation.backupSnapshotSource().capture()
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val blocker = launch(Dispatchers.IO) { generation.coordinator.withWrite { entered.complete(Unit); release.await() } }
        entered.await()
        val commit = async(start = CoroutineStart.UNDISPATCHED) { writer.commit(target, picture()) }
        commit.cancel(); release.complete(Unit); blocker.join()
        try { commit.await(); fail("Cancelled commit unexpectedly completed") } catch (_: CancellationException) { }
        assertEquals(before, generation.backupSnapshotSource().capture())
        assertTrue(File(generation.directory, "media").listFiles()!!.isEmpty())
    }

    @Test fun successful_photo_is_in_format4_backup_and_missing_or_corrupt_bytes_block_success() = runBlocking {
        val png = picture(); val result = writer.commit(capture(), png) as PhotoLiteCommitResult.Success
        val relative = "media/${result.assetId}.png"
        roots.media(root, listOf(relative), generation.id)
        val archive = File(root, "photo-lite.lithicbackup")
        roots.files(root, archive.name)
        assertTrue(BackupExportService(context, generation).export(target(archive)) is BackupExportResult.Success)
        val extraction = File(root, "extracted"); check(extraction.mkdir())
        val validated = BackupValidator().validate(archive.toPath(), extraction.toPath())
        roots.files(root, "extracted/manifest.json", *validated.manifest.files.map { "extracted/${it.path}" }.toTypedArray())
        assertEquals(4, validated.manifest.formatVersion)
        val backedUp = validated.snapshot.data.mediaAssets.single()
        assertEquals(png.sha256, backedUp.sha256)
        assertEquals(result.assetId, validated.snapshot.data.projects.single().coverAssetId)
        val image = File(generation.directory, relative)
        check(image.delete()) // Only the synthetic file that this method just authenticated.
        roots.files(root, "missing.lithicbackup", "corrupt.lithicbackup")
        val missing = BackupExportService(context, generation).export(target(File(root, "missing.lithicbackup")))
        assertTrue(missing is BackupExportResult.Failed)
        assertEquals(BackupFailureReason.INVALID_MEDIA, (missing as BackupExportResult.Failed).reason)
        check(image.createNewFile()); image.writeBytes(png.copyBytes().apply { this[0] = 0 })
        val corrupt = BackupExportService(context, generation).export(target(File(root, "corrupt.lithicbackup")))
        assertTrue(corrupt is BackupExportResult.Failed)
        assertEquals(BackupFailureReason.INVALID_MEDIA, (corrupt as BackupExportResult.Failed).reason)
        assertEquals(result.assetId, generation.database.projectDao().findProject(project.id)!!.coverAssetId)
    }

    @Test fun confirmed_photo_restores_original_bytes_metadata_cover_and_money_then_reopens_in_new_owner() = runBlocking {
        val png = picture()
        val result = writer.commit(capture(), png) as PhotoLiteCommitResult.Success
        val asset = generation.database.mediaDao().find(result.assetId)!!
        roots.media(root, listOf(asset.relativePath), generation.id)
        val beforeBackup = generation.backupSnapshotSource().capture()
        val backedUpProject = generation.database.projectDao().findProject(project.id)!!
        val originalMoney = generation.database.projectDao().eventsForProjects(listOf(project.id))
        val reference = CoverReference(CoverOwnerType.PROJECT, project.id, result.assetId)
        val oldReader = GenerationCoverReader(generation) { current === generation }
        assertTrue(oldReader.read(reference) is CoverReadResult.Ready)

        val archive = File(root, "photo-lite-restore.lithicbackup")
        roots.files(root, archive.name)
        val exported = BackupExportService(context, generation).export(target(archive))
        assertTrue(exported is BackupExportResult.Success)
        assertFalse((exported as BackupExportResult.Success).temporaryFilesRemaining)
        val extraction = File(root, "photo-lite-restore-payload"); check(extraction.mkdir())
        val validated = BackupValidator().validate(archive.toPath(), extraction.toPath())
        roots.files(root, "photo-lite-restore-payload/manifest.json",
            *validated.manifest.files.map { "photo-lite-restore-payload/${it.path}" }.toTypedArray())
        assertEquals(4, validated.manifest.formatVersion)
        assertEquals(4, validated.manifest.minReaderFormat)
        assertEquals(5, validated.manifest.sourceDatabaseSchemaVersion)
        assertEquals(beforeBackup, validated.snapshot)
        assertArrayEquals(png.copyBytes(), File(extraction, asset.relativePath).readBytes())

        // Synthetic edit after the export proves that activation restores the archived facts;
        // expectedSnapshot is the actual current set, never the pre-edit backup snapshot.
        generation.coordinator.withWrite {
            assertEquals(1, generation.database.projectDao().updateProjectMetadata(
                backedUpProject.id, backedUpProject.updatedAt, "备份后修改的合成名称",
                backedUpProject.startedOn, backedUpProject.categoryId, "备份后修改的合成备注",
                backedUpProject.updatedAt.plusSeconds(1),
            ))
        }
        val beforeRestore = generation.backupSnapshotSource().capture()
        assertNotEquals(beforeBackup.data.projects, beforeRestore.data.projects)
        val candidate = store.prepareRestore(ZoneId.of(beforeBackup.settings.recordZoneId)) {
            RestoredGenerationWriter().populate(it, validated)
        }
        roots.generation(root, candidate.id)
        roots.media(root, listOf(asset.relativePath), candidate.id)
        val restored = store.activateRestore(generation.id, candidate, beforeRestore)
        current = restored; opened += restored
        assertNotEquals(generation.id, restored.id)
        assertFalse(generation.database.isOpen)
        val expected = RestoreReminderProtection.snapshot(beforeBackup, restoreClock.instant())
        assertEquals(expected, restored.backupSnapshotSource().capture())
        RestoredGenerationWriter().verify(restored, expected)
        assertEquals(backedUpProject, restored.database.projectDao().findProject(project.id))
        assertEquals(originalMoney, restored.database.projectDao().eventsForProjects(listOf(project.id)))
        assertEquals(asset, restored.database.mediaDao().find(result.assetId))
        val restoredFile = File(restored.directory, asset.relativePath)
        val restoredBytes = restoredFile.readBytes()
        assertEquals(png.byteLength, restoredFile.length())
        assertEquals(png.byteLength, restoredBytes.size.toLong())
        assertArrayEquals(png.copyBytes(), restoredBytes)
        assertEquals(png.sha256, sha(restoredBytes))
        val inspected = PhotoLitePng.checked(restoredBytes)
        assertEquals(png.width, inspected.width); assertEquals(png.height, inspected.height)
        val bitmap = checkNotNull(BitmapFactory.decodeByteArray(restoredBytes, 0, restoredBytes.size,
            BitmapFactory.Options().apply { inScaled = false; inPreferredConfig = Bitmap.Config.ARGB_8888 }))
        try {
            assertEquals(png.width, bitmap.width); assertEquals(png.height, bitmap.height)
            assertEquals(0xff839bb0.toInt(), bitmap.getPixel(0, 0))
            assertEquals(0xff839bb0.toInt(), bitmap.getPixel(bitmap.width - 1, bitmap.height - 1))
        } finally { bitmap.recycle() }
        val reader = GenerationCoverReader(restored) { current === restored }
        assertNotSame(oldReader.ownerKey, reader.ownerKey)
        val image = (reader.read(reference) as CoverReadResult.Ready).image
        assertArrayEquals(restoredBytes, image.bytes)
        assertEquals(png.width, image.width); assertEquals(png.height, image.height)
        assertEquals(CoverReadResult.Unavailable(CoverReadFailure.OWNER_UNAVAILABLE), oldReader.read(reference))
        assertEquals(PhotoLiteCaptureResult.Rejected(PhotoLiteFailure.OWNER_UNAVAILABLE), writer.capture(project.id))
        assertFalse(generation.database.isOpen)

        val restoredId = restored.id
        restored.close(); assertFalse(restored.database.isOpen)
        val reopened = DataGenerationStore(context, root, restoreClock = restoreClock).open()
        current = reopened; opened += reopened; roots.generation(root, reopened.id)
        assertEquals(restoredId, reopened.id); assertNotSame(restored, reopened)
        assertEquals(expected, reopened.backupSnapshotSource().capture())
        assertEquals(asset, reopened.database.mediaDao().find(result.assetId))
        assertEquals(originalMoney, reopened.database.projectDao().eventsForProjects(listOf(project.id)))
        val reopenedImage = (GenerationCoverReader(reopened) { current === reopened }
            .read(reference) as CoverReadResult.Ready).image
        assertArrayEquals(png.copyBytes(), reopenedImage.bytes)
        assertEquals(png.byteLength, reopenedImage.bytes.size.toLong())
        assertEquals(png.sha256, sha(reopenedImage.bytes))
        assertEquals(png.width, reopenedImage.width); assertEquals(png.height, reopenedImage.height)
    }

    private suspend fun capture(): PhotoLiteTarget = (writer.capture(project.id) as PhotoLiteCaptureResult.Ready).target
    private fun picture(): PhotoLitePng {
        val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
        return try {
            bitmap.eraseColor(0xff839bb0.toInt())
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                PhotoLitePng.checked(output.toByteArray())
            }
        } finally { bitmap.recycle() }
    }
    private fun target(file: File): BackupTarget = object : BackupTarget {
        override fun openWrite() = file.outputStream()
        override fun openRead() = file.inputStream()
        override fun deletePartial(): Boolean = !file.exists() || file.delete()
    }
    private fun uuid(): String = UUID.randomUUID().toString()
    private fun sha(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}
