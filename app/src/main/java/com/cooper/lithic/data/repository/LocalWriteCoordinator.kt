package com.cooper.lithic.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Shared by Room writes, settings, media changes, and a future backup snapshot. */
class LocalWriteCoordinator {
    private val mutex = Mutex()

    // The mutex is intentionally non-reentrant; a caller must acquire each boundary once.
    suspend fun <T> withWrite(block: suspend () -> T): T = mutex.withLock { block() }
    suspend fun <T> withSnapshot(block: suspend () -> T): T = mutex.withLock { block() }
}
