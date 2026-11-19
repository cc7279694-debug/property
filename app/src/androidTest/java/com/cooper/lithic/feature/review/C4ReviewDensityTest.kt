package com.cooper.lithic.feature.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.*
import com.cooper.lithic.domain.report.*
import java.time.LocalDate
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Production composition with synthetic facts; compilation is not Android visual evidence. */
class C4ReviewDensityTest {
    @get:Rule val compose = createComposeRule()
    private val day = LocalDate.of(2026, 10, 27)

    @Test fun summary_uses_a_short_single_series_with_visible_counts_and_information() {
        show()
        compose.onNodeWithTag("total-trend-total").assertTextEquals("¥10.00/天")
        compose.onNodeWithTag("total-trend-selection-label").assertTextEquals("观察日")
        compose.onNodeWithTag("total-trend-selected-date").assertTextEquals(day.toString())
        val chart = compose.onNodeWithTag("total-trend-canvas").assertIsDisplayed().fetchSemanticsNode()
        val density = compose.density.density
        assertTrue("Review preview should stay shorter than the former 172dp plot", chart.size.height < 172 * density)
        compose.onNodeWithTag("total-trend-axis").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-legend-project").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-tools-toggle").assertDoesNotExist()
        compose.onNodeWithTag("review-counts-compact").assertDoesNotExist()
        compose.onNodeWithTag("review-summary-scope").assertTextContains("历史", substring = true)
        compose.onNodeWithTag("review-information").performClick()
        compose.onNodeWithTag("review-project-count").assertTextContains("1 条长期项目")
        compose.onNodeWithTag("review-count-scope").assertTextContains("当前和历史", substring = true)
    }

    @Test fun chart_action_names_curves_and_exact_date_keeps_the_original_callback() {
        var selected: LocalDate? = null
        var toggles = 0
        compose.setContent { LithicTheme {
            TotalCostTrendScreen(state(), onToggleBreakdown = { toggles++ }, onSelectDate = { selected = it },
                onRetry = {}, onOpenRecord = { _, _ -> }, onBack = {})
        } }
        openTools()
        scroll("total-trend-breakdown").assertTextContains("显示分项曲线").performClick()
        assertEquals(1, toggles)
        scroll("total-trend-date-toggle").performClick()
        scroll("total-trend-date-input").performTextReplacement("2026-10-26")
        scroll("total-trend-date-apply").performClick()
        assertEquals(day.minusDays(1), selected)
    }

    @Test fun negative_and_real_zero_remain_full_and_read_failure_is_always_visible() {
        compose.setContent { LithicTheme {
            TotalCostTrendScreen(state(recovery = true).copy(error = "合成读取失败"), {}, {}, {}, { _, _ -> }, {})
        } }
        scroll("total-trend-total").assertTextEquals("−¥10.00/天")
        scroll("total-trend-project").assertTextEquals("−¥10.00/天")
        scroll("total-trend-subscription").assertTextEquals("¥0.00/天")
        scroll("total-trend-error").assertIsDisplayed()
        compose.onNodeWithTag("review-empty-state").assertDoesNotExist()
    }

    @Test fun local_double_font_at_360_keeps_date_controls_and_complete_amount_accessible() {
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
            LithicTheme { Box(Modifier.requiredSize(360.dp, 800.dp)) {
                TotalCostTrendScreen(state(), {}, {}, {}, { _, _ -> }, {})
            } }
        } }
        scroll("total-trend-total").assertTextEquals("¥10.00/天")
        openTools()
        scroll("total-trend-date-toggle").assertHeightIsAtLeast(48.dp).performClick()
        scroll("total-trend-date-input").assertIsDisplayed()
        scroll("total-trend-date-apply").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test fun visible_status_distinguishes_real_zero_from_history_without_current_contribution() {
        val initial = state()
        val realZero = requireNotNull(initial.selectedPoint).copy(totalRate = BigDecimal.ZERO, projectRate = BigDecimal.ZERO,
            hasPositiveContribution = false, hasNegativeContribution = false)
        val ui = mutableStateOf(initial.copy(selectedPoint = realZero))
        compose.setContent { LithicTheme { TotalCostTrendScreen(ui.value, {}, {}, {}, { _, _ -> }, {}) } }
        scroll("total-trend-details-toggle").performClick()
        compose.onNodeWithTag("total-trend-selected-status").assertTextContains("真实日均成本为0", substring = true)
        compose.runOnIdle { ui.value = initial.copy(selectedPoint = realZero.copy(activeProjectCount = 0)) }
        compose.onNodeWithTag("total-trend-selected-status").assertTextContains("历史记录仍保留", substring = true)
        compose.onNodeWithTag("review-empty-state").assertDoesNotExist()
    }

    private fun show(value: TotalCostTrendUiState = state()) {
        compose.setContent { LithicTheme { Box(Modifier.requiredSize(400.dp, 800.dp)) {
            ReviewShell(1, trendState = value)
        } } }
    }

    private fun state(recovery: Boolean = false): TotalCostTrendUiState {
        val start = day.minusDays(9)
        val events = listOf(ProjectMoneyEvent(if (recovery) MoneyDirection.RECOVERY else MoneyDirection.OUTLAY,
            Money(10000), start))
        val project = ProjectSnapshot(LongTermProject(start), events)
        val ledger = LedgerSnapshot(OverviewSnapshot(listOf(OverviewCategory("c4", "其他", OverviewKind.PROJECT)),
            listOf(OverviewProject("c4-record", "c4", project)), emptyList()))
        val point = CostTimelineEngine.point(ledger, day)
        return TotalCostTrendUiState(snapshot = ledger, selectedDate = day, selectedPoint = point,
            timeline = CostTimeline(day, start, listOf(CostTimelineEngine.point(ledger, start), point), emptyList(), 240, 10, false))
    }

    private fun openTools() { scroll("total-trend-tools-toggle").performClick() }

    private fun scroll(tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }
}
