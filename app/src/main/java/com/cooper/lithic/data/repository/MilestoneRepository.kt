package com.cooper.lithic.data.repository

import androidx.room.withTransaction
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.NotificationReceiptEntity
import com.cooper.lithic.data.local.NotificationReceiptIdentity
import com.cooper.lithic.domain.reminder.MilestoneOwnerType
import com.cooper.lithic.platform.notification.MilestonePublisher
import java.time.Clock

/** Uses the existing generation owner. Initialization is committed before any publish admission. */
class MilestoneRepository(private val generation: ManagedGeneration, private val isCurrent: () -> Boolean,
    private val publisher: MilestonePublisher, private val clock: Clock = Clock.systemUTC()) {
    suspend fun schedulingSnapshot(): MilestoneDispatchInput = generation.coordinator.withWrite {
        check(isCurrent()) { "Generation changed" }
        inputWithinLease(initialize = true)
    }

    suspend fun publish(): ReminderDispatchResult = MilestoneDispatch(generation.id, generation.coordinator,
        isCurrent, { inputWithinLease(initialize = true) }, { rows, at ->
            generation.database.withTransaction {
                rows.forEach { row ->
                    generation.database.notificationReceiptDao().insert(NotificationReceiptEntity(row.logicalKey,
                        row.ownerId, if (row.ownerType == MilestoneOwnerType.PROJECT) "PROJECT_MILESTONE" else "SUBSCRIPTION_MILESTONE",
                        row.nodeCode, at))
                }
            }
        }, publisher, clock::instant).execute()

    suspend fun resolve(reference: MilestoneNoticeRef): List<com.cooper.lithic.domain.reminder.MilestoneCandidate> =
        generation.coordinator.withSnapshot {
            check(isCurrent() && generation.id == reference.generationId) { "Notification belongs to an old dataset" }
            val input = inputWithinLease(initialize = false)
            check(!reference.publicationOn.isAfter(clock.instant().atZone(input.recordZone).toLocalDate()))
            MilestoneSummaries.resolve(reference, generation.id, input)
        }

    private suspend fun inputWithinLease(initialize: Boolean): MilestoneDispatchInput {
        val zone = generation.settings.backupRecordZoneWithinSnapshot(generation.coordinator)
        val preferences = generation.settings.reminderPreferencesWithinSnapshot(generation.coordinator)
        return generation.database.withTransaction {
            check(isCurrent()) { "Generation changed" }
            val sources = if (initialize) MilestoneWatermarkStore.initializeWithinTransaction(generation.database,
                clock.instant().atZone(zone).toLocalDate()) else MilestoneWatermarkStore.sourcesWithinTransaction(generation.database)
            val projects = sources.projects.map { it.ownerId }.toSet()
            val subscriptions = sources.subscriptions.map { it.ownerId }.toSet()
            val all = generation.database.notificationReceiptDao().list()
            requireReceiptOwners(all, projects, subscriptions)
            MilestoneDispatchInput(sources.projects, sources.subscriptions,
                generation.database.milestoneWatermarkDao().list().map { it.domain() },
                all.mapNotNull { row -> NotificationReceiptIdentity.milestoneOwnerType(row.kind)?.let { type ->
                    MilestonePublishedReceipt(type, row.ownerId, row.boundary, row.logicalKey, row.publishedAt)
                } }, preferences, zone)
        }
    }
}

/** Shared strict branch validation: known milestone rows must not poison renewal dispatch. */
internal fun requireReceiptOwners(rows: List<NotificationReceiptEntity>, projects: Set<String>, subscriptions: Set<String>) {
    check(rows.map { it.logicalKey }.distinct().size == rows.size) { "Duplicate notification receipt" }
    rows.forEach { row ->
        NotificationReceiptIdentity.validate(row.logicalKey, row.ownerId, row.kind, row.boundary)
        val owners = when (row.kind) {
            "RENEWAL", "SUBSCRIPTION_MILESTONE" -> subscriptions
            "PROJECT_MILESTONE" -> projects
            else -> error("Unknown receipt kind")
        }
        check(row.ownerId in owners) { "A receipt has an unknown typed owner" }
    }
}
