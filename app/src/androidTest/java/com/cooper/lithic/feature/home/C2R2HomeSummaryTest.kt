package com.cooper.lithic.feature.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.domain.report.OverviewResult
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

/** Runtime status remains NOT_RUN; these assertions must not be claimed as screenshots. */
class C2R2HomeSummaryTest {
    @get:Rule val compose = createComposeRule()
    @Test fun current_contributor_counts_are_distinct_from_all_record_counts() {
        val snapshot = OverviewResult(LocalDate.of(2026, 10, 26), BigDecimal("6.48"),
            BigDecimal("4.31"), BigDecimal("2.17"), 8, 5, 2, 1, emptyList(), emptyList())
        compose.setContent { LithicTheme { HomeShell(8, {}, {}, 5, {}, {}, overview = snapshot) } }
        compose.onNodeWithTag("home-active-project-count").assertTextEquals("当前有效 2")
        compose.onNodeWithTag("home-active-subscription-count").assertTextEquals("当前有效 1")
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("已录入 8 条长期项目（含历史）")
    }
    @Test fun unknown_snapshot_does_not_invent_zero_active_counts() {
        compose.setContent { LithicTheme { HomeShell(8, {}, {}, 5, {}, {}) } }
        compose.onNodeWithTag("home-active-project-count").assertTextEquals("当前有效数量未取得")
        compose.onNodeWithTag("home-active-subscription-count").assertTextEquals("当前有效数量未取得")
    }
}
