package com.cooper.lithic.data.media;

/** Test-only observation arbitration. It creates no Android/FD/Stage or recovery evidence.
 * The real adapter supplies the exact returned facts; only one complete closure may be claimed.
 */
public final class M10B1ClosureObservations {
    public enum Origin { INITIAL, LATE_ORIGINAL_QUIESCENCE }
    public enum Decision { WAIT, CLOSE_ONCE }
    public static final class Observation {
        final Origin origin; final String outcome, worker; final int resultCount;
        final boolean sourceClosed, outputClosed, hostFdsClosed, rpcQuiescent, originalSessionQuiescent;
        final boolean binderDead, matchingProcessDead, guardReleased, connectionCleaned;
        public Observation(Origin origin, String outcome, String worker, int resultCount,
                boolean sourceClosed, boolean outputClosed, boolean hostFdsClosed, boolean rpcQuiescent,
                boolean originalSessionQuiescent, boolean binderDead, boolean matchingProcessDead,
                boolean guardReleased, boolean connectionCleaned) {
            this.origin=origin;this.outcome=outcome;this.worker=worker;this.resultCount=resultCount;
            this.sourceClosed=sourceClosed;this.outputClosed=outputClosed;this.hostFdsClosed=hostFdsClosed;
            this.rpcQuiescent=rpcQuiescent;this.originalSessionQuiescent=originalSessionQuiescent;
            this.binderDead=binderDead;this.matchingProcessDead=matchingProcessDead;
            this.guardReleased=guardReleased;this.connectionCleaned=connectionCleaned;
        }
    }
    private String expectedWorker;
    private int seen, count;
    private boolean claimed, stopped;
    public synchronized void registerWorker(String worker) {
        require(!stopped && expectedWorker==null && worker!=null && worker.matches("[1-9][0-9]*/[1-9][0-9]*/[1-9][0-9]*"), "Worker registration conflict");
        expectedWorker=worker;
    }
    public synchronized Decision observe(Observation value) {
        require(!stopped,"Observation window ended");
        count++;
        require(count<=2 && expectedWorker!=null && value!=null && value.origin!=null,"Observation bound or identity is unknown");
        int bit=value.origin==Origin.INITIAL?1:2;
        require((seen&bit)==0,"Duplicate observation entry");seen|=bit;
        require("CANCELLED".equals(value.outcome)||"RESULT_UNKNOWN".equals(value.outcome),"Unexpected operation outcome");
        require(value.worker==null||expectedWorker.equals(value.worker),"Worker instance conflict");
        require(value.resultCount==0 && !value.sourceClosed && !value.outputClosed,"A pre-delivery cancellation cannot invent a Worker RESULT or data-FD closure");
        boolean full="CANCELLED".equals(value.outcome) && expectedWorker.equals(value.worker) &&
            value.hostFdsClosed && value.rpcQuiescent && value.originalSessionQuiescent &&
            value.binderDead && value.matchingProcessDead && value.guardReleased && value.connectionCleaned;
        require(!value.originalSessionQuiescent||full,"Claimed quiescence omits actual termination facts");
        require(value.origin!=Origin.LATE_ORIGINAL_QUIESCENCE||full,"Late closure is incomplete or unknown");
        if(!full)return Decision.WAIT;
        require(!claimed,"Multiple complete closure claims");claimed=true;
        return Decision.CLOSE_ONCE;
    }
    public synchronized int observations(){return count;}
    public synchronized boolean closureClaimed(){return claimed;}
    public synchronized void stop(){stopped=true;}
    private void require(boolean condition,String reason){if(!condition){stopped=true;throw new IllegalStateException(reason);}}
}
