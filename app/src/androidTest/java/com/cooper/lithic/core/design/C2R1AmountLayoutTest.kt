package com.cooper.lithic.core.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** These are compile-only until an independently authorized Android run. */
class C2R1AmountLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test fun normal_360dp_long_negative_is_one_complete_line_with_full_unit_description() {
        show("-123456789.01", 1f, 328)
        verify("−¥123456789.01", "负123456789.01元/天")
    }
    @Test fun font_scale_two_keeps_the_entire_amount_without_decimal_or_sign_wrapping() {
        show("-123456789.01", 2f, 328)
        verify("−¥123456789.01", "负123456789.01元/天")
    }
    @Test fun overflow_has_discoverable_horizontal_view_and_never_ellipsis() {
        show("-92233720368547758.07", 2f, 120)
        verify("−¥92233720368547758.07", "负92233720368547758.07元/天")
        compose.onNodeWithTag("amount-horizontal-hint", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("amount-horizontal-view", useUnmergedTree = true).assertHeightIsAtLeast(48.dp).performTouchInput { swipeLeft() }
        verify("−¥92233720368547758.07", "负92233720368547758.07元/天")
    }
    private fun show(raw: String, scale: Float, width: Int) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                LithicTheme { Box(Modifier.width(width.dp)) { AmountText(BigDecimal(raw), Modifier.testTag("r1-amount")) } }
            }
        }
    }
    private fun verify(text: String, description: String) {
        compose.onNodeWithTag("r1-amount").assertContentDescriptionEquals(description)
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.single().lineCount)
        assertEquals(text, layouts.single().layoutInput.text.text)
        assertFalse(layouts.single().didOverflowWidth)
    }
}
