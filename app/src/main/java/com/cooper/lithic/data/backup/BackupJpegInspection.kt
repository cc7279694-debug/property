package com.cooper.lithic.data.backup

import java.io.File

/** Container validation for an already registered JPEG whose original bytes match its stored SHA.
 * Reads every scan byte and validates marker boundaries, but does NOT decode entropy or certify
 * strict JPEG completeness. BitmapFactory is a separate bounded readability/dimension cross-check.
 * This is backup validation, not the M10 strict-import validator or a replacement for it.
 */
internal object BackupJpegInspection {
    fun inspect(file: File, checkpoint: () -> Unit = {}): Pair<Int, Int> {
        if (file.length() !in 4 until BackupLimits.MAX_MEDIA_BYTES) invalid()
        val bytes = file.readBytes()
        if (bytes.size.toLong() != file.length()) invalid()
        var position = 0
        fun byte(): Int {
            if (position >= bytes.size) invalid()
            return bytes[position++].toInt() and 255
        }
        fun marker(): Int {
            if (byte() != 255) invalid()
            var code = byte()
            while (code == 255) code = byte()
            if (code == 0) invalid()
            return code
        }
        if (byte() != 255 || byte() != 0xd8) invalid()
        var frame: Frame? = null
        val quantization = BooleanArray(4)
        val dc = BooleanArray(4)
        val ac = BooleanArray(4)
        val seen = mutableSetOf<Int>()
        var scans = 0
        var restartInterval = 0
        var entropy = false
        while (position < bytes.size) {
            checkpoint()
            val code = if (entropy) {
                var consumed = false
                var restart = 0
                var found = -1
                while (position < bytes.size && found < 0) {
                    if (position % 65536 == 0) checkpoint()
                    val next = byte()
                    if (next != 255) { consumed = true; continue }
                    var following = byte()
                    while (following == 255) following = byte()
                    when (following) {
                        0 -> consumed = true // stuffed literal FF, not a marker
                        in 0xd0..0xd7 -> {
                            if (!consumed || restartInterval == 0 || following != 0xd0 + restart) invalid()
                            restart = (restart + 1) and 7
                        }
                        else -> found = following
                    }
                }
                if (!consumed || found < 0) invalid()
                entropy = false
                found
            } else marker()
            when (code) {
                0xd9 -> {
                    val complete = frame ?: invalid()
                    if (position != bytes.size || scans == 0 || seen != complete.components.keys) invalid()
                    return complete.width to complete.height
                }
                0xd8, 0x00, in 0xd0..0xd7 -> invalid()
                0x01 -> invalid() // TEM/arithmetic coding is not a supported stored cover encoding.
            }
            val length = (byte() shl 8) or byte()
            if (length < 2 || length - 2 > bytes.size - position) invalid()
            val end = position + length - 2
            fun segmentByte(): Int { if (position >= end) invalid(); return byte() }
            fun segmentWord() = (segmentByte() shl 8) or segmentByte()
            when (code) {
                0xc0, 0xc1, 0xc2 -> {
                    if (frame != null) invalid()
                    val precision = segmentByte()
                    val height = segmentWord(); val width = segmentWord(); val count = segmentByte()
                    if (precision != 8) unsupported()
                    if (width !in 1..1600 || height !in 1..1600 || count !in 1..4 || length != 8 + 3 * count) invalid()
                    val components = mutableMapOf<Int, Int>()
                    repeat(count) {
                        val id = segmentByte(); val sampling = segmentByte(); val table = segmentByte()
                        if (id in components || sampling shr 4 !in 1..4 || sampling and 15 !in 1..4 || table !in 0..3) invalid()
                        components[id] = table
                    }
                    frame = Frame(width, height, code == 0xc2, components)
                }
                0xdb -> {
                    if (position == end) invalid()
                    while (position < end) {
                        val info = segmentByte(); val precision = info shr 4; val id = info and 15
                        if (precision !in 0..1 || id !in 0..3) invalid()
                        repeat(64) { if ((if (precision == 0) segmentByte() else segmentWord()) == 0) invalid() }
                        quantization[id] = true
                    }
                }
                0xc4 -> {
                    if (position == end) invalid()
                    while (position < end) {
                        val info = segmentByte(); val kind = info shr 4; val id = info and 15
                        if (kind !in 0..1 || id !in 0..3) invalid()
                        var symbols = 0; var available = 1
                        repeat(16) {
                            val count = segmentByte()
                            symbols += count; available = (available shl 1) - count
                            if (available < 0) invalid()
                        }
                        if (symbols !in 1..256) invalid()
                        repeat(symbols) { val symbol = segmentByte(); if (kind == 0 && symbol > 15) invalid() }
                        (if (kind == 0) dc else ac)[id] = true
                    }
                }
                0xdd -> { if (length != 4) invalid(); restartInterval = segmentWord() }
                0xda -> {
                    val current = frame ?: invalid()
                    val count = segmentByte()
                    if (count !in 1..current.components.size || length != 6 + 2 * count) invalid()
                    val selectors = mutableMapOf<Int, Int>()
                    repeat(count) {
                        val id = segmentByte(); val tables = segmentByte()
                        if (id !in current.components || id in selectors || tables shr 4 !in 0..3 || tables and 15 !in 0..3) invalid()
                        if (!quantization[current.components.getValue(id)]) invalid()
                        selectors[id] = tables
                    }
                    val first = segmentByte(); val last = segmentByte(); val approximation = segmentByte()
                    if (current.progressive) {
                        if (first !in 0..63 || last !in first..63 || (first == 0 && last != 0) || (first != 0 && count != 1)) invalid()
                        val high = approximation shr 4; val low = approximation and 15
                        if (high > 13 || low > 13 || (high != 0 && high != low + 1)) invalid()
                        for ((id, tables) in selectors) {
                            if (first == 0 && high == 0) { if (!dc[tables shr 4]) invalid(); seen += id }
                            if (first > 0 && !ac[tables and 15]) invalid()
                        }
                    } else {
                        if (first != 0 || last != 63 || approximation != 0) invalid()
                        for ((id, tables) in selectors) {
                            if (!dc[tables shr 4] || !ac[tables and 15] || id in seen) invalid()
                            seen += id
                        }
                    }
                    scans++; entropy = true
                }
                in 0xe0..0xef, 0xfe -> position = end // Preserve APP/EXIF/ICC/COM bytes as-is.
                else -> unsupported()
            }
            if (position != end) invalid()
        }
        invalid()
    }
    private data class Frame(val width: Int, val height: Int, val progressive: Boolean, val components: Map<Int, Int>)
    private fun unsupported(): Nothing = throw BackupMediaException("Stored JPEG coding is not supported for this backup", true)
    private fun invalid(): Nothing = throw BackupMediaException("JPEG container bytes are incomplete or inconsistent")
}
