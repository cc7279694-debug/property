package com.cooper.lithic.m10r3.outsider;

/** Owns one Activity-context binding attempt and its matching cleanup. */
final class BindingAttempt implements AutoCloseable {
    interface Binding { boolean bind(); void unbind(); }
    enum Outcome { NOT_ATTEMPTED, RETURNED_TRUE, RETURNED_FALSE, DENIED, ERROR }
    enum Cleanup { NOT_NEEDED, REQUIRED, RELEASED, NOT_REGISTERED, FAILED }
    private final Binding binding;
    private Outcome outcome = Outcome.NOT_ATTEMPTED;
    private Cleanup cleanup = Cleanup.NOT_NEEDED;
    private Throwable bindingError, cleanupError, firstUnexpectedCleanupError;
    BindingAttempt(Binding binding) { this.binding = binding; }
    void run() {
        if (outcome != Outcome.NOT_ATTEMPTED) throw new IllegalStateException("Binding already attempted");
        // Android can register a ServiceConnection before bindService returns or throws.
        outcome = Outcome.ERROR;
        cleanup = Cleanup.REQUIRED;
        try {
            outcome = binding.bind() ? Outcome.RETURNED_TRUE : Outcome.RETURNED_FALSE;
        } catch (SecurityException denied) {
            outcome = Outcome.DENIED; bindingError = denied;
        } catch (RuntimeException error) {
            outcome = Outcome.ERROR; bindingError = error;
        } finally {
            close();
        }
    }
    @Override public void close() {
        if (cleanup == Cleanup.NOT_NEEDED || cleanup == Cleanup.RELEASED || cleanup == Cleanup.NOT_REGISTERED) return;
        try {
            binding.unbind();
            cleanup = Cleanup.RELEASED;
            cleanupError = null;
        } catch (IllegalArgumentException unregistered) {
            cleanupError = unregistered;
            if (outcome == Outcome.RETURNED_TRUE) unexpectedCleanupFailure(unregistered);
            else cleanup = Cleanup.NOT_REGISTERED;
        } catch (RuntimeException error) {
            cleanupError = error;
            unexpectedCleanupFailure(error);
        }
    }
    private void unexpectedCleanupFailure(RuntimeException error) {
        cleanup = Cleanup.FAILED;
        if (firstUnexpectedCleanupError == null) firstUnexpectedCleanupError = error;
    }
    Outcome outcome() { return outcome; }
    Cleanup cleanup() { return cleanup; }
    Throwable bindingError() { return bindingError; }
    Throwable cleanupError() { return cleanupError; }
    Throwable firstUnexpectedCleanupError() { return firstUnexpectedCleanupError; }
    boolean probePassed() {
        return (outcome == Outcome.DENIED || outcome == Outcome.RETURNED_FALSE)
                && (cleanup == Cleanup.RELEASED || cleanup == Cleanup.NOT_REGISTERED)
                && firstUnexpectedCleanupError == null;
    }
}
