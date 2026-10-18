package com.cooper.lithic.data.media.jpegworker;

import org.junit.Test;
import static org.junit.Assert.*;

public class JpegSessionFenceTest {
    private JpegSessionFence gate() { return new JpegSessionFence("attempt", "session", "nonce", 101, 1000); }
    private JpegSessionFence ready() { JpegSessionFence g=gate(); assertTrue(g.hello("attempt","session","nonce",1000,202,9));return g; }
    @Test public void unknownWorkerNeverGrants(){assertFalse(gate().beginDelivery());}
    @Test public void sameProcessRejected(){assertFalse(gate().hello("attempt","session","nonce",1000,101,9));}
    @Test public void wrongUidRejected(){assertFalse(gate().hello("attempt","session","nonce",2000,202,9));}
    @Test public void nonceMismatchRejected(){assertFalse(gate().hello("attempt","session","other",1000,202,9));}
    @Test public void unknownStartRejected(){assertFalse(gate().hello("attempt","session","nonce",1000,202,0));}
    @Test public void duplicateHelloRejected(){JpegSessionFence g=ready();assertFalse(g.hello("attempt","session","nonce",1000,202,9));}
    @Test public void onlyOneDelivery(){JpegSessionFence g=ready();assertTrue(g.beginDelivery());assertFalse(g.beginDelivery());}
    @Test public void failureBeforeDeliveryRejects(){JpegSessionFence g=ready();g.fail("CANCELLED");assertFalse(g.beginDelivery());}
    @Test public void firstFailureIsImmutable(){JpegSessionFence g=ready();g.fail("TIMEOUT");g.fail("UNKNOWN");assertEquals("TIMEOUT",g.failure());}
    @Test public void oldEpochResultRejected(){JpegSessionFence g=ready();g.beginDelivery();assertFalse(g.result("other","session","nonce",202,9,true,true,true,true,true));}
    @Test public void resultRequiresDelivery(){assertFalse(ready().result("attempt","session","nonce",202,9,true,true,true,true,true));}
    @Test public void wrongProcessResultRejected(){JpegSessionFence g=ready();g.beginDelivery();assertFalse(g.result("attempt","session","nonce",203,9,true,true,true,true,true));}
    @Test public void resultAfterFailureRejected(){JpegSessionFence g=ready();g.beginDelivery();g.fail("CANCELLED");assertFalse(g.result("attempt","session","nonce",202,9,true,true,true,true,true));}
    @Test public void resultThenMatchingDeathMayPrepare(){JpegSessionFence g=ready();g.beginDelivery();assertTrue(g.result("attempt","session","nonce",202,9,true,true,true,true,true));g.ended(true,true,true);assertTrue(g.canPrepare());}
    @Test public void matchingDeathThenResultMayPrepare(){JpegSessionFence g=ready();g.beginDelivery();g.ended(true,true,true);assertTrue(g.result("attempt","session","nonce",202,9,true,true,true,true,true));assertTrue(g.canPrepare());}
    @Test public void duplicateResultCannotPrepareTwice(){JpegSessionFence g=ready();g.beginDelivery();assertTrue(g.result("attempt","session","nonce",202,9,true,true,true,true,true));assertFalse(g.result("attempt","session","nonce",202,9,true,true,true,true,true));assertEquals(1,g.acceptedResults());}
    @Test public void closedFdWithoutDeathCannotPrepare(){JpegSessionFence g=ready();g.beginDelivery();g.result("attempt","session","nonce",202,9,true,true,true,true,true);assertFalse(g.canPrepare());}
    @Test public void processMissingWithoutBinderDeathCannotPrepare(){JpegSessionFence g=ready();g.beginDelivery();g.result("attempt","session","nonce",202,9,true,true,true,true,true);g.ended(false,true,true);assertFalse(g.canPrepare());}
    @Test public void heldGuardCannotPrepare(){JpegSessionFence g=ready();g.beginDelivery();g.result("attempt","session","nonce",202,9,true,true,true,true,true);g.ended(true,true,false);assertFalse(g.canPrepare());}
    @Test public void missingSourceCloseCannotPrepare(){JpegSessionFence g=ready();g.beginDelivery();g.result("attempt","session","nonce",202,9,true,false,true,true,true);g.ended(true,true,true);assertFalse(g.canPrepare());}
    @Test public void nonSuccessSafeTerminationStillNotPrepare(){JpegSessionFence g=ready();g.beginDelivery();g.result("attempt","session","nonce",202,9,false,true,true,false,true);g.ended(true,true,true);assertFalse(g.canPrepare());assertTrue(g.consumerEnded());}
    @Test public void cancelAfterSuccessBeforeDeathRejectsPrepare(){JpegSessionFence g=ready();g.beginDelivery();g.result("attempt","session","nonce",202,9,true,true,true,true,true);g.fail("CANCELLED");g.ended(true,true,true);assertFalse(g.canPrepare());}
    @Test public void oldFailureCannotAffectNewFence(){JpegSessionFence a=ready(),b=ready();a.fail("TIMEOUT");assertTrue(b.beginDelivery());}
    @Test public void deliveryFailureRaceHasSingleLinearization() throws Exception {for(int n=0;n<100;n++){JpegSessionFence g=ready();Thread a=new Thread(()->g.beginDelivery());Thread b=new Thread(()->g.fail("TIMEOUT"));a.start();b.start();a.join();b.join();assertFalse(g.canPrepare());assertFalse(g.beginDelivery());}}
}
