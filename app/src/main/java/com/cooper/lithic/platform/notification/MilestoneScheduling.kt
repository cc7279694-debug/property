package com.cooper.lithic.platform.notification

import android.content.Context
import androidx.work.*
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.repository.MilestoneRepository
import com.cooper.lithic.domain.reminder.MilestonePlanner
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Reuses M20's daily reconcile. This namespace can cancel only milestone candidates. */
object MilestoneScheduling {
    const val CANDIDATE_TAG = "lithic-milestone-candidate-v1"
    private const val NAME_TAG = "lithic-milestone-task:"
    private val mutex = Mutex()
    internal suspend fun reconcile(context: Context, generation: ManagedGeneration, isCurrent: () -> Boolean,
        clock: Clock = Clock.systemUTC(),
        publisher: MilestonePublisher = NotificationPublisher(context).also { it.ensureChannel() }) = mutex.withLock {
        val manager = WorkManager.getInstance(context)
        val input = MilestoneRepository(generation, isCurrent, publisher, clock).schedulingSnapshot()
        val observed = clock.instant().atZone(input.recordZone)
        val eligibleAt = if (observed.hour < 9) observed.toLocalDate().atTime(9, 0).atZone(input.recordZone) else observed
        val candidates = MilestonePlanner.evaluate(input.projects, input.subscriptions, eligibleAt,
            input.watermarks, input.receipts.map { it.logicalKey }.toSet(), input.preferences.milestoneEnabled)
        val name = "lithic-milestone-${generation.id}-${observed.toLocalDate()}"
        val desired = if (candidates.isNotEmpty() && publisher.canPublish()) name else null
        if (!isCurrent()) return@withLock
        for (work in manager.getWorkInfosByTagFlow(CANDIDATE_TAG).first()) {
            val oldName = work.tags.singleOrNull { it.startsWith(NAME_TAG) }?.removePrefix(NAME_TAG)
            if (oldName != desired && !work.state.isFinished) manager.cancelWorkById(work.id).await()
        }
        if (desired != null && isCurrent()) {
            manager.enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(Duration.between(observed, eligibleAt).toMillis().coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                .setInputData(workDataOf(ReminderWorker.MODE to ReminderWorker.MILESTONE_PUBLISH,
                    ReminderWorker.GENERATION to generation.id))
                .addTag(CANDIDATE_TAG).addTag(NAME_TAG + name).build()).await()
        }
    }
}
