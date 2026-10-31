package com.cooper.lithic.feature.media

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.util.Size
import androidx.activity.result.contract.ActivityResultContracts
import com.cooper.lithic.BuildConfig
import com.cooper.lithic.data.media.photolite.PhotoLiteLimits
import com.cooper.lithic.data.media.photolite.PhotoLitePng
import com.cooper.lithic.data.media.photolite.PhotoLitePngOutput
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

/** No source URI, path, provider exception or original image metadata is exposed in this message. */
class PhotoLiteImageException(val reason: String, val unknown: Boolean = false) : IOException(reason)

/**
 * A transient fresh canvas, never a durable media asset. Closing cannot recycle a bitmap already
 * handed to Compose. An in-progress encoder retains the payload charge until its actual return.
 */
class PhotoLitePreview internal constructor(val bitmap: Bitmap, private val reservation: CoverPixelReservation) : AutoCloseable {
    private var closed = false
    private var published = false
    private var encoders = 0
    private var disposed = false

    @Synchronized fun markPublished() {
        if (closed || disposed) throw PhotoLiteImageException("PREVIEW_CLOSED")
        published = true
    }

    @Synchronized internal fun retainForEncoding(): PhotoLiteEncodingLease {
        if (closed || disposed || bitmap.isRecycled) throw PhotoLiteImageException("PREVIEW_CLOSED")
        val lease = PhotoLiteEncodingLease(bitmap) { releaseEncoder() }
        encoders++
        return lease
    }

    @Synchronized override fun close() {
        closed = true
        disposeWhenUnused()
    }

    @Synchronized private fun releaseEncoder() {
        check(encoders > 0)
        encoders--
        disposeWhenUnused()
    }

    private fun disposeWhenUnused() {
        if (!closed || encoders != 0 || disposed) return
        disposed = true
        if (!published) discardRaster(bitmap, reservation) else reservation.close()
    }
}

internal class PhotoLiteEncodingLease(val bitmap: Bitmap, private val release: () -> Unit) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    override fun close() { if (closed.compareAndSet(false, true)) release() }
}

/**
 * PHOTO-LITE is an explicit API36 experiment. loadThumbnail decodes in this host process; a requested
 * size, cancellation signal and post-return allocation checks are not a hard native/RSS bound, an
 * isolated JPEG validator or proof that a selected JPEG's original entropy stream is complete.
 */
open class PhotoLiteThumbnailPipeline internal constructor(
    private val thumbnailLoader: (Uri, Size, CancellationSignal) -> Bitmap,
    private val mimeType: (Uri) -> String?,
    private val available: () -> Boolean,
) {
    constructor(context: Context) : this(
        thumbnailLoader = { uri, requested, signal ->
            if (Build.VERSION.SDK_INT < 29) throw PhotoLiteImageException("UNSUPPORTED_PLATFORM")
            context.applicationContext.contentResolver.loadThumbnail(uri, requested, signal)
        },
        mimeType = { uri -> context.applicationContext.contentResolver.getType(uri) },
        available = {
            BuildConfig.PHOTO_LITE_EXPERIMENT && Build.VERSION.SDK_INT == 36 &&
                ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context.applicationContext)
        },
    )

    open fun isAvailable(): Boolean = try { available() } catch (_: Exception) { false }

    open suspend fun load(uri: Uri): PhotoLitePreview {
        if (!isAvailable()) throw PhotoLiteImageException("UNSUPPORTED_PLATFORM")
        if (uri.scheme != ContentResolver.SCHEME_CONTENT || uri.authority.isNullOrBlank() ||
            uri.authority.orEmpty().contains('@')) throw PhotoLiteImageException("INVALID_PHOTO_SOURCE")
        val admission = PhotoLiteNativeAdmission.enter()
        val signal = CancellationSignal()
        val pending = PendingPhotoLiteRaster()
        try {
            runNative(signal) {
                CoverPreviewPipeline.serialized {
                    try {
                        currentCoroutineContext().ensureActive()
                        val type = mimeType(uri)
                        if (type == null || !type.startsWith("image/") || type == "image/*")
                            throw PhotoLiteImageException("INVALID_PHOTO_TYPE")
                        pending.sourceReservation = CoverPreviewPipeline.pixels.reserve(PhotoLiteLimits.MAX_SOURCE_RASTER_BYTES)
                            ?: throw PhotoLiteImageException("PIXEL_BUDGET_BUSY")
                        // Keep the owner outside cancellable dispatch: prompt cancellation must not lose a returned raster.
                        pending.source = thumbnailLoader(uri, Size(PhotoLiteLimits.REQUEST_EDGE, PhotoLiteLimits.REQUEST_EDGE), signal)
                        currentCoroutineContext().ensureActive()
                        val source = requireNotNull(pending.source)
                        if (source.isRecycled || !PhotoLiteLimits.acceptsThumbnail(source.width, source.height,
                                source.allocationByteCount.toLong(), source.config != null && source.config != Bitmap.Config.HARDWARE))
                            throw PhotoLiteImageException("THUMBNAIL_LIMIT_EXCEEDED")
                        val (width, height) = PhotoLiteLimits.normalizedSize(source.width, source.height)
                        val expectedBytes = width.toLong() * height * 4L
                        pending.normalizedReservation = CoverPreviewPipeline.pixels.reserve(expectedBytes)
                            ?: throw PhotoLiteImageException("PIXEL_BUDGET_BUSY")
                        // A new sRGB raster carries pixels only; no original EXIF, location or external URI is copied.
                        pending.normalized = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888, true,
                            ColorSpace.get(ColorSpace.Named.SRGB))
                        val normalized = requireNotNull(pending.normalized)
                        if (normalized.allocationByteCount.toLong() !in 1..expectedBytes)
                            throw PhotoLiteImageException("RASTER_ALLOCATION_UNKNOWN", unknown = true)
                        Canvas(normalized).drawBitmap(source, null, Rect(0, 0, width, height),
                            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                        currentCoroutineContext().ensureActive()
                        pending.preview = PhotoLitePreview(normalized, requireNotNull(pending.normalizedReservation))
                        pending.normalized = null
                        pending.normalizedReservation = null
                    } finally { pending.closeRasters() }
                }
            }
            currentCoroutineContext().ensureActive()
            return requireNotNull(pending.preview).also { pending.preview = null }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: OutOfMemoryError) {
            PhotoLiteNativeAdmission.poison()
            throw PhotoLiteImageException("NATIVE_MEMORY_UNKNOWN", unknown = true)
        } catch (failure: PhotoLiteImageException) { throw failure }
        catch (_: Exception) { throw PhotoLiteImageException("THUMBNAIL_UNAVAILABLE") }
        finally {
            try { pending.close() } finally { admission.close() }
        }
    }

    /** Called only after explicit confirmation. No file or database is written by this pipeline. */
    open suspend fun encode(preview: PhotoLitePreview): PhotoLitePng {
        if (!isAvailable()) throw PhotoLiteImageException("UNSUPPORTED_PLATFORM")
        var encoding: PhotoLiteEncodingLease? = null
        var admission: AutoCloseable? = null
        try {
            val retained = preview.retainForEncoding()
            encoding = retained
            admission = PhotoLiteNativeAdmission.enter()
            var verified: PhotoLitePng? = null
            runNative {
                CoverPreviewPipeline.serialized {
                    currentCoroutineContext().ensureActive()
                    val raster = retained.bitmap
                    if (raster.isRecycled || raster.config != Bitmap.Config.ARGB_8888 ||
                        raster.width !in 1..PhotoLiteLimits.FINAL_EDGE || raster.height !in 1..PhotoLiteLimits.FINAL_EDGE ||
                        raster.allocationByteCount.toLong() !in 1..(PhotoLiteLimits.FINAL_EDGE.toLong() * PhotoLiteLimits.FINAL_EDGE * 4))
                        throw PhotoLiteImageException("INVALID_NORMALIZED_RASTER")
                    PhotoLitePngOutput().use { output ->
                        if (!raster.compress(Bitmap.CompressFormat.PNG, 100, output))
                            throw PhotoLiteImageException("PNG_ENCODING_FAILED")
                        currentCoroutineContext().ensureActive()
                        verified = PhotoLitePng.checked(output.toByteArray())
                    }
                }
            }
            currentCoroutineContext().ensureActive()
            return requireNotNull(verified)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: OutOfMemoryError) {
            PhotoLiteNativeAdmission.poison()
            throw PhotoLiteImageException("NATIVE_MEMORY_UNKNOWN", unknown = true)
        } catch (failure: PhotoLiteImageException) { throw failure }
        catch (_: Exception) { throw PhotoLiteImageException("PNG_VERIFICATION_FAILED") }
        finally {
            try { encoding?.close() } finally { admission?.close() }
        }
    }
}

/** One PHOTO request may wait for the shared C6 slot; further requests never build an unbounded queue. */
private object PhotoLiteNativeAdmission {
    private val busy = AtomicBoolean(false)
    private val poisoned = AtomicBoolean(false)
    fun poison() { poisoned.set(true) }
    fun enter(): AutoCloseable {
        if (poisoned.get()) throw PhotoLiteImageException("PHOTO_RESOURCE_UNKNOWN", unknown = true)
        val lease = AutoCloseable { busy.set(false) }
        if (!busy.compareAndSet(false, true)) throw PhotoLiteImageException("PHOTO_WORK_PENDING", unknown = true)
        if (poisoned.get()) { busy.set(false); throw PhotoLiteImageException("PHOTO_RESOURCE_UNKNOWN", unknown = true) }
        return lease
    }
}

/** A cancellation request never releases native admission or a raster lease before real return. */
private suspend fun runNative(signal: CancellationSignal? = null, body: suspend () -> Unit) = supervisorScope {
    val worker = async(Dispatchers.IO) {
        try { body() }
        catch (failure: OutOfMemoryError) {
            // await may already have been cancelled: join alone does not rethrow this late failure.
            PhotoLiteNativeAdmission.poison()
            throw failure
        } catch (failure: PhotoLiteImageException) {
            if (failure.unknown) PhotoLiteNativeAdmission.poison()
            throw failure
        }
    }
    try { worker.await() }
    catch (cancelled: CancellationException) {
        try { signal?.cancel() }
        catch (_: RuntimeException) { PhotoLiteNativeAdmission.poison() }
        finally { withContext(NonCancellable) { worker.join() } }
        throw cancelled
    }
}

private class PendingPhotoLiteRaster {
    var source: Bitmap? = null
    var sourceReservation: CoverPixelReservation? = null
    var normalized: Bitmap? = null
    var normalizedReservation: CoverPixelReservation? = null
    var preview: PhotoLitePreview? = null

    fun closeRasters() {
        try { discardRaster(source, sourceReservation) }
        finally {
            source = null
            sourceReservation = null
            try { discardRaster(normalized, normalizedReservation) }
            finally { normalized = null; normalizedReservation = null }
        }
    }
    fun close() {
        try { preview?.close() }
        finally { preview = null; closeRasters() }
    }
}

private fun discardRaster(bitmap: Bitmap?, reservation: CoverPixelReservation?) {
    try {
        if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
        reservation?.close()
    } catch (_: RuntimeException) {
        PhotoLiteNativeAdmission.poison()
        // Keep the charge on ambiguous cleanup; the process cannot start another PHOTO operation.
        throw PhotoLiteImageException("RASTER_CLOSE_UNKNOWN", unknown = true)
    } catch (_: OutOfMemoryError) {
        PhotoLiteNativeAdmission.poison()
        throw PhotoLiteImageException("RASTER_CLOSE_UNKNOWN", unknown = true)
    }
}
