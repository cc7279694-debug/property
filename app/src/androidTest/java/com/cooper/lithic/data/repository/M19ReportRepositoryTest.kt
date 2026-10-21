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

/** Actual Room/transaction tests, restricted to new disposable M19 synthetic roots. */
@RunWith(AndroidJUnit4::class)
class M19ReportRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val today = LocalDate.of(2029, 6, 10)
    private val time = Instant.parse("2026-10-21T04:00:00Z")
    private val clock = FixedBusinessClock(today)
    private val coordinator = LocalWriteCoordinator()
    private lateinit var root: File
    private lateinit var db: LithicDatabase
    @Volatile private var queryGate: ((String) -> Unit)? = null
    private val reports get() = ReportRepository(db, coordinator, clock)

    @Before fun before(): Unit = runBlocking {
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m19-reports-${id()}").canonicalFile
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
            check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.name.startsWith("m19-reports-"))
            check(root.deleteRecursively())
        }
    }

    @Test fun typed_facts_read_every_project_subscription_and_payment_beyond_fifty_without_writing(): Unit = runBlocking {
        repeat(57) { index ->
            val row = project("合成项目$index", LocalDate.of(2026, 1, 1), cents = 100)
            event(row.id, MoneyDirection.OUTLAY, 10, LocalDate.of(2028, 4, 3))
            val service = subscription("合成订阅$index")
            payment(service.id, LocalDate.of(2026, 12, 20), LocalDate.of(2027, 1, 1), LocalDate.of(2028, 1, 1), 36_500)
        }
        val before = facts()
        val snapshot = reports.loadLedgerSnapshot(today)
        assertEquals(57, snapshot.overview.projects.size)
        assertEquals(57, snapshot.overview.subscriptions.size)
        assertEquals(171, snapshot.financialFacts!!.size)
        assertEquals(171, snapshot.financialFacts!!.map { it.id }.toSet().size)
        assertEquals(114, snapshot.recordIdentities.size)
        val first = AnnualReportEngine.evaluate(snapshot, 2026, today)
        val maintenance = AnnualReportEngine.evaluate(snapshot, 2028, today)
        val coverageOnly = AnnualReportEngine.evaluate(snapshot, 2027, today)
        assertEquals(5_700L, first.amounts.projectOutlayCents)
        assertEquals(2_080_500L, first.amounts.subscriptionPaidCents)
        assertEquals(570L, maintenance.amounts.projectOutlayCents)
        assertFalse(coverageOnly.amounts.hasRecords)
        val overview = reports.loadOverview(today)
        val categories = CategoryReportEngine.evaluate(snapshot, today)
        val projectRate = categories.filter { it.category.kind == OverviewKind.PROJECT }
            .fold(BigDecimal.ZERO) { sum, item -> sum + item.currentRate }
        val subscriptionRate = categories.filter { it.category.kind == OverviewKind.SUBSCRIPTION }
            .fold(BigDecimal.ZERO) { sum, item -> sum + item.currentRate }
        assertEquals(0, overview.projectRate.compareTo(projectRate))
        assertEquals(0, overview.subscriptionRate.compareTo(subscriptionRate))
        assertEquals(before, facts())
    }

    @Test fun queued_write_cannot_mix_typed_money_with_a_different_overview_version(): Unit = runBlocking {
        val row = project("合成快照隔离", LocalDate.of(2026, 1, 1), cents = 800_000)
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
            coordinator.withWrite { db.withTransaction { event(row.id, MoneyDirection.OUTLAY, 80_000, LocalDate.of(2028, 1, 1)) } }
        }
        try {
            assertTrue(withContext(Dispatchers.IO) { writerStarted.await(5, TimeUnit.SECONDS) })
            delay(100); assertFalse(writing.isCompleted)
        } finally { queryGate = null; release.countDown() }
        val before = reading.await(); writing.await()
        val after = reports.loadLedgerSnapshot(today)
        assertEquals(1, before.financialFacts!!.size)
        assertEquals(1, before.overview.projects.single().snapshot.moneyEvents.size)
        assertFalse(AnnualReportEngine.evaluate(before, 2028, today).amounts.hasRecords)
        assertEquals(2, after.financialFacts!!.size)
        assertEquals(2, after.overview.projects.single().snapshot.moneyEvents.size)
        assertEquals(80_000L, AnnualReportEngine.evaluate(after, 2028, today).amounts.projectOutlayCents)
    }

    @Test fun actual_category_rename_and_merge_restate_all_years_without_changing_money_facts(): Unit = runBlocking {
        val categories = CategoryRepository(db, coordinator)
        val sourceId = id(); val targetId = id()
        assertTrue(categories.create(CategoryType.PROJECT, "合成设备", sourceId) is CategoryMutationResult.Applied)
        assertTrue(categories.create(CategoryType.PROJECT, "合成长期物品", targetId) is CategoryMutationResult.Applied)
        val row = project("合成旧物", LocalDate.of(2026, 2, 1), LocalDate.of(2027, 2, 1), 800_000, sourceId)
        event(row.id, MoneyDirection.OUTLAY, 80_000, LocalDate.of(2028, 4, 1))
        event(row.id, MoneyDirection.RECOVERY, 20_000, LocalDate.of(2028, 5, 1))
        val original = reports.loadLedgerSnapshot(today)
        val moneyBefore = db.projectDao().listEvents()
        assertEquals(800_000L, AnnualReportEngine.evaluate(original, 2026, today).categories.single { it.category.id == sourceId }.amounts.projectOutlayCents)
        val originalCategory = checkNotNull(db.categoryDao().find(sourceId))
        assertTrue(categories.rename(originalCategory, "合成设备改名") is CategoryMutationResult.Applied)
        val renamed = reports.loadLedgerSnapshot(today)
        assertEquals("合成设备改名", CategoryReportEngine.evaluate(renamed, today).single { it.category.id == sourceId }.category.name)
        assertEquals(original.financialFacts, renamed.financialFacts)
        assertTrue(categories.merge(categories.previewMerge(sourceId, targetId)) is CategoryMutationResult.Applied)
        val merged = reports.loadLedgerSnapshot(today)
        assertNull(db.categoryDao().find(sourceId))
        assertEquals(moneyBefore, db.projectDao().listEvents())
        assertEquals(original.financialFacts, merged.financialFacts)
        assertEquals(targetId, merged.recordIdentities.single { it.id == row.id }.categoryId)
        listOf(2026, 2028).forEach { year ->
            assertEquals(AnnualReportEngine.evaluate(original, year, today).amounts,
                AnnualReportEngine.evaluate(merged, year, today).amounts)
        }
        val category = CategoryReportEngine.evaluate(merged, today).single { it.category.id == targetId }
        assertEquals(0, category.currentRate.signum())
        assertEquals(880_000L, category.historical.projectOutlayCents)
        assertEquals(20_000L, category.historical.projectRecoveryCents)
        assertEquals(setOf(row.id), category.records.map { it.id }.toSet())
        assertEquals(setOf(2026, 2028), category.annual.map { it.year }.toSet())
    }

    @Test fun trade_in_reads_three_saved_facts_once_and_payment_year_can_be_corrected(): Unit = runBlocking {
        val old = project("合成旧机", LocalDate.of(2026, 1, 1), cents = 900_000)
        val detail = checkNotNull(ProjectRepository(db, coordinator, clock).loadProjectDetail(old.id))
        val operation = id(); val tradeDate = LocalDate.of(2028, 2, 1); val revision = time.plusSeconds(1)
        val next = LongTermProjectEntity(TradeInIdentities.newProjectId(operation), "合成新机",
            LithicDatabase.PROJECT_FALLBACK_ID, tradeDate, createdAt = revision, updatedAt = revision)
        assertTrue(ReplacementRepository(db, coordinator, clock).recordTradeIn(TradeInCommand(operation, old,
            detail.events, emptyList(), next, tradeDate, 500_000, 300_000, true, revision)) is ReplacementMutationResult.TradeInRecorded)
        val service = subscription("合成跨年年付")
        payment(service.id, LocalDate.of(2027, 12, 30), LocalDate.of(2028, 1, 1), LocalDate.of(2029, 1, 1), 36_500)
        val before = reports.loadLedgerSnapshot(today)
        val trade = AnnualReportEngine.evaluate(before, 2028, today).amounts
        assertEquals(800_000L, trade.projectOutlayCents)
        assertEquals(300_000L, trade.projectRecoveryCents)
        assertEquals(500_000L, trade.projectNetCents)
        assertTrue(trade.containsNonCash)
        assertEquals(3, before.financialFacts!!.count { it.date == tradeDate })
        assertEquals(2, before.financialFacts!!.count { it.nonCash })
        assertEquals(36_500L, AnnualReportEngine.evaluate(before, 2027, today).amounts.subscriptionPaidCents)
        val writer = SubscriptionRepository(db, coordinator, clock)
        val current = checkNotNull(writer.loadSubscriptionDetail(service.id))
        val payment = current.renewals.single()
        assertTrue(writer.updateRenewal(UpdateSubscriptionRenewalCommand(id(), current.facts(), payment,
            payment.copy(paidOn = LocalDate.of(2028, 1, 1)))) is SubscriptionLifecycleResult.Saved)
        val corrected = reports.loadLedgerSnapshot(today)
        assertEquals(0L, AnnualReportEngine.evaluate(corrected, 2027, today).amounts.subscriptionPaidCents)
        assertEquals(36_500L, AnnualReportEngine.evaluate(corrected, 2028, today).amounts.subscriptionPaidCents)
        assertEquals(4, corrected.financialFacts!!.count { it.kind != FinancialFactKind.SUBSCRIPTION_PAYMENT })
    }

    @Test fun correcting_occurrence_year_and_confirmed_deletion_rebuild_reports_from_current_facts(): Unit = runBlocking {
        val row = project("合成普通可纠错", LocalDate.of(2026, 1, 1), cents = 800_000)
        event(row.id, MoneyDirection.OUTLAY, 80_000, LocalDate.of(2028, 1, 1))
        val writer = ProjectRepository(db, coordinator, clock)
        val before = checkNotNull(writer.loadProjectDetail(row.id))
        val repair = before.events.single { it.role == ProjectMoneyEventRole.REGULAR }
        assertTrue(writer.correctMoneyEvent(CorrectMoneyEventCommand(id(), before.project, repair, 80_000,
            LocalDate.of(2027, 12, 31), "合成真实发生年纠错", time.plusSeconds(1))) is MutationResult.MoneyCorrected)
        val changed = reports.loadLedgerSnapshot(today)
        assertEquals(800_000L, AnnualReportEngine.evaluate(changed, 2026, today).amounts.projectOutlayCents)
        assertEquals(80_000L, AnnualReportEngine.evaluate(changed, 2027, today).amounts.projectOutlayCents)
        assertFalse(AnnualReportEngine.evaluate(changed, 2028, today).amounts.hasRecords)
        val current = checkNotNull(writer.loadProjectDetail(row.id))
        assertTrue(writer.deleteProject(DeleteProjectCommand(id(), current.project, current.events, current.replacementLinks)) is MutationResult.ProjectDeleted)
        val absent = reports.loadLedgerSnapshot(today)
        assertTrue(absent.financialFacts!!.isEmpty())
        assertFalse(AnnualReportEngine.evaluate(absent, 2026, today).amounts.hasRecords)
    }

    @Test fun corrupt_category_or_incomplete_trade_in_rejects_whole_report_read(): Unit = runBlocking {
        val row = project("合成坏引用", today, cents = 0)
        db.openHelper.writableDatabase.execSQL("UPDATE long_term_projects SET categoryId=? WHERE id=?",
            arrayOf(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, row.id))
        assertNotNull(try { reports.loadLedgerSnapshot(today); null } catch (failure: Exception) { failure })
        db.openHelper.writableDatabase.execSQL("UPDATE long_term_projects SET categoryId=? WHERE id=?",
            arrayOf(LithicDatabase.PROJECT_FALLBACK_ID, row.id))
        val money = db.projectDao().listEvents().single()
        db.openHelper.writableDatabase.execSQL("UPDATE project_money_events SET basis=?,exchangeGroupId=? WHERE id=?",
            arrayOf(MoneyBasis.TRADE_IN_CREDIT.name, id(), money.id))
        assertNotNull(try { reports.loadLedgerSnapshot(today); null } catch (failure: Exception) { failure })
    }

    @Test fun format_two_roundtrip_preserves_annual_category_media_facts_and_rejects_old_generation(): Unit = runBlocking {
        val sourceOwner = AppContainer(DataGenerationStore(context, File(root, "source")), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        val targetOwner = AppContainer(DataGenerationStore(context, File(root, "target")), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        try {
            sourceOwner.start(ZoneId.of("Asia/Shanghai")); targetOwner.start(ZoneId.of("UTC"))
            val source = sourceOwner.openStorage(); val old = targetOwner.openStorage()
            val pictures = RestoreSyntheticFixture.insert(source)
            val captured = ReportRepository(source.database, source.coordinator, clock).loadLedgerSnapshot(today)
            val oldReader = ReportRepository(old.database, old.coordinator, clock)
            val before = source.backupSnapshotSource().capture()
            val archive = File(root, "m19-controlled-target.lithicbackup")
            assertTrue(BackupExportService(context, source).export(RestoreSyntheticFixture.target(archive)) is BackupExportResult.Success)
            val restorer = RestoreCoordinator(context, targetOwner)
            val preview = restorer.prepare { archive.inputStream() }
            assertEquals(RestoreResult.Success, restorer.confirm(preview.token))
            val restored = targetOwner.openStorage()
            val actual = ReportRepository(restored.database, restored.coordinator, clock).loadLedgerSnapshot(today)
            assertEquals(captured, actual)
            val years = captured.financialFacts!!.map { it.date.year }.toSet()
            years.forEach { assertEquals(AnnualReportEngine.evaluate(captured, it, today), AnnualReportEngine.evaluate(actual, it, today)) }
            assertEquals(CategoryReportEngine.evaluate(captured, today), CategoryReportEngine.evaluate(actual, today))
            assertEquals(before, restored.backupSnapshotSource().capture())
            pictures.forEach { (path, bytes) -> assertArrayEquals(bytes, File(restored.directory, path).readBytes()) }
            assertNotNull(try { oldReader.loadLedgerSnapshot(today); null } catch (failure: Exception) { failure })
        } finally { sourceOwner.close(); targetOwner.close() }
    }

    private suspend fun project(name: String, start: LocalDate, end: LocalDate? = null, cents: Long,
        category: String = LithicDatabase.PROJECT_FALLBACK_ID): LongTermProjectEntity {
        val row = LongTermProjectEntity(id(), name, category, start, end, endReason = end?.let { "合成结束" }, createdAt = time, updatedAt = time)
        db.projectDao().insertProject(row)
        db.projectDao().insertEvent(ProjectMoneyEventEntity(id(), row.id, MoneyDirection.OUTLAY, cents, start,
            role = ProjectMoneyEventRole.INITIAL, operationId = id()))
        return row
    }
    private suspend fun event(projectId: String, direction: MoneyDirection, cents: Long, date: LocalDate) {
        db.projectDao().insertEvent(ProjectMoneyEventEntity(id(), projectId, direction, cents, date,
            "合成真实发生", ProjectMoneyEventRole.REGULAR, id()))
    }
    private suspend fun subscription(name: String) = SubscriptionEntity(id(), name, LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
        createdAt = time, updatedAt = time).also { db.subscriptionDao().insertSubscription(it) }
    private suspend fun payment(subscriptionId: String, paid: LocalDate, from: LocalDate, until: LocalDate, cents: Long) {
        db.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(id(), subscriptionId, paid, from, until, until,
            cents, BillingCycle.YEARLY, 365, id()))
    }
    private suspend fun facts(): List<Any> {
        val ids = db.subscriptionDao().validationSubscriptions(null, 500).map { it.id }
        return listOf(db.categoryDao().list(), db.projectDao().listProjects(), db.projectDao().listEvents(),
            db.subscriptionDao().subscriptionsForIds(ids), db.subscriptionDao().renewalsForSubscriptions(ids),
            db.subscriptionDao().statesForSubscriptions(ids), db.replacementDao().listReplacements())
    }
    private fun id() = UUID.randomUUID().toString()
}
