package com.cooper.lithic.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.data.repository.LedgerReadRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI lifetime only; Application's AppContainer remains the sole persistent storage owner. */
internal class LedgerUiStateHolder(
    private val storageStates: StateFlow<StorageInitializationState>,
) : ViewModel() {
    private val mutableState = MutableStateFlow<LedgerUiState>(LedgerUiState.Initializing)
    val state: StateFlow<LedgerUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            storageStates.collectLatest { storage ->
                mutableState.value = when (storage) {
                    StorageInitializationState.Initializing -> LedgerUiState.Initializing
                    is StorageInitializationState.Failed -> LedgerUiState.Failed
                    is StorageInitializationState.Ready -> {
                        mutableState.value = LedgerUiState.Initializing
                        try {
                            withContext(Dispatchers.IO) {
                                val generation = storage.generation
                                LedgerUiState.Ready(
                                    LedgerReadRepository(generation.database, generation.coordinator).snapshot(),
                                    generation.settings.recordZone(),
                                )
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            LedgerUiState.Failed
                        }
                    }
                }
            }
        }
    }
}
