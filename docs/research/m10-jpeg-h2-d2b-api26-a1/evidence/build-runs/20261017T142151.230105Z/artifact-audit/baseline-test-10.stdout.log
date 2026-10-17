package com.cooper.lithic.m10r3.probe;

import static org.junit.Assert.*;

import org.junit.Test;

/** Direct tests of the real Java grant decision, independent of Android Binder acquisition. */
public final class SessionGateFailureFenceTest {
    private SessionGate fresh() {
        return new SessionGate(1, "nonce", "attempt", "generation", "sha", 8192, 10123);
    }

    @Test public void grantRequiresCompletedHandshake() {
        assertFalse(fresh().grant());
    }

    @Test public void grantCannotInterleaveItsRevokedAndDeadChecks() {
        assertTrue("grant must be one synchronized memory decision",
                java.lang.reflect.Modifier.isSynchronized(grantMethod().getModifiers()));
    }

    private java.lang.reflect.Method grantMethod() {
        try { return SessionGate.class.getMethod("grant"); }
        catch (NoSuchMethodException impossible) { throw new AssertionError(impossible); }
    }

    @Test public void completedHandshakeAllowsGrantUntilRevocationOrDeath() {
        SessionGate revoked = fresh();
        assertTrue(revoked.handshake(10123, 4000, 4000, 10123, true));
        assertTrue(revoked.grant());
        revoked.revoke("FIRST_FAILURE");
        assertFalse(revoked.grant());
        SessionGate dead = fresh();
        assertTrue(dead.handshake(10123, 4000, 4000, 10123, true));
        dead.died();
        assertFalse(dead.grant());
    }

    @Test public void failureAfterSuccessBeforeDeathStillPreventsPreparation() {
        SessionGate gate = fresh();
        assertTrue(gate.handshake(10123, 4000, 4000, 10123, true));
        assertTrue(gate.result(1, "nonce", "attempt", "generation", "sha", 8192, "SUCCESS", true));
        gate.revoke("BIND_FAILURE");
        gate.died();
        assertFalse(gate.canPrepare());
        assertFalse(gate.grant());
        assertEquals("BIND_FAILURE", gate.status());
    }
}
