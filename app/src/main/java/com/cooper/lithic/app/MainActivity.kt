package com.cooper.lithic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cooper.lithic.core.design.LithicTheme

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
                LithicApp(state = state, onExit = { finish() })
            }
        }
    }
}
