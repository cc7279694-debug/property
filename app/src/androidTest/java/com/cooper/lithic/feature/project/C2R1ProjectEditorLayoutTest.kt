package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Production editor at fixed test-host constraints; LocalDensity is not a system-font/IME test. */
class C2R1ProjectEditorLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ordinary_360dp_header_and_separate_label_have_their_own_space() {
        show(1f)
        assertHeaderAndLabelBounds()
        assertEndCanScrollAboveSave()
    }

    @Test fun large_font_360dp_header_does_not_compete_with_back_and_end_clears_save() {
        show(2f)
        assertHeaderAndLabelBounds()
        assertEndCanScrollAboveSave()
    }

    @Test fun hidden_note_error_expands_and_remains_reachable_above_measured_footer() {
        show(2f, noteError = true)
        compose.onNodeWithTag("project-note").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("请核对备注内容").performScrollTo().assertIsDisplayed()
        val error = compose.onNodeWithText("请核对备注内容").fetchSemanticsNode().boundsInRoot
        val footer = compose.onNodeWithTag("project-editor-save-bar").fetchSemanticsNode().boundsInRoot
        assertTrue("The actual error must be above the measured save region", error.bottom <= footer.top + 1f)
    }

    private fun show(scale: Float, noteError: Boolean = false) {
        val state = ProjectEditorUiState(projectId = "r1-layout-synthetic", name = "降噪耳机",
            startedOn = "2026-01-02", initialPaidOn = "2026-01-02", initialAmount = "0",
            categoryId = LithicDatabase.PROJECT_FALLBACK_ID,
            categories = listOf(CategoryEntity(LithicDatabase.PROJECT_FALLBACK_ID, CategoryType.PROJECT, "其他", "其他", true)),
            today = LocalDate.of(2026, 1, 2), errors = if (noteError) mapOf("note" to "请核对备注内容") else emptyMap())
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                LithicTheme {
                    Box(Modifier.requiredSize(360.dp, 800.dp)) {
                        ProjectEditorScreen(state, {}, {}, {}, {}, {}, {}, {}, {}, {})
                    }
                }
            }
        }
    }

    private fun assertHeaderAndLabelBounds() {
        val back = compose.onNodeWithTag("project-editor-back").fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithTag("project-editor-title").fetchSemanticsNode().boundsInRoot
        val field = compose.onNodeWithTag("project-name").fetchSemanticsNode().boundsInRoot
        val label = compose.onNodeWithText("名称").fetchSemanticsNode().boundsInRoot
        val minTarget = with(compose.density) { 48.dp.toPx() }
        val labelGap = with(compose.density) { 8.dp.toPx() }
        assertTrue(back.width + 1f >= minTarget && back.height + 1f >= minTarget)
        assertTrue("Title has its own row below back", title.top + 1f >= back.bottom)
        assertTrue("Title uses the complete body width", title.width >= with(compose.density) { 320.dp.toPx() })
        assertTrue("Label is separate from the outlined field", field.top - label.bottom + 1f >= labelGap)
    }

    private fun assertEndCanScrollAboveSave() {
        compose.onNodeWithTag("project-editor-content-end").performScrollTo().assertIsDisplayed()
        val end = compose.onNodeWithTag("project-editor-content-end").fetchSemanticsNode().boundsInRoot
        val save = compose.onNodeWithTag("project-editor-save-bar").fetchSemanticsNode().boundsInRoot
        assertTrue("Scrolling can expose the final content above save", end.bottom <= save.top + 1f)
        compose.onNodeWithTag("project-save").assertIsDisplayed()
    }
}
