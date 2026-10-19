package com.cooper.lithic.data.media

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.system.ErrnoException
import com.cooper.lithic.data.media.jpegworker.JpegProcessRole
import com.cooper.lithic.data.media.jpegworker.JpegResourceEvidence
import com.cooper.lithic.data.media.jpegworker.JpegWorkerIdentity
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONArray
import org.json.JSONObject

/** Test-only observer. It neither owns Worker FDs/leases nor stops/cancels a consumer. */
internal class M10ResourceSampler(context: Context, private val host: JpegWorkerIdentity, private val boot: String) {
    private val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val target = AtomicReference<JpegWorkerIdentity?>()
    private val stop = AtomicBoolean(false)
    private val samples = mutableListOf<JSONObject>()
    private val errors = mutableListOf<String>()
    private val workerAbsent = AtomicBoolean(false)
    private val ticksPerSecond = Os.sysconf(OsConstants._SC_CLK_TCK).also { require(it > 0) }
    private var polls = 0
    private val thread = Thread({ poll() }, "m10-b2-resource-sampler").apply { isDaemon = true }

    fun start() { sample(host, "HOST"); thread.start() }
    fun attach(worker: JpegWorkerIdentity) {
        check(worker.pid != host.pid && worker.uid == host.uid && target.compareAndSet(null, worker))
        sample(worker, "WORKER")
    }

    private fun poll() {
        while (!stop.get()) {
            if (++polls > 1200) { synchronized(errors) { errors.add("POLL_LIMIT_UNKNOWN") }; return }
            sample(host, "HOST")
            target.get()?.let { if (!workerAbsent.get()) sample(it, "WORKER") }
            try { Thread.sleep(100) } catch (error: InterruptedException) {
                synchronized(errors) { errors.add("SAMPLER_INTERRUPTED_UNKNOWN") }; return
            }
        }
    }

    private fun sample(id: JpegWorkerIdentity, role: String) {
        try {
            val began = SystemClock.elapsedRealtimeNanos()
            val status = read(File("/proc/${id.pid}/status"), 32768)
            val stat = read(File("/proc/${id.pid}/stat"), 8192)
            val proc = JpegResourceEvidence.proc(status, stat, id.pid, id.uid, id.startTicks)
            val memory = manager.getProcessMemoryInfo(intArrayOf(id.pid)).single()
            check(memory.totalPss > 0) { "PSS_UNAVAILABLE" }
            check(JpegProcessRole.identity(id.pid) == id) { "PID_REPLACED" }
            val sample = JSONObject().put("role", role).put("pid", id.pid).put("uid", id.uid)
                .put("startTicks", id.startTicks).put("bootId", boot).put("elapsedRealtimeNs", began)
                .put("proc", JSONObject(proc)).put("pssKiB", memory.totalPss)
                .put("nativePssKiB", memory.nativePss).put("dalvikPssKiB", memory.dalvikPss)
                .put("procStatusSHA256", sha(status)).put("procStatSHA256", sha(stat))
                .put("observationCostWallNs", JpegResourceEvidence.delta(SystemClock.elapsedRealtimeNanos(), began))
            synchronized(samples) { samples.add(sample) }
        } catch (error: Exception) {
            // Natural retirement may race a read. Absence is recorded separately
            // and is never substituted for final Client/Binder/guard evidence.
            val absent = if (role == "WORKER") try {
                Os.stat("/proc/${id.pid}"); false
            } catch (missing: ErrnoException) { missing.errno == OsConstants.ENOENT } else false
            if (absent) workerAbsent.set(true)
            else synchronized(errors) { errors.add("$role:${error.javaClass.simpleName}") }
        }
    }

    fun finish(): JSONObject {
        stop.set(true)
        thread.join(3000)
        if (thread.isAlive) synchronized(errors) { errors.add("SAMPLER_STOP_UNKNOWN") }
        sample(host, "HOST")
        val captured = synchronized(samples) { samples.toList() }
        val failures = synchronized(errors) { errors.toList() }
        val hostRows = captured.filter { it.getString("role") == "HOST" }
        val workerRows = captured.filter { it.getString("role") == "WORKER" }
        val known = failures.isEmpty() && hostRows.isNotEmpty() && workerRows.isNotEmpty() && !thread.isAlive
        fun peaks(rows: List<JSONObject>) = JSONObject()
            .put("rssKiB", rows.maxOfOrNull { it.getJSONObject("proc").getLong("rssKiB") } ?: JSONObject.NULL)
            .put("pssKiB", rows.maxOfOrNull { it.getLong("pssKiB") } ?: JSONObject.NULL)
            .put("hwmAsOfSampleKiB", rows.maxOfOrNull { it.getJSONObject("proc").getLong("hwmKiB") } ?: JSONObject.NULL)
        return JSONObject().put("status", if (known) "MEASURED" else "UNKNOWN")
            .put("intervalMs", 100).put("maximumPolls", 1200).put("clockTicksPerSecond", ticksPerSecond)
            .put("samples", JSONArray(captured)).put("errors", JSONArray(failures))
            .put("hostPeaks", peaks(hostRows)).put("workerPeaks", peaks(workerRows))
            .put("workerGoneDuringPolling", workerAbsent.get()).put("samplerStopped", !thread.isAlive)
            .put("rssPssScope", "SAMPLED_LOWER_BOUND_PSS_MAY_BE_PLATFORM_THROTTLED")
            .put("hwmScope", "PROCESS_BIRTH_TO_SAMPLE_NOT_FINAL_DEATH_HWM")
            .put("cpuScope", "PROC_UTIME_STIME_TICKS_PROCESS_TOTAL_NOT_NATIVE_ONLY")
    }

    private fun read(file: File, limit: Int): String = FileInputStream(file).use { input ->
        val buffer = ByteArray(limit + 1)
        var size = 0
        while (size < buffer.size) {
            val count = input.read(buffer, size, buffer.size - size)
            if (count < 0) break
            check(count > 0); size += count
        }
        check(size in 1..limit)
        buffer.copyOf(size).toString(Charsets.UTF_8).also { check(!it.contains('\uFFFD')) }
    }
    private fun sha(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}
