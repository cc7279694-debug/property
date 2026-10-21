package com.cooper.lithic.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryNames
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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Only a fresh synthetic Room database; never the normal ledger or any media directory. */
@RunWith(AndroidJUnit4::class)
class CategoryRepositoryTest {
    private lateinit var db: LithicDatabase
    private lateinit var coordinator: LocalWriteCoordinator
    private lateinit var repository: CategoryRepository
    private val day = LocalDate.of(2026, 1, 10)
    private val at = Instant.parse("2026-01-01T00:00:00Z")

    @Before fun before() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), LithicDatabase::class.java).build()
        coordinator = LocalWriteCoordinator()
        repository = CategoryRepository(db, coordinator)
        db.initializeDefaults()
    }

    @After fun after() { if (::db.isInitialized) db.close() }

    @Test fun merge_is_atomic_updates_current_and_history_and_preserves_total_and_money() = runBlocking {
        val source = category(CategoryType.PROJECT, "旧设备")
        val target = category(CategoryType.PROJECT, "日常设备")
        val current = project(source.id)
        val ended = project(source.id, endedOn = day)
        val untouched = project(target.id)
        val money = db.projectDao().listEvents()
        val before = ReportRepository(db, coordinator, FixedBusinessClock(day)).loadOverview()
        val preview = repository.previewMerge(source.id, target.id)
        assertEquals(2L, preview.affectedCount)
        assertApplied(repository.merge(preview))
        assertNull(db.categoryDao().find(source.id))
        assertEquals(target.id, db.projectDao().findProject(current.id)!!.categoryId)
        assertEquals(target.id, db.projectDao().findProject(ended.id)!!.categoryId)
        assertEquals(untouched, db.projectDao().findProject(untouched.id))
        assertEquals(money, db.projectDao().listEvents())
        val after = ReportRepository(db, coordinator, FixedBusinessClock(day)).loadOverview()
        assertEquals(before.totalRate, after.totalRate)
        assertEquals(before.projectRate, after.projectRate)
        assertEquals(target.id, after.projectCategories.single().categoryId)
        assertEquals(3L, repository.list(CategoryType.PROJECT).single { it.category.id == target.id }.recordCount)
    }

    @Test fun subscription_merge_preserves_payments_states_and_identity_except_category() = runBlocking {
        val source = category(CategoryType.SUBSCRIPTION, "旧服务")
        val target = category(CategoryType.SUBSCRIPTION, "常用服务")
        val subscription = subscription(source.id)
        val renewals = db.subscriptionDao().renewalsForSubscriptions(listOf(subscription.id))
        val states = db.subscriptionDao().statesForSubscriptions(listOf(subscription.id))
        val before = ReportRepository(db, coordinator, FixedBusinessClock(day)).loadOverview()
        assertApplied(repository.merge(repository.previewMerge(source.id, target.id)))
        assertEquals(subscription.copy(categoryId = target.id), db.subscriptionDao().findSubscription(subscription.id))
        assertEquals(renewals, db.subscriptionDao().renewalsForSubscriptions(listOf(subscription.id)))
        assertEquals(states, db.subscriptionDao().statesForSubscriptions(listOf(subscription.id)))
        assertEquals(before.totalRate, ReportRepository(db, coordinator, FixedBusinessClock(day)).loadOverview().totalRate)
    }

    @Test fun cross_type_merge_rejected_without_mutating_any_category_or_reference() = runBlocking {
        val source = category(CategoryType.PROJECT, "设备")
        val target = category(CategoryType.SUBSCRIPTION, "设备")
        val project = project(source.id)
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.previewMerge(source.id, target.id) } }
        assertTrue(repository.merge(CategoryMergePreview(source, target, 1)) is CategoryMutationResult.ValidationFailure)
        assertEquals(source, db.categoryDao().find(source.id))
        assertEquals(project, db.projectDao().findProject(project.id))
    }

    @Test fun normalized_names_are_unique_per_type_and_creation_can_be_safely_replayed() = runBlocking {
        val id = id()
        assertApplied(repository.create(CategoryType.PROJECT, " Camera ", id))
        val replay = repository.create(CategoryType.PROJECT, " Camera ", id)
        assertTrue(replay is CategoryMutationResult.Applied && replay.replayed)
        assertTrue(repository.create(CategoryType.PROJECT, "ＣＡＭＥＲＡ") is CategoryMutationResult.Conflict)
        assertApplied(repository.create(CategoryType.SUBSCRIPTION, "camera"))
        assertEquals(1, repository.list(CategoryType.PROJECT).count { it.category.normalizedName == "camera" })
    }

    @Test fun blank_and_control_names_are_rejected_and_fallback_stays_fixed() = runBlocking {
        assertTrue(repository.create(CategoryType.PROJECT, " \u3000 ") is CategoryMutationResult.ValidationFailure)
        assertTrue(repository.create(CategoryType.PROJECT, "设备\n分类") is CategoryMutationResult.ValidationFailure)
        val fallback = db.categoryDao().find(LithicDatabase.PROJECT_FALLBACK_ID)!!
        val target = category(CategoryType.PROJECT, "设备")
        assertTrue(repository.rename(fallback, "默认") is CategoryMutationResult.ValidationFailure)
        assertTrue(repository.merge(CategoryMergePreview(fallback, target, 0)) is CategoryMutationResult.ValidationFailure)
        assertEquals(fallback, db.categoryDao().find(fallback.id))
    }

    @Test fun ordinary_category_can_merge_into_same_type_fallback() = runBlocking {
        val source = category(CategoryType.PROJECT, "临时类别")
        val row = project(source.id)
        assertApplied(repository.merge(repository.previewMerge(source.id, LithicDatabase.PROJECT_FALLBACK_ID)))
        assertEquals(LithicDatabase.PROJECT_FALLBACK_ID, db.projectDao().findProject(row.id)!!.categoryId)
        db.requireDefaults()
    }

    @Test fun rename_conflicts_and_stale_loaded_category_are_rejected() = runBlocking {
        val source = category(CategoryType.PROJECT, "设备")
        category(CategoryType.PROJECT, "家电")
        assertTrue(repository.rename(source, "家电") is CategoryMutationResult.Conflict)
        assertApplied(repository.rename(source, "电子设备"))
        assertTrue(repository.rename(source, "旧草稿覆盖") is CategoryMutationResult.Conflict)
        assertEquals("电子设备", db.categoryDao().find(source.id)!!.name)
    }

    @Test fun stale_merge_count_or_category_does_not_silently_expand_confirmed_scope() = runBlocking {
        val source = category(CategoryType.PROJECT, "设备")
        val target = category(CategoryType.PROJECT, "日常")
        val preview = repository.previewMerge(source.id, target.id)
        val row = project(source.id)
        assertTrue(repository.merge(preview) is CategoryMutationResult.Conflict)
        assertEquals(row, db.projectDao().findProject(row.id))
        val next = repository.previewMerge(source.id, target.id)
        assertApplied(repository.rename(target, "已改名"))
        assertTrue(repository.merge(next) is CategoryMutationResult.Conflict)
        assertEquals(source.id, db.projectDao().findProject(row.id)!!.categoryId)
    }

    @Test fun failure_after_reference_update_rolls_back_all_rows_and_source_delete() = runBlocking {
        val source = category(CategoryType.PROJECT, "设备")
        val target = category(CategoryType.PROJECT, "日常")
        val rows = listOf(project(source.id), project(source.id, endedOn = day))
        val events = db.projectDao().listEvents()
        val preview = repository.previewMerge(source.id, target.id)
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER m14_reject_source_delete BEFORE DELETE ON categories WHEN OLD.id='${source.id}' BEGIN SELECT RAISE(ABORT,'synthetic rollback'); END")
        assertFalse(repository.merge(preview) is CategoryMutationResult.Applied)
        assertEquals(source, db.categoryDao().find(source.id))
        rows.forEach { assertEquals(it, db.projectDao().findProject(it.id)) }
        assertEquals(events, db.projectDao().listEvents())
    }

    @Test fun repeated_merge_cannot_delete_or_move_other_records() = runBlocking {
        val source = category(CategoryType.PROJECT, "设备")
        val target = category(CategoryType.PROJECT, "日常")
        project(source.id)
        val preview = repository.previewMerge(source.id, target.id)
        assertApplied(repository.merge(preview))
        val categories = db.categoryDao().list()
        val rows = db.projectDao().listProjects()
        assertFalse(repository.merge(preview) is CategoryMutationResult.Applied)
        assertEquals(categories, db.categoryDao().list())
        assertEquals(rows, db.projectDao().listProjects())
    }

    @Test fun retired_generation_rejects_category_writes_without_reopening_storage() = runBlocking {
        coordinator.retire(verify = {}, dispose = {})
        assertFalse(repository.create(CategoryType.PROJECT, "迟到写入") is CategoryMutationResult.Applied)
        assertEquals(2, db.categoryDao().list().size)
    }

    private suspend fun category(type: CategoryType, name: String): CategoryEntity {
        val category = CategoryEntity(id(), type, name, CategoryNames.normalized(name), false)
        coordinator.withWrite { db.withTransaction { db.categoryDao().insert(category) } }
        return category
    }

    private suspend fun project(categoryId: String, endedOn: LocalDate? = null): LongTermProjectEntity {
        val row = LongTermProjectEntity(id(), "合成设备", categoryId, day.minusDays(9), endedOn,
            if (endedOn != null) "合成结束" else null, createdAt = at, updatedAt = at)
        coordinator.withWrite { db.withTransaction {
            db.projectDao().insertProject(row)
            db.projectDao().insertEvent(ProjectMoneyEventEntity(id(), row.id, MoneyDirection.OUTLAY,
                10000, day.minusDays(9), null, ProjectMoneyEventRole.INITIAL, id()))
        } }
        return row
    }

    private suspend fun subscription(categoryId: String): SubscriptionEntity {
        val row = SubscriptionEntity(id(), "合成服务", categoryId, createdAt = at, updatedAt = at)
        coordinator.withWrite { db.withTransaction {
            db.subscriptionDao().insertSubscription(row)
            db.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(id(), row.id, day.minusDays(9),
                day.minusDays(9), day.plusDays(21), day.plusDays(21), 15000, BillingCycle.MONTHLY, 30, id()))
            db.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(), row.id, day.plusDays(20),
                SubscriptionOperationState.STOPPED, operationId = id()))
        } }
        return row
    }

    private fun assertApplied(result: CategoryMutationResult) { assertTrue(result.toString(), result is CategoryMutationResult.Applied) }
    private fun id() = UUID.randomUUID().toString()
}
