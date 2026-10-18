package com.cooper.lithic.data.media.jpegworker;

/** Memory-only worker retirement. An authenticated request arms exit; only detach/dead host releases it. */
public final class JpegRetirementHandshake {
    private boolean everBound,bound,detached,armed,normal,hostDead,exitClaimed;
    public synchronized boolean onBound() {
        if(everBound||armed||hostDead||exitClaimed)return false;
        everBound=true;bound=true;return true;
    }
    public synchronized boolean prepareNormal(boolean authenticated,boolean resourcesClosed) {
        if(!authenticated||!resourcesClosed||!bound||detached||hostDead||exitClaimed||(armed&&!normal))return false;
        armed=true;normal=true;return true;
    }
    public synchronized boolean prepareRevoked(boolean authenticated) {
        if(!authenticated||!bound||detached||hostDead||exitClaimed)return false;
        armed=true;normal=false;return true;
    }
    public synchronized boolean onUnbind() {
        if(!bound)return false;
        bound=false;detached=true;return takeExit();
    }
    /** A deadline revokes eligibility, but never kills a still-bound worker into an auto-create loop. */
    public synchronized boolean deadline() {armed=true;normal=false;return takeExit();}
    public synchronized boolean hostDied() {hostDead=true;armed=true;normal=false;return takeExit();}
    private boolean takeExit() {
        if(exitClaimed||!armed||(!detached&&!hostDead))return false;
        exitClaimed=true;return true;
    }
}
