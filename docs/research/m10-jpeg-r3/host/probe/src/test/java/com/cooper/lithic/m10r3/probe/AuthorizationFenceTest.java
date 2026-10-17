package com.cooper.lithic.m10r3.probe;

import static org.junit.Assert.*;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/** Host decision tests only, not Android Binder/FD/lease evidence. */
public final class AuthorizationFenceTest {
    @Test public void revokedPriorAttemptCannotConsumeQueuedResult() throws Exception {
        AtomicInteger mutations = new AtomicInteger();
        assertFalse(AuthorizationFence.decide(new Object(), () -> false, () -> { mutations.incrementAndGet(); return true; }));
        assertEquals(0, mutations.get());
    }
    @Test public void replacedCurrentCannotHandshakeOrGrantAgain() throws Exception {
        Object lock = new Object(); String[] current = {"old"};
        assertTrue(AuthorizationFence.decide(lock, () -> current[0].equals("old"), () -> true));
        synchronized (lock) { current[0] = "new"; }
        for (int i = 0; i < 3; i++) assertFalse(AuthorizationFence.decide(lock, () -> current[0].equals("old"), () -> true));
    }
    @Test public void corruptDurableAuthorityDoesNotRunMutation() throws Exception {
        AtomicInteger mutations = new AtomicInteger();
        try {
            AuthorizationFence.decide(new Object(), () -> { throw new IOException("corrupt owner"); }, () -> { mutations.incrementAndGet(); return true; });
            fail("corrupt authority must be reported to fail-closed callback");
        } catch (IOException expected) { assertEquals("corrupt owner", expected.getMessage()); }
        assertEquals(0, mutations.get());
    }
    @Test public void validCurrentKeepsSessionGateDuplicateProtection() throws Exception {
        Object lock = new Object();
        SessionGate gate = new SessionGate(1,"n","a","g","sha",8192,10123);
        assertTrue(AuthorizationFence.decide(lock, () -> true, () -> gate.handshake(10123,123,123,10123,true)));
        assertTrue(AuthorizationFence.decide(lock, () -> true, () -> gate.result(1,"n","a","g","sha",8192,"SUCCESS",true)));
        assertFalse(AuthorizationFence.decide(lock, () -> true, () -> gate.result(1,"n","a","g","sha",8192,"SUCCESS",true)));
    }
    @Test public void closeAndPublishCannotInterleaveBetweenAuthorityAndMutation() throws Exception {
        Object lock = new Object(); CountDownLatch checked = new CountDownLatch(1), release = new CountDownLatch(1), closerStarted = new CountDownLatch(1);
        AtomicInteger sequence = new AtomicInteger(), acceptedAt = new AtomicInteger(), closedAt = new AtomicInteger();
        Thread callback = new Thread(() -> {
            try { AuthorizationFence.decide(lock, () -> { checked.countDown(); if (!release.await(3,TimeUnit.SECONDS)) throw new IOException("test release timeout"); return true; }, () -> { acceptedAt.set(sequence.incrementAndGet()); return true; }); }
            catch (Exception error) { acceptedAt.set(-1); }
        });
        callback.start();
        try {
            assertTrue(checked.await(3,TimeUnit.SECONDS));
            Thread closer = new Thread(() -> { closerStarted.countDown(); synchronized (lock) { closedAt.set(sequence.incrementAndGet()); } });
            closer.start(); assertTrue(closerStarted.await(3,TimeUnit.SECONDS));
            assertEquals(0, closedAt.get()); release.countDown(); callback.join(3000); closer.join(3000);
            assertFalse(callback.isAlive()); assertFalse(closer.isAlive()); assertEquals(1, acceptedAt.get()); assertEquals(2, closedAt.get());
        } finally { release.countDown(); callback.join(3000); }
    }
}
