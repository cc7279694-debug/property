package com.cooper.lithic.data.media.photolite

import com.cooper.lithic.data.backup.BackupPngInspection
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import org.junit.Assert.*
import org.junit.Test

/** Source prepared while the user has deferred JVM/Gradle execution. */
class PhotoLitePngTest {
    @Test fun checked_png_matches_the_unchanged_backup_inspector_and_actual_bytes() {
        val bytes = png()
        val image = PhotoLitePng.checked(bytes)
        assertEquals(2, image.width)
        assertEquals(3, image.height)
        assertEquals(bytes.size.toLong(), image.byteLength)
        assertEquals(sha(bytes), image.sha256)
        assertEquals(2 to 3, BackupPngInspection.inspect(image.copyBytes()))
    }

    @Test fun callers_cannot_mutate_prepared_bytes_after_verification() {
        val source = png()
        val expected = source.copyOf()
        val image = PhotoLitePng.checked(source)
        source.fill(0)
        image.copyBytes().fill(0)
        assertArrayEquals(expected, image.copyBytes())
        assertEquals(sha(expected), image.sha256)
    }

    @Test fun empty_corrupt_truncated_and_trailing_bytes_are_rejected() {
        val valid = png()
        val damaged = valid.copyOf().also { it[44] = (it[44].toInt() xor 1).toByte() }
        for (bytes in listOf(byteArrayOf(), damaged, valid.copyOf(valid.size - 1), valid + 0.toByte())) {
            expectRejected { PhotoLitePng.checked(bytes) }
        }
    }

    @Test fun output_at_the_frozen_two_mib_limit_or_over_the_experiment_edge_is_rejected() {
        expectRejected { PhotoLitePng.checked(ByteArray(PhotoLiteLimits.MAX_PNG_BYTES_EXCLUSIVE)) }
        expectRejected { PhotoLitePng.checked(png(width = 641, height = 1)) }
    }

    @Test fun exif_and_text_are_not_normalized_product_output_even_with_valid_chunk_crcs() {
        for (metadata in listOf("eXIf", "tEXt", "zTXt", "iTXt", "iCCP")) {
            expectRejected { PhotoLitePng.checked(png(extraChunk = metadata)) }
        }
    }

    @Test fun standard_color_precision_does_not_get_confused_with_personal_metadata() {
        assertEquals(2 to 3, PhotoLitePng.checked(png(extraChunk = "sBIT")).let { it.width to it.height })
        val rgb = png(colorType = 2, beforeData = listOf("sBIT" to byteArrayOf(8, 8, 8)))
        assertEquals(2 to 3, PhotoLitePng.checked(rgb).let { it.width to it.height })
        val allColorChunks = png(beforeData = standardMetadata())
        assertArrayEquals(allColorChunks, PhotoLitePng.checked(allColorChunks).copyBytes())
    }

    @Test fun crc_correct_but_malformed_standard_metadata_is_not_a_normalized_cover() {
        val malformed = listOf(
            "sBIT" to byteArrayOf(8, 8, 8),
            "sBIT" to byteArrayOf(8, 8, 8, 0),
            "sBIT" to byteArrayOf(8, 8, 8, 9),
            "sRGB" to byteArrayOf(),
            "sRGB" to byteArrayOf(4),
            "sRGB" to byteArrayOf(0, 0),
            "gAMA" to byteArrayOf(1, 2, 3),
            "gAMA" to integers(0),
            "gAMA" to integers(-1),
            "cHRM" to ByteArray(31),
            "cHRM" to integers(31270, 32900, 64000, 33000, 30000, 60000, 15000, Int.MIN_VALUE),
            "pHYs" to ByteArray(8),
            "pHYs" to (integers(3780, 3780) + byteArrayOf(2)),
            "pHYs" to (integers(-1, 3780) + byteArrayOf(1)),
        )
        for (metadata in malformed) {
            val bytes = png(beforeData = listOf(metadata))
            // The frozen inspector verifies this fixture's PNG container, CRCs and zlib.
            // New-import metadata policy is independently responsible for this rejection.
            assertEquals(2 to 3, BackupPngInspection.inspect(bytes))
            expectRejected { PhotoLitePng.checked(bytes) }
        }
    }

    @Test fun repeated_or_after_idat_metadata_is_rejected_without_weakening_base_png_checks() {
        for (metadata in standardMetadata()) {
            val duplicate = png(beforeData = listOf(metadata, metadata))
            val late = png(afterData = listOf(metadata))
            for (bytes in listOf(duplicate, late)) {
                assertEquals(2 to 3, BackupPngInspection.inspect(bytes))
                expectRejected { PhotoLitePng.checked(bytes) }
            }
        }
    }

    @Test fun only_fresh_eight_bit_rgb_or_rgba_is_accepted_as_new_product_output() {
        for ((depth, color) in listOf(8 to 0, 8 to 4, 16 to 6, 16 to 2)) {
            val bytes = png(bitDepth = depth, colorType = color)
            assertEquals(2 to 3, BackupPngInspection.inspect(bytes))
            expectRejected { PhotoLitePng.checked(bytes) }
        }
    }

    private fun expectRejected(block: () -> Unit) {
        try { block(); fail("Unverified or metadata-bearing content must not become a cover") }
        catch (_: IllegalArgumentException) { }
    }

    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun standardMetadata(): List<Pair<String, ByteArray>> = listOf(
        "sBIT" to byteArrayOf(8, 8, 8, 8),
        "sRGB" to byteArrayOf(0),
        "gAMA" to integers(45455),
        "cHRM" to integers(31270, 32900, 64000, 33000, 30000, 60000, 15000, 6000),
        "pHYs" to (integers(3780, 3780) + byteArrayOf(1)),
    )

    private fun integers(vararg values: Int): ByteArray = ByteArrayOutputStream().also { output ->
        DataOutputStream(output).use { data -> values.forEach(data::writeInt) }
    }.toByteArray()

    private fun png(
        width: Int = 2,
        height: Int = 3,
        extraChunk: String? = null,
        bitDepth: Int = 8,
        colorType: Int = 6,
        beforeData: List<Pair<String, ByteArray>> = emptyList(),
        afterData: List<Pair<String, ByteArray>> = emptyList(),
    ): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10))
        fun chunk(name: String, data: ByteArray) {
            val type = name.toByteArray(Charsets.US_ASCII)
            DataOutputStream(output).apply {
                writeInt(data.size); write(type); write(data)
                writeInt(CRC32().apply { update(type); update(data) }.value.toInt())
            }
        }
        val header = ByteArrayOutputStream().also {
            DataOutputStream(it).apply {
                writeInt(width); writeInt(height)
                write(byteArrayOf(bitDepth.toByte(), colorType.toByte(), 0, 0, 0))
            }
        }.toByteArray()
        chunk("IHDR", header)
        extraChunk?.let { chunk(it, if (it == "sBIT") byteArrayOf(8, 8, 8, 8) else byteArrayOf(1, 2, 3)) }
        beforeData.forEach { (kind, data) -> chunk(kind, data) }
        val channels = when (colorType) { 0 -> 1; 2 -> 3; 4 -> 2; 6 -> 4; else -> error("Unsupported fixture color") }
        val rows = ByteArrayOutputStream().also { target ->
            DeflaterOutputStream(target).use { z ->
                repeat(height) {
                    z.write(0)
                    repeat(width) {
                        repeat(channels) { channel ->
                            if (bitDepth == 16) z.write(0)
                            z.write(if (channel == 3) 255 else (channel + 1) * 8)
                        }
                    }
                }
            }
        }.toByteArray()
        chunk("IDAT", rows)
        afterData.forEach { (kind, data) -> chunk(kind, data) }
        chunk("IEND", byteArrayOf())
        return output.toByteArray()
    }
}
