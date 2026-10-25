package com.cooper.lithic.feature.subscription

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.feature.project.SubmissionPhase
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** P01 controlled Compose assertions; compilation is not an Android runtime result. */
@RunWith(AndroidJUnit4::class)
class P01SubscriptionExperienceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cycle_selection_and_reminder_guidance_match_current_product() {
        var state by mutableStateOf(editor())
        compose.setContent { LithicTheme { CreateHost(state, onCycle = { state = state.copy(cycle = it) }) } }

        compose.onNodeWithTag("subscription-cycle-YEARLY").performScrollTo().performClick()
            .assertIsSelected().assertTextContains("已选", substring = true)
        compose.onNodeWithTag("subscription-cycle-selection").performScrollTo()
            .assertTextContains("年付", substring = true)
        compose.onNodeWithTag("subscription-cycle-explanation").performScrollTo()
            .assertTextContains("不代表已经使用", substring = true)
        compose.onNodeWithTag("subscription-effective-explanation").performScrollTo()
            .assertTextContains("预付未来周期", substring = true)
        compose.onNodeWithTag("subscription-reminder-explanation").performScrollTo()
            .assertTextContains("允许系统通知", substring = true)
            .assertTextContains("不保证准点", substring = true)
        compose.onNodeWithText("提醒到期日独立记录；当前仅保存日期，不发送提醒。").assertDoesNotExist()
        assertEquals(BillingCycle.YEARLY, state.cycle)
    }

    @Test fun pending_and_saved_payment_explanations_keep_existing_safe_actions() {
        var state by mutableStateOf(editor().copy(phase = SubmissionPhase.UNCONFIRMED))
        val confirmations = AtomicInteger()
        val reads = AtomicInteger()
        compose.setContent { LithicTheme {
            CreateHost(state, onConfirm = { confirmations.incrementAndGet() }, onRead = { reads.incrementAndGet() })
        } }

        compose.onNodeWithTag("subscription-amount").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("subscription-payment-unconfirmed").performScrollTo()
            .assertTextContains("请勿另建订阅", substring = true)
        compose.onNodeWithTag("subscription-confirm-payment").performScrollTo()
            .assertIsEnabled().assertTextContains("核对并重试同一付款")
        compose.runOnIdle { state = state.copy(phase = SubmissionPhase.COMMITTED) }
        compose.onNodeWithTag("subscription-payment-saved").performScrollTo()
            .assertTextContains("不会再记录一笔付款", substring = true)
        compose.onNodeWithTag("subscription-confirm-payment").assertDoesNotExist()
        compose.onNodeWithTag("subscription-retry-read").performScrollTo().performClick()
        assertEquals(0, confirmations.get())
        assertEquals(1, reads.get())
    }

    @Test fun detail_explains_future_prepaid_time_and_service_provider_boundary() {
        val facts = M09SubscriptionFixtures.facts(futurePayment = true)
        val detail = M09SubscriptionFixtures.detail(facts)
        val future = facts.renewals.single { it.effectiveFrom.isAfter(detail.asOf) }
        compose.setContent { LithicTheme {
            SubscriptionDetailScreen(SubscriptionDetailUiState(detail,
                RenewalHistoryUiState(detail.renewals, detail.renewals.size.toLong(), loading = false), loading = false),
                {}, {}, {}, {})
        } }

        detailNode("subscription-detail-vendor-explanation")
            .assertTextContains("取消服务商自动续费需到服务商处操作", substring = true)
        detailNode("subscription-detail-stop").assertIsEnabled()
        detailNode("subscription-renewal-future-${future.id}").assertTextContains("尚未生效")
        detailNode("subscription-renewal-future-explanation-${future.id}")
            .assertTextContains("不代表已经使用", substring = true)
        assertEquals(facts.renewals.first().id, detail.currentRenewal?.id)
    }

    @Test fun state_confirmation_exposes_selection_and_confirmed_readback() {
        var state by mutableStateOf(SubscriptionStateEditorUiState(serviceName = "合成订阅",
            action = "STOP_AT_END", effectiveOn = "2026-02-01", fixedStopOn = "2026-02-01",
            today = LocalDate.of(2026, 1, 25)))
        val confirms = AtomicInteger()
        val reads = AtomicInteger()
        compose.setContent { LithicTheme {
            SubscriptionStateDialog(state, {}, {}, {}, {}, {},
                { early -> state = state.copy(action = if (early) "STOP_EARLY" else "STOP_AT_END") },
                { confirms.incrementAndGet() }, { reads.incrementAndGet() })
        } }

        compose.onNodeWithTag("subscription-stop-selection").performScrollTo()
            .assertTextContains("到期后停止", substring = true)
        compose.onNodeWithTag("subscription-stop-early").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("subscription-stop-selection").performScrollTo()
            .assertTextContains("提前停止", substring = true)
        compose.runOnIdle { state = state.copy(phase = SubmissionPhase.UNCONFIRMED) }
        compose.onNodeWithTag("subscription-state-unconfirmed").performScrollTo()
            .assertTextContains("不要重新发起另一笔", substring = true)
        compose.runOnIdle { state = state.copy(phase = SubmissionPhase.COMMITTED) }
        compose.onNodeWithTag("subscription-state-readback-status").performScrollTo()
            .assertTextContains("不会再次修改记录", substring = true)
        compose.onNodeWithTag("subscription-state-confirm").assertDoesNotExist()
        compose.onNodeWithTag("subscription-state-retry-read").performScrollTo().performClick()
        assertEquals(0, confirms.get())
        assertEquals(1, reads.get())
    }

    @Test fun verified_block_and_last_payment_suppress_submission_status_and_replay() {
        var state by mutableStateOf(SubscriptionStateEditorUiState(serviceName = "合成订阅",
            kind = SubscriptionStateEditorKind.CANCEL_STOP, phase = SubmissionPhase.COMMITTED,
            verifiedCannotWrite = true))
        val confirms = AtomicInteger()
        compose.setContent { LithicTheme {
            SubscriptionStateDialog(state, {}, {}, {}, {}, {}, {}, { confirms.incrementAndGet() }, {})
        } }
        fun assertBlocked() {
            compose.onNodeWithTag("subscription-state-unconfirmed").assertDoesNotExist()
            compose.onNodeWithTag("subscription-state-readback-status").assertDoesNotExist()
            compose.onNodeWithTag("subscription-state-confirm").assertDoesNotExist()
            compose.onNodeWithTag("subscription-state-retry-read").assertDoesNotExist()
            compose.onNodeWithText("已有记录保持原样，请返回后核对。").assertExists()
        }
        assertBlocked()
        compose.runOnIdle { state = state.copy(phase = SubmissionPhase.UNCONFIRMED) }
        assertBlocked()
        compose.runOnIdle { state = state.copy(phase = SubmissionPhase.COMMITTED,
            verifiedCannotWrite = false, lastPayment = true) }
        assertBlocked()
        assertEquals(0, confirms.get())
    }

    private fun detailNode(tag: String): androidx.compose.ui.test.SemanticsNodeInteraction {
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }

    @Composable
    private fun CreateHost(state: SubscriptionEditorUiState, onCycle: (BillingCycle) -> Unit = {},
        onConfirm: () -> Unit = {}, onRead: () -> Unit = {}) {
        SubscriptionEditorScreen(state, {}, {}, {}, {}, {}, onCycle, {}, {}, {}, {}, onConfirm, onRead)
    }

    private fun editor() = SubscriptionEditorUiState(serviceName = "合成订阅", categoryId = "other",
        amount = "150", paidOn = "2026-01-01", effectiveFrom = "2026-01-01",
        paidUntilExclusive = "2026-02-01", reminderDueOn = "2026-01-29",
        today = LocalDate.of(2026, 1, 25), categories = listOf(
            CategoryEntity("other", CategoryType.SUBSCRIPTION, "其他", "其他", true)))
}
