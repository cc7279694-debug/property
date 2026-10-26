package com.cooper.lithic.feature.project

import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class C2R1ProjectEventLabelsTest {
    @Test fun regular_outlay_and_recovery_stay_explicit() {
        assertEquals("投入", projectMoneyEventTypeLabel(MoneyDirection.OUTLAY, ProjectMoneyEventRole.REGULAR))
        assertEquals("收回", projectMoneyEventTypeLabel(MoneyDirection.RECOVERY, ProjectMoneyEventRole.REGULAR))
    }

    @Test fun initial_and_end_settlement_are_not_flattened_into_regular_events() {
        assertEquals("初始投入", projectMoneyEventTypeLabel(MoneyDirection.OUTLAY, ProjectMoneyEventRole.INITIAL))
        assertEquals("结束关联收回", projectMoneyEventTypeLabel(MoneyDirection.RECOVERY, ProjectMoneyEventRole.END_SETTLEMENT))
    }

    @Test fun trade_in_credit_is_never_labeled_as_a_cash_receipt() {
        assertEquals("非现金折抵", projectMoneyEventBasisLabel(MoneyBasis.TRADE_IN_CREDIT))
        assertEquals("现金", projectMoneyEventBasisLabel(MoneyBasis.CASH))
    }

    @Test fun long_timelines_keep_a_quick_history_route_only_when_the_real_callback_exists() {
        assertTrue(projectDetailMenuEntries(false, false, false, false, true)
            .any { it.action == ProjectDetailAction.COST_HISTORY })
        assertFalse(projectDetailMenuEntries(false, false, false, false)
            .any { it.action == ProjectDetailAction.COST_HISTORY })
    }
}
