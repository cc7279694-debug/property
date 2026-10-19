package com.cooper.lithic.data.media

import android.content.Context
import android.os.Build
import android.os.Process
import android.system.Os
import android.system.OsConstants
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.data.media.jpegstage.JpegRecoveryVerifier
import com.cooper.lithic.data.media.jpegstage.JpegStageV2
import com.cooper.lithic.data.media.jpegworker.AndroidJpegRecoveryProbe
import com.cooper.lithic.data.media.jpegworker.JpegBootIdentity
import com.cooper.lithic.data.media.jpegworker.JpegProcessRole
import com.cooper.lithic.data.media.jpegworker.JpegWorkerClient
import com.cooper.lithic.data.media.jpegworker.JpegWorkerIdentity
import com.cooper.lithic.data.media.jpegworker.JpegWorkerOutcome
import com.cooper.lithic.data.media.jpegworker.JpegWorkerRequest
import com.cooper.lithic.data.media.jpegworker.JpegWorkerResult
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.LinkOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

/** Exact test-only fault points. SOURCE_ONLY until a new Test APK is independently admitted.
 * No production protocol, Room record, media reference or existing dataset is modified.
 * Host fault methods intentionally interrupt instrumentation; they cannot report JUnit PASS.
 */
@RunWith(AndroidJUnit4::class)
class M10B1ExactProcessFaultTest {
    @Test fun exact_worker_death_after_pin_before_data_fd(): Unit = runBlocking {
        inject(Kind.WORKER_BEFORE_DATA_FD, WORKER_METHOD)
    }

    @Test fun exact_host_death_after_pin_before_data_fd(): Unit = runBlocking {
        inject(Kind.HOST_BEFORE_DATA_FD, HOST_PIN_METHOD)
    }

    @Test fun exact_host_death_after_real_retirement_before_receipt(): Unit = runBlocking {
        inject(Kind.HOST_AFTER_RETIRE_BEFORE_RECEIPT, HOST_RETIRED_METHOD)
    }

    @Test fun recover_only_exact_ended_host_fault_without_new_consumer(): Unit {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val args = InstrumentationRegistry.getArguments()
        val kind = Kind.valueOf(requireNotNull(args.getString("m10FaultKind")))
        check(kind != Kind.WORKER_BEFORE_DATA_FD) { "A live Worker-fault Host must never be restarted to obtain closure" }
        val run = begin(context, RECOVER_METHOD, kind)
        val oldRun = canonical(requireNotNull(args.getString("m10FaultOriginalRunId")))
        check(oldRun != run.runId)
        val oldEvidence = File(File(context.noBackupFilesDir.canonicalFile, "m10-b1b2-safety"), oldRun)
        val checkpointFile = File(oldEvidence, "fault-checkpoint.json")
        val checkpoint = readCheckpoint(checkpointFile)
        check(checkpoint.getString("protocol") == CHECKPOINT && checkpoint.getString("runId") == oldRun &&
            checkpoint.getString("kind") == kind.name && checkpoint.getString("targetProof") == targetProof() &&
            checkpoint.getString("bootId") == run.boot && checkpoint.getInt("api") == Build.VERSION.SDK_INT)
        val originalHost = identity(checkpoint.getJSONObject("host"))
        val oldWorker = identity(checkpoint.getJSONObject("worker"))
        check(originalHost.uid == run.host.uid && oldWorker.uid == run.host.uid && oldWorker.pid != originalHost.pid)
        check(JpegProcessRole.instanceAbsent(originalHost) && JpegProcessRole.instanceAbsent(oldWorker)) {
            "Both exact old instances must already have ended; this method never kills or binds"
        }
        M10B1B2SafetySupport.noWorker(context)
        val directory = File(context.noBackupFilesDir.canonicalFile, "m10-b1-fault-$oldRun")
        check(directory.isDirectory && directory.canonicalFile == directory.absoluteFile)
        val owner = readOwner(checkpoint.getJSONObject("owner"))
        check(owner.generation == directory.name)
        val files = MediaFileProtocol(directory)
        val supervisor = JpegStageV2.SupervisorIdentity(run.boot, originalHost.pid, originalHost.uid, originalHost.startTicks)
        val before = files.jpeg.snapshot(owner)
        check(before.worker == oldWorker.stage() && before.pinned && before.outcome == null)
        check(!File(files.jpeg.sourceFile(owner).parentFile, "terminal.v2").exists())
        check(!File(files.jpeg.sourceFile(owner).parentFile, "prepared.v2").exists())
        // A fresh proof reference records the host supervisor's external admission;
        // actual death/ownership/lease eligibility is still checked again by the App verifier.
        val externalProof = requireNotNull(args.getString("m10FaultExternalEndProofSHA256"))
        check(externalProof.matches(Regex("[0-9a-f]{64}")))
        M10B1B2SafetySupport.write(M10B1B2SafetySupport.Run(context, oldEvidence, oldRun,
            originalHost, run.boot, checkpoint.getString("method")), "recovery-attempt.json", JSONObject()
            .put("protocol", "M10_B1_HOST_RECOVERY_CLAIM_V1").put("recoveryRunId", run.runId)
            .put("externalEndProofSHA256", externalProof).put("status", "ATTEMPT_ISSUED"))
        val probe = AndroidJpegRecoveryProbe(context)
        files.jpeg.recoverNoResult(owner) { request ->
            check(request.supervisor == supervisor && request.worker == oldWorker.stage())
            JpegRecoveryVerifier(probe).verify(request)
        }
        assertIndependentClosure(files, owner)
        M10B1B2SafetySupport.assertHost(run)
        M10B1B2SafetySupport.noWorker(context)
        M10B1B2SafetySupport.write(run, "result.json", JSONObject().put("protocol", "M10_B1_HOST_RECOVERY_RESULT_V1")
            .put("status", "PASS").put("originalRunId", oldRun).put("runId", run.runId).put("kind", kind.name)
            .put("externalEndProofSHA256", externalProof).put("externalProofReferenceNotSelfAuthenticated", true)
            .put("oldHostAndWorkerIndependentlyAbsent", true).put("abandonedNotDecodeTerminal", true)
            .put("newWorkerOrRoomRecordCreated", false).put("ownedPayloadCleanup", true))
    }

    private suspend fun inject(kind: Kind, method: String): Unit {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val run = begin(context, method, kind)
        M10B1B2SafetySupport.noWorker(context)
        val directory = File(context.noBackupFilesDir.canonicalFile, "m10-b1-fault-${run.runId}")
        check(directory.mkdir() && File(directory, "media").mkdir())
        val files = MediaFileProtocol(directory)
        val owner = JpegStageV2.Owner(directory.name, uuid(), uuid(), uuid(), uuid())
        val supervisor = JpegStageV2.SupervisorIdentity(run.boot, run.host.pid, run.host.uid, run.host.startTicks)
        val initial = M10JpegFixtures.bytes(M10JpegFixtures.COMPLETE).inputStream().use { files.jpeg.create(owner, it, supervisor) }
        check(initial.source.sha256 == "2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907")
        val source = files.jpeg.sourceFile(owner)
        val output = files.jpeg.outputFile(owner)
        val guard = files.jpeg.guardFile(owner)
        val guardIdentity = files.identity(guard)
        val request = JpegWorkerRequest(owner.generation, owner.assetId, owner.attemptId, owner.sessionId,
            owner.nonce, initial.source.bytes, initial.source.sha256, initial.source.device, initial.source.inode,
            guardIdentity.device, guardIdentity.inode)
        val workerRef = AtomicReference<JpegWorkerIdentity?>()
        val issued = AtomicBoolean(false)
        val late = AtomicBoolean(false)
        val result = JpegWorkerClient(context).execute(source, output, guard, request,
            beforeDelivery = { worker ->
                check(workerRef.compareAndSet(null, worker))
                validateExactWorker(run, worker)
                val pin = files.jpeg.registerConsumer(owner, worker.stage())
                check(pin.pinned && pin.source == initial.source)
                files.jpeg.markDelivery(owner)
                if (kind != Kind.HOST_AFTER_RETIRE_BEFORE_RECEIPT) {
                    checkpoint(run, kind, files, owner, worker, issued, null)
                    validateExactWorker(run, worker)
                    if (kind == Kind.WORKER_BEFORE_DATA_FD) Process.killProcess(worker.pid)
                    else Process.killProcess(run.host.pid)
                    // Returning from kill is never death proof; do not proceed to DELIVER.
                    throw IOException("AUTHORIZED_EXACT_FAULT_REQUESTED_BEFORE_DATA_FD")
                }
            }, revoke = { reason -> files.jpeg.revoke(owner, reason) },
            abandonedResult = { observed ->
                check(late.compareAndSet(false, true)) { "Unexpected duplicate late fault observation" }
                M10B1B2SafetySupport.write(run, "fault-late-observation.json", observation(observed)
                    .put("status", "OBSERVED_NOT_RECOVERY_AUTHORITY"))
            })
        val worker = requireNotNull(workerRef.get())
        check(result.workerIdentity == worker)
        if (kind == Kind.HOST_AFTER_RETIRE_BEFORE_RECEIPT) {
            check(result.canPrepare && result.outcome == JpegWorkerOutcome.SUCCESS && result.resultCount == 1)
            checkpoint(run, kind, files, owner, worker, issued, result)
            M10B1B2SafetySupport.assertHost(run)
            Process.killProcess(run.host.pid)
            throw IOException("HOST_SELF_TERMINATION_REQUESTED_NOT_A_TEST_PASS")
        }
        check(kind == Kind.WORKER_BEFORE_DATA_FD) { "Host termination did not occur; preserve stage and stop" }
        M10B1B2SafetySupport.write(run, "fault-observation.json", observation(result)
            .put("status", "OBSERVED_NOT_YET_RECOVERED"))
        check(issued.get() && result.outcome != JpegWorkerOutcome.SUCCESS && result.resultCount == 0 && !result.canPrepare)
        check(result.hostFdsClosed && result.rpcQuiescent && result.originalSessionQuiescent &&
            result.connectionCleaned && result.binderDead && result.matchingProcessDead && result.guardReleased) {
            "Real original Session remains UNKNOWN; do not restart or clean it"
        }
        check(!result.sourceFdClosed && !result.outputFdClosed) { "A pre-delivery fault cannot invent Worker data-FD receipts" }
        val live = JpegRecoveryVerifier.LiveQuiescence(owner, supervisor, worker.stage(), result.hostFdsClosed,
            result.rpcQuiescent, result.originalSessionQuiescent, result.connectionCleaned,
            result.binderDead, result.matchingProcessDead, result.guardReleased)
        val probe = AndroidJpegRecoveryProbe(context)
        files.jpeg.recoverNoResult(owner) { facts -> JpegRecoveryVerifier(probe).verify(facts, live) }
        assertIndependentClosure(files, owner)
        M10B1B2SafetySupport.assertHost(run)
        M10B1B2SafetySupport.noWorker(context)
        M10B1B2SafetySupport.write(run, "result.json", observation(result).put("status", "PASS")
            .put("protocol", "M10_B1_EXACT_WORKER_FAULT_RESULT_V1").put("runId", run.runId)
            .put("kind", kind.name).put("abandonedNotDecodeTerminal", true).put("ownedPayloadCleanup", true)
            .put("closureUsesOnlyLiveOriginalResult", true).put("wireResultOrWorkerFdFlagsInvented", false))
    }

    private fun checkpoint(run: M10B1B2SafetySupport.Run, kind: Kind, files: MediaFileProtocol,
        owner: JpegStageV2.Owner, worker: JpegWorkerIdentity, issued: AtomicBoolean, result: JpegWorkerResult?) {
        check(issued.compareAndSet(false, true)) { "Fault is one-shot" }
        M10B1B2SafetySupport.assertHost(run)
        val stage = files.jpeg.snapshot(owner)
        check(stage.pinned && stage.worker == worker.stage() && stage.outcome == null)
        val parent = files.jpeg.sourceFile(owner).parentFile
        check(!File(parent, "terminal.v2").exists() && !File(parent, "prepared.v2").exists())
        val value = JSONObject().put("protocol", CHECKPOINT).put("status", "ARMED_BEFORE_EXACT_CALL_NOT_DEATH_PROOF")
            .put("method", run.method).put("kind", kind.name).put("runId", run.runId)
            .put("api", Build.VERSION.SDK_INT).put("targetProof", targetProof()).put("bootId", run.boot)
            .put("host", identity(run.host)).put("worker", identity(worker)).put("owner", ownerJson(owner))
            .put("source", fileIdentity(stage.source)).put("output", fileIdentity(files.identity(files.jpeg.outputFile(owner))))
            .put("guard", fileIdentity(files.identity(files.jpeg.guardFile(owner))))
            .put("sourceAndOutputSubmittedToRoom", false).put("pinDurable", true)
            .put("dataFdDelivered", kind == Kind.HOST_AFTER_RETIRE_BEFORE_RECEIPT)
            .put("decodeReceiptCreated", false).put("killReturnNeverProvesDeath", true)
        if (result != null) value.put("realRetiredResult", observation(result))
        M10B1B2SafetySupport.write(run, "fault-checkpoint.json", value)
    }

    private fun begin(context: Context, method: String, kind: Kind): M10B1B2SafetySupport.Run {
        val args = InstrumentationRegistry.getArguments()
        check(args.getString("m10FaultPermit") == "M10_B1B2_EXACT_FAULT_V1" && args.getString("m10FaultKind") == kind.name)
        check(args.getString("m10FaultExpectedUid") == Process.myUid().toString())
        check(args.getString("m10FaultExpectedBoot") == JpegBootIdentity.current(context))
        check(args.getString("m10FaultExpectedAppSHA256") == APP_SHA && sha(File(context.applicationInfo.sourceDir)) == APP_SHA)
        val expectedTest = requireNotNull(args.getString("m10FaultExpectedTestSHA256"))
        check(expectedTest.matches(Regex("[0-9a-f]{64}")))
        val testContext = InstrumentationRegistry.getInstrumentation().context
        check(testContext.packageName == "com.cooper.lithic.debug.test")
        // Reading the installed APK is fail-closed: unavailable access is never a default identity.
        check(sha(File(testContext.applicationInfo.sourceDir)) == expectedTest)
        targetProof()
        return M10B1B2SafetySupport.begin(context, method)
    }

    private fun targetProof() = canonical(requireNotNull(InstrumentationRegistry.getArguments().getString("m10FaultTargetProof")))
    private fun validateExactWorker(run: M10B1B2SafetySupport.Run, worker: JpegWorkerIdentity) {
        M10B1B2SafetySupport.assertHost(run)
        check(worker.pid != run.host.pid && worker.uid == run.host.uid && JpegProcessRole.identity(worker.pid) == worker)
        check(JpegProcessRole.readCmdline(File("/proc/${worker.pid}/cmdline")) == run.context.packageName + ":jpeg_worker")
    }
    private fun assertIndependentClosure(files: MediaFileProtocol, owner: JpegStageV2.Owner) {
        val source = files.jpeg.sourceFile(owner); val output = files.jpeg.outputFile(owner)
        val closed = files.jpeg.snapshot(owner)
        check(closed.state == JpegStageV2.State.ABANDONED && !closed.pinned && closed.outcome == null)
        check(!File(source.parentFile, "terminal.v2").exists() && !File(source.parentFile, "prepared.v2").exists())
        check(File(source.parentFile, "abandoned.v2").isFile && File(source.parentFile, "revoked.v2").isFile)
        files.jpeg.safeCleanup(owner)
        check(!source.exists() && !output.exists())
        files.jpeg.admission()
    }
    private fun readCheckpoint(file: File): JSONObject {
        check(file.canonicalFile == file.absoluteFile)
        val before = Os.lstat(file.path)
        check(OsConstants.S_ISREG(before.st_mode) && before.st_uid == Process.myUid() &&
            before.st_nlink == 1L && before.st_size in 1..16_384)
        val buffer = ByteBuffer.allocate(16_385)
        FileChannel.open(file.toPath(), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS).use { channel ->
            while (buffer.hasRemaining()) {
                val count = channel.read(buffer)
                if (count < 0) break
                check(count > 0)
            }
        }
        val after = Os.lstat(file.path)
        check(before.st_dev == after.st_dev && before.st_ino == after.st_ino && before.st_size == after.st_size &&
            OsConstants.S_ISREG(after.st_mode) && buffer.position().toLong() == before.st_size)
        return JSONObject(String(buffer.array(), 0, buffer.position(), Charsets.UTF_8))
    }
    private fun identity(worker: JpegWorkerIdentity) = JSONObject().put("pid", worker.pid).put("uid", worker.uid).put("startTicks", worker.startTicks)
    private fun identity(value: JSONObject) = JpegWorkerIdentity(value.getInt("pid"), value.getInt("uid"), value.getLong("startTicks"))
    private fun ownerJson(value: JpegStageV2.Owner) = JSONObject().put("generation", value.generation).put("asset", value.assetId)
        .put("attempt", value.attemptId).put("session", value.sessionId).put("nonce", value.nonce)
    private fun readOwner(value: JSONObject) = JpegStageV2.Owner(value.getString("generation"), canonical(value.getString("asset")),
        canonical(value.getString("attempt")), canonical(value.getString("session")), canonical(value.getString("nonce")))
    private fun fileIdentity(value: JpegStageV2.FileIdentity) = JSONObject().put("device", value.device).put("inode", value.inode)
        .put("bytes", value.bytes).put("sha256", value.sha256)
    private fun observation(value: JpegWorkerResult) = JSONObject().put("outcome", value.outcome.name).put("resultCount", value.resultCount)
        .put("hostFdsClosed", value.hostFdsClosed).put("rpcQuiescent", value.rpcQuiescent)
        .put("originalSessionQuiescent", value.originalSessionQuiescent).put("connectionCleaned", value.connectionCleaned)
        .put("binderDead", value.binderDead).put("matchingProcessDead", value.matchingProcessDead).put("guardReleased", value.guardReleased)
        .put("sourceFdClosed", value.sourceFdClosed).put("outputFdClosed", value.outputFdClosed)
        .put("strictFinished", value.strictFinished).put("decoderDestroyed", value.decoderDestroyed).put("detail", value.detail)
    private fun JpegWorkerIdentity.stage() = JpegStageV2.WorkerIdentity(pid, uid, startTicks)
    private fun sha(file: File): String = file.inputStream().use { input ->
        val digest = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(64 * 1024)
        while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    private fun canonical(value: String): String { check(value.length == 36 && UUID.fromString(value).toString() == value); return value }
    private fun uuid() = UUID.randomUUID().toString()
    private enum class Kind { WORKER_BEFORE_DATA_FD, HOST_BEFORE_DATA_FD, HOST_AFTER_RETIRE_BEFORE_RECEIPT }
    companion object {
        private const val APP_SHA = "b7c90277fa6c27e6ab314aa52c293de8e445ba6a3deba9e984ab96f4280e8a4c"
        private const val CHECKPOINT = "M10_B1_EXACT_FAULT_CHECKPOINT_V1"
        private const val PREFIX = "com.cooper.lithic.data.media.M10B1ExactProcessFaultTest#"
        const val WORKER_METHOD = PREFIX + "exact_worker_death_after_pin_before_data_fd"
        const val HOST_PIN_METHOD = PREFIX + "exact_host_death_after_pin_before_data_fd"
        const val HOST_RETIRED_METHOD = PREFIX + "exact_host_death_after_real_retirement_before_receipt"
        const val RECOVER_METHOD = PREFIX + "recover_only_exact_ended_host_fault_without_new_consumer"
    }
}
