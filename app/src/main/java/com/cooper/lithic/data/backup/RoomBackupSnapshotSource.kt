package com.cooper.lithic.data.backup

import androidx.room.withTransaction

/**
 * Reads only this owned generation. Every business/settings mutation and generation close uses
 * its coordinator. Media metadata is retained here; this does not certify or export media bytes.
 */
class RoomBackupSnapshotSource internal constructor(private val generation: ManagedGeneration) : BackupSnapshotSource {
    internal suspend fun <T> captureInto(consume: suspend (BackupSnapshot) -> T): T = generation.coordinator.withSnapshot {
        consume(captureWithinOwnedBoundary(generation.coordinator))
    }

    /** Only the storage owner already holding this exact coordinator may use this retirement read. */
    internal suspend fun captureWithinOwnedBoundary(expected:com.cooper.lithic.data.repository.LocalWriteCoordinator):BackupSnapshot {
        check(expected === generation.coordinator)
        // Validate the settings owner before touching Room: a closed generation must not reopen it.
        // The coordinator remains held through all reads, so this value cannot straddle a write.
        val settings = BackupSettingsV1(
            generation.settings.backupRecordZoneWithinSnapshot(generation.coordinator).id,
        )
        val reminders = generation.settings.reminderPreferencesWithinSnapshot(generation.coordinator)
        val snapshot = generation.database.withTransaction {
            val database = generation.database
            val counts = TABLES.map { table ->
                database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { cursor ->
                    check(cursor.moveToFirst()) { "A backup table count is missing" }
                    val count = cursor.getLong(0)
                    check(!cursor.moveToNext()) { "A backup table count is ambiguous" }
                    count
                }
            }
            requireBackupRecordCounts(counts, expectedTables = 10)
            check(database.milestoneWatermarkDao().countMalformed() == 0L) { "Malformed stored milestone watermark" }
            com.cooper.lithic.data.repository.ReplacementStoredFacts.requireValid(database)
            val categories = database.categoryDao().list()
            val projects = database.projectDao().listProjects()
            val events = database.projectDao().listEvents()
            val media = database.mediaDao().list()
            val subscriptionDao = database.subscriptionDao()
            // The transaction's preflight bound proves the total is < 200000 before this read.
            val subscriptions = subscriptionDao.validationSubscriptions(null, BackupLimits.MAX_RECORDS.toInt())
            val idChunks = subscriptions.map { it.id }.chunked(IDS_PER_QUERY)
            val renewals = idChunks.flatMap { subscriptionDao.renewalsForSubscriptions(it) }
            val states = idChunks.flatMap { subscriptionDao.statesForSubscriptions(it) }
            val replacements=database.replacementDao().listReplacements()
            val receipts=database.notificationReceiptDao().list()
            val watermarks=database.milestoneWatermarkDao().list()
            check(counts == listOf(categories.size, projects.size, events.size, media.size,
                subscriptions.size, renewals.size, states.size,replacements.size,receipts.size,watermarks.size).map { it.toLong() }) {
                "A backup query omitted persisted rows"
            }
            BackupSnapshot(
                BackupDtoMapper.fromRows(categories, projects, events, media, subscriptions, renewals, states,replacements),
                settings,
                receipts.map { BackupNotificationReceiptV3(it.logicalKey, it.ownerId, it.kind, it.boundary, it.publishedAt.toString()) },
                reminders,
                watermarks.map { BackupMilestoneWatermarkV4(it.ownerType.name, it.ownerId, it.cutoffOn.toString(), it.suppressedMask) },
            )
        }
        return snapshot
    }

    override suspend fun capture(): BackupSnapshot = captureInto { it }

    private companion object {
        // Fixed names, never SQL identifiers from external input. Match the count/row order above.
        val TABLES = listOf("categories", "long_term_projects", "project_money_events", "local_media_assets",
            "subscriptions", "subscription_renewals", "subscription_state_changes","replacements","notification_receipts","milestone_watermarks")
        const val IDS_PER_QUERY = 400
    }
}
