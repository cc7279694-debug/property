package com.cooper.lithic.data.media.isolation

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.json.JSONObject

/** Compiled only. Actual cross-UID/Binder/FD evidence requires separately authorized execution.
 * Every method owns a fresh synthetic root. Nothing is deleted, including failed/unknown sessions.
 */
@RunWith(AndroidJUnit4::class)
class MediaIsolationChannelTest {
    @get:Rule val methodName = TestName()
    private lateinit var syntheticRoot: File
    private lateinit var probeContext: Context
    private var observationIndex = 0

    @Before fun requirePlainApplicationAndCreateOnlySyntheticRoot() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        check(context.packageName == "com.cooper.lithic.c6isolation.debug")
        check(InstrumentationRegistry.getInstrumentation().context.packageName ==
            "com.cooper.lithic.c6isolation.debug.test")
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
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
            putString("c6.phase", "SYNTHETIC_FIXTURE_CREATED_NOT_TEST_COMPLETION")
            putString("c6.fixtureUuid", syntheticRoot.name)
            putString("c6.method", methodName.methodName)
        })
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
        retainObservation(observed)

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
        retainObservation(observed)

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
            retainObservation(observed)
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
            retainObservation(observed)
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

    /** Separate from JUnit: PASS may still leave closure UNKNOWN. No evidence means UNKNOWN too.
     * New, bounded metadata only in this method's UUID; never list or delete another namespace.
     */
    private fun retainObservation(observed: IsolationProbeObservation) {
        check(syntheticRoot.canonicalFile == syntheticRoot.absoluteFile)
        check(File(syntheticRoot, "test-owner.txt").readText() == "C6-2C synthetic Android fixture\n")
        val s = observed.session
        val c = s.closure
        val json = JSONObject().apply {
            put("kind", "OBSERVATION_NOT_JUNIT_COMPLETION")
            put("method", methodName.methodName)
            put("fixtureUuid", syntheticRoot.name)
            put("sessionNonce", s.expectedNonce)
            put("api", Build.VERSION.SDK_INT)
            put("abis", Build.SUPPORTED_ABIS.joinToString(","))
            put("hostUid", Process.myUid()); put("hostPid", Process.myPid())
            put("workerUid", observed.workerUid ?: JSONObject.NULL)
            put("workerPid", observed.workerPid ?: JSONObject.NULL)
            put("workerRole", observed.workerRole ?: JSONObject.NULL)
            put("workerOwnerInitialized", observed.workerOwnerInitialized ?: JSONObject.NULL)
            put("workerCanReadHostDirectory", observed.workerCanReadHostDirectory ?: JSONObject.NULL)
            put("inputWriteRejected", observed.inputWriteRejected ?: JSONObject.NULL)
            put("guardHeldConfirmed", observed.guardHeldConfirmed)
            put("outputMatches", observed.outputMatches)
            put("journalPersisted", observed.journalPersisted)
            put("connectionCount", observed.connectionCount)
            put("phase", s.phase.name); put("outcome", s.outcome.name)
            put("acceptedResult", s.acceptedResult); put("revoked", s.revoked)
            put("detail", observed.detail)
            put("binderDead", c.binderDead); put("guardReturned", c.guardReturned)
            put("workerReportedInputClosed", c.workerReportedInputClosed)
            put("workerReportedOutputClosed", c.workerReportedOutputClosed)
            put("hostFdsClosed", c.hostFdsClosed); put("rpcQuiescent", c.rpcQuiescent)
        }.toString().toByteArray(Charsets.UTF_8)
        check(json.size <= 16 * 1024)
        val file = File(syntheticRoot, "observation-${++observationIndex}.json")
        check(file.canonicalFile.parentFile == syntheticRoot.canonicalFile && file.createNewFile())
        FileOutputStream(file).use { it.write(json); it.fd.sync() }
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
            putString("c6.observation", json.toString(Charsets.UTF_8))
            putString("c6.observationFile", "c6-isolation-android-tests/${syntheticRoot.name}/${file.name}")
        })
    }
}
