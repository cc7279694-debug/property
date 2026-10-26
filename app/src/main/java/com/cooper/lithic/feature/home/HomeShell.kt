package com.cooper.lithic.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val empty = c2HomeIsEmpty(overview, hasReadNotice)
    val heroSurface = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.inverseSurface
    val heroText = Color(0xFFF1F3ED)
    val heroSecondary = Color(0xFFC3D9CA)
    Box(modifier.fillMaxSize().testTag("home-content-area")) {
    Box(Modifier.fillMaxSize().padding(bottom = if (empty) 0.dp else 72.dp).testTag("home-scroll-viewport")) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(16.dp)
        .testTag("home-screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("现在", Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
            onSearch?.let { search -> IconButton(search, Modifier.size(48.dp).testTag("open-name-search")) {
                C2Icon(C2Glyph.Search, description = "搜索项目与订阅名称")
            } }
        }
        Surface(Modifier.fillMaxWidth().testTag("home-current-cost-summary"),
            shape = MaterialTheme.shapes.large, color = heroSurface,
            contentColor = heroText, shadowElevation = 1.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("当前日均成本", style = MaterialTheme.typography.bodyLarge)
                        HomeHeadlineAmount(overview?.takeIf { it.hasRecords }?.totalRate, heroText, heroSecondary)
                    }
                    IconButton({ explanationOpen = true },
                        Modifier.size(48.dp).testTag("home-cost-information")) { C2Icon(C2Glyph.Info, description = "查看数量与成本口径") }
                }
                overview?.let { Text("截至 ${it.asOf} · 非今日扣款", style = MaterialTheme.typography.bodySmall,
                    color = heroSecondary) }
                when (homeOverviewStatus(overview)) {
                    HomeOverviewStatus.EMPTY -> if (hasReadNotice) Text("当前记录状态尚未确认", style = MaterialTheme.typography.bodyMedium)
                    HomeOverviewStatus.UNAVAILABLE -> Text("当前日均成本尚未取得", style = MaterialTheme.typography.bodyMedium)
                    HomeOverviewStatus.NO_CURRENT -> Text("暂无当前有效记录；历史记录仍在。", Modifier.testTag("home-no-active-records"),
                        style = MaterialTheme.typography.bodyMedium)
                    HomeOverviewStatus.CURRENT -> Unit
                }
                if (hasReadNotice) Text(if (overview == null) "成本与数量尚未取得，请先重试读取。" else "暂保留上次取得的成本与数量，请先重试读取。", style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFF2B6AC), modifier = Modifier.testTag("home-retained-notice"))
            }
        }
        HomeRecordCounts(overview, hasReadNotice)
        if (empty) {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("从一条真实记录开始", style = MaterialTheme.typography.titleSmall)
                Text("记下物品或订阅，开始看见它们的成本。", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button({ addOpen = true }, Modifier.heightIn(min = 48.dp).testTag("home-first-record")) {
                    Text("记下第一条记录")
                }
            }
        }
        if (!empty) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PreviewHeading("长期项目", onProjectList, "open-project-list")
            val status = projectPreview?.let { c2PreviewStatus(it.loading, it.error, it.items.map { d -> d.asOf }, overview?.asOf) }
            PreviewNotice(status, projectPreview?.error, "还没有当前长期项目", onRetryPreview)
            if (status in setOf(C2PreviewStatus.READY, C2PreviewStatus.RETAINED)) projectPreview?.items?.take(2)?.forEach { row ->
                PreviewRow(row.project.name, row.category.name, C2Glyph.Project,
                    projectStatusLabel(row), "持有 ${row.cost.heldDays} 天", "当前日均成本", "日均",
                    row.cost.dailyRate, row.project.coverAssetId != null, "home-project-preview-${row.project.id}", onOpenProject?.let { { it(row.project.id) } })
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PreviewHeading("订阅服务", onSubscriptionList, "open-subscription-list")
            val status = subscriptionPreview?.let { c2PreviewStatus(it.loading, it.error, it.items.map { d -> d.asOf }, overview?.asOf) }
            PreviewNotice(status, subscriptionPreview?.error, "还没有当前订阅", onRetryPreview)
            if (status in setOf(C2PreviewStatus.READY, C2PreviewStatus.RETAINED)) subscriptionPreview?.items?.take(2)?.forEach { row ->
                PreviewRow(row.subscription.name, row.category.name, C2Glyph.Subscription,
                    subscriptionStatusLabel(row.cost.displayStatus), "折算付费${row.cost.standardPurchasedDays}天",
                    "历史平均日成本", "历史均价", row.cost.historicalAverage, row.subscription.coverAssetId != null, "home-subscription-preview-${row.subscription.id}",
                    onOpenSubscription?.let { { it(row.subscription.id) } })
            }
        }
        }
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
    if (!empty) FloatingActionButton(onClick = { addOpen = true },
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp)
            .size(56.dp).testTag("home-open-add"), shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 3.dp)) {
        C2Icon(C2Glyph.Add, description = "新增记录")
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
        text = { Text("两张数量卡为已录入总数，包含历史记录（已结束的长期项目和已停止的订阅）。当前日均成本仅含已录入且当前有效的记录，不是今天实际扣款，也不代表全部生活消费。有记录时，可在首页下方展开查看两类小计及类别金额；金额使用原计算引擎，展示时舍入。", Modifier.verticalScroll(rememberScrollState()).testTag("home-count-scope")) },
        dismissButton = {
            onCalculationExplanation?.let { explain ->
                TextButton({ explanationOpen = false; explain() }, Modifier.testTag("home-full-calculation-explanation")) {
                    Text("完整计算说明")
                }
            }
        },
        confirmButton = { TextButton({ explanationOpen = false }) { Text("知道了") } })
}

/** Reserve the unit first, then size the number to its measured text, never to a whole weighted row. */
@Composable
private fun HomeHeadlineAmount(amount: java.math.BigDecimal?, color: Color, supportingColor: Color) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val headlineStyle = MaterialTheme.typography.displayLarge
    val unitStyle = MaterialTheme.typography.bodySmall
    val display = AmountFormatter.formatDailyRate(amount)
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("home-amount-line")) {
        val unitWidth = measurer.measure(AnnotatedString("元/天"), unitStyle, softWrap = false).size.width
        val gap = with(density) { 4.dp.roundToPx() }
        val available = (constraints.maxWidth - unitWidth - gap).coerceAtLeast(1)
        val layout = remember(display, headlineStyle, available, density.fontScale, density.density, measurer) {
            amountLayout(headlineStyle.fontSize.value, available) { fontSp ->
                measurer.measure(AnnotatedString(display), headlineStyle.copy(fontSize = fontSp.sp),
                    softWrap = false, maxLines = 1).size.width
            }
        }
        val numberStyle = headlineStyle.copy(fontSize = layout.fontSp.sp,
            lineHeight = (headlineStyle.lineHeight.value * layout.fontSp / headlineStyle.fontSize.value).sp)
        val numberWidth = if (layout.horizontalScroll) available else minOf(available,
            measurer.measure(AnnotatedString(display), numberStyle, softWrap = false, maxLines = 1).size.width)
        val numberWidthDp = with(density) { numberWidth.toDp() }
        val unitTop = with(density) { maxOf(0f, numberStyle.lineHeight.value - unitStyle.lineHeight.value).sp.toDp() }
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AmountText(amount, Modifier.width(numberWidthDp).testTag("home-total-rate"), style = numberStyle,
                showUnit = false, color = color, supportingColor = supportingColor)
            Text("元/天", Modifier.padding(top = unitTop).testTag("home-total-unit"), style = unitStyle,
                color = supportingColor, softWrap = false)
        }
    }
}

@Composable
private fun HomeRecordCounts(overview: OverviewResult?, retained: Boolean) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("home-record-counts")) {
        if (maxWidth < 260.dp || LocalDensity.current.fontScale > 1.4f) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordCount("长期项目", overview?.projectCount, C2Glyph.Project, "project", retained)
                RecordCount("订阅服务", overview?.subscriptionCount, C2Glyph.Subscription, "subscription", retained)
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) { RecordCount("长期项目", overview?.projectCount, C2Glyph.Project, "project", retained) }
            Column(Modifier.weight(1f)) { RecordCount("订阅服务", overview?.subscriptionCount, C2Glyph.Subscription, "subscription", retained) }
        }
    }
}

@Composable
private fun RecordCount(label: String, count: Long?, icon: C2Glyph, kind: String, retained: Boolean) {
    Surface(Modifier.fillMaxWidth().testTag("home-$kind-count-card"), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(Modifier.heightIn(min = 72.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.size(36.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) { C2Icon(icon) }
            }
            Column(Modifier.weight(1f)) {
                Text(count?.toString() ?: "—", Modifier.horizontalScroll(rememberScrollState())
                    .testTag("home-$kind-record-count").semantics {
                        contentDescription = when {
                            count == null -> "$label，数量尚未取得"
                            retained -> "$label，上次已录入数量$count，包含历史，尚未更新"
                            else -> "$label，已录入数量$count，包含历史"
                        }
                    }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold,
                    softWrap = false)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HomeSubtotals(overview: OverviewResult?) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 260.dp || LocalDensity.current.fontScale > 1.4f) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Subtotal("长期项目", overview?.takeIf { it.hasRecords }?.projectRate, overview?.activeProjectCount, C2Glyph.Project, "home-project-rate", "home-active-project-count")
                Subtotal("订阅服务", overview?.takeIf { it.hasRecords }?.subscriptionRate, overview?.activeSubscriptionCount, C2Glyph.Subscription, "home-subscription-rate", "home-active-subscription-count")
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) { Subtotal("长期项目", overview?.takeIf { it.hasRecords }?.projectRate, overview?.activeProjectCount, C2Glyph.Project, "home-project-rate", "home-active-project-count") }
            Column(Modifier.weight(1f)) { Subtotal("订阅服务", overview?.takeIf { it.hasRecords }?.subscriptionRate, overview?.activeSubscriptionCount, C2Glyph.Subscription, "home-subscription-rate", "home-active-subscription-count") }
        }
    }
}

@Composable
private fun Subtotal(label: String, amount: java.math.BigDecimal?, activeCount: Long?, icon: C2Glyph, tag: String, countTag: String) {
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                C2Icon(icon)
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AmountText(amount, Modifier.weight(1f).testTag(tag), style = MaterialTheme.typography.titleMedium, showUnit = false)
                Text("元/天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (activeCount == null) "当前有效数量未取得" else "当前有效 $activeCount", Modifier.testTag(countTag),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
private fun PreviewRow(name: String, category: String, icon: C2Glyph, status: String, time: String, metric: String, compactMetric: String,
    amount: java.math.BigDecimal?, hasCoverReference: Boolean, tag: String, onOpen: (() -> Unit)?) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val amountStyle = MaterialTheme.typography.titleMedium
    val metricStyle = MaterialTheme.typography.bodySmall
    val amountWidth = with(density) {
        maxOf(measurer.measure(AnnotatedString(AmountFormatter.formatDailyRate(amount)), amountStyle, softWrap = false).size.width,
            measurer.measure(AnnotatedString("$compactMetric · 元/天"), metricStyle, softWrap = false).size.width).toDp()
    }
    Surface(Modifier.fillMaxWidth().testTag(tag), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(Modifier.fillMaxWidth().then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier).padding(12.dp)) {
            val beside = density.fontScale <= 1.3f && maxWidth >= 48.dp + 12.dp + 112.dp + 12.dp + amountWidth
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (hasCoverReference) Surface(Modifier.size(48.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Box(contentAlignment = Alignment.Center) { C2Icon(C2Glyph.ImageError, description = "封面暂不可用") }
                } else C2RecordIdentity(name, category, icon, Modifier.size(48.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(status, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(time, style = metricStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (hasCoverReference) Text("封面暂不可用", style = metricStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!beside) PreviewAmount(metric, compactMetric, amount)
                }
                if (beside) Column(Modifier.width(amountWidth)) { PreviewAmount(metric, compactMetric, amount) }
            }
        }
    }
}

@Composable
private fun PreviewAmount(metric: String, compactMetric: String, amount: java.math.BigDecimal?) {
    Column(Modifier.semantics(mergeDescendants = true) {
        contentDescription = "$metric，${AmountFormatter.formatDailyRate(amount)}，元/天"
    }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AmountText(amount, style = MaterialTheme.typography.titleMedium, showUnit = false)
        Text("$compactMetric · 元/天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            AmountText(category.dailyRate, modifier = Modifier.testTag("$tagPrefix-${category.categoryId}"),
                style = MaterialTheme.typography.bodyLarge)
        }
    }
}
