package com.cooper.lithic.m10r3.probe;

import org.junit.Test;
import java.io.IOException;
import java.util.*;
import static org.junit.Assert.*;

/** Ordinary-run admission must not recover, revoke, delete, or publish any prior attempt. */
public final class StageReadOnlyAdmissionTest {
    static final class Prior implements StageAdmission.Backend {
        final List<String> ids=new ArrayList<>(Arrays.asList("old"));
        final Set<String> held=new HashSet<>(),terminal=new HashSet<>(Arrays.asList("old"));
        final Map<String,List<String>> children=new HashMap<>();
        String current="old",busy,invalid,closeFailure;int mutations,proofs;
        public List<String> attempts(){return new ArrayList<>(ids);}
        public String currentAttempt(){return current;}
        public StageAdmission.Lease acquire(String id)throws Exception {
            if(id.equals(busy))throw new IOException("consumer active");
            assertTrue(held.add(id));
            return ()->{assertTrue(held.remove(id));if(id.equals(closeFailure))throw new IOException("lease close unknown");};
        }
        public void validate(String id)throws Exception {assertTrue(held.containsAll(ids));if(id.equals(invalid))throw new IOException("owner/receipt/inode/unknown child rejected");StageAuthorization.assertNoPending(children.getOrDefault(id,Collections.emptyList()));}
        public boolean terminal(String id){assertTrue(held.containsAll(ids));return terminal.contains(id);}
        public void revoke(String id){mutations++;}
        public void removeKnownData(String id){mutations++;}
        public void writeTerminal(String id){mutations++;}
        public void verifyTerminal(String id)throws Exception {assertTrue("every lease must remain held",held.containsAll(ids));if(!terminal.contains(id))throw new IOException("prior is not proven terminal");proofs++;}
        public void publish(String id){mutations++;current=id;}
    }
    static void rejected(Prior b)throws Exception {
        try{StageAdmission.assertAllTerminal(b);fail("unsafe prior must block ordinary run");}catch(IOException expected){assertEquals(0,b.mutations);assertTrue(b.held.isEmpty());}
    }
    @Test public void confirmedTerminalPriorAllowsReadOnlyAdmission()throws Exception {Prior b=new Prior();StageAdmission.assertAllTerminal(b);assertEquals(1,b.proofs);assertEquals(0,b.mutations);assertEquals("old",b.current);assertTrue(b.held.isEmpty());}
    @Test public void pinnedNonterminalPriorIsNeverAutomaticallyClosed()throws Exception {Prior b=new Prior();b.terminal.clear();rejected(b);assertEquals("old",b.current);}
    @Test public void terminalProofWithPendingMetadataStillBlocksOrdinaryRun()throws Exception {Prior b=new Prior();b.children.put("old",Arrays.asList("cleanup.json","revoked.json.pending"));rejected(b);assertEquals(0,b.proofs);assertEquals("old",b.current);}
    @Test public void unpointedNonterminalStageBlocksWithoutMutation()throws Exception {Prior b=new Prior();b.ids.add("unpointed");rejected(b);}
    @Test public void occupiedConsumerLeaseBlocksAdmission()throws Exception {Prior b=new Prior();b.busy="old";rejected(b);assertEquals(0,b.proofs);}
    @Test public void corruptOwnerReceiptInodeOrUnknownChildBlocks()throws Exception {Prior b=new Prior();b.invalid="old";rejected(b);assertEquals(0,b.proofs);}
    @Test public void allLeasesStayHeldThroughEveryTerminalProof()throws Exception {Prior b=new Prior();b.ids.add("another");b.terminal.add("another");StageAdmission.assertAllTerminal(b);assertEquals(2,b.proofs);assertEquals(0,b.mutations);assertTrue(b.held.isEmpty());}
    @Test public void laterBusyLeaseReleasesPreviouslyAcquiredLease()throws Exception {Prior b=new Prior();b.ids.add("z-busy");b.busy="z-busy";rejected(b);}
    @Test public void currentOutsideNamespaceBlocks()throws Exception {Prior b=new Prior();b.current="missing";rejected(b);}
    @Test public void duplicateNamespaceIdentityBlocks()throws Exception {Prior b=new Prior();b.ids.add("old");rejected(b);}
    @Test public void noStagesAllowsFreshRunWithoutPublishing()throws Exception {Prior b=new Prior();b.ids.clear();b.current=null;StageAdmission.assertAllTerminal(b);assertEquals(0,b.proofs);assertEquals(0,b.mutations);}
    @Test public void terminalAdmissionCanRepeatWithoutCleanupOrPointerChange()throws Exception {Prior b=new Prior();StageAdmission.assertAllTerminal(b);StageAdmission.assertAllTerminal(b);assertEquals(2,b.proofs);assertEquals(0,b.mutations);assertEquals("old",b.current);}
    @Test public void leaseReleaseUnknownDoesNotBecomeSuccessfulAdmission()throws Exception {Prior b=new Prior();b.closeFailure="old";try{StageAdmission.assertAllTerminal(b);fail("lease close unknown must propagate");}catch(IOException expected){assertEquals("lease close unknown",expected.getMessage());assertEquals(0,b.mutations);}}
}
