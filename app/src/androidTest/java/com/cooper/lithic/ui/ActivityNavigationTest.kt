package com.cooper.lithic.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.app.LithicApplication
import com.cooper.lithic.app.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

/** Actual Activity recreation and system dispatcher; process death is tested separately on-device. */
@RunWith(AndroidJUnit4::class)
class ActivityNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun activity_recreation_preserves_a_legal_selected_route_and_same_storage_owner() = runBlocking {
        ready()
        val owner = (compose.activity.application as LithicApplication).appContainer
        val generation = owner.openStorage()
        compose.onNodeWithTag("tab-settings").performClick()
        compose.onNodeWithTag("settings-screen").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("settings-screen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("settings-screen").assertIsDisplayed()
        assertSame(generation, (compose.activity.application as LithicApplication).appContainer.openStorage())
    }

    @Test fun system_back_returns_to_home_then_finishes_the_actual_activity() {
        ready()
        compose.onNodeWithTag("tab-review").performClick()
        compose.onNodeWithTag("review-screen").assertIsDisplayed()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("home-screen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(10_000) { compose.activityRule.scenario.state == Lifecycle.State.DESTROYED }
        assertEquals(Lifecycle.State.DESTROYED, compose.activityRule.scenario.state)
    }

    private fun ready() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes().isNotEmpty() }
    }
}
