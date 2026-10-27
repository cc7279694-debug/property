package com.cooper.lithic.core.design

import androidx.compose.ui.graphics.Color

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/** Source/compilation coverage only until separately authorized Android execution. */
class C5MoneyMetricTest {
    @get:Rule val compose = createComposeRule()

    @Test fun daily_amount_and_smaller_unit_are_one_complete_baseline_and_one_spoken_readout() {
        compose.setContent { LithicTheme {
            Box(Modifier.width(328.dp)) {
                MoneyMetric(BigDecimal("0.666666"), Modifier.testTag("daily"), level = MoneyMetricLevel.HERO,
                    label = "历史平均日成本")
            }
        } }
        compose.onNodeWithTag("daily").assertContentDescriptionEquals("历史平均日成本，0.67元/天")
        compose.onAllNodes(hasContentDescription("历史平均日成本，0.67元/天")).assertCountEquals(1)
        val layout = layout("daily")
        assertEquals("¥0.67/天", layout.layoutInput.text.text.replace("\u2009", ""))
        assertEquals(1, layout.lineCount)
        assertEquals(38.sp, layout.layoutInput.style.fontSize)
        assertEquals(14.sp, layout.layoutInput.text.spanStyles.single().item.fontSize)
        assertFalse(layout.didOverflowWidth)
        // The parent is the single value node; the drawn child exposes no second value.
        compose.onAllNodesWithText("¥0.67", substring = true, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test fun cny_has_no_independent_yuan_line_and_keeps_the_complete_formatted_value() {
        compose.setContent { LithicTheme {
            MoneyMetric(BigDecimal("2"), Modifier.testTag("cny"), AmountUnit.YUAN, MoneyMetricLevel.DETAIL,
                label = "非现金折抵")
        } }
        compose.onNodeWithTag("cny").assertContentDescriptionEquals("非现金折抵，2.00元")
        assertEquals("¥2.00", layout("cny").layoutInput.text.text)
        compose.onAllNodesWithText("元", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun four_roles_keep_the_specified_preferred_sizes_for_short_values() {
        compose.setContent { LithicTheme {
            Column(Modifier.width(328.dp)) {
                MoneyMetricLevel.entries.forEach { level ->
                    MoneyMetric(BigDecimal("2"), Modifier.testTag(level.name), level = level)
                }
            }
        } }
        assertEquals(38.sp, layout("HERO").layoutInput.style.fontSize)
        assertEquals(28.sp, layout("DETAIL").layoutInput.style.fontSize)
        assertEquals(24.sp, layout("CARD").layoutInput.style.fontSize)
        assertEquals(18.sp, layout("ROW").layoutInput.style.fontSize)
        compose.onNodeWithTag("amount-open-full", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun font_scale_two_preserves_the_whole_negative_daily_group_and_accessible_full_entry() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                LithicTheme { Box(Modifier.width(120.dp)) {
                    MoneyMetric(BigDecimal("-92233720368547758.07"), Modifier.testTag("large"),
                        level = MoneyMetricLevel.HERO)
                } }
            }
        }
        val expected = "−¥92233720368547758.07/天"
        val first = layout("large")
        assertEquals(2f, first.layoutInput.density.fontScale)
        assertEquals(28.sp, first.layoutInput.style.fontSize)
        assertEquals(expected, first.layoutInput.text.text.replace("\u2009", ""))
        assertEquals(1, first.lineCount)
        assertFalse(first.didOverflowWidth)
        compose.onNodeWithTag("amount-horizontal-view", useUnmergedTree = true)
            .assertHeightIsAtLeast(48.dp).performTouchInput { swipeLeft() }
        compose.onNodeWithTag("amount-open-full", useUnmergedTree = true)
            .assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithTag("amount-full-value").assertContentDescriptionEquals("负92233720368547758.07元/天")
        assertEquals(expected, layout("amount-full-value").layoutInput.text.text.replace("\u2009", ""))
        compose.onNodeWithTag("amount-copy-full", useUnmergedTree = true).assertHeightIsAtLeast(48.dp)
    }

    @Test fun full_value_uses_dialog_surface_colors_even_when_the_hero_was_white() {
        var expected = Color.Unspecified
        compose.setContent { LithicTheme(darkTheme = false) {
            expected = MaterialTheme.colorScheme.onSurface
            Box(Modifier.width(100.dp)) {
                MoneyMetric(BigDecimal("-123456789012345.67"), Modifier.testTag("hero-white"),
                    level = MoneyMetricLevel.HERO, color = Color.White, supportingColor = Color.White)
            }
        } }
        compose.onNodeWithTag("amount-open-full", useUnmergedTree = true).performClick()
        assertEquals(expected, layout("amount-full-value").layoutInput.style.color)
        compose.onNodeWithTag("amount-full-value").assertContentDescriptionEquals("负123456789012345.67元/天")
    }

    private fun layout(tag: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single()
    }
}
