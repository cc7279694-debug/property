package com.cooper.lithic.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PhotoStorageSummaryTest {
    @Test fun empty_metadata_is_an_exact_empty_summary() {
        assertEquals(PhotoStorageSummary(0, 0, 0, 0, 0), photoStorageSummary(0, 0, 0, estimatedHistoricalBytes = 0))
    }

    @Test fun referenced_count_is_already_distinct_across_both_business_types() {
        assertEquals(PhotoStorageSummary(3, 1, 2, 600, 500), photoStorageSummary(3, 1, 600, estimatedHistoricalBytes = 500))
    }

    @Test fun replacing_adds_registered_history_without_claiming_a_disk_saving() {
        val before = photoStorageSummary(1, 1, 100, estimatedHistoricalBytes = 0)
        val after = photoStorageSummary(2, 1, 350, estimatedHistoricalBytes = 100)
        assertEquals(0L, before.historicalCount)
        assertEquals(PhotoStorageSummary(2, 1, 1, 350, 100), after)
    }

    @Test fun cancelling_display_retains_all_registered_bytes_as_history() {
        assertEquals(PhotoStorageSummary(2, 0, 2, 350, 350), photoStorageSummary(2, 0, 350, estimatedHistoricalBytes = 350))
    }

    @Test fun zero_byte_length_estimate_is_permitted_by_stored_metadata_contract() {
        assertEquals(PhotoStorageSummary(1, 1, 0, 0, 0), photoStorageSummary(1, 1, 0, estimatedHistoricalBytes = 0))
    }

    @Test fun missing_reference_cannot_be_presented_as_a_successful_count() {
        assertThrows(IllegalArgumentException::class.java) { photoStorageSummary(2, 1, 100, missingReferences = 1, estimatedHistoricalBytes = 50) }
    }

    @Test fun negative_counts_and_more_references_than_media_are_rejected() {
        for ((registered, referenced) in listOf(-1L to 0L, 1L to -1L, 1L to 2L)) {
            assertThrows(IllegalArgumentException::class.java) { photoStorageSummary(registered, referenced, 100, estimatedHistoricalBytes = 0) }
        }
    }

    @Test fun negative_or_wrapped_byte_estimates_are_rejected_without_clamping_to_zero() {
        for (bytes in listOf(-1L, Long.MIN_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { photoStorageSummary(1, 1, bytes, estimatedHistoricalBytes = 0) }
        }
    }

    @Test fun malformed_length_or_negative_diagnostic_counts_are_rejected() {
        assertThrows(IllegalArgumentException::class.java) { photoStorageSummary(1, 0, 100, invalidLengths = 1, estimatedHistoricalBytes = 100) }
        assertThrows(IllegalArgumentException::class.java) { photoStorageSummary(1, 0, 100, missingReferences = -1, estimatedHistoricalBytes = 100) }
        assertThrows(IllegalArgumentException::class.java) { photoStorageSummary(1, 0, 100, invalidLengths = -1, estimatedHistoricalBytes = 100) }
    }

    @Test fun integer_boundary_remains_exact_and_is_never_rounded_through_double() {
        val summary = photoStorageSummary(Long.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE, estimatedHistoricalBytes = Long.MAX_VALUE - 1)
        assertEquals(1L, summary.historicalCount)
        assertEquals(Long.MAX_VALUE, summary.estimatedRegisteredBytes)
        assertEquals(Long.MAX_VALUE - 1, summary.estimatedHistoricalBytes)
    }

    @Test fun historical_bytes_are_exact_and_are_not_an_average_of_all_photos() {
        val summary = photoStorageSummary(3, 2, 1000, estimatedHistoricalBytes = 17)
        assertEquals(1L, summary.historicalCount)
        assertEquals(17L, summary.estimatedHistoricalBytes)
    }

    @Test fun historical_bytes_must_not_be_negative_wrapped_or_larger_than_registered_total() {
        for (invalid in listOf(-1L, Long.MIN_VALUE, 101L)) {
            assertThrows(IllegalArgumentException::class.java) {
                photoStorageSummary(2, 1, 100, estimatedHistoricalBytes = invalid)
            }
        }
    }

    @Test fun all_referenced_photos_have_no_historical_bytes() {
        assertEquals(0L, photoStorageSummary(2, 2, 100, estimatedHistoricalBytes = 0).estimatedHistoricalBytes)
        assertThrows(IllegalArgumentException::class.java) {
            photoStorageSummary(2, 2, 100, estimatedHistoricalBytes = 1)
        }
    }

    @Test fun when_no_photo_is_referenced_all_registered_bytes_are_historical() {
        assertEquals(100L, photoStorageSummary(2, 0, 100, estimatedHistoricalBytes = 100).estimatedHistoricalBytes)
        assertThrows(IllegalArgumentException::class.java) {
            photoStorageSummary(2, 0, 100, estimatedHistoricalBytes = 99)
        }
    }
}
