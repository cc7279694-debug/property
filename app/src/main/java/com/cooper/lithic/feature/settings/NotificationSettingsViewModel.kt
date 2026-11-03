package com.cooper.lithic.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.data.settings.ReminderPreferences
import com.cooper.lithic.data.settings.SettingsStore
import com.cooper.lithic.platform.notification.EnqueueStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

internal data class NotificationSettingsUiState(
    val loading: Boolean = true,
    val preferences: ReminderPreferences? = null,
    val saving: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val scheduleStatus: EnqueueStatus? = null,
    val saveOutcomeUnknown: Boolean = false,
)

/** This model is owned by the existing generation UI scope, not a new DataStore owner. */
internal class NotificationSettingsViewModel(
    settings: SettingsStore,
    onPreferencesChanged: () -> Unit = {},
    schedulePort: (() -> StateFlow<EnqueueStatus>?)? = null,
    isOwnerActive: () -> Boolean = { true },
    bindingIdentity: Any? = null,
) : ViewModel() {
    private val controller = NotificationSettingsController(viewModelScope,
        settings::reminderPreferences,
        { renewal, milestone -> settings.updateReminderPreferences(renewal, milestone) },
        onPreferencesChanged, schedulePort, isOwnerActive, bindingIdentity)
    val state = controller.state
    fun refresh() = controller.refresh()
    fun setRenewalEnabled(enabled: Boolean) = controller.setRenewalEnabled(enabled)
    fun setMilestoneEnabled(enabled: Boolean) = controller.setMilestoneEnabled(enabled)
    fun requestScheduleOnResume() = controller.requestScheduleOnResume()
    fun stopScheduleObservation() = controller.stopScheduleObservation()
    fun rebindSchedule(identity: Any, port: (() -> StateFlow<EnqueueStatus>?)?, isActive: () -> Boolean): Boolean =
        controller.rebindSchedule(identity, port, isActive)
}

/** Failed reads have no editable fallback; changes are displayed only after persistence. */
internal class NotificationSettingsController(
    private val scope: CoroutineScope,
    private val readPreferences: suspend () -> ReminderPreferences,
    private val updatePreferences: suspend (Boolean?, Boolean?) -> ReminderPreferences,
    private val onPreferencesChanged: () -> Unit = {},
    private var schedulePort: (() -> StateFlow<EnqueueStatus>?)? = null,
    private var isOwnerActive: () -> Boolean = { true },
    private var bindingIdentity: Any? = null,
) {
    private val mutableState = MutableStateFlow(NotificationSettingsUiState())
    val state = mutableState.asStateFlow()
    private var readJob: Job? = null
    private var scheduleJob: Job? = null
    private var requestSequence = 0L
    private var scheduleSequence = 0L
    private var bindingSequence = 0L
    private var pageVisible = true
    private enum class SaveOutcome { IN_FLIGHT, SAVED, UNKNOWN }
    private class SaveAttempt(val binding: Long) { var outcome = SaveOutcome.IN_FLIGHT }
    private var saveAttempt: SaveAttempt? = null
    private var resumePendingWhileSaving = false

    init { refresh() }

    fun refresh() {
        if (state.value.saving || state.value.saveOutcomeUnknown || !isOwnerActive()) return
        val ticket = ++requestSequence
        val binding = bindingSequence
        readJob?.cancel()
        // Reading preferences is independent of a pending/unknown WorkManager operation.
        // Do not discard its observer or manufacture a fresh enqueue.
        mutableState.value = state.value.copy(loading = true, preferences = null, error = null, notice = null)
        readJob = scope.launch {
            try {
                val preferences = readPreferences()
                currentCoroutineContext().ensureActive()
                if (ticket == requestSequence && binding == bindingSequence && isOwnerActive()) {
                    mutableState.value = mutableState.value.copy(loading = false, preferences = preferences, error = null)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                if (ticket == requestSequence && binding == bindingSequence && isOwnerActive()) {
                    mutableState.value = mutableState.value.copy(loading = false, preferences = null,
                        error = "提醒偏好暂时无法读取，请重试。当前记录仍可正常使用。")
                }
            }
        }
    }

    fun setRenewalEnabled(enabled: Boolean) = update(renewal = enabled)
    fun setMilestoneEnabled(enabled: Boolean) = update(milestone = enabled)

    private fun update(renewal: Boolean? = null, milestone: Boolean? = null) {
        val before = state.value
        val preferences = before.preferences ?: return
        if (before.loading || before.saving || !isOwnerActive()) return
        if ((renewal == null || renewal == preferences.renewalEnabled) &&
            (milestone == null || milestone == preferences.milestoneEnabled)) return
        invalidateSchedule()
        mutableState.value = before.copy(saving = true, error = null, notice = null, scheduleStatus = null)
        val binding = bindingSequence
        val attempt = SaveAttempt(binding)
        saveAttempt = attempt
        scope.launch {
            var writeReturned = false
            try {
                val persisted = try {
                    updatePreferences(renewal, milestone).also {
                        writeReturned = true
                        currentCoroutineContext().ensureActive()
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    currentCoroutineContext().ensureActive()
                    if (binding != bindingSequence || !isOwnerActive()) return@launch
                    // A foreground request coalesced during the failed write was never
                    // submitted; do not silently drop it or automatically enqueue.
                    val schedulingUnknown = resumePendingWhileSaving || before.scheduleStatus != null
                    mutableState.value = before.copy(saving = false,
                        error = "提醒偏好未能保存，已保留原设置，请重试。", notice = null,
                        scheduleStatus = if (schedulingUnknown) EnqueueStatus.UNKNOWN else null)
                    return@launch
                }
                if (binding != bindingSequence || !isOwnerActive()) return@launch
                mutableState.value = NotificationSettingsUiState(loading = false, preferences = persisted)
                resumePendingWhileSaving = false
                try {
                    onPreferencesChanged()
                    submitSchedule()
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    if (binding == bindingSequence && isOwnerActive())
                        mutableState.value = state.value.copy(notice = "提醒偏好已保存，后台安排暂未更新；下次打开应用时会重试。")
                }
            } finally {
                // Record the terminal outcome even if the old owner has already retired.
                // Cancellation after the DataStore call has not returned is not rollback proof.
                attempt.outcome = if (writeReturned) SaveOutcome.SAVED else SaveOutcome.UNKNOWN
                settleSave(attempt, submitAfterReread = true)
            }
        }
    }

    private fun markSaveUnknown() {
        resumePendingWhileSaving = false
        mutableState.value = NotificationSettingsUiState(loading = false,
            error = "之前的提醒偏好保存结果无法确认，请稍后重新打开应用核对。",
            scheduleStatus = EnqueueStatus.UNKNOWN, saveOutcomeUnknown = true)
    }

    /** Called both when a write finishes and when a replacement owner binds. */
    private fun settleSave(attempt: SaveAttempt, submitAfterReread: Boolean): Boolean {
        if (saveAttempt !== attempt || attempt.outcome == SaveOutcome.IN_FLIGHT || !isOwnerActive()) return false
        if (attempt.binding == bindingSequence && !state.value.saving) {
            // The original binding already displayed its success or known failure.
            saveAttempt = null
            resumePendingWhileSaving = false
            return true
        }
        saveAttempt = null
        if (attempt.outcome == SaveOutcome.UNKNOWN) {
            markSaveUnknown()
            return true
        }
        if (attempt.binding == bindingSequence) {
            // A locally cancelled observer after a returned write must not invent feedback.
            markSaveUnknown()
            return true
        }
        mutableState.value = state.value.copy(saving = false)
        refresh()
        if (submitAfterReread && pageVisible) submitSchedule()
        resumePendingWhileSaving = false
        return true
    }

    /** A retired owner or a superseded UI request may not update this page. */
    private fun invalidateSchedule() {
        scheduleSequence++
        scheduleJob?.cancel()
        scheduleJob = null
    }

    /** Rebind a retained ViewModel without accepting results from its prior owner instance. */
    fun rebindSchedule(identity: Any, port: (() -> StateFlow<EnqueueStatus>?)?, isActive: () -> Boolean): Boolean {
        if (bindingIdentity === identity) return false
        bindingIdentity = identity
        bindingSequence++
        requestSequence++
        readJob?.cancel()
        invalidateSchedule()
        schedulePort = port
        isOwnerActive = isActive
        if (isOwnerActive()) {
            // Preserve UNKNOWN until its outcome can be independently established.
            if (!state.value.saveOutcomeUnknown) mutableState.value = state.value.copy(scheduleStatus = null)
            val settled = saveAttempt?.let { settleSave(it, submitAfterReread = false) } ?: false
            // Do not permit a second write while an earlier owner-bound save is unresolved.
            if (!settled && !state.value.saving && !state.value.saveOutcomeUnknown) refresh()
        }
        return true
    }

    fun requestScheduleOnResume() {
        pageVisible = true
        if (state.value.saveOutcomeUnknown) return
        if (state.value.saving) {
            // A single pending signal covers any number of foreground events during the write.
            resumePendingWhileSaving = true
            return
        }
        submitSchedule()
    }

    fun stopScheduleObservation() {
        pageVisible = false
        invalidateSchedule()
        if (isOwnerActive() && !state.value.saveOutcomeUnknown)
            mutableState.value = state.value.copy(scheduleStatus = null)
    }

    private fun submitSchedule() {
        val port = schedulePort ?: return
        if (!isOwnerActive() || state.value.saveOutcomeUnknown || state.value.saving) return
        invalidateSchedule()
        val ticket = scheduleSequence
        if (pageVisible) mutableState.value = state.value.copy(scheduleStatus = EnqueueStatus.PENDING)
        val updates = try { port() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            // A throwing adapter might already have submitted work; its system outcome is unknown.
            if (pageVisible && ticket == scheduleSequence && isOwnerActive())
                mutableState.value = state.value.copy(scheduleStatus = EnqueueStatus.UNKNOWN)
            return
        }
        if (!pageVisible || !isOwnerActive() || ticket != scheduleSequence) return
        if (updates == null) {
            mutableState.value = state.value.copy(scheduleStatus = EnqueueStatus.UNKNOWN)
            return
        }
        scheduleJob = scope.launch {
            updates.collect { value ->
                currentCoroutineContext().ensureActive()
                if (pageVisible && ticket == scheduleSequence && isOwnerActive()) {
                    mutableState.value = state.value.copy(scheduleStatus = value)
                }
            }
        }
    }
}
