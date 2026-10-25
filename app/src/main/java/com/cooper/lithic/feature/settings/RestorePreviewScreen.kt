package com.cooper.lithic.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.data.backup.RestorePreview

@Composable
internal fun RestorePreviewScreen(preview: RestorePreview, onConfirm: () -> Unit, onDiscard: () -> Unit,
    busy: Boolean = false) {
    Column(Modifier.fillMaxWidth().testTag("restore-preview"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("核对备份", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("备份创建时间：${preview.createdAt}", style = MaterialTheme.typography.bodyMedium)
        Text("备份记录时区：${preview.recordZoneId}", style = MaterialTheme.typography.bodyMedium)
        Text("记录数量对照：当前账本 → 备份内容", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() })
        Text("当前数量为校验时快照；当前记录若改变，请重新选择备份并核对。",
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("restore-count-snapshot-note"))
        listOf("long_term_projects" to "长期项目", "project_money_events" to "项目金额记录",
            "subscriptions" to "订阅", "subscription_renewals" to "订阅付款",
            "subscription_state_changes" to "订阅状态记录", "categories" to "类别",
            "local_media_assets" to "图片", "replacements" to "替换关系",
            "notification_receipts" to "提醒回执", "milestone_watermarks" to "里程碑抑制水位").forEach { (key, label) ->
            Column(Modifier.testTag("restore-count-$key")) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text("当前 ${preview.currentCounts[key] ?: "未知"} · 备份 ${preview.backupCounts[key] ?: "未知"}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("完整覆盖当前记录，不合并。当前记录、图片和设置将被备份内容替换。",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        Text("项目与订阅均以备份内容为准，备份中没有的当前记录也不会合并保留。记录时区和提醒偏好等设置会一起替换，系统通知权限不会从备份取得。",
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("restore-overwrite-scope"))
        Text("确认前可取消；如需保留当前内容，请先取消并导出当前完整备份。恢复后按今天重新计算天数和状态。",
            style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onConfirm, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("confirm-backup-restore")) { Text("确认覆盖恢复") }
        TextButton(onClick = onDiscard, enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("discard-backup-restore")) { Text("取消恢复，保留当前记录") }
    }
}
