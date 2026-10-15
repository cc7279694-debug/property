package com.cooper.lithic.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.app.HomePreview
import com.cooper.lithic.core.design.LithicTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UIC1RefinedHomeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun overview_has_two_separate_real_numeric_metrics_with_history_scope() {
        compose.setContent { LithicTheme { HomeShell(12, {}, {}, 3, {}, {}) } }
        val projects = compose.onNodeWithTag("home-project-count").assertIsDisplayed()
            .assertContentDescriptionEquals("已录入 12 条长期项目").fetchSemanticsNode().boundsInRoot
        val subscriptions = compose.onNodeWithTag("home-subscription-count").assertIsDisplayed()
            .assertContentDescriptionEquals("已录入 3 条订阅服务").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("12", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("3", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(projects.top, subscriptions.top)
        assertTrue(projects.right <= subscriptions.left)
        compose.onNodeWithText("数量包含历史与已停止记录").assertIsDisplayed()
        compose.onNodeWithText("当前日均成本").assertDoesNotExist()
    }

    @Test fun empty_home_has_one_hero_without_repeated_empty_sections() {
        compose.setContent { LithicTheme { HomeShell(0, {}, {}, 0, {}, {}) } }
        compose.onNodeWithTag("home-first-record").assertIsDisplayed()
        compose.onNodeWithTag("home-add-record").assertDoesNotExist()
        compose.onNodeWithText("我的长期项目").assertDoesNotExist()
        compose.onNodeWithText("我的订阅").assertDoesNotExist()
        compose.onNodeWithText("还没有长期项目").assertDoesNotExist()
        compose.onNodeWithText("还没有订阅服务").assertDoesNotExist()
        // Keep the original browse routes available without additional empty panels or create CTAs.
        compose.onNodeWithTag("open-project-list").assertIsDisplayed()
        compose.onNodeWithTag("open-subscription-list").assertIsDisplayed()
    }

    @Test fun narrow_double_font_add_action_is_measured_outside_scrollable_content() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                LithicTheme { Surface { Box(Modifier.width(360.dp).fillMaxHeight()) {
                    HomeShell(2, {}, {}, 1, {}, {},
                        preview = HomePreview(emptyList(), emptyList(), 0, 0, LocalDate.of(2026, 10, 15)))
                } } }
            }
        }
        val action = compose.onNodeWithTag("home-add-record").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val content = compose.onNodeWithTag("home-scroll-content").fetchSemanticsNode().boundsInRoot
        assertTrue("The add action must not overlay the scroll viewport", action.bottom <= content.top)
        compose.onNodeWithTag("home-subscription-history").performScrollTo().assertIsDisplayed()
        val after = compose.onNodeWithTag("home-add-record").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(after.bottom <= content.top)
    }
}
