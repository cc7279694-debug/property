package com.cooper.lithic.platform.notification

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.cooper.lithic.app.M13IsolatedRunner
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * NOT_RUN. Requires a future explicitly authorized, disposable corebootqa package and
 * M13 plain Application. The package does not exist in the current build configuration.
 * No Preview, user ledger, Android system setting, or existing identity may be used.
 */
@RunWith(AndroidJUnit4::class)
class CoreReminderCancellationIsolationTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun requireDisposablePlainApplication() {
        assertEquals("com.cooper.lithic.corebootqa.debug", context.packageName)
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        assertEquals(M13IsolatedRunner::class.java, InstrumentationRegistry.getInstrumentation().javaClass)
    }

    private fun workManager(): WorkManager = synchronized(initializationLock) {
        if (!initialized) {
            val configuration = Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .setTaskExecutor(SynchronousExecutor())
                .build()
            WorkManagerTestInitHelper.initializeTestWorkManager(context, configuration)
            initialized = true
        }
        WorkManager.getInstance(context)
    }

    private fun delayedWork() = OneTimeWorkRequestBuilder<NeverRunWorker>()
        .setInitialDelay(1, TimeUnit.DAYS).build()

    private fun awaitState(manager: WorkManager, id: UUID, expected: WorkInfo.State): WorkInfo.State {
        val deadline = SystemClock.elapsedRealtime() + 5_000
        var state: WorkInfo.State? = null
        while (SystemClock.elapsedRealtime() < deadline) {
            state = manager.getWorkInfoById(id).get(2, TimeUnit.SECONDS)?.state
            if (state == expected) return state
            Thread.sleep(25)
        }
        throw AssertionError("Expected $expected; observed $state")
    }

    @Test fun reminder_cancellation_does_not_cancel_an_independent_unrelated_task() {
        val manager = workManager()
        val legacy = delayedWork()
        val unrelated = delayedWork()
        manager.enqueueUniqueWork("lithic-renewal-reconcile-v1", ExistingWorkPolicy.KEEP, legacy)
            .result.get(5, TimeUnit.SECONDS)
        manager.enqueueUniqueWork("coreboot-independent-${UUID.randomUUID()}", ExistingWorkPolicy.KEEP, unrelated)
            .result.get(5, TimeUnit.SECONDS)
        ReminderLegacyCancellation.start(context)
        assertEquals(WorkInfo.State.CANCELLED, awaitState(manager, legacy.id, WorkInfo.State.CANCELLED))
        val unrelatedState = requireNotNull(manager.getWorkInfoById(unrelated.id).get(5, TimeUnit.SECONDS)).state
        assertNotEquals(WorkInfo.State.CANCELLED, unrelatedState)
        assertTrue(!unrelatedState.isFinished)
    }

    @Test fun workmanager_cancellation_also_cancels_an_unrelated_dependent_task() {
        val manager = workManager()
        val root = delayedWork()
        val unrelatedDependent = delayedWork()
        val syntheticName = "coreboot-cascade-${UUID.randomUUID()}"
        manager.beginUniqueWork(syntheticName, ExistingWorkPolicy.KEEP, root)
            .then(unrelatedDependent).enqueue().result.get(5, TimeUnit.SECONDS)
        manager.cancelUniqueWork(syntheticName).result.get(5, TimeUnit.SECONDS)
        assertEquals(WorkInfo.State.CANCELLED, awaitState(manager, root.id, WorkInfo.State.CANCELLED))
        // This records platform behavior, not permission to cancel unrelated work.
        assertEquals(WorkInfo.State.CANCELLED,
            awaitState(manager, unrelatedDependent.id, WorkInfo.State.CANCELLED))
    }

    companion object {
        private val initializationLock = Any()
        private var initialized = false
    }
}

/** Test-only class; delayed tasks are never deliberately executed. */
class NeverRunWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result = error("Synthetic delayed work must not run")
}
