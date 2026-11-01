package com.cooper.lithic.data.media

import android.app.Application
import android.content.Context
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

/** Compiled only. Future execution requires an independently authorized plain-Application host. */
@RunWith(AndroidJUnit4::class)
class GenerationPhotoStorageReaderTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val roots = M20TestRoots()
    private val opened = mutableListOf<ManagedGeneration>()
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var reader: GenerationPhotoStorageReader
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
        root = roots.create(context, "b1-photo-storage")
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        opened += generation; current = generation; roots.generation(root, generation.id)
        val pictures = RestoreSyntheticFixture.insert(generation)
        roots.media(root, pictures.keys, generation.id)
        reader = GenerationPhotoStorageReader(generation) { current === generation }
        setupComplete = true
    }

    @Test fun coherent_metadata_counts_are_read_without_changing_business_or_exposing_paths() = runBlocking {
        val before = generation.backupSnapshotSource().capture()
        val summary = ready()
        assertEquals(3L, summary.registeredCount)
        assertEquals(2L, summary.referencedCount)
        assertEquals(1L, summary.historicalCount)
        assertEquals(before.data.mediaAssets.sumOf { it.byteLength }, summary.estimatedRegisteredBytes)
        assertEquals(before, generation.backupSnapshotSource().capture())
        assertSame(reader.ownerKey, reader.ownerKey)
        assertFalse(reader.ownerKey is ManagedGeneration)
        assertNotSame(reader.ownerKey, GenerationPhotoStorageReader(generation) { true }.ownerKey)
        assertFalse(summary.toString().contains(generation.directory.path))
        assertFalse(summary.toString().contains("media/"))
    }

    @Test fun project_and_subscription_shared_cover_is_counted_once_including_historical_records() = runBlocking {
        val facts = generation.backupSnapshotSource().capture()
        val asset = facts.data.projects.first { it.coverAssetId != null }.coverAssetId!!
        generation.coordinator.withWrite {
            generation.database.openHelper.writableDatabase.execSQL("UPDATE subscriptions SET coverAssetId=?", arrayOf(asset))
            generation.database.openHelper.writableDatabase.execSQL("UPDATE long_term_projects SET coverAssetId=?", arrayOf(asset))
        }
        assertEquals(PhotoStorageSummary(3, 1, 2, facts.data.mediaAssets.sumOf { it.byteLength }), ready())
    }

    @Test fun replacement_and_display_cancellation_facts_leave_old_media_registered_for_full_backup() = runBlocking {
        val before = generation.backupSnapshotSource().capture()
        val project = before.data.projects.first { it.coverAssetId != null }
        val used = (before.data.projects.mapNotNull { it.coverAssetId } + before.data.subscriptions.mapNotNull { it.coverAssetId }).toSet()
        val replacement = before.data.mediaAssets.single { it.id !in used }.id
        generation.coordinator.withWrite {
            generation.database.openHelper.writableDatabase.execSQL("UPDATE long_term_projects SET coverAssetId=? WHERE id=?", arrayOf(replacement, project.id))
        }
        assertEquals(PhotoStorageSummary(3, 2, 1, before.data.mediaAssets.sumOf { it.byteLength }), ready())
        generation.coordinator.withWrite {
            generation.database.openHelper.writableDatabase.execSQL("UPDATE long_term_projects SET coverAssetId=NULL WHERE id=?", arrayOf(project.id))
        }
        assertEquals(PhotoStorageSummary(3, 1, 2, before.data.mediaAssets.sumOf { it.byteLength }), ready())
        val after = generation.backupSnapshotSource().capture()
        assertEquals(before.data.mediaAssets, after.data.mediaAssets)
        assertEquals(before.data.projectMoneyEvents, after.data.projectMoneyEvents)
    }

    @Test fun same_generation_id_does_not_reauthorize_a_closed_owner_or_reopen_its_database() = runBlocking {
        generation.close(); assertFalse(generation.database.isOpen)
        val reopened = DataGenerationStore(context, root).open()
        opened += reopened; current = reopened
        assertEquals(generation.id, reopened.id)
        assertFailure(PhotoStorageReadFailure.OWNER_UNAVAILABLE, reader.read())
        assertFailure(PhotoStorageReadFailure.OWNER_UNAVAILABLE, GenerationPhotoStorageReader(generation) { true }.read())
        assertFalse(generation.database.isOpen)
        assertTrue(GenerationPhotoStorageReader(reopened) { current === reopened }.read() is PhotoStorageReadResult.Ready)
    }

    @Test fun missing_media_reference_is_unavailable_instead_of_false_zero() = runBlocking {
        generation.coordinator.withWrite {
            val db = generation.database.openHelper.writableDatabase
            db.execSQL("PRAGMA foreign_keys=OFF")
            try { db.execSQL("UPDATE subscriptions SET coverAssetId=?", arrayOf(UUID.randomUUID().toString())) }
            finally { db.execSQL("PRAGMA foreign_keys=ON") }
        }
        assertFailure(PhotoStorageReadFailure.INCONSISTENT_METADATA, reader.read())
    }

    @Test fun negative_metadata_and_integer_sum_overflow_cannot_produce_a_verified_estimate() = runBlocking {
        generation.coordinator.withWrite {
            generation.database.openHelper.writableDatabase.execSQL("UPDATE local_media_assets SET byteLength=-1")
        }
        assertFailure(PhotoStorageReadFailure.INCONSISTENT_METADATA, reader.read())
        generation.coordinator.withWrite {
            generation.database.openHelper.writableDatabase.execSQL("UPDATE local_media_assets SET byteLength=?", arrayOf(Long.MAX_VALUE))
        }
        assertFailure(PhotoStorageReadFailure.READ_FAILED, reader.read())
    }

    @Test fun queued_cancellation_never_changes_facts_or_turns_into_an_empty_success() = runBlocking {
        val before = generation.backupSnapshotSource().capture()
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val holder = launch { generation.coordinator.withWrite { entered.complete(Unit); release.await() } }
        entered.await()
        val started = CompletableDeferred<Unit>()
        val cancellableReader = GenerationPhotoStorageReader(generation) { started.complete(Unit); current === generation }
        val pending = async { cancellableReader.read() }
        try {
            started.await()
            pending.cancelAndJoin()
            assertTrue(pending.isCancelled)
        } finally { release.complete(Unit); holder.join(); pending.cancelAndJoin() }
        assertEquals(before, generation.backupSnapshotSource().capture())
    }

    @Test fun owner_change_while_queued_cannot_publish_an_old_summary() = runBlocking {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
        val holder = launch { generation.coordinator.withWrite { entered.complete(Unit); release.await() } }
        entered.await()
        val started = CompletableDeferred<Unit>()
        val queuedReader = GenerationPhotoStorageReader(generation) { started.complete(Unit); current === generation }
        val pending = async { queuedReader.read() }
        try {
            started.await()
            current = null; release.complete(Unit); holder.join()
            assertFailure(PhotoStorageReadFailure.OWNER_UNAVAILABLE, pending.await())
        } finally { release.complete(Unit); holder.join(); pending.cancelAndJoin() }
    }

    private suspend fun ready(): PhotoStorageSummary = (reader.read() as PhotoStorageReadResult.Ready).summary
    private fun assertFailure(reason: PhotoStorageReadFailure, result: PhotoStorageReadResult) {
        assertEquals(PhotoStorageReadResult.Unavailable(reason), result)
    }
}
