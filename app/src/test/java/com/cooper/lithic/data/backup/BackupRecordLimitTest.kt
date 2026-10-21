package com.cooper.lithic.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BackupRecordLimitTest {
    @Test
    fun total_below_limit_is_accepted() {
        assertEquals(199999L, requireBackupRecordCounts(listOf(2, 199990, 3, 1, 1, 1, 1, 0)))
    }

    @Test
    fun reaching_the_total_record_limit_is_rejected_before_materializing_rows() {
        expectRejected(listOf(2, 199991, 3, 1, 1, 1, 1, 0))
    }

    @Test
    fun negative_or_missing_table_counts_are_rejected() {
        expectRejected(listOf(2, -1, 0, 0, 0, 0, 0, 0))
        expectRejected(listOf(2, 0, 0, 0, 0, 0))
    }

    @Test
    fun hostile_count_total_cannot_wrap_into_an_accepted_small_count() {
        expectRejected(listOf(2, Long.MAX_VALUE, Long.MAX_VALUE, 0, 0, 0, 0, 0))
    }

    private fun expectRejected(counts: List<Long>) {
        try {
            requireBackupRecordCounts(counts)
            fail("Out-of-range record count must fail before loading backup rows")
        } catch (_: IllegalArgumentException) {
            // Explicit input rejection, never a truncated successful backup.
        } catch (_: ArithmeticException) {
            // Exact addition must not wrap on hostile or corrupt counts.
        }
    }
}
