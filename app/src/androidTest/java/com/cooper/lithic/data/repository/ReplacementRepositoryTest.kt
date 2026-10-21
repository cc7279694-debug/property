package com.cooper.lithic.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.ReplacementEntity
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Android Room transactions using only this test's disposable generation. */
@RunWith(AndroidJUnit4::class)
class ReplacementRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var directory: File
    private lateinit var databaseFile: File
    private lateinit var db: LithicDatabase
    private val coordinator = LocalWriteCoordinator()
    private val today = LocalDate.of(2026, 10, 21)
    private val instant = Instant.parse("2026-10-21T03:04:05Z")
    private val clock = FixedBusinessClock(today)
    private val writer get() = ProjectRepository(db, coordinator, clock)
    private val replacements get() = ReplacementRepository(db, coordinator, clock)

    @Before fun setUp() = runBlocking {
        directory = File(context.noBackupFilesDir, "m16-replacement-test-${UUID.randomUUID()}")
        check(directory.mkdirs())
        databaseFile = File(directory, "data.db")
        db = withContext(Dispatchers.IO) {
            Room.databaseBuilder(context, LithicDatabase::class.java, databaseFile.path)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING).build()
        }
        db.initializeDefaults()
    }
    @After fun tearDown() {
        if (::db.isInitialized) db.close()
        if (::directory.isInitialized) {
            check(directory.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
            check(directory.name.startsWith("m16-replacement-test-"))
            directory.deleteRecursively()
        }
    }

    @Test fun one_predecessor_successor_no_cycles() = runBlocking {
        val a = create("a"); val b = create("b"); val c = create("c")
        assertTrue(replacements.link(link("ab", a, b)) is ReplacementMutationResult.Linked)
        assertTrue(replacements.link(link("bc", b, c)) is ReplacementMutationResult.Linked)
        val before = db.replacementDao().listReplacements()
        for ((name, from, to) in listOf(Triple("self", a, a), Triple("cycle", c, a), Triple("fan-out", a, c), Triple("fan-in", c, b))) {
            assertTrue(replacements.link(link(name, from, to)) is ReplacementMutationResult.Conflict)
            assertEquals(before, db.replacementDao().listReplacements())
        }
        val absent = c.copy(id = id("missing"))
        assertTrue(replacements.link(link("missing", a, absent)) is ReplacementMutationResult.Conflict)
        assertEquals(listOf(a.id, b.id, c.id), replacements.loadChain(b.id)!!.items.map { it.project.id })
    }

    @Test fun link_and_unlink_do_not_end_or_write_money() = runBlocking {
        val a = create("a"); val b = create("b")
        val beforeProjects = db.projectDao().listProjects(); val beforeEvents = db.projectDao().listEvents()
        val command = link("ab", a, b)
        val first = replacements.link(command) as ReplacementMutationResult.Linked
        assertEquals(first.copy(replayed = true), replacements.link(command))
        val relation = db.replacementDao().find(first.linkId)!!
        val unlink = UnlinkReplacementCommand(id("unlink"), relation, a, b, replacements.loadLinks())
        assertEquals(ReplacementMutationResult.Unlinked(relation.id, false), replacements.unlink(unlink))
        assertEquals(ReplacementMutationResult.Unlinked(relation.id, true), replacements.unlink(unlink))
        assertEquals(beforeProjects, db.projectDao().listProjects())
        assertEquals(beforeEvents, db.projectDao().listEvents())
        assertTrue(replacements.loadLinks().isEmpty())
    }

    @Test fun trade_in_balance_three_facts_no_duplicate_initial() = runBlocking {
        val old = create("old", 1_000_000)
        val before = writer.loadProjectDetail(old.id)!!
        val command = trade("trade", before)
        assertTrue(replacements.recordTradeIn(command) is ReplacementMutationResult.TradeInRecorded)
        val oldAfter = writer.loadProjectDetail(old.id)!!
        val newAfter = writer.loadProjectDetail(command.newProject.id)!!
        val group = db.projectDao().listEvents().filter { it.exchangeGroupId == command.commandId }
        assertEquals(3, group.size)
        assertEquals(3, group.map { it.operationId }.toSet().size)
        assertEquals(3, group.map { it.id }.toSet().size)
        assertEquals(300_000L, group.single { it.projectId == old.id }.amountCents)
        assertEquals(MoneyDirection.RECOVERY, group.single { it.projectId == old.id }.direction)
        assertEquals(MoneyBasis.TRADE_IN_CREDIT, group.single { it.projectId == old.id }.basis)
        assertEquals(500_000L, group.single { it.role == ProjectMoneyEventRole.INITIAL }.amountCents)
        assertEquals(MoneyBasis.CASH, group.single { it.role == ProjectMoneyEventRole.INITIAL }.basis)
        assertEquals(300_000L, group.single { it.role == ProjectMoneyEventRole.REGULAR }.amountCents)
        assertEquals(800_000L, newAfter.cost.netCents)
        assertEquals(500_000L, oldAfter.cost.netCents + newAfter.cost.netCents - before.cost.netCents)
        assertEquals(1, newAfter.events.count { it.role == ProjectMoneyEventRole.INITIAL })
        assertEquals(today, oldAfter.project.endedOn)
        assertEquals(1L, newAfter.cost.heldDays)
        assertNull(TradeInFacts.validate(db.projectDao().listProjects(), db.projectDao().listEvents()))
    }

    @Test fun trade_in_rolls_back_at_every_write_boundary() = runBlocking {
        val old = create("old")
        val command = trade("rollback", writer.loadProjectDetail(old.id)!!)
        val originalProjects = db.projectDao().listProjects(); val originalEvents = db.projectDao().listEvents()
        val boundaries = listOf(
            "BEFORE UPDATE ON long_term_projects WHEN OLD.id = '${old.id}'",
            "BEFORE INSERT ON long_term_projects WHEN NEW.id = '${command.newProject.id}'",
            "BEFORE INSERT ON project_money_events WHEN NEW.id = '${TradeInIdentities.eventId(command.commandId, TradeInIdentities.OLD_CREDIT)}'",
            "BEFORE INSERT ON project_money_events WHEN NEW.id = '${TradeInIdentities.eventId(command.commandId, TradeInIdentities.NEW_CASH)}'",
            "BEFORE INSERT ON project_money_events WHEN NEW.id = '${TradeInIdentities.eventId(command.commandId, TradeInIdentities.NEW_CREDIT)}'",
            "BEFORE INSERT ON replacements",
        )
        for (boundary in boundaries) {
            sql("CREATE TRIGGER m16_fail $boundary BEGIN SELECT RAISE(ABORT, 'm16 injected rollback'); END")
            val failed = replacements.recordTradeIn(command)
            assertTrue("$boundary -> $failed", failed is ReplacementMutationResult.Conflict || failed is ReplacementMutationResult.StorageFailure)
            assertEquals(originalProjects, db.projectDao().listProjects())
            assertEquals(originalEvents, db.projectDao().listEvents())
            assertTrue(replacements.loadLinks().isEmpty())
            sql("DROP TRIGGER m16_fail")
        }
        assertTrue(replacements.recordTradeIn(command) is ReplacementMutationResult.TradeInRecorded)
        assertIntegrity()
    }

    @Test fun exact_retry_and_double_submit_are_one_complete_transaction() = runBlocking {
        val old = create("old")
        val command = trade("same-command", writer.loadProjectDetail(old.id)!!)
        val results = (1..6).map { async(Dispatchers.IO) { replacements.recordTradeIn(command) } }.awaitAll()
        assertTrue(results.all { it is ReplacementMutationResult.TradeInRecorded })
        assertEquals(1, results.count { !(it as ReplacementMutationResult.TradeInRecorded).replayed })
        assertEquals(2, db.projectDao().listProjects().size)
        assertEquals(4, db.projectDao().listEvents().size)
        assertEquals(1, replacements.loadLinks().size)
        val before = db.projectDao().listEvents()
        assertTrue(replacements.recordTradeIn(command.copy(cashCents = 1)) is ReplacementMutationResult.Conflict)
        assertEquals(before, db.projectDao().listEvents())
    }

    @Test fun zero_cash_and_same_day_are_kept_as_real_facts() = runBlocking {
        val old = create("old", startedOn = today)
        val command = trade("zero", writer.loadProjectDetail(old.id)!!).copy(cashCents = 0, linkReplacement = false)
        assertTrue(replacements.recordTradeIn(command) is ReplacementMutationResult.TradeInRecorded)
        val detail = writer.loadProjectDetail(command.newProject.id)!!
        assertEquals(0L, detail.events.single { it.role == ProjectMoneyEventRole.INITIAL }.amountCents)
        assertEquals(300_000L, detail.cost.netCents)
        assertEquals(1L, detail.cost.heldDays)
        assertEquals(1L, writer.loadProjectDetail(old.id)!!.cost.heldDays)
        assertTrue(replacements.loadLinks().isEmpty())
    }

    @Test fun stale_money_confirmation_cannot_end_or_create_any_project() = runBlocking {
        val old = create("old")
        val command = trade("stale", writer.loadProjectDetail(old.id)!!)
        val event = ProjectMoneyEventEntity(id("late-event"), old.id, MoneyDirection.OUTLAY, 1, today, role = ProjectMoneyEventRole.REGULAR, operationId = id("late-op"))
        assertTrue(writer.appendMoneyEvent(AddProjectMoneyEventCommand(event.operationId, event)) is MutationResult.Success)
        assertTrue(replacements.recordTradeIn(command) is ReplacementMutationResult.Conflict)
        assertEquals(old, db.projectDao().findProject(old.id))
        assertNull(db.projectDao().findProject(command.newProject.id))
        assertEquals(2, db.projectDao().listEvents().size)
    }

    @Test fun relation_conflict_rejects_entire_trade() = runBlocking {
        val old = create("old"); val occupied = create("occupied")
        assertTrue(replacements.link(link("existing", old, occupied)) is ReplacementMutationResult.Linked)
        val command = trade("conflict", writer.loadProjectDetail(old.id)!!)
        val events = db.projectDao().listEvents()
        assertTrue(replacements.recordTradeIn(command) is ReplacementMutationResult.Conflict)
        assertEquals(old, db.projectDao().findProject(old.id))
        assertNull(db.projectDao().findProject(command.newProject.id))
        assertEquals(events, db.projectDao().listEvents())
    }

    @Test fun delete_middle_only_detaches_existing_edges() = runBlocking {
        val a = create("a"); val b = create("b"); val c = create("c")
        replacements.link(link("ab", a, b)); replacements.link(link("bc", b, c))
        val aFacts = writer.loadProjectDetail(a.id)!!; val cFacts = writer.loadProjectDetail(c.id)!!
        val middle = writer.loadProjectDetail(b.id)!!
        val edges = replacements.loadLinks()
        sql("CREATE TRIGGER m16_delete_fail BEFORE DELETE ON project_money_events WHEN OLD.projectId = '${b.id}' BEGIN SELECT RAISE(ABORT, 'm16 delete rollback'); END")
        val failed = writer.deleteProject(DeleteProjectCommand(id("delete-b"), middle.project, middle.events, middle.replacementLinks))
        assertTrue(failed is MutationResult.Conflict || failed is MutationResult.StorageFailure)
        assertEquals(edges, replacements.loadLinks())
        assertEquals(middle.project, db.projectDao().findProject(b.id))
        assertEquals(middle.events, db.projectDao().eventsForProjects(listOf(b.id)))
        sql("DROP TRIGGER m16_delete_fail")
        assertEquals(MutationResult.ProjectDeleted(b.id), writer.deleteProject(DeleteProjectCommand(id("delete-b"), middle.project, middle.events, middle.replacementLinks)))
        assertTrue(replacements.loadLinks().isEmpty())
        assertEquals(aFacts.project, writer.loadProjectDetail(a.id)!!.project)
        assertEquals(aFacts.events, writer.loadProjectDetail(a.id)!!.events)
        assertEquals(cFacts.project, writer.loadProjectDetail(c.id)!!.project)
        assertEquals(cFacts.events, writer.loadProjectDetail(c.id)!!.events)
        assertNull(writer.loadProjectDetail(b.id))
    }

    @Test fun transaction_groups_and_links_survive_production_database_reopen() = runBlocking {
        val old = create("old")
        val command = trade("reopen", writer.loadProjectDetail(old.id)!!)
        assertTrue(replacements.recordTradeIn(command) is ReplacementMutationResult.TradeInRecorded)
        val beforeProjects = db.projectDao().listProjects(); val beforeEvents = db.projectDao().listEvents(); val beforeLinks = replacements.loadLinks()
        db.close()
        db = withContext(Dispatchers.IO) { LithicDatabase.open(context, databaseFile, requireExisting = true) }
        db.requireDefaults()
        assertEquals(beforeProjects, db.projectDao().listProjects())
        assertEquals(beforeEvents, db.projectDao().listEvents())
        assertEquals(beforeLinks, replacements.loadLinks())
        assertEquals(ReplacementMutationResult.TradeInRecorded(old.id, command.newProject.id, command.commandId, true), replacements.recordTradeIn(command))
    }

    @Test fun a_project_can_be_new_then_old_in_separate_transaction_groups() = runBlocking {
        val a = create("a")
        val first = trade("first", writer.loadProjectDetail(a.id)!!)
        assertTrue(replacements.recordTradeIn(first) is ReplacementMutationResult.TradeInRecorded)
        val second = trade("second", writer.loadProjectDetail(first.newProject.id)!!).copy(updatedAt = instant.plusSeconds(2), newProject =
            trade("second", writer.loadProjectDetail(first.newProject.id)!!).newProject.copy(createdAt = instant.plusSeconds(2), updatedAt = instant.plusSeconds(2)))
        assertTrue(replacements.recordTradeIn(second) is ReplacementMutationResult.TradeInRecorded)
        assertEquals(6, db.projectDao().listEvents().count { it.exchangeGroupId != null })
        assertNull(TradeInFacts.validate(db.projectDao().listProjects(), db.projectDao().listEvents()))
        assertEquals(3, replacements.loadChain(first.newProject.id)!!.items.size)
    }

    @Test fun group_money_and_lifecycle_single_endpoint_mutations_are_rejected() = runBlocking {
        val old = create("old")
        val command = trade("protected", writer.loadProjectDetail(old.id)!!)
        assertTrue(replacements.recordTradeIn(command) is ReplacementMutationResult.TradeInRecorded)
        val oldAfter = writer.loadProjectDetail(old.id)!!; val newAfter = writer.loadProjectDetail(command.newProject.id)!!
        val cash = newAfter.events.single { it.role == ProjectMoneyEventRole.INITIAL }
        assertTrue(writer.correctMoneyEvent(CorrectMoneyEventCommand(id("wrong-cash"), newAfter.project, cash, 1, cash.occurredOn, cash.description, instant.plusSeconds(2))) is MutationResult.ValidationFailure)
        assertTrue(writer.updateProject(UpdateProjectCommand(id("wrong-start"), newAfter.project, newAfter.project.name, today.minusDays(1), newAfter.project.categoryId, newAfter.project.note, instant.plusSeconds(2))) is MutationResult.ValidationFailure)
        assertTrue(writer.deleteProject(DeleteProjectCommand(id("wrong-delete"), oldAfter.project, oldAfter.events, oldAfter.replacementLinks)) is MutationResult.ValidationFailure)
        assertEquals(oldAfter.project, writer.loadProjectDetail(old.id)!!.project)
        assertEquals(newAfter.events, writer.loadProjectDetail(command.newProject.id)!!.events)
    }

    @Test fun grouped_end_amount_date_correction_rejects_changes_but_keeps_description_identity() = runBlocking {
        val old = create("old")
        val trade = trade("end-protection", writer.loadProjectDetail(old.id)!!)
        assertTrue(replacements.recordTradeIn(trade) is ReplacementMutationResult.TradeInRecorded)
        val detail = writer.loadProjectDetail(old.id)!!
        val settlement = detail.events.single { it.role == ProjectMoneyEventRole.END_SETTLEMENT }
        val correction = UpdateEndCommand(id("end-correction"), detail.project, settlement, today, "折抵完成", settlement.amountCents,
            settlement.occurredOn, "核实折抵凭证", detail.project.updatedAt.plusSeconds(1))
        for (invalid in listOf(correction.copy(endedOn = today.minusDays(1)), correction.copy(settlementAmountCents = 1), correction.copy(settlementOccurredOn = today.minusDays(1)))) {
            assertTrue(writer.updateEnd(invalid) is MutationResult.ValidationFailure)
            assertEquals(detail.project, writer.loadProjectDetail(old.id)!!.project)
            assertEquals(detail.events, writer.loadProjectDetail(old.id)!!.events)
        }
        assertTrue(writer.updateEnd(correction) is MutationResult.LifecycleApplied)
        val after = writer.loadProjectDetail(old.id)!!
        assertEquals(settlement.copy(description = "核实折抵凭证"), after.events.single { it.id == settlement.id })
        assertEquals("折抵完成", after.project.endReason)
        assertNull(TradeInFacts.validate(db.projectDao().listProjects(), db.projectDao().listEvents()))
    }

    @Test fun grouped_project_allows_metadata_ordinary_events_and_description_without_erasing_basis() = runBlocking {
        val old = create("old")
        val trade = trade("metadata", writer.loadProjectDetail(old.id)!!)
        assertTrue(replacements.recordTradeIn(trade) is ReplacementMutationResult.TradeInRecorded)
        val before = writer.loadProjectDetail(trade.newProject.id)!!
        val category = com.cooper.lithic.data.local.CategoryEntity(id("custom-category"), com.cooper.lithic.data.local.CategoryType.PROJECT, "换新设备", "换新设备", false)
        db.categoryDao().insert(category)
        assertTrue(writer.updateProject(UpdateProjectCommand(id("metadata-edit"), before.project, "新设备", before.project.startedOn, category.id, "仅修改资料", before.project.updatedAt.plusSeconds(1))) is MutationResult.ProjectUpdated)
        val metadata = writer.loadProjectDetail(trade.newProject.id)!!
        assertEquals(before.events, metadata.events)
        val cash = metadata.events.single { it.role == ProjectMoneyEventRole.INITIAL }
        assertTrue(writer.correctMoneyEvent(CorrectMoneyEventCommand(id("description-edit"), metadata.project, cash, cash.amountCents, cash.occurredOn, "实付凭证", metadata.project.updatedAt.plusSeconds(1))) is MutationResult.MoneyCorrected)
        val corrected = writer.loadProjectDetail(trade.newProject.id)!!
        assertEquals(cash.copy(description = "实付凭证"), corrected.events.single { it.id == cash.id })
        val extra = ProjectMoneyEventEntity(id("ordinary-refund"), corrected.project.id, MoneyDirection.RECOVERY, 10_000, today,
            role = ProjectMoneyEventRole.REGULAR, operationId = id("ordinary-refund-op"))
        assertTrue(writer.appendMoneyEvent(AddProjectMoneyEventCommand(extra.operationId, extra)) is MutationResult.Success)
        val after = writer.loadProjectDetail(trade.newProject.id)!!
        assertEquals(790_000L, after.cost.netCents)
        assertEquals("新设备", after.project.name)
        assertEquals(category.id, after.project.categoryId)
        assertNull(TradeInFacts.validate(db.projectDao().listProjects(), db.projectDao().listEvents()))
    }

    @Test fun changed_replacement_confirmation_cannot_silently_delete_new_edges() = runBlocking {
        val a = create("a"); val b = create("b"); val c = create("c")
        replacements.link(link("ab", a, b))
        val confirmed = writer.loadProjectDetail(b.id)!!
        replacements.link(link("bc", b, c))
        val links = replacements.loadLinks()
        assertTrue(writer.deleteProject(DeleteProjectCommand(id("stale-edge-delete"), confirmed.project, confirmed.events, confirmed.replacementLinks)) is MutationResult.Conflict)
        assertEquals(b, db.projectDao().findProject(b.id))
        assertEquals(links, replacements.loadLinks())
    }

    @Test fun relationship_only_writes_invalidate_project_and_comparison_readers() = runBlocking {
        val a = create("a"); val b = create("b")
        val projectSignals = Channel<Unit>(Channel.UNLIMITED)
        val comparisonSignals = Channel<Unit>(Channel.UNLIMITED)
        val projectObserver = launch(Dispatchers.IO) { writer.changes.collect { projectSignals.send(Unit) } }
        val comparisonObserver = launch(Dispatchers.IO) { replacements.changes.collect { comparisonSignals.send(Unit) } }
        try {
            withTimeout(3_000) { projectSignals.receive(); comparisonSignals.receive() }
            val linked = replacements.link(link("ab", a, b)) as ReplacementMutationResult.Linked
            withTimeout(3_000) { projectSignals.receive(); comparisonSignals.receive() }
            assertEquals(2, replacements.loadChain(a.id)!!.items.size)
            val edge = replacements.loadLinks().single { it.id == linked.linkId }
            assertTrue(replacements.unlink(UnlinkReplacementCommand(id("unlink-observed"), edge, a, b, replacements.loadLinks())) is ReplacementMutationResult.Unlinked)
            withTimeout(3_000) { projectSignals.receive(); comparisonSignals.receive() }
            assertEquals(1, replacements.loadChain(a.id)!!.items.size)
            assertTrue(writer.loadProjectDetail(a.id)!!.replacementLinks.isEmpty())
        } finally { projectObserver.cancel(); comparisonObserver.cancel() }
    }

    @Test fun corrupt_group_read_is_an_error_and_never_an_empty_or_partial_result() = runBlocking {
        val old = create("old")
        val trade = trade("corrupt-read", writer.loadProjectDetail(old.id)!!)
        assertTrue(replacements.recordTradeIn(trade) is ReplacementMutationResult.TradeInRecorded)
        sql("UPDATE project_money_events SET amountCents = 1 WHERE id = '${TradeInIdentities.eventId(trade.commandId, TradeInIdentities.NEW_CREDIT)}'")
        var projectRejected = false
        try { writer.loadProjectDetail(old.id) } catch (_: IllegalStateException) { projectRejected = true }
        assertTrue(projectRejected)
        var chainRejected = false
        try { replacements.loadChain(old.id) } catch (_: IllegalStateException) { chainRejected = true }
        assertTrue(chainRejected)
        val snapshot = db.projectDao().findProject(old.id)!!
        assertTrue(writer.updateProject(UpdateProjectCommand(id("corrupt-metadata"), snapshot, "not written", snapshot.startedOn,
            snapshot.categoryId, snapshot.note, snapshot.updatedAt.plusSeconds(1))) is MutationResult.StorageFailure)
        assertEquals(snapshot, db.projectDao().findProject(old.id))
        assertEquals(2, db.projectDao().listProjects().size)
        assertEquals(4, db.projectDao().listEvents().size)
    }

    @Test fun single_payment_limits_and_combined_cost_are_not_confused() = runBlocking {
        val old = create("old")
        val trade = trade("amount-limits", writer.loadProjectDetail(old.id)!!)
        for (invalid in listOf(trade.copy(cashCents = -1), trade.copy(creditCents = com.cooper.lithic.core.model.Money.MAX_CENTS + 1))) {
            assertTrue(replacements.recordTradeIn(invalid) is ReplacementMutationResult.ValidationFailure)
            assertEquals(listOf(old), db.projectDao().listProjects())
            assertEquals(1, db.projectDao().listEvents().size)
        }
        val maximum = trade.copy(cashCents = com.cooper.lithic.core.model.Money.MAX_CENTS, creditCents = com.cooper.lithic.core.model.Money.MAX_CENTS)
        assertTrue(replacements.recordTradeIn(maximum) is ReplacementMutationResult.TradeInRecorded)
        assertEquals(Math.multiplyExact(com.cooper.lithic.core.model.Money.MAX_CENTS, 2), writer.loadProjectDetail(maximum.newProject.id)!!.cost.netCents)
    }

    @Test fun old_category_type_is_reread_inside_trade_transaction_before_any_write() = runBlocking {
        val initial = create("old-category")
        val category = com.cooper.lithic.data.local.CategoryEntity(id("old-custom-category"), com.cooper.lithic.data.local.CategoryType.PROJECT,
            "需要核对的类别", "需要核对的类别", false)
        db.categoryDao().insert(category)
        assertTrue(writer.updateProject(UpdateProjectCommand(id("old-category-edit"), initial, initial.name, initial.startedOn, category.id,
            initial.note, initial.updatedAt.plusSeconds(1))) is MutationResult.ProjectUpdated)
        val detail = writer.loadProjectDetail(initial.id)!!
        val command = trade("old-category-check", detail)
        sql("UPDATE categories SET type='SUBSCRIPTION' WHERE id='${category.id}'")
        val result = replacements.recordTradeIn(command)
        assertTrue(result is ReplacementMutationResult.ValidationFailure)
        assertEquals(detail.project, db.projectDao().findProject(initial.id))
        assertEquals(detail.events, db.projectDao().eventsForProjects(listOf(initial.id)))
        assertNull(db.projectDao().findProject(command.newProject.id))
        assertTrue(replacements.loadLinks().isEmpty())
    }

    private suspend fun create(name: String, amount: Long = 1_000_000, startedOn: LocalDate = today.minusDays(9)): LongTermProjectEntity {
        val project = LongTermProjectEntity(id("project:$name"), name, LithicDatabase.PROJECT_FALLBACK_ID, startedOn, createdAt = instant, updatedAt = instant)
        val initial = ProjectMoneyEventEntity(id("initial:$name"), project.id, MoneyDirection.OUTLAY, amount, startedOn, role = ProjectMoneyEventRole.INITIAL, operationId = id("create:$name"))
        assertTrue(writer.createProject(CreateProjectCommand(initial.operationId, project, initial)) is MutationResult.Success)
        return project
    }
    private suspend fun link(name: String, a: LongTermProjectEntity, b: LongTermProjectEntity) =
        LinkReplacementCommand(id(name), a, b, replacements.loadLinks(), instant)
    private suspend fun trade(name: String, old: ProjectDetail): TradeInCommand {
        val commandId = id("trade:$name")
        val revision = old.project.updatedAt.plusSeconds(1)
        val fresh = LongTermProjectEntity(TradeInIdentities.newProjectId(commandId), "新项目-$name", LithicDatabase.PROJECT_FALLBACK_ID,
            today, createdAt = revision, updatedAt = revision)
        return TradeInCommand(commandId, old.project, old.events, replacements.loadLinks(), fresh, today, 500_000, 300_000, true, revision)
    }
    private fun id(value: String) = UUID.nameUUIDFromBytes("m16-test:$value".toByteArray(Charsets.UTF_8)).toString()
    private suspend fun sql(value: String) = withContext(Dispatchers.IO) { db.openHelper.writableDatabase.execSQL(value) }
    private suspend fun assertIntegrity() = withContext(Dispatchers.IO) {
        db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        db.openHelper.writableDatabase.query("PRAGMA quick_check").use { assertTrue(it.moveToFirst()); assertEquals("ok", it.getString(0)) }
    }
}
