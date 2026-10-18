package com.cooper.lithic.data.media.jpegworker;
import org.junit.Test;
import static org.junit.Assert.*;
public class JpegResultWaitTest {
 @Test public void liveWorkerWithoutResultWaits(){assertEquals(JpegResultWait.Decision.WAIT,new JpegResultWait(1000,100).observe(10,false,false));}
 @Test public void liveWorkerAtDeadlineIsTimeout(){assertEquals(JpegResultWait.Decision.TIMEOUT,new JpegResultWait(1000,100).observe(1000,false,false));}
 @Test public void deathAloneAllowsShortResultOrderingWindow(){assertEquals(JpegResultWait.Decision.WAIT,new JpegResultWait(1000,100).observe(10,false,true));}
 @Test public void deathThenResultWithinWindowIsAccepted(){JpegResultWait w=new JpegResultWait(1000,100);w.observe(10,false,true);assertEquals(JpegResultWait.Decision.RESULT_READY,w.observe(50,true,true));}
 @Test public void resultBeforeDeathIsAccepted(){assertEquals(JpegResultWait.Decision.RESULT_READY,new JpegResultWait(1000,100).observe(10,true,false));}
 @Test public void deadWithoutResultIsUnknownNotTimeout(){JpegResultWait w=new JpegResultWait(1000,100);w.observe(10,false,true);assertEquals(JpegResultWait.Decision.DEAD_WITHOUT_RESULT,w.observe(110,false,true));}
 @Test public void repeatedDeathDoesNotExtendWindow(){JpegResultWait w=new JpegResultWait(1000,100);w.observe(10,false,true);w.observe(99,false,true);assertEquals(JpegResultWait.Decision.DEAD_WITHOUT_RESULT,w.observe(110,false,true));}
 @Test public void totalDeadlineDoesNotExtendForLateDeath(){JpegResultWait w=new JpegResultWait(1000,100);w.observe(990,false,true);assertEquals(JpegResultWait.Decision.DEAD_WITHOUT_RESULT,w.observe(1000,false,true));}
 @Test public void lateResultCannotReviveDeadUnknown(){JpegResultWait w=new JpegResultWait(1000,100);w.observe(10,false,true);w.observe(110,false,true);assertEquals(JpegResultWait.Decision.DEAD_WITHOUT_RESULT,w.observe(111,true,true));}
 @Test public void lateResultCannotReviveTimeout(){JpegResultWait w=new JpegResultWait(1000,100);w.observe(1000,false,false);assertEquals(JpegResultWait.Decision.TIMEOUT,w.observe(1001,true,false));}
 @Test public void backwardsClockFailsClosed(){JpegResultWait w=new JpegResultWait(1000,100);w.observe(10,false,false);assertEquals(JpegResultWait.Decision.CLOCK_UNKNOWN,w.observe(9,true,false));}
 @Test public void resultAfterTotalDeadlineIsTimeout(){assertEquals(JpegResultWait.Decision.TIMEOUT,new JpegResultWait(1000,100).observe(1001,true,false));}
 @Test public void bindingUnknownBlocksAdmissionDespiteConsumerEnd(){assertFalse(JpegResultWait.mayReleaseAdmission(true,false));}
 @Test public void closedBindingWithoutConsumerProofBlocksAdmission(){assertFalse(JpegResultWait.mayReleaseAdmission(false,true));}
 @Test public void bothTerminationAndConnectionCleanupMayRelease(){assertTrue(JpegResultWait.mayReleaseAdmission(true,true));}
 @Test(expected=IllegalArgumentException.class) public void negativeGraceRejected(){new JpegResultWait(1000,-1);}
 @Test(expected=IllegalArgumentException.class) public void negativeDeadlineRejected(){new JpegResultWait(-1,100);}
}
