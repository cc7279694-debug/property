package com.cooper.lithic.data.media

import android.content.Context
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.ErrnoException
import android.system.OsConstants
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.data.media.jpegstage.JpegStageV2
import com.cooper.lithic.data.media.jpegworker.JpegBootIdentity
import com.cooper.lithic.data.media.jpegworker.JpegProcessRole
import com.cooper.lithic.data.media.jpegworker.JpegResourceEvidence
import com.cooper.lithic.data.media.jpegworker.JpegWorkerClient
import com.cooper.lithic.data.media.jpegworker.JpegWorkerIdentity
import com.cooper.lithic.data.media.jpegworker.JpegWorkerOutcome
import com.cooper.lithic.data.media.jpegworker.JpegWorkerRequest
import com.cooper.lithic.data.media.jpegworker.JpegWorkerResult
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.LinkOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Three new natural resource methods, NOT_RUN until separately authorized on a dedicated device.
 * Inputs are pre-authorized synthetic files, never bundled assets or user photos. No fault,
 * cancellation, Room commit, recursive teardown, unknown cleanup or process stop is offered.
 */
@RunWith(AndroidJUnit4::class)
class M10JpegResourceEvidenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun baseline_420_40mp_worker_resource_evidence() = runBlocking { runOne("large_40mp_baseline_420.jpg") }
    @Test fun progressive_420_40mp_worker_resource_evidence() = runBlocking { runOne("large_40mp_progressive_420.jpg") }
    @Test fun progressive_444_40mp_worker_resource_evidence() = runBlocking { runOne("valid_40mp_progressive_444.jpg") }

    private suspend fun runOne(name: String) {
        val host = JpegProcessRole.identity()
        check(host.pid == Process.myPid() && host.uid == Process.myUid() && JpegProcessRole.role(context.packageName) == JpegProcessRole.Role.MAIN)
        val boot = JpegBootIdentity.current(context)
        val owner = JpegStageV2.Owner("m10-resource-v2-${uuid()}", uuid(), uuid(), uuid(), uuid())
        val base = File(context.noBackupFilesDir.canonicalFile, "m10-b2-resource-diagnostics")
        privateDirectory(base, create = true)
        // Even a whole-class invocation cannot bypass a prior UNKNOWN attempt.
        for (old in requireNotNull(base.listFiles())) {
            check(old.name.matches(Regex(UUID_PATTERN)))
            privateDirectory(old, create = false)
            val result = readJson(File(old, "test-result.json"), 2 * 1024 * 1024)
            check(result.getString("status") == "MEASURED" && result.getBoolean("resourceEnded")) { "Prior B2 attempt is not conclusively terminal" }
        }
        val directory = File(base, owner.attemptId)
        check(directory.mkdir()); Os.chmod(directory.path, 0x1c0)
        val report = JSONObject().put("protocol", "M10_B2_RESOURCE_TEST_V1").put("status", "UNKNOWN")
            .put("fixture", name).put("generation", owner.generation).put("asset", owner.assetId)
            .put("attempt", owner.attemptId).put("session", owner.sessionId).put("nonce", owner.nonce)
            .put("bootId", boot).put("hostPid", host.pid).put("hostUid", host.uid).put("hostStartTicks", host.startTicks)
            .put("resourceEnded", false).put("roomOrFormalCoverCommitted", false)
            .put("operationOutcome", "NOT_RUN").put("closureStatus", "NOT_PROVEN").put("measurementStatus", "UNKNOWN")
        var sampler: M10ResourceSampler? = null
        var sampled = false
        var inputClosed = false
        val wallStart = SystemClock.elapsedRealtimeNanos()
        val cpuStart = Process.getElapsedCpuTime()
        try {
            val source = fixedInput(name)
            val expected = JpegResourceEvidence.fixture(name)
            report.put("sourceBytes", expected[0].toLong()).put("sourceSha256", expected[1])
            val permit = linkedMapOf("version" to "1", "generation" to owner.generation, "assetId" to owner.assetId,
                "attemptId" to owner.attemptId, "sessionId" to owner.sessionId, "nonce" to owner.nonce,
                "sourceBytes" to expected[0], "sourceSha" to expected[1], "fixtureName" to name,
                "hostPid" to host.pid.toString(), "hostUid" to host.uid.toString(), "hostStartTicks" to host.startTicks.toString(), "bootId" to boot)
            val marker = permit.entries.joinToString("\n", postfix = "\n") { "${it.key}=${it.value}" }
            JpegResourceEvidence.match(JpegResourceEvidence.permit(marker), permit)
            write(File(directory, "allow.properties"), marker.toByteArray(Charsets.UTF_8))
            val root = File(context.noBackupFilesDir.canonicalFile, owner.generation)
            check(root.mkdir() && File(root, "media").mkdir())
            Os.chmod(root.path, 0x1c0); Os.chmod(File(root, "media").path, 0x1c0)
            val files = MediaFileProtocol(root)
            val activeSampler = M10ResourceSampler(context, host, boot).also { it.start() }
            sampler = activeSampler
            val sourceChannel = FileChannel.open(source.toPath(), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)
            val stream = Channels.newInputStream(sourceChannel)
            val supervisor = JpegStageV2.SupervisorIdentity(boot, host.pid, host.uid, host.startTicks)
            val initial = try { stream.use { files.jpeg.create(owner, it, supervisor) } } finally {
                // InputStream.close completed or the enclosing test cannot claim success.
                sourceChannel.close(); inputClosed = !sourceChannel.isOpen
            }
            check(initial.source.bytes == expected[0].toLong() && initial.source.sha256 == expected[1])
            val stagedSource = files.jpeg.sourceFile(owner)
            val output = files.jpeg.outputFile(owner)
            val guard = files.jpeg.guardFile(owner)
            val guardIdentity = files.identity(guard)
            val request = JpegWorkerRequest(owner.generation, owner.assetId, owner.attemptId, owner.sessionId,
                owner.nonce, initial.source.bytes, initial.source.sha256, initial.source.device, initial.source.inode,
                guardIdentity.device, guardIdentity.inode)
            val registered = AtomicReference<JpegWorkerIdentity?>()
            val result = JpegWorkerClient(context).execute(stagedSource, output, guard, request,
                beforeDelivery = { worker ->
                    check(registered.compareAndSet(null, worker) && worker.pid != host.pid && worker.uid == host.uid)
                    val pin = files.jpeg.registerConsumer(owner, worker.stage())
                    check(pin.pinned && pin.source == initial.source)
                    files.jpeg.markDelivery(owner)
                    activeSampler.attach(worker)
                }, revoke = { reason -> files.jpeg.revoke(owner, reason) },
                abandonedResult = { /* A late result has no cleanup authority here; retain the Stage for independent review. */ })
            report.put("workerResult", resultObservation(result)).put("operationOutcome", result.outcome.name)
            if (result.outcome != JpegWorkerOutcome.SUCCESS) {
                closeStructuredFailure(files, owner, initial.source, stagedSource, output, registered.get(), result, report)
            }
            // A safely closed resource rejection is still a failed 40MP resource experiment.
            assertEquals(JpegWorkerOutcome.SUCCESS, result.outcome)
            assertEquals(registered.get(), result.workerIdentity)
            assertEquals(1, result.resultCount)
            assertTrue(result.sourceFdClosed && result.outputFdClosed && result.decoderDestroyed && result.strictFinished)
            assertTrue(result.binderDead && result.matchingProcessDead && result.guardReleased && result.connectionCleaned)
            assertTrue(result.consumerEnded && result.canPrepare && result.mayReleaseSupervisor)
            assertTrue(result.width in 1..1600 && result.height in 1..1600 && result.outputSize in 1..2_097_152)
            val out = files.identity(output)
            assertEquals(result.outputSha256, out.sha256); assertEquals(result.outputSize, out.bytes)
            val worker = requireNotNull(result.workerIdentity)
            val completion = JpegStageV2.Completion(owner, worker.stage(), JpegStageV2.Outcome.SUCCESS,
                initial.source, out, result.width, result.height, result.resultCount,
                result.sourceFdClosed, result.outputFdClosed, result.binderDead, result.matchingProcessDead,
                result.strictFinished, result.decoderDestroyed, result.connectionCleaned)
            val terminal = files.jpeg.complete(completion)
            check(!terminal.pinned && terminal.outcome == JpegStageV2.Outcome.SUCCESS)
            files.jpeg.recordPrepared(owner)
            files.jpeg.safeCleanup(owner)
            check(!stagedSource.exists() && !output.exists())
            check(files.jpeg.snapshot(owner).state == JpegStageV2.State.TERMINAL)
            files.jpeg.admission()
            report.put("resourceEnded", true).put("closureStatus", "TERMINAL_PROVED")
                .put("stageTerminal", true).put("pinReleased", true)
                .put("ownedSourceAndOutputRemoved", true).put("originalInputClosed", inputClosed)
            val sample = activeSampler.finish(); sampled = true
            write(File(directory, "polling.json"), (sample.toString() + "\n").toByteArray(Charsets.UTF_8))
            report.put("sampling", JSONObject().put("status", sample.getString("status"))
                .put("hostPeaks", sample.getJSONObject("hostPeaks")).put("workerPeaks", sample.getJSONObject("workerPeaks")))
            check(sample.getString("status") == "MEASURED") { "Resource sampling remains UNKNOWN" }
            validatePhases(directory, request, worker, boot)
            report.put("status", "MEASURED").put("measurementStatus", "MEASURED")
                .put("scope", "THIS_SYNTHETIC_WORKER_RUN_NOT_A_DEVICE_MEMORY_CAP")
                .put("postRetiringTailPeak", "NOT_PROVEN").put("nativeFailureInjectionAndroid", "NOT_RUN")
        } catch (error: Throwable) {
            report.put("errorType", error.javaClass.simpleName).put("status", "UNKNOWN")
            throw error
        } finally {
            if (sampler != null && !sampled) {
                try { write(File(directory, "polling.json"), (sampler.finish().toString() + "\n").toByteArray(Charsets.UTF_8)) }
                catch (error: Throwable) { report.put("samplerEnd", "UNKNOWN:${error.javaClass.simpleName}") }
            }
            report.put("fullTestWallNs", JpegResourceEvidence.delta(SystemClock.elapsedRealtimeNanos(), wallStart))
                .put("hostProcessCpuMs", JpegResourceEvidence.delta(Process.getElapsedCpuTime(), cpuStart))
                .put("originalInputClosed", inputClosed)
            write(File(directory, "test-result.json"), (report.toString() + "\n").toByteArray(Charsets.UTF_8))
        }
    }

    /** Only a real, uniquely matched failed RESULT with complete retirement/Host closure may close.
     * A missing/unknown result is retained for the separate B1 recovery authority, never invented here.
     * The caller still fails its SUCCESS expectation after this safety-only closure.
     */
    private fun closeStructuredFailure(files: MediaFileProtocol, owner: JpegStageV2.Owner,
        source: JpegStageV2.FileIdentity, stagedSource: File, output: File,
        registered: JpegWorkerIdentity?, result: JpegWorkerResult, report: JSONObject) {
        check(result.outcome != JpegWorkerOutcome.SUCCESS)
        report.put("closureStatus", "RETAINED_UNKNOWN").put("measurementStatus", "UNKNOWN")
        val outcome = when (result.outcome) {
            JpegWorkerOutcome.INVALID_IMAGE -> JpegStageV2.Outcome.IMAGE
            JpegWorkerOutcome.RESOURCE_REJECTED -> JpegStageV2.Outcome.RESOURCE
            JpegWorkerOutcome.LOAD_ERROR, JpegWorkerOutcome.UNSUPPORTED -> JpegStageV2.Outcome.LOAD
            JpegWorkerOutcome.CANCELLED -> JpegStageV2.Outcome.CANCELLED
            JpegWorkerOutcome.TIMEOUT -> JpegStageV2.Outcome.TIMEOUT
            JpegWorkerOutcome.NATIVE_CRASH -> JpegStageV2.Outcome.NATIVE_CRASH
            JpegWorkerOutcome.RESULT_UNKNOWN -> return
            JpegWorkerOutcome.SUCCESS -> error("Success is not a failure closure")
        }
        val worker = result.workerIdentity
        val facts = linkedMapOf(
            "matchingRegisteredWorker" to (worker != null && worker == registered),
            "uniqueStructuredResult" to (result.resultCount == 1),
            "sourceFdClosed" to result.sourceFdClosed, "outputFdClosed" to result.outputFdClosed,
            "binderDead" to result.binderDead, "matchingProcessDead" to result.matchingProcessDead,
            "guardReleased" to result.guardReleased, "connectionCleaned" to result.connectionCleaned,
            "hostFdsClosed" to result.hostFdsClosed, "rpcQuiescent" to result.rpcQuiescent,
            "originalSessionQuiescent" to result.originalSessionQuiescent,
            "consumerEnded" to result.consumerEnded, "mayReleaseSupervisor" to result.mayReleaseSupervisor)
        report.put("failureClosureFacts", JSONObject(facts))
        if (facts.values.any { !it }) return
        try {
            val actualWorker = requireNotNull(worker)
            val before = files.jpeg.snapshot(owner)
            check(before.owner == owner && before.source == source && before.worker == actualWorker.stage() && before.pinned)
            files.jpeg.revoke(owner, result.outcome.name)
            val terminal = files.jpeg.complete(JpegStageV2.Completion(
                owner = owner, worker = actualWorker.stage(), outcome = outcome, source = source,
                resultCount = result.resultCount, sourceFdClosed = result.sourceFdClosed,
                outputFdClosed = result.outputFdClosed, binderDead = result.binderDead,
                matchingProcessDead = result.matchingProcessDead, strictFinished = result.strictFinished,
                decoderDestroyed = result.decoderDestroyed, connectionCleaned = result.connectionCleaned))
            check(terminal.owner == owner && terminal.worker == actualWorker.stage() &&
                terminal.source == source && terminal.state == JpegStageV2.State.TERMINAL &&
                terminal.outcome == outcome && !terminal.pinned)
            // No prepared receipt or business record is made for a failed decode.
            files.jpeg.safeCleanup(owner)
            requireAbsent(stagedSource); requireAbsent(output)
            val after = files.jpeg.snapshot(owner)
            check(after.state == JpegStageV2.State.TERMINAL && after.outcome == outcome && !after.pinned)
            files.jpeg.admission()
            report.put("closureStatus", "TERMINAL_PROVED").put("resourceEnded", true)
                .put("stageTerminal", true).put("pinReleased", true).put("ownedSourceAndOutputRemoved", true)
                .put("failedStageOutcome", outcome.name)
        } catch (error: Throwable) {
            report.put("closureStatus", "UNKNOWN").put("closureErrorType", error.javaClass.simpleName)
            throw error
        }
    }

    private fun requireAbsent(file: File) {
        try {
            Os.lstat(file.path)
            error("Owned payload is still present")
        } catch (error: ErrnoException) {
            check(error.errno == OsConstants.ENOENT) { "Owned payload absence is UNKNOWN" }
        }
    }

    private fun validatePhases(directory: File, request: JpegWorkerRequest, worker: JpegWorkerIdentity, boot: String) {
        val names = listOf("OPEN", "NATIVE_COMPLETE", "NORMALIZATION_BEGIN", "NORMALIZATION_END", "FINISHED", "RETIRING")
        val allowed = names.mapIndexed { index, name -> "${index.toString().padStart(2, '0')}-$name.json" }.toSet() +
            setOf("allow.properties", "worker-claim.json", "polling.json")
        check(requireNotNull(directory.list()).toSet() == allowed) { "Unknown or missing diagnostic child" }
        val claim = readJson(File(directory, "worker-claim.json"), 16384)
        check(claim.getInt("workerPid") == worker.pid && claim.getInt("workerUid") == worker.uid &&
            claim.getLong("workerStartTicks") == worker.startTicks && claim.getString("bootId") == boot)
        check(claim.getString("attempt") == request.attemptId && claim.getString("session") == request.sessionId &&
            claim.getString("nonce") == request.nonce)
        var previousTime = -1L
        for ((index, name) in names.withIndex()) {
            val phase = readJson(File(directory, "${index.toString().padStart(2, '0')}-$name.json"), 16384)
            check(phase.getString("phase") == name && phase.getString("status") == "MEASURED")
            check(phase.getString("generation") == request.generation && phase.getString("asset") == request.assetId &&
                phase.getString("attempt") == request.attemptId && phase.getString("session") == request.sessionId &&
                phase.getString("nonce") == request.nonce && phase.getString("sourceSha256") == request.sourceSha256)
            check(phase.getInt("pid") == worker.pid && phase.getInt("uid") == worker.uid &&
                phase.getLong("startTicks") == worker.startTicks && phase.getString("bootId") == boot)
            val time = phase.getLong("elapsedRealtimeNs"); check(time > previousTime); previousTime = time
            JpegResourceEvidence.proc(phase.getString("procStatusRaw"), phase.getString("procStatRaw"), worker.pid, worker.uid, worker.startTicks)
            if (name == "NATIVE_COMPLETE") {
                val raw = phase.getJSONObject("extra").getJSONArray("long16")
                val native = LongArray(raw.length()) { raw.getLong(it) }
                JpegResourceEvidence.nativeResult(native)
                check(native[0] == 0L && native[8] == 5000L && native[9] == 5000L)
            }
            if (name == "FINISHED") {
                val extra = phase.getJSONObject("extra")
                check(extra.getString("outcome") == "SUCCESS" && extra.getBoolean("sourceClosed") &&
                    extra.getBoolean("outputClosed") && extra.getBoolean("scratchClean"))
            }
        }
    }

    private fun fixedInput(name: String): File {
        val dir = File(context.filesDir.canonicalFile, "m10-b2-test-inputs")
        privateDirectory(dir, create = false)
        val allowed = setOf("large_40mp_baseline_420.jpg", "large_40mp_progressive_420.jpg", "valid_40mp_progressive_444.jpg")
        check(requireNotNull(dir.listFiles()).all { it.name in allowed })
        val file = File(dir, name)
        val before = Os.lstat(file.path)
        val expected = JpegResourceEvidence.fixture(name)
        check(OsConstants.S_ISREG(before.st_mode) && before.st_uid == Process.myUid() && before.st_nlink == 1L && before.st_size == expected[0].toLong())
        val digest = MessageDigest.getInstance("SHA-256")
        FileChannel.open(file.toPath(), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS).use { channel ->
            val buffer = ByteBuffer.allocate(65536)
            while (true) {
                val count = channel.read(buffer); if (count < 0) break; check(count > 0)
                buffer.flip(); digest.update(buffer); buffer.clear()
            }
        }
        val after = Os.lstat(file.path)
        check(before.st_dev == after.st_dev && before.st_ino == after.st_ino && before.st_size == after.st_size)
        check(digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) } == expected[1])
        return file
    }

    private fun privateDirectory(file: File, create: Boolean) {
        if (!file.exists() && create) { check(file.mkdir()); Os.chmod(file.path, 0x1c0) }
        val stat = Os.lstat(file.path)
        check(OsConstants.S_ISDIR(stat.st_mode) && stat.st_uid == Process.myUid() && stat.st_mode and 0x3f == 0 && file.canonicalFile == file.absoluteFile)
    }
    private fun write(file: File, bytes: ByteArray) {
        check(bytes.size in 1..2 * 1024 * 1024)
        FileChannel.open(file.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS).use { channel ->
            Os.chmod(file.path, 0x180)
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining()) channel.write(buffer)
            channel.force(true)
        }
        check(file.readBytes().contentEquals(bytes))
    }
    private fun readJson(file: File, limit: Int): JSONObject {
        val stat = Os.lstat(file.path)
        check(OsConstants.S_ISREG(stat.st_mode) && stat.st_uid == Process.myUid() && stat.st_size in 1..limit.toLong())
        return JSONObject(file.readText(Charsets.UTF_8))
    }
    private fun JpegWorkerIdentity.stage() = JpegStageV2.WorkerIdentity(pid, uid, startTicks)
    private fun resultObservation(result: JpegWorkerResult) = JSONObject().put("outcome", result.outcome.name)
        .put("workerPid", result.workerIdentity?.pid).put("workerUid", result.workerIdentity?.uid)
        .put("workerStartTicks", result.workerIdentity?.startTicks).put("sourceClosed", result.sourceFdClosed)
        .put("outputClosed", result.outputFdClosed).put("decoderDestroyed", result.decoderDestroyed)
        .put("strictFinished", result.strictFinished).put("binderDead", result.binderDead)
        .put("matchingProcessDead", result.matchingProcessDead).put("guardReleased", result.guardReleased)
        .put("connectionCleaned", result.connectionCleaned).put("resultCount", result.resultCount).put("detail", result.detail)
        .put("hostFdsClosed", result.hostFdsClosed).put("rpcQuiescent", result.rpcQuiescent)
        .put("originalSessionQuiescent", result.originalSessionQuiescent).put("consumerEnded", result.consumerEnded)
        .put("mayReleaseSupervisor", result.mayReleaseSupervisor)
    private fun uuid() = UUID.randomUUID().toString()
    private companion object { const val UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}" }
}
