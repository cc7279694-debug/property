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

@Composable
internal fun HomeShell(projectCount: Long, onProjectList: () -> Unit, onCreateProject: () -> Unit,
    subscriptionCount: Long, onSubscriptionList: () -> Unit, onCreateSubscription: () -> Unit, modifier: Modifier = Modifier) {
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
        Text(if (projectCount == 0L && subscriptionCount == 0L) "暂无记录" else "已录入 $projectCount 条长期项目",
            style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("长期项目", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() })
            Text(if (projectCount == 0L) "还没有长期项目" else "查看持有时间、实际投入与收回",
                style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onProjectList,
                modifier = Modifier.heightIn(min = 48.dp).testTag("open-project-list")) {
                Text("查看长期项目", style = MaterialTheme.typography.bodyLarge)
            }
            TextButton(onClick = onCreateProject,
                modifier = Modifier.heightIn(min = 48.dp).testTag("home-create-project")) {
                Text("新增长期项目", style = MaterialTheme.typography.bodyLarge)
            }
        }
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("订阅服务", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() })
            Text(if (subscriptionCount == 0L) "还没有订阅服务" else "已录入 $subscriptionCount 条订阅服务",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onSubscriptionList, modifier = Modifier.heightIn(min = 48.dp).testTag("open-subscription-list")) {
                Text("查看订阅服务", style = MaterialTheme.typography.bodyLarge)
            }
            TextButton(onClick = onCreateSubscription, modifier = Modifier.heightIn(min = 48.dp).testTag("home-create-subscription")) {
                Text("新增订阅服务", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
