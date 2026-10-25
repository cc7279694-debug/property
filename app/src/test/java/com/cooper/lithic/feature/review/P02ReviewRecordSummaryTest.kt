package com.cooper.lithic.feature.review

import org.junit.Assert.assertEquals
import org.junit.Test

class P02ReviewRecordSummaryTest {
    @Test fun both_types_empty_without_notice_is_a_verified_empty_snapshot() {
        assertEquals(ReviewRecordSummary.EMPTY, reviewRecordSummary(0, 0, false))
    }

    @Test fun subscription_only_snapshot_is_not_an_empty_ledger() {
        assertEquals(ReviewRecordSummary.RECORDED, reviewRecordSummary(0, 3, false))
    }

    @Test fun project_only_snapshot_is_not_an_empty_ledger() {
        assertEquals(ReviewRecordSummary.RECORDED, reviewRecordSummary(2, 0, false))
    }

    @Test fun both_types_have_records_without_combining_their_costs() {
        assertEquals(ReviewRecordSummary.RECORDED, reviewRecordSummary(2, 3, false))
    }

    @Test fun zero_counts_with_read_or_recovery_notice_do_not_assert_current_emptiness() {
        assertEquals(ReviewRecordSummary.SNAPSHOT_NOTICE, reviewRecordSummary(0, 0, true))
    }

    @Test fun positive_counts_with_read_or_recovery_notice_stay_snapshot_qualified() {
        assertEquals(ReviewRecordSummary.SNAPSHOT_NOTICE, reviewRecordSummary(2, 3, true))
    }

    @Test fun presentation_does_not_sum_counts_and_overflow() {
        assertEquals(ReviewRecordSummary.RECORDED,
            reviewRecordSummary(Long.MAX_VALUE, Long.MAX_VALUE, false))
    }
}
