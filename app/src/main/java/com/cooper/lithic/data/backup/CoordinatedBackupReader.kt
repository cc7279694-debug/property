package com.cooper.lithic.data.backup

import com.cooper.lithic.data.repository.LocalWriteCoordinator

/** Keeps one coordinator acquisition across the production Room transaction and settings read. */
internal class CoordinatedBackupReader<T>(
    private val coordinator: LocalWriteCoordinator,
    private val readWithinSnapshot: suspend () -> T,
) {
    suspend fun capture(): T = coordinator.withSnapshot { readWithinSnapshot() }
}

/** Counts are read before materializing rows; reaching the frozen limit is an explicit failure. */
internal fun requireBackupRecordCounts(counts: List<Long>): Long {
    require(counts.size == 8) { "A current backup must account for all eight Room v3 tables" }
    var total = 0L
    counts.forEach { count ->
        require(count >= 0) { "Backup record counts cannot be negative" }
        total = Math.addExact(total, count)
    }
    require(total < BackupLimits.MAX_RECORDS) { "The backup reaches the record limit" }
    return total
}
