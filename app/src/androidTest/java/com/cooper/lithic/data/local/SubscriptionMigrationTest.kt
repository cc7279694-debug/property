package com.cooper.lithic.data.local

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.repository.AddProjectMoneyEventCommand
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.settings.SettingsStore
import java.io.DataInputStream
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** These APIs compile against frozen production v1; missing v2 behavior must be a real assertion RED. */
@RunWith(AndroidJUnit4::class)
class SubscriptionMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var root: File
    private val handles = mutableListOf<LithicDatabase>()
    private val generations = mutableListOf<ManagedGeneration>()
    private val v2Tables = V1DatabaseFixture.TABLES + setOf("subscriptions", "subscription_renewals", "subscription_state_changes")

    @Before fun setUp() {
        root = File(context.noBackupFilesDir, "m08-migration-test-${UUID.randomUUID()}")
        check(root.mkdir())
    }

    @After fun tearDown() = runBlocking {
        generations.asReversed().forEach { it.close() }
        handles.asReversed().forEach { it.close() }
        check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
        check(root.deleteRecursively())
    }

    // M08-T01: omission of migration, any legacy rewrite, or adoption of an empty ledger breaks this.
    @Test fun v1_to_v2_preserves_projects() = runBlocking {
        val file = v1File()
        val before = V1DatabaseFixture.snapshot(file)
        assertEquals(1, before.version)
        assertEquals(V1DatabaseFixture.IDENTITY, before.identity)
        assertEquals(4, before.legacy.tables.getValue("categories").rows.size)
        assertEquals(3, before.legacy.tables.getValue("long_term_projects").rows.size)
        assertEquals(10, before.legacy.tables.getValue("project_money_events").rows.size)
        assertEquals(3, before.legacy.tables.getValue("local_media_assets").rows.size)
        val opened = open(file)
        assertEquals("An existing genuine v1 database must migrate rather than remain v1", 2, opened.openHelper.writableDatabase.version)
        opened.requireDefaults()
        val after = V1DatabaseFixture.snapshot(file)
        assertEquals(before.legacy, after.legacy)
        assertEquals(v2Tables, after.businessTables)
        assertNotEquals(before.identity, after.identity)
        assertIntegrity(opened)
        assertEmptyNewTables(opened)
        val initial = opened.projectDao().findEvent(V1DatabaseFixture.CORRECTED_INITIAL_EVENT_ID)!!
        assertEquals(14683L, initial.amountCents)
        assertEquals(ProjectMoneyEventRole.INITIAL, initial.role)
        assertEquals(V1DatabaseFixture.id(801), initial.operationId)
        val settlement = opened.projectDao().findEvent(V1DatabaseFixture.END_SETTLEMENT_EVENT_ID)!!
        assertEquals(MoneyDirection.RECOVERY, settlement.direction)
        assertEquals(ProjectMoneyEventRole.END_SETTLEMENT, settlement.role)
        assertEquals(V1DatabaseFixture.ENDED_PROJECT_ID, settlement.projectId)
        opened.close()
        val reopened = open(file)
        assertEquals(2, reopened.openHelper.writableDatabase.version)
        assertEquals(before.legacy, V1DatabaseFixture.snapshot(file).legacy)
        assertIntegrity(reopened)
    }

    // A fresh-v2 success is separately required and cannot stand in for the preceding upgrade.
    @Test fun fresh_database_is_v2_with_empty_subscription_tables() = runBlocking {
        val file = File(root, "fresh.db")
        val opened = withContext(Dispatchers.IO) { LithicDatabase.open(context, file) }.also(handles::add)
        opened.initializeDefaults()
        assertEquals(2, opened.openHelper.writableDatabase.version)
        assertEquals(v2Tables, V1DatabaseFixture.snapshot(file).businessTables)
        assertEquals(2, opened.categoryDao().list().size)
        assertTrue(opened.projectDao().listProjects().isEmpty())
        assertEmptyNewTables(opened)
        assertIntegrity(opened)
    }

    // A valid upgraded file must pass the same existing-file preflight on each later launch.
    @Test fun known_v2_reopens_and_old_project_writes_still_work() = runBlocking {
        val file = v1File()
        open(file).close()
        val opened = open(file)
        assertEquals(2, opened.openHelper.writableDatabase.version)
        val repository = ProjectRepository(opened, LocalWriteCoordinator(), FixedBusinessClock(LocalDate.of(2026, 1, 12)))
        val before = repository.loadProjectDetail(V1DatabaseFixture.ENDED_PROJECT_ID)!!
        assertEquals(-500L, before.cost.netCents)
        assertFalse(before.cost.contributesToCurrentTotal)
        assertEquals(8L, before.cost.heldDays)
        val operation = V1DatabaseFixture.id(950)
        val event = ProjectMoneyEventEntity(V1DatabaseFixture.id(951), before.project.id, MoneyDirection.OUTLAY,
            125L, LocalDate.of(2026, 1, 12), "升级后真实尾款", ProjectMoneyEventRole.REGULAR, operation)
        assertTrue(repository.appendMoneyEvent(AddProjectMoneyEventCommand(operation, event)) is MutationResult.Success)
        val after = repository.loadProjectDetail(before.project.id)!!
        assertEquals(before.project, after.project)
        assertEquals(-375L, after.cost.netCents)
        assertEquals(8L, after.cost.heldDays)
        assertFalse(after.cost.contributesToCurrentTotal)
        assertEquals(before.events + event, after.events)
        assertIntegrity(opened)
    }

    @Test fun known_v1_legacy_identity_migrates_without_changing_old_rows() = runBlocking {
        val file = v1File()
        modify(file) { execSQL("UPDATE room_master_table SET identity_hash=? WHERE id=42", arrayOf(V1DatabaseFixture.LEGACY_IDENTITY)) }
        val before = V1DatabaseFixture.snapshot(file)
        val opened = open(file)
        assertEquals(2, opened.openHelper.writableDatabase.version)
        assertEquals(before.legacy, V1DatabaseFixture.snapshot(file).legacy)
        assertIntegrity(opened)
    }

    @Test fun unknown_version_is_rejected_without_changing_old_facts() = runBlocking {
        val file = v1File()
        modify(file) { version = 99 }
        assertRejectedWithFactsPreserved(file)
    }

    @Test fun version_zero_is_rejected_without_initializing_existing_file() = runBlocking {
        val file = v1File()
        modify(file) { version = 0 }
        assertRejectedWithFactsPreserved(file)
    }

    @Test fun arbitrary_room_identity_is_rejected_without_changing_old_facts() = runBlocking {
        val file = v1File()
        modify(file) { execSQL("UPDATE room_master_table SET identity_hash='foreign-room-identity' WHERE id=42") }
        assertRejectedWithFactsPreserved(file)
    }

    // A known hash cannot authorize an altered real index structure.
    @Test fun known_identity_with_missing_index_is_rejected_before_migration() = runBlocking {
        val file = v1File()
        modify(file) { execSQL("DROP INDEX index_project_money_events_operationId") }
        assertRejectedWithFactsPreserved(file)
    }

    // Room same-version hash acceptance alone does not prove the actual table shape.
    @Test fun known_identity_with_extra_column_is_rejected_before_migration() = runBlocking {
        val file = v1File()
        modify(file) { execSQL("ALTER TABLE project_money_events ADD COLUMN unexpected TEXT") }
        assertRejectedWithFactsPreserved(file)
    }

    @Test fun known_identity_with_extra_business_table_is_rejected_before_migration() = runBlocking {
        val file = v1File()
        modify(file) {
            execSQL("CREATE TABLE foreign_records(id TEXT PRIMARY KEY, value TEXT)")
            execSQL("INSERT INTO foreign_records VALUES('synthetic','preserve')")
        }
        assertRejectedWithFactsPreserved(file)
        modify(file) {
            rawQuery("SELECT value FROM foreign_records WHERE id='synthetic'", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("preserve", cursor.getString(0))
            }
        }
    }

    // Preflight must find broken FK before any v2 DDL/hash/version is committed.
    @Test fun broken_v1_foreign_key_is_rejected_before_migration() = runBlocking {
        val file = v1File()
        modify(file) {
            setForeignKeyConstraintsEnabled(false)
            execSQL("INSERT INTO project_money_events VALUES(?,?,?,?,?,?,?,?)", arrayOf<Any>(
                V1DatabaseFixture.id(990), V1DatabaseFixture.id(991), "OUTLAY", 1L,
                LocalDate.of(2026, 1, 1).toEpochDay(), "orphan fixture", "REGULAR", V1DatabaseFixture.id(992),
            ))
        }
        assertRejectedWithFactsPreserved(file)
    }

    @Test fun committed_v1_wal_is_respected_during_upgrade() = runBlocking {
        val file = File(root, "live-wal.db")
        V1DatabaseFixture.create(file, keepCommittedWal = true).use { source ->
            assertTrue(File(root, "live-wal.db-wal").length() > 32)
            val mainVersion = DataInputStream(file.inputStream()).use { input -> input.skipBytes(60); input.readInt() }
            assertNotEquals("The fixture must retain user_version=1 only in committed WAL", 1, mainVersion)
            assertEquals(1, source.version)
            val before = V1DatabaseFixture.snapshot(source)
            val opened = open(file)
            assertEquals(2, opened.openHelper.writableDatabase.version)
            assertEquals(before.legacy, V1DatabaseFixture.snapshot(file).legacy)
            assertIntegrity(opened)
        }
    }

    @Test fun v1_generation_upgrade_keeps_pointer_ready_settings_zone_and_media() = runBlocking {
        val generationRoot = File(root, "managed").apply { check(mkdir()) }
        val generationId = V1DatabaseFixture.id(999)
        val directory = File(generationRoot, "generations/$generationId").apply { check(mkdirs()) }
        V1DatabaseFixture.create(File(directory, "data.db")).close()
        V1DatabaseFixture.writeMedia(directory)
        val coordinator = LocalWriteCoordinator()
        val settingsFile = File(directory, "settings.preferences_pb")
        val settings = SettingsStore.open(settingsFile, coordinator, ZoneId.of("Asia/Shanghai"))
        settings.close()
        val ready = File(directory, "ready.json").apply {
            writeText(JSONObject().put("formatVersion", 1).put("generationId", generationId).toString())
        }
        val pointer = File(generationRoot, "active-generation").apply {
            writeText(JSONObject().put("formatVersion", 1).put("activeId", generationId)
                .put("previousId", JSONObject.NULL).put("stage", "STABLE").toString())
        }
        val pointerBytes = pointer.readBytes()
        val readyBytes = ready.readBytes()
        val settingsBytes = settingsFile.readBytes()
        val mediaBytes = V1DatabaseFixture.MEDIA_PATHS.associateWith { File(directory, it).readBytes() }
        val oldFacts = V1DatabaseFixture.snapshot(File(directory, "data.db")).legacy
        val opened = DataGenerationStore(context, generationRoot).open(ZoneId.of("America/New_York")).also(generations::add)
        assertEquals(2, opened.database.openHelper.writableDatabase.version)
        assertEquals(generationId, opened.id)
        assertEquals(directory.canonicalFile, opened.directory)
        assertEquals(ZoneId.of("Asia/Shanghai"), opened.settings.recordZone())
        assertArrayEquals(pointerBytes, pointer.readBytes())
        assertArrayEquals(readyBytes, ready.readBytes())
        assertArrayEquals(settingsBytes, settingsFile.readBytes())
        mediaBytes.forEach { (path, bytes) -> assertArrayEquals(bytes, File(directory, path).readBytes()) }
        assertEquals(1, File(generationRoot, "generations").listFiles()!!.size)
        assertEquals(oldFacts, V1DatabaseFixture.snapshot(File(directory, "data.db")).legacy)
        opened.close()
        val reopened = DataGenerationStore(context, generationRoot).open(ZoneId.of("Europe/Paris")).also(generations::add)
        assertEquals(generationId, reopened.id)
        assertEquals(2, reopened.database.openHelper.writableDatabase.version)
        assertEquals(ZoneId.of("Asia/Shanghai"), reopened.settings.recordZone())
        assertArrayEquals(pointerBytes, pointer.readBytes())
        assertArrayEquals(settingsBytes, settingsFile.readBytes())
    }

    // A real CREATE failure after production DDL must roll back all new schema, not just rows.
    @Test fun create_failure_during_real_room_upgrade_rolls_back_v1_schema_and_facts() = runBlocking {
        val file = v1File()
        val before = V1DatabaseFixture.snapshot(file)
        val reachedProductionDdl = AtomicBoolean(false)
        val failingMigration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                DatabaseMigrations.MIGRATION_1_2.migrate(db)
                db.query("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('subscriptions','subscription_renewals','subscription_state_changes')").use { cursor ->
                    check(cursor.moveToFirst() && cursor.getInt(0) == 3)
                }
                reachedProductionDdl.set(true)
                db.execSQL("CREATE TABLE subscriptions(id TEXT NOT NULL PRIMARY KEY)")
            }
        }
        val attempted = Room.databaseBuilder(context, LithicDatabase::class.java, file.path)
            .addMigrations(failingMigration)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
        var rejected = false
        try {
            withContext(Dispatchers.IO) { attempted.openHelper.writableDatabase }
        } catch (_: Exception) {
            rejected = true
        } finally {
            attempted.close()
        }
        assertTrue("The fixture must reach actual production CREATE statements before failing", reachedProductionDdl.get())
        assertTrue("A duplicate CREATE must fail the actual Room upgrade", rejected)
        assertEquals("The old version, identity, schema and every old typed field must roll back", before, V1DatabaseFixture.snapshot(file))
        val successful = open(file)
        assertEquals(2, successful.openHelper.writableDatabase.version)
        assertEquals(before.legacy, V1DatabaseFixture.snapshot(file).legacy)
    }

    // LIKE's '_' must not hide an extra user table that happens to start with sqliteX.
    @Test fun known_v1_hash_rejects_sqliteXforeign_user_table_without_migration() = runBlocking {
        assertSqliteLikeNameRejected(1)
    }

    @Test fun known_v2_hash_rejects_sqliteXforeign_user_table_without_repair() = runBlocking {
        assertSqliteLikeNameRejected(2)
    }

    // table_info alone cannot reveal changed primary-key string comparison semantics.
    @Test fun known_v1_hash_rejects_nocase_primary_key_without_migration() = runBlocking {
        assertNoCasePrimaryKeyRejected(1)
    }

    @Test fun known_v2_hash_rejects_nocase_primary_key_without_repair() = runBlocking {
        assertNoCasePrimaryKeyRejected(2)
    }

    // Reserved platform/Room table names must not hide same-named triggers on business facts.
    @Test fun known_v1_hash_rejects_room_master_table_trigger_without_migration() = runBlocking {
        assertReservedNameTriggerRejected(1, "room_master_table")
    }

    @Test fun known_v2_hash_rejects_room_master_table_trigger_without_repair() = runBlocking {
        assertReservedNameTriggerRejected(2, "room_master_table")
    }

    @Test fun known_v1_hash_rejects_android_metadata_trigger_without_migration() = runBlocking {
        assertReservedNameTriggerRejected(1, "android_metadata")
    }

    @Test fun known_v2_hash_rejects_android_metadata_trigger_without_repair() = runBlocking {
        assertReservedNameTriggerRejected(2, "android_metadata")
    }

    private suspend fun assertReservedNameTriggerRejected(version: Int, name: String) {
        val file = v1File()
        if (version == 2) {
            open(file).close()
            seedRetainedSubscriptionFacts(file)
        }
        val before = V1DatabaseFixture.snapshot(file)
        val originalRows = completeBusinessRows(file)
        assertEquals(version, before.version)
        assertEquals(if (version == 1) V1DatabaseFixture.IDENTITY else "a8fd277b0ef08ce9cf59b704734d0c25", before.identity)
        assertEquals(4, before.legacy.tables.getValue("categories").rows.size)
        assertEquals(3, before.legacy.tables.getValue("long_term_projects").rows.size)
        assertEquals(10, before.legacy.tables.getValue("project_money_events").rows.size)
        assertEquals(3, before.legacy.tables.getValue("local_media_assets").rows.size)
        if (version == 2) {
            listOf("subscriptions", "subscription_renewals", "subscription_state_changes").forEach { table ->
                assertEquals(1, originalRows.getValue(table).rows.size)
            }
        }
        modify(file) {
            execSQL(when (name) {
                "room_master_table" -> "CREATE TRIGGER room_master_table AFTER UPDATE ON categories BEGIN DELETE FROM categories; END"
                "android_metadata" -> "CREATE TRIGGER android_metadata AFTER INSERT ON categories BEGIN DELETE FROM categories; END"
                else -> error("Unsupported reserved-name trigger fixture")
            })
            rawQuery("SELECT tbl_name FROM sqlite_master WHERE type='trigger' AND name=?", arrayOf(name)).use { cursor ->
                assertTrue("The real trigger fixture must exist before production preflight", cursor.moveToFirst())
                assertEquals("categories", cursor.getString(0))
                assertFalse(cursor.moveToNext())
            }
            rawQuery("PRAGMA foreign_key_check", null).use { assertFalse(it.moveToFirst()) }
        }
        val forged = V1DatabaseFixture.snapshot(file)
        assertEquals(before.version, forged.version)
        assertEquals(before.identity, forged.identity)
        assertEquals(before.legacy.tables, forged.legacy.tables)
        assertEquals(originalRows, completeBusinessRows(file))
        assertRejectedWithFactsPreserved(file)
        assertEquals("Rejection must retain every old and subscription typed field", originalRows, completeBusinessRows(file))
    }

    private fun seedRetainedSubscriptionFacts(file: File) = modify(file) {
        setForeignKeyConstraintsEnabled(true)
        beginTransaction()
        try {
            execSQL("INSERT INTO subscriptions VALUES(?,?,?,?,?,?,?)", arrayOf<Any?>(
                V1DatabaseFixture.id(960), "保留订阅", V1DatabaseFixture.id(102), V1DatabaseFixture.id(401), "完整订阅\n备注",
                "2026-01-01T03:04:05.123456789Z", "2026-01-12T03:04:05.987654321Z",
            ))
            execSQL("INSERT INTO subscription_renewals VALUES(?,?,?,?,?,?,?,?,?,?)", arrayOf<Any>(
                V1DatabaseFixture.id(961), V1DatabaseFixture.id(960), LocalDate.of(2026, 1, 1).toEpochDay(),
                LocalDate.of(2026, 1, 2).toEpochDay(), LocalDate.of(2026, 2, 2).toEpochDay(),
                LocalDate.of(2026, 1, 29).toEpochDay(), 1983L, "MONTHLY", 30, V1DatabaseFixture.id(962),
            ))
            execSQL("INSERT INTO subscription_state_changes VALUES(?,?,?,?,?,?,?)", arrayOf<Any?>(
                V1DatabaseFixture.id(963), V1DatabaseFixture.id(960), LocalDate.of(2026, 1, 2).toEpochDay(),
                "ENABLED", null, "保留状态事实", V1DatabaseFixture.id(964),
            ))
            rawQuery("PRAGMA foreign_key_check", null).use { assertFalse(it.moveToFirst()) }
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private fun completeBusinessRows(file: File): Map<String, V1DatabaseFixture.TableRows> = SQLiteDatabase.openDatabase(
        file.path, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
    ).use { raw ->
        V1DatabaseFixture.businessTables(raw).sorted().associateWith { table ->
            raw.rawQuery("SELECT * FROM `$table` ORDER BY id", null).use { cursor ->
                val rows = buildList {
                    while (cursor.moveToNext()) add(cursor.columnNames.indices.map { column ->
                        val type = cursor.getType(column)
                        V1DatabaseFixture.SqlValue(type, when (type) {
                            Cursor.FIELD_TYPE_NULL -> null
                            Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(column).toString()
                            Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(column).toString()
                            Cursor.FIELD_TYPE_STRING -> cursor.getString(column)
                            Cursor.FIELD_TYPE_BLOB -> cursor.getBlob(column).joinToString("") { "%02x".format(it) }
                            else -> error("Unknown SQLite storage class")
                        })
                    })
                }
                V1DatabaseFixture.TableRows(cursor.columnNames.toList(), rows)
            }
        }
    }

    private suspend fun assertSqliteLikeNameRejected(version: Int) {
        val file = v1File()
        if (version == 2) open(file).close()
        val legacyBefore = V1DatabaseFixture.snapshot(file).legacy
        modify(file) {
            execSQL("CREATE TABLE sqliteXforeign(id TEXT PRIMARY KEY,value TEXT NOT NULL)")
            execSQL("INSERT INTO sqliteXforeign VALUES('synthetic','preserve foreign user fact')")
        }
        assertEquals(legacyBefore, V1DatabaseFixture.snapshot(file).legacy)
        assertTrue(V1DatabaseFixture.snapshot(file).businessTables.contains("sqliteXforeign"))
        assertRejectedWithFactsPreserved(file)
        modify(file) {
            rawQuery("SELECT id,value FROM sqliteXforeign", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("synthetic", cursor.getString(0))
                assertEquals("preserve foreign user fact", cursor.getString(1))
                assertFalse(cursor.moveToNext())
            }
        }
    }

    private suspend fun assertNoCasePrimaryKeyRejected(version: Int) {
        val file = v1File()
        if (version == 2) open(file).close()
        val before = V1DatabaseFixture.snapshot(file)
        modify(file) {
            setForeignKeyConstraintsEnabled(false)
            beginTransaction()
            try {
                execSQL("CREATE TABLE categories_nocase(id TEXT COLLATE NOCASE NOT NULL,type TEXT NOT NULL,name TEXT NOT NULL,normalizedName TEXT NOT NULL,isFallback INTEGER NOT NULL,PRIMARY KEY(id))")
                execSQL("INSERT INTO categories_nocase SELECT * FROM categories")
                execSQL("DROP TABLE categories")
                execSQL("ALTER TABLE categories_nocase RENAME TO categories")
                execSQL("CREATE UNIQUE INDEX index_categories_type_normalizedName ON categories(type,normalizedName)")
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
            rawQuery("PRAGMA foreign_key_check", null).use { assertFalse(it.moveToFirst()) }
            val primaryIndex = rawQuery("PRAGMA index_list('categories')", null).use { cursor ->
                var found: String? = null
                while (cursor.moveToNext()) if (cursor.getString(cursor.getColumnIndexOrThrow("origin")) == "pk") {
                    check(found == null)
                    found = cursor.getString(1)
                }
                checkNotNull(found)
            }
            rawQuery("PRAGMA index_xinfo(`$primaryIndex`)", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("id", cursor.getString(2))
                assertEquals("NOCASE", cursor.getString(4))
            }
        }
        val forged = V1DatabaseFixture.snapshot(file)
        assertEquals(before.version, forged.version)
        assertEquals(before.identity, forged.identity)
        assertEquals(before.legacy.tables, forged.legacy.tables)
        assertRejectedWithFactsPreserved(file)
    }

    private fun v1File(): File = File(root, "v1.db").also { V1DatabaseFixture.create(it).close() }
    private suspend fun open(file: File): LithicDatabase = withContext(Dispatchers.IO) {
        LithicDatabase.open(context, file, requireExisting = true)
    }.also(handles::add)

    private fun modify(file: File, action: SQLiteDatabase.() -> Unit) {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.NO_LOCALIZED_COLLATORS).use(action)
    }

    private suspend fun assertRejectedWithFactsPreserved(file: File) {
        val before = V1DatabaseFixture.snapshot(file)
        val originalSchema = completeSchema(file)
        var rejected = false
        try { open(file).close() } catch (_: Exception) { rejected = true }
        assertTrue("An invalid known-version file must fail before normal Room storage is returned", rejected)
        assertEquals("Rejection must preserve version, identity, old typed fields and actual old schema", before, V1DatabaseFixture.snapshot(file))
        assertEquals("Rejection must preserve every original schema object", originalSchema, completeSchema(file))
    }

    private fun completeSchema(file: File): List<List<String?>> = SQLiteDatabase.openDatabase(
        file.path, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
    ).use { raw ->
        raw.rawQuery("SELECT type,name,tbl_name,sql FROM sqlite_master ORDER BY type,name", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add((0..3).map { column -> if (cursor.isNull(column)) null else cursor.getString(column) })
            }
        }
    }

    private fun assertEmptyNewTables(opened: LithicDatabase) {
        listOf("subscriptions", "subscription_renewals", "subscription_state_changes").forEach { table ->
            opened.openHelper.writableDatabase.query("SELECT COUNT(*) FROM `$table`").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0L, cursor.getLong(0))
            }
        }
    }

    private fun assertIntegrity(opened: LithicDatabase) {
        opened.openHelper.writableDatabase.query("PRAGMA quick_check(1)").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("ok", cursor.getString(0))
            assertFalse(cursor.moveToNext())
        }
        opened.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
    }
}
