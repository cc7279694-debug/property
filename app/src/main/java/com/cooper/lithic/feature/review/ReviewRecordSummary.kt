package com.cooper.lithic.feature.review

/** Presentation of counts supplied by the same Ready snapshot, not a new ledger query or total. */
internal enum class ReviewRecordSummary { EMPTY, RECORDED, SNAPSHOT_NOTICE }

internal fun reviewRecordSummary(projectCount: Long, subscriptionCount: Long,
    hasReadNotice: Boolean): ReviewRecordSummary = when {
    // Ready.readError can be a refresh failure or a recovery warning; do not guess which from its text.
    hasReadNotice -> ReviewRecordSummary.SNAPSHOT_NOTICE
    projectCount == 0L && subscriptionCount == 0L -> ReviewRecordSummary.EMPTY
    else -> ReviewRecordSummary.RECORDED
}
