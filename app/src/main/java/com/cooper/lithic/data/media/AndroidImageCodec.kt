package com.cooper.lithic.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.io.EOFException
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.zip.CRC32
import kotlin.math.max

internal class ImageStoreException(val kind: MediaFailureKind, message: String) : IOException(message)
internal data class ImageContainer(val mime: String, val width: Int, val height: Int)

/** All scanning is bounded by the private source cap, before a pixel allocation. */

internal object AndroidImageCodec {
    const val SOURCE_LIMIT = 64L * 1024L * 1024L
    const val STORED_LIMIT = 2_097_152L
    const val PIXEL_LIMIT = 40_000_000L
    const val EDGE_LIMIT = 1600
    private val pngSignature = byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)

    fun inspect(file: File, stored: Boolean = false): ImageContainer {
        val length = file.length()
        if (length <= 0) invalid("The selected image is empty")
        if (length > if (stored) STORED_LIMIT else SOURCE_LIMIT) tooLarge("The image exceeds the safe file size")
        return RandomAccessFile(file, "r").use { input ->
            val prefix = ByteArray(minOf(8L, length).toInt())
            input.readFully(prefix)
            input.seek(0)
            val container = try {
                when {
                    prefix.contentEquals(pngSignature) -> png(input)
                    prefix.size >= 2 && prefix[0] == 0xff.toByte() && prefix[1] == 0xd8.toByte() -> jpeg(input)
                    else -> throw ImageStoreException(MediaFailureKind.UNSUPPORTED, "Choose a static JPEG or PNG image")
                }
            } catch (_: EOFException) {
                invalid("The image container is truncated")
            }
            checkPixels(container.width, container.height)
            if (stored && max(container.width, container.height)>EDGE_LIMIT) tooLarge("The stored cover dimensions exceed the safe limit")
            container
        }
    }

    fun normalize(source: File, work: File, declaredMime: String?, faults: ImageStoreFaults): ImageContainer {
        val info = inspect(source)
        if (declaredMime != null && declaredMime != info.mime) invalid("The declared image type does not match its content")
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inScaled = false
        }
        BitmapFactory.decodeFile(source.path, bounds)
        if (bounds.outWidth != info.width || bounds.outHeight != info.height || bounds.outMimeType != info.mime) invalid("The image header cannot be decoded consistently")
        checkPixels(bounds.outWidth, bounds.outHeight)
        var sampling = 1
        while ((max(info.width, info.height).toLong() + sampling - 1L) / sampling > EDGE_LIMIT) sampling *= 2
        faults.at(ImageStoreStage.BEFORE_DECODE)
        val decoded = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply {
                inSampleSize = sampling
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }) ?: invalid("The image cannot be safely decoded")
        if (decoded.width <= 0 || decoded.height <= 0 || max(decoded.width, decoded.height)>EDGE_LIMIT) {
            decoded.recycle()
            invalid("The decoder exceeded the sampled dimension limit")
        }
        var oriented: Bitmap? = null
        try {
            val orientation = if (info.mime == "image/jpeg") ExifInterface(source.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) else 1
            oriented = orient(decoded, orientation)
            val format = if (info.mime == "image/jpeg") Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
            // Three finite dimensions; JPEG also has three finite quality attempts at each size.
            for (scale in doubleArrayOf(1.0, 0.8, 0.64)) {
                val width = max(1, (oriented.width * scale).toInt())
                val height = max(1, (oriented.height * scale).toInt())
                val outputBitmap = if (width == oriented.width && height == oriented.height) oriented else Bitmap.createScaledBitmap(oriented, width, height, true)
                try {
                    val qualities = if (format == Bitmap.CompressFormat.JPEG) intArrayOf(90, 80, 65) else intArrayOf(100)
                    for (quality in qualities) {
                        faults.at(ImageStoreStage.BEFORE_ENCODE)
                        try {
                            if (Files.isSymbolicLink(work.toPath()))throw ImageStoreException(MediaFailureKind.PATH, "A linked image preparation cannot be written")
                            FileChannel.open(work.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { channel ->
                                val limited = LimitedImageOutput(Channels.newOutputStream(channel))
                                if (!outputBitmap.compress(format, quality, limited)) {
                                    if (limited.exceeded)throw EncodedLimitExceeded()
                                    invalid("The image could not be encoded")
                                }
                                limited.flush()
                                channel.force(true)
                            }
                            val final = inspect(work, true)
                            if (final.width != outputBitmap.width || final.height != outputBitmap.height || final.mime != info.mime) invalid("The normalized image failed final verification")
                            return final
                        } catch (_: EncodedLimitExceeded) {
                            if (work.exists() && !work.delete()) throw IOException("Cannot reset the oversized private image")
                        }
                    }
                } finally {
                if (outputBitmap !== oriented) outputBitmap.recycle()
                }
            }
            tooLarge("The image cannot fit the 2 MiB cover limit safely")
        } finally {
            if (oriented != null && oriented !== decoded) oriented.recycle()
            decoded.recycle()
        }
    }

    fun verifyDecode(file: File, info: ImageContainer) {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inScaled = false
        }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth != info.width || bounds.outHeight != info.height || bounds.outMimeType != info.mime) invalid("The saved image metadata does not match its bytes")
        val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }) ?: invalid("The saved image cannot be decoded")
        try {
            if (bitmap.width != info.width || bitmap.height != info.height) invalid("The saved image dimensions do not match")
        }
        finally {
            bitmap.recycle()
        }
    }

    private fun orient(bitmap: Bitmap, orientation: Int): Bitmap {
        if (orientation == 1 || orientation == 0) return bitmap
        val matrix = Matrix()
        when (orientation) {
            2 -> matrix.setScale(-1f, 1f)
            3 -> matrix.setRotate(180f)
            4 -> matrix.setScale(1f, -1f)
            5 -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            6 -> matrix.setRotate(90f)
            7 -> {
                matrix.setRotate(90f)
                matrix.postScale(1f, -1f)
            }
            8 -> matrix.setRotate(270f)
            else -> invalid("The image direction metadata is invalid")
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun checkPixels(width: Int, height: Int) {
        if (width <= 0 || height <= 0) invalid("Image dimensions must be positive")
        if (width.toLong()>PIXEL_LIMIT / height.toLong()) tooLarge("Choose an image of at most 40 megapixels")
    }

    private fun png(input: RandomAccessFile): ImageContainer {
        input.seek(8)
        var info: ImageContainer? = null
        var data = false
        var ended = false
        var dataEnded = false
        val scratch = ByteArray(64 * 1024)
        while (input.filePointer<input.length()) {
            if (input.length() - input.filePointer<12) invalid("The PNG is truncated")
            val length = input.readInt().toLong() and 0xffffffffL
            val tag = ByteArray(4)
            input.readFully(tag)
            val name = String(tag, Charsets.US_ASCII)
            if (tag.any {
                    it.toInt() !in 65..90 && it.toInt() !in 97..122
                }) invalid("The PNG chunk type is invalid")
            if (length>input.length() - input.filePointer - 4 || length>SOURCE_LIMIT) invalid("The PNG chunk is truncated")
            if (info == null && name != "IHDR") invalid("The PNG does not start with dimensions")
            if (name in setOf("acTL", "fcTL", "fdAT")) throw ImageStoreException(MediaFailureKind.UNSUPPORTED, "Animated PNG covers are not supported")
            if (name == "eXIf") throw ImageStoreException(MediaFailureKind.UNSUPPORTED, "PNG direction metadata is not supported; choose a JPEG or normalized PNG")
            val crc = CRC32().apply {
                update(tag)
            }
            if (name == "IHDR") {
                if (info != null || length != 13L) invalid("The PNG dimensions are invalid")
                val bytes = ByteArray(13)
                input.readFully(bytes)
                crc.update(bytes)
                val width = readInt(bytes, 0)
                val height = readInt(bytes, 4)
                checkPixels(width, height)
                val depth = bytes[8].toInt() and 255
                val color = bytes[9].toInt() and 255
                val supported = when (color) {
                    0 -> depth in setOf(1, 2, 4, 8, 16)
                    2, 4, 6 -> depth in setOf(8, 16)
                    3 -> depth in setOf(1, 2, 4, 8)
                    else -> false
                }
                if (!supported || bytes[10].toInt() != 0 || bytes[11].toInt() != 0 || bytes[12].toInt() !in 0..1) throw ImageStoreException(MediaFailureKind.UNSUPPORTED, "The PNG encoding is not supported")
                info = ImageContainer("image/png", width, height)
            } else {
                if (name == "IDAT") {
                    if (dataEnded) invalid("The PNG image data is disordered")
                    data = true
                }
                else if (data) dataEnded = true
                if (name == "IEND") {
                    if (length != 0L || !data) invalid("The PNG end marker is invalid")
                    ended = true
                }
                if (name != "IDAT" && name != "IEND" && name != "PLTE" && name[0].isUpperCase()) throw ImageStoreException(MediaFailureKind.UNSUPPORTED, "The PNG contains an unsupported critical chunk")
                var remaining = length
                while (remaining>0) {
                    val count = minOf(remaining, scratch.size.toLong()).toInt()
                    input.readFully(scratch, 0, count)
                    crc.update(scratch, 0, count)
                    remaining -= count
                }
            }
            val expected = input.readInt().toLong() and 0xffffffffL
            if (crc.value != expected) invalid("The PNG checksum is damaged")
            if (ended) {
                if (input.filePointer != input.length()) invalid("The PNG has unexpected trailing content")
                break
            }
        }
        if (!ended || info == null || !data) invalid("The PNG is incomplete")
        return info
    }

    private fun jpeg(input: RandomAccessFile): ImageContainer {
        if (input.readUnsignedShort() != 0xffd8) invalid("The JPEG signature is invalid")
        var info: ImageContainer? = null
        var sawScan = false
        while (input.filePointer<input.length()) {
            if (input.readUnsignedByte() != 255) invalid("The JPEG marker is invalid")
            var marker = input.readUnsignedByte()
            while (marker == 255)marker = input.readUnsignedByte()
            if (marker == 0xd9) {
                if (info == null || !sawScan || input.filePointer != input.length()) invalid("The JPEG is incomplete or has trailing content")
                return info
            }
            if (marker == 0xd8 || marker == 0 || marker in 0xd0..0xd7) invalid("The JPEG contains unexpected frame content")
            if (marker == 1) continue
            if (input.length() - input.filePointer<2) invalid("The JPEG is truncated")
            val segmentLength = input.readUnsignedShort()
            val end = input.filePointer + segmentLength - 2L
            if (segmentLength<2 || end>input.length()) invalid("The JPEG segment is truncated")
            if (marker == 0xe2 && segmentLength >= 6) {
                val prefix = ByteArray(4)
                input.readFully(prefix)
                if (prefix.contentEquals(byteArrayOf(77, 80, 70, 0))) throw ImageStoreException(MediaFailureKind.UNSUPPORTED, "Multi-picture JPEG covers are not supported")
            }
            if (marker in setOf(0xc0, 0xc1, 0xc2, 0xc3, 0xc5, 0xc6, 0xc7, 0xc9, 0xca, 0xcb, 0xcd, 0xce, 0xcf)) {
                if (marker != 0xc0 && marker != 0xc2) throw ImageStoreException(MediaFailureKind.UNSUPPORTED, "The JPEG encoding is not supported")
                if (info != null || segmentLength<8) invalid("The JPEG frame is invalid")
                val precision = input.readUnsignedByte()
                val height = input.readUnsignedShort()
                val width = input.readUnsignedShort()
                val components = input.readUnsignedByte()
                if (precision != 8 || components !in 1..3 || segmentLength != 8 + 3 * components) throw ImageStoreException(MediaFailureKind.UNSUPPORTED, "The JPEG color encoding is not supported")
                checkPixels(width, height)
                info = ImageContainer("image/jpeg", width, height)
            }
            input.seek(end)
            if (marker == 0xda) {
                if (info == null || segmentLength<6) invalid("The JPEG scan is invalid")
                sawScan = true
                var found = false
                while (input.filePointer<input.length()) {
                    if (input.readUnsignedByte() != 255)continue
                    var next = input.readUnsignedByte()
                    while (next == 255)next = input.readUnsignedByte()
                    if (next == 0 || next in 0xd0..0xd7)continue
                    input.seek(input.filePointer - 2)
                    found = true
                    break
                }
                if (!found) invalid("The JPEG is missing its final marker")
            }
        }
        invalid("The JPEG is incomplete")
    }

    private fun readInt(bytes: ByteArray, offset: Int): Int = ((bytes[offset].toInt() and 255) shl 24) or ((bytes[offset + 1].toInt() and 255) shl 16) or ((bytes[offset + 2].toInt() and 255) shl 8) or (bytes[offset + 3].toInt() and 255)

    private fun invalid(message: String): Nothing = throw ImageStoreException(MediaFailureKind.INVALID_IMAGE, message)

    private fun tooLarge(message: String): Nothing = throw ImageStoreException(MediaFailureKind.TOO_LARGE, message)
    private class EncodedLimitExceeded: IOException()
    private class LimitedImageOutput(private val destination: OutputStream): OutputStream() {
        private var count = 0L
        var exceeded = false
        private set
        override fun write(value: Int) {
            reserve(1)
            destination.write(value)
        }
        override fun write(bytes: ByteArray, offset: Int, length: Int) {
            reserve(length)
            destination.write(bytes, offset, length)
        }
        override fun flush() = destination.flush()

        private fun reserve(length: Int) {
            if (length<0 || count>STORED_LIMIT - length) {
                exceeded = true
                throw EncodedLimitExceeded()
            }
            count += length
        }
    }
}
