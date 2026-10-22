package com.cooper.lithic.data.backup

import com.cooper.lithic.data.settings.ReminderPreferences
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.domain.reminder.MilestoneOwnerType
import com.cooper.lithic.domain.reminder.MilestonePlanner
import com.cooper.lithic.domain.reminder.MilestoneWatermark
import com.cooper.lithic.domain.reminder.ProjectMilestoneSource
import com.cooper.lithic.domain.reminder.ReminderRenewal
import com.cooper.lithic.domain.reminder.RenewalReminderSource
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId

/** Imported triggers on or before this business day are never rebuilt as new notifications. */
internal object RestoreReminderProtection {
    fun preferences(value: ReminderPreferences, zone: ZoneId, restoredAt: Instant): ReminderPreferences {
        val today = restoredAt.atZone(zone).toLocalDate()
        return value.copy(importCutoff = maxOf(value.importCutoff ?: today, today))
    }

    /** Only called for an unpublished and authenticated candidate. Existing masks/receipts
     * never retreat; today's already reached nodes are suppressed, without inventing a receipt. */
    fun snapshot(value: BackupSnapshot, restoredAt: Instant): BackupSnapshot {
        val zone = ZoneId.of(value.settings.recordZoneId)
        val today = restoredAt.atZone(zone).toLocalDate()
        val prior = value.milestoneWatermarks.associate { row ->
            val type = MilestoneOwnerType.valueOf(row.ownerType)
            (type to row.ownerId) to MilestoneWatermark(type, row.ownerId, LocalDate.parse(row.cutoffOn), row.suppressedMask)
        }
        require(prior.size == value.milestoneWatermarks.size) { "Duplicate restoration watermark" }
        val expectedOwners = value.data.projects.map { MilestoneOwnerType.PROJECT to it.id }.toSet() +
            value.data.subscriptions.map { MilestoneOwnerType.SUBSCRIPTION to it.id }
        require(prior.keys.all { it in expectedOwners }) { "Orphaned restoration watermark" }
        val projectMarks = value.data.projects.map { row ->
            MilestonePlanner.projectWatermark(ProjectMilestoneSource(row.id, row.name,
                LocalDate.parse(row.startedOn), row.endedOn?.let(LocalDate::parse)), today, prior[MilestoneOwnerType.PROJECT to row.id])
        }
        val renewals = value.data.subscriptionRenewals.groupBy { it.subscriptionId }
        val states = value.data.subscriptionStateChanges.groupBy { it.subscriptionId }
        val subscriptionMarks = value.data.subscriptions.map { row ->
            val source = RenewalReminderSource(row.id, row.name, renewals[row.id].orEmpty().map { payment ->
                ReminderRenewal(payment.id, Renewal(Money(payment.amountCents), BillingCycle.valueOf(payment.cycle),
                    LocalDate.parse(payment.paidOn), LocalDate.parse(payment.effectiveFrom), LocalDate.parse(payment.paidUntilExclusive),
                    LocalDate.parse(payment.reminderDueOn), payment.standardDays))
            }, states[row.id].orEmpty().map { state ->
                SubscriptionStateChange(LocalDate.parse(state.effectiveOn), SubscriptionOperationState.valueOf(state.state))
            })
            MilestonePlanner.subscriptionWatermark(source, today, prior[MilestoneOwnerType.SUBSCRIPTION to row.id])
        }
        return value.copy(reminderPreferences = preferences(value.reminderPreferences, zone, restoredAt),
            milestoneWatermarks = (projectMarks + subscriptionMarks).map {
                BackupMilestoneWatermarkV4(it.ownerType.name, it.ownerId, it.cutoffOn.toString(), it.suppressedMask)
            }.sortedWith(compareBy<BackupMilestoneWatermarkV4> { it.ownerType }.thenBy { it.ownerId }))
    }
}
