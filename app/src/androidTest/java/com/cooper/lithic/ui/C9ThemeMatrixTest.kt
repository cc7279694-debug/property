package com.cooper.lithic.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cooper.lithic.app.C2PrimaryNavigationBar
import com.cooper.lithic.app.Home
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.feature.appearance.*
import com.cooper.lithic.feature.home.HomeShell
import com.cooper.lithic.feature.project.ProjectDetailScreen
import com.cooper.lithic.feature.project.ProjectDetailUiState
import com.cooper.lithic.feature.subscription.SubscriptionDetailScreen
import com.cooper.lithic.feature.subscription.SubscriptionDetailUiState
import com.cooper.lithic.feature.review.ReviewShell
import com.cooper.lithic.feature.settings.SettingsShell
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** 15 production-component positions; androidTest-only immutable facts, no Room or Owner.
 * NOT_RUN. No PNGs, device font changes, C3 capture entry, or visual acceptance are produced.
 * A future execution still requires an independently authorized plain-Application Runner. */
class C9ThemeMatrixTest {
    @get:Rule val compose = createComposeRule()
    private enum class Page { HOME, PROJECT, SUBSCRIPTION, REVIEW, SETTINGS }

    @Test fun home_has_three_fixed_theme_positions() = matrix(Page.HOME, "home-screen")
    @Test fun project_detail_has_three_fixed_theme_positions() = matrix(Page.PROJECT, "project-detail-screen")
    @Test fun subscription_detail_has_three_fixed_theme_positions() = matrix(Page.SUBSCRIPTION, "subscription-detail-screen")
    @Test fun review_has_three_fixed_theme_positions() = matrix(Page.REVIEW, "review-screen")
    @Test fun settings_has_three_fixed_theme_positions() = matrix(Page.SETTINGS, "settings-screen")

    private fun matrix(page: Page, tag: String) {
        val facts = C2VisualFixtures(populated = true)
        val theme = mutableStateOf(LithicAppearance.STONE_MIST)
        var actions = 0
        compose.setContent {
            LithicTheme(appearance = theme.value) {
                CompositionLocalProvider(LocalAppearanceActions provides AppearanceActions(
                    AppearanceUiState(selected = theme.value, loading = false, readable = true), { theme.value = it }, {})) {
                    Surface(Modifier.fillMaxSize()) {
                        when (page) {
                            Page.HOME -> HomeShell(1, { actions++ }, { actions++ }, 1, { actions++ }, { actions++ },
                                overview = facts.overview, projectPreview = facts.projects, subscriptionPreview = facts.subscriptions)
                            Page.PROJECT -> ProjectDetailScreen(ProjectDetailUiState(facts.projects.items.single(), loading = false),
                                { actions++ }, { actions++ }, { actions++ }, { actions++ })
                            Page.SUBSCRIPTION -> SubscriptionDetailScreen(SubscriptionDetailUiState(
                                facts.subscriptions.items.single(), loading = false),
                                { actions++ }, { actions++ }, { actions++ }, { actions++ })
                            Page.REVIEW -> ReviewShell(1, subscriptionCount = 1, trendState = facts.trend)
                            Page.SETTINGS -> SettingsShell(ZoneId.of("Asia/Shanghai"))
                        }
                    }
                }
            }
        }
        LithicAppearance.entries.forEach { selected ->
            compose.runOnIdle { theme.value = selected }
            compose.onNodeWithTag(tag).assertExists()
            if (page == Page.HOME || page == Page.PROJECT) {
                val canvas = if (page == Page.HOME) "home-content-area" else tag
                val expectedCanvas = when (selected) {
                    LithicAppearance.CHARCOAL -> Color(0xFF121212)
                    LithicAppearance.PURE_WHITE -> Color.White
                    LithicAppearance.STONE_MIST -> Color(0xFFEDF2F7)
                }
                assertSurfaceColor(canvas, expectedCanvas)
                if (page == Page.HOME) {
                    assertSurfaceColor("home-current-cost-summary",
                        if (selected == LithicAppearance.CHARCOAL) Color(0xFF1E1E1E) else Color.White)
                }
            }
            compose.runOnIdle { assertEquals(0, actions) }
        }
    }

    @Test fun primary_navigation_uses_a_theme_surface_beneath_the_selected_tab() {
        val theme = mutableStateOf(LithicAppearance.STONE_MIST)
        compose.setContent { LithicTheme(appearance = theme.value) { C2PrimaryNavigationBar(Home) {} } }
        val expected = mapOf(
            LithicAppearance.CHARCOAL to Color(0xFF1E1E1E),
            LithicAppearance.PURE_WHITE to Color(0xFFF7F8FA),
            LithicAppearance.STONE_MIST to Color(0xFFE8EFF6),
        )
        expected.forEach { (appearance, color) ->
            compose.runOnIdle { theme.value = appearance }
            compose.onNodeWithTag("tab-now").assertIsSelected()
            assertSurfaceColor("tab-now", color)
        }
    }

    private fun assertSurfaceColor(tag: String, expected: Color) {
        val pixels = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
        // Left of centrally aligned content and well below rounded card corners or the navigation divider.
        val sample = pixels[minOf(3, pixels.width - 1), pixels.height / 2]
        assertEquals("$tag, red", expected.red, sample.red, 0.02f)
        assertEquals("$tag, green", expected.green, sample.green, 0.02f)
        assertEquals("$tag, blue", expected.blue, sample.blue, 0.02f)
    }

    @Test fun settings_offers_exactly_three_named_choices_and_keeps_the_panel_open_on_switch() {
        val theme = mutableStateOf(LithicAppearance.STONE_MIST)
        compose.setContent {
            LithicTheme(appearance = theme.value) {
                CompositionLocalProvider(LocalAppearanceActions provides AppearanceActions(
                    AppearanceUiState(selected = theme.value, loading = false, readable = true), { theme.value = it }, {})) {
                    SettingsShell(ZoneId.of("Asia/Shanghai"))
                }
            }
        }
        compose.onNodeWithTag("appearance-open").performClick()
        LithicAppearance.entries.forEach { selected ->
            compose.onNodeWithTag("appearance-${selected.storageId}").performScrollTo().performClick()
            compose.onNodeWithTag("appearance-${selected.storageId}").assertIsSelected()
            compose.onNodeWithTag("appearance-sheet").assertExists()
        }
    }
}
