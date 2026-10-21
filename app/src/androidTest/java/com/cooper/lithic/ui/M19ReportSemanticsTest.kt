package com.cooper.lithic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.domain.report.*
import com.cooper.lithic.feature.category.CategoryAnalysisScreen
import com.cooper.lithic.feature.review.*
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated parameter-page semantics; no MainActivity, system font or TalkBack claim. */
@RunWith(AndroidJUnit4::class)
class M19ReportSemanticsTest {
    @get:Rule val compose = createComposeRule()
    private val day = LocalDate.of(2026, 10, 21)
    private val category = OverviewCategory("p", "合成工具", OverviewKind.PROJECT)
    private val subscription = OverviewCategory("s", "合成服务", OverviewKind.SUBSCRIPTION)
    private val amounts = ReportAmounts(1_300_000, 350_000, 36_500, 3, 1, true)

    @Test fun annual_measures_keep_types_noncash_and_real_zero_distinct_from_empty() {
        compose.setContent { LithicTheme { AnnualReviewScreen(state(), {}, {}, { _, _ -> }, {}) } }
        scrollAnnual("annual-outlay"); compose.onNodeWithTag("annual-outlay").assertTextContains("¥13000.00")
        scrollAnnual("annual-recovery"); compose.onNodeWithTag("annual-recovery").assertTextContains("¥3500.00")
        scrollAnnual("annual-net"); compose.onNodeWithTag("annual-net").assertTextContains("¥9500.00")
        scrollAnnual("annual-paid"); compose.onNodeWithTag("annual-paid").assertTextContains("¥365.00")
        scrollAnnual("annual-noncash"); compose.onNodeWithTag("annual-noncash").assertTextContains("含非现金折抵；不是纯现金流水。")
        compose.onNodeWithTag("annual-empty-year").assertDoesNotExist()
        compose.onNodeWithText("最值得购买").assertDoesNotExist()
    }

    @Test fun year_text_entry_can_select_empty_past_year_and_rejects_future_without_callback() {
        var chosen: Int? = null
        val empty = state().copy(annual = AnnualReport(2026, ReportAmounts(), emptyList(), emptyList()))
        compose.setContent { LithicTheme { AnnualReviewScreen(empty, { chosen = it }, {}, { _, _ -> }, {}) } }
        scrollAnnual("annual-empty-year")
        compose.onNodeWithTag("annual-empty-year").assertTextContains("未录入相关记录，不能据此断言该年没有花钱。")
        scrollAnnual("annual-year-input"); compose.onNodeWithTag("annual-year-input").performTextReplacement("2023")
        scrollAnnual("annual-year-apply"); compose.onNodeWithTag("annual-year-apply").performClick()
        compose.runOnIdle { assertEquals(2023, chosen); chosen = null }
        scrollAnnual("annual-year-input"); compose.onNodeWithTag("annual-year-input").performTextReplacement("2027")
        scrollAnnual("annual-year-apply"); compose.onNodeWithTag("annual-year-apply").performClick()
        compose.onNodeWithText("请输入截至业务今天的有效年度。", useUnmergedTree = true).assertExists()
        compose.runOnIdle { assertNull(chosen) }
    }

    @Test fun category_record_window_keeps_stable_identity_and_type_and_current_classification() {
        var opened: Pair<OverviewKind, String>? = null
        val records = (0..50).map { ReportRecord("p-${it.toString().padStart(2, '0')}", OverviewKind.PROJECT,
            "合成项目$it", category.id, ReportRecordStatus.ENDED) }
        val report = CategoryReport(category, BigDecimal.ZERO, amounts.copy(subscriptionPaidCents = 0, subscriptionPaymentCount = 0),
            listOf(CategoryYearReport(2026, amounts)), records)
        compose.setContent { LithicTheme { CategoryAnalysisScreen(state().copy(selectedCategoryId = "p", categories = listOf(report)),
            { _, _ -> }, {}, { k, id -> opened = k to id }, {}) } }
        compose.onNodeWithTag("category-analysis-basis").assertTextContains("按当前分类")
        scrollCategory("category-analysis-more"); compose.onNodeWithTag("category-analysis-more").performClick()
        scrollCategory("category-analysis-open-p-50")
        compose.onNodeWithTag("category-analysis-open-p-50").performClick()
        compose.runOnIdle { assertEquals(OverviewKind.PROJECT to "p-50", opened) }
        compose.onNodeWithTag("category-analysis-paid").assertDoesNotExist()
    }

    @Test fun failure_is_not_empty_and_double_font_date_and_back_controls_remain_reachable() {
        var backs = 0
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
            LithicTheme { Box(Modifier.width(360.dp).fillMaxSize()) {
                AnnualReviewScreen(state().copy(error = "合成读取失败"), {}, {}, { _, _ -> }, { backs++ })
            } }
        } }
        scrollAnnual("annual-review-error")
        compose.onNodeWithText("读取失败；下方保留上次结果，尚未刷新。", useUnmergedTree = true).assertExists()
        scrollAnnual("annual-year-input"); compose.onNodeWithTag("annual-year-input").assertIsDisplayed()
        scrollAnnual("annual-year-apply"); compose.onNodeWithTag("annual-year-apply").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        scrollAnnual("annual-review-back"); compose.onNodeWithTag("annual-review-back").assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, backs) }
    }

    private fun state(): ReportAnalysisUiState {
        val snapshot = LedgerSnapshot(OverviewSnapshot(listOf(category, subscription), emptyList(), emptyList()))
        val annual = AnnualReport(2026, amounts,
            listOf(AnnualCategoryReport(category, amounts.copy(subscriptionPaidCents = 0, subscriptionPaymentCount = 0)),
                AnnualCategoryReport(subscription, ReportAmounts(subscriptionPaidCents = 36_500, subscriptionPaymentCount = 1))), emptyList())
        return ReportAnalysisUiState(snapshot = snapshot, asOf = day, selectedYear = 2026, years = listOf(2026), annual = annual,
            annualTrend = listOf(annual))
    }
    private fun scrollAnnual(tag: String) = compose.onNodeWithTag("annual-review-screen").performScrollToNode(hasTestTag(tag))
    private fun scrollCategory(tag: String) = compose.onNodeWithTag("category-analysis-screen").performScrollToNode(hasTestTag(tag))
}
