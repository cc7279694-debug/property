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
import com.cooper.lithic.core.design.ChartEvent
import com.cooper.lithic.core.design.CostChartPage
import com.cooper.lithic.core.design.CostChartPoint
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.design.moneyMetricDisplay
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.TotalCostEvent
import com.cooper.lithic.feature.appearance.LithicAppearance
import com.cooper.lithic.ui.C2VisualFixtures
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Source-prepared Compose assertions only. BUILD_DEFERRED / Android execution NOT_RUN.
 * Immutable androidTest facts and a controlled LocalDensity host open no Owner, Room or ledger.
 * These assertions do not claim Android visual, real system font or TalkBack acceptance.
 */
class C9R1ReviewSemanticsTest {
    @get:Rule val compose = createComposeRule()
    private val facts = C2VisualFixtures(populated = true)

    @Test fun three_themes_keep_exact_review_selection_units_and_distinct_legend_at_large_font() {
        val theme = mutableStateOf(LithicAppearance.STONE_MIST)
        var selections = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                LithicTheme(appearance = theme.value) { Box(Modifier.requiredSize(360.dp, 1000.dp)) {
                    ReviewShell(1, subscriptionCount = 1, trendState = facts.trend.copy(showBreakdown = true),
                        onSelectTrendDate = { selections++ })
                } }
            }
        }
        LithicAppearance.entries.forEach { appearance ->
            compose.runOnIdle { theme.value = appearance }
            scroll("total-trend-selection-label").assertTextEquals("所选日期")
            scroll("total-trend-selected-date").assertTextEquals(facts.day.toString())
            scroll("total-trend-total").assertTextEquals(moneyMetricDisplay(facts.trend.selectedPoint!!.totalRate))
            scroll("total-trend-axis-unit").assertTextEquals("纵轴 · 元/天").assertIsDisplayed()
            scroll("total-trend-legend-total").assert(hasAnyDescendant(hasText("总日均成本 · 实线")))
            scroll("total-trend-legend-project").assert(hasAnyDescendant(hasText("长期项目 · 长虚线")))
            scroll("total-trend-legend-subscription").assert(hasAnyDescendant(hasText("订阅 · 短虚线阶梯")))
            scroll("total-trend-project").assertTextEquals(moneyMetricDisplay(facts.trend.selectedPoint!!.projectRate))
            scroll("total-trend-subscription").assertTextEquals(moneyMetricDisplay(facts.trend.selectedPoint!!.subscriptionRate))
            compose.runOnIdle { assertEquals(0, selections) }
        }
    }

    @Test fun unit_slot_and_plot_frame_remain_stable_from_waiting_through_reading_to_ready() {
        val ui = mutableStateOf(TotalCostTrendUiState())
        compose.setContent { LithicTheme { Box(Modifier.requiredSize(400.dp, 1200.dp)) {
            ReviewShell(1, subscriptionCount = 1, trendState = ui.value)
        } } }
        val frame = compose.onNodeWithTag("total-trend-plot-frame").fetchSemanticsNode().boundsInRoot
        val unit = compose.onNodeWithTag("total-trend-axis-unit").fetchSemanticsNode().boundsInRoot
        listOf(TotalCostTrendUiState(loading = true), facts.trend).forEach { state ->
            compose.runOnIdle { ui.value = state }
            val currentFrame = compose.onNodeWithTag("total-trend-plot-frame").fetchSemanticsNode().boundsInRoot
            val currentUnit = compose.onNodeWithTag("total-trend-axis-unit").fetchSemanticsNode().boundsInRoot
            assertEquals(frame.top, currentFrame.top, 1f)
            assertEquals(frame.height, currentFrame.height, 1f)
            assertEquals(unit.top, currentUnit.top, 1f)
            compose.onNodeWithTag("total-trend-axis-unit").assertTextEquals("纵轴 · 元/天")
        }
    }

    @Test fun pending_date_and_failed_refresh_keep_full_events_and_old_cutoff_without_old_amount() {
        val recordId = facts.ledger.overview.projects.single().id
        val events = (0..50).map { index -> TotalCostEvent("c9-r1-${index.toString().padStart(2, '0')}",
            facts.day.minusDays(5), OverviewKind.PROJECT, recordId, "合成项目", "真实资金事件", "完整说明$index",
            amountCents = 100, nonCash = index == 50) }
        val requested = facts.day.minusDays(1)
        val state = facts.trend.copy(snapshot = facts.ledger.copy(events = events),
            timeline = facts.trend.timeline!!.copy(events = events), selectedDate = requested,
            events = emptyList(), error = "合成选日读取失败")
        var opened: Pair<OverviewKind, String>? = null
        compose.setContent { LithicTheme { Box(Modifier.requiredSize(360.dp, 1000.dp)) {
            TotalCostTrendScreen(state, {}, {}, {}, { kind, id -> opened = kind to id }, {})
        } } }
        scroll("total-trend-selected-date").assertTextEquals(requested.toString())
        compose.onNodeWithTag("total-trend-total").assertDoesNotExist()
        scroll("total-trend-total-pending").assertTextEquals("—")
        scroll("total-trend-project-pending").assertTextEquals("—")
        scroll("total-trend-subscription-pending").assertTextEquals("—")
        scroll("total-trend-canvas").assertExists()
        scroll("total-trend-cutoff").assertTextEquals("截至 ${facts.day}")
        scroll("total-trend-error").assertIsDisplayed()
        compose.onNodeWithText("合成选日读取失败", useUnmergedTree = true).assertExists()
        scroll("total-trend-tools-toggle").performClick()
        scroll("total-trend-events-toggle").performClick()
        scroll("total-trend-event-pending").assertExists()
        scroll("total-trend-events-scope").performClick()
        scroll("total-trend-event-count").assertTextEquals("全部已记录历史事件：51条")
        scroll("total-trend-events-more").assertHeightIsAtLeast(48.dp).performClick()
        scroll("total-trend-event-c9-r1-50").assert(hasAnyDescendant(hasText("完整说明50")))
        compose.onNodeWithText("非现金折抵（不是现金扣款）", useUnmergedTree = true).assertExists()
        scroll("total-trend-open-c9-r1-50").performClick()
        compose.runOnIdle { assertEquals(OverviewKind.PROJECT to recordId, opened) }
    }

    @Test fun single_item_chart_keeps_negative_precision_units_and_expanded_events_when_theme_changes() {
        val theme = mutableStateOf(LithicAppearance.STONE_MIST)
        val value = BigDecimal("-0.0005")
        val events = (0..50).map { ChartEvent("item-$it", facts.day, "同日事实$it", "完整说明$it", 100, it == 50) }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                LithicTheme(appearance = theme.value) { Box(Modifier.requiredSize(360.dp, 1000.dp)) {
                    CostChartPage("合成长期项目", "c9-r1-item-chart", "项目日均成本历史", "原领域引擎结果。",
                        facts.day, listOf(CostChartPoint(facts.day, value)), facts.day, value, "有效长期项目",
                        events, emptyList(), false, false, false, null, false, {}, {}, {})
                } }
            }
        }
        itemScroll("chart-selected-value").assertContentDescriptionEquals("项目日均成本历史，负值，绝对值小于0.01元/天")
        itemScroll("chart-cutoff").assertTextEquals("结果截至 ${facts.day}")
        itemScroll("cost-chart-axis-unit").assertTextEquals("纵轴 · 元/天")
        itemScroll("cost-chart-legend").assert(hasAnyDescendant(hasText("项目日均成本 · 实线")))
        itemScroll("chart-events-toggle").assertHeightIsAtLeast(48.dp).performClick()
        itemScroll("chart-event-day-${facts.day}").performClick()
        itemScroll("chart-events-more").performClick()
        LithicAppearance.entries.forEach { appearance ->
            compose.runOnIdle { theme.value = appearance }
            itemScroll("chart-event-item-50").assert(hasAnyDescendant(hasText("完整说明50")))
            compose.onNodeWithText("非现金折抵（不是现金扣款）", useUnmergedTree = true).assertExists()
            itemScroll("chart-selected-value").assertTextEquals(moneyMetricDisplay(value))
        }
    }

    private fun scroll(tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }

    private fun itemScroll(tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("c9-r1-item-chart").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }
}
