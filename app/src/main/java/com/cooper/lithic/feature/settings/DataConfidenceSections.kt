package com.cooper.lithic.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Section
import com.cooper.lithic.core.design.C2SettingRow
import com.cooper.lithic.core.design.LithicTextButton
import com.cooper.lithic.core.design.UiFeedbackText
import com.cooper.lithic.core.design.UiMessageTone
import com.cooper.lithic.data.backup.BackupInspectionStatus
import com.cooper.lithic.data.media.PhotoStorageReadFailure

@Composable
internal fun BackupInspectionFeedback(state: BackupInspectionUiState, onCancel: () -> Unit) {
    if (state.phase == BackupInspectionPhase.CHECKING) {
        LithicTextButton(onCancel, Modifier.heightIn(min = 48.dp).testTag("cancel-backup-inspection"),
            enabled = !state.cancelling) { Text("取消检查") }
    }
    state.message?.let { UiFeedbackText(it, state.messageTone, "backup-inspection-feedback") }
    val result = state.result
    if (result?.status == BackupInspectionStatus.VERIFIED) result.summary?.let { summary ->
        Column(Modifier.fillMaxWidth().testTag("backup-inspection-summary"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("格式 ${summary.formatVersion} · ${summary.createdAt}", style = MaterialTheme.typography.bodyMedium)
            Text("物品 ${summary.recordCounts["long_term_projects"] ?: 0L} · 订阅 ${summary.recordCounts["subscriptions"] ?: 0L} · 照片 ${summary.mediaCount}",
                style = MaterialTheme.typography.bodyMedium)
            Text("照片登记长度合计 ${summary.mediaBytes} 字节", style = MaterialTheme.typography.bodyMedium)
            Text("结构、记录关联、长度、校验和及媒体规则均通过。实际恢复尚未验证。",
                Modifier.testTag("backup-inspection-limits"), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun PhotoStorageSection(state: PhotoStorageUiState, onRefresh: (() -> Unit)?) {
    C2Section("照片空间管理", Modifier.testTag("photo-storage-section")) {
        when (state) {
            PhotoStorageUiState.Idle -> Text("尚未读取照片登记信息。", style = MaterialTheme.typography.bodyMedium)
            PhotoStorageUiState.Loading -> Text("正在读取照片登记信息…", Modifier.testTag("photo-storage-loading"),
                style = MaterialTheme.typography.bodyMedium)
            is PhotoStorageUiState.Unavailable -> UiFeedbackText(when (state.reason) {
                PhotoStorageReadFailure.OWNER_UNAVAILABLE -> "当前记录已切换或尚未就绪，照片统计暂不可用。"
                PhotoStorageReadFailure.INCONSISTENT_METADATA -> "照片登记或引用信息未能一致核对，暂不显示数量。"
                PhotoStorageReadFailure.READ_FAILED -> "照片登记信息暂时无法读取，不能据此判断照片数量。"
            }, UiMessageTone.WARNING, "photo-storage-unavailable")
            is PhotoStorageUiState.Ready -> {
                val s = state.summary
                C2SettingRow("已登记照片", C2Glyph.Project, trailing = "${s.registeredCount} 张", trailingTag = "photo-storage-registered")
                C2SettingRow("当前记录引用", C2Glyph.Project, subtitle = "含历史项目与订阅，照片去重",
                    trailing = "${s.referencedCount} 张", trailingTag = "photo-storage-referenced")
                C2SettingRow("历史未引用照片", C2Glyph.Review, trailing = "${s.historicalCount} 张", trailingTag = "photo-storage-historical")
                C2SettingRow("历史照片登记体积", C2Glyph.Info, trailing = "${s.estimatedHistoricalBytes} 字节",
                    trailingTag = "photo-storage-historical-bytes")
                C2SettingRow("预计登记体积", C2Glyph.Info, trailing = "${s.estimatedRegisteredBytes} 字节", trailingTag = "photo-storage-bytes")
            }
        }
        Text("按登记长度估算。未逐张验证，未计临时副本和外部备份。",
            Modifier.testTag("photo-storage-estimate-note"), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("取消显示只解除封面引用。历史照片可能仍占空间，并纳入完整备份。",
            Modifier.testTag("photo-storage-retention-note"), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("登记体积与实际磁盘占用、可释放空间有差异。暂不支持逐张查看或清理。",
            Modifier.testTag("photo-storage-cleanup-unavailable"), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onRefresh != null) LithicTextButton(onRefresh, Modifier.heightIn(min = 48.dp).testTag("refresh-photo-storage"),
            enabled = state != PhotoStorageUiState.Loading) { Text("重新读取") }
    }
}
