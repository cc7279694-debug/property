package com.cooper.lithic.feature.home

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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountText
import com.cooper.lithic.core.design.EntryCard
import com.cooper.lithic.data.local.LongTermProjectEntity

@Composable
internal fun HomeShell(projectCount: Int, onProjectList: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(16.dp).testTag("home-screen"),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("现在", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("当前日均成本", style = MaterialTheme.typography.bodyLarge)
            AmountText(null)
            Text("日均成本汇总尚未开放", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(if (projectCount == 0) "暂无记录" else "已录入 $projectCount 条长期项目",
            style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("长期项目", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() })
            Text(if (projectCount == 0) "还没有长期项目" else "只读查看已录入的项目",
                style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onProjectList,
                modifier = Modifier.heightIn(min = 48.dp).testTag("open-project-list")) {
                Text("查看长期项目", style = MaterialTheme.typography.bodyLarge)
            }
        }
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("订阅服务", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() })
            Text("订阅记录尚未开放", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ProjectListShell(projects: List<LongTermProjectEntity>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().testTag("project-list-screen")) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("长期项目", style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() })
            TextButton(onClick = onBack,
                modifier = Modifier.heightIn(min = 48.dp).testTag("project-list-back")) {
                Text("返回现在", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (projects.isEmpty()) {
                Text("暂无长期项目", style = MaterialTheme.typography.bodyLarge)
            } else {
                Text("已录入 ${projects.size} 条长期项目", style = MaterialTheme.typography.bodyMedium)
                projects.forEach { project ->
                    val dates = project.endedOn?.let { "开始：${project.startedOn}\n结束：$it" }
                        ?: "开始：${project.startedOn}"
                    EntryCard(name = project.name,
                        statusLabel = if (project.endedOn == null) "未记录结束日期" else "已记录结束日期",
                        timeLabel = dates, dailyCost = null)
                }
            }
            Text("详情与编辑尚未开放", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
