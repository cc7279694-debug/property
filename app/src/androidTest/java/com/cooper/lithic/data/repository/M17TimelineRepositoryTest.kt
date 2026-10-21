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
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.*
import com.cooper.lithic.data.local.*
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.report.ProjectTimelineEngine
import com.cooper.lithic.domain.report.SubscriptionTimelineEngine
import com.cooper.lithic.domain.subscription.*
import java.io.File
import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Only new synthetic UUID roots; never opens MainActivity, the default ledger or another device. */
@RunWith(AndroidJUnit4::class)
class M17TimelineRepositoryTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val today = LocalDate.of(2026, 10, 21)
    private val writtenAt = Instant.parse("2026-10-21T04:00:00Z")
    private val clock = FixedBusinessClock(today)
    private val coordinator = LocalWriteCoordinator()
    private lateinit var root: File
    private lateinit var db: LithicDatabase
    @Volatile private var queryGate: ((String) -> Unit)? = null
    private val projects get() = ProjectRepository(db, coordinator, clock)
    private val subscriptions get() = SubscriptionRepository(db, coordinator, clock)
    private val repository get() = ItemTimelineRepository(projects, subscriptions, clock)

    @Before fun before(): Unit = runBlocking {
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m17-timeline-${id()}").canonicalFile
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.mkdir())
        db = Room.databaseBuilder(context, LithicDatabase::class.java, File(root, "synthetic.db").path)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .setQueryCallback({ sql, _ -> queryGate?.invoke(sql) }, { runnable -> runnable.run() })
            .build()
        db.initializeDefaults()
    }

    @After fun after() {
        queryGate = null
        if (::db.isInitialized) db.close()
        if (::root.isInitialized && root.exists()) {
            check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.name.startsWith("m17-timeline-"))
            check(root.deleteRecursively())
        }
    }

    @Test fun complete_project_facts_cross_fifty_events_without_querying_each_display_day(): Unit = runBlocking {
        val project = project("完整关联历史", today.minusYears(2), cents = 100_000)
        db.withTransaction {
            repeat(104) { index -> event(project.id, MoneyDirection.OUTLAY, (index + 1).toLong(),
                today.minusDays((104 - index).toLong()), "真实合成投入${index + 1}") }
        }
        val before = facts()
        val eventReads = AtomicInteger()
        queryGate = { if (it.contains("FROM project_money_events WHERE projectId IN", true)) eventReads.incrementAndGet() }
        val loaded = checkNotNull(repository.loadProject(project.id))
        queryGate = null
        assertEquals(105, loaded.detail.events.size)
        assertEquals(105, loaded.snapshot.moneyEvents.size)
        assertEquals(1, eventReads.get())
        assertEquals(today, loaded.detail.asOf)
        assertEquals(today, loaded.timeline.asOf)
        (loaded.timeline.holdingPoints + loaded.timeline.postEndAdjustments).forEach { point ->
            assertEquals(ProjectCostEngine.evaluate(loaded.snapshot, point.date), point.cost)
        }
        assertEquals(100_000L + (1..104).sum(), loaded.detail.cost.netCents)
        assertEquals(before, facts())
    }

    @Test fun q02_real_room_history_keeps_observation_day_denominator_and_separate_post_end_settlement(): Unit = runBlocking {
        val start = LocalDate.of(2026, 1, 1)
        val end = start.plusDays(9)
        val project = project("Q02合成已结束项目", start, end, 100_000)
        event(project.id, MoneyDirection.RECOVERY, 20_000, start.plusDays(19), "结束后真实收回")
        val before = facts()
        val loaded = checkNotNull(repository.loadProject(project.id))
        fun point(day: Int) = ProjectTimelineEngine.point(loaded.snapshot, today, start.plusDays((day - 1).toLong())).cost
        assertEquals(0, checkNotNull(point(2).dailyRate).compareTo(BigDecimal("500")))
        assertEquals(0, checkNotNull(point(9).dailyRate).compareTo(BigDecimal("1000").divide(BigDecimal("9"), MathContext.DECIMAL128)))
        assertEquals(0, checkNotNull(point(10).dailyRate).compareTo(BigDecimal("100")))
        assertFalse(point(10).contributesToCurrentTotal)
        assertEquals(0, checkNotNull(point(19).dailyRate).compareTo(BigDecimal("100")))
        assertEquals(0, checkNotNull(point(20).dailyRate).compareTo(BigDecimal("80")))
        assertEquals(10L, point(20).heldDays)
        assertTrue(loaded.timeline.holdingPoints.all { !it.date.isAfter(end) })
        assertEquals(end, loaded.timeline.holdingPoints.last().date)
        assertEquals(listOf(start.plusDays(19)), loaded.timeline.postEndAdjustments.map { it.date })
        assertEquals(0, checkNotNull(loaded.timeline.postEndAdjustments.single().cost.dailyRate).compareTo(BigDecimal("80")))
        assertEquals(before, facts())
    }

    @Test fun complete_subscription_history_crosses_fifty_payments_and_retains_actual_price_and_state_boundaries(): Unit = runBlocking {
        val identity = SubscriptionEntity(id(), "55期合成服务", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            createdAt = writtenAt, updatedAt = writtenAt)
        val from = LocalDate.of(2022, 4, 1)
        db.withTransaction {
            db.subscriptionDao().insertSubscription(identity)
            repeat(55) { index ->
                val start = from.plusMonths(index.toLong())
                val until = start.plusMonths(1).minusDays(if (index == 15) 3L else 0L)
                val paid = if (index == 18) start.plusDays(2) else start.minusDays(5)
                db.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(id(), identity.id, paid, start, until, until,
                    3_000L + index * 1_000L, BillingCycle.MONTHLY, 30, id()))
            }
            db.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(), identity.id,
                from.plusMonths(30).plusDays(2), SubscriptionOperationState.PAUSED, operationId = id()))
            db.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(), identity.id,
                from.plusMonths(30).plusDays(4), SubscriptionOperationState.ENABLED, operationId = id()))
            db.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(), identity.id,
                today.plusDays(1), SubscriptionOperationState.STOPPED, operationId = id()))
        }
        val before = facts()
        val loaded = checkNotNull(repository.loadSubscription(identity.id))
        assertEquals(55, loaded.detail.renewals.size)
        assertEquals(55, loaded.snapshot.renewals.size)
        assertEquals(3, loaded.snapshot.stateChanges.size)
        loaded.timeline.points.forEach { assertEquals(SubscriptionCostEngine.evaluate(loaded.snapshot, it.date), it.cost) }
        fun at(day: LocalDate) = SubscriptionTimelineEngine.point(loaded.snapshot, today, day).cost
        assertEquals(SubscriptionDisplayStatus.EXPIRED, at(from.plusMonths(16).minusDays(1)).displayStatus)
        assertEquals(0, at(from.plusMonths(16).minusDays(1)).currentRate.signum())
        assertEquals(SubscriptionDisplayStatus.EXPIRED, at(from.plusMonths(18)).displayStatus)
        assertEquals(SubscriptionDisplayStatus.ACTIVE, at(from.plusMonths(18).plusDays(2)).displayStatus)
        assertEquals(SubscriptionDisplayStatus.PAUSED, at(from.plusMonths(30).plusDays(2)).displayStatus)
        assertEquals(SubscriptionDisplayStatus.ACTIVE, at(from.plusMonths(30).plusDays(4)).displayStatus)
        assertEquals(0, at(today).currentRate.compareTo(BigDecimal("19")))
        assertTrue(loaded.timeline.points.none { it.date.isAfter(today) })
        assertEquals(before, facts())
    }

    @Test fun one_detail_snapshot_does_not_mix_a_queued_money_write_into_old_history(): Unit = runBlocking {
        val project = project("一致快照", today.minusDays(9), cents = 100_000)
        val paused = CountDownLatch(1)
        val release = CountDownLatch(1)
        val writerStarted = CountDownLatch(1)
        val pauseOnce = AtomicBoolean(true)
        queryGate = { sql ->
            if (sql.contains("FROM project_money_events WHERE projectId IN", true) && pauseOnce.compareAndSet(true, false)) {
                check(db.inTransaction()); paused.countDown(); check(release.await(10, TimeUnit.SECONDS))
            }
        }
        val reading = async(Dispatchers.IO) { repository.loadProject(project.id) }
        val writing = async(Dispatchers.IO) {
            check(paused.await(5, TimeUnit.SECONDS)); writerStarted.countDown()
            coordinator.withWrite { db.withTransaction { event(project.id, MoneyDirection.OUTLAY, 200_000, today, "排队真实投入") } }
        }
        try {
            assertTrue(withContext(Dispatchers.IO) { writerStarted.await(5, TimeUnit.SECONDS) })
            delay(100)
            assertFalse(writing.isCompleted)
        } finally { queryGate = null; release.countDown() }
        val old = checkNotNull(reading.await())
        writing.await()
        assertEquals(1, old.detail.events.size)
        assertEquals(100_000L, old.detail.cost.netCents)
        val current = checkNotNull(repository.loadProject(project.id))
        assertEquals(2, current.detail.events.size)
        assertEquals(300_000L, current.detail.cost.netCents)
        assertEquals(ProjectCostEngine.evaluate(old.snapshot, today), old.timeline.holdingPoints.last().cost)
        assertEquals(ProjectCostEngine.evaluate(current.snapshot, today), current.timeline.holdingPoints.last().cost)
    }

    @Test fun trade_in_charts_consume_only_each_projects_saved_events_and_keep_non_cash_facts_once(): Unit = runBlocking {
        val old = project("合成旧机", today.minusYears(3), cents = 900_000)
        val oldDetail = checkNotNull(projects.loadProjectDetail(old.id))
        val commandId = id()
        val revision = writtenAt.plusSeconds(1)
        val next = LongTermProjectEntity(TradeInIdentities.newProjectId(commandId), "合成新机", LithicDatabase.PROJECT_FALLBACK_ID,
            today, createdAt = revision, updatedAt = revision)
        val result = ReplacementRepository(db, coordinator, clock).recordTradeIn(TradeInCommand(commandId, old,
            oldDetail.events, emptyList(), next, today, 500_000, 300_000, true, revision))
        assertTrue("Real trade-in must commit before history checks: $result", result is ReplacementMutationResult.TradeInRecorded)
        val before = facts()
        val oldGraph = checkNotNull(repository.loadProject(old.id))
        val newGraph = checkNotNull(repository.loadProject(next.id))
        assertEquals(2, oldGraph.detail.events.size)
        assertEquals(2, newGraph.detail.events.size)
        assertEquals(800_000L, newGraph.timeline.holdingPoints.single().cost.grossOutlayCents)
        assertEquals(600_000L, oldGraph.timeline.holdingPoints.last().cost.netCents)
        assertEquals(500_000L, oldGraph.detail.cost.netCents + newGraph.detail.cost.netCents - 900_000L)
        assertEquals(1, newGraph.detail.events.count { it.role == ProjectMoneyEventRole.INITIAL })
        assertEquals(1, newGraph.detail.events.count { it.basis == MoneyBasis.TRADE_IN_CREDIT })
        assertFalse(oldGraph.timeline.holdingPoints.last().cost.contributesToCurrentTotal)
        assertEquals(before, facts())
    }

    @Test fun missing_zero_and_corrupt_data_remain_distinct_instead_of_a_fake_empty_chart(): Unit = runBlocking {
        assertNull(repository.loadProject(id()))
        assertNull(repository.loadSubscription(id()))
        val zero = project("真实零元", today, cents = 0)
        val loaded = checkNotNull(repository.loadProject(zero.id))
        assertEquals(0, checkNotNull(loaded.timeline.holdingPoints.single().cost.dailyRate).signum())
        db.openHelper.writableDatabase.execSQL("UPDATE long_term_projects SET categoryId=? WHERE id=?",
            arrayOf(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, zero.id))
        val error = try { repository.loadProject(zero.id); null } catch (failure: Exception) { failure }
        assertNotNull("Corrupt stored facts cannot become a zero or missing chart", error)
    }

    @Test fun format_v2_restore_recomputes_both_charts_from_same_facts_and_rejects_retired_generation_reads(): Unit = runBlocking {
        val sourceOwner = AppContainer(DataGenerationStore(context, File(root, "source")), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        val targetOwner = AppContainer(DataGenerationStore(context, File(root, "target")), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        try {
            sourceOwner.start(ZoneId.of("Asia/Shanghai")); targetOwner.start(ZoneId.of("UTC"))
            val source = sourceOwner.openStorage()
            val oldTarget = targetOwner.openStorage()
            val pictures = RestoreSyntheticFixture.insert(source)
            val sourceProjects = ProjectRepository(source.database, source.coordinator, clock)
            val sourceSubscriptions = SubscriptionRepository(source.database, source.coordinator, clock)
            val original = ItemTimelineRepository(sourceProjects, sourceSubscriptions, clock)
            val oldReader = ItemTimelineRepository(ProjectRepository(oldTarget.database, oldTarget.coordinator, clock),
                SubscriptionRepository(oldTarget.database, oldTarget.coordinator, clock), clock)
            val projectIds = source.database.projectDao().listProjects().map { it.id }
            val subscriptionIds = source.database.subscriptionDao().validationSubscriptions(null, 50).map { it.id }
            val before = source.backupSnapshotSource().capture()
            assertEquals(8, before.data.recordCounts().size)
            val expectedProjects = projectIds.associateWith { original.loadProject(it) }
            val expectedSubscriptions = subscriptionIds.associateWith { original.loadSubscription(it) }
            assertEquals(before, source.backupSnapshotSource().capture())
            val archive = File(root, "m17-controlled-file.lithicbackup")
            assertTrue(BackupExportService(context, source).export(RestoreSyntheticFixture.target(archive)) is BackupExportResult.Success)
            val restorer = RestoreCoordinator(context, targetOwner)
            val preview = restorer.prepare { archive.inputStream() }
            assertEquals(before.data.recordCounts(), preview.backupCounts)
            assertEquals(RestoreResult.Success, restorer.confirm(preview.token))
            val restored = targetOwner.openStorage()
            assertNotEquals(oldTarget.id, restored.id)
            val actual = ItemTimelineRepository(ProjectRepository(restored.database, restored.coordinator, clock),
                SubscriptionRepository(restored.database, restored.coordinator, clock), clock)
            projectIds.forEach { assertEquals(expectedProjects[it], actual.loadProject(it)) }
            subscriptionIds.forEach { assertEquals(expectedSubscriptions[it], actual.loadSubscription(it)) }
            assertEquals(before, restored.backupSnapshotSource().capture())
            pictures.forEach { (path, bytes) -> assertArrayEquals(bytes, File(restored.directory, path).readBytes()) }
            val retired = try { oldReader.loadProject(projectIds.first()); null } catch (failure: Exception) { failure }
            assertNotNull("A retired generation cannot return old or newly restored chart facts", retired)
        } finally { sourceOwner.close(); targetOwner.close() }
    }

    private suspend fun project(name: String, start: LocalDate, end: LocalDate? = null, cents: Long): LongTermProjectEntity {
        val entity = LongTermProjectEntity(id(), name, LithicDatabase.PROJECT_FALLBACK_ID, start, end,
            endReason = end?.let { "合成正常结束" }, createdAt = writtenAt, updatedAt = writtenAt)
        db.projectDao().insertProject(entity)
        db.projectDao().insertEvent(ProjectMoneyEventEntity(id(), entity.id, MoneyDirection.OUTLAY, cents, start,
            role = ProjectMoneyEventRole.INITIAL, operationId = id()))
        return entity
    }
    private suspend fun event(projectId: String, direction: MoneyDirection, cents: Long, date: LocalDate, description: String) {
        db.projectDao().insertEvent(ProjectMoneyEventEntity(id(), projectId, direction, cents, date, description,
            ProjectMoneyEventRole.REGULAR, id()))
    }
    private suspend fun facts(): List<Any> {
        val ids = db.subscriptionDao().validationSubscriptions(null, 500).map { it.id }
        return listOf(db.categoryDao().list(), db.projectDao().listProjects(), db.projectDao().listEvents(),
            db.subscriptionDao().subscriptionsForIds(ids), db.subscriptionDao().renewalsForSubscriptions(ids),
            db.subscriptionDao().statesForSubscriptions(ids), db.replacementDao().listReplacements())
    }
    private fun id() = UUID.randomUUID().toString()
}
