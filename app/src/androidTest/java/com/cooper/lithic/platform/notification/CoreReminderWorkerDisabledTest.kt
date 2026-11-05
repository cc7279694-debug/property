package com.cooper.lithic.platform.notification

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.app.M13IsolatedRunner
import com.cooper.lithic.app.StorageInitializationState
import com.cooper.lithic.data.repository.MilestoneSummary
import com.cooper.lithic.domain.reminder.RenewalReminderCandidate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** NOT_RUN until separately authorized in an isolated M13 plain-Application test host. */
@RunWith(AndroidJUnit4::class)
class CoreReminderWorkerDisabledTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var owner: AppContainer
    private var publisherCalls = 0

    private val renewalPublisher = object : ReminderPublisher {
        override fun canPublish(): Boolean = unexpectedPublish()
        override fun publish(candidate: RenewalReminderCandidate): Boolean = unexpectedPublish()
    }
    private val milestonePublisher = object : MilestonePublisher {
        override fun canPublish(): Boolean = unexpectedPublish()
        override fun publish(summary: MilestoneSummary): Boolean = unexpectedPublish()
    }

    private fun unexpectedPublish(): Boolean {
        publisherCalls++
        error("Disabled reminder worker must not inspect or publish notifications")
    }

    @Before fun requirePlainApplicationAndUnstartedOwner() {
        assertEquals(M13IsolatedRunner::class.java, InstrumentationRegistry.getInstrumentation().javaClass)
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        owner = AppContainer(context) // no start(), Room, generation, WorkManager, or user ledger
        assertSame(StorageInitializationState.Initializing, owner.storageState.value)
    }

    @After fun closeUnusedOwner() = runBlocking {
        if (::owner.isInitialized) owner.close()
    }

    private fun worker(mode: String): ReminderWorker = TestListenableWorkerBuilder<ReminderWorker>(context)
        .setInputData(workDataOf(ReminderWorker.MODE to mode))
        .setWorkerFactory(object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? =
                if (workerClassName == ReminderWorker::class.java.name)
                    ReminderWorker(appContext, workerParameters, owner, renewalPublisher,
                        milestonePublisher = milestonePublisher)
                else null
        }).build()

    private fun assertDisabled(mode: String) = runBlocking {
        // An openStorage() call on this unstarted owner throws; the real worker's exception
        // path returns retry. Success therefore checks that the capability gate ran first.
        assertEquals(ListenableWorker.Result.success(), worker(mode).doWork())
        assertSame(StorageInitializationState.Initializing, owner.storageState.value)
        assertEquals(0, publisherCalls)
    }

    @Test fun old_reconcile_exits_before_opening_owner() = assertDisabled(ReminderWorker.RECONCILE)
    @Test fun old_milestone_reconcile_exits_before_opening_owner() = assertDisabled(ReminderWorker.MILESTONE_RECONCILE)
    @Test fun old_publish_exits_before_opening_owner() = assertDisabled(ReminderWorker.PUBLISH)
    @Test fun old_milestone_publish_exits_before_opening_owner() = assertDisabled(ReminderWorker.MILESTONE_PUBLISH)
}
