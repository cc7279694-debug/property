package com.cooper.lithic.feature.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.design.moneyMetricDisplay
import com.cooper.lithic.domain.report.CostTimelineEngine
import com.cooper.lithic.ui.C2VisualFixtures
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Production Compose and immutable androidTest-only facts. Controlled 400/360dp hosts and
 * LocalDensity font cases are layout assertions, not real-device, system-font or IME evidence.
 * No Room, storage Owner, screenshot capture or personal ledger is opened.
 * Android execution remains NOT_RUN until separate device authorization.
 */
class C5R1ReviewStabilityTest {
    @get:Rule val compose = createComposeRule()
    private val facts = C2VisualFixtures(true)

    @Test fun first_waiting_reading_and_ready_frames_keep_plot_and_analysis_positions() {
        val ui = mutableStateOf(TotalCostTrendUiState())
        showReview({ ui.value })
        compose.onNodeWithTag("total-trend-loading-placeholder").assertExists()
        compose.onNodeWithTag("total-trend-total-pending").assertTextEquals("—")
        compose.onNodeWithTag("total-trend-canvas").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-total").assertDoesNotExist()
        val plot = stamp("total-trend-plot-frame")
        val menu = stamp("review-category-analysis")

        compose.runOnIdle { ui.value = TotalCostTrendUiState(loading = true) }
        sameLayout(plot, "total-trend-plot-frame")
        sameLayout(menu, "review-category-analysis")
        compose.runOnIdle { ui.value = facts.trend }

        compose.onNodeWithTag("total-trend-total")
            .assertTextEquals(moneyMetricDisplay(requireNotNull(facts.trend.selectedPoint).totalRate))
        compose.onNodeWithTag("total-trend-canvas").assertExists()
        compose.onNodeWithTag("total-trend-loading-placeholder").assertDoesNotExist()
        sameLayout(plot, "total-trend-plot-frame")
        sameLayout(menu, "review-category-analysis")
    }

    @Test fun same_snapshot_refresh_keeps_selected_date_plot_and_visible_old_cutoff() {
        val selected = facts.day.minusDays(7)
        val retained = facts.trend.copy(selectedDate = selected,
            selectedPoint = CostTimelineEngine.point(facts.ledger, selected))
        val ui = mutableStateOf(retained)
        showReview({ ui.value })
        val date = stamp("total-trend-selected-date")
        val plot = stamp("total-trend-plot-frame")
        val amount = moneyMetricDisplay(requireNotNull(retained.selectedPoint).totalRate)

        compose.runOnIdle { ui.value = retained.copy(loading = true) }
        compose.onNodeWithTag("total-trend-selected-date").assertTextEquals(selected.toString())
        compose.onNodeWithTag("total-trend-total").assertTextEquals(amount)
        compose.onNodeWithTag("total-trend-refresh-status").assertTextContains("刷新", substring = true)
        compose.onNodeWithTag("total-trend-cutoff").assertTextContains(facts.day.toString(), substring = true)
        compose.onNodeWithTag("total-trend-loading-placeholder").assertDoesNotExist()
        sameLayout(date, "total-trend-selected-date")
        sameLayout(plot, "total-trend-plot-frame")

        compose.runOnIdle { ui.value = retained }
        sameLayout(date, "total-trend-selected-date")
        sameLayout(plot, "total-trend-plot-frame")
        compose.onNodeWithTag("total-trend-total").assertTextEquals(amount)
    }

    @Test fun initial_and_retained_read_failures_keep_real_errors_and_original_retry() {
        val ui = mutableStateOf(TotalCostTrendUiState(error = "合成首读失败"))
        var retries = 0
        showReview({ ui.value }, onRetry = { retries++ })
        compose.onNodeWithTag("review-empty-state").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-canvas").assertDoesNotExist()
        scroll("total-trend-error").assertIsDisplayed()
        compose.onNodeWithText("合成首读失败").assertExists()
        scroll("total-trend-retry").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, retries)

        compose.runOnIdle { ui.value = facts.trend.copy(error = "合成刷新失败，保留旧图") }
        scroll("total-trend-canvas").assertIsDisplayed()
        compose.onNodeWithTag("total-trend-total")
            .assertTextEquals(moneyMetricDisplay(requireNotNull(facts.trend.selectedPoint).totalRate))
        scroll("total-trend-cutoff").assertTextContains(facts.day.toString(), substring = true)
        scroll("total-trend-error").assertIsDisplayed()
        compose.onNodeWithText("合成刷新失败，保留旧图").assertExists()
        scroll("total-trend-retry").performClick()
        assertEquals(2, retries)
    }

    @Test fun date_computation_without_read_loading_never_borrows_old_amount_or_zero() {
        val requested = facts.day.minusDays(1)
        val pending = facts.trend.copy(selectedDate = requested, loading = false)
        // The deliberately mismatched old point exercises the presentation date fence.
        showReview({ pending })
        compose.onNodeWithTag("total-trend-selected-date").assertTextEquals(requested.toString())
        compose.onNodeWithTag("total-trend-total").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-project").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-subscription").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-total-pending").assertTextEquals("—")
        compose.onNodeWithTag("total-trend-project-pending").assertTextEquals("—")
        compose.onNodeWithTag("total-trend-subscription-pending").assertTextEquals("—")
        compose.onNodeWithTag("total-trend-canvas").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
                "$requested，尚未取得所选日期的精确结果"))
    }

    @Test fun fresh_scoped_state_drops_every_old_plot_date_and_amount_without_a_ui_cache() {
        val ui = mutableStateOf(facts.trend)
        showReview({ ui.value })
        compose.onNodeWithTag("total-trend-canvas").assertExists()
        compose.onNodeWithTag("total-trend-total").assertExists()
        compose.runOnIdle { ui.value = TotalCostTrendUiState(loading = true) }
        compose.onNodeWithTag("total-trend-loading-placeholder").assertExists()
        compose.onNodeWithTag("total-trend-canvas").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-selected-date").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-cutoff").assertTextEquals("截止日期待读取")
        compose.onNodeWithTag("total-trend-total").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-project").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-subscription").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-total-pending").assertTextEquals("—")
    }

    @Test fun full_tools_at_local_360_double_font_keep_exact_date_curves_and_one_information_entry() {
        var selected: LocalDate? = null
        var toggles = 0
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                LithicTheme { Box(Modifier.requiredSize(360.dp, 1000.dp)) {
                    TotalCostTrendScreen(facts.trend, onSelectDate = { selected = it },
                        onToggleBreakdown = { toggles++ }, onRetry = {}, onOpenRecord = { _, _ -> }, onBack = {})
                } }
            }
        }
        scroll("total-trend-total")
            .assertTextEquals(moneyMetricDisplay(requireNotNull(facts.trend.selectedPoint).totalRate))
        compose.onNodeWithTag("total-trend-date-input").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-axis-toggle").assertDoesNotExist()
        scroll("total-trend-tools-toggle").assertHeightIsAtLeast(48.dp).performClick()
        scroll("total-trend-date-toggle").assertHeightIsAtLeast(48.dp).performClick()
        scroll("total-trend-date-input").performTextReplacement(facts.day.minusDays(1).toString())
        scroll("total-trend-date-apply").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(facts.day.minusDays(1), selected)
        scroll("total-trend-breakdown").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, toggles)
        scroll("total-trend-details-toggle").assertHeightIsAtLeast(48.dp).performClick()
        compose.onAllNodesWithTag("total-trend-axis").assertCountEquals(1)
        compose.onAllNodesWithTag("total-trend-selection-meaning").assertCountEquals(1)
        compose.onAllNodesWithTag("total-trend-selected-status").assertCountEquals(1)
        compose.onNodeWithText("知道了").assertHeightIsAtLeast(48.dp).performClick()
    }

    private fun showReview(value: () -> TotalCostTrendUiState, onRetry: () -> Unit = {}) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
                LithicTheme { Box(Modifier.requiredSize(400.dp, 1200.dp)) {
                    ReviewShell(1, subscriptionCount = 1, trendState = value(), onRetryTrend = onRetry,
                        onTotalCostTrend = {}, onCategoryAnalysis = {}, onAnnualReview = {},
                        onLongTermReview = {}, onProjectHistory = {}, onSubscriptionHistory = {})
                } }
            }
        }
    }

    private data class LayoutStamp(val x: Float, val y: Float, val width: Float, val height: Float)
    private fun stamp(tag: String): LayoutStamp = compose.onNodeWithTag(tag).fetchSemanticsNode().let {
        LayoutStamp(it.positionInRoot.x, it.positionInRoot.y, it.size.width.toFloat(), it.size.height.toFloat())
    }
    private fun sameLayout(expected: LayoutStamp, tag: String) {
        val actual = stamp(tag)
        assertEquals("$tag x", expected.x, actual.x, 1f)
        assertEquals("$tag y", expected.y, actual.y, 1f)
        assertEquals("$tag width", expected.width, actual.width, 1f)
        assertEquals("$tag height", expected.height, actual.height, 1f)
    }
    private fun scroll(tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }
}
