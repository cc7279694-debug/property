package com.cooper.lithic.m10r3.probe;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** Host JVM timing policy only, never evidence of Android scheduling or resource termination. */
public final class BindingTimelineTest {
    private static final long SECOND = 1_000_000_000L;
    private static final class Case {
        final AtomicLong now = new AtomicLong(SECOND);
        final AtomicReference<String> failure = new AtomicReference<>();
        final AtomicInteger failures = new AtomicInteger();
        final BindingTimeline timeline = new BindingTimeline(now::get,
                BindingTimeline.Limits.researchDefaults(), reason -> {
                    failures.incrementAndGet(); failure.compareAndSet(null, reason);
                });
        void advance(long seconds) { now.addAndGet(seconds * SECOND); }
        void connected() {
            assertTrue(timeline.posted()); assertTrue(timeline.callBegin());
            assertTrue(timeline.bindReturned(true, null)); assertTrue(timeline.connectedAccepted());
        }
        void ready() {
            connected(); assertTrue(timeline.reserveIssued());
            assertTrue(timeline.helloAccepted()); assertTrue(timeline.reserveReturned(true));
        }
    }
    @Test public void queuedTimeoutClosesMemoryFence() {
        AtomicLong now = new AtomicLong(1_000_000_000L);
        AtomicReference<String> failure = new AtomicReference<>();
        BindingTimeline timeline = new BindingTimeline(now::get,
                BindingTimeline.Limits.researchDefaults(), failure::set);
        assertTrue(timeline.posted());
        now.addAndGet(5_000_000_000L);
        timeline.checkDeadline();
        assertEquals("UI_BIND_QUEUE_TIMEOUT", failure.get());
        assertEquals("UI_BIND_QUEUE_TIMEOUT", timeline.firstFailure());
    }
    @Test public void normalBoundariesProduceBothReadyFacts() {
        Case c = new Case(); c.ready();
        assertTrue(c.timeline.bindingReady()); assertTrue(c.timeline.handshakeReady());
        assertEquals("INVOCATION_RUNNING", c.timeline.phase()); assertNull(c.failure.get());
    }
    @Test public void connectedMayPrecedeSuccessfulBindReturn() {
        Case c = new Case(); c.timeline.posted(); c.timeline.callBegin();
        assertTrue(c.timeline.connectedAccepted()); assertFalse(c.timeline.bindingReady());
        assertTrue(c.timeline.bindReturned(true, null)); assertTrue(c.timeline.bindingReady());
    }
    @Test public void helloMayPrecedeReserveReturnButDoesNotFinishHandshake() {
        Case c = new Case(); c.connected(); c.timeline.reserveIssued();
        assertTrue(c.timeline.helloAccepted()); assertFalse(c.timeline.handshakeReady());
        assertTrue(c.timeline.reserveReturned(true)); assertTrue(c.timeline.handshakeReady());
    }
    @Test public void reserveReturnMayPrecedeHello() {
        Case c = new Case(); c.connected(); c.timeline.reserveIssued();
        assertTrue(c.timeline.reserveReturned(true)); assertFalse(c.timeline.handshakeReady());
        assertTrue(c.timeline.helloAccepted()); assertTrue(c.timeline.handshakeReady());
    }
    @Test public void synchronousBindHasItsOwnDeadline() {
        Case c = new Case(); c.timeline.posted(); c.advance(1); c.timeline.callBegin();
        c.advance(5); c.timeline.checkDeadline();
        assertEquals("BIND_SYNC_CALL_TIMEOUT", c.failure.get());
    }
    @Test public void earlyConnectedDoesNotCancelSynchronousBindDeadline() {
        Case c = new Case(); c.timeline.posted(); c.timeline.callBegin(); c.timeline.connectedAccepted();
        c.advance(5); c.timeline.checkDeadline();
        assertEquals("BIND_SYNC_CALL_TIMEOUT", c.failure.get()); assertFalse(c.timeline.bindingReady());
    }
    @Test public void callbackWaitStartsAtActualSuccessfulBindReturn() {
        Case c = new Case(); c.timeline.posted(); c.timeline.callBegin(); c.advance(4);
        c.timeline.bindReturned(true, null); c.advance(4); c.timeline.checkDeadline();
        assertNull(c.failure.get()); c.advance(1); c.timeline.checkDeadline();
        assertEquals("CONNECTION_CALLBACK_TIMEOUT", c.failure.get());
    }
    @Test public void unissuedReserveIsBoundedAfterConnection() {
        Case c = new Case(); c.connected(); c.advance(5); c.timeline.checkDeadline();
        assertEquals("RESERVE_NOT_ISSUED_TIMEOUT", c.failure.get());
    }
    @Test public void helloWaitHasItsOwnDeadline() {
        Case c = new Case(); c.connected(); c.timeline.reserveIssued();
        c.timeline.reserveReturned(true); c.advance(5); c.timeline.checkDeadline();
        assertEquals("HELLO_TIMEOUT", c.failure.get());
    }
    @Test public void helloAloneDoesNotHideHungReserveCall() {
        Case c = new Case(); c.connected(); c.timeline.reserveIssued(); c.timeline.helloAccepted();
        c.advance(5); c.timeline.checkDeadline();
        assertEquals("RESERVE_SYNC_RETURN_TIMEOUT", c.failure.get());
    }
    @Test public void bindingTotalCannotBeResetByPhaseProgress() {
        Case c = new Case(); c.timeline.posted(); c.advance(4); c.timeline.callBegin();
        c.advance(4); c.timeline.bindReturned(true, null); c.advance(4);
        c.timeline.connectedAccepted(); c.timeline.reserveIssued(); c.advance(3);
        c.timeline.checkDeadline(); assertEquals("BINDING_TOTAL_TIMEOUT", c.failure.get());
    }
    @Test public void callbackAtExactDeadlineIsRejectedWithoutWaitingForTimer() {
        Case c = new Case(); c.timeline.posted(); c.advance(5);
        assertFalse(c.timeline.callBegin()); assertEquals("UI_BIND_QUEUE_TIMEOUT", c.failure.get());
    }
    @Test public void lateReturnIsRecordedButDoesNotRestoreAuthority() {
        Case c = new Case(); c.timeline.posted(); c.timeline.callBegin(); c.advance(5);
        c.timeline.checkDeadline(); assertFalse(c.timeline.bindReturned(true, null));
        assertTrue(c.timeline.snapshot().get("BIND_RETURNED_NANOS") > 0);
        assertFalse(c.timeline.bindingReady()); assertEquals("BIND_SYNC_CALL_TIMEOUT", c.failure.get());
    }
    @Test public void returnedFalseImmediatelyFences() {
        Case c = new Case(); c.timeline.posted(); c.timeline.callBegin();
        assertFalse(c.timeline.bindReturned(false, null)); assertEquals("BIND_RETURNED_FALSE", c.failure.get());
    }
    @Test public void thrownBindIsDistinctUnknownResult() {
        Case c = new Case(); c.timeline.posted(); c.timeline.callBegin();
        assertFalse(c.timeline.bindReturned(false, "SecurityException"));
        assertEquals("BIND_THROW_UNKNOWN", c.failure.get());
    }
    @Test public void rejectedReserveImmediatelyFences() {
        Case c = new Case(); c.connected(); c.timeline.reserveIssued();
        assertFalse(c.timeline.reserveReturned(false)); assertEquals("RESERVE_RETURNED_FALSE", c.failure.get());
    }
    @Test public void duplicateBoundaryCannotExtendDeadline() {
        Case c = new Case(); c.timeline.posted(); c.advance(4);
        assertFalse(c.timeline.posted()); assertEquals("DUPLICATE_UI_BIND_POSTED", c.failure.get());
        assertEquals(Long.valueOf(SECOND), c.timeline.snapshot().get("UI_BIND_POSTED_NANOS"));
    }
    @Test public void reserveBeforeBothBindingFactsIsRejected() {
        Case c = new Case(); c.timeline.posted(); c.timeline.callBegin(); c.timeline.connectedAccepted();
        assertFalse(c.timeline.reserveIssued()); assertEquals("OUT_OF_ORDER_RESERVE_ISSUED", c.failure.get());
    }
    @Test public void firstFailureIsDeliveredExactlyOnce() {
        Case c = new Case(); c.timeline.posted(); c.advance(5); c.timeline.checkDeadline();
        c.timeline.watchdogUnknown("SECOND"); c.timeline.checkDeadline(); c.timeline.callBegin();
        assertEquals(1, c.failures.get()); assertEquals("UI_BIND_QUEUE_TIMEOUT", c.timeline.firstFailure());
    }
    @Test public void invocationTotalRemainsArmedAfterHello() {
        Case c = new Case(); c.ready(); c.advance(29); c.timeline.checkDeadline(); assertNull(c.failure.get());
        c.advance(1); c.timeline.checkDeadline(); assertEquals("INVOCATION_TOTAL_TIMEOUT", c.failure.get());
    }
    @Test public void completionRequiresHandshakeFacts() {
        Case c = new Case(); c.connected(); assertFalse(c.timeline.completed());
        assertEquals("OUT_OF_ORDER_COMPLETED", c.failure.get());
    }
    @Test public void explicitCompletionStopsFutureDeadlineObservation() {
        Case c = new Case(); c.ready(); assertTrue(c.timeline.completed());
        c.advance(50); c.timeline.checkDeadline(); assertTrue(c.timeline.isCompleted());
        assertNull(c.failure.get()); assertFalse(c.timeline.helloAccepted());
    }
    @Test public void monotonicRegressionIsUnknownAndFailClosed() {
        Case c = new Case(); c.timeline.posted(); c.now.set(SECOND - 1); c.timeline.checkDeadline();
        assertEquals("MONOTONIC_CLOCK_REGRESSION", c.failure.get());
    }
    @Test public void negativeClockIsRejected() {
        AtomicReference<String> failure = new AtomicReference<>();
        BindingTimeline timeline = new BindingTimeline(() -> -1L, BindingTimeline.Limits.researchDefaults(), failure::set);
        assertFalse(timeline.posted()); assertEquals("MONOTONIC_CLOCK_REGRESSION", failure.get());
    }
    @Test public void clockExceptionIsExplicitUnknown() {
        AtomicReference<String> failure = new AtomicReference<>();
        BindingTimeline timeline = new BindingTimeline(() -> { throw new IllegalStateException("clock"); },
                BindingTimeline.Limits.researchDefaults(), failure::set);
        assertFalse(timeline.posted()); assertEquals("MONOTONIC_CLOCK_UNKNOWN", failure.get());
    }
    @Test public void arithmeticOverflowRejectsInsteadOfWrappingDeadline() {
        AtomicReference<String> failure = new AtomicReference<>();
        BindingTimeline timeline = new BindingTimeline(() -> Long.MAX_VALUE - 1,
                BindingTimeline.Limits.researchDefaults(), failure::set);
        assertFalse(timeline.posted()); assertEquals("DEADLINE_ARITHMETIC_UNKNOWN", failure.get());
    }
    @Test public void failureCallbackRunsOutsideTimelineMonitor() {
        AtomicReference<BindingTimeline> holder = new AtomicReference<>();
        AtomicLong now = new AtomicLong(SECOND);
        BindingTimeline timeline = new BindingTimeline(now::get, BindingTimeline.Limits.researchDefaults(),
                reason -> assertFalse(Thread.holdsLock(holder.get())));
        holder.set(timeline); timeline.posted(); now.addAndGet(5 * SECOND); timeline.checkDeadline();
        assertNull(timeline.failureDeliveryError());
    }
    @Test public void failureCallbackExceptionIsPreservedAsUnknown() {
        AtomicLong now = new AtomicLong(SECOND); IllegalStateException error = new IllegalStateException("sink");
        BindingTimeline timeline = new BindingTimeline(now::get, BindingTimeline.Limits.researchDefaults(),
                reason -> { throw error; });
        timeline.posted(); now.addAndGet(5 * SECOND); timeline.checkDeadline();
        assertSame(error, timeline.failureDeliveryError());
        assertEquals(Long.valueOf(1), timeline.snapshot().get("FAILURE_DELIVERY_UNKNOWN"));
    }
    @Test public void snapshotContainsThreadAndNanosForEveryAcceptedBoundary() {
        Case c = new Case(); c.ready(); Map<String, Long> snapshot = c.timeline.snapshot();
        for (String name : new String[]{"UI_BIND_POSTED", "BIND_CALL_BEGIN", "BIND_RETURNED",
                "CONNECTED_ACCEPTED", "RESERVE_ISSUED", "HELLO_ACCEPTED", "RESERVE_RETURNED"}) {
            assertTrue(snapshot.get(name + "_NANOS") >= SECOND);
            assertTrue(snapshot.get(name + "_THREAD_ID") > 0);
        }
        try { snapshot.put("spoof", 1L); fail("snapshot mutable"); }
        catch (UnsupportedOperationException expected) { }
    }
    @Test public void unpostedTimelineDoesNotInventDeadlineOrClaimSuccess() {
        Case c = new Case(); c.advance(100); c.timeline.checkDeadline();
        assertNull(c.failure.get()); assertEquals("NOT_POSTED", c.timeline.phase());
        assertFalse(c.timeline.bindingReady()); assertFalse(c.timeline.handshakeReady());
    }
    @Test public void invalidBudgetsAreRejected() {
        try { new BindingTimeline.Limits(0, 1, 1, 1, 2, 3); fail("zero budget"); }
        catch (IllegalArgumentException expected) { }
        try { new BindingTimeline.Limits(1, 1, 1, 1, 3, 2); fail("inverted totals"); }
        catch (IllegalArgumentException expected) { }
    }
    @Test public void snapshotDerivesActualMonotonicPhaseDurations() {
        Case c = new Case(); c.timeline.posted(); c.advance(1); c.timeline.callBegin();
        c.advance(2); c.timeline.bindReturned(true, null); c.advance(1); c.timeline.connectedAccepted();
        c.advance(1); c.timeline.reserveIssued(); c.advance(2); c.timeline.helloAccepted();
        c.advance(1); c.timeline.reserveReturned(true);
        Map<String, Long> snapshot = c.timeline.snapshot();
        assertEquals(Long.valueOf(SECOND), snapshot.get("queueNanos"));
        assertEquals(Long.valueOf(2 * SECOND), snapshot.get("bindSyncNanos"));
        assertEquals(Long.valueOf(3 * SECOND), snapshot.get("connectedFromCallNanos"));
        assertEquals(Long.valueOf(SECOND), snapshot.get("connectionAfterReturnNanos"));
        assertEquals(Long.valueOf(2 * SECOND), snapshot.get("helloNanos"));
        assertEquals(Long.valueOf(3 * SECOND), snapshot.get("reserveSyncNanos"));
    }
    @Test public void durationSnapshotPreservesMissingAndEarlyCallbackSemantics() {
        Case c = new Case();
        for (String key : new String[]{"queueNanos", "bindSyncNanos", "connectedFromCallNanos",
                "connectionAfterReturnNanos", "helloNanos", "reserveSyncNanos"})
            assertEquals(Long.valueOf(-1), c.timeline.snapshot().get(key));
        c.timeline.posted(); c.timeline.callBegin(); c.advance(1); c.timeline.connectedAccepted();
        c.advance(1); c.timeline.bindReturned(true, null);
        Map<String, Long> snapshot = c.timeline.snapshot();
        assertEquals(Long.valueOf(0), snapshot.get("queueNanos"));
        assertEquals(Long.valueOf(2 * SECOND), snapshot.get("bindSyncNanos"));
        assertEquals(Long.valueOf(SECOND), snapshot.get("connectedFromCallNanos"));
        assertEquals(Long.valueOf(0), snapshot.get("connectionAfterReturnNanos"));
        assertEquals(Long.valueOf(-1), snapshot.get("helloNanos"));
        assertEquals(Long.valueOf(-1), snapshot.get("reserveSyncNanos"));
    }
}
