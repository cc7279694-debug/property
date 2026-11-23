package com.cooper.lithic.feature.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.C2PageHeader
import com.cooper.lithic.core.design.C5InformationButton
import com.cooper.lithic.core.design.LithicOutlinedButton
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.LithicTextButton
import com.cooper.lithic.core.design.LocalLithicVisuals
import com.cooper.lithic.core.design.MoneyMetric
import com.cooper.lithic.core.design.MoneyMetricLevel
import com.cooper.lithic.domain.report.CategoryReport
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.feature.review.ReportAnalysisUiState
import com.cooper.lithic.feature.review.ReportAnnualBars
import com.cooper.lithic.feature.review.ReportKindSegment
import com.cooper.lithic.feature.review.ReportMoneyValues
import com.cooper.lithic.feature.review.ReportReadStatus
import com.cooper.lithic.feature.review.ReportRecordCard
import com.cooper.lithic.feature.review.reportScale

@Composable
internal fun CategoryAnalysisScreen(
    state: ReportAnalysisUiState,
    onSelectCategory: (OverviewKind, String?) -> Unit,
    onRetry: () -> Unit,
    onOpenRecord: (OverviewKind, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = categoryAnalysisSelection(state)
    var recordLimit by remember(state.snapshot, state.selectedKind, selected?.category?.id) { mutableIntStateOf(50) }
    var yearLimit by remember(state.snapshot, state.selectedKind, selected?.category?.id) { mutableIntStateOf(50) }
    val categories = state.categories.filter { it.category.kind == state.selectedKind }
    val kindLabel = if (state.selectedKind == OverviewKind.PROJECT) "长期项目" else "订阅"

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 560.dp).fillMaxSize().testTag("category-analysis-screen"),
            contentPadding = PaddingValues(start = LithicSpacing.page, end = LithicSpacing.page, top = 12.dp, bottom = 20.dp),
        ) {
            item("heading") {
                CategoryAnalysisHeading(state, onBack)
                Box(Modifier.padding(top = 12.dp)) {
                    ReportKindSegment(state.selectedKind, "category-analysis") { onSelectCategory(it, null) }
                }
            }
            if (state.loading || state.error != null || state.notice != null) {
                item("read-status") {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportReadStatus(state, onRetry, "category-analysis")
                    }
                }
            }
            if (state.snapshot != null) {
                if (categories.isEmpty()) {
                    item("no-categories") {
                        CategoryAnalysisNote("未读取到该类型类别。", Modifier.padding(top = 16.dp).testTag("category-analysis-empty"))
                    }
                } else {
                    item("category-picker") {
                        CategoryAnalysisPicker(
                            categories, selected, state.selectedKind, onSelectCategory, Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }
            selected?.let { report ->
                item("selected") {
                    CategoryCurrentRate(report, kindLabel, showName = state.snapshot == null)
                }
                item("historical") { CategoryHistoricalAmounts(report) }
                item("year-heading") {
                    CategoryAnalysisTitle("各年度真实金额", Modifier.testTag("category-analysis-years"))
                }
                val years = report.annual.sortedByDescending { it.year }
                val scale = reportScale(years.map { it.amounts }, report.category.kind)
                itemsIndexed(years.take(yearLimit), key = { _, year -> "year-${year.year}" }) { index, year ->
                    Column(Modifier.fillMaxWidth().testTag("category-analysis-year-${year.year}")) {
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(
                            Modifier.padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ReportAnnualBars(year.year, year.amounts, scale, report.category.kind)
                        }
                    }
                }
                if (years.isEmpty()) item("no-years") {
                    CategoryAnalysisNote("未录入相关年度资金记录。", Modifier.padding(top = 8.dp))
                }
                if (years.size > yearLimit) item("more-years") {
                    LithicOutlinedButton(
                        { yearLimit += 50 },
                        Modifier.heightIn(min = 48.dp).testTag("category-analysis-years-more"),
                    ) { Text("继续查看50个年度") }
                }
                item("records-heading") { CategoryAnalysisTitle("当前与历史记录（${report.records.size}条）") }
                items(report.records.take(recordLimit), key = { "record-${it.kind}-${it.id}" }) { record ->
                    ReportRecordCard(record, "category-analysis", onOpenRecord)
                }
                if (report.records.isEmpty()) item("no-records") {
                    CategoryAnalysisNote("未录入相关记录。", Modifier.padding(top = 8.dp))
                }
                if (report.records.size > recordLimit) item("more-records") {
                    LithicOutlinedButton(
                        { recordLimit += 50 },
                        Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 48.dp).testTag("category-analysis-more"),
                    ) { Text("继续查看50条记录") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryAnalysisHeading(state: ReportAnalysisUiState, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        C2PageHeader("类别分析", onBack, "category-analysis-back", backLabel = "返回回看") {
            C5InformationButton("类别分析口径", Modifier.testTag("category-analysis-information"), iconOnly = true) {
                Text("改名、合并或纠错后按当前分类计算。项目、订阅分列，区分当前日均与历史实际金额。")
                Text("按实际发生年或付款年统计。无记录的年份不显示。图表左负右正，共用金额尺度。")
                Text("可选择仅有历史记录的类别。")
            }
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            state.asOf?.let { CategoryAnalysisNote("结果截至 $it") }
            CategoryAnalysisNote("按当前分类", Modifier.testTag("category-analysis-basis"))
        }
    }
}

@Composable
private fun CategoryAnalysisPicker(
    categories: List<CategoryReport>,
    selected: CategoryReport?,
    kind: OverviewKind,
    onSelect: (OverviewKind, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable(kind) { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag(if (selected == null) "category-analysis-picker" else "category-analysis-name")
                .clickable(onClickLabel = "选择类别", role = Role.Button) { open = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                selected?.category?.name ?: "选择类别",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            C2Icon(C2Glyph.Chevron, Modifier.size(18.dp))
        }
    }
    if (open) {
        val selectedIndex = categories.indexOfFirst { it.category.id == selected?.category?.id }.coerceAtLeast(0)
        val optionsState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
        AlertDialog(
            onDismissRequest = { open = false },
            modifier = Modifier.testTag("category-analysis-picker-dialog"),
            containerColor = colors.surface,
            title = { Text("选择类别") },
            text = {
                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 320.dp)
                        .selectableGroup().testTag("category-analysis-picker-list"),
                    state = optionsState,
                ) {
                    items(categories, key = { it.category.id }) { report ->
                        val category = report.category
                        val isSelected = category.id == selected?.category?.id
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("category-select-${category.id}")
                                .selectable(selected = isSelected, role = Role.RadioButton) {
                                    open = false
                                    onSelect(category.kind, category.id)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Text(
                                category.name,
                                Modifier.weight(1f),
                                color = colors.onSurface,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                LithicTextButton({ open = false }, Modifier.heightIn(min = 48.dp)) { Text("取消") }
            },
        )
    }
}

@Composable
private fun CategoryCurrentRate(report: CategoryReport, kindLabel: String, showName: Boolean) {
    Surface(
        Modifier.fillMaxWidth().padding(top = 16.dp).testTag("category-analysis-selected"),
        shape = RoundedCornerShape(14.dp),
        color = LocalLithicVisuals.current.costSurface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showName) {
                Text(report.category.name, Modifier.testTag("category-analysis-name"), style = MaterialTheme.typography.titleMedium)
            }
            Text("当前日均成本贡献", style = MaterialTheme.typography.bodyMedium)
            MoneyMetric(
                report.currentRate,
                Modifier.fillMaxWidth().testTag("category-analysis-rate"),
                level = MoneyMetricLevel.DETAIL,
                label = "$kindLabel · 当前日均成本贡献",
                keepPreferredSize = true,
            )
        }
    }
}

@Composable
private fun CategoryHistoricalAmounts(report: CategoryReport) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryAnalysisTitle("历史累计真实金额")
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (report.historical.hasRecords) {
                    ReportMoneyValues(report.historical, "category-analysis", report.category.kind)
                } else {
                    CategoryAnalysisNote("未录入相关资金记录。", Modifier.testTag("category-analysis-no-facts"))
                }
            }
        }
    }
}

@Composable
private fun CategoryAnalysisTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(top = 20.dp).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun CategoryAnalysisNote(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A complete captured report can be retained during refresh/error; ReportReadStatus qualifies it. */
internal fun categoryAnalysisSelection(state: ReportAnalysisUiState): CategoryReport? {
    state.selectedCategory?.let { return it }
    if (state.selectedCategoryId != null || state.snapshot == null || state.asOf == null ||
        state.notice != null) return null
    return state.categories.filter { it.category.kind == state.selectedKind }.singleOrNull()
}
