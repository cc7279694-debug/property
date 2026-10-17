package com.cooper.lithic.m10r3.probe;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/** Tests the real research gate; Android Binder identity acquisition remains device coverage. */
public final class SessionGateTest {
    private static final String SHA = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private SessionGate fresh() {
        return new SessionGate(1, "session-one", "attempt-one", "generation-one", SHA, 1234L, 10123);
    }

    private void hello(SessionGate gate) {
        assertTrue(gate.handshake(10123, 4321, 4321, 10123, true));
    }

    private boolean success(SessionGate gate) {
        return gate.result(1, "session-one", "attempt-one", "generation-one", SHA, 1234L, "SUCCESS", true);
    }

    @Test public void matchingResultRequiresWorkerDeathBeforePreparation() {
        SessionGate gate = fresh();
        hello(gate);
        assertTrue(success(gate));
        assertFalse(gate.canPrepare());
        gate.died();
        assertTrue(gate.canPrepare());
        assertEquals(1, gate.acceptedResults());
    }

    @Test public void deathBeforeQueuedMatchingResultCanConvergeOnce() {
        SessionGate gate = fresh();
        hello(gate);
        gate.died();
        assertEquals("UNKNOWN", gate.status());
        assertFalse(gate.canPrepare());
        assertTrue(success(gate));
        assertTrue(gate.canPrepare());
        assertFalse(success(gate));
        assertEquals(1, gate.acceptedResults());
    }

    @Test public void deathWithoutMatchingResultStaysUnknownAndCannotPrepare() {
        SessionGate gate = fresh();
        hello(gate);
        gate.died();
        gate.died();
        assertTrue(gate.isDead());
        assertEquals("UNKNOWN", gate.status());
        assertEquals(0, gate.acceptedResults());
        assertFalse(gate.canPrepare());
    }

    @Test public void resultBeforeHandshakeCannotAuthorizePreparation() {
        SessionGate gate = fresh();
        assertFalse(success(gate));
        gate.died();
        assertFalse(gate.canPrepare());
        assertEquals(0, gate.acceptedResults());
    }

    @Test public void wrongUidCannotClaimSameApplicationWorker() {
        SessionGate gate = fresh();
        assertFalse(gate.handshake(10124, 4321, 4321, 10123, true));
        assertFalse(gate.handshake(10123, 4321, 4321, 10124, true));
        assertFalse(success(gate));
        hello(gate);
        assertTrue(success(gate));
    }

    @Test public void claimedPidMustMatchPositiveActualBinderCallerPid() {
        SessionGate gate = fresh();
        assertFalse(gate.handshake(10123, 4321, 4322, 10123, true));
        assertFalse(gate.handshake(10123, 0, 0, 10123, true));
        assertFalse(gate.handshake(10123, -1, -1, 10123, true));
        assertFalse(success(gate));
        hello(gate);
    }

    @Test public void unconfirmedApplicationRoleCannotStartSession() {
        SessionGate gate = fresh();
        assertFalse(gate.handshake(10123, 4321, 4321, 10123, false));
        assertFalse(success(gate));
        hello(gate);
    }

    @Test public void repeatedHandshakeCannotReplaceCurrentWorker() {
        SessionGate gate = fresh();
        hello(gate);
        assertFalse(gate.handshake(10123, 4321, 4321, 10123, true));
        assertFalse(gate.handshake(10123, 9999, 9999, 10123, true));
        assertTrue(success(gate));
        gate.died();
        assertTrue(gate.canPrepare());
    }

    @Test public void deadOrRevokedSessionCannotHandshakeAgain() {
        SessionGate dead = fresh();
        dead.died();
        assertFalse(dead.handshake(10123, 4321, 4321, 10123, true));
        SessionGate revoked = fresh();
        revoked.revoke("CANCELLED");
        assertFalse(revoked.handshake(10123, 4321, 4321, 10123, true));
    }

    @Test public void eachResultIdentityFieldMustMatchExactly() {
        SessionGate gate = fresh();
        hello(gate);
        assertFalse(gate.result(2, "session-one", "attempt-one", "generation-one", SHA, 1234L, "SUCCESS", true));
        assertFalse(gate.result(1, "stale-session", "attempt-one", "generation-one", SHA, 1234L, "SUCCESS", true));
        assertFalse(gate.result(1, "session-one", "stale-attempt", "generation-one", SHA, 1234L, "SUCCESS", true));
        assertFalse(gate.result(1, "session-one", "attempt-one", "other-generation", SHA, 1234L, "SUCCESS", true));
        assertFalse(gate.result(1, "session-one", "attempt-one", "generation-one", SHA.toUpperCase(), 1234L, "SUCCESS", true));
        assertFalse(gate.result(1, "session-one", "attempt-one", "generation-one", SHA, 1235L, "SUCCESS", true));
        assertEquals(0, gate.acceptedResults());
        assertTrue(success(gate));
        gate.died();
        assertTrue(gate.canPrepare());
    }

    @Test public void missingIdentityOrUnknownStatusCannotConsumeResultSlot() {
        SessionGate gate = fresh();
        hello(gate);
        assertFalse(gate.result(1, null, "attempt-one", "generation-one", SHA, 1234L, "SUCCESS", true));
        assertFalse(gate.result(1, "session-one", null, "generation-one", SHA, 1234L, "SUCCESS", true));
        assertFalse(gate.result(1, "session-one", "attempt-one", null, SHA, 1234L, "SUCCESS", true));
        assertFalse(gate.result(1, "session-one", "attempt-one", "generation-one", null, 1234L, "SUCCESS", true));
        assertFalse(gate.result(1, "session-one", "attempt-one", "generation-one", SHA, 1234L, null, true));
        assertFalse(gate.result(1, "session-one", "attempt-one", "generation-one", SHA, 1234L, "INVENTED_OK", true));
        assertEquals(0, gate.acceptedResults());
        assertTrue(success(gate));
    }

    @Test public void successfulComputationWithoutFdCloseCannotPrepare() {
        SessionGate gate = fresh();
        hello(gate);
        assertTrue(gate.result(1, "session-one", "attempt-one", "generation-one", SHA, 1234L, "SUCCESS", false));
        gate.died();
        assertFalse(gate.canPrepare());
        assertFalse(success(gate));
        assertEquals(1, gate.acceptedResults());
    }

    @Test public void everyFailureOutcomeForbidsPreparationEvenAfterDeath() {
        String[] statuses = {"CANCELLED", "TIMEOUT", "UNKNOWN", "RESULT_UNKNOWN", "VALIDATION_FAILED",
                "RESOURCE_REJECTED", "LOAD_ERROR", "NATIVE_CRASH"};
        for (String status : statuses) {
            SessionGate gate = fresh();
            hello(gate);
            assertTrue(status, gate.result(1, "session-one", "attempt-one", "generation-one", SHA, 1234L, status, true));
            gate.died();
            assertEquals(status, gate.status());
            assertFalse(status, gate.canPrepare());
            assertFalse(status, success(gate));
        }
    }

    @Test public void revocationRejectsLateSuccessAndCannotBeOverwritten() {
        SessionGate gate = fresh();
        hello(gate);
        gate.revoke("TIMEOUT");
        assertTrue(gate.isRevoked());
        gate.died();
        assertFalse(success(gate));
        gate.revoke("CANCELLED");
        assertEquals("TIMEOUT", gate.status());
        assertFalse(gate.canPrepare());
        assertEquals(0, gate.acceptedResults());
    }

    @Test public void revocationAfterQueuedSuccessStillForbidsPreparation() {
        SessionGate gate = fresh();
        hello(gate);
        assertTrue(success(gate));
        gate.revoke("CANCELLED");
        gate.died();
        assertTrue(gate.isRevoked());
        assertFalse(gate.canPrepare());
        assertEquals("CANCELLED", gate.status());
        assertEquals(1, gate.acceptedResults());
    }

    @Test public void nullRevocationReasonStillRevokesPermanently() {
        SessionGate gate = fresh();
        hello(gate);
        gate.revoke(null);
        gate.died();
        assertTrue(gate.isRevoked());
        assertFalse(success(gate));
        assertFalse(gate.canPrepare());
        assertEquals("REVOKED", gate.status());
    }

    @Test public void concurrentDuplicateResultsAreAcceptedAtMostOnce() throws Exception {
        final SessionGate gate = fresh();
        hello(gate);
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicInteger accepted = new AtomicInteger();
        Thread[] threads = new Thread[8];
        for (int index = 0; index < threads.length; index++) {
            threads[index] = new Thread(() -> {
                try { start.await(); } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt(); return;
                }
                if (success(gate)) accepted.incrementAndGet();
            });
            threads[index].start();
        }
        start.countDown();
        for (Thread thread : threads) { thread.join(10000); assertFalse(thread.isAlive()); }
        assertEquals(1, accepted.get());
        assertEquals(1, gate.acceptedResults());
        gate.died();
        assertTrue(gate.canPrepare());
    }
}
