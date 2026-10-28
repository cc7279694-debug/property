package com.cooper.lithic.feature.media

import com.cooper.lithic.data.media.CoverReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal data class CoverPreviewTarget(val width: Int, val height: Int) {
    init { require(width in 1..1024 && height in 1..1024) }
}

internal fun coverPreviewTarget(width: Int, height: Int): CoverPreviewTarget? =
    if (width <= 0 || height <= 0) null else CoverPreviewTarget(width.coerceAtMost(1024), height.coerceAtMost(1024))

internal data class CoverSamplePlan(val sampleSize: Int, val minimumWidth: Int, val minimumHeight: Int,
    val maxWidth: Int, val maxHeight: Int, val reservedBytes: Long)

/** Uses stored, already validated bounds before any native allocation; all pixel arithmetic is Long. */
internal fun coverSamplePlan(width: Int, height: Int, target: CoverPreviewTarget): CoverSamplePlan {
    require(width in 1..1600 && height in 1..1600 && width.toLong() * height <= 2_560_000L)
    fun ceiling(value: Int, sample: Int) = ((value.toLong() + sample - 1) / sample).toInt()
    var sample = 1
    while (ceiling(width, sample) > target.width || ceiling(height, sample) > target.height) sample *= 2
    val maxWidth = ceiling(width, sample)
    val maxHeight = ceiling(height, sample)
    return CoverSamplePlan(sample, (width / sample).coerceAtLeast(1), (height / sample).coerceAtLeast(1),
        maxWidth, maxHeight, maxWidth.toLong() * maxHeight * 4L)
}

/** Accounting for active/pending raster payload only, not a claim about GC, codec, GPU or process RSS. */
internal class CoverPixelBudget(private val limitBytes: Long) {
    private val used = AtomicLong(0)
    init { require(limitBytes > 0) }
    fun reserve(bytes: Long): CoverPixelReservation? {
        if (bytes <= 0 || bytes > limitBytes) return null
        while (true) {
            val before = used.get()
            if (before > limitBytes - bytes) return null
            if (used.compareAndSet(before, before + bytes)) return CoverPixelReservation(bytes) { used.addAndGet(-bytes) }
        }
    }
}

internal class CoverPixelReservation(val bytes: Long, private val release: () -> Unit) : AutoCloseable {
    private val released = AtomicBoolean(false)
    override fun close() { if (released.compareAndSet(false, true)) release() }
}

/** Disposal revokes delivery and releases only accounting; a published bitmap must not be recycled while Canvas may use it. */
internal class CoverPreviewTicket(private val generationId: String, private val ownerKey: Any,
    private val reference: CoverReference, private val target: CoverPreviewTarget?) {
    private var active = true
    private var attached: CoverPixelReservation? = null
    @Synchronized fun matches(generation: String, owner: Any, ref: CoverReference, size: CoverPreviewTarget?, current: Boolean) =
        active && current && generationId == generation && ownerKey === owner && reference == ref && target == size
    @Synchronized fun attach(reservation: CoverPixelReservation): Boolean {
        if (!active || attached != null) return false
        attached = reservation
        return true
    }
    fun retire() {
        val prior = synchronized(this) { active = false; attached.also { attached = null } }
        prior?.close()
    }
}

/** One slot across every generation/reader, held until the actual read/decode body returns even on cancellation. */
internal object CoverPreviewPipeline {
    private val slot = Semaphore(1)
    val pixels = CoverPixelBudget(16L * 1024 * 1024)
    suspend fun <T> serialized(block: suspend () -> T): T = slot.withPermit { block() }
}
