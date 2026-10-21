package com.cooper.lithic.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** New in-memory databases only. Never attaches the application's/default ledger. */
@RunWith(AndroidJUnit4::class)
class SearchRepositoryTest {
    private lateinit var db: LithicDatabase
    private val coordinator = LocalWriteCoordinator()
    private val today = LocalDate.of(2026, 1, 10)
    private val instant = Instant.parse("2026-01-01T00:00:00Z")
    private val repository get() = SearchRepository(db, coordinator, FixedBusinessClock(today))

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), LithicDatabase::class.java).build()
        db.initializeDefaults()
    }

    @After fun tearDown() { db.close() }

    @Test fun search_current_and_history_with_frozen_subscription_statuses() = runBlocking {
        project(1, "相机 current")
        project(2, "相机 ended", endedOn = today)
        subscription(3, "相机 active")
        subscription(4, "相机 paused", state = SubscriptionOperationState.PAUSED)
        subscription(5, "相机 stopped", state = SubscriptionOperationState.STOPPED)
        subscription(6, "相机 expired", until = today)
        val result = repository.loadWindow("相机", 1)
        assertEquals(6L, result.totalCount)
        val byId = result.items.associateBy { it.id }
        assertEquals(SearchStatus.PROJECT_CURRENT, byId.getValue(id(1)).status)
        assertEquals(SearchStatus.PROJECT_ENDED, byId.getValue(id(2)).status)
        assertTrue(byId.getValue(id(2)).isHistory)
        assertEquals(SearchStatus.ACTIVE, byId.getValue(id(3)).status)
        assertEquals(SearchStatus.PAUSED, byId.getValue(id(4)).status)
        assertEquals(SearchStatus.STOPPED, byId.getValue(id(5)).status)
        assertTrue(byId.getValue(id(5)).isHistory)
        assertFalse(byId.getValue(id(4)).isHistory)
        assertEquals(SearchStatus.EXPIRED, byId.getValue(id(6)).status)
    }

    @Test fun wildcards_quotes_escape_and_sql_injection_text_are_literal() = runBlocking {
        project(1, "100% 相机")
        project(2, "100_ 相机")
        project(3, "100X 相机")
        project(4, "O'Reilly")
        subscription(5, "Hello!world")
        assertEquals(listOf(id(1)), repository.loadWindow("%", 1).items.map { it.id })
        assertEquals(listOf(id(2)), repository.loadWindow("_", 1).items.map { it.id })
        assertEquals(listOf(id(4)), repository.loadWindow("'", 1).items.map { it.id })
        assertEquals(listOf(id(5)), repository.loadWindow("!", 1).items.map { it.id })
        assertTrue(repository.loadWindow("' OR 1=1 --", 1).items.isEmpty())
        assertEquals(4L, db.projectDao().countProjects())
    }

    @Test fun only_names_match_not_notes_categories_amounts_or_payments() = runBlocking {
        project(1, "相机", note = "needle")
        subscription(2, "服务", note = "needle")
        assertTrue(repository.loadWindow("needle", 1).items.isEmpty())
        assertTrue(repository.loadWindow("3000", 1).items.isEmpty())
        assertTrue(repository.loadWindow("其他", 1).items.isEmpty())
        assertTrue(repository.loadWindow("", 1).items.isEmpty())
        assertTrue(repository.loadWindow("  ", 1).items.isEmpty())
    }

    @Test fun stable_pages_across_both_types_have_no_duplicates_or_omissions() = runBlocking {
        db.withTransaction {
            (1..82).forEach { project(it, "共同项目") }
            (1001..1075).forEach { subscription(it, "共同服务") }
        }
        val collected = mutableListOf<SearchItem>()
        var cursor: SearchCursor? = null
        do {
            val page = repository.searchNames("共同", 50, cursor)
            assertTrue(page.items.size <= 50)
            assertEquals(157L, page.totalCount)
            collected += page.items
            cursor = page.nextCursor
        } while (cursor != null)
        assertEquals(157, collected.size)
        assertEquals(157, collected.map { it.type to it.id }.distinct().size)
        assertEquals(collected.map { it.type.name to it.id }.sortedWith(compareBy({ it.first }, { it.second })), collected.map { it.type.name to it.id })
        assertEquals(collected, repository.loadWindow("共同", 4).items)
    }

    @Test fun cursor_is_bound_to_query_and_observation_date() = runBlocking {
        project(1, "相机")
        project(2, "相机")
        val cursor = checkNotNull(repository.searchNames("相机", 1).nextCursor)
        assertRejected { repository.searchNames("服务", 1, cursor) }
        assertRejected { repository.searchNames("相机", 1, cursor, today.plusDays(1)) }
        assertRejected { repository.searchNames("相机", 51) }
        assertRejected { repository.loadWindow("相机", 0) }
    }

    @Test fun loaded_window_rebuilds_after_identity_and_category_changes() = runBlocking {
        (1..55).forEach { project(it, "相机") }
        val first = repository.loadWindow("相机", 2)
        assertEquals(55, first.items.size)
        db.openHelper.writableDatabase.execSQL("UPDATE long_term_projects SET name='别的名称' WHERE id=?", arrayOf(id(2)))
        project(56, "相机")
        val changed = repository.loadWindow("相机", 2)
        assertEquals(55, changed.items.size)
        assertFalse(changed.items.any { it.id == id(2) })
        assertTrue(changed.items.any { it.id == id(56) })
        assertEquals(55, changed.items.map { it.id }.distinct().size)
    }

    @Test fun subscription_future_state_and_payment_use_historical_engine_boundaries() = runBlocking {
        subscription(1, "历史服务")
        db.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(2001), id(1), today.plusDays(1), SubscriptionOperationState.PAUSED, operationId = id(3001)))
        assertEquals(SearchStatus.ACTIVE, repository.loadWindow("历史服务", 1).items.single().status)
        assertEquals(SearchStatus.PAUSED, repository.loadWindow("历史服务", 1, today.plusDays(1)).items.single().status)
        assertEquals(SearchStatus.EXPIRED, repository.loadWindow("历史服务", 1, today.minusDays(20)).items.single().status)
    }

    @Test fun invalid_stored_category_type_is_a_read_error() = runBlocking {
        project(1, "坏类别")
        db.openHelper.writableDatabase.execSQL("UPDATE long_term_projects SET categoryId=? WHERE id=?", arrayOf(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, id(1)))
        var failed = false
        try { repository.loadWindow("坏类别", 1) } catch (_: IllegalStateException) { failed = true }
        assertTrue(failed)
    }

    @Test fun default_sorts_use_ownership_date_and_real_paid_on_with_stable_id_ties() = runBlocking {
        project(3, "later", startedOn = today.minusDays(1))
        project(2, "old 2", startedOn = today.minusYears(4))
        project(1, "old 1", startedOn = today.minusYears(4))
        val projects = ProjectRepository(db, coordinator, FixedBusinessClock(today)).loadCurrentProjectWindow(1)
        assertEquals(listOf(id(1), id(2), id(3)), projects.items.map { it.project.id })
        subscription(10, "recent", paidOn = today.minusDays(1), from = today.plusDays(20), until = today.plusDays(50))
        subscription(11, "older 1", paidOn = today.minusDays(4))
        subscription(12, "older 2", paidOn = today.minusDays(4))
        val subs = SubscriptionRepository(db, coordinator, FixedBusinessClock(today)).loadSubscriptionWindow(1)
        assertEquals(listOf(id(10), id(11), id(12)), subs.items.map { it.subscription.id })
    }

    @Test fun default_list_paging_with_equal_dates_has_no_duplicates() = runBlocking {
        db.withTransaction { (1..105).forEach { project(it, "项目"); subscription(1000 + it, "服务") } }
        val projects = ProjectRepository(db, coordinator, FixedBusinessClock(today)).loadCurrentProjectWindow(3)
        val subs = SubscriptionRepository(db, coordinator, FixedBusinessClock(today)).loadSubscriptionWindow(3)
        assertEquals(105, projects.items.size)
        assertEquals(105, projects.items.map { it.project.id }.distinct().size)
        assertEquals(105, subs.items.size)
        assertEquals(105, subs.items.map { it.subscription.id }.distinct().size)
        assertEquals((1..105).map(::id), projects.items.map { it.project.id })
        assertEquals((1001..1105).map(::id), subs.items.map { it.subscription.id })
    }

    @Test fun history_sorts_by_frozen_held_days_instead_of_purchase_date() = runBlocking {
        project(1, "早买早结束", startedOn = LocalDate.of(2000, 1, 1), endedOn = LocalDate.of(2001, 1, 1))
        project(2, "稍晚买持有更久", startedOn = LocalDate.of(2010, 1, 1), endedOn = LocalDate.of(2020, 1, 1))
        val result = ProjectRepository(db, coordinator, FixedBusinessClock(today)).loadProjectHistoryWindow(1)
        assertEquals(listOf(id(2), id(1)), result.items.map { it.project.id })
        assertTrue(result.items[0].cost.heldDays > result.items[1].cost.heldDays)
    }

    @Test fun history_equal_held_days_keep_stable_identity_paging_across_different_start_dates() = runBlocking {
        db.withTransaction {
            (105 downTo 1).forEach { number ->
                val start = LocalDate.of(2020, 1, 1).plusDays(number.toLong())
                project(number, "冻结时长相同", startedOn = start, endedOn = start.plusDays(99))
            }
        }
        val repository = ProjectRepository(db, coordinator, FixedBusinessClock(today))
        val first = repository.loadProjectHistoryWindow(1)
        assertEquals(50, first.items.size)
        assertEquals(100L, checkNotNull(first.nextCursor).heldDays)
        val all = repository.loadProjectHistoryWindow(3)
        assertEquals(105, all.items.size)
        assertEquals((1..105).map(::id), all.items.map { it.project.id })
        assertEquals(105, all.items.map { it.project.id }.distinct().size)
        assertTrue(all.items.all { it.cost.heldDays == 100L })
    }

    private suspend fun project(number: Int, name: String, startedOn: LocalDate = today.minusDays(9), endedOn: LocalDate? = null, note: String? = null) {
        db.projectDao().insertProject(LongTermProjectEntity(id(number), name, LithicDatabase.PROJECT_FALLBACK_ID, startedOn, endedOn,
            endReason = endedOn?.let { "其他" }, note = note, createdAt = instant, updatedAt = instant))
        db.projectDao().insertEvent(ProjectMoneyEventEntity(id(100_000 + number), id(number), MoneyDirection.OUTLAY, 1_000, startedOn,
            role = ProjectMoneyEventRole.INITIAL, operationId = id(200_000 + number)))
    }

    private suspend fun subscription(number: Int, name: String, state: SubscriptionOperationState? = null,
        paidOn: LocalDate = today.minusDays(9), from: LocalDate = today.minusDays(9), until: LocalDate = today.plusDays(21), note: String? = null) {
        db.subscriptionDao().insertSubscription(SubscriptionEntity(id(number), name, LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            note = note, createdAt = instant, updatedAt = instant))
        db.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(id(300_000 + number), id(number), paidOn, from, until, until, 3_000,
            BillingCycle.MONTHLY, 30, id(400_000 + number)))
        state?.let { db.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(500_000 + number), id(number), today, it,
            operationId = id(600_000 + number))) }
    }

    private suspend fun assertRejected(block: suspend () -> Unit) {
        var rejected = false
        try { block() } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
    }

    private fun id(number: Int): String = UUID(0, number.toLong()).toString()
}
