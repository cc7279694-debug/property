package com.cooper.lithic.feature.media

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.media.*
import com.cooper.lithic.feature.home.HomeArchiveRow
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Isolated production Compose and synthetic in-memory PNG bytes only. No Room, Owner, files or snapshots.
 * Android execution is NOT_RUN until a separately authorized runtime check; these are not C3 capture methods. */
class CoverPreviewsTest {
    @get:Rule val compose = createComposeRule()
    private val projectId = "63000000-0000-0000-0000-000000000001"
    private val assetId = "63000000-0000-0000-0000-000000000002"
    private val otherAssetId = "63000000-0000-0000-0000-000000000003"
    private val reference = CoverReference(CoverOwnerType.PROJECT, projectId, assetId)

    @Test fun no_reference_keeps_the_type_fallback_without_reading() {
        val calls = AtomicInteger(0)
        val reader = fake { calls.incrementAndGet(); error("No-reference preview must not read") }
        show(reader, reference.copy(assetId = null))
        compose.onNodeWithTag("cover-fallback").assertIsDisplayed()
        compose.onNodeWithTag("record-cover-unavailable").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, calls.get()) }
    }

    @Test fun a_reference_without_a_reader_has_a_visible_readonly_error_explanation() {
        show(null, reference)
        compose.onNodeWithContentDescription("封面暂不可用").assertIsDisplayed().performClick()
        compose.onNodeWithText("封面暂不可用").assertIsDisplayed()
        compose.onNodeWithText("暂时无法读取这张封面。查看封面不会修改记录。").assertIsDisplayed()
        compose.onNodeWithText("知道了").performClick()
        compose.onNodeWithTag("cover-fallback").assertDoesNotExist()
    }

    @Test fun landscape_fit_retains_all_edges_while_default_crop_trims_the_sides() {
        assertFitAndDefaultCrop(reference, width = 48, height = 24, description = "长期项目封面")
    }

    @Test fun portrait_fit_retains_all_edges_while_default_crop_trims_top_and_bottom() {
        val subscription = reference.copy(type = CoverOwnerType.SUBSCRIPTION)
        assertFitAndDefaultCrop(subscription, width = 24, height = 48, description = "订阅封面")
    }

    private fun assertFitAndDefaultCrop(ref: CoverReference, width: Int, height: Int, description: String) {
        val image = VerifiedCoverImage(requireNotNull(ref.assetId), "image/png", width, height, edgedPng(width, height))
        val reader = fake { actual ->
            check(actual == ref)
            CoverReadResult.Ready(image)
        }
        val project = ref.type == CoverOwnerType.PROJECT
        compose.setContent { LithicTheme { ProvideCoverPreviews(reader) {
            Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                Box(Modifier.testTag("fit-frame")) {
                    HomeArchiveRow(
                        name = "合成记录", category = "其他", icon = if (project) C2Glyph.Project else C2Glyph.Subscription,
                        status = "有效", time = "10 天", metric = if (project) "项目日均成本" else "历史平均日成本",
                        amount = null, coverReference = ref, tag = "archive-row", onOpen = null,
                    )
                }
                Box(Modifier.size(80.dp, 60.dp).testTag("crop-frame")) {
                    RecordCover(ref, Modifier.fillMaxSize()) {}
                }
            }
        } } }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("record-cover-image", useUnmergedTree = true).fetchSemanticsNodes().size == 2
        }
        val fit = coverPixels("fit-frame", description)
        val crop = coverPixels("crop-frame", description)
        compose.onNodeWithTag("record-cover-unavailable").assertDoesNotExist()

        val landscape = width > height
        val fitWidth = if (landscape) fit.width.toFloat() else fit.height / 2f
        val fitHeight = if (landscape) fit.width / 2f else fit.height.toFloat()
        val left = (fit.width - fitWidth) / 2f
        val top = (fit.height - fitHeight) / 2f
        val xInset = fitWidth * EDGE_PX / (2 * width)
        val yInset = fitHeight * EDGE_PX / (2 * height)
        assertPixel(fit, left + xInset, fit.height / 2f, Color.RED)
        assertPixel(fit, left + fitWidth - xInset, fit.height / 2f, Color.BLUE)
        assertPixel(fit, fit.width / 2f, top + yInset, Color.GREEN)
        assertPixel(fit, fit.width / 2f, top + fitHeight - yInset, Color.CYAN)

        val middleX = crop.width / 2f
        val middleY = crop.height / 2f
        val nearLeft = crop.width * 0.08f
        val nearTop = crop.height * 0.08f
        if (landscape) {
            assertLetterbox(fit, fit.width / 2f, top / 2f)
            assertPixel(crop, nearLeft, middleY, Color.MAGENTA)
            assertPixel(crop, crop.width - nearLeft, middleY, Color.MAGENTA)
            assertPixel(crop, middleX, nearTop, Color.GREEN)
            assertPixel(crop, middleX, crop.height - nearTop, Color.CYAN)
        } else {
            assertLetterbox(fit, left / 2f, fit.height / 2f)
            assertPixel(crop, middleX, nearTop, Color.MAGENTA)
            assertPixel(crop, middleX, crop.height - nearTop, Color.MAGENTA)
            assertPixel(crop, nearLeft, middleY, Color.RED)
            assertPixel(crop, crop.width - nearLeft, middleY, Color.BLUE)
        }
    }

    private fun coverPixels(frameTag: String, description: String): PixelMap =
        compose.onNode(hasTestTag("record-cover-image") and hasAnyAncestor(hasTestTag(frameTag)), useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
            .assertWidthIsEqualTo(80.dp)
            .assertHeightIsEqualTo(60.dp)
            .assertContentDescriptionEquals(description)
            .captureToImage().toPixelMap()

    private fun assertPixel(image: PixelMap, x: Float, y: Float, expected: Int) {
        assertEquals("Pixel at ($x, $y)", expected, image[x.roundToInt(), y.roundToInt()].toArgb())
    }

    private fun assertLetterbox(image: PixelMap, x: Float, y: Float) {
        val pixel = image[x.roundToInt(), y.roundToInt()].toArgb()
        assertFalse("Expected a frame margin at ($x, $y)",
            pixel in setOf(Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.MAGENTA))
    }

    private fun edgedPng(width: Int, height: Int): ByteArray {
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            when {
                y < EDGE_PX -> Color.GREEN
                y >= height - EDGE_PX -> Color.CYAN
                x < EDGE_PX -> Color.RED
                x >= width - EDGE_PX -> Color.BLUE
                else -> Color.MAGENTA
            }
        }
        return png(Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888))
    }

    @Test fun unreadable_or_metadata_mismatched_bytes_cannot_be_shown_as_a_cover() {
        val reader = mutableStateOf<CoverReader>(fake { CoverReadResult.Unavailable(CoverReadFailure.INTEGRITY) })
        compose.setContent { LithicTheme { ProvideCoverPreviews(reader.value) {
            Box(Modifier.size(64.dp)) { RecordCover(reference, Modifier.size(64.dp)) { Text("类型图标", Modifier.testTag("cover-fallback")) } }
        } } }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("record-cover-unavailable").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithTag("record-cover-image").assertDoesNotExist()
        val wrongMetadata = VerifiedCoverImage(assetId, "image/png", 3, 2, png(android.graphics.Color.RED))
        val wrongReads = AtomicInteger(0)
        compose.runOnIdle { reader.value = fake { wrongReads.incrementAndGet(); CoverReadResult.Ready(wrongMetadata) } }
        compose.waitUntil(5_000) { wrongReads.get() == 1 }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("record-cover-unavailable").fetchSemanticsNodes().size == 1 }
        compose.onNodeWithTag("record-cover-image").assertDoesNotExist()
    }

    @Test fun a_late_retired_owner_cannot_replace_the_current_owners_real_bitmap() {
        val release = CompletableDeferred<Unit>()
        val oldCalls = AtomicInteger(0)
        val newCalls = AtomicInteger(0)
        val red = VerifiedCoverImage(assetId, "image/png", 2, 2, png(android.graphics.Color.RED))
        val blue = VerifiedCoverImage(assetId, "image/png", 2, 2, png(android.graphics.Color.BLUE))
        val old = fake("old-generation") {
            oldCalls.incrementAndGet()
            withContext(NonCancellable) { release.await() }
            CoverReadResult.Ready(red)
        }
        val next = fake("restored-generation") { newCalls.incrementAndGet(); CoverReadResult.Ready(blue) }
        val owner = mutableStateOf<CoverReader>(old)
        try {
            compose.setContent { LithicTheme { ProvideCoverPreviews(owner.value) {
                Box(Modifier.size(64.dp)) { RecordCover(reference, Modifier.size(64.dp)) { Text("类型图标") } }
            } } }
            compose.waitUntil(5_000) { oldCalls.get() == 1 }
            compose.runOnIdle { old.current = false; owner.value = next }
            compose.onNodeWithTag("record-cover-image").assertDoesNotExist()
            compose.runOnIdle { assertEquals(0, newCalls.get()) }
            release.complete(Unit)
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("record-cover-image").fetchSemanticsNodes().size == 1 }
            val pixels = compose.onNodeWithTag("record-cover-image").captureToImage().toPixelMap()
            assertEquals(android.graphics.Color.BLUE, pixels[pixels.width / 2, pixels.height / 2].toArgb())
            compose.runOnIdle { assertEquals(1, oldCalls.get()); assertEquals(1, newCalls.get()) }
        } finally { release.complete(Unit) }
    }

    @Test fun replacing_the_asset_reference_resets_the_previous_bitmap_before_the_new_read() {
        val release = CompletableDeferred<Unit>()
        val calls = AtomicInteger(0)
        val red = VerifiedCoverImage(assetId, "image/png", 2, 2, png(android.graphics.Color.RED))
        val blue = VerifiedCoverImage(otherAssetId, "image/png", 2, 2, png(android.graphics.Color.BLUE))
        val reader = fake { ref ->
            calls.incrementAndGet()
            if (ref.assetId == assetId) CoverReadResult.Ready(red)
            else { release.await(); CoverReadResult.Ready(blue) }
        }
        val ref = mutableStateOf(reference)
        try {
            compose.setContent { LithicTheme { ProvideCoverPreviews(reader) {
                Box(Modifier.size(64.dp)) { RecordCover(ref.value, Modifier.size(64.dp)) { Text("类型图标") } }
            } } }
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("record-cover-image").fetchSemanticsNodes().size == 1 }
            compose.runOnIdle { ref.value = reference.copy(assetId = otherAssetId) }
            compose.onNodeWithTag("record-cover-image").assertDoesNotExist()
            release.complete(Unit)
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("record-cover-image").fetchSemanticsNodes().size == 1 }
            val pixels = compose.onNodeWithTag("record-cover-image").captureToImage().toPixelMap()
            assertEquals(android.graphics.Color.BLUE, pixels[pixels.width / 2, pixels.height / 2].toArgb())
            compose.runOnIdle { assertEquals(2, calls.get()) }
        } finally { release.complete(Unit) }
    }

    private fun show(reader: CoverReader?, ref: CoverReference) {
        compose.setContent { LithicTheme { ProvideCoverPreviews(reader) {
            Box(Modifier.size(64.dp)) { RecordCover(ref, Modifier.size(64.dp)) { Text("类型图标", Modifier.testTag("cover-fallback")) } }
        } } }
    }
    private fun fake(generation: String = "synthetic", read: suspend (CoverReference) -> CoverReadResult) = FakeReader(generation, read)
    private class FakeReader(override val generationId: String, private val block: suspend (CoverReference) -> CoverReadResult) : CoverReader {
        override val ownerKey = Any()
        @Volatile var current = true
        override fun isCurrent() = current
        override suspend fun read(reference: CoverReference) = block(reference)
    }
    private fun png(color: Int): ByteArray =
        png(Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply { eraseColor(color) })

    private fun png(bitmap: Bitmap): ByteArray {
        return try {
            ByteArrayOutputStream().use { out -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)); out.toByteArray() }
        } finally { bitmap.recycle() }
    }

    private companion object { const val EDGE_PX = 6 }
}
