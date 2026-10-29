package com.cooper.lithic.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.cooper.lithic.core.design.LithicIconButton as IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.*
import java.time.ZoneId
import com.cooper.lithic.feature.appearance.AppearanceThemePicker

@Composable
internal fun SettingsShell(
    recordZone: ZoneId,
    modifier: Modifier = Modifier,
    backupState: BackupExportUiState = BackupExportUiState(),
    onExportBackup: (() -> Unit)? = null,
    onCancelBackup: () -> Unit = {},
    restoreState: BackupRestoreUiState = BackupRestoreUiState(),
    onRestoreBackup: (() -> Unit)? = null,
    onCancelRestore: () -> Unit = {},
    onConfirmRestore: () -> Unit = {},
    onDiscardRestore: () -> Unit = {},
    onManageCategories: (() -> Unit)? = null,
    onNotificationClick: (() -> Unit)? = null,
    onCalculationExplanation: (() -> Unit)? = null,
    onAboutLithic: (() -> Unit)? = null,
) {
    var informationOpen by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = LithicSpacing.page, vertical = 16.dp)
        .testTag("settings-screen"), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("设置", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f).semantics { heading() })
            IconButton({ informationOpen = true }, Modifier.testTag("settings-information")) {
                C2Icon(C2Glyph.Info, description = "查看本地保存与备份说明")
            }
        }
        C2Section("外观") { AppearanceThemePicker() }
        C2Section("记录管理") {
            onManageCategories?.let { manage ->
                C2SettingRow("管理类别", C2Glyph.Project, Modifier.testTag("open-category-management"), onClick = manage)
                HorizontalDivider()
            }
            C2SettingRow("记录时区", C2Glyph.Now, trailing = recordZone.id, trailingTag = "record-zone")
        }
        onNotificationClick?.let { open ->
            C2Section("提醒") {
                C2SettingRow("提醒设置", C2Glyph.Info, Modifier.testTag("open-notification-settings"), onClick = open)
            }
        }
        C2Section("备份与恢复") {
            C2SettingRow(when (backupState.phase) {
                BackupUiPhase.IDLE -> "导出完整备份"
                BackupUiPhase.CHOOSING -> "正在选择保存位置"
                BackupUiPhase.EXPORTING -> "正在导出并校验"
            }, C2Glyph.Project, Modifier.testTag("export-complete-backup"),
                subtitle = "文件未加密，请妥善保管",
                enabled = onExportBackup != null && backupState.phase == BackupUiPhase.IDLE && restoreState.phase == RestoreUiPhase.IDLE,
                onClick = { onExportBackup?.invoke() })
            if (backupState.phase == BackupUiPhase.EXPORTING) {
                TextButton(onClick = onCancelBackup,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("cancel-backup-export")) { Text("取消导出") }
            }
            backupState.message?.let { feedback ->
                UiFeedbackText(feedback, backupState.messageTone, "backup-export-feedback")
            }
            HorizontalDivider()
            C2SettingRow(when (restoreState.phase) {
                RestoreUiPhase.IDLE -> "选择完整备份"
                RestoreUiPhase.CHOOSING -> "正在选择备份"
                RestoreUiPhase.VALIDATING -> "正在读取并校验备份"
                RestoreUiPhase.PREVIEW -> "请核对下方备份摘要"
                RestoreUiPhase.DISCARDING -> "正在取消恢复"
                RestoreUiPhase.RESTORING -> "正在安全恢复"
            }, C2Glyph.Review, Modifier.testTag("restore-complete-backup"),
                subtitle = "完整覆盖，不合并；先预览再确认",
                enabled = onRestoreBackup != null && backupState.phase == BackupUiPhase.IDLE && restoreState.phase == RestoreUiPhase.IDLE,
                onClick = { onRestoreBackup?.invoke() })
            if (restoreState.phase == RestoreUiPhase.VALIDATING) {
                TextButton(onClick = onCancelRestore,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("cancel-backup-restore")) { Text("取消校验") }
            }
            if (restoreState.phase == RestoreUiPhase.PREVIEW) {
                restoreState.preview?.let { RestorePreviewScreen(it, onConfirmRestore, onDiscardRestore) }
            }
            if (restoreState.phase == RestoreUiPhase.RESTORING) {
                Text("正在切换完整本地记录，请稍候。完成前不会开始另一项备份或恢复。",
                    style = MaterialTheme.typography.bodyMedium)
            }
            restoreState.message?.let { feedback ->
                UiFeedbackText(feedback, restoreState.messageTone, "backup-restore-feedback")
            }
        }
        C2Section("说明与关于") {
            onCalculationExplanation?.let { open ->
                C2SettingRow("成本计算说明", C2Glyph.Info, Modifier.testTag("open-calculation-explanation"), onClick = open)
            }
            onAboutLithic?.let { open ->
                HorizontalDivider()
                C2SettingRow("关于砺石", C2Glyph.Settings, Modifier.testTag("open-about-lithic"), onClick = open)
            }
        }
    }
    if (informationOpen) AlertDialog(onDismissRequest = { informationOpen = false },
        title = { Text("本地保存与备份") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).testTag("settings-information-content"),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("记录主要保存在当前设备，可离线使用。卸载可能导致本地记录丢失，请手动备份。")
                Text("完整备份包含记录、设置和已登记图片；文件未加密，请妥善保管。没有自动云同步。")
                Text("外观选择仅属于本机显示偏好，不包含在完整备份的业务设置中。")
                if (backupState.phase == BackupUiPhase.IDLE && backupState.feedback == BackupExportFeedback.NOT_STARTED) {
                    Text("尚未开始导出。选择保存位置后，只有完整写入并校验通过才会提示成功。",
                        modifier = Modifier.testTag("backup-not-started"))
                }
                Text("恢复先校验与预览，再明确确认完整覆盖当前记录、图片和设置，不合并。请先核对预览中的当前与备份内容。")
                Text("续费与里程碑提醒不保证准点送达。停止App中的订阅记录不会取消服务商扣费。")
            }
        }, confirmButton = { TextButton({ informationOpen = false }, Modifier.testTag("settings-information-close")) { Text("知道了") } })
}
