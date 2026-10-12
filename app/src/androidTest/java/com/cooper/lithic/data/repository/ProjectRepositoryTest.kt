package com.cooper.lithic.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.core.time.ZonedBusinessClock
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import java.io.File
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real synthetic file databases; no production records or old test sources are changed. */
@RunWith(AndroidJUnit4::class)
class ProjectRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var directory: File
    private lateinit var databaseFile: File
    private lateinit var db: LithicDatabase
    private val coordinator = LocalWriteCoordinator()
    private val today = LocalDate.of(2026, 1, 10)
    private val instant = Instant.parse("2026-01-10T03:04:05.123456789Z")
    private val queries = CopyOnWriteArrayList<String>()
    @Volatile private var queryGate: ((String) -> Unit)? = null
    private val repository get() = ProjectRepository(db, coordinator, FixedBusinessClock(today))

    @Before fun setUp() = runBlocking {
        directory = File(context.noBackupFilesDir, "m06-repository-test-${UUID.randomUUID()}")
        check(directory.mkdirs())
        databaseFile = File(directory, "data.db")
        db = withContext(Dispatchers.IO) {
            Room.databaseBuilder(context, LithicDatabase::class.java, databaseFile.path)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .setQueryCallback({ sql, _ -> queries += sql; queryGate?.invoke(sql) }, { work -> work.run() })
                .build()
        }
        db.initializeDefaults()
    }

    @After fun tearDown() {
        if (::db.isInitialized) db.close()
        if (::directory.isInitialized) {
            check(directory.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
            directory.deleteRecursively()
        }
    }

    // M06-T01: replacing the atomic writer or omitting persisted reads loses these rows.
    @Test fun create_zero_and_normal_project_survives_reopen() = runBlocking {
        val zero = createCommand(1, amount = 0)
        val normal = createCommand(2, amount = 100_000)
        assertTrue(repository.createProject(zero) is MutationResult.Success)
        assertTrue(repository.createProject(normal) is MutationResult.Success)
        reopen()
        val first = repository.loadProjectDetail(zero.project.id)
        val second = repository.loadProjectDetail(normal.project.id)
        assertNotNull(first)
        assertNotNull(second)
        assertEquals(0L, first!!.cost.netCents)
        assertEquals(100_000L, second!!.cost.netCents)
        assertEquals(zero.project, first.project)
        assertEquals(normal.initialEvent, second.events.single())
        assertEquals(2L, repository.loadSummary().projectCount)
    }

    // M06-T02: counting a duplicate purchase price would produce 2000, not 1000 yuan.
    @Test fun initial_cost_counted_once_after_replay_and_reopen() = runBlocking {
        val input = createCommand(1)
        assertTrue(repository.createProject(input) is MutationResult.Success)
        assertEquals(MutationResult.Success(input.project.id, input.initialEvent.id, true), repository.createProject(input))
        reopen()
        val detail = repository.loadProjectDetail(input.project.id)
        assertNotNull(detail)
        assertEquals(1, detail!!.events.size)
        assertEquals(100_000L, detail.cost.grossOutlayCents)
        assertEquals(10L, detail.cost.heldDays)
        assertRate("100", detail.cost.dailyRate)
    }

    // A committed operation whose return value was lost must replay the same identities.
    @Test fun lost_create_result_replays_original_identity_without_second_payment() = runBlocking {
        val input = createCommand(1)
        repository.createProject(input) // Deliberately discard the already committed response.
        reopen()
        assertEquals(MutationResult.Success(input.project.id, input.initialEvent.id, true), repository.createProject(input))
        assertEquals(listOf(input.project), db.projectDao().listProjects())
        assertEquals(listOf(input.initialEvent), db.projectDao().listEvents())
    }

    // M06-T03: regenerating operation identities in the facade duplicates payments.
    @Test fun concurrent_create_and_append_replay_without_duplicate_money() = runBlocking {
        val input = createCommand(1)
        val created = (1..12).map { async(Dispatchers.IO) { repository.createProject(input) } }.awaitAll()
        assertEquals(1, created.count { it == MutationResult.Success(input.project.id, input.initialEvent.id, false) })
        val extra = moneyCommand(100, input.project.id, MoneyDirection.OUTLAY, 20_000, LocalDate.of(2026, 1, 5))
        val appended = (1..12).map { async(Dispatchers.IO) { repository.appendMoneyEvent(extra) } }.awaitAll()
        assertEquals(1, appended.count { it == MutationResult.Success(input.project.id, extra.event.id, false) })
        assertEquals(11, appended.count { it == MutationResult.Success(input.project.id, extra.event.id, true) })
        assertEquals(1, db.projectDao().listProjects().size)
        assertEquals(2, db.projectDao().listEvents().size)
    }

    @Test fun same_money_operation_with_changed_content_is_conflict() = runBlocking {
        val input = createCommand(1)
        assertTrue(repository.createProject(input) is MutationResult.Success)
        val extra = moneyCommand(100, input.project.id, MoneyDirection.RECOVERY, 30_000, today)
        assertTrue(repository.appendMoneyEvent(extra) is MutationResult.Success)
        assertTrue(repository.appendMoneyEvent(extra.copy(event = extra.event.copy(amountCents = 40_000))) is MutationResult.Conflict)
        assertEquals(2, db.projectDao().listEvents().size)
    }

    @Test fun invalid_create_and_future_money_leave_real_database_unchanged() = runBlocking {
        val original = createCommand(1)
        val wrongCategory = original.copy(project = original.project.copy(categoryId = LithicDatabase.SUBSCRIPTION_FALLBACK_ID))
        assertTrue(repository.createProject(wrongCategory) is MutationResult.ValidationFailure)
        assertTrue(db.projectDao().listProjects().isEmpty())
        assertTrue(repository.createProject(original) is MutationResult.Success)
        val future = moneyCommand(100, original.project.id, MoneyDirection.OUTLAY, 100, today.plusDays(1))
        assertTrue(repository.appendMoneyEvent(future) is MutationResult.ValidationFailure)
        assertEquals(listOf(original.initialEvent), db.projectDao().listEvents())
    }

    // Limit the synthetic SQLite file, so a genuine SQLiteFullException reaches the writer.
    @Test fun real_sqlite_full_failure_rolls_back_and_same_command_retries_safely() = runBlocking {
        val existing = createCommand(1)
        assertTrue(repository.createProject(existing) is MutationResult.Success)
        // Exercise the production opener; query telemetry is not part of this failure fixture.
        reopen()
        // A write transaction pins Android SQLite's primary connection. A naked rawQuery
        // can configure a pooled reader and leave the writer's max_page_count unchanged.
        val imposedLimit = db.withTransaction {
            val sqlite = db.openHelper.writableDatabase
            val pageCount = sqlite.query("PRAGMA page_count").use { cursor -> check(cursor.moveToFirst()); cursor.getLong(0) }
            sqlite.query("PRAGMA max_page_count = $pageCount").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(pageCount, cursor.getLong(0))
            }
            pageCount
        }
        val attempted = createCommand(2).let { it.copy(project = it.project.copy(name = "合成存储上限测试".repeat(20_000))) }
        try {
            val failure = repository.createProject(attempted)
            assertTrue("Real SQLite page exhaustion must be a typed storage failure: $failure", failure is MutationResult.StorageFailure)
            val storageFailure = failure as MutationResult.StorageFailure
            // In this controlled max_page_count case, SQLite auto-abort can leave Room's
            // rollback with SQLITE_ERROR. Preserve the honest generic failure rather than
            // reclassifying production errors by message; all atomicity/retry checks remain.
            assertTrue(
                "Production opener without QueryCallback; actual storage failure: $failure; imposed page limit: $imposedLimit",
                storageFailure.kind == StorageFailureKind.DISK_FULL ||
                    (storageFailure.kind == StorageFailureKind.DATABASE &&
                        storageFailure.message.contains("cannot rollback - no transaction is active", ignoreCase = true)),
            )
            assertEquals(listOf(existing.project), db.projectDao().listProjects())
            assertEquals(listOf(existing.initialEvent), db.projectDao().listEvents())
        } finally {
            db.withTransaction {
                db.openHelper.writableDatabase.query("PRAGMA max_page_count = 2147483646").use { cursor -> check(cursor.moveToFirst()) }
            }
        }
        assertEquals(MutationResult.Success(attempted.project.id, attempted.initialEvent.id, false), repository.createProject(attempted))
        assertEquals(MutationResult.Success(attempted.project.id, attempted.initialEvent.id, true), repository.createProject(attempted))
        assertEquals(2, db.projectDao().listProjects().size)
        assertEquals(2, db.projectDao().listEvents().size)
    }

    // Device timezone changes must not change the injected record-zone date boundary.
    @Test fun persisted_business_zone_boundary_ignores_device_timezone_changes() = runBlocking {
        val previousZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            val business = ZonedBusinessClock(Clock.fixed(Instant.parse("2026-01-10T16:30:00Z"), ZoneId.of("Asia/Shanghai")))
            val zonedRepository = ProjectRepository(db, coordinator, business)
            val input = createCommand(1).let { it.copy(initialEvent = it.initialEvent.copy(occurredOn = LocalDate.of(2026, 1, 11))) }
            assertTrue(zonedRepository.createProject(input) is MutationResult.Success)
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            val future = moneyCommand(100, input.project.id, MoneyDirection.OUTLAY, 100, LocalDate.of(2026, 1, 12))
            assertTrue(zonedRepository.appendMoneyEvent(future) is MutationResult.ValidationFailure)
            assertEquals(1, db.projectDao().listEvents().size)
        } finally { TimeZone.setDefault(previousZone) }
    }

    @Test fun recovery_exceeding_outlay_produces_real_negative_cost() = runBlocking {
        val input = createCommand(1, amount = 0)
        assertTrue(repository.createProject(input) is MutationResult.Success)
        assertTrue(repository.appendMoneyEvent(moneyCommand(100, input.project.id, MoneyDirection.RECOVERY, 30_000, today)) is MutationResult.Success)
        val detail = repository.loadProjectDetail(input.project.id)
        assertNotNull(detail)
        assertEquals(0L, detail!!.cost.grossOutlayCents)
        assertEquals(30_000L, detail.cost.recoveryCents)
        assertEquals(-30_000L, detail.cost.netCents)
        assertRate("-30", detail.cost.dailyRate)
    }

    @Test fun real_prepayment_before_ownership_has_no_fake_zero_daily_cost() = runBlocking {
        val input = createCommand(1, start = LocalDate.of(2026, 1, 20), paidOn = LocalDate.of(2026, 1, 5))
        assertTrue(repository.createProject(input) is MutationResult.Success)
        val detail = repository.loadProjectDetail(input.project.id)
        assertNotNull(detail)
        assertEquals(100_000L, detail!!.cost.netCents)
        assertEquals(0L, detail.cost.heldDays)
        assertNull(detail.cost.dailyRate)
        assertFalse(detail.cost.contributesToCurrentTotal)
    }

    // M06-T05: reading only cached/newly-entered totals misses backdated facts.
    @Test fun backdated_money_recalculates_detail_page_and_observation_day() = runBlocking {
        val input = createCommand(1)
        assertTrue(repository.createProject(input) is MutationResult.Success)
        assertTrue(repository.appendMoneyEvent(moneyCommand(100, input.project.id, MoneyDirection.OUTLAY, 20_000, LocalDate.of(2026, 1, 5))) is MutationResult.Success)
        val now = repository.loadProjectDetail(input.project.id)
        val before = repository.loadProjectDetail(input.project.id, LocalDate.of(2026, 1, 4))
        assertNotNull(now)
        assertNotNull(before)
        assertRate("120", now!!.cost.dailyRate)
        assertRate("250", before!!.cost.dailyRate)
        assertEquals(100_000L, before.cost.netCents)
        assertEquals(now.cost, repository.loadProjectPage().items.single().cost)
        assertEquals(1L, repository.loadSummary().projectCount)
        reopen()
        assertEquals(now.cost, repository.loadProjectDetail(input.project.id)!!.cost)
    }

    @Test fun same_day_events_are_all_preserved_in_stable_id_order() = runBlocking {
        val input = createCommand(1)
        assertTrue(repository.createProject(input) is MutationResult.Success)
        val second = moneyCommand(102, input.project.id, MoneyDirection.OUTLAY, 20_000, LocalDate.of(2026, 1, 5))
        val first = moneyCommand(101, input.project.id, MoneyDirection.RECOVERY, 5_000, LocalDate.of(2026, 1, 5))
        assertTrue(repository.appendMoneyEvent(second) is MutationResult.Success)
        assertTrue(repository.appendMoneyEvent(first) is MutationResult.Success)
        val detail = repository.loadProjectDetail(input.project.id)
        assertNotNull(detail)
        assertEquals(listOf(input.initialEvent.id, first.event.id, second.event.id), detail!!.events.map { it.id })
        assertEquals(115_000L, detail.cost.netCents)
    }

    // A whole-row replacement sourced from a partial form would erase protected fields.
    @Test fun metadata_edit_preserves_created_end_cover_and_old_money_events() = runBlocking {
        val asset = LocalMediaAssetEntity(id(900), "media/synthetic.webp", "image/webp", 10, 10, 100, "a".repeat(64))
        db.mediaDao().insert(asset)
        val input = createCommand(1).let { it.copy(project = it.project.copy(endedOn = LocalDate.of(2026, 1, 8), endReason = "既有历史", coverAssetId = asset.id)) }
        assertTrue(repository.createProject(input) is MutationResult.Success)
        val extra = moneyCommand(100, input.project.id, MoneyDirection.RECOVERY, 3_000, today)
        assertTrue(repository.appendMoneyEvent(extra) is MutationResult.Success)
        val edit = editCommand(input.project).copy(name = "资料修正", startedOn = LocalDate.of(2026, 1, 2), note = "保留真实事件")
        assertEquals(MutationResult.ProjectUpdated(input.project.id, false), repository.updateProject(edit))
        val persisted = db.projectDao().findProject(input.project.id)!!
        assertEquals(input.project.id, persisted.id)
        assertEquals(input.project.createdAt, persisted.createdAt)
        assertEquals(input.project.endedOn, persisted.endedOn)
        assertEquals(input.project.endReason, persisted.endReason)
        assertEquals(asset.id, persisted.coverAssetId)
        assertEquals("资料修正", persisted.name)
        assertEquals("保留真实事件", persisted.note)
        assertEquals(edit.updatedAt, persisted.updatedAt)
        assertEquals(listOf(input.initialEvent, extra.event), db.projectDao().listEvents())
        reopen()
        assertEquals(persisted, repository.loadProjectDetail(input.project.id)!!.project)
    }

    @Test fun editing_start_recalculates_held_days_in_detail_and_page() = runBlocking {
        val input = createCommand(1)
        assertTrue(repository.createProject(input) is MutationResult.Success)
        assertTrue(repository.updateProject(editCommand(input.project).copy(startedOn = LocalDate.of(2026, 1, 6))) is MutationResult.ProjectUpdated)
        val detail = repository.loadProjectDetail(input.project.id)
        assertNotNull(detail)
        assertEquals(5L, detail!!.cost.heldDays)
        assertRate("200", detail.cost.dailyRate)
        assertEquals(detail.cost, repository.loadProjectPage().items.single().cost)
    }

    @Test fun stale_editor_cannot_overwrite_a_committed_metadata_change() = runBlocking {
        val input = createCommand(1)
        assertTrue(repository.createProject(input) is MutationResult.Success)
        val first = editCommand(input.project).copy(name = "第一份已保存")
        assertTrue(repository.updateProject(first) is MutationResult.ProjectUpdated)
        val stale = first.copy(commandId = id(801), name = "陈旧覆盖", updatedAt = instant.plusSeconds(2))
        assertTrue(repository.updateProject(stale) is MutationResult.Conflict)
        assertEquals("第一份已保存", db.projectDao().findProject(input.project.id)!!.name)
    }

    @Test fun full_baseline_comparison_detects_same_timestamp_changed_row() = runBlocking {
        val input = createCommand(1)
        assertTrue(repository.createProject(input) is MutationResult.Success)
        withContext(Dispatchers.IO) {
            db.openHelper.writableDatabase.execSQL("UPDATE long_term_projects SET name = ? WHERE id = ?", arrayOf("另一份资料", input.project.id))
        }
        assertTrue(repository.updateProject(editCommand(input.project)) is MutationResult.Conflict)
        assertEquals("另一份资料", db.projectDao().findProject(input.project.id)!!.name)
    }

    @Test fun lost_edit_result_replays_stable_target_after_reopen() = runBlocking {
        val input = createCommand(1)
        assertTrue(repository.createProject(input) is MutationResult.Success)
        val edit = editCommand(input.project)
        repository.updateProject(edit) // Lost response; retry must compare the exact stable target.
        reopen()
        assertEquals(MutationResult.ProjectUpdated(input.project.id, true), repository.updateProject(edit))
        assertEquals(edit.updatedAt, db.projectDao().findProject(input.project.id)!!.updatedAt)
        assertEquals(1, db.projectDao().listEvents().size)
    }

    @Test fun concurrent_edit_commands_only_apply_one_target() = runBlocking {
        val input = createCommand(1)
        assertTrue(repository.createProject(input) is MutationResult.Success)
        val edit = editCommand(input.project)
        val results = (1..12).map { async(Dispatchers.IO) { repository.updateProject(edit) } }.awaitAll()
        assertEquals(1, results.count { it == MutationResult.ProjectUpdated(input.project.id, false) })
        assertEquals(11, results.count { it == MutationResult.ProjectUpdated(input.project.id, true) })
        assertEquals(1, db.projectDao().listEvents().size)
    }

    @Test fun moving_start_after_existing_end_is_rejected_without_erasing_end() = runBlocking {
        val input = createCommand(1).let { it.copy(project = it.project.copy(endedOn = LocalDate.of(2026, 1, 8), endReason = "完成")) }
        assertTrue(repository.createProject(input) is MutationResult.Success)
        assertTrue(repository.updateProject(editCommand(input.project).copy(startedOn = LocalDate.of(2026, 1, 9))) is MutationResult.ValidationFailure)
        assertEquals(input.project, db.projectDao().findProject(input.project.id))
        assertEquals(listOf(input.initialEvent), db.projectDao().listEvents())
    }

    @Test fun editing_into_subscription_category_is_rejected_without_mutation() = runBlocking {
        val input = createCommand(1)
        assertTrue(repository.createProject(input) is MutationResult.Success)
        assertTrue(repository.updateProject(editCommand(input.project).copy(categoryId = LithicDatabase.SUBSCRIPTION_FALLBACK_ID)) is MutationResult.ValidationFailure)
        assertEquals(input.project, db.projectDao().findProject(input.project.id))
    }

    @Test fun edit_requires_increasing_revision_and_valid_business_identities() = runBlocking {
        val input = createCommand(1)
        assertTrue(repository.createProject(input) is MutationResult.Success)
        val edit = editCommand(input.project)
        assertTrue(repository.updateProject(edit.copy(updatedAt = instant)) is MutationResult.ValidationFailure)
        assertTrue(repository.updateProject(edit.copy(commandId = "invalid")) is MutationResult.ValidationFailure)
        assertTrue(repository.updateProject(edit.copy(name = " ")) is MutationResult.ValidationFailure)
        assertEquals(input.project, db.projectDao().findProject(input.project.id))
    }

    @Test fun frozen_create_replay_remains_strict_after_metadata_edit() = runBlocking {
        val input = createCommand(1)
        assertTrue(repository.createProject(input) is MutationResult.Success)
        assertTrue(repository.updateProject(editCommand(input.project)) is MutationResult.ProjectUpdated)
        assertTrue(repository.createProject(input) is MutationResult.Conflict)
        assertEquals(1, db.projectDao().listProjects().size)
        assertEquals(1, db.projectDao().listEvents().size)
    }

    @Test fun fifty_one_projects_page_as_fifty_and_one() = runBlocking { assertPaging(51) }
    @Test fun one_hundred_projects_have_stable_two_pages() = runBlocking { assertPaging(100) }
    @Test fun one_thousand_projects_remain_bounded_and_stably_sorted() = runBlocking { assertPaging(1000) }

    // SQL telemetry proves this is not full-table reads followed by UI take(50).
    @Test fun page_limits_rows_in_sql_and_batch_reads_visible_events_without_n_plus_one() = runBlocking {
        seedMany(100)
        queries.clear()
        val page = repository.loadProjectPage()
        assertEquals(50, page.items.size)
        val selects = queries.filter { it.trimStart().startsWith("SELECT", ignoreCase = true) }
        val projects = selects.filter { it.contains("FROM long_term_projects", ignoreCase = true) && !it.contains("COUNT", ignoreCase = true) }
        assertEquals(1, projects.size)
        assertTrue("Rows must be bounded inside SQL", projects.single().contains("LIMIT", ignoreCase = true))
        val events = selects.filter { it.contains("FROM project_money_events", ignoreCase = true) }
        assertEquals("Visible events must be fetched in one query", 1, events.size)
        assertTrue(events.single().contains(" IN ", ignoreCase = true))
        val categories = selects.filter { it.contains("FROM categories", ignoreCase = true) }
        assertEquals("Visible categories must not be read once per row", 1, categories.size)
        assertEquals(50, page.items.sumOf { it.events.size })
    }

    @Test fun home_summary_uses_only_count_without_loading_all_projects_or_events() = runBlocking {
        seedMany(1000)
        queries.clear()
        assertEquals(1000L, repository.loadSummary().projectCount)
        val selects = queries.filter { it.trimStart().startsWith("SELECT", ignoreCase = true) }
        // Room can refresh its internal invalidation log asynchronously after a transaction.
        // Classify exactly that metadata read; every other SELECT still counts against this contract.
        val metadata = selects.filter {
            it.trim().equals("SELECT * FROM room_table_modification_log WHERE invalidated = 1", ignoreCase = true)
        }
        val business = selects - metadata.toSet()
        val capturedSql = "Captured SQL: $selects; Room metadata: $metadata"
        assertEquals(capturedSql, 1, business.size)
        assertTrue(capturedSql, business.single().contains("COUNT", ignoreCase = true))
        assertTrue(capturedSql, business.single().contains("FROM long_term_projects", ignoreCase = true))
        assertFalse(capturedSql, business.single().contains("project_money_events", ignoreCase = true))
        assertFalse(capturedSql, business.single().contains("SELECT *", ignoreCase = true))
    }

    @Test fun selectable_categories_include_only_project_type() = runBlocking {
        db.categoryDao().insert(CategoryEntity(id(900), CategoryType.PROJECT, "工具", "工具", false))
        val categories = repository.loadProjectCategories()
        assertEquals(setOf(LithicDatabase.PROJECT_FALLBACK_ID, id(900)), categories.map { it.id }.toSet())
        assertTrue(categories.all { it.type == CategoryType.PROJECT })
    }

    // A once-only snapshot or count-only distinctUntilChanged would miss name/event updates.
    @Test fun real_room_changes_emit_initial_create_edit_money_and_category_invalidations() = runBlocking {
        val signals = Channel<Unit>(Channel.UNLIMITED)
        val observing = launch(Dispatchers.IO) { repository.changes.collect { signals.send(Unit) } }
        try {
            assertNotNull("Room must publish the initial query signal", withTimeoutOrNull(3_000) { signals.receive() })
            val input = createCommand(1)
            assertTrue(repository.createProject(input) is MutationResult.Success)
            assertNotNull("Create must invalidate readers", withTimeoutOrNull(3_000) { signals.receive() })
            assertEquals(1L, repository.loadSummary().projectCount)
            while (signals.tryReceive().isSuccess) { /* Discard already-observed invalidations. */ }
            assertTrue(repository.updateProject(editCommand(input.project).copy(name = "已刷新")) is MutationResult.ProjectUpdated)
            assertNotNull("A rename with unchanged count must invalidate readers", withTimeoutOrNull(3_000) { signals.receive() })
            assertEquals("已刷新", repository.loadProjectPage().items.single().project.name)
            while (signals.tryReceive().isSuccess) { }
            assertTrue(repository.appendMoneyEvent(moneyCommand(100, input.project.id, MoneyDirection.OUTLAY, 20_000, LocalDate.of(2026, 1, 5))) is MutationResult.Success)
            assertNotNull("Money events must invalidate readers", withTimeoutOrNull(3_000) { signals.receive() })
            assertRate("120", repository.loadProjectDetail(input.project.id)!!.cost.dailyRate)
            while (signals.tryReceive().isSuccess) { }
            db.categoryDao().insert(CategoryEntity(id(900), CategoryType.PROJECT, "工具", "工具", false))
            assertNotNull("Category changes must invalidate readers", withTimeoutOrNull(3_000) { signals.receive() })
            assertEquals(2, repository.loadProjectCategories().size)
        } finally { observing.cancel(); observing.join(); signals.close() }
    }

    @Test fun missing_project_is_null_but_read_failure_is_never_fake_empty_data() = runBlocking {
        assertNull(repository.loadProjectDetail(id(999)))
        val input = createCommand(1)
        assertTrue(repository.createProject(input) is MutationResult.Success)
        db.close()
        assertReadFails { repository.loadProjectDetail(input.project.id) }
        assertReadFails { repository.loadProjectPage() }
        assertReadFails { repository.loadSummary() }
        assertReadFails { repository.loadProjectCategories() }
    }

    @Test fun invalid_page_size_and_noncanonical_cursor_are_rejected() = runBlocking {
        for (size in listOf(0, -1, 51, 1000)) {
            var failed = false
            try { repository.loadProjectPage(pageSize = size) } catch (_: IllegalArgumentException) { failed = true }
            assertTrue("Page size $size must not bypass the database limit", failed)
        }
        var failed = false
        try { repository.loadProjectPage(ProjectCursor(today, "invalid")) } catch (_: IllegalArgumentException) { failed = true }
        assertTrue(failed)
    }

    @Test fun fresh_page_window_after_sort_key_edit_has_no_duplicate_or_missing_ids() = runBlocking {
        seedMany(51)
        val before = repository.loadProjectPage()
        assertEquals(50, before.items.size)
        val moving = before.items.first().project
        assertTrue(repository.updateProject(editCommand(moving).copy(startedOn = LocalDate.of(2026, 1, 9))) is MutationResult.ProjectUpdated)
        val first = repository.loadProjectPage()
        val second = repository.loadProjectPage(first.nextCursor)
        val ids = (first.items + second.items).map { it.project.id }
        assertEquals(51, ids.toSet().size)
        assertEquals(51, ids.size)
        assertEquals(moving.id, ids.last())
    }

    // A write queued during page one must not move that identity into a later page of this window.
    @Test fun window_is_one_snapshot_when_a_sort_key_edit_is_queued_between_pages() = runBlocking {
        seedMany(101)
        val input = createCommand(1)
        val beforeEvents = CountDownLatch(1)
        val continueRead = CountDownLatch(1)
        val writingStarted = CountDownLatch(1)
        val pausedOnce = AtomicBoolean(false)
        queryGate = { sql ->
            if (sql.contains("FROM project_money_events", ignoreCase = true) && pausedOnce.compareAndSet(false, true)) {
                beforeEvents.countDown()
                check(continueRead.await(10, TimeUnit.SECONDS))
            }
        }
        val reading = async(Dispatchers.IO) { repository.loadProjectWindow(3) }
        var writing: kotlinx.coroutines.Deferred<MutationResult>? = null
        try {
            assertTrue("Pause at the first page's real event read", withContext(Dispatchers.IO) { beforeEvents.await(5, TimeUnit.SECONDS) })
            writing = async(Dispatchers.IO) {
                writingStarted.countDown()
                repository.updateProject(editCommand(input.project).copy(startedOn = LocalDate.of(2026, 1, 9)))
            }
            assertTrue(withContext(Dispatchers.IO) { writingStarted.await(5, TimeUnit.SECONDS) })
            delay(100)
            assertFalse("The active window snapshot must keep the shared write boundary", writing.isCompleted)
        } finally {
            continueRead.countDown()
            queryGate = null
        }
        val window = reading.await()
        val writeResult = writing!!.await()
        assertTrue(writeResult is MutationResult.ProjectUpdated)
        assertEquals(101L, window.totalCount)
        assertEquals("One window must not contain an old row twice after it moves", 101, window.items.size)
        assertEquals(101, window.items.map { it.project.id }.toSet().size)
        assertEquals((1..101).map { id(1_000 + it) }, window.items.map { it.project.id })
        assertNull(window.nextCursor)
        val refreshed = repository.loadProjectWindow(3)
        assertEquals(101, refreshed.items.size)
        assertEquals((2..101).map { id(1_000 + it) } + input.project.id, refreshed.items.map { it.project.id })
        assertEquals(101, refreshed.items.map { it.project.id }.toSet().size)
        assertNull(refreshed.nextCursor)
    }

    @Test fun legacy_end_and_future_end_are_evaluated_without_reactivating_settlement() = runBlocking {
        val ended = createCommand(1).let { it.copy(project = it.project.copy(endedOn = LocalDate.of(2026, 1, 5), endReason = "已有结束")) }
        val futureEnd = createCommand(2).let { it.copy(project = it.project.copy(endedOn = LocalDate.of(2026, 1, 20), endReason = "已有未来日期")) }
        assertTrue(repository.createProject(ended) is MutationResult.Success)
        assertTrue(repository.createProject(futureEnd) is MutationResult.Success)
        assertTrue(repository.appendMoneyEvent(moneyCommand(100, ended.project.id, MoneyDirection.RECOVERY, 20_000, today)) is MutationResult.Success)
        val history = repository.loadProjectDetail(ended.project.id)
        val future = repository.loadProjectDetail(futureEnd.project.id)
        assertNotNull(history)
        assertNotNull(future)
        assertEquals(5L, history!!.cost.heldDays)
        assertRate("160", history.cost.dailyRate)
        assertFalse(history.cost.contributesToCurrentTotal)
        assertEquals(10L, future!!.cost.heldDays)
        assertTrue(future.cost.contributesToCurrentTotal)
    }

    private suspend fun assertPaging(count: Int) {
        seedMany(count)
        val seen = mutableListOf<String>()
        var cursor: ProjectCursor? = null
        do {
            val page = repository.loadProjectPage(cursor)
            assertEquals(count.toLong(), page.totalCount)
            assertTrue(page.items.isNotEmpty())
            assertTrue(page.items.size <= 50)
            assertTrue(page.items.all { it.events.size == 1 && it.cost.grossOutlayCents == 100_000L })
            seen += page.items.map { it.project.id }
            cursor = page.nextCursor
        } while (cursor != null)
        assertEquals(count, seen.size)
        assertEquals(count, seen.toSet().size)
        assertEquals((1..count).map { id(1_000 + it) }, seen)
    }

    private suspend fun seedMany(count: Int) = db.withTransaction {
        for (index in count downTo 1) {
            val command = createCommand(index)
            db.projectDao().insertProject(command.project)
            db.projectDao().insertEvent(command.initialEvent)
        }
    }

    private suspend fun reopen() {
        db.close()
        db = withContext(Dispatchers.IO) { LithicDatabase.open(context, databaseFile, requireExisting = true) }
    }

    private suspend fun assertReadFails(block: suspend () -> Unit) {
        try { block(); fail("An unavailable database must not become null, zero or an empty page") }
        catch (expected: Exception) { assertFalse(expected is kotlinx.coroutines.CancellationException) }
    }

    private fun createCommand(
        index: Int,
        amount: Long = 100_000,
        start: LocalDate = LocalDate.of(2026, 1, 1),
        paidOn: LocalDate = LocalDate.of(2026, 1, 1),
    ): CreateProjectCommand {
        val project = LongTermProjectEntity(id(1_000 + index), "项目$index", LithicDatabase.PROJECT_FALLBACK_ID, start, createdAt = instant, updatedAt = instant)
        val operation = id(3_000 + index)
        return CreateProjectCommand(operation, project, ProjectMoneyEventEntity(id(2_000 + index), project.id, MoneyDirection.OUTLAY, amount, paidOn, "初始实际投入", ProjectMoneyEventRole.INITIAL, operation))
    }

    private fun moneyCommand(index: Int, projectId: String, direction: MoneyDirection, amount: Long, date: LocalDate): AddProjectMoneyEventCommand {
        val operation = id(30_000 + index)
        return AddProjectMoneyEventCommand(operation, ProjectMoneyEventEntity(id(20_000 + index), projectId, direction, amount, date, "真实资金事实", ProjectMoneyEventRole.REGULAR, operation))
    }

    private fun editCommand(expected: LongTermProjectEntity) = UpdateProjectCommand(id(800), expected, "修正后名称", expected.startedOn, expected.categoryId, "修正后备注", instant.plusSeconds(1))
    private fun id(value: Int): String = "20000000-0000-4000-8000-${value.toString().padStart(12, '0')}"
    private fun assertRate(expected: String, actual: BigDecimal?) {
        assertNotNull(actual)
        assertEquals(0, BigDecimal(expected).compareTo(actual!!))
    }
}
