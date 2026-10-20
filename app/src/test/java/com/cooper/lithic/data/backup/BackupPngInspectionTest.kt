package com.cooper.lithic.data.backup

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import org.junit.Assert.*
import org.junit.Test

class BackupPngInspectionTest {
    @Test fun intact_static_png_checks_all_chunks_and_dimensions() {
        val file = Files.createTempFile("backup-png", ".png").toFile()
        file.writeBytes(png())
        assertEquals(2 to 3, BackupPngInspection.inspect(file))
    }
    @Test fun truncation_checksum_damage_or_extra_bytes_are_not_valid_media() {
        val valid = png()
        for (bytes in listOf(valid.copyOf(valid.size-1), valid.copyOf().apply { this[44] = (this[44].toInt() xor 1).toByte() }, valid + byteArrayOf(0))) {
            val file = Files.createTempFile("backup-bad-png", ".png").toFile()
            file.writeBytes(bytes)
            try { BackupPngInspection.inspect(file); fail("Broken media cannot be complete") } catch (_: Exception) { }
        }
    }
    private fun png(): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(byteArrayOf(137.toByte(),80,78,71,13,10,26,10))
        fun chunk(type:String, data:ByteArray) {
            val name=type.toByteArray(Charsets.US_ASCII)
            DataOutputStream(output).apply {
                writeInt(data.size); write(name); write(data)
                writeInt(CRC32().apply { update(name); update(data) }.value.toInt())
            }
        }
        val header=ByteArrayOutputStream().also { DataOutputStream(it).apply { writeInt(2);writeInt(3);write(byteArrayOf(8,6,0,0,0)) } }.toByteArray()
        chunk("IHDR",header)
        val compressed=ByteArrayOutputStream().also { DeflaterOutputStream(it).use { z -> repeat(3) { z.write(byteArrayOf(0,1,2,3,255.toByte(),4,5,6,255.toByte())) } } }.toByteArray()
        chunk("IDAT",compressed);chunk("IEND",byteArrayOf())
        return output.toByteArray()
    }
}
