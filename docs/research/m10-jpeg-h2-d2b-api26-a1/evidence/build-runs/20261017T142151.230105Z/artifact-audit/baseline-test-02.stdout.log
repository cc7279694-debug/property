package com.cooper.lithic.m10r3.probe;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** Tests the same shared binding adapter used by ProbeRunner, without Android calls. */
public final class BindingFailureFenceIntegrationTest {
    @Test public void returnedFalseFencesInvocationBeforeMatchingUnbind() {
        InvocationFence fence = new InvocationFence();
        InvocationFence.Ticket ticket = fence.begin();
        SessionGate gate = new SessionGate(1, "nonce", "attempt", "generation", "sha", 8192, 10123);
        fence.attach(ticket, gate);
        assertTrue(fence.finishCandidate(fence.beginCandidate(ticket, InvocationFence.CandidateKind.CURRENT)));
        AtomicInteger unbinds = new AtomicInteger();
        final BindingResponsibility[] owner = new BindingResponsibility[1];
        owner[0] = new BindingResponsibility(BindingResponsibility.fenced(new BindingResponsibility.Operations() {
            public boolean bind() { assertFalse(Thread.holdsLock(fence)); return false; }
            public void unbind() {
                unbinds.incrementAndGet();
                assertFalse("failure must precede external cleanup", fence.isCurrentOpen(ticket));
                assertFalse(Thread.holdsLock(fence)); assertFalse(Thread.holdsLock(owner[0]));
            }
        }, (reason, failure) -> {
            assertFalse(Thread.holdsLock(owner[0]));
            assertNull(failure); fence.fail(ticket, reason);
        }));
        owner[0].executeBind(() -> fence.isCurrentOpen(ticket));
        assertEquals(1, unbinds.get());
        assertEquals("BIND_RETURNED_FALSE", ticket.firstFailure());
        assertTrue(gate.isRevoked());
        assertEquals(BindingResponsibility.CleanupState.UNBOUND, owner[0].cleanupState());
    }

    @Test public void thrownBindFencesBeforeUnbindAndPreservesOriginalException() {
        InvocationFence fence = new InvocationFence();
        InvocationFence.Ticket ticket = fence.begin();
        SessionGate gate = new SessionGate(1, "nonce", "attempt", "generation", "sha", 8192, 10123);
        fence.attach(ticket, gate);
        assertTrue(fence.finishCandidate(fence.beginCandidate(ticket, InvocationFence.CandidateKind.CURRENT)));
        SecurityException original = new SecurityException("Android registration rejected");
        AtomicInteger unbinds = new AtomicInteger();
        final BindingResponsibility[] owner = new BindingResponsibility[1];
        owner[0] = new BindingResponsibility(BindingResponsibility.fenced(new BindingResponsibility.Operations() {
            public boolean bind() { assertFalse(Thread.holdsLock(fence)); throw original; }
            public void unbind() {
                unbinds.incrementAndGet();
                assertFalse("failure must precede external cleanup", fence.isCurrentOpen(ticket));
                assertFalse(Thread.holdsLock(fence)); assertFalse(Thread.holdsLock(owner[0]));
            }
        }, (reason, failure) -> {
            assertSame(original, failure); assertFalse(Thread.holdsLock(owner[0]));
            fence.fail(ticket, reason);
        }));
        owner[0].executeBind(() -> fence.isCurrentOpen(ticket));
        assertEquals(1, unbinds.get()); assertSame(original, owner[0].bindingError());
        assertEquals("BIND_THROW", ticket.firstFailure()); assertTrue(gate.isRevoked());
        assertEquals(BindingResponsibility.CleanupState.UNBOUND, owner[0].cleanupState());
    }
}
