package com.cooper.lithic.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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

/** New synthetic file databases only; never opens MainActivity or its ledger. */
@RunWith(AndroidJUnit4::class)
class RankingRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var directory: File
    private lateinit var db: LithicDatabase
    private val coordinator = LocalWriteCoordinator()
    private val today = LocalDate.of(2026, 1, 10)
    private val instant = Instant.parse("2026-01-01T00:00:00Z")
    @Volatile private var queryGate: ((String) -> Unit)? = null
    private val repository get() = RankingRepository(db, coordinator, FixedBusinessClock(today))

    @Before fun setUp() = runBlocking {
        directory = File(context.noBackupFilesDir, "m15-ranking-${UUID.randomUUID()}").canonicalFile
        check(directory.mkdir())
        db = Room.databaseBuilder(context, LithicDatabase::class.java, File(directory, "data.db").path)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .setQueryCallback({ sql, _ -> queryGate?.invoke(sql) }, { work -> work.run() })
            .build()
        db.initializeDefaults()
    }

    @After fun tearDown() {
        queryGate = null
        db.close()
        check(directory.parentFile == context.noBackupFilesDir.canonicalFile && directory.name.startsWith("m15-ranking-"))
        check(directory.deleteRecursively())
    }

    @Test fun global_cost_ranking_precedes_the_fifty_row_window_and_keeps_stable_ties() = runBlocking {
        db.withTransaction {
            (1..104).forEach { project(it, amount = 10_000) }
            project(105, amount = 1)
        }
        val first = repository.loadWindow(ReviewRanking.PROJECT_COST)
        assertEquals(105L, first.totalCount)
        assertEquals(105L, first.rankableCount)
        assertEquals(50, first.items.size)
        assertEquals(id(105), first.items.first().id)
        assertTrue(first.hasMore)
        val all = repository.loadWindow(ReviewRanking.PROJECT_COST, 3)
        assertEquals(listOf(id(105)) + (1..104).map(::id), all.items.map { it.id })
        assertEquals(first.items, all.items.take(50))
        assertEquals(105, all.items.map { it.id }.distinct().size)
        assertFalse(all.hasMore)
    }

    @Test fun active_eight_year_project_is_ranked_with_ended_projects_after_global_read() = runBlocking {
        db.withTransaction {
            (1..55).forEach { number ->
                val start = LocalDate.of(2000, 1, 1).plusDays(number.toLong())
                project(number, startedOn = start, endedOn = start.plusDays(1))
            }
            project(99, startedOn = today.minusYears(8))
        }
        val rows = repository.loadWindow(ReviewRanking.PROJECT_TIME, 2).items
        assertEquals(id(99), rows.first().id)
        assertFalse(checkNotNull(rows.first().project).isHistory)
        assertTrue(checkNotNull(rows.first().project).cost.heldDays >= 2_920L)
        assertTrue(rows.drop(1).all { checkNotNull(it.project).isHistory })
        assertEquals((1..55).map(::id), rows.drop(1).map { it.id })
    }

    @Test fun zero_negative_and_same_display_precision_use_raw_rates_and_future_is_unavailable() = runBlocking {
        db.withTransaction {
            project(1, amount = 1_000)
            project(2, amount = 0)
            recovery(2, 2_000, today)
            project(3, amount = 0)
            project(4, amount = 1_001)
            project(5, amount = 0, startedOn = today.plusDays(1), paidOn = today)
        }
        val result = repository.loadWindow(ReviewRanking.PROJECT_COST)
        assertEquals(listOf(id(2), id(3), id(1), id(4), id(5)), result.items.map { it.id })
        assertEquals(5L, result.totalCount)
        assertEquals(4L, result.rankableCount)
        val rates = result.items.associate { it.id to checkNotNull(it.project).cost.dailyRate }
        assertTrue(checkNotNull(rates[id(2)]) < BigDecimal.ZERO)
        assertEquals(0, checkNotNull(rates[id(3)]).compareTo(BigDecimal.ZERO))
        assertTrue(checkNotNull(rates[id(1)]) < checkNotNull(rates[id(4)]))
        assertEquals(checkNotNull(rates[id(1)]).setScale(2, RoundingMode.HALF_UP),
            checkNotNull(rates[id(4)]).setScale(2, RoundingMode.HALF_UP))
        assertNull(rates[id(5)])
    }

    @Test fun ended_post_settlement_changes_known_cost_without_extending_held_days() = runBlocking {
        val start = today.minusDays(19)
        val end = start.plusDays(9)
        project(1, amount = 10_000, startedOn = start, endedOn = end)
        recovery(1, 2_000, today)
        val before = checkNotNull(repository.loadWindow(ReviewRanking.PROJECT_COST, asOf = end).items.single().project)
        val after = checkNotNull(repository.loadWindow(ReviewRanking.PROJECT_COST).items.single().project)
        assertEquals(10L, before.cost.heldDays)
        assertEquals(before.cost.heldDays, after.cost.heldDays)
        assertEquals(10_000L, before.cost.netCents)
        assertEquals(8_000L, after.cost.netCents)
        assertTrue(after.isHistory)
        assertFalse(after.cost.contributesToCurrentTotal)
    }

    @Test fun history_reuses_real_filters_and_excludes_pause_expiry_and_future_stop() = runBlocking {
        project(1, endedOn = today)
        project(2, endedOn = today.plusDays(1))
        subscription(10, state = SubscriptionOperationState.STOPPED)
        subscription(11, state = SubscriptionOperationState.PAUSED)
        subscription(12, until = today)
        subscription(13, state = SubscriptionOperationState.STOPPED, stateOn = today.plusDays(1))
        val projectHistory = ProjectRepository(db, coordinator, FixedBusinessClock(today)).loadProjectHistoryWindow(1)
        val subHistory = SubscriptionRepository(db, coordinator, FixedBusinessClock(today)).loadManagementWindow(true, 1)
        assertEquals(listOf(id(1)), projectHistory.items.map { it.project.id })
        assertEquals(listOf(id(10)), subHistory.items.map { it.subscription.id })
        val ranks = repository.loadWindow(ReviewRanking.SUBSCRIPTION_TIME).items.associateBy { it.id }
        assertEquals(SubscriptionDisplayStatus.PAUSED, checkNotNull(ranks.getValue(id(11)).subscription).cost.displayStatus)
        assertEquals(SubscriptionDisplayStatus.EXPIRED, checkNotNull(ranks.getValue(id(12)).subscription).cost.displayStatus)
        assertEquals(SubscriptionDisplayStatus.ACTIVE, checkNotNull(ranks.getValue(id(13)).subscription).cost.displayStatus)
    }

    @Test fun yearly_paid_today_is_standard_365_average_ten_and_elapsed_one_not_365() = runBlocking {
        subscription(1, cycle = BillingCycle.YEARLY, amount = 365_000, paidOn = today, from = today, until = today.plusYears(1))
        subscription(2, state = SubscriptionOperationState.PAUSED)
        val result = repository.loadWindow(ReviewRanking.SUBSCRIPTION_TIME)
        assertTrue(result.items.all { it.type == CategoryType.SUBSCRIPTION && it.project == null })
        val yearly = checkNotNull(result.items.first().subscription).cost
        assertEquals(id(1), result.items.first().id)
        assertEquals(365L, yearly.standardPurchasedDays)
        assertEquals(1L, yearly.elapsedPaidDays)
        assertEquals(0, BigDecimal.TEN.compareTo(checkNotNull(yearly.historicalAverage)))
        val paused = checkNotNull(result.items.last().subscription).cost
        assertEquals(30L, paused.standardPurchasedDays)
        assertEquals(0, paused.currentRate.compareTo(BigDecimal.ZERO))
        assertNotEquals(0, checkNotNull(paused.historicalAverage).compareTo(BigDecimal.ZERO))
    }

    @Test fun subscription_global_time_ranking_crosses_fifty_ids_and_has_no_omissions() = runBlocking {
        db.withTransaction {
            (1..104).forEach { subscription(it) }
            subscription(105, cycle = BillingCycle.YEARLY, amount = 365_000)
        }
        val first = repository.loadWindow(ReviewRanking.SUBSCRIPTION_TIME)
        val all = repository.loadWindow(ReviewRanking.SUBSCRIPTION_TIME, 3)
        assertEquals(id(105), first.items.first().id)
        assertEquals(listOf(id(105)) + (1..104).map(::id), all.items.map { it.id })
        assertEquals(105L, all.totalCount)
        assertEquals(105, all.items.map { it.id }.distinct().size)
        assertEquals(first.items, all.items.take(50))
    }

    @Test fun browsing_and_switching_ranks_never_changes_business_facts() = runBlocking {
        db.withTransaction { project(1); project(2, endedOn = today); subscription(3, state = SubscriptionOperationState.PAUSED) }
        val before = facts()
        ReviewRanking.entries.forEach { repository.loadWindow(it) }
        assertEquals(before, facts())
    }

    @Test fun category_edit_and_unchanged_identity_fact_edit_invalidate_and_recompute() = runBlocking {
        val category = CategoryEntity(id(90_001), CategoryType.PROJECT, "设备", "设备", false)
        db.withTransaction { db.categoryDao().insert(category); project(1, categoryId = category.id) }
        val emissions = Channel<Unit>(Channel.UNLIMITED)
        val observing = launch(Dispatchers.IO) { repository.changes.collect { emissions.send(Unit) } }
        try {
            withTimeout(5_000) { emissions.receive() }
            assertTrue(CategoryRepository(db, coordinator).rename(category, "长期设备") is CategoryMutationResult.Applied)
            withTimeout(5_000) { emissions.receive() }
            assertEquals("长期设备", repository.loadWindow(ReviewRanking.PROJECT_TIME).items.single().categoryName)
            coordinator.withWrite { db.withTransaction { recovery(1, 2_000, today) } }
            withTimeout(5_000) { emissions.receive() }
            assertEquals(-1_000L, checkNotNull(repository.loadWindow(ReviewRanking.PROJECT_COST).items.single().project).cost.netCents)
        } finally { observing.cancel(); observing.join() }
    }

    @Test fun full_window_uses_one_transaction_and_blocks_mid_batch_business_write() = runBlocking {
        db.withTransaction { (1..101).forEach { project(it) } }
        val readPaused = CountDownLatch(1)
        val releaseRead = CountDownLatch(1)
        val writerStarted = CountDownLatch(1)
        val pauseOnce = AtomicBoolean(false)
        queryGate = { sql ->
            if (sql.contains("FROM project_money_events WHERE projectId IN", true) && pauseOnce.compareAndSet(false, true)) {
                check(db.inTransaction())
                readPaused.countDown()
                check(releaseRead.await(10, TimeUnit.SECONDS))
            }
        }
        val reading = async(Dispatchers.IO) { repository.loadWindow(ReviewRanking.PROJECT_COST, 3) }
        val writing = async(Dispatchers.IO) {
            check(readPaused.await(5, TimeUnit.SECONDS))
            writerStarted.countDown()
            coordinator.withWrite { db.withTransaction { project(102, amount = 0) } }
        }
        try {
            assertTrue(withContext(Dispatchers.IO) { writerStarted.await(5, TimeUnit.SECONDS) })
            delay(100)
            assertFalse(writing.isCompleted)
        } finally { releaseRead.countDown(); queryGate = null }
        assertEquals(101L, reading.await().totalCount)
        writing.await()
        assertEquals(id(102), repository.loadWindow(ReviewRanking.PROJECT_COST, 3).items.first().id)
    }

    @Test fun clock_is_sampled_once_inside_boundary_and_explicit_day_does_not_read_it() = runBlocking {
        project(1)
        val calls = AtomicInteger(0)
        var date = today
        val observed = RankingRepository(db, coordinator, BusinessClock { calls.incrementAndGet(); date })
        val locked = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val holding = launch(Dispatchers.IO) { coordinator.withSnapshot { locked.complete(Unit); release.await() } }
        locked.await()
        val started = CompletableDeferred<Unit>()
        val reading = async(Dispatchers.IO) { started.complete(Unit); observed.loadWindow(ReviewRanking.PROJECT_TIME) }
        try {
            started.await(); delay(100)
            assertEquals(0, calls.get())
            date = today.plusDays(1)
        } finally { release.complete(Unit) }
        holding.join()
        assertEquals(date, reading.await().asOf)
        assertEquals(1, calls.get())
        val historic = RankingRepository(db, coordinator, BusinessClock { error("Explicit date must not sample clock") })
            .loadWindow(ReviewRanking.PROJECT_TIME, asOf = today.minusDays(1))
        assertEquals(today.minusDays(1), historic.asOf)
        assertEquals(9L, checkNotNull(historic.items.single().project).cost.heldDays)
    }

    @Test fun invalid_facts_are_read_errors_while_empty_and_zero_and_unavailable_remain_distinct() = runBlocking {
        val empty = repository.loadWindow(ReviewRanking.PROJECT_COST)
        assertEquals(0L, empty.totalCount)
        assertTrue(empty.items.isEmpty())
        project(1, amount = 0)
        val zero = repository.loadWindow(ReviewRanking.PROJECT_COST)
        assertEquals(1L, zero.rankableCount)
        assertEquals(0, BigDecimal.ZERO.compareTo(checkNotNull(checkNotNull(zero.items.single().project).cost.dailyRate)))
        project(2, amount = 0, startedOn = today.plusDays(1), paidOn = today)
        assertEquals(1L, repository.loadWindow(ReviewRanking.PROJECT_COST).rankableCount)
        db.openHelper.writableDatabase.execSQL("UPDATE long_term_projects SET categoryId=? WHERE id=?",
            arrayOf(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, id(1)))
        assertReadFails { repository.loadWindow(ReviewRanking.PROJECT_COST) }
        assertReadFails { repository.loadWindow(ReviewRanking.PROJECT_COST, 0) }
    }

    private suspend fun project(number: Int, amount: Long = 1_000, startedOn: LocalDate = today.minusDays(9),
        endedOn: LocalDate? = null, categoryId: String = LithicDatabase.PROJECT_FALLBACK_ID, paidOn: LocalDate = startedOn) {
        db.projectDao().insertProject(LongTermProjectEntity(id(number), "合成项目$number", categoryId, startedOn, endedOn,
            endReason = endedOn?.let { "合成结束" }, createdAt = instant, updatedAt = instant))
        db.projectDao().insertEvent(ProjectMoneyEventEntity(id(10_000 + number), id(number), MoneyDirection.OUTLAY,
            amount, paidOn, role = ProjectMoneyEventRole.INITIAL, operationId = id(20_000 + number)))
    }

    private suspend fun recovery(number: Int, amount: Long, on: LocalDate) {
        db.projectDao().insertEvent(ProjectMoneyEventEntity(id(30_000 + number), id(number), MoneyDirection.RECOVERY,
            amount, on, role = ProjectMoneyEventRole.REGULAR, operationId = id(40_000 + number)))
    }

    private suspend fun subscription(number: Int, cycle: BillingCycle = BillingCycle.MONTHLY, amount: Long = 3_000,
        paidOn: LocalDate = today.minusDays(9), from: LocalDate = today.minusDays(9), until: LocalDate = today.plusDays(21),
        state: SubscriptionOperationState? = null, stateOn: LocalDate = today) {
        db.subscriptionDao().insertSubscription(SubscriptionEntity(id(number), "合成服务$number",
            LithicDatabase.SUBSCRIPTION_FALLBACK_ID, createdAt = instant, updatedAt = instant))
        db.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(id(50_000 + number), id(number), paidOn, from,
            until, until, amount, cycle, cycle.standardDays, id(60_000 + number)))
        state?.let { db.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(70_000 + number), id(number),
            stateOn, it, operationId = id(80_000 + number))) }
    }

    private suspend fun facts(): List<Any> {
        val ids = db.subscriptionDao().validationSubscriptions(null, 500).map { it.id }
        return listOf(db.categoryDao().list(), db.projectDao().listProjects(), db.projectDao().listEvents(),
            db.subscriptionDao().subscriptionsForIds(ids), db.subscriptionDao().renewalsForSubscriptions(ids),
            db.subscriptionDao().statesForSubscriptions(ids))
    }

    private suspend fun assertReadFails(block: suspend () -> ReviewPage) {
        val failure = try { block(); null } catch (failure: Exception) { failure }
        assertNotNull("Invalid data or a retired generation cannot become an empty success", failure)
    }

    private fun id(number: Int): String = UUID(0L, number.toLong()).toString()
}
