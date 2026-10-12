package com.cooper.lithic.domain.project

import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ProjectLifecycleRulesTest {
    private val start = LocalDate.of(2026, 1, 1)

    @Test fun rejects_end_before_ownership_without_changing_cost_rules() {
        assertNotNull(ProjectLifecycleRules.validateEnd(start, start.minusDays(1), "卖出"))
        assertNull(ProjectLifecycleRules.validateEnd(start, start, "卖出"))
    }

    @Test fun requires_a_real_nonblank_end_reason() {
        assertNotNull(ProjectLifecycleRules.validateEnd(start, start, " \n\t"))
        assertNull(ProjectLifecycleRules.validateEnd(start, start.plusDays(1), "结束使用"))
    }

    @Test fun end_day_is_already_history_but_future_end_is_current() {
        val end = start.plusDays(9)
        assertFalse(ProjectLifecycleRules.isEnded(null, end))
        assertFalse(ProjectLifecycleRules.isEnded(end, end.minusDays(1)))
        assertTrue(ProjectLifecycleRules.isEnded(end, end))
        assertTrue(ProjectLifecycleRules.isEnded(end, end.plusDays(10)))
    }

    // Frozen engine behavior is exercised directly; LifecycleRules never stores or computes days.
    @Test fun frozen_engine_keeps_ten_days_after_late_installment_and_negative_recovery() {
        val input = ProjectSnapshot(LongTermProject(start, start.plusDays(9)), listOf(
            ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(100_000), start),
            ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(20_000), start.plusDays(19)),
            ProjectMoneyEvent(MoneyDirection.RECOVERY, Money(130_000), start.plusDays(20)),
        ))
        val ninth = ProjectCostEngine.evaluate(input, start.plusDays(8))
        assertEquals(9L, ninth.heldDays)
        assertTrue(ninth.contributesToCurrentTotal)
        val ended = ProjectCostEngine.evaluate(input, start.plusDays(9))
        assertEquals(10L, ended.heldDays)
        assertFalse(ended.contributesToCurrentTotal)
        val later = ProjectCostEngine.evaluate(input, start.plusDays(20))
        assertEquals(10L, later.heldDays)
        assertEquals(-10_000L, later.netCents)
        assertFalse(later.contributesToCurrentTotal)
    }
}
