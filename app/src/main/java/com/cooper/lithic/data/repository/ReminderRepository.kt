package com.cooper.lithic.data.repository

import com.cooper.lithic.core.time.ZonedBusinessClock
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.NotificationReceiptEntity
import com.cooper.lithic.platform.notification.ReminderPublisher
import java.time.Clock

/** Facade only: the caller supplies the one actual generation owner and current-generation gate. */
class ReminderRepository(private val generation: ManagedGeneration, private val isCurrent: () -> Boolean,
    private val publisher: ReminderPublisher, private val clock: Clock = Clock.systemUTC()) {
    suspend fun schedulingSnapshot(): ReminderDispatchInput = generation.coordinator.withSnapshot {
        check(isCurrent()) { "Generation changed" }
        inputWithinLease()
    }
    suspend fun publish(logicalKey: String, renewalId: String, revision: String): ReminderDispatchResult =
        ReminderDispatch(generation.coordinator, isCurrent, ::inputWithinLease, { candidate, at ->
            generation.database.notificationReceiptDao().insert(NotificationReceiptEntity(
                candidate.logicalKey, candidate.ownerId, "RENEWAL", candidate.boundary.toString(), at))
        }, publisher, clock::instant).execute(logicalKey, renewalId, revision)

    private suspend fun inputWithinLease(): ReminderDispatchInput {
        val zone = generation.settings.backupRecordZoneWithinSnapshot(generation.coordinator)
        val preferences = generation.settings.reminderPreferencesWithinSnapshot(generation.coordinator)
        val sources = ReportRepository(generation.database, generation.coordinator, ZonedBusinessClock(clock.withZone(zone)))
            .loadRenewalReminderSourcesWithinBoundary(generation.coordinator)
        val receipts = generation.database.notificationReceiptDao().list()
        val projectIds = receipts.filter { it.kind == "PROJECT_MILESTONE" }.map { it.ownerId }.distinct()
            .chunked(50).flatMap { generation.database.projectDao().projectsForIds(it) }.map { it.id }.toSet()
        requireReceiptOwners(receipts, projectIds, sources.map { it.ownerId }.toSet())
        return ReminderDispatchInput(sources, preferences, receipts.filter { it.kind == "RENEWAL" }.map { it.logicalKey }.toSet(), zone)
    }
}
