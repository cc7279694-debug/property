package com.cooper.lithic.feature.project

import org.junit.Assert.*
import org.junit.Test

class ReplacementSubmissionGateTest {
    @Test fun dirty_form_requires_discard_confirmation() {
        val gate = ReplacementSubmissionGate()
        assertEquals(LeaveDecision.ALLOW, gate.leaveDecision())
        gate.changed()
        assertEquals(LeaveDecision.CONFIRM_DISCARD, gate.leaveDecision())
    }
    @Test fun confirmation_is_required_and_edit_invalidates_it() {
        val gate = ReplacementSubmissionGate()
        gate.changed()
        assertFalse(gate.begin("fixed-command"))
        gate.confirm()
        gate.changed()
        assertFalse(gate.begin("fixed-command"))
    }
    @Test fun duplicate_click_and_unknown_retry_keep_one_identity() {
        val gate = ReplacementSubmissionGate()
        gate.confirm()
        assertTrue(gate.begin("fixed-command"))
        assertFalse(gate.begin("fixed-command"))
        gate.unknown()
        assertEquals(LeaveDecision.BLOCK_PENDING, gate.leaveDecision())
        assertFalse(gate.begin("different-command"))
        assertTrue(gate.begin("fixed-command"))
    }
    @Test fun failed_or_cancelled_submission_remains_pending() {
        val gate = ReplacementSubmissionGate()
        gate.confirm(); assertTrue(gate.begin("same")); gate.unknown()
        gate.changed()
        assertEquals(SubmissionPhase.UNCONFIRMED, gate.phase)
        assertEquals(LeaveDecision.BLOCK_PENDING, gate.leaveDecision())
    }
    @Test fun restored_inflight_requires_reconciliation_with_exact_command() {
        val gate = ReplacementSubmissionGate(SubmissionPhase.IN_FLIGHT, true, true, "same")
        assertEquals(SubmissionPhase.UNCONFIRMED, gate.phase)
        assertFalse(gate.begin("other"))
        assertTrue(gate.begin("same"))
        gate.committed()
        assertEquals(LeaveDecision.ALLOW, gate.leaveDecision())
    }
    @Test fun confirmed_validation_failure_can_edit_after_a_definite_rejection() {
        val gate = ReplacementSubmissionGate()
        gate.confirm(); assertTrue(gate.begin("same")); gate.rejected()
        assertEquals(SubmissionPhase.IDLE, gate.phase)
        gate.changed()
        assertEquals(LeaveDecision.CONFIRM_DISCARD, gate.leaveDecision())
        assertFalse(gate.confirmed)
    }
}
