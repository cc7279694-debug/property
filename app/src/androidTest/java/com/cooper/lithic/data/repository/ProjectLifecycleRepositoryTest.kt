package com.cooper.lithic.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic file-backed Android SQLite fixtures; no production generation or old tests change. */
@RunWith(AndroidJUnit4::class)
class ProjectLifecycleRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var directory: File
    private lateinit var databaseFile: File
    private lateinit var db: LithicDatabase
    private val coordinator = LocalWriteCoordinator()
    private val start = LocalDate.of(2026, 1, 1)
    private val today = LocalDate.of(2026, 1, 31)
    private val instant = Instant.parse("2026-01-31T03:04:05.123456789Z")
    private data class Trace(val sql: String, val arguments: List<Any?>)
    private val queries = CopyOnWriteArrayList<Trace>()
    @Volatile private var queryGate: ((String) -> Unit)? = null
    private val repository get() = ProjectRepository(db, coordinator, FixedBusinessClock(today))

    @Before fun setUp() = runBlocking {
        directory = File(context.noBackupFilesDir, "m07-lifecycle-test-${UUID.randomUUID()}")
        check(directory.mkdirs())
        databaseFile = File(directory, "data.db")
        db = withContext(Dispatchers.IO) {
            Room.databaseBuilder(context, LithicDatabase::class.java, databaseFile.path)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .setQueryCallback({ sql, args -> queries += Trace(sql, args.toList()); queryGate?.invoke(sql) }, { it.run() })
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

    // M07-T01: a real SQLite ABORT after the project update must roll that update back.
    @Test fun end_and_recovery_atomic() = runBlocking {
        val input = create(1)
        val command = end(input.project)
        sql("CREATE TRIGGER fail_end_insert BEFORE INSERT ON project_money_events WHEN NEW.role = 'END_SETTLEMENT' BEGIN SELECT RAISE(ABORT, 'injected end insert failure'); END")
        val failed = repository.endProject(command)
        assertTrue(failed is MutationResult.Conflict || failed is MutationResult.StorageFailure)
        assertEquals(input.project, detail(input.project.id).project)
        assertEquals(listOf(input.initialEvent), detail(input.project.id).events)
        sql("DROP TRIGGER fail_end_insert")
        val results = (1..8).map { async(Dispatchers.IO) { repository.endProject(command) } }.awaitAll()
        assertTrue(results.all { it is MutationResult.LifecycleApplied })
        assertEquals(1, results.count { !(it as MutationResult.LifecycleApplied).replayed })
        assertEquals(2, detail(input.project.id).events.size)
        assertEquals(listOf(command.settlement), detail(input.project.id).events.filter { it.role == ProjectMoneyEventRole.END_SETTLEMENT })
        assertTrue(repository.endProject(command) == MutationResult.LifecycleApplied(input.project.id, command.settlement!!.id, true))
        assertTrue(repository.endProject(command.copy(settlement = command.settlement.copy(amountCents = 1))) is MutationResult.Conflict)
        assertIntegrity()
    }

    // M07-T02: amendments update the same event, and an unknown-result retry is exact target replay.
    @Test fun edit_end_recovery_not_duplicated() = runBlocking {
        val input = create(1)
        val ordinary = append(input.project.id, 1, MoneyDirection.OUTLAY, 20_000, start.plusDays(2))
        assertApplied(repository.endProject(end(input.project)))
        val original = detail(input.project.id)
        val first = amend(original, 40_000)
        assertApplied(repository.updateEnd(first))
        assertEquals(MutationResult.LifecycleApplied(input.project.id, first.expectedSettlement!!.id, true), repository.updateEnd(first))
        assertTrue(repository.updateEnd(first.copy(settlementAmountCents = 45_000)) is MutationResult.Conflict)
        val second = amend(detail(input.project.id), 60_000)
        assertApplied(repository.updateEnd(second))
        assertTrue(repository.updateEnd(first.copy(updatedAt = instant.plusSeconds(50))) is MutationResult.Conflict)
        val after = detail(input.project.id)
        val settlement = after.events.single { it.role == ProjectMoneyEventRole.END_SETTLEMENT }
        assertEquals(60_000L, settlement.amountCents)
        assertEquals(first.expectedSettlement.copy(amountCents = 60_000), settlement)
        assertEquals(input.initialEvent, after.events.single { it.role == ProjectMoneyEventRole.INITIAL })
        assertEquals(ordinary, after.events.single { it.role == ProjectMoneyEventRole.REGULAR })
        assertEquals(3, after.events.size)
    }

    // M07-T03: only the frozen engine decides duration; late money does not change lifecycle.
    @Test fun late_installment_does_not_reactivate() = runBlocking {
        val input = create(1)
        assertApplied(repository.endProject(end(input.project, settlementCents = null)))
        append(input.project.id, 1, MoneyDirection.OUTLAY, 20_000, start.plusDays(19))
        append(input.project.id, 2, MoneyDirection.RECOVERY, 130_000, start.plusDays(20))
        val after = detail(input.project.id)
        assertEquals(10L, after.cost.heldDays)
        assertEquals(120_000L, after.cost.grossOutlayCents)
        assertEquals(-10_000L, after.cost.netCents)
        assertRate("-10", after.cost.dailyRate)
        assertFalse(after.cost.contributesToCurrentTotal)
        assertTrue(repository.loadCurrentProjectWindow(1).items.isEmpty())
        assertEquals(input.project.id, repository.loadProjectHistoryWindow(1).items.single().project.id)
        assertEquals(10L, detail(input.project.id, start.plusDays(9)).cost.heldDays)
        assertEquals(100_000L, detail(input.project.id, start.plusDays(9)).cost.netCents)
    }

    // M07-T04: changing the date changes derived results and observation-day filtering.
    @Test fun correct_end_date_updates_result() = runBlocking {
        val input = create(1)
        assertApplied(repository.endProject(end(input.project, settlementCents = null)))
        val before = detail(input.project.id)
        val command = amend(before, null).copy(endedOn = start.plusDays(4))
        assertApplied(repository.updateEnd(command))
        assertEquals(5L, detail(input.project.id).cost.heldDays)
        assertRate("200", detail(input.project.id).cost.dailyRate)
        val current = detail(input.project.id)
        assertTrue(repository.updateEnd(amend(current, null).copy(endedOn = start.minusDays(1))) is MutationResult.ValidationFailure)
        assertApplied(repository.updateEnd(amend(current, null).copy(endedOn = start)))
        assertEquals(1L, detail(input.project.id, start).cost.heldDays)
        assertFalse(detail(input.project.id, start).cost.contributesToCurrentTotal)
        assertEquals(input.project.id, repository.loadProjectHistoryWindow(1, start).items.single().project.id)
        val sameDay = detail(input.project.id)
        assertApplied(repository.updateEnd(amend(sameDay, null).copy(endedOn = start.plusDays(40))))
        assertTrue(repository.loadProjectHistoryWindow(1).items.isEmpty())
        assertEquals(input.project.id, repository.loadCurrentProjectWindow(1).items.single().project.id)
    }

    // M07-T05 data boundary: only an explicit complete snapshot command deletes; UI owns cancellation.
    @Test fun delete_requires_confirmation() = runBlocking {
        val input = create(1)
        append(input.project.id, 1, MoneyDirection.OUTLAY, 10_000, start)
        append(input.project.id, 2, MoneyDirection.RECOVERY, 5_000, start)
        val snapshot = detail(input.project.id)
        val command = DeleteProjectCommand(id(70_001), snapshot.project, snapshot.events.reversed())
        assertEquals(3, detail(input.project.id).events.size)
        sql("CREATE TRIGGER fail_project_delete BEFORE DELETE ON long_term_projects BEGIN SELECT RAISE(ABORT, 'injected delete failure'); END")
        val failed = repository.deleteProject(command)
        assertTrue(failed is MutationResult.Conflict || failed is MutationResult.StorageFailure)
        assertEquals(snapshot, detail(input.project.id))
        sql("DROP TRIGGER fail_project_delete")
        assertEquals(MutationResult.ProjectDeleted(input.project.id), repository.deleteProject(command))
        assertNull(repository.loadProjectDetail(input.project.id))
        assertTrue(db.projectDao().eventsForProjects(listOf(input.project.id)).isEmpty())
        assertEquals(0L, repository.loadSummary().projectCount)
        assertTrue(repository.loadProjectHistoryWindow(1).items.isEmpty())
        assertEquals(MutationResult.ProjectAbsent(input.project.id), repository.deleteProject(command))
        assertIntegrity()
    }

    @Test fun no_settlement_and_explicit_zero_settlement_preserve_distinct_facts() = runBlocking {
        val noMoney = create(1)
        val zero = create(2)
        assertApplied(repository.endProject(end(noMoney.project, settlementCents = null)))
        assertApplied(repository.endProject(end(zero.project, settlementCents = 0)))
        assertEquals(1, detail(noMoney.project.id).events.size)
        assertEquals(0L, detail(zero.project.id).events.single { it.role == ProjectMoneyEventRole.END_SETTLEMENT }.amountCents)
        assertEquals(100_000L, detail(noMoney.project.id).cost.netCents)
        assertEquals(100_000L, detail(zero.project.id).cost.netCents)
    }

    @Test fun end_rejects_invalid_date_reason_revision_and_future_real_money() = runBlocking {
        val input = create(1)
        val command = end(input.project)
        for (invalid in listOf(
            command.copy(endedOn = start.minusDays(1)), command.copy(endReason = " \t"),
            command.copy(updatedAt = instant), command.copy(settlement = command.settlement!!.copy(occurredOn = today.plusDays(1))),
            command.copy(settlement = command.settlement.copy(direction = MoneyDirection.OUTLAY)),
            command.copy(settlement = command.settlement.copy(role = ProjectMoneyEventRole.REGULAR)),
            command.copy(settlement = command.settlement.copy(operationId = id(90_000))),
            command.copy(commandId = "invalid"),
            command.copy(expectedProject = command.expectedProject.copy(categoryId = "invalid")),
            command.copy(settlement = command.settlement.copy(id = "invalid")),
        )) assertTrue(repository.endProject(invalid) is MutationResult.ValidationFailure)
        assertEquals(input.project, detail(input.project.id).project)
        assertEquals(listOf(input.initialEvent), detail(input.project.id).events)
    }

    @Test fun different_concurrent_end_commands_conflict_instead_of_creating_two_settlements() = runBlocking {
        val input = create(1)
        val first = end(input.project)
        val second = first.copy(commandId = id(40_002), settlement = first.settlement!!.copy(id = id(50_002), operationId = id(40_002)))
        val results = listOf(first, second).map { async(Dispatchers.IO) { repository.endProject(it) } }.awaitAll()
        assertEquals(1, results.count { it is MutationResult.LifecycleApplied })
        assertEquals(1, results.count { it is MutationResult.Conflict })
        assertEquals(1, detail(input.project.id).events.count { it.role == ProjectMoneyEventRole.END_SETTLEMENT })
    }

    @Test fun full_project_baseline_rejects_same_revision_changed_metadata() = runBlocking {
        val input = create(1)
        sql("UPDATE long_term_projects SET note = 'new external note' WHERE id = '${input.project.id}'")
        assertTrue(repository.endProject(end(input.project)) is MutationResult.Conflict)
        assertTrue(repository.updateProject(UpdateProjectCommand(id(80_001), input.project, "旧编辑", start, input.project.categoryId, null, instant.plusSeconds(1))) is MutationResult.Conflict)
        assertNull(detail(input.project.id).project.endedOn)
        assertEquals("new external note", detail(input.project.id).project.note)
    }

    @Test fun amend_rolls_back_project_when_real_settlement_update_fails() = runBlocking {
        val input = create(1)
        assertApplied(repository.endProject(end(input.project)))
        val snapshot = detail(input.project.id)
        sql("CREATE TRIGGER fail_settlement_update BEFORE UPDATE ON project_money_events WHEN OLD.role = 'END_SETTLEMENT' BEGIN SELECT RAISE(ABORT, 'injected amend failure'); END")
        val result = repository.updateEnd(amend(snapshot, 70_000).copy(endedOn = start.plusDays(14)))
        assertTrue(result is MutationResult.Conflict || result is MutationResult.StorageFailure)
        assertEquals(snapshot, detail(input.project.id))
        sql("DROP TRIGGER fail_settlement_update")
    }

    @Test fun amend_without_an_existing_settlement_cannot_create_or_remove_money() = runBlocking {
        val input = create(1)
        assertApplied(repository.endProject(end(input.project, settlementCents = null)))
        val snapshot = detail(input.project.id)
        assertTrue(repository.updateEnd(amend(snapshot, null).copy(settlementAmountCents = 0, settlementOccurredOn = start)) is MutationResult.ValidationFailure)
        assertApplied(repository.updateEnd(amend(snapshot, null)))
        assertEquals(listOf(input.initialEvent), detail(input.project.id).events)
        val withMoney = create(2)
        assertApplied(repository.endProject(end(withMoney.project)))
        val ended = detail(withMoney.project.id)
        assertTrue(repository.updateEnd(amend(ended, null)) is MutationResult.ValidationFailure)
        assertEquals(2, detail(withMoney.project.id).events.size)
    }

    @Test fun legacy_first_end_settlement_is_compatible_but_every_add_rejects_a_second() = runBlocking {
        val input = create(1, endedOn = start.plusDays(9))
        val command = end(input.project).settlement!!
        assertTrue(repository.appendMoneyEvent(AddProjectMoneyEventCommand(command.operationId, command)) is MutationResult.Success)
        assertEquals(MutationResult.Success(input.project.id, command.id, true), repository.appendMoneyEvent(AddProjectMoneyEventCommand(command.operationId, command)))
        val second = command.copy(id = id(50_002), operationId = id(40_002))
        assertTrue(repository.appendMoneyEvent(AddProjectMoneyEventCommand(second.operationId, second)) is MutationResult.Conflict)
        append(input.project.id, 1, MoneyDirection.RECOVERY, 20_000, today)
        assertEquals(3, detail(input.project.id).events.size)
    }

    @Test fun ambiguous_legacy_settlements_conflict_at_all_formal_write_entry_points() = runBlocking {
        val input = create(1, endedOn = start.plusDays(9))
        val first = end(input.project).settlement!!
        val second = first.copy(id = id(50_002), operationId = id(40_002))
        db.withTransaction { db.projectDao().insertEvent(first); db.projectDao().insertEvent(second) }
        val snapshot = detail(input.project.id)
        assertTrue(repository.appendMoneyEvent(money(input.project.id, 1, MoneyDirection.RECOVERY, 10, today)) is MutationResult.Conflict)
        assertTrue(repository.appendMoneyEvent(AddProjectMoneyEventCommand(first.operationId, first)) is MutationResult.Conflict)
        assertTrue(repository.createProject(input) is MutationResult.Conflict)
        assertTrue(repository.updateProject(UpdateProjectCommand(id(80_001), snapshot.project, "不猜关联", start, snapshot.project.categoryId, null, instant.plusSeconds(1))) is MutationResult.Conflict)
        assertTrue(repository.updateEnd(UpdateEndCommand(id(60_001), snapshot.project, first, start.plusDays(9), "修正", 10, today, null, instant.plusSeconds(1))) is MutationResult.Conflict)
        assertTrue(repository.endProject(end(snapshot.project)) is MutationResult.Conflict)
        assertTrue(repository.correctMoneyEvent(correct(snapshot, input.initialEvent, 90_000)) is MutationResult.Conflict)
        assertTrue(repository.deleteProject(DeleteProjectCommand(id(70_001), snapshot.project, snapshot.events)) is MutationResult.Conflict)
        assertEquals(snapshot, detail(input.project.id))
    }

    @Test fun historical_initial_and_regular_corrections_preserve_identity_and_refunds_are_new_facts() = runBlocking {
        val input = create(1)
        val ordinary = append(input.project.id, 1, MoneyDirection.OUTLAY, 20_000, start.plusDays(2))
        val first = correct(detail(input.project.id), input.initialEvent, 90_000).copy(occurredOn = start.minusDays(1), description = "修正初始事实")
        assertEquals(MutationResult.MoneyCorrected(input.project.id, input.initialEvent.id, false), repository.correctMoneyEvent(first))
        assertEquals(MutationResult.MoneyCorrected(input.project.id, input.initialEvent.id, true), repository.correctMoneyEvent(first))
        assertTrue(repository.correctMoneyEvent(first.copy(amountCents = 80_000)) is MutationResult.Conflict)
        val second = correct(detail(input.project.id), ordinary, 10_000).copy(occurredOn = start.plusDays(3), description = "历史纠错")
        assertTrue(repository.correctMoneyEvent(second) is MutationResult.MoneyCorrected)
        append(input.project.id, 2, MoneyDirection.RECOVERY, 5_000, start.plusDays(4))
        val after = detail(input.project.id)
        assertEquals(3, after.events.size)
        assertEquals(input.initialEvent.copy(amountCents = 90_000, occurredOn = start.minusDays(1), description = "修正初始事实"), after.events.single { it.id == input.initialEvent.id })
        assertEquals(ordinary.copy(amountCents = 10_000, occurredOn = start.plusDays(3), description = "历史纠错"), after.events.single { it.id == ordinary.id })
        assertEquals(95_000L, after.cost.netCents)
        assertEquals(90_000L, detail(input.project.id, start.plusDays(2)).cost.netCents)
    }

    @Test fun historical_correction_rejects_settlement_future_money_and_changed_full_event() = runBlocking {
        val input = create(1)
        assertApplied(repository.endProject(end(input.project)))
        val snapshot = detail(input.project.id)
        val settlement = snapshot.events.single { it.role == ProjectMoneyEventRole.END_SETTLEMENT }
        assertTrue(repository.correctMoneyEvent(correct(snapshot, settlement, 10)) is MutationResult.ValidationFailure)
        assertTrue(repository.correctMoneyEvent(correct(snapshot, input.initialEvent, 10).copy(occurredOn = today.plusDays(1))) is MutationResult.ValidationFailure)
        sql("UPDATE project_money_events SET description = 'concurrent event edit' WHERE id = '${input.initialEvent.id}'")
        assertTrue(repository.correctMoneyEvent(correct(snapshot, input.initialEvent, 10)) is MutationResult.Conflict)
        assertEquals(100_000L, detail(input.project.id).events.single { it.role == ProjectMoneyEventRole.INITIAL }.amountCents)
    }

    @Test fun historical_correction_is_atomic_with_its_project_revision() = runBlocking {
        val input = create(1)
        val snapshot = detail(input.project.id)
        sql("CREATE TRIGGER fail_money_correction BEFORE UPDATE ON project_money_events BEGIN SELECT RAISE(ABORT, 'injected correction failure'); END")
        val result = repository.correctMoneyEvent(correct(snapshot, input.initialEvent, 70_000))
        assertTrue(result is MutationResult.Conflict || result is MutationResult.StorageFailure)
        assertEquals(snapshot, detail(input.project.id))
        sql("DROP TRIGGER fail_money_correction")
    }

    @Test fun deletion_refuses_stale_project_and_any_new_or_corrected_event() = runBlocking {
        val input = create(1)
        val old = detail(input.project.id)
        val confirmation = DeleteProjectCommand(id(70_001), old.project, old.events)
        append(input.project.id, 1, MoneyDirection.RECOVERY, 5_000, start)
        assertTrue(repository.deleteProject(confirmation) is MutationResult.Conflict)
        val fresh = detail(input.project.id)
        val freshConfirmation = confirmation.copy(expectedProject = fresh.project, expectedEvents = fresh.events)
        assertTrue(repository.correctMoneyEvent(correct(fresh, input.initialEvent, 90_000)) is MutationResult.MoneyCorrected)
        assertTrue(repository.deleteProject(freshConfirmation) is MutationResult.Conflict)
        val current = detail(input.project.id)
        assertTrue(repository.deleteProject(freshConfirmation.copy(expectedProject = current.project)) is MutationResult.Conflict)
        assertEquals(1L, repository.loadSummary().projectCount)
    }

    @Test fun deleting_one_project_keeps_other_project_shared_media_and_real_file() = runBlocking {
        val media = LocalMediaAssetEntity(id(90_001), "media/shared.webp", "image/webp", 1, 1, 3, "a".repeat(64))
        db.mediaDao().insert(media)
        val mediaDirectory = File(directory, "media").also { check(it.mkdir()) }
        val file = File(mediaDirectory, "shared.webp").also { it.writeBytes(byteArrayOf(1, 2, 3)) }
        val first = create(1, cover = media.id)
        val second = create(2, cover = media.id)
        val snapshot = detail(first.project.id)
        assertEquals(MutationResult.ProjectDeleted(first.project.id), repository.deleteProject(DeleteProjectCommand(id(70_001), snapshot.project, snapshot.events)))
        assertEquals(second.project, detail(second.project.id).project)
        assertEquals(media, db.mediaDao().find(media.id))
        assertTrue(file.readBytes().contentEquals(byteArrayOf(1, 2, 3)))
        val last = detail(second.project.id)
        assertEquals(MutationResult.ProjectDeleted(second.project.id), repository.deleteProject(DeleteProjectCommand(id(70_002), last.project, last.events)))
        assertEquals(media, db.mediaDao().find(media.id))
        assertTrue(file.exists())
        assertIntegrity()
    }

    @Test fun current_and_history_are_filtered_in_sql_and_bounded_by_stable_fifty_row_pages() = runBlocking {
        seedWindows(101)
        queries.clear()
        val history = repository.loadProjectHistoryWindow(3)
        assertEquals(101L, history.totalCount)
        assertEquals(101, history.items.size)
        assertEquals((1..101).map { id(10_000 + it) }, history.items.map { it.project.id })
        assertNull(history.nextCursor)
        assertTrue(history.items.all { it.events.size == 1 && !it.cost.contributesToCurrentTotal })
        assertFilteredPageQueries("endedOn <=", 3)
        queries.clear()
        val current = repository.loadCurrentProjectWindow(3)
        assertEquals(103L, current.totalCount)
        assertEquals(103, current.items.size)
        assertEquals(103, current.items.map { it.project.id }.toSet().size)
        assertTrue(current.items.none { it.project.endedOn != null && !it.project.endedOn.isAfter(today) })
        assertTrue(current.items.any { it.project.startedOn.isAfter(today) && it.cost.heldDays == 0L })
        assertFilteredPageQueries("endedOn >", 3)
        assertEquals(204L, repository.loadSummary().projectCount)
        assertEquals(204L, repository.loadProjectWindow(1).totalCount)
        assertEquals(50, repository.loadProjectHistoryWindow(1).items.size)
        assertNotNull(repository.loadProjectHistoryWindow(1).nextCursor)
    }

    @Test fun filtered_window_holds_one_snapshot_when_end_write_is_queued_between_pages() = runBlocking {
        seedWindows(51)
        val moving = detail(id(20_001)).project
        val paused = CountDownLatch(1)
        val continueRead = CountDownLatch(1)
        val once = AtomicBoolean(false)
        queryGate = { sql ->
            if (sql.contains("FROM project_money_events", true) && once.compareAndSet(false, true)) {
                paused.countDown(); check(continueRead.await(10, TimeUnit.SECONDS))
            }
        }
        val reading = async(Dispatchers.IO) { repository.loadCurrentProjectWindow(2) }
        var writing: kotlinx.coroutines.Deferred<MutationResult>? = null
        try {
            assertTrue(withContext(Dispatchers.IO) { paused.await(5, TimeUnit.SECONDS) })
            writing = async(Dispatchers.IO) { repository.endProject(end(moving, settlementCents = null)) }
            delay(100)
            assertFalse(writing.isCompleted)
        } finally { continueRead.countDown(); queryGate = null }
        val snapshot = reading.await()
        assertEquals(53L, snapshot.totalCount)
        assertEquals(53, snapshot.items.map { it.project.id }.toSet().size)
        assertTrue(snapshot.items.any { it.project.id == moving.id && it.project.endedOn == null })
        assertApplied(writing!!.await())
        assertEquals(52L, repository.loadCurrentProjectWindow(2).totalCount)
        assertEquals(52L, repository.loadProjectHistoryWindow(2).totalCount)
    }

    @Test fun empty_filtered_reads_are_real_and_database_failure_does_not_become_empty_history() = runBlocking {
        assertTrue(repository.loadCurrentProjectWindow(1).items.isEmpty())
        assertTrue(repository.loadProjectHistoryWindow(1).items.isEmpty())
        for (count in listOf(0, -1)) {
            try { repository.loadProjectHistoryWindow(count); fail("Invalid window must fail") } catch (_: IllegalArgumentException) { }
        }
        db.close()
        try { repository.loadProjectHistoryWindow(1); fail("Closed database must not look like empty history") } catch (_: IllegalStateException) { }
    }

    @Test fun end_and_settlement_survive_real_file_database_reopen() = runBlocking {
        val input = create(1)
        assertApplied(repository.endProject(end(input.project)))
        val before = detail(input.project.id)
        db.close()
        db = withContext(Dispatchers.IO) { LithicDatabase.open(context, databaseFile, requireExisting = true) }
        assertEquals(before, detail(input.project.id))
        assertEquals(input.project.id, repository.loadProjectHistoryWindow(1).items.single().project.id)
        assertIntegrity()
    }

    private suspend fun seedWindows(count: Int) = db.withTransaction {
        for (index in count downTo 1) {
            for ((offset, ended) in listOf(10_000 to start.plusDays(9), 20_000 to null)) {
                val p = LongTermProjectEntity(id(offset + index), "分页$index", LithicDatabase.PROJECT_FALLBACK_ID, start, ended, ended?.let { "已结束" }, createdAt = instant, updatedAt = instant)
                db.projectDao().insertProject(p)
                db.projectDao().insertEvent(ProjectMoneyEventEntity(id(offset + 100_000 + index), p.id, MoneyDirection.OUTLAY, 100_000, start, null, ProjectMoneyEventRole.INITIAL, id(offset + 200_000 + index)))
            }
        }
        for ((index, started, ended) in listOf(Triple(1, start, today.plusDays(10)), Triple(2, today.plusDays(10), null))) {
            val p = LongTermProjectEntity(id(30_000 + index), "未来$index", LithicDatabase.PROJECT_FALLBACK_ID, started, ended, ended?.let { "未来结束" }, createdAt = instant, updatedAt = instant)
            db.projectDao().insertProject(p)
            db.projectDao().insertEvent(ProjectMoneyEventEntity(id(130_000 + index), p.id, MoneyDirection.OUTLAY, 100_000, start, null, ProjectMoneyEventRole.INITIAL, id(230_000 + index)))
        }
    }

    private fun assertFilteredPageQueries(predicate: String, count: Int) {
        val pages = queries.filter { it.sql.contains("FROM long_term_projects", true) && it.sql.contains("LIMIT", true) }
        assertEquals(count, pages.size)
        assertTrue(pages.all { it.sql.contains(predicate, true) && (it.arguments.last() as Number).toInt() == 51 })
        assertFalse(queries.any { it.sql.trim().equals("SELECT * FROM long_term_projects ORDER BY startedOn, id", true) })
    }

    private suspend fun create(index: Int, endedOn: LocalDate? = null, cover: String? = null): CreateProjectCommand {
        val p = LongTermProjectEntity(id(1_000 + index), "项目$index", LithicDatabase.PROJECT_FALLBACK_ID, start, endedOn, endedOn?.let { "原有结束" }, cover, createdAt = instant, updatedAt = instant)
        val operation = id(3_000 + index)
        val command = CreateProjectCommand(operation, p, ProjectMoneyEventEntity(id(2_000 + index), p.id, MoneyDirection.OUTLAY, 100_000, start, "初始事实", ProjectMoneyEventRole.INITIAL, operation))
        assertTrue(repository.createProject(command) is MutationResult.Success)
        return command
    }

    private fun end(expected: LongTermProjectEntity, settlementCents: Long? = 30_000): EndProjectCommand {
        val operation = id(40_001)
        val event = settlementCents?.let { ProjectMoneyEventEntity(id(50_001), expected.id, MoneyDirection.RECOVERY, it, start.plusDays(9), "真实卖出", ProjectMoneyEventRole.END_SETTLEMENT, operation) }
        return EndProjectCommand(operation, expected, start.plusDays(9), "卖出", event, expected.updatedAt.plusSeconds(1))
    }

    private fun amend(snapshot: ProjectDetail, cents: Long?): UpdateEndCommand {
        val event = snapshot.events.singleOrNull { it.role == ProjectMoneyEventRole.END_SETTLEMENT }
        return UpdateEndCommand(id(60_001), snapshot.project, event, snapshot.project.endedOn!!, "修正结束", cents, cents?.let { event!!.occurredOn }, event?.description, snapshot.project.updatedAt.plusSeconds(1))
    }

    private fun correct(snapshot: ProjectDetail, event: ProjectMoneyEventEntity, cents: Long) = CorrectMoneyEventCommand(id(80_001), snapshot.project, event, cents, event.occurredOn, event.description, snapshot.project.updatedAt.plusSeconds(1))
    private fun money(projectId: String, index: Int, direction: MoneyDirection, cents: Long, date: LocalDate): AddProjectMoneyEventCommand {
        val operation = id(30_000 + index)
        return AddProjectMoneyEventCommand(operation, ProjectMoneyEventEntity(id(20_000 + index), projectId, direction, cents, date, "真实后续资金", ProjectMoneyEventRole.REGULAR, operation))
    }
    private suspend fun append(projectId: String, index: Int, direction: MoneyDirection, cents: Long, date: LocalDate): ProjectMoneyEventEntity {
        val command = money(projectId, index, direction, cents, date)
        assertTrue(repository.appendMoneyEvent(command) is MutationResult.Success)
        return command.event
    }
    private suspend fun detail(id: String, asOf: LocalDate = today): ProjectDetail = checkNotNull(repository.loadProjectDetail(id, asOf))
    private suspend fun sql(statement: String) = withContext(Dispatchers.IO) { db.openHelper.writableDatabase.execSQL(statement) }
    private suspend fun assertIntegrity() = withContext(Dispatchers.IO) {
        db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM project_money_events e LEFT JOIN long_term_projects p ON p.id = e.projectId WHERE p.id IS NULL").use { check(it.moveToFirst()); assertEquals(0L, it.getLong(0)) }
    }
    private fun assertApplied(result: MutationResult) = assertTrue("Expected lifecycle write, got $result", result is MutationResult.LifecycleApplied)
    private fun assertRate(expected: String, actual: BigDecimal?) { assertNotNull(actual); assertEquals(0, BigDecimal(expected).compareTo(actual!!)) }
    private fun id(value: Int): String = "70000000-0000-4000-8000-${value.toString().padStart(12, '0')}"
}
