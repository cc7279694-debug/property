package com.cooper.lithic.platform.notification

import android.content.Context
import androidx.work.await
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.repository.ReminderRepository
import com.cooper.lithic.data.repository.ReminderTaskRevision
import com.cooper.lithic.domain.reminder.RenewalReminderPlanner
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first

/** Task state is rebuildable. Every candidate is generation AND complete source-revision bound. */
object ReminderScheduling {
    const val CANDIDATE_TAG="lithic-renewal-candidate-v1"
    private const val NAME_TAG="lithic-task:"
    private val reconciliation = Mutex()
    fun start(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("lithic-renewal-daily-v1", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderWorker>(24,TimeUnit.HOURS)
                .setInputData(workDataOf(ReminderWorker.MODE to ReminderWorker.RECONCILE))
                .setInitialDelay(24,TimeUnit.HOURS).build())
        request(context)
    }
    fun request(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork("lithic-renewal-reconcile-v1", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>().setInputData(workDataOf(ReminderWorker.MODE to ReminderWorker.RECONCILE)).build())
    }
    internal suspend fun reconcile(context: Context, generation: ManagedGeneration, isCurrent: () -> Boolean,
        clock: Clock = Clock.systemUTC()) = reconciliation.withLock {
        val manager = WorkManager.getInstance(context)
        val publisher = NotificationPublisher(context).also { it.ensureChannel() }
        val input = ReminderRepository(generation,isCurrent,publisher,clock).schedulingSnapshot()
        val now = clock.instant(); val today=now.atZone(input.recordZone).toLocalDate()
        val desired = linkedMapOf<String, androidx.work.OneTimeWorkRequest>()
        if (input.preferences.renewalEnabled && publisher.canPublish()) {
            input.sources.forEach { source ->
                val revision=ReminderTaskRevision.of(source)
                source.renewals.forEach { row ->
                    val due=row.renewal.reminderDueOn
                    val key=RenewalReminderPlanner.logicalKey(source.ownerId,due)
                    val at=RenewalReminderPlanner.nextAttemptAt(due,now.atZone(input.recordZone))
                    if (!row.renewal.paidOn.isAfter(today) && due.isAfter(today) && key !in input.successfulKeys &&
                        (input.preferences.importCutoff==null || due.minusDays(3).isAfter(input.preferences.importCutoff))) {
                        // Potential tasks may be suppressed by fresh complete facts at execution.
                        val name="lithic-renewal-${generation.id}-${RenewalReminderPlanner.notificationId(key)}-$revision-${row.id}"
                        desired[name]=OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(
                            Duration.between(now,at.toInstant()).toMillis().coerceAtLeast(0),TimeUnit.MILLISECONDS)
                            .setBackoffCriteria(BackoffPolicy.LINEAR,15,TimeUnit.MINUTES)
                            .setInputData(workDataOf(ReminderWorker.MODE to ReminderWorker.PUBLISH, ReminderWorker.GENERATION to generation.id,
                                ReminderWorker.KEY to key,ReminderWorker.RENEWAL to row.id,ReminderWorker.REVISION to revision))
                            .addTag(CANDIDATE_TAG).addTag(NAME_TAG+name).build()
                    }
                }
            }
        }
        if (!isCurrent()) return@withLock
        for (old in manager.getWorkInfosByTagFlow(CANDIDATE_TAG).first()) {
            val name=old.tags.singleOrNull { it.startsWith(NAME_TAG) }?.removePrefix(NAME_TAG)
            if (name !in desired && !old.state.isFinished) manager.cancelWorkById(old.id).await()
        }
        for ((name,work) in desired) {
            if (!isCurrent()) return@withLock
            manager.enqueueUniqueWork(name,ExistingWorkPolicy.KEEP,work).await()
        }
    }
}
