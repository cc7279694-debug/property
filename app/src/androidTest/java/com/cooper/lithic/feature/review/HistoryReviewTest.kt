package com.cooper.lithic.feature.review

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.ReviewRankItem
import com.cooper.lithic.data.repository.ReviewRanking
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.report.RankedProject
import com.cooper.lithic.domain.report.RankedSubscription
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated ComponentActivity + synthetic facts evaluated by the frozen engines; not MainActivity. */
@RunWith(AndroidJUnit4::class)
class HistoryReviewTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val today = LocalDate.of(2026, 10, 21)

    @Test fun current_final_labels_and_zero_negative_results_open_the_exact_project() {
        val current = project("current", "仍在陪伴的八年电脑", today.minusYears(8), null, 0, 0)
        val ended = project("ended", "已结束的相机", today.minusDays(99), today.minusDays(50), 10_000, 15_000)
        var opened: String? = null
        show { LongTermReviewScreen(state(listOf(current, ended)), {}, {}, {}, {}, { opened = it }, {}) }
        row("current")
        compose.onNodeWithTag("review-status-current", true).assertTextEquals("持有中")
        compose.onNodeWithTag("review-cost-label-current", true).assertTextEquals("当前日均成本")
        capture("project-current-mixed")
        compose.onNodeWithTag("review-row-current").performClick()
        assertEquals("current", opened)
        row("ended")
        compose.onNodeWithTag("review-status-ended", true).assertTextEquals("已结束")
        compose.onNodeWithTag("review-cost-label-ended", true).assertTextEquals("已结束；结果截至今天")
        compose.onNodeWithTag("review-time-ended", true).assertTextEquals("持有 50 天")
        capture("project-ended-negative")
        compose.onNodeWithTag("review-row-ended").performClick()
        assertEquals("ended", opened)
        assertEquals(0, current.project!!.cost.dailyRate!!.compareTo(BigDecimal.ZERO))
        assertTrue(ended.project!!.cost.dailyRate!!.signum() < 0)
    }

    @Test fun subscription_time_label_is_standard_not_elapsed_and_average_uses_real_payments() {
        val snapshot = SubscriptionSnapshot(Subscription("annual"), listOf(Renewal(Money(365_000),
            BillingCycle.YEARLY, today, today, today.plusYears(1))))
        val cost = SubscriptionCostEngine.evaluate(snapshot, today)
        assertEquals(1L, cost.elapsedPaidDays)
        assertEquals(365L, cost.standardPurchasedDays)
        assertEquals(0, BigDecimal.TEN.compareTo(cost.historicalAverage))
        val item = ReviewRankItem(CategoryType.SUBSCRIPTION, "annual", "年度数字工具", "数字服务",
            subscription = RankedSubscription("annual", "digital", cost))
        val pausedCost = SubscriptionCostEngine.evaluate(snapshot.copy(subscription = Subscription("paused"),
            stateChanges = listOf(SubscriptionStateChange(today, SubscriptionOperationState.PAUSED))), today)
        assertEquals(0, BigDecimal.ZERO.compareTo(pausedCost.currentRate))
        val paused = ReviewRankItem(CategoryType.SUBSCRIPTION, "paused", "暂停中的年度工具", "数字服务",
            subscription = RankedSubscription("paused", "digital", pausedCost))
        var opened: String? = null
        show { LongTermReviewScreen(state(listOf(item, paused), ReviewRanking.SUBSCRIPTION_TIME), {}, {}, {}, {}, {}, { opened = it }) }
        row("annual")
        compose.onNodeWithTag("review-time-annual", true).assertTextEquals("折算付费时长：365 天")
        compose.onNodeWithTag("review-cost-label-annual", true).assertTextEquals("历史平均日成本")
        compose.onNodeWithTag("review-status-annual", true).assertTextEquals("订阅中")
        capture("subscription-standard-duration")
        compose.onNodeWithTag("review-row-annual").performClick()
        assertEquals("annual", opened)
        row("paused")
        compose.onNodeWithTag("review-status-paused", true).assertTextEquals("已暂停")
        compose.onNodeWithTag("review-rate-paused", true).assertContentDescriptionEquals("10.00元/天")
    }

    @Test fun unread_failure_empty_and_unavailable_are_distinct_without_fake_zero() {
        val view = mutableStateOf(ReviewUiState(error = "读取失败"))
        var retried = false
        show { LongTermReviewScreen(view.value, {}, {}, { retried = true }, {}, {}, {}) }
        compose.onNodeWithTag("review-ranks-error").assertTextEquals("读取失败")
        compose.onNodeWithTag("review-ranks-empty").assertDoesNotExist()
        compose.onNodeWithTag("review-ranks-retry").performClick()
        assertTrue(retried)
        compose.runOnIdle { view.value = state(emptyList()) }
        compose.onNodeWithTag("review-ranks-empty").assertTextEquals("暂无长期项目记录")
        val future = project("future", "尚未开始的项目", today.plusDays(1), null, 0, 0)
        compose.runOnIdle { view.value = state(listOf(future)).copy(rankableCount = 0) }
        row("future")
        compose.onNodeWithTag("review-name-future", true).assertTextEquals("未排名 · 尚未开始的项目")
        compose.onNodeWithTag("review-status-future", true).assertTextEquals("尚未开始")
        compose.onNodeWithTag("review-cost-label-future", true).assertTextEquals("暂无可计算日均成本")
        assertNull(future.project!!.cost.dailyRate)
    }

    @Test fun narrow_200percent_test_host_keeps_switches_back_and_rows_reachable() {
        val project = project("large", "长期陪伴且名称很长的便携电脑", today.minusYears(8), null, 800_000, 0)
        var selected: ReviewRanking? = null
        var backed = false
        show(dark = true, fontScale = 2f) {
            LongTermReviewScreen(state(listOf(project)), { backed = true }, { selected = it }, {}, {}, {}, {})
        }
        compose.onNodeWithTag("review-ranks-screen").performScrollToNode(hasTestTag("review-rank-PROJECT_COST"))
        compose.onNodeWithTag("review-rank-PROJECT_COST").assertIsDisplayed().performClick()
        assertEquals(ReviewRanking.PROJECT_COST, selected)
        row("large")
        compose.onNodeWithTag("review-time-large", true).assertIsDisplayed()
        compose.onNodeWithTag("review-rate-large", true).performScrollTo().assertIsDisplayed()
        capture("project-360dp-200percent")
        compose.onNodeWithTag("review-ranks-screen").performScrollToNode(hasTestTag("review-ranks-back"))
        compose.onNodeWithTag("review-ranks-back").assertIsDisplayed().performClick()
        assertTrue(backed)
    }

    @Test fun review_shell_separates_cross_status_review_and_two_exact_archives() {
        val opened = mutableListOf<String>()
        show { ReviewShell(2, onProjectHistory = { opened += "projects" },
            onSubscriptionHistory = { opened += "subscriptions" }, onLongTermReview = { opened += "review" }) }
        compose.onNodeWithTag("review-long-term").assertIsDisplayed().performClick()
        compose.onNodeWithTag("review-project-history").assertIsDisplayed().performClick()
        compose.onNodeWithTag("review-subscription-history").performScrollTo()
        compose.onNodeWithTag("review-subscription-history").assertIsDisplayed().performClick()
        assertEquals(listOf("review", "projects", "subscriptions"), opened)
        capture("history-entry")
    }

    private fun state(items: List<ReviewRankItem>, ranking: ReviewRanking = ReviewRanking.PROJECT_TIME) =
        ReviewUiState(ranking, items, items.size.toLong(), items.size.toLong(), today)

    private fun project(id: String, name: String, start: LocalDate, end: LocalDate?, outlay: Long, recovery: Long): ReviewRankItem {
        val facts = ProjectSnapshot(LongTermProject(start, end), listOf(
            ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(outlay), start),
            ProjectMoneyEvent(MoneyDirection.RECOVERY, Money(recovery), today)))
        val result = RankedProject(id, "devices", ProjectCostEngine.evaluate(facts, today), end != null && !end.isAfter(today))
        return ReviewRankItem(CategoryType.PROJECT, id, name, "设备", result)
    }

    private fun row(id: String) {
        compose.onNodeWithTag("review-ranks-screen").performScrollToNode(hasTestTag("review-row-$id"))
        compose.onNodeWithTag("review-row-$id").assertIsDisplayed()
    }

    private fun show(dark: Boolean = false, fontScale: Float = 1f, content: @Composable () -> Unit) {
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize().testTag("m15-ui-test-viewport")) {
                val canvasDensity = constraints.maxWidth / 360f
                val canvasHeight = minOf(640f, constraints.maxHeight / canvasDensity).dp
                CompositionLocalProvider(LocalDensity provides Density(canvasDensity, fontScale)) {
                    LithicTheme(darkTheme = dark) {
                        Box(Modifier.requiredSize(360.dp, canvasHeight).testTag("m15-ui-test-canvas")) {
                            Surface(Modifier.fillMaxSize()) { content() }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        assertEquals(ComponentActivity::class.java, compose.activity.javaClass)
        compose.waitForIdle()
        val viewport = compose.onNodeWithTag("m15-ui-test-viewport").fetchSemanticsNode()
        val canvas = compose.onNodeWithTag("m15-ui-test-canvas").fetchSemanticsNode()
        assertTrue("360dp canvas exceeds the visible viewport", canvas.positionInRoot.x >= viewport.positionInRoot.x - .5f &&
            canvas.positionInRoot.x + canvas.size.width <= viewport.positionInRoot.x + viewport.size.width + .5f)
        val bitmap = compose.onNodeWithTag("m15-ui-test-canvas").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = context.filesDir.canonicalFile
        val directory = File(root, "m15-evidence")
        check(directory.canonicalFile.parentFile == root && directory.absoluteFile == directory.canonicalFile)
        check(directory.isDirectory || directory.mkdir())
        val file = File(directory, "$name-${UUID.randomUUID()}.png")
        check(file.createNewFile())
        FileOutputStream(file).use { output -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); output.fd.sync() }
        check(file.length() > 0)
        println("M15_COMPONENT_ACTIVITY_SCREENSHOT=${file.absolutePath}")
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
            putString("M15_COMPONENT_ACTIVITY_SCREENSHOT", file.absolutePath)
        })
    }
}
