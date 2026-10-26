package com.cooper.lithic.feature.subscription

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso
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

/** Controlled rendering tests; actual Room and navigation journeys are a separate suite. */
@RunWith(AndroidJUnit4::class)
class SubscriptionScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun create_inputs_are_controlled_and_subscription_category_only() {
        var state by mutableStateOf(editor())
        val confirmations = AtomicInteger()
        compose.setContent { LithicTheme { CreateHost(state, { state = it }, { confirmations.incrementAndGet() }) } }
        compose.onNodeWithTag("subscription-name").performScrollTo().performTextReplacement("真实服务")
        compose.onNodeWithTag("subscription-amount").performScrollTo().performTextReplacement("146.83")
        compose.onNodeWithTag("subscription-cycle-YEARLY").performScrollTo().performClick().assertIsSelected()
        changeDate("subscription-paid-on-input", "2026-01-05")
        changeDate("subscription-effective-from-input", "2026-02-01")
        changeDate("subscription-paid-until-input", "2027-02-01")
        changeDate("subscription-reminder-due-input", "2027-01-28")
        compose.onNodeWithTag("subscription-category").performScrollTo().performClick()
        compose.onNodeWithText("项目专属类别").assertDoesNotExist()
        compose.onNodeWithTag("subscription-category-$SECOND_CATEGORY").performClick()
        compose.onNodeWithTag("subscription-note").performScrollTo().performTextReplacement("保留的真实备注")
        assertEquals("真实服务", state.serviceName)
        assertEquals("146.83", state.amount)
        assertEquals(BillingCycle.YEARLY, state.cycle)
        assertEquals("2026-01-05", state.paidOn)
        assertEquals("2026-02-01", state.effectiveFrom)
        assertEquals("2027-02-01", state.paidUntilExclusive)
        assertEquals("2027-01-28", state.reminderDueOn)
        assertEquals(SECOND_CATEGORY, state.categoryId)
        assertEquals("保留的真实备注", state.note)
        assertEquals(0, confirmations.get())
        compose.onNodeWithTag("subscription-confirm-payment")
            .assertTextContains("确认已付款并创建订阅").performClick()
        assertEquals(1, confirmations.get())
    }

    @Test fun renewal_requires_explicit_payment_action_and_keeps_reference_readonly() {
        var state by mutableStateOf(editor().copy(kind = SubscriptionEditorKind.RENEWAL))
        val confirmations = AtomicInteger()
        compose.setContent { LithicTheme { RenewalHost(state, { state = it }, { confirmations.incrementAndGet() }) } }
        compose.onNodeWithTag("renewal-service-name").assertTextContains("测试服务")
        compose.onNodeWithTag("subscription-name").assertDoesNotExist()
        compose.onNodeWithTag("subscription-category").assertDoesNotExist()
        compose.onNodeWithText("前次金额和周期仅供参考，请核对本次实际付款及四个日期。")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("renewal-amount").performScrollTo().performTextReplacement("148.10")
        assertEquals("148.10", state.amount)
        assertEquals(0, confirmations.get())
        Espresso.closeSoftKeyboard(); compose.waitForIdle()
        compose.onNodeWithTag("renewal-confirm-payment")
            .assertIsDisplayed().assertIsEnabled().assertTextContains("确认这笔真实付款").performClick()
        compose.waitForIdle()
        assertEquals(1, confirmations.get())
    }

    @Test fun failed_form_retains_invalid_raw_and_all_four_dates() {
        val amountError = "金额最多保留两位小数"
        val state = editor().copy(amount = "1.234", cycle = BillingCycle.YEARLY,
            paidOn = "2026-01-05", effectiveFrom = "2026-02-01",
            paidUntilExclusive = "2027-02-01", reminderDueOn = "2027-01-28",
            errors = mapOf("amount" to amountError), message = "本地保存失败，输入已保留")
        compose.setContent { LithicTheme { CreateHost(state) } }
        compose.onNodeWithTag("subscription-amount").performScrollTo().assertTextContains("1.234")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, amountError))
        compose.onNodeWithTag("subscription-cycle-YEARLY").performScrollTo().assertIsSelected()
        assertDate("subscription-paid-on-input", "2026-01-05")
        assertDate("subscription-effective-from-input", "2026-02-01")
        assertDate("subscription-paid-until-input", "2027-02-01")
        assertDate("subscription-reminder-due-input", "2027-01-28")
        compose.onNodeWithTag("subscription-form-error").assertTextContains("输入已保留", substring = true)
    }

    @Test fun in_flight_form_locks_inputs_and_duplicate_confirm() {
        val confirmations = AtomicInteger()
        compose.setContent { LithicTheme {
            CreateHost(editor().copy(phase = SubmissionPhase.IN_FLIGHT), onConfirm = { confirmations.incrementAndGet() })
        } }
        compose.onNodeWithTag("subscription-name").assertIsNotEnabled()
        compose.onNodeWithTag("subscription-amount").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("subscription-paid-on-input").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("subscription-confirm-payment").assertIsNotEnabled()
        compose.onNodeWithText("正在保存到本地").performScrollTo().assertIsDisplayed()
        assertEquals(0, confirmations.get())
    }

    @Test fun unconfirmed_form_only_replays_same_payment() {
        val confirmations = AtomicInteger()
        compose.setContent { LithicTheme {
            RenewalHost(editor().copy(kind = SubscriptionEditorKind.RENEWAL,
                phase = SubmissionPhase.UNCONFIRMED, message = "付款记录是否保存尚未确认"),
                onConfirm = { confirmations.incrementAndGet() })
        } }
        compose.onNodeWithTag("renewal-amount").performScrollTo().assertIsNotEnabled().assertTextContains("150")
        compose.onNodeWithTag("renewal-confirm-payment")
            .assertTextContains("核对并重试同一付款").performClick()
        assertEquals(1, confirmations.get())
    }

    @Test fun committed_form_offers_read_retry_without_payment() {
        val confirmations = AtomicInteger()
        val reads = AtomicInteger()
        compose.setContent { LithicTheme {
            CreateHost(editor().copy(phase = SubmissionPhase.COMMITTED,
                message = "已保存，暂时无法读取。请重试读取，不必重新付款。"),
                onConfirm = { confirmations.incrementAndGet() }, onRead = { reads.incrementAndGet() })
        } }
        compose.onNodeWithTag("subscription-name").assertIsNotEnabled()
        compose.onNodeWithTag("subscription-confirm-payment").assertDoesNotExist()
        compose.onNodeWithTag("subscription-retry-read").performClick()
        assertEquals(0, confirmations.get())
        assertEquals(1, reads.get())
    }

    @Test fun conflict_requires_explicit_read_instead_of_new_payment() {
        val confirmations = AtomicInteger()
        val reads = AtomicInteger()
        compose.setContent { LithicTheme {
            RenewalHost(editor().copy(kind = SubscriptionEditorKind.RENEWAL,
                phase = SubmissionPhase.UNCONFIRMED, conflict = true, message = "付款身份冲突，请核对已保存事实"),
                onConfirm = { confirmations.incrementAndGet() }, onRead = { reads.incrementAndGet() })
        } }
        compose.onNodeWithTag("renewal-confirm-payment").assertDoesNotExist()
        compose.onNodeWithTag("renewal-amount").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("renewal-resolve-conflict").performClick()
        assertEquals(0, confirmations.get())
        assertEquals(1, reads.get())
    }

    @Test fun verified_missing_target_locks_input_and_offers_safe_return_without_payment() {
        val confirmations = AtomicInteger()
        val backs = AtomicInteger()
        compose.setContent { LithicTheme {
            RenewalHost(editor().copy(kind = SubscriptionEditorKind.RENEWAL, verifiedCannotWrite = true),
                onConfirm = { confirmations.incrementAndGet() }, onBack = { backs.incrementAndGet() })
        } }
        compose.onNodeWithTag("renewal-amount").performScrollTo().assertIsNotEnabled().assertTextContains("150")
        compose.onNodeWithTag("renewal-paid-on-input").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("renewal-confirm-payment").assertDoesNotExist()
        compose.onNodeWithTag("renewal-cannot-write")
            .assertTextContains("本次输入不能提交", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("renewal-editor-back").performClick()
        assertEquals(0, confirmations.get())
        assertEquals(1, backs.get())
    }

    @Test fun list_loading_and_read_failure_do_not_claim_empty() {
        var state by mutableStateOf(SubscriptionListUiState())
        val reads = AtomicInteger()
        compose.setContent { LithicTheme {
            SubscriptionListScreen(state, {}, {}, {}, {}, { reads.incrementAndGet() })
        } }
        compose.onNodeWithTag("subscription-list-screen").assertIsDisplayed()
        compose.onNodeWithText("暂无订阅服务").assertDoesNotExist()
        compose.runOnIdle { state = state.copy(loading = false, error = "无法读取本地订阅记录") }
        compose.onNodeWithTag("subscription-list-content").performScrollToNode(hasTestTag("subscription-list-error"))
        compose.onNodeWithTag("subscription-list-error").assertTextContains("无法读取", substring = true)
        compose.onNodeWithText("暂无订阅服务").assertDoesNotExist()
        compose.onNodeWithTag("subscription-list-retry").performClick()
        assertEquals(1, reads.get())
    }

    @Test fun genuine_empty_list_exposes_create_without_fake_metrics() {
        val creates = AtomicInteger()
        compose.setContent { LithicTheme {
            SubscriptionListScreen(SubscriptionListUiState(totalCount = 0, loading = false), {},
                { creates.incrementAndGet() }, {}, {}, {})
        } }
        compose.onNodeWithText("暂无订阅服务").assertIsDisplayed()
        compose.onNodeWithText("¥0.00").assertDoesNotExist()
        compose.onNodeWithTag("subscription-list-create").performClick()
        assertEquals(1, creates.get())
    }

    @Test fun detail_read_failure_and_missing_are_distinct() {
        var state by mutableStateOf(SubscriptionDetailUiState(loading = false, error = "暂时无法读取订阅"))
        val reads = AtomicInteger()
        compose.setContent { LithicTheme {
            SubscriptionDetailScreen(state, {}, {}, {}, { reads.incrementAndGet() })
        } }
        compose.onNodeWithTag("subscription-detail-error").assertTextContains("无法读取", substring = true)
        compose.onNodeWithTag("subscription-detail-missing").assertDoesNotExist()
        compose.onNodeWithText("暂无付费记录").assertDoesNotExist()
        compose.onNodeWithTag("subscription-detail-retry").performClick()
        assertEquals(1, reads.get())
        compose.runOnIdle { state = state.copy(error = null, missing = true) }
        compose.onNodeWithTag("subscription-detail-missing").assertIsDisplayed()
        compose.onNodeWithTag("subscription-detail-error").assertDoesNotExist()
    }

    @Test fun narrow_large_font_form_actions_and_errors_remain_reachable() {
        val confirmations = AtomicInteger()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                LithicTheme { Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.width(360.dp).fillMaxHeight()) {
                        CreateHost(editor().copy(message = "核对实际付款与四个日期，全部输入保持原样"),
                            onConfirm = { confirmations.incrementAndGet() })
                    }
                } }
            }
        }
        compose.onNodeWithTag("subscription-name").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("subscription-amount").performScrollTo().assertIsDisplayed()
        assertDate("subscription-paid-on-input", "2026-01-01")
        assertDate("subscription-effective-from-input", "2026-01-01")
        assertDate("subscription-paid-until-input", "2026-02-01")
        assertDate("subscription-reminder-due-input", "2026-01-29")
        compose.onNodeWithTag("subscription-form-error").assertIsDisplayed()
        compose.onNodeWithTag("subscription-confirm-payment").assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, confirmations.get())
    }

    private fun changeDate(tag: String, value: String) {
        compose.onNodeWithTag(tag).performScrollTo().performTextReplacement(value)
    }

    private fun assertDate(tag: String, value: String) {
        compose.onNodeWithTag(tag).performScrollTo().assertTextContains(value)
    }

    @Composable
    private fun CreateHost(
        state: SubscriptionEditorUiState,
        onChange: (SubscriptionEditorUiState) -> Unit = {},
        onConfirm: () -> Unit = {},
        onRead: () -> Unit = {},
    ) {
        SubscriptionEditorScreen(state, {},
            { onChange(state.copy(serviceName = it)) }, { onChange(state.copy(categoryId = it)) },
            { onChange(state.copy(note = it)) }, { onChange(state.copy(amount = it)) },
            { onChange(state.copy(cycle = it)) }, { onChange(state.copy(paidOn = it)) },
            { onChange(state.copy(effectiveFrom = it)) }, { onChange(state.copy(paidUntilExclusive = it)) },
            { onChange(state.copy(reminderDueOn = it)) }, onConfirm, onRead)
    }

    @Composable
    private fun RenewalHost(
        state: SubscriptionEditorUiState,
        onChange: (SubscriptionEditorUiState) -> Unit = {},
        onConfirm: () -> Unit = {},
        onRead: () -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        RenewalEditorScreen(state, onBack, { onChange(state.copy(amount = it)) },
            { onChange(state.copy(cycle = it)) }, { onChange(state.copy(paidOn = it)) },
            { onChange(state.copy(effectiveFrom = it)) }, { onChange(state.copy(paidUntilExclusive = it)) },
            { onChange(state.copy(reminderDueOn = it)) }, onConfirm, onRead)
    }

    private fun editor() = SubscriptionEditorUiState(
        subscriptionId = "00000000-0000-4000-8000-000000000010", serviceName = "测试服务",
        categoryId = FIRST_CATEGORY, amount = "150", paidOn = "2026-01-01",
        effectiveFrom = "2026-01-01", paidUntilExclusive = "2026-02-01", reminderDueOn = "2026-01-29",
        today = LocalDate.of(2026, 1, 20), categories = listOf(
            CategoryEntity(FIRST_CATEGORY, CategoryType.SUBSCRIPTION, "其他", "其他", true),
            CategoryEntity(SECOND_CATEGORY, CategoryType.SUBSCRIPTION, "娱乐", "娱乐", false),
            CategoryEntity("00000000-0000-4000-8000-000000000004", CategoryType.PROJECT,
                "项目专属类别", "项目专属类别", false),
        ),
    )

    private companion object {
        const val FIRST_CATEGORY = "00000000-0000-4000-8000-000000000002"
        const val SECOND_CATEGORY = "00000000-0000-4000-8000-000000000003"
    }
}
