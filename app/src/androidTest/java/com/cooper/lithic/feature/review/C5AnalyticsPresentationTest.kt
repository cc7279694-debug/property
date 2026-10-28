package com.cooper.lithic.feature.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.ChartEvent
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.*
import com.cooper.lithic.domain.report.*
import com.cooper.lithic.feature.project.ProjectCostChart
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Production Compose with synthetic facts. Source compilation is not runtime or visual evidence. */
class C5AnalyticsPresentationTest {
    @get:Rule val compose = createComposeRule()
    private val day = LocalDate.of(2026, 10, 27)

    @Test fun full_trend_orders_exact_readout_plot_contributions_and_compact_date_actions() {
        var requested: LocalDate? = null
        compose.setContent { LithicTheme { Box(Modifier.requiredSize(400.dp, 900.dp)) {
            TotalCostTrendScreen(state(), { requested = it }, {}, {}, { _, _ -> }, {})
        } } }
        compose.onNodeWithTag("total-trend-selected-date").assertTextEquals(day.toString())
        compose.onNodeWithTag("total-trend-total").assertContentDescriptionEquals("总日均成本，10.00元/天")
        val amount = compose.onNodeWithTag("total-trend-total").fetchSemanticsNode()
        val plot = compose.onNodeWithTag("total-trend-canvas").assertIsDisplayed().fetchSemanticsNode()
        val contribution = compose.onNodeWithTag("total-trend-project").fetchSemanticsNode()
        assertTrue(amount.positionInRoot.y < plot.positionInRoot.y)
        assertTrue(plot.positionInRoot.y + plot.size.height <= contribution.positionInRoot.y)
        compose.onNodeWithTag("total-trend-project").assertContentDescriptionEquals("长期项目贡献，10.00元/天")
        compose.onNodeWithTag("total-trend-subscription").assertContentDescriptionEquals("订阅贡献，0.00元/天")
        compose.onNodeWithTag("total-trend-date-input").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-axis").assertDoesNotExist()
        scroll("total-trend-tools-toggle").assertHeightIsAtLeast(48.dp).performClick()
        scroll("total-trend-date-toggle").performClick()
        scroll("total-trend-date-input").performTextReplacement(day.minusDays(3).toString())
        scroll("total-trend-date-apply").performClick()
        assertEquals(day.minusDays(3), requested)
    }

    @Test fun new_requested_day_never_uses_the_previous_exact_amount_or_contribution() {
        val original = state()
        compose.setContent { LithicTheme {
            TotalCostTrendScreen(original.copy(selectedDate = day.minusDays(1)), {}, {}, {}, { _, _ -> }, {})
        } }
        compose.onNodeWithTag("total-trend-selected-date").assertTextEquals(day.minusDays(1).toString())
        compose.onNodeWithTag("total-trend-total").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-project").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-subscription").assertDoesNotExist()
        scroll("total-trend-canvas").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
            "${day.minusDays(1)}，尚未取得所选日期的精确结果"))
    }

    @Test fun long_report_amount_stacks_before_the_metric_adapts_to_a_partial_row() {
        compose.setContent { LithicTheme { Column(Modifier.requiredSize(400.dp, 600.dp)) {
            ReportMoneyValues(ReportAmounts(projectOutlayCents = 123_456_789_012_345_600), "c5-report", OverviewKind.PROJECT)
        } } }
        val label = compose.onNodeWithText("长期项目投入").fetchSemanticsNode()
        val amount = compose.onNodeWithTag("c5-report-outlay")
            .assertContentDescriptionEquals("长期项目投入，1234567890123456.00元").fetchSemanticsNode()
        assertTrue(amount.positionInRoot.y >= label.positionInRoot.y + label.size.height)
    }

    @Test fun long_daily_contribution_stacks_at_the_preferred_row_size() {
        compose.setContent { LithicTheme { Box(Modifier.requiredSize(400.dp, 900.dp)) {
            TotalCostTrendScreen(state(123_456_789_012_345_600), {}, {}, {}, { _, _ -> }, {})
        } } }
        val amount = scroll("total-trend-project")
            .assertContentDescriptionEquals("长期项目贡献，123456789012345.60元/天").fetchSemanticsNode()
        val label = compose.onNodeWithText("长期项目贡献").fetchSemanticsNode()
        assertTrue(amount.positionInRoot.y >= label.positionInRoot.y + label.size.height)
    }

    @Test fun single_item_chart_keeps_pending_date_distinct_and_events_remain_available_on_demand() {
        val input = project()
        val requested = day.minusDays(1)
        compose.setContent { LithicTheme {
            ProjectCostChart("合成项目", ProjectTimelineEngine.build(input, day),
                ProjectTimelineEngine.point(input, day, day),
                listOf(ChartEvent("c5-event", day, "真实收回", "完整事实", 100, nonCash = true)),
                false, null, false, {}, {}, {}, requestedDate = requested)
        } }
        val container = compose.onNodeWithTag("project-cost-chart-screen")
        container.performScrollToNode(hasTestTag("chart-selected-date"))
        compose.onNodeWithTag("chart-selected-date").assertTextEquals("所选日期：$requested")
        compose.onNodeWithTag("chart-selected-value").assertDoesNotExist()
        container.performScrollToNode(hasTestTag("chart-events-toggle"))
        compose.onNodeWithTag("chart-events-toggle").performClick()
        container.performScrollToNode(hasTestTag("chart-event-day-$day"))
        compose.onNodeWithTag("chart-event-day-$day").performClick()
        container.performScrollToNode(hasTestTag("chart-event-c5-event"))
        compose.onNodeWithText("非现金折抵（不是现金扣款）", useUnmergedTree = true).assertExists()
    }

    private fun project(outlayCents: Long = 10_000) = ProjectSnapshot(LongTermProject(day.minusDays(9)),
        listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(outlayCents), day.minusDays(9))))

    private fun state(outlayCents: Long = 10_000): TotalCostTrendUiState {
        val ledger = LedgerSnapshot(OverviewSnapshot(listOf(OverviewCategory("c5", "其他", OverviewKind.PROJECT)),
            listOf(OverviewProject("c5-record", "c5", project(outlayCents))), emptyList()))
        val exact = CostTimelineEngine.point(ledger, day)
        return TotalCostTrendUiState(snapshot = ledger, selectedDate = day, selectedPoint = exact,
            timeline = CostTimeline(day, day.minusDays(9), listOf(CostTimelineEngine.point(ledger, day.minusDays(9)), exact),
                emptyList(), 240, 10, false))
    }

    private fun scroll(tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }
}
