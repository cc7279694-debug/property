package com.cooper.lithic.m10r3.probe;

/**
 * Research host's single-attempt authorization gate, not a JPEG or business validator.
 * The caller must acquire UID/PID synchronously from the actual Binder call and retain
 * the matching worker endpoint; this class cannot prove operating-system death itself.
 */
public final class SessionGate {
    private final int protocol;
    private final String nonce;
    private final String attempt;
    private final String generation;
    private final String sha;
    private final long size;
    private final int hostUid;
    private boolean handshaken;
    private boolean revoked;
    private boolean dead;
    private boolean fdClosed;
    private int results;
    private String resultStatus;
    private String revocationReason;

    public SessionGate(int protocol, String nonce, String attempt, String generation, String sha,
            long size, int hostUid) {
        this.protocol = protocol;
        this.nonce = java.util.Objects.requireNonNull(nonce, "nonce");
        this.attempt = java.util.Objects.requireNonNull(attempt, "attempt");
        this.generation = java.util.Objects.requireNonNull(generation, "generation");
        this.sha = java.util.Objects.requireNonNull(sha, "sha");
        this.size = size;
        this.hostUid = hostUid;
    }

    public synchronized boolean handshake(int callingUid, int callingPid, int claimedPid,
            int workerUid, boolean roleReady) {
        if (handshaken || revoked || dead || !roleReady || callingPid <= 0
                || callingPid != claimedPid || callingUid != hostUid || workerUid != hostUid) {
            return false;
        }
        handshaken = true;
        return true;
    }

    /** One decision with the same memory boundary as handshake, result and failure. */
    public synchronized boolean grant() { return handshaken && !revoked && !dead; }

    public synchronized boolean result(int protocol, String nonce, String attempt, String generation,
            String sha, long size, String status, boolean fdClosed) {
        if (!handshaken || revoked || results != 0 || protocol != this.protocol
                || !this.nonce.equals(nonce) || !this.attempt.equals(attempt)
                || !this.generation.equals(generation) || !this.sha.equals(sha)
                || size != this.size || !isKnownStatus(status)) {
            return false;
        }
        // Binder result delivery and the matching endpoint's death may be observed in
        // either order. A death alone never supplies a successful result.
        results = 1;
        resultStatus = status;
        this.fdClosed = fdClosed;
        return true;
    }

    public synchronized void revoke(String reason) {
        if (revoked) return;
        revoked = true;
        revocationReason = reason == null || reason.trim().isEmpty() ? "REVOKED" : reason;
    }

    public synchronized void died() { dead = true; }

    public synchronized boolean canPrepare() {
        return handshaken && !revoked && dead && results == 1 && fdClosed
                && "SUCCESS".equals(resultStatus);
    }

    public synchronized boolean isRevoked() { return revoked; }
    public synchronized boolean isDead() { return dead; }
    public synchronized int acceptedResults() { return results; }

    public synchronized String status() {
        if (revoked) return revocationReason;
        if (results == 1) return resultStatus;
        if (dead) return "UNKNOWN";
        return handshaken ? "RUNNING" : "PENDING";
    }

    private static boolean isKnownStatus(String status) {
        return "SUCCESS".equals(status) || "CANCELLED".equals(status)
                || "TIMEOUT".equals(status) || "UNKNOWN".equals(status)
                || "RESULT_UNKNOWN".equals(status) || "VALIDATION_FAILED".equals(status)
                || "RESOURCE_REJECTED".equals(status) || "LOAD_ERROR".equals(status)
                || "NATIVE_CRASH".equals(status);
    }
}
