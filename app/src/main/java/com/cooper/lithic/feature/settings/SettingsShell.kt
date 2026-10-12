package com.cooper.lithic.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.ZoneId

@Composable
internal fun SettingsShell(recordZone: ZoneId, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        .testTag("settings-screen"), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("设置", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
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
            Text("完整备份与恢复尚未开放", style = MaterialTheme.typography.bodyLarge)
            Text("提醒功能尚未开放", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("当前为 Debug 工程阶段，暂不建议长期保存无法丢失的重要记录",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
