package com.cooper.lithic.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.IOException

/** Transient read grant only. A callback needs the live controller's matching selection request. */
@Composable
internal fun rememberBackupInspectionDocumentPicker(model: BackupInspectionViewModel): () -> Unit {
    val context = LocalContext.current.applicationContext
    var launchedRequest by rememberSaveable { mutableStateOf<String?>(null) }
    // Rotation keeps the live request; process recreation only restores a cancel-only obligation.
    SideEffect { launchedRequest?.let(model::retainPendingSelection) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val request = launchedRequest
        launchedRequest = null
        if (request != null) {
            if (uri == null) model.selectionCancelled(request)
            else model.selected(request) {
                context.contentResolver.openInputStream(uri) ?: throw IOException("Selected backup could not be opened")
            }
        }
    }
    return {
        if (launchedRequest != null) model.previousSelectionPending()
        else model.beginSelection()?.let { request ->
            launchedRequest = request
            try { launcher.launch(arrayOf("*/*")) }
            catch (_: Exception) { launchedRequest = null; model.selectionLaunchFailed(request) }
        }
    }
}
