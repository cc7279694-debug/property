package com.cooper.lithic.feature.review

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Actual production Compose; prepared LocalDensity cases are not Android runtime evidence. */
class C2ReviewEmptyCompositionTest {
    @get:Rule val compose = createComposeRule()
    private var pixelsPerDp = 1f

    @Test fun dark_400dp_empty_has_bounded_vector_identity_without_a_full_width_card() {
        var homeRequests = 0
        show(400f, 1f) { homeRequests++ }
        compose.onNodeWithTag("review-empty-state").assertIsDisplayed()
        val identity = compose.onNodeWithTag("review-empty-identity").assertIsDisplayed().fetchSemanticsNode()
        assertEquals(112f * pixelsPerDp, identity.size.width.toFloat(), 1.5f)
        assertEquals(88f * pixelsPerDp, identity.size.height.toFloat(), 1.5f)
        val region = compose.onNodeWithTag("review-empty-state").fetchSemanticsNode()
        assertTrue("Empty content should have bounded proportions, not consume a full viewport",
            region.size.height <= 300f * pixelsPerDp)
        val pixels = compose.onNodeWithTag("review-empty-state").captureToImage().toPixelMap()
        val corner = pixels[minOf(2, pixels.width - 1), minOf(2, pixels.height - 1)]
        val page = Color(0xFF111815)
        assertEquals("No opaque rectangle behind the complete empty region", page.red, corner.red, 0.02f)
        assertEquals(page.green, corner.green, 0.02f)
        assertEquals(page.blue, corner.blue, 0.02f)
        compose.onAllNodesWithTag("review-record-summary").assertCountEquals(1)
        compose.onAllNodesWithTag("review-add-record").assertCountEquals(1)
        compose.onNodeWithTag("review-add-record").assertTextContains("去首页记录").assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, homeRequests) }
        assertNoInventedFacts()
    }

    @Test fun local_large_font_360dp_grows_naturally_and_keeps_the_only_action_reachable() {
        show(360f, 2f)
        val canvas = compose.onNodeWithTag("c2-review-canvas").fetchSemanticsNode().boundsInRoot
        for (tag in listOf("review-record-summary", "review-add-record")) {
            val bounds = compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.left >= canvas.left - 1f)
            assertTrue(bounds.right <= canvas.right + 1f)
        }
        compose.onNodeWithTag("review-add-record").assertHeightIsAtLeast(48.dp)
        compose.onAllNodesWithTag("review-add-record").assertCountEquals(1)
        assertNoInventedFacts()
    }

    private fun assertNoInventedFacts() {
        listOf("total-trend-canvas", "total-trend-total", "total-trend-empty", "review-analysis-group",
            "review-archive-group", "review-full-history-group").forEach {
            compose.onNodeWithTag(it).assertDoesNotExist()
        }
    }

    private fun show(widthDp: Float, fontScale: Float, onHome: () -> Unit = {}) {
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                pixelsPerDp = constraints.maxWidth / widthDp
                val height = minOf(800f, constraints.maxHeight / pixelsPerDp).dp
                CompositionLocalProvider(LocalDensity provides Density(pixelsPerDp, fontScale)) {
                    LithicTheme(darkTheme = true) {
                        Box(Modifier.requiredSize(widthDp.dp, height).background(MaterialTheme.colorScheme.background)
                            .testTag("c2-review-canvas")) {
                            ReviewShell(0, subscriptionCount = 0, onAddRecord = onHome,
                                onProjectHistory = {}, onSubscriptionHistory = {}, onCategoryAnalysis = {},
                                onAnnualReview = {}, onLongTermReview = {}, onTotalCostTrend = {})
                        }
                    }
                }
            }
        }
    }
}
