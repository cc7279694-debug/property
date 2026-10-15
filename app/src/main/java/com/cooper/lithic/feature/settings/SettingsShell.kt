package com.cooper.lithic.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicCard
import com.cooper.lithic.core.design.PageHeader
import java.time.ZoneId

@Composable
internal fun SettingsShell(recordZone: ZoneId, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val installedVersion = remember(context) {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName ?: "版本信息不可用"
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        .testTag("settings-screen"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageHeader("设置")
        Surface(Modifier.fillMaxWidth().testTag("development-warning"), shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("开发体验版", style = MaterialTheme.typography.titleMedium)
                Text("完整备份与恢复尚未开放", style = MaterialTheme.typography.bodyLarge)
                Text("请勿将重要记录仅保存在本App中。", style = MaterialTheme.typography.bodyMedium)
            }
        }
        SectionTitle("记录与数据")
        LithicCard {
            SettingsValue("记录时区", recordZone.id, "record-zone")
            Text("记录时区已保存在当前设备", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            SettingsValue("本地保存", "已有记录保存在当前设备，可离线读取")
        }
        SectionTitle("关于")
        LithicCard {
            SettingsValue("应用", "砺石 LITHIC")
            HorizontalDivider()
            SettingsValue("当前安装版本", installedVersion, "installed-version")
            Text("真实记录 · 长期陪伴", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun SettingsValue(label: String, value: String, tag: String? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, if (tag == null) Modifier else Modifier.testTag(tag), style = MaterialTheme.typography.bodyLarge)
    }
}
