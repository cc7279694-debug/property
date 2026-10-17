package com.cooper.lithic.m10r3.probe;

import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public final class BindingResponsibilityTest {
    private static final class Calls implements BindingResponsibility.Operations {
        boolean answer = true;
        int binds, unbinds;
        Throwable bindFailure, unbindFailure;
        @Override public boolean bind() {
            binds++;
            if (bindFailure != null) throwUnchecked(bindFailure);
            return answer;
        }
        @Override public void unbind() {
            unbinds++;
            if (unbindFailure != null) throwUnchecked(unbindFailure);
        }
        private static void throwUnchecked(Throwable error) {
            if (error instanceof Error) throw (Error) error;
            throw (RuntimeException) error;
        }
    }

    @Test public void failedBeforeQueuedBindNeverCallsAndroid() {
        Calls calls = new Calls(); BindingResponsibility r = new BindingResponsibility(calls);
        r.executeBind(() -> false);
        assertEquals(0, calls.binds);
        assertEquals(BindingResponsibility.BindState.CLOSED_NOT_STARTED, r.bindState());
        assertFalse(r.hasCleanupDuty());
    }

    @Test public void cleanupBeforeQueueExecutesPreventsBinding() {
        Calls calls = new Calls(); BindingResponsibility r = new BindingResponsibility(calls);
        r.requestCleanup(); r.executeBind(() -> true);
        assertEquals(0, calls.binds); assertEquals(0, calls.unbinds);
        assertEquals(BindingResponsibility.CleanupState.NOT_NEEDED, r.cleanupState());
    }

    @Test public void returnedTrueKeepsDutyUntilExplicitCleanup() {
        Calls calls = new Calls(); BindingResponsibility r = new BindingResponsibility(calls);
        r.executeBind(() -> true);
        assertEquals(BindingResponsibility.BindState.RETURNED_TRUE, r.bindState());
        assertTrue(r.hasCleanupDuty()); assertEquals(0, calls.unbinds);
        r.requestCleanup();
        assertEquals(1, calls.unbinds); assertFalse(r.hasCleanupDuty());
        assertEquals(BindingResponsibility.CleanupState.UNBOUND, r.cleanupState());
    }

    @Test public void returnedFalseStillCleansMatchingRegistration() {
        Calls calls = new Calls(); calls.answer = false;
        BindingResponsibility r = new BindingResponsibility(calls); r.executeBind(() -> true);
        assertEquals(BindingResponsibility.BindState.RETURNED_FALSE, r.bindState());
        assertEquals(1, calls.unbinds); assertFalse(r.hasCleanupDuty());
    }

    @Test public void securityExceptionPreservesBindingErrorAndCleanup() {
        Calls calls = new Calls(); calls.bindFailure = new SecurityException("denied");
        BindingResponsibility r = new BindingResponsibility(calls); r.executeBind(() -> true);
        assertSame(calls.bindFailure, r.bindingError());
        assertEquals(BindingResponsibility.BindState.THREW, r.bindState());
        assertEquals(1, calls.unbinds); assertEquals(BindingResponsibility.CleanupState.UNBOUND, r.cleanupState());
    }

    @Test public void bindRuntimeExceptionAlsoHasMatchingCleanup() {
        Calls calls = new Calls(); calls.bindFailure = new IllegalStateException("binder runtime");
        BindingResponsibility r = new BindingResponsibility(calls); r.executeBind(() -> true);
        assertSame(calls.bindFailure, r.bindingError()); assertEquals(1, calls.unbinds);
    }

    @Test public void bindErrorIsRecordedAndStillCleansResponsibility() {
        Calls calls = new Calls(); calls.bindFailure = new AssertionError("synthetic error");
        BindingResponsibility r = new BindingResponsibility(calls); r.executeBind(() -> true);
        assertSame(calls.bindFailure, r.bindingError()); assertEquals(1, calls.unbinds);
    }

    @Test public void deniedAndNotRegisteredIsKnownNoRegistration() {
        Calls calls = new Calls(); calls.bindFailure = new SecurityException("denied");
        calls.unbindFailure = new IllegalArgumentException("not registered");
        BindingResponsibility r = new BindingResponsibility(calls); r.executeBind(() -> true);
        assertEquals(BindingResponsibility.CleanupState.NOT_REGISTERED, r.cleanupState());
        assertSame(calls.unbindFailure, r.cleanupError()); assertNull(r.firstCleanupError());
        assertFalse(r.hasCleanupDuty());
    }

    @Test public void trueThenNotRegisteredIsUnknownAndPreservesDuty() {
        Calls calls = new Calls(); calls.unbindFailure = new IllegalArgumentException("unexpected");
        BindingResponsibility r = new BindingResponsibility(calls); r.executeBind(() -> true); r.requestCleanup();
        assertEquals(BindingResponsibility.CleanupState.UNBIND_UNKNOWN, r.cleanupState());
        assertSame(calls.unbindFailure, r.firstCleanupError()); assertTrue(r.hasCleanupDuty());
    }

    @Test public void unknownUnbindDoesNotEraseOrSilentlyRetryDuty() {
        Calls calls = new Calls(); calls.unbindFailure = new IllegalStateException("unbind failed");
        BindingResponsibility r = new BindingResponsibility(calls); r.executeBind(() -> true); r.requestCleanup();
        Throwable first = r.firstCleanupError(); r.requestCleanup();
        assertEquals(1, calls.unbinds); assertSame(first, r.firstCleanupError());
        assertEquals(BindingResponsibility.CleanupState.UNBIND_UNKNOWN, r.cleanupState());
        assertTrue(r.hasCleanupDuty());
    }

    @Test public void knownUnboundCleanupIsIdempotent() {
        Calls calls = new Calls(); BindingResponsibility r = new BindingResponsibility(calls);
        r.executeBind(() -> true); r.requestCleanup(); r.requestCleanup();
        assertEquals(1, calls.unbinds); assertFalse(r.hasCleanupDuty());
    }

    @Test public void cleanupDuringBindDefersUntilTrueReturn() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        Calls calls = new Calls();
        BindingResponsibility r = new BindingResponsibility(new BindingResponsibility.Operations() {
            public boolean bind() { entered.countDown(); await(release); return calls.bind(); }
            public void unbind() { calls.unbind(); }
        });
        Thread thread = new Thread(() -> r.executeBind(() -> true)); thread.start();
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        r.requestCleanup();
        assertEquals(BindingResponsibility.CleanupState.DEFERRED, r.cleanupState());
        assertEquals(0, calls.unbinds); release.countDown(); thread.join(2000);
        assertFalse(thread.isAlive()); assertEquals(1, calls.unbinds);
        assertEquals(BindingResponsibility.CleanupState.UNBOUND, r.cleanupState());
    }

    @Test public void externalBindAndUnbindDoNotHoldMonitor() {
        final BindingResponsibility[] box = new BindingResponsibility[1];
        AtomicBoolean bindHeld = new AtomicBoolean(), unbindHeld = new AtomicBoolean();
        box[0] = new BindingResponsibility(new BindingResponsibility.Operations() {
            public boolean bind() { bindHeld.set(Thread.holdsLock(box[0])); return true; }
            public void unbind() { unbindHeld.set(Thread.holdsLock(box[0])); }
        });
        box[0].executeBind(() -> { assertFalse(Thread.holdsLock(box[0])); return true; });
        box[0].requestCleanup();
        assertFalse(bindHeld.get()); assertFalse(unbindHeld.get());
    }

    @Test public void oldResponsibilityCannotCleanDifferentConnection() {
        Calls old = new Calls(), current = new Calls();
        BindingResponsibility a = new BindingResponsibility(old), b = new BindingResponsibility(current);
        a.executeBind(() -> true); b.executeBind(() -> true); a.requestCleanup();
        assertEquals(1, old.unbinds); assertEquals(0, current.unbinds); assertTrue(b.hasCleanupDuty());
    }

    private static void await(CountDownLatch latch) {
        try { if (!latch.await(2, TimeUnit.SECONDS)) throw new AssertionError("test barrier timeout"); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new AssertionError(error); }
    }
}
