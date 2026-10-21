package com.cooper.lithic.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.data.repository.SearchRepository

internal class SearchViewModel(repository: SearchRepository) : ViewModel() {
    private val controller = SearchController(viewModelScope, repository.changes,
        { query, pages -> repository.loadWindow(query, pages) })
    val state = controller.state
    fun changeQuery(value: String) = controller.changeQuery(value)
    fun refresh() = controller.refresh()
    fun loadMore() = controller.loadMore()
}
