package com.cooper.lithic.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.domain.report.OverviewResult
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Production Compose with synthetic snapshots. LocalDensity is not a system-font or device result. */
class C2C1HomeVisualContractTest {
    @get:Rule val compose = createComposeRule()
    private var pixelsPerDp = 1f

    @Test fun dark_verified_empty_360_keeps_green_card_compact_dash_and_real_zero_counts() {
        show(widthDp = 360, fontScale = 1f)
        assertVerifiedEmpty()
    }

    @Test fun dark_verified_empty_400_keeps_green_card_compact_dash_and_real_zero_counts() {
        show(widthDp = 400, fontScale = 1f)
        assertVerifiedEmpty()
    }

    @Test fun local_large_font_360_keeps_empty_structure_and_single_reachable_add() {
        show(widthDp = 360, fontScale = 2f)
        assertVerifiedEmpty()
    }

    @Test fun local_large_font_400_keeps_empty_structure_and_single_reachable_add() {
        show(widthDp = 400, fontScale = 2f)
        assertVerifiedEmpty()
    }

    @Test fun unknown_overview_does_not_borrow_compatibility_counts_or_show_verified_empty_action() {
        show(widthDp = 400, fontScale = 1f, overview = null, dark = false)
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("—")
        compose.onNodeWithTag("home-subscription-record-count").assertTextEquals("—")
        compose.onNodeWithTag("home-total-rate").assertTextContains("—")
        compose.onNodeWithTag("home-first-record").assertDoesNotExist()
        assertCompactAmount()
        assertGreenHero(Color(0xFF26362C))
    }

    @Test fun stale_empty_snapshot_is_labeled_and_does_not_claim_fresh_onboarding() {
        show(widthDp = 360, fontScale = 1f, retained = true)
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("0")
        compose.onNodeWithTag("home-retained-notice").assertExists().assertTextContains("上次", substring = true)
        compose.onNodeWithText("当前记录状态尚未确认").assertExists()
        compose.onNodeWithTag("home-first-record").assertDoesNotExist()
    }

    @Test fun historical_records_with_no_current_contribution_keep_true_zero_cost() {
        show(widthDp = 400, fontScale = 1f, overview = snapshot().copy(projectCount = 3))
        compose.onNodeWithTag("home-total-rate").assertTextContains("¥0.00")
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("3")
        compose.onNodeWithTag("home-no-active-records").assertExists()
        compose.onNodeWithTag("home-first-record").assertDoesNotExist()
        assertCompactAmount()
    }

    @Test fun long_negative_large_font_retains_full_amount_access_and_separate_unit_space() {
        val amount = BigDecimal("-12345678901234567890.01")
        show(widthDp = 360, fontScale = 2f, overview = snapshot().copy(
            projectCount = 1, activeProjectCount = 1, totalRate = amount, projectRate = amount))
        compose.onNodeWithTag("home-total-rate").assertContentDescriptionEquals("负12345678901234567890.01元/天")
        assertCompactAmount()
        compose.onNodeWithTag("amount-open-full", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithTag("amount-full-value").assertTextEquals("−¥12345678901234567890.01")
        compose.onNodeWithTag("amount-full-unit").assertTextEquals("元/天")
    }

    private fun assertVerifiedEmpty() {
        compose.onNodeWithTag("home-total-rate").assertTextContains("—")
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("0")
        compose.onNodeWithTag("home-subscription-record-count").assertTextEquals("0")
        compose.onAllNodesWithTag("home-first-record").assertCountEquals(1)
        compose.onNodeWithTag("home-first-record").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("home-open-add").assertDoesNotExist()
        compose.onNodeWithTag("open-project-list").assertDoesNotExist()
        compose.onNodeWithTag("open-subscription-list").assertDoesNotExist()
        compose.onNodeWithText("¥0.00", useUnmergedTree = true).assertDoesNotExist()
        assertCompactAmount()
        assertGreenHero(Color(0xFF2D4135))
    }

    private fun assertCompactAmount() {
        compose.onNodeWithTag("home-current-cost-summary").performScrollTo()
        val amount = compose.onNodeWithTag("home-total-rate").fetchSemanticsNode().boundsInRoot
        val unit = compose.onNodeWithTag("home-total-unit").fetchSemanticsNode().boundsInRoot
        val hero = compose.onNodeWithTag("home-current-cost-summary").fetchSemanticsNode().boundsInRoot
        assertEquals("Unit follows the measured number, not the far edge of a weighted row",
            4f * pixelsPerDp, unit.left - amount.right, 1.5f)
        assertTrue(unit.right <= hero.right - 15f * pixelsPerDp)
        assertTrue(amount.left >= hero.left + 15f * pixelsPerDp)
        val canvas = compose.onNodeWithTag("c1-home-canvas").fetchSemanticsNode().boundsInRoot
        assertTrue(hero.left >= canvas.left && hero.right <= canvas.right + 1f)
    }

    private fun assertGreenHero(expected: Color) {
        compose.onNodeWithTag("home-current-cost-summary").performScrollTo()
        val image = compose.onNodeWithTag("home-current-cost-summary").captureToImage().toPixelMap()
        val sample = image[image.width / 2, minOf(image.height - 1, 2)]
        assertEquals(expected.red, sample.red, 0.02f)
        assertEquals(expected.green, sample.green, 0.02f)
        assertEquals(expected.blue, sample.blue, 0.02f)
    }

    private fun show(widthDp: Int, fontScale: Float, overview: OverviewResult? = snapshot(),
        dark: Boolean = true, retained: Boolean = false) {
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = constraints.maxWidth / widthDp.toFloat()
                val height = minOf(860f, constraints.maxHeight / density).dp
                pixelsPerDp = density
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                    LithicTheme(darkTheme = dark) {
                        Box(Modifier.requiredSize(widthDp.dp, height).background(MaterialTheme.colorScheme.background)
                            .testTag("c1-home-canvas")) {
                            HomeShell(99, {}, {}, 99, {}, {}, overview = overview, hasReadNotice = retained)
                        }
                    }
                }
            }
        }
    }

    private fun snapshot() = OverviewResult(LocalDate.of(2026, 10, 26), BigDecimal.ZERO,
        BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0, 0, emptyList(), emptyList())
}
