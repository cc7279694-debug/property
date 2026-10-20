package com.cooper.lithic.data.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One generation's writes/reads/close boundary; old queued work cannot outlive restore. */
class LocalWriteCoordinator {
    private val mutex=Mutex()
    @Volatile internal var isRetired=false
        private set
    suspend fun <T> withWrite(block:suspend()->T):T=mutex.withLock { check(!isRetired) { "Generation is retired" };block() }
    suspend fun <T> withSnapshot(block:suspend()->T):T=mutex.withLock { check(!isRetired) { "Generation is retired" };block() }
    internal suspend fun retire(verify:suspend()->Unit,dispose:suspend()->Unit)=mutex.withLock {
        check(!isRetired) { "Generation is retired" }
        verify() // Failed compare/validation leaves the old generation usable.
        isRetired=true // Fence queued work BEFORE closing any resource.
        dispose()
    }
    /** Storage-owner shutdown only, including retries after an uncertain close. */
    internal suspend fun <T> withShutdown(block:suspend()->T):T=mutex.withLock { block() }
}
