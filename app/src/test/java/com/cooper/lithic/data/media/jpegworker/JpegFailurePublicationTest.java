package com.cooper.lithic.data.media.jpegworker;
import org.junit.Test;
import static org.junit.Assert.*;
public class JpegFailurePublicationTest {
 @Test public void completedVerifiedRevokeAllowsKnownFailurePublication(){assertTrue(JpegResultWait.mayPublishFailure(true,true));}
 @Test public void pendingRevokeCannotRaceCallerStageMutation(){assertFalse(JpegResultWait.mayPublishFailure(false,false));}
 @Test public void failedRevokePublishesUnknownInstead(){assertFalse(JpegResultWait.mayPublishFailure(true,false));}
 @Test public void successFlagWithoutCompletedActorIsNotEnough(){assertFalse(JpegResultWait.mayPublishFailure(false,true));}
}
