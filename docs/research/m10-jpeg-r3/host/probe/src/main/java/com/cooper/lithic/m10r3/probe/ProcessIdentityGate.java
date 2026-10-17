package com.cooper.lithic.m10r3.probe;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Research process identity, separate from Session/owner authorization. A main-thread
 * role observation may allow onBind to expose the endpoint, but never authorizes
 * reserve. Only a completed independent background check can make this gate READY.
 * No Android APIs, disk writes, Binder calls or diagnostic sinks run under its lock.
 */
public final class ProcessIdentityGate {
    public enum State { PENDING, READY, FAILED }
    public interface NameReader { String read() throws Exception; }
    public interface NanoClock { long now() throws Exception; }

    private static final String HOST = "com.cooper.lithic.m10r3.probe";
    private static final String WORKER = HOST + ":jpeg_worker";
    private static final String UNKNOWN = "UNKNOWN";
    // Rejection policy AFTER the read returns; neither this nor 256 bytes is a hard
    // syscall deadline. Real API26/27 main-thread latency and ANR remain NOT_RUN.
    private static final long MAX_PROBE_NANOS = TimeUnit.MILLISECONDS.toNanos(100);
    private final long binderWaitNanos;
    private State state = State.PENDING;
    private String firstFailure, frameworkHint, backgroundObserved, fastObservation;
    private boolean backgroundCompleted;
    private int syncInFlight;
    private long probeCount, lastProbeNanos = -1;

    public ProcessIdentityGate() { this(1000); }
    /** Shorter waits support deterministic offline tests, never a longer production wait. */
    ProcessIdentityGate(long binderWaitMs) {
        if (binderWaitMs <= 0 || binderWaitMs > 1000) throw new IllegalArgumentException("binder wait");
        binderWaitNanos = TimeUnit.MILLISECONDS.toNanos(binderWaitMs);
    }

    /** A framework hint is role-only evidence, never READY or owner initialization. */
    public synchronized void frameworkHint(String name) {
        if (acceptObservation(name)) frameworkHint = name;
    }

    /** Called only with a separately read /proc identity on the serial background path. */
    public synchronized String beginBackground(String actual) {
        if (!acceptObservation(actual)) return "REJECT";
        backgroundObserved = actual;
        return knownRole(actual);
    }

    /**
     * Call only after the caller's owner/Runner initialization succeeds. False means
     * FAILED, or readiness DEFERRED until an already-started synchronous observation
     * returns. The latter must not be treated as failure or as permission to authorize.
     */
    public synchronized boolean completeBackground() {
        if (state == State.FAILED) return false;
        if (backgroundObserved == null) { fail("BACKGROUND_OBSERVATION_MISSING"); return false; }
        backgroundCompleted = true;
        publishReadyIfComplete();
        return state == State.READY;
    }

    public synchronized void fail(String reason) {
        if (state == State.FAILED) return;
        firstFailure = reason == null ? "IDENTITY_FAILURE_UNKNOWN" : reason;
        state = State.FAILED;
        notifyAll();
    }

    public synchronized State state() { return state; }
    public synchronized String failureReason() { return firstFailure; }
    public synchronized String verifiedRole() {
        return state == State.READY ? knownRole(backgroundObserved) : "REJECT";
    }

    /**
     * Only API26/27's pending MAIN entry uses the explicitly authorized bounded proc
     * observation. Binder/background entries require READY, waiting at most one second
     * without holding this monitor. UNKNOWN is a stale snapshot sentinel, not a role.
     */
    public String role(String claimed, int api, boolean main, NameReader reader, NanoClock clock) {
        if (!validClaim(claimed) || api < 26) return "REJECT";
        if (!main) return awaitVerifiedRole(claimed);
        synchronized (this) {
            if (state == State.FAILED) return "REJECT";
            if (state == State.READY) return claimedRole(claimed, backgroundObserved);
            if (api >= 28) return claimedRole(claimed, frameworkHint);
            syncInFlight++;
            probeCount++;
        }
        // No monitor is held while reading the file or the clock. Concurrent FAILED or
        // READY transitions are rechecked below, and READY waits for every in-flight read.
        String actual = null;
        String error = null;
        long elapsed = -1;
        try {
            if (reader == null || clock == null) throw new IOException("identity probe dependency missing");
            long before = clock.now();
            if (before < 0) throw new IOException("identity probe start clock unknown");
            actual = reader.read();
            long after = clock.now();
            if (after < 0) throw new IOException("identity probe end clock unknown");
            elapsed = Math.subtractExact(after, before);
            if (elapsed < 0 || elapsed >= MAX_PROBE_NANOS) error = "PROBE_TIME_UNKNOWN_OR_OVER_BUDGET";
        } catch (Exception failure) {
            error = "PROBE_FAILED: " + failure;
        }
        synchronized (this) {
            syncInFlight--;
            lastProbeNanos = elapsed;
            if (error != null) fail(error);
            if (state == State.FAILED) return "REJECT";
            if (!acceptObservation(actual)) return "REJECT";
            fastObservation = actual;
            publishReadyIfComplete();
            return claimedRole(claimed, actual);
        }
    }

    private synchronized String awaitVerifiedRole(String claimed) {
        long started = System.nanoTime();
        long remaining = binderWaitNanos;
        while (state == State.PENDING && remaining > 0) {
            try {
                long millis = TimeUnit.NANOSECONDS.toMillis(remaining);
                int nanos = (int) (remaining - TimeUnit.MILLISECONDS.toNanos(millis));
                wait(millis, nanos);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return "REJECT";
            }
            long elapsed = System.nanoTime() - started;
            if (elapsed < 0 || elapsed >= binderWaitNanos) remaining = 0;
            else remaining = binderWaitNanos - elapsed;
        }
        // Timeout is an individual rejection, never an inference that the worker has
        // stopped or that background initialization may be skipped.
        return state == State.READY && remaining > 0 ? claimedRole(claimed, backgroundObserved) : "REJECT";
    }

    /** Memory-only report. The caller may serialize it off main; this method does no I/O. */
    public synchronized Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("state", state.name());
        out.put("firstFailure", firstFailure);
        out.put("frameworkHint", frameworkHint);
        out.put("backgroundObserved", backgroundObserved);
        out.put("backgroundCompleted", backgroundCompleted);
        out.put("fastObservation", fastObservation);
        out.put("syncInFlight", syncInFlight);
        out.put("probeCount", probeCount);
        out.put("lastProbeNanos", lastProbeNanos);
        out.put("probePolicyNanos", MAX_PROBE_NANOS);
        return out;
    }

    private boolean acceptObservation(String actual) {
        if (state == State.FAILED) return false;
        if ("REJECT".equals(knownRole(actual))) { fail("UNKNOWN_PROCESS_OBSERVATION"); return false; }
        if ((frameworkHint != null && !frameworkHint.equals(actual))
                || (backgroundObserved != null && !backgroundObserved.equals(actual))
                || (fastObservation != null && !fastObservation.equals(actual))) {
            fail("PROCESS_IDENTITY_CONFLICT");
            return false;
        }
        return true;
    }

    private void publishReadyIfComplete() {
        if (state != State.FAILED && backgroundCompleted && syncInFlight == 0) {
            state = State.READY;
            notifyAll();
        }
    }

    private static boolean validClaim(String name) {
        return UNKNOWN.equals(name) || !"REJECT".equals(knownRole(name));
    }

    private static String claimedRole(String claimed, String observed) {
        if (observed == null || (!UNKNOWN.equals(claimed) && !observed.equals(claimed))) return "REJECT";
        return knownRole(observed);
    }

    private static String knownRole(String name) {
        return HOST.equals(name) ? "HOST" : WORKER.equals(name) ? "WORKER" : "REJECT";
    }

    /**
     * Reads at most 256 bytes total; the first argument must terminate inside that
     * bound and be one exact ASCII research process name. The caller owns and closes
     * the stream with try-with-resources so read/close errors both reject the probe.
     */
    public static String readCmdline(InputStream input) throws IOException {
        if (input == null) throw new IOException("cmdline stream missing");
        byte[] bytes = new byte[256];
        int used = 0;
        while (used < bytes.length) {
            int count = input.read(bytes, used, bytes.length - used);
            if (count <= 0 || count > bytes.length - used) throw new IOException("empty or truncated cmdline");
            int until = used + count;
            for (int i = used; i < until; i++) {
                int value = bytes[i] & 0xff;
                if (value == 0) {
                    if (i == 0) throw new IOException("empty cmdline argument");
                    String name = new String(bytes, 0, i, StandardCharsets.US_ASCII);
                    if ("REJECT".equals(knownRole(name))) throw new IOException("unknown cmdline role");
                    return name;
                }
                if (value < 33 || value > 126) throw new IOException("non-ASCII or control cmdline");
            }
            used = until;
        }
        throw new IOException("cmdline exceeds 256-byte terminated bound");
    }
}
