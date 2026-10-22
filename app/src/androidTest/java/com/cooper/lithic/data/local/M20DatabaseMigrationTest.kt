package com.cooper.lithic.data.local

import android.content.Context
import com.cooper.lithic.testutil.M20TestRoots
import org.junit.After
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Only new synthetic roots, no default ledger or previously retained device directories. */
@RunWith(AndroidJUnit4::class)
class M20DatabaseMigrationTest {
    private val roots = M20TestRoots()
    @After fun cleanClosedRoots() { roots.cleanupClosed() }
    @Test fun productionOpenMigratesVersionsOneTwoThreeAndReopensVersionFour() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (version in 1..3) {
            val root = roots.create(context, "migration").also { roots.database(it) }
            val file = File(root, "data.db")
            val json = InstrumentationRegistry.getInstrumentation().context.assets
                .open("com.cooper.lithic.data.local.LithicDatabase/$version.json")
                .use { JSONObject(it.bufferedReader().readText()).getJSONObject("database") }
            SQLiteDatabase.openOrCreateDatabase(file, null).use { raw ->
                val entities = json.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val table = entities.getJSONObject(i)
                    val name = table.getString("tableName")
                    raw.execSQL(table.getString("createSql").replace("\${TABLE_NAME}", name))
                    val indices = table.getJSONArray("indices")
                    for (j in 0 until indices.length()) raw.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", name))
                }
                val setup = json.getJSONArray("setupQueries")
                for (i in 0 until setup.length()) raw.execSQL(setup.getString(i))
                raw.execSQL("INSERT INTO categories VALUES ('00000000-0000-4000-8000-000000000001','PROJECT','其他','其他',1)")
                raw.execSQL("INSERT INTO categories VALUES ('00000000-0000-4000-8000-000000000002','SUBSCRIPTION','其他','其他',1)")
                raw.execSQL("INSERT INTO long_term_projects (id,name,categoryId,startedOn,endedOn,endReason,coverAssetId,note,createdAt,updatedAt) VALUES ('00000000-0000-4000-8000-000000000101','保留项目','00000000-0000-4000-8000-000000000001',20000,NULL,NULL,NULL,'原备注','2026-01-01T00:00:00Z','2026-01-01T00:00:00Z')")
                raw.execSQL("INSERT INTO project_money_events (id,projectId,direction,amountCents,occurredOn,description,role,operationId) VALUES ('00000000-0000-4000-8000-000000000102','00000000-0000-4000-8000-000000000101','OUTLAY',1234567,20000,'原真实付款','INITIAL','00000000-0000-4000-8000-000000000103')")
                if (version >= 2) {
                    raw.execSQL("INSERT INTO subscriptions VALUES ('00000000-0000-4000-8000-000000000201','保留订阅','00000000-0000-4000-8000-000000000002',NULL,'原订阅备注','2026-01-01T00:00:00Z','2026-01-01T00:00:00Z')")
                    raw.execSQL("INSERT INTO subscription_renewals VALUES ('00000000-0000-4000-8000-000000000202','00000000-0000-4000-8000-000000000201',20000,20000,20030,20035,98765,'MONTHLY',30,'00000000-0000-4000-8000-000000000203')")
                }
                raw.version = version
            }
            val database = LithicDatabase.open(context, file, requireExisting = true)
            try {
                assertEquals(5, database.openHelper.readableDatabase.version)
                database.requireDefaults()
                assertTrue(database.notificationReceiptDao().list().isEmpty())
                assertTrue(database.milestoneWatermarkDao().list().isEmpty())
                assertEquals("原备注", database.projectDao().listProjects().single().note)
                assertEquals(1234567L, database.projectDao().listEvents().single().amountCents)
                assertEquals(MoneyBasis.CASH, database.projectDao().listEvents().single().basis)
                assertNull(database.projectDao().listEvents().single().exchangeGroupId)
                if (version >= 2) {
                    val subscription = database.subscriptionDao().validationSubscriptions(null, 10).single()
                    assertEquals("原订阅备注", subscription.note)
                    val renewal = database.subscriptionDao().renewalsForSubscriptions(listOf(subscription.id)).single()
                    assertEquals(98765L, renewal.amountCents)
                    assertEquals(20035L, renewal.reminderDueOn.toEpochDay())
                }
            } finally { database.close(); roots.ownersClosed(root) }
            roots.ownersOpening(root)
            val reopened = LithicDatabase.open(context, file, requireExisting = true)
            try {
                assertEquals(5, reopened.openHelper.readableDatabase.version)
                reopened.requireDefaults()
                assertEquals(2, reopened.categoryDao().list().size)
                assertEquals(1234567L, reopened.projectDao().listEvents().single().amountCents)
            } finally { reopened.close(); roots.ownersClosed(root) }
        }
    }

    @Test fun receiptUniqueKeySurvivesProductionReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = roots.create(context, "receipt").also { roots.database(it) }
        val file = File(root, "data.db")
        val owner = UUID.randomUUID().toString()
        val receipt = NotificationReceiptEntity("RENEWAL:$owner:2026-10-25", owner, "RENEWAL", "2026-10-25", Instant.parse("2026-10-22T02:00:00Z"))
        val database = LithicDatabase.open(context, file)
        try {
            database.notificationReceiptDao().insert(receipt)
            try { database.notificationReceiptDao().insert(receipt); fail("Duplicate receipt inserted") }
            catch (_: android.database.sqlite.SQLiteConstraintException) { }
        } finally { database.close(); roots.ownersClosed(root) }
        roots.ownersOpening(root)
        val reopened = LithicDatabase.open(context, file, true)
        try { assertEquals(receipt, reopened.notificationReceiptDao().find(receipt.logicalKey)) }
        finally { reopened.close(); roots.ownersClosed(root) }
    }
}
