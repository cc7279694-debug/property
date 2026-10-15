package com.cooper.lithic.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicIcon
import com.cooper.lithic.core.design.PageHeader

@Composable
@Suppress("UNUSED_PARAMETER") // The shell has no reliable per-history count; never infer one from all projects.
internal fun ReviewShell(projectCount: Long, modifier: Modifier = Modifier, onProjectHistory: (() -> Unit)? = null,
    onSubscriptionHistory: (() -> Unit)? = null) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("review-screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PageHeader("回看", subtitle = "查看历史记录与变更")
        onSubscriptionHistory?.let {
            HistoryEntry("已停止订阅", "查看原服务、真实付款和状态历史，可从原身份重新开通。",
                LithicIcon.Subscription, "review-subscription-history", it)
        }
        onProjectHistory?.let {
            HistoryEntry("历史档案", "查看已结束的长期项目与真实金额记录",
                LithicIcon.Project, "review-project-history", it)
        }
    }
}

@Composable
private fun HistoryEntry(title: String, description: String, icon: LithicIcon, tag: String, onClick: () -> Unit) {
    Card(onClick, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(48.dp), shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    LithicIcon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LithicIcon(LithicIcon.ChevronRight, null)
        }
    }
}
