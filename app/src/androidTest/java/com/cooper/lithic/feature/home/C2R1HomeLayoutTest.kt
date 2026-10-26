package com.cooper.lithic.feature.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.domain.report.OverviewResult
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class C2R1HomeLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test fun verified_empty_home_has_one_add_action_and_no_duplicate_collection_sections() {
        compose.setContent { LithicTheme { HomeShell(0, {}, {}, 0, {}, {}, overview = snapshot()) } }
        compose.onNodeWithTag("home-first-record").assertDoesNotExist()
        compose.onAllNodesWithTag("home-open-add").assertCountEquals(1)
        compose.onNodeWithTag("home-open-add").assertIsDisplayed()
        compose.onAllNodesWithTag("home-empty-guidance").assertCountEquals(1)
        compose.onNodeWithTag("open-project-list").assertDoesNotExist()
        compose.onNodeWithTag("open-subscription-list").assertDoesNotExist()
        compose.onNodeWithTag("home-project-rate").assertDoesNotExist()
        compose.onNodeWithText("¥0.00", useUnmergedTree = true).assertDoesNotExist()
    }
    @Test fun history_without_current_cost_keeps_zero_and_history_context() {
        val state = snapshot().copy(projectCount = 3)
        compose.setContent { LithicTheme { HomeShell(3, {}, {}, 0, {}, {}, overview = state) } }
        compose.onNodeWithTag("home-first-record").assertDoesNotExist()
        compose.onNodeWithTag("home-no-active-records").assertExists()
        compose.onNodeWithTag("open-project-list").assertExists()
    }
    private fun snapshot() = OverviewResult(LocalDate.of(2026, 10, 26), BigDecimal.ZERO,
        BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0, 0, emptyList(), emptyList())
}
