package com.cooper.lithic.data.media.isolation

import android.content.Context
import android.os.ParcelFileDescriptor
import android.os.Process
import android.system.Os
import android.system.OsConstants
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.util.UUID

/** Host-owned, synthetic-only workspace. Unknown records/files are preserved, never swept. */
internal class IsolationProbeFiles private constructor(val directory: File, val nonce: String, private val journal: AtomicFile) {
    val input = File(directory, "input.synthetic")
    val output = File(directory, "output.synthetic")
    val guard = File(directory, "guard")
    @Synchronized fun record(state: IsolationJournalState) {
        val file = journal.baseFile
        if (file.exists()) requireRegular(file)
        val stream = journal.startWrite()
        try {
            stream.write(("C6_ISOLATION_1\n" + nonce + "\n" + state.name + "\n").toByteArray(Charsets.US_ASCII))
            journal.finishWrite(stream)
        } catch (error: Exception) { journal.failWrite(stream); throw error }
    }
    fun openInput() = openExisting(input, OsConstants.O_RDONLY)
    fun openOutput() = openExisting(output, OsConstants.O_RDWR)
    fun openGuard() = openExisting(guard, OsConstants.O_RDWR)
    fun observeGuard(): FileOutputStream {
        requireRegular(guard)
        val before = Os.lstat(guard.path)
        val fd = Os.open(guard.path, OsConstants.O_RDWR or OsConstants.O_NOFOLLOW or CLOSE_ON_EXEC or OsConstants.O_NONBLOCK, 0)
        try {
            MediaSandboxProtocol.identity(fd, Process.myUid(), OsConstants.O_RDWR, 0)
            val actual = Os.fstat(fd)
            require(actual.st_dev == before.st_dev && actual.st_ino == before.st_ino)
            return FileOutputStream(fd) // genuinely independent open; this stream owns the FD
        } catch (error: Exception) { Os.close(fd); throw error }
    }
    fun outputMatches(): Boolean {
        requireRegular(output)
        if (output.length() != MediaSandboxProtocol.outputBytes.size.toLong()) return false
        return output.readBytes().contentEquals(MediaSandboxProtocol.outputBytes)
    }
    companion object {
        // Android Linux ABI flag, as in the frozen backup/cover reader; Java field is API27+.
        private const val CLOSE_ON_EXEC = 0x80000
        fun create(context: Context): IsolationProbeFiles {
            val parent = context.noBackupFilesDir.canonicalFile
            requireDirectory(parent)
            val root = File(parent, "c6-isolation-stage1")
            val fresh = !root.exists()
            if (fresh) check(root.mkdir())
            requireDirectory(root)
            val control = File(root, "session.txt")
            val atomic = AtomicFile(control)
            if (!fresh) {
                // A pending AtomicFile backup or unknown/missing control cannot authorize another run.
                if (File(root, "session.txt.bak").exists() || File(root, "session.txt.new").exists()) throw IOException("Previous isolation journal UNKNOWN")
                requireRegular(control)
                require(control.length() in 1..4096)
                val lines = control.readText(Charsets.US_ASCII).lines()
                require(lines.size == 4 && lines[0] == "C6_ISOLATION_1" && MediaSandboxProtocol.validNonce(lines[1]))
                require(lines[2] == IsolationJournalState.CLOSED.name) { "Previous isolation session UNKNOWN" }
                // This stage authorizes one probe per namespace; recorded CLOSED is not a new run permit.
                throw IOException("Recorded isolation session requires fresh explicit fixture authorization")
            }
            val nonce = UUID.randomUUID().toString()
            val directory = File(root, nonce); check(directory.mkdir()); requireDirectory(directory)
            val files = IsolationProbeFiles(directory, nonce, atomic)
            files.record(IsolationJournalState.ADMITTED) // durable before any consumer/Service
            createFile(files.input, MediaSandboxProtocol.inputBytes)
            createFile(files.output, byteArrayOf())
            createFile(files.guard, byteArrayOf())
            return files
        }
        private fun createFile(file: File, bytes: ByteArray) {
            val fd = Os.open(file.path, OsConstants.O_CREAT or OsConstants.O_EXCL or OsConstants.O_WRONLY or OsConstants.O_NOFOLLOW or CLOSE_ON_EXEC, 384)
            FileOutputStream(fd).use { it.write(bytes); it.fd.sync() }
            requireRegular(file)
        }
        private fun openExisting(file: File, flags: Int): ParcelFileDescriptor {
            requireRegular(file)
            val before = Os.lstat(file.path)
            val fd = Os.open(file.path, flags or OsConstants.O_NOFOLLOW or CLOSE_ON_EXEC or OsConstants.O_NONBLOCK, 0)
            try {
                val opened = Os.fstat(fd)
                require(opened.st_dev == before.st_dev && opened.st_ino == before.st_ino)
                return ParcelFileDescriptor.dup(fd)
            } finally { Os.close(fd) }
        }
        private fun requireDirectory(file: File) {
            val s = Os.lstat(file.path)
            require(OsConstants.S_ISDIR(s.st_mode) && s.st_uid == Process.myUid() && file.canonicalFile == file.absoluteFile)
        }
        private fun requireRegular(file: File) {
            val s = Os.lstat(file.path)
            require(OsConstants.S_ISREG(s.st_mode) && s.st_uid == Process.myUid() && s.st_nlink == 1L && file.canonicalFile == file.absoluteFile)
        }
    }
}
