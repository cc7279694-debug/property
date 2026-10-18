package com.cooper.lithic.data.media.jpegworker;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Callable;

/** Binding and in-flight RPC responsibility survives timeout. Neither unbind nor RPC return proves consumer death. */
public final class JpegBindingDuty {
    public enum Returned { NOT_CALLED, CALLING, BOUND, FALSE, THREW }
    private Returned returned=Returned.NOT_CALLED;
    private boolean cleanupClaimed, cleaned, rpcClosed;
    private long nextRpcTicket, activeRpcTicket;
    private String cleanupError;
    public synchronized boolean begin() {if(returned!=Returned.NOT_CALLED||cleanupClaimed||cleaned||rpcClosed)return false;returned=Returned.CALLING;return true;}
    public synchronized void returned(boolean bound) {if(returned!=Returned.CALLING)throw new IllegalStateException("bind return order");returned=bound?Returned.BOUND:Returned.FALSE;}
    public synchronized void threw() {if(returned!=Returned.CALLING)throw new IllegalStateException("bind exception order");returned=Returned.THREW;}
    public synchronized boolean takeCleanup() {if(!closeRpcAdmission())return false;if(returned==Returned.NOT_CALLED){cleaned=true;return false;}if(cleanupClaimed||cleaned)return false;cleanupClaimed=true;return true;}
    public synchronized void cleanupSucceeded() {if(!cleanupClaimed)throw new IllegalStateException("cleanup not claimed");cleaned=true;cleanupError=null;}
    public synchronized void cleanupFailed(String error) {if(!cleanupClaimed)throw new IllegalStateException("cleanup not claimed");cleanupError=error==null?"UNBIND_UNKNOWN":error;}
    public synchronized boolean cleaned() {return cleaned;}
    public synchronized Returned outcome() {return returned;}
    public synchronized String cleanupError() {return cleanupError;}
    /** Claim when the queued callable actually begins, never when its Future is submitted. */
    public synchronized long claimRpc() {
        if(rpcClosed||cleanupClaimed||cleaned||returned!=Returned.BOUND||activeRpcTicket!=0)return 0;
        if(nextRpcTicket==Long.MAX_VALUE){rpcClosed=true;return 0;}
        activeRpcTicket=++nextRpcTicket;return activeRpcTicket;
    }
    public synchronized boolean finishRpc(long ticket) {
        if(ticket<=0||ticket!=activeRpcTicket)return false;
        activeRpcTicket=0;return true;
    }
    /** Closing RPC admission is irreversible; only a matching actual return releases an in-flight ticket. */
    public synchronized boolean closeRpcAdmission() {rpcClosed=true;return activeRpcTicket==0;}
    public <T> T callRpc(Callable<T> action) throws Exception {
        Objects.requireNonNull(action);
        long ticket=claimRpc();
        if(ticket==0)throw new IOException("RPC rejected without active binding responsibility");
        Throwable failure=null;
        try {return action.call();}
        catch(Exception|Error error){failure=error;throw error;}
        finally {
            if(!finishRpc(ticket)) {
                IllegalStateException proof=new IllegalStateException("RPC completion identity unknown");
                if(failure!=null)failure.addSuppressed(proof);else throw proof;
            }
        }
    }
}
