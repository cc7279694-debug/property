package com.cooper.lithic.feature.subscription

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cooper.lithic.core.design.LithicTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Controlled presentation contracts only; these are not device, IME or database evidence. */
class C5PaymentFormsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun shared_payment_amount_preserves_invalid_blank_and_zero_input_without_confirmation() {
        var raw by mutableStateOf("1.234")
        var confirmations = 0
        compose.setContent { LithicTheme {
            RenewalEditorScreen(SubscriptionEditorUiState(kind = SubscriptionEditorKind.CORRECT_RENEWAL,
                amount = raw, errors = mapOf("amount" to "金额最多保留两位小数")), {}, { raw = it },
                {}, {}, {}, {}, {}, { confirmations++ }, {})
        } }
        compose.onNodeWithTag("renewal-amount").performScrollTo().assertTextContains("1.234")
        compose.onNodeWithTag("renewal-amount").performTextReplacement("")
        compose.runOnIdle { assertEquals("", raw) }
        compose.onNodeWithTag("renewal-amount").performTextReplacement("0")
        compose.runOnIdle { assertEquals("0", raw) }
        assertEquals(0, confirmations)
    }

    @Test fun all_payment_modes_keep_raw_amount_and_dates_when_reading_optional_guidance() {
        var kind by mutableStateOf(SubscriptionEditorKind.CREATE)
        var fieldChanges = 0
        var confirmations = 0
        compose.setContent { LithicTheme {
            val state = SubscriptionEditorUiState(kind = kind, serviceName = "合成付款服务", amount = "1.234",
                paidOn = "2026-01-", effectiveFrom = "2026-02-01", paidUntilExclusive = "2026-03-01",
                reminderDueOn = "2026-02-28", today = LocalDate.of(2026, 1, 20),
                errors = mapOf("amount" to "金额最多保留两位小数"))
            if (kind == SubscriptionEditorKind.CREATE) SubscriptionEditorScreen(state, {}, { fieldChanges++ },
                { fieldChanges++ }, { fieldChanges++ }, { fieldChanges++ }, { fieldChanges++ },
                { fieldChanges++ }, { fieldChanges++ }, { fieldChanges++ }, { fieldChanges++ },
                { confirmations++ }, {})
            else RenewalEditorScreen(state, {}, { fieldChanges++ }, { fieldChanges++ }, { fieldChanges++ },
                { fieldChanges++ }, { fieldChanges++ }, { fieldChanges++ }, { confirmations++ }, {})
        } }
        SubscriptionEditorKind.entries.forEach { mode ->
            compose.runOnIdle { kind = mode }
            val prefix = if (mode == SubscriptionEditorKind.CREATE) "subscription" else "renewal"
            compose.onNodeWithTag("$prefix-amount").performScrollTo().assertTextContains("1.234")
            compose.onNodeWithText("金额最多保留两位小数").assertExists()
            compose.onNodeWithTag("$prefix-paid-on-input").performScrollTo().assertTextContains("2026-01-")
            compose.onNodeWithTag("$prefix-payment-information").performScrollTo().performClick()
            compose.onNodeWithTag("$prefix-cycle-explanation").performScrollTo()
                .assertTextContains("不代表已经使用", substring = true)
            compose.onNodeWithTag("$prefix-effective-explanation").performScrollTo()
                .assertTextContains("预付未来周期", substring = true)
            compose.onNodeWithTag("$prefix-reminder-explanation").performScrollTo()
                .assertTextContains("不会延长服务", substring = true)
            compose.onNodeWithText("知道了").performClick()
            compose.onNodeWithTag("$prefix-amount").performScrollTo().assertTextContains("1.234")
        }
        assertEquals(0, fieldChanges)
        assertEquals(0, confirmations)
    }

    @Test fun delete_payment_keeps_unparseable_original_amount_without_zero_or_read_request() {
        var reads = 0
        compose.setContent { LithicTheme {
            SubscriptionStateDialog(SubscriptionStateEditorUiState(kind = SubscriptionStateEditorKind.DELETE_RENEWAL,
                serviceName = "合成服务", renewalAmount = "尚待核对的原金额", renewalPaidOn = "2026-01-02"),
                {}, {}, {}, {}, {}, {}, {}, { reads++ })
        } }
        compose.onNodeWithTag("subscription-delete-payment-unavailable").performScrollTo()
            .assertTextContains("尚待核对的原金额")
        compose.onNodeWithTag("subscription-delete-payment-amount").assertDoesNotExist()
        compose.onNodeWithText("¥0.00").assertDoesNotExist()
        compose.onNodeWithTag("subscription-delete-payment-explanation").performScrollTo()
            .assertTextContains("这不是退款", substring = true)
        assertEquals(0, reads)
    }
}
