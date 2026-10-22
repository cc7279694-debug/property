package com.cooper.lithic.platform.notification

import android.Manifest
import com.cooper.lithic.testutil.M20TestRoots
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.repository.ProjectWriteRepository
import com.cooper.lithic.data.repository.DeleteProjectCommand
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ReminderRepository
import com.cooper.lithic.data.repository.ReminderTaskRevision
import com.cooper.lithic.domain.reminder.RenewalReminderCandidate
import com.cooper.lithic.domain.reminder.RenewalReminderPlanner
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
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

/** NOT_RUN until an authorized device exists. Requires the plain-Application isolated
 * runner; every ledger is new, no WorkManager singleton/default owner or system publish. */
@RunWith(AndroidJUnit4::class)
class M20ReminderWorkerTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val roots = M20TestRoots()
    private lateinit var root: File
    private val at = Instant.parse("2026-10-17T01:00:00Z")
    private val due = LocalDate.parse("2026-10-20")
    private val clock = Clock.fixed(at, ZoneOffset.UTC)
    private lateinit var owner: AppContainer
    private lateinit var generation: ManagedGeneration
    private lateinit var payment: SubscriptionRenewalEntity
    private lateinit var payload: Data
    private val attempts = mutableListOf<RenewalReminderCandidate>()
    private var allowed = true
    private val publisher = object : ReminderPublisher {
        override fun canPublish() = allowed
        override fun publish(candidate: RenewalReminderCandidate): Boolean { attempts += candidate; return true }
    }

    @Before fun before(): Unit = runBlocking {
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        root = roots.create(context, "worker")
        owner = AppContainer(DataGenerationStore(context, File(root, "ledger")), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        owner.start(ZoneId.of("Asia/Shanghai")); generation = owner.openStorage()
        roots.generation(root, generation.id, prefix = "ledger/")
        val subscription = SubscriptionEntity(id(), "合成Worker服务", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            createdAt = at, updatedAt = at)
        payment = SubscriptionRenewalEntity(id(), subscription.id, due.minusMonths(1), due.minusMonths(1), due, due,
            15_000, BillingCycle.MONTHLY, 30, id())
        generation.coordinator.withWrite { generation.database.withTransaction {
            generation.database.subscriptionDao().insertSubscription(subscription)
            generation.database.subscriptionDao().insertRenewal(payment)
        } }
        val source = repository().schedulingSnapshot().sources.single()
        payload = workDataOf(ReminderWorker.MODE to ReminderWorker.PUBLISH, ReminderWorker.GENERATION to generation.id,
            ReminderWorker.KEY to RenewalReminderPlanner.logicalKey(source.ownerId, due),
            ReminderWorker.RENEWAL to payment.id, ReminderWorker.REVISION to ReminderTaskRevision.of(source))
    }

    @After fun after(): Unit = runBlocking {
        if (::owner.isInitialized) { owner.close(); roots.ownersClosed(root) }
        roots.cleanupClosed()
    }

    private fun id() = UUID.randomUUID().toString()
    private fun repository() = ReminderRepository(generation, { true }, publisher, clock)
    private fun worker(data: Data = payload): ReminderWorker = TestListenableWorkerBuilder<ReminderWorker>(context)
        .setInputData(data).setWorkerFactory(object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
                if (workerClassName == ReminderWorker::class.java.name) ReminderWorker(appContext, workerParameters, owner, publisher, clock) else null
        }).build()

    @Test fun deleting_project_with_same_uuid_cannot_delete_subscription_receipt(): Unit = runBlocking {
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        val receipt = generation.database.notificationReceiptDao().list().single()
        val project = LongTermProjectEntity(payment.subscriptionId, "同UUID不同类型", LithicDatabase.PROJECT_FALLBACK_ID,
            payment.effectiveFrom, createdAt = at, updatedAt = at)
        val event = ProjectMoneyEventEntity(id(), project.id, MoneyDirection.OUTLAY, 1000,
            project.startedOn, null, ProjectMoneyEventRole.INITIAL, id())
        generation.coordinator.withWrite { generation.database.withTransaction {
            generation.database.projectDao().insertProject(project)
            generation.database.projectDao().insertEvent(event)
        } }
        val writes = ProjectWriteRepository(generation.database, generation.coordinator,
            FixedBusinessClock(LocalDate.parse("2026-10-17")))
        assertEquals(MutationResult.ProjectDeleted(project.id), writes.deleteProject(DeleteProjectCommand(id(), project, listOf(event))))
        assertEquals(receipt, generation.database.notificationReceiptDao().find(receipt.logicalKey))
        assertEquals(payment, generation.database.subscriptionDao().renewalsForSubscriptions(listOf(payment.subscriptionId)).single())
    }

    @Test fun actual_worker_publishes_once_and_room_receipt_suppresses_queued_retry(): Unit = runBlocking {
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertEquals(1, attempts.size)
        val receipt = generation.database.notificationReceiptDao().list().single()
        assertEquals(attempts.single().logicalKey, receipt.logicalKey)
        assertEquals(payment.subscriptionId, receipt.ownerId)
        assertEquals(at, receipt.publishedAt)
        assertEquals(payment, generation.database.subscriptionDao().findRenewal(payment.id))
    }

    @Test fun permission_denial_leaves_no_receipt_and_preserves_complete_existing_business_facts(): Unit = runBlocking {
        allowed = false
        val before = generation.backupSnapshotSource().capture()
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertTrue(attempts.isEmpty())
        assertTrue(generation.database.notificationReceiptDao().list().isEmpty())
        assertEquals(before, generation.backupSnapshotSource().capture())
    }

    @Test fun old_generation_payload_cannot_publish_or_write_current_generation_receipt(): Unit = runBlocking {
        val old = Data.Builder().putAll(payload).putString(ReminderWorker.GENERATION, id()).build()
        val before = generation.backupSnapshotSource().capture()
        assertEquals(ListenableWorker.Result.success(), worker(old).doWork())
        assertTrue(attempts.isEmpty())
        assertEquals(before, generation.backupSnapshotSource().capture())
    }

    @Test fun same_row_date_or_state_correction_rejects_the_saved_worker_revision(): Unit = runBlocking {
        generation.coordinator.withWrite { assertEquals(1, generation.database.subscriptionDao().updateRenewal(payment.copy(paidOn = payment.paidOn.minusDays(1)))) }
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertTrue(attempts.isEmpty())
        generation.coordinator.withWrite { generation.database.withTransaction {
            assertEquals(1, generation.database.subscriptionDao().updateRenewal(payment))
            generation.database.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(), payment.subscriptionId,
                due.minusDays(4), SubscriptionOperationState.PAUSED, operationId = id()))
        } }
        assertEquals(ListenableWorker.Result.success(), worker().doWork())
        assertTrue(attempts.isEmpty())
        assertTrue(generation.database.notificationReceiptDao().list().isEmpty())
    }

    @Test fun actual_publisher_channel_and_permission_status_use_platform_api_without_sending(): Unit = runBlocking {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        assertTrue(testContext.packageName.endsWith(".test"))
        assertEquals(InstrumentationRegistry.getInstrumentation().targetContext.packageName, context.packageName)
        assertEquals(context.packageManager.getApplicationInfo(context.packageName, 0).uid, android.os.Process.myUid())
        val actual = NotificationPublisher(context)
        actual.ensureChannel()
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = checkNotNull(manager.getNotificationChannel(NotificationPublisher.CHANNEL))
        val access = actual.access()
        assertEquals(Build.VERSION.SDK_INT >= 33, access.requiresRuntimePermission)
        assertEquals(Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED, access.runtimeGranted)
        assertEquals(manager.areNotificationsEnabled(), access.appEnabled)
        assertEquals(channel.importance != NotificationManager.IMPORTANCE_NONE, access.channelEnabled)
        assertEquals(access.allowed, actual.canPublish())
    }
}
