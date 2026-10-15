package com.cooper.lithic.feature.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountText
import com.cooper.lithic.core.design.AmountUnit
import com.cooper.lithic.core.design.LithicCard
import com.cooper.lithic.core.design.LithicIcon
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.StatusBadge
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.feature.media.CoverDisplay
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.math.BigDecimal
import java.time.LocalDate

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
    mediaRepository: MediaRepository? = null,
    onEditCover: () -> Unit = {},
) {
    Column(modifier.fillMaxSize().testTag("subscription-detail-screen")) {
        LazyColumn(Modifier.fillMaxSize().testTag("subscription-detail-content"),
            contentPadding = PaddingValues(horizontal = LithicSpacing.medium, vertical = LithicSpacing.xSmall),
            verticalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onBack, Modifier.weight(1f).heightIn(min = 48.dp).testTag("subscription-detail-back")
                        .semantics { contentDescription = backLabel }) {
                        LithicIcon(LithicIcon.Back, null)
                        Text(backLabel, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, softWrap = true)
                    }
                    if (!state.missing && state.detail != null) {
                        TextButton(onEditCover, Modifier.widthIn(max = 136.dp).heightIn(min = 48.dp).testTag("subscription-cover-edit")
                            .semantics {
                                contentDescription = if (state.detail.subscription.coverAssetId == null) "添加封面" else "管理封面"
                            }, enabled = !state.loading && state.error == null) {
                            Text(if (state.detail.subscription.coverAssetId == null) "添加封面" else "管理封面",
                                style = MaterialTheme.typography.bodyMedium, softWrap = true)
                        }
                    }
                }
            }
            if (state.loading) item { Text("正在读取订阅", style = MaterialTheme.typography.bodyLarge) }
            state.error?.let { message -> item {
                Column {
                    Text(message, Modifier.testTag("subscription-detail-error"), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge)
                    if (state.detail != null) Text("以下为上次读取记录。", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-detail-retry")) { Text("重新读取") }
                }
            } }
            if (state.missing) item { Text("订阅不存在，返回列表查看当前记录。", Modifier
                .testTag("subscription-detail-missing"), style = MaterialTheme.typography.bodyLarge) }
            state.detail?.takeUnless { state.missing }?.let { detail ->
                if (detail.subscription.coverAssetId != null) item {
                    CoverDisplay(detail.subscription.coverAssetId, mediaRepository, "subscription", previewAspectRatio = 2.2f)
                }
                item {
                    LithicCard(Modifier.testTag("subscription-detail-cost-summary"), emphasis = true) {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            if (LocalDensity.current.fontScale >= 1.5f || maxWidth < 280.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                                    SubscriptionName(detail)
                                    StatusBadge(subscriptionStatusLabel(detail.cost.displayStatus),
                                        Modifier.testTag("subscription-detail-status").semantics(mergeDescendants = true) {},
                                        subdued = detail.cost.displayStatus == SubscriptionDisplayStatus.STOPPED)
                                }
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LithicSpacing.small),
                                    verticalAlignment = Alignment.Top) {
                                    Column(Modifier.weight(1f)) { SubscriptionName(detail) }
                                    StatusBadge(subscriptionStatusLabel(detail.cost.displayStatus),
                                        Modifier.testTag("subscription-detail-status").semantics(mergeDescendants = true) {},
                                        subdued = detail.cost.displayStatus == SubscriptionDisplayStatus.STOPPED)
                                }
                            }
                        }
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            if (LocalDensity.current.fontScale >= 1.5f || maxWidth < 300.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
                                    SubscriptionHistoricalAverage(detail)
                                    SubscriptionPurchasedDays(detail)
                                }
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LithicSpacing.medium),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) { SubscriptionHistoricalAverage(detail) }
                                    Column(Modifier.weight(1f)) { SubscriptionPurchasedDays(detail) }
                                }
                            }
                        }
                        Text("实际已过去的付费有效天数：${detail.cost.elapsedPaidDays}天",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("按月30天、季90天、年365天折算；包含已确认付款的未来周期，不代表实际已使用时长。",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("结果截至 ${detail.asOf}", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (detail.cost.displayStatus == SubscriptionDisplayStatus.PAUSED && detail.renewals.none {
                            !it.paidOn.isAfter(detail.asOf) && !it.effectiveFrom.isAfter(detail.asOf) && detail.asOf.isBefore(it.paidUntilExclusive)
                        }) Text("暂停状态保留；真实付费覆盖已到期，恢复时请核对已付款的未来覆盖或真实续费。",
                            Modifier.testTag("subscription-detail-paused-expired"), style = MaterialTheme.typography.bodyMedium)
                        if (detail.cost.historicalAverage == null) Text("暂无已确认付款，历史平均日成本暂无数据。",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item {
                    LithicCard(Modifier.testTag("subscription-detail-current-plan")) {
                        Text("当前套餐", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        val current = detail.currentRenewal
                        if (current != null && detail.cost.currentPrice != null) {
                            Text(billingCycleLabel(current.cycle), style = MaterialTheme.typography.bodyLarge)
                            AmountText(BigDecimal.valueOf(detail.cost.currentPrice.cents, 2), unit = AmountUnit.YUAN,
                                style = MaterialTheme.typography.titleLarge)
                            Text("当前标准日均价", style = MaterialTheme.typography.bodyMedium)
                            AmountText(detail.cost.currentRate, Modifier.testTag("subscription-detail-current-rate"),
                                style = MaterialTheme.typography.titleLarge)
                            Text("实际生效日期：${current.effectiveFrom}", style = MaterialTheme.typography.bodyMedium)
                            Text("付费到期边界：${current.paidUntilExclusive}（当天不包含）", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("当前无有效套餐", Modifier.testTag("subscription-detail-no-current-plan"),
                                style = MaterialTheme.typography.bodyLarge)
                            Text("当前成本贡献为0元/天", style = MaterialTheme.typography.bodyMedium)
                            if (detail.renewals.any { !it.paidOn.isAfter(detail.asOf) && it.effectiveFrom.isAfter(detail.asOf) })
                                Text("已付下一期尚未生效。", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (detail.cost.displayStatus != SubscriptionDisplayStatus.STOPPED) item {
                    Button(onRenewal, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("subscription-detail-renewal"),
                        enabled = !state.loading && state.error == null) {
                        Text("记录真实续费", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                item {
                    val enabled = !state.loading && state.error == null
                    LithicCard {
                        Text("记录管理", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        when (detail.cost.displayStatus) {
                            SubscriptionDisplayStatus.PAUSED -> OutlinedButton(onResume, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("subscription-detail-resume"), enabled) { Text("恢复") }
                            SubscriptionDisplayStatus.STOPPED -> OutlinedButton(onReopen, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("subscription-detail-reopen"), enabled) { Text("重新开通") }
                            else -> OutlinedButton(onPause, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("subscription-detail-pause"), enabled) { Text("暂停") }
                        }
                        val scheduled = detail.stateChanges.filter { it.state == SubscriptionOperationState.STOPPED && it.effectiveOn.isAfter(detail.asOf) }
                            .minByOrNull { it.effectiveOn }
                        if (detail.cost.displayStatus != SubscriptionDisplayStatus.STOPPED && scheduled == null) {
                            Text("停止本App记录不等于替用户向服务商取消订阅。", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onStop, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("subscription-detail-stop"), enabled) { Text("停止记录", style = MaterialTheme.typography.bodyLarge) }
                        }
                        scheduled?.let { stop ->
                            Text("计划于 ${stop.effectiveOn} 停止。日期已固定，后续付款纠错不会自动调整。",
                                Modifier.testTag("subscription-detail-scheduled-stop"), style = MaterialTheme.typography.bodyMedium)
                            OutlinedButton({ onCorrectState(stop.id) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("subscription-detail-correct-stop"), enabled) { Text("修正停止计划") }
                            OutlinedButton({ onCancelStop(stop.id) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("subscription-detail-cancel-stop"), enabled) { Text("取消尚未生效的停止计划") }
                        }
                    }
                }
                if (detail.stateChanges.isNotEmpty()) item {
                    LithicCard {
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
                    LithicCard {
                        Text("基本信息", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        Text("累计已确认实际付款", style = MaterialTheme.typography.bodyMedium)
                        AmountText(BigDecimal.valueOf(detail.cost.confirmedPaidCents, 2),
                            Modifier.testTag("subscription-detail-total-paid"), AmountUnit.YUAN,
                            style = MaterialTheme.typography.titleLarge)
                        Text("类别：${detail.category.name}", style = MaterialTheme.typography.bodyLarge)
                        detail.subscription.note?.takeIf { it.isNotBlank() }?.let {
                            Text("备注", style = MaterialTheme.typography.bodyMedium)
                            Text(it, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                        Text("真实付款历史", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        Text("按真实付款日期排序；付费到期边界当天不包含在前一期。", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (state.history.loading) item { Text("正在读取付款历史", style = MaterialTheme.typography.bodyLarge) }
                state.history.error?.let { message -> item {
                    Column {
                        Text(message, Modifier.testTag("subscription-detail-history-error"), color = MaterialTheme.colorScheme.error)
                        if (state.history.items.isNotEmpty()) Text("以下为上次读取的付款历史。", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("subscription-detail-history-retry")) { Text("重新读取付款历史") }
                    }
                } }
                if (!state.history.loading && state.history.error == null && state.history.totalCount == 0L && state.history.items.isEmpty()) item {
                    Text("暂无付费记录", style = MaterialTheme.typography.bodyLarge)
                }
                items(state.history.items, key = { it.id }) { renewal -> RenewalHistoryRow(renewal, detail.asOf,
                    !state.loading && state.error == null && state.history.error == null,
                    { onCorrectRenewal(renewal.id) }, { onDeleteRenewal(renewal.id) }) }
                if (state.history.hasMore) item {
                    Button(onLoadMoreHistory, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("subscription-detail-load-more-history"), enabled = !state.history.loading && !state.history.loadingMore) {
                        Text(if (state.history.loadingMore) "正在读取下一页" else "继续读取付款历史", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            item { Spacer(Modifier.height(LithicSpacing.medium)) }
        }
    }
}

@Composable
private fun RenewalHistoryRow(renewal: SubscriptionRenewalEntity, asOf: LocalDate, enabled: Boolean,
    onCorrect: () -> Unit, onDelete: () -> Unit) {
    LithicCard(Modifier.testTag("subscription-renewal-${renewal.id}")) {
        Text("真实付款日期：${renewal.paidOn}", style = MaterialTheme.typography.bodyLarge)
        AmountText(BigDecimal.valueOf(renewal.amountCents, 2), unit = AmountUnit.YUAN, style = MaterialTheme.typography.titleLarge)
        Text("${billingCycleLabel(renewal.cycle)} · 折算${renewal.standardDays}天", style = MaterialTheme.typography.bodyLarge)
        if (renewal.effectiveFrom.isAfter(asOf)) Text("尚未生效", Modifier.testTag("subscription-renewal-future-${renewal.id}"),
            style = MaterialTheme.typography.bodyLarge)
        Text("实际生效日期：${renewal.effectiveFrom}", style = MaterialTheme.typography.bodyMedium)
        Text("付费到期边界：${renewal.paidUntilExclusive}（当天不包含）", style = MaterialTheme.typography.bodyMedium)
        Text("提醒到期日：${renewal.reminderDueOn}", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onCorrect, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("subscription-renewal-correct-${renewal.id}"), enabled) {
            Text("修正这笔付款")
        }
        OutlinedButton(onDelete, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("subscription-renewal-delete-${renewal.id}"), enabled) {
            Text("删除错误付款")
        }
    }
}

@Composable
private fun SubscriptionName(detail: SubscriptionDetail) {
    if (detail.subscription.coverAssetId == null) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                LithicIcon(LithicIcon.Subscription, "尚未设置封面", Modifier.testTag("subscription-cover-empty"))
            }
        }
    }
    Text(detail.subscription.name, Modifier.testTag("subscription-detail-name").semantics { heading() },
        style = MaterialTheme.typography.headlineMedium, softWrap = true)
}

@Composable
private fun SubscriptionHistoricalAverage(detail: SubscriptionDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xxSmall)) {
        Text("历史平均日成本", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AmountText(detail.cost.historicalAverage, Modifier.testTag("subscription-detail-historical-average"))
    }
}

@Composable
private fun SubscriptionPurchasedDays(detail: SubscriptionDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xxSmall)) {
        Text("折算付费时长：${detail.cost.standardPurchasedDays}天", Modifier.testTag("subscription-detail-purchased-days"),
            style = MaterialTheme.typography.bodyLarge)
    }
}
