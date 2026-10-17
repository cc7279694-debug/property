package com.cooper.lithic.m10r3.probe;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * One research Host's bounded serial execution owner. Submission performs only bounded
 * validation and memory admission; initialization, durable claims, tasks and observations
 * always execute on the same background worker. This queue is not a Session safety gate:
 * a claimed task must still pass InvocationFence and the persistent StageStore admission.
 */
public final class HostCommandQueue implements AutoCloseable {
    public interface Initializer { void initialize() throws Exception; }
    public interface Claim { boolean claim() throws Exception; }
    public interface Task { void run() throws Exception; }
    public interface Observer {
        void onComplete(String command, String runId, Outcome outcome, Throwable error) throws Exception;
    }
    public enum Readiness { BOOTSTRAPPING, READY, UNKNOWN, CLOSED }
    public enum Submission { ACCEPTED, INVALID, QUEUE_FULL, DUPLICATE, INITIALIZATION_UNKNOWN, CLOSED }
    public enum Outcome { EXECUTED, CLAIM_REPLAY, CLAIM_UNKNOWN, TASK_FAILED, INITIALIZATION_UNKNOWN }

    private final Object admission = new Object();
    private final Set<String> activeKeys = new HashSet<>();
    private final ThreadPoolExecutor executor;
    private final Observer observer;
    private final AtomicLong observerFailures = new AtomicLong();
    private volatile Readiness readiness = Readiness.BOOTSTRAPPING;
    private volatile boolean closed;
    private volatile Throwable initializationError;
    private volatile Throwable observerError;

    public HostCommandQueue(int capacity, Initializer initializer) {
        this(capacity, initializer, null);
    }

    public HostCommandQueue(int capacity, Initializer initializer, Observer observer) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        Objects.requireNonNull(initializer, "initializer");
        this.observer = observer;
        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(capacity), runnable -> {
                    Thread worker = new Thread(runnable, "m10-r3-host-command");
                    worker.setDaemon(true);
                    return worker;
                }, new ThreadPoolExecutor.AbortPolicy());
        // The sole executor's very first job is bootstrap. No caller can submit before
        // this constructor has queued it, and no second executor bypasses readiness.
        executor.execute(() -> {
            try {
                initializer.initialize();
                readiness = Readiness.READY;
            } catch (Throwable failure) {
                initializationError = failure;
                readiness = Readiness.UNKNOWN;
            }
        });
    }

    public Readiness readiness() { return closed ? Readiness.CLOSED : readiness; }
    public Throwable initializationError() { return initializationError; }
    public Throwable observerError() { return observerError; }
    public long observerFailures() { return observerFailures.get(); }

    /**
     * No I/O, wait, caller-runs policy or durable claim occurs here. An accepted key
     * remains reserved through its outcome; this prevents a replay intent from waiting
     * behind the Invocation that is itself waiting for that rejection observation.
     * At most one running command plus capacity waiting commands can own keys.
     */
    public Submission submit(String command, String runId, Claim claim, Task task) {
        if (!valid(command, runId) || claim == null || task == null) return Submission.INVALID;
        final String key = command + '\0' + runId;
        synchronized (admission) {
            if (closed) return Submission.CLOSED;
            if (readiness == Readiness.UNKNOWN) return Submission.INITIALIZATION_UNKNOWN;
            if (activeKeys.contains(key)) return Submission.DUPLICATE;
            activeKeys.add(key);
            try {
                executor.execute(() -> execute(command, runId, key, claim, task));
                return Submission.ACCEPTED;
            } catch (RejectedExecutionException rejected) {
                activeKeys.remove(key);
                return closed ? Submission.CLOSED : Submission.QUEUE_FULL;
            } catch (Throwable unknown) {
                activeKeys.remove(key);
                throw unknown;
            }
        }
    }

    private void execute(String command, String runId, String key, Claim claim, Task task) {
        Outcome outcome;
        Throwable error = null;
        if (readiness != Readiness.READY) {
            outcome = Outcome.INITIALIZATION_UNKNOWN;
            error = initializationError;
        } else {
            boolean claimed = false;
            try {
                claimed = claim.claim();
                outcome = claimed ? Outcome.EXECUTED : Outcome.CLAIM_REPLAY;
            } catch (Throwable failure) {
                outcome = Outcome.CLAIM_UNKNOWN;
                error = failure;
            }
            if (claimed) {
                try { task.run(); }
                catch (Throwable failure) { outcome = Outcome.TASK_FAILED; error = failure; }
            }
        }
        // Outcome is fixed before releasing the in-memory replay key. A later accepted
        // replay must still run its durable claim; releasing the key is no authorization.
        synchronized (admission) { activeKeys.remove(key); }
        observe(command, runId, outcome, error);
    }

    private void observe(String command, String runId, Outcome outcome, Throwable error) {
        if (observer == null) return;
        try { observer.onComplete(command, runId, outcome, error); }
        catch (Throwable failure) {
            if (observerError == null) observerError = failure; // Observer is single-worker owned.
            observerFailures.incrementAndGet();
        }
    }

    private static boolean valid(String command, String runId) {
        return command != null && command.length() >= 1 && command.length() <= 20
                && runId != null && runId.length() >= 1 && runId.length() <= 80
                && command.matches("[a-z_]{1,20}") && runId.matches("[a-zA-Z0-9_-]{1,80}");
    }

    /** Test/owner shutdown only; never interrupts, cancels or claims remote termination. */
    @Override public void close() {
        synchronized (admission) { closed = true; }
        executor.shutdown();
    }
}
