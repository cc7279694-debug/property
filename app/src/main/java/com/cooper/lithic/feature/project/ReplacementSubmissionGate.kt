package com.cooper.lithic.feature.project

/** The actual editor uses this small gate; an uncertain command cannot become a fresh command. */
internal class ReplacementSubmissionGate(
    phase: SubmissionPhase = SubmissionPhase.IDLE,
    var dirty: Boolean = false,
    var confirmed: Boolean = false,
    private var submittedCommand: String? = null,
) {
    var phase = if (phase == SubmissionPhase.IN_FLIGHT) SubmissionPhase.UNCONFIRMED else phase
        private set
    fun changed() {
        if (phase != SubmissionPhase.IDLE) return
        dirty = true
        confirmed = false
    }
    fun confirm() { if (phase == SubmissionPhase.IDLE) confirmed = true }
    fun begin(commandId: String): Boolean {
        if (phase == SubmissionPhase.UNCONFIRMED) {
            if (submittedCommand != commandId) return false
        } else if (phase != SubmissionPhase.IDLE || !confirmed) return false
        submittedCommand = commandId
        phase = SubmissionPhase.IN_FLIGHT
        return true
    }
    fun unknown() { if (phase != SubmissionPhase.COMMITTED) phase = SubmissionPhase.UNCONFIRMED }
    fun rejected() { phase = SubmissionPhase.IDLE; confirmed = false; submittedCommand = null }
    fun committed() { phase = SubmissionPhase.COMMITTED }
    fun leaveDecision(): LeaveDecision = when (phase) {
        SubmissionPhase.IN_FLIGHT, SubmissionPhase.UNCONFIRMED -> LeaveDecision.BLOCK_PENDING
        SubmissionPhase.COMMITTED -> LeaveDecision.ALLOW
        SubmissionPhase.IDLE -> if (dirty) LeaveDecision.CONFIRM_DISCARD else LeaveDecision.ALLOW
    }
}
