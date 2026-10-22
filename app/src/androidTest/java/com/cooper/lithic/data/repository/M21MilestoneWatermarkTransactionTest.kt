package com.cooper.lithic.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.NotificationReceiptEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.domain.reminder.MilestoneOwnerType
import com.cooper.lithic.domain.reminder.MilestonePlanner
import com.cooper.lithic.domain.subscription.BillingCycle
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic in-memory SQLite only. Device execution remains separately authorized. */
@RunWith(AndroidJUnit4::class)
class M21MilestoneWatermarkTransactionTest {
    private lateinit var db: LithicDatabase
    private val coordinator = LocalWriteCoordinator()
    private val today = LocalDate.of(2026, 4, 10)
    private val instant = Instant.parse("2026-04-10T02:00:00Z")
    private fun projects(on: LocalDate = today) = ProjectWriteRepository(db, coordinator, FixedBusinessClock(on))
    private fun subscriptions(on: LocalDate = today) = SubscriptionRepository(db, coordinator, FixedBusinessClock(on))

    @Before fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LithicDatabase::class.java).build()
        db.initializeDefaults()
    }

    @After fun tearDown() { if (::db.isInitialized) db.close() }

    @Test fun new_project_and_today_inclusive_baseline_commit_together() = runBlocking {
        val command = project()
        assertTrue(projects().createProject(command) is MutationResult.Success)
        val watermark = db.milestoneWatermarkDao().find(MilestoneOwnerType.PROJECT, command.project.id)!!
        assertEquals(today, watermark.cutoffOn)
        assertTrue(watermark.suppressedMask and MilestonePlanner.nodeBit(MilestoneOwnerType.PROJECT, "D100") != 0)
        assertTrue(db.notificationReceiptDao().list().isEmpty())
    }

    @Test fun watermark_insert_failure_rolls_back_identity_and_initial_money() = runBlocking {
        rejectWatermark()
        val command = project()
        val result = projects().createProject(command)
        assertTrue(result is MutationResult.Conflict || result is MutationResult.StorageFailure)
        assertNull(db.projectDao().findProject(command.project.id))
        assertTrue(db.projectDao().eventsForProjects(listOf(command.project.id)).isEmpty())
        assertTrue(db.milestoneWatermarkDao().list().isEmpty())
    }

    @Test fun historical_time_and_watermark_write_failure_roll_back_as_one_transaction() = runBlocking {
        val command = project()
        assertTrue(projects(today.minusDays(1)).createProject(command) is MutationResult.Success)
        val before = db.milestoneWatermarkDao().find(MilestoneOwnerType.PROJECT, command.project.id)!!
        rejectWatermark()
        val result = projects().updateProject(UpdateProjectCommand(id(), command.project, command.project.name,
            command.project.startedOn.minusDays(1), command.project.categoryId, null, instant.plusSeconds(1)))
        assertTrue(result is MutationResult.Conflict || result is MutationResult.StorageFailure)
        assertEquals(command.project, db.projectDao().findProject(command.project.id))
        assertEquals(listOf(command.initialEvent), db.projectDao().eventsForProjects(listOf(command.project.id)))
        assertEquals(before, db.milestoneWatermarkDao().find(MilestoneOwnerType.PROJECT, command.project.id))
    }

    @Test fun rename_and_amount_only_correction_do_not_swallow_today_node() = runBlocking {
        val command = project()
        assertTrue(projects(today.minusDays(1)).createProject(command) is MutationResult.Success)
        val before = db.milestoneWatermarkDao().find(MilestoneOwnerType.PROJECT, command.project.id)!!
        assertEquals(0, before.suppressedMask and MilestonePlanner.nodeBit(MilestoneOwnerType.PROJECT, "D100"))
        assertTrue(projects().updateProject(UpdateProjectCommand(id(), command.project, "改名", command.project.startedOn,
            command.project.categoryId, "备注", instant.plusSeconds(1))) is MutationResult.ProjectUpdated)
        val current = db.projectDao().findProject(command.project.id)!!
        assertTrue(projects().correctMoneyEvent(CorrectMoneyEventCommand(id(), current, command.initialEvent,
            20_000, command.initialEvent.occurredOn, "金额纠错", instant.plusSeconds(2))) is MutationResult.MoneyCorrected)
        assertEquals(before, db.milestoneWatermarkDao().find(MilestoneOwnerType.PROJECT, command.project.id))
    }

    @Test fun historical_start_correction_suppresses_newly_crossed_node_and_replay_is_stable() = runBlocking {
        val command = project().let { it.copy(project = it.project.copy(startedOn = it.project.startedOn.plusDays(1)),
            initialEvent = it.initialEvent.copy(occurredOn = it.initialEvent.occurredOn.plusDays(1))) }
        assertTrue(projects(today.minusDays(1)).createProject(command) is MutationResult.Success)
        val update = UpdateProjectCommand(id(), command.project, command.project.name, command.project.startedOn.minusDays(1),
            command.project.categoryId, null, instant.plusSeconds(1))
        assertTrue(projects().updateProject(update) is MutationResult.ProjectUpdated)
        val after = db.milestoneWatermarkDao().find(MilestoneOwnerType.PROJECT, command.project.id)!!
        assertEquals(today, after.cutoffOn)
        assertTrue(after.suppressedMask and MilestonePlanner.nodeBit(MilestoneOwnerType.PROJECT, "D100") != 0)
        assertEquals(MutationResult.ProjectUpdated(command.project.id, true), projects().updateProject(update))
        assertEquals(after, db.milestoneWatermarkDao().find(MilestoneOwnerType.PROJECT, command.project.id))
        assertTrue(db.notificationReceiptDao().list().isEmpty())
    }

    @Test fun ordinary_end_on_target_day_keeps_its_legal_baseline() = runBlocking {
        val command = project()
        assertTrue(projects(today.minusDays(1)).createProject(command) is MutationResult.Success)
        val before = db.milestoneWatermarkDao().find(MilestoneOwnerType.PROJECT, command.project.id)!!
        assertTrue(projects().endProject(EndProjectCommand(id(), command.project, today, "结束", null,
            instant.plusSeconds(1))) is MutationResult.LifecycleApplied)
        assertEquals(before, db.milestoneWatermarkDao().find(MilestoneOwnerType.PROJECT, command.project.id))
        assertEquals(today, db.projectDao().findProject(command.project.id)!!.endedOn)
    }

    @Test fun normal_renewal_preserves_baseline_but_backfilled_coverage_merges_it() = runBlocking {
        val command = subscription(today.minusDays(10), today.minusDays(2))
        assertTrue(subscriptions(today.minusDays(1)).createWithRenewal(command) is SubscriptionMutationResult.Success)
        val before = db.milestoneWatermarkDao().find(MilestoneOwnerType.SUBSCRIPTION, command.subscription.id)!!
        val ordinary = renewal(command.subscription.id, today, today.plusDays(30))
        assertTrue(subscriptions().confirmRenewal(ConfirmRenewalCommand(ordinary.operationId, ordinary)) is SubscriptionMutationResult.Success)
        assertEquals(before, db.milestoneWatermarkDao().find(MilestoneOwnerType.SUBSCRIPTION, command.subscription.id))
        val backfilled = renewal(command.subscription.id, today.minusDays(100), today.minusDays(10))
        assertTrue(subscriptions().confirmRenewal(ConfirmRenewalCommand(backfilled.operationId, backfilled)) is SubscriptionMutationResult.Success)
        val after = db.milestoneWatermarkDao().find(MilestoneOwnerType.SUBSCRIPTION, command.subscription.id)!!
        assertEquals(today, after.cutoffOn)
        assertTrue(after.suppressedMask and before.suppressedMask == before.suppressedMask)
    }

    @Test fun project_delete_preserves_same_uuid_subscription_and_renewal_receipt() = runBlocking {
        val command = project()
        assertTrue(projects().createProject(command) is MutationResult.Success)
        val sub = subscription(today.minusDays(200), today.plusDays(10), command.project.id)
        assertTrue(subscriptions().createWithRenewal(sub) is SubscriptionMutationResult.Success)
        val projectKey = MilestonePlanner.logicalKey(MilestoneOwnerType.PROJECT, command.project.id, "D100")
        val subKey = MilestonePlanner.logicalKey(MilestoneOwnerType.SUBSCRIPTION, sub.subscription.id, "D100")
        val renewalBoundary = today.plusDays(10).toString()
        val renewalKey = "RENEWAL:${sub.subscription.id}:$renewalBoundary"
        db.notificationReceiptDao().insert(NotificationReceiptEntity(projectKey, command.project.id, "PROJECT_MILESTONE", "D100", instant))
        db.notificationReceiptDao().insert(NotificationReceiptEntity(subKey, sub.subscription.id, "SUBSCRIPTION_MILESTONE", "D100", instant))
        db.notificationReceiptDao().insert(NotificationReceiptEntity(renewalKey, sub.subscription.id, "RENEWAL", renewalBoundary, instant))
        assertTrue(projects().deleteProject(DeleteProjectCommand(id(), command.project, listOf(command.initialEvent))) is MutationResult.ProjectDeleted)
        assertNull(db.milestoneWatermarkDao().find(MilestoneOwnerType.PROJECT, command.project.id))
        assertNotNull(db.milestoneWatermarkDao().find(MilestoneOwnerType.SUBSCRIPTION, command.project.id))
        assertEquals(setOf(subKey, renewalKey), db.notificationReceiptDao().list().map { it.logicalKey }.toSet())
        assertEquals(sub.subscription, db.subscriptionDao().findSubscription(sub.subscription.id))
    }

    @Test fun legacy_initialization_is_complete_and_idempotent_without_fake_receipts() = runBlocking {
        val command = project()
        val sub = subscription(today.minusDays(400), today.plusDays(1))
        db.withTransaction {
            db.projectDao().insertProject(command.project)
            db.projectDao().insertEvent(command.initialEvent)
            db.subscriptionDao().insertSubscription(sub.subscription)
            db.subscriptionDao().insertRenewal(sub.initialRenewal)
            MilestoneWatermarkStore.initializeWithinTransaction(db, today)
        }
        val before = db.milestoneWatermarkDao().list()
        assertEquals(2, before.size)
        db.withTransaction { MilestoneWatermarkStore.initializeWithinTransaction(db, today.plusDays(1)) }
        assertEquals(before, db.milestoneWatermarkDao().list())
        assertTrue(db.notificationReceiptDao().list().isEmpty())
        val sources = db.withTransaction { MilestoneWatermarkStore.sourcesWithinTransaction(db) }
        assertEquals(listOf(command.project.id), sources.projects.map { it.ownerId })
        assertEquals(listOf(sub.subscription.id), sources.subscriptions.map { it.ownerId })
    }

    @Test fun invalid_stored_time_facts_fail_closed_without_partial_initialization() = runBlocking {
        val good = project()
        val bad = project().let { it.copy(project = it.project.copy(endedOn = it.project.startedOn.minusDays(1), endReason = "无效历史")) }
        db.withTransaction {
            listOf(good, bad).forEach {
                db.projectDao().insertProject(it.project)
                db.projectDao().insertEvent(it.initialEvent)
            }
        }
        val failure = runCatching { db.withTransaction { MilestoneWatermarkStore.initializeWithinTransaction(db, today) } }.exceptionOrNull()
        assertNotNull(failure)
        assertTrue(db.milestoneWatermarkDao().list().isEmpty())
        assertEquals(2L, db.projectDao().countProjects())
        assertEquals(bad.project, db.projectDao().findProject(bad.project.id))
    }

    private suspend fun rejectWatermark() = withContext(Dispatchers.IO) {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_m21_watermark BEFORE INSERT ON milestone_watermarks BEGIN SELECT RAISE(ABORT,'synthetic watermark failure'); END")
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_m21_watermark_update BEFORE UPDATE ON milestone_watermarks BEGIN SELECT RAISE(ABORT,'synthetic watermark failure'); END")
    }

    private fun project(ownerId: String = id()): CreateProjectCommand {
        val commandId = id()
        val row = LongTermProjectEntity(ownerId, "合成项目", LithicDatabase.PROJECT_FALLBACK_ID,
            LocalDate.of(2026, 1, 1), createdAt = instant, updatedAt = instant)
        val event = ProjectMoneyEventEntity(id(), ownerId, MoneyDirection.OUTLAY, 10_000, row.startedOn,
            role = ProjectMoneyEventRole.INITIAL, operationId = commandId)
        return CreateProjectCommand(commandId, row, event)
    }

    private fun subscription(from: LocalDate, until: LocalDate, ownerId: String = id()): CreateSubscriptionCommand {
        val row = SubscriptionEntity(ownerId, "合成订阅", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            createdAt = instant, updatedAt = instant)
        val payment = renewal(ownerId, from, until).copy(paidOn = today.minusDays(1))
        return CreateSubscriptionCommand(payment.operationId, row, payment)
    }

    private fun renewal(ownerId: String, from: LocalDate, until: LocalDate): SubscriptionRenewalEntity =
        SubscriptionRenewalEntity(id(), ownerId, today, from, until, until, 10_000,
            BillingCycle.MONTHLY, 30, id())

    private fun id() = UUID.randomUUID().toString()
}
