package com.cooper.lithic.feature.category

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.design.moneyMetricDisplay
import com.cooper.lithic.core.model.*
import com.cooper.lithic.domain.report.*
import com.cooper.lithic.domain.subscription.*
import com.cooper.lithic.feature.review.ReportAnalysisUiState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Immutable synthetic facts and production Compose; no Room, Owner or screenshots.
 * Controlled 400dp/fontScale1 host is not real-system layout or TalkBack evidence. NOT_RUN. */
class SingleCategoryPresentationSemanticsTest {
    @get:Rule val compose = createComposeRule()
    private val day = LocalDate.of(2026, 10, 28)
    private val pc = OverviewCategory("single-pc", "其他", OverviewKind.PROJECT)
    private val sc = OverviewCategory("single-sc", "其他订阅", OverviewKind.SUBSCRIPTION)
    private val extra = OverviewCategory("single-extra", "主动选择的类别", OverviewKind.PROJECT)

    @Test fun sole_category_opens_real_analysis_without_callback_and_type_and_explicit_choice_remain_honest() {
        val ui = mutableStateOf(ready())
        var selections = 0
        var opened: Pair<OverviewKind, String>? = null
        show({ ui.value }, select = { kind, id -> selections++; ui.value = ui.value.copy(selectedKind = kind, selectedCategoryId = id) },
            open = { kind, id -> opened = kind to id })
        scroll("category-analysis-name").assertTextEquals(pc.name)
        val report = ui.value.categories.single { it.category == pc }
        scroll("category-analysis-rate").assertTextEquals(moneyMetricDisplay(report.currentRate))
        compose.onNodeWithText("请选择类别。当前没有有效贡献但有历史记录的类别也可查看。").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, selections) }
        scroll("category-analysis-open-single-project").performClick()
        compose.runOnIdle { assertEquals(OverviewKind.PROJECT to "single-project", opened) }
        scroll("category-analysis-subscription").performClick()
        scroll("category-analysis-name").assertTextEquals(sc.name)
        compose.runOnIdle { assertEquals(1, selections) }
        compose.runOnIdle { ui.value = ready(extraCategory = true).copy(selectedCategoryId = extra.id) }
        scroll("category-analysis-name").assertTextEquals(extra.name)
        compose.runOnIdle { ui.value = ui.value.copy(loading = true, error = "合成刷新失败") }
        scroll("category-analysis-name").assertTextEquals(extra.name)
        scroll("category-analysis-error").assertIsDisplayed()
        compose.onNodeWithText("合成刷新失败").assertExists()
    }

    @Test fun retained_report_survives_refresh_failure_while_incomplete_and_empty_states_stay_distinct() {
        val known = ready()
        val ui = mutableStateOf(known.copy(loading = true))
        var retries = 0
        var selections = 0
        show({ ui.value }, select = { _, _ -> selections++ }, retry = { retries++ })
        scroll("category-analysis-name").assertTextEquals(pc.name)
        compose.onNodeWithText("正在刷新；下方为上次截止日和选择的结果。").assertExists()
        compose.onNodeWithText("结果截至 $day").assertExists()
        compose.runOnIdle { ui.value = known.copy(error = "合成刷新失败") }
        scroll("category-analysis-name").assertTextEquals(pc.name)
        scroll("category-analysis-error").assertIsDisplayed()
        compose.onNodeWithText("读取失败；下方保留上次结果，尚未刷新。").assertExists()
        scroll("category-analysis-retry").performClick()
        compose.runOnIdle { assertEquals(1, retries); assertEquals(0, selections) }
        listOf(known.copy(snapshot = null, error = "合成首次读取失败"),
            known.copy(notice = "所选类别已删除，待核对"),
            known.copy(snapshot = null), known.copy(asOf = null)).forEach { state ->
            compose.runOnIdle { ui.value = state }
            compose.onNodeWithTag("category-analysis-selected").assertDoesNotExist()
        }
        val none = LedgerSnapshot(OverviewSnapshot(emptyList(), emptyList(), emptyList()), financialFacts = emptyList())
        compose.runOnIdle { ui.value = ReportAnalysisUiState(snapshot = none, asOf = day) }
        scroll("category-analysis-empty").assertTextEquals("未读取到该类型类别。")
        compose.onNodeWithTag("category-analysis-selected").assertDoesNotExist()
        compose.runOnIdle { ui.value = ready(populated = false) }
        scroll("category-analysis-name").assertTextEquals(pc.name)
        scroll("category-analysis-no-facts").assertTextEquals("未录入相关资金记录。")
        compose.onNodeWithTag("category-analysis-empty").assertDoesNotExist()
        compose.onNodeWithTag("category-analysis-screen").performScrollToNode(hasText("未录入相关记录。"))
        compose.onNodeWithText("未录入相关记录。").assertExists()
    }

    private fun show(value: () -> ReportAnalysisUiState, select: (OverviewKind, String?) -> Unit = { _, _ -> },
        retry: () -> Unit = {}, open: (OverviewKind, String) -> Unit = { _, _ -> }) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
                LithicTheme { Box(Modifier.requiredSize(400.dp, 1200.dp)) {
                    CategoryAnalysisScreen(value(), select, retry, open, {})
                } }
            }
        }
    }

    private fun ready(populated: Boolean = true, extraCategory: Boolean = false): ReportAnalysisUiState {
        val start = day.minusDays(9)
        val project = OverviewProject("single-project", pc.id,
            ProjectSnapshot(LongTermProject(start), listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(10000), start))))
        val subscription = OverviewSubscription("single-subscription", sc.id,
            SubscriptionSnapshot(Subscription("single-subscription"), listOf(
                Renewal(Money(1800), BillingCycle.MONTHLY, start, start, start.plusMonths(1)))))
        val snapshot = LedgerSnapshot(OverviewSnapshot(listOf(pc, sc) + if (extraCategory) listOf(extra) else emptyList(),
            if (populated) listOf(project) else emptyList(), if (populated) listOf(subscription) else emptyList()),
            financialFacts = if (populated) listOf(
                FinancialFact("single-investment", project.id, FinancialFactKind.PROJECT_OUTLAY, start, 10000),
                FinancialFact("single-payment", subscription.id, FinancialFactKind.SUBSCRIPTION_PAYMENT, start, 1800)) else emptyList(),
            recordIdentities = if (populated) listOf(
                ReportRecordIdentity(project.id, OverviewKind.PROJECT, "合成项目", pc.id),
                ReportRecordIdentity(subscription.id, OverviewKind.SUBSCRIPTION, "合成订阅", sc.id)) else emptyList())
        return ReportAnalysisUiState(snapshot = snapshot, asOf = day, categories = CategoryReportEngine.evaluate(snapshot, day))
    }

    private fun scroll(tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("category-analysis-screen").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }
}
