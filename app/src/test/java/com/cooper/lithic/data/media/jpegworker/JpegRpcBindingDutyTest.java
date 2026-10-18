package com.cooper.lithic.data.media.jpegworker;
import org.junit.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;
/** Pure JVM exercises the real binding owner; Android/Binder effects stay NOT_RUN. */
public class JpegRpcBindingDutyTest {
 private JpegBindingDuty bound(){JpegBindingDuty d=new JpegBindingDuty();assertTrue(d.begin());d.returned(true);return d;}
 @Test public void unboundInvocationCannotClaimRpc(){assertEquals(0,new JpegBindingDuty().claimRpc());}
 @Test public void successfulBindingOwnsOneRpcTicket(){JpegBindingDuty d=bound();assertTrue(d.claimRpc()>0);assertEquals(0,d.claimRpc());}
 @Test public void earlyConnectedCallbackCannotReplaceActualBindReturn(){JpegBindingDuty d=new JpegBindingDuty();assertTrue(d.begin());assertEquals(0,d.claimRpc());d.returned(true);assertTrue(d.claimRpc()>0);}
 @Test public void falseBindNeverAllowsRpc(){JpegBindingDuty d=new JpegBindingDuty();d.begin();d.returned(false);assertEquals(0,d.claimRpc());}
 @Test public void thrownBindNeverAllowsRpc(){JpegBindingDuty d=new JpegBindingDuty();d.begin();d.threw();assertEquals(0,d.claimRpc());}
 @Test public void cleanupClaimPermanentlyRejectsQueuedRpc(){JpegBindingDuty d=bound();assertTrue(d.takeCleanup());assertEquals(0,d.claimRpc());d.cleanupSucceeded();assertEquals(0,d.claimRpc());}
 @Test public void activeRpcPreventsCleanupClaim(){JpegBindingDuty d=bound();assertTrue(d.claimRpc()>0);assertFalse(d.takeCleanup());assertFalse(d.cleaned());}
 @Test public void inflightClosureNeedsMatchingReturnBeforeCleanup(){JpegBindingDuty d=bound();long ticket=d.claimRpc();assertFalse(d.closeRpcAdmission());assertTrue(d.finishRpc(ticket));assertTrue(d.closeRpcAdmission());assertTrue(d.takeCleanup());assertFalse(d.cleaned());}
 @Test public void completedRpcMayCloseThenUnbind(){JpegBindingDuty d=bound();long ticket=d.claimRpc();assertTrue(d.finishRpc(ticket));assertTrue(d.closeRpcAdmission());assertTrue(d.takeCleanup());d.cleanupSucceeded();assertTrue(d.cleaned());}
 @Test public void wrongTicketCannotReleaseInFlightRpc(){JpegBindingDuty d=bound();long ticket=d.claimRpc();assertFalse(d.finishRpc(ticket+1));assertFalse(d.closeRpcAdmission());assertFalse(d.takeCleanup());}
 @Test public void duplicateCompletionCannotReleaseNewerRpc(){JpegBindingDuty d=bound();long first=d.claimRpc();assertTrue(d.finishRpc(first));long second=d.claimRpc();assertTrue(second>first);assertFalse(d.finishRpc(first));assertFalse(d.closeRpcAdmission());assertTrue(d.finishRpc(second));}
 @Test public void invalidCompletionCannotClaimResourceProof(){JpegBindingDuty d=bound();assertFalse(d.finishRpc(0));assertFalse(d.finishRpc(-1));}
 @Test public void repeatedIdleClosureIsIdempotent(){JpegBindingDuty d=bound();assertTrue(d.closeRpcAdmission());assertTrue(d.closeRpcAdmission());assertEquals(0,d.claimRpc());assertTrue(d.takeCleanup());assertFalse(d.takeCleanup());}
 @Test public void closureBeforeUiBindRunsPreventsNewBind(){JpegBindingDuty d=new JpegBindingDuty();assertTrue(d.closeRpcAdmission());assertFalse(d.begin());assertEquals(0,d.claimRpc());}
 @Test public void timedOutRpcDoesNotReleaseItsTicket() throws Exception {
  JpegBindingDuty d=bound();ExecutorService exec=Executors.newSingleThreadExecutor();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
  Future<?> pending=exec.submit(()->d.callRpc(()->{entered.countDown();release.await();return null;}));
  try {assertTrue(entered.await(5,TimeUnit.SECONDS));try{pending.get(1,TimeUnit.MILLISECONDS);fail("Expected bounded waiter timeout");}catch(TimeoutException expected){}assertFalse(d.closeRpcAdmission());assertFalse(d.takeCleanup());}
  finally {release.countDown();pending.get(5,TimeUnit.SECONDS);exec.shutdownNow();}
  assertTrue(d.closeRpcAdmission());assertTrue(d.takeCleanup());
 }
 @Test public void lateQueuedRpcNeverRunsAfterConnectionClosure() throws Exception {
  JpegBindingDuty d=bound();ExecutorService exec=Executors.newSingleThreadExecutor();CountDownLatch occupied=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger binderCalls=new AtomicInteger();
  Future<?> blocker=exec.submit(()->{occupied.countDown();try{release.await();}catch(InterruptedException e){throw new AssertionError(e);}});
  try {assertTrue(occupied.await(5,TimeUnit.SECONDS));Future<Boolean> queued=exec.submit(()->{try{d.callRpc(()->{binderCalls.incrementAndGet();return null;});return true;}catch(java.io.IOException expected){return false;}});assertTrue(d.closeRpcAdmission());assertTrue(d.takeCleanup());d.cleanupSucceeded();release.countDown();blocker.get(5,TimeUnit.SECONDS);assertFalse(queued.get(5,TimeUnit.SECONDS));assertEquals(0,binderCalls.get());}
  finally {release.countDown();exec.shutdownNow();}
 }
 @Test public void rpcExceptionStillCompletesMatchingTicket(){JpegBindingDuty d=bound();long ticket=d.claimRpc();try{throw new IllegalStateException("Synthetic Binder exception");}catch(IllegalStateException expected){}finally {assertTrue(d.finishRpc(ticket));}assertTrue(d.closeRpcAdmission());assertTrue(d.takeCleanup());}
 @Test public void completionAfterClosedGateCannotReopenAuthorization(){JpegBindingDuty d=bound();long ticket=d.claimRpc();d.closeRpcAdmission();assertTrue(d.finishRpc(ticket));assertEquals(0,d.claimRpc());assertTrue(d.takeCleanup());}
 @Test public void simultaneousClosureAndRpcClaimHasOneLinearization() throws Exception {for(int n=0;n<100;n++){JpegBindingDuty d=bound();CountDownLatch go=new CountDownLatch(1);AtomicLong ticket=new AtomicLong();AtomicBoolean closed=new AtomicBoolean();Thread a=new Thread(()->{await(go);ticket.set(d.claimRpc());});Thread b=new Thread(()->{await(go);closed.set(d.closeRpcAdmission());});a.start();b.start();go.countDown();a.join();b.join();if(ticket.get()>0)assertFalse(closed.get());else assertTrue(closed.get());assertEquals(0,d.claimRpc());}}
 @Test public void competingRpcStartsCannotBothOwnTheConnection() throws Exception {for(int n=0;n<100;n++){JpegBindingDuty d=bound();CountDownLatch go=new CountDownLatch(1);AtomicLong aTicket=new AtomicLong(),bTicket=new AtomicLong();Thread a=new Thread(()->{await(go);aTicket.set(d.claimRpc());});Thread b=new Thread(()->{await(go);bTicket.set(d.claimRpc());});a.start();b.start();go.countDown();a.join();b.join();assertEquals(1,(aTicket.get()>0?1:0)+(bTicket.get()>0?1:0));}}
 private static void await(CountDownLatch gate){try{gate.await();}catch(InterruptedException e){throw new AssertionError(e);}}
}
