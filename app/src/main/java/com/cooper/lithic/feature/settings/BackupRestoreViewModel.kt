package com.cooper.lithic.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.data.backup.RestoreCoordinator
import java.io.InputStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** The coordinator and preview survive rotation; no URI, token or confirmation is saved for replay. */
internal class BackupRestoreViewModel(
    private val saved: SavedStateHandle,
    coordinator: RestoreCoordinator,
) : ViewModel() {
    private val controller = BackupRestoreController(saved.get<Boolean>(ACTIVE_KEY) == true,
        prepare = coordinator::prepare, confirm = coordinator::confirm, discard = coordinator::discard)
    val state = controller.state
    private var operation: Job? = null

    fun beginSelection(): Boolean {
        val accepted = controller.beginSelection()
        if (accepted) saved[ACTIVE_KEY] = true
        return accepted
    }

    fun selectionCancelled() {
        controller.selectionCancelled()
        updateSavedMarker()
    }

    fun selectionLaunchFailed() {
        controller.selectionLaunchFailed()
        updateSavedMarker()
    }

    fun selected(openInput: () -> InputStream) = runOperation { controller.selected(openInput) }
    fun confirmOverwrite() = runOperation { controller.confirmOverwrite() }
    fun discardPreview() = runOperation { controller.discardPreview() }

    fun cancelValidation() {
        if (state.value.phase == RestoreUiPhase.VALIDATING) operation?.cancel()
    }

    private fun runOperation(block: suspend () -> Unit) {
        if (operation?.isActive == true) return
        operation = viewModelScope.launch {
            try { block() }
            finally { updateSavedMarker() }
        }
    }

    private fun updateSavedMarker() { saved[ACTIVE_KEY] = state.value.requiresRecreationWarning() }

    companion object { private const val ACTIVE_KEY = "lithic.restore.incomplete" }
}
