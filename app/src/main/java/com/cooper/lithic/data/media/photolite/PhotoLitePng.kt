package com.cooper.lithic.data.media.photolite

import com.cooper.lithic.data.backup.BackupLimits
import com.cooper.lithic.data.backup.BackupPngInspection
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest

/** Product-output policy only; not certification of the original selected JPEG. */
object PhotoLiteLimits {
    const val REQUEST_EDGE = 512
    const val FINAL_EDGE = 640
    const val MAX_THUMBNAIL_EDGE = 1600
    const val MAX_SOURCE_RASTER_BYTES = 12L * 1024 * 1024
    const val MAX_PNG_BYTES_EXCLUSIVE = 2 * 1024 * 1024

    // Android 29's platform implementation differs in its sampling of extreme aspect ratios.
    // This first disposable experiment admits only the specifically reviewed API36 path.
    fun supports(api: Int, realPhotoPickerAvailable: Boolean): Boolean = api == 36 && realPhotoPickerAvailable

    fun acceptsThumbnail(width: Int, height: Int, allocationBytes: Long, software: Boolean): Boolean =
        software && width in 1..MAX_THUMBNAIL_EDGE && height in 1..MAX_THUMBNAIL_EDGE &&
            width.toLong() * height <= MAX_THUMBNAIL_EDGE.toLong() * MAX_THUMBNAIL_EDGE &&
            allocationBytes in 1..MAX_SOURCE_RASTER_BYTES

    fun normalizedSize(width: Int, height: Int): Pair<Int, Int> {
        require(width in 1..MAX_THUMBNAIL_EDGE && height in 1..MAX_THUMBNAIL_EDGE)
        val longest = maxOf(width, height)
        if (longest <= FINAL_EDGE) return width to height
        return maxOf(1, (width.toLong() * FINAL_EDGE / longest).toInt()) to
            maxOf(1, (height.toLong() * FINAL_EDGE / longest).toInt())
    }
}

/** Immutable verified normalized PNG. No URI, original bytes, path or live descriptor is retained. */
class PhotoLitePng private constructor(
    val width: Int,
    val height: Int,
    private val captured: ByteArray,
    val sha256: String,
) {
    val byteLength: Long get() = captured.size.toLong()
    internal fun copyBytes(): ByteArray = captured.copyOf()

    companion object {
        fun checked(bytes: ByteArray): PhotoLitePng {
            require(bytes.size.toLong() in 8 until BackupLimits.MAX_MEDIA_BYTES)
            require(bytes.size < PhotoLiteLimits.MAX_PNG_BYTES_EXCLUSIVE)
            val copy = bytes.copyOf()
            val dimensions = try { BackupPngInspection.inspect(copy) }
            catch (failure: Exception) { throw IllegalArgumentException("Normalized PNG verification failed", failure) }
            require(dimensions.first in 1..PhotoLiteLimits.FINAL_EDGE && dimensions.second in 1..PhotoLiteLimits.FINAL_EDGE)
            // The unchanged backup validator intentionally preserves existing ancillary metadata.
            // New imports instead accept only pixel/standard-color chunks from the fresh canvas.
            val permitted = setOf("IHDR", "IDAT", "IEND", "sRGB", "gAMA", "cHRM", "pHYs", "sBIT")
            val ancillarySeen = mutableSetOf<String>()
            var channels = 0
            var dataStarted = false
            var at = 8
            while (at < copy.size) {
                require(copy.size - at >= 12)
                val size = (0..3).fold(0L) { value, i -> (value shl 8) or (copy[at + i].toLong() and 255) }
                require(size <= copy.size.toLong() - at - 12)
                val kind = copy.copyOfRange(at + 4, at + 8).toString(Charsets.US_ASCII)
                require(kind in permitted) { "Unexpected metadata in normalized PNG" }
                val data = at + 8
                when (kind) {
                    "IHDR" -> {
                        require(size == 13L && copy[data + 8].toInt() == 8) { "Expected fresh 8-bit PNG pixels" }
                        channels = when (copy[data + 9].toInt()) {
                            2 -> 3
                            6 -> 4
                            else -> throw IllegalArgumentException("Expected fresh RGB or RGBA PNG pixels")
                        }
                    }
                    "IDAT" -> dataStarted = true
                    "IEND" -> Unit
                    else -> {
                        require(!dataStarted && ancillarySeen.add(kind)) { "Repeated or late normalized PNG metadata" }
                        when (kind) {
                            "sBIT" -> {
                                require(size == channels.toLong())
                                require((0 until channels).all { copy[data + it].toInt() == 8 })
                            }
                            "sRGB" -> require(size == 1L && (copy[data].toInt() and 255) in 0..3)
                            "gAMA" -> {
                                require(size == 4L)
                                require(unsigned(copy, data) in 1L..Int.MAX_VALUE.toLong())
                            }
                            "cHRM" -> {
                                require(size == 32L)
                                // PNG four-byte unsigned fields are limited to 2^31-1. Do not
                                // invent narrower display-gamut limits for legal chromaticities.
                                require((0..7).all { unsigned(copy, data + it * 4) <= Int.MAX_VALUE })
                            }
                            "pHYs" -> {
                                require(size == 9L)
                                require(unsigned(copy, data) <= Int.MAX_VALUE && unsigned(copy, data + 4) <= Int.MAX_VALUE)
                                require((copy[data + 8].toInt() and 255) in 0..1)
                            }
                            else -> throw IllegalArgumentException("Unexpected normalized PNG metadata")
                        }
                    }
                }
                at += size.toInt() + 12
            }
            require(at == copy.size)
            val sha = MessageDigest.getInstance("SHA-256").digest(copy)
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            return PhotoLitePng(dimensions.first, dimensions.second, copy, sha)
        }

        private fun unsigned(bytes: ByteArray, at: Int): Long =
            (0..3).fold(0L) { value, i -> (value shl 8) or (bytes[at + i].toLong() and 255) }
    }
}

/** Stop encoding before reaching the exclusive on-disk/backup limit; never truncate a success. */
class PhotoLitePngOutput(private val maxBytesExclusive: Int = PhotoLiteLimits.MAX_PNG_BYTES_EXCLUSIVE) : OutputStream() {
    private val output = ByteArrayOutputStream(minOf(64 * 1024, maxBytesExclusive.coerceAtLeast(1)))
    init { require(maxBytesExclusive in 1..PhotoLiteLimits.MAX_PNG_BYTES_EXCLUSIVE) }
    override fun write(value: Int) {
        requireRoom(1)
        output.write(value)
    }
    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (offset < 0 || length < 0 || offset > bytes.size - length) throw IndexOutOfBoundsException()
        requireRoom(length)
        output.write(bytes, offset, length)
    }
    private fun requireRoom(extra: Int) {
        if (output.size().toLong() + extra >= maxBytesExclusive) throw IOException("Normalized PNG exceeds experiment limit")
    }
    fun toByteArray(): ByteArray = output.toByteArray()
}
