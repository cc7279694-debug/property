package com.cooper.lithic.feature.review

import com.cooper.lithic.domain.report.CostTimelinePoint
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class C2ReviewSelectionLabelTest {
    private val point = CostTimelinePoint(LocalDate.of(2026, 10, 26), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)

    @Test fun records_not_yet_effective_and_historical_records_without_current_contribution_are_distinct() {
        assertTrue(reviewSelectionLabel(point).contains("尚无"))
        assertTrue(reviewSelectionLabel(point.copy(projectCount = 1)).contains("历史记录仍保留"))
        assertNotEquals(reviewSelectionLabel(point), reviewSelectionLabel(point.copy(projectCount = 1)))
    }

    @Test fun actual_zero_with_an_active_record_is_not_empty_or_inactive() {
        val zero = point.copy(projectCount = 1, activeProjectCount = 1)
        assertTrue(reviewSelectionLabel(zero).contains("真实日均成本为0"))
        assertNotEquals(reviewSelectionLabel(zero), reviewSelectionLabel(point.copy(projectCount = 1)))
    }

    @Test fun offsetting_contributions_stay_distinct_from_a_zero_cost_record() {
        val offset = point.copy(projectCount = 1, subscriptionCount = 1, activeProjectCount = 1,
            activeSubscriptionCount = 1, projectRate = BigDecimal.TEN.negate(), subscriptionRate = BigDecimal.TEN,
            hasPositiveContribution = true, hasNegativeContribution = true)
        assertTrue(reviewSelectionLabel(offset).contains("正负贡献相抵"))
    }

    @Test fun negative_value_is_not_reclassified_or_changed_by_presentation() {
        val negative = point.copy(projectCount = 1, activeProjectCount = 1,
            projectRate = BigDecimal("-123.456789"), totalRate = BigDecimal("-123.456789"))
        assertEquals("该日有效贡献：1个项目、0个订阅。", reviewSelectionLabel(negative))
        assertEquals(BigDecimal("-123.456789"), negative.totalRate)
    }
}
