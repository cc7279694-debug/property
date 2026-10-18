package com.cooper.lithic.data.media.jpegworker;

/** Main-thread binding responsibility survives rejection and exceptions. It is not consumer-death evidence. */
public final class JpegBindingDuty {
    public enum Returned { NOT_CALLED, CALLING, BOUND, FALSE, THREW }
    private Returned returned=Returned.NOT_CALLED;
    private boolean cleanupClaimed, cleaned;
    private String cleanupError;
    public synchronized boolean begin() {if(returned!=Returned.NOT_CALLED||cleanupClaimed||cleaned)return false;returned=Returned.CALLING;return true;}
    public synchronized void returned(boolean bound) {if(returned!=Returned.CALLING)throw new IllegalStateException("bind return order");returned=bound?Returned.BOUND:Returned.FALSE;}
    public synchronized void threw() {if(returned!=Returned.CALLING)throw new IllegalStateException("bind exception order");returned=Returned.THREW;}
    public synchronized boolean takeCleanup() {if(returned==Returned.NOT_CALLED){cleaned=true;return false;}if(cleanupClaimed||cleaned)return false;cleanupClaimed=true;return true;}
    public synchronized void cleanupSucceeded() {if(!cleanupClaimed)throw new IllegalStateException("cleanup not claimed");cleaned=true;cleanupError=null;}
    public synchronized void cleanupFailed(String error) {if(!cleanupClaimed)throw new IllegalStateException("cleanup not claimed");cleanupError=error==null?"UNBIND_UNKNOWN":error;}
    public synchronized boolean cleaned() {return cleaned;}
    public synchronized Returned outcome() {return returned;}
    public synchronized String cleanupError() {return cleanupError;}
}
