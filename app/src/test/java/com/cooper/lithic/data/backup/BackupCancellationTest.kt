package com.cooper.lithic.data.backup

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class BackupCancellationTest {
    @Test fun verified_result_is_not_lost_at_dispatcher_return_after_late_cancel() = runBlocking {
        var observed: String? = null
        val operation = launch {
            val caller = currentCoroutineContext()[Job]!!
            observed = withBackupIo { checkpoint ->
                checkpoint()
                // A fully verified result wins this linearization point; a later cancel cannot
                // discard it and make an outer fallback delete the completed target.
                caller.cancel()
                "VERIFIED"
            }
        }
        operation.join()
        assertEquals("VERIFIED", observed)
    }
    @Test fun actual_io_checkpoint_still_observes_cancellation_and_delivers_cleanup_outcome() = runBlocking {
        var observed: String? = null
        var wrote = false
        val operation = launch {
            val caller = currentCoroutineContext()[Job]!!
            observed = withBackupIo { checkpoint ->
                caller.cancel()
                try { checkpoint(); wrote = true; "BAD" }
                catch (_: CancellationException) { "CANCELLED_AND_CLEANED" }
            }
        }
        operation.join()
        assertFalse(wrote)
        assertEquals("CANCELLED_AND_CLEANED", observed)
    }
}
