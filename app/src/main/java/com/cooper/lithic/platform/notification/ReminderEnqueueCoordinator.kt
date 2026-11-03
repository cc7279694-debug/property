package com.cooper.lithic.platform.notification

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * An instance-specific gate. Implementations must serialize [withActive] with retirement:
 * after retirement starts no new block may enter. Blocks must be short and non-suspending.
 * [isActive] must be a non-blocking read and must not acquire the retirement lock.
 * A non-null result proves only that the block was entered, not that an OS task ran.
 */
internal interface EnqueueLease {
    fun <T : Any> withActive(block: () -> T): T?
    fun isActive(): Boolean
}

internal enum class EnqueueKind { PERIODIC, RECONCILE }
internal enum class EnqueueStatus { PENDING, ENQUEUED, FAILED, UNKNOWN, CANCELLED }

/** A callback observes the real terminal outcome without cancelling the underlying operation. */
internal fun interface EnqueueOperation {
    fun addTerminalListener(listener: (Result<Unit>) -> Unit)
}

/** enqueue must return promptly and never wait for WorkManager operation completion. */
internal fun interface EnqueuePort {
    fun enqueue(kind: EnqueueKind): EnqueueOperation
}

/** May only wrap a failure known to happen before any platform enqueue side effect. */
internal class EnqueueNotSubmittedException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Two bounded lanes: at most one issued operation and one latest pending request each.
 * A timeout changes the local result to UNKNOWN but retains the terminal listener and the
 * occupied lane. A pending request is issued only after the old operation actually terminates.
 * This class never cancels an underlying Operation or automatically retries one.
 */
internal class ReminderEnqueueCoordinator(
    private val scope: CoroutineScope,
    private val port: EnqueuePort,
    private val timeoutMillis: Long,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val onOutcome: (EnqueueKind, EnqueueStatus) -> Unit = { _, _ -> },
) {
    init { require(timeoutMillis > 0) }

    private class Request(val kind: EnqueueKind, val lease: EnqueueLease) {
        val state = MutableStateFlow(EnqueueStatus.PENDING)
        var timeout: Job? = null
    }
    private class Lane {
        var active: Request? = null
        var pending: Request? = null
        var outcomeUnknown: Boolean = false
    }

    private val lock = Any()
    private val lanes = EnqueueKind.entries.associateWith { Lane() }
    // Diagnostic delivery is best effort and bounded. A slow logger cannot block enqueue;
    // the request StateFlow remains authoritative even if repeated diagnostics coalesce.
    private val diagnostics = Channel<Pair<EnqueueKind, EnqueueStatus>>(Channel.CONFLATED)

    init {
        scope.launch(Dispatchers.IO) {
            for ((kind, status) in diagnostics) {
                try { onOutcome(kind, status) }
                catch (cancelled: CancellationException) { if (!isActive) throw cancelled }
                catch (_: Exception) { /* Diagnostics cannot alter the scheduling outcome. */ }
            }
        }
        scope.coroutineContext[Job]?.invokeOnCompletion {
            synchronized(lock) {
                lanes.values.forEach { lane ->
                    lane.pending?.state?.value = EnqueueStatus.CANCELLED
                    lane.pending = null
                    lane.active?.state?.value = EnqueueStatus.CANCELLED
                    lane.active?.timeout?.cancel()
                }
                diagnostics.close()
            }
        }
    }

    /** Cancellation here is local observation/supersession only, never OS cancellation. */
    fun request(kind: EnqueueKind, lease: EnqueueLease): StateFlow<EnqueueStatus> {
        val request = Request(kind, lease)
        val lane = lanes.getValue(kind)
        val toStart = synchronized(lock) {
            if (!scope.isActive || !lease.isActive()) {
                request.state.value = EnqueueStatus.CANCELLED
                null
            } else {
                lane.pending?.state?.value = EnqueueStatus.CANCELLED
                // An old ticket can no longer update a newer settings page; the operation
                // itself remains outstanding until its terminal callback is observed.
                lane.active?.let { old ->
                    if (old.state.value == EnqueueStatus.PENDING) old.state.value = EnqueueStatus.CANCELLED
                }
                lane.pending = request
                if (lane.outcomeUnknown) request.state.value = EnqueueStatus.UNKNOWN
                reserveNextLocked(lane)
            }
        }
        launchReserved(kind, lane, toStart)
        return request.state.asStateFlow()
    }

    /** Retire an instance, but keep an already-issued operation occupying its lane. */
    fun retire(lease: EnqueueLease) {
        synchronized(lock) {
            lanes.values.forEach { lane ->
                if (lane.pending?.lease === lease) {
                    lane.pending?.state?.value = EnqueueStatus.CANCELLED
                    lane.pending = null
                }
                if (lane.active?.lease === lease) {
                    lane.active?.state?.value = EnqueueStatus.CANCELLED
                    // Keep the process-level timeout alive: a new owner waiting behind this
                    // issued operation must see UNKNOWN if its outcome does not arrive.
                }
            }
        }
    }

    private fun reserveNextLocked(lane: Lane): Request? {
        if (lane.active != null || !scope.isActive) return null
        val request = lane.pending ?: return null
        lane.pending = null
        request.state.value = EnqueueStatus.PENDING
        lane.outcomeUnknown = false
        lane.active = request
        return request
    }

    /** Always launch outside the state lock, even with an immediate/test dispatcher. */
    private fun launchReserved(kind: EnqueueKind, lane: Lane, request: Request?) {
        if (request == null) return
        // The deadline starts before calling the port or registering its listener. It uses
        // a separate dispatcher so a blocked submission dispatcher cannot hide UNKNOWN.
        val timer = scope.launch(Dispatchers.Default) {
            delay(timeoutMillis)
            synchronized(lock) { markUnknownLocked(lane, request) }
        }
        synchronized(lock) {
            if (lane.active === request) request.timeout = timer else timer.cancel()
        }
        scope.launch(dispatcher) { submit(kind, lane, request) }
    }

    private fun submit(kind: EnqueueKind, lane: Lane, request: Request) {
        synchronized(lock) {
            if (lane.active !== request) return
            if (!scope.isActive) {
                request.timeout?.cancel()
                request.state.value = EnqueueStatus.CANCELLED
                lane.active = null
                return
            }
        }
        val operation = try {
            // Never acquire the owner lock while holding the coordinator lock: publishing
            // owner retirement can resume a collector that needs the coordinator lock.
            // The short, non-suspending enqueue remains fenced against retirement itself.
            request.lease.withActive {
                if (!scope.isActive) throw CancellationException("Enqueue observer closed")
                port.enqueue(kind)
            }
        } catch (_: CancellationException) {
            synchronized(lock) {
                // Cancellation while enqueue is being invoked is not proof that the
                // platform did not accept work. No handle exists to prove termination.
                markUnknownLocked(lane, request)
            }
            return
        } catch (_: EnqueueNotSubmittedException) {
            val next = synchronized(lock) {
                if (lane.active === request) {
                    diagnostics.trySend(kind to EnqueueStatus.FAILED)
                    request.timeout?.cancel()
                    if (request.lease.isActive() && request.state.value != EnqueueStatus.CANCELLED)
                        request.state.value = EnqueueStatus.FAILED
                    lane.active = null
                    reserveNextLocked(lane)
                } else null
            }
            launchReserved(kind, lane, next)
            return
        } catch (_: Exception) {
            // An arbitrary synchronous exception may occur after the platform accepted
            // work. Without an operation handle its final effect cannot be established.
            synchronized(lock) { markUnknownLocked(lane, request) }
            return
        }
        if (operation == null) {
            val next = synchronized(lock) {
                if (lane.active === request) {
                    request.timeout?.cancel()
                    request.state.value = EnqueueStatus.CANCELLED
                    lane.active = null
                    reserveNextLocked(lane)
                } else null
            }
            launchReserved(kind, lane, next)
            return
        }
        // Listener registration is outside the coordinator/owner locks: an already complete
        // future may invoke its listener inline. Registration failure has unknown outcome.
        try {
            operation.addTerminalListener { result -> finish(kind, lane, request, result) }
        } catch (_: Exception) {
            synchronized(lock) {
                markUnknownLocked(lane, request)
            }
            return
        }
    }

    private fun finish(kind: EnqueueKind, lane: Lane, request: Request, result: Result<Unit>) {
        val next = synchronized(lock) {
            // Ignores duplicate and late callbacks for a prior operation.
            if (lane.active !== request) return
            request.timeout?.cancel()
            if (result.exceptionOrNull() is CancellationException) {
                // A cancelled result Future does not prove that WorkManager's transaction
                // was cancelled. Keep the lane occupied rather than issuing newer work.
                markUnknownLocked(lane, request)
                return
            }
            if (scope.isActive && request.lease.isActive() && request.state.value != EnqueueStatus.CANCELLED) {
                request.state.value = if (result.isSuccess) EnqueueStatus.ENQUEUED else EnqueueStatus.FAILED
            }
            if (result.isFailure) diagnostics.trySend(kind to EnqueueStatus.FAILED)
            lane.active = null
            lane.outcomeUnknown = false
            reserveNextLocked(lane)
        }
        launchReserved(kind, lane, next)
    }

    private fun markUnknownLocked(lane: Lane, request: Request) {
        if (lane.active !== request) return
        if (!lane.outcomeUnknown) diagnostics.trySend(request.kind to EnqueueStatus.UNKNOWN)
        lane.outcomeUnknown = true
        if (scope.isActive && request.lease.isActive() && request.state.value != EnqueueStatus.CANCELLED)
            request.state.value = EnqueueStatus.UNKNOWN
        lane.pending?.let { pending ->
            if (scope.isActive && pending.lease.isActive()) pending.state.value = EnqueueStatus.UNKNOWN
        }
    }
}
