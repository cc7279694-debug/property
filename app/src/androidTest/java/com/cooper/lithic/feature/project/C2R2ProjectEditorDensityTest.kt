package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Desktop compile-only. This is not evidence of system font/IME/device layout. */
class C2R2ProjectEditorDensityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compact_summary_opens_the_original_date_input_without_changing_dates_or_submitting() {
        var dateWrites = 0
        var saves = 0
        val state = ProjectEditorUiState(projectId = "r2-editor-density-synthetic", name = "书桌",
            startedOn = "2026-01-02", initialPaidOn = "2026-01-02", initialAmount = "0",
            categoryId = LithicDatabase.PROJECT_FALLBACK_ID,
            categories = listOf(CategoryEntity(LithicDatabase.PROJECT_FALLBACK_ID, CategoryType.PROJECT, "其他", "其他", true)),
            today = LocalDate.of(2026, 1, 2))
        compose.setContent { LithicTheme { Box(Modifier.requiredSize(360.dp, 800.dp)) {
            ProjectEditorScreen(state, {}, {}, { dateWrites++ }, {}, {}, {}, {}, { saves++ }, {})
        } } }
        compose.onNodeWithTag("project-category").performScrollTo()
        val category = compose.onNodeWithTag("project-category").fetchSemanticsNode().boundsInRoot
        assertTrue(category.height >= with(compose.density) { 48.dp.toPx() })
        assertTrue(category.height <= with(compose.density) { 64.dp.toPx() })
        compose.onNodeWithTag("project-started-on-summary").performScrollTo().performClick()
        compose.onNodeWithTag("project-started-on-input").performScrollTo().assertTextContains("2026-01-02")
        assertEquals(0, dateWrites)
        assertEquals(0, saves)
    }
}
