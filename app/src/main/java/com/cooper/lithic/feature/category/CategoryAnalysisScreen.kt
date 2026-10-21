package com.cooper.lithic.feature.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountText
import com.cooper.lithic.domain.report.*
import com.cooper.lithic.feature.review.*

@Composable
internal fun CategoryAnalysisScreen(
    state: ReportAnalysisUiState,
    onSelectCategory: (OverviewKind, String?) -> Unit,
    onRetry: () -> Unit,
    onOpenRecord: (OverviewKind, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = state.selectedCategory
    var recordLimit by remember(state.snapshot, state.selectedCategoryId) { mutableIntStateOf(50) }
    var yearLimit by remember(state.snapshot, state.selectedCategoryId) { mutableIntStateOf(50) }
    val categories = state.categories.filter { it.category.kind == state.selectedKind }
    val kindLabel = if (state.selectedKind == OverviewKind.PROJECT) "长期项目" else "订阅"
    LazyColumn(modifier.fillMaxSize().testTag("category-analysis-screen"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item("heading") { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onBack, Modifier.heightIn(min = 48.dp).testTag("category-analysis-back")) { Text("返回回看") }
            Text("类别分析", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Text("按当前分类", Modifier.testTag("category-analysis-basis"), style = MaterialTheme.typography.titleMedium)
            Text("改名、合并和历史纠错后重新计算；不推断过去的类别名称。当前日均成本与历史真实金额是不同指标。")
            state.asOf?.let { Text("结果截至 $it") }
            OutlinedButton({ onSelectCategory(OverviewKind.PROJECT, null) }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("category-analysis-project")) {
                Text(if (state.selectedKind == OverviewKind.PROJECT) "长期项目 · 已选择" else "长期项目")
            }
            OutlinedButton({ onSelectCategory(OverviewKind.SUBSCRIPTION, null) }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("category-analysis-subscription")) {
                Text(if (state.selectedKind == OverviewKind.SUBSCRIPTION) "订阅 · 已选择" else "订阅")
            }
        } }
        item("read-status") { ReportReadStatus(state, onRetry, "category-analysis") }
        if (state.snapshot != null) {
            item("category-heading") { Text("$kindLabel 类别", style = MaterialTheme.typography.titleLarge) }
            items(categories, key = { "category-${it.category.id}" }) { category ->
                OutlinedButton({ onSelectCategory(category.category.kind, category.category.id) },
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("category-select-${category.category.id}")) {
                    Text("${category.category.name}${if (selected?.category?.id == category.category.id) " · 已选择" else ""}")
                }
            }
            if (categories.isEmpty()) item("no-categories") { Text("未读取到该类型类别。", Modifier.testTag("category-analysis-empty")) }
            if (selected == null) item("select-category") { Text("请选择类别。当前没有有效贡献但有历史记录的类别也可查看。") }
        }
        selected?.let { report ->
            item("selected") { Card(Modifier.fillMaxWidth().testTag("category-analysis-selected")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(report.category.name, Modifier.testTag("category-analysis-name"), style = MaterialTheme.typography.titleLarge)
                    Text("$kindLabel · 当前日均成本贡献")
                    AmountText(report.currentRate, Modifier.testTag("category-analysis-rate"), style = MaterialTheme.typography.headlineMedium)
                    Text("历史累计真实金额 · 按当前分类")
                    ReportMoneyValues(report.historical, "category-analysis", report.category.kind)
                    if (!report.historical.hasRecords) Text("未录入相关资金记录。", Modifier.testTag("category-analysis-no-facts"))
                }
            } }
            item("year-heading") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("各年度真实金额", Modifier.testTag("category-analysis-years"), style = MaterialTheme.typography.titleLarge)
                    Text("仅列已录入资金事实的年份，缺年不代表没有花钱。按实际发生年或付款年归属；左负右正，所有年份共用金额尺度。")
                }
            }
            val years = report.annual.sortedByDescending { it.year }
            val scale = reportScale(years.map { it.amounts }, report.category.kind)
            items(years.take(yearLimit), key = { "year-${it.year}" }) { year ->
                Card(Modifier.fillMaxWidth().testTag("category-analysis-year-${year.year}")) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportAnnualBars(year.year, year.amounts, scale, report.category.kind)
                    }
                }
            }
            if (years.isEmpty()) item("no-years") { Text("未录入相关年度资金记录。") }
            if (years.size > yearLimit) item("more-years") {
                OutlinedButton({ yearLimit += 50 }, Modifier.heightIn(min = 48.dp).testTag("category-analysis-years-more")) { Text("继续查看50个年度") }
            }
            item("records-heading") {
                Text("该类别的当前与历史记录（${report.records.size}条）", style = MaterialTheme.typography.titleLarge)
            }
            items(report.records.take(recordLimit), key = { "record-${it.kind}-${it.id}" }) { record -> ReportRecordCard(record, "category-analysis", onOpenRecord) }
            if (report.records.isEmpty()) item("no-records") { Text("未录入相关记录。") }
            if (report.records.size > recordLimit) item("more-records") {
                OutlinedButton({ recordLimit += 50 }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("category-analysis-more")) { Text("继续查看50条记录") }
            }
        }
    }
}
