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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
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
import com.cooper.lithic.core.design.moneyMetricFitsAtPreferredSize
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.ProjectDetail
import java.math.BigDecimal
import com.cooper.lithic.core.design.LithicButton as Button

private const val INITIAL_MONEY_RECORD_COUNT = 3
private val ProjectDetailNavy = Color(0xFF223246)

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
    var showAllMoney by remember(projectId, presentationIdentity) { mutableStateOf(false) }
    var selectedMoneyId by remember(projectId, presentationIdentity) { mutableStateOf<String?>(null) }
    val colors = MaterialTheme.colorScheme
    val light = colors.background.luminance() > 0.5f
    Column(modifier.fillMaxSize().background(colors.background).testTag("project-detail-screen")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, Modifier.size(48.dp).testTag("project-detail-back").semantics { contentDescription = backLabel }) {
                C2Icon(C2Glyph.Back)
            }
            Spacer(Modifier.weight(1f))
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
            contentPadding = PaddingValues(start = LithicSpacing.page, end = LithicSpacing.page, top = 8.dp, bottom = 24.dp)) {
            if (state.loading) item { Text("正在读取项目", Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.bodyLarge) }
            state.error?.let { message -> item {
                Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(message, Modifier.testTag("project-detail-error"), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge)
                    if (detail != null) Text("暂时无法刷新，以下为上次读取记录。",
                        Modifier.testTag("project-detail-stale"), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("project-detail-retry")) { Text("重新读取") }
                }
            } }
            if (state.missing) item { Text("项目不存在，返回列表查看当前记录。", style = MaterialTheme.typography.bodyLarge) }
            if (detail != null) {
                item {
                    Column(Modifier.padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                item { ProjectSecondaryTotals(detail) }
                val hasMoreMoney = detail.events.size > INITIAL_MONEY_RECORD_COUNT
                val visibleEvents = if (showAllMoney) detail.events else detail.events.take(INITIAL_MONEY_RECORD_COUNT)
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("金额记录", Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        if (hasMoreMoney) TextButton({ showAllMoney = !showAllMoney },
                            Modifier.testTag("project-money-all").semantics {
                                stateDescription = if (showAllMoney) "已展开" else "已收起"
                            }) {
                            Text(if (showAllMoney) "收起" else "全部")
                        }
                    }
                }
                if (detail.events.isEmpty()) item { Text("没有金额记录", style = MaterialTheme.typography.bodyLarge) }
                itemsIndexed(visibleEvents, key = { _, event -> event.id }) { index, event ->
                    ProjectMoneyRecord(event, first = index == 0, last = index == visibleEvents.lastIndex,
                        onOpen = { selectedMoneyId = event.id })
                }
                item {
                    Column(Modifier.padding(top = 20.dp).testTag("project-secondary-management"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalDivider()
                        Text("资料与历史", Modifier.padding(top = 12.dp).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
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
                        onCostHistory?.let { action ->
                            TextButton(action, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-cost-history")) {
                                Text("查看成本历史", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                C2Icon(C2Glyph.Chevron)
                            }
                        }
                    }
                }
            }
        }
        if (detail != null) Surface(color = colors.background) {
            Column {
                HorizontalDivider(color = colors.outlineVariant)
                Button({ moneySheetOpen = true }, Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.page, vertical = 9.dp)
                    .testTag("project-record-money"), shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = if (light) ProjectDetailNavy else colors.primary,
                        contentColor = if (light) Color.White else colors.onPrimary)) {
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
        modifier = Modifier.testTag("project-money-sheet")) {
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
                Text("累计持有", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
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
    val metricLabel = projectDetailRateLabel(detail.project.endedOn != null, detail.cost.contributesToCurrentTotal)
    val colors = MaterialTheme.colorScheme
    val cardColor = when {
        colors.background.luminance() < 0.5f -> Color(0xFF202C39)
        colors.background == Color.White -> Color(0xFFEFF4FA)
        else -> colors.primaryContainer
    }
    Surface(Modifier.fillMaxWidth().testTag("project-core-metrics"), shape = RoundedCornerShape(14.dp),
        color = cardColor, border = BorderStroke(1.dp, colors.outlineVariant)) {
        BoxWithConstraints(Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)) {
            val wide = maxWidth >= 274.dp && LocalDensity.current.fontScale <= 1.3f &&
                moneyMetricFitsAtPreferredSize(detail.cost.dailyRate, maxWidth - 134.dp, level = MoneyMetricLevel.HERO)
            val metric: @Composable () -> Unit = {
                Text(metricLabel, style = MaterialTheme.typography.titleSmall)
                MoneyMetric(detail.cost.dailyRate, Modifier.fillMaxWidth().testTag("project-detail-daily"),
                    level = MoneyMetricLevel.HERO, label = metricLabel)
            }
            val cutoff: @Composable () -> Unit = {
                Text("截至", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text(detail.asOf.toString(), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            val information: @Composable () -> Unit = {
                C5InformationButton("项目成本口径", Modifier.testTag("project-detail-metric-information"), iconOnly = true) {
                    Text("日均成本来自真实投入和收回，开始日计入持有自然日。展示舍入不改变原计算结果。")
                    Text("结束后仍可补录真实尾款或退款；持有天数不再增加。")
                }
            }
            Column {
                if (wide) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { metric() }
                    Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(36.dp).background(colors.outlineVariant))
                    Column(Modifier.widthIn(min = 73.dp)) { cutoff() }
                    information()
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) { metric() }
                        information()
                    }
                    cutoff()
                }
                if (detail.cost.unavailableReason != null) Text("尚未开始，没有有效日均成本。", style = MaterialTheme.typography.bodySmall)
                if (detail.project.endedOn != null && !detail.cost.contributesToCurrentTotal && detail.cost.unavailableReason == null) {
                    val hasObservedLateMoney = detail.events.any { event ->
                        event.occurredOn.isAfter(detail.project.endedOn) && !event.occurredOn.isAfter(detail.asOf)
                    }
                    Text(if (hasObservedLateMoney) "包含结束后的实际结算；持有天数不再增加。" else "持有天数已冻结。",
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ProjectSecondaryTotals(detail: ProjectDetail) {
    val includesCredit = detail.events.any { it.basis == MoneyBasis.TRADE_IN_CREDIT }
    val totals = listOf(
        Triple(if (includesCredit) "累计投入（含非现金折抵）" else "累计实际投入", detail.cost.grossOutlayCents, "project-detail-outlay"),
        Triple(if (includesCredit) "累计收回（含非现金折抵）" else "累计实际收回", detail.cost.recoveryCents, "project-detail-recovery"),
        Triple("净投入", detail.cost.netCents, "project-detail-net"),
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columnWidth = ((maxWidth - 16.dp) / 3 - 16.dp).coerceAtLeast(0.dp)
        val stack = includesCredit || LocalDensity.current.fontScale > 1.3f || totals.any {
            !moneyMetricFitsAtPreferredSize(BigDecimal.valueOf(it.second, 2), columnWidth,
                AmountUnit.YUAN, level = MoneyMetricLevel.ROW)
        }
        val group = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("project-financial-totals")
        if (stack) Column(group, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            totals.forEach { (label, cents, tag) -> ProjectTotal(label, cents, tag, Modifier.fillMaxWidth()) }
        } else Row(group, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            totals.forEach { (label, cents, tag) ->
                ProjectTotal(label, cents, tag, Modifier.weight(1f), centered = true)
            }
        }
    }
}

@Composable
private fun ProjectTotal(label: String, cents: Long, tag: String, modifier: Modifier, centered: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, colors.outlineVariant),
        color = colors.surface, shadowElevation = 1.dp) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            MoneyMetric(BigDecimal.valueOf(cents, 2), Modifier.testTag(tag), AmountUnit.YUAN,
                level = MoneyMetricLevel.ROW, label = label)
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
                        style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Text(eventTypeLabel, Modifier.testTag("project-event-type-${event.id}"), style = MaterialTheme.typography.titleSmall)
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
