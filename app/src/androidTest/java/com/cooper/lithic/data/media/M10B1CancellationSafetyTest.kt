package com.cooper.lithic.data.media

import android.content.Context
import android.content.ContextWrapper
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.data.media.jpegstage.JpegRecoveryVerifier
import com.cooper.lithic.data.media.jpegstage.JpegStageV2
import com.cooper.lithic.data.media.jpegworker.AndroidJpegRecoveryProbe
import com.cooper.lithic.data.media.jpegworker.JpegProcessRole
import com.cooper.lithic.data.media.jpegworker.JpegWorkerClient
import com.cooper.lithic.data.media.jpegworker.JpegWorkerIdentity
import com.cooper.lithic.data.media.jpegworker.JpegWorkerOutcome
import com.cooper.lithic.data.media.jpegworker.JpegWorkerRequest
import com.cooper.lithic.data.media.jpegworker.JpegWorkerResult
import com.cooper.lithic.data.media.jpegworker.JpegWorkerWire
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.OverlappingFileLockException
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

/** SOURCE_ONLY until a new test APK and isolated device are separately admitted.
 * Two separate bounded tests: pre-FD caller cancellation and delayed test-side return
 * after a real DELIVER Binder reply. Neither claims permanent kernel/native cancellation.
 */
@RunWith(AndroidJUnit4::class)
class M10B1CancellationSafetyTest {
    /** A real Binder reply has already returned. Only its test-side Java return is delayed.
     * This is not a permanently stuck kernel RPC or a native-cancellation experiment. */
    @Test fun delayed_real_delivery_return_keeps_binding_until_original_failed_session_closes(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val run = M10B1B2SafetySupport.begin(context, DELAYED_METHOD)
        val report = JSONObject().put("protocol", "M10_B1_DELAYED_REAL_RETURN_V1")
            .put("runId", run.runId).put("method", DELAYED_METHOD).put("status", "UNKNOWN")
            .put("scope", "REAL_DELIVER_RETURNED_TEST_JAVA_RETURN_DELAYED_ONLY")
            .put("kernelRpcPermanentlyInFlight", "NOT_TESTED").put("nativeComputeCancellation", "NOT_RUN")
            .put("workerFaultKill", "NOT_RUN").put("hostDeath", "NOT_RUN")
            .put("roomOrFormalCoverCommitted", false).put("resourceEnded", false)
        val wrapper = DelayedDeliveryContext(context)
        val registered = AtomicReference<JpegWorkerIdentity?>()
        val initialResult = AtomicReference<JpegWorkerResult?>()
        val initialFinished = CountDownLatch(1)
        val revoked = CountDownLatch(1)
        val lateFinished = CountDownLatch(1)
        val lateResult = AtomicReference<JpegWorkerResult?>()
        val lateCalls = AtomicInteger()
        val lateError = AtomicReference<Throwable?>()
        val callerError = AtomicReference<Throwable?>()
        val observationWindow = AtomicBoolean(true)
        var caller: kotlinx.coroutines.Job? = null
        try {
            M10B1B2SafetySupport.noWorker(context)
            val directory = File(context.noBackupFilesDir.canonicalFile, "m10-b1-delay-${run.runId}")
            check(directory.mkdir() && File(directory, "media").mkdir())
            val files = MediaFileProtocol(directory)
            val owner = JpegStageV2.Owner(directory.name, uuid(), uuid(), uuid(), uuid())
            val supervisor = JpegStageV2.SupervisorIdentity(run.boot, run.host.pid, run.host.uid, run.host.startTicks)
            val initial = M10JpegFixtures.bytes(M10JpegFixtures.COMPLETE).inputStream().use { files.jpeg.create(owner, it, supervisor) }
            val source = files.jpeg.sourceFile(owner)
            val output = files.jpeg.outputFile(owner)
            val guard = files.jpeg.guardFile(owner)
            val guardId = files.identity(guard)
            val request = JpegWorkerRequest(owner.generation, owner.assetId, owner.attemptId, owner.sessionId,
                owner.nonce, initial.source.bytes, initial.source.sha256, initial.source.device,
                initial.source.inode, guardId.device, guardId.inode)
            report.put("generation", owner.generation).put("asset", owner.assetId).put("attempt", owner.attemptId)
                .put("session", owner.sessionId).put("nonce", owner.nonce)
                .put("sourceSha256", initial.source.sha256).put("sourceBytes", initial.source.bytes)
            caller = launch(Dispatchers.Default) {
                try {
                    val result = JpegWorkerClient(wrapper).execute(source, output, guard, request,
                        beforeDelivery = { worker ->
                            check(registered.compareAndSet(null, worker))
                            check(worker.pid != run.host.pid && worker.uid == run.host.uid && JpegProcessRole.identity(worker.pid) == worker)
                            check(files.jpeg.registerConsumer(owner, worker.stage()).pinned)
                            files.jpeg.markDelivery(owner)
                        }, revoke = { reason ->
                            val state = files.jpeg.revoke(owner, reason)
                            check(state.state == JpegStageV2.State.REVOKED && !files.jpeg.isAuthorized(owner))
                            revoked.countDown()
                        }, abandonedResult = { result ->
                            try {
                                check(observationWindow.get()) { "Late observation outside its bounded window" }
                                check(lateCalls.incrementAndGet() == 1) { "Duplicate late closure handoff" }
                                check(result.detail == "LATE_FAILED_SESSION_QUIESCENCE")
                                check(result.outcome == JpegWorkerOutcome.TIMEOUT && !result.canPrepare)
                                check(result.workerIdentity == registered.get() && result.resultCount in 0..1)
                                check(result.hostFdsClosed && result.rpcQuiescent && result.originalSessionQuiescent &&
                                    result.connectionCleaned && result.binderDead && result.matchingProcessDead && result.guardReleased)
                                check(lateResult.compareAndSet(null, result))
                                M10B1B2SafetySupport.write(run, "late-closure.json", observation(result,
                                    M10B1ClosureObservations.Origin.LATE_ORIGINAL_QUIESCENCE))
                            } catch (error: Throwable) {
                                lateError.compareAndSet(null, error)
                                try { M10B1B2SafetySupport.write(run, "callback-error.json", JSONObject()
                                    .put("status", "UNKNOWN").put("errorType", error.javaClass.simpleName)) }
                                catch (evidenceError: Throwable) { error.addSuppressed(evidenceError) }
                                throw error
                            } finally { lateFinished.countDown() }
                        })
                    check(initialResult.compareAndSet(null, result))
                } catch (error: Throwable) { callerError.compareAndSet(null, error) }
                finally { initialFinished.countDown() }
            }
            check(wrapper.realDeliveryReturned.await(20, TimeUnit.SECONDS)) { "No actual completed DELIVER transaction" }
            check(wrapper.deliveryCalls.get() == 1 && wrapper.realDeliveryAccepted.get() && !wrapper.returnReleased.get())
            val actualWorker = requireNotNull(registered.get())
            check(JpegProcessRole.identity(actualWorker.pid) == actualWorker)
            // The Worker acquired this exact guard inside the actual DELIVER before returning.
            val guardHeld = RandomAccessFile(guard, "rw").use { file ->
                try { val lock = file.channel.tryLock(); if (lock == null) true else { lock.release(); false } }
                catch (_: OverlappingFileLockException) { true }
            }
            check(guardHeld)
            val originalSource = files.identity(source)
            check(originalSource.bytes == initial.source.bytes && originalSource.sha256 == initial.source.sha256)
            var gcRejected = false
            try { files.jpeg.safeCleanup(owner) } catch (_: IOException) { gcRejected = true }
            check(gcRejected && source.exists() && output.exists() && files.identity(source) == originalSource)
            M10B1B2SafetySupport.write(run, "in-flight-checkpoint.json", JSONObject()
                .put("actualKernelTransactAlreadyReturned", true).put("realReturnAccepted", true)
                .put("clientJavaReturnStillHeld", true).put("guardActuallyHeld", true)
                .put("activeCleanupRejected", true).put("sourceIdentityPreserved", true)
                .put("workerPid", actualWorker.pid).put("workerUid", actualWorker.uid)
                .put("workerStartTicks", actualWorker.startTicks).put("realReturnElapsedMs", wrapper.realReturnAt.get()))
            check(revoked.await(20, TimeUnit.SECONDS)) { "Fixed Client timeout did not durably revoke" }
            check(initialFinished.await(30, TimeUnit.SECONDS)) { "No bounded initial failed-session observation" }
            callerError.get()?.let { throw AssertionError("Original Client failed without structured observation", it) }
            val first = requireNotNull(initialResult.get())
            check(first.outcome == JpegWorkerOutcome.TIMEOUT && !first.canPrepare && first.workerIdentity == actualWorker)
            check(first.resultCount in 0..1 && !first.rpcQuiescent && !first.originalSessionQuiescent && !first.connectionCleaned)
            check(wrapper.unbindCalls.get() == 0 && !wrapper.returnReleased.get())
            check(!files.jpeg.isAuthorized(owner) && files.jpeg.snapshot(owner).pinned)
            M10B1B2SafetySupport.write(run, "initial-observation.json", observation(first,
                M10B1ClosureObservations.Origin.INITIAL).put("unbindCallsWhileReturnHeld", 0)
                .put("durableRevokeConfirmed", true).put("initialUnknownNotCleanupAuthority", true))
            wrapper.release.countDown()
            check(lateFinished.await(25, TimeUnit.SECONDS)) { "Original Session never independently closed" }
            lateError.get()?.let { throw AssertionError("Late original-session closure UNKNOWN", it) }
            val closed = requireNotNull(lateResult.get())
            check(wrapper.returnReleased.get() && wrapper.unbindCalls.get() == 1 && wrapper.unbindSucceeded.get() &&
                wrapper.unbindOnMain.get() && wrapper.connectionMapEmpty())
            check(closed.resultCount == first.resultCount) { "A result was accepted after the failure fence" }
            val probe = AndroidJpegRecoveryProbe(context)
            val live = JpegRecoveryVerifier.LiveQuiescence(owner, supervisor, actualWorker.stage(),
                closed.hostFdsClosed, closed.rpcQuiescent, closed.originalSessionQuiescent,
                closed.connectionCleaned, closed.binderDead, closed.matchingProcessDead, closed.guardReleased)
            files.jpeg.recoverNoResult(owner) { facts -> JpegRecoveryVerifier(probe).verify(facts, live) }
            val abandoned = files.jpeg.snapshot(owner)
            check(abandoned.state == JpegStageV2.State.ABANDONED && !abandoned.pinned && abandoned.outcome == null)
            check(!File(source.parentFile, "terminal.v2").exists() && !File(source.parentFile, "prepared.v2").exists())
            files.jpeg.safeCleanup(owner)
            check(!source.exists() && !output.exists())
            val closedRecords = source.parentFile!!.listFiles()!!.associate { it.name to files.identity(it) }
            var wrongSessionRejected = false
            try { files.jpeg.snapshot(owner.copy(sessionId = uuid())) } catch (_: IOException) { wrongSessionRejected = true }
            check(wrongSessionRejected)
            var duplicateVerifierCalls = 0
            check(files.jpeg.recoverNoResult(owner) { facts ->
                duplicateVerifierCalls++
                JpegRecoveryVerifier(probe).verify(facts, live)
            }.state == JpegStageV2.State.ABANDONED)
            check(duplicateVerifierCalls == 0)
            var failedReceiptRejected = false
            try { files.jpeg.complete(JpegStageV2.Completion(owner, actualWorker.stage(), JpegStageV2.Outcome.TIMEOUT,
                initial.source, resultCount = closed.resultCount, sourceFdClosed = closed.sourceFdClosed,
                outputFdClosed = closed.outputFdClosed, binderDead = closed.binderDead,
                matchingProcessDead = closed.matchingProcessDead, strictFinished = closed.strictFinished,
                decoderDestroyed = closed.decoderDestroyed, connectionCleaned = closed.connectionCleaned))
            } catch (_: IOException) { failedReceiptRejected = true }
            check(failedReceiptRejected && closedRecords == source.parentFile!!.listFiles()!!.associate { it.name to files.identity(it) })
            check(!files.jpeg.isAuthorized(owner))
            files.jpeg.admission()
            M10B1B2SafetySupport.assertHost(run)
            M10B1B2SafetySupport.noWorker(context)
            report.put("status", "PASS").put("resourceEnded", true).put("operationOutcome", "TIMEOUT")
                .put("closureStatus", "SAME_ORIGINAL_SESSION_ABANDONED_AND_OWNED_PAYLOAD_CLEANED")
                .put("workerPid", actualWorker.pid).put("workerUid", actualWorker.uid).put("workerStartTicks", actualWorker.startTicks)
                .put("realKernelTransactReturnedBeforeTestHold", true).put("activeGuardCleanupRejected", true)
                .put("realDeliveryCalls", wrapper.deliveryCalls.get()).put("unbindCalls", wrapper.unbindCalls.get())
                .put("unbindOnMain", wrapper.unbindOnMain.get()).put("realUnbindSucceeded", wrapper.unbindSucceeded.get())
                .put("initialFailureWhileTicketHeld", true).put("persistentRevokeConfirmed", true)
                .put("resultCount", closed.resultCount).put("acceptedResultsAfterInitialFailure", 0)
                .put("lateClosureCalls", lateCalls.get()).put("wrongSessionRejectedWithoutWrite", true)
                .put("duplicateRecoveryIdempotentWithoutWrite", true).put("lateCompletionRejectedWithoutWrite", true)
                .put("hostFdsClosed", closed.hostFdsClosed).put("rpcQuiescent", closed.rpcQuiescent)
                .put("originalSessionQuiescent", closed.originalSessionQuiescent).put("binderDead", closed.binderDead)
                .put("matchingProcessDead", closed.matchingProcessDead).put("guardReleased", closed.guardReleased)
                .put("connectionCleaned", closed.connectionCleaned).put("abandonedReceipt", true)
                .put("decodeTerminalOrPreparedCreated", false).put("ownedPayloadCleanup", true)
                .put("retirementKind", "FAIL_CLOSED_DETACH_EXTERNAL_BRANCH_PROOF_REQUIRED")
                .put("externalOSAndStageProof", "REQUIRED_SEPARATELY")
        } catch (error: Throwable) {
            report.put("errorType", error.javaClass.simpleName)
            throw error
        } finally {
            observationWindow.set(false)
            wrapper.release.countDown() // Release this test checkpoint, never unbind or kill on the test's behalf.
            caller?.cancel()
            report.put("realDeliveryCalls", wrapper.deliveryCalls.get()).put("unbindCalls", wrapper.unbindCalls.get())
                .put("testCheckpointReleased", wrapper.returnReleased.get()).put("observationWindowClosed", true)
            M10B1B2SafetySupport.write(run, "result.json", report)
        }
    }

    private class DelayedDeliveryContext(private val real: Context) : ContextWrapper(real) {
        val realDeliveryReturned = CountDownLatch(1)
        val release = CountDownLatch(1)
        val deliveryCalls = AtomicInteger()
        val realDeliveryAccepted = AtomicBoolean()
        val returnReleased = AtomicBoolean()
        val realReturnAt = java.util.concurrent.atomic.AtomicLong()
        val unbindCalls = AtomicInteger()
        val unbindSucceeded = AtomicBoolean()
        val unbindOnMain = AtomicBoolean()
        private val connections = mutableMapOf<ServiceConnection, ServiceConnection>()
        override fun getApplicationContext(): Context = this
        override fun bindService(intent: Intent, original: ServiceConnection, flags: Int): Boolean {
            val bridge = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, actual: IBinder) {
                    val proxy = object : IBinder by actual {
                        override fun transact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                            val returned = actual.transact(code, data, reply, flags)
                            if (code == JpegWorkerWire.DELIVER) {
                                check(deliveryCalls.incrementAndGet() == 1)
                                check(returned)
                                realDeliveryAccepted.set(returned)
                                realReturnAt.set(SystemClock.elapsedRealtime())
                                realDeliveryReturned.countDown()
                                check(release.await(65, TimeUnit.SECONDS)) { "Bounded test return checkpoint expired" }
                                returnReleased.set(true)
                            }
                            return returned
                        }
                    }
                    original.onServiceConnected(name, proxy)
                }
                override fun onServiceDisconnected(name: ComponentName) = original.onServiceDisconnected(name)
                override fun onNullBinding(name: ComponentName) = original.onNullBinding(name)
                override fun onBindingDied(name: ComponentName) = original.onBindingDied(name)
            }
            synchronized(connections) { check(!connections.containsKey(original)); connections[original] = bridge }
            return real.bindService(intent, bridge, flags)
        }
        override fun unbindService(original: ServiceConnection) {
            check(unbindCalls.incrementAndGet() == 1)
            unbindOnMain.set(Looper.myLooper() == Looper.getMainLooper())
            val bridge = synchronized(connections) { requireNotNull(connections[original]) }
            real.unbindService(bridge) // Remove responsibility only after the matching real Context returned.
            synchronized(connections) { check(connections.remove(original) === bridge) }
            unbindSucceeded.set(true)
        }
        fun connectionMapEmpty(): Boolean = synchronized(connections) { connections.isEmpty() }
    }

    @Test fun cancellation_after_persistent_pin_before_fd_delivery_closes_only_failed_attempt(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val run = M10B1B2SafetySupport.begin(context, METHOD)
        val report = JSONObject().put("protocol", "M10_B1_EARLY_CANCELLATION_V1")
            .put("runId", run.runId).put("method", METHOD).put("status", "UNKNOWN")
            .put("scope", "AFTER_VERIFIED_HELLO_AND_PERSISTENT_PIN_BEFORE_DATA_FD_DELIVERY_ONLY")
            .put("nativeComputeCancellation", "NOT_RUN").put("lateRpc", "NOT_RUN")
            .put("workerFaultKill", "NOT_RUN").put("hostDeath", "NOT_RUN")
            .put("roomOrFormalCoverCommitted", false).put("resourceEnded", false)
        val pinReached = CountDownLatch(1)
        val returnBeforeDelivery = CountDownLatch(1)
        val hookFinished = CountDownLatch(1)
        val registered = AtomicReference<JpegWorkerIdentity?>()
        val deliveredResult = AtomicReference<JpegWorkerResult?>()
        val hookError = AtomicReference<Throwable?>()
        val callerError = AtomicReference<Throwable?>()
        val hookCalls = AtomicInteger()
        val observations = M10B1ClosureObservations()
        var caller: kotlinx.coroutines.Job? = null
        try {
            M10B1B2SafetySupport.noWorker(context)
            val directory = File(context.noBackupFilesDir.canonicalFile, "m10-b1-cancel-${run.runId}")
            check(directory.mkdir() && File(directory, "media").mkdir())
            val files = MediaFileProtocol(directory)
            val owner = JpegStageV2.Owner(directory.name, uuid(), uuid(), uuid(), uuid())
            val supervisor = JpegStageV2.SupervisorIdentity(run.boot, run.host.pid, run.host.uid, run.host.startTicks)
            val initial = M10JpegFixtures.bytes(M10JpegFixtures.COMPLETE).inputStream().use {
                files.jpeg.create(owner, it, supervisor)
            }
            val source = files.jpeg.sourceFile(owner)
            val output = files.jpeg.outputFile(owner)
            val guard = files.jpeg.guardFile(owner)
            val guardIdentity = files.identity(guard)
            val request = JpegWorkerRequest(owner.generation, owner.assetId, owner.attemptId,
                owner.sessionId, owner.nonce, initial.source.bytes, initial.source.sha256,
                initial.source.device, initial.source.inode, guardIdentity.device, guardIdentity.inode)
            report.put("generation", owner.generation).put("asset", owner.assetId)
                .put("attempt", owner.attemptId).put("session", owner.sessionId).put("nonce", owner.nonce)
                .put("sourceSha256", initial.source.sha256).put("sourceBytes", initial.source.bytes)
            val invocation = launch(Dispatchers.Default) {
                try {
                    val value = JpegWorkerClient(context).execute(source, output, guard, request,
                        beforeDelivery = { worker ->
                            check(registered.compareAndSet(null, worker))
                            check(worker.pid != run.host.pid && worker.uid == run.host.uid &&
                                JpegProcessRole.identity(worker.pid) == worker)
                            val pin = files.jpeg.registerConsumer(owner, worker.stage())
                            check(pin.pinned && pin.source == initial.source)
                            files.jpeg.markDelivery(owner)
                            observations.registerWorker(worker.key())
                            pinReached.countDown()
                            // The real Client cannot beginDelivery or send data FDs until this returns.
                            if (!returnBeforeDelivery.await(20, TimeUnit.SECONDS)) throw IOException("Test did not release its exact pre-FD checkpoint")
                        }, revoke = { reason -> files.jpeg.revoke(owner, reason) },
                        abandonedResult = resultHandler@ { result ->
                            try {
                                val ordinal = hookCalls.incrementAndGet()
                                val origin = if (result.detail == "LATE_FAILED_SESSION_QUIESCENCE")
                                    M10B1ClosureObservations.Origin.LATE_ORIGINAL_QUIESCENCE
                                else M10B1ClosureObservations.Origin.INITIAL
                                // An initial UNKNOWN observation and a later closure are independent
                                // Client handoffs, not duplicate Worker RESULT callbacks.
                                if (ordinal <= 2) M10B1B2SafetySupport.write(run,
                                    "observation-${ordinal.toString().padStart(2, '0')}.json",
                                    observation(result, origin).put("ordinal", ordinal))
                                val decision = observations.observe(M10B1ClosureObservations.Observation(
                                    origin, result.outcome.name, result.workerIdentity?.key(), result.resultCount,
                                    result.sourceFdClosed, result.outputFdClosed, result.hostFdsClosed,
                                    result.rpcQuiescent, result.originalSessionQuiescent, result.binderDead,
                                    result.matchingProcessDead, result.guardReleased, result.connectionCleaned))
                                if (decision == M10B1ClosureObservations.Decision.WAIT) return@resultHandler
                                check(deliveredResult.compareAndSet(null, result) && !result.canPrepare)
                                check(result.workerIdentity == registered.get())
                                val worker = requireNotNull(result.workerIdentity)
                                val probe = AndroidJpegRecoveryProbe(context)
                                val live = JpegRecoveryVerifier.LiveQuiescence(owner, supervisor, worker.stage(),
                                    result.hostFdsClosed, result.rpcQuiescent, result.originalSessionQuiescent,
                                    result.connectionCleaned, result.binderDead, result.matchingProcessDead, result.guardReleased)
                                files.jpeg.recoverNoResult(owner) { facts -> JpegRecoveryVerifier(probe).verify(facts, live) }
                                val closed = files.jpeg.snapshot(owner)
                                check(closed.state == JpegStageV2.State.ABANDONED && !closed.pinned && closed.outcome == null)
                                check(!File(source.parentFile, "terminal.v2").exists())
                                check(!File(source.parentFile, "prepared.v2").exists())
                                files.jpeg.safeCleanup(owner)
                                check(!source.exists() && !output.exists())
                                files.jpeg.admission()
                                hookFinished.countDown() // Only an actually verified closure completes the wait.
                            } catch (error: Throwable) {
                                if (hookError.compareAndSet(null, error)) {
                                    try { M10B1B2SafetySupport.write(run, "callback-error.json", JSONObject()
                                        .put("status", "UNKNOWN").put("errorType", error.javaClass.simpleName)
                                        .put("observationCount", hookCalls.get()).put("furtherRecoveryBlocked", true)) }
                                    catch (evidenceError: Throwable) { error.addSuppressed(evidenceError) }
                                }
                                observations.stop()
                                hookFinished.countDown()
                                throw error // Client records its hook failure; no admission release is inferred.
                            }
                        })
                    callerError.compareAndSet(null, AssertionError("Cancelled continuation unexpectedly returned ${value.outcome}"))
                } catch (_: CancellationException) { /* Expected caller outcome, never retirement proof. */ }
                catch (error: Throwable) { callerError.compareAndSet(null, error) }
            }
            caller = invocation
            check(pinReached.await(20, TimeUnit.SECONDS)) { "Real verified pin checkpoint was not reached" }
            invocation.cancel(CancellationException("AUTHORIZED_EARLY_WINDOW_ONLY"))
            returnBeforeDelivery.countDown()
            withTimeout(5_000) { invocation.join() }
            check(hookFinished.await(35, TimeUnit.SECONDS)) { "No conclusive original-session closure hook" }
            hookError.get()?.let { throw AssertionError("Closure hook failed or stayed UNKNOWN", it) }
            callerError.get()?.let { throw AssertionError("Caller path was not the expected cancellation", it) }
            val result = requireNotNull(deliveredResult.get())
            check(hookCalls.get() in 1..2 && hookCalls.get() == observations.observations() && observations.closureClaimed())
            M10B1B2SafetySupport.assertHost(run)
            M10B1B2SafetySupport.noWorker(context)
            report.put("status", "PASS").put("resourceEnded", true)
                .put("workerPid", result.workerIdentity?.pid).put("workerUid", result.workerIdentity?.uid)
                .put("workerStartTicks", result.workerIdentity?.startTicks)
                .put("resultCount", result.resultCount).put("callerOutcome", "CANCELLED")
                .put("observationCount", observations.observations()).put("completeClosureClaims", 1)
                .put("initialUnknownDoesNotProveClosure", true)
                .put("wireResultInvented", false).put("workerDataFdCloseFlagsInvented", false)
                .put("hostFdsClosed", result.hostFdsClosed).put("rpcQuiescent", result.rpcQuiescent)
                .put("originalSessionQuiescent", result.originalSessionQuiescent)
                .put("binderDead", result.binderDead).put("matchingProcessDead", result.matchingProcessDead)
                .put("guardReleased", result.guardReleased).put("connectionCleaned", result.connectionCleaned)
                .put("abandonedReceipt", true).put("decodeTerminalOrPreparedCreated", false)
                .put("ownedPayloadCleanup", true).put("externalOSAndStageProof", "REQUIRED_SEPARATELY")
        } catch (error: Throwable) {
            report.put("errorType", error.javaClass.simpleName)
            throw error
        } finally {
            observations.stop() // No new late recovery may begin after this bounded observation window.
            report.put("observationCount", hookCalls.get()).put("observationWindowClosed", true)
            returnBeforeDelivery.countDown()
            caller?.cancel() // Same already-authorized synthetic Invocation; no process kill or new request.
            M10B1B2SafetySupport.write(run, "result.json", report)
        }
    }
    private fun observation(result: JpegWorkerResult, origin: M10B1ClosureObservations.Origin) = JSONObject()
        .put("origin", origin.name).put("outcome", result.outcome.name).put("detail", result.detail)
        .put("workerPid", result.workerIdentity?.pid).put("workerUid", result.workerIdentity?.uid)
        .put("workerStartTicks", result.workerIdentity?.startTicks).put("resultCount", result.resultCount)
        .put("sourceFdClosed", result.sourceFdClosed).put("outputFdClosed", result.outputFdClosed)
        .put("hostFdsClosed", result.hostFdsClosed).put("rpcQuiescent", result.rpcQuiescent)
        .put("originalSessionQuiescent", result.originalSessionQuiescent).put("binderDead", result.binderDead)
        .put("matchingProcessDead", result.matchingProcessDead).put("guardReleased", result.guardReleased)
        .put("connectionCleaned", result.connectionCleaned).put("status", "OBSERVED_NOT_CLEANUP_AUTHORITY")
    private fun JpegWorkerIdentity.key() = "$pid/$uid/$startTicks"
    private fun JpegWorkerIdentity.stage() = JpegStageV2.WorkerIdentity(pid, uid, startTicks)
    private fun uuid() = UUID.randomUUID().toString()
    companion object {
        const val DELAYED_METHOD = "com.cooper.lithic.data.media.M10B1CancellationSafetyTest#delayed_real_delivery_return_keeps_binding_until_original_failed_session_closes"
        const val METHOD = "com.cooper.lithic.data.media.M10B1CancellationSafetyTest#cancellation_after_persistent_pin_before_fd_delivery_closes_only_failed_attempt"
    }
}
