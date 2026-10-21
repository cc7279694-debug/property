package com.cooper.lithic.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.ZoneId

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
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        .testTag("settings-screen"), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("设置", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        onManageCategories?.let { manage ->
            TextButton(onClick = manage, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open-category-management")) {
                Text("管理类别", style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider()
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("记录时区", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics { heading() })
            Text(recordZone.id, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("record-zone"))
            Text("记录时区已保存在当前设备", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("本地保存", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics { heading() })
            Text("已有记录保存在当前设备，可离线读取", style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("完整备份", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text("手动保存全部记录、设置和已登记图片。导出不会改变当前记录；无法完整校验时不会提示成功。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("备份文件可直接读取，请妥善保管。保存位置由您选择。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { onExportBackup?.invoke() }, enabled = onExportBackup != null && backupState.phase == BackupUiPhase.IDLE && restoreState.phase == RestoreUiPhase.IDLE,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("export-complete-backup")) {
                Text(when (backupState.phase) {
                    BackupUiPhase.IDLE -> "导出完整备份"
                    BackupUiPhase.CHOOSING -> "正在选择保存位置"
                    BackupUiPhase.EXPORTING -> "正在导出并校验"
                })
            }
            if (backupState.phase == BackupUiPhase.EXPORTING) {
                TextButton(onClick = onCancelBackup,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("cancel-backup-export")) { Text("取消导出") }
            }
            backupState.message?.let { feedback ->
                Text(feedback, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("backup-export-feedback").semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("恢复完整备份", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text("选择备份后先完整校验并展示摘要。只有您再次确认后才覆盖当前内容，不合并记录。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { onRestoreBackup?.invoke() },
                enabled = onRestoreBackup != null && backupState.phase == BackupUiPhase.IDLE && restoreState.phase == RestoreUiPhase.IDLE,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("restore-complete-backup")) {
                Text(when (restoreState.phase) {
                    RestoreUiPhase.IDLE -> "选择完整备份"
                    RestoreUiPhase.CHOOSING -> "正在选择备份"
                    RestoreUiPhase.VALIDATING -> "正在读取并校验备份"
                    RestoreUiPhase.PREVIEW -> "请核对下方备份摘要"
                    RestoreUiPhase.DISCARDING -> "正在取消恢复"
                    RestoreUiPhase.RESTORING -> "正在安全恢复"
                })
            }
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
                Text(feedback, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("backup-restore-feedback").semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("提醒功能尚未开放", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("当前为 Debug 工程阶段，暂不建议长期保存无法丢失的重要记录",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
