package com.cooper.lithic.m10r3.probe;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Real pure identity comparisons; does not simulate Android lstat or filesystem durability. */
public final class StageIdentityTest {
    Map<String,Object> identity(){Map<String,Object> i=new HashMap<>();i.put("attemptId","old");i.put("sessionNonce","nonce-1");i.put("generationIdentity","generation-1");i.put("sourceInode",17L);return i;}
    Set<String> extra(){return Collections.singleton("revoked");}
    Map<String,Object> record(){Map<String,Object> r=identity();r.put("revoked",true);return r;}
    @Test public void exactFullIdentityIsAccepted(){assertTrue(StageIdentity.sameRecord(identity(),record(),extra()));}
    @Test public void oldAttemptCannotMatchNewAttempt(){Map<String,Object> r=record();r.put("attemptId","new");assertFalse(StageIdentity.sameRecord(identity(),r,extra()));}
    @Test public void nonceMismatchRejectsLateIdentity(){Map<String,Object> r=record();r.put("sessionNonce","late-nonce");assertFalse(StageIdentity.sameRecord(identity(),r,extra()));}
    @Test public void wrongGenerationIsRejected(){Map<String,Object> r=record();r.put("generationIdentity","generation-2");assertFalse(StageIdentity.sameRecord(identity(),r,extra()));}
    @Test public void missingIdentityFieldIsRejected(){Map<String,Object> r=record();r.remove("sourceInode");assertFalse(StageIdentity.sameRecord(identity(),r,extra()));}
    @Test public void unknownRecordFieldIsRejected(){Map<String,Object> r=record();r.put("unknown",true);assertFalse(StageIdentity.sameRecord(identity(),r,extra()));}
    @Test public void numericStringDoesNotMasqueradeAsAnInode(){Map<String,Object> r=record();r.put("sourceInode","17");assertFalse(StageIdentity.sameRecord(identity(),r,extra()));}
    @Test public void missingProofFieldIsRejected(){Map<String,Object> r=record();r.remove("revoked");assertFalse(StageIdentity.sameRecord(identity(),r,extra()));}
    @Test public void matchingRegularInodeIsAccepted(){assertTrue(StageIdentity.sameInode(true,2,17,2,17));}
    @Test public void wrongSourceInodeIsRejected(){assertFalse(StageIdentity.sameInode(true,2,17,2,18));}
    @Test public void wrongDeviceIsRejected(){assertFalse(StageIdentity.sameInode(true,2,17,3,17));}
    @Test public void nonRegularResourceIsRejected(){assertFalse(StageIdentity.sameInode(false,2,17,2,17));}
}
