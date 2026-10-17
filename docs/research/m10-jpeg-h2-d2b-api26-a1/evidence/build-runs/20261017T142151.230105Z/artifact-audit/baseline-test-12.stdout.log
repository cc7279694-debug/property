package com.cooper.lithic.m10r3.probe;

import org.junit.Test;
import java.io.IOException;
import java.util.*;
import static org.junit.Assert.*;

/** Synthetic file/lease adapter: proves orchestration, not Android kernel behavior. */
public final class StageAdmissionTest {
    static final class Fake implements StageAdmission.Backend {
        final List<String> ids=new ArrayList<>(Arrays.asList("old"));
        final Set<String> held=new HashSet<>(),done=new HashSet<>(),revoked=new HashSet<>(),removed=new HashSet<>();
        final Map<String,String> invalid=new HashMap<>();
        final List<String> events=new ArrayList<>();
        String current="old",busy,failRemove;
        public List<String> attempts(){events.add("enumerate");return new ArrayList<>(ids);}
        public String currentAttempt(){return current;}
        public StageAdmission.Lease acquire(String id)throws Exception {
            if(id.equals(busy))throw new IOException("consumer lease held");
            assertTrue(held.add(id));events.add("acquire:"+id);
            return ()->{assertTrue(held.remove(id));events.add("release:"+id);};
        }
        void lease(String id){assertTrue("operation escaped the consumer lease",held.contains(id));}
        public void validate(String id)throws Exception {lease(id);events.add("validate:"+id);if(invalid.containsKey(id))throw new IOException(invalid.get(id));}
        public boolean terminal(String id){lease(id);return done.contains(id);}
        public void revoke(String id){lease(id);revoked.add(id);events.add("revoke:"+id);}
        public void removeKnownData(String id)throws Exception {lease(id);assertTrue(revoked.contains(id));events.add("remove:"+id);if(id.equals(failRemove))throw new IOException("delete failed");removed.add(id);}
        public void writeTerminal(String id){lease(id);assertTrue(revoked.contains(id)&&removed.contains(id));done.add(id);events.add("terminal:"+id);}
        public void verifyTerminal(String id)throws Exception {lease(id);if(!done.contains(id))throw new IOException("not terminal");events.add("verified:"+id);}
        public void publish(String id){current=id;events.add("publish:"+id);}
    }
    static void fails(Fake b)throws Exception {try{StageAdmission.closeAll(b);fail("must fail closed");}catch(IOException expected){assertEquals("old",b.current);assertTrue(b.held.isEmpty());}}
    @Test public void completeClosureRevokesAndVerifiesBeforeReleasingTheSameLease()throws Exception {
        Fake b=new Fake();StageAdmission.closeAll(b);
        assertEquals(Arrays.asList("enumerate","acquire:old","validate:old","revoke:old","remove:old","terminal:old","verified:old","release:old"),b.events);
        assertEquals("old",b.current);
    }
    @Test public void occupiedOldConsumerBlocksClosureAndPointerReplacement()throws Exception {Fake b=new Fake();b.busy="old";fails(b);assertTrue(b.revoked.isEmpty());}
    @Test public void corruptOwnerBlocksBeforeAnyRevocation()throws Exception {Fake b=new Fake();b.invalid.put("old","owner mismatch");fails(b);assertTrue(b.revoked.isEmpty());}
    @Test public void corruptReceiptBlocksBeforeAnyRevocation()throws Exception {Fake b=new Fake();b.invalid.put("old","receipt mismatch");fails(b);assertTrue(b.removed.isEmpty());}
    @Test public void wrongInodeBlocksBeforeAnyRemoval()throws Exception {Fake b=new Fake();b.invalid.put("old","inode mismatch");fails(b);assertTrue(b.removed.isEmpty());}
    @Test public void unknownChildBlocksAdmissionAndIsNotRemoved()throws Exception {Fake b=new Fake();b.invalid.put("old","unknown child");fails(b);assertTrue(b.removed.isEmpty());}
    @Test public void cleanupFailureKeepsCurrentAndNoTerminalProof()throws Exception {Fake b=new Fake();b.failRemove="old";fails(b);assertFalse(b.done.contains("old"));assertTrue(b.revoked.contains("old"));}
    @Test public void everyStageIsPreflightedBeforeAnyMutation()throws Exception {
        Fake b=new Fake();b.ids.add("unpointed");b.invalid.put("unpointed","unknown stage facts");fails(b);assertTrue(b.revoked.isEmpty());assertTrue(b.events.contains("validate:unpointed"));
    }
    @Test public void unpointedLiveConsumerBlocksAdmission()throws Exception {Fake b=new Fake();b.ids.add("unpointed");b.busy="unpointed";fails(b);assertTrue(b.revoked.isEmpty());}
    @Test public void terminalRecoveryIsIdempotentAndDoesNotRepeatDeletion()throws Exception {
        Fake b=new Fake();StageAdmission.closeAll(b);int n=Collections.frequency(b.events,"remove:old");StageAdmission.closeAll(b);
        assertEquals(1,n);assertEquals(1,Collections.frequency(b.events,"remove:old"));assertEquals(2,Collections.frequency(b.events,"verified:old"));
    }
    @Test public void currentMustBelongToTheValidatedNamespace()throws Exception {Fake b=new Fake();b.ids.clear();fails(b);assertTrue(b.revoked.isEmpty());}
    @Test public void newPointerRequiresAllOtherStagesToBeTerminal()throws Exception {
        Fake b=new Fake();b.ids.add("new");try{StageAdmission.publish(b,"new");fail("unclosed prior must block");}catch(IOException expected){assertEquals("old",b.current);}assertTrue(b.held.isEmpty());
    }
    @Test public void safelyClosedPriorAllowsExactlyTheRequestedNewPointer()throws Exception {
        Fake b=new Fake();StageAdmission.closeAll(b);b.ids.add("new");StageAdmission.publish(b,"new");assertEquals("new",b.current);assertTrue(b.held.isEmpty());assertTrue(b.done.contains("old"));
    }
    @Test public void unknownStageWithNoCurrentIsStillVisited()throws Exception {
        Fake b=new Fake();b.current=null;StageAdmission.closeAll(b);assertTrue(b.done.contains("old"));assertTrue(b.held.isEmpty());
    }
}
