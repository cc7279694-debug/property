package com.cooper.lithic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.feature.settings.BackupExportViewModel
import com.cooper.lithic.feature.settings.BackupRestoreViewModel
import com.cooper.lithic.feature.settings.BackupUiPhase
import com.cooper.lithic.feature.settings.RestoreUiPhase
import com.cooper.lithic.feature.settings.rememberBackupDocumentPicker
import com.cooper.lithic.feature.settings.rememberBackupRestoreDocumentPicker
import com.cooper.lithic.data.backup.BackupExportService
import com.cooper.lithic.data.backup.RestoreCoordinator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LithicApplication).appContainer
        val holder = ViewModelProvider(this, viewModelFactory {
            initializer { LedgerUiStateHolder(container.storageState) }
        })[LedgerUiStateHolder::class.java]
        val backups = ViewModelProvider(this, viewModelFactory {
            initializer {
                BackupExportViewModel(createSavedStateHandle()) { target ->
                    val generation = container.openStorage()
                    BackupExportService(applicationContext, generation).export(target)
                }
            }
        })[BackupExportViewModel::class.java]
        val restores = ViewModelProvider(this, viewModelFactory {
            initializer { BackupRestoreViewModel(createSavedStateHandle(), RestoreCoordinator(applicationContext, container)) }
        })[BackupRestoreViewModel::class.java]
        setContent {
            LithicTheme {
                val state by holder.state.collectAsStateWithLifecycle()
                val backupState by backups.state.collectAsStateWithLifecycle()
                val restoreState by restores.state.collectAsStateWithLifecycle()
                val chooseBackupTarget = rememberBackupDocumentPicker(backups)
                val chooseRestoreSource = rememberBackupRestoreDocumentPicker(restores)
                val export: () -> Unit = {
                    if (restores.state.value.phase == RestoreUiPhase.IDLE) chooseBackupTarget()
                }
                val restore: () -> Unit = {
                    if (backups.state.value.phase == BackupUiPhase.IDLE) chooseRestoreSource()
                }
                LithicApp(state = state, onExit = { finish() }, onRetryLedger = holder::retryRead,
                    backupState = backupState, onExportBackup = export, onCancelBackup = backups::cancelExport,
                    restoreState = restoreState, onRestoreBackup = restore, onCancelRestore = restores::cancelValidation,
                    onConfirmRestore = restores::confirmOverwrite, onDiscardRestore = restores::discardPreview)
            }
        }
    }
}

/** A retained UI owner, separate from the storage owner and from backup/restore operations. */
internal class GenerationUiScopeViewModel(private val saved: SavedStateHandle) : ViewModel(), ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
    private var repositoryIdentity: Any? = null
    var savedState = restoreForms(saved.get<Bundle>(FORMS_KEY))
        private set

    init { saved.setSavedStateProvider(FORMS_KEY, ::saveForms) }

    fun bind(generationId: String, identity: Any): Long {
        require(generationId.isNotBlank())
        val previous = saved.get<String>(GENERATION_KEY)
        val changed = (previous != null && previous != generationId) ||
            (repositoryIdentity != null && repositoryIdentity !== identity)
        var epoch = saved.get<Long>(EPOCH_KEY) ?: 0L
        if (changed) {
            viewModelStore.clear()
            // Give new models a new handle too: cancelled old jobs can only update the old handle.
            savedState = SavedStateHandle()
            saved.remove<Bundle>(FORMS_KEY)
            epoch = Math.addExact(epoch, 1L)
        }
        repositoryIdentity = identity
        saved[GENERATION_KEY] = generationId
        saved[EPOCH_KEY] = epoch
        return epoch
    }

    /** Only the existing saved form envelopes and confirmed IDs belong to this temporary UI state. */
    fun saveForms(): Bundle = Bundle().apply {
        val bundles = Bundle()
        val strings = Bundle()
        savedState.keys().forEach { key ->
            when (val value = savedState.get<Any?>(key)) {
                null -> strings.putString(key, null)
                is String -> strings.putString(key, value)
                is Bundle -> bundles.putBundle(key, Bundle(value))
                else -> error("Unsupported saved form value")
            }
        }
        putBundle("bundles", bundles)
        putBundle("strings", strings)
    }

    override fun onCleared() { viewModelStore.clear() }

    companion object {
        internal const val GENERATION_KEY = "lithic.ui.generation"
        internal const val EPOCH_KEY = "lithic.ui.epoch"
        internal const val FORMS_KEY = "lithic.ui.forms"

        private fun restoreForms(state: Bundle?): SavedStateHandle {
            val values = mutableMapOf<String, Any?>()
            state?.getBundle("bundles")?.let { bundle ->
                bundle.keySet().forEach { key -> values[key] = bundle.getBundle(key)?.let(::Bundle) }
            }
            state?.getBundle("strings")?.let { bundle ->
                bundle.keySet().forEach { key -> values[key] = bundle.getString(key) }
            }
            return SavedStateHandle(values)
        }
    }
}
