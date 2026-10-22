package com.cooper.lithic.data.backup

import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.repository.TradeInIdentities
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

/** Real writer/parser admission; no alternate in-memory business model. */
class M16BackupV2Test {
    @Test fun newly_prepared_complete_archive_declares_format_two_schema_three() {
        val parent=Files.createTempDirectory("m16-format")
        BackupCoreStager().prepare(emptyLegacySnapshot(),parent).use { core ->
            val manifest=BackupManifestV1.complete(core,"m16-test",Instant.parse(AT))
            // Historical method identity retained; v2 input fixtures below stay v2.
            assertEquals(3,manifest.formatVersion);assertEquals(3,manifest.minReaderFormat)
            assertEquals(4,manifest.sourceDatabaseSchemaVersion)
        }
    }
    @Test fun normalized_snapshot_accounts_for_the_eighth_replacement_table() {
        assertEquals(0L,emptyLegacySnapshot().data.recordCounts()["replacements"])
        assertEquals(8,emptyLegacySnapshot().data.recordCounts().size)
    }
    @Test fun all_eight_tables_are_required_by_the_snapshot_count_budget() {
        assertEquals(0L,requireBackupRecordCounts(List(8) { 0L }))
        rejected { requireBackupRecordCounts(List(7) { 0L }) }
    }
    @Test fun real_v1_retained_archive_validates_original_counts_then_upgrades_cash_and_empty_links() {
        val fixture=Paths.get("src/androidTest/assets/m16/legacy-v1.lithicbackup").toAbsolutePath().normalize()
        val result=BackupValidator().validate(fixture,Files.createTempDirectory("m16-v1"))
        assertEquals(1,result.manifest.formatVersion);assertEquals(2,result.manifest.sourceDatabaseSchemaVersion)
        assertEquals(7,result.manifest.recordCounts.size);assertEquals(8,result.snapshot.data.recordCounts().size)
        assertTrue(result.snapshot.data.replacements.isEmpty())
        assertTrue(result.snapshot.data.projectMoneyEvents.all { it.basis=="CASH" && it.exchangeGroupId==null })
        assertEquals("Asia/Shanghai",result.snapshot.settings.recordZoneId)
        val asset=result.snapshot.data.mediaAssets.single()
        assertEquals(asset.byteLength,Files.size(result.directory.resolve(asset.relativePath)))
        assertEquals(asset.sha256,BackupFileEntry.fromBytes(asset.relativePath,Files.readAllBytes(result.directory.resolve(asset.relativePath))).sha256)
    }
    @Test fun v2_complete_trade_in_and_replacement_round_trip_preserves_each_fact_once() {
        val data=groupData()
        val result=BackupValidator().validate(archive(data),Files.createTempDirectory("m16-v2"))
        assertEquals(data.ordered(),result.snapshot.data)
        assertEquals(4,result.snapshot.data.projectMoneyEvents.size)
        assertEquals(1,result.snapshot.data.replacements.size)
    }
    @Test fun v2_missing_nullable_or_default_looking_fields_are_never_legacy_defaulted() {
        for(field in listOf("basis","exchangeGroupId")) {
            rejectedArchive(archive(groupData()) { root -> root + ("projectMoneyEvents" to JsonArray(root.getValue("projectMoneyEvents").jsonArray.mapIndexed { n,value ->
                if(n==0) JsonObject(value.jsonObject-field) else value
            })) })
        }
        rejectedArchive(archive(groupData()) { it-"replacements" })
    }
    @Test fun v2_unknown_enum_and_incomplete_trade_group_are_rejected_before_extraction() {
        val data=groupData()
        rejectedArchive(archive(data.copy(projectMoneyEvents=data.projectMoneyEvents.mapIndexed { n,row -> if(n==0) row.copy(basis="UNKNOWN") else row })))
        rejectedArchive(archive(data.copy(projectMoneyEvents=data.projectMoneyEvents.dropLast(1))))
    }
    @Test fun v2_trade_credit_mismatch_and_foreign_operation_identity_are_rejected() {
        val data=groupData()
        rejectedArchive(archive(data.copy(projectMoneyEvents=data.projectMoneyEvents.map { row ->
            if(row.id==TradeInIdentities.eventId(GROUP,TradeInIdentities.NEW_CREDIT)) row.copy(amountCents=299999) else row })))
        rejectedArchive(archive(data.copy(projectMoneyEvents=data.projectMoneyEvents.map { row ->
            if(row.exchangeGroupId==GROUP && row.basis=="CASH") row.copy(operationId=id(99)) else row })))
    }
    @Test fun v2_cycle_or_missing_project_reference_is_rejected() {
        val data=groupData();val newId=TradeInIdentities.newProjectId(GROUP)
        rejectedArchive(archive(data.copy(replacements=data.replacements+BackupReplacementV2(id(51),newId,id(10),AT))))
        rejectedArchive(archive(data.copy(replacements=listOf(BackupReplacementV2(id(52),id(10),id(999),AT)))))
    }
    @Test fun v2_duplicate_link_identity_and_unknown_top_level_field_are_rejected() {
        val data=groupData()
        rejectedArchive(archive(data.copy(replacements=data.replacements+data.replacements)))
        rejectedArchive(archive(data) { it+("unknown" to JsonPrimitive(true)) })
    }
    @Test fun unsupported_format_and_schema_pairs_cannot_downgrade_to_v1() {
        for(triple in listOf(Triple(2,1,3),Triple(2,2,2),Triple(3,2,3),Triple(1,1,3))) {
            rejectedArchive(archive(groupData(),tuple=triple))
        }
    }

    private fun groupData():BackupDataV2 {
        val newId=TradeInIdentities.newProjectId(GROUP)
        fun event(role:String,project:String,direction:String,amount:Long,storedRole:String,basis:String)=
            BackupProjectMoneyEventV2(TradeInIdentities.eventId(GROUP,role),project,direction,amount,DAY,null,storedRole,
                TradeInIdentities.operationId(GROUP,role),basis,GROUP)
        return BackupDataV2(
            listOf(BackupCategoryV1(LithicDatabase.PROJECT_FALLBACK_ID,"PROJECT","其他","其他",true),
                BackupCategoryV1(LithicDatabase.SUBSCRIPTION_FALLBACK_ID,"SUBSCRIPTION","其他","其他",true)),
            listOf(BackupProjectV1(id(10),"旧设备",LithicDatabase.PROJECT_FALLBACK_ID,"2020-01-01",DAY,"以旧换新",null,null,AT,AT),
                BackupProjectV1(newId,"新设备",LithicDatabase.PROJECT_FALLBACK_ID,DAY,null,null,null,null,AT,AT)),
            listOf(BackupProjectMoneyEventV2(id(11),id(10),"OUTLAY",1000000,"2020-01-01",null,"INITIAL",id(12),"CASH",null),
                event(TradeInIdentities.OLD_CREDIT,id(10),"RECOVERY",300000,"END_SETTLEMENT","TRADE_IN_CREDIT"),
                event(TradeInIdentities.NEW_CASH,newId,"OUTLAY",500000,"INITIAL","CASH"),
                event(TradeInIdentities.NEW_CREDIT,newId,"OUTLAY",300000,"REGULAR","TRADE_IN_CREDIT")),
            emptyList(),emptyList(),emptyList(),emptyList(),listOf(BackupReplacementV2(id(50),id(10),newId,AT)))
    }
    private fun archive(data:BackupDataV2,tuple:Triple<Int,Int,Int> = Triple(2,2,3),
        change:(JsonObject)->Map<String,JsonElement> = { it }):Path {
        val dataBytes=JsonObject(change(Json.parseToJsonElement(BackupJson.encodeData(data).decodeToString()).jsonObject)).toString().toByteArray()
        val payload=linkedMapOf("data.json" to dataBytes,"settings.json" to BackupJson.encodeSettings(BackupSettingsV1("Asia/Shanghai")))
        val manifest=BackupManifestV1(tuple.first,tuple.second,"m16-synthetic",tuple.third,AT,"Asia/Shanghai",data.recordCounts(),
            payload.map { (name,bytes) -> BackupFileEntry.fromBytes(name,bytes) })
        val path=Files.createTempFile("m16-archive",".lithicbackup")
        ZipOutputStream(Files.newOutputStream(path)).use { zip ->
            for((name,bytes) in linkedMapOf("manifest.json" to manifest.encode())+payload) {
                zip.putNextEntry(ZipEntry(name));zip.write(bytes);zip.closeEntry()
            }
        }
        return path
    }
    private fun rejectedArchive(path:Path) {
        val root=Files.createTempDirectory("m16-rejected")
        rejected { BackupValidator().validate(path,root) }
        Files.list(root).use { assertEquals(0L,it.count()) }
    }
    private fun rejected(block:()->Unit) { try { block();fail("Unsafe data was accepted") } catch(_:IllegalArgumentException) {} }
    private fun emptyLegacySnapshot()=BackupSnapshot(BackupDataV1(emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList()),BackupSettingsV1("Asia/Shanghai"))
    private fun id(n:Int)="00000000-0000-4000-8000-${n.toString().padStart(12,'0')}"
    private companion object { const val AT="2026-10-21T00:00:00Z";const val DAY="2026-10-21";const val GROUP="00000000-0000-4000-8000-000000000040" }
}
