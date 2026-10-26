package com.cooper.lithic.feature.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Grouped links retain guarded caller callbacks; compile-only in this desktop task. */
class C2R2ReviewGroupingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compact_analysis_group_preserves_destination_callbacks_without_fabricating_a_chart() {
        val selected = mutableListOf<String>()
        compose.setContent { LithicTheme { Box(Modifier.requiredSize(360.dp, 800.dp)) {
            ReviewShell(2, subscriptionCount = 1,
                onCategoryAnalysis = { selected += "category" },
                onAnnualReview = { selected += "annual" },
                onLongTermReview = { selected += "long-term" })
        } } }
        compose.onNodeWithTag("review-analysis-group").assertIsDisplayed()
        compose.onNodeWithTag("review-category-analysis").performClick()
        compose.onNodeWithTag("review-annual").performClick()
        compose.onNodeWithTag("review-long-term").performClick()
        assertEquals(listOf("category", "annual", "long-term"), selected)
        compose.onNodeWithTag("total-trend-canvas").assertDoesNotExist()
        compose.onNodeWithTag("review-trend-unavailable").assertIsDisplayed()
    }
}
