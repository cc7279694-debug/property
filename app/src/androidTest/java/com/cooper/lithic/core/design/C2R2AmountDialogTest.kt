package com.cooper.lithic.core.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import java.math.BigDecimal
import org.junit.Rule
import org.junit.Test

/** Compile-only; no Android execution is authorized in A-R2. */
class C2R2AmountDialogTest {
    @get:Rule val compose = createComposeRule()
    @Test fun extreme_amount_can_be_expanded_in_full_and_dismissed_without_writes() {
        compose.setContent { LithicTheme { Box(Modifier.width(100.dp)) { AmountText(BigDecimal("-92233720368547758.07")) } } }
        compose.onNodeWithTag("amount-open-full", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("amount-full-value")
            .assertContentDescriptionEquals("负92233720368547758.07元/天")
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("amount-full-value").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals("−¥92233720368547758.07/天", layouts.single().layoutInput.text.text.replace("\u2009", ""))
        compose.onNodeWithTag("amount-close-full").performClick()
        compose.onNodeWithTag("amount-full-value", useUnmergedTree = true).assertDoesNotExist()
    }
    @Test fun ordinary_amount_keeps_the_primary_visual_without_an_unneeded_expand_action() {
        compose.setContent { LithicTheme { Box(Modifier.width(328.dp)) { AmountText(BigDecimal("6.48"), Modifier.testTag("ordinary-amount")) } } }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("ordinary-amount").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(38.sp, layouts.single().layoutInput.style.fontSize)
        compose.onNodeWithTag("amount-open-full", useUnmergedTree = true).assertDoesNotExist()
    }
}
