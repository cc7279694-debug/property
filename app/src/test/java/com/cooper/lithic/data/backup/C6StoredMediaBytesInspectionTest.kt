package com.cooper.lithic.data.backup

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.util.Base64
import java.util.concurrent.CancellationException
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import org.junit.Assert.*
import org.junit.Test

/** Same registered-media rules for backup File callers and captured display bytes. */
class C6StoredMediaBytesInspectionTest {
    @Test fun png_file_and_captured_bytes_have_the_same_rules_and_do_not_rewrite_input() {
        val bytes = png()
        val before = bytes.copyOf()
        withFile(bytes) { file ->
            assertEquals(2 to 3, BackupPngInspection.inspect(bytes))
            assertEquals(BackupPngInspection.inspect(file), BackupPngInspection.inspect(bytes))
            assertArrayEquals(before, file.readBytes())
            assertArrayEquals(before, bytes)
        }
    }

    @Test fun captured_png_rejects_the_same_crc_truncation_and_trailing_damage() {
        val valid = png()
        for (bytes in listOf(valid.copyOf(valid.size - 1), valid + byteArrayOf(0),
            valid.copyOf().apply { this[44] = (this[44].toInt() xor 1).toByte() })) {
            withFile(bytes) { file ->
                assertThrows(Exception::class.java) { BackupPngInspection.inspect(file) }
                assertThrows(Exception::class.java) { BackupPngInspection.inspect(bytes) }
            }
        }
    }

    @Test fun registered_jpeg_file_and_captured_bytes_preserve_the_container_only_contract() {
        val bytes = jpeg()
        withFile(bytes) { file ->
            assertEquals(7 to 5, BackupJpegInspection.inspect(bytes))
            assertEquals(BackupJpegInspection.inspect(file), BackupJpegInspection.inspect(bytes))
        }
        val sos = bytes.indices.first { it + 3 < bytes.size && bytes[it] == 0xff.toByte() && bytes[it + 1] == 0xda.toByte() }
        val entropy = sos + 2 + (bytes[sos + 2].toInt() and 255) * 256 + (bytes[sos + 3].toInt() and 255)
        val altered = bytes.copyOf().apply { this[entropy] = (this[entropy].toInt() xor 1).toByte() }
        // Deliberately preserves the existing limitation; this is not new-JPEG admission.
        assertEquals(7 to 5, BackupJpegInspection.inspect(altered))
        assertThrows(BackupMediaException::class.java) { BackupJpegInspection.inspect(bytes.copyOf(bytes.size - 1)) }
        assertThrows(BackupMediaException::class.java) { BackupJpegInspection.inspect(bytes + byteArrayOf(0)) }
    }

    @Test fun captured_inspection_preserves_cancellation_and_exclusive_media_byte_limit() {
        val cancelled = CancellationException("synthetic stop")
        assertSame(cancelled, assertThrows(CancellationException::class.java) { BackupPngInspection.inspect(png()) { throw cancelled } })
        assertSame(cancelled, assertThrows(CancellationException::class.java) { BackupJpegInspection.inspect(jpeg()) { throw cancelled } })
        val tooLarge = ByteArray(BackupLimits.MAX_MEDIA_BYTES.toInt())
        assertThrows(BackupMediaException::class.java) { BackupPngInspection.inspect(tooLarge) }
        assertThrows(BackupMediaException::class.java) { BackupJpegInspection.inspect(tooLarge) }
    }

    private fun withFile(bytes: ByteArray, block: (java.io.File) -> Unit) {
        val file = Files.createTempFile("c6-stored-bytes-", ".image").toFile()
        try { file.writeBytes(bytes); block(file) } finally { check(file.delete()) }
    }

    private fun png(): ByteArray = ByteArrayOutputStream().also { output ->
        output.write(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10))
        fun chunk(type: String, bytes: ByteArray) {
            val name = type.toByteArray(Charsets.US_ASCII)
            DataOutputStream(output).apply { writeInt(bytes.size); write(name); write(bytes)
                writeInt(CRC32().apply { update(name); update(bytes) }.value.toInt()) }
        }
        chunk("IHDR", ByteArrayOutputStream().also { DataOutputStream(it).apply {
            writeInt(2); writeInt(3); write(byteArrayOf(8, 6, 0, 0, 0)) } }.toByteArray())
        chunk("IDAT", ByteArrayOutputStream().also { DeflaterOutputStream(it).use { compressed ->
            repeat(3) { compressed.write(byteArrayOf(0, 1, 2, 3, 255.toByte(), 4, 5, 6, 255.toByte())) }
        } }.toByteArray())
        chunk("IEND", byteArrayOf())
    }.toByteArray()

    // Existing synthetic 7x5 JDK-generated baseline fixture; not a user photograph.
    private fun jpeg(): ByteArray = Base64.getDecoder().decode("/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAAFAAcDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwCpovg3Tdo47f3aKKK8LEYuv7R+8xZTj8T9Uj77P//Z")
}
