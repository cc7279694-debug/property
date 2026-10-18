package com.cooper.lithic.data.media.jpegworker;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
public class JpegRetirementHandshakeTest {
 private JpegRetirementHandshake bound(){JpegRetirementHandshake h=new JpegRetirementHandshake();assertTrue(h.onBound());return h;}
 @Test public void authenticatedClosedResultArmsWhileStillBound(){JpegRetirementHandshake h=bound();assertTrue(h.prepareNormal(true,true));}
 @Test public void normalAckDoesNotExitBeforeOnUnbind(){JpegRetirementHandshake h=bound();assertTrue(h.prepareNormal(true,true));assertFalse(h.deadline());}
 @Test public void acknowledgedNormalExitsOnlyAfterDetach(){JpegRetirementHandshake h=bound();assertTrue(h.prepareNormal(true,true));assertTrue(h.onUnbind());}
 @Test public void unauthenticatedNormalCannotArm(){JpegRetirementHandshake h=bound();assertFalse(h.prepareNormal(false,true));assertFalse(h.onUnbind());}
 @Test public void unknownDataCloseCannotArmNormal(){JpegRetirementHandshake h=bound();assertFalse(h.prepareNormal(true,false));assertFalse(h.onUnbind());}
 @Test public void unpreparedDetachIsNotAuthenticatedRetirement(){assertFalse(bound().onUnbind());}
 @Test public void requestAfterDetachIsRejected(){JpegRetirementHandshake h=bound();h.onUnbind();assertFalse(h.prepareNormal(true,true));assertFalse(h.prepareRevoked(true));}
 @Test public void revokedWorkMayExitWithUnknownNativeCloseOnlyAfterDetach(){JpegRetirementHandshake h=bound();assertTrue(h.prepareRevoked(true));assertTrue(h.onUnbind());}
 @Test public void unauthenticatedRevokeCannotArm(){JpegRetirementHandshake h=bound();assertFalse(h.prepareRevoked(false));assertFalse(h.onUnbind());}
 @Test public void deadlineWhileBoundCannotKillAndRestartWorker(){JpegRetirementHandshake h=bound();assertFalse(h.deadline());assertTrue(h.onUnbind());}
 @Test public void deadlineAfterUnknownDetachCanExitWithoutNewBinding(){JpegRetirementHandshake h=bound();assertFalse(h.onUnbind());assertTrue(h.deadline());}
 @Test public void deadlineRejectsLaterNormalAuthorization(){JpegRetirementHandshake h=bound();h.deadline();assertFalse(h.prepareNormal(true,true));}
 @Test public void revokedAuthorizationCannotBecomeNormalAgain(){JpegRetirementHandshake h=bound();h.prepareRevoked(true);assertFalse(h.prepareNormal(true,true));}
 @Test public void cancellationMaySupersedeNormalBeforeDetach(){JpegRetirementHandshake h=bound();assertTrue(h.prepareNormal(true,true));assertTrue(h.prepareRevoked(true));assertFalse(h.prepareNormal(true,true));assertTrue(h.onUnbind());}
 @Test public void duplicateRequestIsIdempotentBeforeDetach(){JpegRetirementHandshake h=bound();assertTrue(h.prepareNormal(true,true));assertTrue(h.prepareNormal(true,true));assertTrue(h.onUnbind());assertFalse(h.onUnbind());}
 @Test public void knownDeadHostAllowsExitWithoutUiDetach(){JpegRetirementHandshake h=bound();assertTrue(h.hostDied());assertFalse(h.onUnbind());}
 @Test public void duplicateHostDeathCannotClaimExitTwice(){JpegRetirementHandshake h=bound();assertTrue(h.hostDied());assertFalse(h.hostDied());assertFalse(h.deadline());}
 @Test public void terminalProcessCannotAcquireNewBinding(){JpegRetirementHandshake h=bound();h.prepareNormal(true,true);h.onUnbind();assertFalse(h.onBound());}
 @Test public void unpreparedDetachedProcessCannotAcquireNewBinding(){JpegRetirementHandshake h=bound();h.onUnbind();assertFalse(h.onBound());}
 @Test public void duplicateBindingRejectedWithoutExpandingConsumerCount(){JpegRetirementHandshake h=bound();assertFalse(h.onBound());assertTrue(h.prepareNormal(true,true));assertTrue(h.onUnbind());}
 @Test public void unboundNewProcessCannotAcceptRetirement(){JpegRetirementHandshake h=new JpegRetirementHandshake();assertFalse(h.prepareNormal(true,true));assertFalse(h.prepareRevoked(true));assertFalse(h.onUnbind());}
 @Test public void deadlineAndDetachRaceClaimsExactlyOneExit() throws Exception {for(int n=0;n<100;n++){JpegRetirementHandshake h=bound();AtomicInteger exits=new AtomicInteger();Thread a=new Thread(()->{if(h.deadline())exits.incrementAndGet();});Thread b=new Thread(()->{if(h.onUnbind())exits.incrementAndGet();});a.start();b.start();a.join();b.join();assertEquals(1,exits.get());}}
 @Test public void deathAndDetachRaceClaimsExactlyOneExit() throws Exception {for(int n=0;n<100;n++){JpegRetirementHandshake h=bound();h.prepareNormal(true,true);AtomicInteger exits=new AtomicInteger();Thread a=new Thread(()->{if(h.hostDied())exits.incrementAndGet();});Thread b=new Thread(()->{if(h.onUnbind())exits.incrementAndGet();});a.start();b.start();a.join();b.join();assertEquals(1,exits.get());}}
}
