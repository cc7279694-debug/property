package com.cooper.lithic.feature.review

import com.cooper.lithic.core.design.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.cooper.lithic.core.design.LithicOutlinedButton as OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.domain.report.*
import java.math.BigDecimal
import java.math.MathContext

@Composable
internal fun ReportReadStatus(state: ReportAnalysisUiState, retry: () -> Unit, tag: String) {
    if (state.loading) Text(if (state.snapshot == null) "正在读取完整账本。" else "正在刷新；下方为上次截止日和选择的结果。")
    state.error?.let {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().testTag("$tag-error")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (state.snapshot == null) "读取失败，不能把错误作为空账本。" else "读取失败；下方保留上次结果，尚未刷新。")
                Text(it)
                OutlinedButton(retry, Modifier.heightIn(min = 48.dp).testTag("$tag-retry")) { Text("重试") }
            }
        }
    }
    state.notice?.let { Text(it, Modifier.testTag("$tag-notice")) }
}

@Composable
internal fun ReportMoneyValues(amounts: ReportAmounts, prefix: String, kind: OverviewKind? = null) {
    if (kind != OverviewKind.SUBSCRIPTION) {
        ReportMoney("长期项目投入", amounts.projectOutlayCents, "$prefix-outlay")
        ReportMoney("长期项目收回", amounts.projectRecoveryCents, "$prefix-recovery")
        ReportMoney("长期项目净投入", amounts.projectNetCents, "$prefix-net")
        if (amounts.containsNonCash) Text("含非现金折抵；不是纯现金流水。", Modifier.testTag("$prefix-noncash"))
    }
    if (kind != OverviewKind.PROJECT) ReportMoney("订阅实际付款", amounts.subscriptionPaidCents, "$prefix-paid")
}

@Composable
private fun ReportMoney(label: String, cents: Long, tag: String) {
    ReportMoneyLine(label, cents, MoneyMetricLevel.CARD, Modifier.testTag(tag))
}

@Composable
private fun ReportMoneyLine(label: String, cents: Long, level: MoneyMetricLevel, modifier: Modifier = Modifier) {
    val fontScale = LocalDensity.current.fontScale
    val amount = BigDecimal.valueOf(cents, 2)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val rowAmountWidth = ((maxWidth - 12.dp).coerceAtLeast(0.dp) * 1.2f) / 2.2f
        val amountFitsRow = moneyMetricFitsAtPreferredSize(amount, rowAmountWidth, AmountUnit.YUAN, level)
        if (maxWidth < 320.dp || fontScale > 1.3f || !amountFitsRow) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                MoneyMetric(amount, modifier.fillMaxWidth(), unit = AmountUnit.YUAN, level = level, label = label)
            }
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            MoneyMetric(amount, modifier.weight(1.2f), unit = AmountUnit.YUAN, level = level, label = label)
        }
    }
}

internal fun reportScale(values: List<ReportAmounts>, kind: OverviewKind? = null): BigDecimal = values
    .flatMap { reportValues(it, kind).map { entry -> BigDecimal.valueOf(entry.second).abs() } }
    .maxOrNull()?.max(BigDecimal.ONE) ?: BigDecimal.ONE

private fun reportValues(value: ReportAmounts, kind: OverviewKind?): List<Pair<String, Long>> = buildList {
    if (kind != OverviewKind.SUBSCRIPTION) {
        add("长期项目投入" to value.projectOutlayCents)
        add("长期项目收回" to value.projectRecoveryCents)
        add("长期项目净投入" to value.projectNetCents)
    }
    if (kind != OverviewKind.PROJECT) add("订阅实际付款" to value.subscriptionPaidCents)
}

/** Display geometry only: cents are never summed or converted through floating point.
 * All years in a section use the same absolute range; zero is the centre of each bar. */
@Composable
internal fun ReportAnnualBars(year: Int, amounts: ReportAmounts, scale: BigDecimal, kind: OverviewKind? = null) {
    val colour = LocalLithicVisuals.current.chartTotal
    val axis = LocalLithicVisuals.current.chartGrid
    Text("$year 年", style = MaterialTheme.typography.titleMedium)
    reportValues(amounts, kind).forEach { (label, cents) ->
        ReportMoneyLine(label, cents, MoneyMetricLevel.ROW)
        val ratio = BigDecimal.valueOf(cents).abs().divide(scale, MathContext.DECIMAL128).coerceIn(BigDecimal.ZERO, BigDecimal.ONE).toFloat()
        Canvas(Modifier.fillMaxWidth().height(20.dp).semantics {
            contentDescription = "$year 年，${moneyMetricDescription(BigDecimal.valueOf(cents, 2), AmountUnit.YUAN, label)}。左负右正，所有年份共用金额尺度。"
        }) {
            val centre = size.width / 2f
            drawLine(axis, Offset(centre, 0f), Offset(centre, size.height), 1.dp.toPx())
            val width = centre * ratio
            if (cents != 0L) drawRect(colour, Offset(if (cents < 0) centre - width else centre, 4.dp.toPx()), Size(width, size.height - 8.dp.toPx()))
        }
    }
    if (!amounts.hasRecords) Text("未录入相关记录；不能据此断言该年没有花钱。")
    if (amounts.containsNonCash) Text("含非现金折抵")
}

internal fun ReportRecord.statusLabel(): String = when (status) {
    ReportRecordStatus.NOT_STARTED -> "尚未开始"
    ReportRecordStatus.CURRENT -> "当前持有"
    ReportRecordStatus.ENDED -> "已结束"
    ReportRecordStatus.ACTIVE -> "订阅中"
    ReportRecordStatus.PAUSED -> "暂停"
    ReportRecordStatus.EXPIRED -> "到期"
    ReportRecordStatus.STOPPED -> "停止"
}

@Composable
internal fun ReportRecordCard(record: ReportRecord, prefix: String, open: (OverviewKind, String) -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("$prefix-record-${record.id}")) {
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { open(record.kind, record.id) }
            .padding(vertical = 12.dp).heightIn(min = 64.dp).testTag("$prefix-open-${record.id}"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            C2RecordIdentity(record.name, "", if (record.kind == OverviewKind.PROJECT) C2Glyph.Project else C2Glyph.Subscription)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(record.name, style = MaterialTheme.typography.titleMedium)
                C2StatusChip("${if (record.kind == OverviewKind.PROJECT) "长期项目" else "订阅"} · ${record.statusLabel()}")
                Text("查看记录详情", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            C2Icon(C2Glyph.Chevron)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
