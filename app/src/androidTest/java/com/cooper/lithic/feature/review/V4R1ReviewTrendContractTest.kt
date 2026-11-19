package com.cooper.lithic.feature.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.design.moneyMetricDisplay
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.report.CostTimelineEngine
import com.cooper.lithic.domain.report.LedgerSnapshot
import com.cooper.lithic.domain.report.OverviewCategory
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.OverviewProject
import com.cooper.lithic.domain.report.OverviewSnapshot
import com.cooper.lithic.domain.report.OverviewSubscription
import com.cooper.lithic.domain.report.TimelineSampler
import com.cooper.lithic.domain.report.TotalCostEvent
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.feature.appearance.LithicAppearance
import com.cooper.lithic.ui.C2VisualFixtures
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Controlled Compose with synthetic, engine-derived facts. These density cases simulate font scale. */
class V4R1ReviewTrendContractTest {
    @get:Rule val compose = createComposeRule()
    private val day = LocalDate.of(2026, 11, 16)

    @Test fun one_day_shows_real_facts_and_keeps_exact_date_and_text_points_available() {
        val initial = facts(outlay = 3000)
        val ui = mutableStateOf(initial)
        var selected: LocalDate? = null
        compose.setContent { LithicTheme {
            Trend(ui.value, onSelect = { selected = it })
        } }
        full("trend-single-date-facts").assertIsDisplayed()
        full("total-trend-selected-date").assertTextEquals(day.toString())
        full("total-trend-total").assertTextEquals("¥30.00/天")
        full("total-trend-project").assertTextEquals("¥30.00/天")
        full("total-trend-subscription").assertTextEquals("¥0.00/天")
        compose.onNodeWithTag("total-trend-canvas").assertDoesNotExist()
        compose.onNodeWithTag("total-trend-legend-total").assertDoesNotExist()

        full("total-trend-points-toggle").assertHeightIsAtLeast(48.dp).performClick()
        full("total-trend-point-total-$day").assertTextEquals("¥30.00/天")
        full("total-trend-points-toggle").performClick()
        compose.onNodeWithTag("total-trend-point-total-$day").assertDoesNotExist()

        full("total-trend-tools-toggle").performClick()
        full("total-trend-date-toggle").performClick()
        val historical = day.minusDays(2)
        full("total-trend-date-input").performTextReplacement(historical.toString())
        full("total-trend-date-apply").assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle {
            assertEquals(historical, selected)
            ui.value = initial.onSelectingDate(historical)
        }
        full("total-trend-selected-date").assertTextEquals(historical.toString())
        full("total-trend-total-pending").assertTextEquals("—")
        full("total-trend-project-pending").assertTextEquals("—")
        full("total-trend-subscription-pending").assertTextEquals("—")
        compose.onNodeWithTag("total-trend-total").assertDoesNotExist()

        compose.runOnIdle {
            ui.value = ui.value.copy(selectedPoint = CostTimelineEngine.point(requireNotNull(initial.snapshot), historical))
        }
        full("total-trend-single-status").assertTextContains("没有有效成本贡献", substring = true)
        full("total-trend-cutoff").assertTextEquals("截至 $day")
        compose.onNodeWithTag("total-trend-canvas").assertDoesNotExist()
    }

    @Test fun one_day_distinguishes_active_zero_offset_and_no_active_contribution() {
        val genuineZero = facts(outlay = 0)
        val cancellation = facts(outlay = 100, recovery = 200, subscription = true)
        val noContribution = facts(outlay = 1000, ended = true)
        val ui = mutableStateOf(genuineZero)
        compose.setContent { LithicTheme { Trend(ui.value) } }

        full("total-trend-total").assertTextEquals("¥0.00/天")
        full("total-trend-single-status").assertTextContains("真实日均成本为0", substring = true)
        compose.runOnIdle { ui.value = cancellation }
        full("total-trend-total").assertTextEquals("¥0.00/天")
        full("total-trend-project").assertTextEquals("−¥1.00/天")
        full("total-trend-subscription").assertTextEquals("¥1.00/天")
        full("total-trend-single-status").assertTextContains("正负贡献相抵为0", substring = true)
        compose.runOnIdle { ui.value = noContribution }
        full("total-trend-single-status").assertTextContains("历史记录仍保留", substring = true)
        compose.onNodeWithTag("total-trend-empty").assertDoesNotExist()
    }

    @Test fun sampled_constant_zero_over_many_days_with_one_real_event_keeps_the_full_plot() {
        val multi = facts(outlay = 0, start = day.minusYears(2))
        val timeline = requireNotNull(multi.timeline)
        assertTrue(timeline.sampled)
        assertTrue(timeline.evaluatedDayCount > 1)
        assertEquals(1, timeline.events.size)
        assertTrue(timeline.points.all { it.totalRate.signum() == 0 })
        compose.setContent { LithicTheme { Trend(multi.copy(showBreakdown = true)) } }

        full("total-trend-canvas").assertIsDisplayed()
        full("total-trend-axis-unit").assertTextEquals("纵轴 · 元/天")
        full("total-trend-legend-project").assertIsDisplayed()
        full("total-trend-legend-subscription").assertIsDisplayed()
        full("total-trend-total").assertTextEquals("¥0.00/天")
        compose.onNodeWithTag("trend-single-date-facts").assertDoesNotExist()
    }

    @Test fun full_plot_horizontal_drag_selects_exact_days_and_vertical_swipe_scrolls_the_page() {
        val start = day.minusDays(10)
        // Retain only endpoints so a midpoint gesture cannot be satisfied by a displayed point.
        val source = facts(outlay = 3000, start = start)
        val fullTimeline = requireNotNull(source.timeline)
        val ui = mutableStateOf(source.copy(timeline = fullTimeline.copy(
            points = listOf(fullTimeline.points.first(), fullTimeline.points.last()), sampled = true
        )))
        val selections = mutableListOf<LocalDate>()
        compose.setContent { LithicTheme { Trend(ui.value, onSelect = { selections += it }) } }
        val inset = with(compose.density) { 12.dp.toPx() }

        full("total-trend-canvas").performTouchInput {
            val usable = width - 2 * inset
            swipe(Offset(inset + usable * .1f, centerY), Offset(inset + usable * .6f, centerY), 400)
        }
        val count = compose.runOnIdle {
            assertEquals(start.plusDays(6), selections.last())
            selections.size
        }
        val scrollRange = compose.onNodeWithTag("total-cost-trend-screen").fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        val before = compose.runOnIdle { scrollRange.value() }
        compose.onNodeWithTag("total-trend-canvas").performTouchInput { swipeUp() }
        compose.runOnIdle {
            assertTrue("Vertical gesture must scroll the enclosing page", scrollRange.value() > before)
            assertEquals(count, selections.size)
        }

        val date = start.minusDays(1)
        compose.runOnIdle { ui.value = ui.value.onSelectingDate(date) }
        full("total-trend-canvas").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
            "$date，尚未取得所选日期的精确结果"))
        val exact = CostTimelineEngine.point(requireNotNull(source.snapshot), date)
        compose.runOnIdle { ui.value = ui.value.copy(selectedPoint = exact) }
        val description = full("total-trend-canvas").fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        assertTrue(description.startsWith(date.toString()))
        assertTrue(description.endsWith("该日期不在绘图范围内"))
        full("total-trend-total").assertTextEquals(moneyMetricDisplay(exact.totalRate))
    }

    @Test fun both_pages_fit_three_themes_and_widths_at_normal_and_simulated_double_font() {
        val fixture = C2VisualFixtures(populated = true)
        val selected = fixture.day.minusDays(7)
        val point = CostTimelineEngine.point(fixture.ledger, selected)
        val trend = fixture.trend.copy(selectedDate = selected, selectedPoint = point)
        val layout = mutableStateOf(LayoutCase(LithicAppearance.STONE_MIST, 360, 1f))
        val inTrend = mutableStateOf(false)
        compose.setContent {
            val current = layout.value
            key(current) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val pixelsPerDp = constraints.maxWidth.toFloat() / current.width
                    val height = minOf(800f, constraints.maxHeight / pixelsPerDp)
                    CompositionLocalProvider(LocalDensity provides Density(pixelsPerDp, current.fontScale)) {
                        LithicTheme(current.appearance) {
                            Box(Modifier.requiredSize(current.width.dp, height.dp).testTag("r1-layout-host")) {
                                if (inTrend.value) {
                                    Trend(trend, onBack = { inTrend.value = false })
                                } else {
                                    ReviewShell(1, subscriptionCount = 1, trendState = trend,
                                        onTotalCostTrend = { inTrend.value = true }, onSubscriptionHistory = {})
                                }
                            }
                        }
                    }
                }
            }
        }

        LithicAppearance.entries.forEach { appearance ->
            listOf(320, 360, 400).forEach { width ->
                listOf(1f, 2f).forEach { scale ->
                    compose.runOnIdle { layout.value = LayoutCase(appearance, width, scale) }
                    review("total-trend-selection-label").assertTextEquals("所选日期")
                    review("total-trend-selected-date").assertTextEquals(selected.toString())
                    assertWithinHost(review("total-trend-total").assertTextEquals(moneyMetricDisplay(point.totalRate)))
                    assertWithinHost(review("review-subscription-history").assertHeightIsAtLeast(48.dp))
                    review("review-total-cost-trend").assertHeightIsAtLeast(48.dp).performClick()
                    assertWithinHost(full("total-trend-total").assertTextEquals(moneyMetricDisplay(point.totalRate)))
                    assertWithinHost(full("total-trend-canvas"))
                    full("total-trend-back").assertHeightIsAtLeast(48.dp).performClick()
                    review("total-trend-selected-date").assertTextEquals(selected.toString())
                }
            }
        }
    }

    @Composable
    private fun Trend(state: TotalCostTrendUiState, onSelect: (LocalDate) -> Unit = {}, onBack: () -> Unit = {}) {
        TotalCostTrendScreen(state, onSelect, {}, {}, { _, _ -> }, onBack)
    }

    private fun facts(outlay: Long, recovery: Long = 0, subscription: Boolean = false,
        ended: Boolean = false, start: LocalDate = day): TotalCostTrendUiState {
        val project = ProjectSnapshot(LongTermProject(start, day.takeIf { ended }), buildList {
            add(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(outlay), start))
            if (recovery > 0) add(ProjectMoneyEvent(MoneyDirection.RECOVERY, Money(recovery), day))
        })
        val projects = listOf(OverviewProject("r1-synthetic-project", "r1-project-category", project))
        val subscriptions = if (subscription) {
            listOf(OverviewSubscription("r1-synthetic-subscription", "r1-subscription-category",
                SubscriptionSnapshot(Subscription("r1-synthetic-subscription"), listOf(
                    Renewal(Money(3000), BillingCycle.MONTHLY, day, day, day.plusMonths(1))
                ))))
        } else emptyList()
        val categories = listOf(OverviewCategory("r1-project-category", "合成项目分类", OverviewKind.PROJECT),
            OverviewCategory("r1-subscription-category", "合成订阅分类", OverviewKind.SUBSCRIPTION))
        val ledger = LedgerSnapshot(OverviewSnapshot(categories, projects, subscriptions), listOf(
            TotalCostEvent("r1-one-outlay", start, OverviewKind.PROJECT, projects.single().id,
                "合成长期项目", "项目投入", "初始录入", amountCents = outlay)))
        val timeline = TimelineSampler.sample(ledger, day, basePointBudget = 12)
        return TotalCostTrendUiState(snapshot = ledger, timeline = timeline, selectedDate = day,
            selectedPoint = CostTimelineEngine.point(ledger, day), events = timeline.events.filter { it.date == day })
    }

    private fun assertWithinHost(node: SemanticsNodeInteraction) {
        val item = node.assertIsDisplayed().fetchSemanticsNode()
        val host = compose.onNodeWithTag("r1-layout-host").fetchSemanticsNode()
        assertTrue("Node must start within layout width", item.positionInRoot.x >= host.positionInRoot.x - 1)
        assertTrue("Node must end within layout width",
            item.positionInRoot.x + item.size.width <= host.positionInRoot.x + host.size.width + 1)
    }

    private fun review(tag: String) = scroll("review-screen", tag)
    private fun full(tag: String) = scroll("total-cost-trend-screen", tag)

    private fun scroll(root: String, tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag(root).performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }

    private data class LayoutCase(val appearance: LithicAppearance, val width: Int, val fontScale: Float)
}
