package com.cooper.lithic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.ChartEvent
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.report.ProjectTimeline
import com.cooper.lithic.domain.report.ProjectTimelineEngine
import com.cooper.lithic.domain.report.ProjectTimelinePoint
import com.cooper.lithic.domain.report.SubscriptionTimelineEngine
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import com.cooper.lithic.feature.project.ProjectCostChart
import com.cooper.lithic.feature.subscription.SubscriptionPriceChart
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated parameter pages; no MainActivity, database, real TalkBack or system font change. */
@RunWith(AndroidJUnit4::class)
class ChartSemanticsTest {
    @get:Rule val compose = createComposeRule()
    private val start = LocalDate.of(2026, 1, 1)
    private val today = start.plusDays(19)

    @Test fun date_input_selects_exact_date_without_drag_and_rejects_future() {
        var requested: LocalDate? = null
        project(onSelect = { requested = it })
        scroll("chart-date-toggle"); compose.onNodeWithTag("chart-date-toggle").performClick()
        scroll("chart-date-input")
        compose.onNodeWithTag("chart-date-input").performTextReplacement("2026-01-07")
        scroll("chart-date-apply")
        compose.onNodeWithTag("chart-date-apply").performClick()
        compose.runOnIdle { assertEquals(start.plusDays(6), requested) }
        scroll("chart-date-input")
        compose.onNodeWithTag("chart-date-input").performTextReplacement("2027-01-01")
        scroll("chart-date-apply")
        compose.onNodeWithTag("chart-date-apply").performClick()
        compose.onNodeWithText("不能查看业务今天之后的历史日期。", useUnmergedTree = true).assertExists()
        compose.runOnIdle { assertEquals(start.plusDays(6), requested) }
    }

    @Test fun same_day_complete_events_are_reachable_beyond_fifty_and_non_cash_is_explicit() {
        val events = (0..50).map { ChartEvent("event-$it", today, "同日事实$it", "完整说明$it", 100, nonCash = it == 50) }
        project(events = events)
        scroll("chart-events-toggle"); compose.onNodeWithTag("chart-events-toggle").performClick()
        scroll("chart-show-selected-events")
        compose.onNodeWithTag("chart-show-selected-events").performClick()
        scroll("chart-events-more")
        compose.onNodeWithTag("chart-events-more").performClick()
        scroll("chart-event-event-50")
        compose.onNodeWithTag("chart-event-event-50").assertIsDisplayed()
        compose.onNodeWithText("非现金折抵（不是现金扣款）", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun negative_subcent_result_retains_accessible_value_and_no_rounded_zero() {
        val input = ProjectSnapshot(LongTermProject(start), listOf(ProjectMoneyEvent(MoneyDirection.RECOVERY, Money(1), start)))
        val timeline = ProjectTimelineEngine.build(input, today)
        project(timeline = timeline, selected = ProjectTimelineEngine.point(input, today, today))
        compose.onNodeWithTag("chart-selected-value").assertContentDescriptionEquals("项目日均成本历史，负值，绝对值小于0.01元/天")
        compose.onNodeWithTag("chart-selected-value").assertTextContains("−<¥0.01")
        compose.onNodeWithTag("cost-chart-canvas").assertExists()
    }

    @Test fun paused_subscription_keeps_zero_reason_and_future_plan_is_read_only() {
        val selections = AtomicInteger()
        val input = SubscriptionSnapshot(Subscription("s"), listOf(Renewal(Money(15000), BillingCycle.MONTHLY,
            start, start, start.plusMonths(1))), listOf(SubscriptionStateChange(today, SubscriptionOperationState.PAUSED)))
        val future = today.plusDays(2)
        compose.setContent {
            LithicTheme {
                SubscriptionPriceChart("合成订阅", SubscriptionTimelineEngine.build(input, today),
                    SubscriptionTimelineEngine.point(input, today, today),
                    listOf(ChartEvent("future", future, "计划停止", "尚未生效")), false, null, false,
                    { selections.incrementAndGet() }, {}, {})
            }
        }
        compose.onNodeWithTag("chart-selected-value").assertContentDescriptionEquals("标准日均价格历史，0.00元/天")
        compose.onNodeWithTag("chart-selected-status").assertTextContains("暂停", substring = true)
        scroll("chart-events-toggle", subscription = true); compose.onNodeWithTag("chart-events-toggle").performClick()
        scroll("chart-event-day-$future", subscription = true)
        compose.onNodeWithTag("chart-event-day-$future").performClick()
        scroll("chart-event-future", subscription = true)
        compose.onNodeWithText("尚未生效的计划，不属于历史曲线。", useUnmergedTree = true).assertExists()
        compose.runOnIdle { assertEquals(0, selections.get()) }
    }

    @Test fun read_failure_and_missing_are_not_fake_zero_or_empty_chart() {
        compose.setContent { LithicTheme {
            ProjectCostChart("读取失败", null, null, emptyList(), false, "合成读取异常", false, {}, {}, {})
        } }
        compose.onNodeWithTag("chart-error").assertExists()
        compose.onNodeWithTag("chart-selected-value").assertDoesNotExist()
        compose.onNodeWithTag("cost-chart-canvas").assertDoesNotExist()
    }

    @Test fun narrow_double_font_page_keeps_back_date_and_full_events_reachable() {
        val backs = AtomicInteger()
        val input = normalInput()
        val timeline = ProjectTimelineEngine.build(input, today)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                LithicTheme { Surface(Modifier.fillMaxSize()) { Box(Modifier.width(360.dp)) {
                    ProjectCostChart("用于验证窄屏大字体的很长项目名称", timeline,
                        ProjectTimelineEngine.point(input, today, today),
                        listOf(ChartEvent("narrow", today, "真实事件含义", "保留完整事件说明")),
                        false, null, false, {}, {}, { backs.incrementAndGet() })
                } } }
            }
        }
        scroll("chart-date-toggle"); compose.onNodeWithTag("chart-date-toggle").performClick()
        scroll("chart-date-apply")
        compose.onNodeWithTag("chart-date-apply").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        scroll("chart-events-toggle"); compose.onNodeWithTag("chart-events-toggle").performClick()
        scroll("chart-event-day-$today")
        compose.onNodeWithTag("chart-event-day-$today").assertIsDisplayed().assertHasClickAction().performClick()
        scroll("chart-event-narrow")
        compose.onNodeWithTag("chart-event-narrow").assertIsDisplayed()
        scroll("chart-back")
        compose.onNodeWithTag("chart-back").assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, backs.get()) }
    }

    private fun normalInput() = ProjectSnapshot(LongTermProject(start),
        listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(10000), start)))

    private fun project(
        timeline: ProjectTimeline = ProjectTimelineEngine.build(normalInput(), today),
        selected: ProjectTimelinePoint = ProjectTimelineEngine.point(normalInput(), today, today),
        events: List<ChartEvent> = emptyList(),
        onSelect: (LocalDate) -> Unit = {},
    ) {
        compose.setContent { LithicTheme {
            ProjectCostChart("合成长期项目", timeline, selected, events, false, null, false, onSelect, {}, {})
        } }
    }

    private fun scroll(tag: String, subscription: Boolean = false) {
        compose.onNodeWithTag(if (subscription) "subscription-price-chart-screen" else "project-cost-chart-screen")
            .performScrollToNode(hasTestTag(tag))
    }
}
