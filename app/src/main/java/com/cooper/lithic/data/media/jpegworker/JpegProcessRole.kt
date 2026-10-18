package com.cooper.lithic.data.media.jpegworker

import android.app.Application
import android.os.Build
import android.os.Process
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/** Exact process roles. Same UID is a code-responsibility boundary, not a hostile-native sandbox. */
internal object JpegProcessRole {
    enum class Role { MAIN, WORKER, UNKNOWN }
    fun role(packageName: String): Role = try {
        val proc = readCmdline(File("/proc/self/cmdline"))
        if (Build.VERSION.SDK_INT >= 28 && Application.getProcessName() != proc) Role.UNKNOWN else when (proc) {
            packageName -> Role.MAIN
            "$packageName:jpeg_worker" -> Role.WORKER
            else -> Role.UNKNOWN
        }
    } catch (_: Exception) { Role.UNKNOWN }

    // API26/27 onBind must not wait for an asynchronous initializer. This bounded identity probe
    // is the previously approved exception; it is not a global READY or storage-owner assertion.
    fun readCmdline(file: File): String = FileInputStream(file).use { input ->
        val bytes = ByteArray(256)
        val count = input.read(bytes)
        val end = (0 until maxOf(count, 0)).firstOrNull { bytes[it] == 0.toByte() }
            ?: throw IOException("Unterminated process identity")
        if (end == 0 || bytes.take(end).any { it.toInt() !in 33..126 }) throw IOException("Invalid process identity")
        String(bytes, 0, end, Charsets.US_ASCII)
    }
    fun identity(pid: Int = Process.myPid()): JpegWorkerIdentity {
        if (pid <= 0) throw IOException("Invalid process PID")
        val stat = bounded(File("/proc/$pid/stat"), 4096)
        val close = stat.lastIndexOf(')')
        if (close <= 0 || !stat.startsWith("$pid (")) throw IOException("Process identity shape")
        val fields = stat.substring(close + 2).trim().split(Regex("\\s+"))
        val start = fields.getOrNull(19)?.toLongOrNull()?.takeIf { it > 0 } ?: throw IOException("Process start missing")
        val uid = Math.toIntExact(Os.stat("/proc/$pid").st_uid.toLong())
        return JpegWorkerIdentity(pid, uid, start)
    }
    fun instanceAbsent(identity: JpegWorkerIdentity): Boolean = try {
        // A different process instance is not the retired worker. A permission/read error is UNKNOWN.
        this.identity(identity.pid) != identity
    } catch (error: ErrnoException) { if (error.errno == OsConstants.ENOENT) true else throw error }
    catch (error: java.io.FileNotFoundException) {
        // Missing Java proc read alone may also mean access denial; verify ENOENT through stat.
        try { Os.stat("/proc/${identity.pid}"); throw error }
        catch (stat: ErrnoException) { if (stat.errno == OsConstants.ENOENT) true else throw stat }
    }
    fun bounded(file: File, limit: Int): String = FileInputStream(file).use { input ->
        val bytes = ByteArray(limit + 1)
        var total = 0
        while (total < bytes.size) { val n=input.read(bytes,total,bytes.size-total); if(n<0)break; total+=n }
        if(total>limit)throw IOException("Metadata bound")
        String(bytes,0,total,Charsets.US_ASCII)
    }
}

internal data class JpegWorkerIdentity(val pid: Int, val uid: Int, val startTicks: Long)
