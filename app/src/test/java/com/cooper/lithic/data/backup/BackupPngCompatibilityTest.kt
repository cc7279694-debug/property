package com.cooper.lithic.data.backup

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import org.junit.Assert.*
import org.junit.Test

class BackupPngCompatibilityTest {
    @Test fun all_standard_color_depth_pairs_and_adam7_are_supported_without_reencoding() {
        val legal=mapOf(0 to listOf(1,2,4,8,16),2 to listOf(8,16),3 to listOf(1,2,4,8),4 to listOf(8,16),6 to listOf(8,16))
        for ((color,depths) in legal) for (depth in depths) for (interlace in 0..1) for ((width,height) in listOf(1 to 1,2 to 3,11 to 9)) {
            inspect(png(color,depth,interlace,width=width,height=height)) { file ->
                val original=file.readBytes()
                assertEquals(width to height,BackupPngInspection.inspect(file))
                assertArrayEquals(original,file.readBytes())
            }
        }
    }
    @Test fun palette_order_count_bad_depth_bad_filter_and_extra_inflated_rows_fail_closed() {
        for (bytes in listOf(png(3,1,0,palette=false),png(3,1,0,paletteEntries=3),png(0,3,0),png(6,8,1,badFilter=true),png(2,8,0,extraRow=true))) inspect(bytes) {
            try { BackupPngInspection.inspect(it);fail("Broken PNG accepted") } catch (_: BackupMediaException) { }
        }
    }
    @Test fun exif_and_unknown_ancillary_metadata_are_preserved_but_animation_is_not_a_static_cover() {
        inspect(png(6,8,0,metadata=true)) { assertEquals(11 to 9,BackupPngInspection.inspect(it)) }
        inspect(png(6,8,0,animated=true)) {
            try { BackupPngInspection.inspect(it);fail("Animated PNG accepted as static") } catch (_: BackupMediaException) { }
        }
    }
    private fun png(color:Int,depth:Int,interlace:Int,palette:Boolean=true,paletteEntries:Int=2,badFilter:Boolean=false,extraRow:Boolean=false,metadata:Boolean=false,animated:Boolean=false,width:Int=11,height:Int=9):ByteArray {
        val output=ByteArrayOutputStream();output.write(byteArrayOf(137.toByte(),80,78,71,13,10,26,10))
        fun chunk(type:String,data:ByteArray) {
            val name=type.toByteArray(Charsets.US_ASCII)
            DataOutputStream(output).apply { writeInt(data.size);write(name);write(data);writeInt(CRC32().apply { update(name);update(data) }.value.toInt()) }
        }
        chunk("IHDR",ByteArrayOutputStream().also { DataOutputStream(it).apply { writeInt(width);writeInt(height);write(byteArrayOf(depth.toByte(),color.toByte(),0,0,interlace.toByte())) } }.toByteArray())
        if(color==3 && palette) chunk("PLTE",ByteArray(paletteEntries*3))
        if(metadata) { chunk("eXIf",byteArrayOf(73,73,42,0,8,0,0,0,0,0,0,0,0,0));chunk("vpAg",byteArrayOf(1,2,3)) }
        if(animated) chunk("acTL",byteArrayOf(0,0,0,1,0,0,0,0))
        val channels=when(color) { 0,3->1;2->3;4->2;else->4 }
        val raw=ByteArrayOutputStream()
        val passes=if(interlace==0) listOf(intArrayOf(0,0,1,1)) else listOf(intArrayOf(0,0,8,8),intArrayOf(4,0,8,8),intArrayOf(0,4,4,8),intArrayOf(2,0,4,4),intArrayOf(0,2,2,4),intArrayOf(1,0,2,2),intArrayOf(0,1,1,2))
        for (p in passes) {
            val passWidth=if(width<=p[0]) 0 else (width-p[0]+p[2]-1)/p[2]
            val passHeight=if(height<=p[1]) 0 else (height-p[1]+p[3]-1)/p[3]
            if(passWidth>0&&passHeight>0) repeat(passHeight) { raw.write(if(badFilter)5 else 0);raw.write(ByteArray((passWidth*channels*depth+7)/8)) }
        }
        if(extraRow) raw.write(byteArrayOf(0))
        chunk("IDAT",ByteArrayOutputStream().also { DeflaterOutputStream(it).use { z->z.write(raw.toByteArray()) } }.toByteArray());chunk("IEND",byteArrayOf())
        return output.toByteArray()
    }
    private fun inspect(bytes:ByteArray,block:(java.io.File)->Unit) {
        val file=Files.createTempFile("backup-png-compatible-", ".png").toFile()
        try { file.writeBytes(bytes);block(file) } finally { file.delete() }
    }
}
