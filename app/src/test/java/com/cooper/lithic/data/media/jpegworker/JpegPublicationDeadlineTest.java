package com.cooper.lithic.data.media.jpegworker;
import org.junit.Test;
import static org.junit.Assert.*;
public class JpegPublicationDeadlineTest {
 @Test public void provenResultBeforeFixedDeadlineMayPublish(){assertEquals(JpegResultWait.Decision.RESULT_READY,JpegResultWait.publicationAt(1199,1000,1200));}
 @Test public void delayedWatchdogCannotAllowExpiredPublication(){assertEquals(JpegResultWait.Decision.TIMEOUT,JpegResultWait.publicationAt(1200,1000,1200));assertEquals(JpegResultWait.Decision.TIMEOUT,JpegResultWait.publicationAt(1201,1000,1200));}
 @Test public void backwardsOrInvalidClockCannotApprovePublication(){assertEquals(JpegResultWait.Decision.CLOCK_UNKNOWN,JpegResultWait.publicationAt(999,1000,1200));assertEquals(JpegResultWait.Decision.CLOCK_UNKNOWN,JpegResultWait.publicationAt(1200,1200,1000));}
}
