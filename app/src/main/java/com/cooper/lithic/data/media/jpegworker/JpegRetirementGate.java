package com.cooper.lithic.data.media.jpegworker;

/** Normal retirement detaches restart responsibility first; unbind is never a substitute for death proof. */
public final class JpegRetirementGate {
    private JpegRetirementGate() {}
    public static boolean mayRequest(boolean matchedResult,boolean hostClosed,boolean workerClosed,
            boolean connectionCleaned,boolean failureFenced) {
        return matchedResult&&hostClosed&&workerClosed&&connectionCleaned&&!failureFenced;
    }
    public static boolean mayPrepare(boolean normalRetireRequested,boolean binderDead,
            boolean matchingProcessDead,boolean guardReleased) {
        return normalRetireRequested&&binderDead&&matchingProcessDead&&guardReleased;
    }
}
