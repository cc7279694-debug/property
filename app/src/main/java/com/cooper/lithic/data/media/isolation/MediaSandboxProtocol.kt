package com.cooper.lithic.data.media.isolation

import android.os.Build
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import java.io.FileDescriptor
import java.security.MessageDigest
import java.util.UUID

internal enum class IsolationProbeMode { ECHO, HOLD_UNTIL_RETIRE }

/** Phase one accepts a fixed tiny synthetic payload, never a user image or a business path. */
internal object MediaSandboxProtocol {
    const val VERSION = 1
    const val OPEN = 1
    const val COMPUTE = 2
    const val REVOKE = 3
    const val HELLO = 11
    const val GUARD_HELD = 12
    const val RESULT = 13
    const val ERROR = 14
    val inputBytes: ByteArray get() = "LITHIC_C6_ISOLATION_V1\n".toByteArray(Charsets.US_ASCII)
    val outputBytes: ByteArray get() = "ISOLATED_SYNTHETIC_OK\n".toByteArray(Charsets.US_ASCII)
    fun sha(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    fun validNonce(value: String?): Boolean = value != null && runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)
    fun packet(nonce: String) = Bundle().apply { putInt("version", VERSION); putString("nonce", nonce) }
    fun matches(bundle: Bundle, nonce: String): Boolean = bundle.getInt("version", -1) == VERSION && bundle.getString("nonce") == nonce
    fun send(peer: Messenger, what: Int, data: Bundle, reply: Messenger? = null) {
        peer.send(Message.obtain().apply { this.what = what; this.data = data; replyTo = reply })
    }
    @Suppress("DEPRECATION")
    fun fd(bundle: Bundle, key: String): ParcelFileDescriptor = requireNotNull(bundle.getParcelable<ParcelFileDescriptor>(key))
    fun identity(fd: FileDescriptor, hostUid: Int, mode: Int, expectedSize: Long? = null) {
        val s = Os.fstat(fd)
        require(OsConstants.S_ISREG(s.st_mode) && s.st_uid == hostUid && s.st_nlink == 1L) { "FD ownership/type rejected" }
        // Public fcntlInt is API30+. On API26 the fixed probe proves capabilities through
        // an actual rejected input write, a successful output write, and an exclusive writable lock.
        if (Build.VERSION.SDK_INT >= 30)
            require(Os.fcntlInt(fd, OsConstants.F_GETFL, 0) and OsConstants.O_ACCMODE == mode) { "FD access mode rejected" }
        if (expectedSize != null) require(s.st_size == expectedSize) { "FD size rejected" }
    }
    fun sameIdentity(fd: FileDescriptor, bundle: Bundle, prefix: String) {
        val s = Os.fstat(fd)
        require(s.st_dev == bundle.getLong(prefix + "Dev", -1) && s.st_ino == bundle.getLong(prefix + "Ino", -1))
    }
    fun putIdentity(bundle: Bundle, prefix: String, fd: FileDescriptor) {
        val s = Os.fstat(fd); bundle.putLong(prefix + "Dev", s.st_dev); bundle.putLong(prefix + "Ino", s.st_ino)
    }
}
