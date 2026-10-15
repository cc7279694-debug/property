package com.cooper.lithic.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.core.time.ZonedBusinessClock
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SubscriptionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class HomeReadResult(val projectCount: Long, val subscriptionCount: Long, val preview: HomePreview)

/** Existing APIs retain their validation boundaries; these four reads are not an atomic snapshot. */
internal suspend fun readHomeOverview(projects: ProjectRepository, subscriptions: SubscriptionRepository,
    asOf: LocalDate): HomeReadResult {
    val projectCount = projects.loadSummary().projectCount
    val subscriptionCount = subscriptions.loadSubscriptionCount()
    // One project window remains 50 plus its sentinel, rather than a new SQL limit of three.
    val projectWindow = projects.loadCurrentProjectWindow(1, asOf)
    // This API still performs the existing full-facts precheck before its bounded page.
    val subscriptionPage = subscriptions.loadManagementPage(false, null, 3, asOf)
    return HomeReadResult(projectCount, subscriptionCount, HomePreview(projectWindow.items.take(3),
        subscriptionPage.items.take(3), projectWindow.totalCount, subscriptionPage.totalCount, asOf))
}

/** UI lifetime only; Application's AppContainer remains the sole storage owner. */
internal class LedgerUiStateHolder(
    private val storageStates: StateFlow<StorageInitializationState>,
    private val homeReader: suspend (ProjectRepository, SubscriptionRepository, LocalDate) -> HomeReadResult = ::readHomeOverview,
    private val clockFactory: (ZoneId) -> BusinessClock = { ZonedBusinessClock(Clock.system(it)) },
) : ViewModel() {
    private data class ReadSession(val generation: ManagedGeneration, val ready: LedgerUiState.Ready)
    private val mutableState = MutableStateFlow<LedgerUiState>(LedgerUiState.Initializing)
    val state: StateFlow<LedgerUiState> = mutableState.asStateFlow()
    // Accessed by viewModelScope on Main, including retries and change notifications.
    private var activeSession: ReadSession? = null
    private var requestSequence = 0L

    init {
        viewModelScope.launch {
            storageStates.collectLatest { storage ->
                requestSequence++
                activeSession = null
                when (storage) {
                    StorageInitializationState.Initializing -> mutableState.value = LedgerUiState.Initializing
                    is StorageInitializationState.Failed -> mutableState.value = LedgerUiState.Failed
                    is StorageInitializationState.Ready -> observeGeneration(storage.generation)
                }
            }
        }
    }

    private suspend fun observeGeneration(generation: ManagedGeneration) {
        mutableState.value = LedgerUiState.Initializing
        var session: ReadSession? = null
        try {
            val ready = withContext(Dispatchers.IO) {
                val zone = generation.settings.recordZone()
                val clock = clockFactory(zone)
                val repository = ProjectRepository(generation.database, generation.coordinator, clock)
                val subscriptions = SubscriptionRepository(generation.database, generation.coordinator, clock)
                val media = MediaRepository(generation.database, generation.coordinator,
                    generation.images, clock, generation.mediaResolver)
                LedgerUiState.Ready(0L, zone, repository, clock, subscriptions, 0L, mediaRepository = media)
            }
            if ((storageStates.value as? StorageInitializationState.Ready)?.generation !== generation) return
            val currentSession = ReadSession(generation, ready)
            session = currentSession
            activeSession = currentSession
            // Existing Room flows emit initially. Observe before reading to avoid losing queued writes.
            merge(ready.repository.changes, ready.subscriptionRepository.changes).collectLatest { refresh(currentSession) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if ((storageStates.value as? StorageInitializationState.Ready)?.generation === generation &&
                (session == null || activeSession === session)) {
                requestSequence++
                val previous = mutableState.value as? LedgerUiState.Ready
                mutableState.value = previous?.copy(readError = READ_ERROR, isRefreshing = false) ?: LedgerUiState.Failed
            }
        }
    }

    fun retryRead() {
        val session = activeSession ?: return
        if (state.value !is LedgerUiState.Ready) return
        viewModelScope.launch { refresh(session) }
    }

    private suspend fun refresh(session: ReadSession) {
        if (!ownsCurrentStorage(session)) return
        val request = ++requestSequence
        val previous = state.value as? LedgerUiState.Ready
        if (previous != null && sameRepositories(previous, session.ready)) {
            mutableState.value = previous.copy(isRefreshing = true)
        }
        try {
            val result = withContext(Dispatchers.IO) {
                val asOf = session.ready.businessClock.today()
                homeReader(session.ready.repository, session.ready.subscriptionRepository, asOf)
            }
            if (!canPublish(session, request)) return
            mutableState.value = session.ready.copy(projectCount = result.projectCount,
                subscriptionCount = result.subscriptionCount, homePreview = result.preview,
                readError = null, isRefreshing = false)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (!canPublish(session, request)) return
            val current = state.value as? LedgerUiState.Ready
            mutableState.value = current?.copy(readError = READ_ERROR, isRefreshing = false) ?: LedgerUiState.Failed
        }
    }

    private fun ownsCurrentStorage(session: ReadSession): Boolean = activeSession === session &&
        (storageStates.value as? StorageInitializationState.Ready)?.generation === session.generation

    private fun canPublish(session: ReadSession, request: Long): Boolean = request == requestSequence &&
        ownsCurrentStorage(session) && ((state.value as? LedgerUiState.Ready)?.let { sameRepositories(it, session.ready) } != false)

    private fun sameRepositories(first: LedgerUiState.Ready, second: LedgerUiState.Ready): Boolean =
        first.repository === second.repository && first.subscriptionRepository === second.subscriptionRepository &&
            first.mediaRepository === second.mediaRepository

    companion object { private const val READ_ERROR = "本地记录暂时无法刷新，当前显示上次读取结果。" }
}
