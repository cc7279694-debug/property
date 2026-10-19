package com.cooper.lithic.data.media.jpegworker;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

/** HOST_ONLY characterization. A passing assertion reproduces an OPEN blocker, never its repair. */
public final class LateRpcOpenBlockerTest {
    @Test public void lateReturnAfterTwoRejectedCleanupOpportunitiesDoesNotAutomaticallyClean() throws Exception {
        JpegBindingDuty duty = new JpegBindingDuty();
        assertTrue(duty.begin());
        duty.returned(true);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Void> actualRpc = executor.submit(() -> duty.callRpc(() -> {
            entered.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Synthetic RPC release not delivered");
            return null;
        }));
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            // Model only the two explicit Client cleanup opportunities; no Binder or Client is simulated.
            assertFalse(duty.closeRpcAdmission());
            assertFalse(duty.takeCleanup());
            assertFalse(duty.closeRpcAdmission());
            assertFalse(duty.takeCleanup());
            assertFalse(duty.cleaned());
            release.countDown();
            actualRpc.get(5, TimeUnit.SECONDS); // The real callRpc() finally completes its actual ticket.
            assertFalse("An RPC return does not itself perform connection cleanup", duty.cleaned());
            assertEquals("Closed admission cannot be reopened by the late return", 0, duty.claimRpc());
            assertTrue("A new responsible cleanup actor could still claim cleanup", duty.takeCleanup());
            assertFalse("Claiming responsibility is not proof Android unbind occurred", duty.cleaned());
            System.out.println("REPRODUCES_OPEN_BLOCKER: two_cleanup_opportunities_rejected; late_rpc_returned; "
                    + "automatic_cleanup=false; manual_cleanup_claim_possible=true; Android_unbind=NOT_RUN");
        } finally {
            release.countDown();
            executor.shutdown();
            assertTrue("Synthetic executor must terminate", executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
