package com.cooper.lithic.data.backup

/** A detached logical snapshot, never a copy of a live SQLite database file. */
fun interface BackupSnapshotSource {
    suspend fun capture(): BackupSnapshot
}
