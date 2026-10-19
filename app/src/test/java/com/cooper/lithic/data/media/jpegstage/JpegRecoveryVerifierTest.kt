package com.cooper.lithic.data.media.jpegstage

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.util.UUID

class JpegRecoveryVerifierTest {
    private val boot = "6c697468-6963-4000-8000-000000000008"
    private val oldHost = JpegStageV2.SupervisorIdentity(boot, 100, 10123, 111)
    private val worker = JpegStageV2.WorkerIdentity(101, 10123, 222)
    private fun id() = UUID.randomUUID().toString()
    private fun request() = JpegStageV2.RecoveryRequest(
        JpegStageV2.Owner(id(), id(), id(), id(), id()), oldHost, worker,
        JpegStageV2.FileIdentity(1, 2, 8, "a".repeat(64)),
        JpegStageV2.FileIdentity(1, 3, 0, "b".repeat(64)),
        JpegStageV2.FileIdentity(1, 4, 0, "c".repeat(64)))
    private class Probe(var current: JpegStageV2.SupervisorIdentity) : JpegRecoveryVerifier.Probe {
        var main = true
        var error = false
        var changedBoot = false
        val ended = mutableSetOf<JpegRecoveryVerifier.ProcessRef>()
        override fun currentSupervisor() = current
        override fun bootId() = if (changedBoot) "6c697468-6963-4000-8000-000000000009" else current.bootId
        override fun isMainProcess() = main
        override fun instanceEnded(identity: JpegRecoveryVerifier.ProcessRef): Boolean {
            if (error) throw IOException("UNKNOWN_PROC_ACCESS")
            return identity in ended
        }
    }
    private fun endedProbe() = Probe(oldHost.copy(pid = 200, startTicks = 333)).also {
        it.ended += JpegRecoveryVerifier.ProcessRef(100, 10123, 111)
        it.ended += JpegRecoveryVerifier.ProcessRef(101, 10123, 222)
    }
    private fun denied(action: () -> Unit) {
        try { action(); fail("Unproved recovery must be rejected") } catch (_: IOException) { }
    }
    private fun live(r: JpegStageV2.RecoveryRequest) = JpegRecoveryVerifier.LiveQuiescence(
        r.owner, oldHost, worker, hostFdsClosed = true, rpcQuiescent = true,
        originalSessionQuiescent = true, connectionCleaned = true, binderDead = true,
        matchingProcessDead = true, guardReleased = true)

    @Test fun exact_old_host_and_worker_ended_can_form_non_success_proof() {
        val r = request(); val proof = JpegRecoveryVerifier(endedProbe()).verify(r)
        assertEquals(r, proof.request)
        assertEquals(JpegStageV2.HostClosure.ORIGINAL_INSTANCE_ENDED, proof.hostClosure)
        assertTrue(proof.workerEnded && proof.hostFdsClosed && proof.noFutureRpc && proof.connectionCleaned)
    }
    @Test fun a_single_missing_host_pid_does_not_prove_worker_end() {
        val p = endedProbe(); p.ended.remove(JpegRecoveryVerifier.ProcessRef(101, 10123, 222))
        denied { JpegRecoveryVerifier(p).verify(request()) }
    }
    @Test fun missing_worker_does_not_prove_original_host_or_future_rpc_end() {
        val p = endedProbe(); p.ended.remove(JpegRecoveryVerifier.ProcessRef(100, 10123, 111))
        denied { JpegRecoveryVerifier(p).verify(request()) }
    }
    @Test fun same_host_requires_scoped_quiescent_session() {
        denied { JpegRecoveryVerifier(Probe(oldHost)).verify(request()) }
    }
    @Test fun same_host_with_all_live_and_os_facts_can_form_non_success_proof() {
        val r = request(); val p = Probe(oldHost).also { it.ended += JpegRecoveryVerifier.ProcessRef(101, 10123, 222) }
        val proof = JpegRecoveryVerifier(p).verify(r, live(r))
        assertEquals(JpegStageV2.HostClosure.SAME_SESSION_QUIESCENT, proof.hostClosure)
        assertFalse(proof.noConsumerActorsEnded)
    }
    @Test fun old_session_quiescence_cannot_close_another_attempt() {
        val r = request(); val p = endedProbe().also { it.current = oldHost }
        denied { JpegRecoveryVerifier(p).verify(r, live(r).copy(owner = r.owner.copy(sessionId = id()))) }
    }
    @Test fun old_worker_quiescence_cannot_close_a_different_process_instance() {
        val r = request(); val p = endedProbe().also { it.current = oldHost }
        denied { JpegRecoveryVerifier(p).verify(r, live(r).copy(worker = worker.copy(startTicks = 999))) }
    }
    @Test fun any_inflight_rpc_or_unknown_host_fd_blocks_recovery() {
        val r = request(); val p = endedProbe().also { it.current = oldHost }
        denied { JpegRecoveryVerifier(p).verify(r, live(r).copy(rpcQuiescent = false)) }
        denied { JpegRecoveryVerifier(p).verify(r, live(r).copy(hostFdsClosed = false)) }
    }
    @Test fun dispatch_failure_or_unproven_binder_guard_state_blocks_recovery() {
        val r = request(); val p = endedProbe().also { it.current = oldHost }
        denied { JpegRecoveryVerifier(p).verify(r, live(r).copy(connectionCleaned = false)) }
        denied { JpegRecoveryVerifier(p).verify(r, live(r).copy(binderDead = false)) }
        denied { JpegRecoveryVerifier(p).verify(r, live(r).copy(guardReleased = false)) }
    }
    @Test fun proc_permission_error_is_unknown_never_absence() {
        denied { JpegRecoveryVerifier(endedProbe().also { it.error = true }).verify(request()) }
    }
    @Test fun missing_consumer_identity_is_not_inferred_from_an_empty_pin() {
        denied { JpegRecoveryVerifier(endedProbe()).verify(request().copy(worker = null)) }
    }
    @Test fun uid_conflict_and_unknown_process_role_are_rejected() {
        denied { JpegRecoveryVerifier(endedProbe().also { it.main = false }).verify(request()) }
        denied { JpegRecoveryVerifier(endedProbe().also { it.current = it.current.copy(uid = 10124) }).verify(request()) }
    }
    @Test fun monotonic_boot_epoch_proves_prior_processes_ended_without_pid_guess() {
        val p = Probe(oldHost.copy(bootId = "6c697468-6963-4000-8000-000000000009", pid = 200))
        val proof = JpegRecoveryVerifier(p).verify(request())
        assertEquals(JpegStageV2.HostClosure.ORIGINAL_INSTANCE_ENDED, proof.hostClosure)
        assertTrue(proof.workerEnded && proof.hostFdsClosed)
    }
    @Test fun decreased_boot_count_and_unknown_epoch_cannot_prove_process_death() {
        denied { JpegRecoveryVerifier(Probe(oldHost.copy(bootId = "6c697468-6963-4000-8000-000000000007"))).verify(request()) }
        denied { JpegRecoveryVerifier(Probe(oldHost.copy(bootId = UUID.randomUUID().toString()))).verify(request()) }
    }
    @Test fun boot_observation_conflict_keeps_stage_unknown() {
        denied { JpegRecoveryVerifier(endedProbe().also { it.changedBoot = true }).verify(request()) }
    }
    @Test fun boot_count_encoding_is_canonical_and_has_checked_range() {
        assertEquals(boot, JpegRecoveryVerifier.bootId(8))
        assertEquals(0, JpegRecoveryVerifier.bootCount(JpegRecoveryVerifier.bootId(0)))
        assertEquals(Int.MAX_VALUE, JpegRecoveryVerifier.bootCount(JpegRecoveryVerifier.bootId(Int.MAX_VALUE)))
        denied { JpegRecoveryVerifier.bootId(-1) }
        denied { JpegRecoveryVerifier.bootCount("6c697468-6963-4000-8000-ffffffffffff") }
    }
}
