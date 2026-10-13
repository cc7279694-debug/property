package com.cooper.lithic.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun ReviewShell(projectCount: Long, modifier: Modifier = Modifier, onProjectHistory: (() -> Unit)? = null,
    onSubscriptionHistory: (() -> Unit)? = null) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        .testTag("review-screen"), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("回看", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Text(if (projectCount == 0L) "暂无记录" else "已录入 $projectCount 条长期项目",
            style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider()
        onSubscriptionHistory?.let { openHistory ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(openHistory, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("review-subscription-history")) {
                    Text("已停止订阅", style = MaterialTheme.typography.bodyLarge)
                }
                Text("查看原服务、真实付款和状态历史，可从原身份重新开通。", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        for (label in listOf("长期复盘", "历史档案", "成本趋势", "类别分析", "年度回顾")) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (label == "历史档案" && onProjectHistory != null) {
                    OutlinedButton(onProjectHistory, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("review-project-history")) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text("查看已结束的长期项目与真实金额记录", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.semantics { heading() })
                    Text("尚未开放", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
