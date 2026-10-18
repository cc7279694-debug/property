package com.cooper.lithic.data.media

import android.content.Context
import android.graphics.Bitmap
import android.system.Os
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.media.jpegstage.JpegStageV2
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Candidate platform checks. Compiling this class is not device execution. */
@RunWith(AndroidJUnit4::class)
class M10StageV2CompatibilityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var root: File
    private lateinit var files: MediaFileProtocol
    @Before fun createSyntheticRoot() {
        root = File(context.noBackupFilesDir, "m10-v2-candidate-${UUID.randomUUID()}")
        check(root.mkdir()); check(File(root, "media").mkdir())
        files = MediaFileProtocol(root)
    }
    @After fun removeOnlySyntheticRoot() {
        check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile && root.name.startsWith("m10-v2-candidate-"))
        if (::files.isInitialized && files.hasJpegPreparations) {
            try {
                files.jpeg.admission()
            } catch (failure: IOException) {
                android.util.Log.w("LITHIC_M10_TEST", "RETAINED_UNPROVEN_STAGE:${root.name}:${failure.javaClass.simpleName}")
                M10JpegObservation.record("v2-retained-unproven-stage", org.json.JSONObject()
                    .put("category", "RETAINED_UNPROVEN_STAGE").put("generation", root.name)
                    .put("reason", failure.javaClass.simpleName).put("cleanup", "NOT_PERFORMED"))
                return
            }
        }
        root.deleteRecursively()
    }
    private fun legacy(): LocalMediaAssetEntity {
        val id = UUID.randomUUID().toString()
        val bytes = M10JpegFixtures.bytes(M10JpegFixtures.COMPLETE)
        val stage = files.createStage(id)
        File(stage, "image.jpg").writeBytes(bytes)
        return LocalMediaAssetEntity(id, "media/$id.jpg", "image/jpeg", 256, 256, bytes.size.toLong(), M10JpegFixtures.sha(bytes)).also {
            files.saveAsset(stage, it)
        }
    }
    @Test fun legacy_uncommitted_jpeg_is_preserved_without_fabricated_receipt() = runBlocking {
        val asset = legacy(); val payload = files.preparedFile(asset).readBytes()
        val result = LocalImageStore(root).restorePrepared(asset.id)
        assertTrue(result is MediaResult.Failure && result.kind == MediaFailureKind.UNSUPPORTED)
        assertArrayEquals(payload, files.preparedFile(asset).readBytes())
        assertFalse(files.isJpegStage(asset.id))
    }
    @Test fun legacy_owned_final_is_viewable_but_does_not_prove_a_new_strict_preparation() = runBlocking {
        val asset = legacy(); val target = files.finalFile(asset)
        target.writeBytes(files.preparedFile(asset).readBytes())
        val stat = Os.lstat(target.path); files.recordOwnedFinal(asset, stat.st_dev, stat.st_ino)
        val store = LocalImageStore(root)
        assertTrue(store.openAsset(asset) is MediaResult.Success)
        assertTrue(store.restorePrepared(asset.id) is MediaResult.Failure)
        assertTrue(store.managedAssetsForCleanup().let { it is MediaResult.Success && it.value.none { value -> value.id == asset.id } })
        assertEquals(asset.sha256, M10JpegFixtures.sha(target.readBytes()))
    }
    @Test fun unresolved_v2_pin_blocks_owner_close_and_preserves_source() = runBlocking {
        val owner = JpegStageV2.Owner(root.name, UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString())
        files.jpeg.create(owner, M10JpegFixtures.bytes(M10JpegFixtures.COMPLETE).inputStream())
        files.jpeg.registerConsumer(owner, JpegStageV2.WorkerIdentity(12345, 12345, 12345))
        files.jpeg.markDelivery(owner)
        val before = files.identity(files.jpeg.sourceFile(owner))
        try { LocalImageStore(root).closeForGeneration(); fail("Unresolved consumer closed its owner") } catch (_: IOException) { }
        assertEquals(before, files.identity(files.jpeg.sourceFile(owner)))
        assertTrue(files.jpeg.snapshot(owner).pinned)
    }
    @Test fun png_still_uses_v1_preparation_and_original_output_limits() = runBlocking {
        val bitmap = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().use { out -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)); out.toByteArray() }
        bitmap.recycle()
        val source = M10ImageSourceFixtures.source(bytes)
        try {
            val id = UUID.randomUUID().toString()
            val result = LocalImageStore(root).prepareImport(context.contentResolver, source.uri, id)
            assertTrue(result is MediaResult.Success)
            val asset = (result as MediaResult.Success).value.asset
            assertEquals("image/png", asset.mime); assertFalse(files.isJpegStage(id))
            assertTrue(asset.byteLength <= AndroidImageCodec.STORED_LIMIT)
            assertTrue(maxOf(asset.width, asset.height) <= AndroidImageCodec.EDGE_LIMIT)
        } finally { source.delete() }
    }
}
