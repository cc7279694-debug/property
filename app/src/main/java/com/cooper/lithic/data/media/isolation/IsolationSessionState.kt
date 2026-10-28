package com.cooper.lithic.data.media.isolation

enum class IsolationPhase { NEW, HELLO, GUARD_HELD, COMPUTING, RESULT_CANDIDATE, RETIRING, CLOSED }
enum class IsolationOutcome { UNKNOWN, PROBE_CLOSED, REVOKED_CLOSED }
enum class IsolationJournalState { ADMITTED, CLOSED, UNKNOWN }
enum class IsolationRecovery { NEW, CLOSED_RECORDED, UNKNOWN }

/** Separate observations; no flag is inferred from another flag, a timeout, or a PID. */
data class IsolationClosureEvidence(
    val binderDead: Boolean = false,
    val guardReturned: Boolean = false,
    val workerReportedInputClosed: Boolean = false,
    val workerReportedOutputClosed: Boolean = false,
    val hostFdsClosed: Boolean = false,
    val rpcQuiescent: Boolean = false,
)

data class IsolationSessionSnapshot(
    val phase: IsolationPhase,
    val expectedNonce: String,
    val hostUid: Int,
    val workerUid: Int?,
    val acceptedResult: Boolean,
    val revoked: Boolean,
    val revocationReason: String?,
    val closure: IsolationClosureEvidence,
    val outcome: IsolationOutcome,
)

/**
 * One authenticated synthetic probe. Callers supply kernel/FD observations, never message claims
 * for those observations. No outcome grants a media write, Owner, or recovery capability.
 */
class IsolationSessionState(val nonce: String, val hostUid: Int, val protocolVersion: Int = 1) {
    init {
        require(nonce.length in 1..128 && nonce.all { it.code in 33..126 })
        require(hostUid > 0)
        require(protocolVersion == 1)
    }

    private var phase = IsolationPhase.NEW
    private var workerUid: Int? = null
    private var acceptedResult = false
    private var revoked = false
    private var revocationReason: String? = null
    private var closure = IsolationClosureEvidence()
    private var hostConfirmedGuard = false

    @Synchronized
    fun hello(senderUid: Int, version: Int, receivedNonce: String): Boolean {
        if (phase != IsolationPhase.NEW || revoked || senderUid <= 0 || senderUid == hostUid ||
            version != protocolVersion || receivedNonce != nonce) return false
        workerUid = senderUid
        phase = IsolationPhase.HELLO
        return true
    }

    @Synchronized
    fun guardHeld(senderUid: Int, version: Int, receivedNonce: String): Boolean {
        if (phase != IsolationPhase.HELLO || !authenticated(senderUid, version, receivedNonce)) return false
        // This is only an authenticated Worker claim. Host must independently observe its lock.
        phase = IsolationPhase.GUARD_HELD
        return true
    }

    /** Host calls only after its independently opened same-inode channel observes the held lock. */
    @Synchronized
    fun startComputing(): Boolean {
        if (phase != IsolationPhase.GUARD_HELD || revoked || closure.binderDead) return false
        hostConfirmedGuard = true
        phase = IsolationPhase.COMPUTING
        return true
    }

    @Synchronized
    fun candidate(
        senderUid: Int, version: Int, receivedNonce: String,
        inputClosed: Boolean, outputClosed: Boolean,
    ): Boolean {
        if (phase == IsolationPhase.CLOSED || !authenticated(senderUid, version, receivedNonce)) return false
        if (acceptedResult) {
            revokeLocked("DUPLICATE_RESULT")
            return false
        }
        if (phase != IsolationPhase.COMPUTING || closure.binderDead || closure.guardReturned) return false
        acceptedResult = true
        closure = closure.copy(workerReportedInputClosed = inputClosed, workerReportedOutputClosed = outputClosed)
        phase = IsolationPhase.RESULT_CANDIDATE
        return true
    }

    /** Normal retirement retains a candidate. Cancellation uses revoke instead. */
    @Synchronized
    fun retire(): Boolean {
        if (phase == IsolationPhase.CLOSED || phase == IsolationPhase.RETIRING) return false
        phase = IsolationPhase.RETIRING
        finishIfObserved()
        return true
    }

    @Synchronized
    fun revoke(reason: String): Boolean {
        if (phase == IsolationPhase.CLOSED) return false
        revokeLocked(reason)
        return true
    }

    @Synchronized
    fun recordBinderDeath() {
        if (phase == IsolationPhase.CLOSED) return
        closure = closure.copy(binderDead = true)
        // An endpoint dying before retirement cannot later produce a trusted success.
        if (phase != IsolationPhase.RETIRING) revokeLocked("EARLY_WORKER_EXIT")
        finishIfObserved()
    }

    @Synchronized
    fun recordGuardReturned() {
        if (phase == IsolationPhase.CLOSED || !hostConfirmedGuard) return
        closure = closure.copy(guardReturned = true)
        if (phase != IsolationPhase.RETIRING) revokeLocked("EARLY_GUARD_RETURN")
        finishIfObserved()
    }

    @Synchronized
    fun recordHostFdsClosed() {
        if (phase == IsolationPhase.CLOSED) return
        closure = closure.copy(hostFdsClosed = true)
        finishIfObserved()
    }

    /** Only a final retirement observation, not an ordinary gap between live RPCs. */
    @Synchronized
    fun recordRpcQuiescent() {
        if (phase != IsolationPhase.RETIRING) return
        closure = closure.copy(rpcQuiescent = true)
        finishIfObserved()
    }

    @Synchronized
    fun snapshot() = IsolationSessionSnapshot(
        phase, nonce, hostUid, workerUid, acceptedResult, revoked, revocationReason, closure, outcome(),
    )

    private fun authenticated(senderUid: Int, version: Int, receivedNonce: String): Boolean =
        !revoked && workerUid != null && senderUid == workerUid &&
            version == protocolVersion && receivedNonce == nonce

    private fun revokeLocked(reason: String) {
        if (!revoked) revocationReason = reason.trim().take(64).ifEmpty { "ERROR" }
        revoked = true
        phase = IsolationPhase.RETIRING
        finishIfObserved()
    }

    private fun finishIfObserved() {
        if (phase == IsolationPhase.RETIRING && closure.binderDead && closure.guardReturned &&
            closure.hostFdsClosed && closure.rpcQuiescent) phase = IsolationPhase.CLOSED
    }

    private fun outcome(): IsolationOutcome = when {
        phase != IsolationPhase.CLOSED -> IsolationOutcome.UNKNOWN
        revoked -> IsolationOutcome.REVOKED_CLOSED
        acceptedResult && closure.workerReportedInputClosed && closure.workerReportedOutputClosed -> IsolationOutcome.PROBE_CLOSED
        else -> IsolationOutcome.UNKNOWN
    }
}

/** Persisted CLOSED is a record, not proof of a currently running process or graceful shutdown. */
fun inferIsolationRecovery(hasStage: Boolean, journal: IsolationJournalState?): IsolationRecovery = when {
    !hasStage && journal == null -> IsolationRecovery.NEW
    journal == IsolationJournalState.CLOSED -> IsolationRecovery.CLOSED_RECORDED
    else -> IsolationRecovery.UNKNOWN
}
