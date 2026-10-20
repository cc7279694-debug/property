package com.cooper.lithic.data.backup

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.zip.CRC32
import java.util.zip.Inflater

internal class BackupMediaException(message: String, val notCertified: Boolean = false) : IOException(message)

/** Bounded read-only PNG container checks, derived from the M10 r4159682 PNG checks.
 * Uses the platform zlib implementation; never implements an image/entropy decoder.
 * Only the noninterlaced 8-bit RGB/RGBA stored encoding can currently be certified here.
 */
internal object BackupPngInspection {
    fun inspect(file: File): Pair<Int, Int> {
        if (file.length() !in 1 until BackupLimits.MAX_MEDIA_BYTES) invalid()
        return RandomAccessFile(file, "r").use { input ->
            val signature = ByteArray(8).also { input.readFully(it) }
            if (!signature.contentEquals(byteArrayOf(137.toByte(),80,78,71,13,10,26,10))) invalid()
            var dimensions: Pair<Int,Int>? = null
            var channels = 0
            var dataStarted = false
            var dataEnded = false
            val compressed = ByteArrayOutputStream()
            while (input.filePointer < input.length()) {
                if (input.length() - input.filePointer < 12) invalid()
                val length = input.readInt().toLong() and 0xffffffffL
                if (length > input.length()-input.filePointer-8 || length >= BackupLimits.MAX_MEDIA_BYTES) invalid()
                val type = ByteArray(4).also { input.readFully(it) }
                if (type.any { it.toInt() !in 65..90 && it.toInt() !in 97..122 }) invalid()
                val name = type.toString(Charsets.US_ASCII)
                val bytes = ByteArray(length.toInt()).also { input.readFully(it) }
                val crc = input.readInt().toLong() and 0xffffffffL
                if (CRC32().apply { update(type); update(bytes) }.value != crc) invalid()
                if (dimensions == null && name != "IHDR") invalid()
                if (dataStarted && name != "IDAT") dataEnded = true
                when (name) {
                    "IHDR" -> {
                        if (dimensions != null || bytes.size != 13) invalid()
                        fun int(at:Int) = (0..3).fold(0) { n,i -> (n shl 8) or (bytes[at+i].toInt() and 255) }
                        val width = int(0); val height = int(4)
                        if (width !in 1..1600 || height !in 1..1600 || width.toLong()*height > 40_000_000) invalid()
                        channels = when (bytes[9].toInt()) { 2 -> 3; 6 -> 4; else -> 0 }
                        if (bytes[8].toInt()!=8 || channels==0 || bytes[10].toInt()!=0 || bytes[11].toInt()!=0 || bytes[12].toInt()!=0)
                            throw BackupMediaException("PNG encoding does not have a certified backup validator", true)
                        dimensions = width to height
                    }
                    "IDAT" -> { if (dataEnded) invalid(); dataStarted=true; compressed.write(bytes) }
                    "IEND" -> {
                        if (bytes.isNotEmpty() || !dataStarted || input.filePointer != input.length()) invalid()
                        val size = requireNotNull(dimensions)
                        verifyZlib(compressed.toByteArray(), size.first, size.second, channels)
                        return@use size
                    }
                    "acTL", "fcTL", "fdAT", "eXIf" -> throw BackupMediaException("Image encoding cannot be certified", true)
                    else -> if (type[0].toInt() in 65..90 && name != "PLTE") invalid()
                }
            }
            invalid()
        }
    }

    private fun verifyZlib(data: ByteArray, width:Int, height:Int, channels:Int) {
        val row = Math.addExact(Math.multiplyExact(width, channels), 1)
        val expected = Math.multiplyExact(row, height)
        val inflater = Inflater()
        try {
            inflater.setInput(data)
            val buffer = ByteArray(64*1024)
            var count = 0
            while (!inflater.finished()) {
                val n = inflater.inflate(buffer)
                if (n==0 && !inflater.finished()) invalid()
                if (count > expected-n) invalid()
                for (i in 0 until n) if ((count+i)%row==0 && buffer[i].toInt() !in 0..4) invalid()
                count += n
            }
            if (count != expected || inflater.remaining != 0) invalid()
        } finally { inflater.end() }
    }
    private fun invalid(): Nothing = throw BackupMediaException("PNG bytes are incomplete or inconsistent")
}
