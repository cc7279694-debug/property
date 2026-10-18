package com.cooper.lithic.data.media.jpegworker;

/** Authenticate retirement while a binding owns the worker; unbind is never a substitute for death proof. */
public final class JpegRetirementGate {
    private JpegRetirementGate() {}
    public static boolean mayRequest(boolean matchedResult,boolean hostClosed,boolean workerClosed,
            boolean connectionCleaned,boolean failureFenced) {
        return matchedResult&&hostClosed&&workerClosed&&!connectionCleaned&&!failureFenced;
    }
    public static boolean mayPrepare(boolean normalRetireRequested,boolean binderDead,
            boolean matchingProcessDead,boolean guardReleased) {
        return normalRetireRequested&&binderDead&&matchingProcessDead&&guardReleased;
    }
    public static boolean acknowledgement(String expectedPhase,String actualPhase,boolean requestMatches,
            boolean workerMatches,boolean accepted) {
        return ("RETIRE_ARMED".equals(expectedPhase)||"REVOKE_ARMED".equals(expectedPhase))&&
                expectedPhase.equals(actualPhase)&&requestMatches&&workerMatches&&accepted;
    }
}
