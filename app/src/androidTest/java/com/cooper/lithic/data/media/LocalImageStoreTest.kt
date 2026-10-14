package com.cooper.lithic.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.ExifInterface
import android.net.Uri
import android.system.Os
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Random
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalImageStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var root: File
    private lateinit var store: LocalImageStore
    private val sources = mutableListOf<SyntheticImageSource>()
    @Before fun setUp() { root = File(context.noBackupFilesDir, "m10-image-test-${UUID.randomUUID()}").apply { mkdirs(); File(this, "media").mkdir() }; store = LocalImageStore(root) }
    @After fun tearDown() { sources.forEach { it.delete() }; root.deleteRecursively() }

    @Test fun copy_survives_source_removal() = runBlocking {
        val source = source(png()); val asset = committed(source)
        val bytes = ok(store.openAsset(asset)).bytes
        assertTrue(source.delete())
        try { context.contentResolver.openInputStream(source.uri).use { fail("Removed source must no longer be readable") } } catch (_: java.io.FileNotFoundException) { }
        val reopened = LocalImageStore(root)
        val stored = ok(reopened.openAsset(asset))
        assertArrayEquals(bytes, stored.bytes); assertEquals(asset.sha256, sha(stored.bytes))
        assertEquals(asset, stored.asset)
    }
    @Test fun normalized_metadata_matches_real_private_bytes() = runBlocking {
        val asset = committed(source(png(2200, 1000)))
        assertTrue(asset.width <= 1600 && asset.height <= 1600)
        val file = File(root, asset.relativePath); val bytes = file.readBytes()
        val actual = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertEquals(actual.width, asset.width); assertEquals(actual.height, asset.height)
        assertEquals(bytes.size.toLong(), asset.byteLength); assertEquals(sha(bytes), asset.sha256)
        assertEquals("image/png", asset.mime); assertTrue(asset.byteLength <= 2_097_152L)
        actual.recycle()
    }
    @Test fun private_relative_paths() = runBlocking {
        val source = source(png()); val prepared = ok(store.prepareImport(context.contentResolver, source.uri, id()))
        assertTrue(prepared.asset.relativePath.matches(Regex("media/[a-f0-9-]{36}\\.png")))
        assertFalse(prepared.asset.relativePath.contains("content:")); assertFalse(File(prepared.asset.relativePath).isAbsolute)
    }
    @Test fun prepare_does_not_create_formal_file() = runBlocking {
        val p = prepared(); assertFalse(File(root, p.asset.relativePath).exists())
        assertTrue(store.isPreparedActive(p.asset.id))
    }
    @Test fun prompt_cancellation_after_preparation_releases_lease_and_preserves_recoverable_bytes() = runBlocking {
        val assetId=id(); val src=source(png()); var cancelledAtWrite=false
        lateinit var task: Deferred<MediaResult<PreparedImage>>
        val cancelling=LocalImageStore(root,faults=ImageStoreFaults { stage ->
            if(stage==ImageStoreStage.BEFORE_PREPARED_WRITE) { cancelledAtWrite=true;task.cancel() }
        })
        task=async(Dispatchers.IO,start=CoroutineStart.LAZY) { cancelling.prepareImport(context.contentResolver,src.uri,assetId) }
        task.start()
        withTimeout(15_000) { try { task.await();fail("Preparation cancellation must propagate") } catch(_:CancellationException) { } }
        assertTrue(cancelledAtWrite)
        assertFalse("An unreceived cancelled preparation must not retain a permanent lease",cancelling.isPreparedActive(assetId))
        assertFalse(File(root,"media/$assetId.png").exists())
        val restored=ok(LocalImageStore(root).restorePrepared(assetId))
        assertEquals(assetId,restored.asset.id)
    }
    @Test fun prepared_identity_can_be_restored_without_source_uri() = runBlocking {
        val src = source(png()); val p = ok(store.prepareImport(context.contentResolver, src.uri, id()))
        assertTrue(src.delete()); store.releasePrepared(p.asset.id)
        val restarted = LocalImageStore(root)
        assertEquals(p, ok(restarted.restorePrepared(p.asset.id)))
        assertEquals(p.asset, ok(restarted.commitPreparedAsset(p)))
    }
    @Test fun repeated_prepare_does_not_encode_or_create_second_asset() = runBlocking {
        val src = source(png()); val uuid = id(); val p = ok(store.prepareImport(context.contentResolver, src.uri, uuid))
        val again = ok(store.prepareImport(context.contentResolver, src.uri, uuid))
        assertEquals(p, again); assertEquals(listOf(p.asset), ok(store.managedAssetsForCleanup()))
    }
    @Test fun stable_uuid_cannot_overwrite_existing_file() = runBlocking {
        val p = prepared(); val final = File(root, p.asset.relativePath); val original = "untouchable".toByteArray(); final.writeBytes(original)
        failure(store.commitPreparedAsset(p), MediaFailureKind.CONFLICT); assertArrayEquals(original, final.readBytes())
    }
    @Test fun repeated_commit_is_exact_read_only_replay() = runBlocking {
        val p = prepared(); val first = ok(store.commitPreparedAsset(p)); val original = File(root, first.relativePath).readBytes()
        assertEquals(first, ok(store.commitPreparedAsset(p))); assertArrayEquals(original, File(root, first.relativePath).readBytes())
    }
    @Test fun partial_final_with_exclusive_inode_receipt_is_reclaimable_without_touching_unknown_files() = runBlocking {
        val failing=LocalImageStore(root,faults=ImageStoreFaults { stage ->
            if(stage==ImageStoreStage.AFTER_FINAL_OWNERSHIP) throw IOException("Synthetic body-write interruption")
        })
        val src=source(png()); val p=ok(failing.prepareImport(context.contentResolver,src.uri,id()))
        failure(failing.commitPreparedAsset(p),MediaFailureKind.IO)
        val partial=File(root,p.asset.relativePath)
        assertTrue(partial.exists());assertEquals(0L,partial.length())
        assertTrue(File(root,"media/.m10-imports/${p.asset.id}/owned-final.json").isFile)
        val unknown=File(root,"media/${id()}.png").apply{writeBytes(png())}
        val unknownBytes=unknown.readBytes()
        failing.releasePrepared(p.asset.id)
        assertTrue(ok(LocalImageStore(root).removeUnreferenced(p.asset)))
        assertFalse(partial.exists());assertArrayEquals(unknownBytes,unknown.readBytes())
    }
    @Test fun same_uuid_final_without_ownership_receipt_is_preserved_by_cleanup() = runBlocking {
        val p=prepared();val unknown=File(root,p.asset.relativePath).apply{writeBytes(png())}
        val bytes=unknown.readBytes();store.releasePrepared(p.asset.id)
        failure(store.removeUnreferenced(p.asset),MediaFailureKind.PATH)
        assertArrayEquals(bytes,unknown.readBytes())
    }
    @Test fun replaced_inode_with_same_uuid_and_old_receipt_is_never_overwritten_or_deleted() = runBlocking {
        val p=prepared();ok(store.commitPreparedAsset(p));store.releasePrepared(p.asset.id)
        val final=File(root,p.asset.relativePath)
        val retainedOriginal=File(root,"retained-original.png")
        assertTrue(final.renameTo(retainedOriginal))
        val replacement=File(root,"different-inode.png").apply{writeBytes(png(30,20))}
        val bytes=replacement.readBytes()
        assertTrue(replacement.renameTo(final))
        assertTrue(Os.lstat(final.path).st_ino!=Os.lstat(retainedOriginal.path).st_ino)
        failure(store.commitPreparedAsset(p),MediaFailureKind.CONFLICT)
        failure(store.removeUnreferenced(p.asset),MediaFailureKind.PATH)
        assertArrayEquals(bytes,final.readBytes());assertTrue(retainedOriginal.exists())
    }
    @Test fun remove_active_prepared_resource_is_rejected() = runBlocking {
        val p = prepared(); failure(store.removeUnreferenced(p.asset), MediaFailureKind.IN_USE)
        assertEquals(p, ok(store.restorePrepared(p.asset.id)))
    }
    @Test fun orphan_final_can_be_reclaimed_after_restart() = runBlocking {
        val p = prepared(); ok(store.commitPreparedAsset(p)); store.releasePrepared(p.asset.id)
        val restarted = LocalImageStore(root)
        assertEquals(listOf(p.asset), ok(restarted.managedAssetsForCleanup()))
        assertTrue(ok(restarted.removeUnreferenced(p.asset))); assertFalse(File(root, p.asset.relativePath).exists())
        assertTrue(ok(restarted.managedAssetsForCleanup()).isEmpty())
    }
    @Test fun cleanup_preserves_unknown_file_and_invalid_owner() = runBlocking {
        val unknown = File(root, "media/${id()}.png").apply { writeBytes(png()) }
        val invalid = File(root, "media/.m10-imports/${id()}").apply { mkdirs(); File(this, "owner.json").writeText("{}") }
        assertTrue(ok(store.managedAssetsForCleanup()).isEmpty())
        assertTrue(unknown.exists()); assertTrue(File(invalid, "owner.json").exists())
    }
    @Test fun discard_prepared_does_not_delete_final_with_uncertain_db_result() = runBlocking {
        val p = prepared(); ok(store.commitPreparedAsset(p)); ok(store.discardPreparedAsset(p.asset.id))
        assertTrue(File(root, p.asset.relativePath).exists()); assertEquals(p.asset.sha256, sha(ok(store.openAsset(p.asset)).bytes))
        assertEquals(listOf(p.asset), ok(store.managedAssetsForCleanup()))
    }
    @Test fun discarded_uncommitted_preparation_is_safely_removed() = runBlocking {
        val p = prepared(); assertTrue(ok(store.discardPreparedAsset(p.asset.id)))
        failure(store.restorePrepared(p.asset.id), MediaFailureKind.NOT_FOUND)
        assertTrue(ok(store.managedAssetsForCleanup()).isEmpty())
    }
    @Test fun repeated_reads_do_not_change_file_or_hash() = runBlocking {
        val asset = committed(source(png())); val file = File(root, asset.relativePath); val original = file.readBytes(); val modified = file.lastModified()
        repeat(3) { assertArrayEquals(original, ok(store.verifyAsset(asset)).bytes) }
        assertEquals(modified, file.lastModified()); assertEquals(asset.sha256, sha(file.readBytes()))
    }
    @Test fun png_transparency_is_not_silently_jpeg_encoded() = runBlocking {
        val b = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.TRANSPARENT); setPixel(15, 12, Color.argb(97, 220, 10, 30)) }
        val src = source(encode(b, Bitmap.CompressFormat.PNG)); b.recycle()
        val asset = committed(src); assertEquals("image/png", asset.mime)
        val bytes = ok(store.openAsset(asset)).bytes; val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertEquals(0, Color.alpha(image.getPixel(0, 0))); assertEquals(97, Color.alpha(image.getPixel(15, 12))); image.recycle()
    }
    @Test fun jpeg_direction_and_location_metadata_are_normalized() = runBlocking {
        val bytes = withOrientation(jpeg(80, 40), 6); val src = source(bytes, "image/jpeg")
        requireNotNull(context.contentResolver.openFileDescriptor(src.uri,"rw")).use { descriptor ->
            val sourceExif=ExifInterface(descriptor.fileDescriptor)
            sourceExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,"31/1,13/1,0/1")
            sourceExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF,"N")
            sourceExif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,"121/1,28/1,0/1")
            sourceExif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF,"E")
            sourceExif.saveAttributes()
        }
        requireNotNull(context.contentResolver.openFileDescriptor(src.uri,"r")).use { descriptor ->
            assertNotNull(ExifInterface(descriptor.fileDescriptor).getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        }
        val asset = committed(src)
        assertEquals(40, asset.width); assertEquals(80, asset.height)
        val saved = File(root, asset.relativePath); val exif = ExifInterface(saved.path)
        assertTrue(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) in listOf(0, 1))
        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)); assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
    }
    @Test fun all_eight_exif_orientations_use_correct_corner_geometry() = runBlocking {
        val expected = arrayOf(intArrayOf(0,1,2,3), intArrayOf(1,0,3,2), intArrayOf(3,2,1,0), intArrayOf(2,3,0,1), intArrayOf(0,2,1,3), intArrayOf(2,0,3,1), intArrayOf(3,1,2,0), intArrayOf(1,3,0,2))
        val colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW)
        for (orientation in 1..8) {
            val b = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
            for (y in 0 until 40) for (x in 0 until 80) b.setPixel(x,y, colors[(if(y>=20)2 else 0)+(if(x>=40)1 else 0)])
            val asset = committed(source(withOrientation(encode(b, Bitmap.CompressFormat.JPEG), orientation), "image/jpeg")); b.recycle()
            val data = ok(store.openAsset(asset)).bytes; val image = BitmapFactory.decodeByteArray(data,0,data.size)
            val points = arrayOf(5 to 5, image.width-6 to 5, 5 to image.height-6, image.width-6 to image.height-6)
            points.forEachIndexed { index, (x,y) -> val actual = image.getPixel(x,y); val color = colors[expected[orientation-1][index]]; assertTrue("orientation $orientation corner $index", Math.abs(Color.red(actual)-Color.red(color))<40 && Math.abs(Color.green(actual)-Color.green(color))<40 && Math.abs(Color.blue(actual)-Color.blue(color))<40) }
            image.recycle()
        }
    }
    @Test fun bounded_decode_rejects_real_over_40mp_before_bitmap_allocation() = runBlocking {
        var decodeCalls = 0; val observed = LocalImageStore(root, faults=ImageStoreFaults { if(it==ImageStoreStage.BEFORE_DECODE) decodeCalls++ })
        val src = source(largeGrayPng(20_001, 2_000))
        failure(observed.prepareImport(context.contentResolver, src.uri, id()), MediaFailureKind.TOO_LARGE)
        assertEquals(0, decodeCalls)
    }
    @Test fun enormous_png_dimensions_are_rejected_before_decode() = runBlocking {
        val src = source(largeHeaderPng(Int.MAX_VALUE, Int.MAX_VALUE))
        failure(store.prepareImport(context.contentResolver, src.uri, id()), MediaFailureKind.TOO_LARGE)
    }
    @Test fun zero_png_dimensions_are_invalid() = runBlocking { failure(store.prepareImport(context.contentResolver, source(largeHeaderPng(0,1)).uri, id()), MediaFailureKind.INVALID_IMAGE) }
    @Test fun corrupt_png_crc_is_rejected() = runBlocking { val bytes=png(); bytes[29]=(bytes[29].toInt() xor 1).toByte(); failure(store.prepareImport(context.contentResolver, source(bytes).uri,id()),MediaFailureKind.INVALID_IMAGE) }
    @Test fun truncated_png_is_rejected() = runBlocking { val bytes=png(); failure(store.prepareImport(context.contentResolver, source(bytes.copyOf(bytes.size-6)).uri,id()),MediaFailureKind.INVALID_IMAGE) }
    @Test fun truncated_jpeg_is_rejected() = runBlocking { val bytes=jpeg(); failure(store.prepareImport(context.contentResolver, source(bytes.copyOf(bytes.size-2),"image/jpeg").uri,id()),MediaFailureKind.INVALID_IMAGE) }
    @Test fun non_image_input_is_explicitly_rejected() = runBlocking { failure(store.prepareImport(context.contentResolver,source("not an image".toByteArray()).uri,id()),MediaFailureKind.UNSUPPORTED) }
    @Test fun incorrect_declared_mime_is_rejected() = runBlocking { failure(store.prepareImport(context.contentResolver,source(png(),"image/jpeg").uri,id()),MediaFailureKind.INVALID_IMAGE) }
    @Test fun gif_is_not_accepted_as_static_cover() = runBlocking { failure(store.prepareImport(context.contentResolver,source("GIF89a".toByteArray(),"image/gif").uri,id()),MediaFailureKind.UNSUPPORTED) }
    @Test fun apng_animation_control_is_rejected() = runBlocking { val original=png(); val bytes=original.copyOfRange(0,33)+chunk("acTL",byteArrayOf(0,0,0,2,0,0,0,0))+original.copyOfRange(33,original.size); failure(store.prepareImport(context.contentResolver,source(bytes).uri,id()),MediaFailureKind.UNSUPPORTED) }
    @Test fun mpo_multiframe_jpeg_is_rejected() = runBlocking { val original=jpeg(); val segment=byteArrayOf(0xff.toByte(),0xe2.toByte(),0,6,77,80,70,0); val bytes=original.copyOfRange(0,2)+segment+original.copyOfRange(2,original.size); failure(store.prepareImport(context.contentResolver,source(bytes,"image/jpeg").uri,id()),MediaFailureKind.UNSUPPORTED) }
    @Test fun output_still_over_2mib_is_explicit_failure() = runBlocking {
        val b=Bitmap.createBitmap(1600,1600,Bitmap.Config.ARGB_8888); val random=Random(42)
        val row=IntArray(1600); for(y in 0 until 1600){ for(x in row.indices) row[x]=random.nextInt(); b.setPixels(row,0,1600,0,y,1600,1) }
        val bytes=encode(b,Bitmap.CompressFormat.PNG); b.recycle(); assertTrue(bytes.size>2_097_152)
        failure(store.prepareImport(context.contentResolver,source(bytes).uri,id()),MediaFailureKind.TOO_LARGE)
    }
    @Test fun oversized_source_stream_is_bounded() = runBlocking {
        val src=source(png()); src.write { output -> val block=ByteArray(1024*1024); repeat(65){output.write(block)} }
        failure(store.prepareImport(context.contentResolver,src.uri,id()),MediaFailureKind.TOO_LARGE)
    }
    @Test fun decode_failure_leaves_no_managed_asset() = runBlocking { val src=source(png().copyOf(25)); assertTrue(store.prepareImport(context.contentResolver,src.uri,id()) is MediaResult.Failure); assertTrue(ok(store.managedAssetsForCleanup()).isEmpty()) }
    @Test fun encoding_failure_leaves_no_formal_resource() = runBlocking { val failing=LocalImageStore(root,faults=ImageStoreFaults { if(it==ImageStoreStage.BEFORE_ENCODE) throw IOException("synthetic encoding failure") }); val src=source(png()); failure(failing.prepareImport(context.contentResolver,src.uri,id()),MediaFailureKind.IO); assertTrue(ok(failing.managedAssetsForCleanup()).isEmpty()) }
    @Test fun file_write_failure_leaves_no_formal_resource() = runBlocking { val failing=LocalImageStore(root,faults=ImageStoreFaults { if(it==ImageStoreStage.BEFORE_PREPARED_WRITE) throw IOException("synthetic write failure") }); val src=source(png()); failure(failing.prepareImport(context.contentResolver,src.uri,id()),MediaFailureKind.IO); assertTrue(ok(failing.managedAssetsForCleanup()).isEmpty()) }
    @Test fun missing_file_is_not_reported_as_no_cover() = runBlocking { val asset=committed(source(png())); assertTrue(File(root,asset.relativePath).delete()); failure(store.openAsset(asset),MediaFailureKind.NOT_FOUND) }
    @Test fun changed_bytes_are_not_reencoded_or_silently_repaired() = runBlocking { val asset=committed(source(png())); val file=File(root,asset.relativePath); val original=file.readBytes(); val altered=original.clone().also{it[30]=(it[30].toInt() xor 1).toByte()}; file.writeBytes(altered); failure(store.openAsset(asset),MediaFailureKind.INVALID_IMAGE); assertArrayEquals(altered,file.readBytes()) }
    @Test fun metadata_mismatch_is_explicit_error() = runBlocking { val asset=committed(source(png())); failure(store.openAsset(asset.copy(width=asset.width+1)),MediaFailureKind.INVALID_IMAGE) }
    @Test fun path_into_other_generation_cannot_read_or_delete() = runBlocking { val asset=committed(source(png())); val other=File(root.parentFile,"m10-other-${id()}").apply{mkdirs();File(this,"media").mkdir()}; try{ val file=File(other,asset.relativePath).apply{writeBytes(File(root,asset.relativePath).readBytes())}; val here=LocalImageStore(other); failure(here.removeUnreferenced(asset),MediaFailureKind.PATH); assertTrue(file.exists()) }finally{other.deleteRecursively()} }
    @Test fun symlink_target_cannot_escape_private_media() = runBlocking { val asset=committed(source(png())); val file=File(root,asset.relativePath); val outside=File(root,"protected.txt").apply{writeText("keep")}; assertTrue(file.delete()); Os.symlink(outside.path,file.path); failure(store.openAsset(asset),MediaFailureKind.PATH); failure(store.removeUnreferenced(asset),MediaFailureKind.PATH); assertEquals("keep",outside.readText()) }
    @Test fun symlink_media_directory_is_rejected() = runBlocking { val media=File(root,"media"); assertTrue(media.delete()); val outside=File(root,"other").apply{mkdir()}; Os.symlink(outside.path,media.path); failure(store.prepareImport(context.contentResolver,source(png()).uri,id()),MediaFailureKind.PATH); assertTrue(outside.listFiles()!!.isEmpty()) }
    @Test fun nested_or_absolute_asset_path_is_rejected() = runBlocking { val asset=committed(source(png())); failure(store.openAsset(asset.copy(relativePath="media/nested/${asset.id}.png")),MediaFailureKind.PATH) }
    @Test fun original_uri_is_not_a_persistent_asset_path() = runBlocking { failure(store.prepareImport(context.contentResolver,Uri.fromFile(File(root,"external-source.png")),id()),MediaFailureKind.PATH) }

    @Test fun committed_cover_releases_only_redundant_prepared_payload_and_keeps_partial_payload() = runBlocking {
        val prepared = prepared()
        val normalizedBytes = File(root, "media/.m10-imports/${prepared.asset.id}/image.png").readBytes()
        val asset = ok(store.commitPreparedAsset(prepared))
        val completedStage = File(root, "media/.m10-imports/${asset.id}")
        assertEquals(
            setOf("owner.json", "asset.json", "owned-final.json"),
            completedStage.list()!!.toSet(),
        )
        assertArrayEquals(normalizedBytes, ok(store.openAsset(asset)).bytes)
        store.releasePrepared(asset.id)
        val restored = ok(store.restorePrepared(asset.id))
        assertEquals(prepared, restored)
        assertEquals(asset, ok(store.commitPreparedAsset(restored)))
        assertArrayEquals(normalizedBytes, File(root, asset.relativePath).readBytes())

        val interruptedStore = LocalImageStore(root, faults = ImageStoreFaults { stage ->
            if (stage == ImageStoreStage.AFTER_FINAL_OWNERSHIP) {
                throw IOException("Synthetic body-write interruption")
            }
        })
        val source = source(png())
        val interrupted = ok(interruptedStore.prepareImport(context.contentResolver, source.uri, id()))
        val preparedFile = File(root, "media/.m10-imports/${interrupted.asset.id}/image.png")
        val retainedBytes = preparedFile.readBytes()
        failure(interruptedStore.commitPreparedAsset(interrupted), MediaFailureKind.IO)
        assertArrayEquals(retainedBytes, preparedFile.readBytes())
        assertEquals(0L, File(root, interrupted.asset.relativePath).length())
    }

    @Test fun replay_syncs_owned_complete_final_before_allowing_reference_or_pruning_payload() = runBlocking {
        var syncAttempts = 0
        val interruptedStore = LocalImageStore(root, faults = ImageStoreFaults { stage ->
            if (stage == ImageStoreStage.BEFORE_FINAL_SYNC) {
                syncAttempts += 1
                if (syncAttempts == 1) throw IOException("Synthetic final synchronization failure")
            }
        })
        val source = source(png())
        val prepared = ok(interruptedStore.prepareImport(context.contentResolver, source.uri, id()))
        val stageDirectory = File(root, "media/.m10-imports/${prepared.asset.id}")
        val preparedFile = File(stageDirectory, "image.png")
        val normalizedBytes = preparedFile.readBytes()
        failure(interruptedStore.commitPreparedAsset(prepared), MediaFailureKind.IO)
        val final = File(root, prepared.asset.relativePath)
        assertArrayEquals(normalizedBytes, final.readBytes())
        assertArrayEquals(normalizedBytes, preparedFile.readBytes())
        assertTrue(File(stageDirectory, "owned-final.json").isFile)
        assertEquals(1, syncAttempts)

        assertEquals(prepared.asset, ok(interruptedStore.commitPreparedAsset(prepared)))
        assertEquals(2, syncAttempts)
        assertArrayEquals(normalizedBytes, ok(interruptedStore.openAsset(prepared.asset)).bytes)
        assertEquals(
            setOf("owner.json", "asset.json", "owned-final.json"),
            stageDirectory.list()!!.toSet(),
        )
    }

    private suspend fun prepared(): PreparedImage { val src=source(png()); return ok(store.prepareImport(context.contentResolver,src.uri,id())) }
    private suspend fun committed(src:SyntheticImageSource):LocalMediaAssetEntity { val p=ok(store.prepareImport(context.contentResolver,src.uri,id())); return ok(store.commitPreparedAsset(p)) }
    private fun source(bytes:ByteArray,mime:String="image/png")=M10ImageSourceFixtures.source(bytes,mime).also{sources+=it}
    private fun id()=UUID.randomUUID().toString()
    private fun <T> ok(result:MediaResult<T>):T { assertTrue("Expected successful real private image operation, got $result",result is MediaResult.Success); return (result as MediaResult.Success).value }
    private fun failure(result:MediaResult<*>,kind:MediaFailureKind){ assertTrue("Expected $kind, got $result",result is MediaResult.Failure); assertEquals(kind,(result as MediaResult.Failure).kind) }
    private fun png(width:Int=48,height:Int=32):ByteArray { val b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.argb(180,34,90,140))}; return encode(b,Bitmap.CompressFormat.PNG).also{b.recycle()} }
    private fun jpeg(width:Int=48,height:Int=32):ByteArray { val b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}; return encode(b,Bitmap.CompressFormat.JPEG).also{b.recycle()} }
    private fun encode(bitmap:Bitmap,format:Bitmap.CompressFormat):ByteArray=ByteArrayOutputStream().use{ output->assertTrue(bitmap.compress(format,95,output));output.toByteArray() }
    private fun sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
    private fun withOrientation(jpeg:ByteArray,orientation:Int):ByteArray {
        val exif=byteArrayOf(69,120,105,102,0,0,73,73,42,0,8,0,0,0,1,0,18,1,3,0,1,0,0,0,orientation.toByte(),0,0,0,0,0,0,0)
        val length=exif.size+2; return jpeg.copyOfRange(0,2)+byteArrayOf(0xff.toByte(),0xe1.toByte(),(length shr 8).toByte(),length.toByte())+exif+jpeg.copyOfRange(2,jpeg.size)
    }
    private fun largeHeaderPng(width:Int,height:Int):ByteArray { val header=ByteArrayOutputStream();DataOutputStream(header).use{it.writeInt(width);it.writeInt(height);it.write(byteArrayOf(8,0,0,0,0))};return byteArrayOf(137.toByte(),80,78,71,13,10,26,10)+chunk("IHDR",header.toByteArray())+chunk("IDAT",byteArrayOf())+chunk("IEND",byteArrayOf()) }
    private fun largeGrayPng(width:Int,height:Int):ByteArray { val raw=ByteArrayOutputStream();DeflaterOutputStream(raw).use{compressed->val row=ByteArray(width+1);repeat(height){compressed.write(row)}};val header=ByteArrayOutputStream();DataOutputStream(header).use{it.writeInt(width);it.writeInt(height);it.write(byteArrayOf(8,0,0,0,0))};return byteArrayOf(137.toByte(),80,78,71,13,10,26,10)+chunk("IHDR",header.toByteArray())+chunk("IDAT",raw.toByteArray())+chunk("IEND",byteArrayOf()) }
    private fun chunk(name:String,data:ByteArray):ByteArray { val bytes=name.toByteArray(Charsets.US_ASCII);val crc=CRC32().apply{update(bytes);update(data)};return ByteArrayOutputStream().use{buffer->DataOutputStream(buffer).use{it.writeInt(data.size);it.write(bytes);it.write(data);it.writeInt(crc.value.toInt())};buffer.toByteArray()} }
}
