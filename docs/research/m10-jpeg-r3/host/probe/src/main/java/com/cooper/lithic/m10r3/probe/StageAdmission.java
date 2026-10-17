package com.cooper.lithic.m10r3.probe;

import java.io.IOException;
import java.util.*;

/** Research-only admission orchestration; the Android adapter owns FD/lease/file checks. */
final class StageAdmission {
    interface Lease extends AutoCloseable { @Override void close() throws Exception; }
    interface Backend {
        List<String> attempts() throws Exception;
        String currentAttempt() throws Exception;
        Lease acquire(String id) throws Exception;
        void validate(String id) throws Exception;
        boolean terminal(String id) throws Exception;
        void revoke(String id) throws Exception;
        void removeKnownData(String id) throws Exception;
        void writeTerminal(String id) throws Exception;
        void verifyTerminal(String id) throws Exception;
        void publish(String id) throws Exception;
    }
    static void closeAll(Backend b) throws Exception {
        withAllLeases(b,null,false);
    }
    /** Ordinary run may observe terminal proofs, never manufacture them or clean an old attempt. */
    static void assertAllTerminal(Backend b) throws Exception { withAllLeases(b,null,true); }
    static void publish(Backend b,String id) throws Exception { withAllLeases(b,id,false); }
    private static void withAllLeases(Backend b,String newId,boolean terminalOnly) throws Exception {
        List<String> ids=new ArrayList<>(b.attempts());
        if(new HashSet<>(ids).size()!=ids.size())throw new IOException("duplicate stage");
        Collections.sort(ids);
        String prior=b.currentAttempt();
        if(prior!=null&&!ids.contains(prior))throw new IOException("current outside stage namespace");
        if(newId!=null&&!ids.contains(newId))throw new IOException("new stage absent");
        List<Lease> leases=new ArrayList<>();
        Exception failure=null;
        try {
            // Hold every lease until all stages have passed validation and closure/publication.
            // A check followed by a new acquisition would leave a consumer admission gap.
            for(String id:ids)leases.add(b.acquire(id));
            for(String id:ids)b.validate(id);
            if(terminalOnly) {
                for(String id:ids)b.verifyTerminal(id);
            } else if(newId!=null) {
                for(String id:ids)if(!id.equals(newId))b.verifyTerminal(id);
                if(b.terminal(newId))throw new IOException("cannot republish terminal attempt");
                b.publish(newId);
            } else {
                for(String id:ids) {
                    if(!b.terminal(id)) {
                        b.revoke(id);
                        b.removeKnownData(id);
                        b.writeTerminal(id);
                    }
                    b.verifyTerminal(id);
                }
            }
        } catch(Exception e) { failure=e; throw e; }
        finally {
            Exception closeFailure=null;
            for(int i=leases.size()-1;i>=0;i--)try{leases.get(i).close();}catch(Exception e){if(closeFailure==null)closeFailure=e;else closeFailure.addSuppressed(e);}
            if(closeFailure!=null){if(failure!=null)failure.addSuppressed(closeFailure);else throw closeFailure;}
        }
    }
}
