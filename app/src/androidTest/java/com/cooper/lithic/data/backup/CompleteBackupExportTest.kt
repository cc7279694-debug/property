package com.cooper.lithic.data.backup

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.*
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.io.*
import java.security.MessageDigest
import java.time.*
import java.util.UUID
import java.util.zip.ZipFile
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Entirely synthetic, isolated DataGenerationStore. No access to the normal application ledger. */
@RunWith(AndroidJUnit4::class)
class CompleteBackupExportTest {
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun before() = runBlocking {
        root = File(context.noBackupFilesDir,"m11b-test-${UUID.randomUUID()}")
        check(root.mkdir())
        generation = DataGenerationStore(context,root).open(ZoneId.of("Asia/Shanghai"))
    }
    @After fun after() = runBlocking { generation.close(); check(root.deleteRecursively()) }

    @Test fun empty_media_is_a_complete_verified_archive() = runBlocking {
        val destination = File(root,"empty.lithicbackup")
        assertTrue(BackupExportService(context,generation).export(target(destination)) is BackupExportResult.Success)
        ZipFile(destination).use { zip ->
            assertEquals(setOf("manifest.json","data.json","settings.json"), zip.entries().asSequence().map { it.name }.toSet())
        }
    }

    @Test fun seven_tables_settings_and_nonempty_png_are_exported_without_changing_any_source_bytes() = runBlocking {
        val source = fixture()
        val before = generation.backupSnapshotSource().capture()
        val bytes = source.readBytes()
        val destination = File(root,"full.lithicbackup")
        val result = BackupExportService(context,generation).export(target(destination))
        assertTrue(result is BackupExportResult.Success)
        ZipFile(destination).use { zip ->
            val data = Json.decodeFromString<BackupDataV1>(zip.getInputStream(zip.getEntry("data.json")).reader().readText())
            assertEquals(before.data,data)
            val settings = Json.decodeFromString<BackupSettingsV1>(zip.getInputStream(zip.getEntry("settings.json")).reader().readText())
            assertEquals(before.settings,settings)
            assertArrayEquals(bytes,zip.getInputStream(zip.getEntry("media/cover.png")).readBytes())
            val manifest = Json.parseToJsonElement(zip.getInputStream(zip.getEntry("manifest.json")).reader().readText()).jsonObject
            for (entry in manifest.getValue("files").jsonArray) {
                val file = entry.jsonObject
                val payload = zip.getInputStream(zip.getEntry(file.getValue("path").jsonPrimitive.content)).readBytes()
                assertEquals(payload.size.toLong(),file.getValue("byteLength").jsonPrimitive.long)
                assertEquals(sha(payload),file.getValue("sha256").jsonPrimitive.content)
            }
        }
        assertEquals(before,generation.backupSnapshotSource().capture())
        assertArrayEquals(bytes,source.readBytes())
    }

    @Test fun missing_or_damaged_registered_media_rejects_whole_export_and_preserves_business() = runBlocking {
        val source = fixture()
        val before = generation.backupSnapshotSource().capture()
        source.writeBytes(byteArrayOf(1,2,3))
        val destination = File(root,"rejected.lithicbackup")
        assertTrue(BackupExportService(context,generation).export(target(destination)) is BackupExportResult.Failed)
        assertFalse(destination.exists())
        assertEquals(before,generation.backupSnapshotSource().capture())
        assertArrayEquals(byteArrayOf(1,2,3),source.readBytes())
        assertTrue(source.delete())
        assertTrue(BackupExportService(context,generation).export(target(destination)) is BackupExportResult.Failed)
        assertEquals(before,generation.backupSnapshotSource().capture())
    }

    @Test fun target_close_failure_or_wrong_readback_never_reports_success() = runBlocking {
        for (closeFails in listOf(true,false)) {
            var removed = false
            val broken = object: BackupTarget {
                override fun openWrite() = object: ByteArrayOutputStream() {
                    override fun close() { if(closeFails) throw IOException("synthetic close failure") }
                }
                override fun openRead() = byteArrayOf(0).inputStream()
                override fun deletePartial(): Boolean { removed=true;return true }
            }
            assertTrue(BackupExportService(context,generation).export(broken) is BackupExportResult.Failed)
            assertTrue(removed)
        }
    }

    @Test fun coordinated_media_capture_blocks_a_mutation_until_the_snapshot_is_detached() = runBlocking {
        fixture()
        val reached = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val capture = async(start=CoroutineStart.UNDISPATCHED) {
            RoomBackupSnapshotSource(generation).captureInto { snapshot ->
                reached.complete(Unit);release.await();snapshot
            }
        }
        reached.await()
        val writer = async(start=CoroutineStart.UNDISPATCHED) {
            generation.coordinator.withWrite { generation.database.categoryDao().insert(
                CategoryEntity(id(),CategoryType.PROJECT,"later","later",false)) }
        }
        assertFalse(writer.isCompleted)
        release.complete(Unit)
        val saved = capture.await()
        writer.await()
        assertEquals(saved.data.categories.size+1,generation.backupSnapshotSource().capture().data.categories.size)
    }

    @Test fun jpg_without_a_strict_certificate_is_rejected_and_never_silently_omitted() = runBlocking {
        val at=Instant.parse("2026-01-01T00:00:00Z")
        val bytes=ByteArrayOutputStream().also { output ->
            val bitmap=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888)
            try { check(bitmap.compress(Bitmap.CompressFormat.JPEG,90,output)) } finally { bitmap.recycle() }
        }.toByteArray()
        val source=File(generation.directory,"media/cover.jpg").also { it.writeBytes(bytes) }
        generation.coordinator.withWrite { generation.database.mediaDao().insert(LocalMediaAssetEntity(id(),"media/cover.jpg","image/jpeg",2,2,bytes.size.toLong(),sha(bytes))) }
        val destination=File(root,"jpeg-blocked.lithicbackup")
        val result=BackupExportService(context,generation).export(target(destination))
        assertEquals(BackupFailureReason.MEDIA_NOT_CERTIFIED,(result as BackupExportResult.Failed).reason)
        assertFalse(destination.exists())
        assertArrayEquals(bytes,source.readBytes())
    }

    private suspend fun fixture(): File {
        val at=Instant.parse("2026-01-01T00:00:00Z");val day=LocalDate.parse("2026-01-01")
        val bytes=ByteArrayOutputStream().also { output ->
            val bitmap=Bitmap.createBitmap(2,3,Bitmap.Config.ARGB_8888)
            try { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,output)) } finally { bitmap.recycle() }
        }.toByteArray()
        val file=File(generation.directory,"media/cover.png").also { it.writeBytes(bytes) }
        generation.coordinator.withWrite {
            val media=LocalMediaAssetEntity(id(),"media/cover.png","image/png",2,3,bytes.size.toLong(),sha(bytes))
            val project=LongTermProjectEntity(id(),"synthetic",LithicDatabase.PROJECT_FALLBACK_ID,day,coverAssetId=media.id,createdAt=at,updatedAt=at)
            val subscription=SubscriptionEntity(id(),"synthetic",LithicDatabase.SUBSCRIPTION_FALLBACK_ID,media.id,createdAt=at,updatedAt=at)
            generation.database.mediaDao().insert(media)
            generation.database.projectDao().insertProject(project)
            generation.database.projectDao().insertEvent(ProjectMoneyEventEntity(id(),project.id,MoneyDirection.OUTLAY,12345,day,null,ProjectMoneyEventRole.INITIAL,id()))
            generation.database.subscriptionDao().insertSubscription(subscription)
            generation.database.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(id(),subscription.id,day,day,day.plusMonths(1),day.plusMonths(1),18000,BillingCycle.MONTHLY,30,id()))
            generation.database.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(),subscription.id,day,SubscriptionOperationState.ENABLED,operationId=id()))
        }
        return file
    }
    private fun target(file:File)=object:BackupTarget {
        override fun openWrite():OutputStream=FileOutputStream(file)
        override fun openRead():InputStream=FileInputStream(file)
        override fun deletePartial()=!file.exists()||file.delete()
    }
    private fun id()=UUID.randomUUID().toString()
    private fun sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt() and 255)}
}
