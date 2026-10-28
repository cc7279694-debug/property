package com.cooper.lithic.feature.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.cooper.lithic.core.design.*
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
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
    var managementOpen by rememberSaveable { mutableStateOf(false) }
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
            verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
            if (state.loading) item { Text("正在读取订阅", Modifier.padding(LithicSpacing.medium)) }
            state.error?.let { message -> item {
                Column(Modifier.padding(horizontal = LithicSpacing.medium)) {
                    Text(message, Modifier.testTag("subscription-detail-error"), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge)
                    if (state.detail != null) Text("以下为上次读取记录。", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-detail-retry")) { Text("重新读取") }
                }
            } }
            if (state.missing) item { Text("订阅不存在，返回列表查看当前记录。", Modifier.padding(LithicSpacing.medium)
                .testTag("subscription-detail-missing"), style = MaterialTheme.typography.bodyLarge) }
            state.detail?.takeUnless { state.missing }?.let { detail ->
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (detail.subscription.coverAssetId != null && coverContent != null) {
                                Box(Modifier.size(64.dp)) { coverContent() }
                            } else if (detail.subscription.coverAssetId == null) C2RecordIdentity(detail.subscription.name, detail.category.name, C2Glyph.Subscription)
                            else Surface(Modifier.size(64.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                Box(contentAlignment = Alignment.Center) { C2Icon(C2Glyph.ImageError, description = "封面暂不可用") }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(detail.subscription.name, Modifier.testTag("subscription-detail-name").semantics { heading() },
                                    style = MaterialTheme.typography.headlineMedium)
                                Text(detail.category.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                C2StatusChip(subscriptionStatusLabel(detail.cost.displayStatus), Modifier.testTag("subscription-detail-status"))
                                if (detail.subscription.coverAssetId != null && coverContent == null) Text("封面暂不可用", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (detail.cost.displayStatus == SubscriptionDisplayStatus.PAUSED && detail.renewals.none {
                            !it.paidOn.isAfter(detail.asOf) && !it.effectiveFrom.isAfter(detail.asOf) && detail.asOf.isBefore(it.paidUntilExclusive)
                        }) Text("暂停状态保留；真实付费覆盖已到期，恢复时请核对已付款的未来覆盖或真实续费。",
                            Modifier.testTag("subscription-detail-paused-expired"), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item {
                    C2Section("当前套餐", Modifier.padding(horizontal = 16.dp).testTag("subscription-detail-current-plan")) {
                        val current = detail.currentRenewal
                        if (current != null && detail.cost.currentPrice != null) {
                            Text(billingCycleLabel(current.cycle), style = MaterialTheme.typography.bodyLarge)
                            MoneyMetric(BigDecimal.valueOf(detail.cost.currentPrice.cents, 2), unit = AmountUnit.YUAN,
                                level = MoneyMetricLevel.DETAIL, label = "当前套餐价格（${billingCycleLabel(current.cycle)}）")
                            Text("当前标准日均价", style = MaterialTheme.typography.bodyMedium)
                            MoneyMetric(detail.cost.currentRate, Modifier.testTag("subscription-detail-current-rate"),
                                level = MoneyMetricLevel.ROW, label = "当前标准日均价")
                            Text("实际生效日期：${current.effectiveFrom}", style = MaterialTheme.typography.bodyMedium)
                            Text("付费到期边界：${current.paidUntilExclusive}（当天不包含）", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("当前无有效套餐", Modifier.testTag("subscription-detail-no-current-plan"),
                                style = MaterialTheme.typography.bodyLarge)
                            Text("当前日均贡献", style = MaterialTheme.typography.bodyMedium)
                            MoneyMetric(detail.cost.currentRate, Modifier.testTag("subscription-detail-no-plan-rate"),
                                level = MoneyMetricLevel.ROW, label = "当前日均贡献")
                        }
                        if (detail.renewals.any { !it.paidOn.isAfter(detail.asOf) && it.effectiveFrom.isAfter(detail.asOf) })
                            Text("已付下一期尚未生效。", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item { Column(Modifier.padding(horizontal = 16.dp)) { SubscriptionArchiveMetrics(detail) } }
                if (detail.stateChanges.isNotEmpty()) item {
                    Column(Modifier.padding(horizontal = LithicSpacing.medium), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("状态事实", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        detail.stateChanges.forEach { row ->
                            Text("${row.effectiveOn} · ${when (row.state) { SubscriptionOperationState.ENABLED -> "已启用"; SubscriptionOperationState.PAUSED -> "已暂停"; SubscriptionOperationState.STOPPED -> "已停止" }}",
                                Modifier.testTag("subscription-state-row-${row.id}"))
                            row.reason?.let { Text("原因：$it", style = MaterialTheme.typography.bodyMedium) }
                            row.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            TextButton({ onCorrectState(row.id) }, Modifier.heightIn(min = 48.dp).testTag("subscription-state-correct-${row.id}"),
                                enabled = !state.loading && state.error == null) { Text("修正这条状态事实") }
                        }
                    }
                }
                item {
                    C2Section("付款与备注", Modifier.padding(horizontal = 16.dp)) {
                        Text("累计已确认实际付款", style = MaterialTheme.typography.bodyMedium)
                        MoneyMetric(BigDecimal.valueOf(detail.cost.confirmedPaidCents, 2),
                            Modifier.testTag("subscription-detail-total-paid"), AmountUnit.YUAN,
                            level = MoneyMetricLevel.CARD, label = "累计已确认实际付款")
                        Text("类别：${detail.category.name}", style = MaterialTheme.typography.bodyLarge)
                        detail.subscription.note?.takeIf { it.isNotBlank() }?.let {
                            Text("备注", style = MaterialTheme.typography.bodyMedium)
                            Text(it, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text("实际已过去的付费有效天数：${detail.cost.elapsedPaidDays}天",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = LithicSpacing.medium), verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                        HorizontalDivider()
                        Text("真实付款历史", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        C5InformationButton("付款历史说明") { Text("按真实付款日期排序；付费到期边界当天不包含在前一期。") }
                    }
                }
                if (state.history.loading) item { Text("正在读取付款历史", Modifier.padding(LithicSpacing.medium)) }
                state.history.error?.let { message -> item {
                    Column(Modifier.padding(horizontal = LithicSpacing.medium)) {
                        Text(message, Modifier.testTag("subscription-detail-history-error"), color = MaterialTheme.colorScheme.error)
                        if (state.history.items.isNotEmpty()) Text("以下为上次读取的付款历史。", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-detail-history-retry")) { Text("重新读取付款历史") }
                    }
                } }
                if (!state.history.loading && state.history.error == null && state.history.totalCount == 0L && state.history.items.isEmpty()) item {
                    Text("暂无付费记录", Modifier.padding(LithicSpacing.medium), style = MaterialTheme.typography.bodyLarge)
                }
                items(state.history.items, key = { it.id }) { renewal -> RenewalHistoryRow(renewal, detail.asOf,
                    !state.loading && state.error == null && state.history.error == null,
                    { onCorrectRenewal(renewal.id) }, { onDeleteRenewal(renewal.id) }) }
                if (state.history.hasMore) item {
                    OutlinedButton(onLoadMoreHistory, Modifier.fillMaxWidth().padding(LithicSpacing.medium).heightIn(min = 48.dp)
                        .testTag("subscription-detail-load-more-history"), enabled = !state.history.loading && !state.history.loadingMore) {
                        Text(if (state.history.loadingMore) "正在读取下一页" else "继续读取付款历史", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        state.detail?.takeUnless { state.missing }?.let { detail ->
            if (detail.cost.displayStatus != SubscriptionDisplayStatus.STOPPED) Surface(shadowElevation = 1.dp) {
                Button(onRenewal, Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 48.dp)
                    .testTag("subscription-detail-renewal"), enabled = !state.loading && state.error == null) {
                    Text("记录真实续费", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
    if (managementOpen) state.detail?.takeUnless { state.missing }?.let { detail ->
        ModalBottomSheet(onDismissRequest = { managementOpen = false }) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("订阅管理", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    IconButton({ managementOpen = false }, Modifier.size(48.dp).testTag("subscription-detail-close-management")) {
                        C2Icon(C2Glyph.Close, description = "关闭订阅管理")
                    }
                }
                onCostHistory?.let { action ->
                    TextButton({ managementOpen = false; action() }, Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .heightIn(min = 48.dp).testTag("subscription-price-history")) { Text("查看价格历史") }
                }
                    val enabled = !state.loading && state.error == null
                    Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
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
private fun SubscriptionArchiveMetrics(detail: SubscriptionDetail) {
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shadowElevation = 1.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val amountWidth = ((maxWidth - 12.dp) / 2).coerceAtLeast(0.dp)
                val amountFits = moneyMetricFitsAtPreferredSize(detail.cost.historicalAverage, amountWidth,
                    level = MoneyMetricLevel.CARD)
                val metrics: @Composable (Modifier) -> Unit = { column ->
                    Column(column, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("历史平均日成本", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MoneyMetric(detail.cost.historicalAverage, Modifier.testTag("subscription-detail-historical-average"),
                            level = MoneyMetricLevel.CARD, color = MaterialTheme.colorScheme.primary, label = "历史平均日成本")
                    }
                }
                val days: @Composable (Modifier) -> Unit = { column ->
                    Column(column, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val labelStyle = MaterialTheme.typography.bodyMedium.toSpanStyle()
                        val valueStyle = MaterialTheme.typography.headlineMedium.toSpanStyle()
                        Text(buildAnnotatedString {
                            withStyle(labelStyle) { append("折算付费时长：") }
                            withStyle(valueStyle) { append("${detail.cost.standardPurchasedDays}天") }
                        }, Modifier.testTag("subscription-detail-purchased-days"))
                    }
                }
                if (LocalDensity.current.fontScale > 1.3f || maxWidth < 280.dp || !amountFits) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    metrics(Modifier.fillMaxWidth()); days(Modifier.fillMaxWidth())
                } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    metrics(Modifier.weight(1f)); days(Modifier.weight(1f))
                }
            }
            Text("结果截至 ${detail.asOf}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            C5InformationButton("订阅历史指标", Modifier.testTag("subscription-history-metric-information")) {
                Text("按月30天、季90天、年365天折算；包含已确认付款的未来周期，不代表实际已使用时长。")
                Text("历史平均日成本与当前套餐价格不同；实际已过去的付费有效天数按真实覆盖和状态计算。")
            }
            if (detail.cost.historicalAverage == null) Text("暂无已确认付款，历史平均日成本暂无数据。", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RenewalHistoryRow(renewal: SubscriptionRenewalEntity, asOf: LocalDate, enabled: Boolean,
    onCorrect: () -> Unit, onDelete: () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("subscription-renewal-${renewal.id}"),
        color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("真实付款日期：${renewal.paidOn}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyMetric(BigDecimal.valueOf(renewal.amountCents, 2), unit = AmountUnit.YUAN,
            level = MoneyMetricLevel.ROW, label = "真实付款金额")
        Text("${billingCycleLabel(renewal.cycle)} · 折算${renewal.standardDays}天", style = MaterialTheme.typography.bodyLarge)
        if (renewal.effectiveFrom.isAfter(asOf)) C2StatusChip("尚未生效", Modifier.testTag("subscription-renewal-future-${renewal.id}"))
        Text("生效 ${renewal.effectiveFrom} · 到期 ${renewal.paidUntilExclusive}（不含当日）",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        C5InformationButton("付款及覆盖日期", Modifier.testTag("subscription-renewal-date-information-${renewal.id}")) {
            if (renewal.effectiveFrom.isAfter(asOf)) Text("这笔已确认付款用于未来周期，不代表已经使用该段时间。当前套餐以实际生效日期为准。",
                Modifier.testTag("subscription-renewal-future-explanation-${renewal.id}"))
            Text("实际生效日期：${renewal.effectiveFrom}")
            Text("付费到期边界：${renewal.paidUntilExclusive}（当天不包含）")
            Text("提醒到期日：${renewal.reminderDueOn}")
        }
        var paymentActionsOpen by rememberSaveable(renewal.id) { mutableStateOf(false) }
        Box {
            TextButton({ paymentActionsOpen = true }, Modifier.heightIn(min = 48.dp).testTag("subscription-renewal-actions-${renewal.id}"), enabled) {
                C2Icon(C2Glyph.More); Text("付款操作")
            }
            DropdownMenu(paymentActionsOpen, { paymentActionsOpen = false }) {
                DropdownMenuItem(text = { Text("修正这笔付款") }, onClick = { paymentActionsOpen = false; onCorrect() },
                    modifier = Modifier.testTag("subscription-renewal-correct-${renewal.id}"), enabled = enabled)
                DropdownMenuItem(text = { Text("删除错误付款", color = MaterialTheme.colorScheme.error) },
                    onClick = { paymentActionsOpen = false; onDelete() },
                    modifier = Modifier.testTag("subscription-renewal-delete-${renewal.id}"), enabled = enabled)
            }
        }
        HorizontalDivider()
    }
    }
}
