package com.cooper.lithic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.feature.appearance.LithicAppearance
import com.cooper.lithic.feature.home.HomeShell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Source only. Synthetic facts; no storage, bitmap capture or device configuration changes. */
class C9R1HomeHierarchyTest {
    @get:Rule val compose = createComposeRule()

    @Test fun real_records_precede_auxiliary_counts_in_three_themes_at_small_large_type() {
        val facts = C2VisualFixtures(true)
        val appearance = mutableStateOf(LithicAppearance.STONE_MIST)
        var writes = 0
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val scale = constraints.maxWidth / 360f
                val height = minOf(800f, constraints.maxHeight / scale).dp
                CompositionLocalProvider(LocalDensity provides Density(scale, 2f)) {
                    LithicTheme(appearance = appearance.value) {
                        Box(Modifier.requiredSize(360.dp, height)) {
                            HomeShell(1, {}, { writes++ }, 1, {}, { writes++ }, overview = facts.overview,
                                projectPreview = facts.projects, subscriptionPreview = facts.subscriptions)
                        }
                    }
                }
            }
        }
        LithicAppearance.entries.forEach { selected ->
            compose.runOnIdle { appearance.value = selected }
            val record = compose.onNodeWithTag("home-project-preview-${facts.projects.items.single().project.id}")
            record.performScrollTo().assertIsDisplayed()
            val row = record.fetchSemanticsNode()
            val counts = compose.onNodeWithTag("home-record-counts").fetchSemanticsNode()
            assertTrue("Auxiliary counts must follow real record content",
                counts.positionInRoot.y >= row.positionInRoot.y + row.size.height)
            compose.onNodeWithTag("home-project-record-count").assertTextEquals("1")
            compose.onNodeWithTag("home-subscription-record-count").assertTextEquals("1")
            compose.onNodeWithTag("home-category-disclosure").performScrollTo().performClick()
            compose.onNodeWithTag("home-project-rate").performScrollTo().assertTextContains("¥")
            compose.onNodeWithTag("home-subscription-rate").performScrollTo().assertTextContains("¥")
            compose.onNodeWithTag("home-category-disclosure").performScrollTo().performClick()
            compose.onAllNodesWithTag("home-open-add").assertCountEquals(1)
            compose.runOnIdle { assertEquals(0, writes) }
        }
    }
}
