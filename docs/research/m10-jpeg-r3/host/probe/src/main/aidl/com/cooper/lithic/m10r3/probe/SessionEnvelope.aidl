package com.cooper.lithic.m10r3.probe;
parcelable SessionEnvelope {
    int protocolVersion;
    String sessionNonce;
    String attemptId;
    String generationIdentity;
    long sourceSize;
    String sourceSHA256;
    long sourceDevice;
    long sourceInode;
    long guardDevice;
    long guardInode;
    String consumerLeaseName;
    long outputDevice;
    long outputInode;
    String requestStatus;
    String workerResult;
    String resourceCloseStatus;
    boolean fdClosed;
    String kind;
    int workerPid;
    int workerUid;
    boolean ownerInitialized;
    int durationMs;
    String scenario;
    boolean delayHostResult;
}
