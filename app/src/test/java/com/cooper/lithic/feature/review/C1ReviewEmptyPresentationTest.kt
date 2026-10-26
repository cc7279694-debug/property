package com.cooper.lithic.feature.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Presentation gates only; the existing Ready snapshot remains the source of record counts. */
class C1ReviewEmptyPresentationTest {
    @Test fun verified_zero_counts_allow_one_welcoming_empty_region() {
        assertTrue(reviewShowsEmptyLedger(0, 0, false))
    }

    @Test fun history_in_either_type_prevents_an_empty_ledger_even_when_current_cost_is_zero() {
        assertFalse(reviewShowsEmptyLedger(1, 0, false))
        assertFalse(reviewShowsEmptyLedger(0, 1, false))
    }

    @Test fun read_or_restore_notice_keeps_zero_snapshot_qualified() {
        assertFalse(reviewShowsEmptyLedger(0, 0, true))
    }

    @Test fun loading_must_not_turn_into_an_empty_result() {
        assertFalse(reviewShowsEmptyLedger(0, 0, false, trendLoading = true))
    }

    @Test fun failed_or_notice_bearing_trend_must_not_be_hidden_by_zero_ready_counts() {
        assertFalse(reviewShowsEmptyLedger(0, 0, false, trendHasNotice = true))
    }

    @Test fun retained_or_conflicting_facts_are_preserved_instead_of_replaced_by_empty() {
        assertFalse(reviewShowsEmptyLedger(0, 0, false, trendHasRecords = true))
    }
}
