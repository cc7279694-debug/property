package com.cooper.lithic.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.data.backup.BackupExportResult
import com.cooper.lithic.data.backup.BackupTarget
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Retained by the Activity, so leaving Settings or rotation does not create another export. */
internal class BackupExportViewModel(
    private val saved: SavedStateHandle,
    export: suspend (BackupTarget) -> BackupExportResult,
) : ViewModel() {
    private val controller = BackupExportController(saved.get<Boolean>(ACTIVE_KEY) == true, export)
    val state = controller.state
    private var exportJob: Job? = null

    fun beginSelection(): Boolean {
        val accepted = controller.beginSelection()
        if (accepted) saved[ACTIVE_KEY] = true
        return accepted
    }

    fun selectionCancelled() {
        controller.selectionCancelled()
        updateSavedMarker()
    }

    fun selectionLaunchFailed(possibleResidual: Boolean = false) {
        controller.selectionLaunchFailed(possibleResidual)
        updateSavedMarker()
    }

    fun selected(target: BackupTarget) {
        if (exportJob?.isActive == true) return
        exportJob = viewModelScope.launch {
            try { controller.selected(target) }
            finally { updateSavedMarker() }
        }
    }

    fun cancelExport() { exportJob?.cancel() }

    private fun updateSavedMarker() { saved[ACTIVE_KEY] = state.value.requiresRecreationWarning() }

    companion object { private const val ACTIVE_KEY = "lithic.backup.incomplete" }
}
