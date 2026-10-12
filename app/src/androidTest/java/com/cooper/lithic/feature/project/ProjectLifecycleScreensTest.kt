package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectCostResult
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.ProjectDetail
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Controlled presentation tests do not replace the real Room lifecycle flows. */
@RunWith(AndroidJUnit4::class)
class ProjectLifecycleScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun end_form_only_records_recovery_after_explicit_opt_in_and_accepts_zero() {
        var state by mutableStateOf(end())
        compose.setContent { LithicTheme { EndHost(state, { state = it }) } }
        compose.onNodeWithTag("end-recovery-amount").assertDoesNotExist()
        compose.onNodeWithTag("end-record-recovery").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithTag("end-recovery-amount").performScrollTo().performTextReplacement("0")
        compose.onNodeWithTag("end-recovery-on-input").performScrollTo().performTextReplacement("2026-01-09")
        compose.onNodeWithTag("end-recovery-description").performScrollTo().performTextReplacement("实际收到0元")
        assertEquals(true, state.recordRecovery)
        assertEquals("0", state.recoveryAmount)
        assertEquals("2026-01-09", state.recoveryOn)
        assertEquals("实际收到0元", state.recoveryDescription)
        compose.onNodeWithText("结束不是删除；从结束日期起退出当前项目列表。")
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun end_validation_preserves_raw_dates_amount_reason_and_visible_errors() {
        val state = end().copy(recordRecovery = true, endedOn = "2026-01-", endReason = "保留的原因",
            recoveryAmount = "1.234", recoveryOn = "2026-01-20", recoveryDescription = "真实收回说明",
            errors = mapOf("endedOn" to "日期格式不完整", "recoveryAmount" to "金额最多保留两位小数",
                "recoveryOn" to "不能记录未来收回"), message = "请核对输入，记录尚未保存")
        compose.setContent { LithicTheme { EndHost(state) } }
        compose.onNodeWithTag("end-ended-on-input").performScrollTo().assertTextContains("2026-01-")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "日期格式不完整"))
        compose.onNodeWithTag("end-reason").performScrollTo().assertTextContains("保留的原因")
        compose.onNodeWithTag("end-recovery-amount").performScrollTo().assertTextContains("1.234")
        compose.onNodeWithTag("end-recovery-on-input").performScrollTo().assertTextContains("2026-01-20")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "不能记录未来收回"))
        compose.onNodeWithTag("end-recovery-description").performScrollTo().assertTextContains("真实收回说明")
        compose.onNodeWithTag("end-form-error").performScrollTo().assertTextContains("请核对输入，记录尚未保存")
    }

    @Test fun correcting_end_without_settlement_never_offers_creating_recovery() {
        compose.setContent { LithicTheme { EndHost(end().copy(correcting = true, hasSettlement = false)) } }
        compose.onNodeWithText("修正结束记录").assertIsDisplayed()
        compose.onNodeWithTag("end-record-recovery").assertDoesNotExist()
        compose.onNodeWithTag("end-recovery-amount").assertDoesNotExist()
        compose.onNodeWithText("本次只修正结束记录；真实退款请在详情新增收回。")
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun correcting_linked_settlement_keeps_the_existing_event_and_has_no_toggle_or_delete() {
        var state by mutableStateOf(end().copy(correcting = true, hasSettlement = true,
            recordRecovery = true, recoveryAmount = "100", recoveryOn = "2026-01-08"))
        compose.setContent { LithicTheme { EndHost(state, { state = it }) } }
        compose.onNodeWithTag("end-record-recovery").assertDoesNotExist()
        compose.onNodeWithTag("end-recovery-amount").performScrollTo().performTextReplacement("200")
        compose.onNodeWithText("更新原有关联收回记录，不新增或删除金额记录。")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("删除金额记录").assertDoesNotExist()
        assertEquals("200", state.recoveryAmount)
    }

    @Test fun end_pending_submission_locks_inputs_and_committed_refresh_only_reads() {
        val saves = AtomicInteger()
        val reads = AtomicInteger()
        var state by mutableStateOf(end().copy(phase = SubmissionPhase.IN_FLIGHT))
        compose.setContent { LithicTheme { EndHost(state, onSave = { saves.incrementAndGet() }, onRetry = { reads.incrementAndGet() }) } }
        compose.onNodeWithTag("end-ended-on-input").assertIsNotEnabled()
        compose.onNodeWithTag("end-save").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { state = state.copy(phase = SubmissionPhase.UNCONFIRMED, message = "核对原结束提交") }
        compose.onNodeWithTag("end-save").performScrollTo().assertTextContains("核对并重试").performClick()
        assertEquals(1, saves.get())
        compose.runOnIdle { state = state.copy(phase = SubmissionPhase.COMMITTED, message = "已保存，暂时无法读取") }
        compose.onNodeWithTag("end-save").assertDoesNotExist()
        compose.onNodeWithTag("end-retry-read").performScrollTo().performClick()
        assertEquals(1, saves.get())
        assertEquals(1, reads.get())
    }

    @Test fun correction_keeps_direction_and_role_read_only_and_explains_real_refund() {
        var state by mutableStateOf(correction().copy(role = ProjectMoneyEventRole.INITIAL, amount = "1000"))
        compose.setContent { LithicTheme { CorrectionHost(state, { state = it }) } }
        compose.onNodeWithTag("correction-direction").assertTextContains("方向：投入")
        compose.onNodeWithTag("correction-role").assertTextContains("记录类型：初始投入")
        compose.onNodeWithTag("money-direction-recovery").assertDoesNotExist()
        compose.onNodeWithTag("correction-amount").performScrollTo().performTextReplacement("900")
        compose.onNodeWithTag("correction-occurred-on-input").performScrollTo().performTextReplacement("2025-12-30")
        compose.onNodeWithText("纠正原记录的金额、真实日期或说明；真实退款请新增收回记录。")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("删除金额记录").assertDoesNotExist()
        assertEquals(MoneyDirection.OUTLAY, state.direction)
        assertEquals(ProjectMoneyEventRole.INITIAL, state.role)
        assertEquals("900", state.amount)
    }

    @Test fun correction_initial_read_and_conflict_offer_reload_without_resubmitting() {
        val saves = AtomicInteger()
        val reads = AtomicInteger()
        var state by mutableStateOf(correction().copy(needsInitialRead = true, amount = "1.234",
            message = "原记录暂时无法读取，输入已保留"))
        compose.setContent { LithicTheme { CorrectionHost(state, onSave = { saves.incrementAndGet() }, onRetry = { reads.incrementAndGet() }) } }
        compose.onNodeWithTag("correction-save").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("correction-initial-read-retry").performScrollTo().performClick()
        compose.runOnIdle { state = state.copy(needsInitialRead = false, conflict = true, phase = SubmissionPhase.UNCONFIRMED) }
        compose.onNodeWithTag("correction-save").assertDoesNotExist()
        compose.onNodeWithTag("correction-resolve-conflict").performScrollTo().performClick()
        assertEquals(0, saves.get())
        assertEquals(2, reads.get())
    }

    @Test fun delete_confirmation_states_are_explicit_and_do_not_allow_duplicate_or_unknown_cancel() {
        val confirms = AtomicInteger()
        val cancels = AtomicInteger()
        val reads = AtomicInteger()
        var state by mutableStateOf(DeleteProjectUiState(projectId = PROJECT_ID, projectName = "测试项目"))
        compose.setContent { LithicTheme {
            DeleteProjectDialog(state, { cancels.incrementAndGet() }, { confirms.incrementAndGet() }, { reads.incrementAndGet() })
        } }
        compose.onNodeWithTag("project-delete-dialog").assertIsDisplayed()
        compose.onNodeWithText("将删除项目及金额记录，历史统计也会重算；无法在App内撤回。").assertIsDisplayed()
        compose.onNodeWithTag("project-delete-cancel").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, cancels.get())
        assertEquals(0, confirms.get())
        compose.runOnIdle { state = state.copy(phase = SubmissionPhase.IN_FLIGHT) }
        compose.onNodeWithTag("project-delete-confirm").assertIsNotEnabled()
        compose.onNodeWithTag("project-delete-cancel").assertIsNotEnabled()
        compose.runOnIdle { state = state.copy(phase = SubmissionPhase.UNCONFIRMED, message = "删除结果尚未确认") }
        compose.onNodeWithTag("project-delete-confirm").assertTextContains("核对原删除提交").performClick()
        compose.onNodeWithTag("project-delete-cancel").assertIsNotEnabled()
        assertEquals(1, confirms.get())
        compose.runOnIdle { state = state.copy(conflict = true) }
        compose.onNodeWithTag("project-delete-confirm").assertDoesNotExist()
        compose.onNodeWithTag("project-delete-retry-read").performClick()
        assertEquals(1, reads.get())
    }

    @Test fun history_preserves_visible_rows_when_read_fails_and_exposes_database_continuation() {
        var state by mutableStateOf(ProjectListUiState(loading = false, totalCount = 0))
        var opened: String? = null
        val pages = AtomicInteger()
        val reads = AtomicInteger()
        compose.setContent { LithicTheme {
            ProjectHistoryScreen(state, {}, { opened = it }, { pages.incrementAndGet() }, { reads.incrementAndGet() })
        } }
        compose.onNodeWithText("暂无已结束项目").assertIsDisplayed()
        compose.runOnIdle { state = ProjectListUiState(items = listOf(detail()), loading = false, totalCount = 51,
            hasMore = true, error = "历史暂时无法读取，已显示内容保留") }
        compose.onNodeWithTag("project-history-row-$PROJECT_ID").performClick()
        assertEquals(PROJECT_ID, opened)
        compose.onNodeWithTag("project-history-retry").performClick()
        compose.onNodeWithTag("project-history-content").performScrollToNode(hasTestTag("project-history-load-more"))
        compose.onNodeWithTag("project-history-load-more").performClick()
        assertEquals(1, pages.get())
        assertEquals(1, reads.get())
    }

    @Test fun detail_routes_regular_and_initial_corrections_separately_from_end_settlement() {
        var corrected: String? = null
        val updateEnd = AtomicInteger()
        val deletion = AtomicInteger()
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ProjectDetailUiState(detail(), loading = false), {}, {}, {}, {},
                onCorrectMoney = { corrected = it }, onUpdateEnd = { updateEnd.incrementAndGet() },
                onDelete = { deletion.incrementAndGet() }, backLabel = "返回历史档案")
        } }
        compose.onNodeWithTag("project-detail-back").assertTextContains("返回历史档案")
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-correct-event-$EVENT_ID"))
        compose.onNodeWithTag("project-correct-event-$EVENT_ID").performClick()
        assertEquals(EVENT_ID, corrected)
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-correct-event-$SETTLEMENT_ID"))
        compose.onNodeWithTag("project-correct-event-$SETTLEMENT_ID").performClick()
        assertEquals(1, updateEnd.get())
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-delete"))
        compose.onNodeWithTag("project-delete").performClick()
        assertEquals(1, deletion.get())
        compose.onNodeWithTag("project-end").assertDoesNotExist()
        assertHoldingDaysFrozen()
        compose.onNodeWithText("包含结束后的实际结算；持有天数不再增加。").assertDoesNotExist()
        compose.onNodeWithText("删除金额记录").assertDoesNotExist()
    }

    @Test fun missing_detail_never_presents_stale_costs_or_mutation_actions() {
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ProjectDetailUiState(detail = detail(), loading = false, missing = true), {}, {}, {}, {})
        } }
        compose.onNodeWithText("项目不存在，返回列表查看当前记录。").assertIsDisplayed()
        compose.onNodeWithTag("project-detail-name").assertDoesNotExist()
        compose.onNodeWithTag("project-detail-daily").assertDoesNotExist()
        compose.onNodeWithTag("project-add-outlay").assertDoesNotExist()
        compose.onNodeWithTag("project-delete").assertDoesNotExist()
    }

    @Test fun ended_detail_without_late_money_does_not_claim_post_end_settlement() {
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ProjectDetailUiState(detail = detail(), loading = false), {}, {}, {}, {})
        } }
        assertHoldingDaysFrozen()
        compose.onNodeWithText("包含结束后的实际结算；持有天数不再增加。").assertDoesNotExist()
    }

    @Test fun ended_detail_with_only_future_late_money_does_not_claim_an_observed_settlement() {
        val value = detail().let { it.copy(events = it.events + lateMoney(LocalDate.of(2026, 1, 11))) }
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ProjectDetailUiState(detail = value, loading = false), {}, {}, {}, {})
        } }
        assertHoldingDaysFrozen()
        compose.onNodeWithText("包含结束后的实际结算；持有天数不再增加。").assertDoesNotExist()
    }

    @Test fun ended_detail_with_real_observed_late_money_explains_its_included_settlement() {
        val value = detail().let { it.copy(events = it.events + lateMoney(LocalDate.of(2026, 1, 9))) }
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ProjectDetailUiState(detail = value, loading = false), {}, {}, {}, {})
        } }
        assertHoldingDaysFrozen()
        compose.onNodeWithText("包含结束后的实际结算；持有天数不再增加。").assertIsDisplayed()
    }

    private fun assertHoldingDaysFrozen() {
        compose.onNodeWithTag("project-detail-content")
            .performScrollToNode(hasText("持有天数不再增加", substring = true))
        compose.onNode(hasText("持有天数不再增加", substring = true)).assertIsDisplayed()
    }

    private fun lateMoney(date: LocalDate) = ProjectMoneyEventEntity(
        "e7000000-0000-0000-0000-000000000004", PROJECT_ID, MoneyDirection.RECOVERY, 0, date,
        description = "已录入的零元结算", role = ProjectMoneyEventRole.REGULAR,
        operationId = "e7000000-0000-0000-0000-000000000005")

    @Test fun end_and_correction_forms_are_reachable_at_360dp_with_200_percent_font() {
        var showEnd by mutableStateOf(true)
        val saves = AtomicInteger()
        compose.setContent { LithicTheme {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                Box(Modifier.width(360.dp).fillMaxHeight()) {
                    if (showEnd) EndHost(end().copy(recordRecovery = true, recoveryAmount = "0",
                        projectName = "窄屏和大字体下仍需完整显示的长期项目名称"), onSave = { saves.incrementAndGet() })
                    else CorrectionHost(correction(), onSave = { saves.incrementAndGet() })
                }
            }
        } }
        compose.onNodeWithTag("end-ended-on-input").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("end-recovery-description").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("end-save").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { showEnd = false }
        compose.onNodeWithTag("correction-amount").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("correction-description").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("correction-save").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(2, saves.get())
    }

    @Composable private fun EndHost(state: EndProjectUiState, onChange: (EndProjectUiState) -> Unit = {},
        onSave: () -> Unit = {}, onRetry: () -> Unit = {}) {
        EndProjectDialog(state, {}, { onChange(state.copy(endedOn = it)) }, { onChange(state.copy(endReason = it)) },
            { onChange(state.copy(recordRecovery = it)) }, { onChange(state.copy(recoveryAmount = it)) },
            { onChange(state.copy(recoveryOn = it)) }, { onChange(state.copy(recoveryDescription = it)) }, onSave, onRetry)
    }

    @Composable private fun CorrectionHost(state: MoneyCorrectionUiState, onChange: (MoneyCorrectionUiState) -> Unit = {},
        onSave: () -> Unit = {}, onRetry: () -> Unit = {}) {
        MoneyEventCorrectionScreen(state, {}, { onChange(state.copy(amount = it)) },
            { onChange(state.copy(occurredOn = it)) }, { onChange(state.copy(description = it)) }, onSave, onRetry)
    }

    private fun end() = EndProjectUiState(projectId = PROJECT_ID, projectName = "测试项目",
        endedOn = "2026-01-08", recoveryOn = "2026-01-08", today = LocalDate.of(2026, 1, 10))
    private fun correction() = MoneyCorrectionUiState(projectId = PROJECT_ID, eventId = EVENT_ID,
        projectName = "测试项目", amount = "100", occurredOn = "2026-01-01", today = LocalDate.of(2026, 1, 10))

    private fun detail(): ProjectDetail {
        val time = Instant.parse("2026-01-01T00:00:00Z")
        return ProjectDetail(
            LongTermProjectEntity(PROJECT_ID, "测试项目", LithicDatabase.PROJECT_FALLBACK_ID, LocalDate.of(2026, 1, 1),
                endedOn = LocalDate.of(2026, 1, 8), endReason = "已不用", createdAt = time, updatedAt = time),
            CategoryEntity(LithicDatabase.PROJECT_FALLBACK_ID, CategoryType.PROJECT, "其他", "其他", true),
            listOf(ProjectMoneyEventEntity(EVENT_ID, PROJECT_ID, MoneyDirection.OUTLAY, 100000, LocalDate.of(2026, 1, 1),
                role = ProjectMoneyEventRole.INITIAL, operationId = EVENT_ID),
                ProjectMoneyEventEntity(SETTLEMENT_ID, PROJECT_ID, MoneyDirection.RECOVERY, 20000, LocalDate.of(2026, 1, 8),
                    role = ProjectMoneyEventRole.END_SETTLEMENT, operationId = SETTLEMENT_ID)),
            ProjectCostResult(8, 100000, 20000, 80000, BigDecimal("100"), false, null), LocalDate.of(2026, 1, 10))
    }

    private companion object {
        const val PROJECT_ID = "e7000000-0000-0000-0000-000000000001"
        const val EVENT_ID = "e7000000-0000-0000-0000-000000000002"
        const val SETTLEMENT_ID = "e7000000-0000-0000-0000-000000000003"
    }
}
