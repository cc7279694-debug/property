package com.cooper.lithic.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.cooper.lithic.app.HomePreview
import com.cooper.lithic.core.design.LithicTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Starts against the unchanged HomeShell API: missing UI fails as behavior, not compilation. */
class UIC1HomeSampleTest {
    @get:Rule val compose = createComposeRule()

    @Test fun recorded_overview_does_not_offer_an_unimplemented_cost_summary() {
        render(projects = 2, subscriptions = 1)
        compose.onNodeWithText("日均成本汇总尚未开放").assertDoesNotExist()
        compose.onNodeWithText("当前日均成本").assertDoesNotExist()
        compose.onNodeWithTag("home-record-overview").assertIsDisplayed()
        compose.onNodeWithText("已录入 2 条长期项目").assertIsDisplayed()
        compose.onNodeWithText("已录入 1 条订阅服务").assertIsDisplayed()
    }

    @Test fun successful_empty_home_has_one_first_record_action_instead_of_a_fab() {
        render(projects = 0, subscriptions = 0)
        compose.onNodeWithText("从第一条记录开始").assertIsDisplayed()
        compose.onNodeWithTag("home-first-record").assertIsDisplayed()
        compose.onNodeWithTag("home-add-record").assertDoesNotExist()
        compose.onNodeWithTag("home-create-project").assertDoesNotExist()
        compose.onNodeWithTag("home-create-subscription").assertDoesNotExist()
        compose.onNodeWithTag("home-first-record").performClick()
        compose.onNodeWithTag("add-record-sheet").assertIsDisplayed()
    }

    @Test fun dismissing_the_add_sheet_does_not_start_either_creation() {
        var projects = 0
        var subscriptions = 0
        render(2, 1, createProject = { projects++ }, createSubscription = { subscriptions++ })
        compose.onNodeWithTag("home-add-record").performClick()
        compose.onNodeWithTag("add-record-sheet").assertIsDisplayed()
        compose.onNodeWithTag("home-create-project").assertIsDisplayed()
        compose.onNodeWithTag("home-create-subscription").assertIsDisplayed()
        compose.onNodeWithTag("add-record-dismiss").performClick()
        compose.onNodeWithTag("add-record-sheet").assertDoesNotExist()
        assertEquals(0, projects)
        assertEquals(0, subscriptions)
    }

    @Test fun project_option_only_calls_the_original_project_creation_callback() {
        var projects = 0
        var subscriptions = 0
        render(2, 1, createProject = { projects++ }, createSubscription = { subscriptions++ })
        compose.onNodeWithTag("home-add-record").performClick()
        compose.onNodeWithTag("home-create-project").performClick()
        assertEquals(1, projects)
        assertEquals(0, subscriptions)
        compose.onNodeWithTag("add-record-sheet").assertDoesNotExist()
    }

    @Test fun dark_home_uses_the_same_subscription_creation_path() {
        var projects = 0
        var subscriptions = 0
        render(2, 1, dark = true, createProject = { projects++ }, createSubscription = { subscriptions++ })
        compose.onNodeWithTag("home-add-record").performClick()
        compose.onNodeWithTag("home-create-subscription").performClick()
        assertEquals(0, projects)
        assertEquals(1, subscriptions)
        compose.onNodeWithTag("add-record-sheet").assertDoesNotExist()
    }

    @Test fun refresh_in_progress_does_not_present_a_first_record_action() {
        compose.setContent { LithicTheme {
            HomeShell(0, {}, {}, 0, {}, {}, refreshing = true)
        } }
        compose.onNodeWithTag("home-refreshing").assertIsDisplayed()
        compose.onNodeWithTag("home-first-record").assertDoesNotExist()
        compose.onNodeWithText("从第一条记录开始").assertDoesNotExist()
    }

    @Test fun failed_read_does_not_turn_a_stale_zero_into_a_confirmed_empty_ledger() {
        compose.setContent { LithicTheme {
            HomeShell(0, {}, {}, 0, {}, {}, hasReadError = true)
        } }
        compose.onNodeWithTag("home-stale-data").assertIsDisplayed()
        compose.onNodeWithTag("home-first-record").assertDoesNotExist()
        compose.onNodeWithText("从第一条记录开始").assertDoesNotExist()
    }

    @Test fun history_only_counts_offer_both_history_routes_instead_of_first_record() {
        var projectHistory = 0
        var subscriptionHistory = 0
        compose.setContent { LithicTheme {
            HomeShell(1, {}, {}, 1, {}, {},
                preview = HomePreview(emptyList(), emptyList(), 0, 0, LocalDate.of(2026, 1, 25)),
                onProjectHistory = { projectHistory++ }, onSubscriptionHistory = { subscriptionHistory++ })
        } }
        compose.onNodeWithTag("home-first-record").assertDoesNotExist()
        compose.onNodeWithTag("home-add-record").assertIsDisplayed()
        compose.onNodeWithTag("home-project-history").performScrollTo().performClick()
        compose.onNodeWithTag("home-subscription-history").performScrollTo().performClick()
        assertEquals(1, projectHistory)
        assertEquals(1, subscriptionHistory)
    }

    private fun render(projects: Long, subscriptions: Long, dark: Boolean = false,
        createProject: () -> Unit = {}, createSubscription: () -> Unit = {}) {
        compose.setContent {
            LithicTheme(darkTheme = dark) {
                HomeShell(projects, {}, createProject, subscriptions, {}, createSubscription)
            }
        }
    }
}
