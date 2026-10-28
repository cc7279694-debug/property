package com.cooper.lithic.feature.media

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.media.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger
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
    private fun png(color: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        return try {
            bitmap.eraseColor(color)
            ByteArrayOutputStream().use { out -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)); out.toByteArray() }
        } finally { bitmap.recycle() }
    }
}
