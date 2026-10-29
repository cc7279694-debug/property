package com.cooper.lithic.feature.review

import com.cooper.lithic.core.design.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import com.cooper.lithic.core.design.LithicOutlinedButton as OutlinedButton
import com.cooper.lithic.core.design.LithicButton as Button
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cooper.lithic.domain.report.*

@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun AnnualReviewScreen(
    state: ReportAnalysisUiState,
    onSelectYear: (Int) -> Unit,
    onRetry: () -> Unit,
    onOpenRecord: (OverviewKind, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rawYear by remember(state.selectedYear) { mutableStateOf(state.selectedYear?.toString().orEmpty()) }
    var inputError by remember { mutableStateOf<String?>(null) }
    var recordLimit by remember(state.snapshot, state.selectedYear) { mutableIntStateOf(50) }
    var yearLimit by remember(state.snapshot) { mutableIntStateOf(50) }
    val annual = state.annual
    LazyColumn(modifier.fillMaxSize().imePadding().testTag("annual-review-screen"),
        contentPadding = PaddingValues(horizontal = LithicSpacing.page, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item("heading") { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            C2PageHeader("年度回顾", onBack, "annual-review-back", backLabel = "返回回看")
            state.asOf?.let { Text("结果截至 $it") }
        } }
        item("read-status") { ReportReadStatus(state, onRetry, "annual-review") }
        item("year-selector") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(rawYear, { rawYear = it; inputError = null },
                        Modifier.weight(1f).testTag("annual-year-input"), label = { Text("查看年度") }, singleLine = true,
                        colors = lithicInputColors(), isError = inputError != null)
                    Button({
                        val year = rawYear.trim().toIntOrNull()
                        val ceiling = state.asOf?.year
                        if (year == null || ceiling == null || year !in 1..ceiling) inputError = "请输入截至业务今天的有效年度。"
                        else { inputError = null; onSelectYear(year) }
                    }, Modifier.heightIn(min = 48.dp).testTag("annual-year-apply"), enabled = state.asOf != null) { Text("查看") }
                }
                inputError?.let { Text(it, Modifier.testTag("annual-year-error"), color = MaterialTheme.colorScheme.error) }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.selectedYear?.let { year ->
                        TextButton({ onSelectYear(year - 1) }, Modifier.heightIn(min = 48.dp).testTag("annual-year-previous"), enabled = year > 1) { Text("上一年度") }
                        TextButton({ onSelectYear(year + 1) }, Modifier.heightIn(min = 48.dp).testTag("annual-year-next"), enabled = year < (state.asOf?.year ?: year)) { Text("下一年度") }
                    }
                    C5InformationButton("年度回顾口径", Modifier.testTag("annual-information")) {
                        Text("按当前分类与现存已修正事实生成；年度真实金额不是总日均成本，也不是完整生活支出。")
                        Text("按真实资金发生日或订阅付款日归年；可以选择没有记录的过去年度。真实0元记录保留，没有录入不代表没有花钱。")
                        Text("长期项目投入、收回、净投入与订阅实际付款分别呈现，不合并为年度总支出。含非现金折抵的项目金额不是纯现金流水。")
                        Text("条形图左负右正，所有年份共用金额尺度；缺失年份不填0。")
                    }
                }
            }
        }
        annual?.let { report ->
            item("summary") { Column(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${report.year} 年", Modifier.testTag("annual-selected-year"), style = MaterialTheme.typography.titleLarge)
                    if (!report.amounts.hasRecords) Text("未录入相关记录，不能据此断言该年没有花钱。", Modifier.testTag("annual-empty-year"))
                    Text("长期项目", style = MaterialTheme.typography.titleMedium)
                    ReportMoneyValues(report.amounts, "annual", OverviewKind.PROJECT)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("订阅", style = MaterialTheme.typography.titleMedium)
                    ReportMoneyValues(report.amounts, "annual", OverviewKind.SUBSCRIPTION)
                }
            } }
            item("category-heading") { Text("年度类别构成 · 按当前分类", style = MaterialTheme.typography.titleLarge) }
            for (kind in OverviewKind.entries) {
                item("kind-$kind") { Text(if (kind == OverviewKind.PROJECT) "长期项目" else "订阅实际付款", style = MaterialTheme.typography.titleMedium) }
                items(report.categories.filter { it.category.kind == kind && it.amounts.hasRecords }, key = { "category-${it.category.id}" }) { category ->
                    Column(Modifier.fillMaxWidth().testTag("annual-category-${category.category.id}")) {
                        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(category.category.name, style = MaterialTheme.typography.titleMedium)
                            ReportMoneyValues(category.amounts, "annual-category-${category.category.id}", category.category.kind)
                        }
                    }
                }
            }
        }
        if (state.snapshot != null) {
            item("years-heading") { Text("年度真实金额趋势", style = MaterialTheme.typography.titleLarge) }
            val reports = state.annualTrend
            val scale = reportScale(reports.map { it.amounts })
            items(reports.take(yearLimit), key = { "trend-${it.year}" }) { report ->
                Column(Modifier.fillMaxWidth().testTag("annual-trend-${report.year}")) {
                    Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportAnnualBars(report.year, report.amounts, scale)
                        TextButton({ onSelectYear(report.year) }, Modifier.heightIn(min = 48.dp).testTag("annual-trend-select-${report.year}")) { Text("查看该年明细") }
                    }
                }
            }
            if (reports.size > yearLimit) item("years-more") {
                OutlinedButton({ yearLimit += 50 }, Modifier.heightIn(min = 48.dp).testTag("annual-years-more")) { Text("继续查看50个年度") }
            }
        }
        annual?.let { report ->
            item("records-heading") { Text("该年度相关记录（${report.records.size}条）", style = MaterialTheme.typography.titleLarge) }
            items(report.records.take(recordLimit), key = { "record-${it.kind}-${it.id}" }) { record -> ReportRecordCard(record, "annual", onOpenRecord) }
            if (report.records.size > recordLimit) item("records-more") {
                OutlinedButton({ recordLimit += 50 }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("annual-records-more")) { Text("继续查看50条记录") }
            }
        }
    }
}
