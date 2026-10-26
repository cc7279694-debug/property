package com.cooper.lithic.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.*
import com.cooper.lithic.domain.report.CategoryDailyCost
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.OverviewResult
import com.cooper.lithic.feature.project.ProjectListUiState
import com.cooper.lithic.feature.project.projectStatusLabel
import com.cooper.lithic.feature.subscription.SubscriptionListUiState
import com.cooper.lithic.feature.subscription.subscriptionStatusLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeShell(projectCount: Long, onProjectList: () -> Unit, onCreateProject: () -> Unit,
    subscriptionCount: Long, onSubscriptionList: () -> Unit, onCreateSubscription: () -> Unit,
    modifier: Modifier = Modifier, overview: OverviewResult? = null, onSearch: (() -> Unit)? = null,
    onCategoryAnalysis: ((OverviewKind, String) -> Unit)? = null,
    projectPreview: ProjectListUiState? = null, subscriptionPreview: SubscriptionListUiState? = null,
    onOpenProject: ((String) -> Unit)? = null, onOpenSubscription: ((String) -> Unit)? = null,
    onCalculationExplanation: (() -> Unit)? = null, onRetryPreview: () -> Unit = {},
    hasReadNotice: Boolean = false) {
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var categoriesOpen by rememberSaveable { mutableStateOf(false) }
    var explanationOpen by rememberSaveable { mutableStateOf(false) }
    val recordedProjects = overview?.projectCount ?: projectCount
    val recordedSubscriptions = overview?.subscriptionCount ?: subscriptionCount
    val empty = c2HomeIsEmpty(overview, hasReadNotice)
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        .testTag("home-screen"), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("现在", Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.headlineLarge)
            onSearch?.let { search -> IconButton(search, Modifier.size(48.dp).testTag("open-name-search")) {
                C2Icon(C2Glyph.Search, description = "搜索项目与订阅名称")
            } }
            if (!empty) IconButton({ addOpen = true }, Modifier.size(48.dp).testTag("home-open-add")) {
                C2Icon(C2Glyph.Add, description = "新增记录")
            }
        }
        Surface(Modifier.fillMaxWidth().testTag("home-current-cost-summary"),
            shape = MaterialTheme.shapes.large, color = if (empty) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(horizontal = if (empty) 0.dp else 16.dp, vertical = if (empty) 8.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("当前日均成本", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton({ onCalculationExplanation?.invoke() ?: run { explanationOpen = true } },
                        Modifier.size(48.dp).testTag("home-cost-information")) { C2Icon(C2Glyph.Info, description = "查看成本计算口径") }
                }
                AmountText(overview?.takeIf { it.hasRecords }?.totalRate, Modifier.testTag("home-total-rate"))
                if (!empty) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    HomeSubtotals(overview)
                }
                overview?.let { Text("截至 ${it.asOf} · 非今日扣款", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                when (homeOverviewStatus(overview)) {
                    HomeOverviewStatus.EMPTY -> if (hasReadNotice) Text("当前记录状态尚未确认", style = MaterialTheme.typography.bodyMedium)
                    HomeOverviewStatus.UNAVAILABLE -> Text("当前日均成本尚未取得", style = MaterialTheme.typography.bodyMedium)
                    HomeOverviewStatus.NO_CURRENT -> Text("暂无当前有效记录；历史记录仍在。", Modifier.testTag("home-no-active-records"),
                        style = MaterialTheme.typography.bodyMedium)
                    HomeOverviewStatus.CURRENT -> Unit
                }
                if (hasReadNotice) Text("暂保留上次取得的结果，请先重试读取。", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("home-retained-notice"))
            }
        }
        if (empty) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("把长期拥有的物品和订阅记录在这里。", style = MaterialTheme.typography.bodyLarge)
                Button({ addOpen = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home-first-record")) {
                    Text("记下第一条记录")
                }
            }
        }
        if (!empty) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PreviewHeading("长期项目", onProjectList, "open-project-list")
            if (recordedProjects > 0L) Text("已录入 $recordedProjects 条长期项目（含历史）",
                Modifier.testTag("home-project-record-count"), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val status = projectPreview?.let { c2PreviewStatus(it.loading, it.error, it.items.map { d -> d.asOf }, overview?.asOf) }
            PreviewNotice(status, projectPreview?.error, "还没有当前长期项目", onRetryPreview)
            if (status in setOf(C2PreviewStatus.READY, C2PreviewStatus.RETAINED)) projectPreview?.items?.take(2)?.forEach { row ->
                PreviewRow(row.project.name, C2Glyph.Project,
                    "${projectStatusLabel(row)} · 持有 ${row.cost.heldDays} 天", "当前日均成本",
                    row.cost.dailyRate, "home-project-preview-${row.project.id}", onOpenProject?.let { { it(row.project.id) } })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PreviewHeading("订阅服务", onSubscriptionList, "open-subscription-list")
            if (recordedSubscriptions > 0L) Text("已录入 $recordedSubscriptions 条订阅服务（含历史）",
                Modifier.testTag("home-subscription-record-count"), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val status = subscriptionPreview?.let { c2PreviewStatus(it.loading, it.error, it.items.map { d -> d.asOf }, overview?.asOf) }
            PreviewNotice(status, subscriptionPreview?.error, "还没有当前订阅", onRetryPreview)
            if (status in setOf(C2PreviewStatus.READY, C2PreviewStatus.RETAINED)) subscriptionPreview?.items?.take(2)?.forEach { row ->
                PreviewRow(row.subscription.name, C2Glyph.Subscription,
                    "${subscriptionStatusLabel(row.cost.displayStatus)} · 折算付费时长 ${row.cost.standardPurchasedDays} 天",
                    "历史平均日成本", row.cost.historicalAverage, "home-subscription-preview-${row.subscription.id}",
                    onOpenSubscription?.let { { it(row.subscription.id) } })
            }
        }
        }
        if (overview?.hasRecords == true) {
            TextButton({ categoriesOpen = !categoriesOpen }, Modifier.heightIn(min = 48.dp).testTag("home-category-disclosure")) {
                Text(if (categoriesOpen) "收起类别构成" else "查看类别构成")
                C2Icon(C2Glyph.Chevron)
            }
            if (categoriesOpen) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("长期项目", style = MaterialTheme.typography.titleMedium)
                CategoryCosts(overview.projectCategories, "home-project-category", onCategoryAnalysis?.let { open -> { id -> open(OverviewKind.PROJECT, id) } })
                Text("订阅服务", style = MaterialTheme.typography.titleMedium)
                CategoryCosts(overview.subscriptionCategories, "home-subscription-category", onCategoryAnalysis?.let { open -> { id -> open(OverviewKind.SUBSCRIPTION, id) } })
                Text("分项四舍五入，显示金额相加可能有分位差；汇总使用未舍入结果。",
                    Modifier.testTag("home-rounding-note"), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (addOpen) ModalBottomSheet(onDismissRequest = { addOpen = false }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            AddRow("长期项目", "记录物品的投入、收回与持有时间", C2Glyph.Project, "home-create-project") {
                addOpen = false; onCreateProject()
            }
            AddRow("订阅服务", "记录真实付款与生效周期", C2Glyph.Subscription, "home-create-subscription") {
                addOpen = false; onCreateSubscription()
            }
        }
    }
    if (explanationOpen) AlertDialog(onDismissRequest = { explanationOpen = false }, title = { Text("当前日均成本") },
        text = { Text("仅含已录入且当前有效的长期项目和订阅，不是今天实际扣款，也不代表全部生活消费。记录数量包含历史；金额使用原计算引擎，展示时舍入。") },
        confirmButton = { TextButton({ explanationOpen = false }) { Text("知道了") } })
}

@Composable
private fun HomeSubtotals(overview: OverviewResult?) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 260.dp || fontScale > 1.4f) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Subtotal("长期项目", overview?.takeIf { it.hasRecords }?.projectRate, "home-project-rate")
                Subtotal("订阅服务", overview?.takeIf { it.hasRecords }?.subscriptionRate, "home-subscription-rate")
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) { Subtotal("长期项目", overview?.takeIf { it.hasRecords }?.projectRate, "home-project-rate") }
            Column(Modifier.weight(1f)) { Subtotal("订阅服务", overview?.takeIf { it.hasRecords }?.subscriptionRate, "home-subscription-rate") }
        }
    }
}

@Composable
private fun Subtotal(label: String, amount: java.math.BigDecimal?, tag: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label · 元/天", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AmountText(amount, Modifier.testTag(tag), style = MaterialTheme.typography.titleMedium, showUnit = false)
    }
}

@Composable
private fun PreviewHeading(label: String, onAll: () -> Unit, tag: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        TextButton(onAll, Modifier.heightIn(min = 48.dp).testTag(tag)) { Text("全部"); C2Icon(C2Glyph.Chevron) }
    }
}

@Composable
private fun PreviewNotice(status: C2PreviewStatus?, error: String?, empty: String, onRetry: () -> Unit) {
    when (status) {
        C2PreviewStatus.ERROR, C2PreviewStatus.RETAINED -> {
            Text(error ?: "预览暂时无法读取", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            if (status == C2PreviewStatus.RETAINED) Text("以下保留上次读取结果", style = MaterialTheme.typography.bodySmall)
            TextButton(onRetry, Modifier.heightIn(min = 48.dp)) { Text("重试读取") }
        }
        C2PreviewStatus.LOADING -> Text("正在读取预览", style = MaterialTheme.typography.bodyMedium)
        C2PreviewStatus.REFRESHING -> Text("预览日期正在更新", style = MaterialTheme.typography.bodyMedium)
        C2PreviewStatus.EMPTY -> Text(empty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        null -> Text("预览尚未取得", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        C2PreviewStatus.READY -> Unit
    }
}

@Composable
private fun PreviewRow(name: String, icon: C2Glyph, status: String, metric: String, amount: java.math.BigDecimal?, tag: String, onOpen: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier)
        .padding(vertical = 12.dp).testTag(tag), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(Modifier.size(64.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) { C2Icon(icon) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$metric · 元/天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AmountText(amount, style = MaterialTheme.typography.titleMedium, showUnit = false)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun AddRow(label: String, subtitle: String, icon: C2Glyph, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 72.dp)
        .padding(12.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        C2Icon(icon)
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        C2Icon(C2Glyph.Chevron)
    }
}

@Composable
private fun CategoryCosts(categories: List<CategoryDailyCost>, tagPrefix: String, onOpen: ((String) -> Unit)? = null) {
    val contributingCategories = categories.filter { it.contributingCount > 0L }
    if (contributingCategories.isEmpty()) return
    Text("类别构成", style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.semantics { heading() })
    Text("按当前日均成本归类，不是年度支出占比。", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    contributingCategories.forEach { category ->
        Column(Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .then(if (onOpen != null) Modifier.clickable(role = Role.Button) { onOpen(category.categoryId) } else Modifier)
            .testTag("$tagPrefix-open-${category.categoryId}"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(category.name, style = MaterialTheme.typography.bodyLarge)
            AmountText(category.dailyRate, modifier = Modifier.testTag("$tagPrefix-${category.categoryId}"),
                style = MaterialTheme.typography.bodyLarge)
        }
    }
}
