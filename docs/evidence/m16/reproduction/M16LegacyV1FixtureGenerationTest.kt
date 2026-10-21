package com.cooper.lithic.data.backup

import com.cooper.lithic.data.local.LithicDatabase
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

/** One-off retained fixture generation, run against the unmodified frozen M15 format-v1 writer. */
class M16LegacyV1FixtureGenerationTest {
    @Test fun generate_using_frozen_format_v1_writer_before_m16_production_changes() {
        val parent = Paths.get("build/m16-fixtures").toAbsolutePath().normalize()
        Files.createDirectories(parent)
        val archive = parent.resolve("legacy-v1.lithicbackup")
        check(!Files.exists(archive)) { "Original fixture already exists; never overwrite" }
        val at = "2026-10-21T00:00:00Z"
        val png = originalPng()
        val file = BackupFileEntry.fromBytes("media/m16-legacy-original.png", png)
        val data = BackupDataV1(
            listOf(BackupCategoryV1(LithicDatabase.PROJECT_FALLBACK_ID,"PROJECT","其他","其他",true),
                BackupCategoryV1(LithicDatabase.SUBSCRIPTION_FALLBACK_ID,"SUBSCRIPTION","其他","其他",true)),
            listOf(BackupProjectV1(id(10),"旧格式相机",LithicDatabase.PROJECT_FALLBACK_ID,"2020-01-01",null,null,id(20),"冻结V1原件",at,at)),
            listOf(BackupProjectMoneyEventV1(id(11),id(10),"OUTLAY",10000,"2020-01-01",null,"INITIAL",id(12))),
            listOf(BackupMediaAssetV1(id(20),file.path,"image/png",1,1,file.byteLength,file.sha256)),
            listOf(BackupSubscriptionV1(id(30),"旧格式服务",LithicDatabase.SUBSCRIPTION_FALLBACK_ID,id(20),null,at,at)),
            listOf(BackupSubscriptionRenewalV1(id(31),id(30),"2026-01-01","2026-01-01","2026-02-01","2026-01-29",15000,"MONTHLY",30,id(32))),
            listOf(BackupSubscriptionStateChangeV1(id(33),id(30),"2026-01-15","PAUSED",null,null,id(34))),
        )
        val snapshot = BackupSnapshot(data,BackupSettingsV1("Asia/Shanghai"))
        val core = BackupCoreStager().prepare(snapshot,parent)
        Files.createDirectory(core.directory.resolve("media"))
        Files.write(core.directory.resolve(file.path),png)
        val complete = core.copy(entries = core.entries + file)
        val manifest = BackupManifestV1.complete(complete,"frozen-661d2b5-v1-fixture",Instant.parse(at))
        check(manifest.formatVersion == 1 && manifest.minReaderFormat == 1 && manifest.sourceDatabaseSchemaVersion == 2)
        val verified = BackupArchiveFiles.create(complete,manifest,archive)
        val restored = BackupValidator().validate(archive,Files.createDirectory(parent.resolve("validated-v1")))
        assertEquals(snapshot,restored.snapshot)
        println("M16_LEGACY_V1_FIXTURE path=${verified.path} bytes=${verified.byteLength} sha256=${verified.sha256}")
    }
    /** Original 1x1 RGBA PNG: one opaque pixel, lossless zlib bytes, every chunk CRC generated. */
    private fun originalPng():ByteArray {
        val bytes=java.io.ByteArrayOutputStream()
        bytes.write(byteArrayOf(137.toByte(),80,78,71,13,10,26,10))
        val output=java.io.DataOutputStream(bytes)
        fun chunk(type:String,payload:ByteArray) {
            val name=type.toByteArray(Charsets.US_ASCII)
            output.writeInt(payload.size);output.write(name);output.write(payload)
            val crc=java.util.zip.CRC32();crc.update(name);crc.update(payload)
            output.writeInt(crc.value.toInt())
        }
        val header=java.io.ByteArrayOutputStream()
        java.io.DataOutputStream(header).use { it.writeInt(1);it.writeInt(1);it.write(byteArrayOf(8,6,0,0,0)) }
        chunk("IHDR",header.toByteArray())
        val compressed=java.io.ByteArrayOutputStream()
        java.util.zip.DeflaterOutputStream(compressed).use { it.write(byteArrayOf(0,43,71,89,255.toByte())) }
        chunk("IDAT",compressed.toByteArray());chunk("IEND",byteArrayOf())
        return bytes.toByteArray()
    }
    private fun id(number:Int)="00000000-0000-4000-8000-${number.toString().padStart(12,'0')}"
}
