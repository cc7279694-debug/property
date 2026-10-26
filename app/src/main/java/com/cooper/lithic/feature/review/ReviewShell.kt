package com.cooper.lithic.feature.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.domain.report.OverviewKind
import java.time.LocalDate

/** One trend LazyColumn owns scrolling. Counts still come from the navigation's Ready snapshot. */
@Composable
internal fun ReviewShell(projectCount: Long, modifier: Modifier = Modifier, onProjectHistory: (() -> Unit)? = null,
    onSubscriptionHistory: (() -> Unit)? = null, onLongTermReview: (() -> Unit)? = null,
    onTotalCostTrend: (() -> Unit)? = null, onCategoryAnalysis: (() -> Unit)? = null,
    onAnnualReview: (() -> Unit)? = null, subscriptionCount: Long = 0L,
    hasReadNotice: Boolean = false, trendState: TotalCostTrendUiState? = null,
    onSelectTrendDate: (LocalDate) -> Unit = {}, onToggleTrendBreakdown: () -> Unit = {},
    onRetryTrend: () -> Unit = {}, onOpenTrendRecord: (OverviewKind, String) -> Unit = { _, _ -> }) {
    val header: @Composable () -> Unit = {
        ReviewHeading(projectCount, subscriptionCount, hasReadNotice)
    }
    val destinations: @Composable () -> Unit = {
        ReviewDestinations(onProjectHistory, onSubscriptionHistory, onLongTermReview,
            onTotalCostTrend, onCategoryAnalysis, onAnnualReview)
    }
    Box(modifier.fillMaxSize().testTag("review-screen")) {
        if (trendState != null) {
            TotalCostTrendScreen(trendState, onSelectTrendDate, onToggleTrendBreakdown,
                onRetryTrend, onOpenTrendRecord, onBack = {}, reviewHeader = header,
                reviewFooter = destinations, summaryOnly = true)
        } else {
            // No fabricated chart or numbers when a preview/caller has no real trend snapshot.
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                item("heading") { header() }
                item("unavailable") { Text("成本趋势尚未读取。", Modifier.testTag("review-trend-unavailable"),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item("destinations") { destinations() }
            }
        }
    }
}

@Composable
private fun ReviewHeading(projectCount: Long, subscriptionCount: Long, hasReadNotice: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("回看", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        val summary = reviewRecordSummary(projectCount, subscriptionCount, hasReadNotice)
        Text(when (summary) {
            ReviewRecordSummary.EMPTY -> "暂无长期项目或订阅记录"
            ReviewRecordSummary.RECORDED -> "总日均成本与历史记录"
            ReviewRecordSummary.SNAPSHOT_NOTICE -> "以下为已取得的记录快照"
        }, Modifier.testTag("review-record-summary"), style = MaterialTheme.typography.bodyLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("$projectCount 条长期项目", Modifier.weight(1f).testTag("review-project-count"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$subscriptionCount 条订阅服务", Modifier.weight(1f).testTag("review-subscription-count"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("数量包含当前和历史记录。", Modifier.testTag("review-count-scope"),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (summary == ReviewRecordSummary.SNAPSHOT_NOTICE) Text(
            "当前有读取或恢复提示；这里只展示已取得的快照，最新记录需重新核对。",
            Modifier.testTag("review-record-notice"), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun ReviewDestinations(onProjectHistory: (() -> Unit)?, onSubscriptionHistory: (() -> Unit)?,
    onLongTermReview: (() -> Unit)?, onTotalCostTrend: (() -> Unit)?, onCategoryAnalysis: (() -> Unit)?,
    onAnnualReview: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        onTotalCostTrend?.let { ReviewLink("全部历史与事件", "完整日期清单和记录来源", C2Glyph.Review,
            "review-total-cost-trend", it) }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text("换个角度看", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        onCategoryAnalysis?.let { ReviewLink("类别分析", "长期项目与订阅分别查看", C2Glyph.Project,
            "review-category-analysis", it) }
        onAnnualReview?.let { ReviewLink("年度回顾", "按真实资金年份回看", C2Glyph.Review, "review-annual", it) }
        onLongTermReview?.let { ReviewLink("长期复盘", "陪伴时间与折算付费时长", C2Glyph.Review, "review-long-term", it) }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text("历史档案", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        onProjectHistory?.let { ReviewLink("已结束长期项目", "保留原记录与真实结算", C2Glyph.Project,
            "review-project-history", it) }
        onSubscriptionHistory?.let { ReviewLink("已停止订阅", "暂停、到期未停止的仍在订阅列表", C2Glyph.Subscription,
            "review-subscription-history", it) }
    }
}

@Composable
private fun ReviewLink(title: String, description: String, glyph: C2Glyph, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag(tag)
        .clickable(role = Role.Button, onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        C2Icon(glyph)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        C2Icon(C2Glyph.Chevron)
    }
}
