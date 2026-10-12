package com.cooper.lithic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.feature.project.ProjectViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val holder = ViewModelProvider(this, viewModelFactory {
            initializer { LedgerUiStateHolder((application as LithicApplication).appContainer.storageState) }
        })[LedgerUiStateHolder::class.java]
        setContent {
            LithicTheme {
                val state by holder.state.collectAsStateWithLifecycle()
                val ready = state as? LedgerUiState.Ready
                val projectModel = ready?.let { storage ->
                    remember(storage.repository) {
                        ViewModelProvider(this, viewModelFactory {
                            initializer { ProjectViewModel(storage.repository, createSavedStateHandle(), storage.businessClock) }
                        })[ProjectViewModel::class.java]
                    }
                }
                LithicApp(state = state, onExit = { finish() }, projectViewModel = projectModel, onRetryLedger = holder::retryRead)
            }
        }
    }
}
