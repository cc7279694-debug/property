package com.cooper.lithic.data.media.jpegworker;

/** Production wait decision uses monotonic observations. It never infers a native crash or resource release. */
public final class JpegResultWait {
    public enum Decision { WAIT, RESULT_READY, DEAD_WITHOUT_RESULT, TIMEOUT, CLOCK_UNKNOWN }
    private final long deadlineMs, orderingGraceMs;
    private long firstDeathMs=-1, lastNow=-1;
    private Decision terminal;
    public JpegResultWait(long deadlineMs,long orderingGraceMs) {
        if(deadlineMs<0||orderingGraceMs<0)throw new IllegalArgumentException("Invalid wait bounds");
        this.deadlineMs=deadlineMs;this.orderingGraceMs=orderingGraceMs;
    }
    public synchronized Decision observe(long nowMs,boolean resultAvailable,boolean binderDead) {
        if(terminal!=null)return terminal;
        if(nowMs<0||nowMs<lastNow)return terminal=Decision.CLOCK_UNKNOWN;
        lastNow=nowMs;
        if(resultAvailable)return terminal=nowMs<deadlineMs?Decision.RESULT_READY:Decision.TIMEOUT;
        if(binderDead) {
            if(firstDeathMs<0)firstDeathMs=nowMs;
            if(nowMs>=deadlineMs||nowMs-firstDeathMs>=orderingGraceMs)return terminal=Decision.DEAD_WITHOUT_RESULT;
        } else if(nowMs>=deadlineMs)return terminal=Decision.TIMEOUT;
        return Decision.WAIT;
    }
    /** Connection registration can restart a service; consumer death alone cannot release admission. */
    public static boolean mayReleaseAdmission(boolean consumerEnded,boolean connectionCleaned) {
        return consumerEnded&&connectionCleaned;
    }
}
