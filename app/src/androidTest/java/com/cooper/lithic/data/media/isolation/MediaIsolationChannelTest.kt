package com.cooper.lithic.data.media.isolation

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Compiled only. Actual cross-UID/Binder/FD evidence requires separately authorized execution.
 * Every method owns a fresh synthetic root. Nothing is deleted, including failed/unknown sessions.
 */
@RunWith(AndroidJUnit4::class)
class MediaIsolationChannelTest {
    private lateinit var syntheticRoot: File
    private lateinit var probeContext: Context

    @Before fun requirePlainApplicationAndCreateOnlySyntheticRoot() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.applicationContext.javaClass == Application::class.java)
        check(InstrumentationRegistry.getInstrumentation().javaClass.name ==
            "com.cooper.lithic.app.M13IsolatedRunner")
        check(context.applicationInfo.uid == Process.myUid())
        val parent = File(context.noBackupFilesDir.canonicalFile, "c6-isolation-android-tests")
        check(parent.canonicalFile == parent.absoluteFile)
        check(parent.mkdirs() || parent.isDirectory)
        syntheticRoot = File(parent, UUID.randomUUID().toString())
        check(syntheticRoot.mkdir())
        File(syntheticRoot, "test-owner.txt").writeText("C6-2C synthetic Android fixture\n")
        // All Host stage/journal writes stay in this method's new UUID directory.
        // Package identity and actual Service binding continue to use the real target Context.
        probeContext = object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
            override fun getNoBackupFilesDir(): File = syntheticRoot
        }
    }

    @Test fun echo_runs_under_independent_uid_with_read_only_input_and_separate_closure_evidence() = runBlocking {
        val channel = MediaIsolationChannel(probeContext)
        val observed = withTimeout(30_000) { channel.runSyntheticProbe() }

        assertEquals(IsolationOutcome.PROBE_CLOSED, observed.session.outcome)
        assertEquals(IsolationPhase.CLOSED, observed.session.phase)
        assertFalse(observed.session.revoked)
        assertTrue(observed.session.acceptedResult)
        assertNotNull(observed.workerPid)
        assertNotNull(observed.workerUid)
        assertTrue(requireNotNull(observed.workerUid) != Process.myUid())
        assertEquals(observed.workerUid, observed.session.workerUid)
        assertEquals("SANDBOX", observed.workerRole)
        assertEquals(false, observed.workerOwnerInitialized)
        assertEquals(false, observed.workerCanReadHostDirectory)
        assertEquals(true, observed.inputWriteRejected)
        assertTrue(observed.guardHeldConfirmed)
        assertTrue(observed.outputMatches)
        assertTrue(observed.journalPersisted)
        assertEquals(1, observed.connectionCount)

        // Each is a separate observation; one true flag is never sufficient proof for the others.
        assertTrue(observed.session.closure.binderDead)
        assertTrue(observed.session.closure.guardReturned)
        assertTrue(observed.session.closure.workerReportedInputClosed)
        assertTrue(observed.session.closure.workerReportedOutputClosed)
        assertTrue(observed.session.closure.hostFdsClosed)
        assertTrue(observed.session.closure.rpcQuiescent)
        assertOwnRootAndNoBusinessGeneration(observed)
    }

    @Test fun nonreturning_synthetic_compute_times_out_without_promoting_late_success() = runBlocking {
        val channel = MediaIsolationChannel(probeContext)
        val observed = withTimeout(30_000) {
            channel.runSyntheticProbe(IsolationProbeMode.HOLD_UNTIL_RETIRE, timeoutMillis = 2_000)
        }

        assertTrue(observed.session.revoked)
        assertFalse(observed.session.acceptedResult)
        assertNotEquals(IsolationOutcome.PROBE_CLOSED, observed.session.outcome)
        assertFalse(observed.outputMatches)
        assertTrue(observed.filesDirectory.exists())
        assertEquals(1, observed.connectionCount)
        assertOwnRootAndNoBusinessGeneration(observed)
        // An exit/FD flag can remain UNKNOWN; timeout alone never asserts successful closure.
    }

    @Test fun cancelling_after_computing_keeps_revocation_and_preserves_the_session_files() = runBlocking {
        val channel = MediaIsolationChannel(probeContext)
        val computing = CompletableDeferred<Unit>()
        val running = launch(Dispatchers.Default) {
            channel.runSyntheticProbe(IsolationProbeMode.HOLD_UNTIL_RETIRE, timeoutMillis = 10_000,
                onComputing = { computing.complete(Unit) })
        }
        try {
            withTimeout(15_000) { computing.await() }
            withTimeout(20_000) { running.cancelAndJoin() }
            val observed = requireNotNull(channel.lastObservation)
            assertTrue(observed.session.revoked)
            assertFalse(observed.session.acceptedResult)
            assertNotEquals(IsolationOutcome.PROBE_CLOSED, observed.session.outcome)
            assertTrue(observed.filesDirectory.exists())
            assertEquals(1, observed.connectionCount)
            assertOwnRootAndNoBusinessGeneration(observed)
        } finally {
            withContext(NonCancellable) { withTimeout(20_000) { running.cancelAndJoin() } }
        }
    }

    @Test fun admitted_record_after_host_state_loss_blocks_same_and_recreated_channel_without_binding() = runBlocking {
        val stage = File(syntheticRoot, "c6-isolation-stage1")
        check(stage.mkdir())
        val oldSessionId = UUID.randomUUID().toString()
        val oldSession = File(stage, oldSessionId)
        check(oldSession.mkdir())
        val syntheticInput = File(oldSession, "input.bin").apply { writeText("retained synthetic input") }
        val journal = File(stage, "session.txt")
        val admitted = "C6_ISOLATION_1\n$oldSessionId\nADMITTED\n"
        journal.writeText(admitted)
        val channel = MediaIsolationChannel(probeContext)

        for (candidate in listOf(channel, channel, MediaIsolationChannel(probeContext))) {
            val observed = withTimeout(10_000) { candidate.runSyntheticProbe() }
            assertEquals(IsolationOutcome.UNKNOWN, observed.session.outcome)
            assertEquals("ADMISSION_UNKNOWN", observed.detail)
            assertEquals(0, observed.connectionCount)
            assertNull(observed.workerPid)
            assertNull(observed.workerUid)
            assertFalse(observed.session.acceptedResult)
            assertEquals(admitted, journal.readText())
            assertEquals("retained synthetic input", syntheticInput.readText())
            assertOwnRootAndNoBusinessGeneration(observed)
        }
    }

    private fun assertOwnRootAndNoBusinessGeneration(observed: IsolationProbeObservation) {
        assertTrue(observed.filesDirectory.canonicalFile.toPath().startsWith(syntheticRoot.canonicalFile.toPath()))
        assertFalse(File(syntheticRoot, "active-generation").exists())
        assertFalse(File(syntheticRoot, "generations").exists())
        assertTrue(File(syntheticRoot, "test-owner.txt").isFile)
    }
}
