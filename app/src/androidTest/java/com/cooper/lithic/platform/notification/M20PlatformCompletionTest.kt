package com.cooper.lithic.platform.notification

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.job.JobScheduler
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Process
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
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
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.domain.reminder.RenewalReminderPlanner
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
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

/** Finishes exactly one already-published synthetic run; never starts business work. */
@RunWith(AndroidJUnit4::class)
class M20PlatformCompletionTest {
    @Test fun finish_existing_published_platform_run_without_new_work() = runBlocking {
        val run = InstrumentationRegistry.getArguments().getString("m20PlatformRun")
        assumeTrue("An explicit existing synthetic run is required", run != null)
        require(UUID.fromString(run).toString() == run)
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        assertEquals("com.cooper.lithic.debug", context.packageName)
        val root = File(context.noBackupFilesDir.canonicalFile, "m20-platform-$run")
        fun owned(file: File, directory: Boolean = false) = Os.lstat(file.path).also {
            check(it.st_uid == Process.myUid() && if (directory) OsConstants.S_ISDIR(it.st_mode) else OsConstants.S_ISREG(it.st_mode))
            check(file.canonicalFile == file.absoluteFile && (directory || it.st_nlink == 1L))
        }
        fun read(file: File): ByteArray { val before = owned(file); check(before.st_size in 1..131072)
            return file.readBytes().also { check(it.size.toLong() == before.st_size); val after = owned(file)
                check(before.st_dev == after.st_dev && before.st_ino == after.st_ino && before.st_size == after.st_size) } }
        val rootIdentity = owned(root, true)
        assertEquals(setOf("owner.txt", "ledger", "platform-result.json"), root.list()!!.toSet())
        assertEquals(run, read(File(root, "owner.txt")).toString(Charsets.UTF_8))
        val originalFile = File(root, "platform-result.json"); val originalBytes = read(originalFile)
        val original = JSONObject(originalBytes.toString(Charsets.UTF_8))
        check(original.getString("runId") == run && original.getInt("uid") == Process.myUid())
        check(original.getString("status") == "UNKNOWN_OR_FAILED_RETAINED" && !original.getBoolean("ownerClosed"))
        check(original.getString("errorClass") == "kotlinx.coroutines.TimeoutCancellationException" && original.getString("error") == "Timed out waiting for 10000 ms")
        check(original.getString("publisher") == "REAL_ANDROID_NOTIFICATION_MANAGER" && original.getString("candidateState") == "SUCCEEDED")
        val oldProc = File("/proc/${original.getInt("pid")}")
        try { Os.stat(oldProc.path); check(File(oldProc, "stat").readText().substringAfterLast(") ").split(' ')[19] != original.getString("startTicks")) { "Original owner remains alive" } }
        catch (absent: ErrnoException) { check(absent.errno == OsConstants.ENOENT) }
        val ledger = File(root, "ledger"); owned(ledger, true)
        val pointer = JSONObject(read(File(ledger, "active-generation")).toString(Charsets.UTF_8))
        val generationId = original.getString("generation"); require(UUID.fromString(generationId).toString() == generationId)
        check(pointer.getString("stage") == "STABLE" && pointer.getString("activeId") == generationId)
        val periodic = UUID.fromString(original.getString("periodicId")); val candidate = UUID.fromString(original.getString("candidateId"))
        val expectedIds = (0 until original.getJSONArray("workRows").length()).map { original.getJSONArray("workRows").getJSONObject(it).getString("id") }.toSet()
        check(expectedIds.size == 3 && periodic.toString() in expectedIds && candidate.toString() in expectedIds)
        val database = File(original.getString("persistentWorkDatabase")).canonicalFile; owned(database)
        check(database.canonicalFile == File(context.noBackupFilesDir, "androidx.work.workdb").canonicalFile)
        SQLiteDatabase.openDatabase(database.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            val rows = mutableMapOf<String, Int>(); db.rawQuery("SELECT id,state,worker_class_name FROM WorkSpec", null).use { c ->
                while (c.moveToNext()) { check(c.getString(2) == ReminderWorker::class.java.name); rows[c.getString(0)] = c.getInt(1) } }
            check(rows.keys == expectedIds && rows.filterValues { it !in setOf(2, 3, 5) } == mapOf(periodic.toString() to 0))
        }
        val attempted = AtomicInteger()
        WorkManager.initialize(context, Configuration.Builder().setWorkerFactory(object : WorkerFactory() {
            override fun createWorker(c: Context, name: String, p: WorkerParameters): ListenableWorker? { attempted.incrementAndGet(); error("Completion forbids new Worker creation") }
        }).build())
        val manager = WorkManager.getInstance(context)
        fun info(id: String) = checkNotNull(manager.getWorkInfoById(UUID.fromString(id)).get(5, TimeUnit.SECONDS))
        check(info(candidate.toString()).state == WorkInfo.State.SUCCEEDED && info(periodic.toString()).state == WorkInfo.State.ENQUEUED)
        check(expectedIds.filter { it != periodic.toString() }.all { info(it).state.isFinished } && attempted.get() == 0)
        val jobs = context.getSystemService(JobScheduler::class.java).forNamespace("androidx.work.systemjobscheduler")
        val ownJobs = jobs.allPendingJobs; check(ownJobs.size == 1 && ownJobs.single().extras.getString("EXTRA_WORK_SPEC_ID") == periodic.toString())
        check(ownJobs.single().service.packageName == context.packageName && ownJobs.single().service.className == "androidx.work.impl.background.systemjob.SystemJobService")
        val notifications = context.getSystemService(NotificationManager::class.java); val key = original.getString("logicalKey")
        val id = RenewalReminderPlanner.notificationId(key); val notice = notifications.activeNotifications.single { it.tag == key && it.id == id }
        check(notice.notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString() == original.getString("notificationTitle"))
        check(notice.notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString() == original.getString("notificationText") && notice.postTime == original.getLong("notificationPostTime"))
        notifications.cancel(key, id); manager.cancelWorkById(periodic).await()
        withTimeout(10_000) { while (notifications.activeNotifications.any { it.tag == key && it.id == id } || jobs.allPendingJobs.any { it.extras.getString("EXTRA_WORK_SPEC_ID") == periodic.toString() }) delay(50) }
        check(expectedIds.all { info(it).state.isFinished } && info(periodic.toString()).state == WorkInfo.State.CANCELLED && attempted.get() == 0)
        val owner = AppContainer(DataGenerationStore(context, ledger), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        var closed = false
        try {
            owner.start(ZoneId.of(original.getString("recordZone"))); val generation = owner.openStorage(); check(generation.id == generationId)
            val snapshot = generation.backupSnapshotSource().capture(); val data = snapshot.data; val today = LocalDate.parse(original.getString("businessDay"))
            check(data.projects.isEmpty() && data.projectMoneyEvents.isEmpty() && data.mediaAssets.isEmpty() && data.replacements.isEmpty() && data.subscriptionStateChanges.isEmpty())
            check(data.categories.map { it.id }.toSet() == setOf(LithicDatabase.PROJECT_FALLBACK_ID, LithicDatabase.SUBSCRIPTION_FALLBACK_ID))
            val subscription = data.subscriptions.single(); check(subscription.name == "合成平台续费验证" && subscription.categoryId == LithicDatabase.SUBSCRIPTION_FALLBACK_ID && subscription.coverAssetId == null && subscription.note == null)
            // The fixture made two distinct Clock.instant() calls, not one shared timestamp.
            check(!Instant.parse(subscription.createdAt).isAfter(Instant.parse(subscription.updatedAt)) &&
                !Instant.parse(subscription.updatedAt).isAfter(Instant.parse(original.getString("receiptPublishedAt"))))
            val payment = data.subscriptionRenewals.single(); check(payment.subscriptionId == subscription.id && payment.amountCents == 15000L && payment.cycle == "MONTHLY" && payment.standardDays == 30)
            check(payment.paidOn == today.minusDays(31).toString() && payment.effectiveFrom == payment.paidOn && payment.paidUntilExclusive == today.minusDays(1).toString() && payment.reminderDueOn == today.plusDays(2).toString())
            val receipt = snapshot.notificationReceipts.single(); check(receipt.logicalKey == key && receipt.ownerId == subscription.id && receipt.kind == "RENEWAL" && receipt.boundary == payment.reminderDueOn && receipt.publishedAt == original.getString("receiptPublishedAt"))
            check(RenewalReminderPlanner.logicalKey(subscription.id, LocalDate.parse(payment.reminderDueOn)) == key && snapshot.settings.recordZoneId == "Asia/Shanghai")
            check(snapshot.reminderPreferences.renewalEnabled && snapshot.reminderPreferences.milestoneEnabled && snapshot.reminderPreferences.importCutoff == null)
        } finally { owner.close(); closed = true }
        check(read(originalFile).contentEquals(originalBytes) && owned(root, true).st_ino == rootIdentity.st_ino && attempted.get() == 0)
        val result = JSONObject().put("status", "PASS").put("runId", run).put("generation", generationId).put("ownerClosed", closed)
            .put("originalResultSha256", MessageDigest.getInstance("SHA-256").digest(originalBytes).joinToString("") { "%02x".format(it.toInt() and 255) })
            .put("oldPid", original.getInt("pid")).put("oldStartTicks", original.getString("startTicks")).put("completionPid", Process.myPid()).put("uid", Process.myUid())
            .put("periodicId", periodic.toString()).put("candidateId", candidate.toString()).put("authenticatedOsJobIds", JSONArray(ownJobs.map { it.id }))
            .put("periodicFinalState", "CANCELLED").put("ownedNotificationFinalState", "ABSENT").put("newWorkerCreations", attempted.get())
        File(root, "platform-completion.json").apply { check(createNewFile()) }.let { file -> FileOutputStream(file).use { it.write(result.toString(2).toByteArray()); it.fd.sync() } }
        InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply { putString("m20_platform_completion_root", root.path) })
        println("M20_PLATFORM_COMPLETION=${root.path}")
    }
}
