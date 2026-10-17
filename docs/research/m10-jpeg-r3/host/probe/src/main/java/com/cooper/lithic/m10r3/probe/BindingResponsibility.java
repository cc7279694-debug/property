package com.cooper.lithic.m10r3.probe;

import java.util.function.BooleanSupplier;
import java.util.Objects;

/**
 * Owns exactly one matching Context/ServiceConnection adapter.
 * External operations never run while this object's short state monitor is held.
 * Unknown unbind results retain duty and require separate, explicit investigation.
 */
final class BindingResponsibility {
    interface Operations { boolean bind(); void unbind(); }
    /** Only short memory changes belong here; no trace, Binder, filesystem or waits. */
    interface FailureFence { void close(String reason, Throwable failure); }
    static Operations fenced(Operations operations, FailureFence failureFence) {
        Objects.requireNonNull(operations); Objects.requireNonNull(failureFence);
        return new Operations() {
            @Override public boolean bind() {
                final boolean bound;
                try { bound = operations.bind(); }
                catch (RuntimeException | Error original) {
                    try { failureFence.close("BIND_THROW", original); }
                    catch (RuntimeException | Error fenceError) {
                        if (fenceError != original) original.addSuppressed(fenceError);
                    }
                    throw original;
                }
                if (!bound) failureFence.close("BIND_RETURNED_FALSE", null);
                return bound;
            }
            @Override public void unbind() { operations.unbind(); }
        };
    }
    enum BindState { QUEUED, CALLING, RETURNED_TRUE, RETURNED_FALSE, THREW, CLOSED_NOT_STARTED }
    enum CleanupState { NOT_NEEDED, REQUIRED, DEFERRED, UNBINDING, UNBOUND, NOT_REGISTERED, UNBIND_UNKNOWN }
    private final Operations operations;
    private BindState bind = BindState.QUEUED;
    private CleanupState cleanup = CleanupState.NOT_NEEDED;
    private boolean attempted, requested;
    private Throwable bindingError, cleanupError, firstCleanupError;
    BindingResponsibility(Operations operations) { this.operations = Objects.requireNonNull(operations); }

    void executeBind(BooleanSupplier stillAuthorized) {
        Objects.requireNonNull(stillAuthorized);
        synchronized (this) {
            if (bind == BindState.CLOSED_NOT_STARTED) return;
            if (bind != BindState.QUEUED) throw new IllegalStateException("Binding already attempted");
            bind = BindState.CALLING;
        }
        final boolean allowed;
        try { allowed = stillAuthorized.getAsBoolean(); }
        catch (Throwable error) {
            synchronized (this) { bind = BindState.THREW; bindingError = error; cleanup = CleanupState.NOT_NEEDED; }
            return;
        }
        synchronized (this) {
            if (!allowed || requested) {
                bind = BindState.CLOSED_NOT_STARTED;
                cleanup = CleanupState.NOT_NEEDED;
                return;
            }
            // Android may register before returning false or throwing SecurityException.
            attempted = true;
            cleanup = CleanupState.REQUIRED;
        }
        boolean answer = false;
        Throwable error = null;
        try { answer = operations.bind(); }
        catch (Throwable failure) { error = failure; }
        boolean close;
        synchronized (this) {
            bindingError = error;
            bind = error != null ? BindState.THREW : answer ? BindState.RETURNED_TRUE : BindState.RETURNED_FALSE;
            close = requested || error != null || !answer;
            if (!close) cleanup = CleanupState.REQUIRED;
        }
        if (close) requestCleanup();
    }

    void requestCleanup() {
        final BindState outcome;
        synchronized (this) {
            requested = true;
            if (bind == BindState.QUEUED) {
                bind = BindState.CLOSED_NOT_STARTED; cleanup = CleanupState.NOT_NEEDED; return;
            }
            if (bind == BindState.CALLING) { cleanup = CleanupState.DEFERRED; return; }
            if (!attempted) { cleanup = CleanupState.NOT_NEEDED; return; }
            if (cleanup == CleanupState.UNBOUND || cleanup == CleanupState.NOT_REGISTERED
                    || cleanup == CleanupState.UNBIND_UNKNOWN || cleanup == CleanupState.UNBINDING) return;
            cleanup = CleanupState.UNBINDING;
            outcome = bind;
        }
        Throwable error = null;
        try { operations.unbind(); }
        catch (Throwable failure) { error = failure; }
        synchronized (this) {
            cleanupError = error;
            if (error == null) cleanup = CleanupState.UNBOUND;
            else if (error instanceof IllegalArgumentException && outcome != BindState.RETURNED_TRUE) {
                // The matching adapter explicitly reports no registration after false/throw.
                cleanup = CleanupState.NOT_REGISTERED;
            } else {
                cleanup = CleanupState.UNBIND_UNKNOWN;
                if (firstCleanupError == null) firstCleanupError = error;
            }
        }
    }
    synchronized BindState bindState() { return bind; }
    synchronized CleanupState cleanupState() { return cleanup; }
    synchronized boolean cleanupRequested() { return requested; }
    synchronized boolean hasCleanupDuty() {
        return attempted && cleanup != CleanupState.UNBOUND && cleanup != CleanupState.NOT_REGISTERED;
    }
    synchronized Throwable bindingError() { return bindingError; }
    synchronized Throwable cleanupError() { return cleanupError; }
    synchronized Throwable firstCleanupError() { return firstCleanupError; }
}
