package com.cooper.lithic.domain.reminder

import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class ReminderRenewal(val id: String, val renewal: Renewal)

data class RenewalReminderSource(
    val ownerId: String,
    val name: String,
    val renewals: List<ReminderRenewal>,
    val stateChanges: List<SubscriptionStateChange> = emptyList(),
)

data class RenewalReminderCandidate(
    val ownerId: String,
    val renewalId: String,
    val boundary: LocalDate,
    val triggerOn: LocalDate,
    val remainingDays: Long,
    val logicalKey: String,
    val notificationId: Int,
    val title: String,
    val text: String,
)

object RenewalReminderPlanner {
    /** Pure eligibility only. The caller must re-read these facts under the generation's
     * coordinator immediately before publishing; this result is not a delivery receipt. */
    fun evaluate(
        sources: List<RenewalReminderSource>,
        recordZoneNow: ZonedDateTime,
        renewalEnabled: Boolean = true,
        restoreCutoff: LocalDate? = null,
        successfulKeys: Set<String> = emptySet(),
    ): List<RenewalReminderCandidate> {
        val today = recordZoneNow.toLocalDate()
        require(sources.map { it.ownerId }.distinct().size == sources.size) { "Reminder owners must be unique" }
        val paymentIds = mutableSetOf<String>()
        val prepared = sources.map { source ->
            require(source.ownerId.isNotBlank() && source.name.isNotBlank()) { "A reminder requires its actual owner and name" }
            source.renewals.forEach { row ->
                require(row.id.isNotBlank() && paymentIds.add(row.id)) { "A reminder payment requires a unique stable row identity" }
            }
            val snapshot = SubscriptionSnapshot(Subscription(source.ownerId), source.renewals.map { it.renewal }, source.stateChanges)
            // The frozen engine validates all intervals and state days, including rows
            // outside the window. EXPIRED is deliberately not a reminder exclusion: Q04
            // permits an independently confirmed reminder date after paid coverage ends.
            source to SubscriptionCostEngine.evaluate(snapshot, today)
        }
        if (!renewalEnabled || recordZoneNow.hour < 9) return emptyList()

        val candidates = prepared.flatMap { (source, observation) ->
            if (observation.displayStatus == SubscriptionDisplayStatus.STOPPED) return@flatMap emptyList()
            val states = source.stateChanges.sortedBy { it.effectiveOn }
            // Newest coverage first is only a deterministic tie-break for the same
            // owner/due key; it never chooses a current price or creates paid coverage.
            source.renewals.sortedWith(compareByDescending<ReminderRenewal> { it.renewal.effectiveFrom }.thenBy { it.id })
                .mapNotNull { row ->
                    val payment = row.renewal
                    val boundary = payment.reminderDueOn
                    val scheduled = scheduledAt(boundary, recordZoneNow.zone)
                    val triggerOn = scheduled.toLocalDate()
                    val key = logicalKey(source.ownerId, boundary)
                    if (payment.paidOn.isAfter(today) || recordZoneNow.isBefore(scheduled) || !today.isBefore(boundary) ||
                        restoreCutoff?.let { !triggerOn.isAfter(it) } == true || key in successfulKeys
                    ) return@mapNotNull null

                    val stateAtDue = states.lastOrNull { !it.effectiveOn.isAfter(boundary) }?.state
                        ?: SubscriptionOperationState.ENABLED
                    if (stateAtDue == SubscriptionOperationState.STOPPED) return@mapNotNull null

                    val alreadyRenewed = source.renewals.any { next ->
                        next.id != row.id && !next.renewal.paidOn.isAfter(today) &&
                            !next.renewal.effectiveFrom.isBefore(payment.paidUntilExclusive) &&
                            !next.renewal.effectiveFrom.isAfter(boundary)
                    }
                    if (alreadyRenewed) return@mapNotNull null

                    val remaining = ChronoUnit.DAYS.between(today, boundary)
                    RenewalReminderCandidate(
                        ownerId = source.ownerId,
                        renewalId = row.id,
                        boundary = boundary,
                        triggerOn = triggerOn,
                        remainingDays = remaining,
                        logicalKey = key,
                        notificationId = notificationId(key),
                        title = "${source.name} · 续费提醒",
                        text = "砺石记录中的到期日为${boundary}，还剩${remaining}天。此提醒不表示服务商已经扣款，也不代替你在服务商侧续费或取消。",
                    )
                }
        }
        return candidates.distinctBy { it.logicalKey }.sortedBy { it.logicalKey }
    }

    fun scheduledAt(boundary: LocalDate, recordZone: ZoneId): ZonedDateTime =
        boundary.minusDays(3).atTime(9, 0).atZone(recordZone)

    /** Catch-up remains non-exact and never consumes the last eligible day before 09:00. */
    fun nextAttemptAt(boundary: LocalDate, recordZoneNow: ZonedDateTime): ZonedDateTime =
        maxOf(scheduledAt(boundary, recordZoneNow.zone), recordZoneNow,
            recordZoneNow.toLocalDate().atTime(9, 0).atZone(recordZoneNow.zone))

    fun logicalKey(ownerId: String, boundary: LocalDate): String {
        require(ownerId.isNotBlank()) { "A logical reminder key requires its stable owner identity" }
        return "RENEWAL:$ownerId:$boundary"
    }

    /** Android must also use the full logical key as its tag, so a hash collision cannot
     * replace a different logical event. Masking handles MIN_VALUE without abs overflow. */
    fun notificationId(logicalKey: String): Int {
        require(logicalKey.isNotBlank()) { "A system notification requires its logical key" }
        return logicalKey.hashCode() and Int.MAX_VALUE
    }
}
