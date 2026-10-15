package com.cooper.lithic.feature.project

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
import com.cooper.lithic.ui.openHomeCreate
import com.cooper.lithic.ui.projectDetailAction
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.DeleteProjectCommand
import com.cooper.lithic.data.repository.LedgerReadRepository
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ProjectRepository
import java.io.File
import java.io.IOException
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** All user mutations use production Nav3 and the production ViewModel over actual Room. */
@RunWith(AndroidJUnit4::class)
class ProjectLifecycleFlowTest {
    @get:Rule val compose = createComposeRule()
    private val clock = FixedBusinessClock(LocalDate.of(2026, 1, 10))
    private val viewModels = ViewModelStore()
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var holder: LedgerUiStateHolder
    private lateinit var model: ProjectViewModel
    private lateinit var savedState: SavedStateHandle
    private lateinit var repository: ProjectRepository
    private val activeModel = mutableStateOf<ProjectViewModel?>(null)
    private val showApp = mutableStateOf(true)

    @Before fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.noBackupFilesDir, "m07-lifecycle-flow-${UUID.randomUUID()}")
        check(root.mkdirs())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        val storage = MutableStateFlow<StorageInitializationState>(StorageInitializationState.Ready(generation))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            holder = LedgerUiStateHolder(storage) { clock }
            viewModels.put("ledger", holder)
        }
        repository = withTimeout(15_000) { holder.state.filterIsInstance<LedgerUiState.Ready>().first() }.repository
    }

    @After fun tearDown() = runBlocking {
        clearUiOwners()
        if (::generation.isInitialized) generation.close()
        if (::root.isInitialized) check(root.deleteRecursively())
    }

    @Test fun end_and_recovery_atomic(): Unit = runBlocking {
        render()
        val id = createProject("结束与回收原子写入", "1000")
        val before = repository.loadProjectDetail(id)!!
        generation.database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER m07_flow_fail_end BEFORE INSERT ON project_money_events " +
                "WHEN NEW.role = 'END_SETTLEMENT' BEGIN SELECT RAISE(ABORT, 'synthetic settlement failure'); END",
        )
        openEnd()
        fillEnd("2026-01-08", "不再使用", "200")
        clickEndSave()
        compose.waitUntil(15_000) { model.state.value.endEditor?.let { !it.busy && it.message != null } == true }
        val failed = repository.loadProjectDetail(id)!!
        assertEquals(before.project, failed.project)
        assertEquals(before.events, failed.events)
        compose.onNodeWithTag("end-form-error").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("end-ended-on-input").performScrollTo().assertTextContains("2026-01-08")
        compose.onNodeWithTag("end-reason").performScrollTo().assertTextContains("不再使用")
        compose.onNodeWithTag("end-recovery-amount").performScrollTo().assertTextContains("200")
        assertTrue(model.state.value.endEditor!!.conflict)
        generation.database.openHelper.writableDatabase.execSQL("DROP TRIGGER m07_flow_fail_end")
        compose.onNodeWithTag("end-resolve-conflict").performScrollTo().performClick()
        compose.waitUntil(10_000) { model.state.value.endEditor?.let { !it.loading && !it.conflict && it.phase == SubmissionPhase.IDLE } == true }
        assertNull(repository.loadProjectDetail(id)!!.project.endedOn)
        fillEnd("2026-01-08", "不再使用", "200")
        saveEnd()
        val after = repository.loadProjectDetail(id)!!
        assertEquals(LocalDate.of(2026, 1, 8), after.project.endedOn)
        assertEquals(2, after.events.size)
        assertEquals(1, after.events.count { it.role == ProjectMoneyEventRole.END_SETTLEMENT })
        assertEquals(MoneyDirection.RECOVERY, after.events.single { it.role == ProjectMoneyEventRole.END_SETTLEMENT }.direction)
        assertEquals(80000L, after.cost.netCents)
        compose.onNodeWithTag("project-detail-daily").assert(hasText("¥100.00"))
    }

    @Test fun edit_end_recovery_not_duplicated(): Unit = runBlocking {
        render()
        val id = createProject("多次修正原结束回收", "1000")
        endProject("2026-01-05", "200")
        val original = repository.loadProjectDetail(id)!!.events.single { it.role == ProjectMoneyEventRole.END_SETTLEMENT }
        for (amount in listOf("300", "250")) {
            detailAction("project-update-end")
            waitForEnd()
            compose.onNodeWithTag("end-record-recovery").assertDoesNotExist()
            compose.onNodeWithTag("end-recovery-amount").performScrollTo().performTextReplacement(amount)
            val ready = model.state.value.endEditor!!
            assertEquals("The controlled amount must reach the production model", amount, ready.recoveryAmount)
            assertEquals("2026-01-05", ready.endedOn)
            assertEquals("2026-01-05", ready.recoveryOn)
            assertEquals("不再使用", ready.endReason)
            assertEquals(SubmissionPhase.IDLE, ready.phase)
            assertTrue(ready.correcting && ready.hasSettlement)
            assertTrue(ready.errors.isEmpty())
            saveEnd("linked end correction to $amount")
        }
        val after = repository.loadProjectDetail(id)!!
        val corrected = after.events.single { it.role == ProjectMoneyEventRole.END_SETTLEMENT }
        assertEquals(2, after.events.size)
        assertEquals(original.id, corrected.id)
        assertEquals(original.operationId, corrected.operationId)
        assertEquals(original.projectId, corrected.projectId)
        assertEquals(original.direction, corrected.direction)
        assertEquals(25000L, corrected.amountCents)
        assertEquals(75000L, after.cost.netCents)
        assertEquals(5L, after.cost.heldDays)
        reopenFixture()
        assertEquals(corrected, repository.loadProjectDetail(id)!!.events.single { it.role == ProjectMoneyEventRole.END_SETTLEMENT })
    }

    @Test fun late_installment_does_not_reactivate(): Unit = runBlocking {
        render()
        val id = createProject("结束后真实尾款", "1000")
        endProject("2026-01-05", "200")
        addMoney("project-add-outlay", "100", "2026-01-10", "实际支付尾款")
        val detail = repository.loadProjectDetail(id)!!
        assertEquals(5L, detail.cost.heldDays)
        assertEquals(90000L, detail.cost.netCents)
        assertFalse(detail.cost.contributesToCurrentTotal)
        compose.onNodeWithTag("project-detail-daily").assert(hasText("¥180.00"))
        detailAction("project-detail-back")
        waitForCurrent()
        assertFalse(model.state.value.list.items.any { it.project.id == id })
        openHistory()
        compose.onNodeWithTag("project-history-row-$id").performClick()
        waitForDetail()
        compose.onNodeWithTag("project-detail-back").assertTextContains("返回历史档案")
        compose.onNodeWithTag("tab-review").assertIsSelected()
        assertEquals(5L, model.state.value.detail.detail!!.cost.heldDays)
    }

    @Test fun correct_end_date_updates_result(): Unit = runBlocking {
        render()
        val id = createProject("修正真实结束日期", "1000")
        endProject("2026-01-05")
        assertEquals(0, BigDecimal("200").compareTo(repository.loadProjectDetail(id)!!.cost.dailyRate))
        detailAction("project-update-end")
        waitForEnd()
        compose.onNodeWithTag("end-ended-on-input").performScrollTo().performTextReplacement("2025-12-31")
        clickEndSave()
        compose.waitUntil(10_000) { model.state.value.endEditor?.errors?.containsKey("endedOn") == true }
        compose.onNodeWithTag("end-ended-on-input").performScrollTo()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)).assertTextContains("2025-12-31")
        assertEquals(LocalDate.of(2026, 1, 5), repository.loadProjectDetail(id)!!.project.endedOn)
        compose.onNodeWithTag("end-ended-on-input").performTextReplacement("2026-01-08")
        saveEnd()
        val after = repository.loadProjectDetail(id)!!
        assertEquals(LocalDate.of(2026, 1, 8), after.project.endedOn)
        assertEquals(8L, after.cost.heldDays)
        assertEquals(0, BigDecimal("125").compareTo(after.cost.dailyRate))
        assertEquals(1, after.events.size)
        compose.onNodeWithTag("project-detail-daily").assert(hasText("¥125.00"))
    }

    @Test fun delete_requires_confirmation(): Unit = runBlocking {
        render()
        val id = createProject("删除需要明确二次确认", "1000")
        endProject("2026-01-05", "200")
        detailAction("project-detail-back")
        waitForCurrent()
        openHistory()
        compose.onNodeWithTag("project-history-row-$id").performClick()
        waitForDetail()
        val before = repository.loadProjectDetail(id)!!
        detailAction("project-delete")
        waitForDelete()
        compose.onNodeWithText("将删除项目及金额记录，历史统计也会重算；无法在App内撤回。").assertIsDisplayed()
        compose.onNodeWithTag("project-delete-cancel").performClick()
        compose.onNodeWithTag("project-detail-screen").assertIsDisplayed()
        assertEquals(before, repository.loadProjectDetail(id))
        detailAction("project-delete")
        waitForDelete()
        compose.onNodeWithTag("project-delete-confirm").performClick()
        compose.waitUntil(15_000) { model.state.value.deleteConfirmation == null && model.state.value.history.totalCount == 0L }
        compose.onNodeWithTag("project-history-screen").assertIsDisplayed()
        compose.onNodeWithTag("tab-review").assertIsSelected()
        assertNull(repository.loadProjectDetail(id))
        val snapshot = LedgerReadRepository(generation.database, generation.coordinator).snapshot()
        assertTrue(snapshot.projects.isEmpty())
        assertTrue(snapshot.moneyEvents.isEmpty())
        compose.onNodeWithText("暂无已结束项目").assertIsDisplayed()
        reopenFixture()
        assertEquals(0L, repository.loadSummary().projectCount)
        assertNull(repository.loadProjectDetail(id))
    }

    @Test fun history_correction_and_real_refund_are_distinct_and_preserve_original_identity(): Unit = runBlocking {
        render()
        val id = createProject("纠错与真实退款分开", "1000")
        addMoney("project-add-outlay", "200", "2026-01-03", "原录入投入")
        endProject("2026-01-05")
        detailAction("project-detail-back")
        waitForCurrent()
        openHistory()
        compose.onNodeWithTag("project-history-row-$id").performClick()
        waitForDetail()
        val original = repository.loadProjectDetail(id)!!.events.single { it.role == ProjectMoneyEventRole.REGULAR }
        detailAction("project-correct-event-${original.id}")
        waitForCorrection()
        compose.onNodeWithTag("correction-direction").assertTextContains("方向：投入")
        compose.onNodeWithTag("correction-amount").performScrollTo().performTextReplacement("100")
        compose.onNodeWithTag("correction-occurred-on-input").performScrollTo().performTextReplacement("2026-01-02")
        compose.onNodeWithTag("correction-description").performScrollTo().performTextReplacement("纠正录入金额及日期")
        compose.onNodeWithTag("correction-save").performScrollTo().performClick()
        waitForDetail()
        val corrected = repository.loadProjectDetail(id)!!.events.single { it.id == original.id }
        assertEquals(original.id, corrected.id)
        assertEquals(original.operationId, corrected.operationId)
        assertEquals(original.projectId, corrected.projectId)
        assertEquals(original.role, corrected.role)
        assertEquals(original.direction, corrected.direction)
        assertEquals(10000L, corrected.amountCents)
        assertEquals(LocalDate.of(2026, 1, 2), corrected.occurredOn)
        assertEquals(2, repository.loadProjectDetail(id)!!.events.size)
        addMoney("project-add-recovery", "50", "2026-01-10", "真实收到退款")
        val after = repository.loadProjectDetail(id)!!
        assertEquals(3, after.events.size)
        assertEquals(corrected, after.events.single { it.id == original.id })
        val refund = after.events.single { it.direction == MoneyDirection.RECOVERY }
        assertEquals(ProjectMoneyEventRole.REGULAR, refund.role)
        assertEquals(5000L, refund.amountCents)
        assertEquals(105000L, after.cost.netCents)
        assertEquals(5L, after.cost.heldDays)
        assertFalse(after.cost.contributesToCurrentTotal)
        detailAction("project-detail-back")
        compose.onNodeWithTag("project-history-screen").assertIsDisplayed()
        compose.onNodeWithTag("tab-review").assertIsSelected()
    }

    @Test fun saved_instance_route_and_vm_bundle_restore_end_input_without_business_write(): Unit = runBlocking {
        val restoration = render(restorable = true)!!
        val id = createProject("恢复未提交结束输入", "1000")
        openEnd()
        compose.onNodeWithTag("end-ended-on-input").performScrollTo().performTextReplacement("2026-01-")
        compose.onNodeWithTag("end-reason").performScrollTo().performTextReplacement("保留原因")
        compose.onNodeWithTag("end-record-recovery").performScrollTo().performClick()
        compose.onNodeWithTag("end-recovery-amount").performScrollTo().performTextReplacement("123.456")
        Espresso.closeSoftKeyboard()
        val copy = SavedStateHandle(savedState.keys().associateWith { key ->
            val value = savedState.get<Any?>(key)
            if (value is Bundle) Bundle(value) else value
        })
        val oldJob = model.viewModelScope.coroutineContext[Job]
        compose.runOnIdle {
            savedState = copy
            model = ProjectViewModel(repository, savedState, clock)
            viewModels.put("projects", model)
            activeModel.value = model
        }
        oldJob?.join()
        restoration.emulateSavedInstanceStateRestore()
        waitForEnd()
        compose.onNodeWithTag("end-ended-on-input").performScrollTo().assertTextContains("2026-01-")
        compose.onNodeWithTag("end-recovery-amount").performScrollTo().assertTextContains("123.456")
        assertEquals("保留原因", model.state.value.endEditor!!.endReason)
        compose.onNodeWithTag("tab-review").performClick()
        compose.onNodeWithTag("discard-dialog").assertIsDisplayed()
        compose.onNodeWithTag("discard-keep").performClick()
        assertNull(repository.loadProjectDetail(id)!!.project.endedOn)
        assertEquals(1, repository.loadProjectDetail(id)!!.events.size)
        compose.onNodeWithTag("end-back").performScrollTo().performClick()
        compose.onNodeWithTag("discard-confirm").performClick()
        compose.onNodeWithTag("project-detail-screen").assertIsDisplayed()
    }

    @Test fun deletion_with_lost_result_stays_on_original_confirmation_then_reconciles_absence(): Unit = runBlocking {
        val lossy = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            private var loseResult = true
            override suspend fun deleteProject(command: DeleteProjectCommand): MutationResult {
                val result = super.deleteProject(command)
                if (loseResult) { loseResult = false; throw IOException("synthetic result interruption after actual delete") }
                return result
            }
        }
        render(lossy)
        val id = createProject("核对未知删除结果", "1000")
        detailAction("project-delete")
        waitForDelete()
        compose.onNodeWithTag("project-delete-confirm").performClick()
        compose.waitUntil(15_000) { model.state.value.deleteConfirmation?.phase == SubmissionPhase.UNCONFIRMED }
        assertNull(repository.loadProjectDetail(id))
        compose.onNodeWithTag("project-delete-dialog").assertIsDisplayed()
        compose.onNodeWithTag("project-delete-cancel").assertIsNotEnabled()
        compose.onNodeWithTag("project-delete-confirm").assertTextContains("核对原删除提交").performClick()
        compose.waitUntil(15_000) { model.state.value.deleteConfirmation == null }
        compose.onNodeWithTag("project-list-screen").assertIsDisplayed()
        assertEquals(0L, repository.loadSummary().projectCount)
        assertTrue(LedgerReadRepository(generation.database, generation.coordinator).snapshot().moneyEvents.isEmpty())
    }

    @Test fun lifecycle_confirmation_is_reachable_at_360dp_and_200_percent_font(): Unit = runBlocking {
        render(largeFont = true)
        val id = createProject("窄屏大字体生命周期", "0")
        endProject("2026-01-08", "0")
        assertEquals(0L, repository.loadProjectDetail(id)!!.cost.netCents)
        detailAction("project-delete")
        waitForDelete()
        compose.onNodeWithTag("project-delete-cancel").assertIsDisplayed().performClick()
        compose.onNodeWithTag("project-detail-screen").assertIsDisplayed()
        assertEquals(2, repository.loadProjectDetail(id)!!.events.size)
    }

    private fun render(projectRepository: ProjectRepository = repository, restorable: Boolean = false,
        largeFont: Boolean = false): StateRestorationTester? {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            savedState = SavedStateHandle()
            model = ProjectViewModel(projectRepository, savedState, clock)
            viewModels.put("projects", model)
            activeModel.value = model
        }
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            LithicTheme {
                val ledger by holder.state.collectAsState()
                if (showApp.value) {
                    if (largeFont) {
                        val density = LocalDensity.current.density
                        CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                            Box(Modifier.width(360.dp).fillMaxHeight()) { LithicApp(ledger, {}, activeModel.value!!) }
                        }
                    } else LithicApp(ledger, {}, activeModel.value!!)
                }
            }
        }
        val restoration = if (restorable) StateRestorationTester(compose).also { it.setContent(content) }
        else { compose.setContent(content); null }
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
        return restoration
    }

    private fun createProject(name: String, amount: String): String {
        compose.openHomeCreate("home-create-project")
        compose.waitUntil(10_000) { model.state.value.editor?.today != null }
        compose.onNodeWithTag("project-name").performScrollTo().performTextReplacement(name)
        compose.onNodeWithTag("project-started-on-input").performScrollTo().performTextReplacement("2026-01-01")
        compose.onNodeWithTag("project-initial-amount").performScrollTo().performTextReplacement(amount)
        compose.onNodeWithTag("project-save").performScrollTo().performClick()
        waitForDetail()
        return model.state.value.detail.detail!!.project.id
    }

    private fun openEnd() { detailAction("project-end"); waitForEnd() }
    private fun fillEnd(date: String, reason: String, recovery: String? = null) {
        compose.onNodeWithTag("end-ended-on-input").performScrollTo().performTextReplacement(date)
        compose.onNodeWithTag("end-reason").performScrollTo().performTextReplacement(reason)
        if (recovery != null) {
            compose.onNodeWithTag("end-record-recovery").performScrollTo().performClick()
            compose.onNodeWithTag("end-recovery-amount").performScrollTo().performTextReplacement(recovery)
            compose.onNodeWithTag("end-recovery-on-input").performScrollTo().performTextReplacement(date)
        }
    }
    private fun endProject(date: String, recovery: String? = null) { openEnd(); fillEnd(date, "不再使用", recovery); saveEnd() }
    private fun saveEnd(context: String = "end submission") {
        clickEndSave()
        try { waitForDetail() }
        catch (failure: Throwable) {
            throw AssertionError("$context did not reach detail. End state=${model.state.value.endEditor}; " +
                "detail state=${model.state.value.detail}; committedId=${model.state.value.committedProjectId}; " +
                "original failure:\n${failure.stackTraceToString()}")
        }
    }
    private fun clickEndSave() {
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
        compose.onNodeWithTag("end-save").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
    }
    private fun addMoney(action: String, amount: String, date: String, description: String) {
        detailAction(action)
        compose.waitUntil(10_000) { model.state.value.moneyEditor?.today != null }
        compose.onNodeWithTag("money-amount").performScrollTo().performTextReplacement(amount)
        compose.onNodeWithTag("money-occurred-on-input").performScrollTo().performTextReplacement(date)
        compose.onNodeWithTag("money-description").performScrollTo().performTextReplacement(description)
        compose.onNodeWithTag("money-save").performScrollTo().performClick()
        waitForDetail()
    }
    private fun detailAction(tag: String) {
        compose.projectDetailAction(tag)
    }
    private fun openHistory() {
        compose.onNodeWithTag("tab-review").performClick()
        compose.onNodeWithTag("review-project-history").performScrollTo().performClick()
        compose.waitUntil(10_000) { !model.state.value.history.loading }
        compose.onNodeWithTag("project-history-screen").assertIsDisplayed()
    }
    private fun waitForDetail() {
        compose.waitUntil(15_000) {
            model.state.value.let { it.editor == null && it.moneyEditor == null && it.endEditor == null &&
                it.moneyCorrection == null && !it.detail.loading && it.detail.detail != null }
        }
        compose.onNodeWithTag("project-detail-screen").assertIsDisplayed()
    }
    private fun waitForEnd() {
        compose.waitUntil(10_000) { model.state.value.endEditor?.let { !it.loading && it.today != null } == true }
        compose.onNodeWithTag("end-editor-screen").assertIsDisplayed()
    }
    private fun waitForCorrection() {
        compose.waitUntil(10_000) { model.state.value.moneyCorrection?.let { !it.loading && it.today != null } == true }
        compose.onNodeWithTag("money-correction-screen").assertIsDisplayed()
    }
    private fun waitForDelete() {
        compose.waitUntil(10_000) { model.state.value.deleteConfirmation?.let { !it.loading } == true }
        compose.onNodeWithTag("project-delete-dialog").assertIsDisplayed()
    }
    private fun waitForCurrent() {
        compose.waitUntil(10_000) { !model.state.value.list.loading }
        compose.onNodeWithTag("project-list-screen").assertIsDisplayed()
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
        val jobs = listOfNotNull(if (::holder.isInitialized) holder.viewModelScope.coroutineContext[Job] else null,
            if (::model.isInitialized) model.viewModelScope.coroutineContext[Job] else null)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModels.clear() }
        jobs.forEach { it.join() }
    }
}
