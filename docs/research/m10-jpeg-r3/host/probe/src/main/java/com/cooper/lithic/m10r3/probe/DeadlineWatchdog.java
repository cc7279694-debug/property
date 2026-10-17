package com.cooper.lithic.m10r3.probe;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Dedicated timer, separate from Android UI, Host executor, Binder and trace writers. */
final class DeadlineWatchdog implements AutoCloseable {
    private final BindingTimeline timeline;
    private final ScheduledThreadPoolExecutor timer;
    private ScheduledFuture<?> future;
    private boolean started, closed;

    DeadlineWatchdog(BindingTimeline timeline) {
        this.timeline = Objects.requireNonNull(timeline);
        timer = new ScheduledThreadPoolExecutor(1, task -> {
            Thread t = new Thread(task, "lithic-r3-host-deadline");
            t.setDaemon(true); return t;
        });
        timer.setRemoveOnCancelPolicy(true);
        timer.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        timer.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    void start() {
        Throwable schedulingError = null;
        synchronized (this) {
            if (started || closed) throw new IllegalStateException("watchdog is single-use");
            started = true;
            try {
                future = timer.scheduleAtFixedRate(() -> {
                    try { timeline.checkDeadline(); }
                    catch (Throwable error) { timeline.watchdogUnknown("WATCHDOG_TICK_UNKNOWN"); }
                }, 0, 20, TimeUnit.MILLISECONDS);
            } catch (Throwable error) { schedulingError = error; }
        }
        if (schedulingError != null) timeline.watchdogUnknown("WATCHDOG_SCHEDULE_UNKNOWN");
    }

    /** Cancels observation only. It does not cancel or prove remote/native task termination. */
    @Override public void close() {
        synchronized (this) {
            if (closed) return;
            closed = true;
            if (future != null) future.cancel(false);
        }
        timer.shutdown();
    }
}
