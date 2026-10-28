package com.cooper.lithic.data.media

import android.app.Application
import android.content.Context
import android.system.Os
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.backup.RestoreSyntheticFixture
import com.cooper.lithic.testutil.M20TestRoots
import java.io.File
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
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

/** Real Room/file assertions, compiled only until separately authorized isolated execution. */
@RunWith(AndroidJUnit4::class)
class GenerationCoverReaderTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val roots = M20TestRoots()
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private val opened = mutableListOf<ManagedGeneration>()
    private lateinit var reader: GenerationCoverReader
    private lateinit var project: CoverReference
    private lateinit var subscription: CoverReference
    private lateinit var pictures: Map<String, ByteArray>
    private var current: ManagedGeneration? = null
    private var setupComplete = false

    @get:Rule val closeOwnedResources = TestRule { base, _ -> object : Statement() {
        override fun evaluate() {
            var failure: Throwable? = null
            try { base.evaluate() } catch (caught: Throwable) { failure = caught }
            try {
                runBlocking { withContext(NonCancellable) {
                    for (owner in opened.asReversed()) { owner.close(); check(!owner.database.isOpen) }
                } }
                if (setupComplete) {
                    roots.ownersClosed(root)
                    // The existing capability helper checks every known file; failures preserve full fixtures.
                    if (failure == null) roots.cleanupClosed()
                }
            } catch (closing: Throwable) {
                if (failure == null) failure = closing else failure.addSuppressed(closing)
            }
            failure?.let { throw it }
        }
    } }

    @Before fun createOwnedFixture() = runBlocking {
        check(context.applicationContext.javaClass == Application::class.java)
        check(InstrumentationRegistry.getInstrumentation().javaClass.name == "com.cooper.lithic.app.M13IsolatedRunner")
        root = roots.create(context, "c6-reader")
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        opened += generation; current = generation; roots.generation(root, generation.id)
        pictures = RestoreSyntheticFixture.insert(generation)
        roots.media(root, pictures.keys, generation.id)
        val facts = generation.backupSnapshotSource().capture()
        project = facts.data.projects.first { it.coverAssetId != null }.let { CoverReference(CoverOwnerType.PROJECT, it.id, it.coverAssetId) }
        subscription = facts.data.subscriptions.single().let { CoverReference(CoverOwnerType.SUBSCRIPTION, it.id, it.coverAssetId) }
        reader = GenerationCoverReader(generation) { current === generation }
        setupComplete = true
    }

    @Test fun registered_png_and_jpeg_are_returned_as_original_verified_bytes_without_business_writes() = runBlocking {
        val before = generation.backupSnapshotSource().capture()
        assertSame(reader.ownerKey, reader.ownerKey)
        assertFalse(reader.ownerKey is ManagedGeneration)
        assertNotSame(reader.ownerKey, GenerationCoverReader(generation) { true }.ownerKey)
        for ((reference, path) in listOf(project to "media/nested/cover.png", subscription to "media/cover.jpg")) {
            val image = (reader.read(reference) as CoverReadResult.Ready).image
            assertEquals(reference.assetId, image.assetId)
            assertArrayEquals(pictures.getValue(path), image.bytes)
        }
        assertEquals(before, generation.backupSnapshotSource().capture())
        pictures.forEach { (path, bytes) -> assertArrayEquals(bytes, File(generation.directory, path).readBytes()) }
    }

    @Test fun no_cover_is_distinct_from_wrong_record_type_stale_reference_and_missing_record() = runBlocking {
        val noCover = generation.backupSnapshotSource().capture().data.projects.single { it.coverAssetId == null }
        assertEquals(CoverReadResult.NoCover, reader.read(CoverReference(CoverOwnerType.PROJECT, noCover.id, null)))
        assertFailure(CoverReadFailure.RECORD_MISMATCH, reader.read(project.copy(assetId = null)))
        assertFailure(CoverReadFailure.RECORD_MISMATCH, reader.read(project.copy(type = CoverOwnerType.SUBSCRIPTION)))
        assertFailure(CoverReadFailure.RECORD_MISMATCH, reader.read(project.copy(recordId = UUID.randomUUID().toString())))
        assertFailure(CoverReadFailure.RECORD_MISMATCH, reader.read(project.copy(assetId = UUID.randomUUID().toString())))
    }

    @Test fun missing_file_and_checksum_damage_do_not_clear_cover_or_change_metadata() = runBlocking {
        val before = generation.backupSnapshotSource().capture()
        val file = File(generation.directory, "media/nested/cover.png")
        check(file.delete())
        assertFailure(CoverReadFailure.MISSING_FILE, reader.read(project))
        file.writeBytes(pictures.getValue("media/nested/cover.png").copyOf().apply { this[0] = 0 })
        assertFailure(CoverReadFailure.INTEGRITY, reader.read(project))
        assertEquals(before, generation.backupSnapshotSource().capture())
    }

    @Test fun corrupt_metadata_unsafe_path_and_wrong_dimensions_are_rejected_without_repair() = runBlocking {
        val db = generation.database.openHelper.writableDatabase
        generation.coordinator.withWrite { db.execSQL("UPDATE local_media_assets SET width=3 WHERE id=?", arrayOf(project.assetId)) }
        assertFailure(CoverReadFailure.INTEGRITY, reader.read(project))
        generation.coordinator.withWrite { db.execSQL("UPDATE local_media_assets SET width=2,relativePath='media/../escaped.png' WHERE id=?", arrayOf(project.assetId)) }
        assertFailure(CoverReadFailure.INTEGRITY, reader.read(project))
        generation.coordinator.withWrite { db.execSQL("UPDATE local_media_assets SET relativePath='media/nested/cover.png',mime='image/webp' WHERE id=?", arrayOf(project.assetId)) }
        assertFailure(CoverReadFailure.FORMAT, reader.read(project))
    }

    @Test fun links_and_missing_metadata_are_not_treated_as_an_unset_cover() = runBlocking {
        val file = File(generation.directory, "media/nested/cover.png")
        check(file.delete())
        Os.symlink(File(generation.directory, "media/unreferenced.png").path, file.path)
        assertFailure(CoverReadFailure.OWNERSHIP, reader.read(project))
        generation.coordinator.withWrite {
            val db = generation.database.openHelper.writableDatabase
            db.execSQL("PRAGMA foreign_keys=OFF")
            try { db.execSQL("DELETE FROM local_media_assets WHERE id=?", arrayOf(subscription.assetId)) }
            finally { db.execSQL("PRAGMA foreign_keys=ON") }
        }
        assertFailure(CoverReadFailure.MISSING_METADATA, reader.read(subscription))
        // The capability cleanup helper conservatively preserves this deliberately linked fixture.
    }

    @Test fun same_generation_id_reopened_with_a_new_owner_rejects_the_closed_reader_without_reopening_it() = runBlocking {
        generation.close(); assertFalse(generation.database.isOpen)
        val reopened = DataGenerationStore(context, root).open()
        opened += reopened; current = reopened
        assertEquals(generation.id, reopened.id)
        assertNotSame(reader.ownerKey, GenerationCoverReader(reopened) { current === reopened }.ownerKey)
        assertFailure(CoverReadFailure.OWNER_UNAVAILABLE, reader.read(project))
        assertFailure(CoverReadFailure.OWNER_UNAVAILABLE, GenerationCoverReader(generation) { true }.read(project))
        assertFalse(generation.database.isOpen)
        assertTrue(GenerationCoverReader(reopened) { current === reopened }.read(project) is CoverReadResult.Ready)
        current = null
        assertFailure(CoverReadFailure.OWNER_UNAVAILABLE, GenerationCoverReader(reopened) { current === reopened }.read(project))
    }

    @Test fun queued_cancelled_read_propagates_cancellation_without_changing_facts() = runBlocking {
        val before = generation.backupSnapshotSource().capture()
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val lock = launch { generation.coordinator.withWrite { entered.complete(Unit); release.await() } }
        entered.await()
        val started = CompletableDeferred<Unit>()
        val cancellableReader = GenerationCoverReader(generation) { started.complete(Unit); current === generation }
        val request = async { cancellableReader.read(project) }
        try {
            started.await()
            request.cancel(CancellationException("synthetic cancellation"))
            try { request.await(); fail("Cancellation must remain cancellation") } catch (_: CancellationException) { }
        } finally { release.complete(Unit); lock.join(); request.cancelAndJoin() }
        assertEquals(before, generation.backupSnapshotSource().capture())
    }

    private fun assertFailure(expected: CoverReadFailure, result: CoverReadResult) {
        assertEquals(expected, (result as CoverReadResult.Unavailable).reason)
    }
}
