package com.cooper.lithic.feature.subscription

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** M09 Phase A: real screens must expose the missing lifecycle and correction operations. */
@RunWith(AndroidJUnit4::class)
class M09SubscriptionScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun active_detail_exposes_pause_and_stop_record_operations() {
        renderDetail(M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts()))
        detailNode("subscription-detail-pause").assertIsDisplayed().assertIsEnabled()
        detailNode("subscription-detail-stop").assertIsDisplayed().assertIsEnabled()
    }

    @Test fun paused_detail_offers_resume_without_hiding_paid_history() {
        val detail = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts(operation = SubscriptionOperationState.PAUSED))
        assertEquals(SubscriptionDisplayStatus.PAUSED, detail.cost.displayStatus)
        renderDetail(detail)
        detailNode("subscription-detail-resume").assertIsDisplayed().assertIsEnabled()
        detailNode("subscription-renewal-${detail.renewals.single().id}").assertIsDisplayed()
        compose.onNodeWithTag("subscription-detail-pause").assertDoesNotExist()
    }

    @Test fun paused_expired_coverage_keeps_paused_status_and_explains_expiry() {
        val detail = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts(operation = SubscriptionOperationState.PAUSED),
            LocalDate.of(2026, 2, 2))
        assertEquals(SubscriptionDisplayStatus.PAUSED, detail.cost.displayStatus)
        renderDetail(detail)
        detailNode("subscription-detail-status").assertTextContains("已暂停")
        detailNode("subscription-detail-paused-expired").assertTextContains("已到期", substring = true)
        detailNode("subscription-detail-resume").assertIsEnabled()
    }

    @Test fun stopped_detail_exposes_same_identity_reopen_and_hides_ordinary_renewal() {
        val detail = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts(operation = SubscriptionOperationState.STOPPED))
        renderDetail(detail)
        detailNode("subscription-detail-reopen").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("subscription-detail-renewal").assertDoesNotExist()
        detailNode("subscription-detail-purchased-days").assertTextContains("30天", substring = true)
    }

    @Test fun future_stop_shows_fixed_date_and_explicit_correct_cancel_entries() {
        val detail = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts(operation = SubscriptionOperationState.STOPPED,
            stateOn = LocalDate.of(2026, 3, 1), futurePayment = true))
        assertEquals(SubscriptionDisplayStatus.ACTIVE, detail.cost.displayStatus)
        renderDetail(detail)
        detailNode("subscription-detail-scheduled-stop").assertTextContains("2026-03-01", substring = true)
        detailNode("subscription-detail-correct-stop").assertIsEnabled()
        detailNode("subscription-detail-cancel-stop").assertIsEnabled()
    }

    @Test fun payment_history_has_per_fact_correction_and_delete_error_entries() {
        val detail = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts(futurePayment = true))
        renderDetail(detail)
        detail.renewals.forEach { renewal ->
            detailNode("subscription-renewal-correct-${renewal.id}").assertIsDisplayed().assertIsEnabled()
            detailNode("subscription-renewal-delete-${renewal.id}").assertIsDisplayed().assertIsEnabled()
        }
    }

    @Test fun current_management_has_independent_active_paused_expired_groups() {
        val active = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts("订阅中的服务"))
        val paused = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts("暂停的服务", SubscriptionOperationState.PAUSED))
        val expired = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts("到期的服务"), LocalDate.of(2026, 2, 2))
        compose.setContent { LithicTheme {
            SubscriptionListScreen(SubscriptionListUiState(listOf(active, paused, expired), 3, loading = false), {}, {}, {}, {}, {})
        } }
        listNode("subscription-group-ACTIVE").assertTextContains("订阅中")
        listNode("subscription-row-${active.subscription.id}").assertIsDisplayed()
        listNode("subscription-group-PAUSED").assertTextContains("暂停中")
        listNode("subscription-row-${paused.subscription.id}").assertIsDisplayed()
        listNode("subscription-group-EXPIRED").assertTextContains("已到期")
        listNode("subscription-row-${expired.subscription.id}").assertIsDisplayed()
    }

    private fun renderDetail(detail: SubscriptionDetail) {
        compose.setContent { LithicTheme {
            SubscriptionDetailScreen(SubscriptionDetailUiState(detail,
                RenewalHistoryUiState(detail.renewals, detail.renewals.size.toLong(), loading = false), loading = false), {}, {}, {}, {})
        } }
    }

    private fun detailNode(tag: String): androidx.compose.ui.test.SemanticsNodeInteraction {
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }

    private fun listNode(tag: String): androidx.compose.ui.test.SemanticsNodeInteraction {
        compose.onNodeWithTag("subscription-list-content").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }
}
