package com.cooper.lithic.feature.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.C5InformationButton
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
    onRetryTrend: () -> Unit = {}, onOpenTrendRecord: (OverviewKind, String) -> Unit = { _, _ -> },
    onAddRecord: (() -> Unit)? = null) {
    val trendHasRecords = trendState?.snapshot?.overview?.let {
        it.projects.isNotEmpty() || it.subscriptions.isNotEmpty()
    } == true || trendState?.timeline?.from != null
    val trendHasNotice = trendState?.error != null || trendState?.notice != null
    val showEmpty = reviewShowsEmptyLedger(projectCount, subscriptionCount, hasReadNotice,
        trendLoading = trendState?.loading == true, trendHasNotice = trendHasNotice,
        trendHasRecords = trendHasRecords)
    val header: @Composable () -> Unit = {
        ReviewHeading(projectCount, subscriptionCount,
            hasReadNotice || trendHasNotice || (projectCount == 0L && subscriptionCount == 0L && trendHasRecords)) {
            C5InformationButton("回看的口径", Modifier.testTag("review-information")) {
                Column(Modifier.testTag("review-information-content"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReviewCounts(projectCount, subscriptionCount)
                    Text("没有录入不等于没有花钱；趋势按已录入、已纠正的事实重建，不代表全部生活消费，也不是当日扣款。类别分析分开查看项目与订阅；年度回顾按真实资金年份归属；长期复盘保留各自指标。历史档案只包含已结束项目和已停止订阅，暂停或到期但未停止的订阅仍在订阅列表。")
                }
            }
        }
    }
    val destinations: @Composable () -> Unit = {
        ReviewDestinations(onProjectHistory, onSubscriptionHistory, onLongTermReview,
            onTotalCostTrend, onCategoryAnalysis, onAnnualReview)
    }
    Box(modifier.fillMaxSize().testTag("review-screen")) {
        if (showEmpty) {
            // Ready counts establish emptiness; no synthetic chart, zero amount, or menu wall.
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                item("heading") { header() }
                item("empty") { ReviewEmptyState(onAddRecord) }
            }
        } else if (trendState != null) {
            TotalCostTrendScreen(trendState, onSelectTrendDate, onToggleTrendBreakdown,
                onRetryTrend, onOpenTrendRecord, onBack = {}, reviewHeader = header,
                reviewFooter = destinations, summaryOnly = true)
        } else {
            // No fabricated chart or numbers when a preview/caller has no real trend snapshot.
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp),
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
private fun ReviewEmptyState(onAddRecord: (() -> Unit)?) {
    // An empty archive has an identity, not a blank data card or a fabricated chart.
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 20.dp)
        .testTag("review-empty-state"), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ReviewEmptyIdentity()
        Text("记下一件物品后，在这里看见成本变化",
            Modifier.widthIn(max = 280.dp).testTag("review-record-summary"),
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        onAddRecord?.let { add ->
            Button(add, Modifier.heightIn(min = 48.dp).testTag("review-add-record")) {
                Text("去首页记录")
                Spacer(Modifier.width(8.dp))
                C2Icon(C2Glyph.Chevron)
            }
        }
    }
}

/** Existing local vectors identify the two record types; these shapes contain no record facts. */
@Composable
private fun ReviewEmptyIdentity() {
    Box(Modifier.size(width = 112.dp, height = 88.dp).testTag("review-empty-identity")) {
        Surface(Modifier.align(Alignment.TopCenter).size(64.dp),
            shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant, shadowElevation = 1.dp) {
            Box(contentAlignment = Alignment.Center) { C2Icon(C2Glyph.Review, Modifier.size(32.dp)) }
        }
        Surface(Modifier.align(Alignment.BottomStart).size(40.dp), shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
            Box(contentAlignment = Alignment.Center) { C2Icon(C2Glyph.Project, Modifier.size(20.dp)) }
        }
        Surface(Modifier.align(Alignment.BottomEnd).size(40.dp), shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
            Box(contentAlignment = Alignment.Center) { C2Icon(C2Glyph.Subscription, Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun ReviewHeading(projectCount: Long, subscriptionCount: Long, hasReadNotice: Boolean, information: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("回看", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f).semantics { heading() })
            information()
        }
        val summary = reviewRecordSummary(projectCount, subscriptionCount, hasReadNotice)
        if (summary == ReviewRecordSummary.SNAPSHOT_NOTICE) Text(
            "以下为已取得的记录快照",
            Modifier.testTag("review-record-summary"), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (summary == ReviewRecordSummary.SNAPSHOT_NOTICE) Text(
            "当前有读取或恢复提示；这里只展示已取得的快照，最新记录需重新核对。",
            Modifier.testTag("review-record-notice"), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun ReviewCounts(projectCount: Long, subscriptionCount: Long) {
    Column(Modifier.testTag("review-counts-compact"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$projectCount 条长期项目", Modifier.testTag("review-project-count"))
        Text("$subscriptionCount 条订阅服务", Modifier.testTag("review-subscription-count"))
        Text("含当前和历史；有读取提示时为已取得的快照数量。", Modifier.testTag("review-count-scope"),
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReviewDestinations(onProjectHistory: (() -> Unit)?, onSubscriptionHistory: (() -> Unit)?,
    onLongTermReview: (() -> Unit)?, onTotalCostTrend: (() -> Unit)?, onCategoryAnalysis: (() -> Unit)?,
    onAnnualReview: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        onTotalCostTrend?.let {
            ReviewGroup("review-full-history-group") {
                ReviewLink("全部历史与事件", "完整日期清单和记录来源", C2Glyph.Review, "review-total-cost-trend", it)
            }
        }
        if (onCategoryAnalysis != null || onAnnualReview != null || onLongTermReview != null) {
            Text("换个角度看", style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            ReviewGroup("review-analysis-group") {
                onCategoryAnalysis?.let { ReviewLink("类别分析", "长期项目与订阅分别查看", C2Glyph.Project,
                    "review-category-analysis", it) }
                if (onCategoryAnalysis != null && (onAnnualReview != null || onLongTermReview != null)) ReviewRowDivider()
                onAnnualReview?.let { ReviewLink("年度回顾", "按真实资金年份回看", C2Glyph.Review, "review-annual", it) }
                if (onAnnualReview != null && onLongTermReview != null) ReviewRowDivider()
                onLongTermReview?.let { ReviewLink("长期复盘", "陪伴时间与折算付费时长", C2Glyph.Subscription, "review-long-term", it) }
            }
        }
        if (onProjectHistory != null || onSubscriptionHistory != null) {
            Text("历史档案", style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            ReviewGroup("review-archive-group") {
                onProjectHistory?.let { ReviewLink("已结束长期项目", "保留原记录与真实结算", C2Glyph.Project,
                    "review-project-history", it) }
                if (onProjectHistory != null && onSubscriptionHistory != null) ReviewRowDivider()
                onSubscriptionHistory?.let { ReviewLink("已停止订阅", "暂停、到期未停止仍在订阅列表", C2Glyph.Subscription,
                    "review-subscription-history", it) }
            }
        }
    }
}

@Composable
private fun ReviewGroup(tag: String, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth().testTag(tag), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column { content() }
    }
}

@Composable
private fun ReviewRowDivider() {
    HorizontalDivider(Modifier.padding(start = 56.dp, end = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ReviewLink(title: String, description: String, glyph: C2Glyph, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag)
        .semantics(mergeDescendants = true) { contentDescription = "$title，$description" }
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(Modifier.size(32.dp), shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
            Box(contentAlignment = Alignment.Center) { C2Icon(glyph) }
        }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        C2Icon(C2Glyph.Chevron)
    }
}
