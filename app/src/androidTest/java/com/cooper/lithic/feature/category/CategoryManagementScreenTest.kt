package com.cooper.lithic.feature.category

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryNames
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.CategoryEntry
import com.cooper.lithic.data.repository.CategoryMergePreview
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Uses only a synthetic presentation; no owner, database or default ledger is opened. */
@RunWith(AndroidJUnit4::class)
class CategoryManagementScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fallback_has_no_rename_or_merge_source_action() {
        val fallback = category("其他", true)
        val normal = category("数码")
        compose.setContent { LithicTheme {
            Render(CategoryManagementUiState(items = listOf(CategoryEntry(fallback, 2), CategoryEntry(normal, 1)), loading = false))
        } }
        compose.onNodeWithTag("category-rename-${fallback.id}").assertDoesNotExist()
        compose.onNodeWithTag("category-merge-${fallback.id}").assertDoesNotExist()
    }

    @Test fun merge_confirmation_displays_exact_affected_history_count_before_commit() {
        val source = category("设备")
        val target = category("数码")
        var confirmed = 0
        val editor = CategoryEditorUiState(CategoryEditMode.MERGE, source.id, source = source, targetId = target.id,
            preview = CategoryMergePreview(source, target, 7))
        compose.setContent { LithicTheme {
            Render(CategoryManagementUiState(loading = false, editor = editor), onConfirm = { confirmed++ })
        } }
        compose.onNodeWithText("“设备”的 7 条记录（含历史）将移至“数码”。").assertTextContains("7 条记录")
        compose.onNodeWithTag("category-merge-confirm").performClick()
        assertEquals(1, confirmed)
    }

    @Test fun uncertain_submission_freezes_name_and_cancel_but_allows_exact_retry() {
        val editor = CategoryEditorUiState(CategoryEditMode.CREATE, UUID.randomUUID().toString(), name = "数码", uncertain = true)
        var retries = 0
        compose.setContent { LithicTheme {
            Render(CategoryManagementUiState(loading = false, editor = editor), onSave = { retries++ })
        } }
        compose.onNodeWithTag("category-name").assertIsNotEnabled()
        compose.onNodeWithTag("category-cancel").assertIsNotEnabled()
        compose.onNodeWithTag("category-save").performClick()
        assertEquals(1, retries)
    }

    @androidx.compose.runtime.Composable
    private fun Render(state: CategoryManagementUiState, onConfirm: () -> Unit = {}, onSave: () -> Unit = {}) =
        CategoryManagementScreen(state, {}, {}, {}, {}, {}, {}, {}, {}, onSave, onConfirm, {}, {})

    private fun category(name: String, fallback: Boolean = false) =
        CategoryEntity(UUID.randomUUID().toString(), CategoryType.PROJECT, name, CategoryNames.normalized(name), fallback)
}
