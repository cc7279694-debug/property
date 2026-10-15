package com.cooper.lithic.feature.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.media.MediaResult
import com.cooper.lithic.data.media.StoredImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal data class CoverPreviewSize(val width: Int, val height: Int) {
    init { require(width in 1..1600 && height in 1..1600) }
}

internal sealed interface CoverPreviewResult {
    data object Failed : CoverPreviewResult
    data class Ready(val bitmap: Bitmap) : CoverPreviewResult
}

/** Identity is the live facade, never a path or an asset UUID reused by another generation. */
internal class CoverRepositoryIdentity(private val repository: MediaRepository?) {
    override fun equals(other: Any?): Boolean = other is CoverRepositoryIdentity && repository === other.repository
    override fun hashCode(): Int = System.identityHashCode(repository)
}

/**
 * UI lifetime only. The semaphore bounds verified reads plus decoding; it is not a storage lock.
 * The LRU owns at most 8 MiB of pixels. Visible Compose images can retain evicted pixels longer.
 */
internal class CoverPreviewLoader(
    internal val repository: MediaRepository,
    private val decode: suspend (StoredImage, CoverPreviewSize) -> Bitmap? = ::decodeCoverPreview,
) {
    private data class Key(val repository: CoverRepositoryIdentity, val assetId: String, val size: CoverPreviewSize)
    private data class Cached(val asset: LocalMediaAssetEntity, val bitmap: Bitmap, val bytes: Long)
    internal data class CacheUsage(val bytes: Long, val entries: Int)

    private val identity = CoverRepositoryIdentity(repository)
    private val owner = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + owner)
    private val permits = Semaphore(2)
    // This monitor protects only the in-memory LRU; openAsset owns its existing storage protocol.
    private val cacheLock = Any()
    private val cache = LinkedHashMap<Key, Cached>(16, 0.75f, true)
    private var cacheBytes = 0L
    @Volatile private var closed = false

    suspend fun load(assetId: String, size: CoverPreviewSize): CoverPreviewResult {
        checkActive()
        val request = scope.async {
            permits.withPermit {
                checkActive()
                loadVerified(assetId, size)
            }
        }
        return try { request.await() }
        finally { if (!request.isCompleted) request.cancel() }
    }

    private suspend fun loadVerified(assetId: String, size: CoverPreviewSize): CoverPreviewResult = try {
        // Every composition entry validates current generation bytes, including an LRU hit.
        val opened = repository.openAsset(assetId)
        currentCoroutineContext().ensureActive()
        checkActive()
        if (opened !is MediaResult.Success || !validBounds(opened.value)) {
            failed(assetId)
        } else {
            val stored = opened.value
            val key = Key(identity, assetId, size)
            val previous = synchronized(cacheLock) { cache[key] }
            if (previous != null && previous.asset == stored.asset && !previous.bitmap.isRecycled) {
                currentCoroutineContext().ensureActive()
                checkActive()
                CoverPreviewResult.Ready(previous.bitmap)
            } else {
                if (previous != null) invalidate(assetId)
                val bitmap = decode(stored, size)
                currentCoroutineContext().ensureActive()
                checkActive()
                if (bitmap == null || bitmap.isRecycled || bitmap.width !in 1..1600 || bitmap.height !in 1..1600) {
                    failed(assetId)
                } else {
                    cache(key, stored.asset, bitmap)
                    CoverPreviewResult.Ready(bitmap)
                }
            }
        }
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { failed(assetId) }

    private fun validBounds(stored: StoredImage): Boolean {
        val asset = stored.asset
        if (stored.bytes.size.toLong() != asset.byteLength || asset.width !in 1..1600 || asset.height !in 1..1600) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(stored.bytes, 0, stored.bytes.size, bounds)
        return bounds.outWidth == asset.width && bounds.outHeight == asset.height && bounds.outMimeType == asset.mime
    }

    private fun failed(assetId: String): CoverPreviewResult.Failed {
        invalidate(assetId)
        return CoverPreviewResult.Failed
    }

    private fun invalidate(assetId: String) = synchronized(cacheLock) {
        val entries = cache.entries.iterator()
        while (entries.hasNext()) {
            val entry = entries.next()
            if (entry.key.assetId == assetId) {
                cacheBytes -= entry.value.bytes
                entries.remove()
            }
        }
    }

    private fun cache(key: Key, asset: LocalMediaAssetEntity, bitmap: Bitmap) = synchronized(cacheLock) {
        if (closed) return@synchronized
        cache.remove(key)?.let { cacheBytes -= it.bytes }
        val bytes = bitmap.allocationByteCount.toLong()
        if (bytes > MAX_CACHE_BYTES) return@synchronized
        val entries = cache.entries.iterator()
        while (cacheBytes + bytes > MAX_CACHE_BYTES && entries.hasNext()) {
            val entry = entries.next()
            cacheBytes -= entry.value.bytes
            entries.remove()
        }
        cache[key] = Cached(asset, bitmap, bytes)
        cacheBytes += bytes
    }

    internal fun cacheUsage(): CacheUsage = synchronized(cacheLock) { CacheUsage(cacheBytes, cache.size) }

    fun close() {
        synchronized(cacheLock) {
            closed = true
            cache.clear()
            cacheBytes = 0
        }
        owner.cancel()
        // Compose may still display an evicted image: never recycle a displayed Bitmap.
    }

    private fun checkActive() { if (closed) throw CancellationException("The cover preview lifecycle has ended") }

    companion object { internal const val MAX_CACHE_BYTES = 8L * 1024 * 1024 }
}

/** Decode only verified bytes and retain enough physical pixels for the requested display. */
internal fun decodeCoverPreview(stored: StoredImage, target: CoverPreviewSize): Bitmap? {
    var sample = 1
    while (stored.asset.width / (sample * 2) >= target.width && stored.asset.height / (sample * 2) >= target.height) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeByteArray(stored.bytes, 0, stored.bytes.size, options)
}
