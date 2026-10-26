package com.cooper.lithic.feature.home

import com.cooper.lithic.domain.report.OverviewResult
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class R1HomeEmptyStateTest {
    @Test fun only_verified_empty_snapshot_uses_the_single_onboarding_section() {
        assertTrue(c2HomeIsEmpty(snapshot(), false))
        assertFalse(c2HomeIsEmpty(null, false))
        assertFalse(c2HomeIsEmpty(snapshot(), true))
    }
    @Test fun historical_records_without_current_contribution_do_not_turn_into_empty_state() {
        assertFalse(c2HomeIsEmpty(snapshot(projects = 2), false))
    }
    @Test fun active_zero_and_negative_records_preserve_their_numeric_values() {
        val zero = snapshot(projects = 1, active = 1)
        val negative = zero.copy(totalRate = BigDecimal("-123456789.01"))
        assertFalse(c2HomeIsEmpty(zero, false))
        assertFalse(c2HomeIsEmpty(negative, false))
        assertEquals(BigDecimal("-123456789.01"), negative.totalRate)
    }
    private fun snapshot(projects: Long = 0, active: Long = 0) = OverviewResult(
        LocalDate.of(2026, 10, 26), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
        projects, 0, active, 0, emptyList(), emptyList())
}
