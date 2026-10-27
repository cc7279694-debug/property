package com.cooper.lithic.feature.project

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

/** Controlled strings and callback contracts; no parser, Room or real-device execution. */
class C5ProjectFormsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun all_six_project_money_inputs_preserve_invalid_blank_and_zero_strings() {
        var screen by mutableStateOf(0)
        var raw by mutableStateOf("1.234")
        val today = LocalDate.of(2026, 1, 20)
        compose.setContent { LithicTheme {
            val error = "金额最多保留两位小数"
            when (screen) {
                0 -> ProjectEditorScreen(ProjectEditorUiState(initialAmount = raw, today = today,
                    errors = mapOf("initialAmount" to error)), {}, {}, {}, { raw = it }, {}, {}, {}, {}, {})
                1 -> MoneyEventEditor(MoneyEventEditorUiState(amount = raw, today = today,
                    errors = mapOf("amount" to error)), {}, {}, { raw = it }, {}, {}, {}, {})
                2 -> MoneyEventCorrectionScreen(MoneyCorrectionUiState(amount = raw, today = today,
                    errors = mapOf("amount" to error)), {}, { raw = it }, {}, {}, {}, {})
                3 -> EndProjectDialog(EndProjectUiState(recordRecovery = true, recoveryAmount = raw, today = today,
                    errors = mapOf("recoveryAmount" to error)), {}, {}, {}, {}, { raw = it }, {}, {}, {}, {})
                else -> TradeInEditorScreen(ReplacementFormUiState(ReplacementFormKind.TRADE_IN, "synthetic-old",
                    cash = raw, credit = raw, today = today, errors = mapOf("cash" to error, "credit" to error)),
                    {}, {}, {}, { raw = it }, { raw = it }, {}, {}, {}, {}, {}, {}, {})
            }
        } }
        listOf(
            0 to "project-initial-amount",
            1 to "money-amount",
            2 to "correction-amount",
            3 to "end-recovery-amount",
            4 to "trade-in-cash",
            4 to "trade-in-credit",
        ).forEach { (host, tag) ->
            compose.runOnIdle { screen = host; raw = "1.234" }
            compose.onNodeWithTag(tag).performScrollTo().assertTextContains("1.234")
            compose.onNodeWithTag(tag).performTextReplacement("")
            compose.runOnIdle { assertEquals("", raw) }
            compose.onNodeWithTag(tag).performTextReplacement("0")
            compose.runOnIdle { assertEquals("0", raw) }
        }
    }

    @Test fun trade_preview_retains_non_cash_group_limits_and_unknown_submission() {
        var saves = 0
        compose.setContent { LithicTheme {
            TradeInEditorScreen(ReplacementFormUiState(ReplacementFormKind.TRADE_IN, "synthetic-old",
                name = "合成新项目", cash = "0", credit = "30", today = LocalDate.of(2026, 1, 20),
                confirmed = true, phase = SubmissionPhase.UNCONFIRMED),
                {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, { saves++ }, {})
        } }
        compose.onNodeWithTag("trade-in-confirm-net-change").performScrollTo()
            .assertContentDescriptionContains("0.00元", substring = true)
        compose.onNodeWithText("折抵不是现金支出；以上不是首页日均成本的增量。三笔记录作为一组保存，不再额外重复记录初始总价。")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("保存后暂不支持整组撤销。交易项目不能单独删除或改取得日期，交易行金额与日期不能单独纠错。名称、类别、备注和后续真实金额仍可管理。")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("trade-in-edit-confirmation").assertDoesNotExist()
        compose.onNodeWithTag("trade-in-save").assertTextContains("同一提交", substring = true).performClick()
        assertEquals(1, saves)
    }
}
