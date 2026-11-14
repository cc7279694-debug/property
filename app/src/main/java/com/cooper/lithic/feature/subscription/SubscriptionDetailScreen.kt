package com.cooper.lithic.feature.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import com.cooper.lithic.core.design.LithicIconButton as IconButton
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import com.cooper.lithic.core.design.LithicOutlinedButton as OutlinedButton
import com.cooper.lithic.core.design.LithicButton as Button
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.*
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.math.BigDecimal
import java.time.LocalDate

private const val VISIBLE_PAYMENTS = 3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SubscriptionDetailScreen(
    state: SubscriptionDetailUiState,
    onBack: () -> Unit,
    onRenewal: () -> Unit,
    onLoadMoreHistory: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onStop: () -> Unit = {},
    onReopen: () -> Unit = {},
    onCorrectState: (String) -> Unit = {},
    onCancelStop: (String) -> Unit = {},
    onCorrectRenewal: (String) -> Unit = {},
    onDeleteRenewal: (String) -> Unit = {},
    backLabel: String = "返回订阅列表",
    onCostHistory: (() -> Unit)? = null,
    coverContent: (@Composable () -> Unit)? = null,
) {
    val detail = state.detail?.takeUnless { state.missing }
    val subscriptionId = detail?.subscription?.id
    var managementOpen by rememberSaveable(subscriptionId) { mutableStateOf(false) }
    var showAllPayments by remember(subscriptionId) { mutableStateOf(false) }
    var showMoreDetails by remember(subscriptionId) { mutableStateOf(false) }
    var planOpen by remember(subscriptionId) { mutableStateOf<String?>(null) }
    var selectedPaymentId by remember(subscriptionId) { mutableStateOf<String?>(null) }
    val loadedPayments = state.history.items
    val payments = if (showAllPayments) loadedPayments else loadedPayments.take(VISIBLE_PAYMENTS)
    val nextPayment = detail?.let(::nextPaidPlan)
    val planPayment = if (planOpen == detail?.currentRenewal?.id && detail?.cost?.currentPrice != null)
        detail.currentRenewal else nextPayment?.takeIf { it.id == planOpen }
    val historyEnabled = !state.loading && state.error == null && state.history.error == null
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxSize().testTag("subscription-detail-screen")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, Modifier.size(48.dp).testTag("subscription-detail-back").semantics { contentDescription = backLabel }) {
                C2Icon(C2Glyph.Back)
            }
            Spacer(Modifier.weight(1f))
            if (state.detail != null && !state.missing) IconButton({ managementOpen = true },
                Modifier.size(48.dp).testTag("subscription-detail-open-management")) {
                C2Icon(C2Glyph.More, description = "更多订阅操作")
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("subscription-detail-content"),
            contentPadding = PaddingValues(horizontal = LithicSpacing.page, vertical = 8.dp)) {
            if (state.loading) item { Text("正在读取订阅", Modifier.padding(LithicSpacing.medium)) }
            state.error?.let { message -> item {
                Column {
                    Text(message, Modifier.testTag("subscription-detail-error"), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge)
                    if (state.detail != null) Text("以下为上次读取记录。", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-detail-retry")) { Text("重新读取") }
                }
            } }
            if (state.missing) item { Text("订阅不存在，请返回列表。", Modifier.padding(LithicSpacing.medium)
                .testTag("subscription-detail-missing"), style = MaterialTheme.typography.bodyLarge) }
            detail?.let { detail ->
                item {
                    Column(Modifier.padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SubscriptionIdentity(detail, coverContent)
                        if (detail.cost.displayStatus == SubscriptionDisplayStatus.PAUSED && detail.renewals.none {
                            !it.paidOn.isAfter(detail.asOf) && !it.effectiveFrom.isAfter(detail.asOf) &&
                                detail.asOf.isBefore(it.paidUntilExclusive)
                        }) Text("付费覆盖已到期。恢复前请核对预付覆盖或实际续费。",
                            Modifier.testTag("subscription-detail-paused-expired"), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item { Box(Modifier.padding(bottom = 12.dp)) { SubscriptionArchiveMetrics(detail) } }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val current = detail.currentRenewal
                        val currentPrice = detail.cost.currentPrice
                        if (current != null && currentPrice != null) {
                            SubscriptionPlanTile("当前套餐", cyclePrice(current.cycle, currentPrice.cents),
                                C2Glyph.Subscription, "subscription-detail-current-plan") { planOpen = current.id }
                        } else {
                            Surface(Modifier.fillMaxWidth().testTag("subscription-detail-current-plan"),
                                shape = MaterialTheme.shapes.medium, color = colors.surface,
                                border = BorderStroke(1.dp, colors.outlineVariant)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("当前无有效套餐", Modifier.testTag("subscription-detail-no-current-plan"),
                                        style = MaterialTheme.typography.titleSmall)
                                    Text("当前日均贡献", style = MaterialTheme.typography.bodySmall)
                                    MoneyMetric(detail.cost.currentRate, Modifier.testTag("subscription-detail-no-plan-rate"),
                                        level = MoneyMetricLevel.ROW, label = "当前日均贡献")
                                }
                            }
                        }
                        nextPayment?.let { next ->
                            SubscriptionPlanTile("下一期", "已付款 · ${shortDate(next.effectiveFrom, detail.asOf)}生效",
                                C2Glyph.Review, "subscription-detail-next-plan",
                                subtitleDescription = "已付款，尚未生效，实际生效日期：${next.effectiveFrom}") { planOpen = next.id }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp),
                            itemVerticalAlignment = Alignment.CenterVertically) {
                            Text("付款历史", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                            state.history.totalCount?.takeIf { !state.history.loading && state.history.error == null &&
                                !state.loading && state.error == null }?.let {
                                Text("$it 笔", Modifier.testTag("subscription-detail-payment-count"),
                                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                        }
                        if (loadedPayments.size > VISIBLE_PAYMENTS) {
                            TextButton({ showAllPayments = !showAllPayments },
                                Modifier.testTag("subscription-detail-show-payments").semantics {
                                    stateDescription = if (showAllPayments) "当前已加载付款已展开"
                                        else "当前已加载付款已收起"
                                }) {
                                Text(if (showAllPayments) "收起" else "全部")
                                C2Icon(C2Glyph.Chevron)
                            }
                        }
                    }
                }
                if (state.history.loading) item { Text("正在读取付款历史", Modifier.padding(LithicSpacing.medium)) }
                state.history.error?.let { message -> item {
                    Column {
                        Text(message, Modifier.testTag("subscription-detail-history-error"), color = MaterialTheme.colorScheme.error)
                        if (state.history.items.isNotEmpty()) Text("以下为上次读取的付款历史。", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-detail-history-retry")) { Text("重新读取付款历史") }
                    }
                } }
                if (!state.history.loading && state.history.error == null && state.history.totalCount == 0L && state.history.items.isEmpty()) item {
                    Text("暂无付费记录", Modifier.padding(LithicSpacing.medium), style = MaterialTheme.typography.bodyLarge)
                }
                items(payments, key = { it.id }) { renewal ->
                    RenewalHistoryRow(renewal, detail.asOf, first = renewal.id == payments.first().id,
                        last = renewal.id == payments.last().id) { selectedPaymentId = renewal.id }
                }
                if (state.history.hasMore) item {
                    Column(Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onLoadMoreHistory, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("subscription-detail-load-more-history"), enabled = !state.history.loading && !state.history.loadingMore) {
                            Text(if (state.history.loadingMore) "正在读取下一页" else "继续读取付款历史", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                item {
                    Column(Modifier.padding(top = 8.dp, bottom = 12.dp)) {
                        TextButton({ showMoreDetails = !showMoreDetails },
                            Modifier.testTag("subscription-detail-more").semantics {
                                stateDescription = if (showMoreDetails) "已展开" else "已收起"
                            }) {
                            Text(if (showMoreDetails) "收起其他资料" else "其他资料")
                            C2Icon(C2Glyph.Chevron)
                        }
                        if (showMoreDetails) SubscriptionOtherDetails(detail, !state.loading && state.error == null, onCorrectState)
                    }
                }
            }
        }
        if (detail != null && detail.cost.displayStatus != SubscriptionDisplayStatus.STOPPED) {
            Surface(color = colors.background) {
                Button(onRenewal, Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.page, vertical = 9.dp).heightIn(min = 48.dp)
                    .testTag("subscription-detail-renewal"), enabled = !state.loading && state.error == null, shape = CircleShape) {
                    C2Icon(C2Glyph.Add)
                    Spacer(Modifier.width(8.dp))
                    Text("记录续费", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
    if (detail != null) {
        if (planOpen != null && planPayment == null) LaunchedEffect(planOpen) { planOpen = null }
        planPayment?.let { payment -> SubscriptionPlanDialog(detail, payment, payment.id == detail.currentRenewal?.id) { planOpen = null } }
        if (selectedPaymentId != null && loadedPayments.none { it.id == selectedPaymentId }) LaunchedEffect(selectedPaymentId) {
            selectedPaymentId = null
        }
        loadedPayments.firstOrNull { it.id == selectedPaymentId }?.let { payment ->
            RenewalHistoryDialog(payment, detail.asOf, historyEnabled, onDismiss = { selectedPaymentId = null },
                onCorrect = { selectedPaymentId = null; onCorrectRenewal(payment.id) },
                onDelete = { selectedPaymentId = null; onDeleteRenewal(payment.id) })
        }
    }
    if (managementOpen) state.detail?.takeUnless { state.missing }?.let { detail ->
        ModalBottomSheet(onDismissRequest = { managementOpen = false },
            containerColor = LocalLithicVisuals.current.sheetSurface) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.page), verticalAlignment = Alignment.CenterVertically) {
                    Text("订阅管理", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton({ managementOpen = false }, Modifier.size(48.dp).testTag("subscription-detail-close-management")) {
                        C2Icon(C2Glyph.Close, description = "关闭订阅管理")
                    }
                }
                onCostHistory?.let { action ->
                    TextButton({ managementOpen = false; action() }, Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.page)
                        .heightIn(min = 48.dp).testTag("subscription-price-history")) { Text("查看价格历史") }
                }
                    val enabled = !state.loading && state.error == null
                    Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.page),
                        verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                        Text("暂停、恢复或停止只改变砺石中的记录；取消服务商自动续费需到服务商处操作。",
                            Modifier.testTag("subscription-detail-vendor-explanation"),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        when (detail.cost.displayStatus) {
                            SubscriptionDisplayStatus.PAUSED -> TextButton({ managementOpen = false; onResume() }, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("subscription-detail-resume"), enabled) { Text("恢复") }
                            SubscriptionDisplayStatus.STOPPED -> TextButton({ managementOpen = false; onReopen() }, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("subscription-detail-reopen"), enabled) { Text("重新开通") }
                            else -> TextButton({ managementOpen = false; onPause() }, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("subscription-detail-pause"), enabled) { Text("暂停") }
                        }
                        val scheduled = detail.stateChanges.filter { it.state == SubscriptionOperationState.STOPPED && it.effectiveOn.isAfter(detail.asOf) }
                            .minByOrNull { it.effectiveOn }
                        if (detail.cost.displayStatus != SubscriptionDisplayStatus.STOPPED && scheduled == null) TextButton({ managementOpen = false; onStop() },
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("subscription-detail-stop"), enabled) { Text("停止记录") }
                        scheduled?.let { stop ->
                            Text("计划于 ${stop.effectiveOn} 停止。日期已固定，后续付款纠错不会自动调整。",
                                Modifier.testTag("subscription-detail-scheduled-stop"), style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton({ managementOpen = false; onCorrectState(stop.id) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("subscription-detail-correct-stop"), enabled) { Text("修正停止计划") }
                            OutlinedButton({ managementOpen = false; onCancelStop(stop.id) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("subscription-detail-cancel-stop"), enabled) { Text("取消尚未生效的停止计划") }
                        }
                    }
            }
        }
    }
}

@Composable
private fun SubscriptionIdentity(detail: SubscriptionDetail, coverContent: (@Composable () -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    val unavailable = detail.subscription.coverAssetId != null && coverContent == null
    val status: @Composable () -> Unit = {
        C2StatusChip(subscriptionStatusLabel(detail.cost.displayStatus), Modifier.testTag("subscription-detail-status"))
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stackStatus = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                detail.subscription.coverAssetId != null && coverContent != null -> Box(Modifier.size(56.dp)) { coverContent() }
                unavailable -> Surface(Modifier.size(56.dp), shape = MaterialTheme.shapes.small, color = colors.surfaceContainerHigh) {
                    Box(contentAlignment = Alignment.Center) { C2Icon(C2Glyph.ImageError, description = "封面暂不可用") }
                }
                else -> C2RecordIdentity(detail.subscription.name, detail.category.name, C2Glyph.Subscription, Modifier.size(56.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(detail.subscription.name, Modifier.testTag("subscription-detail-name").semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                val secondary = listOfNotNull(
                    detail.currentRenewal?.let { renewal -> detail.cost.currentPrice?.let { cyclePrice(renewal.cycle, it.cents) } },
                    detail.category.name,
                ).joinToString(" · ")
                Text(secondary, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
                    modifier = Modifier.testTag("subscription-detail-identity-facts"))
                if (unavailable) Text("封面暂不可用", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                if (stackStatus) status()
            }
            if (!stackStatus) status()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubscriptionArchiveMetrics(detail: SubscriptionDetail) {
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth().testTag("subscription-core-metrics"), shape = MaterialTheme.shapes.medium,
        color = LocalLithicVisuals.current.costSurface, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Column(Modifier.padding(start = 14.dp, end = 10.dp, bottom = 12.dp)) {
            FlowRow(itemVerticalAlignment = Alignment.CenterVertically) {
                Text("历史平均日成本", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                C5InformationButton("订阅历史指标", Modifier.testTag("subscription-history-metric-information"), iconOnly = true) {
                    Text("截至 ${detail.asOf}")
                    Text("按月30天、季90天、年365天折算；包含已确认付款的未来周期，不代表实际已使用时长。")
                    Text("历史平均日成本与当前套餐价格不同；实际已过去的付费有效天数按真实覆盖和状态计算。")
                }
            }
            MoneyMetric(detail.cost.historicalAverage, Modifier.fillMaxWidth().testTag("subscription-detail-historical-average"),
                level = MoneyMetricLevel.HERO, label = "历史平均日成本")
            if (detail.cost.historicalAverage == null) Text("暂无已确认付款。", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun nextPaidPlan(detail: SubscriptionDetail): SubscriptionRenewalEntity? = detail.renewals.asSequence()
    .filter { !it.paidOn.isAfter(detail.asOf) && it.effectiveFrom.isAfter(detail.asOf) }
    .minWithOrNull(compareBy(SubscriptionRenewalEntity::effectiveFrom, SubscriptionRenewalEntity::id))

private fun cyclePrice(cycle: BillingCycle, cents: Long): String =
    "${billingCycleLabel(cycle)} · ${moneyMetricDisplay(BigDecimal.valueOf(cents, 2), AmountUnit.YUAN)}"

private fun shortDate(date: LocalDate, asOf: LocalDate): String =
    if (date.year == asOf.year) "${date.monthValue}月${date.dayOfMonth}日" else date.toString()

@Composable
private fun SubscriptionPlanTile(title: String, subtitle: String, icon: C2Glyph, tag: String,
    subtitleDescription: String? = null, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onOpen, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(tag),
        shape = MaterialTheme.shapes.medium, color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant), shadowElevation = 1.dp) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            C2Icon(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, Modifier.semantics { subtitleDescription?.let { contentDescription = it } },
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            C2Icon(C2Glyph.Chevron, Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SubscriptionPlanDialog(detail: SubscriptionDetail, payment: SubscriptionRenewalEntity, current: Boolean,
    onClose: () -> Unit) {
    val currentPrice = detail.cost.currentPrice
    if (current && currentPrice == null) return
    AlertDialog(onDismissRequest = onClose, modifier = Modifier.testTag("subscription-detail-plan-dialog"),
        title = { Text(if (current) "当前套餐" else "下一期") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!current) Text("已付款，尚未生效。")
                Text(billingCycleLabel(payment.cycle))
                MoneyMetric(BigDecimal.valueOf(if (current) requireNotNull(currentPrice).cents else payment.amountCents, 2),
                    Modifier.testTag("subscription-detail-plan-price"), unit = AmountUnit.YUAN,
                    level = MoneyMetricLevel.CARD, label = if (current) "当前套餐价格" else "下一期实际付款")
                if (current) {
                    Text("当前标准日均价")
                    MoneyMetric(detail.cost.currentRate, Modifier.testTag("subscription-detail-current-rate"),
                        level = MoneyMetricLevel.ROW, label = "当前标准日均价")
                } else Text("真实付款日期：${payment.paidOn}")
                Text("实际生效日期：${payment.effectiveFrom}")
                Text("付费到期边界：${payment.paidUntilExclusive}（当天不包含）")
            }
        },
        confirmButton = {
            TextButton(onClose, Modifier.testTag("subscription-detail-plan-close")) { Text("关闭") }
        },
    )
}

@Composable
private fun SubscriptionOtherDetails(detail: SubscriptionDetail, enabled: Boolean, onCorrectState: (String) -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).testTag("subscription-detail-other-content"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("累计实际付款")
        MoneyMetric(BigDecimal.valueOf(detail.cost.confirmedPaidCents, 2),
            Modifier.testTag("subscription-detail-total-paid"), AmountUnit.YUAN,
            level = MoneyMetricLevel.ROW, label = "累计已确认实际付款")
        Text("折算付费时长：${detail.cost.standardPurchasedDays}天", Modifier.testTag("subscription-detail-purchased-days"))
        Text("已过去的付费有效天数：${detail.cost.elapsedPaidDays}天",
            Modifier.testTag("subscription-detail-elapsed-days"))
        detail.subscription.note?.takeIf { it.isNotBlank() }?.let { Text("备注：$it") }
        if (detail.stateChanges.isNotEmpty()) {
            Text("状态记录", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            detail.stateChanges.forEach { row ->
                val label = when (row.state) {
                    SubscriptionOperationState.ENABLED -> "已启用"
                    SubscriptionOperationState.PAUSED -> "已暂停"
                    SubscriptionOperationState.STOPPED -> "已停止"
                }
                Text("${row.effectiveOn} · $label", Modifier.testTag("subscription-state-row-${row.id}"))
                row.reason?.let { Text("原因：$it", style = MaterialTheme.typography.bodyMedium) }
                row.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                TextButton({ onCorrectState(row.id) }, Modifier.testTag("subscription-state-correct-${row.id}"),
                    enabled = enabled) { Text("修正状态记录") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenewalHistoryRow(renewal: SubscriptionRenewalEntity, asOf: LocalDate, first: Boolean, last: Boolean, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val future = !renewal.paidOn.isAfter(asOf) && renewal.effectiveFrom.isAfter(asOf)
    val radius = 14.dp
    val shape = RoundedCornerShape(if (first) radius else 0.dp, if (first) radius else 0.dp,
        if (last) radius else 0.dp, if (last) radius else 0.dp)
    Surface(Modifier.fillMaxWidth().testTag("subscription-renewal-${renewal.id}"), color = colors.surface, shape = shape) {
        Column {
            val amount = BigDecimal.valueOf(renewal.amountCents, 2)
            BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("subscription-renewal-actions-${renewal.id}")
                .clipToBounds()
                .drawBehind {
                    val line = 1.dp.toPx()
                    val edge = radius.toPx()
                    val top = if (first) 0f else edge
                    val bottom = if (last) 0f else edge
                    drawRoundRect(colors.outlineVariant, Offset(line / 2, line / 2 - top),
                        Size(size.width - line, size.height - line + top + bottom),
                        CornerRadius(edge), style = Stroke(line))
                }
                .clickable(role = Role.Button, onClickLabel = "查看付款详情", onClick = onOpen)
                .padding(horizontal = 10.dp, vertical = 5.dp)) {
                val amountWidth = ((maxWidth - 16.dp) * .43f).coerceAtLeast(0.dp)
                val stacked = maxWidth < 250.dp || LocalDensity.current.fontScale > 1.3f ||
                    !moneyMetricFitsAtPreferredSize(amount, amountWidth, AmountUnit.YUAN)
                val facts: @Composable () -> Unit = {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        itemVerticalAlignment = Alignment.CenterVertically) {
                        Text(shortDate(renewal.paidOn, asOf),
                            Modifier.testTag("subscription-renewal-paid-on-${renewal.id}").semantics {
                                contentDescription = "真实付款日期：${renewal.paidOn}"
                            }, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        Text("订阅付款", style = MaterialTheme.typography.bodySmall)
                        if (future) Text("尚未生效", Modifier.testTag("subscription-renewal-future-${renewal.id}"),
                            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                val paid: @Composable () -> Unit = {
                    MoneyMetric(amount, Modifier.testTag("subscription-renewal-amount-${renewal.id}"),
                        unit = AmountUnit.YUAN, level = MoneyMetricLevel.ROW, label = "真实付款金额")
                }
                if (stacked) Column { facts(); paid() }
                else Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { facts() }
                    Box(Modifier.width(amountWidth), contentAlignment = Alignment.CenterEnd) { paid() }
                }
            }
            if (!last) HorizontalDivider(Modifier.padding(horizontal = 10.dp), color = colors.outlineVariant)
        }
    }
}

@Composable
private fun RenewalHistoryDialog(renewal: SubscriptionRenewalEntity, asOf: LocalDate, enabled: Boolean,
    onDismiss: () -> Unit, onCorrect: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("subscription-renewal-facts"),
        title = { Text("付款详情") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("真实付款日期：${renewal.paidOn}", Modifier.testTag("subscription-renewal-facts-paid-on-${renewal.id}"))
                MoneyMetric(BigDecimal.valueOf(renewal.amountCents, 2), Modifier.testTag("subscription-renewal-facts-amount-${renewal.id}"),
                    unit = AmountUnit.YUAN,
                    level = MoneyMetricLevel.CARD, label = "真实付款金额")
                Text("${billingCycleLabel(renewal.cycle)} · 折算${renewal.standardDays}天")
                if (!renewal.paidOn.isAfter(asOf) && renewal.effectiveFrom.isAfter(asOf))
                    Text("预付周期尚未生效，不代表已经使用。",
                        Modifier.testTag("subscription-renewal-future-explanation-${renewal.id}"))
                Text("实际生效日期：${renewal.effectiveFrom}")
                Text("付费到期边界：${renewal.paidUntilExclusive}（当天不包含）")
                Text("提醒到期日：${renewal.reminderDueOn}")
                TextButton(onCorrect, Modifier.testTag("subscription-renewal-correct-${renewal.id}"), enabled = enabled) {
                    Text("修正这笔付款")
                }
                TextButton(onDelete, Modifier.testTag("subscription-renewal-delete-${renewal.id}"), enabled = enabled) {
                    Text("删除错误付款", color = if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onDismiss, Modifier.testTag("subscription-renewal-facts-close")) { Text("关闭") }
        },
    )
}
