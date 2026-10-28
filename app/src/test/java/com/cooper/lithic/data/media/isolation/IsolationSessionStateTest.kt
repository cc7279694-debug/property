package com.cooper.lithic.data.media.isolation

import org.junit.Assert.*
import org.junit.Test

class IsolationSessionStateTest {
    private val nonce = "1b98639d-36e8-4995-b3c0-41f474915902"
    private val host = 10123
    private val sandbox = 99031
    private fun session() = IsolationSessionState(nonce, host)

    private fun computing(): IsolationSessionState = session().also {
        assertTrue(it.hello(sandbox, 1, nonce))
        assertTrue(it.guardHeld(sandbox, 1, nonce))
        assertTrue(it.startComputing())
    }
    private fun candidate(): IsolationSessionState = computing().also {
        assertTrue(it.candidate(sandbox, 1, nonce, inputClosed = true, outputClosed = true))
    }
    private fun evidence(it: IsolationSessionState, omit: String? = null) {
        if (omit != "binder") it.recordBinderDeath()
        if (omit != "guard") it.recordGuardReturned()
        if (omit != "host") it.recordHostFdsClosed()
        if (omit != "rpc") it.recordRpcQuiescent()
    }

    @Test fun authentic_hello_binds_kernel_uid_once_and_preserves_expected_identity() {
        val it = session()
        assertTrue(it.hello(sandbox, 1, nonce))
        assertEquals(IsolationPhase.HELLO, it.snapshot().phase)
        assertEquals(sandbox, it.snapshot().workerUid)
        assertEquals(host, it.snapshot().hostUid)
        assertEquals(nonce, it.snapshot().expectedNonce)
        assertFalse(it.hello(sandbox, 1, nonce))
        assertFalse(it.hello(sandbox + 1, 1, nonce))
        assertEquals(sandbox, it.snapshot().workerUid)
    }

    @Test fun self_uid_wrong_protocol_and_wrong_nonce_do_not_admit_a_worker() {
        val it = session()
        assertFalse(it.hello(host, 1, nonce))
        assertFalse(it.hello(0, 1, nonce))
        assertFalse(it.hello(sandbox, 2, nonce))
        assertFalse(it.hello(sandbox, 1, "other-nonce"))
        assertNull(it.snapshot().workerUid)
        assertEquals(IsolationPhase.NEW, it.snapshot().phase)
    }

    @Test fun guard_and_compute_require_order_and_the_authenticated_worker() {
        val it = session()
        assertFalse(it.guardHeld(sandbox, 1, nonce))
        assertFalse(it.startComputing())
        assertTrue(it.hello(sandbox, 1, nonce))
        assertFalse(it.startComputing())
        assertFalse(it.guardHeld(sandbox + 1, 1, nonce))
        assertTrue(it.guardHeld(sandbox, 1, nonce))
        assertTrue(it.startComputing())
        assertFalse(it.startComputing())
        assertEquals(IsolationPhase.COMPUTING, it.snapshot().phase)
    }

    @Test fun result_is_not_success_until_retirement_and_every_independent_close_fact() {
        val it = candidate()
        assertEquals(IsolationOutcome.UNKNOWN, it.snapshot().outcome)
        assertTrue(it.retire())
        it.recordBinderDeath()
        assertEquals(IsolationOutcome.UNKNOWN, it.snapshot().outcome)
        it.recordGuardReturned()
        assertEquals(IsolationOutcome.UNKNOWN, it.snapshot().outcome)
        it.recordHostFdsClosed()
        assertEquals(IsolationOutcome.UNKNOWN, it.snapshot().outcome)
        it.recordRpcQuiescent()
        assertEquals(IsolationPhase.CLOSED, it.snapshot().phase)
        assertEquals(IsolationOutcome.PROBE_CLOSED, it.snapshot().outcome)
        assertTrue(it.snapshot().acceptedResult)
        assertFalse(it.snapshot().revoked)
    }

    @Test fun normal_retirement_preserves_candidate_while_each_missing_fact_remains_unknown() {
        for (missing in listOf("binder", "guard", "host", "rpc")) {
            val it = candidate()
            assertTrue(it.retire())
            evidence(it, missing)
            assertFalse(it.snapshot().revoked)
            assertEquals(missing, IsolationOutcome.UNKNOWN, it.snapshot().outcome)
        }
    }

    @Test fun complete_kernel_evidence_does_not_replace_missing_worker_input_close_report() {
        val it = computing()
        assertTrue(it.candidate(sandbox, 1, nonce, inputClosed = false, outputClosed = true))
        it.retire(); evidence(it)
        assertFalse(it.snapshot().closure.workerReportedInputClosed)
        assertTrue(it.snapshot().closure.workerReportedOutputClosed)
        assertEquals(IsolationOutcome.UNKNOWN, it.snapshot().outcome)
    }

    @Test fun complete_kernel_evidence_does_not_replace_missing_worker_output_close_report() {
        val it = computing()
        assertTrue(it.candidate(sandbox, 1, nonce, inputClosed = true, outputClosed = false))
        it.retire(); evidence(it)
        assertEquals(IsolationOutcome.UNKNOWN, it.snapshot().outcome)
    }

    @Test fun cancellation_cannot_be_upgraded_by_a_late_success() {
        val it = computing()
        it.revoke("CANCELLED")
        assertFalse(it.candidate(sandbox, 1, nonce, inputClosed = true, outputClosed = true))
        evidence(it)
        assertTrue(it.snapshot().revoked)
        assertFalse(it.snapshot().acceptedResult)
        assertEquals("CANCELLED", it.snapshot().revocationReason)
        assertEquals(IsolationOutcome.REVOKED_CLOSED, it.snapshot().outcome)
    }

    @Test fun timeout_after_a_candidate_irrevocably_revokes_the_candidate() {
        val it = candidate()
        it.revoke("TIMEOUT")
        it.revoke("SECOND_ERROR")
        evidence(it)
        assertEquals("TIMEOUT", it.snapshot().revocationReason)
        assertEquals(IsolationOutcome.REVOKED_CLOSED, it.snapshot().outcome)
        assertFalse(it.candidate(sandbox, 1, nonce, inputClosed = true, outputClosed = true))
    }

    @Test fun wrong_uid_nonce_or_protocol_result_never_creates_a_candidate() {
        val it = computing()
        assertFalse(it.candidate(host, 1, nonce, true, true))
        assertFalse(it.candidate(sandbox + 1, 1, nonce, true, true))
        assertFalse(it.candidate(sandbox, 2, nonce, true, true))
        assertFalse(it.candidate(sandbox, 1, "different", true, true))
        assertFalse(it.snapshot().acceptedResult)
        assertEquals(IsolationOutcome.UNKNOWN, it.snapshot().outcome)
    }

    @Test fun duplicate_authenticated_result_is_rejected_and_cannot_claim_unique_success() {
        val it = candidate()
        assertFalse(it.candidate(sandbox, 1, nonce, true, true))
        evidence(it)
        assertTrue(it.snapshot().revoked)
        assertEquals(IsolationOutcome.REVOKED_CLOSED, it.snapshot().outcome)
    }

    @Test fun guard_return_cannot_be_inferred_without_a_previously_held_guard() {
        val it = session()
        it.recordGuardReturned()
        assertFalse(it.snapshot().closure.guardReturned)
        assertEquals(IsolationOutcome.UNKNOWN, it.snapshot().outcome)
    }

    @Test fun cancellation_before_hello_is_conservative_and_blocks_all_late_admission() {
        val it = session()
        it.revoke("CANCELLED")
        assertFalse(it.hello(sandbox, 1, nonce))
        assertFalse(it.guardHeld(sandbox, 1, nonce))
        assertFalse(it.startComputing())
        it.recordHostFdsClosed(); it.recordRpcQuiescent()
        assertEquals(IsolationOutcome.UNKNOWN, it.snapshot().outcome)
    }

    @Test fun retirement_without_a_result_is_never_a_success() {
        val it = computing()
        it.retire(); evidence(it)
        assertEquals(IsolationOutcome.UNKNOWN, it.snapshot().outcome)
    }

    @Test fun a_closed_outcome_is_immutable_to_late_messages_and_repeat_observations() {
        val it = candidate()
        it.retire(); evidence(it)
        val before = it.snapshot()
        assertFalse(it.hello(sandbox, 1, nonce))
        assertFalse(it.candidate(sandbox, 1, nonce, true, true))
        assertFalse(it.revoke("LATE_ERROR"))
        evidence(it)
        assertEquals(before, it.snapshot())
    }

    @Test fun concurrent_close_observations_are_idempotent_and_cannot_close_a_revoked_success() {
        val it = candidate()
        it.revoke("CANCELLED")
        val workers = (1..12).map { _ -> Thread { evidence(it) } }
        workers.forEach(Thread::start); workers.forEach(Thread::join)
        assertEquals(IsolationOutcome.REVOKED_CLOSED, it.snapshot().outcome)
        assertTrue(it.snapshot().revoked)
    }

    @Test fun recovery_does_not_turn_recorded_or_lost_host_state_into_fresh_runtime_evidence() {
        assertEquals(IsolationRecovery.NEW, inferIsolationRecovery(false, null))
        assertEquals(IsolationRecovery.UNKNOWN, inferIsolationRecovery(true, null))
        assertEquals(IsolationRecovery.UNKNOWN, inferIsolationRecovery(false, IsolationJournalState.ADMITTED))
        assertEquals(IsolationRecovery.UNKNOWN, inferIsolationRecovery(true, IsolationJournalState.ADMITTED))
        assertEquals(IsolationRecovery.UNKNOWN, inferIsolationRecovery(true, IsolationJournalState.UNKNOWN))
        assertEquals(IsolationRecovery.CLOSED_RECORDED, inferIsolationRecovery(true, IsolationJournalState.CLOSED))
    }
}
