package com.cooper.lithic.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.AtomicFile
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.*
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import com.cooper.lithic.data.repository.TradeInIdentities
import com.cooper.lithic.data.settings.SettingsStore
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** All roots and data are newly synthetic. No default ledger, SAF, process termination or old AVD access. */
@RunWith(AndroidJUnit4::class)
class M16BackupRoundTripTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private var ownedActive:ManagedGeneration?=null

    @Test fun retained_real_v1_archive_restores_original_seven_tables_settings_media_and_reopens_as_v3()=runBlocking {
        scenario { root,owner,current ->
            val backup=legacyBackup(root)
            assertEquals(1,backup.manifest.formatVersion)
            assertEquals(7,backup.manifest.recordCounts.size)
            assertEquals(8,backup.snapshot.data.recordCounts().size)
            val candidate=owner.prepareRestore(ZoneId.of("Asia/Shanghai")) { RestoredGenerationWriter().populate(it,backup) }
            val restored=owner.activateRestore(current.id,candidate)
            ownedActive=restored
            assertEquals(backup.snapshot,restored.backupSnapshotSource().capture())
            assertTrue(restored.database.projectDao().listEvents().all { it.basis==MoneyBasis.CASH && it.exchangeGroupId==null })
            assertTrue(restored.database.replacementDao().listReplacements().isEmpty())
            assertEquals(3,restored.database.openHelper.readableDatabase.version)
            val id=restored.id;restored.close()
            DataGenerationStore(context,root).open().use { reopened ->
                assertEquals(id,reopened.id);RestoredGenerationWriter().verify(reopened,backup.snapshot)
                assertEquals(backup.snapshot,reopened.backupSnapshotSource().capture())
            }
        }
    }

    @Test fun current_v2_roundtrip_preserves_links_trade_in_all_eight_tables_settings_and_original_media()=runBlocking {
        scenario { root,owner,current ->
            RestoreSyntheticFixture.insert(current)
            addTradeIn(current)
            val before=current.backupSnapshotSource().capture()
            val originalMedia=before.data.mediaAssets.associate { it.relativePath to File(current.directory,it.relativePath).readBytes() }
            val archive=File(root,"v2.lithicbackup")
            assertTrue(BackupExportService(context,current).export(RestoreSyntheticFixture.target(archive)) is BackupExportResult.Success)
            val backup=BackupValidator().validate(archive.toPath(),Files.createDirectory(root.toPath().resolve("v2-payload")))
            assertEquals(2,backup.manifest.formatVersion);assertEquals(2,backup.manifest.minReaderFormat)
            assertEquals(3,backup.manifest.sourceDatabaseSchemaVersion)
            assertEquals(before,backup.snapshot)
            val candidate=owner.prepareRestore(ZoneId.of(before.settings.recordZoneId)) { RestoredGenerationWriter().populate(it,backup) }
            val restored=owner.activateRestore(current.id,candidate)
            ownedActive=restored
            assertEquals(before,restored.backupSnapshotSource().capture())
            assertEquals(1,restored.database.replacementDao().listReplacements().size)
            assertEquals(3,restored.database.projectDao().listEvents().count { it.exchangeGroupId!=null })
            originalMedia.forEach { (path,bytes) -> assertArrayEquals(bytes,File(restored.directory,path).readBytes()) }
            val proof=JSONObject(File(restored.directory,"restore-proof.json").readText())
            assertEquals(1,proof.getInt("formatVersion"));assertEquals(2,proof.getInt("dataFormatVersion"))
            val id=restored.id;restored.close()
            DataGenerationStore(context,root).open().use { reopened ->
                assertEquals(id,reopened.id);assertEquals(before,reopened.backupSnapshotSource().capture())
            }
        }
    }

    @Test fun old_schema2_switching_generation_with_five_field_v1_proof_migrates_and_verifies_without_false_rollback()=runBlocking {
        scenario { root,_,old ->
            val backup=legacyBackup(root)
            val previous=old.id;val candidateId=UUID.randomUUID().toString()
            val directory=File(root,"generations/$candidateId").apply { check(mkdir()) }
            File(directory,"media").apply { check(mkdir()) }
            backup.snapshot.data.mediaAssets.forEach { asset ->
                File(directory,asset.relativePath).apply { parentFile!!.mkdirs();writeBytes(Files.readAllBytes(backup.directory.resolve(asset.relativePath))) }
            }
            createSchema2(File(directory,"data.db"),backup.snapshot.data.legacyProjection())
            val gate=LocalWriteCoordinator()
            SettingsStore.open(File(directory,"settings.preferences_pb"),gate,ZoneId.of(backup.snapshot.settings.recordZoneId)).close()
            File(directory,"ready.json").writeText(JSONObject().put("formatVersion",1).put("generationId",candidateId).toString())
            File(directory,"restore-proof.json").writeText(JSONObject().put("formatVersion",1).put("generationId",candidateId)
                .put("nonce",UUID.randomUUID().toString()).put("dataSha256",sha(BackupJson.encodeData(backup.snapshot.data.legacyProjection())))
                .put("settingsSha256",sha(BackupJson.encodeSettings(backup.snapshot.settings))).toString())
            old.close();ownedActive=null
            atomic(File(root,"active-generation"),JSONObject().put("formatVersion",1).put("activeId",candidateId)
                .put("previousId",previous).put("stage","SWITCHING"))
            val resumed=DataGenerationStore(context,root)
            resumed.open().use { migrated ->
                assertEquals(candidateId,migrated.id)
                assertEquals(3,migrated.database.openHelper.readableDatabase.version)
                assertEquals(backup.snapshot,migrated.backupSnapshotSource().capture())
                assertNull(resumed.lastRecoveryWarning)
                val pointer=JSONObject(File(root,"active-generation").readText())
                assertEquals("STABLE",pointer.getString("stage"));assertEquals(candidateId,pointer.getString("activeId"))
                assertFalse(JSONObject(File(directory,"restore-proof.json").readText()).has("dataFormatVersion"))
            }
            DataGenerationStore(context,root).open().use { reopened ->
                assertEquals(candidateId,reopened.id);assertEquals(backup.snapshot,reopened.backupSnapshotSource().capture())
            }
        }
    }

    @Test fun corrupt_v2_group_is_rejected_before_restore_preview_and_current_dataset_stays_readable()=runBlocking {
        scenario { root,_,current ->
            RestoreSyntheticFixture.insert(current);addTradeIn(current)
            val original=current.backupSnapshotSource().capture()
            val core=BackupCoreStager().prepare(original.copy(data=original.data.copy(replacements=listOf(
                BackupReplacementV2(UUID.randomUUID().toString(),original.data.projects.first().id,UUID.randomUUID().toString(),Instant.now().toString())))),root.toPath())
            val complete=BackupMediaSnapshot(current).copy(original,core,{})
            val manifest=BackupManifestV1.complete(complete,"m16-invalid-ref",Instant.now())
            val archive=BackupArchiveFiles.create(complete,manifest,root.toPath().resolve("invalid.lithicbackup"))
            val extraction=Files.createDirectory(root.toPath().resolve("invalid-payload"))
            var rejected=false
            try { BackupValidator().validate(archive.path,extraction) } catch(_:IllegalArgumentException) { rejected=true }
            assertTrue(rejected);Files.list(extraction).use { assertEquals(0L,it.count()) }
            assertEquals(original,current.backupSnapshotSource().capture())
            assertEquals(current.id,JSONObject(File(root,"active-generation").readText()).getString("activeId"))
        }
    }

    @Test fun complete_export_rejects_a_broken_trade_in_group_instead_of_successfully_writing_an_unrestorable_archive()=runBlocking {
        scenario { root,_,current ->
            RestoreSyntheticFixture.insert(current);addTradeIn(current)
            current.coordinator.withWrite { current.database.withTransaction {
                current.database.openHelper.writableDatabase.execSQL("DELETE FROM project_money_events WHERE exchangeGroupId IS NOT NULL AND basis='TRADE_IN_CREDIT' AND direction='OUTLAY'")
            } }
            val damagedEvents=current.database.projectDao().listEvents()
            val damagedProjects=current.database.projectDao().listProjects()
            val result=BackupExportService(context,current).export(RestoreSyntheticFixture.target(File(root,"broken-group.lithicbackup")))
            assertTrue("A complete export may not certify an archive whose required transaction facts are missing: $result",result is BackupExportResult.Failed)
            assertEquals(damagedEvents,current.database.projectDao().listEvents())
            assertEquals(damagedProjects,current.database.projectDao().listProjects())
        }
    }

    private suspend fun addTradeIn(generation:ManagedGeneration) {
        val old=generation.database.projectDao().listProjects().single { it.endedOn==null }
        val group=UUID.randomUUID().toString();val day=LocalDate.parse("2026-01-10");val now=Instant.parse("2026-01-10T00:00:00Z")
        val newId=TradeInIdentities.newProjectId(group)
        generation.coordinator.withWrite { generation.database.withTransaction {
            generation.database.openHelper.writableDatabase.execSQL("UPDATE long_term_projects SET endedOn=?,endReason='以旧换新',updatedAt=? WHERE id=?",arrayOf<Any?>(day.toEpochDay(),now.toString(),old.id))
            generation.database.projectDao().insertProject(LongTermProjectEntity(newId,"换新设备",old.categoryId,day,createdAt=now,updatedAt=now))
            for((role,direction,amount,basis,project,storedRole) in listOf(
                Row(TradeInIdentities.OLD_CREDIT,MoneyDirection.RECOVERY,300000,MoneyBasis.TRADE_IN_CREDIT,old.id,ProjectMoneyEventRole.END_SETTLEMENT),
                Row(TradeInIdentities.NEW_CASH,MoneyDirection.OUTLAY,500000,MoneyBasis.CASH,newId,ProjectMoneyEventRole.INITIAL),
                Row(TradeInIdentities.NEW_CREDIT,MoneyDirection.OUTLAY,300000,MoneyBasis.TRADE_IN_CREDIT,newId,ProjectMoneyEventRole.REGULAR))) {
                generation.database.projectDao().insertEvent(ProjectMoneyEventEntity(TradeInIdentities.eventId(group,role),project,direction,amount,day,null,
                    storedRole,TradeInIdentities.operationId(group,role),basis,group))
            }
            generation.database.replacementDao().insert(ReplacementEntity(TradeInIdentities.linkId(group),old.id,newId,now))
        } }
    }
    private data class Row(val role:String,val direction:MoneyDirection,val amount:Long,val basis:MoneyBasis,val project:String,val storedRole:ProjectMoneyEventRole)

    private suspend fun legacyBackup(root:File):ValidatedBackup {
        val archive=File(root,"legacy-v1.lithicbackup")
        InstrumentationRegistry.getInstrumentation().context.assets.open("m16/legacy-v1.lithicbackup").use { input -> archive.outputStream().use { input.copyTo(it) } }
        assertEquals(1803L,archive.length())
        assertEquals("d7dd7e8bf105721cfeeed687df977bf69921ca5c28e8e9d8d083beb60becb818",sha(archive.readBytes()))
        return BackupValidator().validate(archive.toPath(),Files.createDirectory(root.toPath().resolve("legacy-payload")))
    }
    private suspend fun createSchema2(file:File,data:BackupDataV1)=withContext(Dispatchers.IO) {
        val schema=InstrumentationRegistry.getInstrumentation().context.assets.open("com.cooper.lithic.data.local.LithicDatabase/2.json").use {
            JSONObject(it.readBytes().toString(Charsets.UTF_8)).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(file,null).use { db ->
            db.setForeignKeyConstraintsEnabled(true)
            val entities=schema.getJSONArray("entities")
            for(n in 0 until entities.length()) {
                val entity=entities.getJSONObject(n);val table=entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}",table))
                val indices=entity.getJSONArray("indices")
                for(i in 0 until indices.length()) db.execSQL(indices.getJSONObject(i).getString("createSql").replace("${'$'}{TABLE_NAME}",table))
            }
            val setup=schema.getJSONArray("setupQueries")
            for(n in 0 until setup.length()) db.execSQL(setup.getString(n))
            data.categories.forEach { db.execSQL("INSERT INTO categories VALUES(?,?,?,?,?)",arrayOf<Any?>(it.id,it.type,it.name,it.normalizedName,if(it.isFallback)1 else 0)) }
            data.mediaAssets.forEach { db.execSQL("INSERT INTO local_media_assets VALUES(?,?,?,?,?,?,?)",arrayOf<Any?>(it.id,it.relativePath,it.mime,it.width,it.height,it.byteLength,it.sha256)) }
            data.projects.forEach { db.execSQL("INSERT INTO long_term_projects VALUES(?,?,?,?,?,?,?,?,?,?)",arrayOf<Any?>(it.id,it.name,it.categoryId,LocalDate.parse(it.startedOn).toEpochDay(),it.endedOn?.let { day -> LocalDate.parse(day).toEpochDay() },it.endReason,it.coverAssetId,it.note,it.createdAt,it.updatedAt)) }
            data.projectMoneyEvents.forEach { db.execSQL("INSERT INTO project_money_events VALUES(?,?,?,?,?,?,?,?)",arrayOf<Any?>(it.id,it.projectId,it.direction,it.amountCents,LocalDate.parse(it.occurredOn).toEpochDay(),it.description,it.role,it.operationId)) }
            data.subscriptions.forEach { db.execSQL("INSERT INTO subscriptions VALUES(?,?,?,?,?,?,?)",arrayOf<Any?>(it.id,it.name,it.categoryId,it.coverAssetId,it.note,it.createdAt,it.updatedAt)) }
            data.subscriptionRenewals.forEach { db.execSQL("INSERT INTO subscription_renewals VALUES(?,?,?,?,?,?,?,?,?,?)",arrayOf<Any?>(it.id,it.subscriptionId,epoch(it.paidOn),epoch(it.effectiveFrom),epoch(it.paidUntilExclusive),epoch(it.reminderDueOn),it.amountCents,it.cycle,it.standardDays,it.operationId)) }
            data.subscriptionStateChanges.forEach { db.execSQL("INSERT INTO subscription_state_changes VALUES(?,?,?,?,?,?,?)",arrayOf<Any?>(it.id,it.subscriptionId,epoch(it.effectiveOn),it.state,it.reason,it.note,it.operationId)) }
            db.version=2
        }
    }
    private suspend fun scenario(block:suspend(File,DataGenerationStore,ManagedGeneration)->Unit) {
        val root=File(context.noBackupFilesDir,"m16-backup-${UUID.randomUUID()}").canonicalFile;check(root.mkdir())
        val owner=DataGenerationStore(context,root);val current=owner.open(ZoneId.of("Asia/Shanghai"));ownedActive=current
        try { block(root,owner,current) } finally {
            ownedActive?.close();ownedActive=null
            check(root.canonicalFile.parentFile==context.noBackupFilesDir.canonicalFile)
            check(root.name.startsWith("m16-backup-"));check(root.deleteRecursively())
        }
    }
    private suspend fun <T> ManagedGeneration.use(block:suspend(ManagedGeneration)->T):T=try { block(this) } finally { close() }
    private fun atomic(file:File,value:JSONObject) { val atomic=AtomicFile(file);val out=atomic.startWrite();try { out.write(value.toString().toByteArray());atomic.finishWrite(out) } catch(failure:Throwable) { atomic.failWrite(out);throw failure } }
    private fun epoch(date:String)=LocalDate.parse(date).toEpochDay()
    private fun sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
}
