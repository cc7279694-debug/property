package com.cooper.lithic.data.media

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.system.Os
import android.system.OsConstants
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.data.media.jpegworker.JpegBootIdentity
import com.cooper.lithic.data.media.jpegworker.JpegProcessRole
import com.cooper.lithic.data.media.jpegworker.JpegWorkerIdentity
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.LinkOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject

/** Test-only exclusive authorization/evidence. No App owner, database, Binder or consumer is created here. */
internal object M10B1B2SafetySupport {
    const val PERMIT = "M10_B1B2_ISOLATED_V1"
    data class Run(val context: Context, val directory: File, val runId: String,
        val host: JpegWorkerIdentity, val boot: String, val method: String)

    fun begin(context: Context, method: String): Run {
        val args = InstrumentationRegistry.getArguments()
        check(args.getString("class") == method) { "Only one exact permitted method may run" }
        check(args.getString("m10SafetyPermit") == PERMIT) { "An explicit isolated-safety permit is required" }
        check(listOf("action", "command", "case", "fault").all { args.getString(it) == null })
        check(context.packageName == "com.cooper.lithic.debug" && Build.VERSION.SDK_INT in setOf(26, 36))
        check(args.getString("m10SafetyExpectedApi") == Build.VERSION.SDK_INT.toString())
        val id = requireNotNull(args.getString("m10SafetyRunId"))
        check(UUID.fromString(id).toString() == id && id.length == 36)
        check(JpegProcessRole.role(context.packageName) == JpegProcessRole.Role.MAIN)
        val host = JpegProcessRole.identity()
        check(host.pid == Process.myPid() && host.uid == Process.myUid() && host.startTicks > 0)
        val boot = JpegBootIdentity.current(context)
        val base = File(context.noBackupFilesDir.canonicalFile, "m10-b1b2-safety")
        if (!base.exists()) { check(base.mkdir()); Os.chmod(base.path, 448) }
        privateDirectory(base)
        val directory = File(base, id)
        check(directory.mkdir()) { "Do not overwrite a prior safety attempt" }
        Os.chmod(directory.path, 448) // 0700; only this isolated synthetic test's evidence.
        val run = Run(context, directory, id, host, boot, method)
        write(run, "attempt.json", JSONObject().put("protocol", "M10_B1B2_SAFETY_ATTEMPT_V1")
            .put("method", method).put("runId", id).put("api", Build.VERSION.SDK_INT)
            .put("hostPid", host.pid).put("hostUid", host.uid).put("hostStartTicks", host.startTicks)
            .put("bootId", boot).put("status", "ATTEMPT_ISSUED")
            .put("roomOrFormalCoverCommitted", false))
        return run
    }

    fun write(run: Run, name: String, value: JSONObject) {
        check(name.matches(Regex("[a-z0-9-]+\\.json")))
        privateDirectory(run.directory)
        val data = (value.toString() + "\n").toByteArray(Charsets.UTF_8)
        check(data.size <= 1024 * 1024)
        FileChannel.open(File(run.directory, name).toPath(), StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS).use { channel ->
            val bytes = ByteBuffer.wrap(data)
            while (bytes.hasRemaining()) check(channel.write(bytes) > 0)
            channel.force(true)
        }
        val fd = Os.open(run.directory.path, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW, 0)
        try { Os.fsync(fd) } finally { Os.close(fd) }
    }

    fun assertHost(run: Run) {
        check(JpegBootIdentity.current(run.context) == run.boot)
        check(JpegProcessRole.identity() == run.host)
        check(JpegProcessRole.role(run.context.packageName) == JpegProcessRole.Role.MAIN)
    }

    fun noWorker(context: Context): JSONObject {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val records = requireNotNull(manager.runningAppProcesses) { "Running-process observation unavailable" }
        val workers = records.filter { it.uid == Process.myUid() && it.processName == context.packageName + ":jpeg_worker" }
        check(workers.isEmpty()) { "An existing or late Worker is not an idle preflight" }
        return JSONObject().put("status", "ABSENT_AT_OBSERVATION").put("source", "ACTIVITY_MANAGER_SAME_UID")
            .put("workerRows", 0).put("externalSupervisorProcVerification", "REQUIRED_SEPARATELY")
    }

    /** Metadata only. Only Stage V2 entries contribute to the hash; no business/image byte is opened. */
    fun stages(context: Context): JSONObject {
        val root = context.noBackupFilesDir.canonicalFile
        var visited = 0
        val rows = mutableListOf<String>()
        var roots = 0
        fun visit(file: File, depth: Int, inStage: Boolean) {
            check(depth <= 16 && ++visited <= 10_000) { "Metadata scope exceeded" }
            val info = Os.lstat(file.path)
            check(!OsConstants.S_ISLNK(info.st_mode) && file.canonicalFile == file.absoluteFile)
            val selected = inStage || file.name == ".m10-jpeg-v2"
            if (!inStage && selected) roots++
            if (selected) rows += "${file.relativeTo(root).invariantSeparatorsPath}|${info.st_dev}|${info.st_ino}|${info.st_mode}|${info.st_uid}|${info.st_size}|${info.st_mtime}"
            if (OsConstants.S_ISDIR(info.st_mode)) {
                for (child in requireNotNull(file.listFiles()).sortedBy { it.name }) {
                    if (file == root && child.name == "m10-b1b2-safety") continue
                    visit(child, depth + 1, selected)
                }
            } else check(OsConstants.S_ISREG(info.st_mode))
        }
        visit(root, 0, false)
        val raw = rows.sorted().joinToString("\n").toByteArray(Charsets.UTF_8)
        val sha = MessageDigest.getInstance("SHA-256").digest(raw).joinToString("") { "%02x".format(it.toInt() and 255) }
        return JSONObject().put("stageRoots", roots).put("stageMetadataRows", rows.size)
            .put("stageMetadataSHA256", sha).put("businessOrImageBytesRead", false)
            .put("evidenceNamespaceExcluded", "m10-b1b2-safety")
    }

    private fun privateDirectory(file: File) {
        val stat = Os.lstat(file.path)
        check(OsConstants.S_ISDIR(stat.st_mode) && stat.st_uid == Process.myUid() &&
            stat.st_mode and 63 == 0 && file.canonicalFile == file.absoluteFile)
    }
}
