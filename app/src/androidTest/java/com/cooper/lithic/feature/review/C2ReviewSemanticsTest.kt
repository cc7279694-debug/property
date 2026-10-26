package com.cooper.lithic.feature.review

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.report.CostTimeline
import com.cooper.lithic.domain.report.CostTimelineEngine
import com.cooper.lithic.domain.report.LedgerSnapshot
import com.cooper.lithic.domain.report.OverviewCategory
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.OverviewProject
import com.cooper.lithic.domain.report.OverviewSnapshot
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Production Composable assertions; compilation is not Android execution or a screenshot. */
@RunWith(AndroidJUnit4::class)
class C2ReviewSemanticsTest {
    @get:Rule val compose = createComposeRule()
    private val day = LocalDate.of(2026, 10, 26)

    @Test fun initial_loading_does_not_invent_a_chart_or_zero_value() {
        show(TotalCostTrendUiState(loading = true))
        compose.onNodeWithText("正在读取完整历史。").assertIsDisplayed()
        compose.onNodeWithTag("total-trend-canvas").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-total").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-empty").assertDoesNotExist()
    }

    @Test fun verified_empty_ledger_has_no_decorative_curve() {
        val ledger = LedgerSnapshot(OverviewSnapshot(emptyList(), emptyList(), emptyList()))
        show(TotalCostTrendUiState(snapshot = ledger,
            timeline = CostTimeline(day, null, emptyList(), emptyList(), 240, 0, false)))
        compose.onNodeWithTag("review-empty-state").assertIsDisplayed()
        compose.onNodeWithTag("total-trend-canvas").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-total").assertDoesNotExist()
    }

    @Test fun failed_initial_read_does_not_report_an_empty_ledger_and_retry_is_explicit() {
        var retries = 0
        compose.setContent { LithicTheme {
            ReviewShell(0, trendState = TotalCostTrendUiState(error = "合成读取失败"), hasReadNotice = true,
                onRetryTrend = { retries++ })
        } }
        compose.onNodeWithTag("total-trend-error").assertIsDisplayed()
        compose.onNodeWithTag("total-trend-retry").performClick()
        compose.onNodeWithTag("total-trend-empty").assertDoesNotExist()
        compose.onNodeWithText("暂无长期项目或订阅记录").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun retained_negative_result_is_qualified_after_refresh_failure() {
        show(recordState(recoveryCents = 100).copy(error = "合成刷新失败"))
        compose.onNodeWithText("读取失败；下方保留上次结果，尚未刷新。").assertIsDisplayed()
        scroll("total-trend-total")
        compose.onNodeWithTag("total-trend-total").assertTextContains("−", substring = true)
        scroll("total-trend-canvas")
        compose.onNodeWithTag("total-trend-canvas").assertIsDisplayed()
    }

    @Test fun real_zero_is_not_empty_and_exact_date_query_uses_existing_callback() {
        var selected: LocalDate? = null
        compose.setContent { LithicTheme {
            ReviewShell(1, trendState = recordState(), onSelectTrendDate = { selected = it })
        } }
        scroll("total-trend-total")
        compose.onNodeWithTag("total-trend-total").assertTextContains("¥0.00", substring = true)
        compose.onNodeWithTag("total-trend-empty").assertDoesNotExist()
        scroll("review-trend-date-toggle")
        compose.onNodeWithTag("review-trend-date-toggle").performClick()
        scroll("total-trend-date-input")
        compose.onNodeWithTag("total-trend-date-input").performTextReplacement("2026-10-20")
        scroll("total-trend-date-apply")
        compose.onNodeWithTag("total-trend-date-apply").performClick()
        compose.runOnIdle { assertEquals(LocalDate.of(2026, 10, 20), selected) }
    }

    @Test fun every_existing_review_destination_keeps_its_own_callback() {
        val opened = mutableListOf<String>()
        compose.setContent { LithicTheme {
            ReviewShell(1, trendState = recordState(), onTotalCostTrend = { opened += "trend" },
                onCategoryAnalysis = { opened += "categories" }, onAnnualReview = { opened += "annual" },
                onLongTermReview = { opened += "ranks" }, onProjectHistory = { opened += "projects" },
                onSubscriptionHistory = { opened += "subscriptions" })
        } }
        listOf("review-total-cost-trend", "review-category-analysis", "review-annual", "review-long-term",
            "review-project-history", "review-subscription-history").forEach { tag ->
            scroll(tag); compose.onNodeWithTag(tag).performClick()
        }
        compose.runOnIdle { assertEquals(listOf("trend", "categories", "annual", "ranks", "projects", "subscriptions"), opened) }
    }

    private fun recordState(recoveryCents: Long = 0): TotalCostTrendUiState {
        val startedOn = day.minusDays(10)
        val project = ProjectSnapshot(LongTermProject(startedOn), listOf(
            ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(0), startedOn),
            ProjectMoneyEvent(MoneyDirection.RECOVERY, Money(recoveryCents), day)))
        val ledger = LedgerSnapshot(OverviewSnapshot(listOf(OverviewCategory("c2-project-category", "其他", OverviewKind.PROJECT)),
            listOf(OverviewProject("c2-project", "c2-project-category", project)), emptyList()))
        val point = CostTimelineEngine.point(ledger, day)
        return TotalCostTrendUiState(snapshot = ledger,
            timeline = CostTimeline(day, startedOn, listOf(point), emptyList(), 240, 11, false),
            selectedDate = day, selectedPoint = point)
    }

    private fun show(state: TotalCostTrendUiState) {
        compose.setContent { LithicTheme { ReviewShell(state.snapshot?.overview?.projects?.size?.toLong() ?: 0, trendState = state) } }
    }
    private fun scroll(tag: String) = compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag(tag))
}
