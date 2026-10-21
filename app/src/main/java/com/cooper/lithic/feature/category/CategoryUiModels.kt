package com.cooper.lithic.feature.category

import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.CategoryEntry
import com.cooper.lithic.data.repository.CategoryMergePreview

internal enum class CategoryEditMode { CREATE, RENAME, MERGE }

internal data class CategoryEditorUiState(
    val mode: CategoryEditMode,
    val categoryId: String,
    val source: CategoryEntity? = null,
    val name: String = "",
    val targetId: String? = null,
    val preview: CategoryMergePreview? = null,
    val busy: Boolean = false,
    val uncertain: Boolean = false,
    val conflict: Boolean = false,
    val message: String? = null,
) {
    val canChange: Boolean get() = !busy && !uncertain && !conflict
}

internal data class CategoryManagementUiState(
    val type: CategoryType = CategoryType.PROJECT,
    val items: List<CategoryEntry> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val editor: CategoryEditorUiState? = null,
)
