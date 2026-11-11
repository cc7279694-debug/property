package com.cooper.lithic.feature.home

import com.cooper.lithic.data.media.CoverOwnerType
import com.cooper.lithic.data.media.CoverReference
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.cooper.lithic.core.design.LithicIconButton as IconButton
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cooper.lithic.R
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
    onCalculationExplanation: (() -> Unit)? = null,
    onRetryProjectPreview: () -> Unit = {}, onRetrySubscriptionPreview: () -> Unit = {},
    hasReadNotice: Boolean = false, readError: String? = null) {
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var categoriesOpen by rememberSaveable { mutableStateOf(false) }
    var explanationOpen by rememberSaveable { mutableStateOf(false) }
    val hasSnapshotNotice = hasReadNotice || readError != null
    val empty = c2HomeIsEmpty(overview, hasSnapshotNotice)
    val readStatus = homeOverviewReadStatus(overview, hasSnapshotNotice, readError)
    val useAddBar = LocalDensity.current.fontScale >= 1.5f
    BoxWithConstraints(modifier.fillMaxSize().testTag("home-content-area")) {
        val columnMaxWidth = 560.dp
        val sideGutter = (maxWidth - columnMaxWidth).coerceAtLeast(0.dp) / 2
        val pageInset = if (maxWidth <= 360.dp) 16.dp else 24.dp
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Box(Modifier.align(Alignment.TopCenter).widthIn(max = columnMaxWidth).fillMaxSize().testTag("home-scroll-viewport")) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(bottom = if (useAddBar) 16.dp else 88.dp).testTag("home-screen")) {
                        HomeCostHeader(overview, readStatus, pageInset, onSearch, onExplain = { explanationOpen = true })
                        Column(Modifier.fillMaxWidth().padding(horizontal = pageInset, vertical = LithicSpacing.small),
                            verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
                            if (empty) Text(
                                if (useAddBar) "选择新增记录，记下一件物品或订阅。" else "从右下角＋开始，记下一件物品或订阅。",
                                Modifier.fillMaxWidth().testTag("home-empty-guidance"),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!empty) {
                                Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                                    PreviewHeading("长期项目", onProjectList, "open-project-list")
                                    val status = projectPreview?.let { c2PreviewStatus(it.loading, it.error, it.items.map { d -> d.asOf }, overview?.asOf) }
                                    PreviewNotice(status, projectPreview?.error, "还没有当前长期项目",
                                        "重试读取项目预览", "home-retry-project-preview", onRetryProjectPreview)
                                    if (status in setOf(C2PreviewStatus.READY, C2PreviewStatus.RETAINED)) projectPreview?.items?.take(2)?.forEach { row ->
                                        HomeArchiveRow(row.project.name, row.category.name, C2Glyph.Project,
                                            projectStatusLabel(row), "持有 ${row.cost.heldDays} 天", "项目日均成本",
                                            row.cost.dailyRate, CoverReference(CoverOwnerType.PROJECT, row.project.id, row.project.coverAssetId),
                                            "home-project-preview-${row.project.id}", onOpenProject?.let { { it(row.project.id) } })
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                                    val status = subscriptionPreview?.let { c2PreviewStatus(it.loading, it.error, it.items.map { d -> d.asOf }, overview?.asOf) }
                                    PreviewHeading("订阅服务", onSubscriptionList, "open-subscription-list",
                                        emptyLabel = if (status == C2PreviewStatus.EMPTY) "暂无当前订阅" else null)
                                    if (status != C2PreviewStatus.EMPTY) {
                                        PreviewNotice(status, subscriptionPreview?.error, "还没有当前订阅",
                                            "重试读取订阅预览", "home-retry-subscription-preview", onRetrySubscriptionPreview)
                                    }
                                    if (status in setOf(C2PreviewStatus.READY, C2PreviewStatus.RETAINED)) subscriptionPreview?.items?.take(2)?.forEach { row ->
                                        HomeArchiveRow(row.subscription.name, row.category.name, C2Glyph.Subscription,
                                            subscriptionStatusLabel(row.cost.displayStatus), "折算付费${row.cost.standardPurchasedDays}天",
                                            "历史平均日成本", row.cost.historicalAverage,
                                            CoverReference(CoverOwnerType.SUBSCRIPTION, row.subscription.id, row.subscription.coverAssetId),
                                            "home-subscription-preview-${row.subscription.id}", onOpenSubscription?.let { { it(row.subscription.id) } })
                                    }
                                }
                            }
                            HomeRecordCounts(overview, retained = readStatus == HomeOverviewReadStatus.RETAINED)
                            if (!empty) {
                                TextButton({ categoriesOpen = !categoriesOpen }, Modifier.heightIn(min = 48.dp).testTag("home-category-disclosure")) {
                                    Text(if (categoriesOpen) "收起成本与类别构成" else "查看成本与类别构成")
                                    C2Icon(C2Glyph.Chevron)
                                }
                                if (categoriesOpen) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    HomeSubtotals(overview)
                                    Text("长期项目", style = MaterialTheme.typography.titleMedium)
                                    CategoryCosts(overview?.projectCategories.orEmpty(), "home-project-category", onCategoryAnalysis?.let { open -> { id -> open(OverviewKind.PROJECT, id) } })
                                    Text("订阅服务", style = MaterialTheme.typography.titleMedium)
                                    CategoryCosts(overview?.subscriptionCategories.orEmpty(), "home-subscription-category", onCategoryAnalysis?.let { open -> { id -> open(OverviewKind.SUBSCRIPTION, id) } })
                                    Text("分项四舍五入，显示金额相加可能有分位差；汇总使用未舍入结果。",
                                        Modifier.testTag("home-rounding-note"), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                if (!useAddBar) {
                    FloatingActionButton(onClick = { addOpen = true },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = sideGutter + 16.dp, bottom = 16.dp)
                            .size(56.dp).testTag("home-open-add"), shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 3.dp)) {
                        C2Icon(C2Glyph.Add, description = "新增记录")
                    }
                }
            }
            if (useAddBar) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { addOpen = true },
                            modifier = Modifier.widthIn(max = columnMaxWidth).fillMaxWidth()
                                .padding(horizontal = pageInset, vertical = 8.dp)
                                .heightIn(min = 48.dp).testTag("home-open-add"),
                        ) {
                            C2Icon(C2Glyph.Add)
                            Spacer(Modifier.width(8.dp))
                            Text("新增记录")
                        }
                    }
                }
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
    if (explanationOpen) AlertDialog(onDismissRequest = { explanationOpen = false }, title = { Text("数量与成本口径") },
        text = { Text("已录入数量包含历史记录（已结束的长期项目和已停止的订阅）。当前日均成本仅含已录入且当前有效的记录，不是今天实际扣款，也不代表全部生活消费。有记录时，可在首页下方展开查看两类小计及类别金额；金额使用原计算引擎，展示时舍入。", Modifier.verticalScroll(rememberScrollState()).testTag("home-count-scope")) },
        dismissButton = {
            onCalculationExplanation?.let { explain ->
                TextButton({ explanationOpen = false; explain() }, Modifier.testTag("home-full-calculation-explanation")) {
                    Text("完整计算说明")
                }
            }
        },
        confirmButton = { TextButton({ explanationOpen = false }) { Text("知道了") } })
}

@Composable
private fun HomeCostHeader(overview: OverviewResult?, readStatus: HomeOverviewReadStatus, pageInset: Dp,
    onSearch: (() -> Unit)?, onExplain: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val headerColor = LocalLithicVisuals.current.brandSurface
    val plainHeader = headerColor == colors.background
    Surface(Modifier.fillMaxWidth(), color = headerColor, contentColor = colors.onBackground) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = pageInset, vertical = LithicSpacing.xSmall),
                horizontalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall),
                verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_launcher_scene_p3), contentDescription = null,
                    modifier = Modifier.size(28.dp).clip(MaterialTheme.shapes.extraSmall))
                Text("砺石", Modifier.weight(1f).semantics { heading() }, style = typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold)
                onSearch?.let { search ->
                    IconButton(search, Modifier.size(48.dp).testTag("open-name-search")) {
                        C2Icon(C2Glyph.Search, description = "搜索项目与订阅名称")
                    }
                }
            }
            Column(Modifier.fillMaxWidth().testTag("home-current-cost-summary")
                .padding(start = pageInset, end = pageInset, bottom = LithicSpacing.xSmall),
                verticalArrangement = Arrangement.spacedBy(LithicSpacing.xxSmall)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("当前日均成本", Modifier.weight(1f), style = typography.bodyMedium, fontWeight = FontWeight.Medium)
                    IconButton(onExplain, Modifier.size(48.dp).testTag("home-cost-information")) {
                        C2Icon(C2Glyph.Info, description = "查看数量与成本口径")
                    }
                }
                val readoutTypography = if (plainHeader) {
                    typography.copy(displayLarge = typography.displayLarge.copy(fontWeight = FontWeight.SemiBold))
                } else typography
                MaterialTheme(typography = readoutTypography) {
                    Box(Modifier.fillMaxWidth().testTag("home-amount-line")) {
                        MoneyMetric(overview?.takeIf { it.hasRecords }?.totalRate, Modifier.testTag("home-total-rate"),
                            level = MoneyMetricLevel.HERO, color = colors.onBackground,
                            supportingColor = colors.onSurfaceVariant, label = "当前日均成本")
                    }
                }
                overview?.let { Text("截至 ${it.asOf} · 非今日扣款", style = typography.bodySmall, color = colors.onSurfaceVariant) }
                when (readStatus) {
                    HomeOverviewReadStatus.WAITING -> Text("当前日均成本尚未取得", style = typography.bodyMedium)
                    HomeOverviewReadStatus.FAILED -> Text("暂时无法取得成本数据",
                        Modifier.testTag("home-overview-read-error"), style = typography.bodyMedium, color = colors.error)
                    HomeOverviewReadStatus.RETAINED -> Text("上次取得的结果",
                        Modifier.testTag("home-retained-notice"), style = typography.bodyMedium, color = colors.error)
                    HomeOverviewReadStatus.UNCONFIRMED -> Text("当前记录状态尚未确认",
                        Modifier.testTag("home-retained-notice"), style = typography.bodyMedium)
                    HomeOverviewReadStatus.CURRENT -> if (homeOverviewStatus(overview) == HomeOverviewStatus.NO_CURRENT) {
                        Text("暂无当前有效记录；历史记录仍在。", Modifier.testTag("home-no-active-records"),
                            style = typography.bodyMedium)
                    }
                }
                if (readStatus == HomeOverviewReadStatus.RETAINED && homeOverviewStatus(overview) == HomeOverviewStatus.EMPTY) {
                    Text("当前记录状态尚未确认", style = typography.bodyMedium)
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = if (plainHeader) pageInset else 0.dp), color = colors.outlineVariant)
        }
    }
}

@Composable
private fun HomeRecordCounts(overview: OverviewResult?, retained: Boolean) {
    Column(Modifier.fillMaxWidth().testTag("home-record-counts"), verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
        Text("已录入 · 含历史", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = LithicSpacing.medium
            if (maxWidth < 260.dp || LocalDensity.current.fontScale > 1.4f) {
                Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                    RecordCount("长期项目", overview?.projectCount, "project", retained)
                    RecordCount("订阅服务", overview?.subscriptionCount, "subscription", retained)
                }
            } else {
                val item = Modifier.widthIn(max = (maxWidth - gap) / 2f)
                Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Top) {
                    RecordCount("长期项目", overview?.projectCount, "project", retained, item)
                    RecordCount("订阅服务", overview?.subscriptionCount, "subscription", retained, item)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun RecordCount(label: String, count: Long?, kind: String, retained: Boolean, modifier: Modifier = Modifier) {
    Row(modifier.testTag("home-$kind-count-card"), horizontalArrangement = Arrangement.spacedBy(LithicSpacing.xxSmall),
        verticalAlignment = Alignment.CenterVertically) {
        Text(count?.toString() ?: "—", Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState())
            .testTag("home-$kind-record-count").semantics {
                contentDescription = when {
                    count == null -> "$label，数量尚未取得"
                    retained -> "$label，上次已录入数量$count，包含历史，尚未更新"
                    else -> "$label，已录入数量$count，包含历史"
                }
            }, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, softWrap = false)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HomeSubtotals(overview: OverviewResult?) {
    val projectAmount = overview?.takeIf { it.hasRecords }?.projectRate
    val subscriptionAmount = overview?.takeIf { it.hasRecords }?.subscriptionRate
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val amountWidth = ((maxWidth - 12.dp) / 2 - 24.dp).coerceAtLeast(0.dp)
        val projectFits = moneyMetricFitsAtPreferredSize(projectAmount, amountWidth, level = MoneyMetricLevel.CARD)
        val subscriptionFits = moneyMetricFitsAtPreferredSize(subscriptionAmount, amountWidth, level = MoneyMetricLevel.CARD)
        if (maxWidth < 260.dp || LocalDensity.current.fontScale > 1.4f || !projectFits || !subscriptionFits) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Subtotal("长期项目", projectAmount, overview?.activeProjectCount, C2Glyph.Project, "home-project-rate", "home-active-project-count")
                Subtotal("订阅服务", subscriptionAmount, overview?.activeSubscriptionCount, C2Glyph.Subscription, "home-subscription-rate", "home-active-subscription-count")
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) { Subtotal("长期项目", projectAmount, overview?.activeProjectCount, C2Glyph.Project, "home-project-rate", "home-active-project-count") }
            Column(Modifier.weight(1f)) { Subtotal("订阅服务", subscriptionAmount, overview?.activeSubscriptionCount, C2Glyph.Subscription, "home-subscription-rate", "home-active-subscription-count") }
        }
    }
}

@Composable
private fun Subtotal(label: String, amount: java.math.BigDecimal?, activeCount: Long?, icon: C2Glyph, tag: String, countTag: String) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                C2Icon(icon)
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            MoneyMetric(amount, Modifier.fillMaxWidth().testTag(tag), level = MoneyMetricLevel.CARD,
                label = "$label · 当前日均成本")
            Text(if (activeCount == null) "当前有效数量未取得" else "当前有效 $activeCount", Modifier.testTag(countTag),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PreviewHeading(label: String, onAll: () -> Unit, tag: String, emptyLabel: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            emptyLabel?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("home-subscription-empty-compact")) }
        }
        TextButton(onAll, Modifier.heightIn(min = 48.dp).testTag(tag).semantics { contentDescription = "查看全部$label" }) {
            Text("全部")
            C2Icon(C2Glyph.Chevron)
        }
    }
}

@Composable
private fun PreviewNotice(status: C2PreviewStatus?, error: String?, empty: String,
    retryLabel: String, retryTag: String, onRetry: () -> Unit) {
    when (status) {
        C2PreviewStatus.ERROR, C2PreviewStatus.RETAINED -> {
            Text(error ?: "预览暂时无法读取", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            if (status == C2PreviewStatus.RETAINED) Text("以下保留上次读取结果", style = MaterialTheme.typography.bodySmall)
            TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag(retryTag)) { Text(retryLabel) }
        }
        C2PreviewStatus.LOADING -> Text("正在读取预览", style = MaterialTheme.typography.bodyMedium)
        C2PreviewStatus.REFRESHING -> Text("预览日期正在更新", style = MaterialTheme.typography.bodyMedium)
        C2PreviewStatus.EMPTY -> Text(empty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        null -> Text("预览尚未取得", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        C2PreviewStatus.READY -> Unit
    }
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
            MoneyMetric(category.dailyRate, modifier = Modifier.testTag("$tagPrefix-${category.categoryId}"),
                level = MoneyMetricLevel.ROW, label = "${category.name} · 当前日均成本贡献")
        }
    }
}
