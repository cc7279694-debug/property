package com.cooper.lithic.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.data.backup.BackupInspectionResult
import java.io.InputStream
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Only the interrupted marker survives process death, never a document URI or a restore token. */
internal class BackupInspectionViewModel(
    private val saved: SavedStateHandle,
    currentOwner: () -> Any?,
    ownerChanges: Flow<*>,
    inspect: suspend (() -> InputStream, () -> Boolean) -> BackupInspectionResult,
) : ViewModel() {
    private val controller = BackupInspectionController(saved.get<Boolean>(ACTIVE_KEY) == true, currentOwner, inspect)
    val state = controller.state
    private var operation: Job? = null

    init {
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            ownerChanges.collect {
                if (controller.ownerChanged()) operation?.cancel()
                updateMarker()
            }
        }
    }

    fun beginSelection(): String? = controller.beginSelection().also { updateMarker() }
    fun previousSelectionPending() { controller.previousSelectionPending(); updateMarker() }
    fun retainPendingSelection(id: String) { controller.retainPendingSelection(id); updateMarker() }
    fun selectionCancelled(id: String) { controller.selectionCancelled(id); updateMarker() }
    fun selectionLaunchFailed(id: String) { controller.selectionLaunchFailed(id); updateMarker() }

    fun selected(id: String, openInput: () -> InputStream) {
        if (operation?.isActive == true) return
        operation = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val readingJob = coroutineContext[Job]
            val watchdog = launch {
                delay(CHECK_TIMEOUT_MS)
                if (controller.requestCancellation()) {
                    updateMarker()
                    readingJob?.cancel()
                }
            }
            try { controller.selected(id, openInput) }
            finally { watchdog.cancel(); updateMarker() }
        }
    }

    fun cancelCheck() {
        if (controller.requestCancellation()) { updateMarker(); operation?.cancel() }
    }

    private fun updateMarker() { saved[ACTIVE_KEY] = state.value.requiresRecreationWarning() }

    companion object {
        private const val ACTIVE_KEY = "lithic.backup.inspection.incomplete"
        private const val CHECK_TIMEOUT_MS = 120_000L
    }
}
