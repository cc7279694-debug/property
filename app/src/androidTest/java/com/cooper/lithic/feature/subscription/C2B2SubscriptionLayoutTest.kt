package com.cooper.lithic.feature.subscription

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.feature.project.SubmissionPhase
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
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
        compose.onNodeWithTag("subscription-detail-purchased-days").assertExists()
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
        compose.onNodeWithTag("subscription-detail-close-management").performClick()
        assertEquals(0, writes.get())
    }
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
