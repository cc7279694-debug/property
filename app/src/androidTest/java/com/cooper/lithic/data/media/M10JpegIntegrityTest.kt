package com.cooper.lithic.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class M10JpegIntegrityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var root: File
    private lateinit var store: LocalImageStore
    private val sources = mutableListOf<SyntheticImageSource>()

    @Before fun setUp() {
        root = File(context.noBackupFilesDir, "m10-jpeg-test-${UUID.randomUUID()}")
        check(root.mkdirs() && File(root, "media").mkdir())
        store = LocalImageStore(root)
    }

    @After fun tearDown() = runBlocking {
        // A failed or unknown consumer must preserve both its source fixture and private stage.
        // Propagate the original close failure before deleting any synthetic resource.
        if (::store.isInitialized) store.closeForGeneration()
        sources.forEach { it.delete() }
        if (::root.isInitialized) {
            check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
            root.deleteRecursively()
        }
    }

    @Test fun valid_control_jpeg_imports_successfully() = runBlocking {
        val source = M10JpegFixtures.source(M10JpegFixtures.COMPLETE).also { sources.add(it) }
        val result = store.prepareImport(context.contentResolver, source.uri, uuid())
        M10JpegObservation.record("valid-control", outcome(result))
        assertTrue("Healthy locked control must import, got $result", result is MediaResult.Success)
        val prepared = (result as MediaResult.Success<PreparedImage>).value
        val committed = store.commitPreparedAsset(prepared)
        assertTrue(committed is MediaResult.Success)
        val saved = store.openAsset(prepared.asset)
        assertTrue(saved is MediaResult.Success)
        val image = (saved as MediaResult.Success<StoredImage>).value
        assertEquals("image/jpeg", image.asset.mime)
        assertEquals(256, image.asset.width)
        assertEquals(256, image.asset.height)
        assertEquals(image.asset.sha256, M10JpegFixtures.sha(image.bytes))
    }

    @Test fun jpeg_entropy_truncated_with_eoi_is_rejected() = runBlocking {
        val source = M10JpegFixtures.source(M10JpegFixtures.BROKEN).also { sources.add(it) }
        val id = uuid()
        val result = store.prepareImport(context.contentResolver, source.uri, id)
        M10JpegObservation.record("entropy-cut-import", outcome(result).put("activePreparedLease", store.isPreparedActive(id)))
        assertTrue("EOI-retained entropy-cut JPEG must be rejected, got $result", result is MediaResult.Failure)
        assertEquals(MediaFailureKind.INVALID_IMAGE, (result as MediaResult.Failure).kind)
        assertFalse(store.isPreparedActive(id))
        assertFalse(File(root, "media/$id.jpg").exists())
    }

    /** Diagnostic assertions only constrain the healthy control; damaged outcomes remain actual observations. */
    @Test fun android_decoders_report_locked_fixture_outcomes() {
        for (name in listOf(M10JpegFixtures.COMPLETE, M10JpegFixtures.BROKEN)) {
            val bytes = M10JpegFixtures.bytes(name)
            val file = File(root, name).apply { writeBytes(bytes) }
            val report = JSONObject().put("fixture", name).put("inputBytes", bytes.size)
                .put("inputSha256", M10JpegFixtures.sha(bytes)).put("api", Build.VERSION.SDK_INT)
            try {
                val info = AndroidImageCodec.inspect(file)
                report.put("inspect", JSONObject().put("result", "SUCCESS").put("mime", info.mime)
                    .put("width", info.width).put("height", info.height))
            } catch (failure: ImageStoreException) {
                report.put("inspect", JSONObject().put("result", "FAILURE").put("kind", failure.kind.name).put("message", failure.message))
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            report.put("bitmapBounds", JSONObject().put("width", bounds.outWidth).put("height", bounds.outHeight)
                .put("mime", bounds.outMimeType))
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inScaled = false })
            report.put("bitmapFactory", bitmapOutcome(bitmap))
            bitmap?.recycle()
            try {
                val info = AndroidImageCodec.normalize(file, File(root, "normalized-$name"), "image/jpeg", ImageStoreFaults.NONE)
                report.put("normalize", JSONObject().put("result", "SUCCESS").put("width", info.width).put("height", info.height))
            } catch (failure: ImageStoreException) {
                report.put("normalize", JSONObject().put("result", "FAILURE").put("kind", failure.kind.name).put("message", failure.message))
            }
            if (Build.VERSION.SDK_INT >= 28) {
                report.put("imageDecoder", M10Api28ImageDecoderDiagnostics.report(bytes, ::bitmapOutcome))
            } else report.put("imageDecoder", "NOT_AVAILABLE_BEFORE_API28")
            M10JpegObservation.record("decoder-layers", report)
            if (name == M10JpegFixtures.COMPLETE) {
                assertEquals("SUCCESS", report.getJSONObject("inspect").getString("result"))
                assertEquals("SUCCESS", report.getJSONObject("bitmapFactory").getString("result"))
                assertEquals("SUCCESS", report.getJSONObject("normalize").getString("result"))
            }
        }
    }

    private fun bitmapOutcome(bitmap: Bitmap?): JSONObject {
        if (bitmap == null) return JSONObject().put("result", "NULL_BITMAP")
        check(bitmap.width <= 256 && bitmap.height <= 256) // Locked fixture diagnostic only, never a source-size strategy.
        var grayBottomRows = 0
        for (y in (bitmap.height - 64).coerceAtLeast(0) until bitmap.height) {
            if ((0 until bitmap.width).all { x -> bitmap.getPixel(x, y) == 0xff808080.toInt() }) grayBottomRows++
        }
        return JSONObject().put("result", "SUCCESS").put("width", bitmap.width).put("height", bitmap.height)
            .put("bottom64RowsAllGray128Count", grayBottomRows)
    }

    private fun outcome(result: MediaResult<PreparedImage>): JSONObject = when (result) {
        is MediaResult.Failure -> JSONObject().put("result", "FAILURE").put("kind", result.kind.name).put("message", result.message)
        is MediaResult.Success -> JSONObject().put("result", "SUCCESS").put("assetId", result.value.asset.id)
            .put("mime", result.value.asset.mime).put("width", result.value.asset.width).put("height", result.value.asset.height)
            .put("byteLength", result.value.asset.byteLength).put("storedSha256", result.value.asset.sha256)
    }
    private fun uuid() = UUID.randomUUID().toString()
}
