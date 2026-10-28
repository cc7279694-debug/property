package com.cooper.lithic.feature.media

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CoverPreviewPipelineTest {
    @Test fun cancellation_keeps_the_process_slot_until_native_like_work_has_actually_returned() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val secondEntered = CountDownLatch(1)
        val secondAttempted = CompletableDeferred<Unit>()
        val first = launch(Dispatchers.Default) {
            CoverPreviewPipeline.serialized {
                withContext(Dispatchers.IO) {
                    entered.complete(Unit)
                    check(release.await(5, TimeUnit.SECONDS))
                }
            }
        }
        var second: Job? = null
        try {
            withTimeout(5_000) { entered.await() }
            first.cancel()
            second = launch(Dispatchers.Default) {
                secondAttempted.complete(Unit)
                CoverPreviewPipeline.serialized { secondEntered.countDown() }
            }
            withTimeout(5_000) { secondAttempted.await() }
            // No fake timeout/async can release the slot while the original IO body still owns it.
            assertFalse(secondEntered.await(100, TimeUnit.MILLISECONDS))
            assertFalse(first.isCompleted)
        } finally {
            release.countDown()
            withTimeout(5_000) { first.join(); second?.join() }
        }
        assertTrue(secondEntered.await(1, TimeUnit.SECONDS))
    }

    @Test fun cancelling_a_queued_caller_never_releases_another_callers_slot() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val cancelledEntered = CompletableDeferred<Unit>()
        val holder = launch { CoverPreviewPipeline.serialized { entered.complete(Unit); release.await() } }
        var waiter: Job? = null
        try {
            entered.await()
            waiter = launch { CoverPreviewPipeline.serialized { cancelledEntered.complete(Unit) } }
            yield()
            waiter.cancelAndJoin()
            assertFalse(cancelledEntered.isCompleted)
        } finally { release.complete(Unit); holder.join(); waiter?.join() }
        var thirdEntered = false
        CoverPreviewPipeline.serialized { thirdEntered = true }
        assertTrue(thirdEntered)
    }
}
