package com.cooper.lithic.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.core.time.ZonedBusinessClock
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SubscriptionRepository
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI lifetime only; Application's AppContainer remains the sole storage owner. */
internal class LedgerUiStateHolder(
    private val storageStates: StateFlow<StorageInitializationState>,
    private val clockFactory: (ZoneId) -> BusinessClock = { ZonedBusinessClock(Clock.system(it)) },
) : ViewModel() {
    private val mutableState = MutableStateFlow<LedgerUiState>(LedgerUiState.Initializing)
    val state: StateFlow<LedgerUiState> = mutableState.asStateFlow()
    private var recoveryWarning: String? = null
    init {
        viewModelScope.launch {
            storageStates.collectLatest { storage ->
                when (storage) {
                    StorageInitializationState.Initializing -> mutableState.value = LedgerUiState.Initializing
                    is StorageInitializationState.Failed -> mutableState.value = LedgerUiState.Failed
                    is StorageInitializationState.Ready -> {
                        recoveryWarning = storage.recoveryWarning
                        mutableState.value = LedgerUiState.Initializing
                        try {
                            val ready = withContext(Dispatchers.IO) {
                                val generation = storage.generation
                                val zone = generation.settings.recordZone()
                                val clock = clockFactory(zone)
                                val repository = ProjectRepository(generation.database, generation.coordinator, clock)
                                val subscriptions = SubscriptionRepository(generation.database, generation.coordinator, clock)
                                LedgerUiState.Ready(repository.loadSummary().projectCount, zone, repository, clock,
                                    subscriptions, subscriptions.loadSubscriptionCount(), readError = storage.recoveryWarning,
                                    generationId = generation.id)
                            }
                            mutableState.value = ready
                            merge(ready.repository.changes, ready.subscriptionRepository.changes).collectLatest { refresh(ready) }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) {
                            val previous = mutableState.value
                            mutableState.value = if (previous is LedgerUiState.Ready) previous.copy(readError = readFailureMessage()) else LedgerUiState.Failed
                        }
                    }
                }
            }
        }
    }
    fun retryRead(): Job? {
        val ready = state.value as? LedgerUiState.Ready ?: return null
        return viewModelScope.launch { refresh(ready) }
    }
    private suspend fun refresh(ready: LedgerUiState.Ready) {
        if (currentReady(ready) == null) return
        try {
            val counts = withContext(Dispatchers.IO) {
                ready.repository.loadSummary().projectCount to ready.subscriptionRepository.loadSubscriptionCount()
            }
            val current = currentReady(ready) ?: return
            mutableState.value = current.copy(projectCount = counts.first, subscriptionCount = counts.second, readError = recoveryWarning)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            val current = currentReady(ready) ?: return
            mutableState.value = current.copy(readError = readFailureMessage())
        }
    }
    /** A delayed explicit retry is outside collectLatest and may finish after owner replacement. */
    private fun currentReady(requested: LedgerUiState.Ready): LedgerUiState.Ready? =
        (state.value as? LedgerUiState.Ready)?.takeIf {
            it.repository === requested.repository && it.generationId == requested.generationId
        }
    private fun readFailureMessage() = listOfNotNull(recoveryWarning, READ_ERROR).joinToString(" ")
    companion object { private const val READ_ERROR = "本地记录暂时无法刷新，当前显示上次读取结果。" }
}
