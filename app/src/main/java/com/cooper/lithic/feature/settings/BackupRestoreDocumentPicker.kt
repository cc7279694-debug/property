package com.cooper.lithic.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.IOException

/** URI access is read-only and transient: preparation makes its own bounded private copy. */
@Composable
internal fun rememberBackupRestoreDocumentPicker(model: BackupRestoreViewModel): () -> Unit {
    val context = LocalContext.current.applicationContext
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) model.selectionCancelled()
        else model.selected {
            context.contentResolver.openInputStream(uri) ?: throw IOException("Backup input could not be opened")
        }
    }
    return {
        if (model.beginSelection()) {
            try { launcher.launch(arrayOf("*/*")) }
            catch (_: Exception) { model.selectionLaunchFailed() }
        }
    }
}
