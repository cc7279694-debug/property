package com.cooper.lithic.data.media.jpegworker;
import org.junit.Test;
import static org.junit.Assert.*;
public class JpegRetirementAcknowledgementTest {
 @Test public void exactNormalAcknowledgementMatches(){assertTrue(JpegRetirementGate.acknowledgement("RETIRE_ARMED","RETIRE_ARMED",true,true,true));}
 @Test public void exactRevokedAcknowledgementMatches(){assertTrue(JpegRetirementGate.acknowledgement("REVOKE_ARMED","REVOKE_ARMED",true,true,true));}
 @Test public void genericAcceptedDoesNotAcknowledgeRetirement(){assertFalse(JpegRetirementGate.acknowledgement("RETIRE_ARMED",null,true,true,true));}
 @Test public void staleRequestAcknowledgementIsRejected(){assertFalse(JpegRetirementGate.acknowledgement("RETIRE_ARMED","RETIRE_ARMED",false,true,true));}
 @Test public void wrongProcessAcknowledgementIsRejected(){assertFalse(JpegRetirementGate.acknowledgement("RETIRE_ARMED","RETIRE_ARMED",true,false,true));}
 @Test public void rejectedCommandNeverAcknowledgesRetirement(){assertFalse(JpegRetirementGate.acknowledgement("RETIRE_ARMED","RETIRE_ARMED",true,true,false));}
 @Test public void cancelledAcknowledgementCannotApproveNormal(){assertFalse(JpegRetirementGate.acknowledgement("RETIRE_ARMED","REVOKE_ARMED",true,true,true));}
 @Test public void unknownMatchingPhaseIsNotAccepted(){assertFalse(JpegRetirementGate.acknowledgement("UNKNOWN","UNKNOWN",true,true,true));}
}
