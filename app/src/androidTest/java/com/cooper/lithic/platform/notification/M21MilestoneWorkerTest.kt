package com.cooper.lithic.platform.notification

import android.app.Application
import android.content.Context
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.app.StorageInitializationState
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.repository.DeleteProjectCommand
import com.cooper.lithic.data.repository.MilestoneRepository
import com.cooper.lithic.data.repository.MilestoneSummary
import com.cooper.lithic.data.repository.MilestoneWatermarkStore
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ProjectWriteRepository
import com.cooper.lithic.data.repository.ReminderRepository
import com.cooper.lithic.data.repository.ReminderTaskRevision
import com.cooper.lithic.data.repository.UpdateProjectCommand
import com.cooper.lithic.domain.reminder.MilestoneOwnerType
import com.cooper.lithic.domain.reminder.RenewalReminderCandidate
import com.cooper.lithic.domain.reminder.RenewalReminderPlanner
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.testutil.M20TestRoots
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real Room and direct TestListenableWorkerBuilder, fake publishing only. No periodic
 * WorkManager task/default Application owner/platform notification is started. NOT_RUN
 * until separately authorized on a clean device with the plain isolated runner. */
@RunWith(AndroidJUnit4::class)
class M21MilestoneWorkerTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val roots = M20TestRoots()
    private lateinit var root: File
    private lateinit var owner: AppContainer
    private lateinit var generation: ManagedGeneration
    private lateinit var project: LongTermProjectEntity
    private lateinit var initial: ProjectMoneyEventEntity
    private lateinit var payment: SubscriptionRenewalEntity
    private val today = LocalDate.of(2026, 4, 10)
    private val at = Instant.parse("2026-04-10T01:00:00Z")
    private val clock = Clock.fixed(at, ZoneOffset.UTC)
    private val summaries = mutableListOf<MilestoneSummary>()
    private val renewals = mutableListOf<RenewalReminderCandidate>()
    private var allowed = true
    private val milestonePublisher = object : MilestonePublisher {
        override fun canPublish() = allowed
        override fun publish(summary: MilestoneSummary): Boolean { summaries += summary; return true }
    }
    private val renewalPublisher = object : ReminderPublisher {
        override fun canPublish() = true
        override fun publish(candidate: RenewalReminderCandidate): Boolean { renewals += candidate; return true }
    }

    @Before fun before(): Unit = runBlocking {
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        root = roots.create(context, "m21-worker")
        owner = AppContainer(DataGenerationStore(context, File(root, "ledger")), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        owner.start(ZoneId.of("Asia/Shanghai"))
        generation = owner.openStorage()
        roots.generation(root, generation.id, prefix = "ledger/")
        val sharedId = id()
        project = LongTermProjectEntity(sharedId, "合成第100日项目", LithicDatabase.PROJECT_FALLBACK_ID,
            LocalDate.of(2026, 1, 1), createdAt = at, updatedAt = at)
        initial = ProjectMoneyEventEntity(id(), sharedId, MoneyDirection.OUTLAY, 10_000, project.startedOn,
            role = ProjectMoneyEventRole.INITIAL, operationId = id())
        val subscription = SubscriptionEntity(sharedId, "合成累计100日订阅", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            createdAt = at, updatedAt = at)
        val due = today.plusDays(3)
        payment = SubscriptionRenewalEntity(id(), sharedId, project.startedOn, project.startedOn, due, due,
            36_500, BillingCycle.YEARLY, 365, id())
        generation.coordinator.withWrite { generation.database.withTransaction {
            generation.database.projectDao().insertProject(project)
            generation.database.projectDao().insertEvent(initial)
            generation.database.subscriptionDao().insertSubscription(subscription)
            generation.database.subscriptionDao().insertRenewal(payment)
            // Start the existing records on the day before the real D100 target.
            MilestoneWatermarkStore.initializeWithinTransaction(generation.database, today.minusDays(1))
        } }
    }

    @After fun after(): Unit = runBlocking {
        try {
            if (::owner.isInitialized) {
                owner.close()
                roots.ownersClosed(root)
            }
        } finally {
            roots.cleanupClosed()
        }
    }

    @Test fun room_project_and_subscription_same_day_publish_one_typed_summary_and_complete_batch(): Unit = runBlocking {
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        val summary = summaries.single()
        assertEquals(2, summary.candidates.size)
        assertEquals(setOf(MilestoneOwnerType.PROJECT, MilestoneOwnerType.SUBSCRIPTION), summary.candidates.map { it.ownerType }.toSet())
        assertEquals(setOf(project.id), summary.candidates.map { it.ownerId }.toSet())
        val receipts = generation.database.notificationReceiptDao().list()
        assertEquals(summary.candidates.map { it.logicalKey }.toSet(), receipts.map { it.logicalKey }.toSet())
        assertEquals(setOf("PROJECT_MILESTONE", "SUBSCRIPTION_MILESTONE"), receipts.map { it.kind }.toSet())
        assertTrue(receipts.all { it.publishedAt == at && it.boundary == "D100" })
        assertBusinessFacts()
    }

    @Test fun second_worker_without_new_candidates_does_not_republish_summary(): Unit = runBlocking {
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        val receipts = generation.database.notificationReceiptDao().list()
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(1, summaries.size)
        assertEquals(receipts, generation.database.notificationReceiptDao().list())
    }

    @Test fun mixed_milestone_receipts_do_not_poison_existing_renewal_worker(): Unit = runBlocking {
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(ListenableWorker.Result.success(), worker(renewalPayload()).doWork())
        assertEquals(1, summaries.size)
        assertEquals(1, renewals.size)
        assertEquals(setOf("PROJECT_MILESTONE", "SUBSCRIPTION_MILESTONE", "RENEWAL"),
            generation.database.notificationReceiptDao().list().map { it.kind }.toSet())
        assertBusinessFacts()
    }

    @Test fun permission_and_both_switches_are_independent_and_never_forge_receipts(): Unit = runBlocking {
        allowed = false
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertTrue(summaries.isEmpty())
        assertTrue(generation.database.notificationReceiptDao().list().isEmpty())
        allowed = true
        generation.settings.updateReminderPreferences(milestoneEnabled = false)
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertTrue(summaries.isEmpty())
        assertEquals(ListenableWorker.Result.success(), worker(renewalPayload()).doWork())
        assertEquals(1, renewals.size)
        generation.settings.updateReminderPreferences(renewalEnabled = false, milestoneEnabled = true)
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(1, summaries.size)
        assertEquals(3, generation.database.notificationReceiptDao().list().size)
        assertBusinessFacts()
    }

    @Test fun old_generation_payload_cannot_publish_or_write_receipts(): Unit = runBlocking {
        val data = workDataOf(ReminderWorker.MODE to ReminderWorker.MILESTONE_PUBLISH, ReminderWorker.GENERATION to id())
        assertEquals(ListenableWorker.Result.success(), worker(data).doWork())
        assertTrue(summaries.isEmpty())
        assertTrue(generation.database.notificationReceiptDao().list().isEmpty())
        assertBusinessFacts()
    }

    @Test fun second_receipt_insert_failure_rolls_back_complete_batch_after_publish(): Unit = runBlocking {
        generation.coordinator.withWrite { generation.database.withTransaction {
            generation.database.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_second_m21_receipt BEFORE INSERT ON notification_receipts WHEN (SELECT COUNT(*) FROM notification_receipts) = 1 BEGIN SELECT RAISE(ABORT,'synthetic second receipt failure'); END")
        } }
        assertEquals(ListenableWorker.Result.retry(), worker().doWork())
        assertEquals(1, summaries.size)
        assertTrue(generation.database.notificationReceiptDao().list().isEmpty())
        assertBusinessFacts()
    }

    @Test fun notice_revalidation_rejects_old_generation_changed_history_and_deleted_owner(): Unit = runBlocking {
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        val ref = summaries.single().reference
        val repository = milestoneRepository()
        assertEquals(2, repository.resolve(ref).size)
        assertNotNull(runCatching { repository.resolve(ref.copy(generationId = id())) }.exceptionOrNull())
        val writes = ProjectWriteRepository(generation.database, generation.coordinator, FixedBusinessClock(today))
        assertTrue(writes.updateProject(UpdateProjectCommand(id(), project, project.name, project.startedOn.plusDays(1),
            project.categoryId, null, at.plusSeconds(1))) is MutationResult.ProjectUpdated)
        assertEquals(2, generation.database.notificationReceiptDao().list().size)
        assertNotNull(runCatching { repository.resolve(ref) }.exceptionOrNull())
        val corrected = generation.database.projectDao().findProject(project.id)!!
        assertTrue(writes.deleteProject(DeleteProjectCommand(id(), corrected, listOf(initial))) is MutationResult.ProjectDeleted)
        assertNotNull(runCatching { repository.resolve(ref) }.exceptionOrNull())
        assertEquals(listOf("SUBSCRIPTION_MILESTONE"), generation.database.notificationReceiptDao().list().map { it.kind })
        assertEquals(payment, generation.database.subscriptionDao().findRenewal(payment.id))
    }

    private suspend fun assertBusinessFacts() {
        assertEquals(project, generation.database.projectDao().findProject(project.id))
        assertEquals(listOf(initial), generation.database.projectDao().eventsForProjects(listOf(project.id)))
        assertEquals(payment, generation.database.subscriptionDao().findRenewal(payment.id))
        assertTrue(generation.database.subscriptionDao().statesForSubscriptions(listOf(payment.subscriptionId)).isEmpty())
    }

    private fun current() = (owner.storageState.value as? StorageInitializationState.Ready)?.generation === generation
    private fun milestoneRepository() = MilestoneRepository(generation, ::current, milestonePublisher, clock)
    private suspend fun renewalPayload(): Data {
        val source = ReminderRepository(generation, ::current, renewalPublisher, clock).schedulingSnapshot().sources.single()
        return workDataOf(ReminderWorker.MODE to ReminderWorker.PUBLISH, ReminderWorker.GENERATION to generation.id,
            ReminderWorker.KEY to RenewalReminderPlanner.logicalKey(source.ownerId, payment.reminderDueOn),
            ReminderWorker.RENEWAL to payment.id, ReminderWorker.REVISION to ReminderTaskRevision.of(source))
    }

    private fun worker(data: Data = workDataOf(ReminderWorker.MODE to ReminderWorker.MILESTONE_PUBLISH,
        ReminderWorker.GENERATION to generation.id)): ReminderWorker =
        TestListenableWorkerBuilder<ReminderWorker>(context).setInputData(data).setWorkerFactory(object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
                if (workerClassName == ReminderWorker::class.java.name) ReminderWorker(appContext, workerParameters,
                    owner, renewalPublisher, clock, milestonePublisher = milestonePublisher) else null
        }).build()

    private fun id() = UUID.randomUUID().toString()
}
