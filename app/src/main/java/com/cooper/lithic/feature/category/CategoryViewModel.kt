package com.cooper.lithic.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.CategoryRepository

/** Navigation owns this generation-scoped model; the Repository remains the only writer. */
internal class CategoryViewModel(repository: CategoryRepository) : ViewModel() {
    private val controller = CategoryController(viewModelScope, repository.changes, repository::list,
        { type, name, id -> repository.create(type, name, id) }, repository::rename,
        repository::previewMerge, repository::merge)
    val state = controller.state
    fun refresh() = controller.refresh()
    fun selectType(type: CategoryType) = controller.selectType(type)
    fun startCreate() = controller.startCreate()
    fun startRename(category: CategoryEntity) = controller.startRename(category)
    fun startMerge(category: CategoryEntity) = controller.startMerge(category)
    fun changeName(value: String) = controller.changeName(value)
    fun selectTarget(id: String) = controller.selectTarget(id)
    fun previewMerge() = controller.previewMerge()
    fun save() = controller.save()
    fun confirmMerge() = controller.confirmMerge()
    fun dismissEditor() = controller.dismissEditor()
    fun leaveDecision() = controller.leaveDecision()
    fun discardForm() = controller.discardForm()
}
