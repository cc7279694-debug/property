package com.cooper.lithic.feature.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountUnit
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.C2PageHeader
import com.cooper.lithic.core.design.C5InformationButton
import com.cooper.lithic.core.design.LithicButton
import com.cooper.lithic.core.design.LithicIconButton
import com.cooper.lithic.core.design.LithicOutlinedButton
import com.cooper.lithic.core.design.LithicTextButton
import com.cooper.lithic.core.design.MoneyMetric
import com.cooper.lithic.core.design.MoneyMetricLevel
import com.cooper.lithic.core.design.lithicInputColors
import com.cooper.lithic.core.design.moneyMetricFitsAtPreferredSize
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.ReportAmounts
import java.math.BigDecimal

@Composable
internal fun AnnualReviewScreen(
    state: ReportAnalysisUiState,
    onSelectYear: (Int) -> Unit,
    onRetry: () -> Unit,
    onOpenRecord: (OverviewKind, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rawYear by remember(state.selectedYear) { mutableStateOf(state.selectedYear?.toString().orEmpty()) }
    var inputError by remember(state.selectedYear) { mutableStateOf(false) }
    var recordLimit by remember(state.snapshot, state.selectedYear) { mutableIntStateOf(50) }
    var yearLimit by remember(state.snapshot) { mutableIntStateOf(50) }
    val focusManager = LocalFocusManager.current
    val annual = state.annual
    val latestYear = state.asOf?.year
    val trend = state.annualTrend
    val scale = remember(trend) { reportScale(trend.map { it.amounts }) }

    fun applyYear() {
        val year = rawYear.trim().toIntOrNull()
        if (year == null || latestYear == null || year !in 1..latestYear) {
            inputError = true
        } else {
            inputError = false
            focusManager.clearFocus()
            onSelectYear(year)
        }
    }

    Box(modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 560.dp).fillMaxSize().testTag("annual-review-screen"),
            contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
        ) {
            item("heading") {
                C2PageHeader("年度回顾", onBack, "annual-review-back", backLabel = "返回回看") {
                    C5InformationButton("年度回顾口径", Modifier.testTag("annual-information"), iconOnly = true) {
                        Text("按当前分类汇总已录入和更正的年度实际金额，与日均成本分开。")
                        Text("按实际发生日或订阅付款日归年。可查无记录年份，保留真实 0 元。")
                        Text("项目投入、收回、净投入与订阅付款分列，无年度总支出。含非现金折抵的金额并非纯现金流水。")
                        Text("条形图左负右正，所有年份共用金额尺度；缺失年份不填0。")
                    }
                }
            }
            if (state.loading || state.error != null || state.notice != null) item("read-status") {
                Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportReadStatus(state, onRetry, "annual-review")
                }
            }
            item("year-selector") {
                AnnualYearSelector(state.selectedYear, latestYear, rawYear, inputError,
                    onEditYear = {
                        rawYear = it
                        inputError = false
                    },
                    onApply = ::applyYear, onSelectYear = onSelectYear)
            }
            if (annual != null) {
                item("summary") {
                    Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (annual.amounts.hasRecords) AnnualKeyAmounts(annual.amounts)
                        else Text("该年暂无记录，支出未知。", Modifier.testTag("annual-empty-year"),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        state.asOf?.let {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text("结果截至 $it", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (annual.amounts.hasRecords) item("project-amounts") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnnualSectionTitle("项目实际金额")
                        AnnualAmountGroup {
                            AnnualMoneyRow("投入", annual.amounts.projectOutlayCents, "annual-outlay", "长期项目投入")
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            AnnualMoneyRow("收回", annual.amounts.projectRecoveryCents, "annual-recovery", "长期项目收回")
                        }
                        if (annual.amounts.containsNonCash) AnnualNonCash("annual-noncash")
                    }
                }
                val categories = annual.categories.filter { it.amounts.hasRecords }
                if (categories.isNotEmpty()) {
                    item("category-heading") { AnnualSectionTitle("年度类别构成（当前分类）") }
                    for (kind in OverviewKind.entries) {
                        val group = categories.filter { it.category.kind == kind }
                        if (group.isEmpty()) continue
                        item("kind-$kind") {
                            Text(if (kind == OverviewKind.PROJECT) "长期项目" else "订阅实际付款",
                                Modifier.padding(top = 8.dp, bottom = 4.dp).semantics { heading() },
                                style = MaterialTheme.typography.titleSmall)
                        }
                        itemsIndexed(group, key = { _, entry -> "category-${entry.category.id}" }) { index, entry ->
                            val tag = "annual-category-${entry.category.id}"
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column(Modifier.fillMaxWidth().testTag(tag).padding(vertical = 8.dp)) {
                                Text(entry.category.name, style = MaterialTheme.typography.titleSmall)
                                if (kind == OverviewKind.PROJECT) {
                                    AnnualMoneyRow("投入", entry.amounts.projectOutlayCents, "$tag-outlay", "长期项目投入")
                                    AnnualMoneyRow("收回", entry.amounts.projectRecoveryCents, "$tag-recovery", "长期项目收回")
                                    AnnualMoneyRow("净投入", entry.amounts.projectNetCents, "$tag-net", "长期项目净投入")
                                    if (entry.amounts.containsNonCash) AnnualNonCash("$tag-noncash")
                                } else {
                                    AnnualMoneyRow("订阅实际付款", entry.amounts.subscriptionPaidCents, "$tag-paid")
                                }
                            }
                        }
                    }
                }
            }
            if (state.snapshot != null) {
                item("years-heading") { AnnualSectionTitle("年度真实金额趋势") }
                items(trend.take(yearLimit), key = { "trend-${it.year}" }) { report ->
                    Column(Modifier.fillMaxWidth().testTag("annual-trend-${report.year}").padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (report.amounts.hasRecords) {
                            ReportAnnualBars(report.year, report.amounts, scale)
                        } else {
                            Text("${report.year} 年", style = MaterialTheme.typography.titleMedium)
                            Text("该年暂无记录，支出未知。", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LithicTextButton({ onSelectYear(report.year) }, Modifier.testTag("annual-trend-select-${report.year}")) {
                            Text("查看该年明细")
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                if (trend.size > yearLimit) item("years-more") {
                    LithicOutlinedButton({ yearLimit += 50 }, Modifier.padding(top = 12.dp).testTag("annual-years-more")) {
                        Text("继续查看50个年度")
                    }
                }
            }
            if (annual != null) {
                item("records-heading") { AnnualSectionTitle("该年度相关记录（${annual.records.size}条）") }
                items(annual.records.take(recordLimit), key = { "record-${it.kind}-${it.id}" }) { record ->
                    ReportRecordCard(record, "annual", onOpenRecord)
                }
                if (annual.records.size > recordLimit) item("records-more") {
                    LithicOutlinedButton({ recordLimit += 50 },
                        Modifier.fillMaxWidth().padding(top = 12.dp).testTag("annual-records-more")) {
                        Text("继续查看50条记录")
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnualYearSelector(
    selectedYear: Int?,
    latestYear: Int?,
    rawYear: String,
    inputError: Boolean,
    onEditYear: (String) -> Unit,
    onApply: () -> Unit,
    onSelectYear: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        selectedYear?.let { year ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AnnualYearStep("上一年度", "annual-year-previous", isPrevious = true, enabled = year > 1) {
                    onSelectYear(year - 1)
                }
                Text("$year 年", Modifier.weight(1f).testTag("annual-selected-year"),
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
                AnnualYearStep("下一年度", "annual-year-next", isPrevious = false, enabled = year < (latestYear ?: year)) {
                    onSelectYear(year + 1)
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val field: @Composable (Modifier) -> Unit = { fieldModifier ->
                OutlinedTextField(
                    value = rawYear,
                    onValueChange = onEditYear,
                    modifier = fieldModifier.heightIn(min = 52.dp).testTag("annual-year-input"),
                    label = { Text("查看年度") },
                    singleLine = true,
                    isError = inputError,
                    shape = RoundedCornerShape(14.dp),
                    colors = lithicInputColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (latestYear != null) onApply() }),
                )
            }
            val apply: @Composable (Modifier) -> Unit = { buttonModifier ->
                LithicButton(onApply, buttonModifier.testTag("annual-year-apply"), enabled = latestYear != null) {
                    Text("查看")
                }
            }
            if (maxWidth < 280.dp || LocalDensity.current.fontScale > 1.3f) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    field(Modifier.fillMaxWidth())
                    apply(Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    field(Modifier.weight(1f))
                    apply(Modifier)
                }
            }
        }
        if (inputError) Text("请输入截至业务今天的有效年度。", Modifier.testTag("annual-year-error"),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun AnnualYearStep(label: String, tag: String, isPrevious: Boolean, enabled: Boolean, onClick: () -> Unit) {
    LithicIconButton(onClick, Modifier.size(48.dp).testTag(tag).semantics { contentDescription = label }, enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f))) {
        C2Icon(C2Glyph.Chevron, if (isPrevious) Modifier.rotate(180f) else Modifier)
    }
}

@Composable
private fun AnnualKeyAmounts(amounts: ReportAmounts) {
    val projectNet = BigDecimal.valueOf(amounts.projectNetCents, 2)
    val subscriptionPaid = BigDecimal.valueOf(amounts.subscriptionPaidCents, 2)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columnWidth = (maxWidth - 12.dp).coerceAtLeast(0.dp) / 2
        val stack = LocalDensity.current.fontScale > 1.3f ||
            !moneyMetricFitsAtPreferredSize(projectNet, columnWidth, AmountUnit.YUAN, MoneyMetricLevel.DETAIL) ||
            !moneyMetricFitsAtPreferredSize(subscriptionPaid, columnWidth, AmountUnit.YUAN, MoneyMetricLevel.DETAIL)
        if (stack) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AnnualKeyAmount("项目净投入", projectNet, "annual-net", Modifier.fillMaxWidth())
                AnnualKeyAmount("订阅实际付款", subscriptionPaid, "annual-paid", Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnnualKeyAmount("项目净投入", projectNet, "annual-net", Modifier.weight(1f))
                AnnualKeyAmount("订阅实际付款", subscriptionPaid, "annual-paid", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AnnualKeyAmount(label: String, amount: BigDecimal, tag: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyMetric(amount, Modifier.fillMaxWidth().testTag(tag), unit = AmountUnit.YUAN, level = MoneyMetricLevel.DETAIL,
            label = label, keepPreferredSize = true)
    }
}

@Composable
private fun AnnualMoneyRow(label: String, cents: Long, tag: String, metricLabel: String = label) {
    val amount = BigDecimal.valueOf(cents, 2)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        val amountWidth = (maxWidth - 12.dp).coerceAtLeast(0.dp) / 2
        val stack = maxWidth < 232.dp || LocalDensity.current.fontScale > 1.3f ||
            !moneyMetricFitsAtPreferredSize(amount, amountWidth, AmountUnit.YUAN, MoneyMetricLevel.ROW)
        if (stack) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                MoneyMetric(amount, Modifier.fillMaxWidth().testTag(tag), unit = AmountUnit.YUAN, label = metricLabel)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    MoneyMetric(amount, Modifier.testTag(tag), unit = AmountUnit.YUAN, label = metricLabel)
                }
            }
        }
    }
}

@Composable
private fun AnnualSectionTitle(title: String) {
    Text(title, Modifier.padding(top = 20.dp).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun AnnualAmountGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), content = content)
    }
}

@Composable
private fun AnnualNonCash(tag: String) {
    Text("含非现金折抵；不是纯现金流水。", Modifier.testTag(tag), style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}
