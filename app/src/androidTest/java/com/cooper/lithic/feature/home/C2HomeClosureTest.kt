package com.cooper.lithic.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.domain.report.OverviewResult
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Synthetic production-Compose assertions, compiled only; Android execution remains NOT_RUN. */
class C2HomeClosureTest {
    @get:Rule val compose = createComposeRule()
    private var pixelsPerDp = 1f

    @Test fun count_cards_use_total_snapshot_counts_and_keep_costs_behind_disclosure() {
        show(snapshot())
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("8")
        compose.onNodeWithTag("home-subscription-record-count").assertTextEquals("5")
        compose.onNodeWithTag("home-active-project-count").assertDoesNotExist()
        compose.onNodeWithTag("home-project-rate").assertDoesNotExist()
        compose.onNodeWithTag("home-subscription-rate").assertDoesNotExist()
        compose.onNodeWithTag("home-category-disclosure").performScrollTo().performClick()
        compose.onNodeWithTag("home-project-rate").performScrollTo().assertTextContains("¥4.31")
        compose.onNodeWithTag("home-subscription-rate").performScrollTo().assertTextContains("¥2.17")
        compose.onNodeWithTag("home-active-project-count").performScrollTo().assertTextEquals("当前有效 2")
        compose.onNodeWithTag("home-active-subscription-count").performScrollTo().assertTextEquals("当前有效 1")
        compose.onNodeWithTag("home-cost-information").performScrollTo().performClick()
        compose.onNodeWithTag("home-count-scope").assertTextContains("历史", substring = true)
    }

    @Test fun unknown_snapshot_never_uses_compatibility_counts_or_invents_zero() {
        show(null)
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("—")
        compose.onNodeWithTag("home-subscription-record-count").assertTextEquals("—")
        compose.onNodeWithTag("home-total-rate").assertTextContains("—")
        compose.onNodeWithTag("home-first-record").assertDoesNotExist()
    }

    @Test fun failed_read_retains_counts_with_a_persistent_old_result_notice() {
        show(snapshot(), retained = true)
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("8")
        compose.onNodeWithTag("home-subscription-record-count").assertTextEquals("5")
        compose.onNodeWithTag("home-retained-notice").assertExists().assertTextContains("上次", substring = true)
        compose.onNodeWithTag("home-first-record").assertDoesNotExist()
    }

    @Test fun sole_fab_opens_original_choices_and_dismissal_has_no_create_side_effect() {
        val actions = mutableListOf<String>()
        compose.setContent { LithicTheme {
            HomeShell(99, {}, { actions += "project" }, 99, {}, { actions += "subscription" }, overview = snapshot())
        } }
        compose.onAllNodesWithTag("home-open-add").assertCountEquals(1)
        compose.onNodeWithTag("home-first-record").assertDoesNotExist()
        compose.onNodeWithTag("home-open-add").assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-create-project").assertExists()
        compose.onNodeWithTag("home-create-subscription").assertExists()
        pressBack()
        compose.waitForIdle()
        assertEquals(emptyList<String>(), actions)
        compose.onNodeWithTag("home-open-add").assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-create-subscription").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(listOf("subscription"), actions)
    }

    @Test fun count_scope_is_local_until_full_calculation_explanation_is_requested() {
        var navigationRequests = 0
        compose.setContent { LithicTheme {
            HomeShell(99, {}, {}, 99, {}, {}, overview = snapshot(),
                onCalculationExplanation = { navigationRequests++ })
        } }
        compose.onNodeWithTag("home-cost-information").performScrollTo().performClick()
        compose.onNodeWithTag("home-count-scope").assertTextContains("历史", substring = true)
        compose.runOnIdle { assertEquals(0, navigationRequests) }
        pressBack()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, navigationRequests) }
        compose.onNodeWithTag("home-cost-information").performScrollTo().performClick()
        compose.onNodeWithTag("home-full-calculation-explanation").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, navigationRequests) }
    }

    @Test fun normal_font_360dp_count_cards_share_a_row_and_fab_is_in_home_bounds() {
        showAt360dp(fontScale = 1f)
        compose.onNodeWithTag("home-record-counts").performScrollTo()
        val project = compose.onNodeWithTag("home-project-count-card").fetchSemanticsNode()
        val subscription = compose.onNodeWithTag("home-subscription-count-card").fetchSemanticsNode()
        assertEquals(project.positionInRoot.y, subscription.positionInRoot.y, 1f)
        assertTrue(subscription.positionInRoot.x >= project.positionInRoot.x + project.size.width)
        assertFabInContent()
    }

    @Test fun local_200_percent_font_stacks_counts_without_moving_fab_outside_content() {
        showAt360dp(fontScale = 2f)
        compose.onNodeWithTag("home-subscription-count-card").performScrollTo()
        val project = compose.onNodeWithTag("home-project-count-card").fetchSemanticsNode()
        val subscription = compose.onNodeWithTag("home-subscription-count-card").fetchSemanticsNode()
        assertTrue(subscription.positionInRoot.y >= project.positionInRoot.y + project.size.height)
        assertEquals(project.positionInRoot.x, subscription.positionInRoot.x, 1f)
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("8")
        compose.onNodeWithTag("home-subscription-record-count").assertTextEquals("5")
        assertFabInContent()
    }

    private fun assertFabInContent() {
        val home = compose.onNodeWithTag("closure-home-canvas").fetchSemanticsNode()
        val fab = compose.onNodeWithTag("home-open-add").assertIsDisplayed().fetchSemanticsNode()
        // C9 uses one trailing content inset, rather than shrinking the entire scroll viewport.
        compose.onNodeWithTag("home-screen").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        compose.waitForIdle()
        val lastAction = compose.onNodeWithTag("home-category-disclosure").assertIsDisplayed().fetchSemanticsNode()
        assertTrue("The final content action must scroll above the floating action",
            lastAction.boundsInRoot.bottom <= fab.boundsInRoot.top + 1f)
        val right = home.positionInRoot.x + home.size.width
        val bottom = home.positionInRoot.y + home.size.height
        assertEquals(56f * pixelsPerDp, fab.size.width.toFloat(), 1f)
        assertEquals(56f * pixelsPerDp, fab.size.height.toFloat(), 1f)
        assertEquals(16f * pixelsPerDp, right - fab.positionInRoot.x - fab.size.width, 1f)
        assertEquals(16f * pixelsPerDp, bottom - fab.positionInRoot.y - fab.size.height, 1f)
    }

    private fun showAt360dp(fontScale: Float) {
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = constraints.maxWidth / 360f
                val canvasHeight = minOf(800f, constraints.maxHeight / density).dp
                pixelsPerDp = density
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                    LithicTheme {
                        Box(Modifier.requiredSize(360.dp, canvasHeight).testTag("closure-home-canvas")) {
                            HomeShell(99, {}, {}, 99, {}, {}, overview = snapshot())
                        }
                    }
                }
            }
        }
    }

    private fun show(overview: OverviewResult?, retained: Boolean = false) {
        compose.setContent { LithicTheme {
            HomeShell(99, {}, {}, 99, {}, {}, overview = overview, hasReadNotice = retained)
        } }
    }

    private fun snapshot() = OverviewResult(LocalDate.of(2026, 10, 26), BigDecimal("6.48"),
        BigDecimal("4.31"), BigDecimal("2.17"), 8, 5, 2, 1, emptyList(), emptyList())
}
