package com.cooper.lithic.feature.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.pressBack
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.domain.report.OverviewResult
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Production Composable, synthetic facts only; desktop compilation is not device execution. */
class C2HomeSemanticsTest {
    @get:Rule val compose = createComposeRule()
    private fun overview() = OverviewResult(LocalDate.of(2026, 10, 26), BigDecimal.ZERO,
        BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0, 0, emptyList(), emptyList())

    @Test fun empty_add_sheet_has_two_choices_and_dismissal_does_not_create_a_record() {
        val actions = mutableListOf<String>()
        compose.setContent { LithicTheme { HomeShell(0, {}, { actions += "project" }, 0, {},
            { actions += "subscription" }, overview = overview()) } }
        compose.onNodeWithTag("home-open-add").assertDoesNotExist()
        compose.onNodeWithTag("home-first-record").performClick()
        compose.onNodeWithTag("home-create-project").assertExists()
        compose.onNodeWithTag("home-create-subscription").assertExists()
        pressBack()
        compose.waitForIdle()
        assertEquals(emptyList<String>(), actions)
    }

    @Test fun selecting_the_sheet_item_dispatches_only_the_original_create_callback() {
        val actions = mutableListOf<String>()
        compose.setContent { LithicTheme { HomeShell(0, {}, { actions += "project" }, 0, {},
            { actions += "subscription" }, overview = overview()) } }
        compose.onNodeWithTag("home-first-record").performClick()
        compose.onNodeWithTag("home-create-subscription").performClick()
        compose.waitForIdle()
        assertEquals(listOf("subscription"), actions)
    }

    @Test fun failed_read_after_an_empty_snapshot_does_not_claim_current_emptiness() {
        compose.setContent { LithicTheme { HomeShell(0, {}, {}, 0, {}, {}, overview = overview(), hasReadNotice = true) } }
        compose.onNodeWithText("当前记录状态尚未确认").assertExists()
        compose.onNodeWithTag("home-first-record").assertDoesNotExist()
        compose.onNodeWithTag("home-retained-notice").assertExists()
    }
}
