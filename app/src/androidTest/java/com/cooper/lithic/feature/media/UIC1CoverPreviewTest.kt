package com.cooper.lithic.feature.media

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.media.CoverBaseline
import com.cooper.lithic.data.media.CoverCommand
import com.cooper.lithic.data.media.M10ImageSourceFixtures
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.media.MediaFailureKind
import com.cooper.lithic.data.media.MediaResult
import com.cooper.lithic.data.media.StoredImage
import com.cooper.lithic.data.media.SyntheticImageSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI-only preview behavior, backed by a separate real generation and verified media bytes. */
@RunWith(AndroidJUnit4::class)
class UIC1CoverPreviewTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val date = LocalDate.of(2026, 1, 31)
    private val time = Instant.parse("2026-01-31T03:04:05Z")
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var repository: MediaRepository
    private val sources = mutableListOf<SyntheticImageSource>()
    private val loaders = mutableListOf<CoverPreviewLoader>()
    private var generationClosed = false

    @Before fun setUp() = runBlocking {
        root = File(context.noBackupFilesDir, "ui-c1-cover-${UUID.randomUUID()}")
        generation = DataGenerationStore(context, root).open(ZoneId.of("UTC"))
        repository = MediaRepository(generation.database, generation.coordinator, generation.images,
            FixedBusinessClock(date), context.contentResolver)
    }

    @After fun tearDown() = runBlocking {
        loaders.forEach { it.close() }
        sources.forEach { it.delete() }
        if (::generation.isInitialized && !generationClosed) generation.close()
        if (::root.isInitialized) {
            check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
            root.deleteRecursively()
        }
    }

    @Test fun detail_preview_uses_16_by_9_frame_and_retains_original_view_action() {
        val asset = runBlocking { cover(400, 100) }
        compose.setContent { LithicTheme {
            Box(Modifier.width(320.dp)) { CoverDisplay(asset.id, repository, "ui-c1") }
        } }
        awaitImage()
        val bounds = compose.onNodeWithTag("ui-c1-cover-image").fetchSemanticsNode().boundsInRoot
        assertEquals("The detail frame should be 16:9 regardless of source image aspect ratio",
            16f / 9f, bounds.width / bounds.height, 0.01f)
        compose.onNodeWithTag("ui-c1-cover-view").assertIsDisplayed().performClick()
        compose.onNodeWithTag("ui-c1-cover-close").assertIsDisplayed().performClick()
    }

    @Test fun unset_cover_stays_distinct_from_unreadable_referenced_cover() {
        compose.setContent { LithicTheme { CoverDisplay(null, repository, "ui-c1") } }
        compose.onNodeWithTag("ui-c1-cover-empty").assertIsDisplayed()
        compose.onNodeWithTag("ui-c1-cover-error").assertDoesNotExist()
        compose.onNodeWithTag("ui-c1-cover-image").assertDoesNotExist()
    }

    @Test fun corrupt_current_bytes_show_error_and_preserve_reference_and_bytes() {
        val asset = runBlocking { cover(400, 100) }
        val facts = runBlocking { generation.database.projectDao().listProjects() }
        val corrupt = byteArrayOf(1, 2, 3, 4)
        File(generation.directory, asset.relativePath).writeBytes(corrupt)
        compose.setContent { LithicTheme { CoverDisplay(asset.id, repository, "ui-c1") } }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("ui-c1-cover-error").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("ui-c1-cover-error").assertIsDisplayed()
        compose.onNodeWithTag("ui-c1-cover-empty").assertDoesNotExist()
        compose.onNodeWithTag("ui-c1-cover-image").assertDoesNotExist()
        runBlocking {
            assertEquals(asset, generation.database.mediaDao().find(asset.id))
            assertEquals(facts, generation.database.projectDao().listProjects())
        }
        assertArrayEquals(corrupt, File(generation.directory, asset.relativePath).readBytes())
    }

    @Test fun corrupt_64dp_thumbnail_at_200_percent_font_has_complete_visible_error_representation() {
        val asset = runBlocking { cover(400, 100) }
        File(generation.directory, asset.relativePath).writeBytes(byteArrayOf(1, 2, 3, 4))
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                LithicTheme { CoverPreviewProvider(repository) {
                    Box(Modifier.size(64.dp).testTag("ui-c1-thumbnail-frame")) {
                        CoverThumbnail(asset.id, repository, "ui-c1")
                    }
                } }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("ui-c1-cover-error").fetchSemanticsNodes().isNotEmpty() }
        val error = compose.onNodeWithTag("ui-c1-cover-error").assertIsDisplayed()
        val node = error.fetchSemanticsNode()
        val frame = compose.onNodeWithTag("ui-c1-thumbnail-frame").fetchSemanticsNode().boundsInRoot
        assertTrue("The error representation must have visible bounds", node.boundsInRoot.width > 0 && node.boundsInRoot.height > 0)
        val errorBounds = node.boundsInRoot
        assertTrue("The error representation must stay inside the thumbnail",
            errorBounds.left >= frame.left && errorBounds.top >= frame.top &&
                errorBounds.right <= frame.right && errorBounds.bottom <= frame.bottom)
        if (node.config.contains(SemanticsActions.GetTextLayoutResult)) {
            val layouts = mutableListOf<TextLayoutResult>()
            error.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
            assertEquals(1, layouts.size)
            assertFalse("The full error text must fit at 200% font without clipping or ellipsis", layouts.single().hasVisualOverflow)
        } else {
            val description = if (node.config.contains(SemanticsProperties.ContentDescription))
                node.config[SemanticsProperties.ContentDescription].joinToString(" ") else ""
            assertEquals("A visible broken-image icon needs the complete explicit error description",
                "封面无法读取，进入详情可重新选择或移除", description)
        }
        compose.onNodeWithTag("ui-c1-cover-image").assertDoesNotExist()
        compose.onNodeWithTag("ui-c1-cover-empty").assertDoesNotExist()
    }

    @Test fun loading_64dp_thumbnail_at_200_percent_font_has_complete_visible_progress_representation() {
        val asset = runBlocking { cover(400, 100) }
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val reading = object : CountingRepository() {
            override suspend fun openAsset(id: String): MediaResult<StoredImage> {
                entered.complete(Unit)
                release.await()
                return super.openAsset(id)
            }
        }
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                LithicTheme { CoverPreviewProvider(reading) {
                    Box(Modifier.size(64.dp).testTag("ui-c1-thumbnail-frame")) {
                        CoverThumbnail(asset.id, reading, "ui-c1")
                    }
                } }
            }
        }
        try {
            runBlocking { withTimeout(5_000) { entered.await() } }
            val progress = compose.onNodeWithTag("ui-c1-cover-loading").assertIsDisplayed()
            val node = progress.fetchSemanticsNode()
            val frame = compose.onNodeWithTag("ui-c1-thumbnail-frame").fetchSemanticsNode().boundsInRoot
            val bounds = node.boundsInRoot
            assertTrue("The loading representation must have visible bounds", bounds.width > 0 && bounds.height > 0)
            assertTrue("The loading representation must stay inside the thumbnail",
                bounds.left >= frame.left && bounds.top >= frame.top && bounds.right <= frame.right && bounds.bottom <= frame.bottom)
            if (node.config.contains(SemanticsActions.GetTextLayoutResult)) {
                val layouts = mutableListOf<TextLayoutResult>()
                progress.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
                assertEquals(1, layouts.size)
                assertFalse("The complete loading text must fit at 200% font without clipping or ellipsis", layouts.single().hasVisualOverflow)
            } else {
                val description = if (node.config.contains(SemanticsProperties.ContentDescription))
                    node.config[SemanticsProperties.ContentDescription].joinToString(" ") else ""
                assertEquals("The compact progress indicator needs its complete Chinese description", "正在读取本地封面", description)
                assertTrue("A loading symbol must expose native progress semantics", node.config.contains(SemanticsProperties.ProgressBarRangeInfo))
                assertEquals(ProgressBarRangeInfo.Indeterminate, node.config[SemanticsProperties.ProgressBarRangeInfo])
            }
            compose.onNodeWithTag("ui-c1-cover-error").assertDoesNotExist()
            compose.onNodeWithTag("ui-c1-cover-empty").assertDoesNotExist()
            compose.onNodeWithTag("ui-c1-cover-image").assertDoesNotExist()
        } finally { release.complete(Unit) }
    }

    @Test fun verified_cache_reentry_reads_current_bytes_and_target_sizes_get_sampled_pixels() = runBlocking {
        val asset = cover(1600, 800)
        val counted = CountingRepository()
        val decodes = AtomicInteger()
        val loader = loader(counted) { stored, target ->
            assertNotEquals("Decode must leave the UI thread", Looper.getMainLooper().thread, Thread.currentThread())
            decodes.incrementAndGet()
            decodeCoverPreview(stored, target)
        }
        val small = ready(loader.load(asset.id, CoverPreviewSize(128, 128))).bitmap
        assertTrue(small.width < asset.width && small.height < asset.height)
        assertTrue(small.width <= 512 && small.height <= 512)
        assertTrue(small.width >= 128 && small.height >= 128)
        assertSame(small, ready(loader.load(asset.id, CoverPreviewSize(128, 128))).bitmap)
        assertEquals(2, counted.opens.get())
        assertEquals(1, decodes.get())
        val detail = ready(loader.load(asset.id, CoverPreviewSize(320, 180))).bitmap
        assertTrue(detail.width >= 320 && detail.height >= 180)
        assertTrue(detail.width < asset.width && detail.height < asset.height)
        assertNotSame("Target dimensions belong to the cache key", small, detail)
        assertEquals(2, decodes.get())
    }

    @Test fun cached_success_does_not_hide_deleted_or_corrupted_private_file() = runBlocking {
        val missing = cover(400, 100)
        val corrupt = cover(400, 100)
        val loader = loader()
        val target = CoverPreviewSize(128, 128)
        ready(loader.load(missing.id, target))
        ready(loader.load(corrupt.id, target))
        assertTrue(File(generation.directory, missing.relativePath).delete())
        assertEquals(CoverPreviewResult.Failed, loader.load(missing.id, target))
        val bytes = byteArrayOf(1, 2, 3, 4)
        File(generation.directory, corrupt.relativePath).writeBytes(bytes)
        assertEquals(CoverPreviewResult.Failed, loader.load(corrupt.id, target))
        assertEquals(CoverPreviewLoader.CacheUsage(0, 0), loader.cacheUsage())
        assertEquals(missing, generation.database.mediaDao().find(missing.id))
        assertEquals(corrupt, generation.database.mediaDao().find(corrupt.id))
        assertEquals(2L, generation.database.projectDao().countProjects())
        assertArrayEquals(bytes, File(generation.directory, corrupt.relativePath).readBytes())
    }

    @Test fun closed_generation_fails_reentry_instead_of_serving_success_cache() = runBlocking {
        val asset = cover(400, 100)
        val loader = loader()
        ready(loader.load(asset.id, CoverPreviewSize(128, 128)))
        generation.close()
        generationClosed = true
        assertEquals(CoverPreviewResult.Failed, loader.load(asset.id, CoverPreviewSize(128, 128)))
        assertEquals(CoverPreviewLoader.CacheUsage(0, 0), loader.cacheUsage())
    }

    @Test fun read_and_decode_concurrency_stays_at_two_and_cancel_releases_slot() = runBlocking {
        val asset = cover(400, 100)
        val counted = CountingRepository()
        val enteredTwo = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val active = AtomicInteger()
        val peak = AtomicInteger()
        val loader = loader(counted) { stored, target ->
            val current = active.incrementAndGet()
            peak.updateAndGet { maxOf(it, current) }
            if (current == 2) enteredTwo.complete(Unit)
            try {
                release.await()
                decodeCoverPreview(stored, target)
            } finally { active.decrementAndGet() }
        }
        val requests = List(6) { async(Dispatchers.Default) { loader.load(asset.id, CoverPreviewSize(128 + it, 128)) } }
        try {
            withTimeout(5_000) { enteredTwo.await() }
            delay(100)
            assertEquals("Queued previews must not read before obtaining the shared budget", 2, counted.opens.get())
            assertEquals(2, peak.get())
            requests.first().cancelAndJoin()
            release.complete(Unit)
            requests.drop(1).awaitAll().forEach { ready(it) }
            assertTrue(peak.get() <= 2)
        } finally {
            release.complete(Unit)
            requests.forEach { it.cancelAndJoin() }
        }
    }

    @Test fun caller_cancellation_and_loader_close_propagate_without_caching_unreceived_pixels() = runBlocking {
        val asset = cover(400, 100)
        val entered = CompletableDeferred<Unit>()
        val cancellationSeen = CompletableDeferred<Unit>()
        val blocking = loader { _, _ ->
            entered.complete(Unit)
            try { awaitCancellation() }
            finally { cancellationSeen.complete(Unit) }
        }
        val request = async { blocking.load(asset.id, CoverPreviewSize(128, 128)) }
        withTimeout(5_000) { entered.await() }
        request.cancelAndJoin()
        withTimeout(5_000) { cancellationSeen.await() }
        assertEquals(CoverPreviewLoader.CacheUsage(0, 0), blocking.cacheUsage())
        blocking.close()
        try {
            blocking.load(asset.id, CoverPreviewSize(128, 128))
            fail("A disposed UI loader cannot start another preview")
        } catch (_: CancellationException) { }

        val started = CompletableDeferred<Unit>()
        val owned = loader { _, _ -> started.complete(Unit); awaitCancellation() }
        val ownedRequest = async { owned.load(asset.id, CoverPreviewSize(128, 128)) }
        withTimeout(5_000) { started.await() }
        owned.close()
        try { ownedRequest.await(); fail("Closing the UI lifecycle must cancel its in-flight preview") }
        catch (_: CancellationException) { }
        assertEquals(CoverPreviewLoader.CacheUsage(0, 0), owned.cacheUsage())
    }

    @Test fun pixel_lru_counts_allocations_evicts_oldest_and_skips_single_oversized_image() = runBlocking {
        val assets = List(4) { cover(1600, 1600) }
        val decodes = AtomicInteger()
        val loader = loader { stored, target -> decodes.incrementAndGet(); decodeCoverPreview(stored, target) }
        val target = CoverPreviewSize(512, 512)
        val first = ready(loader.load(assets.first().id, target)).bitmap
        assets.drop(1).forEach { ready(loader.load(it.id, target)) }
        val usage = loader.cacheUsage()
        assertTrue(usage.bytes in 1..CoverPreviewLoader.MAX_CACHE_BYTES)
        assertEquals("Four 800px ARGB previews exceed the budget", 3, usage.entries)
        val beforeCachedRead = decodes.get()
        ready(loader.load(assets.last().id, target))
        assertEquals(beforeCachedRead, decodes.get())
        ready(loader.load(assets.first().id, target))
        assertEquals(beforeCachedRead + 1, decodes.get())
        assertFalse("Eviction must not recycle a bitmap a visible Image can still hold", first.isRecycled)
        val beforeOversize = loader.cacheUsage()
        val full = ready(loader.load(assets.first().id, CoverPreviewSize(1600, 1600))).bitmap
        assertTrue(full.allocationByteCount > CoverPreviewLoader.MAX_CACHE_BYTES)
        assertEquals(beforeOversize, loader.cacheUsage())
        val beforeRepeatedOversize = decodes.get()
        ready(loader.load(assets.first().id, CoverPreviewSize(1600, 1600)))
        assertEquals(beforeRepeatedOversize + 1, decodes.get())
        loader.close()
        assertEquals(CoverPreviewLoader.CacheUsage(0, 0), loader.cacheUsage())
        assertFalse(full.isRecycled)
    }

    @Test fun thumbnail_is_64dp_and_ordinary_recomposition_does_not_repeat_reads() {
        val asset = runBlocking { cover(400, 100) }
        val counted = CountingRepository()
        val tick = mutableStateOf(0)
        compose.setContent { LithicTheme { CoverPreviewProvider(counted) {
            Text("Recomposition ${tick.value}")
            CoverThumbnail(asset.id, counted, "ui-c1")
        } } }
        awaitImage()
        val bounds = compose.onNodeWithTag("ui-c1-cover-image").fetchSemanticsNode().boundsInRoot
        val density = context.resources.displayMetrics.density
        assertEquals(64f, bounds.width / density, 1f)
        assertEquals(bounds.width, bounds.height, 0.1f)
        repeat(5) { compose.runOnIdle { tick.value += 1 } }
        compose.waitForIdle()
        assertEquals(1, counted.opens.get())
    }

    @Test fun repository_switch_rejects_late_old_result_and_never_shares_old_success() {
        val asset = runBlocking { cover(400, 100) }
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val slow = object : CountingRepository() {
            override suspend fun openAsset(id: String): MediaResult<StoredImage> = withContext(NonCancellable) {
                entered.complete(Unit)
                release.await()
                super.openAsset(id)
            }
        }
        val unavailable = object : CountingRepository() {
            override suspend fun openAsset(id: String): MediaResult<StoredImage> =
                MediaResult.Failure(MediaFailureKind.NOT_FOUND, "Synthetic replacement facade")
        }
        val current = mutableStateOf<MediaRepository>(slow)
        compose.setContent { LithicTheme { CoverPreviewProvider(current.value) {
            CoverThumbnail(asset.id, current.value, "ui-c1")
        } } }
        try {
            runBlocking { withTimeout(5_000) { entered.await() } }
            compose.runOnIdle { current.value = unavailable }
            release.complete(Unit)
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("ui-c1-cover-error").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("ui-c1-cover-error").assertIsDisplayed()
            compose.onNodeWithTag("ui-c1-cover-image").assertDoesNotExist()
        } finally { release.complete(Unit) }
    }

    @Test fun shared_cover_reads_leave_both_references_metadata_and_file_bytes_unchanged() = runBlocking {
        val asset = cover(400, 100)
        val first = generation.database.projectDao().listProjects().single()
        val second = first.copy(id = uuid(), name = "UI-C1 second synthetic reference")
        generation.database.projectDao().insertProject(second)
        val before = generation.database.projectDao().listProjects()
        val bytes = success(repository.openAsset(asset.id)).bytes
        val loader = loader()
        repeat(5) { ready(loader.load(asset.id, CoverPreviewSize(128, 128))) }
        assertEquals(before, generation.database.projectDao().listProjects())
        assertEquals(asset, generation.database.mediaDao().find(asset.id))
        assertArrayEquals(bytes, success(repository.openAsset(asset.id)).bytes)
        assertArrayEquals(bytes, File(generation.directory, asset.relativePath).readBytes())
    }

    private open inner class CountingRepository : MediaRepository(generation.database, generation.coordinator,
        generation.images, FixedBusinessClock(date), context.contentResolver) {
        val opens = AtomicInteger()
        override suspend fun openAsset(id: String): MediaResult<StoredImage> {
            assertNotEquals("Verified reads must start off the UI thread", Looper.getMainLooper().thread, Thread.currentThread())
            opens.incrementAndGet()
            return super.openAsset(id)
        }
    }

    private fun loader(repository: MediaRepository = this.repository,
        decode: suspend (StoredImage, CoverPreviewSize) -> Bitmap? = ::decodeCoverPreview): CoverPreviewLoader =
        CoverPreviewLoader(repository, decode).also { loaders += it }

    private fun ready(result: CoverPreviewResult): CoverPreviewResult.Ready {
        assertTrue("Expected validated pixels, got $result", result is CoverPreviewResult.Ready)
        return result as CoverPreviewResult.Ready
    }

    private fun awaitImage() = compose.waitUntil(5_000) {
        compose.onAllNodesWithTag("ui-c1-cover-image").fetchSemanticsNodes().isNotEmpty()
    }

    private suspend fun cover(width: Int, height: Int): LocalMediaAssetEntity {
        val category = generation.database.categoryDao().list().single { it.type == CategoryType.PROJECT }
        val project = LongTermProjectEntity(uuid(), "UI-C1 synthetic cover", category.id, date,
            createdAt = time, updatedAt = time)
        val event = ProjectMoneyEventEntity(uuid(), project.id, MoneyDirection.OUTLAY, 15_000, date,
            "synthetic initial payment", ProjectMoneyEventRole.INITIAL, uuid())
        generation.database.projectDao().insertProject(project)
        generation.database.projectDao().insertEvent(event)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xff657f72.toInt())
        val output = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        bitmap.recycle()
        val source = M10ImageSourceFixtures.source(output.toByteArray()).also { sources += it }
        val prepared = success(repository.prepareImport(source.uri, uuid()))
        success(repository.commit(CoverCommand(uuid(), CoverBaseline.Project(project, listOf(event)),
            prepared.asset, time.plusSeconds(1))))
        return prepared.asset
    }

    private fun uuid() = UUID.randomUUID().toString()
    private fun <T> success(result: MediaResult<T>): T {
        assertTrue("Expected verified success, got $result", result is MediaResult.Success)
        return (result as MediaResult.Success<T>).value
    }
}
