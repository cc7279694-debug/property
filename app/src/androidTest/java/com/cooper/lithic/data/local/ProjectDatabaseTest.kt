package com.cooper.lithic.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.repository.AddProjectMoneyEventCommand
import com.cooper.lithic.data.repository.CreateProjectCommand
import com.cooper.lithic.data.repository.LedgerReadRepository
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ProjectWriteRepository
import com.cooper.lithic.domain.project.ProjectCostEngine
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic file databases exercise this app's schema/repositories on Android SQLite. */
@RunWith(AndroidJUnit4::class)
class ProjectDatabaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var directory: File
    private lateinit var databaseFile: File
    private lateinit var db: LithicDatabase
    private val coordinator = LocalWriteCoordinator()
    private val today = LocalDate.of(2026, 10, 11)
    private val instant = Instant.parse("2026-10-11T03:04:05.123456789Z")
    private val writer get() = ProjectWriteRepository(db, coordinator, FixedBusinessClock(today))
    private val reader get() = LedgerReadRepository(db, coordinator)

    @Before fun setUp() = runBlocking {
        directory = File(context.noBackupFilesDir, "m04-db-test-${UUID.randomUUID()}")
        check(directory.mkdirs())
        databaseFile = File(directory, "data.db")
        db = withContext(Dispatchers.IO) { LithicDatabase.open(context, databaseFile) }
        db.initializeDefaults()
    }

    @After fun tearDown() {
        if (::db.isInitialized) db.close()
        if (::directory.isInitialized) {
            check(directory.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
            directory.deleteRecursively()
        }
    }

    // M04-T01: removing the event->project FK would let the orphan reach SQLite.
    @Test fun fk_rejects_orphan_event() = runBlocking {
        expectConstraint { db.projectDao().insertEvent(command(1).initialEvent) }
        assertTrue(db.projectDao().listEvents().isEmpty())
    }

    // M04-T02: omitting operationId's unique index would create a second row.
    @Test fun operation_id_is_unique() = runBlocking {
        val original = command(1)
        seedProject(original.project)
        db.projectDao().insertEvent(original.initialEvent)
        expectConstraint { db.projectDao().insertEvent(original.initialEvent.copy(id = id(99))) }
        assertEquals(listOf(original.initialEvent), db.projectDao().listEvents())
    }

    // M04-T03: the duplicate event primary key fails AFTER insertion of the new project.
    @Test fun transaction_rolls_back() = runBlocking {
        val existing = command(1)
        seedProject(existing.project)
        db.projectDao().insertEvent(existing.initialEvent)
        val attempted = command(2).let { it.copy(initialEvent = it.initialEvent.copy(id = existing.initialEvent.id)) }
        assertTrue(writer.createProject(attempted) is MutationResult.Conflict)
        assertEquals(listOf(existing.project), db.projectDao().listProjects())
        assertEquals(listOf(existing.initialEvent), db.projectDao().listEvents())
        assertNull(db.projectDao().findProject(attempted.project.id))
    }

    // M04-T06: omitting either typed fallback leaves one business type without a category.
    @Test fun defaults_are_typed() = runBlocking {
        val defaults = db.categoryDao().list()
        assertEquals(2, defaults.size)
        val project = defaults.single { it.type == CategoryType.PROJECT }
        val subscription = defaults.single { it.type == CategoryType.SUBSCRIPTION }
        assertEquals("其他", project.name)
        assertEquals("其他", subscription.name)
        assertEquals("其他", project.normalizedName)
        assertTrue(project.isFallback && subscription.isFallback)
        assertNotEquals(project.id, subscription.id)
        db.initializeDefaults()
        assertEquals(defaults, db.categoryDao().list())
    }

    @Test fun existing_default_validation_does_not_repair_missing_record() = runBlocking {
        withContext(Dispatchers.IO) { db.openHelper.writableDatabase.execSQL("DELETE FROM categories WHERE id = ?", arrayOf(LithicDatabase.PROJECT_FALLBACK_ID)) }
        var rejected = false
        try { db.requireDefaults() } catch (_: IllegalStateException) { rejected = true }
        assertTrue("Missing defaults in an existing generation are data errors", rejected)
        assertNull(db.categoryDao().find(LithicDatabase.PROJECT_FALLBACK_ID))
    }

    @Test fun existing_default_validation_rejects_changed_identity_semantics() = runBlocking {
        withContext(Dispatchers.IO) { db.openHelper.writableDatabase.execSQL("UPDATE categories SET isFallback=0 WHERE id = ?", arrayOf(LithicDatabase.PROJECT_FALLBACK_ID)) }
        var rejected = false
        try { db.requireDefaults() } catch (_: IllegalStateException) { rejected = true }
        assertTrue(rejected)
        assertFalse(db.categoryDao().find(LithicDatabase.PROJECT_FALLBACK_ID)!!.isFallback)
    }

    @Test fun project_and_initial_event_are_saved_together() = runBlocking {
        val input = command(1)
        assertEquals(MutationResult.Success(id(11), id(21), false), writer.createProject(input))
        val snapshot = reader.snapshot()
        assertEquals(listOf(input.project), snapshot.projects)
        assertEquals(listOf(input.initialEvent), snapshot.moneyEvents)
        assertEquals(2, snapshot.categories.size)
    }

    @Test fun exact_command_retry_returns_existing_ids_without_second_payment() = runBlocking {
        val input = command(1)
        assertEquals(MutationResult.Success(id(11), id(21), false), writer.createProject(input))
        assertEquals(MutationResult.Success(id(11), id(21), true), writer.createProject(input))
        assertEquals(1, db.projectDao().listProjects().size)
        assertEquals(1, db.projectDao().listEvents().size)
    }

    @Test fun concurrent_identical_commands_create_only_one_payment() = runBlocking {
        val input = command(1)
        val results = (1..12).map { async(Dispatchers.IO) { writer.createProject(input) } }.awaitAll()
        assertEquals(1, results.count { it == MutationResult.Success(id(11), id(21), false) })
        assertEquals(11, results.count { it == MutationResult.Success(id(11), id(21), true) })
        assertEquals(1, db.projectDao().listEvents().size)
    }

    @Test fun reused_command_with_different_project_or_event_is_conflict() = runBlocking {
        val original = command(1)
        assertTrue(writer.createProject(original) is MutationResult.Success)
        assertTrue(writer.createProject(original.copy(project = original.project.copy(name = "Changed"))) is MutationResult.Conflict)
        assertTrue(writer.createProject(original.copy(initialEvent = original.initialEvent.copy(amountCents = 201))) is MutationResult.Conflict)
        assertTrue(writer.createProject(original.copy(project = original.project.copy(updatedAt = instant.plusNanos(1)))) is MutationResult.Conflict)
        assertEquals(listOf(original.project), db.projectDao().listProjects())
        assertEquals(listOf(original.initialEvent), db.projectDao().listEvents())
    }

    @Test fun subscription_category_cannot_be_used_for_project() = runBlocking {
        val input = command(1).let { it.copy(project = it.project.copy(categoryId = LithicDatabase.SUBSCRIPTION_FALLBACK_ID)) }
        assertTrue(writer.createProject(input) is MutationResult.ValidationFailure)
        assertTrue(db.projectDao().listProjects().isEmpty())
        assertTrue(db.projectDao().listEvents().isEmpty())
    }

    @Test fun normalized_names_are_unique_within_type_but_not_across_types() = runBlocking {
        val category = CategoryEntity(id(50), CategoryType.PROJECT, "工具", "工具", false)
        db.categoryDao().insert(category)
        expectConstraint { db.categoryDao().insert(category.copy(id = id(51), name = " 工具 ")) }
        db.categoryDao().insert(category.copy(id = id(52), type = CategoryType.SUBSCRIPTION))
        assertEquals(2, db.categoryDao().list().count { it.normalizedName == "工具" })
    }

    @Test fun unicode_and_whitespace_normalization_enforces_real_category_uniqueness() = runBlocking {
        assertEquals("tool box", CategoryNames.normalized("  ＴＯＯＬ\u00a0  Box  "))
        val first = CategoryEntity(id(50), CategoryType.PROJECT, "Tool Box", CategoryNames.normalized("Tool Box"), false)
        db.categoryDao().insert(first)
        expectConstraint { db.categoryDao().insert(first.copy(id = id(51), name = "ＴＯＯＬ  BOX", normalizedName = CategoryNames.normalized("ＴＯＯＬ  BOX"))) }
        assertEquals(1, db.categoryDao().list().count { it.normalizedName == "tool box" })
    }

    @Test fun zero_initial_amount_and_real_negative_net_cost_are_valid() = runBlocking {
        val input = command(1).let { it.copy(initialEvent = it.initialEvent.copy(amountCents = 0)) }
        assertTrue(writer.createProject(input) is MutationResult.Success)
        val recovery = ProjectMoneyEventEntity(id(70), input.project.id, MoneyDirection.RECOVERY, 300, today, "真实退款", ProjectMoneyEventRole.REGULAR, id(71))
        assertTrue(writer.addMoneyEvent(AddProjectMoneyEventCommand(id(71), recovery)) is MutationResult.Success)
        val events = reader.snapshot().moneyEvents
        assertEquals(2, events.size)
        val result = ProjectCostEngine.evaluate(ProjectSnapshot(LongTermProject(input.project.startedOn), events.map { ProjectMoneyEvent(it.direction, Money(it.amountCents), it.occurredOn) }), today)
        assertEquals(-300L, result.netCents)
    }

    @Test fun dates_and_nanosecond_instants_survive_file_reopen() = runBlocking {
        val input = command(1).let { it.copy(project = it.project.copy(startedOn = LocalDate.of(-40000000, 2, 29), endedOn = LocalDate.of(30000000, 3, 1), endReason = "完成", updatedAt = instant.plusNanos(8))) }
        assertTrue(writer.createProject(input) is MutationResult.Success)
        db.close()
        db = withContext(Dispatchers.IO) { LithicDatabase.open(context, databaseFile, requireExisting = true) }
        assertEquals(input.project, db.projectDao().findProject(id(11)))
        assertEquals(input.initialEvent, db.projectDao().findEventByOperation(id(31)))
        assertEquals(123456789, db.projectDao().findProject(id(11))!!.createdAt.nano)
        assertEquals(123456797, db.projectDao().findProject(id(11))!!.updatedAt.nano)
    }

    @Test fun amount_and_uuid_and_future_payment_validation_preserve_empty_ledger() = runBlocking {
        val valid = command(1)
        val invalid = listOf(
            valid.copy(commandId = "not-a-uuid", initialEvent = valid.initialEvent.copy(operationId = "not-a-uuid")),
            valid.copy(project = valid.project.copy(id = "bad-id"), initialEvent = valid.initialEvent.copy(projectId = "bad-id")),
            valid.copy(initialEvent = valid.initialEvent.copy(amountCents = -1)),
            valid.copy(initialEvent = valid.initialEvent.copy(amountCents = 1_000_000_000_000L)),
            valid.copy(initialEvent = valid.initialEvent.copy(occurredOn = today.plusDays(1))),
            valid.copy(initialEvent = valid.initialEvent.copy(operationId = id(80))),
        )
        invalid.forEach { assertTrue("Input must be rejected: $it", writer.createProject(it) is MutationResult.ValidationFailure) }
        assertTrue(db.projectDao().listProjects().isEmpty())
        assertTrue(db.projectDao().listEvents().isEmpty())
    }

    @Test fun event_relationship_initial_role_and_lifecycle_are_validated() = runBlocking {
        val valid = command(1)
        val invalid = listOf(
            valid.copy(initialEvent = valid.initialEvent.copy(projectId = id(88))),
            valid.copy(initialEvent = valid.initialEvent.copy(direction = MoneyDirection.RECOVERY)),
            valid.copy(initialEvent = valid.initialEvent.copy(role = ProjectMoneyEventRole.REGULAR)),
            valid.copy(project = valid.project.copy(endedOn = valid.project.startedOn.minusDays(1), endReason = "结束")),
            valid.copy(project = valid.project.copy(endReason = "结束")),
            valid.copy(project = valid.project.copy(endedOn = today)),
            valid.copy(project = valid.project.copy(updatedAt = instant.minusNanos(1))),
            valid.copy(project = valid.project.copy(name = "  ")),
            valid.copy(project = valid.project.copy(categoryId = id(87))),
            valid.copy(project = valid.project.copy(coverAssetId = id(86))),
        )
        invalid.forEach { assertTrue("Input must be rejected: $it", writer.createProject(it) is MutationResult.ValidationFailure) }
        assertTrue(db.projectDao().listProjects().isEmpty())
    }

    @Test fun additional_event_requires_existing_project_and_matching_operation() = runBlocking {
        val event = command(1).initialEvent.copy(role = ProjectMoneyEventRole.REGULAR)
        assertTrue(writer.addMoneyEvent(AddProjectMoneyEventCommand(event.operationId, event)) is MutationResult.ValidationFailure)
        assertTrue(db.projectDao().listEvents().isEmpty())
    }

    @Test fun additional_event_retry_and_conflict_preserve_original_payment() = runBlocking {
        assertTrue(writer.createProject(command(1)) is MutationResult.Success)
        val event = ProjectMoneyEventEntity(id(70), id(11), MoneyDirection.RECOVERY, 250, today, "退款", ProjectMoneyEventRole.REGULAR, id(71))
        assertEquals(MutationResult.Success(id(11), id(70), false), writer.addMoneyEvent(AddProjectMoneyEventCommand(id(71), event)))
        assertEquals(MutationResult.Success(id(11), id(70), true), writer.addMoneyEvent(AddProjectMoneyEventCommand(id(71), event)))
        assertTrue(writer.addMoneyEvent(AddProjectMoneyEventCommand(id(71), event.copy(description = "Changed"))) is MutationResult.Conflict)
        assertEquals(event, db.projectDao().findEventByOperation(id(71)))
        assertEquals(2, db.projectDao().listEvents().size)
    }

    @Test fun maximum_amount_and_real_prepayment_before_ownership_are_valid() = runBlocking {
        val input = command(1).let { it.copy(initialEvent = it.initialEvent.copy(amountCents = 999_999_999_999L, occurredOn = LocalDate.of(2026, 1, 1))) }
        assertTrue(writer.createProject(input) is MutationResult.Success)
        assertEquals(999_999_999_999L, db.projectDao().listEvents().single().amountCents)
        assertEquals(LocalDate.of(2026, 1, 1), db.projectDao().listEvents().single().occurredOn)
    }

    @Test fun real_settlement_after_end_does_not_reopen_project() = runBlocking {
        val input = command(1).let { it.copy(project = it.project.copy(endedOn = LocalDate.of(2026, 10, 2), endReason = "售出")) }
        assertTrue(writer.createProject(input) is MutationResult.Success)
        val settlement = ProjectMoneyEventEntity(id(70), id(11), MoneyDirection.RECOVERY, 350, today, "尾款", ProjectMoneyEventRole.END_SETTLEMENT, id(71))
        assertTrue(writer.addMoneyEvent(AddProjectMoneyEventCommand(id(71), settlement)) is MutationResult.Success)
        assertEquals(LocalDate.of(2026, 10, 2), db.projectDao().findProject(id(11))!!.endedOn)
        assertEquals(2, db.projectDao().listEvents().size)
    }

    @Test fun snapshot_reads_all_four_real_tables_and_cover_relationship() = runBlocking {
        val asset = LocalMediaAssetEntity(id(90), "media/cover.webp", "image/webp", 10, 20, 30L, "a".repeat(64))
        db.mediaDao().insert(asset)
        val input = command(1).let { it.copy(project = it.project.copy(coverAssetId = asset.id)) }
        assertTrue(writer.createProject(input) is MutationResult.Success)
        val snapshot = reader.snapshot()
        assertEquals(listOf(asset), snapshot.mediaAssets)
        assertEquals(id(90), snapshot.projects.single().coverAssetId)
        assertEquals(id(11), snapshot.moneyEvents.single().projectId)
        assertEquals(2, snapshot.categories.size)
    }

    @Test fun snapshot_waits_for_shared_write_coordinator() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val writing = launch(Dispatchers.IO) { coordinator.withWrite { entered.complete(Unit); release.await() } }
        entered.await()
        val reading = async(Dispatchers.IO) { reader.snapshot() }
        try {
            delay(100)
            assertFalse("A snapshot must wait until the coordinated write finishes", reading.isCompleted)
        } finally {
            release.complete(Unit)
            writing.join()
        }
        assertEquals(2, reading.await().categories.size)
    }

    @Test fun concurrent_snapshots_never_expose_project_without_initial_event() = runBlocking {
        val writes = launch(Dispatchers.IO) {
            for (index in 1..20) assertTrue(writer.createProject(command(index)) is MutationResult.Success)
        }
        repeat(30) {
            val snapshot = reader.snapshot()
            assertEquals(snapshot.projects.size, snapshot.moneyEvents.size)
            assertEquals(snapshot.projects.map { project -> project.id }.toSet(), snapshot.moneyEvents.map { event -> event.projectId }.toSet())
        }
        writes.join()
        assertEquals(20, reader.snapshot().projects.size)
    }

    @Test fun room_snapshot_keeps_same_view_across_independent_sqlite_commit() = runBlocking {
        assertTrue(writer.createProject(command(1)) is MutationResult.Success)
        db.close()
        val beforeEvents = CountDownLatch(1)
        val continueRead = CountDownLatch(1)
        val writingStarted = CountDownLatch(1)
        val committed = CountDownLatch(1)
        // A synchronous test-only query observer pauses between the repository's two reads.
        db = Room.databaseBuilder(context, LithicDatabase::class.java, databaseFile.path)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .setQueryCallback({ sql, _ ->
                if (sql.startsWith("SELECT * FROM project_money_events")) {
                    beforeEvents.countDown()
                    check(continueRead.await(10, TimeUnit.SECONDS))
                }
            }, { task -> task.run() })
            .build()
        val reading = async(Dispatchers.IO) { reader.snapshot() }
        var independentWrite: kotlinx.coroutines.Job? = null
        try {
            assertTrue(withContext(Dispatchers.IO) { beforeEvents.await(10, TimeUnit.SECONDS) })
            val second = command(2)
            independentWrite = launch(Dispatchers.IO) {
                android.database.sqlite.SQLiteDatabase.openDatabase(databaseFile.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE or android.database.sqlite.SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING).use { independent ->
                    writingStarted.countDown()
                    independent.beginTransaction()
                    try {
                        independent.execSQL("INSERT INTO long_term_projects (id,name,categoryId,startedOn,endedOn,endReason,coverAssetId,note,createdAt,updatedAt) VALUES (?,?,?,?,NULL,NULL,NULL,NULL,?,?)", arrayOf<Any>(second.project.id, second.project.name, second.project.categoryId, 20727L, "2026-10-11T03:04:05.123456789Z", "2026-10-11T03:04:05.123456789Z"))
                        independent.execSQL("INSERT INTO project_money_events (id,projectId,direction,amountCents,occurredOn,description,role,operationId) VALUES (?,?,?,?,?,?,?,?)", arrayOf<Any>(second.initialEvent.id, second.project.id, "OUTLAY", 200L, 20727L, "初始投入", "INITIAL", second.commandId))
                        independent.setTransactionSuccessful()
                    } finally { independent.endTransaction() }
                }
                committed.countDown()
            }
            assertTrue(withContext(Dispatchers.IO) { writingStarted.await(10, TimeUnit.SECONDS) })
            // A deferred read tx preserves its old view while WAL commits. A legacy
            // Room tx may instead hold the writer until its read is complete.
            withContext(Dispatchers.IO) { committed.await(1, TimeUnit.SECONDS) }
        } finally { continueRead.countDown() }
        val snapshot = reading.await()
        assertEquals(1, snapshot.projects.size)
        assertEquals(1, snapshot.moneyEvents.size)
        assertEquals(id(11), snapshot.moneyEvents.single().projectId)
        independentWrite.join()
        assertEquals(2, db.projectDao().listProjects().size)
        assertEquals(2, db.projectDao().listEvents().size)
    }

    @Test fun schema_v1_has_only_four_business_tables() = runBlocking {
        // Preserve this M04 method's frozen-v1 meaning after the live Room owner evolves.
        // M08 separately verifies fresh/current v2 has all seven business tables.
        withContext(Dispatchers.IO) {
            V1DatabaseFixture.create(File(directory, "frozen-v1-schema.db")).use { original ->
                assertEquals(1, original.version)
                assertEquals(setOf("categories", "long_term_projects", "project_money_events", "local_media_assets"), V1DatabaseFixture.businessTables(original))
            }
        }
    }

    @Test fun require_existing_database_never_creates_missing_file() = runBlocking {
        val missing = File(directory, "missing.db")
        var rejected = false
        try { withContext(Dispatchers.IO) { LithicDatabase.open(context, missing, requireExisting = true) }.close() }
        catch (_: Exception) { rejected = true }
        assertTrue(rejected)
        assertFalse(missing.exists())
    }

    @Test fun corrupt_existing_database_is_rejected_without_deleting_original() = runBlocking {
        val corrupt = File(directory, "corrupt.db")
        val original = "This is synthetic corrupt SQLite data, not a user database".toByteArray()
        corrupt.writeBytes(original)
        var rejected = false
        try { withContext(Dispatchers.IO) { LithicDatabase.open(context, corrupt, requireExisting = true) }.close() }
        catch (_: Exception) { rejected = true }
        assertTrue("Opening corrupt existing storage must fail before returning a handle", rejected)
        assertTrue(corrupt.isFile)
        assertTrue(original.contentEquals(corrupt.readBytes()))
    }

    @Test fun sqlite_page_corruption_preserves_original_through_real_open_handler() = runBlocking {
        // Retain the SQLite magic so the failure passes our header precheck and
        // reaches SQLite's page/schema opening and corruption handling boundary.
        db.close()
        val corruptedBytes = databaseFile.readBytes()
        assertTrue(corruptedBytes.size > 100)
        assertTrue("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII).contentEquals(corruptedBytes.copyOfRange(0, 16)))
        corruptedBytes[100] = 0x7f.toByte() // Invalid page-one b-tree page type.
        databaseFile.writeBytes(corruptedBytes)
        var rejected = false
        try { withContext(Dispatchers.IO) { LithicDatabase.open(context, databaseFile, requireExisting = true) }.close() }
        catch (_: Exception) { rejected = true }
        assertTrue("SQLite page corruption must fail without recovery to a new ledger", rejected)
        assertTrue(databaseFile.isFile)
        assertEquals(corruptedBytes.size.toLong(), databaseFile.length())
        assertTrue(corruptedBytes.contentEquals(databaseFile.readBytes()))
    }

    @Test fun empty_existing_database_is_rejected_without_creating_empty_ledger() = runBlocking {
        val empty = File(directory, "empty.db")
        empty.writeBytes(byteArrayOf())
        var rejected = false
        try { withContext(Dispatchers.IO) { LithicDatabase.open(context, empty, requireExisting = true) }.close() }
        catch (_: Exception) { rejected = true }
        assertTrue(rejected)
        assertTrue(empty.isFile)
        assertEquals(0L, empty.length())
    }

    @Test fun unknown_schema_version_is_rejected_without_destructive_fallback() = runBlocking {
        seedProject(command(1).project)
        withContext(Dispatchers.IO) { db.openHelper.writableDatabase.version = 99 }
        db.close()
        var rejected = false
        try { withContext(Dispatchers.IO) { LithicDatabase.open(context, databaseFile, requireExisting = true) }.close() }
        catch (_: Exception) { rejected = true }
        assertTrue("Unregistered migration must not return a usable new ledger", rejected)
        assertTrue(databaseFile.isFile)
        withContext(Dispatchers.IO) {
            android.database.sqlite.SQLiteDatabase.openDatabase(databaseFile.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY).use { original ->
                original.rawQuery("SELECT name FROM long_term_projects", null).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("项目1", cursor.getString(0))
                }
            }
        }
    }

    @Test fun existing_version_zero_foreign_database_is_not_initialized_in_place() = runBlocking {
        assertForeignDatabasePreserved(0)
    }

    @Test fun existing_version_one_foreign_database_is_not_adopted() = runBlocking {
        assertForeignDatabasePreserved(1)
    }

    @Test fun existing_schema_is_read_from_live_wal_before_opening_room() = runBlocking {
        val original = command(1)
        assertTrue(writer.createProject(original) is MutationResult.Success)
        // The first Room connection remains open; read-only preflight must read WAL,
        // not infer user_version from the main file's potentially older header.
        val reopened = withContext(Dispatchers.IO) {
            LithicDatabase.open(context, databaseFile, requireExisting = true)
        }
        try {
            assertEquals(listOf(original.project), reopened.projectDao().listProjects())
            assertEquals(listOf(original.initialEvent), reopened.projectDao().listEvents())
        } finally {
            reopened.close()
        }
    }

    private suspend fun assertForeignDatabasePreserved(version: Int) = withContext(Dispatchers.IO) {
        val foreign = File(directory, "foreign-$version.db")
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(foreign, null).use { original ->
            original.execSQL("CREATE TABLE original_record(value TEXT NOT NULL)")
            original.execSQL("INSERT INTO original_record VALUES ('synthetic preserved record')")
            original.version = version
        }
        val before = foreign.readBytes()
        var rejected = false
        try { LithicDatabase.open(context, foreign, requireExisting = true).close() }
        catch (_: Exception) { rejected = true }
        assertTrue("An existing foreign/version-zero database must fail before Room onCreate", rejected)
        assertTrue("The existing database bytes must remain intact", before.contentEquals(foreign.readBytes()))
        android.database.sqlite.SQLiteDatabase.openDatabase(foreign.path, null,
            android.database.sqlite.SQLiteDatabase.OPEN_READONLY).use { original ->
            assertEquals(version, original.version)
            original.rawQuery("SELECT value FROM original_record", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("synthetic preserved record", cursor.getString(0))
            }
            original.rawQuery("SELECT count(*) FROM sqlite_master WHERE name IN ('categories','long_term_projects','project_money_events','local_media_assets','room_master_table')", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private suspend fun seedProject(project: LongTermProjectEntity) {
        if (db.categoryDao().find(project.categoryId) == null) {
            db.categoryDao().insert(CategoryEntity(project.categoryId, CategoryType.PROJECT, "其他", "其他", true))
        }
        db.projectDao().insertProject(project)
    }

    private suspend fun expectConstraint(block: suspend () -> Unit) {
        try { block(); fail("Expected a real SQLite constraint rejection") }
        catch (_: SQLiteConstraintException) { /* The original row must remain unchanged. */ }
    }

    private fun command(index: Int): CreateProjectCommand {
        val project = LongTermProjectEntity(id(10 + index), "项目$index", LithicDatabase.PROJECT_FALLBACK_ID, LocalDate.of(2026, 10, 1), createdAt = instant, updatedAt = instant)
        return CreateProjectCommand(id(30 + index), project, ProjectMoneyEventEntity(id(20 + index), project.id, MoneyDirection.OUTLAY, 200L, LocalDate.of(2026, 10, 1), "初始投入", ProjectMoneyEventRole.INITIAL, id(30 + index)))
    }

    private fun id(value: Int): String = "10000000-0000-4000-8000-${value.toString().padStart(12, '0')}"
}
