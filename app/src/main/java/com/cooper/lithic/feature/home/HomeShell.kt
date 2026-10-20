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
import com.cooper.lithic.domain.report.CategoryDailyCost
import com.cooper.lithic.domain.report.OverviewResult

@Composable
internal fun HomeShell(projectCount: Long, onProjectList: () -> Unit, onCreateProject: () -> Unit,
    subscriptionCount: Long, onSubscriptionList: () -> Unit, onCreateSubscription: () -> Unit,
    modifier: Modifier = Modifier, overview: OverviewResult? = null) {
    val recordedProjects = overview?.projectCount ?: projectCount
    val recordedSubscriptions = overview?.subscriptionCount ?: subscriptionCount
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(16.dp).testTag("home-screen"),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("现在", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("当前日均成本", style = MaterialTheme.typography.bodyLarge)
            AmountText(overview?.takeIf { it.hasRecords }?.totalRate,
                modifier = Modifier.testTag("home-total-rate"))
            overview?.let {
                Text("截至 ${it.asOf} · 记录时区日期", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (overview == null) {
                Text("当前日均成本尚未取得", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("仅含已录入且当前有效的项目，按本应用口径计算；不是今天实际扣款。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(if (recordedProjects == 0L && recordedSubscriptions == 0L) "暂无记录" else "已录入 $recordedProjects 条长期项目",
            style = MaterialTheme.typography.bodyLarge)
        if (overview?.hasRecords == true) {
            Text("记录数量包含历史记录；金额仅计当前有效记录。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (overview?.hasRecords == true && overview.activeProjectCount == 0L && overview.activeSubscriptionCount == 0L) {
            Text("暂无当前有效记录；已结束、暂停、已到期或尚未生效的记录不计入当前成本。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("home-no-active-records"))
        }
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("长期项目", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() })
            AmountText(overview?.takeIf { it.projectCount > 0L }?.projectRate,
                modifier = Modifier.testTag("home-project-rate"), style = MaterialTheme.typography.titleLarge)
            Text(when {
                recordedProjects == 0L -> "还没有长期项目"
                overview?.activeProjectCount == 0L -> "当前无有效长期项目；已结束或尚未开始的记录不计入当前成本。"
                else -> "查看持有时间、实际投入与收回"
            },
                style = MaterialTheme.typography.bodyMedium)
            overview?.let { CategoryCosts(it.projectCategories, "home-project-category") }
            Button(onClick = onProjectList,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open-project-list")) {
                Text("查看长期项目", style = MaterialTheme.typography.bodyLarge)
            }
            TextButton(onClick = onCreateProject,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home-create-project")) {
                Text("新增长期项目", style = MaterialTheme.typography.bodyLarge)
            }
        }
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("订阅服务", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() })
            AmountText(overview?.takeIf { it.subscriptionCount > 0L }?.subscriptionRate,
                modifier = Modifier.testTag("home-subscription-rate"), style = MaterialTheme.typography.titleLarge)
            Text(if (recordedSubscriptions == 0L) "还没有订阅服务" else "已录入 $recordedSubscriptions 条订阅服务",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (recordedSubscriptions > 0L && overview?.activeSubscriptionCount == 0L) {
                Text("当前无有效订阅；暂停、已到期、已停止或尚未生效的记录不计入当前成本。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            overview?.let { CategoryCosts(it.subscriptionCategories, "home-subscription-category") }
            Button(onClick = onSubscriptionList,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open-subscription-list")) {
                Text("查看订阅服务", style = MaterialTheme.typography.bodyLarge)
            }
            TextButton(onClick = onCreateSubscription,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home-create-subscription")) {
                Text("新增订阅服务", style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (overview?.hasRecords == true) {
            Text("分项四舍五入，显示金额相加可能有分位差；汇总使用未舍入结果。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("home-rounding-note"))
        }
    }
}

@Composable
private fun CategoryCosts(categories: List<CategoryDailyCost>, tagPrefix: String) {
    val contributingCategories = categories.filter { it.contributingCount > 0L }
    if (contributingCategories.isEmpty()) return
    Text("类别构成", style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    contributingCategories.forEach { category ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(category.name, style = MaterialTheme.typography.bodyLarge)
            AmountText(category.dailyRate, modifier = Modifier.testTag("$tagPrefix-${category.categoryId}"),
                style = MaterialTheme.typography.bodyLarge)
        }
    }
}
