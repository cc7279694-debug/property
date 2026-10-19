package com.cooper.lithic.data.media;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
public class M10B1ClosureObservationsTest {
 private static final String W="123/10001/456";
 private static M10B1ClosureObservations gate(){M10B1ClosureObservations g=new M10B1ClosureObservations();g.registerWorker(W);return g;}
 private static M10B1ClosureObservations.Observation row(M10B1ClosureObservations.Origin origin,String outcome,String worker,boolean full){return new M10B1ClosureObservations.Observation(origin,outcome,worker,0,false,false,full,full,full,full,full,full,full);}
 private static M10B1ClosureObservations.Observation initial(boolean full){return row(M10B1ClosureObservations.Origin.INITIAL,"CANCELLED",W,full);}
 private static M10B1ClosureObservations.Observation late(){return row(M10B1ClosureObservations.Origin.LATE_ORIGINAL_QUIESCENCE,"CANCELLED",W,true);}
 @Test public void initialUnknownDoesNotClaimClosureAndLateProofDoes(){var g=gate();assertEquals(M10B1ClosureObservations.Decision.WAIT,g.observe(row(M10B1ClosureObservations.Origin.INITIAL,"RESULT_UNKNOWN",W,false)));assertEquals(M10B1ClosureObservations.Decision.CLOSE_ONCE,g.observe(late()));assertEquals(2,g.observations());}
 @Test public void fullyKnownInitialCancellationMayCloseOnce(){var g=gate();assertEquals(M10B1ClosureObservations.Decision.CLOSE_ONCE,g.observe(initial(true)));assertTrue(g.closureClaimed());}
 @Test public void incompleteInitialMissingIdentityMayOnlyWait(){var g=gate();assertEquals(M10B1ClosureObservations.Decision.WAIT,g.observe(row(M10B1ClosureObservations.Origin.INITIAL,"RESULT_UNKNOWN",null,false)));assertFalse(g.closureClaimed());}
 @Test public void lateProofThenIncompleteInitialNeverClaimsTwice(){var g=gate();assertEquals(M10B1ClosureObservations.Decision.CLOSE_ONCE,g.observe(late()));assertEquals(M10B1ClosureObservations.Decision.WAIT,g.observe(initial(false)));}
 @Test public void duplicateInitialIsConflict(){var g=gate();g.observe(initial(false));assertThrows(IllegalStateException.class,()->g.observe(initial(false)));}
 @Test public void duplicateLateIsConflict(){var g=gate();g.observe(late());assertThrows(IllegalStateException.class,()->g.observe(late()));}
 @Test public void thirdObservationIsConflict(){var g=gate();g.observe(initial(false));g.observe(late());assertThrows(IllegalStateException.class,()->g.observe(initial(false)));assertEquals(3,g.observations());}
 @Test public void differentWorkerIsConflictEvenWhenIncomplete(){var g=gate();assertThrows(IllegalStateException.class,()->g.observe(row(M10B1ClosureObservations.Origin.INITIAL,"CANCELLED","124/10001/456",false)));}
 @Test public void successfulOutcomeCannotClaimCancellationClosure(){var g=gate();assertThrows(IllegalStateException.class,()->g.observe(row(M10B1ClosureObservations.Origin.INITIAL,"SUCCESS",W,true)));}
 @Test public void absentWorkerCannotClaimCompleteClosure(){var g=gate();assertThrows(IllegalStateException.class,()->g.observe(row(M10B1ClosureObservations.Origin.LATE_ORIGINAL_QUIESCENCE,"CANCELLED",null,true)));}
 @Test public void duplicateCompleteProofCannotRecoverTwice(){var g=gate();g.observe(initial(true));assertThrows(IllegalStateException.class,()->g.observe(late()));}
 @Test public void stoppedWindowCannotStartNewRecovery(){var g=gate();g.observe(initial(false));g.stop();assertThrows(IllegalStateException.class,()->g.observe(late()));assertFalse(g.closureClaimed());}
 @Test public void fakeWorkerDataCloseOrResultIsRejected(){var g=gate();assertThrows(IllegalStateException.class,()->g.observe(new M10B1ClosureObservations.Observation(M10B1ClosureObservations.Origin.INITIAL,"CANCELLED",W,1,true,false,true,true,true,true,true,true,true)));}
 @Test public void unknownOrIncompleteLateProofCannotClose(){var g=gate();assertThrows(IllegalStateException.class,()->g.observe(row(M10B1ClosureObservations.Origin.LATE_ORIGINAL_QUIESCENCE,"RESULT_UNKNOWN",W,false)));}
 @Test public void claimedOriginalQuiescenceCannotOmitActualFlags(){var g=gate();assertThrows(IllegalStateException.class,()->g.observe(new M10B1ClosureObservations.Observation(M10B1ClosureObservations.Origin.INITIAL,"CANCELLED",W,0,false,false,false,true,true,true,true,true,true)));}
 @Test public void unregisteredWorkerNeverReceivesAuthority(){var g=new M10B1ClosureObservations();assertThrows(IllegalStateException.class,()->g.observe(initial(true)));}
 @Test public void secondWorkerRegistrationCannotReplaceIdentity(){var g=gate();assertThrows(IllegalStateException.class,()->g.registerWorker("124/10001/456"));}
 @Test public void concurrentCompleteEntriesHaveExactlyOneClaim()throws Exception{var g=gate();CountDownLatch start=new CountDownLatch(1);AtomicInteger claims=new AtomicInteger(),conflicts=new AtomicInteger();Thread a=new Thread(()->{try{start.await();if(g.observe(initial(true))==M10B1ClosureObservations.Decision.CLOSE_ONCE)claims.incrementAndGet();}catch(IllegalStateException e){conflicts.incrementAndGet();}catch(Exception e){throw new RuntimeException(e);}});Thread b=new Thread(()->{try{start.await();if(g.observe(late())==M10B1ClosureObservations.Decision.CLOSE_ONCE)claims.incrementAndGet();}catch(IllegalStateException e){conflicts.incrementAndGet();}catch(Exception e){throw new RuntimeException(e);}});a.start();b.start();start.countDown();a.join();b.join();assertEquals(1,claims.get());assertEquals(1,conflicts.get());}
}
