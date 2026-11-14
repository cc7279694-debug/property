package com.cooper.lithic.feature.subscription

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.design.moneyMetricDisplay
import com.cooper.lithic.feature.appearance.LithicAppearance
import com.cooper.lithic.feature.project.SubmissionPhase
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Compile-only until separately authorized Android execution. */
class C2B2SubscriptionLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test fun archive_separates_historical_average_and_current_plan_with_one_renewal_action() {
        val facts = M09SubscriptionFixtures.facts(futurePayment = true)
        val detail = M09SubscriptionFixtures.detail(facts)
        compose.setContent { LithicTheme {
            SubscriptionDetailScreen(SubscriptionDetailUiState(detail,
                RenewalHistoryUiState(detail.renewals, detail.renewals.size.toLong(), loading = false), loading = false), {}, {}, {}, {})
        } }
        compose.onNodeWithTag("subscription-detail-historical-average").assertExists()
        compose.onNodeWithTag("subscription-detail-purchased-days").assertDoesNotExist()
        scroll("subscription-detail-current-plan").performClick()
        compose.onNodeWithTag("subscription-detail-current-rate").assertTextEquals(moneyMetricDisplay(detail.cost.currentRate))
        compose.onNodeWithText("实际生效日期：2026-01-01").assertIsDisplayed()
        compose.onNodeWithText("付费到期边界：2026-02-01（当天不包含）").assertIsDisplayed()
        compose.onNodeWithTag("subscription-detail-plan-close").performClick()
        scroll("subscription-detail-next-plan").performClick()
        compose.onNodeWithText("已付款，尚未生效。").assertIsDisplayed()
        compose.onNodeWithText("实际生效日期：2026-02-01").assertIsDisplayed()
        compose.onNodeWithTag("subscription-detail-plan-close").performClick()
        scroll("subscription-detail-more").performClick()
        scroll("subscription-detail-purchased-days").assertTextContains("60天", substring = true)
        scroll("subscription-detail-total-paid").assertTextEquals("¥300.00")
        compose.onNodeWithTag("subscription-detail-renewal").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("subscription-detail-open-management").performClick()
        compose.onNodeWithTag("subscription-detail-vendor-explanation").assertTextContains("服务商", substring = true)
        compose.onNodeWithTag("subscription-detail-stop").assertExists()
    }
    @Test fun closing_management_does_not_invoke_a_business_operation() {
        val facts = M09SubscriptionFixtures.facts()
        val detail = M09SubscriptionFixtures.detail(facts)
        val writes = AtomicInteger()
        compose.setContent { LithicTheme {
            SubscriptionDetailScreen(SubscriptionDetailUiState(detail, loading = false), {}, {}, {}, {}, onPause = { writes.incrementAndGet() })
        } }
        compose.onNodeWithTag("subscription-detail-open-management").performClick()
        pressBack()
        compose.onNodeWithTag("subscription-detail-close-management").assertDoesNotExist()
        compose.onNodeWithTag("subscription-detail-screen").assertIsDisplayed()
        assertEquals(0, writes.get())
    }

    @Test fun loaded_payments_keep_their_order_and_page_only_on_explicit_request() {
        val facts = M09SubscriptionFixtures.facts(futurePayment = true)
        val current = facts.renewals.first()
        val future = facts.renewals.last()
        val older = (8L..9L).map { month ->
            val from = current.effectiveFrom.minusMonths(month)
            current.copy(id = UUID(0, month).toString(), operationId = UUID(0, month + 20).toString(),
                paidOn = from, effectiveFrom = from, paidUntilExclusive = from.plusMonths(1),
                reminderDueOn = from.plusDays(20))
        }.sortedByDescending { it.paidOn }
        val sameDayFirst = future.copy(id = "00000000-0000-4000-8000-000000000201", operationId = "00000000-0000-4000-8000-000000000203",
            effectiveFrom = future.paidUntilExclusive, paidUntilExclusive = future.paidUntilExclusive.plusMonths(1),
            reminderDueOn = future.paidUntilExclusive.plusDays(20))
        val sameDaySecond = future.copy(id = "00000000-0000-4000-8000-000000000202")
        val ordered = listOf(sameDayFirst, sameDaySecond, current) + older
        val reads = AtomicInteger()
        val operations = AtomicInteger()
        val history = mutableStateOf(RenewalHistoryUiState(ordered.take(4), 5, loading = false, hasMore = true))
        val detail = M09SubscriptionFixtures.detail(facts.copy(renewals = ordered))
        compose.setContent { LithicTheme {
            SubscriptionDetailScreen(SubscriptionDetailUiState(detail, history.value, loading = false),
                {}, {}, {
                    reads.incrementAndGet()
                    history.value = history.value.copy(items = ordered, hasMore = false)
                }, {}, onCorrectRenewal = { operations.incrementAndGet() }, onDeleteRenewal = { operations.incrementAndGet() })
        } }
        scroll("subscription-detail-payment-count", useUnmergedTree = true).assertTextEquals("5 笔")
        assertRowOrder(ordered.take(3).map { it.id })
        compose.onNodeWithTag("subscription-renewal-${ordered[3].id}").assertDoesNotExist()
        scroll("subscription-detail-show-payments").assertTextContains("全部").performClick().assertTextContains("收起")
        scroll("subscription-renewal-${ordered[3].id}").assertIsDisplayed()
        assertEquals(0, reads.get())
        scroll("subscription-detail-load-more-history").performClick()
        scroll("subscription-renewal-${ordered[4].id}").assertIsDisplayed()
        assertEquals(1, reads.get())
        scroll("subscription-detail-show-payments").performClick().assertTextContains("全部")
        compose.onNodeWithTag("subscription-renewal-${ordered[3].id}").assertDoesNotExist()
        assertRowOrder(ordered.take(3).map { it.id })
        assertEquals(1, reads.get())
        assertEquals(0, operations.get())
    }

    @Test fun three_themes_keep_long_identity_and_renewal_accessible_at_simulated_large_font() {
        val detail = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts("合成阅读云存储资料与全家共享长期订阅"))
        val selected = mutableStateOf(LithicAppearance.PURE_WHITE)
        val actions = AtomicInteger()
        compose.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(currentDensity.density, fontScale = 2f)) {
                LithicTheme(selected.value) {
                    Box(Modifier.width(360.dp).fillMaxHeight()) {
                        SubscriptionDetailScreen(SubscriptionDetailUiState(detail,
                            RenewalHistoryUiState(detail.renewals, 1, loading = false), loading = false),
                            {}, { actions.incrementAndGet() }, {}, {})
                    }
                }
            }
        }
        for (theme in LithicAppearance.entries) {
            compose.runOnIdle { selected.value = theme }
            scroll("subscription-detail-name").assertTextEquals(detail.subscription.name)
            scroll("subscription-detail-status").assertTextContains("订阅中")
            scroll("subscription-detail-historical-average").assertTextEquals(moneyMetricDisplay(detail.cost.historicalAverage))
            scroll("subscription-renewal-actions-${detail.renewals.single().id}").assertIsDisplayed()
            compose.onNodeWithTag("subscription-detail-show-payments").assertDoesNotExist()
            compose.onNodeWithTag("subscription-detail-next-plan").assertDoesNotExist()
            val viewport = compose.onNodeWithTag("subscription-detail-content").fetchSemanticsNode().boundsInRoot
            val action = compose.onNodeWithTag("subscription-detail-renewal").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            assertTrue(viewport.bottom <= action.fetchSemanticsNode().boundsInRoot.top + 1f)
        }
        compose.onNodeWithTag("subscription-detail-renewal").performClick()
        assertEquals(1, actions.get())
    }

    private fun assertRowOrder(ids: List<String>) {
        scroll("subscription-renewal-${ids.last()}").assertIsDisplayed()
        val rows = ids.map { compose.onNodeWithTag("subscription-renewal-$it").fetchSemanticsNode().positionInRoot.y }
        assertTrue("Payment rows must retain input order", rows.zipWithNext().all { (first, next) -> first < next })
    }

    private fun scroll(tag: String, useUnmergedTree: Boolean = false) =
        compose.onNodeWithTag("subscription-detail-content", useUnmergedTree).performScrollToNode(hasTestTag(tag))
            .let { compose.onNodeWithTag(tag, useUnmergedTree) }
    @Test fun unknown_payment_keeps_single_retry_and_blocks_input() {
        compose.setContent { LithicTheme {
            SubscriptionEditorScreen(SubscriptionEditorUiState(phase = SubmissionPhase.UNCONFIRMED),
                onBack = {}, onNameChange = {}, onCategoryChange = {}, onNoteChange = {}, onAmountChange = {},
                onCycleChange = {}, onPaidOnChange = {}, onEffectiveFromChange = {}, onPaidUntilChange = {},
                onReminderDueChange = {}, onConfirmPayment = {}, onRetryRead = {})
        } }
        compose.onNodeWithTag("subscription-confirm-payment").assertIsDisplayed().assertTextContains("同一付款", substring = true)
        compose.onNodeWithTag("subscription-payment-unconfirmed").assertExists()
        compose.onNodeWithTag("subscription-name").assertIsNotEnabled()
    }
    @Test fun deleting_last_payment_retains_the_readonly_protection() {
        compose.setContent { LithicTheme {
            SubscriptionStateDialog(SubscriptionStateEditorUiState(kind = SubscriptionStateEditorKind.DELETE_RENEWAL, lastPayment = true), {}, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("subscription-state-confirm").assertDoesNotExist()
        compose.onNodeWithTag("subscription-state-readonly-notice").assertExists()
    }
}
