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
import com.cooper.lithic.core.design.AmountFormatter
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.report.*
import com.cooper.lithic.feature.review.TotalCostTrendScreen
import com.cooper.lithic.feature.review.TotalCostTrendUiState
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Parameter-only page checks; not MainActivity, a system font change, or real TalkBack. */
@RunWith(AndroidJUnit4::class)
class M18TotalCostTrendSemanticsTest {
    @get:Rule val compose = createComposeRule()
    private val day = LocalDate.of(2026, 10, 21)

    @Test fun default_total_can_expand_components_with_one_scale_and_text_values() {
        var toggles = 0
        show(state(), toggle = { toggles++ })
        scroll("total-trend-canvas")
        compose.onNodeWithTag("total-trend-canvas").assertContentDescriptionEquals(
            "总日均成本历史实线，负值保留。可展开两类贡献，或在下方输入日期和查看文字清单。纵轴：负1.00元/天 至 1.00元/天；按自然日绘制。")
        openTools()
        scroll("total-trend-breakdown")
        compose.onNodeWithTag("total-trend-breakdown").performClick()
        compose.runOnIdle { assertEquals(1, toggles) }
        scroll("total-trend-project")
        compose.onNodeWithTag("total-trend-project").assertTextContains("−", substring = true)
        scroll("total-trend-subscription")
        compose.onNodeWithTag("total-trend-subscription").assertTextContains(AmountFormatter.formatDailyRate(BigDecimal.TEN))
        scroll("total-trend-details-toggle"); compose.onNodeWithTag("total-trend-details-toggle").performClick()
        compose.onNodeWithTag("total-trend-selected-status").assertTextContains("正负贡献相抵为0", substring = true)
    }

    @Test fun c9_breakdown_legend_names_the_real_series_and_their_distinct_strokes() {
        show(state().copy(showBreakdown = true))
        scroll("total-trend-legend-total")
        compose.onNodeWithTag("total-trend-legend-total")
            .assert(hasAnyDescendant(hasText("总日均成本 · 实线")))
        scroll("total-trend-legend-project")
        compose.onNodeWithTag("total-trend-legend-project")
            .assert(hasAnyDescendant(hasText("长期项目 · 长虚线")))
        scroll("total-trend-legend-subscription")
        compose.onNodeWithTag("total-trend-legend-subscription")
            .assert(hasAnyDescendant(hasText("订阅 · 短虚线阶梯")))
        scroll("total-trend-canvas")
        compose.onNodeWithTag("total-trend-canvas").assertContentDescriptionEquals(
            "总日均成本实线、长期项目长虚线、订阅短虚线阶梯，共同日期和金额尺度。负值保留。下方可输入日期和查看文字清单。纵轴：负10.00元/天 至 10.00元/天；按自然日绘制。")
        scroll("total-trend-total")
        compose.onNodeWithTag("total-trend-total").assertTextContains(AmountFormatter.formatDailyRate(BigDecimal.ZERO))
    }

    @Test fun exact_date_text_entry_avoids_drag_and_future_is_rejected() {
        var requested: LocalDate? = null
        show(state(), select = { requested = it })
        openTools()
        scroll("total-trend-date-toggle"); compose.onNodeWithTag("total-trend-date-toggle").performClick()
        scroll("total-trend-date-input")
        compose.onNodeWithTag("total-trend-date-input").performTextReplacement("2020-04-03")
        scroll("total-trend-date-apply"); compose.onNodeWithTag("total-trend-date-apply").performClick()
        compose.runOnIdle { assertEquals(LocalDate.of(2020, 4, 3), requested) }
        scroll("total-trend-date-input")
        compose.onNodeWithTag("total-trend-date-input").performTextReplacement("2027-01-01")
        scroll("total-trend-date-apply"); compose.onNodeWithTag("total-trend-date-apply").performClick()
        compose.onNodeWithText("不能查看业务今天之后的历史日期。", useUnmergedTree = true).assertExists()
        compose.runOnIdle { assertEquals(LocalDate.of(2020, 4, 3), requested) }
    }

    @Test fun same_day_events_beyond_fifty_keep_record_identity_and_non_cash_note() {
        val events = (0..50).map { TotalCostEvent("event-${it.toString().padStart(2, '0')}", day, OverviewKind.PROJECT,
            "record-$it", "合成项目$it", "同日资金事件", "完整说明$it", 100, nonCash = it == 50) }
        var opened: Pair<OverviewKind, String>? = null
        show(state(events), open = { kind, id -> opened = kind to id })
        openTools()
        scroll("total-trend-events-toggle"); compose.onNodeWithTag("total-trend-events-toggle").performClick()
        scroll("total-trend-events-more"); compose.onNodeWithTag("total-trend-events-more").performClick()
        scroll("total-trend-event-event-50")
        compose.onNodeWithTag("total-trend-event-event-50").assert(hasAnyDescendant(hasText("完整说明50")))
        compose.onNodeWithText("非现金折抵（不是现金扣款）", useUnmergedTree = true).assertExists()
        scroll("total-trend-open-event-50"); compose.onNodeWithTag("total-trend-open-event-50").performClick()
        compose.runOnIdle { assertEquals(OverviewKind.PROJECT to "record-50", opened) }
    }

    @Test fun empty_ledger_has_no_fake_zero_and_read_failure_has_no_fake_empty() {
        show(TotalCostTrendUiState(snapshot = LedgerSnapshot(OverviewSnapshot(emptyList(), emptyList(), emptyList())),
            timeline = CostTimeline(day, null, emptyList(), emptyList(), 240, 0, false)))
        compose.onNodeWithTag("total-trend-empty").assertExists()
        compose.onNodeWithTag("total-trend-total").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-canvas").assertDoesNotExist()
    }

    @Test fun failure_is_visible_and_narrow_double_font_controls_remain_reachable() {
        var backs = 0
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
            LithicTheme { Box(Modifier.width(360.dp).fillMaxSize()) {
                TotalCostTrendScreen(state().copy(error = "合成读取失败"), {}, {}, {}, { _, _ -> }, { backs++ })
            } }
        } }
        scroll("total-trend-error")
        compose.onNodeWithText("读取失败；仍保留上次结果，尚未刷新。", useUnmergedTree = true).assertExists()
        openTools()
        scroll("total-trend-date-toggle"); compose.onNodeWithTag("total-trend-date-toggle").performClick()
        scroll("total-trend-date-apply")
        compose.onNodeWithTag("total-trend-date-apply").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        scroll("total-trend-back")
        compose.onNodeWithTag("total-trend-back").assertIsDisplayed().assertContentDescriptionEquals("返回回看")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, backs) }
    }

    private fun state(events: List<TotalCostEvent> = emptyList()): TotalCostTrendUiState {
        val input = ProjectSnapshot(LongTermProject(day.minusYears(20)),
            listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(0), day.minusYears(20))))
        val ledger = LedgerSnapshot(OverviewSnapshot(listOf(OverviewCategory("c", "其他", OverviewKind.PROJECT)),
            listOf(OverviewProject("p", "c", input)), emptyList()), events)
        val point = CostTimelinePoint(day, BigDecimal.TEN.negate(), BigDecimal.TEN, BigDecimal.ZERO,
            events.map { it.id }, 1, 1, 1, 1, true, true)
        return TotalCostTrendUiState(ledger, CostTimeline(day, day.minusYears(20), listOf(point), events, 240, 7306, false),
            day, point, events)
    }
    private fun show(state: TotalCostTrendUiState, select: (LocalDate) -> Unit = {}, toggle: () -> Unit = {},
        open: (OverviewKind, String) -> Unit = { _, _ -> }) {
        compose.setContent { LithicTheme { TotalCostTrendScreen(state, select, toggle, {}, open, {}) } }
    }
    private fun openTools() { scroll("total-trend-tools-toggle"); compose.onNodeWithTag("total-trend-tools-toggle").performClick() }
    private fun scroll(tag: String) = compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag(tag))
}
