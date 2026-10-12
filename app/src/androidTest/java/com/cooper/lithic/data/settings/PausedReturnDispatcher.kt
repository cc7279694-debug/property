package com.cooper.lithic.data.settings

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job

/** Holds caller continuations while real IO and storage initialization run on their own dispatchers. */
internal class PausedReturnDispatcher : CoroutineDispatcher() {
    private val queued = LinkedBlockingQueue<Runnable>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queued.put(block)
    }

    fun awaitTask(): Runnable = checkNotNull(queued.poll(10, TimeUnit.SECONDS)) {
        "Expected the real IO call to return to its caller dispatcher"
    }

    fun finishCancelled(job: Job, returnTask: Runnable) {
        job.cancel()
        returnTask.run()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!job.isCompleted && System.nanoTime() < deadline) {
            queued.poll(100, TimeUnit.MILLISECONDS)?.run()
        }
        check(job.isCompleted) { "Cancelled acquisition did not finish cleanup" }
    }
}
