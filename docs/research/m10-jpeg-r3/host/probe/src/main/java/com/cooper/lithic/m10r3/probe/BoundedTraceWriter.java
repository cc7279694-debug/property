package com.cooper.lithic.m10r3.probe;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Research Host diagnostics only. Producers never call the sink, logd, filesystem,
 * or a user callback. Capacity includes the in-flight sink write. Loss, timeout and
 * write failure are sticky UNKNOWN; none is evidence that a blocked writer stopped.
 * Authoritative stage/revoke/receipt writes must never use this diagnostic queue.
 */
public final class BoundedTraceWriter implements AutoCloseable {
    public interface Sink { void write(Entry entry) throws Exception; }

    public static final class Entry {
        public final long sequence, createdNano;
        public final String producerThread, payload;
        public final int utf8Bytes;
        private Entry(long sequence, String payload, int utf8Bytes) {
            this.sequence = sequence; this.payload = payload; this.utf8Bytes = utf8Bytes;
            createdNano = System.nanoTime(); producerThread = Thread.currentThread().getName();
        }
    }

    private static final class Failure {
        final String reason;
        final Throwable cause;
        Failure(String reason, Throwable cause) { this.reason = reason; this.cause = cause; }
    }

    public static final class Health {
        public final String status, firstFailure, firstFailureDetail;
        public final Throwable firstFailureCause, observerFailureCause;
        public final long attempted, accepted, written, dropped, writeFailures, observerFailures;
        public final int pending, producers;
        public final boolean writerTerminated;
        private Health(BoundedTraceWriter source) {
            attempted = source.attempted.get(); accepted = source.accepted.get(); written = source.written.get();
            dropped = source.dropped.get(); writeFailures = source.writeFailures.get();
            observerFailures = source.observerFailures.get(); observerFailureCause = source.observerFailure.get();
            pending = source.pending.get(); producers = source.producers.get(); writerTerminated = source.terminated.get();
            Failure failure = source.failure.get();
            firstFailure = failure == null ? null : failure.reason;
            firstFailureCause = failure == null ? null : failure.cause;
            // No overridable Throwable formatting on a UI/watchdog health check.
            // The original Throwable remains available for the off-thread report.
            firstFailureDetail = firstFailureCause == null ? "" : firstFailureCause.getClass().getName();
            status = failure != null ? "UNKNOWN" : pending != 0 || producers != 0 || accepted != written ? "PENDING" : "COMPLETE";
        }
        public boolean hasLoss() { return firstFailure != null; }
    }

    private final int capacity, maxUtf8Bytes;
    private final Sink sink;
    private final Runnable onUnknown;
    private final ConcurrentLinkedQueue<Entry> queue = new ConcurrentLinkedQueue<>();
    private final Semaphore available = new Semaphore(0);
    private final AtomicInteger pending = new AtomicInteger(), producers = new AtomicInteger();
    private final AtomicLong sequence = new AtomicLong(), attempted = new AtomicLong(), accepted = new AtomicLong(),
        written = new AtomicLong(), dropped = new AtomicLong(), writeFailures = new AtomicLong(), observerFailures = new AtomicLong();
    private final AtomicReference<Failure> failure = new AtomicReference<>();
    private final AtomicReference<Throwable> observerFailure = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean(), terminated = new AtomicBoolean();

    public BoundedTraceWriter(int capacity, int maxUtf8Bytes, Sink sink) {
        this(capacity, maxUtf8Bytes, sink, () -> { });
    }

    /** onUnknown must only fence memory: no I/O, Binder, logging, wait or slow lock. */
    public BoundedTraceWriter(int capacity, int maxUtf8Bytes, Sink sink, Runnable onUnknown) {
        if (capacity < 1 || capacity > 4096 || maxUtf8Bytes < 1 || maxUtf8Bytes > 1024 * 1024)
            throw new IllegalArgumentException("bounded diagnostic configuration required");
        this.capacity = capacity; this.maxUtf8Bytes = maxUtf8Bytes; this.sink = Objects.requireNonNull(sink, "sink");
        this.onUnknown = Objects.requireNonNull(onUnknown, "onUnknown");
        Thread writer = new Thread(this::runWriter, "research-host-trace"); writer.setDaemon(true); writer.start();
    }

    /** Best effort nonblocking admission; sink failure is separately visible through health. */
    public boolean offer(String payload) {
        attempted.incrementAndGet(); producers.incrementAndGet();
        boolean reserved = false;
        try {
            if (closed.get()) return reject("WRITER_CLOSED", null);
            if (payload == null) return reject("INVALID_PAYLOAD", null);
            if (payload.length() > maxUtf8Bytes) return reject("PAYLOAD_TOO_LARGE", null);
            int bytes = payload.getBytes(StandardCharsets.UTF_8).length;
            if (bytes > maxUtf8Bytes) return reject("PAYLOAD_TOO_LARGE", null);
            while (true) {
                int count = pending.get();
                if (count >= capacity) return reject("QUEUE_FULL", null);
                if (pending.compareAndSet(count, count + 1)) { reserved = true; break; }
            }
            Entry entry = new Entry(sequence.incrementAndGet(), payload, bytes);
            accepted.incrementAndGet(); queue.add(entry); reserved = false;
            available.release(); return true;
        } catch (RuntimeException | Error unexpected) {
            if (reserved) pending.decrementAndGet();
            recordFailure("ENQUEUE_FAILURE", unexpected);
            throw unexpected;
        } finally {
            producers.decrementAndGet();
            if (closed.get()) available.release();
        }
    }

    private boolean reject(String reason, Throwable error) {
        dropped.incrementAndGet(); recordFailure(reason, error); return false;
    }

    /** Records encoding/other diagnostic failures without running any supplied callback. */
    public void recordFailure(String reason, Throwable error) {
        if (failure.compareAndSet(null, new Failure(Objects.requireNonNull(reason, "reason"), error))) {
            // Failure is already sticky. No queue/sink/user lock is held here.
            try { onUnknown.run(); }
            catch (RuntimeException observerError) {
                observerFailures.incrementAndGet(); observerFailure.compareAndSet(null, observerError);
            } catch (Error fatal) {
                observerFailures.incrementAndGet(); observerFailure.compareAndSet(null, fatal); throw fatal;
            }
        }
    }

    public Health snapshot() { return new Health(this); }

    /** Call only off the UI/Binder/short failure critical path. Timeout is sticky UNKNOWN. */
    public boolean awaitDrained(long timeout, TimeUnit unit) throws InterruptedException {
        if (timeout < 0) throw new IllegalArgumentException("negative timeout");
        long budget = Objects.requireNonNull(unit, "unit").toNanos(timeout), start = System.nanoTime();
        while (true) {
            if (Thread.interrupted()) { recordFailure("DRAIN_INTERRUPTED", null); throw new InterruptedException("diagnostic drain interrupted"); }
            Health health = snapshot();
            if (health.pending == 0 && health.producers == 0) return !health.hasLoss();
            long elapsed = System.nanoTime() - start;
            if (elapsed >= budget) { recordFailure("DRAIN_TIMEOUT", null); return false; }
            LockSupport.parkNanos(this, Math.min(budget - elapsed, TimeUnit.MILLISECONDS.toNanos(1)));
        }
    }

    private void runWriter() {
        try {
            while (true) {
                available.acquire();
                Entry entry;
                while ((entry = queue.poll()) != null) {
                    try { sink.write(entry); written.incrementAndGet(); }
                    catch (Exception writeError) { writeFailures.incrementAndGet(); recordFailure("SINK_FAILURE", writeError); }
                    finally { pending.decrementAndGet(); }
                }
                if (closed.get() && producers.get() == 0 && pending.get() == 0) return;
            }
        } catch (InterruptedException interrupted) {
            recordFailure("WRITER_INTERRUPTED", interrupted); Thread.currentThread().interrupt();
        } catch (Error fatal) {
            recordFailure("WRITER_TERMINATED", fatal); throw fatal;
        } finally { terminated.set(true); }
    }

    /** Requests a drain then exit; never waits or claims a blocked sink has terminated. */
    @Override public void close() { closed.set(true); available.release(); }
}
