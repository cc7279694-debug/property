package com.cooper.lithic.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooper.lithic.R
import com.cooper.lithic.core.design.*
import com.cooper.lithic.core.design.LithicIconButton as IconButton
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import com.cooper.lithic.data.media.CoverOwnerType
import com.cooper.lithic.data.media.CoverReference
import com.cooper.lithic.domain.report.CategoryDailyCost
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.OverviewResult
import com.cooper.lithic.feature.media.RecordCover
import com.cooper.lithic.feature.project.ProjectListUiState
import com.cooper.lithic.feature.project.projectStatusLabel
import com.cooper.lithic.feature.subscription.SubscriptionListUiState
import com.cooper.lithic.feature.subscription.subscriptionStatusLabel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeShell(
    projectCount: Long, onProjectList: () -> Unit, onCreateProject: () -> Unit,
    subscriptionCount: Long, onSubscriptionList: () -> Unit, onCreateSubscription: () -> Unit,
    modifier: Modifier = Modifier, overview: OverviewResult? = null, onSearch: (() -> Unit)? = null,
    onCategoryAnalysis: ((OverviewKind, String) -> Unit)? = null,
    projectPreview: ProjectListUiState? = null, subscriptionPreview: SubscriptionListUiState? = null,
    onOpenProject: ((String) -> Unit)? = null, onOpenSubscription: ((String) -> Unit)? = null,
    onCalculationExplanation: (() -> Unit)? = null,
    onRetryProjectPreview: () -> Unit = {}, onRetrySubscriptionPreview: () -> Unit = {},
    hasReadNotice: Boolean = false, readError: String? = null,
) {
    var subscriptionsSelected by rememberSaveable { mutableStateOf(false) }
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var categoriesOpen by rememberSaveable { mutableStateOf(false) }
    var explanationOpen by rememberSaveable { mutableStateOf(false) }
    val hasSnapshotNotice = hasReadNotice || readError != null
    val empty = c2HomeIsEmpty(overview, hasSnapshotNotice)
    val readStatus = homeOverviewReadStatus(overview, hasSnapshotNotice, readError)
    val colors = MaterialTheme.colorScheme
    val light = colors.background.luminance() > 0.5f
    // The reference's navy is local to the homepage. The saved theme still owns all surfaces.
    val homeColors = if (light) colors.copy(primary = Color(0xFF223246), onPrimary = Color.White) else colors

    MaterialTheme(colorScheme = homeColors) {
        Box(
            modifier.fillMaxSize().background(homeColors.background).testTag("home-content-area"),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().testTag("home-scroll-viewport")) {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("home-screen")
                            .padding(horizontal = 12.dp).padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HomeBrandBar(onSearch = onSearch, onAdd = { addOpen = true })
                        HomeOverviewCard(
                            overview, readStatus,
                            onExplain = { explanationOpen = true },
                            onComposition = if (empty) null else { { categoriesOpen = true } },
                        )
                        if (!empty) {
                            HomeBrowseControls(
                                subscriptionsSelected, { subscriptionsSelected = it },
                                onAll = if (subscriptionsSelected) onSubscriptionList else onProjectList,
                            )
                        }
                        if (empty) {
                            Text("暂无记录", Modifier.padding(vertical = 10.dp).testTag("home-empty-guidance"),
                                style = MaterialTheme.typography.bodyMedium, color = homeColors.onSurfaceVariant)
                        } else if (subscriptionsSelected) {
                            val status = subscriptionPreview?.let {
                                c2PreviewStatus(it.loading, it.error, it.items.map { item -> item.asOf }, overview?.asOf)
                            }
                            PreviewNotice(
                                status, subscriptionPreview?.error, "暂无当前订阅",
                                "重试读取订阅预览", "home-retry-subscription-preview", onRetrySubscriptionPreview,
                                emptyTag = "home-subscription-empty-compact",
                            )
                            if (status == C2PreviewStatus.READY || status == C2PreviewStatus.RETAINED) {
                                HomeGrid(subscriptionPreview.items.take(2)) { row, tile ->
                                    HomeGridCard(
                                        row.subscription.name, row.category.name,
                                        subscriptionStatusLabel(row.cost.displayStatus), null,
                                        "历史平均日成本", row.cost.historicalAverage, C2Glyph.Subscription,
                                        CoverReference(CoverOwnerType.SUBSCRIPTION, row.subscription.id, row.subscription.coverAssetId),
                                        "home-subscription-preview-${row.subscription.id}", tile,
                                        onOpenSubscription?.let { open -> { open(row.subscription.id) } },
                                    )
                                }
                            }
                        } else {
                            val status = projectPreview?.let {
                                c2PreviewStatus(it.loading, it.error, it.items.map { item -> item.asOf }, overview?.asOf)
                            }
                            PreviewNotice(
                                status, projectPreview?.error, "暂无当前项目",
                                "重试读取项目预览", "home-retry-project-preview", onRetryProjectPreview,
                            )
                            if (status == C2PreviewStatus.READY || status == C2PreviewStatus.RETAINED) {
                                HomeGrid(projectPreview.items.take(2)) { row, tile ->
                                    HomeGridCard(
                                        row.project.name, row.category.name, projectStatusLabel(row), "${row.cost.heldDays} 天",
                                        "项目日均成本", row.cost.dailyRate, C2Glyph.Project,
                                        CoverReference(CoverOwnerType.PROJECT, row.project.id, row.project.coverAssetId),
                                        "home-project-preview-${row.project.id}", tile,
                                        onOpenProject?.let { open -> { open(row.project.id) } },
                                    )
                                }
                            }
                        }
                    }
                }
                // Measured below the scroll viewport so neither add entry covers a record or the nav.
                Box(Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center) {
                    Button(
                        onClick = { addOpen = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home-open-add"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = homeColors.primary),
                    ) {
                        C2Icon(C2Glyph.Add, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("添加")
                    }
                }
            }
        }
        if (addOpen) ModalBottomSheet(onDismissRequest = { addOpen = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp)) {
                AddRow("新增项目", C2Glyph.Project, "home-create-project") {
                    addOpen = false
                    onCreateProject()
                }
                AddRow("新增订阅", C2Glyph.Subscription, "home-create-subscription") {
                    addOpen = false
                    onCreateSubscription()
                }
            }
        }
        if (categoriesOpen) ModalBottomSheet(onDismissRequest = { categoriesOpen = false }) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 24.dp).testTag("home-cost-composition"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("成本构成", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                HomeSubtotals(overview)
                CategoryCosts(overview?.projectCategories.orEmpty(), "home-project-category", "项目类别", onCategoryAnalysis?.let { open ->
                    { id -> categoriesOpen = false; open(OverviewKind.PROJECT, id) }
                })
                CategoryCosts(overview?.subscriptionCategories.orEmpty(), "home-subscription-category", "订阅类别", onCategoryAnalysis?.let { open ->
                    { id -> categoriesOpen = false; open(OverviewKind.SUBSCRIPTION, id) }
                })
                Text("分项四舍五入，显示金额相加可能有分位差；汇总使用未舍入结果。",
                    Modifier.testTag("home-rounding-note"), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (explanationOpen) AlertDialog(
            onDismissRequest = { explanationOpen = false },
            title = { Text("数量与成本口径") },
            text = {
                Text(
                    "已录入数量包含历史记录（已结束的长期项目和已停止的订阅）。当前日均成本仅含已录入且当前有效的记录，不是今天实际扣款，也不代表全部生活消费。总投入尚未取得。",
                    Modifier.verticalScroll(rememberScrollState()).testTag("home-count-scope"),
                )
            },
            dismissButton = {
                onCalculationExplanation?.let { explain ->
                    TextButton({ explanationOpen = false; explain() }, Modifier.testTag("home-full-calculation-explanation")) {
                        Text("完整计算说明")
                    }
                }
            },
            confirmButton = { TextButton({ explanationOpen = false }) { Text("知道了") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeBrandBar(onSearch: (() -> Unit)?, onAdd: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painterResource(R.drawable.ic_launcher_scene_p3), contentDescription = null,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
        )
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text("砺石", Modifier.semantics { heading() }, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("LITHIC", fontSize = 12.sp, fontFamily = FontFamily.Serif, letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold)
            }
            Text("记录拥有，理解成本。", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
        onSearch?.let { search ->
            IconButton(search, Modifier.size(48.dp).testTag("open-name-search")) {
                C2Icon(C2Glyph.Search, Modifier.size(21.dp), "搜索项目与订阅名称")
            }
        }
        IconButton(onAdd, Modifier.size(48.dp).testTag("home-open-add-top")) {
            Box(Modifier.size(36.dp).background(colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalContentColor provides colors.onPrimary) {
                    C2Icon(C2Glyph.Add, Modifier.size(21.dp), "新增记录")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeOverviewCard(
    overview: OverviewResult?,
    readStatus: HomeOverviewReadStatus,
    onExplain: () -> Unit,
    onComposition: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val largeType = LocalDensity.current.fontScale > 1.35f
    Surface(
        Modifier.fillMaxWidth().testTag("home-current-cost-summary"), shape = RoundedCornerShape(13.dp),
        color = colors.surface, shadowElevation = 3.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(top = 10.dp)) {
            MaterialTheme(typography = typography.copy(displayLarge = typography.displayLarge.copy(fontWeight = FontWeight.Bold))) {
                if (largeType) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        HomeInvestment()
                        HomeDailyRate(overview, onExplain)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { HomeInvestment() }
                        VerticalDivider(Modifier.height(56.dp), color = colors.outlineVariant)
                        Box(Modifier.weight(1.30f)) { HomeDailyRate(overview, onExplain) }
                    }
                }
            }
            when (readStatus) {
                HomeOverviewReadStatus.WAITING -> Unit
                HomeOverviewReadStatus.FAILED -> Text("暂时无法取得成本数据",
                    Modifier.testTag("home-overview-read-error"), style = typography.bodySmall, color = colors.error)
                HomeOverviewReadStatus.RETAINED -> Text("上次取得的结果",
                    Modifier.testTag("home-retained-notice"), style = typography.bodySmall, color = colors.error)
                HomeOverviewReadStatus.UNCONFIRMED -> Text("当前记录状态尚未确认",
                    Modifier.testTag("home-retained-notice"), style = typography.bodySmall, color = colors.error)
                HomeOverviewReadStatus.CURRENT -> if (homeOverviewStatus(overview) == HomeOverviewStatus.NO_CURRENT) {
                    Text("暂无当前有效记录", Modifier.testTag("home-no-active-records"), style = typography.bodySmall)
                }
            }
            if (readStatus == HomeOverviewReadStatus.RETAINED && homeOverviewStatus(overview) == HomeOverviewStatus.EMPTY) {
                Text("当前记录状态尚未确认", style = typography.bodySmall)
            }
            HorizontalDivider(Modifier.padding(top = 8.dp), color = colors.outlineVariant)
            val retained = readStatus == HomeOverviewReadStatus.RETAINED
            FlowRow(
                Modifier.fillMaxWidth().testTag("home-record-counts"), horizontalArrangement = Arrangement.SpaceBetween,
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                RecordCount("项目", "长期项目", overview?.projectCount, "project", retained)
                RecordCount("订阅", "订阅服务", overview?.subscriptionCount, "subscription", retained)
                if (onComposition != null) {
                    TextButton(
                        onComposition, Modifier.testTag("home-category-disclosure"),
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurface),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) {
                        Text("成本构成", style = typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        C2Icon(C2Glyph.Chevron, Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeInvestment() {
    // The overview supplies rates and counts only. A preview subtotal would mislabel a partial sum.
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("总投入", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        MoneyMetric(null, Modifier.testTag("home-total-investment"), unit = AmountUnit.YUAN,
            level = MoneyMetricLevel.CARD, label = "总投入")
    }
}

@Composable
private fun HomeDailyRate(overview: OverviewResult?, onExplain: () -> Unit) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val showBars = maxWidth >= 145.dp && LocalDensity.current.fontScale <= 1.2f
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("日均成本", style = typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    IconButton(onExplain, Modifier.size(48.dp).testTag("home-cost-information")) {
                        C2Icon(C2Glyph.Info, Modifier.size(14.dp), "查看数量与成本口径")
                    }
                }
                Box(Modifier.fillMaxWidth().testTag("home-amount-line")) {
                    MoneyMetric(overview?.takeIf { it.hasRecords }?.totalRate, Modifier.testTag("home-total-rate"),
                        level = MoneyMetricLevel.CARD, label = "当前日均成本")
                }
                overview?.let {
                    Text(it.asOf.toString(), Modifier.testTag("home-cutoff-date"),
                        style = typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            if (showBars) Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                listOf(7, 13, 21).forEachIndexed { index, height ->
                    Box(Modifier.size(5.dp, height.dp)
                        .background(colors.primary.copy(alpha = 0.25f + index * 0.10f), RoundedCornerShape(1.dp)))
                }
            }
        }
    }
}

@Composable
private fun RecordCount(label: String, spokenLabel: String, count: Long?, kind: String, retained: Boolean) {
    Row(
        Modifier.heightIn(min = 48.dp).padding(horizontal = 4.dp).testTag("home-$kind-count-card"),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            count?.toString() ?: "—",
            Modifier.widthIn(max = 110.dp).horizontalScroll(rememberScrollState())
                .testTag("home-$kind-record-count").semantics {
                    contentDescription = when {
                        count == null -> "$spokenLabel，数量尚未取得"
                        retained -> "$spokenLabel，上次已录入数量$count，包含历史，尚未更新"
                        else -> "$spokenLabel，已录入数量$count，包含历史"
                    }
                },
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, softWrap = false,
        )
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HomeBrowseControls(subscriptions: Boolean, select: (Boolean) -> Unit, onAll: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Row(
            Modifier.weight(1f, fill = false).widthIn(max = 192.dp).clip(CircleShape)
                .background(colors.surfaceContainer).selectableGroup(),
        ) {
            listOf(false to "项目", true to "订阅").forEach { (option, label) ->
                val selected = subscriptions == option
                Box(
                    Modifier.weight(1f).heightIn(min = 48.dp).clip(CircleShape)
                        .background(if (selected) colors.primary else Color.Transparent)
                        .selectable(selected, role = Role.Tab, onClick = { select(option) })
                        .testTag(if (option) "home-subscription-tab" else "home-project-tab"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelLarge,
                        color = if (selected) colors.onPrimary else colors.onSurface)
                }
            }
        }
        TextButton(
            onAll,
            Modifier.testTag(if (subscriptions) "open-subscription-list" else "open-project-list").semantics {
                contentDescription = if (subscriptions) "查看全部订阅服务" else "查看全部长期项目"
            },
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text("全部", fontWeight = FontWeight.SemiBold)
            C2Icon(C2Glyph.Chevron, Modifier.size(16.dp))
        }
    }
}

@Composable
private fun <T> HomeGrid(items: List<T>, content: @Composable (T, Modifier) -> Unit) {
    if (items.isEmpty()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item -> content(item, Modifier.weight(1f)) }
        if (items.size == 1) Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeGridCard(
    name: String, category: String, status: String, duration: String?, metric: String, amount: BigDecimal?,
    icon: C2Glyph, cover: CoverReference, tag: String, modifier: Modifier, onOpen: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Surface(
        modifier.then(if (onOpen == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onOpen))
            .testTag(tag),
        color = colors.surface, shape = RoundedCornerShape(13.dp), shadowElevation = 3.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(5.dp)) {
            RecordCover(
                cover,
                Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(9.dp))
                    .background(colors.surfaceContainer),
                contentScale = ContentScale.Crop,
            ) { C2Icon(c2IdentityGlyph(category) ?: icon, Modifier.size(30.dp)) }
            Column(Modifier.fillMaxWidth().padding(horizontal = 5.dp).padding(top = 5.dp, bottom = 6.dp)) {
                Text(name, Modifier.heightIn(min = 22.dp), style = typography.bodyLarge, fontWeight = FontWeight.Bold)
                FlowRow(
                    Modifier.padding(top = 4.dp).heightIn(min = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    HomeCardLabel(category)
                    HomeCardLabel(status)
                }
                MaterialTheme(typography = typography.copy(displayLarge = typography.displayLarge.copy(fontWeight = FontWeight.Bold))) {
                    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 9.dp)) {
                        val inline = duration != null && LocalDensity.current.fontScale <= 1.25f &&
                            moneyMetricFitsAtPreferredSize(amount, (maxWidth - 44.dp).coerceAtLeast(0.dp))
                        if (inline) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(checkNotNull(duration), style = typography.labelSmall, color = colors.onSurfaceVariant)
                                MoneyMetric(amount, label = metric)
                            }
                        } else {
                            Column(Modifier.fillMaxWidth()) {
                                duration?.let { Text(it, style = typography.labelSmall, color = colors.onSurfaceVariant) }
                                MoneyMetric(amount, Modifier.align(Alignment.End), label = metric)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeCardLabel(text: String) {
    Text(
        text, Modifier.background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun PreviewNotice(
    status: C2PreviewStatus?, error: String?, empty: String, retryLabel: String, retryTag: String,
    onRetry: () -> Unit, emptyTag: String? = null,
) {
    when (status) {
        C2PreviewStatus.ERROR, C2PreviewStatus.RETAINED -> {
            Text(error ?: "预览暂时无法读取", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (status == C2PreviewStatus.RETAINED) Text("以下保留上次读取结果", style = MaterialTheme.typography.bodySmall)
            TextButton(onRetry, Modifier.testTag(retryTag)) { Text(retryLabel) }
        }
        C2PreviewStatus.LOADING -> Text("正在读取预览", style = MaterialTheme.typography.bodySmall)
        C2PreviewStatus.REFRESHING -> Text("预览日期正在更新", style = MaterialTheme.typography.bodySmall)
        C2PreviewStatus.EMPTY -> Text(empty, if (emptyTag == null) Modifier else Modifier.testTag(emptyTag),
            style = MaterialTheme.typography.bodySmall)
        null -> Text("预览尚未取得", style = MaterialTheme.typography.bodySmall)
        C2PreviewStatus.READY -> Unit
    }
}

@Composable
private fun AddRow(label: String, icon: C2Glyph, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 52.dp)
            .padding(horizontal = 8.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        C2Icon(icon)
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        C2Icon(C2Glyph.Chevron)
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
private fun CategoryCosts(categories: List<CategoryDailyCost>, tagPrefix: String, title: String, onOpen: ((String) -> Unit)? = null) {
    val contributingCategories = categories.filter { it.contributingCount > 0L }
    if (contributingCategories.isEmpty()) return
    Text(title, style = MaterialTheme.typography.bodyMedium,
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
