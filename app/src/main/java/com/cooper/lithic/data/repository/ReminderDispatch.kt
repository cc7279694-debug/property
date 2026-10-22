package com.cooper.lithic.data.repository

import com.cooper.lithic.data.settings.ReminderPreferences
import com.cooper.lithic.domain.reminder.RenewalReminderCandidate
import com.cooper.lithic.domain.reminder.RenewalReminderPlanner
import com.cooper.lithic.domain.reminder.RenewalReminderSource
import com.cooper.lithic.platform.notification.ReminderPublisher
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class ReminderDispatchInput(val sources: List<RenewalReminderSource>, val preferences: ReminderPreferences,
    val successfulKeys: Set<String>, val recordZone: ZoneId)

enum class ReminderDispatchResult { PUBLISHED, SUPPRESSED, OLD_GENERATION, WAITING_FOR_NINE }

/** One actual generation lease from fresh read through publish and receipt. Never takes
 * another owner or treats cancellation/permission rejection as successful publication. */
class ReminderDispatch(
    private val coordinator: LocalWriteCoordinator,
    private val isCurrent: () -> Boolean,
    private val read: suspend () -> ReminderDispatchInput,
    private val record: suspend (RenewalReminderCandidate, Instant) -> Unit,
    private val publisher: ReminderPublisher,
    private val now: () -> Instant = Instant::now,
) {
    suspend fun execute(logicalKey: String, renewalId: String, expectedRevision: String): ReminderDispatchResult = coordinator.withWrite {
        if (!isCurrent()) return@withWrite ReminderDispatchResult.OLD_GENERATION
        val input = read()
        currentCoroutineContext().ensureActive()
        if (!isCurrent()) return@withWrite ReminderDispatchResult.OLD_GENERATION
        val source = input.sources.singleOrNull { row -> row.renewals.any { it.id == renewalId } }
            ?: return@withWrite ReminderDispatchResult.SUPPRESSED
        if (ReminderTaskRevision.of(source) != expectedRevision) return@withWrite ReminderDispatchResult.SUPPRESSED
        if (!input.preferences.renewalEnabled) return@withWrite ReminderDispatchResult.SUPPRESSED
        if (!publisher.canPublish() || !isCurrent()) return@withWrite ReminderDispatchResult.SUPPRESSED
        currentCoroutineContext().ensureActive()
        val observed = now().atZone(input.recordZone)
        // A system-delayed task arriving before the business hour retains a bounded
        // eligibility window; no publication or successful receipt is invented.
        val eligibilityTime = if (observed.hour < 9) observed.toLocalDate().atTime(9, 0).atZone(input.recordZone) else observed
        val candidate = RenewalReminderPlanner.evaluate(input.sources, eligibilityTime,
            input.preferences.renewalEnabled, input.preferences.importCutoff, input.successfulKeys)
            .singleOrNull { it.logicalKey == logicalKey && it.renewalId == renewalId }
            ?: return@withWrite ReminderDispatchResult.SUPPRESSED
        if (observed.hour < 9) return@withWrite ReminderDispatchResult.WAITING_FOR_NINE
        currentCoroutineContext().ensureActive()
        if (!isCurrent()) return@withWrite ReminderDispatchResult.OLD_GENERATION
        if (!publisher.publish(candidate)) return@withWrite ReminderDispatchResult.SUPPRESSED
        // Once the synchronous API returned success, cancellation may not erase that fact.
        // Restore cannot retire this coordinator until the receipt lease finishes.
        withContext(NonCancellable) { record(candidate, now()) }
        ReminderDispatchResult.PUBLISHED
    }
}
