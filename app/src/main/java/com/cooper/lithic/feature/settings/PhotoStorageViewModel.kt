package com.cooper.lithic.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.data.media.PhotoStorageReader
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Owned by the existing generation UI scope; a retired scope cannot publish a new snapshot. */
internal class PhotoStorageViewModel(reader: PhotoStorageReader) : ViewModel() {
    private val controller = PhotoStorageController(reader)
    val state = controller.state
    private var operation: Job? = null

    fun refresh() {
        if (operation?.isActive == true) return
        operation = viewModelScope.launch { controller.refresh() }
    }

    override fun onCleared() {
        controller.close()
        operation?.cancel()
        super.onCleared()
    }
}
