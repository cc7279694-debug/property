package com.cooper.lithic.data.media.jpegworker

import android.content.Context
import android.os.Debug
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.LinkOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/** Debug-only, explicit synthetic-permit telemetry. It never changes Worker wire/result authority. */
internal object JpegResourceDiagnostics {
    private val inactive = Session(null, null, null, null)
    private val uuid = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

    fun open(context: Context, request: JpegWorkerRequest): Session {
        if (!request.generation.startsWith("m10-resource-v2-")) return inactive
        return try {
            require(request.generation.removePrefix("m10-resource-v2-").matches(uuid) && request.attemptId.matches(uuid))
            val base = File(context.noBackupFilesDir.canonicalFile, "m10-b2-resource-diagnostics")
            val directory = File(base, request.attemptId)
            privateDirectory(base); privateDirectory(directory)
            val marker = File(directory, "allow.properties")
            val stat = Os.lstat(marker.path)
            require(OsConstants.S_ISREG(stat.st_mode) && stat.st_uid == Process.myUid() && stat.st_nlink == 1L && stat.st_size in 1L..4096L)
            val raw = bounded(marker, 4096)
            val again = Os.lstat(marker.path)
            require(stat.st_dev == again.st_dev && stat.st_ino == again.st_ino && stat.st_size == again.st_size && bounded(marker, 4096) == raw)
            val allow = JpegResourceEvidence.permit(raw)
            val host = JpegProcessRole.identity(allow.getValue("hostPid").toInt())
            require(host.uid == Process.myUid() && host.pid != Process.myPid() &&
                JpegProcessRole.readCmdline(File("/proc/${host.pid}/cmdline")) == context.packageName)
            val fixture = arrayOf("large_40mp_baseline_420.jpg", "large_40mp_progressive_420.jpg", "valid_40mp_progressive_444.jpg")
                .single { JpegResourceEvidence.fixture(it)[1] == request.sourceSha256 }
            val expected = mapOf("version" to "1", "generation" to request.generation, "assetId" to request.assetId,
                "attemptId" to request.attemptId, "sessionId" to request.sessionId, "nonce" to request.nonce,
                "sourceBytes" to request.sourceSize.toString(), "sourceSha" to request.sourceSha256,
                "fixtureName" to fixture, "hostPid" to host.pid.toString(), "hostUid" to host.uid.toString(),
                "hostStartTicks" to host.startTicks.toString(), "bootId" to JpegBootIdentity.current(context))
            JpegResourceEvidence.match(allow, expected)
            val worker = JpegProcessRole.identity()
            val session = Session(directory, request, worker, expected.getValue("bootId"))
            session.writeExclusive("worker-claim.json", JSONObject().put("protocol", "M10_B2_DEBUG_CLAIM_V1")
                .put("attempt", request.attemptId).put("session", request.sessionId).put("nonce", request.nonce)
                .put("workerPid", worker.pid).put("workerUid", worker.uid).put("workerStartTicks", worker.startTicks)
                .put("bootId", expected.getValue("bootId")).put("allowSHA256", sha(raw.toByteArray(Charsets.UTF_8))))
            session.phase("OPEN")
            session
        } catch (error: Throwable) {
            // Missing/malformed permit or write failure is never a measured PASS.
            // No fallback record is created in an unproved directory.
            Log.e("LITHIC_B2_RESOURCE", "DIAGNOSTIC_OPEN_UNKNOWN:${error.javaClass.simpleName}")
            inactive
        }
    }

    private fun privateDirectory(file: File) {
        val info = Os.lstat(file.path)
        require(OsConstants.S_ISDIR(info.st_mode) && info.st_uid == Process.myUid() &&
            info.st_mode and 0x3f == 0 && file.canonicalFile == file.absoluteFile)
    }

    private fun bounded(file: File, limit: Int): String = java.nio.channels.Channels.newInputStream(
        FileChannel.open(file.toPath(), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)).use { input ->
        val bytes = ByteArray(limit + 1)
        var count = 0
        while (count < bytes.size) {
            val n = input.read(bytes, count, bytes.size - count)
            if (n < 0) break
            require(n > 0)
            count += n
        }
        require(count in 1..limit)
        val text = bytes.copyOf(count).toString(Charsets.UTF_8)
        require(!text.contains('\uFFFD'))
        text
    }

    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    class Session internal constructor(private val directory: File?, private val request: JpegWorkerRequest?,
        private val worker: JpegWorkerIdentity?, private val bootEpoch: String?) {
        private var sequence = 0
        private val errors = mutableListOf<String>()
        private var nativeSeen = false
        private var normalizationStarted = false
        private var normalizationEnded = false
        private var finished = false

        internal fun writeExclusive(name: String, value: JSONObject) {
            val dir = requireNotNull(directory)
            privateDirectory(dir)
            val file = File(dir, name)
            val bytes = (value.toString() + "\n").toByteArray(Charsets.UTF_8)
            require(bytes.size <= 16384)
            FileChannel.open(file.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS).use { channel ->
                Os.chmod(file.path, 0x180) // 0600; same-UID synthetic evidence only.
                val data = ByteBuffer.wrap(bytes)
                while (data.hasRemaining()) channel.write(data)
                channel.force(true)
            }
            require(file.readBytes().contentEquals(bytes))
        }

        private fun attempt(action: () -> Unit) {
            if (directory == null) return
            try { action() } catch (error: Throwable) {
                errors.add(error.javaClass.simpleName)
                Log.e("LITHIC_B2_RESOURCE", "DIAGNOSTIC_PHASE_UNKNOWN:${error.javaClass.simpleName}")
            }
        }

        @Synchronized internal fun phase(name: String, extra: JSONObject = JSONObject()) = attempt {
            val id = requireNotNull(worker)
            val req = requireNotNull(request)
            val begun = SystemClock.elapsedRealtimeNanos()
            val statusRaw = bounded(File("/proc/${id.pid}/status"), 32768)
            val statRaw = bounded(File("/proc/${id.pid}/stat"), 8192)
            val proc = JpegResourceEvidence.proc(statusRaw, statRaw, id.pid, id.uid, id.startTicks)
            val memory = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }
            require(memory.totalPss > 0) { "PSS_UNAVAILABLE" }
            val runtime = Runtime.getRuntime()
            val value = JSONObject().put("protocol", "M10_B2_DEBUG_RESOURCE_PHASE_V1")
                .put("phase", name).put("status", if (errors.isEmpty()) "MEASURED" else "UNKNOWN")
                .put("generation", req.generation).put("asset", req.assetId).put("attempt", req.attemptId)
                .put("session", req.sessionId).put("nonce", req.nonce).put("sourceSha256", req.sourceSha256)
                .put("sourceBytes", req.sourceSize).put("pid", id.pid).put("uid", id.uid).put("startTicks", id.startTicks)
                .put("bootId", bootEpoch).put("bootEpochKind", "SETTINGS_GLOBAL_BOOT_COUNT_NOT_RANDOM_NONCE")
                .put("elapsedRealtimeNs", begun).put("processCpuMs", Process.getElapsedCpuTime())
                .put("threadCpuNs", Debug.threadCpuTimeNanos()).put("proc", JSONObject(proc))
                .put("procStatusRaw", statusRaw).put("procStatRaw", statRaw)
                .put("pssKiB", memory.totalPss).put("nativeHeapAllocatedBytes", Debug.getNativeHeapAllocatedSize())
                .put("javaUsedBytes", runtime.totalMemory() - runtime.freeMemory())
                .put("openFdCountSample", requireNotNull(File("/proc/${id.pid}/fd").list()).size)
                .put("extra", extra).put("errors", JSONArray(errors))
                .put("observationCostWallNs", JpegResourceEvidence.delta(SystemClock.elapsedRealtimeNanos(), begun))
                .put("hwmScope", "PROCESS_BIRTH_TO_THIS_SAMPLE_NOT_TO_DEATH")
                .put("pssRssScope", "POINT_SAMPLE_NOT_HARD_CAP")
            writeExclusive("${sequence.toString().padStart(2, '0')}-$name.json", value)
            sequence++
        }

        @Synchronized fun nativeComplete(values: LongArray) = attempt {
            require(!nativeSeen)
            JpegResourceEvidence.nativeResult(values)
            require(values[2] <= JpegWorkerWire.REQUEST_BUDGET)
            nativeSeen = true
            phase("NATIVE_COMPLETE", JSONObject().put("long16", JSONArray(values.toList()))
                .put("requestBudgetBytes", JpegWorkerWire.REQUEST_BUDGET)
                .put("chargedPeakScope", "COVERED_MALLOC_REQUESTS_NOT_PROCESS_MEMORY")
                .put("nativeRssScope", "PROCESS_LIFETIME_HWM_AS_OF_NATIVE_RETURN_NOT_FINAL_WORKER_PEAK"))
        }
        @Synchronized fun normalizationStarted() = attempt {
            require(nativeSeen && !normalizationStarted)
            normalizationStarted = true; phase("NORMALIZATION_BEGIN")
        }
        @Synchronized fun normalizationFinished() = attempt {
            require(normalizationStarted && !normalizationEnded)
            normalizationEnded = true; phase("NORMALIZATION_END")
        }
        @Synchronized fun finish(outcome: String, sourceClosed: Boolean, outputClosed: Boolean, scratchClean: Boolean) = attempt {
            require(!finished); finished = true
            if (outcome == "SUCCESS") require(nativeSeen && normalizationStarted && normalizationEnded)
            phase("FINISHED", JSONObject().put("outcome", outcome).put("sourceClosed", sourceClosed)
                .put("outputClosed", outputClosed).put("scratchClean", scratchClean))
        }
        @Synchronized fun retiring() = attempt {
            require(finished)
            phase("RETIRING", JSONObject().put("remainingTail", "NOT_PROVEN_AFTER_THIS_RECORD"))
        }
    }
}
