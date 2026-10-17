package com.cooper.lithic.m10r3.outsider;

import org.junit.Test;
import static org.junit.Assert.*;

/** Tests our binding/cleanup decisions; Android registration semantics remain device-only. */
public final class BindingAttemptTest {
    @Test public void deniedAfterRegistrationStillReleasesConnection() {
        Platform platform = new Platform(); platform.bindFailure = new SecurityException("denied");
        BindingAttempt attempt = new BindingAttempt(platform); attempt.run();
        assertEquals(BindingAttempt.Outcome.DENIED, attempt.outcome());
        assertEquals(BindingAttempt.Cleanup.RELEASED, attempt.cleanup());
        assertFalse(platform.registered); assertTrue(attempt.probePassed());
        assertSame(platform.bindFailure, attempt.bindingError());
    }

    @Test public void deniedBeforeRegistrationRecordsExpectedUnregisteredCleanup() {
        Platform platform = new Platform(); platform.registerOnBind = false;
        platform.bindFailure = new SecurityException("denied");
        BindingAttempt attempt = new BindingAttempt(platform); attempt.run();
        assertEquals(BindingAttempt.Cleanup.NOT_REGISTERED, attempt.cleanup());
        assertTrue(attempt.probePassed()); assertNotNull(attempt.cleanupError());
    }

    @Test public void returnedTrueReleasesConnectionButNeverPassesSecurityProbe() {
        Platform platform = new Platform(); platform.bound = true;
        BindingAttempt attempt = new BindingAttempt(platform); attempt.run();
        assertEquals(BindingAttempt.Outcome.RETURNED_TRUE, attempt.outcome());
        assertEquals(BindingAttempt.Cleanup.RELEASED, attempt.cleanup());
        assertFalse(platform.registered); assertFalse(attempt.probePassed());
    }

    @Test public void returnedFalseStillReleasesPossibleRegistration() {
        Platform platform = new Platform();
        BindingAttempt attempt = new BindingAttempt(platform); attempt.run();
        assertEquals(BindingAttempt.Outcome.RETURNED_FALSE, attempt.outcome());
        assertEquals(BindingAttempt.Cleanup.RELEASED, attempt.cleanup());
        assertFalse(platform.registered); assertTrue(attempt.probePassed());
    }

    @Test public void returnedFalseWithoutRegistrationIsExplicitlyClassified() {
        Platform platform = new Platform(); platform.registerOnBind = false;
        BindingAttempt attempt = new BindingAttempt(platform); attempt.run();
        assertEquals(BindingAttempt.Cleanup.NOT_REGISTERED, attempt.cleanup());
        assertTrue(attempt.probePassed());
    }

    @Test public void unexpectedBindingExceptionStillReleasesRegistrationAndFailsProbe() {
        Platform platform = new Platform(); platform.bindFailure = new IllegalStateException("binder failed");
        BindingAttempt attempt = new BindingAttempt(platform); attempt.run();
        assertEquals(BindingAttempt.Outcome.ERROR, attempt.outcome());
        assertEquals(BindingAttempt.Cleanup.RELEASED, attempt.cleanup());
        assertFalse(platform.registered); assertFalse(attempt.probePassed());
    }

    @Test public void destroyRetryCanReleaseConnectionWithoutErasingEarlierCleanupFailure() {
        Platform platform = new Platform(); platform.unbindFailure = new SecurityException("unbind failed");
        BindingAttempt attempt = new BindingAttempt(platform); attempt.run();
        assertEquals(BindingAttempt.Cleanup.FAILED, attempt.cleanup());
        assertTrue(platform.registered); assertFalse(attempt.probePassed());
        Throwable original = attempt.firstUnexpectedCleanupError();
        platform.unbindFailure = null; attempt.close();
        assertEquals(BindingAttempt.Cleanup.RELEASED, attempt.cleanup());
        assertFalse(platform.registered); assertSame(original, attempt.firstUnexpectedCleanupError());
        assertFalse(attempt.probePassed());
    }

    @Test public void destroyAfterSuccessfulCleanupDoesNotUnbindTwice() {
        Platform platform = new Platform();
        BindingAttempt attempt = new BindingAttempt(platform); attempt.run(); attempt.close(); attempt.close();
        assertEquals(1, platform.unbindCalls); assertTrue(attempt.probePassed());
    }

    @Test public void destroyWithoutAnyBindingAttemptDoesNotUnbind() {
        Platform platform = new Platform();
        BindingAttempt attempt = new BindingAttempt(platform); attempt.close();
        assertEquals(0, platform.unbindCalls);
        assertEquals(BindingAttempt.Cleanup.NOT_NEEDED, attempt.cleanup());
        assertFalse(attempt.probePassed());
    }

    @Test public void unexpectedlyUnregisteredSuccessfulBindIsNotTreatedAsHarmless() {
        Platform platform = new Platform(); platform.bound = true; platform.registerOnBind = false;
        BindingAttempt attempt = new BindingAttempt(platform); attempt.run();
        assertEquals(BindingAttempt.Cleanup.FAILED, attempt.cleanup());
        assertNotNull(attempt.firstUnexpectedCleanupError()); assertFalse(attempt.probePassed());
    }

    @Test public void secondRunCannotRegisterTheSameConnectionAgain() {
        Platform platform = new Platform(); BindingAttempt attempt = new BindingAttempt(platform); attempt.run();
        assertThrows(IllegalStateException.class, attempt::run);
        assertEquals(1, platform.bindCalls); assertEquals(1, platform.unbindCalls);
    }

    @Test public void fatalBindingErrorIsRethrownAfterCleanupInsteadOfReportedAsRejection() {
        Platform platform = new Platform(); platform.fatalBindFailure = new AssertionError("fatal");
        BindingAttempt attempt = new BindingAttempt(platform);
        AssertionError error = assertThrows(AssertionError.class, attempt::run);
        assertSame(platform.fatalBindFailure, error); assertFalse(platform.registered);
        assertEquals(BindingAttempt.Cleanup.RELEASED, attempt.cleanup()); assertFalse(attempt.probePassed());
    }

    private static final class Platform implements BindingAttempt.Binding {
        boolean registerOnBind = true, registered, bound;
        int bindCalls, unbindCalls;
        RuntimeException bindFailure, unbindFailure;
        Error fatalBindFailure;
        @Override public boolean bind() {
            bindCalls++; registered = registerOnBind;
            if (fatalBindFailure != null) throw fatalBindFailure;
            if (bindFailure != null) throw bindFailure;
            return bound;
        }
        @Override public void unbind() {
            unbindCalls++;
            if (unbindFailure != null) throw unbindFailure;
            if (!registered) throw new IllegalArgumentException("Service not registered");
            registered = false;
        }
    }
}
