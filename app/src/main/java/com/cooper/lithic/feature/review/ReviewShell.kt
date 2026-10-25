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
    onSubscriptionHistory: (() -> Unit)? = null, onLongTermReview: (() -> Unit)? = null,
    onTotalCostTrend: (() -> Unit)? = null, onCategoryAnalysis: (() -> Unit)? = null,
    onAnnualReview: (() -> Unit)? = null, subscriptionCount: Long = 0L,
    hasReadNotice: Boolean = false) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        .testTag("review-screen"), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("回看", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val summary = reviewRecordSummary(projectCount, subscriptionCount, hasReadNotice)
            Text(when (summary) {
                ReviewRecordSummary.EMPTY -> "暂无长期项目或订阅记录"
                ReviewRecordSummary.RECORDED -> "回看已录入的长期项目和订阅"
                ReviewRecordSummary.SNAPSHOT_NOTICE -> "以下为已取得的记录快照"
            }, Modifier.testTag("review-record-summary"), style = MaterialTheme.typography.titleMedium)
            Text("已录入 $projectCount 条长期项目（含历史）", Modifier.testTag("review-project-count"),
                style = MaterialTheme.typography.bodyLarge)
            Text("已录入 $subscriptionCount 条订阅服务（含历史）", Modifier.testTag("review-subscription-count"),
                style = MaterialTheme.typography.bodyLarge)
            Text("数量包括当前和历史记录，不等于当前参与日均成本计算的数量。",
                Modifier.testTag("review-count-scope"), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (summary == ReviewRecordSummary.SNAPSHOT_NOTICE) Text(
                "当前有读取或恢复提示，请先查看上方说明；这里仅展示已取得的快照，最新记录需重新核对。",
                Modifier.testTag("review-record-notice"), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (summary == ReviewRecordSummary.EMPTY) Text("可先在“现在”添加记录，再回到这里查看。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        onCategoryAnalysis?.let { open ->
            OutlinedButton(open, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("review-category-analysis")) {
                Text("类别分析", style = MaterialTheme.typography.bodyLarge)
            }
        }
        onAnnualReview?.let { open ->
            OutlinedButton(open, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("review-annual")) {
                Text("年度回顾", style = MaterialTheme.typography.bodyLarge)
            }
        }
        onTotalCostTrend?.let { openTrend ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(openTrend, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("review-total-cost-trend")) {
                    Text("成本趋势", style = MaterialTheme.typography.bodyLarge)
                }
                Text("按已录入事实回看总日均成本，可展开长期项目和订阅贡献。不是当天实际扣款。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
