package com.cooper.lithic.data.media.jpegworker;
import org.junit.Test;
import static org.junit.Assert.*;
public class JpegBindingDutyTest {
 @Test public void notAttemptedNeedsNoUnbind(){JpegBindingDuty d=new JpegBindingDuty();assertFalse(d.takeCleanup());assertTrue(d.cleaned());assertFalse(d.begin());}
 @Test public void trueReturnRetainsResponsibility(){JpegBindingDuty d=new JpegBindingDuty();assertTrue(d.begin());d.returned(true);assertFalse(d.cleaned());assertTrue(d.takeCleanup());d.cleanupSucceeded();assertTrue(d.cleaned());}
 @Test public void falseReturnStillAttemptsCleanup(){JpegBindingDuty d=new JpegBindingDuty();d.begin();d.returned(false);assertTrue(d.takeCleanup());assertEquals(JpegBindingDuty.Returned.FALSE,d.outcome());}
 @Test public void thrownBindStillAttemptsCleanup(){JpegBindingDuty d=new JpegBindingDuty();d.begin();d.threw();assertTrue(d.takeCleanup());assertEquals(JpegBindingDuty.Returned.THREW,d.outcome());}
 @Test public void unbindErrorRetainsUnknownResponsibility(){JpegBindingDuty d=new JpegBindingDuty();d.begin();d.returned(true);d.takeCleanup();d.cleanupFailed("SecurityException");assertFalse(d.cleaned());assertEquals("SecurityException",d.cleanupError());}
 @Test public void duplicateCleanupCannotRunTwice(){JpegBindingDuty d=new JpegBindingDuty();d.begin();d.returned(true);assertTrue(d.takeCleanup());assertFalse(d.takeCleanup());d.cleanupSucceeded();assertFalse(d.takeCleanup());}
 @Test public void bindCannotBeginAfterCleanupWasClaimed(){JpegBindingDuty d=new JpegBindingDuty();d.begin();d.takeCleanup();assertFalse(d.begin());}
 @Test public void bindReturnRecordedEvenAfterCleanupClaim(){JpegBindingDuty d=new JpegBindingDuty();d.begin();d.takeCleanup();d.returned(true);assertEquals(JpegBindingDuty.Returned.BOUND,d.outcome());}
 @Test(expected=IllegalStateException.class) public void unsolicitedCleanupSuccessRejected(){new JpegBindingDuty().cleanupSucceeded();}
 @Test(expected=IllegalStateException.class) public void unsolicitedCleanupFailureRejected(){new JpegBindingDuty().cleanupFailed("unknown");}
 @Test(expected=IllegalStateException.class) public void duplicateBindReturnRejected(){JpegBindingDuty d=new JpegBindingDuty();d.begin();d.returned(false);d.returned(true);}
}
