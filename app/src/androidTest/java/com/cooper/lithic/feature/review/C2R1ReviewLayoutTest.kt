package com.cooper.lithic.feature.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.report.CostTimeline
import com.cooper.lithic.domain.report.CostTimelineEngine
import com.cooper.lithic.domain.report.LedgerSnapshot
import com.cooper.lithic.domain.report.OverviewCategory
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.OverviewProject
import com.cooper.lithic.domain.report.OverviewSnapshot
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Compile-only here; no browser-derived claim about the production 360dp layout. */
class C2R1ReviewLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun real_plot_is_visible_without_scrolling_at_360_by_800_normal_font() {
        compose.setContent { LithicTheme { Box(Modifier.requiredSize(360.dp, 800.dp)) {
            ReviewShell(1, trendState = state())
        } } }
        compose.onNodeWithTag("total-trend-total").assertTextContains("¥10.00")
        compose.onNodeWithTag("total-trend-canvas").assertIsDisplayed()
    }

    @Test fun contributions_and_exact_date_access_survive_the_more_compact_first_screen() {
        var breakdowns = 0
        compose.setContent { LithicTheme { Box(Modifier.requiredSize(360.dp, 800.dp)) {
            ReviewShell(1, trendState = state(), onToggleTrendBreakdown = { breakdowns++ })
        } } }
        compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag("total-trend-project"))
        compose.onNodeWithTag("total-trend-project").assertTextContains("¥10.00")
        compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag("total-trend-tools-toggle"))
        compose.onNodeWithTag("total-trend-tools-toggle").performClick()
        compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag("total-trend-breakdown"))
        compose.onNodeWithTag("total-trend-breakdown").performClick()
        assertEquals(1, breakdowns)
        compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag("review-trend-date-toggle"))
        compose.onNodeWithTag("review-trend-date-toggle").performClick()
        compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag("total-trend-date-input"))
        compose.onNodeWithTag("total-trend-date-input").assertIsDisplayed()
    }

    private fun state(): TotalCostTrendUiState {
        val day = LocalDate.of(2026, 10, 26)
        val start = day.minusDays(9)
        val project = ProjectSnapshot(LongTermProject(start), listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(10000), start)))
        val ledger = LedgerSnapshot(OverviewSnapshot(listOf(OverviewCategory("r1-category", "其他", OverviewKind.PROJECT)),
            listOf(OverviewProject("r1-project", "r1-category", project)), emptyList()))
        return TotalCostTrendUiState(snapshot = ledger, selectedDate = day,
            selectedPoint = CostTimelineEngine.point(ledger, day),
            timeline = CostTimeline(day, start, listOf(CostTimelineEngine.point(ledger, start), CostTimelineEngine.point(ledger, day)),
                emptyList(), 240, 10, false))
    }
}
