package com.cooper.lithic.m10r3.probe;

import org.junit.Test;
import java.io.IOException;
import java.util.Arrays;
import static org.junit.Assert.*;

/** Tests the exact pure authorization/revoke implementation called by StageStore; no Android I/O claim. */
public final class StageAuthorizationTest {
    private boolean allowed(String state,boolean pin,boolean pending) {
        return StageAuthorization.isAuthorized(true,true,state,pin,false,false,pending);
    }
    @Test public void deliveryUnknownWithVerifiedPinCanAuthorize() {assertTrue(allowed("DELIVERY_UNKNOWN",true,false));}
    @Test public void fdSentWithVerifiedPinCanAuthorize() {assertTrue(allowed("FD_SENT",true,false));}
    @Test public void revokedPinWithoutRevokeFileCannotAuthorize() {assertFalse(allowed("REVOKED",true,false));}
    @Test public void cleanedStateCannotAuthorizeEvenWithPin() {assertFalse(allowed("CLEANED_SYNTHETIC",true,false));}
    @Test public void unknownStateCannotAuthorize() {assertFalse(allowed("UNKNOWN",true,false));}
    @Test public void missingStatusCannotAuthorize() {assertFalse(allowed(null,true,false));}
    @Test public void pendingMetadataCannotAuthorize() {assertFalse(allowed("FD_SENT",true,true));}
    @Test public void missingPinCannotAuthorize() {assertFalse(allowed("FD_SENT",false,false));}
    @Test public void noncurrentAttemptCannotAuthorize() {assertFalse(StageAuthorization.isAuthorized(false,true,"FD_SENT",true,false,false,false));}
    @Test public void missingStateCannotAuthorize() {assertFalse(StageAuthorization.isAuthorized(true,false,"FD_SENT",true,false,false,false));}
    @Test public void terminalProofCannotAuthorize() {assertFalse(StageAuthorization.isAuthorized(true,true,"FD_SENT",true,true,false,false));}
    @Test public void durableRevokeCannotAuthorize() {assertFalse(StageAuthorization.isAuthorized(true,true,"FD_SENT",true,false,true,false));}
    @Test public void completedMetadataNamespaceHasNoPending()throws Exception {StageAuthorization.assertNoPending(Arrays.asList("owner.json","state.json","cleanup.json","revoked.json"));}
    @Test public void everyPendingMetadataKindBlocksOrdinaryRun()throws Exception {
        for(String name:new String[]{"owner.json.pending","state.json.pending","receipt.json.pending","revoked.json.pending","output-owner.json.pending","cleanup.json.pending","unknown.pending"}){
            try{StageAuthorization.assertNoPending(Arrays.asList("cleanup.json",name));fail("pending file accepted: "+name);}
            catch(IOException expected){assertEquals("pending metadata blocks ordinary run",expected.getMessage());}
        }
    }

    static final class Durable implements StageAuthorization.Revocation {
        boolean stored,dropWrite,failWrite,failReadback;int reads,writes;
        public boolean sameOwnerRevoked()throws IOException {reads++;if(failReadback&&reads>1)throw new IOException("readback unknown");return stored;}
        public void writeSameOwnerRevoke()throws IOException {writes++;if(failWrite)throw new IOException("sync failed");if(!dropWrite)stored=true;}
    }
    @Test public void successfulRevokeIsReadBack()throws Exception {Durable d=new Durable();StageAuthorization.revokeVerified(d);assertTrue(d.stored);assertEquals(1,d.writes);assertEquals(2,d.reads);}
    @Test public void repeatedSameOwnerRevokeIsIdempotentAndReadBack()throws Exception {Durable d=new Durable();d.stored=true;StageAuthorization.revokeVerified(d);StageAuthorization.revokeVerified(d);assertEquals(0,d.writes);assertEquals(4,d.reads);}
    @Test public void lostRevokeWriteIsNotSuccessful()throws Exception {Durable d=new Durable();d.dropWrite=true;try{StageAuthorization.revokeVerified(d);fail("missing revoke readback must fail");}catch(IOException expected){assertFalse(d.stored);}}
    @Test public void unknownRevokeReadBackPropagates()throws Exception {Durable d=new Durable();d.failReadback=true;try{StageAuthorization.revokeVerified(d);fail("unknown durability must fail");}catch(IOException expected){assertEquals("readback unknown",expected.getMessage());}}
    @Test public void revokeWriteFailureIsNotMasked()throws Exception {Durable d=new Durable();d.failWrite=true;try{StageAuthorization.revokeVerified(d);fail("sync failure must propagate");}catch(IOException expected){assertEquals("sync failed",expected.getMessage());assertEquals(1,d.reads);}}
    @Test public void wrongOwnerReadFailureNeverWrites()throws Exception {
        final int[] writes={0};
        try{StageAuthorization.revokeVerified(new StageAuthorization.Revocation(){public boolean sameOwnerRevoked()throws Exception{throw new IOException("owner mismatch");}public void writeSameOwnerRevoke(){writes[0]++;}});fail("wrong owner must fail");}
        catch(IOException expected){assertEquals("owner mismatch",expected.getMessage());assertEquals(0,writes[0]);}
    }
}
