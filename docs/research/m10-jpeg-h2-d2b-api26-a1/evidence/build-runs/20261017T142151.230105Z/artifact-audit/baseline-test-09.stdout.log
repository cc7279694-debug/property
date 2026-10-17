package com.cooper.lithic.m10r3.probe;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/** Real Java in-memory coordinator; filesystem durability and Binder/OS behavior remain Android work. */
public final class InvocationFenceTest {
    private static final class Case {
        final InvocationFence fence = new InvocationFence();
        final InvocationFence.Ticket ticket = fence.begin();
        final SessionGate gate = gate();
        Case() { fence.attach(ticket, gate); }
        void publish() {
            assertTrue(fence.finishCandidate(fence.beginCandidate(ticket, InvocationFence.CandidateKind.CURRENT)));
        }
        void hello() { assertTrue(fence.decide(ticket, () -> gate.handshake(10123, 4000, 4000, 10123, true))); }
        void ready() {
            publish(); hello();
            assertTrue(fence.decide(ticket, () -> result(gate)));
            gate.died();
            assertTrue(gate.canPrepare());
        }
        void terminal() {
            ready();
            assertTrue(fence.finishCandidate(fence.beginCandidate(ticket, InvocationFence.CandidateKind.RECEIPT)));
            assertTrue(fence.markTerminal(ticket));
        }
    }
    private static SessionGate gate() { return new SessionGate(1, "nonce", "attempt", "generation", "sha", 8192, 10123); }
    private static boolean result(SessionGate gate) {
        return gate.result(1, "nonce", "attempt", "generation", "sha", 8192, "SUCCESS", true);
    }
    private static void denied(Runnable action) {
        try { action.run(); fail("expected fail-closed rejection"); }
        catch (IllegalStateException | IllegalArgumentException expected) { }
    }

    @Test public void nonterminalInvocationBlocksSecondAdmission() {
        Case c = new Case(); denied(c.fence::begin);
    }
    @Test public void creationAdmissionExistsBeforeGateButFailureClosesIt() {
        InvocationFence fence = new InvocationFence();
        InvocationFence.Ticket ticket = fence.begin();
        assertTrue(fence.isAdmitted(ticket)); assertFalse(fence.isCurrentOpen(ticket));
        assertFalse(fence.decide(ticket, () -> true));
        fence.fail(ticket, "CREATE_FAILED");
        assertFalse(fence.isAdmitted(ticket)); assertFalse(fence.isCurrentOpen(ticket));
    }
    @Test public void creationFailureBeforeSessionAttachStillClosesAdmission() {
        InvocationFence fence = new InvocationFence();
        InvocationFence.Ticket ticket = fence.begin();
        fence.fail(ticket, "CREATE_UNKNOWN");
        assertEquals("CREATE_UNKNOWN", ticket.firstFailure());
        denied(fence::begin);
    }
    @Test public void gateAttachedAfterEarlyFailureIsImmediatelyRevoked() {
        InvocationFence fence = new InvocationFence();
        InvocationFence.Ticket ticket = fence.begin(); fence.fail(ticket, "CREATE_FAILED");
        SessionGate gate = gate(); fence.attach(ticket, gate);
        assertTrue(gate.isRevoked()); assertFalse(fence.isCurrentOpen(ticket));
    }
    @Test public void sessionAttachmentCannotBeReplaced() {
        Case c = new Case(); denied(() -> c.fence.attach(c.ticket, gate()));
    }
    @Test public void unpublishedCurrentCannotAuthorizeCallbackMutation() {
        Case c = new Case(); AtomicInteger called = new AtomicInteger();
        assertFalse(c.fence.decide(c.ticket, () -> { called.incrementAndGet(); return true; }));
        assertEquals(0, called.get());
    }
    @Test public void firstFailureReasonCannotBeOverwrittenByDeathOrLaterFailure() {
        Case c = new Case(); c.publish(); c.hello();
        c.fence.fail(c.ticket, "BIND_TIMEOUT"); c.gate.died(); c.fence.fail(c.ticket, "REPORT_WRITE_FAILED");
        assertEquals("BIND_TIMEOUT", c.ticket.firstFailure()); assertEquals("BIND_TIMEOUT", c.gate.status());
    }
    @Test public void failureRejectsHelloGrantResultAndPrepare() {
        Case c = new Case(); c.publish(); c.fence.fail(c.ticket, "TIMEOUT");
        assertFalse(c.fence.decide(c.ticket, () -> c.gate.handshake(10123, 4000, 4000, 10123, true)));
        assertFalse(c.fence.decide(c.ticket, c.gate::grant));
        assertFalse(c.fence.decide(c.ticket, () -> result(c.gate)));
        c.gate.died(); assertFalse(c.gate.canPrepare()); denied(c.fence::begin);
    }
    @Test public void failureBeforePublicationPreventsCandidateCreation() {
        Case c = new Case(); c.fence.fail(c.ticket, "BEFORE_PUBLISH");
        denied(() -> c.fence.beginCandidate(c.ticket, InvocationFence.CandidateKind.CURRENT));
    }
    @Test public void failureDuringCurrentWriteRejectsItsLogicalCommit() {
        Case c = new Case();
        InvocationFence.Candidate candidate = c.fence.beginCandidate(c.ticket, InvocationFence.CandidateKind.CURRENT);
        c.fence.fail(c.ticket, "WRITE_UNKNOWN");
        assertFalse(c.fence.finishCandidate(candidate));
        assertFalse(c.fence.decide(c.ticket, () -> true)); denied(c.fence::begin);
    }
    @Test public void currentPublicationCandidateIsSingleUseEvenBeforeItsCompletion() {
        Case c = new Case(); InvocationFence.Candidate candidate = c.fence.beginCandidate(c.ticket, InvocationFence.CandidateKind.CURRENT);
        denied(() -> c.fence.beginCandidate(c.ticket, InvocationFence.CandidateKind.CURRENT));
        assertTrue(c.fence.finishCandidate(candidate)); assertFalse(c.fence.finishCandidate(candidate));
    }
    @Test public void receiptRequiresMatchingResultDeathAndDataFdClosure() {
        Case c = new Case(); c.publish(); c.hello();
        denied(() -> c.fence.beginCandidate(c.ticket, InvocationFence.CandidateKind.RECEIPT));
        assertTrue(c.fence.decide(c.ticket, () -> result(c.gate)));
        denied(() -> c.fence.beginCandidate(c.ticket, InvocationFence.CandidateKind.RECEIPT));
        c.gate.died(); assertNotNull(c.fence.beginCandidate(c.ticket, InvocationFence.CandidateKind.RECEIPT));
    }
    @Test public void failureDuringReceiptWriteForbidsCommitAndTerminalAdmission() {
        Case c = new Case(); c.ready();
        InvocationFence.Candidate candidate = c.fence.beginCandidate(c.ticket, InvocationFence.CandidateKind.RECEIPT);
        c.fence.fail(c.ticket, "RECEIPT_UNKNOWN");
        assertFalse(c.fence.finishCandidate(candidate)); assertFalse(c.fence.markTerminal(c.ticket)); denied(c.fence::begin);
    }
    @Test public void receiptCandidateCannotBeRetriedOrCommittedTwice() {
        Case c = new Case(); c.ready();
        InvocationFence.Candidate candidate = c.fence.beginCandidate(c.ticket, InvocationFence.CandidateKind.RECEIPT);
        denied(() -> c.fence.beginCandidate(c.ticket, InvocationFence.CandidateKind.RECEIPT));
        assertTrue(c.fence.finishCandidate(candidate)); assertFalse(c.fence.finishCandidate(candidate));
    }
    @Test public void receiptAloneDoesNotMakeOldInvocationTerminal() {
        Case c = new Case(); c.ready();
        assertTrue(c.fence.finishCandidate(c.fence.beginCandidate(c.ticket, InvocationFence.CandidateKind.RECEIPT)));
        denied(c.fence::begin); assertFalse(c.fence.isTerminal(c.ticket));
    }
    @Test public void successfulTerminalCompletionAllowsExactlyOneLaterEpoch() {
        Case c = new Case(); c.terminal(); assertTrue(c.fence.markTerminal(c.ticket));
        InvocationFence.Ticket next = c.fence.begin(); assertEquals(c.ticket.epoch() + 1, next.epoch());
        denied(c.fence::begin);
    }
    @Test public void currentPostTerminalReportFailureClosesFurtherAdmission() {
        Case c = new Case(); c.terminal();
        c.fence.fail(c.ticket, "FINAL_REPORT_SYNC_UNKNOWN");
        assertFalse(c.fence.isTerminal(c.ticket)); assertFalse(c.fence.markTerminal(c.ticket));
        assertEquals("FINAL_REPORT_SYNC_UNKNOWN", c.ticket.firstFailure()); denied(c.fence::begin);
    }
    @Test public void staleFailureCannotRevokeNewInvocation() {
        Case c = new Case(); c.terminal(); InvocationFence.Ticket next = c.fence.begin();
        SessionGate nextGate = gate(); c.fence.attach(next, nextGate);
        c.fence.fail(c.ticket, "LATE_OLD_CATCH");
        assertNull(next.firstFailure()); assertFalse(nextGate.isRevoked()); assertTrue(c.fence.isCurrentOpen(next));
    }
    @Test public void staleCallbackCannotMutateNewInvocationOrItsEndpoint() {
        Case c = new Case(); c.terminal(); InvocationFence.Ticket next = c.fence.begin();
        SessionGate nextGate = gate(); c.fence.attach(next, nextGate); AtomicInteger mutation = new AtomicInteger();
        assertFalse(c.fence.decide(c.ticket, () -> { mutation.incrementAndGet(); return true; }));
        assertEquals(0, mutation.get()); assertFalse(nextGate.isRevoked());
    }
    @Test public void foreignTicketAndCandidateCannotAuthorizeThisCoordinator() {
        Case first = new Case(), second = new Case();
        assertFalse(first.fence.isCurrentOpen(second.ticket));
        denied(() -> first.fence.attach(second.ticket, gate()));
        denied(() -> first.fence.fail(second.ticket, "FOREIGN"));
        InvocationFence.Candidate candidate = second.fence.beginCandidate(second.ticket, InvocationFence.CandidateKind.CURRENT);
        assertFalse(first.fence.finishCandidate(candidate)); assertTrue(second.fence.finishCandidate(candidate));
    }
    @Test public void healthyDeathBeforeResultRemainsAllowedExactlyOnce() {
        Case c = new Case(); c.publish(); c.hello(); c.gate.died();
        assertTrue(c.fence.decide(c.ticket, () -> result(c.gate)));
        assertFalse(c.fence.decide(c.ticket, () -> result(c.gate))); assertTrue(c.gate.canPrepare());
    }
    @Test public void failureAndGrantShareOneShortLinearizationBoundary() throws Exception {
        for (int iteration = 0; iteration < 128; iteration++) {
            Case c = new Case(); c.publish(); c.hello(); CountDownLatch start = new CountDownLatch(1);
            AtomicInteger grants = new AtomicInteger();
            Thread grant = new Thread(() -> { await(start); if (c.fence.decide(c.ticket, c.gate::grant)) grants.incrementAndGet(); });
            Thread failure = new Thread(() -> { await(start); c.fence.fail(c.ticket, "RACE_FAILURE"); });
            grant.start(); failure.start(); start.countDown(); grant.join(5000); failure.join(5000);
            assertFalse(grant.isAlive()); assertFalse(failure.isAlive()); assertTrue(grants.get() <= 1);
            assertFalse(c.fence.decide(c.ticket, c.gate::grant)); assertEquals("RACE_FAILURE", c.ticket.firstFailure());
        }
    }
    private static void await(CountDownLatch latch) {
        try { latch.await(); } catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
    }
}
