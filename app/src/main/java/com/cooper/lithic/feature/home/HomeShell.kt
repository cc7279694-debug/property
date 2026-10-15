package com.cooper.lithic.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.app.HomePreview
import com.cooper.lithic.core.design.EmptyState
import com.cooper.lithic.core.design.EntryCard
import com.cooper.lithic.core.design.LithicCard
import com.cooper.lithic.core.design.LithicIcon
import com.cooper.lithic.core.design.PageHeader
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.feature.media.CoverThumbnail
import com.cooper.lithic.feature.project.projectStatusLabel
import com.cooper.lithic.feature.subscription.subscriptionStatusLabel

@Composable
internal fun HomeShell(projectCount: Long, onProjectList: () -> Unit, onCreateProject: () -> Unit,
    subscriptionCount: Long, onSubscriptionList: () -> Unit, onCreateSubscription: () -> Unit, modifier: Modifier = Modifier,
    preview: HomePreview? = null, refreshing: Boolean = false, hasReadError: Boolean = false,
    onOpenProject: (String) -> Unit = {}, onOpenSubscription: (String) -> Unit = {},
    onProjectHistory: () -> Unit = {}, onSubscriptionHistory: () -> Unit = {}, mediaRepository: MediaRepository? = null) {
    var showAddMenu by remember { mutableStateOf(false) }
    val hasRecords = projectCount > 0L || subscriptionCount > 0L
    val confirmedEmpty = !hasRecords && !refreshing && !hasReadError
    Box(modifier.fillMaxSize().testTag("home-screen")) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            PageHeader("现在", subtitle = "记录长期拥有的物品与持续付费的服务。")
            if (hasReadError) Text("当前显示上次读取结果", Modifier.testTag("home-stale-data"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            if (refreshing) Text("正在刷新本地记录", Modifier.testTag("home-refreshing"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (confirmedEmpty) {
                LithicCard {
                    EmptyState("从第一条记录开始", "暂无记录", icon = LithicIcon.Project)
                    Button({ showAddMenu = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home-first-record")) {
                        LithicIcon(LithicIcon.Add, null, tint = MaterialTheme.colorScheme.onPrimary)
                        Text("新增记录", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LithicCard(modifier = Modifier.testTag("home-record-overview"), emphasis = true) {
                    Text("记录概览", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    Text("已录入 $projectCount 条长期项目", style = MaterialTheme.typography.bodyLarge)
                    Text("已录入 $subscriptionCount 条订阅服务", style = MaterialTheme.typography.bodyLarge)
                    Text("数量包含历史与已停止记录", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PreviewHeading("我的长期项目", "open-project-list", onProjectList)
                val items = preview?.projects.orEmpty().take(3)
                items.forEach { detail ->
                    key(detail.project.id) {
                        val time = "开始：${detail.project.startedOn}" +
                            if (detail.cost.unavailableReason == null) "\n已持有 ${detail.cost.heldDays} 天" else ""
                        val cover: (@Composable () -> Unit)? = if (detail.project.coverAssetId == null) null else {
                            { CoverThumbnail(detail.project.coverAssetId, mediaRepository, "home-project-${detail.project.id}") }
                        }
                        EntryCard(detail.project.name, projectStatusLabel(detail), time, detail.cost.dailyRate,
                            Modifier.testTag("home-project-${detail.project.id}"),
                            onClick = { onOpenProject(detail.project.id) }, cover = cover)
                    }
                }
                if (items.isEmpty()) {
                    PreviewEmpty(projectCount, preview?.currentProjectCount, refreshing, hasReadError,
                        "还没有长期项目", "查看历史档案", "home-project-history", onProjectHistory)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PreviewHeading("我的订阅", "open-subscription-list", onSubscriptionList)
                val items = preview?.subscriptions.orEmpty().take(3)
                items.forEach { detail ->
                    key(detail.subscription.id) {
                        val cover: (@Composable () -> Unit)? = if (detail.subscription.coverAssetId == null) null else {
                            { CoverThumbnail(detail.subscription.coverAssetId, mediaRepository, "home-subscription-${detail.subscription.id}") }
                        }
                        EntryCard(detail.subscription.name, subscriptionStatusLabel(detail.cost.displayStatus),
                            "折算付费时长：${detail.cost.standardPurchasedDays}天\n历史平均日成本", detail.cost.historicalAverage,
                            Modifier.testTag("home-subscription-${detail.subscription.id}"),
                            onClick = { onOpenSubscription(detail.subscription.id) }, cover = cover,
                            placeholderIcon = LithicIcon.Subscription)
                    }
                }
                if (items.isEmpty()) {
                    PreviewEmpty(subscriptionCount, preview?.managedSubscriptionCount, refreshing, hasReadError,
                        "还没有订阅服务", "查看已停止订阅", "home-subscription-history", onSubscriptionHistory)
                }
            }
        }
        if (hasRecords) FloatingActionButton({ showAddMenu = true }, Modifier.align(Alignment.BottomEnd)
            .padding(24.dp).testTag("home-add-record"), containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary) {
            LithicIcon(LithicIcon.Add, "新增记录", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
    if (showAddMenu) AddRecordSheet({ showAddMenu = false }, onCreateProject, onCreateSubscription)
}

@Composable
private fun PreviewHeading(title: String, tag: String, onViewAll: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        TextButton(onViewAll, Modifier.heightIn(min = 48.dp).testTag(tag)) {
            Text("查看全部")
            LithicIcon(LithicIcon.ChevronRight, null)
        }
    }
}

@Composable
private fun PreviewEmpty(identityCount: Long, currentCount: Long?, refreshing: Boolean, hasReadError: Boolean,
    emptyMessage: String, historyLabel: String, historyTag: String, onHistory: () -> Unit) {
    LithicCard {
        Text(when {
            hasReadError -> "请重试读取本地记录"
            refreshing -> "正在读取本地记录"
            identityCount == 0L -> emptyMessage
            currentCount == 0L -> "当前列表没有记录"
            else -> "查看全部记录"
        }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (identityCount > 0L && currentCount == 0L && !hasReadError && !refreshing) {
            TextButton(onHistory, Modifier.heightIn(min = 48.dp).testTag(historyTag)) { Text(historyLabel) }
        }
    }
}
