package com.cooper.lithic.m10r3.probe;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

/** Independent host timer execution is not an Android worst-case response-time guarantee. */
public final class DeadlineWatchdogTest {
    @Test public void independentTimerClosesFenceWithoutCallerProgress() throws Exception {
        AtomicLong now = new AtomicLong(1_000_000_000L);
        CountDownLatch fenced = new CountDownLatch(1);
        BindingTimeline timeline = new BindingTimeline(now::get,
                BindingTimeline.Limits.researchDefaults(), reason -> fenced.countDown());
        timeline.posted(); now.addAndGet(5_000_000_000L);
        try (DeadlineWatchdog watchdog = new DeadlineWatchdog(timeline)) {
            watchdog.start();
            assertTrue("watchdog must progress independently", fenced.await(2, TimeUnit.SECONDS));
        }
    }
    @Test public void watchdogProgressesWhileSyntheticUiBindAndExecutorAreBlocked() throws Exception {
        CountDownLatch entered = new CountDownLatch(3), release = new CountDownLatch(1), fenced = new CountDownLatch(1);
        Thread[] blocked = new Thread[3];
        for (int i = 0; i < blocked.length; i++) {
            blocked[i] = new Thread(() -> {
                entered.countDown();
                try { release.await(); } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
            }, "synthetic-blocked-" + i); blocked[i].start();
        }
        AtomicLong now = new AtomicLong(1_000_000_000L);
        AtomicReference<String> timerThread = new AtomicReference<>();
        BindingTimeline timeline = new BindingTimeline(now::get, BindingTimeline.Limits.researchDefaults(),
                reason -> { timerThread.set(Thread.currentThread().getName()); fenced.countDown(); });
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS)); timeline.posted(); now.addAndGet(5_000_000_000L);
            try (DeadlineWatchdog watchdog = new DeadlineWatchdog(timeline)) {
                watchdog.start(); assertTrue(fenced.await(2, TimeUnit.SECONDS));
                assertEquals("lithic-r3-host-deadline", timerThread.get());
                for (Thread thread : blocked) assertTrue(thread.isAlive());
            }
        } finally { release.countDown(); for (Thread thread : blocked) thread.join(2000); }
    }
    @Test public void closingTimerDoesNotAssertTaskOrResourceTermination() {
        AtomicLong now = new AtomicLong(1_000_000_000L);
        BindingTimeline timeline = new BindingTimeline(now::get, BindingTimeline.Limits.researchDefaults(), reason -> { });
        timeline.posted(); DeadlineWatchdog watchdog = new DeadlineWatchdog(timeline);
        watchdog.start(); watchdog.close(); watchdog.close();
        assertFalse(timeline.isCompleted()); assertNull(timeline.firstFailure());
    }
    @Test public void watchdogCannotBeStartedTwice() {
        BindingTimeline timeline = new BindingTimeline(() -> 1L, BindingTimeline.Limits.researchDefaults(), reason -> { });
        try (DeadlineWatchdog watchdog = new DeadlineWatchdog(timeline)) {
            watchdog.start();
            try { watchdog.start(); fail("duplicate watchdog"); } catch (IllegalStateException expected) { }
        }
    }
    @Test public void closedWatchdogCannotBeStarted() {
        BindingTimeline timeline = new BindingTimeline(() -> 1L, BindingTimeline.Limits.researchDefaults(), reason -> { });
        DeadlineWatchdog watchdog = new DeadlineWatchdog(timeline); watchdog.close();
        try { watchdog.start(); fail("closed watchdog"); } catch (IllegalStateException expected) { }
    }
    @Test public void timerFencesRealInvocationAndPreservesFirstFailure() throws Exception {
        InvocationFence fence = new InvocationFence(); InvocationFence.Ticket ticket = fence.begin();
        SessionGate gate = new SessionGate(1, "nonce", "attempt", "generation", "sha", 8192, 10123);
        fence.attach(ticket, gate);
        assertTrue(fence.finishCandidate(fence.beginCandidate(ticket, InvocationFence.CandidateKind.CURRENT)));
        AtomicLong now = new AtomicLong(1_000_000_000L); CountDownLatch failed = new CountDownLatch(1);
        BindingTimeline timeline = new BindingTimeline(now::get, BindingTimeline.Limits.researchDefaults(), reason -> {
            fence.fail(ticket, reason); failed.countDown();
        });
        timeline.posted(); now.addAndGet(5_000_000_000L);
        try (DeadlineWatchdog watchdog = new DeadlineWatchdog(timeline)) {
            watchdog.start(); assertTrue(failed.await(2, TimeUnit.SECONDS));
        }
        assertFalse(fence.isCurrentOpen(ticket)); assertTrue(gate.isRevoked());
        fence.fail(ticket, "LATE_CALLBACK"); assertEquals("UI_BIND_QUEUE_TIMEOUT", ticket.firstFailure());
        assertFalse(fence.decide(ticket, gate::grant));
    }
}
