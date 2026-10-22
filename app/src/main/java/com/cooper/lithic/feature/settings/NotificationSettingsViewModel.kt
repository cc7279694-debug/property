package com.cooper.lithic.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cooper.lithic.data.settings.ReminderPreferences
import com.cooper.lithic.data.settings.SettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class NotificationSettingsUiState(
    val loading: Boolean = true,
    val preferences: ReminderPreferences? = null,
    val saving: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

/** This model is owned by the existing generation UI scope, not a new DataStore owner. */
internal class NotificationSettingsViewModel(
    settings: SettingsStore,
    onPreferencesChanged: () -> Unit = {},
) : ViewModel() {
    private val controller = NotificationSettingsController(viewModelScope,
        settings::reminderPreferences,
        { renewal, milestone -> settings.updateReminderPreferences(renewal, milestone) },
        onPreferencesChanged)
    val state = controller.state
    fun refresh() = controller.refresh()
    fun setRenewalEnabled(enabled: Boolean) = controller.setRenewalEnabled(enabled)
    fun setMilestoneEnabled(enabled: Boolean) = controller.setMilestoneEnabled(enabled)
}

/** Failed reads have no editable fallback; changes are displayed only after persistence. */
internal class NotificationSettingsController(
    private val scope: CoroutineScope,
    private val readPreferences: suspend () -> ReminderPreferences,
    private val updatePreferences: suspend (Boolean?, Boolean?) -> ReminderPreferences,
    private val onPreferencesChanged: () -> Unit = {},
) {
    private val mutableState = MutableStateFlow(NotificationSettingsUiState())
    val state = mutableState.asStateFlow()
    private var readJob: Job? = null
    private var requestSequence = 0L

    init { refresh() }

    fun refresh() {
        if (state.value.saving) return
        val ticket = ++requestSequence
        readJob?.cancel()
        mutableState.value = NotificationSettingsUiState()
        readJob = scope.launch {
            try {
                val preferences = readPreferences()
                currentCoroutineContext().ensureActive()
                if (ticket == requestSequence) {
                    mutableState.value = NotificationSettingsUiState(loading = false, preferences = preferences)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                if (ticket == requestSequence) {
                    mutableState.value = NotificationSettingsUiState(loading = false,
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
        if (before.loading || before.saving) return
        if ((renewal == null || renewal == preferences.renewalEnabled) &&
            (milestone == null || milestone == preferences.milestoneEnabled)) return
        mutableState.value = before.copy(saving = true, error = null, notice = null)
        scope.launch {
            val persisted = try {
                updatePreferences(renewal, milestone).also { currentCoroutineContext().ensureActive() }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                mutableState.value = before.copy(saving = false,
                    error = "提醒偏好未能保存，已保留原设置，请重试。", notice = null)
                return@launch
            }
            mutableState.value = NotificationSettingsUiState(loading = false, preferences = persisted)
            try {
                onPreferencesChanged()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutableState.value = state.value.copy(notice = "提醒偏好已保存，后台安排暂未更新；下次打开应用时会重试。")
            }
        }
    }
}
