package com.cooper.lithic.data.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.*
import com.cooper.lithic.data.local.*
import com.cooper.lithic.domain.report.*
import com.cooper.lithic.domain.subscription.*
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Fresh disposable synthetic roots only. A compiled test is not an Android execution PASS. */
@RunWith(AndroidJUnit4::class)
class M18TimelineRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val today = LocalDate.of(2026, 10, 21)
    private val time = Instant.parse("2026-10-21T04:00:00Z")
    private val clock = FixedBusinessClock(today)
    private val coordinator = LocalWriteCoordinator()
    private lateinit var root: File
    private lateinit var db: LithicDatabase
    @Volatile private var queryGate: ((String) -> Unit)? = null
    private val reports get() = ReportRepository(db, coordinator, clock)

    @Before fun before(): Unit = runBlocking {
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m18-ledger-${id()}").canonicalFile
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.mkdir())
        db = Room.databaseBuilder(context, LithicDatabase::class.java, File(root, "synthetic.db").path)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .setQueryCallback({ sql, _ -> queryGate?.invoke(sql) }, { it.run() }).build()
        db.initializeDefaults()
    }

    @After fun after() {
        queryGate = null
        if (::db.isInitialized) db.close()
        if (::root.isInitialized && root.exists()) {
            check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.name.startsWith("m18-ledger-"))
            check(root.deleteRecursively())
        }
    }

    @Test fun complete_snapshot_and_today_match_home_beyond_fifty_records_without_mutating_facts(): Unit = runBlocking {
        repeat(61) { project("合成项目$it", today.minusDays(30L + it), cents = it * 100L) }
        val negative = project("真实负成本", today.minusDays(9), cents = 100)
        event(negative.id, MoneyDirection.RECOVERY, 300, today.minusDays(3))
        project("已结束仍需历史", today.minusYears(2), today.minusDays(4), 100_000)
        val subscription = subscription()
        payment(subscription.id, today.minusDays(9), today.minusDays(9), today.plusDays(21), 15_000)
        payment(subscription.id, today.minusDays(1), today.plusDays(21), today.plusDays(51), 18_000)
        val before = facts()
        val input = reports.loadLedgerSnapshot(today)
        assertEquals(63, input.overview.projects.size)
        val last = CostTimelineEngine.point(input, today)
        val sameFacts = OverviewEngine.evaluate(input.overview, today)
        val home = reports.loadOverview(today)
        listOf(sameFacts, home).forEach {
            assertEquals(0, it.totalRate.compareTo(last.totalRate))
            assertEquals(0, it.projectRate.compareTo(last.projectRate))
            assertEquals(0, it.subscriptionRate.compareTo(last.subscriptionRate))
        }
        assertEquals(0, BigDecimal("5").compareTo(last.subscriptionRate))
        assertTrue(last.hasNegativeContribution)
        assertEquals(before, facts())
    }

    @Test fun full_ledger_read_excludes_a_coordinator_queued_write_from_every_captured_record(): Unit = runBlocking {
        val project = project("一致性", today.minusDays(9), cents = 100_000)
        val paused = CountDownLatch(1)
        val release = CountDownLatch(1)
        val writerStarted = CountDownLatch(1)
        val once = AtomicBoolean(true)
        queryGate = { sql ->
            if (sql.contains("FROM project_money_events WHERE projectId IN", true) && once.compareAndSet(true, false)) {
                check(db.inTransaction()); paused.countDown(); check(release.await(10, TimeUnit.SECONDS))
            }
        }
        val reading = async(Dispatchers.IO) { reports.loadLedgerSnapshot(today) }
        val writing = async(Dispatchers.IO) {
            check(paused.await(5, TimeUnit.SECONDS)); writerStarted.countDown()
            coordinator.withWrite { db.withTransaction { event(project.id, MoneyDirection.OUTLAY, 200_000, today) } }
        }
        try {
            assertTrue(withContext(Dispatchers.IO) { writerStarted.await(5, TimeUnit.SECONDS) })
            delay(100); assertFalse(writing.isCompleted)
        } finally { queryGate = null; release.countDown() }
        val old = reading.await()
        writing.await()
        val current = reports.loadLedgerSnapshot(today)
        assertEquals(1, old.overview.projects.single().snapshot.moneyEvents.size)
        assertEquals(2, current.overview.projects.single().snapshot.moneyEvents.size)
        assertEquals(0, BigDecimal("100").compareTo(CostTimelineEngine.point(old, today).totalRate))
        assertEquals(0, BigDecimal("300").compareTo(CostTimelineEngine.point(current, today).totalRate))
    }

    @Test fun same_day_events_cross_fifty_preserve_identity_and_post_end_money_without_false_contribution(): Unit = runBlocking {
        val jan = LocalDate.of(2026, 1, 1)
        val ended = project("Q02", jan, jan.plusDays(9), 100_000)
        event(ended.id, MoneyDirection.RECOVERY, 20_000, jan.plusDays(19))
        val ongoing = project("同日完整事件", jan, cents = 0)
        repeat(55) { event(ongoing.id, MoneyDirection.OUTLAY, it + 1L, jan.plusDays(19)) }
        val before = facts()
        val captured = reports.loadLedgerSnapshot(today)
        val related = captured.events.filter { it.date == jan.plusDays(19) }
        assertEquals(56, related.size)
        assertEquals(56, related.map { it.id }.toSet().size)
        assertTrue(related.all { it.recordName.isNotBlank() })
        assertEquals(0, CostTimelineEngine.point(captured, jan.plusDays(9)).totalRate.signum())
        assertEquals(0, CostTimelineEngine.point(captured, jan.plusDays(19)).projectRate.compareTo(BigDecimal("0.77")))
        assertEquals(before, facts())
    }

    @Test fun trade_in_contribution_uses_saved_three_money_events_once_and_keeps_noncash_labels(): Unit = runBlocking {
        val old = project("合成旧机", today.minusYears(3), cents = 900_000)
        val oldDetail = checkNotNull(ProjectRepository(db, coordinator, clock).loadProjectDetail(old.id))
        val operation = id()
        val revision = time.plusSeconds(1)
        val next = LongTermProjectEntity(TradeInIdentities.newProjectId(operation), "合成新机",
            LithicDatabase.PROJECT_FALLBACK_ID, today, createdAt = revision, updatedAt = revision)
        val result = ReplacementRepository(db, coordinator, clock).recordTradeIn(TradeInCommand(operation, old,
            oldDetail.events, emptyList(), next, today, 500_000, 300_000, true, revision))
        assertTrue(result is ReplacementMutationResult.TradeInRecorded)
        val before = facts()
        val captured = reports.loadLedgerSnapshot(today)
        val point = CostTimelineEngine.point(captured, today)
        assertEquals(0, BigDecimal("8000").compareTo(point.projectRate))
        assertEquals(3, captured.events.count { it.date == today && it.amountCents != null })
        assertEquals(2, captured.events.count { it.nonCash })
        assertEquals(before, facts())
    }

    @Test fun corrupt_reference_rejects_whole_snapshot_instead_of_fake_empty_or_zero(): Unit = runBlocking {
        val project = project("坏类别引用", today, cents = 0)
        db.openHelper.writableDatabase.execSQL("UPDATE long_term_projects SET categoryId=? WHERE id=?",
            arrayOf(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, project.id))
        assertNotNull(try { reports.loadLedgerSnapshot(today); null } catch (failure: Exception) { failure })
    }

    @Test fun ordinary_correction_and_confirmed_deletion_rebuild_history_from_present_facts(): Unit = runBlocking {
        val project = project("允许纠错和删除的普通项目", today.minusDays(9), cents = 100_000)
        val writer = ProjectRepository(db, coordinator, clock)
        val original = checkNotNull(writer.loadProjectDetail(project.id))
        val date = today.minusDays(5)
        val before = reports.loadLedgerSnapshot(today)
        val corrected = writer.correctMoneyEvent(CorrectMoneyEventCommand(id(), original.project,
            original.events.single(), 200_000, project.startedOn, "纠正已录入的真实金额", time.plusSeconds(1)))
        assertTrue(corrected is MutationResult.MoneyCorrected)
        val changed = reports.loadLedgerSnapshot(today)
        assertEquals(0, CostTimelineEngine.point(before, date).totalRate.multiply(BigDecimal("2"))
            .compareTo(CostTimelineEngine.point(changed, date).totalRate))
        val current = checkNotNull(writer.loadProjectDetail(project.id))
        assertTrue(writer.deleteProject(DeleteProjectCommand(id(), current.project, current.events, current.replacementLinks)) is MutationResult.ProjectDeleted)
        val absent = reports.loadLedgerSnapshot(today)
        assertTrue(absent.overview.projects.isEmpty())
        assertTrue(absent.events.isEmpty())
        assertEquals(0, CostTimelineEngine.point(absent, date).totalRate.signum())
        assertEquals(2, before.events.size)
    }

    @Test fun format_two_roundtrip_rebuilds_total_events_and_retired_generation_cannot_read(): Unit = runBlocking {
        val sourceOwner = AppContainer(DataGenerationStore(context, File(root, "source")), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        val targetOwner = AppContainer(DataGenerationStore(context, File(root, "target")), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        try {
            sourceOwner.start(ZoneId.of("Asia/Shanghai")); targetOwner.start(ZoneId.of("UTC"))
            val source = sourceOwner.openStorage()
            val old = targetOwner.openStorage()
            val pictures = RestoreSyntheticFixture.insert(source)
            val captured = ReportRepository(source.database, source.coordinator, clock).loadLedgerSnapshot(today)
            val oldReader = ReportRepository(old.database, old.coordinator, clock)
            val before = source.backupSnapshotSource().capture()
            val archive = File(root, "m18-controlled-target.lithicbackup")
            assertTrue(BackupExportService(context, source).export(RestoreSyntheticFixture.target(archive)) is BackupExportResult.Success)
            val restorer = RestoreCoordinator(context, targetOwner)
            val preview = restorer.prepare { archive.inputStream() }
            assertEquals(RestoreResult.Success, restorer.confirm(preview.token))
            val restored = targetOwner.openStorage()
            val actual = ReportRepository(restored.database, restored.coordinator, clock).loadLedgerSnapshot(today)
            assertEquals(captured, actual)
            assertEquals(CostTimelineEngine.point(captured, today), CostTimelineEngine.point(actual, today))
            assertEquals(before, restored.backupSnapshotSource().capture())
            pictures.forEach { (path, bytes) -> assertArrayEquals(bytes, File(restored.directory, path).readBytes()) }
            assertNotNull(try { oldReader.loadLedgerSnapshot(today); null } catch (failure: Exception) { failure })
        } finally { sourceOwner.close(); targetOwner.close() }
    }

    private suspend fun project(name: String, start: LocalDate, end: LocalDate? = null, cents: Long): LongTermProjectEntity {
        val row = LongTermProjectEntity(id(), name, LithicDatabase.PROJECT_FALLBACK_ID, start, end,
            endReason = end?.let { "合成结束" }, createdAt = time, updatedAt = time)
        db.projectDao().insertProject(row)
        db.projectDao().insertEvent(ProjectMoneyEventEntity(id(), row.id, MoneyDirection.OUTLAY, cents, start,
            role = ProjectMoneyEventRole.INITIAL, operationId = id()))
        return row
    }
    private suspend fun event(projectId: String, direction: MoneyDirection, cents: Long, date: LocalDate) {
        db.projectDao().insertEvent(ProjectMoneyEventEntity(id(), projectId, direction, cents, date,
            "合成真实发生", ProjectMoneyEventRole.REGULAR, id()))
    }
    private suspend fun subscription(): SubscriptionEntity = SubscriptionEntity(id(), "合成预付服务",
        LithicDatabase.SUBSCRIPTION_FALLBACK_ID, createdAt = time, updatedAt = time).also { db.subscriptionDao().insertSubscription(it) }
    private suspend fun payment(subscriptionId: String, paid: LocalDate, from: LocalDate, until: LocalDate, cents: Long) {
        db.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(id(), subscriptionId, paid, from, until, until,
            cents, BillingCycle.MONTHLY, 30, id()))
    }
    private suspend fun facts(): List<Any> {
        val ids = db.subscriptionDao().validationSubscriptions(null, 500).map { it.id }
        return listOf(db.categoryDao().list(), db.projectDao().listProjects(), db.projectDao().listEvents(),
            db.subscriptionDao().subscriptionsForIds(ids), db.subscriptionDao().renewalsForSubscriptions(ids),
            db.subscriptionDao().statesForSubscriptions(ids), db.replacementDao().listReplacements())
    }
    private fun id() = UUID.randomUUID().toString()
}
