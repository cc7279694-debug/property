package com.cooper.lithic.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.RoomBackupSnapshotSource
import com.cooper.lithic.data.repository.MilestoneWatermarkStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.domain.reminder.MilestoneOwnerType
import com.cooper.lithic.testutil.M20TestRoots
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic schema-4 input and current schema-5 reopen. No historical device roots are inspected. */
@RunWith(AndroidJUnit4::class)
class M21DatabaseMigrationTest {
    private val roots = M20TestRoots()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @After fun cleanupClosed() { roots.cleanupClosed() }

    @Test fun productionFourToFivePreservesEveryOriginalTableRowAndKnownIdentity() = runBlocking {
        val root = roots.create(context, "m21-migration").also { roots.database(it) }
        val file = File(root, "data.db")
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.cooper.lithic.data.local.LithicDatabase/4.json")
            .use { JSONObject(it.bufferedReader().readText()).getJSONObject("database") }
        val before = linkedMapOf<String, Pair<List<String>, List<List<String?>>>>()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i); val name = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", name))
                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", name))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO categories VALUES ('${LithicDatabase.PROJECT_FALLBACK_ID}','PROJECT','其他','其他',1)")
            db.execSQL("INSERT INTO categories VALUES ('${LithicDatabase.SUBSCRIPTION_FALLBACK_ID}','SUBSCRIPTION','其他','其他',1)")
            for (id in listOf(PROJECT, SUCCESSOR)) {
                db.execSQL("INSERT INTO long_term_projects VALUES ('$id','保留项目','${LithicDatabase.PROJECT_FALLBACK_ID}',20000,NULL,NULL,NULL,'原事实','2026-01-01T00:00:00Z','2026-01-01T00:00:00Z')")
            }
            db.execSQL("INSERT INTO project_money_events VALUES ('00000000-0000-4000-8000-000000000102','$PROJECT','OUTLAY',1234567,20000,'原金额','INITIAL','00000000-0000-4000-8000-000000000103','CASH',NULL)")
            db.execSQL("INSERT INTO local_media_assets VALUES ('00000000-0000-4000-8000-000000000301','media/retained.png','image/png',2,3,70,'${"0".repeat(64)}')")
            db.execSQL("INSERT INTO subscriptions VALUES ('$SUBSCRIPTION','保留订阅','${LithicDatabase.SUBSCRIPTION_FALLBACK_ID}',NULL,'原订阅','2026-01-01T00:00:00Z','2026-01-01T00:00:00Z')")
            db.execSQL("INSERT INTO subscription_renewals VALUES ('00000000-0000-4000-8000-000000000202','$SUBSCRIPTION',20000,20000,20030,20035,98765,'MONTHLY',30,'00000000-0000-4000-8000-000000000203')")
            db.execSQL("INSERT INTO subscription_state_changes VALUES ('00000000-0000-4000-8000-000000000204','$SUBSCRIPTION',20000,'ENABLED',NULL,'原状态','00000000-0000-4000-8000-000000000205')")
            db.execSQL("INSERT INTO replacements VALUES ('00000000-0000-4000-8000-000000000501','$PROJECT','$SUCCESSOR','2026-01-01T00:00:00Z')")
            db.execSQL("INSERT INTO notification_receipts VALUES ('RENEWAL:$SUBSCRIPTION:2026-10-25','$SUBSCRIPTION','RENEWAL','2026-10-25','2026-10-22T01:00:00Z')")
            db.version = 4
            for (i in 0 until entities.length()) {
                val name = entities.getJSONObject(i).getString("tableName")
                val columns = mutableListOf<String>()
                db.rawQuery("PRAGMA table_info(`$name`)", null).use { while (it.moveToNext()) columns += it.getString(1) }
                before[name] = columns to readRows(db, name, columns)
            }
        }
        val database = LithicDatabase.open(context, file, true)
        try {
            assertEquals(5, database.openHelper.readableDatabase.version)
            assertTrue(database.milestoneWatermarkDao().list().isEmpty())
            for ((name, shape) in before) {
                database.openHelper.readableDatabase.query("SELECT ${shape.first.joinToString(",") { "`$it`" }} FROM `$name` ORDER BY rowid").use { cursor ->
                    val rows = mutableListOf<List<String?>>()
                    while (cursor.moveToNext()) rows += shape.first.indices.map { if (cursor.isNull(it)) null else cursor.getString(it) }
                    assertEquals("Original row facts changed in $name", shape.second, rows)
                }
            }
        } finally { database.close(); roots.ownersClosed(root) }
        roots.ownersOpening(root)
        val reopened = LithicDatabase.open(context, file, true)
        try { assertEquals(5, reopened.openHelper.readableDatabase.version); assertEquals(1, reopened.notificationReceiptDao().list().size) }
        finally { reopened.close(); roots.ownersClosed(root) }
    }

    @Test fun sameUuidTypedPrimaryKeyAndReceiptDeletionStaySeparateAfterReopen() = runBlocking {
        val root = roots.create(context, "m21-identity").also { roots.database(it) }
        val file = File(root, "data.db"); val id = UUID.randomUUID().toString(); val date = LocalDate.parse("2026-10-22")
        val project = MilestoneWatermarkEntity(MilestoneOwnerType.PROJECT, id, date, 5)
        val subscription = MilestoneWatermarkEntity(MilestoneOwnerType.SUBSCRIPTION, id, date, 1)
        val receipts = listOf(NotificationReceiptEntity("MILESTONE:PROJECT:$id:Y1", id, "PROJECT_MILESTONE", "Y1", Instant.EPOCH),
            NotificationReceiptEntity("MILESTONE:SUBSCRIPTION:$id:D100", id, "SUBSCRIPTION_MILESTONE", "D100", Instant.EPOCH),
            NotificationReceiptEntity("RENEWAL:$id:2026-10-25", id, "RENEWAL", "2026-10-25", Instant.EPOCH))
        val database = LithicDatabase.open(context, file)
        try {
            database.milestoneWatermarkDao().upsert(project); database.milestoneWatermarkDao().upsert(subscription)
            receipts.forEach { database.notificationReceiptDao().insert(it) }
        } finally { database.close(); roots.ownersClosed(root) }
        roots.ownersOpening(root)
        val reopened = LithicDatabase.open(context, file, true)
        try {
            assertEquals(project, reopened.milestoneWatermarkDao().find(MilestoneOwnerType.PROJECT, id))
            assertEquals(subscription, reopened.milestoneWatermarkDao().find(MilestoneOwnerType.SUBSCRIPTION, id))
            reopened.milestoneWatermarkDao().delete(MilestoneOwnerType.PROJECT, id)
            reopened.notificationReceiptDao().deleteMilestones(MilestoneOwnerType.PROJECT, id)
            assertEquals(listOf(subscription), reopened.milestoneWatermarkDao().list())
            assertEquals(receipts.drop(1).map { it.logicalKey }.toSet(), reopened.notificationReceiptDao().list().map { it.logicalKey }.toSet())
        } finally { reopened.close(); roots.ownersClosed(root) }
    }

    @Test fun rawMaskOutsideIntRangeIsRejectedBeforeRoomNarrowingAndRemainsUnchanged() = runBlocking {
        val root = roots.create(context, "m21-malformed")
        val generation = DataGenerationStore(context, root).open(java.time.ZoneId.of("Asia/Shanghai"))
        roots.generation(root, generation.id)
        try {
            val id = UUID.randomUUID().toString(); val today = LocalDate.parse("2026-10-22")
            generation.coordinator.withWrite {
                generation.database.withTransaction {
                    generation.database.projectDao().insertProject(LongTermProjectEntity(id, "合成项目", LithicDatabase.PROJECT_FALLBACK_ID,
                        today.minusYears(2), createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH))
                    generation.database.projectDao().insertEvent(ProjectMoneyEventEntity(UUID.randomUUID().toString(), id,
                        com.cooper.lithic.core.model.MoneyDirection.OUTLAY, 100, today.minusYears(2), null, ProjectMoneyEventRole.INITIAL,
                        UUID.randomUUID().toString()))
                    generation.database.openHelper.writableDatabase.execSQL(
                        "INSERT INTO milestone_watermarks VALUES ('PROJECT', ?, ?, 4294967296)", arrayOf<Any>(id, today.toEpochDay()))
                }
            }
            assertEquals(1L, generation.database.milestoneWatermarkDao().countMalformed())
            try { RoomBackupSnapshotSource(generation).capture(); fail("Overflow mask was narrowed and exported") }
            catch (_: IllegalStateException) { }
            try {
                generation.coordinator.withWrite { generation.database.withTransaction {
                    MilestoneWatermarkStore.initializeWithinTransaction(generation.database, today)
                } }
                fail("Overflow mask was silently normalized by initialization")
            } catch (_: IllegalStateException) { }
            generation.database.openHelper.readableDatabase.query("SELECT suppressedMask FROM milestone_watermarks").use { cursor ->
                assertTrue(cursor.moveToFirst()); assertEquals(4294967296L, cursor.getLong(0)); assertFalse(cursor.moveToNext())
            }
        } finally { generation.close(); roots.ownersClosed(root) }
    }

    private fun readRows(db: SQLiteDatabase, name: String, columns: List<String>): List<List<String?>> =
        db.rawQuery("SELECT ${columns.joinToString(",") { "`$it`" }} FROM `$name` ORDER BY rowid", null).use { cursor ->
            buildList { while (cursor.moveToNext()) add(columns.indices.map { if (cursor.isNull(it)) null else cursor.getString(it) }) }
        }
    private companion object {
        const val PROJECT = "00000000-0000-4000-8000-000000000101"
        const val SUCCESSOR = "00000000-0000-4000-8000-000000000104"
        const val SUBSCRIPTION = "00000000-0000-4000-8000-000000000201"
    }
}
