package com.cooper.lithic.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.core.time.ZonedBusinessClock
import com.cooper.lithic.data.repository.ProjectRepository
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI lifetime only; Application's AppContainer remains the sole storage owner. */
internal class LedgerUiStateHolder(
    private val storageStates: StateFlow<StorageInitializationState>,
    private val clockFactory: (ZoneId) -> BusinessClock = { ZonedBusinessClock(Clock.system(it)) },
) : ViewModel() {
    private val mutableState = MutableStateFlow<LedgerUiState>(LedgerUiState.Initializing)
    val state: StateFlow<LedgerUiState> = mutableState.asStateFlow()
    init {
        viewModelScope.launch {
            storageStates.collectLatest { storage ->
                when (storage) {
                    StorageInitializationState.Initializing -> mutableState.value = LedgerUiState.Initializing
                    is StorageInitializationState.Failed -> mutableState.value = LedgerUiState.Failed
                    is StorageInitializationState.Ready -> {
                        mutableState.value = LedgerUiState.Initializing
                        try {
                            val ready = withContext(Dispatchers.IO) {
                                val generation = storage.generation
                                val zone = generation.settings.recordZone()
                                val clock = clockFactory(zone)
                                val repository = ProjectRepository(generation.database, generation.coordinator, clock)
                                LedgerUiState.Ready(repository.loadSummary().projectCount, zone, repository, clock)
                            }
                            mutableState.value = ready
                            ready.repository.changes.collectLatest { refresh(ready) }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) {
                            val previous = mutableState.value
                            mutableState.value = if (previous is LedgerUiState.Ready) previous.copy(readError = READ_ERROR) else LedgerUiState.Failed
                        }
                    }
                }
            }
        }
    }
    fun retryRead() {
        val ready = state.value as? LedgerUiState.Ready ?: return
        viewModelScope.launch { refresh(ready) }
    }
    private suspend fun refresh(ready: LedgerUiState.Ready) {
        try {
            val count = withContext(Dispatchers.IO) { ready.repository.loadSummary().projectCount }
            mutableState.value = ready.copy(projectCount = count, readError = null)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            val previous = state.value as? LedgerUiState.Ready ?: ready
            mutableState.value = previous.copy(readError = READ_ERROR)
        }
    }
    companion object { private const val READ_ERROR = "本地记录暂时无法刷新，当前显示上次读取结果。" }
}
