package com.cooper.lithic.feature.project

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooper.lithic.core.design.AmountFormatter
import com.cooper.lithic.core.design.AmountText
import com.cooper.lithic.core.design.AmountUnit
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.C2RecordIdentity
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.ProjectDetail
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
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
    // Presentation slot only. The product baseline has no validated media read facade;
    // callers must never synthesize a path or bypass verification to fill this slot.
    coverContent: (@Composable () -> Unit)? = null,
) {
    val detail = state.detail?.takeUnless { state.missing }
    var menuOpen by rememberSaveable(detail?.project?.id) { mutableStateOf(false) }
    var moneySheetOpen by rememberSaveable(detail?.project?.id) { mutableStateOf(false) }
    Column(modifier.fillMaxSize().testTag("project-detail-screen")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onBack, Modifier.weight(1f).heightIn(min = 48.dp).testTag("project-detail-back")) {
                C2Icon(C2Glyph.Back)
                Spacer(Modifier.width(8.dp))
                Text(backLabel, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            }
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (state.loading) item { Text("正在读取项目", style = MaterialTheme.typography.bodyLarge) }
            state.error?.let { message -> item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(message, Modifier.testTag("project-detail-error"), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge)
                    if (detail != null) Text("暂时无法刷新，以下为上次读取记录。",
                        Modifier.testTag("project-detail-stale"), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("project-detail-retry")) { Text("重新读取") }
                }
            } }
            if (state.missing) item { Text("项目不存在，返回列表查看当前记录。", style = MaterialTheme.typography.bodyLarge) }
            if (detail != null) {
                item { ProjectIdentity(detail, coverContent) }
                item { ProjectCostSummary(detail) }
                item { ProjectSecondaryTotals(detail) }
                item {
                    HorizontalDivider()
                    Text("真实金额记录", Modifier.padding(top = 24.dp).semantics { heading() },
                        style = MaterialTheme.typography.titleLarge)
                }
                if (detail.events.isEmpty()) item { Text("没有金额记录", style = MaterialTheme.typography.bodyLarge) }
                // Repository order and the complete fact collection are retained; no new query or truncation.
                itemsIndexed(detail.events, key = { _, event -> event.id }) { _, event ->
                    ProjectMoneyRecord(event, onCorrectMoney, onUpdateEnd)
                }
                item {
                    Column(Modifier.testTag("project-secondary-management"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        if (detail != null) Surface(color = MaterialTheme.colorScheme.background) {
            Button({ moneySheetOpen = true }, Modifier.fillMaxWidth().padding(16.dp)
                .heightIn(min = 48.dp).testTag("project-record-money")) {
                C2Icon(C2Glyph.Add)
                Spacer(Modifier.width(8.dp))
                Text("记一笔", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
    if (moneySheetOpen && detail != null) ModalBottomSheet(onDismissRequest = { moneySheetOpen = false },
        modifier = Modifier.testTag("project-money-sheet")) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
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

@Composable
private fun ProjectIdentity(detail: ProjectDetail, coverContent: (@Composable () -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (detail.project.coverAssetId != null && coverContent != null) {
            Box(Modifier.fillMaxWidth().heightIn(max = 160.dp).clip(RoundedCornerShape(12.dp))
                .testTag("project-detail-cover")) { coverContent() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.testTag("project-identity-symbol")) {
                if (detail.project.coverAssetId != null && coverContent == null) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        C2Icon(C2Glyph.ImageError)
                    }
                } else C2RecordIdentity(detail.project.name, detail.category.name, C2Glyph.Project)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(detail.project.name, Modifier.testTag("project-detail-name").semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium)
                Text(detail.category.name, Modifier.testTag("project-detail-category"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(shape = RoundedCornerShape(50),
                    color = if (detail.cost.contributesToCurrentTotal) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (detail.cost.contributesToCurrentTotal) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant) {
                    Text(projectStatusLabel(detail), Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("project-detail-status"), style = MaterialTheme.typography.labelLarge)
                }
                if (detail.project.coverAssetId != null && coverContent == null) Text("封面暂不可用",
                    Modifier.testTag("project-detail-cover-unavailable"), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProjectCostSummary(detail: ProjectDetail) {
    Surface(Modifier.fillMaxWidth().testTag("project-core-metrics"), shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stack = maxWidth < 280.dp || LocalDensity.current.fontScale > 1.3f ||
                    AmountFormatter.formatDailyRate(detail.cost.dailyRate).length > 9
                val daily: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${projectDetailRateLabel(detail.project.endedOn != null, detail.cost.contributesToCurrentTotal)} · 元/天",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AmountText(detail.cost.dailyRate, Modifier.testTag("project-detail-daily"),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 38.sp), showUnit = false,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                val held: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("累计持有", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (detail.cost.unavailableReason == null) "${detail.cost.heldDays} 天" else "尚未开始",
                            Modifier.testTag("project-detail-held-days"), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp))
                    }
                }
                if (stack) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { daily(); held() }
                else Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1.4f)) { daily() }
                    Box(Modifier.weight(1f)) { held() }
                }
            }
            Text("结果截至 ${detail.asOf}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (detail.cost.unavailableReason != null) Text("尚未开始，没有有效日均成本。", style = MaterialTheme.typography.bodySmall)
            if (detail.project.endedOn != null && !detail.cost.contributesToCurrentTotal && detail.cost.unavailableReason == null) {
                val hasObservedLateMoney = detail.events.any { event ->
                    event.occurredOn.isAfter(detail.project.endedOn) && !event.occurredOn.isAfter(detail.asOf)
                }
                Text(if (hasObservedLateMoney) "包含结束后的实际结算；持有天数不再增加。"
                    else "结束后仍可补录真实尾款或退款；持有天数不再增加。", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val stack = includesCredit || LocalDensity.current.fontScale > 1.3f || totals.any { AmountFormatter.formatCny(BigDecimal.valueOf(it.second, 2)).length > 10 }
    Surface(Modifier.fillMaxWidth().testTag("project-financial-totals"), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface) {
        if (stack) Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            totals.forEachIndexed { index, total ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ProjectTotal(total.first, total.second, total.third)
            }
        } else Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            totals.forEach { (label, cents, tag) -> Box(Modifier.weight(1f)) { ProjectTotal(label, cents, tag) } }
        }
    }
}

@Composable
private fun ProjectTotal(label: String, cents: Long, tag: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AmountText(BigDecimal.valueOf(cents, 2), Modifier.testTag(tag), AmountUnit.YUAN,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectMoneyRecord(event: ProjectMoneyEventEntity, onCorrectMoney: (String) -> Unit, onUpdateEnd: () -> Unit) {
    Row(Modifier.fillMaxWidth().testTag("project-event-${event.id}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.padding(top = 8.dp).size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val type: @Composable () -> Unit = {
                    Text(projectMoneyEventTypeLabel(event.direction, event.role),
                        Modifier.testTag("project-event-type-${event.id}"), style = MaterialTheme.typography.titleMedium)
                }
                val amount: @Composable () -> Unit = {
                    AmountText(BigDecimal.valueOf(event.amountCents, 2), Modifier.testTag("project-event-amount-${event.id}"),
                        unit = AmountUnit.YUAN, style = MaterialTheme.typography.titleLarge, showUnit = false)
                }
                if (maxWidth < 260.dp || LocalDensity.current.fontScale > 1.3f) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { type(); amount() }
                } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) { type() }
                    Box(Modifier.weight(1.2f)) { amount() }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(event.occurredOn.toString(), Modifier.testTag("project-event-date-${event.id}"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(projectMoneyEventBasisLabel(event.basis), Modifier.testTag("project-event-basis-${event.id}"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            event.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            if (event.exchangeGroupId != null) Text("以旧换新关联事实；金额及日期不能单独修改。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton({ if (event.role == ProjectMoneyEventRole.END_SETTLEMENT) onUpdateEnd() else onCorrectMoney(event.id) },
                Modifier.heightIn(min = 48.dp).testTag("project-correct-event-${event.id}")) {
                Text(if (event.exchangeGroupId != null) "修改关联说明" else if (event.role == ProjectMoneyEventRole.END_SETTLEMENT)
                    "修正结束及关联收回" else "纠正这条记录", style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
