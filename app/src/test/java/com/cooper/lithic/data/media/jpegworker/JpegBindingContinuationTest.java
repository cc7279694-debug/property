package com.cooper.lithic.data.media.jpegworker;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** The real binding owner is tested; reflection permits an assertion RED before the new API exists. */
public class JpegBindingContinuationTest {
    private void listen(JpegBindingDuty duty, Runnable callback) throws Exception {
        Method target = null;
        for (Method method : JpegBindingDuty.class.getMethods()) if (method.getName().equals("requestQuiescentCleanup")) target = method;
        assertNotNull("A late real RPC return must retain an original-session cleanup continuation", target);
        target.invoke(duty, callback);
    }
    private JpegBindingDuty bound() { JpegBindingDuty d = new JpegBindingDuty(); assertTrue(d.begin()); d.returned(true); return d; }

    @Test public void lateActualReturnDispatchesOriginalCleanupExactlyOnce() throws Exception {
        JpegBindingDuty d = bound(); long ticket = d.claimRpc(); AtomicInteger calls = new AtomicInteger();
        Runnable callback = () -> { assertFalse(Thread.holdsLock(d)); calls.incrementAndGet(); assertTrue(d.takeCleanup()); d.cleanupSucceeded(); };
        listen(d, callback); assertEquals(0, calls.get()); assertFalse(d.takeCleanup());
        assertTrue(d.finishRpc(ticket)); assertEquals(1, calls.get()); assertTrue(d.cleaned());
        listen(d, callback); assertFalse(d.finishRpc(ticket)); assertEquals(1, calls.get());
        assertNull("Callback assertions must not be hidden by production failure recording", d.quiescentDispatchError());
    }
    @Test public void exceptionReturnAlsoContinuesMatchingResponsibility() throws Exception {
        JpegBindingDuty d = bound(); CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger(); ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> rpc = exec.submit(() -> { try { d.callRpc(() -> { entered.countDown(); release.await(); throw new java.io.IOException("late"); }); fail(); }
            catch (java.io.IOException expected) {} catch (Exception error) { throw new AssertionError(error); } });
        try { assertTrue(entered.await(5, TimeUnit.SECONDS)); listen(d, calls::incrementAndGet); assertEquals(0,calls.get());
            release.countDown(); rpc.get(5,TimeUnit.SECONDS); assertEquals(1,calls.get()); assertFalse(d.cleaned()); }
        finally { release.countDown(); exec.shutdown(); assertTrue(exec.awaitTermination(5,TimeUnit.SECONDS)); }
        assertNull(d.quiescentDispatchError());
    }
    @Test public void callingBindIsNotQuiescentAndCannotUnbind() throws Exception {
        JpegBindingDuty d = new JpegBindingDuty(); assertTrue(d.begin()); AtomicInteger calls = new AtomicInteger();
        listen(d,calls::incrementAndGet); assertFalse(d.closeRpcAdmission()); assertFalse(d.takeCleanup()); assertEquals(0,calls.get());
        d.returned(true); assertEquals(1,calls.get()); assertTrue(d.takeCleanup());
        assertNull(d.quiescentDispatchError());
    }
    @Test public void falseBindReturnDispatchesItsCleanupResponsibility() throws Exception {
        JpegBindingDuty d = new JpegBindingDuty(); d.begin(); AtomicInteger calls = new AtomicInteger();
        listen(d,calls::incrementAndGet); d.returned(false); assertEquals(1,calls.get()); assertTrue(d.takeCleanup());
        assertNull(d.quiescentDispatchError());
    }
    @Test public void thrownBindDispatchesItsCleanupResponsibility() throws Exception {
        JpegBindingDuty d = new JpegBindingDuty(); d.begin(); AtomicInteger calls = new AtomicInteger();
        listen(d,calls::incrementAndGet); d.threw(); assertEquals(1,calls.get()); assertTrue(d.takeCleanup());
        assertNull(d.quiescentDispatchError());
    }
    @Test public void cancelledBeforeBindCannotCreateNewBinding() throws Exception {
        JpegBindingDuty d = new JpegBindingDuty(); AtomicInteger calls = new AtomicInteger();
        listen(d,() -> {calls.incrementAndGet(); assertFalse(d.takeCleanup());});
        assertEquals(1,calls.get()); assertTrue(d.cleaned()); assertFalse(d.begin()); assertEquals(0,d.claimRpc());
        assertNull(d.quiescentDispatchError());
    }
    @Test public void dispatchFailureDoesNotClaimOrEraseUnbindResponsibility() throws Exception {
        JpegBindingDuty d = bound(); listen(d,() -> {throw new java.util.concurrent.RejectedExecutionException("post failed");});
        assertFalse(d.cleaned()); assertTrue(d.takeCleanup()); assertFalse(d.cleaned());
        assertEquals("RejectedExecutionException", d.quiescentDispatchError());
    }
    @Test public void callbackMayAcquireDutyFromAnotherThreadBecauseItRunsOutsideLock() throws Exception {
        JpegBindingDuty d = bound(); AtomicInteger observations = new AtomicInteger();
        listen(d,() -> { Thread t = new Thread(() -> {d.outcome(); observations.incrementAndGet();}); t.start();
            try {t.join(1000);}catch(InterruptedException error){throw new AssertionError(error);} assertEquals(1,observations.get()); });
        assertEquals(1,observations.get());
        assertNull(d.quiescentDispatchError());
    }
    @Test public void callbackCannotReopenRpcOrSuccessAfterFailure() throws Exception {
        JpegBindingDuty d = bound(); JpegSessionFence f = new JpegSessionFence("a","s","n",10,20);
        assertTrue(f.hello("a","s","n",20,11,1)); assertTrue(f.beginDelivery()); f.fail("TIMEOUT");
        long ticket=d.claimRpc(); listen(d,() -> {assertEquals(0,d.claimRpc());assertFalse(f.result("a","s","n",11,1,true,true,true,true,true));});
        assertTrue(d.finishRpc(ticket)); assertFalse(f.canPrepare());
        assertNull(d.quiescentDispatchError());
    }
    @Test public void simultaneousReturnAndRepeatedSameCleanupRequestDispatchOnce() throws Exception {
        JpegBindingDuty d=bound(); long ticket=d.claimRpc(); AtomicInteger calls=new AtomicInteger(); Runnable action=calls::incrementAndGet;
        CountDownLatch go=new CountDownLatch(1); Thread a=new Thread(() -> {try{go.await();listen(d,action);}catch(Exception e){throw new AssertionError(e);}});
        Thread b=new Thread(() -> {try{go.await();d.finishRpc(ticket);}catch(Exception e){throw new AssertionError(e);}});
        a.start();b.start();go.countDown();a.join();b.join();listen(d,action);assertEquals(1,calls.get());
        assertNull(d.quiescentDispatchError());
    }
    @Test public void permanentlyInflightRpcNeverDispatchesOrClaimsCleanup() throws Exception {
        JpegBindingDuty d=bound();assertTrue(d.claimRpc()>0);AtomicInteger calls=new AtomicInteger();listen(d,calls::incrementAndGet);
        assertEquals(0,calls.get());assertFalse(d.takeCleanup());assertFalse(d.cleaned());
        assertNull(d.quiescentDispatchError());
    }
}
