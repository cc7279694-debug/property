package com.cooper.lithic.m10r3.probe;
/** RED witness for old Activity/Runner synchronous bootstrap and claim scheduling only.
 * Not the old production source, nor a claim of Android behavior. */
public final class HostCommandQueue implements AutoCloseable {
    public interface Initializer { void initialize() throws Exception; }
    public interface Claim { boolean claim() throws Exception; }
    public interface Task { void run() throws Exception; }
    public interface Observer { void onComplete(String c, String r, Outcome o, Throwable e) throws Exception; }
    public enum Readiness { BOOTSTRAPPING, READY, UNKNOWN, CLOSED }
    public enum Submission { ACCEPTED, INVALID, QUEUE_FULL, DUPLICATE, INITIALIZATION_UNKNOWN, CLOSED }
    public enum Outcome { EXECUTED, CLAIM_REPLAY, CLAIM_UNKNOWN, TASK_FAILED, INITIALIZATION_UNKNOWN }
    private Readiness ready=Readiness.BOOTSTRAPPING;
    private Throwable error, observerError;
    private long observerFailures;
    private final Observer observer;
    public HostCommandQueue(int capacity,Initializer init){this(capacity,init,null);}
    public HostCommandQueue(int capacity,Initializer init,Observer observer){
        if(capacity<=0)throw new IllegalArgumentException("capacity");
        java.util.Objects.requireNonNull(init,"init");this.observer=observer;
        try{init.initialize();ready=Readiness.READY;}catch(Throwable e){error=e;ready=Readiness.UNKNOWN;}
    }
    public Readiness readiness(){return ready;}
    public Throwable initializationError(){return error;}
    public Throwable observerError(){return observerError;}
    public long observerFailures(){return observerFailures;}
    public Submission submit(String c,String r,Claim claim,Task task){
        if(ready==Readiness.CLOSED)return Submission.CLOSED;
        if(ready==Readiness.UNKNOWN)return Submission.INITIALIZATION_UNKNOWN;
        Outcome o=Outcome.EXECUTED;Throwable failure=null;
        try{if(!claim.claim())o=Outcome.CLAIM_REPLAY;else task.run();}catch(Throwable e){o=Outcome.TASK_FAILED;failure=e;}
        if(observer!=null)try{observer.onComplete(c,r,o,failure);}catch(Throwable e){observerError=e;observerFailures++;}
        return Submission.ACCEPTED;
    }
    public void close(){ready=Readiness.CLOSED;}
}
