package com.cooper.lithic.feature.project

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cooper.lithic.core.design.LithicTheme
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Compile-only UI contracts; these do not execute Room writes or Android failure experiments. */
class C2B3ProjectOperationsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun in_flight_money_keeps_direction_and_save_locked_in_fixed_footer() {
        compose.setContent { LithicTheme {
            MoneyEventEditor(MoneyEventEditorUiState(projectName = "合成项目", amount = "100", phase = SubmissionPhase.IN_FLIGHT), {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("money-direction-recovery").assertIsNotEnabled()
        compose.onNodeWithTag("money-amount").assertIsNotEnabled()
        compose.onNodeWithTag("money-save").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test fun linked_trade_in_correction_keeps_amount_and_date_read_only() {
        compose.setContent { LithicTheme {
            MoneyEventCorrectionScreen(MoneyCorrectionUiState(projectName = "合成项目", amount = "30", occurredOn = "2026-01-01", today = LocalDate.of(2026, 1, 2), tradeInProtected = true), {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("correction-amount").assertIsNotEnabled()
        compose.onNodeWithTag("correction-occurred-on-input").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("以旧换新关联记录只修改说明，不单独修改金额或真实日期。").assertExists()
    }

    @Test fun unconfirmed_end_only_replays_the_original_submission_callback() {
        val saves = AtomicInteger()
        compose.setContent { LithicTheme {
            EndProjectDialog(EndProjectUiState(projectName = "合成项目", phase = SubmissionPhase.UNCONFIRMED), {}, {}, {}, {}, {}, {}, {}, { saves.incrementAndGet() }, {})
        } }
        compose.onNodeWithTag("end-save").assertIsDisplayed().assertTextContains("核对并重试").performClick()
        assertEquals(1, saves.get())
        compose.onNodeWithText("结束不是删除；从结束日期起退出当前项目列表。").assertExists()
    }

    @Test fun trade_confirmation_keeps_all_three_fact_amounts_and_acquisition_total() {
        val state = ReplacementFormUiState(ReplacementFormKind.TRADE_IN, "old", projectName = "旧合成项目", name = "新合成项目", date = "2026-01-01", cash = "100", credit = "30", today = LocalDate.of(2026, 1, 2), confirmed = true)
        compose.setContent { LithicTheme {
            TradeInEditorScreen(state, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("trade-in-confirm-old-credit").performScrollTo().assertContentDescriptionContains("30.00元", substring = true)
        compose.onNodeWithTag("trade-in-confirm-cash").performScrollTo().assertContentDescriptionContains("100.00元", substring = true)
        compose.onNodeWithTag("trade-in-confirm-new-credit").performScrollTo().assertContentDescriptionContains("30.00元", substring = true)
        compose.onNodeWithTag("trade-in-confirm-acquisition").performScrollTo().assertContentDescriptionContains("130.00元", substring = true)
        compose.onNodeWithTag("trade-in-confirm-net-change").performScrollTo().assertContentDescriptionContains("100.00元", substring = true)
        compose.onNodeWithTag("trade-in-save").assertIsDisplayed().assertTextContains("确认并保存整组交易")
    }

    @Test fun delete_unknown_result_keeps_cancel_locked_and_original_confirmation_available() {
        val confirms = AtomicInteger()
        compose.setContent { LithicTheme {
            DeleteProjectDialog(DeleteProjectUiState(projectName = "合成项目", phase = SubmissionPhase.UNCONFIRMED), {}, { confirms.incrementAndGet() }, {})
        } }
        compose.onNodeWithTag("project-delete-cancel").assertIsNotEnabled()
        compose.onNodeWithTag("project-delete-confirm").assertTextContains("核对原删除提交").performClick()
        assertEquals(1, confirms.get())
        compose.onNodeWithText("删除结果尚未确认。请核对原提交，期间不能改选项目或重复创建删除操作。").assertExists()
    }
}
