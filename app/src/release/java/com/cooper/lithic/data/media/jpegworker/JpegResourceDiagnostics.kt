package com.cooper.lithic.data.media.jpegworker

import android.content.Context

/** Release has no diagnostics parser, file access, observer, permit or sampling capability. */
internal object JpegResourceDiagnostics {
    private val inactive = Session()
    @Suppress("UNUSED_PARAMETER") fun open(context: Context, request: JpegWorkerRequest): Session = inactive
    class Session {
        @Suppress("UNUSED_PARAMETER") fun nativeComplete(values: LongArray) = Unit
        fun normalizationStarted() = Unit
        fun normalizationFinished() = Unit
        @Suppress("UNUSED_PARAMETER") fun finish(outcome: String, sourceClosed: Boolean, outputClosed: Boolean, scratchClean: Boolean) = Unit
        fun retiring() = Unit
    }
}
