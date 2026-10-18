package com.cooper.lithic.data.media.jpegworker;
import org.junit.Test;
import static org.junit.Assert.*;
public class JpegRetirementOrderingTest {
 @Test public void retirementIsAuthenticatedBeforeUnbind(){assertTrue(JpegRetirementGate.mayRequest(true,true,true,false,false));}
 @Test public void detachedConnectionCannotCarryNewRetirementRequest(){assertFalse(JpegRetirementGate.mayRequest(true,true,true,true,false));}
}
