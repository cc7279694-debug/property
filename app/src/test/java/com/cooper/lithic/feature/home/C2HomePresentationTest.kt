package com.cooper.lithic.feature.home

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class C2HomePresentationTest {
    private val today = LocalDate.of(2026, 10, 26)
    @Test fun failed_empty_preview_is_an_error_and_never_an_empty_ledger() {
        assertEquals(C2PreviewStatus.ERROR, c2PreviewStatus(false, "read failed", emptyList(), today))
    }
    @Test fun retained_rows_are_explicit_when_reading_failed() {
        assertEquals(C2PreviewStatus.RETAINED, c2PreviewStatus(false, "read failed", listOf(today), today))
    }
    @Test fun old_date_rows_cannot_be_presented_as_current() {
        assertEquals(C2PreviewStatus.REFRESHING, c2PreviewStatus(false, null, listOf(today.minusDays(1)), today))
    }
    @Test fun loading_without_facts_does_not_claim_no_records() {
        assertEquals(C2PreviewStatus.LOADING, c2PreviewStatus(true, null, emptyList(), today))
    }
    @Test fun known_zero_rows_and_known_rows_are_distinct() {
        assertEquals(C2PreviewStatus.EMPTY, c2PreviewStatus(false, null, emptyList(), today))
        assertEquals(C2PreviewStatus.READY, c2PreviewStatus(false, null, listOf(today, today, today), today))
    }
}
