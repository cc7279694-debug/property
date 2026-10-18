package com.cooper.lithic.research;

/** Compile-only declaration; this class never loads or calls a native library. */
public final class StrictJpegCandidate {
    private StrictJpegCandidate() {}

    /**
     * Synchronous research computation. The caller retains its read-only regular
     * FD and must keep it and its file stable until this method returns. Native
     * code duplicates the FD and releases only that duplicate. This has no
     * cancellation, process-lifecycle, stage, normalization or commit authority.
     *
     * Result long[16]: status, error, peak, live, blocks, badFree, fdClosed,
     * memNull, rows, height, finish, wallNs, cpuNs, rssKiB, createAttempted,
     * destroyCalled. Status: 0 ACCEPT, 10 IMAGE, 20 RESOURCE, 40 INFRA.
     * Unavailable/unrepresentable time or RSS metrics are -1. Charged request
     * bytes and process high-water RSS are different measurements. A rejected
     * argument may report fdClosed=true because no duplicate was acquired.
     * A nonpositive/non-size_t limit is rejected without a default fallback.
     * Pending JNI exceptions and allocation failures return no success array.
     */
    public static native long[] validateReadonlyFd(int fd, long limit);
}
