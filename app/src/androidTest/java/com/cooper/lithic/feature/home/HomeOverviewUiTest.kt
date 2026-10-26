package com.cooper.lithic.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.domain.report.CategoryDailyCost
import com.cooper.lithic.domain.report.OverviewResult
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeOverviewUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun unavailable_overview_never_invents_a_zero_cost() {
        compose.setContent { LithicTheme {
            HomeShell(0L, {}, {}, 0L, {}, {})
        } }
        compose.onNodeWithTag("home-total-rate").assertTextContains("—")
        compose.onNodeWithText("当前日均成本尚未取得").assertIsDisplayed()
        compose.onNodeWithText("¥0.00").assertDoesNotExist()
    }

    @Test
    fun empty_ledger_has_no_cost_even_when_the_aggregate_number_is_zero() {
        show(result())
        compose.onNodeWithTag("home-total-rate").assertTextContains("—")
        compose.onNodeWithTag("home-first-record").assertIsDisplayed()
        compose.onNodeWithText("¥0.00").assertDoesNotExist()
    }

    @Test
    fun recorded_zero_cost_remains_a_real_value_and_an_active_category() {
        show(result(projectCount = 1L, activeProjectCount = 1L,
            projectCategories = listOf(CategoryDailyCost("zero", "零元项目", BigDecimal.ZERO, 1L))))
        compose.onNodeWithTag("home-total-rate").assertTextContains("¥0.00")
        compose.onNodeWithText("暂无记录").assertDoesNotExist()
        compose.onNodeWithTag("home-category-disclosure").performScrollTo().performClick()
        compose.onNodeWithTag("home-project-category-zero").performScrollTo().assertTextContains("¥0.00")
        compose.onNodeWithTag("home-no-active-records").assertDoesNotExist()
    }

    @Test
    fun signed_category_values_and_type_subtotals_use_the_supplied_unrounded_results() {
        show(result(total = "0.245", project = "0.120", subscription = "0.125",
            projectCount = 2L, subscriptionCount = 1L, activeProjectCount = 2L, activeSubscriptionCount = 1L,
            projectCategories = listOf(
                CategoryDailyCost("p", "其他", BigDecimal("0.125"), 1L),
                CategoryDailyCost("negative", "净收回", BigDecimal("-0.005"), 1L)),
            subscriptionCategories = listOf(CategoryDailyCost("s", "其他", BigDecimal("0.125"), 1L))))
        compose.onNodeWithTag("home-total-rate").assertTextContains("¥0.25")
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("2")
        compose.onNodeWithTag("home-category-disclosure").performScrollTo().performClick()
        compose.onNodeWithTag("home-project-rate").performScrollTo().assertTextContains("¥0.12")
        compose.onNodeWithTag("home-project-category-p").performScrollTo().assertTextContains("¥0.13")
        compose.onNodeWithTag("home-project-category-negative").performScrollTo().assertTextContains("−<¥0.01")
        compose.onNodeWithTag("home-subscription-rate").performScrollTo().assertTextContains("¥0.13")
        compose.onNodeWithTag("home-subscription-category-s").performScrollTo().assertTextContains("¥0.13")
        compose.onNodeWithTag("home-rounding-note").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun inactive_records_are_explained_without_being_reported_as_an_empty_ledger() {
        show(result(projectCount = 1L, subscriptionCount = 1L,
            projectCategories = listOf(CategoryDailyCost("ended", "结束类别", BigDecimal.ZERO, 0L)),
            subscriptionCategories = listOf(CategoryDailyCost("expired", "过期类别", BigDecimal.ZERO, 0L))))
        compose.onNodeWithTag("home-total-rate").assertTextContains("¥0.00")
        compose.onNodeWithTag("home-no-active-records").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("暂无记录").assertDoesNotExist()
        compose.onNodeWithTag("home-project-category-ended").assertDoesNotExist()
        compose.onNodeWithTag("home-subscription-category-expired").assertDoesNotExist()
    }

    @Test
    fun all_existing_actions_remain_reachable_in_a_narrow_viewport_with_large_text() {
        val clicks = mutableListOf<String>()
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize().testTag("home-test-viewport")) {
                val canvasDensity = constraints.maxWidth / 360f
                val canvasHeight = minOf(640f, constraints.maxHeight / canvasDensity).dp
                CompositionLocalProvider(LocalDensity provides Density(canvasDensity, fontScale = 2f)) {
                    LithicTheme(darkTheme = true) {
                        Box(Modifier.requiredSize(width = 360.dp, height = canvasHeight).testTag("home-test-canvas")) {
                            HomeShell(0L, { clicks += "projects" }, { clicks += "new-project" },
                                0L, { clicks += "subscriptions" }, { clicks += "new-subscription" },
                                overview = result(projectCount = 1L, activeProjectCount = 1L))
                        }
                    }
                }
            }
        }
        assertHomeFitsVisibleWidth()
        compose.onNodeWithTag("open-project-list").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-open-add").assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-create-project").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("open-subscription-list").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-open-add").assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-create-subscription").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(listOf("projects", "new-project", "subscriptions", "new-subscription"), clicks)
    }

    private fun assertHomeFitsVisibleWidth() {
        val viewport = compose.onNodeWithTag("home-test-viewport").fetchSemanticsNode()
        val home = compose.onNodeWithTag("home-test-canvas").fetchSemanticsNode()
        val viewportLeft = viewport.positionInRoot.x
        val viewportRight = viewportLeft + viewport.size.width
        val homeLeft = home.positionInRoot.x
        val homeRight = homeLeft + home.size.width
        // Raw layout pixels avoid clipped bounds and differences between the two local densities.
        assertTrue("Home exceeds visible width: home=[$homeLeft, $homeRight], viewport=[$viewportLeft, $viewportRight]",
            homeLeft >= viewportLeft - 0.5f && homeRight <= viewportRight + 0.5f)
    }

    private fun show(overview: OverviewResult) {
        compose.setContent { LithicTheme {
            // Counts from the supplied overview must win over stale compatibility arguments.
            HomeShell(99L, {}, {}, 99L, {}, {}, overview = overview)
        } }
    }

    private fun result(
        total: String = "0", project: String = "0", subscription: String = "0",
        projectCount: Long = 0L, subscriptionCount: Long = 0L,
        activeProjectCount: Long = 0L, activeSubscriptionCount: Long = 0L,
        projectCategories: List<CategoryDailyCost> = emptyList(),
        subscriptionCategories: List<CategoryDailyCost> = emptyList(),
    ) = OverviewResult(LocalDate.of(2026, 1, 20), BigDecimal(total), BigDecimal(project), BigDecimal(subscription),
        projectCount, subscriptionCount, activeProjectCount, activeSubscriptionCount,
        projectCategories, subscriptionCategories)
}
