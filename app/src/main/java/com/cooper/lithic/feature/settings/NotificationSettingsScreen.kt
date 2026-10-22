package com.cooper.lithic.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Device state only: none of these values are stored in or restored from the ledger. */
internal data class NotificationPermissionState(
    val requiresRuntimePermission: Boolean,
    val runtimePermissionGranted: Boolean,
    val notificationsEnabled: Boolean,
    val channelEnabled: Boolean = true,
) {
    val canPublish: Boolean
        get() = (!requiresRuntimePermission || runtimePermissionGranted) && notificationsEnabled && channelEnabled
}

@Composable
internal fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    permissionState: NotificationPermissionState,
    onRequestPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotificationSettingsContent(state, permissionState, viewModel::setRenewalEnabled,
        viewModel::setMilestoneEnabled, onRequestPermission, onOpenSystemSettings, onBack,
        viewModel::refresh, modifier)
}

/** Permission callbacks run only after explicit button clicks, never on composition or switch reads. */
@Composable
internal fun NotificationSettingsContent(
    state: NotificationSettingsUiState,
    permissionState: NotificationPermissionState,
    onRenewalChange: (Boolean) -> Unit,
    onMilestoneChange: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        .testTag("notification-settings-screen"), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp).testTag("notification-settings-back")) {
            Text("返回设置")
        }
        Text("提醒设置", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Text("提醒以砺石中已经保存的记录为依据，不代表服务商已经扣款，也不会替您续费或取消服务。",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (state.loading) {
            Text("正在读取提醒偏好", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("notification-preferences-loading"))
        }
        state.preferences?.let { preferences ->
            ReminderSwitch("续费提醒", "在记录中的到期日前提醒，是否送达及具体时间受系统限制。",
                preferences.renewalEnabled, !state.loading && !state.saving, onRenewalChange, "renewal-reminder-switch")
            HorizontalDivider()
            ReminderSwitch("里程碑提醒", "记录项目持有日与日历周年，以及订阅累计付费有效天数；同日合并为一条摘要。",
                preferences.milestoneEnabled, !state.loading && !state.saving, onMilestoneChange, "milestone-reminder-switch")
            Text("记录时区9点后尽力提醒；错过节点仅在含当天的7日窗口内补发。首次纳入、历史纠错和恢复不集中补发旧节点，关闭再开启不清除历史记录。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("milestone-reminder-explanation"))
        }
        if (state.saving) {
            Text("正在保存提醒偏好", style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("notification-preferences-saving").semantics { liveRegion = LiveRegionMode.Polite })
        }
        state.error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("notification-preferences-error").semantics { liveRegion = LiveRegionMode.Polite })
            if (state.preferences == null) {
                TextButton(onClick = onRetry, enabled = !state.loading,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("retry-notification-preferences")) { Text("重新读取") }
            }
        }
        state.notice?.let { message ->
            Text(message, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("notification-preferences-notice").semantics { liveRegion = LiveRegionMode.Polite })
        }

        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("系统通知状态", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            val runtimeDenied = permissionState.requiresRuntimePermission && !permissionState.runtimePermissionGranted
            val status = when {
                runtimeDenied -> "系统通知未授权"
                !permissionState.notificationsEnabled -> "系统通知已关闭"
                !permissionState.channelEnabled -> "续费与里程碑提醒通知渠道已关闭"
                else -> "系统允许发送通知"
            }
            Text(status, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("notification-permission-status"))
            if ((state.preferences?.renewalEnabled == true || state.preferences?.milestoneEnabled == true) && !permissionState.canPublish) {
                Text(when {
                    runtimeDenied -> "应用内已开启，系统通知未授权"
                    !permissionState.notificationsEnabled -> "应用内已开启，系统通知已关闭"
                    else -> "应用内已开启，续费与里程碑提醒通知渠道已关闭"
                }, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("notification-permission-explanation"))
            }
            Text("不授权或关闭通知，仍可新增订阅、录入续费、修改状态、浏览记录以及备份和恢复。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (runtimeDenied) {
                Button(onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("request-notification-permission")) {
                    Text("允许系统通知")
                }
            }
            TextButton(onClick = onOpenSystemSettings,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open-system-notification-settings")) {
                Text("打开系统通知设置")
            }
            Text("系统可能延后或限制后台执行。提醒不保证准点送达，请以实际服务和您保存的记录为准。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReminderSwitch(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    tag: String,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
        .padding(vertical = 8.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
