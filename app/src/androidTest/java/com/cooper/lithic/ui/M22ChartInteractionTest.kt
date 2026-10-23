package com.cooper.lithic.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.CostChart
import com.cooper.lithic.core.design.CostChartPoint
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.*
import com.cooper.lithic.domain.report.*
import com.cooper.lithic.feature.project.ProjectCostChart
import com.cooper.lithic.feature.review.TotalCostTrendScreen
import com.cooper.lithic.feature.review.TotalCostTrendUiState
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Compose gestures/semantics in an isolated host; this is not a TalkBack service test. */
@RunWith(AndroidJUnit4::class)
class M22ChartInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val first = LocalDate.of(2026, 10, 1)
    private val last = first.plusDays(10)

    @Test fun horizontal_drag_selects_an_unsampled_calendar_day_not_interpolated_money() {
        var requested: LocalDate? = null
        compose.setContent { LithicTheme {
            CostChart(listOf(CostChartPoint(first, BigDecimal.TEN), CostChartPoint(last, BigDecimal.ONE)),
                null, { requested = it }, false, emptySet())
        } }
        val inset = with(compose.density) { 12.dp.toPx() }
        compose.onNodeWithTag("cost-chart-canvas").performTouchInput {
            val usable = width - inset * 2
            swipe(Offset(inset + usable * .1f, centerY), Offset(inset + usable * .6f, centerY), 400)
        }
        compose.runOnIdle { assertEquals(first.plusDays(6), requested) }
    }

    @Test fun item_semantics_and_text_controls_query_the_same_previous_calendar_day() {
        val input = ProjectSnapshot(LongTermProject(first), listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(1), first)))
        val focused = first.plusDays(5)
        val selectedDates = mutableListOf<LocalDate>()
        compose.setContent { LithicTheme {
            ProjectCostChart("合成精确选日", ProjectTimelineEngine.build(input, last), ProjectTimelineEngine.point(input, last, focused),
                emptyList(), false, null, false, { selectedDates += it }, {}, {})
        } }
        itemScroll("cost-chart-canvas")
        val previousAction = compose.onNodeWithTag("cost-chart-canvas").fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .single { it.label == "前一天" }
        compose.runOnIdle {
            assertTrue(previousAction.action())
        }
        itemScroll("chart-previous-day")
        compose.onNodeWithTag("chart-previous-day").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(listOf(focused.minusDays(1), focused.minusDays(1)), selectedDates) }
    }

    @Test fun total_trend_semantics_and_text_controls_query_same_next_unsampled_day() {
        val focus = first.plusDays(5)
        val zero = BigDecimal.ZERO
        val project = OverviewProject("p", "pc", ProjectSnapshot(LongTermProject(first), listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(0), first))))
        val ledger = LedgerSnapshot(OverviewSnapshot(listOf(OverviewCategory("pc", "项目", OverviewKind.PROJECT)), listOf(project), emptyList()))
        fun point(date: LocalDate) = CostTimelinePoint(date, zero, zero, zero, emptyList(), 1, 0, 1, 0, false, false)
        val timeline = CostTimeline(last, first, listOf(point(first), point(last)), emptyList(), 12, 11, true)
        val selectedDates = mutableListOf<LocalDate>()
        compose.setContent { LithicTheme {
            TotalCostTrendScreen(TotalCostTrendUiState(ledger, timeline, focus, point(focus)), { selectedDates += it }, {}, {}, { _, _ -> }, {})
        } }
        totalScroll("total-trend-canvas")
        val nextAction = compose.onNodeWithTag("total-trend-canvas").fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .single { it.label == "后一天" }
        compose.runOnIdle {
            assertTrue(nextAction.action())
        }
        totalScroll("total-trend-next-day")
        compose.onNodeWithTag("total-trend-next-day").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(listOf(focus.plusDays(1), focus.plusDays(1)), selectedDates) }
    }

    @Test fun vertical_swipe_on_canvas_keeps_outer_page_scrolling_available() {
        val input = ProjectSnapshot(LongTermProject(first), listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(1), first)))
        var selected = 0
        compose.setContent { LithicTheme {
            ProjectCostChart("合成纵向滚动", ProjectTimelineEngine.build(input, last), ProjectTimelineEngine.point(input, last, last),
                emptyList(), false, null, false, { selected++ }, {}, {})
        } }
        itemScroll("cost-chart-canvas")
        val before = compose.onNodeWithTag("cost-chart-canvas").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("cost-chart-canvas").performTouchInput { swipeUp() }
        compose.waitForIdle()
        val after = compose.onNodeWithTag("cost-chart-canvas").fetchSemanticsNode().boundsInRoot.top
        assertTrue("The outer vertical list must still scroll", after < before)
        compose.runOnIdle { assertEquals(0, selected) }
    }

    @Test fun axis_boundary_actions_and_text_controls_both_refuse_to_cross_first_day() {
        val input = ProjectSnapshot(LongTermProject(first), listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(1), first)))
        var requests = 0
        compose.setContent { LithicTheme {
            ProjectCostChart("合成边界选日", ProjectTimelineEngine.build(input, last), ProjectTimelineEngine.point(input, last, first),
                emptyList(), false, null, false, { requests++ }, {}, {})
        } }
        itemScroll("cost-chart-canvas")
        val action = compose.onNodeWithTag("cost-chart-canvas").fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .single { it.label == "前一天" }
        compose.runOnIdle { assertFalse(action.action()) }
        itemScroll("chart-previous-day")
        compose.onNodeWithTag("chart-previous-day").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, requests) }
    }

    @Test fun pending_item_selection_accessibility_uses_requested_date_without_old_amount() {
        val input = ProjectSnapshot(LongTermProject(first), listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(1), first)))
        val focus = first.plusDays(5)
        var request: LocalDate? = null
        compose.setContent { LithicTheme {
            ProjectCostChart("合成待查询日期", ProjectTimelineEngine.build(input, last), null, emptyList(), false, null, false,
                { request = it }, {}, {}, requestedDate = focus)
        } }
        itemScroll("cost-chart-canvas")
        val action = compose.onNodeWithTag("cost-chart-canvas").fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .single { it.label == "后一天" }
        compose.runOnIdle { assertTrue(action.action()) }
        itemScroll("chart-next-day")
        compose.onNodeWithTag("chart-next-day").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(focus.plusDays(1), request) }
        itemScroll("chart-selected-date")
        compose.onNodeWithTag("chart-selected-date").assertTextContains(focus.toString(), substring = true)
        compose.onNodeWithTag("chart-selected-value").assertDoesNotExist()
    }

    private fun itemScroll(tag: String) = compose.onNodeWithTag("project-cost-chart-screen").performScrollToNode(hasTestTag(tag))
    private fun totalScroll(tag: String) = compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag(tag))
}
