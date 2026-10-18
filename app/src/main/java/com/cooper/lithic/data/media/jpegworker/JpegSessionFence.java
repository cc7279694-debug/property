package com.cooper.lithic.data.media.jpegworker;

import java.util.Objects;

/** Memory-only authorization fence. No Binder, disk access, waits or external callbacks under this lock. */
public final class JpegSessionFence {
    private final String attempt, session, nonce;
    private final int hostPid, hostUid;
    private int workerPid;
    private long workerStart;
    private boolean hello, delivered, binderDead, processDead, guardReleased;
    private boolean success, sourceClosed, outputClosed, strictFinished, decoderDestroyed;
    private int accepted;
    private String failure;
    public JpegSessionFence(String attempt,String session,String nonce,int hostPid,int hostUid) {
        this.attempt=Objects.requireNonNull(attempt);this.session=Objects.requireNonNull(session);
        this.nonce=Objects.requireNonNull(nonce);this.hostPid=hostPid;this.hostUid=hostUid;
    }
    public synchronized boolean hello(String attempt,String session,String nonce,int uid,int pid,long start) {
        if(hello||failure!=null||binderDead||!matches(attempt,session,nonce)||uid!=hostUid||pid<=0||pid==hostPid||start<=0)return false;
        hello=true;workerPid=pid;workerStart=start;return true;
    }
    public synchronized boolean beginDelivery() {if(!hello||failure!=null||binderDead||delivered)return false;delivered=true;return true;}
    public synchronized boolean isAuthorized() {return hello&&failure==null&&!binderDead;}
    public synchronized void fail(String reason) {if(failure==null)failure=reason==null?"RESULT_UNKNOWN":reason;}
    public synchronized String failure() {return failure;}
    public synchronized boolean result(String attempt,String session,String nonce,int pid,long start,boolean success,
            boolean sourceClosed,boolean outputClosed,boolean strictFinished,boolean decoderDestroyed) {
        // A valid result and the matching Binder death may legally be delivered in either order.
        if(!hello||!delivered||failure!=null||accepted!=0||!matches(attempt,session,nonce)||pid!=workerPid||start!=workerStart)return false;
        accepted=1;this.success=success;this.sourceClosed=sourceClosed;this.outputClosed=outputClosed;
        this.strictFinished=strictFinished;this.decoderDestroyed=decoderDestroyed;return true;
    }
    public synchronized void binderDied() {binderDead=true;}
    public synchronized void ended(boolean binderDead,boolean processDead,boolean guardReleased) {
        this.binderDead|=binderDead;this.processDead|=processDead;this.guardReleased|=guardReleased;
    }
    public synchronized boolean consumerEnded() {return binderDead&&processDead&&guardReleased;}
    public synchronized boolean canPrepare() {return failure==null&&accepted==1&&success&&sourceClosed&&outputClosed&&strictFinished&&decoderDestroyed&&consumerEnded();}
    public synchronized int acceptedResults() {return accepted;}
    private boolean matches(String a,String s,String n) {return attempt.equals(a)&&session.equals(s)&&nonce.equals(n);}
}
