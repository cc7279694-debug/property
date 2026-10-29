package com.cooper.lithic.feature.settings

import com.cooper.lithic.core.design.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import com.cooper.lithic.core.design.LithicButton as Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
        Column(Modifier.fillMaxWidth()) {
            listOf("long_term_projects" to "长期项目", "project_money_events" to "项目金额记录",
                "subscriptions" to "订阅", "subscription_renewals" to "订阅付款",
                "subscription_state_changes" to "订阅状态记录", "categories" to "类别",
                "local_media_assets" to "图片", "replacements" to "替换关系",
                "notification_receipts" to "提醒回执", "milestone_watermarks" to "里程碑抑制水位").forEach { (key, label) ->
                RestoreCountRow(label, key, preview.currentCounts[key], preview.backupCounts[key])
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        Text("覆盖影响", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() })
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

@Composable
private fun RestoreCountRow(label: String, key: String, current: Long?, backup: Long?) {
    val largeType = LocalDensity.current.fontScale > 1.3f
    val counts = "当前 ${current ?: "未知"} · 备份 ${backup ?: "未知"}"
    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("restore-count-$key")) {
        if (largeType || maxWidth < 320.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(counts, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text(counts, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
