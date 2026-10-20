package com.cooper.lithic.data.backup

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.zip.CRC32
import java.util.zip.DataFormatException
import java.util.zip.Inflater

internal class BackupMediaException(message: String, val notCertified: Boolean = false) : IOException(message)

/** Read-only validation of stored static PNG containers, including all standard color/depth pairs
 * and Adam7 passes. Standard zlib verifies the compressed stream/checksum; no pixel decoder or
 * re-encoder is implemented here. Ancillary metadata, including EXIF, is copied without rewriting.
 */
internal object BackupPngInspection {
    fun inspect(file: File, checkpoint: () -> Unit = {}): Pair<Int, Int> {
        if (file.length() !in 8 until BackupLimits.MAX_MEDIA_BYTES) invalid()
        return RandomAccessFile(file, "r").use { input ->
            val signature = ByteArray(8).also { input.readFully(it) }
            if (!signature.contentEquals(byteArrayOf(137.toByte(),80,78,71,13,10,26,10))) invalid()
            var header: Header? = null
            var paletteEntries: Int? = null
            var transparency = false
            var dataStarted = false
            var dataEnded = false
            val compressed = ByteArrayOutputStream()
            while (input.filePointer < input.length()) {
                checkpoint()
                if (input.length() - input.filePointer < 12) invalid()
                val length = input.readInt().toLong() and 0xffffffffL
                if (length > input.length()-input.filePointer-8 || length >= BackupLimits.MAX_MEDIA_BYTES) invalid()
                val type = ByteArray(4).also { input.readFully(it) }
                if (type.any { it.toInt() !in 65..90 && it.toInt() !in 97..122 } || type[2].toInt() !in 65..90) invalid()
                val name = type.toString(Charsets.US_ASCII)
                val bytes = ByteArray(length.toInt()).also { input.readFully(it) }
                val crc = input.readInt().toLong() and 0xffffffffL
                if (CRC32().apply { update(type); update(bytes) }.value != crc) invalid()
                if (header == null && name != "IHDR") invalid()
                if (dataStarted && name != "IDAT") dataEnded = true
                when (name) {
                    "IHDR" -> {
                        if (header != null || bytes.size != 13) invalid()
                        fun int(at:Int) = (0..3).fold(0) { n,i -> (n shl 8) or (bytes[at+i].toInt() and 255) }
                        val width = int(0); val height = int(4)
                        val depth = bytes[8].toInt() and 255
                        val color = bytes[9].toInt() and 255
                        val channels = when (color) { 0,3 -> 1; 2 -> 3; 4 -> 2; 6 -> 4; else -> invalid() }
                        val depths = when (color) { 0 -> setOf(1,2,4,8,16);3 -> setOf(1,2,4,8);else -> setOf(8,16) }
                        val interlace = bytes[12].toInt() and 255
                        if (width !in 1..1600 || height !in 1..1600 || width.toLong()*height > 40_000_000 ||
                            depth !in depths || bytes[10].toInt()!=0 || bytes[11].toInt()!=0 || interlace !in 0..1) invalid()
                        header = Header(width,height,depth,color,channels,interlace)
                    }
                    "PLTE" -> {
                        val image = header ?: invalid()
                        if (paletteEntries != null || dataStarted || image.color in setOf(0,4) ||
                            bytes.isEmpty() || bytes.size % 3 != 0 || bytes.size > 768) invalid()
                        val count = bytes.size/3
                        if (image.color == 3 && count > (1 shl image.depth)) invalid()
                        paletteEntries = count
                    }
                    "tRNS" -> {
                        val image = header ?: invalid()
                        if (transparency || dataStarted) invalid()
                        fun sample(at:Int) = ((bytes[at].toInt() and 255) shl 8) or (bytes[at+1].toInt() and 255)
                        when (image.color) {
                            0 -> if (bytes.size != 2 || sample(0) >= (1 shl image.depth)) invalid()
                            2 -> if (bytes.size != 6 || (0..2).any { sample(it*2) >= (1 shl image.depth) }) invalid()
                            3 -> if (paletteEntries == null || bytes.isEmpty() || bytes.size > paletteEntries) invalid()
                            else -> invalid()
                        }
                        transparency = true
                    }
                    "IDAT" -> {
                        val image = header ?: invalid()
                        if (dataEnded || (image.color == 3 && paletteEntries == null)) invalid()
                        dataStarted=true; compressed.write(bytes)
                    }
                    "IEND" -> {
                        val image = header ?: invalid()
                        if (bytes.isNotEmpty() || !dataStarted || input.filePointer != input.length()) invalid()
                        verifyZlib(compressed.toByteArray(), image, checkpoint)
                        return@use image.width to image.height
                    }
                    "acTL", "fcTL", "fdAT" -> throw BackupMediaException("Animated PNG is not a supported static stored cover", true)
                    else -> if (type[0].toInt() in 65..90) invalid() // Unknown critical chunk.
                }
            }
            invalid()
        }
    }

    private data class Header(val width:Int,val height:Int,val depth:Int,val color:Int,val channels:Int,val interlace:Int)
    private fun verifyZlib(data: ByteArray, image:Header, checkpoint:()->Unit) {
        // Empty Adam7 passes have no rows or filter bytes. Packed low-bit-depth rows round up.
        val passes = if (image.interlace == 0) listOf(intArrayOf(0,0,1,1)) else listOf(
            intArrayOf(0,0,8,8), intArrayOf(4,0,8,8), intArrayOf(0,4,4,8), intArrayOf(2,0,4,4),
            intArrayOf(0,2,2,4), intArrayOf(1,0,2,2), intArrayOf(0,1,1,2))
        val rows = mutableListOf<Int>()
        var expected = 0
        for (pass in passes) {
            val width = if (image.width <= pass[0]) 0 else (image.width-pass[0]+pass[2]-1)/pass[2]
            val height = if (image.height <= pass[1]) 0 else (image.height-pass[1]+pass[3]-1)/pass[3]
            if (width == 0 || height == 0) continue
            val bits = Math.multiplyExact(Math.multiplyExact(width,image.channels),image.depth)
            val stride = Math.addExact(Math.addExact(bits,7)/8,1)
            expected = Math.addExact(expected, Math.multiplyExact(stride,height))
            repeat(height) { rows += stride }
        }
        val inflater = Inflater()
        try {
            inflater.setInput(data)
            val buffer = ByteArray(64*1024)
            var count = 0; var row = 0; var offset = 0
            while (!inflater.finished()) {
                checkpoint()
                val n = inflater.inflate(buffer)
                if (n==0 && !inflater.finished()) invalid()
                if (count > expected-n) invalid()
                for (i in 0 until n) {
                    if (row >= rows.size) invalid()
                    if (offset == 0 && buffer[i].toInt() !in 0..4) invalid()
                    offset++
                    if (offset == rows[row]) { row++;offset=0 }
                }
                count += n
            }
            if (count != expected || row != rows.size || offset != 0 || inflater.remaining != 0) invalid()
        } catch (failure:DataFormatException) {
            throw BackupMediaException("PNG compressed bytes are invalid").apply { initCause(failure) }
        } finally { inflater.end() }
    }
    private fun invalid(): Nothing = throw BackupMediaException("PNG bytes are incomplete or inconsistent")
}
