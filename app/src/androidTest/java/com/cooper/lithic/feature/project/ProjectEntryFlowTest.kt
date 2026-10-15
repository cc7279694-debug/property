package com.cooper.lithic.feature.project

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.LedgerUiState
import com.cooper.lithic.app.LedgerUiStateHolder
import com.cooper.lithic.app.LithicApp
import com.cooper.lithic.app.StorageInitializationState
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.ui.openHomeCreate
import com.cooper.lithic.ui.projectDetailAction
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.LedgerReadRepository
import com.cooper.lithic.data.repository.ProjectRepository
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Every action goes through production navigation, ViewModel and the fixture's real Room. */
@RunWith(AndroidJUnit4::class)
class ProjectEntryFlowTest {
    @get:Rule val compose = createComposeRule()
    private val clock = FixedBusinessClock(LocalDate.of(2026, 1, 10))
    private val viewModels = ViewModelStore()
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var holder: LedgerUiStateHolder
    private lateinit var projectViewModel: ProjectViewModel
    private lateinit var repository: ProjectRepository
    private val showApp = mutableStateOf(true)

    @Before fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.noBackupFilesDir, "m06-project-flow-${UUID.randomUUID()}")
        check(root.mkdirs())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        val storage = MutableStateFlow<StorageInitializationState>(StorageInitializationState.Ready(generation))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            holder = LedgerUiStateHolder(storage) { clock }
            viewModels.put("ledger", holder)
        }
        val ready = withTimeout(15_000) { holder.state.filterIsInstance<LedgerUiState.Ready>().first() }
        repository = ready.repository
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            projectViewModel = ProjectViewModel(repository, SavedStateHandle(), clock)
            viewModels.put("projects", projectViewModel)
        }
    }

    @After fun tearDown() = runBlocking {
        clearUiOwners()
        if (::generation.isInitialized) generation.close()
        if (::root.isInitialized) check(root.deleteRecursively())
    }

    @Test fun create_zero_and_normal_project_are_real_room_records_after_reopen() = runBlocking {
        render()
        createFromHome("真实零元项目", "0")
        val zeroId = projectViewModel.state.value.detail.detail!!.project.id
        detailAction("project-detail-back")
        compose.onNodeWithTag("project-list-create").performClick()
        fillCreate("真实普通项目", "123.45")
        saveProjectAndWaitForDetail()
        val normalId = projectViewModel.state.value.detail.detail!!.project.id
        assertNotEquals(zeroId, normalId)
        val snapshot = LedgerReadRepository(generation.database, generation.coordinator).snapshot()
        assertEquals(2, snapshot.projects.size)
        assertEquals(2, snapshot.moneyEvents.size)
        assertEquals(setOf(0L, 12345L), snapshot.moneyEvents.map { it.amountCents }.toSet())
        returnHome()
        compose.onNodeWithContentDescription("已录入 2 条长期项目").assertIsDisplayed()
        reopenFixture()
        assertEquals(2L, repository.loadSummary().projectCount)
        assertEquals(0L, repository.loadProjectDetail(zeroId)!!.cost.netCents)
        assertEquals(12345L, repository.loadProjectDetail(normalId)!!.cost.netCents)
        assertEquals(ZoneId.of("Asia/Shanghai"), generation.settings.recordZone())
    }

    @Test fun initial_cost_counted_once_in_room_and_after_close_reopen() = runBlocking {
        render()
        createFromHome("一次初始投入", "1000")
        val id = projectViewModel.state.value.detail.detail!!.project.id
        compose.onNodeWithTag("project-detail-daily").assert(hasText("¥100.00"))
        val detail = repository.loadProjectDetail(id)!!
        assertEquals(1, detail.events.size)
        assertEquals(ProjectMoneyEventRole.INITIAL, detail.events.single().role)
        assertEquals(100000L, detail.cost.grossOutlayCents)
        assertEquals(10L, detail.cost.heldDays)
        assertEquals(0, BigDecimal("100").compareTo(detail.cost.dailyRate))
        val eventId = detail.events.single().id
        reopenFixture()
        val reopened = repository.loadProjectDetail(id)!!
        assertEquals(eventId, reopened.events.single().id)
        assertEquals(100000L, reopened.cost.grossOutlayCents)
        assertEquals(0, BigDecimal("100").compareTo(reopened.cost.dailyRate))
    }

    @Test fun rapid_double_tap_creates_only_one_real_project_and_one_appended_event() = runBlocking {
        render()
        compose.openHomeCreate("home-create-project")
        waitForEditor()
        fillCreate("连点只记一笔", "1000")
        compose.onNodeWithTag("project-save").performScrollTo().performTouchInput { doubleClick() }
        waitForDetail()
        val id = projectViewModel.state.value.detail.detail!!.project.id
        assertEquals(1L, repository.loadSummary().projectCount)
        assertEquals(1, repository.loadProjectDetail(id)!!.events.size)
        detailAction("project-add-outlay")
        waitForMoneyEditor()
        fillMoney("200", "2026-01-05", "真实历史投入")
        compose.onNodeWithTag("money-save").performScrollTo().performTouchInput { doubleClick() }
        waitForDetail()
        val persisted = repository.loadProjectDetail(id)!!
        assertEquals(2, persisted.events.size)
        assertEquals(1, persisted.events.count { it.amountCents == 20000L })
        assertEquals(120000L, persisted.cost.grossOutlayCents)
    }

    @Test fun failed_save_preserves_form_and_canceling_discard_keeps_the_real_unsubmitted_input() = runBlocking {
        render()
        compose.openHomeCreate("home-create-project")
        waitForEditor()
        fillCreate("保留失败输入", "1.234")
        compose.onNodeWithTag("project-note").performScrollTo().performTextReplacement("失败后保留备注")
        compose.onNodeWithTag("project-save").performScrollTo().performClick()
        compose.waitUntil(10_000) { projectViewModel.state.value.editor?.errors?.isNotEmpty() == true }
        compose.onNodeWithTag("project-initial-amount").performScrollTo().assertTextContains("1.234")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        assertEquals(0L, repository.loadSummary().projectCount)
        compose.onNodeWithTag("project-editor-back").performScrollTo().performClick()
        compose.onNodeWithTag("discard-dialog").assertIsDisplayed()
        compose.onNodeWithTag("discard-keep").performClick()
        compose.onNodeWithTag("project-initial-amount").performScrollTo().assertTextContains("1.234")
        compose.onNodeWithTag("project-note").performScrollTo().assertTextContains("失败后保留备注")
        compose.onNodeWithTag("project-initial-amount").performScrollTo().performTextReplacement("1000")
        saveProjectAndWaitForDetail()
        val detail = repository.loadProjectDetail(projectViewModel.state.value.detail.detail!!.project.id)!!
        assertEquals(1, detail.events.size)
        assertEquals("失败后保留备注", detail.project.note)
    }

    @Test fun backdated_money_recalculates_detail_list_home_and_negative_recovery_from_real_room() = runBlocking {
        render()
        createFromHome("历史投入和收回", "1000")
        val id = projectViewModel.state.value.detail.detail!!.project.id
        detailAction("project-add-outlay")
        waitForMoneyEditor()
        fillMoney("200", "2026-01-05", "补录真实投入")
        compose.onNodeWithTag("money-save").performScrollTo().performClick()
        waitForDetail()
        compose.onNodeWithTag("project-detail-daily").assert(hasText("¥120.00"))
        detailAction("project-detail-back")
        waitForList()
        assertRowAmount(id, "¥120.00")
        compose.onNodeWithTag("project-list-back").performClick()
        compose.onNodeWithContentDescription("已录入 1 条长期项目").assertIsDisplayed()
        compose.onNodeWithTag("open-project-list").performScrollTo().performClick()
        waitForList()
        compose.onNodeWithTag("project-row-$id").performClick()
        waitForDetail()
        detailAction("project-add-recovery")
        waitForMoneyEditor()
        fillMoney("1400", "2026-01-08", "真实收回")
        compose.onNodeWithTag("money-save").performScrollTo().performClick()
        waitForDetail()
        compose.onNodeWithTag("project-detail-daily").assert(hasText("−¥20.00"))
        assertEquals(100000L, repository.loadProjectDetail(id, LocalDate.of(2026, 1, 4))!!.cost.netCents)
        assertEquals(120000L, repository.loadProjectDetail(id, LocalDate.of(2026, 1, 5))!!.cost.netCents)
        assertEquals(-20000L, repository.loadProjectDetail(id)!!.cost.netCents)
        detailAction("project-detail-back")
        waitForList()
        assertRowAmount(id, "−¥20.00")
        reopenFixture()
        assertEquals(-20000L, repository.loadProjectDetail(id)!!.cost.netCents)
    }

    @Test fun editing_name_and_start_date_updates_visible_results_without_changing_money_history(): Unit = runBlocking {
        render()
        createFromHome("修改前名称", "1000")
        val id = projectViewModel.state.value.detail.detail!!.project.id
        val before = repository.loadProjectDetail(id)!!
        detailAction("project-edit")
        waitForEditor()
        compose.onNodeWithTag("project-name").performScrollTo().performTextReplacement("修订后的项目")
        compose.onNodeWithTag("project-started-on-input").performScrollTo().performTextReplacement("2026-01-06")
        compose.onNodeWithTag("project-initial-amount").assertDoesNotExist()
        saveProjectAndWaitForDetail()
        compose.onNodeWithTag("project-detail-name").assertTextContains("修订后的项目")
        compose.onNodeWithText("已持有5天").assertIsDisplayed()
        compose.onNodeWithTag("project-detail-daily").assert(hasText("¥200.00"))
        val after = repository.loadProjectDetail(id)!!
        assertEquals(before.project.id, after.project.id)
        assertEquals(before.project.createdAt, after.project.createdAt)
        assertEquals(before.events, after.events)
        assertTrue(after.project.updatedAt.isAfter(before.project.updatedAt))
        detailAction("project-detail-back")
        waitForList()
        compose.onNodeWithTag("project-row-$id").assert(hasText("修订后的项目"))
        assertRowAmount(id, "¥200.00")
        compose.onNodeWithTag("project-list-back").performClick()
        compose.onNodeWithContentDescription("已录入 1 条长期项目").assertIsDisplayed()
    }

    private fun render() {
        compose.setContent {
            LithicTheme {
                val ledger by holder.state.collectAsState()
                if (showApp.value) LithicApp(ledger, {}, projectViewModel)
            }
        }
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
    }

    private fun createFromHome(name: String, amount: String) {
        compose.openHomeCreate("home-create-project")
        waitForEditor()
        fillCreate(name, amount)
        saveProjectAndWaitForDetail()
    }

    private fun fillCreate(name: String, amount: String) {
        compose.onNodeWithTag("project-name").performScrollTo().performTextReplacement(name)
        compose.onNodeWithTag("project-started-on-input").performScrollTo().performTextReplacement("2026-01-01")
        compose.onNodeWithTag("project-initial-amount").performScrollTo().performTextReplacement(amount)
    }

    private fun fillMoney(amount: String, date: String, description: String) {
        compose.onNodeWithTag("money-amount").performScrollTo().performTextReplacement(amount)
        compose.onNodeWithTag("money-occurred-on-input").performScrollTo().performTextReplacement(date)
        compose.onNodeWithTag("money-description").performScrollTo().performTextReplacement(description)
    }

    private fun saveProjectAndWaitForDetail() {
        val expectedId = requireNotNull(projectViewModel.state.value.editor).projectId
        compose.onNodeWithTag("project-save").performScrollTo().performClick()
        waitForDetail(expectedId)
    }

    private fun detailAction(tag: String) {
        compose.projectDetailAction(tag)
    }

    private fun returnHome() {
        detailAction("project-detail-back")
        waitForList()
        compose.onNodeWithTag("project-list-back").performClick()
    }

    private fun waitForEditor() {
        compose.waitUntil(10_000) { projectViewModel.state.value.editor?.let { !it.loading && it.today != null } == true }
        compose.onNodeWithTag("project-editor-screen").assertIsDisplayed()
    }

    private fun waitForMoneyEditor() {
        compose.waitUntil(10_000) { projectViewModel.state.value.moneyEditor?.today != null }
        compose.onNodeWithTag("money-editor-screen").assertIsDisplayed()
    }

    private fun waitForDetail(expectedId: String? = null) {
        compose.waitUntil(15_000) {
            projectViewModel.state.value.let { it.editor == null && it.moneyEditor == null && !it.detail.loading && it.detail.detail != null &&
                (expectedId == null || it.detail.detail.project.id == expectedId) }
        }
        compose.onNodeWithTag("project-detail-screen").assertIsDisplayed()
    }

    private fun waitForList() {
        compose.waitUntil(10_000) { !projectViewModel.state.value.list.loading }
        compose.onNodeWithTag("project-list-screen").assertIsDisplayed()
    }

    private fun assertRowAmount(projectId: String, exactAmount: String) {
        compose.onNode(hasText(exactAmount) and hasAnyAncestor(hasTestTag("project-row-$projectId")),
            useUnmergedTree = true).assertIsDisplayed()
    }

    private suspend fun reopenFixture() {
        compose.runOnIdle { showApp.value = false }
        clearUiOwners()
        generation.close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        generation = DataGenerationStore(context, root).open(ZoneId.of("Pacific/Honolulu"))
        repository = ProjectRepository(generation.database, generation.coordinator, clock)
    }

    private suspend fun clearUiOwners() {
        val jobs = listOfNotNull(
            if (::holder.isInitialized) holder.viewModelScope.coroutineContext[Job] else null,
            if (::projectViewModel.isInitialized) projectViewModel.viewModelScope.coroutineContext[Job] else null,
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModels.clear() }
        jobs.forEach { it.join() }
    }
}
