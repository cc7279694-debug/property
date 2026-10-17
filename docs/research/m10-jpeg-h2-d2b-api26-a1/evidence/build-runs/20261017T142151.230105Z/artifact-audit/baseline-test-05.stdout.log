package com.cooper.lithic.m10r3.probe;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/** JVM scheduling and diagnostic policy only; no Android/logd/fsync claim. */
public final class BoundedTraceWriterTest {
    @Test public void blockedSinkCannotBlockProducer() throws Exception {
        CountDownLatch sinkEntered = new CountDownLatch(1), release = new CountDownLatch(1), returned = new CountDownLatch(1);
        BoundedTraceWriter writer = new BoundedTraceWriter(2, 1024, entry -> { sinkEntered.countDown(); release.await(); });
        Thread producer = new Thread(() -> { writer.offer("event"); returned.countDown(); }, "synthetic-ui-producer");
        try {
            producer.start();
            assertTrue("controlled sink did not start", sinkEntered.await(2, TimeUnit.SECONDS));
            assertTrue("producer waits for diagnostic sink", returned.await(250, TimeUnit.MILLISECONDS));
        } finally {
            release.countDown(); producer.join(2000); writer.close();
        }
    }

    @Test public void successfulDrainHasCompleteCounts() throws Exception {
        try (BoundedTraceWriter writer = new BoundedTraceWriter(8, 1024, entry -> { })) {
            assertTrue(writer.offer("one")); assertTrue(writer.offer("two"));
            assertTrue(writer.awaitDrained(2, TimeUnit.SECONDS));
            BoundedTraceWriter.Health health = writer.snapshot();
            assertEquals("COMPLETE", health.status); assertEquals(2, health.attempted);
            assertEquals(2, health.accepted); assertEquals(2, health.written);
            assertEquals(0, health.pending); assertEquals(0, health.dropped);
            assertFalse(health.hasLoss()); assertNull(health.firstFailure);
        }
    }

    @Test public void sourceThreadMetadataAndSequenceAreCaptured() throws Exception {
        List<BoundedTraceWriter.Entry> entries = Collections.synchronizedList(new ArrayList<>());
        try (BoundedTraceWriter writer = new BoundedTraceWriter(8, 1024, entries::add)) {
            assertTrue(writer.offer("first")); assertTrue(writer.offer("second"));
            assertTrue(writer.awaitDrained(2, TimeUnit.SECONDS));
            assertEquals(2, entries.size()); assertEquals(1, entries.get(0).sequence);
            assertEquals(2, entries.get(1).sequence);
            assertEquals(Thread.currentThread().getName(), entries.get(0).producerThread);
            assertTrue(entries.get(0).createdNano != 0);
            assertEquals("second", entries.get(1).payload); assertEquals(6, entries.get(1).utf8Bytes);
        }
    }

    @Test public void blockedInFlightEntryConsumesCapacity() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        try (BoundedTraceWriter writer = new BoundedTraceWriter(1, 1024, entry -> { started.countDown(); release.await(); })) {
            assertTrue(writer.offer("one")); assertTrue(started.await(2, TimeUnit.SECONDS));
            assertFalse(writer.offer("two"));
            BoundedTraceWriter.Health health = writer.snapshot();
            assertEquals(1, health.pending); assertEquals(1, health.dropped);
            assertEquals("UNKNOWN", health.status); assertEquals("QUEUE_FULL", health.firstFailure);
        } finally { release.countDown(); }
    }

    @Test public void queueOverflowCannotBecomeCompleteAfterLaterDrain() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        try (BoundedTraceWriter writer = new BoundedTraceWriter(1, 1024, entry -> { started.countDown(); release.await(); })) {
            assertTrue(writer.offer("one")); assertTrue(started.await(2, TimeUnit.SECONDS));
            assertFalse(writer.offer("dropped")); release.countDown();
            assertFalse(writer.awaitDrained(2, TimeUnit.SECONDS));
            assertEquals("UNKNOWN", writer.snapshot().status); assertTrue(writer.snapshot().hasLoss());
        } finally { release.countDown(); }
    }

    @Test public void sinkFailureIsStickyAndNextEvidenceCanStillBeWritten() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (BoundedTraceWriter writer = new BoundedTraceWriter(8, 1024, entry -> {
            if (calls.getAndIncrement() == 0) throw new IOException("synthetic-fsync-error");
        })) {
            assertTrue(writer.offer("first")); assertTrue(writer.offer("second"));
            assertFalse(writer.awaitDrained(2, TimeUnit.SECONDS));
            BoundedTraceWriter.Health health = writer.snapshot();
            assertEquals(2, calls.get()); assertEquals(1, health.writeFailures); assertEquals(1, health.written);
            assertEquals(0, health.pending); assertEquals("SINK_FAILURE", health.firstFailure);
            assertEquals(IOException.class.getName(), health.firstFailureDetail);
            assertTrue(health.firstFailureCause.getMessage().contains("synthetic-fsync-error"));
        }
    }

    @Test public void sinkRunsOnDedicatedWriterInsteadOfProducer() throws Exception {
        List<String> names = Collections.synchronizedList(new ArrayList<>());
        try (BoundedTraceWriter writer = new BoundedTraceWriter(8, 1024, entry -> names.add(Thread.currentThread().getName()))) {
            writer.offer("first"); assertTrue(writer.awaitDrained(2, TimeUnit.SECONDS));
            assertNotEquals(Thread.currentThread().getName(), names.get(0));
            assertTrue(names.get(0).startsWith("research-host-trace"));
        }
    }

    @Test public void oversizedPayloadIsExplicitlyRejected() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (BoundedTraceWriter writer = new BoundedTraceWriter(2, 4, entry -> calls.incrementAndGet())) {
            assertFalse(writer.offer("12345")); assertFalse(writer.awaitDrained(2, TimeUnit.SECONDS));
            assertEquals(0, calls.get()); assertEquals(1, writer.snapshot().dropped);
            assertEquals("PAYLOAD_TOO_LARGE", writer.snapshot().firstFailure);
        }
    }

    @Test public void utf8BoundCountsBytesInsteadOfCharacters() throws Exception {
        try (BoundedTraceWriter writer = new BoundedTraceWriter(2, 4, entry -> { })) {
            assertTrue(writer.offer("砺")); assertFalse(writer.offer("砺石"));
            assertFalse(writer.awaitDrained(2, TimeUnit.SECONDS));
            assertEquals(1, writer.snapshot().accepted); assertEquals(1, writer.snapshot().dropped);
        }
    }

    @Test public void nullPayloadIsUnknownRatherThanSilentDrop() throws Exception {
        try (BoundedTraceWriter writer = new BoundedTraceWriter(2, 32, entry -> { })) {
            assertFalse(writer.offer(null)); assertEquals("INVALID_PAYLOAD", writer.snapshot().firstFailure);
            assertEquals("UNKNOWN", writer.snapshot().status);
        }
    }

    @Test public void boundedDrainTimeoutDoesNotClaimSinkStopped() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), finished = new CountDownLatch(1);
        try (BoundedTraceWriter writer = new BoundedTraceWriter(2, 1024, entry -> {
            started.countDown(); release.await(); finished.countDown();
        })) {
            writer.offer("blocked"); assertTrue(started.await(2, TimeUnit.SECONDS));
            long start = System.nanoTime();
            assertFalse(writer.awaitDrained(30, TimeUnit.MILLISECONDS));
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1000);
            assertEquals(1, finished.getCount()); assertEquals(1, writer.snapshot().pending);
            assertEquals("DRAIN_TIMEOUT", writer.snapshot().firstFailure);
            release.countDown(); assertTrue(finished.await(2, TimeUnit.SECONDS));
            assertFalse(writer.awaitDrained(2, TimeUnit.SECONDS));
            assertEquals("UNKNOWN", writer.snapshot().status);
        } finally { release.countDown(); }
    }

    @Test public void pendingIsExplicitBeforeAnyFailure() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        try (BoundedTraceWriter writer = new BoundedTraceWriter(2, 1024, entry -> { started.countDown(); release.await(); })) {
            writer.offer("pending"); assertTrue(started.await(2, TimeUnit.SECONDS));
            assertEquals("PENDING", writer.snapshot().status); assertFalse(writer.snapshot().hasLoss());
            release.countDown(); assertTrue(writer.awaitDrained(2, TimeUnit.SECONDS));
        } finally { release.countDown(); }
    }

    @Test public void closeDoesNotWaitForBlockedSink() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        BoundedTraceWriter writer = new BoundedTraceWriter(2, 1024, entry -> { started.countDown(); release.await(); });
        try {
            writer.offer("blocked"); assertTrue(started.await(2, TimeUnit.SECONDS));
            long start = System.nanoTime(); writer.close(); writer.close();
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1000);
            assertEquals(1, writer.snapshot().pending);
        } finally { release.countDown(); }
    }

    @Test public void offerAfterCloseRetainsExplicitLoss() throws Exception {
        BoundedTraceWriter writer = new BoundedTraceWriter(2, 1024, entry -> { }); writer.close();
        assertFalse(writer.offer("late")); assertEquals("WRITER_CLOSED", writer.snapshot().firstFailure);
        assertEquals(1, writer.snapshot().dropped);
    }

    @Test public void firstLossReasonIsNeverReplaced() throws Exception {
        BoundedTraceWriter writer = new BoundedTraceWriter(2, 4, entry -> { });
        assertFalse(writer.offer("oversized")); writer.close(); assertFalse(writer.offer("late"));
        assertEquals("PAYLOAD_TOO_LARGE", writer.snapshot().firstFailure); assertEquals(2, writer.snapshot().dropped);
    }

    @Test public void concurrentAcceptedEventsHaveUniqueSequenceAndOneWriter() throws Exception {
        List<BoundedTraceWriter.Entry> entries = Collections.synchronizedList(new ArrayList<>());
        List<String> sinkThreads = Collections.synchronizedList(new ArrayList<>());
        try (BoundedTraceWriter writer = new BoundedTraceWriter(256, 1024, entry -> {
            entries.add(entry); sinkThreads.add(Thread.currentThread().getName());
        })) {
            List<Thread> producers = new ArrayList<>();
            for (int p = 0; p < 4; p++) {
                Thread producer = new Thread(() -> { for (int i = 0; i < 25; i++) assertTrue(writer.offer("concurrent")); });
                producers.add(producer); producer.start();
            }
            for (Thread producer : producers) producer.join(2000);
            assertTrue(writer.awaitDrained(2, TimeUnit.SECONDS)); assertEquals(100, entries.size());
            HashSet<Long> sequences = new HashSet<>(); for (BoundedTraceWriter.Entry entry : entries) sequences.add(entry.sequence);
            assertEquals(100, sequences.size()); assertEquals(1, new HashSet<>(sinkThreads).size());
        }
    }

    @Test public void drainInterruptionIsNotCompleteEvidence() throws Exception {
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        try (BoundedTraceWriter writer = new BoundedTraceWriter(2, 1024, entry -> { started.countDown(); release.await(); })) {
            writer.offer("blocked"); assertTrue(started.await(2, TimeUnit.SECONDS));
            Thread.currentThread().interrupt();
            try { writer.awaitDrained(2, TimeUnit.SECONDS); fail("interruption hidden"); }
            catch (InterruptedException expected) { assertEquals("DRAIN_INTERRUPTED", writer.snapshot().firstFailure); }
            finally { Thread.interrupted(); }
        } finally { release.countDown(); }
    }

    @Test public void invalidQueueConfigurationIsRejected() {
        try { new BoundedTraceWriter(0, 1024, entry -> { }); fail("zero capacity"); }
        catch (IllegalArgumentException expected) { }
        try { new BoundedTraceWriter(2, 0, entry -> { }); fail("zero payload bound"); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void invalidDrainTimeoutIsRejected() throws Exception {
        try (BoundedTraceWriter writer = new BoundedTraceWriter(2, 1024, entry -> { })) {
            try { writer.awaitDrained(-1, TimeUnit.MILLISECONDS); fail("negative timeout"); }
            catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void observedExternalDiagnosticFailureIsSticky() throws Exception {
        try (BoundedTraceWriter writer = new BoundedTraceWriter(2, 1024, entry -> { })) {
            writer.recordFailure("ENCODING_FAILURE", new IOException("synthetic-json-error"));
            assertFalse(writer.awaitDrained(2, TimeUnit.SECONDS));
            assertEquals("ENCODING_FAILURE", writer.snapshot().firstFailure);
            assertTrue(writer.snapshot().firstFailureCause.getMessage().contains("synthetic-json-error"));
        }
    }

    @Test public void unknownObserverRunsOnceAfterStickyFailure() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try (BoundedTraceWriter writer = new BoundedTraceWriter(2, 4, entry -> { }, calls::incrementAndGet)) {
            assertFalse(writer.offer("oversized")); writer.recordFailure("SECOND_FAILURE", null);
            assertEquals(1, calls.get()); assertEquals("PAYLOAD_TOO_LARGE", writer.snapshot().firstFailure);
        }
    }

    @Test public void writerFailureNotifiesObserverFromWriterWithoutProducerParticipation() throws Exception {
        CountDownLatch observed = new CountDownLatch(1);
        List<String> threads = Collections.synchronizedList(new ArrayList<>());
        try (BoundedTraceWriter writer = new BoundedTraceWriter(2, 32, entry -> { throw new IOException("sink"); }, () -> {
            threads.add(Thread.currentThread().getName()); observed.countDown();
        })) {
            assertTrue(writer.offer("event")); assertTrue(observed.await(2, TimeUnit.SECONDS));
            assertEquals("research-host-trace", threads.get(0)); assertFalse(writer.awaitDrained(2, TimeUnit.SECONDS));
        }
    }

    @Test public void observerExceptionIsPreservedWithoutClearingUnknown() throws Exception {
        IllegalStateException observerError = new IllegalStateException("synthetic-fence-observer-error");
        try (BoundedTraceWriter writer = new BoundedTraceWriter(2, 4, entry -> { }, () -> { throw observerError; })) {
            assertFalse(writer.offer("oversized"));
            assertEquals("UNKNOWN", writer.snapshot().status); assertEquals(1, writer.snapshot().observerFailures);
            assertSame(observerError, writer.snapshot().observerFailureCause);
            assertEquals("PAYLOAD_TOO_LARGE", writer.snapshot().firstFailure);
        }
    }

    @Test public void healthSnapshotNeverCallsOverridableThrowableFormatting() throws Exception {
        RuntimeException hostileFormatter = new RuntimeException("synthetic") {
            @Override public String toString() { throw new AssertionError("diagnostic formatter executed on caller"); }
        };
        try (BoundedTraceWriter writer = new BoundedTraceWriter(2, 1024, entry -> { })) {
            writer.recordFailure("ENCODING_FAILURE", hostileFormatter);
            assertEquals("UNKNOWN", writer.snapshot().status); assertSame(hostileFormatter, writer.snapshot().firstFailureCause);
        }
    }
}
