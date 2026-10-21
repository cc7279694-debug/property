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
    onSubscriptionHistory: (() -> Unit)? = null, onLongTermReview: (() -> Unit)? = null) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        .testTag("review-screen"), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("回看", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Text(if (projectCount == 0L) "暂无长期项目记录" else "已录入 $projectCount 条长期项目（含历史）",
            style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider()
        onLongTermReview?.let { openReview ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(openReview, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("review-long-term")) {
                    Text("长期复盘", style = MaterialTheme.typography.bodyLarge)
                }
                Text("当前和历史一起看：陪伴时间、日均成本与订阅折算付费时长。", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("历史档案", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text("仅包含已结束的长期项目和已停止的订阅。暂停、到期但未停止的订阅仍在服务列表。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            onProjectHistory?.let { openHistory ->
                OutlinedButton(openHistory, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("review-project-history")) {
                    Text("已结束长期项目", style = MaterialTheme.typography.bodyLarge)
                }
            }
            onSubscriptionHistory?.let { openHistory ->
                OutlinedButton(openHistory, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("review-subscription-history")) {
                    Text("已停止订阅", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
