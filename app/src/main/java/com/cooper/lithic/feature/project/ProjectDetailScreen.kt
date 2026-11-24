package com.cooper.lithic.feature.project

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import com.cooper.lithic.core.design.LithicIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountUnit
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.C2RecordIdentity
import com.cooper.lithic.core.design.C5InformationButton
import com.cooper.lithic.core.design.MoneyMetric
import com.cooper.lithic.core.design.MoneyMetricLevel
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.LocalLithicVisuals
import com.cooper.lithic.core.design.moneyMetricFitsAtPreferredSize
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.feature.archive.ArchiveDetailDisclosure
import com.cooper.lithic.feature.archive.ArchiveDetailFrame
import com.cooper.lithic.feature.archive.ArchivePageInset
import java.math.BigDecimal
import com.cooper.lithic.core.design.LithicButton as Button

private const val INITIAL_MONEY_RECORD_COUNT = 3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ProjectDetailScreen(
    state: ProjectDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddMoney: (MoneyDirection) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onEnd: () -> Unit = {},
    onUpdateEnd: () -> Unit = {},
    onCorrectMoney: (String) -> Unit = {},
    onDelete: () -> Unit = {},
    backLabel: String = "返回项目列表",
    onReplacement: (() -> Unit)? = null,
    onTradeIn: (() -> Unit)? = null,
    onCostHistory: (() -> Unit)? = null,
    // Presentation slot only: callers supply the current owner's validated reader, never a path.
    coverContent: (@Composable () -> Unit)? = null,
    onAddPhoto: (() -> Unit)? = null,
    photoActionEnabled: Boolean = true,
    photoUnavailableMessage: String? = null,
    expandPhotoCover: Boolean = false,
    onReplacePhoto: (() -> Unit)? = null,
    onRemovePhoto: (() -> Unit)? = null,
    presentationIdentity: Any? = null,
) {
    val detail = state.detail?.takeUnless { state.missing }
    val projectId = detail?.project?.id
    var menuOpen by rememberSaveable(projectId, presentationIdentity) { mutableStateOf(false) }
    var moneySheetOpen by rememberSaveable(projectId, presentationIdentity) { mutableStateOf(false) }
    var showFinancialDetails by remember(projectId, presentationIdentity) { mutableStateOf(false) }
    var showAllMoney by remember(projectId, presentationIdentity) { mutableStateOf(false) }
    var showMoreDetails by remember(projectId, presentationIdentity) { mutableStateOf(false) }
    var selectedMoneyId by remember(projectId, presentationIdentity) { mutableStateOf<String?>(null) }
    val colors = MaterialTheme.colorScheme
    ArchiveDetailFrame(modifier.testTag("project-detail-screen")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, Modifier.size(48.dp).testTag("project-detail-back").semantics { contentDescription = backLabel }) {
                C2Icon(C2Glyph.Back)
            }
            Text("项目详情", Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (detail != null) Box {
                IconButton({ menuOpen = true }, Modifier.size(48.dp).testTag("project-detail-more")) {
                    C2Icon(C2Glyph.More, description = "更多项目操作")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false },
                    modifier = Modifier.testTag("project-detail-menu")) {
                    projectDetailMenuEntries(detail.project.endedOn != null,
                        detail.events.any { it.exchangeGroupId != null }, onReplacement != null, onTradeIn != null,
                        onCostHistory != null)
                        .forEach { entry ->
                            if (entry.action == ProjectDetailAction.DELETE) {
                                HorizontalDivider()
                                Text("危险操作", Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                Text("结束保留事实供历史查看；删除会永久移除项目及其金额记录。",
                                    Modifier.padding(horizontal = 12.dp).testTag("project-lifecycle-explanation"),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenuItem(text = { Text(entry.action.label,
                                color = when {
                                    !entry.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                                    entry.action == ProjectDetailAction.DELETE -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurface
                                }) }, enabled = entry.enabled,
                                modifier = Modifier.heightIn(min = 48.dp).testTag(entry.action.tag), onClick = {
                                    menuOpen = false
                                    // Existing callbacks retain requestLeave, confirmation and write qualification.
                                    when (entry.action) {
                                        ProjectDetailAction.EDIT -> onEdit()
                                        ProjectDetailAction.END -> onEnd()
                                        ProjectDetailAction.CORRECT_END -> onUpdateEnd()
                                        ProjectDetailAction.REPLACEMENT -> onReplacement?.invoke()
                                        ProjectDetailAction.TRADE_IN -> onTradeIn?.invoke()
                                        ProjectDetailAction.COST_HISTORY -> onCostHistory?.invoke()
                                        ProjectDetailAction.DELETE -> onDelete()
                                    }
                                })
                        }
                }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("project-detail-content"),
            contentPadding = PaddingValues(start = ArchivePageInset, end = ArchivePageInset, top = 8.dp, bottom = 24.dp)) {
            if (state.loading) item { Text("正在读取项目", Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.bodyLarge) }
            state.error?.let { message -> item {
                Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(message, Modifier.testTag("project-detail-error"), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge)
                    if (detail != null) Text("显示上次读取的记录。",
                        Modifier.testTag("project-detail-stale"), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("project-detail-retry")) { Text("重新读取") }
                }
            } }
            if (state.missing) item { Text("项目不存在，返回列表查看当前记录。", style = MaterialTheme.typography.bodyLarge) }
            if (detail != null) {
                item {
                    Column(Modifier.padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ProjectIdentity(detail, coverContent, expandPhotoCover)
                        if (detail.project.coverAssetId == null && onAddPhoto != null) {
                            TextButton(onAddPhoto, Modifier.heightIn(min = 48.dp).testTag("project-add-photo"),
                                enabled = photoActionEnabled && !state.loading && state.error == null) {
                                C2Icon(C2Glyph.Image)
                                Spacer(Modifier.width(8.dp))
                                Text("添加照片")
                            }
                        }
                        if (detail.project.coverAssetId != null && (onReplacePhoto != null || onRemovePhoto != null)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                onReplacePhoto?.let { action ->
                                    TextButton(action, Modifier.heightIn(min = 48.dp).testTag("project-replace-photo"),
                                        enabled = photoActionEnabled && !state.loading && state.error == null) { Text("更换照片") }
                                }
                                onRemovePhoto?.let { action ->
                                    TextButton(action, Modifier.heightIn(min = 48.dp).testTag("project-remove-photo"),
                                        enabled = photoActionEnabled && !state.loading && state.error == null) { Text("取消封面显示") }
                                }
                            }
                        }
                        photoUnavailableMessage?.let { Text(it, Modifier.testTag("project-photo-unavailable"),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                item { ProjectCostSummary(detail) }
                item {
                    ProjectFinancialDetails(detail, showFinancialDetails, { showFinancialDetails = !showFinancialDetails })
                }
                val hasMoreMoney = detail.events.size > INITIAL_MONEY_RECORD_COUNT
                // The repository supplies ascending occurrence date and ID. Keep that order at either disclosure size.
                val visibleEvents = if (showAllMoney) detail.events else detail.events.takeLast(INITIAL_MONEY_RECORD_COUNT)
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 6.dp).heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall),
                            itemVerticalAlignment = Alignment.CenterVertically) {
                            Text("金额记录", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
                            Text("${detail.events.size} 笔", Modifier.testTag("project-money-count"),
                                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        if (hasMoreMoney) TextButton({ showAllMoney = !showAllMoney },
                            Modifier.testTag("project-money-all").semantics {
                                stateDescription = if (showAllMoney) "已展开" else "已收起"
                            }) {
                            Text(if (showAllMoney) "收起" else "全部")
                        }
                    }
                }
                itemsIndexed(visibleEvents, key = { _, event -> event.id }) { index, event ->
                    ProjectMoneyRecord(event, first = index == 0, last = index == visibleEvents.lastIndex,
                        onOpen = { selectedMoneyId = event.id })
                }
                item {
                    Surface(Modifier.fillMaxWidth().padding(top = 20.dp).testTag("project-secondary-management"),
                        shape = RoundedCornerShape(14.dp), color = colors.surface,
                        border = BorderStroke(1.dp, colors.outlineVariant)) {
                        Column(Modifier.padding(horizontal = 12.dp)) {
                            ArchiveDetailDisclosure("更多资料", showMoreDetails, "project-more-details-expand") {
                                showMoreDetails = !showMoreDetails
                            }
                            if (showMoreDetails) {
                                HorizontalDivider(color = colors.outlineVariant)
                                Box(Modifier.padding(vertical = 12.dp)) { ProjectAdditionalDetails(detail, onCostHistory) }
                            }
                        }
                    }
                }
            }
        }
        if (detail != null) Surface(color = colors.background) {
            Column {
                HorizontalDivider(color = colors.outlineVariant)
                Button({ moneySheetOpen = true }, Modifier.fillMaxWidth().padding(horizontal = ArchivePageInset, vertical = 9.dp)
                    .testTag("project-record-money"), shape = CircleShape) {
                    C2Icon(C2Glyph.Add)
                    Spacer(Modifier.width(8.dp))
                    Text("记一笔", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
    val selectedEvent = selectedMoneyId?.let { id -> detail?.events?.firstOrNull { it.id == id } }
    if (selectedMoneyId != null && selectedEvent == null) {
        LaunchedEffect(selectedMoneyId) { selectedMoneyId = null }
    }
    selectedEvent?.let { event ->
        ProjectMoneyDetail(event, onDismiss = { selectedMoneyId = null }, onCorrect = {
            if (selectedMoneyId == event.id) {
                selectedMoneyId = null
                if (event.role == ProjectMoneyEventRole.END_SETTLEMENT) onUpdateEnd() else onCorrectMoney(event.id)
            }
        })
    }
    if (moneySheetOpen && detail != null) ModalBottomSheet(onDismissRequest = { moneySheetOpen = false },
        modifier = Modifier.testTag("project-money-sheet"), containerColor = LocalLithicVisuals.current.sheetSurface) {
        Column(Modifier.fillMaxWidth().padding(start = LithicSpacing.page, end = LithicSpacing.page, bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("记一笔", Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                IconButton({ moneySheetOpen = false }, Modifier.size(48.dp).testTag("project-money-sheet-close")) {
                    C2Icon(C2Glyph.Close, description = "关闭金额类型选择")
                }
            }
            TextButton({ moneySheetOpen = false; onAddMoney(MoneyDirection.OUTLAY) },
                Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("project-add-outlay")) {
                Text("记录投入", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                C2Icon(C2Glyph.Chevron)
            }
            HorizontalDivider()
            TextButton({ moneySheetOpen = false; onAddMoney(MoneyDirection.RECOVERY) },
                Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("project-add-recovery")) {
                Text("记录收回", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                C2Icon(C2Glyph.Chevron)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectIdentity(detail: ProjectDetail, coverContent: (@Composable () -> Unit)?, expandPhotoCover: Boolean) {
    val colors = MaterialTheme.colorScheme
    val hasCover = detail.project.coverAssetId != null
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (hasCover && coverContent != null) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = 4.dp), contentAlignment = Alignment.Center) {
                val coverWidth = if (LocalDensity.current.fontScale > 1.3f) 240.dp
                    else (maxWidth - 40.dp).coerceIn(288.dp, 310.dp)
                Box(Modifier.width(coverWidth).aspectRatio(4f / 3f).clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceVariant)
                    .then(if (expandPhotoCover) Modifier.testTag("project-detail-cover-expanded") else Modifier)) {
                    Box(Modifier.fillMaxSize().testTag("project-detail-cover")) { coverContent() }
                }
            }
        } else {
            Box(Modifier.size(48.dp).testTag("project-identity-symbol"), contentAlignment = Alignment.Center) {
                if (hasCover) C2Icon(C2Glyph.ImageError)
                else C2RecordIdentity(detail.project.name, detail.category.name, C2Glyph.Project)
            }
        }
        Text(detail.project.name, Modifier.testTag("project-detail-name").semantics { heading() },
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically) {
            Text(detail.category.name, Modifier.testTag("project-detail-category"),
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Surface(shape = CircleShape, color = colors.surfaceVariant, contentColor = colors.onSurface) {
                Text(projectStatusLabel(detail), Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    .testTag("project-detail-status"), style = MaterialTheme.typography.bodyMedium)
            }
            FlowRow(Modifier.testTag("project-ownership-period"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("持有", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Text(if (detail.cost.unavailableReason == null) "${detail.cost.heldDays}天" else "尚未开始",
                    Modifier.testTag("project-detail-held-days"), style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant)
            }
        }
        if (hasCover && coverContent == null) Text("封面暂不可用", Modifier.testTag("project-detail-cover-unavailable"),
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun ProjectCostSummary(detail: ProjectDetail) {
    val outlay = BigDecimal.valueOf(detail.cost.grossOutlayCents, 2)
    val rateLabel = projectDetailRateLabel(detail.project.endedOn != null, detail.cost.contributesToCurrentTotal)
    val spacing = LithicSpacing.small
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("project-core-metrics")) {
        val metricWidth = ((maxWidth - spacing) / 2 - LithicSpacing.small * 2).coerceAtLeast(0.dp)
        val sideBySide = metricWidth >= 88.dp && LocalDensity.current.fontScale <= 1.3f &&
            moneyMetricFitsAtPreferredSize(detail.cost.dailyRate, metricWidth, AmountUnit.YUAN_PER_DAY,
                MoneyMetricLevel.DETAIL, showDailyUnit = false) &&
            moneyMetricFitsAtPreferredSize(outlay, metricWidth, AmountUnit.YUAN, MoneyMetricLevel.DETAIL)
        val cards: @Composable (Modifier) -> Unit = { modifier ->
            ProjectCostCard("日均成本", detail.cost.dailyRate, AmountUnit.YUAN_PER_DAY,
                "project-cost-daily-card", "project-detail-daily", modifier, semanticLabel = rateLabel)
            ProjectCostCard("累计投入", outlay, AmountUnit.YUAN,
                "project-cost-outlay-card", "project-core-outlay", modifier)
        }
        if (sideBySide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                cards(Modifier.weight(1f))
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                cards(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ProjectCostCard(
    title: String,
    amount: BigDecimal?,
    unit: AmountUnit,
    cardTag: String,
    amountTag: String,
    modifier: Modifier,
    semanticLabel: String = title,
) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.testTag(cardTag), shape = RoundedCornerShape(14.dp),
        color = LocalLithicVisuals.current.costSurface, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Column(Modifier.padding(LithicSpacing.small), verticalArrangement = Arrangement.spacedBy(LithicSpacing.xxSmall)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            MoneyMetric(amount, Modifier.fillMaxWidth().testTag(amountTag), unit = unit,
                level = MoneyMetricLevel.DETAIL, label = semanticLabel, showDailyUnit = false, keepPreferredSize = true)
        }
    }
}

@Composable
private fun ProjectFinancialDetails(detail: ProjectDetail, expanded: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val includesCredit = detail.events.any { it.basis == MoneyBasis.TRADE_IN_CREDIT }
    val basis = if (includesCredit) "（含非现金折抵）" else ""
    val outlay = detail.cost.grossOutlayCents
    val recovery = detail.cost.recoveryCents
    Surface(Modifier.fillMaxWidth().padding(top = 12.dp)
        .testTag("project-financial-totals"),
        shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, colors.outlineVariant),
        color = colors.surface) {
        Column(Modifier.padding(horizontal = 12.dp)) {
            ArchiveDetailDisclosure("财务明细", expanded, "project-financial-expand", onToggle)
            if (expanded) {
                Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("project-financial-details"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProjectFinancialAmount("累计投入$basis", outlay, "project-detail-outlay", "project-financial-outlay")
                    HorizontalDivider(color = colors.outlineVariant)
                    ProjectFinancialAmount("累计收回$basis", recovery, "project-detail-recovery", "project-financial-recovery")
                    HorizontalDivider(color = colors.outlineVariant)
                    ProjectFinancialAmount("净投入", detail.cost.netCents, "project-detail-net", "project-financial-net")
                }
            }
        }
    }
}

@Composable
private fun ProjectFinancialAmount(
    label: String,
    cents: Long,
    amountTag: String,
    factTag: String,
) {
    Column(Modifier.testTag(factTag), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyMetric(BigDecimal.valueOf(cents, 2), Modifier.testTag(amountTag), AmountUnit.YUAN,
            level = MoneyMetricLevel.ROW, label = label)
    }
}

@Composable
private fun ProjectAdditionalDetails(detail: ProjectDetail, onCostHistory: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().testTag("project-more-details"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("开始拥有：${detail.project.startedOn}", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        detail.project.endedOn?.let { Text("已记录结束日期：$it", style = MaterialTheme.typography.bodyMedium) }
        detail.project.endReason?.takeIf { it.isNotBlank() }?.let {
            Text("结束原因：$it", style = MaterialTheme.typography.bodyMedium)
        }
        detail.project.note?.takeIf { it.isNotBlank() }?.let {
            Text(it, Modifier.testTag("project-detail-note"), style = MaterialTheme.typography.bodyLarge)
        }
        if (detail.events.any { it.exchangeGroupId != null }) Text(
            "此项目参与以旧换新，暂不支持单独删除或修改交易日期与金额；可继续管理名称、类别、备注及后续真实金额。",
            Modifier.testTag("project-trade-in-protection"), style = MaterialTheme.typography.bodyMedium)
        C5InformationButton("项目成本口径", Modifier.testTag("project-detail-metric-information"), buttonLabel = "成本口径") {
            Text("观察日期 ${detail.asOf}", Modifier.testTag("project-detail-metric-date"))
            Text("日均成本＝净投入 ÷ 持有天数（含开始日），仅显示时舍入。累计投入含非现金折抵。")
            if (detail.cost.unavailableReason != null) Text("尚未开始，没有有效日均成本。")
            val endedOn = detail.project.endedOn
            if (endedOn != null && !detail.cost.contributesToCurrentTotal && detail.cost.unavailableReason == null) {
                val hasObservedLateMoney = detail.events.any { event ->
                    event.occurredOn.isAfter(endedOn) && !event.occurredOn.isAfter(detail.asOf)
                }
                Text(if (hasObservedLateMoney) "包含结束后的实际结算；持有天数不再增加。" else "持有天数已冻结。")
                Text("可补录结束后的尾款或退款。")
            }
        }
        onCostHistory?.let { action ->
            TextButton(action, Modifier.fillMaxWidth().testTag("project-cost-history")) {
                Text("查看成本历史", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                C2Icon(C2Glyph.Chevron)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectMoneyRecord(event: ProjectMoneyEventEntity, first: Boolean, last: Boolean, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val radius = 14.dp
    val shape = RoundedCornerShape(
        topStart = if (first) radius else 0.dp, topEnd = if (first) radius else 0.dp,
        bottomStart = if (last) radius else 0.dp, bottomEnd = if (last) radius else 0.dp,
    )
    val eventAmount = BigDecimal.valueOf(event.amountCents, 2)
    val eventTypeLabel = projectMoneyEventTypeLabel(event.direction, event.role)
    val basis = projectMoneyEventBasisLabel(event.basis)
    // Draw the continuation of the card across lazy rows without measuring hidden history.
    Column(Modifier.fillMaxWidth().clipToBounds().clip(shape).background(colors.surface).drawBehind {
        val line = 1.dp.toPx()
        val topExtension = if (first) 0f else radius.toPx()
        val bottomExtension = if (last) 0f else radius.toPx()
        drawRoundRect(colors.outlineVariant, Offset(line / 2, line / 2 - topExtension),
            Size(size.width - line, size.height - line + topExtension + bottomExtension),
            CornerRadius(radius.toPx()), style = Stroke(line))
    }.testTag("project-event-${event.id}")) {
        BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .testTag("project-event-details-${event.id}")
            .clickable(role = Role.Button, onClickLabel = "查看金额记录", onClick = onOpen)
            .padding(start = 12.dp, end = 10.dp), contentAlignment = Alignment.CenterStart) {
            val amountWidth = ((maxWidth - 52.dp) * 0.42f).coerceAtLeast(0.dp)
            val stacked = maxWidth < 264.dp || LocalDensity.current.fontScale > 1.3f ||
                !moneyMetricFitsAtPreferredSize(eventAmount, amountWidth, AmountUnit.YUAN)
            val meta: @Composable () -> Unit = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                    Text(event.occurredOn.toString(), Modifier.testTag("project-event-date-${event.id}"),
                        style = MaterialTheme.typography.bodyMedium)
                    Text(eventTypeLabel, Modifier.testTag("project-event-type-${event.id}"),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                if (event.basis == MoneyBasis.TRADE_IN_CREDIT) {
                    Text(basis, Modifier.testTag("project-event-basis-${event.id}"),
                        style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            }
            val amount: @Composable () -> Unit = {
                MoneyMetric(eventAmount, Modifier.testTag("project-event-amount-${event.id}"),
                    unit = AmountUnit.YUAN, level = MoneyMetricLevel.ROW, label = "$eventTypeLabel（$basis）")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (stacked) {
                    Column(Modifier.weight(1f).padding(vertical = 6.dp)) { meta(); amount() }
                } else {
                    Column(Modifier.weight(1f).padding(vertical = 5.dp)) { meta() }
                    Box(Modifier.width(amountWidth), contentAlignment = Alignment.CenterEnd) { amount() }
                }
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    C2Icon(C2Glyph.Chevron)
                }
            }
        }
        if (!last) HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = colors.outlineVariant)
    }
}

@Composable
private fun ProjectMoneyDetail(event: ProjectMoneyEventEntity, onDismiss: () -> Unit, onCorrect: () -> Unit) {
    val eventTypeLabel = projectMoneyEventTypeLabel(event.direction, event.role)
    val basis = projectMoneyEventBasisLabel(event.basis)
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("project-event-dialog"),
        title = { Text(eventTypeLabel) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("发生日期 ${event.occurredOn}")
                MoneyMetric(BigDecimal.valueOf(event.amountCents, 2), unit = AmountUnit.YUAN,
                    label = "$eventTypeLabel（$basis）")
                Text(basis, if (event.basis == MoneyBasis.CASH) Modifier.testTag("project-event-basis-${event.id}") else Modifier)
                event.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                if (event.exchangeGroupId != null) Text("以旧换新关联事实；金额及日期不能单独修改。")
            }
        },
        dismissButton = { TextButton(onDismiss, Modifier.testTag("project-event-dialog-close")) { Text("关闭") } },
        confirmButton = {
            TextButton(onCorrect, Modifier.testTag("project-correct-event-${event.id}")) {
                Text(when {
                    event.exchangeGroupId != null -> "修改关联说明"
                    event.role == ProjectMoneyEventRole.END_SETTLEMENT -> "修正结束及关联收回"
                    else -> "纠正这条记录"
                })
            }
        })
}
