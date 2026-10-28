package com.cooper.lithic.data.media.isolation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

internal data class IsolationProbeObservation(
    val session: IsolationSessionSnapshot,
    val workerPid: Int?, val workerUid: Int?, val workerRole: String?,
    val workerOwnerInitialized: Boolean?, val workerCanReadHostDirectory: Boolean?,
    val inputWriteRejected: Boolean?, val guardHeldConfirmed: Boolean,
    val outputMatches: Boolean, val connectionCount: Int, val filesDirectory: File, val detail: String,
    val journalPersisted: Boolean = false,
)

/** Dormant, fixed-synthetic phase-one channel. No UI, image input, native library or write capability. */
internal class MediaIsolationChannel(private val context: Context) {
    @Volatile var lastObservation: IsolationProbeObservation? = null
        private set

    suspend fun runSyntheticProbe(
        mode: IsolationProbeMode = IsolationProbeMode.ECHO,
        timeoutMillis: Long = 5_000,
        onComputing: (() -> Unit)? = null,
    ): IsolationProbeObservation = withContext(Dispatchers.IO) {
        require(timeoutMillis in 100..10_000)
        if (!admission.tryLock()) return@withContext refused("ADMISSION_BUSY")
        try {
            if (quarantined.get() || globalRpc.get() != 0 || globalBindingCalls.get() != 0) return@withContext refused("ADMISSION_UNKNOWN")
            val files = try { IsolationProbeFiles.create(context) }
                catch (_: Exception) { return@withContext refused("ADMISSION_UNKNOWN") }
            runSession(files, mode, timeoutMillis, onComputing)
        } finally { admission.unlock() }
    }
    private fun refused(reason: String): IsolationProbeObservation {
        val session = IsolationSessionState(UUID.randomUUID().toString(), Process.myUid())
        session.revoke(reason); session.retire()
        return IsolationProbeObservation(session.snapshot(), null, null, null, null, null, null,
            false, false, 0, File(context.noBackupFilesDir, "c6-isolation-stage1"), reason).also { lastObservation = it }
    }
    private suspend fun runSession(files: IsolationProbeFiles, mode: IsolationProbeMode, budget: Long, onComputing: (() -> Unit)?): IsolationProbeObservation {
        val state = IsolationSessionState(files.nonce, Process.myUid())
        val finishing = AtomicBoolean(false)
        val bound = AtomicBoolean(false)
        val unboundConfirmed = AtomicBoolean(false)
        val connections = AtomicInteger(0)
        val hostTransfersClosed = AtomicBoolean(true)
        val callbackThread = HandlerThread("media-isolation-replies").also { it.start() }
        var guardObserver: FileOutputStream? = null
        var worker: Messenger? = null
        var binder: IBinder? = null
        var workerPid: Int? = null
        var workerRole: String? = null
        var ownerInitialized: Boolean? = null
        var directoryReadable: Boolean? = null
        var writeRejected: Boolean? = null
        var guardHeldConfirmed = false
        var outputMatches = false
        var detail = "UNKNOWN"
        val deadline = SystemClock.elapsedRealtime() + budget
        val death = IBinder.DeathRecipient { state.recordBinderDeath() }
        lateinit var connection: ServiceConnection

        fun sendAsync(what: Int, data: Bundle) {
            globalRpc.incrementAndGet()
            rpc.execute {
                try { worker?.let { MediaSandboxProtocol.send(it, what, data) } ?: state.revoke("NO_ENDPOINT") }
                catch (_: Exception) { state.revoke("RPC_FAILED") }
                finally { globalRpc.decrementAndGet() }
            }
        }
        fun unbindOnce() {
            if (!bound.compareAndSet(true, false)) return
            globalBindingCalls.incrementAndGet()
            bindings.execute {
                try { context.unbindService(connection); unboundConfirmed.set(true) }
                catch (_: Exception) { quarantined.set(true); state.revoke("UNBIND_UNKNOWN") }
                finally { globalBindingCalls.decrementAndGet() }
            }
        }
        val replies = Messenger(object : Handler(callbackThread.looper) {
            override fun handleMessage(msg: Message) {
                try {
                    val sender = msg.sendingUid
                    val known = state.snapshot().workerUid
                    if (sender <= 0 || sender == Process.myUid() || (msg.what != MediaSandboxProtocol.HELLO && sender != known)) {
                        state.revoke("BAD_SENDER_UID"); return
                    }
                    val b = msg.data
                    if (!MediaSandboxProtocol.matches(b, files.nonce)) { state.revoke("BAD_NONCE_OR_VERSION"); return }
                    when (msg.what) {
                        MediaSandboxProtocol.HELLO -> {
                            if (!state.hello(msg.sendingUid, b.getInt("version"), files.nonce)) return
                            workerPid = b.getInt("pid")
                            workerRole = b.getString("role")
                            ownerInitialized = b.getBoolean("ownerInitialized", true)
                            directoryReadable = b.getBoolean("hostDirectoryReadable", true)
                            if (workerRole != "SANDBOX" || ownerInitialized != false || directoryReadable != false)
                                state.revoke("SANDBOX_STARTUP_REJECTED")
                        }
                        MediaSandboxProtocol.GUARD_HELD -> {
                            if (!state.guardHeld(msg.sendingUid, b.getInt("version"), files.nonce)) return
                            val observer = requireNotNull(guardObserver)
                            val same = android.system.Os.fstat(observer.fd)
                            val path = android.system.Os.lstat(files.guard.path)
                            check(same.st_dev == path.st_dev && same.st_ino == path.st_ino)
                            val unexpected = observer.channel.tryLock(0, Long.MAX_VALUE, false)
                            if (unexpected != null) { unexpected.release(); state.revoke("GUARD_NOT_HELD"); return }
                            guardHeldConfirmed = true
                            if (state.startComputing()) {
                                onComputing?.invoke()
                                sendAsync(MediaSandboxProtocol.COMPUTE, MediaSandboxProtocol.packet(files.nonce))
                            }
                        }
                        MediaSandboxProtocol.RESULT -> {
                            writeRejected = b.getBoolean("inputWriteRejected", false)
                            if (writeRejected != true || b.getString("outputSha") != MediaSandboxProtocol.sha(MediaSandboxProtocol.outputBytes)) {
                                state.revoke("RESULT_REJECTED"); return
                            }
                            if (!state.candidate(msg.sendingUid, b.getInt("version"), files.nonce,
                                b.getBoolean("inputClosed", false), b.getBoolean("outputClosed", false))) {
                                state.revoke("RESULT_SEQUENCE_REJECTED"); return
                            }
                            state.retire() // preserve the candidate before unbinding/observing death
                        }
                        else -> state.revoke("WORKER_ERROR_OR_MESSAGE")
                    }
                } catch (_: Exception) { state.revoke("INVALID_REPLY") }
            }
        })
        connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                if (connections.incrementAndGet() != 1 || finishing.get()) {
                    quarantined.set(true)
                    state.revoke("LATE_OR_DUPLICATE_CONNECTION")
                    rpc.execute { runCatching { files.record(IsolationJournalState.UNKNOWN) } }
                    unbindOnce(); return
                }
                try {
                    check(name.className == MediaSandboxService::class.java.name)
                    binder = service; service.linkToDeath(death, 0); worker = Messenger(service)
                    globalRpc.incrementAndGet()
                    rpc.execute {
                        val transfers = mutableListOf<ParcelFileDescriptor>()
                        try {
                            if (!finishing.get()) {
                                hostTransfersClosed.set(false)
                                val input = files.openInput().also(transfers::add)
                                val output = files.openOutput().also(transfers::add)
                                val guard = files.openGuard().also(transfers::add)
                                val b = MediaSandboxProtocol.packet(files.nonce).apply {
                                    putParcelable("input", input); putParcelable("output", output); putParcelable("guard", guard)
                                    putString("mode", mode.name); putString("sha", MediaSandboxProtocol.sha(MediaSandboxProtocol.inputBytes))
                                    MediaSandboxProtocol.putIdentity(this, "input", input.fileDescriptor)
                                    MediaSandboxProtocol.putIdentity(this, "output", output.fileDescriptor)
                                    MediaSandboxProtocol.putIdentity(this, "guard", guard.fileDescriptor)
                                }
                                if (!finishing.get()) MediaSandboxProtocol.send(requireNotNull(worker), MediaSandboxProtocol.OPEN, b, replies)
                            }
                        } catch (_: Exception) { state.revoke("OPEN_RPC_FAILED") }
                        finally {
                            var closed = true
                            for (fd in transfers) try { fd.close() } catch (_: Exception) { closed = false }
                            hostTransfersClosed.set(closed)
                            globalRpc.decrementAndGet()
                        }
                    }
                } catch (_: Exception) { state.revoke("CONNECTION_FAILED") }
            }
            override fun onServiceDisconnected(name: ComponentName) { if (!finishing.get()) state.revoke("SERVICE_DISCONNECTED") }
            override fun onBindingDied(name: ComponentName) { if (!finishing.get()) state.revoke("BINDING_DIED") }
            override fun onNullBinding(name: ComponentName) { state.revoke("NULL_BINDING") }
        }
        try {
            guardObserver = files.observeGuard() // independent open, never dup of the transferred guard
            globalBindingCalls.incrementAndGet()
            bindings.execute {
                try {
                    val accepted = context.bindService(Intent(context, MediaSandboxService::class.java), connection, Context.BIND_AUTO_CREATE)
                    if (accepted) { bound.set(true); if (finishing.get()) unbindOnce() }
                    else state.revoke("BIND_REJECTED")
                } catch (_: Exception) { state.revoke("BIND_FAILED") }
                finally { globalBindingCalls.decrementAndGet() }
            }
            while (SystemClock.elapsedRealtime() < deadline) {
                val s = state.snapshot()
                if (s.phase == IsolationPhase.RETIRING || s.revoked) break
                delay(10)
            }
            if (state.snapshot().phase != IsolationPhase.RETIRING && !state.snapshot().revoked) state.revoke("TIMEOUT")
        } catch (cancelled: CancellationException) {
            state.revoke("CANCELLED"); throw cancelled
        } catch (_: Exception) { state.revoke("HOST_FAILURE") }
        finally {
            withContext(NonCancellable + Dispatchers.IO) {
                state.retire(); finishing.set(true)
                if (state.snapshot().revoked && worker != null) sendAsync(MediaSandboxProtocol.REVOKE, MediaSandboxProtocol.packet(files.nonce))
                unbindOnce()
                val finishDeadline = SystemClock.elapsedRealtime() + 3_000
                while (SystemClock.elapsedRealtime() < finishDeadline) {
                    if (state.snapshot().closure.binderDead && globalRpc.get() == 0 && globalBindingCalls.get() == 0) break
                    delay(10)
                }
                if (state.snapshot().closure.binderDead && guardHeldConfirmed) {
                    try {
                        val observer = requireNotNull(guardObserver)
                        val s = android.system.Os.fstat(observer.fd); val path = android.system.Os.lstat(files.guard.path)
                        check(s.st_dev == path.st_dev && s.st_ino == path.st_ino)
                        observer.channel.tryLock(0, Long.MAX_VALUE, false)?.use { state.recordGuardReturned() }
                    } catch (_: Exception) { state.revoke("GUARD_RETURN_UNKNOWN") }
                }
                var observerClosed = true
                try { guardObserver?.close() } catch (_: Exception) { observerClosed = false }
                val beforeClose = state.snapshot()
                if (beforeClose.acceptedResult && !beforeClose.revoked && beforeClose.closure.binderDead && beforeClose.closure.guardReturned) {
                    outputMatches = try { files.outputMatches() } catch (_: Exception) { false }
                    if (!outputMatches) state.revoke("OUTPUT_MISMATCH")
                }
                if (observerClosed && hostTransfersClosed.get() && globalRpc.get() == 0) state.recordHostFdsClosed()
                if (globalRpc.get() == 0 && globalBindingCalls.get() == 0 && unboundConfirmed.get() && !quarantined.get()) state.recordRpcQuiescent()
                val end = state.snapshot()
                detail = end.revocationReason ?: if (outputMatches) "SYNTHETIC_PROBE_CLOSED" else "RESOURCE_CLOSE_UNKNOWN"
                var journalClosed = true
                try { files.record(if (!quarantined.get() && end.outcome != IsolationOutcome.UNKNOWN && (outputMatches || end.revoked)) IsolationJournalState.CLOSED else IsolationJournalState.UNKNOWN) }
                catch (_: Exception) { journalClosed = false; detail = "JOURNAL_CLOSE_UNKNOWN" }
                if (!journalClosed || end.outcome == IsolationOutcome.UNKNOWN) quarantined.set(true)
                lastObservation = IsolationProbeObservation(if (journalClosed && !quarantined.get()) end else end.copy(outcome = IsolationOutcome.UNKNOWN), workerPid, end.workerUid, workerRole, ownerInitialized,
                    directoryReadable, writeRejected, guardHeldConfirmed, outputMatches, connections.get(), files.directory, detail, journalPersisted = journalClosed)
                binder?.let { runCatching { it.unlinkToDeath(death, 0) } }
                callbackThread.quitSafely()
            }
        }
        return requireNotNull(lastObservation)
    }
    companion object {
        private val admission = Mutex()
        private val quarantined = AtomicBoolean(false)
        private val globalRpc = AtomicInteger(0)
        private val globalBindingCalls = AtomicInteger(0)
        private val rpc = Executors.newSingleThreadExecutor { Thread(it, "media-isolation-rpc").apply { isDaemon = true } }
        private val bindings = Executors.newSingleThreadExecutor { Thread(it, "media-isolation-binding").apply { isDaemon = true } }
    }
}
