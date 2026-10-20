package com.cooper.lithic.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
import com.cooper.lithic.domain.report.OverviewResult
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.io.File
import java.math.BigDecimal
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Every fixture is a fresh private synthetic file database, never the application's generation. */
@RunWith(AndroidJUnit4::class)
class ReportRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var directory: File
    private lateinit var file: File
    private lateinit var db: LithicDatabase
    private val coordinator = LocalWriteCoordinator()
    private val today = LocalDate.of(2026, 1, 10)
    private val instant = Instant.parse("2026-01-01T00:00:00Z")
    @Volatile private var queryGate: ((String) -> Unit)? = null
    private val repository get() = ReportRepository(db, coordinator, FixedBusinessClock(today))

    @Before fun setUp() = runBlocking {
        directory = File(context.noBackupFilesDir, "m13-report-${UUID.randomUUID()}")
        check(directory.mkdir())
        file = File(directory, "data.db")
        db = openFixture()
        db.initializeDefaults()
    }

    @After fun tearDown() {
        queryGate = null
        if (::db.isInitialized) db.close()
        if (::directory.isInitialized) {
            check(directory.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
            check(directory.deleteRecursively())
        }
    }

    @Test fun complete_batches_do_not_omit_or_duplicate_more_than_one_hundred_identities() = runBlocking {
        coordinator.withWrite { db.withTransaction { (1..103).forEach { project(it); subscription(it) } } }
        val result = repository.loadOverview()
        assertEquals(103L, result.projectCount)
        assertEquals(103L, result.subscriptionCount)
        assertEquals(103L, result.activeProjectCount)
        assertEquals(103L, result.activeSubscriptionCount)
        rate("103", result.projectRate)
        rate("103", result.subscriptionRate)
        rate("206", result.totalRate)
        assertEquals(LithicDatabase.PROJECT_FALLBACK_ID, result.projectCategories.single().categoryId)
        assertEquals(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, result.subscriptionCategories.single().categoryId)
        assertEquals("其他", result.projectCategories.single().name)
        assertEquals("其他", result.subscriptionCategories.single().name)
        assertEquals(103L, result.projectCategories.single().contributingCount)
    }

    @Test fun one_snapshot_and_room_transaction_block_a_write_between_identity_batches() = runBlocking {
        coordinator.withWrite { db.withTransaction { (1..101).forEach { project(it); subscription(it) } } }
        val readPaused = CountDownLatch(1)
        val releaseRead = CountDownLatch(1)
        val writerStarted = CountDownLatch(1)
        val pauseOnce = AtomicBoolean(false)
        queryGate = { sql ->
            if (sql.contains("FROM project_money_events WHERE projectId IN", true) && pauseOnce.compareAndSet(false, true)) {
                check(db.inTransaction()) { "Associated facts must be read inside the same Room transaction" }
                readPaused.countDown()
                check(releaseRead.await(10, TimeUnit.SECONDS))
            }
        }
        val reading = async(Dispatchers.IO) { repository.loadOverview() }
        val writing = async(Dispatchers.IO) {
            check(readPaused.await(5, TimeUnit.SECONDS))
            writerStarted.countDown()
            coordinator.withWrite { db.withTransaction { project(102); subscription(102) } }
        }
        try {
            assertTrue(withContext(Dispatchers.IO) { writerStarted.await(5, TimeUnit.SECONDS) })
            delay(100)
            assertFalse("The complete overview keeps its generation boundary", writing.isCompleted)
        } finally {
            releaseRead.countDown()
            queryGate = null
        }
        val beforeWrite = reading.await()
        writing.await()
        assertEquals(101L, beforeWrite.projectCount)
        assertEquals(101L, beforeWrite.subscriptionCount)
        rate("202", beforeWrite.totalRate)
        rate("204", repository.loadOverview().totalRate)
    }

    @Test fun clock_is_read_once_only_after_acquiring_the_generation_snapshot() = runBlocking {
        coordinator.withWrite { db.withTransaction { project(1); subscription(1) } }
        val calls = AtomicInteger(0)
        var day = today
        val observed = ReportRepository(db, coordinator, BusinessClock { calls.incrementAndGet(); day })
        val locked = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val holding = launch(Dispatchers.IO) { coordinator.withSnapshot { locked.complete(Unit); release.await() } }
        locked.await()
        val started = CompletableDeferred<Unit>()
        val reading = async(Dispatchers.IO) { started.complete(Unit); observed.loadOverview() }
        try {
            started.await()
            delay(100)
            assertEquals("A queued read cannot sample yesterday before its boundary", 0, calls.get())
            day = today.plusDays(1)
        } finally { release.complete(Unit) }
        holding.join()
        val result = reading.await()
        assertEquals(day, result.asOf)
        assertEquals(1, calls.get())
        rate("1.9090909090909090909090909090909091", result.totalRate)
    }

    @Test fun historical_observation_keeps_future_payments_and_state_boundaries_distinct() = runBlocking {
        coordinator.withWrite {
            db.withTransaction {
                subscription(1, amount = 15_000)
                val next = renewal(2, id(1), LocalDate.of(2026, 2, 1), 18_000)
                    .copy(paidOn = LocalDate.of(2026, 1, 20))
                db.subscriptionDao().insertRenewal(next)
                db.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(
                    id(70_001), id(1), LocalDate.of(2026, 2, 5), SubscriptionOperationState.STOPPED,
                    operationId = id(80_001),
                ))
            }
        }
        rate("5", repository.loadOverview(LocalDate.of(2026, 1, 10)).subscriptionRate)
        rate("5", repository.loadOverview(LocalDate.of(2026, 1, 31)).subscriptionRate)
        rate("6", repository.loadOverview(LocalDate.of(2026, 2, 1)).subscriptionRate)
        val stopped = repository.loadOverview(LocalDate.of(2026, 2, 5))
        rate("0", stopped.subscriptionRate)
        assertEquals(1L, stopped.subscriptionCount)
        assertEquals(0L, stopped.activeSubscriptionCount)
    }

    @Test fun explicit_historical_day_does_not_sample_or_replace_the_record_clock() = runBlocking {
        db.withTransaction { project(1); subscription(1) }
        val historical = ReportRepository(db, coordinator, BusinessClock { error("An explicit observation supplies its own date") })
            .loadOverview(LocalDate.of(2026, 1, 5))
        assertEquals(LocalDate.of(2026, 1, 5), historical.asOf)
        rate("3", historical.totalRate)
    }

    @Test fun negative_zero_and_distinct_category_amounts_preserve_unrounded_sums() = runBlocking {
        coordinator.withWrite {
            db.withTransaction {
                val category = CategoryEntity(id(90_001), CategoryType.PROJECT, "设备", "设备", false)
                db.categoryDao().insert(category)
                project(1, amount = 5_000)
                project(2, amount = 0, categoryId = category.id)
                db.projectDao().insertEvent(ProjectMoneyEventEntity(id(50_002), id(2), MoneyDirection.RECOVERY,
                    1_000, today, role = ProjectMoneyEventRole.REGULAR, operationId = id(60_002)))
                project(3, amount = 0, categoryId = category.id)
                subscription(1, amount = 15_000)
                subscription(2, amount = 30_000)
            }
        }
        val result = repository.loadOverview()
        rate("4", result.projectRate)
        rate("15", result.subscriptionRate)
        rate("19", result.totalRate)
        rate("-1", result.projectCategories.single { it.categoryId == id(90_001) }.dailyRate)
        assertEquals(2L, result.projectCategories.single { it.categoryId == id(90_001) }.contributingCount)
        rate(result.projectRate.toPlainString(), result.projectCategories.fold(BigDecimal.ZERO) { sum, row -> sum.add(row.dailyRate) })
    }

    @Test fun valid_empty_overview_is_distinct_from_a_zero_cost_project() = runBlocking {
        val empty = repository.loadOverview()
        assertEquals(0L, empty.projectCount)
        assertEquals(0L, empty.subscriptionCount)
        rate("0", empty.totalRate)
        coordinator.withWrite { db.withTransaction { project(1, amount = 0) } }
        val zero = repository.loadOverview()
        assertEquals(1L, zero.projectCount)
        assertEquals(1L, zero.activeProjectCount)
        rate("0", zero.totalRate)
    }

    @Test fun subscription_without_any_confirmed_payment_is_a_read_failure() = runBlocking {
        db.subscriptionDao().insertSubscription(identity(1))
        readFails { repository.loadOverview() }
    }

    @Test fun noncanonical_uuid_is_a_read_failure_instead_of_a_partial_total() = runBlocking {
        db.projectDao().insertProject(LongTermProjectEntity("not-a-uuid", "坏身份", LithicDatabase.PROJECT_FALLBACK_ID,
            today, createdAt = instant, updatedAt = instant))
        readFails { repository.loadOverview() }
    }

    @Test fun wrong_category_type_is_a_read_failure_instead_of_zero() = runBlocking {
        db.withTransaction { project(1, categoryId = LithicDatabase.SUBSCRIPTION_FALLBACK_ID) }
        readFails { repository.loadOverview() }
    }

    @Test fun missing_cover_metadata_is_a_read_failure_without_reading_media_files() = runBlocking {
        db.withTransaction { project(1) }
        corrupt("UPDATE long_term_projects SET coverAssetId='${id(99_001)}'")
        readFails { repository.loadOverview() }
    }

    @Test fun orphan_project_event_is_a_read_failure_even_without_any_identity() = runBlocking {
        corrupt("INSERT INTO project_money_events (id,projectId,direction,amountCents,occurredOn,description,role,operationId) VALUES ('${id(20_001)}','${id(1)}','OUTLAY',100,${today.toEpochDay()},NULL,'INITIAL','${id(30_001)}')")
        readFails { repository.loadOverview() }
    }

    @Test fun orphan_subscription_renewal_is_a_read_failure_even_without_any_identity() = runBlocking {
        corrupt("INSERT INTO subscription_renewals (id,subscriptionId,paidOn,effectiveFrom,paidUntilExclusive,reminderDueOn,amountCents,cycle,standardDays,operationId) VALUES ('${id(20_001)}','${id(1)}',${today.toEpochDay()},${today.toEpochDay()},${today.plusMonths(1).toEpochDay()},${today.plusMonths(1).toEpochDay()},3000,'MONTHLY',30,'${id(30_001)}')")
        readFails { repository.loadOverview() }
    }

    @Test fun orphan_subscription_state_is_a_read_failure_even_without_any_identity() = runBlocking {
        corrupt("INSERT INTO subscription_state_changes (id,subscriptionId,effectiveOn,state,reason,note,operationId) VALUES ('${id(20_001)}','${id(1)}',${today.toEpochDay()},'PAUSED',NULL,NULL,'${id(30_001)}')")
        readFails { repository.loadOverview() }
    }

    @Test fun invalid_money_does_not_become_a_successful_partial_sum() = runBlocking {
        db.withTransaction { project(1); project(2) }
        corrupt("UPDATE project_money_events SET amountCents=-1 WHERE projectId='${id(2)}'")
        readFails { repository.loadOverview() }
    }

    @Test fun changes_include_amount_and_state_mutations_with_unchanged_identity_counts() = runBlocking {
        db.withTransaction { project(1); subscription(1) }
        val emissions = Channel<Unit>(Channel.UNLIMITED)
        val observing = launch(Dispatchers.IO) { repository.changes.collect { emissions.send(Unit) } }
        try {
            withTimeout(5_000) { emissions.receive() }
            coordinator.withWrite { db.withTransaction {
                db.openHelper.writableDatabase.execSQL("UPDATE project_money_events SET amountCents=2000 WHERE projectId='${id(1)}'")
            } }
            withTimeout(5_000) { emissions.receive() }
            rate("3", repository.loadOverview().totalRate)
            coordinator.withWrite { db.withTransaction {
                db.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(70_001), id(1), today,
                    SubscriptionOperationState.PAUSED, operationId = id(80_001)))
            } }
            withTimeout(5_000) { emissions.receive() }
            rate("2", repository.loadOverview().totalRate)
        } finally { observing.cancel(); observing.join() }
    }

    private suspend fun openFixture(): LithicDatabase = withContext(Dispatchers.IO) {
        Room.databaseBuilder(context, LithicDatabase::class.java, file.path)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .setQueryCallback({ sql, _ -> queryGate?.invoke(sql) }, { work -> work.run() })
            .build()
    }

    private suspend fun corrupt(sql: String) = withContext(Dispatchers.IO) {
        db.close()
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { raw ->
            raw.setForeignKeyConstraintsEnabled(false)
            raw.execSQL(sql)
        }
        db = openFixture()
    }

    private suspend fun project(number: Int, amount: Long = 1_000, categoryId: String = LithicDatabase.PROJECT_FALLBACK_ID) {
        val projectId = id(number)
        db.projectDao().insertProject(LongTermProjectEntity(projectId, "合成项目$number", categoryId,
            LocalDate.of(2026, 1, 1), createdAt = instant, updatedAt = instant))
        db.projectDao().insertEvent(ProjectMoneyEventEntity(id(10_000 + number), projectId, MoneyDirection.OUTLAY,
            amount, LocalDate.of(2026, 1, 1), role = ProjectMoneyEventRole.INITIAL, operationId = id(20_000 + number)))
    }

    private fun identity(number: Int) = SubscriptionEntity(id(number), "合成订阅$number",
        LithicDatabase.SUBSCRIPTION_FALLBACK_ID, createdAt = instant, updatedAt = instant)

    private suspend fun subscription(number: Int, amount: Long = 3_000) {
        db.subscriptionDao().insertSubscription(identity(number))
        db.subscriptionDao().insertRenewal(renewal(number, id(number), LocalDate.of(2026, 1, 1), amount))
    }

    private fun renewal(number: Int, subscriptionId: String, from: LocalDate, amount: Long) = SubscriptionRenewalEntity(
        id(30_000 + number), subscriptionId, from, from, from.plusMonths(1), from.plusMonths(1), amount,
        BillingCycle.MONTHLY, 30, id(40_000 + number),
    )

    private suspend fun readFails(block: suspend () -> OverviewResult) {
        val failure = try { block(); null } catch (failure: Exception) { failure }
        assertTrue("An invalid stored fact must fail the entire overview read", failure != null)
    }

    private fun rate(expected: String, actual: BigDecimal) = assertEquals(0, BigDecimal(expected).compareTo(actual))
    private fun id(value: Int): String = UUID(0L, value.toLong()).toString()
}
