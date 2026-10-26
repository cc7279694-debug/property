package com.cooper.lithic.feature.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Production editor, presentation callbacks only; no Owner, generation or device settings. */
@RunWith(AndroidJUnit4::class)
class C2ProjectEditorSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun expanding_and_collapsing_optional_note_keeps_input_and_never_submits() {
        var state by mutableStateOf(editor())
        var saves = 0
        compose.setContent { LithicTheme {
            ProjectEditorScreen(state, {}, {}, {}, {}, {}, {}, { state = state.copy(note = it) }, { saves++ }, {})
        } }
        compose.onNodeWithTag("project-note").assertDoesNotExist()
        compose.onNodeWithTag("project-extra-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("project-note").performScrollTo().performTextReplacement("保留这条备注")
        compose.onNodeWithTag("project-extra-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("project-note").assertDoesNotExist()
        compose.onNodeWithTag("project-extra-toggle").performScrollTo().performClick()
        compose.onNodeWithTag("project-note").performScrollTo().assertTextContains("保留这条备注")
        assertEquals(0, saves)
        assertEquals("0", state.initialAmount)
    }

    @Test fun payment_error_reveals_original_field_and_zero_save_uses_single_existing_callback() {
        var state by mutableStateOf(editor())
        var saves = 0
        compose.setContent { LithicTheme {
            ProjectEditorScreen(state, {}, {}, {}, {}, {}, {}, {}, { saves++ }, {})
        } }
        compose.onNodeWithTag("project-initial-paid-on-input").assertDoesNotExist()
        compose.runOnIdle { state = state.copy(errors = mapOf("initialPaidOn" to "请核对真实付款日")) }
        compose.onNodeWithTag("project-initial-paid-on-input").assertIsDisplayed()
            .assertTextContains("2026-01-02")
        compose.onNodeWithTag("project-save").assertIsDisplayed().performClick()
        assertEquals(1, saves)
    }

    @Test fun empty_amount_stays_empty_and_unconfirmed_submission_remains_locked() {
        var state by mutableStateOf(editor().copy(initialAmount = ""))
        var backs = 0
        compose.setContent { LithicTheme {
            ProjectEditorScreen(state, { backs++ }, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("project-initial-amount").assertIsDisplayed()
        compose.runOnIdle { state = state.copy(phase = SubmissionPhase.UNCONFIRMED) }
        compose.onNodeWithTag("project-name").assertIsNotEnabled()
        compose.onNodeWithTag("project-save").assertTextContains("核对并重试")
        compose.onNodeWithTag("project-editor-back").performClick()
        assertEquals(1, backs) // The existing navigation guard owns whether leaving is allowed.
        assertEquals("", state.initialAmount)
    }

    private fun editor() = ProjectEditorUiState(projectId = "editor-presentation-only", name = "降噪耳机",
        startedOn = "2026-01-02", initialPaidOn = "2026-01-02", initialAmount = "0",
        categoryId = LithicDatabase.PROJECT_FALLBACK_ID,
        categories = listOf(CategoryEntity(LithicDatabase.PROJECT_FALLBACK_ID, CategoryType.PROJECT, "其他", "其他", true)),
        today = LocalDate.of(2026, 1, 2))
}
