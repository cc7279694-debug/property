package com.cooper.lithic.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Registered above navigation so rotation or leaving Settings does not drop the picker result. */
@Composable
internal fun rememberBackupDocumentPicker(model: BackupExportViewModel): () -> Unit {
    val context = LocalContext.current.applicationContext
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) model.selectionCancelled()
        else {
            try { model.selected(SafBackupTarget(context, uri)) }
            catch (_: Exception) { model.selectionLaunchFailed(possibleResidual = true) }
        }
    }
    return {
        if (model.beginSelection()) {
            val filename = "lithic-backup-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.lithicbackup"
            try { launcher.launch(filename) }
            catch (_: Exception) { model.selectionLaunchFailed() }
        }
    }
}
