package com.cooper.lithic.feature.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.report.*
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Production Compose contract only. LocalDensity cases are not system-font or real-device evidence. */
class C1ReviewVisualContractTest {
    @get:Rule val compose = createComposeRule()
    private val day = LocalDate.of(2026, 10, 26)
    private var pixelsPerDp = 1f

    @Test fun dark_verified_empty_has_one_guidance_and_one_real_action_without_menu_wall() {
        var adds = 0
        compose.setContent { LithicTheme(darkTheme = true) {
            ReviewShell(0, subscriptionCount = 0, trendState = emptyState(), onAddRecord = { adds++ },
                onCategoryAnalysis = {}, onAnnualReview = {}, onLongTermReview = {},
                onProjectHistory = {}, onSubscriptionHistory = {}, onTotalCostTrend = {})
        } }
        compose.onNodeWithTag("review-empty-state").assertIsDisplayed()
        compose.onNodeWithTag("review-empty-identity").assertIsDisplayed()
        compose.onNodeWithTag("review-record-summary").assertTextEquals("记下一件物品后，在这里看见成本变化")
        compose.onAllNodesWithTag("review-add-record").assertCountEquals(1)
        compose.onNodeWithTag("review-add-record").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, adds)
        listOf("review-analysis-group", "review-archive-group", "review-full-history-group",
            "total-trend-empty", "total-trend-canvas", "total-trend-total").forEach {
            compose.onNodeWithTag(it).assertDoesNotExist()
        }
        compose.onNodeWithText("暂无长期项目或订阅记录").assertDoesNotExist()
        compose.onNodeWithTag("review-information-content").assertDoesNotExist()
        compose.onNodeWithTag("review-information").performClick()
        compose.onNodeWithTag("review-information-content").assertExists()
        compose.onNodeWithText("没有录入不等于没有花钱", substring = true).assertExists()
    }

    @Test fun loading_and_failed_zero_snapshots_are_not_replaced_with_welcoming_empty() {
        val trend = mutableStateOf(TotalCostTrendUiState(loading = true))
        var retries = 0
        compose.setContent { LithicTheme {
            ReviewShell(0, trendState = trend.value, onRetryTrend = { retries++ }, onAddRecord = {})
        } }
        compose.onNodeWithTag("review-empty-state").assertDoesNotExist()
        compose.onNodeWithText("正在读取完整历史。").assertIsDisplayed()
        compose.runOnIdle { trend.value = emptyState().copy(error = "合成读取失败") }
        compose.onNodeWithTag("review-empty-state").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-empty").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-error").assertIsDisplayed()
        compose.onNodeWithTag("review-empty-identity").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-retry").performClick()
        assertEquals(1, retries)
    }

    @Test fun external_read_or_restore_notice_suppresses_both_empty_presentations_for_retained_zero_snapshot() {
        compose.setContent { LithicTheme {
            ReviewShell(0, subscriptionCount = 0, trendState = emptyState(), hasReadNotice = true, onAddRecord = {})
        } }
        compose.onNodeWithTag("review-empty-state").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-empty").assertDoesNotExist()
        compose.onNodeWithTag("review-add-record").assertDoesNotExist()
        compose.onNodeWithTag("review-empty-identity").assertDoesNotExist()
        compose.onNodeWithTag("review-record-notice").assertTextContains("读取或恢复提示", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("review-record-summary").assertTextContains("已取得的记录快照", substring = true)
    }

    @Test fun ended_project_with_zero_current_contribution_still_has_real_history() {
        val start = day.minusDays(20)
        val fact = ProjectSnapshot(LongTermProject(start, day.minusDays(1)),
            listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(10000), start)))
        val ledger = LedgerSnapshot(OverviewSnapshot(listOf(OverviewCategory("c1-category", "其他", OverviewKind.PROJECT)),
            listOf(OverviewProject("c1-project", "c1-category", fact)), emptyList()))
        val state = TotalCostTrendUiState(snapshot = ledger,
            timeline = CostTimeline(day, start, listOf(CostTimelineEngine.point(ledger, start), CostTimelineEngine.point(ledger, day)),
                emptyList(), 240, 21, false), selectedDate = day, selectedPoint = CostTimelineEngine.point(ledger, day))
        compose.setContent { LithicTheme {
            ReviewShell(1, trendState = state, onProjectHistory = {})
        } }
        compose.onNodeWithTag("review-empty-state").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-total").assertTextContains("¥0.00", substring = true)
        compose.onNodeWithTag("total-trend-canvas").assertIsDisplayed()
        compose.onNodeWithTag("review-empty-identity").assertDoesNotExist()
        compose.onNodeWithTag("review-add-record").assertDoesNotExist()
    }

    @Test fun normal_400dp_destinations_are_compact_and_keep_all_callbacks() {
        showDestinations(400f, 1f)
        val row = compose.onNodeWithTag("review-category-analysis").assertIsDisplayed().fetchSemanticsNode()
        assertTrue("Normal row should keep the 56dp compact layout", row.size.height <= 64f * pixelsPerDp)
        compose.onNodeWithText("长期项目与订阅分别查看", useUnmergedTree = true).assertDoesNotExist()
        checkDestinations()
    }

    @Test fun local_200_percent_360dp_destinations_grow_and_remain_inside_scrollable_width() {
        showDestinations(360f, 2f)
        checkDestinations()
    }

    private fun showDestinations(widthDp: Float, fontScale: Float) {
        compose.setContent { BoxWithConstraints(Modifier.fillMaxSize()) {
            pixelsPerDp = constraints.maxWidth / widthDp
            val height = minOf(800f, constraints.maxHeight / pixelsPerDp).dp
            CompositionLocalProvider(LocalDensity provides Density(pixelsPerDp, fontScale)) {
                LithicTheme { Box(Modifier.requiredSize(widthDp.dp, height).testTag("c1-review-canvas")) {
                    ReviewShell(1, subscriptionCount = 1, onCategoryAnalysis = {}, onAnnualReview = {},
                        onLongTermReview = {}, onProjectHistory = {}, onSubscriptionHistory = {}, onTotalCostTrend = {})
                } }
            }
        } }
    }

    private fun checkDestinations() {
        val canvas = compose.onNodeWithTag("c1-review-canvas").fetchSemanticsNode()
        listOf("review-total-cost-trend", "review-category-analysis", "review-annual", "review-long-term",
            "review-project-history", "review-subscription-history").forEach { tag ->
            val row = compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().fetchSemanticsNode()
            assertTrue("Minimum target in the controlled local density", row.size.height >= 48f * pixelsPerDp - 1f)
            assertTrue(row.positionInRoot.x >= canvas.positionInRoot.x - 1f)
            assertTrue(row.positionInRoot.x + row.size.width <= canvas.positionInRoot.x + canvas.size.width + 1f)
        }
    }

    private fun emptyState() = TotalCostTrendUiState(snapshot = LedgerSnapshot(OverviewSnapshot(emptyList(), emptyList(), emptyList())),
        timeline = CostTimeline(day, null, emptyList(), emptyList(), 240, 0, false))
}
