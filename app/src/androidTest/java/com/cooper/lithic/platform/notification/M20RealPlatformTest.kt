package com.cooper.lithic.platform.notification

import android.app.Application
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.app.Notification
import android.app.NotificationManager
import android.app.job.JobScheduler
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Process
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.await
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.app.StorageInitializationState
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.repository.ReminderRepository
import com.cooper.lithic.domain.reminder.RenewalReminderPlanner
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import java.io.File
import java.io.FileOutputStream
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.Collections
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Extra platform evidence, separate from the original 22 methods. No WorkManager test
 * driver, fake Publisher, default ledger or forced task execution is used here. */
@RunWith(AndroidJUnit4::class)
class M20RealPlatformTest {
    @Test fun real_persistent_queue_publishes_q04_notice_and_saves_one_receipt() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        assertEquals("com.cooper.lithic.debug", context.packageName)
        val clock = Clock.systemUTC()
        val zone = ZoneId.of("Asia/Shanghai")
        val observed = clock.instant().atZone(zone)
        assumeTrue("Real publication is only eligible after the record-zone business hour", observed.hour >= 9)
        val publisher = NotificationPublisher(context)
        publisher.ensureChannel()
        assertTrue("The supervisor must explicitly grant this new synthetic package's notification permission", publisher.access().allowed)
        assertFalse(File(context.noBackupFilesDir, "active-generation").exists())
        assertFalse(File(context.noBackupFilesDir, "generations").exists())

        val run = UUID.randomUUID().toString()
        val root = File(context.noBackupFilesDir, "m20-platform-$run").canonicalFile
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.mkdir())
        File(root, "owner.txt").writeText(run)
        instrumentation.sendStatus(0, Bundle().apply { putString("m20_platform_evidence_root", root.absolutePath) })
        val owner = AppContainer(DataGenerationStore(context, File(root, "ledger")), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        owner.start(zone)
        val generation = owner.openStorage()
        val today = observed.toLocalDate()
        val due = today.plusDays(2)
        val subscription = SubscriptionEntity(UUID.randomUUID().toString(), "合成平台续费验证", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            createdAt = clock.instant(), updatedAt = clock.instant())
        val payment = SubscriptionRenewalEntity(UUID.randomUUID().toString(), subscription.id,
            today.minusDays(31), today.minusDays(31), today.minusDays(1), due, 15000, BillingCycle.MONTHLY, 30, UUID.randomUUID().toString())
        generation.coordinator.withWrite { generation.database.withTransaction {
            generation.database.subscriptionDao().insertSubscription(subscription)
            generation.database.subscriptionDao().insertRenewal(payment)
        } }
        fun current() = (owner.storageState.value as? StorageInitializationState.Ready)?.generation === generation
        val repository = ReminderRepository(generation, ::current, publisher, clock)
        val input = repository.schedulingSnapshot()
        val source = input.sources.single()
        val cost = SubscriptionCostEngine.evaluate(SubscriptionSnapshot(Subscription(source.ownerId), source.renewals.map { it.renewal }, emptyList()), today)
        assertEquals(SubscriptionDisplayStatus.EXPIRED, cost.displayStatus)
        assertEquals(0, cost.currentRate.signum())
        val before = generation.backupSnapshotSource().capture()
        val key = RenewalReminderPlanner.logicalKey(subscription.id, due)
        val systemId = RenewalReminderPlanner.notificationId(key)
        val events = Collections.synchronizedList(mutableListOf<String>())
        val factory = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, parameters: WorkerParameters): ListenableWorker? {
                if (workerClassName != ReminderWorker::class.java.name) return null
                events += JSONObject().put("mode", parameters.inputData.getString(ReminderWorker.MODE))
                    .put("generation", parameters.inputData.getString(ReminderWorker.GENERATION))
                    .put("pid", Process.myPid()).put("uid", Process.myUid()).toString()
                return ReminderWorker(appContext, parameters, owner, NotificationPublisher(appContext), clock)
            }
        }
        var started = false
        var safelyFinished = false
        var periodic: WorkInfo? = null
        val evidence = JSONObject().put("runId", run).put("pid", Process.myPid()).put("uid", Process.myUid())
            .put("startTicks", File("/proc/self/stat").readText().substringAfterLast(") ").split(' ')[19])
            .put("recordZone", zone.id).put("businessDay", today.toString()).put("logicalKey", key)
            .put("generation", generation.id).put("publisher", "REAL_ANDROID_NOTIFICATION_MANAGER")
            .put("workManager", "REAL_DEFAULT_SCHEDULERS_NO_TEST_DRIVER")
        try {
            WorkManager.initialize(context, Configuration.Builder().setWorkerFactory(factory).build())
            val manager = WorkManager.getInstance(context)
            ReminderScheduling.start(context)
            started = true
            withTimeout(90_000) {
                while (generation.database.notificationReceiptDao().find(key) == null) delay(100)
            }
            val candidates = withTimeout(15_000) {
                var value: List<WorkInfo>
                do {
                    value = manager.getWorkInfosByTag(ReminderScheduling.CANDIDATE_TAG).get(5, TimeUnit.SECONDS)
                        .filter { row -> row.tags.any { it.contains(generation.id) } }
                    if (value.size != 1 || value.single().state != WorkInfo.State.SUCCEEDED) delay(50)
                } while (value.size != 1 || value.single().state != WorkInfo.State.SUCCEEDED)
                value
            }
            periodic = manager.getWorkInfosForUniqueWork("lithic-renewal-daily-v1").get(5, TimeUnit.SECONDS).single()
            assertEquals(WorkInfo.State.ENQUEUED, periodic!!.state)
            val databases = listOf(File(context.noBackupFilesDir, "androidx.work.workdb"), context.getDatabasePath("androidx.work.workdb"))
                .distinctBy { it.canonicalPath }.filter { it.isFile }
            assertEquals("Exactly one actual WorkManager database must be identifiable", 1, databases.size)
            val rows = JSONArray()
            SQLiteDatabase.openDatabase(databases.single().absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { database ->
                database.rawQuery("SELECT id,state,worker_class_name,initial_delay,interval_duration FROM WorkSpec", null).use { cursor ->
                    while (cursor.moveToNext()) rows.put(JSONObject().put("id", cursor.getString(0)).put("state", cursor.getInt(1))
                        .put("workerClass", cursor.getString(2)).put("initialDelayMs", cursor.getLong(3)).put("intervalMs", cursor.getLong(4)))
                }
            }
            assertTrue((0 until rows.length()).any { rows.getJSONObject(it).getString("id") == periodic!!.id.toString() && rows.getJSONObject(it).getLong("intervalMs") == 86_400_000L })
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            val active = notificationManager.activeNotifications.single { it.tag == key && it.id == systemId }
            val title = active.notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
            val text = active.notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
            assertEquals("合成平台续费验证 · 续费提醒", title)
            assertTrue(text.contains("砺石记录中的到期日为$due"))
            assertTrue(text.contains("还剩2天"))
            val receipt = checkNotNull(generation.database.notificationReceiptDao().find(key))
            assertEquals(payment.subscriptionId, receipt.ownerId)
            val after = generation.backupSnapshotSource().capture()
            assertEquals(before.data, after.data)
            assertEquals(before.settings, after.settings)
            assertEquals(before.reminderPreferences, after.reminderPreferences)
            assertEquals(1, after.notificationReceipts.size)
            ReminderScheduling.reconcile(context, generation, ::current, clock)
            assertEquals(candidates.single().id, manager.getWorkInfosByTag(ReminderScheduling.CANDIDATE_TAG).get(5, TimeUnit.SECONDS)
                .single { row -> row.tags.any { it.contains(generation.id) } }.id)
            assertEquals(1, generation.database.notificationReceiptDao().list().size)
            val scheduler = context.getSystemService(JobScheduler::class.java).let {
                if (Build.VERSION.SDK_INT >= 34) it.forNamespace("androidx.work.systemjobscheduler") else it
            }
            val pendingJobs = scheduler.allPendingJobs
                .filter { it.extras.getString("EXTRA_WORK_SPEC_ID") == periodic!!.id.toString() }
            assertTrue("The persistent periodic WorkSpec must have its own actual OS job", pendingJobs.isNotEmpty())
            evidence.put("persistentWorkDatabase", databases.single().absolutePath).put("workRows", rows)
                .put("factoryTrace", JSONArray(events.toList())).put("periodicId", periodic!!.id.toString())
                .put("candidateId", candidates.single().id.toString()).put("candidateState", candidates.single().state.name)
                .put("ownPendingOsJobIds", JSONArray(pendingJobs.map { it.id })).put("notificationTitle", title)
                .put("notificationText", text).put("notificationPostTime", active.postTime).put("receiptPublishedAt", receipt.publishedAt.toString())
            // API36 shade content can occupy a different interactive window from the focus root.
            val serviceInfo = checkNotNull(instrumentation.uiAutomation.serviceInfo)
            serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            instrumentation.uiAutomation.serviceInfo = serviceInfo
            assertTrue(instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS))
            instrumentation.waitForIdleSync()
            withTimeout(10_000) {
                while (!ownNoticeIsVisible()) delay(100)
            }
            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            val screenshot = File(root, "real-notification-tray.png").apply { check(createNewFile()) }
            try { FileOutputStream(screenshot).use { output -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); output.fd.sync() } }
            finally { bitmap.recycle(); assertTrue(instrumentation.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)) }
            notificationManager.cancel(key, systemId)
            assertTrue(notificationManager.activeNotifications.none { it.tag == key && it.id == systemId })
            manager.cancelWorkById(periodic!!.id).await()
            assertEquals(WorkInfo.State.CANCELLED, checkNotNull(manager.getWorkInfoById(periodic!!.id).get(5, TimeUnit.SECONDS)).state)
            withTimeout(10_000) {
                while (manager.getWorkInfosByTag(ReminderScheduling.CANDIDATE_TAG).get(5, TimeUnit.SECONDS).any { !it.state.isFinished } ||
                    manager.getWorkInfosForUniqueWork("lithic-renewal-reconcile-v1").get(5, TimeUnit.SECONDS).any { !it.state.isFinished }) delay(50)
            }
            assertFalse(File(context.noBackupFilesDir, "active-generation").exists())
            assertFalse(File(context.noBackupFilesDir, "generations").exists())
            evidence.put("status", "PASS").put("periodicFinalState", "CANCELLED").put("ownedNotificationFinalState", "ABSENT")
            safelyFinished = true
        } catch (failure: Throwable) {
            evidence.put("status", "UNKNOWN_OR_FAILED_RETAINED").put("errorClass", failure.javaClass.name).put("error", failure.message)
            throw failure
        } finally {
            if (!started || safelyFinished) owner.close()
            evidence.put("ownerClosed", !started || safelyFinished)
            File(root, "platform-result.json").apply { check(createNewFile()) }.writeText(evidence.toString(2))
            println("M20_REAL_PLATFORM_EVIDENCE=${root.absolutePath}")
        }
    }

    private fun ownNoticeIsVisible(): Boolean {
        for (window in InstrumentationRegistry.getInstrumentation().uiAutomation.windows) {
            val root = window.root ?: continue
            try {
                if (root.packageName?.toString() != "com.android.systemui") continue
                val titles = root.findAccessibilityNodeInfosByViewId("android:id/title")
                try {
                    if (titles.any { it.isVisibleToUser && it.text?.toString() == "合成平台续费验证 · 续费提醒" }) return true
                } finally { titles.forEach { it.recycle() } }
            } finally { root.recycle() }
        }
        return false
    }
}
