package com.cooper.lithic.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Only result delivery/cleanup are shielded; real IO explicitly checks the original caller. */
internal suspend fun <T> withBackupIo(block: suspend (checkpoint: () -> Unit) -> T): T {
    val caller = currentCoroutineContext()
    return withContext(NonCancellable) {
        withContext(Dispatchers.IO) { block { caller.ensureActive() } }
    }
}
