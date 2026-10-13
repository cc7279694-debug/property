package com.cooper.lithic.feature.project

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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.CostUnavailableReason
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun create_form_exposes_real_payment_date_zero_amount_and_controlled_category() {
        var state by mutableStateOf(editor().copy(initialAmount = "0"))
        compose.setContent { LithicTheme { EditorHost(state, onChange = { state = it }) } }
        compose.onNodeWithTag("project-initial-amount").performScrollTo().assertTextContains("0")
        compose.onNodeWithTag("project-initial-paid-on-input").performScrollTo()
            .performTextReplacement("2025-12-20")
        compose.onNodeWithTag("project-initial-paid-on-input").assertTextContains("2025-12-20")
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithTag("project-category").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("category-$SECOND_CATEGORY_ID").assertIsDisplayed()
        compose.onNodeWithTag("category-$SECOND_CATEGORY_ID").performClick()
        compose.onNodeWithTag("project-category").assert(hasText("数码"))
        compose.onNodeWithText("订阅专属类别").assertDoesNotExist()
        assertEquals("2025-12-20", state.initialPaidOn)
        assertEquals(SECOND_CATEGORY_ID, state.categoryId)
    }

    @Test fun editing_project_details_does_not_offer_initial_money_history_changes() {
        compose.setContent { LithicTheme { EditorHost(editor().copy(mode = ProjectEditorMode.EDIT)) } }
        compose.onNodeWithTag("project-name").assertTextContains("测试项目")
        compose.onNodeWithTag("project-started-on-input").performScrollTo().assertTextContains("2026-01-01")
        compose.onNodeWithTag("project-initial-amount").assertDoesNotExist()
        compose.onNodeWithTag("project-initial-paid-on-input").assertDoesNotExist()
        compose.onNodeWithText("删除项目").assertDoesNotExist()
        compose.onNodeWithText("结束项目").assertDoesNotExist()
        compose.onNodeWithText("添加封面").assertDoesNotExist()
    }

    @Test fun in_flight_submission_disables_editing_and_duplicate_save_actions() {
        val saves = AtomicInteger()
        compose.setContent {
            LithicTheme { EditorHost(editor().copy(phase = SubmissionPhase.IN_FLIGHT), onSave = { saves.incrementAndGet() }) }
        }
        compose.onNodeWithTag("project-name").assertIsNotEnabled()
        compose.onNodeWithTag("project-save").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("正在保存到本地").performScrollTo().assertIsDisplayed()
        assertEquals(0, saves.get())
    }

    @Test fun failed_save_preserves_all_controlled_input_and_persistent_error() {
        val error = "本地保存失败，输入已保留，请核对后重试"
        val amountError = "金额最多保留两位小数"
        compose.setContent {
            LithicTheme { EditorHost(editor().copy(initialAmount = "1.234", note = "保留的备注",
                errors = mapOf("initialAmount" to amountError), message = error)) }
        }
        compose.onNodeWithTag("project-name").assertTextContains("测试项目")
        compose.onNodeWithTag("project-initial-amount").performScrollTo().assertTextContains("1.234")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, amountError))
        compose.onNodeWithTag("project-note").performScrollTo().assertTextContains("保留的备注")
        compose.onNodeWithTag("project-form-error").performScrollTo().assertTextContains(error)
        compose.onNodeWithTag("project-editor-screen").assertIsDisplayed()
    }

    @Test fun unconfirmed_submission_locks_content_but_allows_safe_replay_action() {
        val replays = AtomicInteger()
        compose.setContent {
            LithicTheme { EditorHost(editor().copy(phase = SubmissionPhase.UNCONFIRMED,
                message = "保存结果尚未确认，请核对原操作"), onSave = { replays.incrementAndGet() }) }
        }
        compose.onNodeWithTag("project-name").assertIsNotEnabled()
        compose.onNodeWithTag("project-save").performScrollTo().assertTextContains("核对并重试").performClick()
        assertEquals(1, replays.get())
    }

    @Test fun committed_write_with_failed_refresh_only_offers_read_retry() {
        val saves = AtomicInteger()
        val reads = AtomicInteger()
        compose.setContent {
            LithicTheme { EditorHost(editor().copy(phase = SubmissionPhase.COMMITTED,
                message = "已保存，暂时无法读取"), onSave = { saves.incrementAndGet() }, onRetryRead = { reads.incrementAndGet() }) }
        }
        compose.onNodeWithTag("project-name").assertIsNotEnabled()
        compose.onNodeWithTag("project-save").assertDoesNotExist()
        compose.onNodeWithTag("project-retry-read").performScrollTo().performClick()
        assertEquals(0, saves.get())
        assertEquals(1, reads.get())
    }

    @Test fun stale_edit_conflict_offers_explicit_reload_without_resubmitting_the_old_command() {
        val saves = AtomicInteger()
        val reads = AtomicInteger()
        compose.setContent {
            LithicTheme { EditorHost(editor().copy(mode = ProjectEditorMode.EDIT,
                phase = SubmissionPhase.UNCONFIRMED, conflict = true,
                message = "项目资料已改变，当前修改尚未提交"),
                onSave = { saves.incrementAndGet() }, onRetryRead = { reads.incrementAndGet() }) }
        }
        compose.onNodeWithTag("project-name").assertIsNotEnabled().assertTextContains("测试项目")
        compose.onNodeWithText("重新读取会用当前已提交资料替换此处输入，请重新核对后编辑。").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("project-resolve-conflict").performScrollTo().performClick()
        assertEquals(0, saves.get())
        assertEquals(1, reads.get())
    }

    @Test fun initial_edit_read_failure_preserves_raw_and_only_offers_read_recovery() {
        val saves = AtomicInteger()
        val reads = AtomicInteger()
        compose.setContent {
            LithicTheme { EditorHost(editor().copy(mode = ProjectEditorMode.EDIT,
                name = "尚未写入的名称", note = "恢复后保留的输入", needsInitialRead = true,
                loading = false, message = "项目资料暂时无法读取，输入已保留"),
                onSave = { saves.incrementAndGet() }, onRetryRead = { reads.incrementAndGet() }) }
        }
        compose.onNodeWithTag("project-name").assertTextContains("尚未写入的名称")
        compose.onNodeWithTag("project-note").performScrollTo().assertTextContains("恢复后保留的输入")
        compose.onNodeWithTag("project-save").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("project-initial-read-retry").performScrollTo().assertHeightIsAtLeast(48.dp)
            .assertTextContains("重新读取项目资料").performClick()
        assertEquals(0, saves.get())
        assertEquals(1, reads.get())
    }

    @Test fun money_editor_uses_absolute_amount_and_selected_outlay_or_recovery() {
        var state by mutableStateOf(MoneyEventEditorUiState(PROJECT_ID, "测试项目",
            amount = "1200", occurredOn = "2026-01-05", today = LocalDate.of(2026, 1, 10)))
        compose.setContent {
            LithicTheme {
                MoneyEventEditor(state, {}, { state = state.copy(direction = it) },
                    { state = state.copy(amount = it) }, { state = state.copy(occurredOn = it) },
                    { state = state.copy(description = it) }, {}, onRetryRead = {})
            }
        }
        compose.onNodeWithTag("money-direction-outlay").assertIsSelected().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("money-direction-recovery").performClick().assertIsSelected()
        compose.onNodeWithTag("money-amount").performScrollTo().assertTextContains("1200")
        compose.onNodeWithTag("money-occurred-on-input").performScrollTo().assertTextContains("2026-01-05")
        assertEquals(MoneyDirection.RECOVERY, state.direction)
        compose.onNodeWithText("维修分类").assertDoesNotExist()
        compose.onNodeWithText("未付余额").assertDoesNotExist()
    }

    @Test fun money_editor_preserves_failed_amount_date_and_description() {
        val state = MoneyEventEditorUiState(PROJECT_ID, "测试项目", amount = "1.234",
            occurredOn = "2026-01-20", description = "真实维修款", today = LocalDate.of(2026, 1, 10),
            errors = mapOf("amount" to "金额最多保留两位小数", "occurredOn" to "不能记录未来付款"),
            message = "本地保存失败，输入已保留")
        compose.setContent { LithicTheme { MoneyEventEditor(state, {}, {}, {}, {}, {}, {}, onRetryRead = {}) } }
        compose.onNodeWithTag("money-amount").performScrollTo().assertTextContains("1.234")
        compose.onNodeWithTag("money-occurred-on-input").performScrollTo().assertTextContains("2026-01-20")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "不能记录未来付款"))
        compose.onNodeWithTag("money-description").performScrollTo().assertTextContains("真实维修款")
        compose.onNodeWithTag("money-form-error").performScrollTo().assertTextContains("本地保存失败，输入已保留")
    }

    @Test fun project_list_opens_stable_identity_and_exposes_database_page_continuation() {
        var opened: String? = null
        val pages = AtomicInteger()
        compose.setContent {
            LithicTheme { ProjectListScreen(ProjectListUiState(listOf(detail()), 51, false,
                hasMore = true), {}, {}, { opened = it }, { pages.incrementAndGet() }, {}) }
        }
        compose.onNodeWithTag("project-row-$PROJECT_ID").performClick()
        assertEquals(PROJECT_ID, opened)
        compose.onNodeWithTag("project-list-content").performScrollToNode(hasTestTag("project-list-load-more"))
        compose.onNodeWithTag("project-list-load-more").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, pages.get())
    }

    @Test fun detail_shows_given_domain_results_and_real_event_dates_instead_of_recalculating() {
        compose.setContent { LithicTheme { ProjectDetailScreen(ProjectDetailUiState(detail(), false), {}, {}, {}, {}) } }
        compose.onNodeWithTag("project-detail-daily").assert(hasText("¥100.00"))
        compose.onNodeWithText("已持有10天").assertIsDisplayed()
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-event-$EVENT_ID"))
        compose.onNodeWithTag("project-event-$EVENT_ID").assert(hasText("2026-01-01", substring = true))
        compose.onNode(hasText("¥1000.00") and hasAnyAncestor(hasTestTag("project-event-$EVENT_ID")),
            useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun not_started_detail_has_no_effective_daily_cost_and_negative_net_is_preserved() {
        var state by mutableStateOf(ProjectDetailUiState(detail(cost = ProjectCostResult(
            0, 100000, 0, 100000, null, false, CostUnavailableReason.PROJECT_NOT_STARTED)), false))
        compose.setContent { LithicTheme { ProjectDetailScreen(state, {}, {}, {}, {}) } }
        compose.onNodeWithText("尚未开始").assertIsDisplayed()
        compose.onNodeWithTag("project-detail-daily").assert(hasText("—"))
        compose.runOnIdle {
            state = ProjectDetailUiState(detail(cost = ProjectCostResult(10, 100000, 120000,
                -20000, BigDecimal("-20"), true, null)), false)
        }
        compose.onNodeWithTag("project-detail-daily").assert(hasText("−¥20.00"))
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-detail-net"))
        compose.onNodeWithTag("project-detail-net").assert(hasText("−¥200.00"))
    }

    @Test fun failed_list_read_is_visible_and_keeps_prior_records_instead_of_a_fake_empty_state() {
        val message = "暂时无法读取，当前显示上次读取的记录"
        compose.setContent {
            LithicTheme { ProjectListScreen(ProjectListUiState(listOf(detail()), 1, false,
                error = message), {}, {}, {}, {}, {}) }
        }
        compose.onNodeWithTag("project-list-error").assertTextContains(message)
        compose.onNodeWithTag("project-row-$PROJECT_ID").assertIsDisplayed()
        compose.onNodeWithText("暂无长期项目").assertDoesNotExist()
    }

    @Test fun narrow_large_text_form_keeps_ime_error_save_and_back_actions_reachable() {
        val saves = AtomicInteger()
        val backs = AtomicInteger()
        val error = "请输入准确开始拥有日期，其他已输入的名称、金额和备注会保留。"
        var state by mutableStateOf(editor().copy(name = "", errors = mapOf("startedOn" to error)))
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                LithicTheme {
                    Surface(Modifier.fillMaxSize()) {
                        Box(Modifier.width(360.dp).fillMaxHeight()) {
                            EditorHost(state, { state = it }, { saves.incrementAndGet() }, { backs.incrementAndGet() })
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("project-name").performScrollTo().performClick().performTextInput("大字体测试项目")
        compose.onNodeWithText(error, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(error, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertFalse(layouts.single().hasVisualOverflow)
        compose.onNodeWithTag("project-save").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithTag("project-editor-back").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("大字体测试项目", state.name)
        assertEquals(1, saves.get())
        assertEquals(1, backs.get())
    }

    @Composable
    private fun EditorHost(
        state: ProjectEditorUiState,
        onChange: (ProjectEditorUiState) -> Unit = {},
        onSave: () -> Unit = {},
        onBack: () -> Unit = {},
        onRetryRead: () -> Unit = {},
    ) {
        ProjectEditorScreen(state, onBack,
            { onChange(state.copy(name = it)) }, { onChange(state.copy(startedOn = it)) },
            { onChange(state.copy(initialAmount = it)) }, { onChange(state.copy(initialPaidOn = it)) },
            { onChange(state.copy(categoryId = it)) }, { onChange(state.copy(note = it)) },
            onSave, onRetryRead = onRetryRead)
    }

    private fun editor() = ProjectEditorUiState(projectId = PROJECT_ID, name = "测试项目",
        startedOn = "2026-01-01", initialAmount = "1000", initialPaidOn = "2026-01-01",
        categoryId = LithicDatabase.PROJECT_FALLBACK_ID, categories = listOf(FALLBACK, SECOND,
            CategoryEntity(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, CategoryType.SUBSCRIPTION,
                "订阅专属类别", "订阅专属类别", true)), today = LocalDate.of(2026, 1, 10))

    private fun detail(cost: ProjectCostResult = ProjectCostResult(10, 100000, 0,
        100000, BigDecimal("100"), true, null)): ProjectDetail {
        val timestamp = Instant.parse("2026-01-01T00:00:00Z")
        return ProjectDetail(
            LongTermProjectEntity(PROJECT_ID, "测试项目", FALLBACK.id, LocalDate.of(2026, 1, 1),
                createdAt = timestamp, updatedAt = timestamp), FALLBACK,
            listOf(ProjectMoneyEventEntity(EVENT_ID, PROJECT_ID, MoneyDirection.OUTLAY, 100000,
                LocalDate.of(2026, 1, 1), "真实初始付款", ProjectMoneyEventRole.INITIAL, OPERATION_ID)),
            cost, LocalDate.of(2026, 1, 10),
        )
    }

    private companion object {
        const val PROJECT_ID = "10000000-0000-0000-0000-000000000001"
        const val EVENT_ID = "10000000-0000-0000-0000-000000000002"
        const val OPERATION_ID = "10000000-0000-0000-0000-000000000003"
        const val SECOND_CATEGORY_ID = "10000000-0000-0000-0000-000000000004"
        val FALLBACK = CategoryEntity(LithicDatabase.PROJECT_FALLBACK_ID, CategoryType.PROJECT, "其他", "其他", true)
        val SECOND = CategoryEntity(SECOND_CATEGORY_ID, CategoryType.PROJECT, "数码", "数码", false)
    }
}
