package com.cooper.lithic.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Genuine retained schema DDL, production preflight/open/migrate/close/reopen, no APK downgrade. */
@RunWith(AndroidJUnit4::class)
class M16MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Test fun frozen_v1_migrates_through_v2_to_v3_without_rewriting_legacy_facts() = runBlocking {
        fixture { file ->
            V1DatabaseFixture.create(file).close()
            val before = rows(file)
            open(file).useDb { db ->
                assertEquals(3, db.openHelper.writableDatabase.version)
                db.requireDefaults()
                assertTrue(db.projectDao().listEvents().all { it.basis == MoneyBasis.CASH && it.exchangeGroupId == null })
                assertTrue(db.replacementDao().listReplacements().isEmpty())
            }
            assertEquals(before, rows(file))
            open(file).useDb { db -> assertEquals(3, db.openHelper.writableDatabase.version); db.requireDefaults() }
        }
    }
    @Test fun frozen_v2_seven_tables_survive_production_upgrade_and_reopen() = runBlocking {
        fixture { file ->
            createV2(file)
            val before = rows(file)
            open(file).useDb { db ->
                assertEquals(3, db.openHelper.writableDatabase.version)
                assertEquals(12345L, db.projectDao().listEvents().single().amountCents)
                assertEquals(30, db.subscriptionDao().renewalsForSubscriptions(listOf(V1DatabaseFixture.id(920))).single().standardDays)
                assertEquals(1, db.subscriptionDao().statesForSubscriptions(listOf(V1DatabaseFixture.id(920))).size)
                assertTrue(db.replacementDao().listReplacements().isEmpty())
            }
            assertEquals(before, rows(file))
            open(file).useDb { db -> assertEquals(MoneyBasis.CASH, db.projectDao().listEvents().single().basis) }
        }
    }
    @Test fun fresh_v3_and_v3_reopen_use_the_real_exported_identity_and_defaults() = runBlocking {
        fixture { file ->
            withContext(Dispatchers.IO) { LithicDatabase.open(context,file) }.useDb { db ->
                db.initializeDefaults()
                assertEquals(3,db.openHelper.writableDatabase.version)
                assertTrue(db.projectDao().listEvents().isEmpty())
                assertTrue(db.replacementDao().listReplacements().isEmpty())
            }
            open(file).useDb { it.requireDefaults(); assertEquals(3,it.openHelper.writableDatabase.version) }
        }
    }
    @Test fun unknown_v3_identity_is_rejected_without_repair() = runBlocking {
        fixture { file ->
            withContext(Dispatchers.IO) { LithicDatabase.open(context,file) }.useDb { it.initializeDefaults() }
            SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("UPDATE room_master_table SET identity_hash='foreign' WHERE id=42")
            }
            val before=file.readBytes()
            var rejected=false
            try { open(file).close() } catch (_: Exception) { rejected=true }
            assertTrue(rejected)
            assertArrayEquals(before,file.readBytes())
        }
    }
    @Test fun failure_after_real_v3_ddl_rolls_back_v2_rows_columns_and_identity() = runBlocking {
        fixture { file ->
            createV2(file)
            val before = rows(file)
            var reached = false
            val failing = object : androidx.room.migration.Migration(2,3) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    DatabaseMigrations.MIGRATION_2_3.migrate(db)
                    reached = true
                    db.execSQL("CREATE TABLE replacements(id TEXT PRIMARY KEY)")
                }
            }
            val attempted = androidx.room.Room.databaseBuilder(context,LithicDatabase::class.java,file.path)
                .addMigrations(failing).build()
            var rejected=false
            try { withContext(Dispatchers.IO) { attempted.openHelper.writableDatabase }; Unit }
            catch (_: Exception) { rejected=true }
            finally { attempted.close() }
            assertTrue(reached);assertTrue(rejected);assertEquals(before,rows(file))
            SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READONLY).use { db ->
                assertEquals(2,db.version)
                db.rawQuery("PRAGMA table_info(project_money_events)",null).use { c ->
                    while(c.moveToNext()) assertFalse(c.getString(1) in setOf("basis","exchangeGroupId"))
                }
                db.rawQuery("SELECT identity_hash FROM room_master_table WHERE id=42",null).use { c ->
                    assertTrue(c.moveToFirst());assertEquals(schema(2).getString("identityHash"),c.getString(0))
                }
            }
            open(file).useDb { db -> assertEquals(3,db.openHelper.writableDatabase.version) }
        }
    }
    private suspend fun <T> LithicDatabase.useDb(block:suspend (LithicDatabase)->T):T = try { block(this) } finally { close() }
    private suspend fun fixture(block: suspend (File)->Unit) {
        val root=File(context.noBackupFilesDir,"m16-migration-${UUID.randomUUID()}")
        check(root.mkdir())
        try { block(File(root,"ledger.db")) } finally {
            check(root.canonicalFile.parentFile==context.noBackupFilesDir.canonicalFile)
            check(root.deleteRecursively())
        }
    }
    private suspend fun open(file: File)=withContext(Dispatchers.IO) { LithicDatabase.open(context,file,true) }
    private fun schema(version:Int):JSONObject {
        val bytes=InstrumentationRegistry.getInstrumentation().context.assets.open("com.cooper.lithic.data.local.LithicDatabase/$version.json").use { it.readBytes() }
        return JSONObject(bytes.toString(Charsets.UTF_8)).getJSONObject("database")
    }
    /** Project legacy columns, plus every v2 subscription column. New columns cannot mask changes. */
    private fun rows(file:File):Map<String,List<List<String?>>> = SQLiteDatabase.openDatabase(file.path,null,SQLiteDatabase.OPEN_READONLY).use { db ->
        val old=schema(2).getJSONArray("entities")
        buildMap {
            for(i in 0 until old.length()) {
                val table=old.getJSONObject(i); val fields=table.getJSONArray("fields")
                val names=(0 until fields.length()).map { fields.getJSONObject(it).getString("columnName") }
                val name=table.getString("tableName")
                val exists=db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?",arrayOf(name)).use { it.moveToFirst() }
                if(exists) put(name,db.rawQuery("SELECT "+names.joinToString(",") { "`$it`" }+" FROM `$name` ORDER BY id",null).use { c ->
                    buildList { while(c.moveToNext()) add((0 until c.columnCount).map { if(c.isNull(it)) null else c.getString(it) }) }
                }) else put(name,emptyList())
            }
        }
    }
    private fun createV2(file:File) {
        val schema=schema(2)
        SQLiteDatabase.openOrCreateDatabase(file,null).use { db ->
            db.setForeignKeyConstraintsEnabled(true)
            val entities=schema.getJSONArray("entities")
            for(i in 0 until entities.length()) {
                val table=entities.getJSONObject(i);val name=table.getString("tableName")
                db.execSQL(table.getString("createSql").replace("${'$'}{TABLE_NAME}",name))
                val indices=table.getJSONArray("indices")
                for(j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("${'$'}{TABLE_NAME}",name))
            }
            val queries=schema.getJSONArray("setupQueries")
            for(i in 0 until queries.length()) db.execSQL(queries.getString(i))
            val project=V1DatabaseFixture.id(910);val subscription=V1DatabaseFixture.id(920)
            db.execSQL("INSERT INTO categories VALUES(?, 'PROJECT','其他','其他',1)",arrayOf(LithicDatabase.PROJECT_FALLBACK_ID))
            db.execSQL("INSERT INTO categories VALUES(?, 'SUBSCRIPTION','其他','其他',1)",arrayOf(LithicDatabase.SUBSCRIPTION_FALLBACK_ID))
            db.execSQL("INSERT INTO local_media_assets VALUES(?,'media/retained.png','image/png',1,1,67,?)",arrayOf(V1DatabaseFixture.id(930),"a".repeat(64)))
            db.execSQL("INSERT INTO long_term_projects VALUES(?, 'v2 retained', ?, 19000,NULL,NULL,NULL,NULL,'2026-01-01T00:00:00Z','2026-01-01T00:00:00Z')",arrayOf(project,LithicDatabase.PROJECT_FALLBACK_ID))
            db.execSQL("INSERT INTO project_money_events VALUES(?,?,'OUTLAY',12345,19000,NULL,'INITIAL',?)",arrayOf(V1DatabaseFixture.id(911),project,V1DatabaseFixture.id(912)))
            db.execSQL("INSERT INTO subscriptions VALUES(?,'v2 service',?,NULL,NULL,'2026-01-01T00:00:00Z','2026-01-01T00:00:00Z')",arrayOf(subscription,LithicDatabase.SUBSCRIPTION_FALLBACK_ID))
            db.execSQL("INSERT INTO subscription_renewals VALUES(?,?,20000,20000,20031,20031,15000,'MONTHLY',30,?)",arrayOf(V1DatabaseFixture.id(921),subscription,V1DatabaseFixture.id(922)))
            db.execSQL("INSERT INTO subscription_state_changes VALUES(?,?,20000,'ENABLED',NULL,NULL,?)",arrayOf(V1DatabaseFixture.id(923),subscription,V1DatabaseFixture.id(924)))
            db.version=2
        }
    }
}
