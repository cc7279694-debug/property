package com.cooper.lithic.data.media

import android.content.Context
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.data.media.jpegstage.JpegStageV2
import com.cooper.lithic.data.media.jpegworker.JpegProcessRole
import com.cooper.lithic.data.media.jpegworker.JpegWorkerClient
import com.cooper.lithic.data.media.jpegworker.JpegWorkerIdentity
import com.cooper.lithic.data.media.jpegworker.JpegWorkerOutcome
import com.cooper.lithic.data.media.jpegworker.JpegWorkerRequest
import com.cooper.lithic.data.media.jpegworker.JpegWorkerResult
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Platform admission tests, not host simulations. They are compile-only until a new isolated
 * device is explicitly authorized. Run one method at a time and stop on any failure/unknown.
 * There is intentionally no recursive teardown: an unproved consumer keeps all of its stage.
 * Even passing cases retain the terminal tombstone; only the production safeCleanup removes
 * the proven-owned payload while holding the production guard lease.
 */
@RunWith(AndroidJUnit4::class)
class M10WorkerRetirementReviewTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun healthy_jpeg_retires_real_worker_before_stage_completion() = runBlocking {
        runOne(M10JpegFixtures.COMPLETE, JpegWorkerOutcome.SUCCESS)
    }

    @Test fun locked_damaged_jpeg_is_rejected_and_real_worker_safely_retires() = runBlocking {
        runOne(M10JpegFixtures.BROKEN, JpegWorkerOutcome.INVALID_IMAGE)
    }

    private suspend fun runOne(fixture: String, expected: JpegWorkerOutcome) {
        val root = File(context.noBackupFilesDir, "m10-worker-review-${uuid()}")
        check(root.mkdir() && File(root, "media").mkdir())
        check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
        val files = MediaFileProtocol(root)
        val owner = JpegStageV2.Owner(root.name, uuid(), uuid(), uuid(), uuid())
        val initial = files.jpeg.create(owner, M10JpegFixtures.bytes(fixture).inputStream())
        val source = files.jpeg.sourceFile(owner)
        val output = files.jpeg.outputFile(owner)
        val guard = files.jpeg.guardFile(owner)
        val guardIdentity = files.identity(guard)
        val host = JpegProcessRole.identity()
        assertEquals(Process.myPid(), host.pid)
        assertEquals(Process.myUid(), host.uid)
        assertEquals(JpegProcessRole.Role.MAIN, JpegProcessRole.role(context.packageName))
        val request = JpegWorkerRequest(owner.generation, owner.assetId, owner.attemptId,
            owner.sessionId, owner.nonce, initial.source.bytes, initial.source.sha256,
            initial.source.device, initial.source.inode, guardIdentity.device, guardIdentity.inode)
        val pinConfirmed = AtomicBoolean(false)
        val registeredWorker = AtomicReference<JpegWorkerIdentity?>()
        val result = JpegWorkerClient(context).execute(source, output, guard, request,
            beforeDelivery = { worker ->
                check(registeredWorker.compareAndSet(null, worker)) { "Duplicate consumer registration" }
                check(worker.pid != host.pid && worker.uid == host.uid && worker.startTicks > 0)
                val pin = files.jpeg.registerConsumer(owner, worker.stageIdentity())
                check(pin.pinned && pin.source == initial.source && pin.worker == worker.stageIdentity())
                val delivered = files.jpeg.markDelivery(owner)
                check(delivered.pinned && delivered.state == JpegStageV2.State.DELIVERY_UNKNOWN)
                pinConfirmed.set(true) // Client cannot deliver data FDs until this callback returns.
            },
            revoke = { reason -> files.jpeg.revoke(owner, reason) },
            abandonedResult = { abandoned ->
                observe("worker-review-abandoned", owner, abandoned, pinConfirmed.get(), false)
                // Abandonment is not retirement or cleanup proof. Preserve the complete stage.
            })
        observe("worker-review-result", owner, result, pinConfirmed.get(), false)
        assertTrue("Persistent pin must precede delivery", pinConfirmed.get())
        assertEquals(expected, result.outcome)
        val worker = requireNotNull(result.workerIdentity)
        assertEquals(registeredWorker.get(), worker)
        assertNotEquals(host.pid, worker.pid)
        assertEquals(host.uid, worker.uid)
        assertTrue(worker.startTicks > 0)
        assertEquals("Exactly one matching structured result", 1, result.resultCount)
        assertTrue("Worker source FD close must be proved", result.sourceFdClosed)
        assertTrue("Worker output FD close must be proved", result.outputFdClosed)
        assertTrue("Connection responsibility must be discharged", result.connectionCleaned)
        assertTrue("A RETIRE request or unbind alone is insufficient", result.binderDead)
        assertTrue("Matching OS process instance must have ended", result.matchingProcessDead)
        assertTrue("Matching consumer guard must be released", result.guardReleased)
        assertTrue(result.consumerEnded)
        assertTrue(result.mayReleaseSupervisor)
        assertTrue("Native decoder destruction is a separate requirement", result.decoderDestroyed)
        assertEquals(host, JpegProcessRole.identity())
        assertEquals(initial.source, files.identity(source))
        val outputIdentity = if (expected == JpegWorkerOutcome.SUCCESS) files.identity(output) else null
        if (expected == JpegWorkerOutcome.SUCCESS) {
            assertTrue(result.strictFinished)
            assertTrue(result.canPrepare)
            assertEquals(256, result.width)
            assertEquals(256, result.height)
            assertEquals(requireNotNull(outputIdentity).bytes, result.outputSize)
            assertEquals(outputIdentity.sha256, result.outputSha256)
        } else {
            assertFalse(result.strictFinished)
            assertFalse(result.canPrepare)
        }
        val completion = JpegStageV2.Completion(owner, worker.stageIdentity(),
            if (expected == JpegWorkerOutcome.SUCCESS) JpegStageV2.Outcome.SUCCESS else JpegStageV2.Outcome.IMAGE,
            initial.source, outputIdentity,
            width = if (expected == JpegWorkerOutcome.SUCCESS) result.width else null,
            height = if (expected == JpegWorkerOutcome.SUCCESS) result.height else null,
            resultCount = result.resultCount, sourceFdClosed = result.sourceFdClosed,
            outputFdClosed = result.outputFdClosed, binderDead = result.binderDead,
            matchingProcessDead = result.matchingProcessDead, strictFinished = result.strictFinished,
            decoderDestroyed = result.decoderDestroyed, connectionCleaned = result.connectionCleaned)
        val terminal = files.jpeg.complete(completion) // Rechecks identities under real exclusive lease.
        assertFalse(terminal.pinned)
        assertEquals(completion.outcome, terminal.outcome)
        if (expected == JpegWorkerOutcome.SUCCESS) {
            val prepared = files.jpeg.recordPrepared(owner)
            assertEquals(outputIdentity, prepared.output)
            assertEquals(owner, prepared.owner)
        }
        files.jpeg.safeCleanup(owner)
        assertFalse(source.exists())
        assertFalse(output.exists())
        assertEquals(JpegStageV2.State.TERMINAL, files.jpeg.snapshot(owner).state)
        files.jpeg.admission()
        observe("worker-review-terminal", owner, result, true, true)
        // No Room owner, media row, final cover or business operation is created by this test.
    }

    private fun JpegWorkerIdentity.stageIdentity() = JpegStageV2.WorkerIdentity(pid, uid, startTicks)

    private fun observe(name: String, owner: JpegStageV2.Owner, result: JpegWorkerResult,
        pinBeforeDelivery: Boolean, safeCleanup: Boolean) {
        M10JpegObservation.record(name, JSONObject().put("generation", owner.generation)
            .put("attempt", owner.attemptId).put("session", owner.sessionId)
            .put("outcome", result.outcome.name).put("hostPid", Process.myPid()).put("hostUid", Process.myUid())
            .put("workerPid", result.workerIdentity?.pid).put("workerUid", result.workerIdentity?.uid)
            .put("workerStartTicks", result.workerIdentity?.startTicks).put("resultCount", result.resultCount)
            .put("pinBeforeDelivery", pinBeforeDelivery).put("sourceClosed", result.sourceFdClosed)
            .put("outputClosed", result.outputFdClosed).put("connectionCleaned", result.connectionCleaned)
            .put("binderDead", result.binderDead).put("matchingProcessDead", result.matchingProcessDead)
            .put("guardReleased", result.guardReleased).put("strictFinished", result.strictFinished)
            .put("decoderDestroyed", result.decoderDestroyed).put("safeCleanup", safeCleanup)
            .put("detail", result.detail))
    }

    private fun uuid() = UUID.randomUUID().toString()
}
