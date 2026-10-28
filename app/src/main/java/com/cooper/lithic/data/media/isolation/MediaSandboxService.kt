package com.cooper.lithic.data.media.isolation

import android.app.Service
import android.content.Intent
import android.os.*
import android.system.Os
import android.system.OsConstants
import com.cooper.lithic.app.AppProcessRole
import com.cooper.lithic.app.LithicApplication
import java.io.File
import java.nio.channels.FileLock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** No image/native parser, Owner, storage facade, scheduling or user entry point exists here. */
class MediaSandboxService : Service() {
    private lateinit var control: HandlerThread
    private lateinit var endpoint: Messenger
    private val lastBindingReleased = AtomicBoolean(false)
    private val watchdog = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var used = false
    private var nonce: String? = null
    private var callback: Messenger? = null
    private var hostUid = -1
    private var source: ParcelFileDescriptor? = null
    private var sink: ParcelFileDescriptor? = null
    private var guardStream: ParcelFileDescriptor.AutoCloseOutputStream? = null
    private var guardLock: FileLock? = null
    private var computing = false
    @Volatile private var revoked = false
    private var mode = IsolationProbeMode.ECHO
    private val neverReturns = CountDownLatch(1)

    override fun onCreate() {
        super.onCreate()
        val app = application as? LithicApplication
        check(app?.processRole == AppProcessRole.SANDBOX && !app.businessOwnerInitialized) { "Sandbox startup rejected" }
        hostUid = applicationInfo.uid
        check(Process.myUid() != hostUid) { "Isolated UID required" }
        control = HandlerThread("media-isolation-control").also { it.start() }
        endpoint = Messenger(object : Handler(control.looper) {
            override fun handleMessage(msg: Message) { receive(msg) }
        })
        // Self-termination only after the framework reports the last binding released.
        // No deadline/PID guess ever authorizes terminating another process.
        watchdog.scheduleWithFixedDelay({
            if (lastBindingReleased.get()) Process.killProcess(Process.myPid())
        }, 100, 100, TimeUnit.MILLISECONDS)
    }
    override fun onBind(intent: Intent): IBinder? {
        lastBindingReleased.set(false)
        // Rebinding is not a fresh session permit; the Host must observe NULL/UNKNOWN and unbind.
        return if (used) null else endpoint.binder
    }
    override fun onUnbind(intent: Intent): Boolean { lastBindingReleased.set(true); return false }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { stopSelf(startId); return START_NOT_STICKY }
    override fun onDestroy() {
        // onDestroy/stopSelf do not prove unbinding or permit an exit/restart loop.
        super.onDestroy()
    }
    private fun receive(msg: Message) {
        if (msg.sendingUid != hostUid) return // kernel Messenger identity, captured before async work
        try {
            when (msg.what) {
                MediaSandboxProtocol.OPEN -> open(msg)
                MediaSandboxProtocol.COMPUTE -> {
                    check(MediaSandboxProtocol.matches(msg.data, requireNotNull(nonce)) && !revoked && !computing)
                    computing = true
                    Thread({ compute() }, "media-isolation-synthetic").start()
                }
                MediaSandboxProtocol.REVOKE -> if (MediaSandboxProtocol.matches(msg.data, requireNotNull(nonce))) revoked = true
                else -> throw IllegalArgumentException("Unknown isolation message")
            }
        } catch (_: Exception) {
            revoked = true
            runCatching { reply(MediaSandboxProtocol.ERROR, MediaSandboxProtocol.packet(nonce ?: "")) }
        }
    }
    private fun open(msg: Message) {
        check(!used && !lastBindingReleased.get()); used = true
        val b = msg.data
        check(b.getInt("version", -1) == MediaSandboxProtocol.VERSION)
        val key = b.getString("nonce"); check(MediaSandboxProtocol.validNonce(key)); nonce = key
        callback = requireNotNull(msg.replyTo)
        val app = application as LithicApplication
        reply(MediaSandboxProtocol.HELLO, MediaSandboxProtocol.packet(requireNotNull(key)).apply {
            putInt("pid", Process.myPid()); putString("role", app.processRole.name)
            putBoolean("ownerInitialized", app.businessOwnerInitialized)
            putBoolean("hostDirectoryReadable", File(applicationInfo.dataDir).canRead())
        })
        source = MediaSandboxProtocol.fd(b, "input")
        sink = MediaSandboxProtocol.fd(b, "output")
        val guard = MediaSandboxProtocol.fd(b, "guard")
        // Adopt guard ownership once: no extra open/dup/close in this process may release its fcntl lease.
        guardStream = ParcelFileDescriptor.AutoCloseOutputStream(guard)
        MediaSandboxProtocol.identity(requireNotNull(source).fileDescriptor, hostUid, OsConstants.O_RDONLY, MediaSandboxProtocol.inputBytes.size.toLong())
        MediaSandboxProtocol.identity(requireNotNull(sink).fileDescriptor, hostUid, OsConstants.O_RDWR, 0)
        MediaSandboxProtocol.identity(guard.fileDescriptor, hostUid, OsConstants.O_RDWR, 0)
        for ((prefix, fd) in listOf("input" to requireNotNull(source).fileDescriptor, "output" to requireNotNull(sink).fileDescriptor, "guard" to guard.fileDescriptor))
            MediaSandboxProtocol.sameIdentity(fd, b, prefix)
        check(b.getString("sha") == MediaSandboxProtocol.sha(MediaSandboxProtocol.inputBytes))
        mode = IsolationProbeMode.valueOf(requireNotNull(b.getString("mode")))
        guardLock = requireNotNull(requireNotNull(guardStream).channel.tryLock(0, Long.MAX_VALUE, false))
        reply(MediaSandboxProtocol.GUARD_HELD, MediaSandboxProtocol.packet(requireNotNull(key)))
    }
    private fun compute() {
        try {
            if (mode == IsolationProbeMode.HOLD_UNTIL_RETIRE) {
                // Lifecycle test only, not a claim about native decoder cancellation.
                neverReturns.await()
            }
            val input = requireNotNull(source)
            val bytes = ByteArray(MediaSandboxProtocol.inputBytes.size)
            check(Os.pread(input.fileDescriptor, bytes, 0, bytes.size, 0) == bytes.size)
            check(bytes.contentEquals(MediaSandboxProtocol.inputBytes))
            var writeRejected = false
            try { Os.write(input.fileDescriptor, byteArrayOf(1), 0, 1) }
            catch (error: android.system.ErrnoException) { writeRejected = error.errno == OsConstants.EBADF || error.errno == OsConstants.EACCES }
            check(writeRejected)
            input.close(); source = null
            ParcelFileDescriptor.AutoCloseOutputStream(requireNotNull(sink)).use { out ->
                out.write(MediaSandboxProtocol.outputBytes); out.flush(); out.fd.sync()
            }; sink = null
            if (!revoked) reply(MediaSandboxProtocol.RESULT, MediaSandboxProtocol.packet(requireNotNull(nonce)).apply {
                putBoolean("inputClosed", true); putBoolean("outputClosed", true)
                putBoolean("inputWriteRejected", writeRejected)
                putString("outputSha", MediaSandboxProtocol.sha(MediaSandboxProtocol.outputBytes))
            })
        } catch (_: Exception) {
            runCatching { source?.close(); source = null; sink?.close(); sink = null }
            runCatching { reply(MediaSandboxProtocol.ERROR, MediaSandboxProtocol.packet(requireNotNull(nonce))) }
        }
        // Keep the guard lease until process exit, independent of RESULT/callback observations.
    }
    private fun reply(what: Int, b: Bundle) = MediaSandboxProtocol.send(requireNotNull(callback), what, b)
}
