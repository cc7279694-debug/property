package com.cooper.lithic.feature.media

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.CancellationSignal
import android.util.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.data.media.photolite.PhotoLiteLimits
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic rasters/providers only. No picker, URI grant, Room, private photo or device setting. */
@RunWith(AndroidJUnit4::class)
class PhotoLiteThumbnailPipelineTest {
    // A diagnostic guard only. Pre-startup protection comes from the opt-in build and M13 runner.
    @Before fun requireIndependentPlainHost() { assertPhotoLitePlainHost() }

    @Test fun synthetic_thumbnail_is_not_enlarged_and_fresh_png_is_verified() = runBlocking {
        val original = AtomicReference<Bitmap>()
        val pipeline = pipeline { _, requested, _ ->
            assertEquals(Size(PhotoLiteLimits.REQUEST_EDGE, PhotoLiteLimits.REQUEST_EDGE), requested)
            bitmap(80, 40).also { original.set(it) }
        }
        val preview = pipeline.load(URI)
        try {
            assertEquals(80, preview.bitmap.width)
            assertEquals(40, preview.bitmap.height)
            assertEquals(Bitmap.Config.ARGB_8888, preview.bitmap.config)
            assertTrue(original.get().isRecycled)
            // This is the actual Android encoder/unchanged backup validator roundtrip, not a host substitute.
            val png = pipeline.encode(preview)
            assertEquals(80, png.width)
            assertEquals(40, png.height)
            assertTrue(png.byteLength in 1 until PhotoLiteLimits.MAX_PNG_BYTES_EXCLUSIVE.toLong())
            assertEquals(64, png.sha256.length)
        } finally { preview.close() }
        assertTrue(preview.bitmap.isRecycled)
    }

    @Test fun larger_synthetic_thumbnail_is_downsampled_without_changing_aspect() = runBlocking {
        val pipeline = pipeline { _, _, _ -> bitmap(800, 400) }
        val preview = pipeline.load(URI)
        try {
            assertEquals(640, preview.bitmap.width)
            assertEquals(320, preview.bitmap.height)
            assertEquals(Color.rgb(24, 76, 53), preview.bitmap.getPixel(320, 160))
        } finally { preview.close() }
    }

    @Test fun unavailable_environment_and_non_image_uri_never_call_thumbnail_provider() = runBlocking {
        val calls = AtomicInteger()
        val unavailable = PhotoLiteThumbnailPipeline(
            thumbnailLoader = { _, _, _ -> calls.incrementAndGet(); bitmap(2, 2) },
            mimeType = { "image/png" }, available = { false },
        )
        expectRejected { unavailable.load(URI) }
        val nonImage = PhotoLiteThumbnailPipeline(
            thumbnailLoader = { _, _, _ -> calls.incrementAndGet(); bitmap(2, 2) },
            mimeType = { "text/plain" }, available = { true },
        )
        expectRejected { nonImage.load(URI) }
        val valid = pipeline { _, _, _ -> calls.incrementAndGet(); bitmap(2, 2) }
        expectRejected { valid.load(Uri.parse("file:///synthetic-not-a-real-file")) }
        assertEquals(0, calls.get())
    }

    @Test fun oversized_platform_thumbnail_is_rejected_and_unpublished_raster_is_closed() = runBlocking {
        val source = AtomicReference<Bitmap>()
        val pipeline = pipeline { _, _, _ -> bitmap(1601, 2).also { source.set(it) } }
        expectRejected { pipeline.load(URI) }
        assertTrue(source.get().isRecycled)
    }

    @Test fun cancellation_keeps_native_admission_until_fake_provider_actually_returns() = runBlocking {
        val entered = CountDownLatch(1)
        val cancelObserved = CountDownLatch(1)
        val released = CountDownLatch(1)
        val source = AtomicReference<Bitmap>()
        val blocking = pipeline { _, _, signal ->
            // This fake acts as the recipient/provider. Application code never installs this listener.
            signal.setOnCancelListener { cancelObserved.countDown() }
            entered.countDown()
            check(released.await(5, TimeUnit.SECONDS))
            bitmap(2, 2).also { source.set(it) }
        }
        val work = launch(Dispatchers.Default) {
            try { blocking.load(URI).close(); fail("Cancelled thumbnail was published") }
            catch (_: CancellationException) { /* expected after the provider really returns */ }
        }
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            work.cancel()
            assertTrue(cancelObserved.await(5, TimeUnit.SECONDS))
            assertFalse(work.isCompleted)
            val second = pipeline { _, _, _ -> fail("A second provider must not start"); bitmap(2, 2) }
            val busy = expectRejected { second.load(URI) }
            assertTrue(busy.unknown)
        } finally {
            released.countDown()
            withTimeout(5_000) { work.join() }
        }
        assertNotNull(source.get())
        assertTrue(source.get().isRecycled)
        pipeline { _, _, _ -> bitmap(2, 2) }.load(URI).close()
    }

    @Test fun closing_preview_waits_for_encoder_reference_before_recycling_or_releasing_budget() {
        val releases = AtomicInteger()
        val raster = bitmap(2, 2)
        val preview = PhotoLitePreview(raster, CoverPixelReservation(16) { releases.incrementAndGet() })
        val encoder = preview.retainForEncoding()
        preview.close()
        assertFalse(raster.isRecycled)
        assertEquals(0, releases.get())
        encoder.close()
        assertTrue(raster.isRecycled)
        assertEquals(1, releases.get())
        encoder.close()
        preview.close()
        assertEquals(1, releases.get())
    }

    @Test fun closing_published_preview_does_not_recycle_bitmap_still_reachable_by_canvas() {
        val releases = AtomicInteger()
        val raster = bitmap(2, 2)
        val preview = PhotoLitePreview(raster, CoverPixelReservation(16) { releases.incrementAndGet() })
        preview.markPublished()
        preview.close()
        assertFalse(raster.isRecycled)
        assertEquals(1, releases.get())
        // No Canvas used this synthetic raster; only the test may now release it explicitly.
        raster.recycle()
    }

    @Test fun closed_preview_cannot_be_encoded_or_revived() = runBlocking {
        val raster = bitmap(2, 2)
        val preview = PhotoLitePreview(raster, CoverPixelReservation(16) {})
        preview.close()
        expectRejected { pipeline { _, _, _ -> bitmap(2, 2) }.encode(preview) }
        try { preview.markPublished(); fail("Closed preview was revived") }
        catch (_: PhotoLiteImageException) { /* expected */ }
    }

    private fun pipeline(loader: (Uri, Size, CancellationSignal) -> Bitmap) =
        PhotoLiteThumbnailPipeline(thumbnailLoader = loader, mimeType = { "image/png" }, available = { true })

    private suspend fun expectRejected(block: suspend () -> Any?): PhotoLiteImageException {
        try { block(); fail("Expected safe rejection") }
        catch (failure: PhotoLiteImageException) { return failure }
        throw AssertionError("Expected safe rejection")
    }

    private fun bitmap(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(24, 76, 53)) }

    private companion object { val URI: Uri = Uri.parse("content://photo-lite-synthetic/one") }
}
