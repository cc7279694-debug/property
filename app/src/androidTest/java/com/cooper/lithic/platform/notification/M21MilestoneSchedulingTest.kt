package com.cooper.lithic.platform.notification

import android.app.Application
import android.content.Context
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.MilestoneSummary
import com.cooper.lithic.data.repository.MilestoneWatermarkStore
import com.cooper.lithic.testutil.M20TestRoots
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Standalone plain-Application isolated-runner method. Work-testing 2.12.0 uses its own
 * in-memory WorkDatabase/TestScheduler. Counting Worker proves only test scheduling;
 * it never opens a ledger or sends a platform notification. Android run is NOT_RUN. */
@RunWith(AndroidJUnit4::class)
class M21MilestoneSchedulingTest {
    @Test fun test_driver_enforces_nine_delay_and_switch_cancels_only_owned_milestone_work(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        // Never replace or cancel an unknown pre-existing WorkManager queue.
        assertTrue("Run this isolated method in a fresh plain Application process",
            runCatching { WorkManager.getInstance(context) }.isFailure)
        val roots = M20TestRoots()
        val root = roots.create(context, "m21-scheduling")
        val ownedWork = linkedSetOf<UUID>()
        val executed = CopyOnWriteArrayList<UUID>()
        var owner: AppContainer? = null
        var manager: WorkManager? = null
        var generation: ManagedGeneration? = null
        var initialized = false
        val today = LocalDate.of(2026, 4, 10)
        val at = Instant.parse("2026-04-10T00:30:00Z") // 08:30 in the record zone.
        val clock = Clock.fixed(at, ZoneOffset.UTC)
        val publisher = object : MilestonePublisher {
            override fun canPublish() = true
            override fun publish(summary: MilestoneSummary): Boolean = error("Scheduling must not publish")
        }
        try {
            val configuration = Configuration.Builder().setExecutor(SynchronousExecutor())
                .setTaskExecutor(SynchronousExecutor()).setWorkerFactory(object : WorkerFactory() {
                    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
                        if (workerClassName != ReminderWorker::class.java.name) return null
                        return object : Worker(appContext, workerParameters) {
                            override fun doWork(): Result {
                                executed += id
                                return Result.success()
                            }
                        }
                    }
                }).build()
            WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
            initialized = true
            val work = WorkManager.getInstance(context).also { manager = it }
            val driver = checkNotNull(WorkManagerTestInitHelper.getTestDriver(context))
            val container = AppContainer(DataGenerationStore(context, File(root, "ledger")),
                CoroutineScope(SupervisorJob() + Dispatchers.IO)).also { owner = it }
            container.start(ZoneId.of("Asia/Shanghai"))
            val current = container.openStorage().also { generation = it }
            roots.generation(root, current.id, prefix = "ledger/")
            current.coordinator.withWrite { current.database.withTransaction {
                val project = LongTermProjectEntity(UUID.randomUUID().toString(), "合成第100日项目",
                    LithicDatabase.PROJECT_FALLBACK_ID, LocalDate.of(2026, 1, 1), createdAt = at, updatedAt = at)
                current.database.projectDao().insertProject(project)
                current.database.projectDao().insertEvent(ProjectMoneyEventEntity(UUID.randomUUID().toString(), project.id,
                    MoneyDirection.OUTLAY, 1000, project.startedOn, role = ProjectMoneyEventRole.INITIAL,
                    operationId = UUID.randomUUID().toString()))
                MilestoneWatermarkStore.initializeWithinTransaction(current.database, today.minusDays(1))
            } }
            // A known one-time renewal task; never install the M20 24h periodic queue.
            val renewal = OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(1, TimeUnit.HOURS)
                .setInputData(workDataOf(ReminderWorker.MODE to ReminderWorker.PUBLISH))
                .addTag(ReminderScheduling.CANDIDATE_TAG).build()
            ownedWork += renewal.id
            work.enqueue(renewal).await()
            MilestoneScheduling.reconcile(context, current, { true }, clock, publisher)
            val first = withTimeout(10_000) {
                work.getWorkInfosByTagFlow(MilestoneScheduling.CANDIDATE_TAG).first { it.size == 1 }.single()
            }
            ownedWork += first.id
            assertEquals(30 * 60 * 1000L, first.initialDelayMillis)
            assertNull(first.periodicityInfo)
            assertEquals(WorkInfo.State.ENQUEUED, first.state)
            assertTrue(executed.isEmpty())
            driver.setInitialDelayMet(first.id)
            withTimeout(10_000) { work.getWorkInfoByIdFlow(first.id).first { it?.state == WorkInfo.State.SUCCEEDED } }
            assertEquals(listOf(first.id), executed.toList())
            assertEquals(WorkInfo.State.ENQUEUED, work.getWorkInfoById(renewal.id).get(10, TimeUnit.SECONDS)!!.state)
            // Counting did not publish/receipt; reconcile produces another pending candidate.
            MilestoneScheduling.reconcile(context, current, { true }, clock, publisher)
            val pending = withTimeout(10_000) {
                work.getWorkInfosByTagFlow(MilestoneScheduling.CANDIDATE_TAG).first {
                    it.any { row -> row.id != first.id && row.state == WorkInfo.State.ENQUEUED }
                }.single { it.id != first.id && it.state == WorkInfo.State.ENQUEUED }
            }
            ownedWork += pending.id
            current.settings.updateReminderPreferences(milestoneEnabled = false)
            MilestoneScheduling.reconcile(context, current, { true }, clock, publisher)
            withTimeout(10_000) { work.getWorkInfoByIdFlow(pending.id).first { it?.state == WorkInfo.State.CANCELLED } }
            assertEquals(WorkInfo.State.ENQUEUED, work.getWorkInfoById(renewal.id).get(10, TimeUnit.SECONDS)!!.state)
            driver.setInitialDelayMet(renewal.id)
            withTimeout(10_000) { work.getWorkInfoByIdFlow(renewal.id).first { it?.state == WorkInfo.State.SUCCEEDED } }
            assertEquals(setOf(first.id, renewal.id), executed.toSet())
            assertTrue(current.database.notificationReceiptDao().list().isEmpty())
            assertTrue(work.getWorkInfosByTag(ReminderScheduling.CANDIDATE_TAG).get(10, TimeUnit.SECONDS).all { it.periodicityInfo == null })
        } finally {
            try {
                if (manager != null) {
                    // Recover only IDs bearing this exact synthetic generation's name tag
                    // if an enqueue succeeded before an assertion/observation failed.
                    val mine = generation?.let { "lithic-milestone-task:lithic-milestone-${it.id}-$today" }
                    if (mine != null) manager!!.getWorkInfosByTag(MilestoneScheduling.CANDIDATE_TAG)
                        .get(10, TimeUnit.SECONDS).filter { mine in it.tags }.forEach { ownedWork += it.id }
                    ownedWork.forEach { manager!!.cancelWorkById(it).await() }
                }
            } finally {
                try {
                    if (initialized) WorkManagerTestInitHelper.closeWorkDatabase()
                } finally {
                    try {
                        owner?.close()
                        roots.ownersClosed(root)
                    } finally {
                        roots.cleanupClosed()
                    }
                }
            }
        }
    }
}
