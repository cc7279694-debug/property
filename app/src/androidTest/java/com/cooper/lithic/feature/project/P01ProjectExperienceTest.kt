package com.cooper.lithic.feature.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
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
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectCostResult
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.repository.ProjectDetail
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Presentation assertions only; no storage Owner, ledger or system-setting changes are needed. */
@RunWith(AndroidJUnit4::class)
class P01ProjectExperienceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unconfirmed_project_keeps_input_locked_and_explains_original_submission() {
        val saves = AtomicInteger()
        compose.setContent { LithicTheme {
            EditorHost(editor().copy(phase = SubmissionPhase.UNCONFIRMED), onSave = { saves.incrementAndGet() })
        } }
        compose.onNodeWithTag("project-name").assertIsNotEnabled().assertTextContains("测试项目")
        compose.onNodeWithTag("project-submission-status").performScrollTo()
            .assertTextContains("原提交已保留", substring = true).assertTextContains("不要另建相同记录", substring = true)
        compose.onNodeWithTag("project-save").performScrollTo().assertTextContains("核对并重试").performClick()
        assertEquals(1, saves.get()) // The screen dispatches the existing replay callback, not a new ledger write.
    }

    @Test fun committed_project_acknowledges_save_and_only_offers_readback() {
        val saves = AtomicInteger()
        val reads = AtomicInteger()
        compose.setContent { LithicTheme {
            EditorHost(editor().copy(phase = SubmissionPhase.COMMITTED),
                onSave = { saves.incrementAndGet() }, onRetry = { reads.incrementAndGet() })
        } }
        compose.onNodeWithTag("project-submission-status").performScrollTo()
            .assertTextContains("记录已保存", substring = true).assertTextContains("不会再次保存", substring = true)
        compose.onNodeWithTag("project-save").assertDoesNotExist()
        compose.onNodeWithTag("project-retry-read").performScrollTo().performClick()
        assertEquals(0, saves.get())
        assertEquals(1, reads.get())
    }

    @Test fun money_direction_selection_has_visible_feedback_and_keeps_selected_semantics() {
        var state by mutableStateOf(MoneyEventEditorUiState(projectName = "测试项目",
            direction = MoneyDirection.OUTLAY, today = LocalDate.of(2026, 1, 10)))
        compose.setContent { LithicTheme {
            MoneyEventEditor(state, {}, { state = state.copy(direction = it) }, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("money-direction-outlay").assertIsSelected()
        compose.onNodeWithTag("money-selected-direction").assertTextContains("本次记录：投入")
        compose.onNodeWithTag("money-direction-recovery").performClick().assertIsSelected()
        compose.onNodeWithTag("money-selected-direction").assertTextContains("本次记录：收回")
        assertEquals(MoneyDirection.RECOVERY, state.direction)
    }

    @Test fun history_read_failure_identifies_preserved_rows_without_claiming_empty_history() {
        var opened: String? = null
        compose.setContent { LithicTheme {
            ProjectHistoryScreen(ProjectListUiState(items = listOf(detail()), totalCount = 1,
                loading = false, error = "历史暂时无法读取"), {}, { opened = it }, {}, {})
        } }
        compose.onNodeWithTag("project-history-stale").assertTextContains("上次读取记录", substring = true)
        compose.onNodeWithText("暂无已结束项目").assertDoesNotExist()
        compose.onNodeWithTag("project-history-row-$PROJECT_ID").performScrollTo().performClick()
        assertEquals(PROJECT_ID, opened)
    }

    @Test fun detail_explains_end_versus_delete_without_hiding_negative_recorded_cost() {
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ProjectDetailUiState(detail = detail(), loading = false), {}, {}, {}, {})
        } }
        compose.onNodeWithTag("project-detail-daily").performScrollTo().assertTextContains("−¥12.50")
        compose.onNodeWithTag("project-detail-content")
            .performScrollToNode(hasTestTag("project-lifecycle-explanation"))
        compose.onNodeWithTag("project-lifecycle-explanation")
            .assertTextContains("结束保留事实", substring = true).assertTextContains("删除会永久移除", substring = true)
            .assertIsDisplayed()
    }

    @Composable private fun EditorHost(state: ProjectEditorUiState, onSave: () -> Unit = {},
        onRetry: () -> Unit = {}) {
        ProjectEditorScreen(state, {}, {}, {}, {}, {}, {}, {}, onSave, onRetry)
    }

    private fun editor() = ProjectEditorUiState(projectId = PROJECT_ID, name = "测试项目",
        startedOn = "2026-01-01", initialAmount = "0", initialPaidOn = "2026-01-01",
        categoryId = LithicDatabase.PROJECT_FALLBACK_ID, categories = listOf(category()),
        today = LocalDate.of(2026, 1, 10))

    private fun category() = CategoryEntity(LithicDatabase.PROJECT_FALLBACK_ID,
        CategoryType.PROJECT, "其他", "其他", true)

    private fun detail(): ProjectDetail {
        val timestamp = Instant.parse("2026-01-01T00:00:00Z")
        return ProjectDetail(LongTermProjectEntity(PROJECT_ID, "测试项目", category().id,
            LocalDate.of(2026, 1, 1), endedOn = LocalDate.of(2026, 1, 8), endReason = "不再使用",
            createdAt = timestamp, updatedAt = timestamp), category(), emptyList(),
            ProjectCostResult(8, 10000, 20000, -10000, BigDecimal("-12.50"), false, null),
            LocalDate.of(2026, 1, 10))
    }

    private companion object {
        const val PROJECT_ID = "71000000-0000-0000-0000-000000000001"
    }
}
