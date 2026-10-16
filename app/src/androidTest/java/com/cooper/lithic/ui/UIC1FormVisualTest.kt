package com.cooper.lithic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.feature.project.MoneyEventEditor
import com.cooper.lithic.feature.project.MoneyEventEditorUiState
import com.cooper.lithic.feature.project.ProjectEditorScreen
import com.cooper.lithic.feature.project.ProjectEditorUiState
import com.cooper.lithic.feature.project.SubmissionPhase
import com.cooper.lithic.feature.subscription.RenewalEditorScreen
import com.cooper.lithic.feature.subscription.SubscriptionEditorKind
import com.cooper.lithic.feature.subscription.SubscriptionEditorScreen
import com.cooper.lithic.feature.subscription.SubscriptionEditorUiState
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual form presentations in an isolated host; no ledger or ViewModel is opened. */
@RunWith(AndroidJUnit4::class)
class UIC1FormVisualTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun light_project_header_wraps_and_zero_amount_keeps_save_and_back_callbacks() {
        val saves = AtomicInteger()
        val backs = AtomicInteger()
        var state by mutableStateOf(ProjectEditorUiState(
            projectId = PROJECT_ID, name = "自费取得的项目", startedOn = "2026-10-01",
            initialAmount = "0", initialPaidOn = "2026-10-01", today = TODAY,
            categoryId = PROJECT_CATEGORY_ID, categories = listOf(
                CategoryEntity(PROJECT_CATEGORY_ID, CategoryType.PROJECT, "其他", "其他", true)),
        ))
        compose.setContent {
            NarrowFormHost(dark = false) {
                ProjectEditorScreen(state, { backs.incrementAndGet() },
                    { state = state.copy(name = it) }, { state = state.copy(startedOn = it) },
                    { state = state.copy(initialAmount = it) }, { state = state.copy(initialPaidOn = it) },
                    { state = state.copy(categoryId = it) }, { state = state.copy(note = it) },
                    { saves.incrementAndGet() }, {})
            }
        }
        assertHeader("project-editor-back", "新增长期项目")
        compose.onNodeWithTag("project-initial-amount").performScrollTo().assertTextContains("0")
        compose.onNodeWithTag("project-save").performScrollTo().assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithTag("project-editor-back").performScrollTo().performClick()
        assertEquals("0", state.initialAmount)
        assertEquals(1, saves.get())
        assertEquals(1, backs.get())
    }

    @Test
    fun dark_money_form_keeps_raw_precision_date_and_wrapped_failure_reachable() {
        val amountError = "金额最多保留两位小数，原始输入已保留，请核对真实金额后重试。"
        val dateError = "请输入准确的真实发生日期"
        val message = "本地保存失败，已经输入的金额、日期和说明仍然保留。"
        val saves = AtomicInteger()
        val backs = AtomicInteger()
        var state by mutableStateOf(MoneyEventEditorUiState(
            projectId = PROJECT_ID, projectName = "已结束但仍可补录实际结算的项目",
            amount = "1.234", occurredOn = "2026-13-40", description = "保留的真实结算说明",
            today = TODAY, errors = mapOf("amount" to amountError, "occurredOn" to dateError),
            message = message,
        ))
        compose.setContent {
            NarrowFormHost(dark = true) {
                MoneyEventEditor(state, { backs.incrementAndGet() }, { state = state.copy(direction = it) },
                    { state = state.copy(amount = it) }, { state = state.copy(occurredOn = it) },
                    { state = state.copy(description = it) }, { saves.incrementAndGet() }, {})
            }
        }
        assertHeader("money-editor-back", "记录投入或收回")
        compose.onNodeWithTag("money-amount").performScrollTo().performTextReplacement(" 1.234 ")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("money-amount").assertTextContains(" 1.234 ")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, amountError))
        assertTextFits(amountError)
        compose.onNodeWithTag("money-occurred-on-input").performScrollTo().assertTextContains("2026-13-40")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, dateError))
        compose.onNodeWithTag("money-description").performScrollTo().assertTextContains("保留的真实结算说明")
        assertTextFits(message)
        compose.onNodeWithTag("money-save").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("money-editor-back").performScrollTo().performClick()
        assertEquals(" 1.234 ", state.amount)
        assertEquals("2026-13-40", state.occurredOn)
        assertEquals(0, saves.get())
        assertEquals(1, backs.get())
    }

    @Test
    fun light_subscription_form_keeps_four_raw_dates_error_and_explicit_payment_confirmation() {
        val message = "付款还没有保存，请核对金额和四个日期，当前原始输入已经保留。"
        val confirmations = AtomicInteger()
        val backs = AtomicInteger()
        var state by mutableStateOf(subscription().copy(message = message))
        compose.setContent {
            NarrowFormHost(dark = false) {
                SubscriptionEditorScreen(state, { backs.incrementAndGet() },
                    { state = state.copy(serviceName = it) }, { state = state.copy(categoryId = it) },
                    { state = state.copy(note = it) }, { state = state.copy(amount = it) },
                    { state = state.copy(cycle = it) }, { state = state.copy(paidOn = it) },
                    { state = state.copy(effectiveFrom = it) }, { state = state.copy(paidUntilExclusive = it) },
                    { state = state.copy(reminderDueOn = it) }, { confirmations.incrementAndGet() }, {})
            }
        }
        assertHeader("subscription-editor-back", "新增订阅服务")
        compose.onNodeWithTag("subscription-amount").performScrollTo().assertTextContains("150.00")
        listOf("paid-on" to "2026-10-01", "effective-from" to "2026-11-01",
            "paid-until" to "2026-12-01", "reminder-due" to "2026-11-28").forEach { (field, raw) ->
            compose.onNodeWithTag("subscription-$field-input").performScrollTo().assertTextContains(raw)
        }
        assertTextFits(message)
        compose.onNodeWithTag("subscription-confirm-payment").performScrollTo().assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).assertTextContains("确认已付款并创建订阅").performClick()
        compose.onNodeWithTag("subscription-editor-back").performScrollTo().performClick()
        assertEquals("2026-11-01", state.effectiveFrom)
        assertEquals("2026-12-01", state.paidUntilExclusive)
        assertEquals(1, confirmations.get())
        assertEquals(1, backs.get())
    }

    @Test
    fun dark_committed_reenable_keeps_wrapped_header_read_only_values_and_only_read_recovery() {
        val confirmations = AtomicInteger()
        val reads = AtomicInteger()
        val backs = AtomicInteger()
        val state = subscription().copy(kind = SubscriptionEditorKind.REENABLE,
            phase = SubmissionPhase.COMMITTED, message = "已保存，暂时无法读取。请重试读取，不必重新付款。")
        compose.setContent {
            NarrowFormHost(dark = true) {
                RenewalEditorScreen(state, { backs.incrementAndGet() }, {}, {}, {}, {}, {}, {},
                    { confirmations.incrementAndGet() }, { reads.incrementAndGet() })
            }
        }
        assertHeader("renewal-editor-back", "确认真实付款并启用原订阅", mustWrap = true)
        compose.onNodeWithTag("renewal-amount").performScrollTo().assertIsNotEnabled().assertTextContains("150.00")
        listOf("paid-on" to "2026-10-01", "effective-from" to "2026-11-01",
            "paid-until" to "2026-12-01", "reminder-due" to "2026-11-28").forEach { (field, raw) ->
            compose.onNodeWithTag("renewal-$field-input").performScrollTo().assertIsNotEnabled().assertTextContains(raw)
        }
        compose.onNodeWithTag("renewal-confirm-payment").assertDoesNotExist()
        assertTextFits(requireNotNull(state.message))
        compose.onNodeWithTag("renewal-retry-read").performScrollTo().assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithTag("renewal-editor-back").performScrollTo().performClick()
        assertEquals(0, confirmations.get())
        assertEquals(1, reads.get())
        assertEquals(1, backs.get())
    }

    private fun assertHeader(backTag: String, title: String, mustWrap: Boolean = false) {
        compose.onNodeWithTag("form-visual-host").assertWidthIsEqualTo(360.dp)
        compose.onNodeWithTag("$backTag-header").assertIsDisplayed()
        compose.onNodeWithTag(backTag).assertIsDisplayed().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        compose.onNodeWithText(title, useUnmergedTree = true)
            .assert(hasAnyAncestor(hasTestTag("$backTag-header")))
        val layout = assertTextFits(title)
        if (mustWrap) assertTrue("The header must wrap at 360dp and 200% font", layout.lineCount > 1)
    }

    private fun assertTextFits(text: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals("Text must expose one actual layout", 1, layouts.size)
        return layouts.single().also {
            assertFalse("Full text must remain visible without clipping or ellipsis", it.hasVisualOverflow)
        }
    }

    @Composable
    private fun NarrowFormHost(dark: Boolean, content: @Composable () -> Unit) {
        val density = LocalDensity.current.density
        CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
            LithicTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.width(360.dp).fillMaxHeight().testTag("form-visual-host")) { content() }
                    }
                }
            }
        }
    }

    private fun subscription() = SubscriptionEditorUiState(
        subscriptionId = SUBSCRIPTION_ID, serviceName = "保留原身份的订阅服务", amount = "150.00",
        paidOn = "2026-10-01", effectiveFrom = "2026-11-01", paidUntilExclusive = "2026-12-01",
        reminderDueOn = "2026-11-28", today = TODAY, categoryId = SUBSCRIPTION_CATEGORY_ID, categories = listOf(
            CategoryEntity(SUBSCRIPTION_CATEGORY_ID, CategoryType.SUBSCRIPTION, "其他", "其他", true)),
    )

    private companion object {
        const val PROJECT_ID = "00000000-0000-4000-8000-000000000101"
        const val PROJECT_CATEGORY_ID = "00000000-0000-4000-8000-000000000102"
        const val SUBSCRIPTION_ID = "00000000-0000-4000-8000-000000000103"
        const val SUBSCRIPTION_CATEGORY_ID = "00000000-0000-4000-8000-000000000104"
        val TODAY: LocalDate = LocalDate.of(2026, 10, 15)
    }
}
