package com.cooper.lithic.feature.project

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.LedgerUiState
import com.cooper.lithic.app.LedgerUiStateHolder
import com.cooper.lithic.app.LithicApp
import com.cooper.lithic.app.StorageInitializationState
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.CreateProjectCommand
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ProjectRepository
import java.io.File
import java.io.IOException
import java.time.Instant
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectNavigationGuardTest {
    @get:Rule val compose = createComposeRule()
    private val clock = FixedBusinessClock(LocalDate.of(2026, 1, 10))
    private val viewModels = ViewModelStore()
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var holder: LedgerUiStateHolder
    private lateinit var model: ProjectViewModel
    private lateinit var repository: ProjectRepository

    @Before fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.noBackupFilesDir, "m06-nav-guard-${UUID.randomUUID()}")
        check(root.mkdirs())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        val states = MutableStateFlow<StorageInitializationState>(StorageInitializationState.Ready(generation))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            holder = LedgerUiStateHolder(states) { clock }
            viewModels.put("ledger", holder)
        }
        repository = withTimeout(15_000) { holder.state.filterIsInstance<LedgerUiState.Ready>().first() }.repository
    }

    @After fun tearDown() = runBlocking {
        val jobs = listOfNotNull(holder.viewModelScope.coroutineContext[Job],
            if (::model.isInitialized) model.viewModelScope.coroutineContext[Job] else null)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModels.clear() }
        jobs.forEach { it.join() }
        generation.close()
        check(root.deleteRecursively())
    }

    @Test fun dirty_create_form_guards_same_tab_other_tabs_toolbar_and_system_back() = runBlocking {
        render()
        openCreate()
        compose.onNodeWithTag("project-name").performScrollTo().performTextReplacement("未提交名称")
        compose.onNodeWithTag("project-initial-amount").performScrollTo().performTextReplacement("12.34")
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        val actions: List<() -> Unit> = listOf(
            { compose.onNodeWithTag("tab-now").performClick() },
            { compose.onNodeWithTag("tab-review").performClick() },
            { compose.onNodeWithTag("tab-settings").performClick() },
            { compose.onNodeWithTag("project-editor-back").performScrollTo().performClick() },
            { Espresso.pressBack() },
        )
        actions.forEach { leave ->
            leave()
            compose.onNodeWithTag("discard-dialog").assertIsDisplayed()
            compose.onNodeWithTag("discard-keep").performClick()
            compose.onNodeWithTag("project-editor-screen").assertIsDisplayed()
            assertEquals("未提交名称", model.state.value.editor!!.name)
            assertEquals("12.34", model.state.value.editor!!.initialAmount)
        }
        assertEquals(0L, repository.loadSummary().projectCount)
        compose.onNodeWithTag("tab-review").performClick()
        compose.onNodeWithTag("discard-confirm").performClick()
        compose.onNodeWithTag("review-screen").assertIsDisplayed()
        assertEquals(0L, repository.loadSummary().projectCount)
    }

    @Test fun dirty_money_form_uses_the_same_guard_and_discard_does_not_write_an_event() = runBlocking {
        val id = seedProject()
        render()
        compose.onNodeWithTag("open-project-list").performScrollTo().performClick()
        compose.waitUntil(10_000) { !model.state.value.list.loading }
        compose.onNodeWithTag("project-row-$id").performClick()
        compose.waitUntil(10_000) { model.state.value.detail.detail != null }
        detailAction("project-add-outlay")
        compose.waitUntil(10_000) { model.state.value.moneyEditor != null }
        compose.onNodeWithTag("money-amount").performScrollTo().performTextReplacement("200")
        compose.onNodeWithTag("money-description").performScrollTo().performTextReplacement("未提交说明")
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        val actions: List<() -> Unit> = listOf(
            { compose.onNodeWithTag("tab-now").performClick() },
            { compose.onNodeWithTag("tab-settings").performClick() },
            { compose.onNodeWithTag("money-editor-back").performScrollTo().performClick() },
            { Espresso.pressBack() },
        )
        actions.forEach { leave ->
            leave()
            compose.onNodeWithTag("discard-dialog").assertIsDisplayed()
            compose.onNodeWithTag("discard-keep").performClick()
            compose.onNodeWithTag("money-editor-screen").assertIsDisplayed()
            assertEquals("200", model.state.value.moneyEditor!!.amount)
            assertEquals("未提交说明", model.state.value.moneyEditor!!.description)
        }
        compose.onNodeWithTag("money-editor-back").performScrollTo().performClick()
        compose.onNodeWithTag("discard-confirm").performClick()
        compose.onNodeWithTag("project-detail-screen").assertIsDisplayed()
        assertEquals(1, repository.loadProjectDetail(id)!!.events.size)
    }

    @Test fun untouched_form_leaves_without_a_discard_prompt_or_a_business_record() = runBlocking {
        render()
        openCreate()
        assertEquals(false, model.state.value.editor!!.dirty)
        Espresso.pressBack()
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
        compose.onNodeWithTag("discard-dialog").assertDoesNotExist()
        assertEquals(0L, repository.loadSummary().projectCount)
    }

    @Test fun unconfirmed_real_commit_blocks_leaving_and_replays_the_same_written_project() = runBlocking {
        val lossy = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            private var loseResult = true
            override suspend fun createProject(command: CreateProjectCommand): MutationResult {
                val result = super.createProject(command)
                if (loseResult) { loseResult = false; throw IOException("test result delivery interrupted after commit") }
                return result
            }
        }
        render(lossy)
        openCreate()
        compose.onNodeWithTag("project-name").performScrollTo().performTextReplacement("真实已写但结果丢失")
        compose.onNodeWithTag("project-initial-amount").performScrollTo().performTextReplacement("1000")
        compose.onNodeWithTag("project-save").performScrollTo().performClick()
        compose.waitUntil(15_000) { model.state.value.editor?.phase == SubmissionPhase.UNCONFIRMED }
        val before = repository.loadProjectPage().items.single()
        assertEquals(1, before.events.size)
        assertEquals(100000L, before.cost.netCents)
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("tab-review").performClick()
        compose.onNodeWithTag("pending-submit-dialog").assertIsDisplayed()
        compose.onNodeWithTag("discard-dialog").assertDoesNotExist()
        compose.onNodeWithTag("pending-submit-keep").performClick()
        Espresso.pressBack()
        compose.onNodeWithTag("pending-submit-dialog").assertIsDisplayed()
        compose.onNodeWithTag("pending-submit-keep").performClick()
        compose.onNodeWithTag("project-save").performScrollTo().assertTextContains("核对并重试").performClick()
        compose.waitUntil(15_000) { model.state.value.editor == null && model.state.value.detail.detail != null }
        compose.onNodeWithTag("project-detail-screen").assertIsDisplayed()
        val after = repository.loadProjectPage().items.single()
        assertEquals(before.project.id, after.project.id)
        assertEquals(before.events, after.events)
        assertEquals(1L, repository.loadSummary().projectCount)
    }

    private fun render(projectRepository: ProjectRepository = repository) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            model = ProjectViewModel(projectRepository, SavedStateHandle(), clock)
            viewModels.put("project", model)
        }
        compose.setContent {
            LithicTheme {
                val ledger by holder.state.collectAsState()
                LithicApp(ledger, {}, model)
            }
        }
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
    }

    private fun openCreate() {
        compose.onNodeWithTag("home-create-project").performScrollTo().performClick()
        compose.waitUntil(10_000) { model.state.value.editor?.today != null }
        compose.onNodeWithTag("project-editor-screen").assertIsDisplayed()
    }

    private fun detailAction(tag: String) {
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performClick()
    }

    private suspend fun seedProject(): String {
        val id = UUID.randomUUID().toString()
        val command = UUID.randomUUID().toString()
        val time = Instant.parse("2026-01-01T00:00:00Z")
        val result = repository.createProject(CreateProjectCommand(command,
            LongTermProjectEntity(id, "真实导航守卫项目", LithicDatabase.PROJECT_FALLBACK_ID,
                LocalDate.of(2026, 1, 1), createdAt = time, updatedAt = time),
            ProjectMoneyEventEntity(UUID.randomUUID().toString(), id, MoneyDirection.OUTLAY,
                100000, LocalDate.of(2026, 1, 1), role = ProjectMoneyEventRole.INITIAL, operationId = command)))
        assertTrue(result is MutationResult.Success)
        return id
    }
}
