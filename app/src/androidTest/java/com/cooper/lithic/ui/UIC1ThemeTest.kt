package com.cooper.lithic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.EntryCard
import com.cooper.lithic.core.design.LithicTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Visual contracts for the approved three-page sample; no production ledger is opened. */
@RunWith(AndroidJUnit4::class)
class UIC1ThemeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dual_theme_uses_distinct_heading_roles_and_a_38sp_primary_figure() {
        var dark by mutableStateOf(false)
        var captured: Typography? = null
        compose.setContent {
            LithicTheme(darkTheme = dark) {
                val typography = MaterialTheme.typography
                SideEffect { captured = typography }
                Surface { Text("页面标题", style = typography.headlineMedium) }
            }
        }
        for (night in listOf(false, true)) {
            compose.runOnIdle { dark = night }
            compose.waitForIdle()
            compose.runOnIdle {
                val typography = requireNotNull(captured)
                assertEquals(38.sp, typography.displayLarge.fontSize)
                assertEquals(46.sp, typography.displayLarge.lineHeight)
                assertEquals(28.sp, typography.headlineLarge.fontSize)
                assertEquals(24.sp, typography.headlineMedium.fontSize)
                assertEquals(20.sp, typography.titleLarge.fontSize)
                assertEquals(18.sp, typography.titleMedium.fontSize)
                assertEquals(16.sp, typography.bodyLarge.fontSize)
                assertEquals(14.sp, typography.bodyMedium.fontSize)
            }
            compose.onNodeWithText("页面标题").assertIsDisplayed()
        }
    }

    @Test
    fun dual_theme_uses_warm_page_and_ink_green_actions_with_readable_pairs() {
        var dark by mutableStateOf(false)
        var captured: ColorScheme? = null
        compose.setContent {
            LithicTheme(darkTheme = dark) {
                val colors = MaterialTheme.colorScheme
                SideEffect { captured = colors }
                Surface { Text("真实内容") }
            }
        }
        for (night in listOf(false, true)) {
            compose.runOnIdle { dark = night }
            compose.waitForIdle()
            compose.runOnIdle {
                val colors = requireNotNull(captured)
                assertEquals(if (night) Color(0xFF111815) else Color(0xFFF6F5F1), colors.background)
                assertEquals(if (night) Color(0xFF9EBCAA) else Color(0xFF365F4A), colors.primary)
                val textPairs = listOf(
                    colors.onBackground to colors.background,
                    colors.onSurface to colors.surface,
                    colors.onSurfaceVariant to colors.surface,
                    colors.onPrimary to colors.primary,
                    colors.primary to colors.background,
                    colors.primary to colors.surface,
                    colors.onPrimaryContainer to colors.primaryContainer,
                    colors.onSecondary to colors.secondary,
                    colors.onSecondaryContainer to colors.secondaryContainer,
                    colors.onTertiary to colors.tertiary,
                    colors.onTertiaryContainer to colors.tertiaryContainer,
                    colors.error to colors.surface,
                    colors.onError to colors.error,
                    colors.onErrorContainer to colors.errorContainer,
                    colors.inverseOnSurface to colors.inverseSurface,
                    colors.inversePrimary to colors.inverseSurface,
                )
                textPairs.forEach { (foreground, background) ->
                    assertTrue("Text pair must reach 4.5:1: $foreground on $background",
                        contrast(foreground, background) >= 4.5f)
                }
                listOf(colors.surfaceContainerLowest, colors.surfaceContainerLow, colors.surfaceContainer,
                    colors.surfaceContainerHigh, colors.surfaceContainerHighest).forEach { container ->
                    assertTrue("Card text must remain readable", contrast(colors.onSurface, container) >= 4.5f)
                    assertTrue("Secondary card text must remain readable", contrast(colors.onSurfaceVariant, container) >= 4.5f)
                }
                assertTrue("Visible control boundary must reach 3:1", contrast(colors.outline, colors.surface) >= 3f)
            }
        }
    }

    @Test
    fun ordinary_card_aligns_name_status_and_time_beside_its_thumbnail() {
        compose.setContent {
            LithicTheme {
                Surface {
                    Box(Modifier.width(360.dp).padding(16.dp)) {
                        EntryCard("日常项目", "持有中", "已持有365天", BigDecimal("3.20"))
                    }
                }
            }
        }
        val name = compose.onNodeWithText("日常项目", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val status = compose.onNodeWithText("持有中", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val time = compose.onNodeWithText("已持有365天", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val badgeInset = with(compose.density) { 8.dp.toPx() }
        assertTrue("Status pill belongs to the name column, including its 8dp inner inset",
            status.left >= name.left && status.left <= name.left + badgeInset + 1f)
        assertEquals("Time belongs to the same text column as the name", name.left, time.left, 1f)
        assertTrue("Status follows the name", status.top >= name.bottom)
        assertTrue("Time follows the status", time.top >= status.bottom)
    }

    @Test
    fun compact_cards_keep_long_negative_subcent_zero_and_missing_values_distinct() {
        val name = "用于窄屏检验的很长中文项目名称以及完整持有状态"
        val amount = "−¥92233720368547758.07"
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                LithicTheme {
                    Surface(Modifier.fillMaxSize()) {
                        Box(Modifier.width(360.dp)) {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                                EntryCard(name, "已结束", "结果截至2026-10-12", BigDecimal("-92233720368547758.07"))
                                EntryCard("微小负值", "持有中", "已持有365天", BigDecimal("-0.005"))
                                EntryCard("真实零值", "持有中", "已持有1天", BigDecimal.ZERO)
                                EntryCard("尚未开始的项目", "尚未开始", "开始：2026-11-01", null)
                            }
                        }
                    }
                }
            }
        }
        assertTextFits(name)
        assertTextFits(amount)
        compose.onNodeWithContentDescription("负值，绝对值小于0.01元/天")
            .performScrollTo().assertIsDisplayed().assert(hasText("−<¥0.01"))
        compose.onNodeWithContentDescription("0.00元/天")
            .performScrollTo().assertIsDisplayed().assert(hasText("¥0.00"))
        compose.onNodeWithContentDescription("暂无数据")
            .performScrollTo().assertIsDisplayed().assert(hasText("—"))
            .assertContentDescriptionEquals("暂无数据")
    }

    private fun assertTextFits(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
        assertTrue("Text must expose its actual layout", layouts.isNotEmpty())
        assertFalse("Full value must remain visible without clipping or ellipsis", layouts.single().hasVisualOverflow)
    }

    private fun contrast(foreground: Color, background: Color): Float {
        val first = foreground.luminance()
        val second = background.luminance()
        return (maxOf(first, second) + 0.05f) / (minOf(first, second) + 0.05f)
    }
}
