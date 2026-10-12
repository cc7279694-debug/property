package com.cooper.lithic.feature.project

import android.content.Context
import android.os.Bundle
import androidx.room.withTransaction
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.repository.CreateProjectCommand
import com.cooper.lithic.data.repository.AddProjectMoneyEventCommand
import com.cooper.lithic.data.repository.UpdateProjectCommand
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.data.repository.ProjectRepository
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Test decorators keep actual transactions; only result delivery or a later read is interrupted. */
@RunWith(AndroidJUnit4::class)
class ProjectViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val day = LocalDate.of(2026, 1, 10)
    private val clock = FixedBusinessClock(day)
    private val timestamp = Instant.parse("2026-01-10T12:00:00Z")
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var repository: ProjectRepository
    private val stores = mutableListOf<ViewModelStore>()
    private val modelStores = mutableMapOf<ProjectViewModel, ViewModelStore>()

    @Before fun before() = runBlocking {
        root = File(context.noBackupFilesDir, "m06-vm-${UUID.randomUUID()}")
        check(root.mkdir())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        repository = ProjectRepository(generation.database, generation.coordinator, clock)
    }

    @After fun after() = runBlocking {
        onMain { stores.forEach { it.clear() } }
        generation.close()
        check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
        check(root.deleteRecursively())
    }

    @Test fun raw_input_and_dirty_guard_survive_saved_state_recreation() {
        val handle = SavedStateHandle()
        val first = vm(handle = handle)
        onMain {
            first.startCreate()
            first.changeName("未保存名称")
            first.changeInitialAmount("123.456")
            first.changeNote("重要备注")
        }
        assertNotNull("The active form must exist", first.state.value.editor)
        assertEquals(LeaveDecision.CONFIRM_DISCARD, first.leaveDecision())
        val restored = vm(handle = restoredHandle(handle))
        assertEquals("未保存名称", restored.state.value.editor!!.name)
        assertEquals("123.456", restored.state.value.editor!!.initialAmount)
        assertEquals("重要备注", restored.state.value.editor!!.note)
        assertEquals(LeaveDecision.CONFIRM_DISCARD, restored.leaveDecision())
        onMain { restored.discardForm() }
        assertNull(restored.state.value.editor)
        assertEquals(LeaveDecision.ALLOW, restored.leaveDecision())
    }

    @Test fun create_zero_and_normal_project_persist_one_initial_event_each() = runBlocking {
        for ((name, amount, cents) in listOf(Triple("零元", "0", 0L), Triple("普通", "1000", 100000L))) {
            val model = vm()
            fillCreate(model, name, amount)
            onMain { model.save() }
            await(model) { it.committedProjectId != null }
            val id = model.state.value.committedProjectId!!
            val detail = repository.loadProjectDetail(id)!!
            assertEquals(name, detail.project.name)
            assertEquals(1, detail.events.size)
            assertEquals(cents, detail.cost.grossOutlayCents)
            assertEquals(10L, detail.cost.heldDays)
            assertEquals(if (cents == 0L) "0" else "100", detail.cost.dailyRate!!.stripTrailingZeros().toPlainString())
        }
        assertEquals(2, generation.database.projectDao().listProjects().size)
        assertEquals(2, generation.database.projectDao().listEvents().size)
    }

    @Test fun invalid_amount_preserves_form_without_writing_or_closing() = runBlocking {
        val model = vm()
        fillCreate(model, "保留输入", "1.001")
        onMain { model.save() }
        val editor = model.state.value.editor
        assertNotNull(editor)
        assertEquals("保留输入", editor!!.name)
        assertEquals("1.001", editor.initialAmount)
        assertTrue(editor.errors.containsKey("initialAmount"))
        assertEquals(SubmissionPhase.IDLE, editor.phase)
        assertEquals(LeaveDecision.CONFIRM_DISCARD, model.leaveDecision())
        assertTrue(generation.database.projectDao().listProjects().isEmpty())
        assertNull(model.state.value.committedProjectId)
    }

    @Test fun explicit_prepayment_stays_independent_of_ownership_date() = runBlocking {
        val model = vm()
        fillCreate(model, "预付", "100")
        onMain {
            model.changeInitialPaidOn("2025-12-25")
            model.changeStartedOn("2026-01-02")
            model.save()
        }
        await(model) { it.committedProjectId != null }
        val detail = repository.loadProjectDetail(model.state.value.committedProjectId!!)!!
        assertEquals(LocalDate.of(2025, 12, 25), detail.events.single().occurredOn)
        assertEquals(LocalDate.of(2026, 1, 2), detail.project.startedOn)
    }

    @Test fun future_payment_is_rejected_using_injected_business_today() = runBlocking {
        val model = vm()
        fillCreate(model, "未来实际付款不成立", "100")
        onMain { model.changeInitialPaidOn("2026-01-11"); model.save() }
        assertNotNull(model.state.value.editor)
        assertTrue(model.state.value.editor!!.errors.containsKey("initialPaidOn"))
        assertTrue(generation.database.projectDao().listEvents().isEmpty())
    }

    @Test fun busy_double_tap_and_field_change_cannot_duplicate_or_change_command() = runBlocking {
        val written = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val delayed = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun createProject(command: CreateProjectCommand): MutationResult {
                val result = super.createProject(command)
                written.complete(Unit)
                release.await()
                return result
            }
        }
        val model = vm(delayed)
        fillCreate(model, "一次投入", "1000")
        onMain { model.save(); model.save(); model.changeInitialAmount("2000") }
        assertTrue("The real transaction should complete", withTimeoutOrNull(5_000) { written.await(); true } == true)
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
        assertEquals("1000", model.state.value.editor!!.initialAmount)
        assertEquals(1, generation.database.projectDao().listProjects().size)
        assertEquals(100000L, generation.database.projectDao().listEvents().single().amountCents)
        release.complete(Unit)
        await(model) { it.committedProjectId != null }
    }

    @Test fun committed_result_interruption_restores_and_replays_original_identity() = runBlocking {
        val written = CompletableDeferred<Unit>()
        val neverDelivered = CompletableDeferred<Unit>()
        val delayed = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun createProject(command: CreateProjectCommand): MutationResult {
                val result = super.createProject(command)
                if (result is MutationResult.Success && !result.replayed) { written.complete(Unit); neverDelivered.await() }
                return result
            }
        }
        val handle = SavedStateHandle()
        val first = vm(delayed, handle)
        fillCreate(first, "结果丢失", "1000")
        onMain { first.save() }
        assertTrue(withTimeoutOrNull(5_000) { written.await(); true } == true)
        val originalProject = generation.database.projectDao().listProjects().single()
        val originalEvent = generation.database.projectDao().listEvents().single()
        clearModel(first)
        val restored = vm(repository, restoredHandle(handle))
        assertEquals(SubmissionPhase.UNCONFIRMED, restored.state.value.editor!!.phase)
        assertEquals(LeaveDecision.BLOCK_PENDING, restored.leaveDecision())
        onMain { restored.changeName("另一笔不得替换"); restored.save() }
        await(restored) { it.committedProjectId != null }
        assertEquals(originalProject.id, restored.state.value.committedProjectId)
        assertEquals(listOf(originalProject), generation.database.projectDao().listProjects())
        assertEquals(listOf(originalEvent), generation.database.projectDao().listEvents())
    }

    @Test fun storage_failure_preserves_input_and_unknown_envelope_for_safe_retry() = runBlocking {
        val model = vm()
        fillCreate(model, "数据库不可读写", "200")
        generation.database.close()
        onMain { model.save() }
        await(model) { it.editor?.phase == SubmissionPhase.UNCONFIRMED }
        assertEquals("数据库不可读写", model.state.value.editor!!.name)
        assertEquals("200", model.state.value.editor!!.initialAmount)
        assertNotNull(model.state.value.editor!!.message)
        assertNull(model.state.value.committedProjectId)
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
    }

    @Test fun appended_commit_result_loss_restores_same_event_and_operation() = runBlocking {
        val seed = vm()
        fillCreate(seed, "追加结果中断", "1000")
        onMain { seed.save() }
        await(seed) { it.committedProjectId != null }
        val id = seed.state.value.committedProjectId!!
        val written = CompletableDeferred<Unit>()
        val neverDelivered = CompletableDeferred<Unit>()
        val delayed = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun appendMoneyEvent(command: AddProjectMoneyEventCommand): MutationResult {
                val result = super.appendMoneyEvent(command)
                if (result is MutationResult.Success && !result.replayed) { written.complete(Unit); neverDelivered.await() }
                return result
            }
        }
        val handle = SavedStateHandle()
        val first = vm(delayed, handle)
        onMain {
            first.startMoney(id, MoneyDirection.OUTLAY)
            first.changeAmount("200")
            first.changeOccurredOn("2026-01-05")
            first.changeDescription("实际补录")
            first.save()
        }
        assertTrue(withTimeoutOrNull(5_000) { written.await(); true } == true)
        val persisted = generation.database.projectDao().listEvents()
        assertEquals(2, persisted.size)
        clearModel(first)
        val restored = vm(repository, restoredHandle(handle))
        assertEquals(SubmissionPhase.UNCONFIRMED, restored.state.value.moneyEditor!!.phase)
        assertEquals("200", restored.state.value.moneyEditor!!.amount)
        assertEquals("2026-01-05", restored.state.value.moneyEditor!!.occurredOn)
        assertEquals("实际补录", restored.state.value.moneyEditor!!.description)
        onMain { restored.changeAmount("300"); restored.save(); restored.save() }
        await(restored) { it.committedProjectId == id }
        assertEquals(persisted, generation.database.projectDao().listEvents())
        assertEquals(120000L, repository.loadProjectDetail(id)!!.cost.grossOutlayCents)
    }

    @Test fun edited_commit_result_loss_restores_complete_baseline_and_target_for_cas_replay() = runBlocking {
        val seed = vm()
        fillCreate(seed, "原资料", "1000")
        onMain { seed.save() }
        await(seed) { it.committedProjectId != null }
        val id = seed.state.value.committedProjectId!!
        val baseline = repository.loadProjectDetail(id)!!.project
        val events = generation.database.projectDao().listEvents()
        val written = CompletableDeferred<Unit>()
        val neverDelivered = CompletableDeferred<Unit>()
        val delayed = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun updateProject(command: UpdateProjectCommand): MutationResult {
                val result = super.updateProject(command)
                if (result is MutationResult.ProjectUpdated && !result.replayed) { written.complete(Unit); neverDelivered.await() }
                return result
            }
        }
        val handle = SavedStateHandle()
        val first = vm(delayed, handle)
        onMain { first.startEdit(id) }
        await(first) { it.editor?.loading == false }
        onMain { first.changeName("已写资料"); first.changeStartedOn("2026-01-02"); first.changeNote("恢复后仍保留"); first.save() }
        assertTrue(withTimeoutOrNull(5_000) { written.await(); true } == true)
        val target = repository.loadProjectDetail(id)!!.project
        assertEquals(baseline.updatedAt.plusNanos(1), target.updatedAt)
        assertEquals(baseline.createdAt, target.createdAt)
        assertEquals(LocalDate.of(2026, 1, 2), target.startedOn)
        clearModel(first)
        val restored = vm(repository, restoredHandle(handle))
        assertEquals(SubmissionPhase.UNCONFIRMED, restored.state.value.editor!!.phase)
        assertEquals("已写资料", restored.state.value.editor!!.name)
        assertEquals("恢复后仍保留", restored.state.value.editor!!.note)
        assertEquals(LeaveDecision.BLOCK_PENDING, restored.leaveDecision())
        onMain { restored.changeName("不能换提交内容"); restored.save(); restored.save() }
        await(restored) { it.committedProjectId == id }
        assertEquals(target, repository.loadProjectDetail(id)!!.project)
        assertEquals(events, generation.database.projectDao().listEvents())
        assertEquals(1, generation.database.projectDao().listProjects().size)
    }

    @Test fun successful_write_then_detail_read_failure_remains_committed() = runBlocking {
        val unreadable = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadProjectDetail(id: String, asOf: LocalDate): ProjectDetail? = throw IOException("Read delivery failed")
        }
        val model = vm(unreadable)
        fillCreate(model, "已提交", "1000")
        onMain { model.save() }
        await(model) { it.committedProjectId != null }
        val id = model.state.value.committedProjectId!!
        onMain { model.openDetail(id) }
        await(model) { it.detail.error != null }
        assertEquals(SubmissionPhase.COMMITTED, model.state.value.editor!!.phase)
        onMain { model.save(); model.retryRead() }
        assertEquals(1, generation.database.projectDao().listProjects().size)
        assertEquals(1, generation.database.projectDao().listEvents().size)
        assertEquals(id, model.state.value.committedProjectId)
        assertFalse(model.state.value.detail.missing)
    }

    @Test fun backdated_recovery_refreshes_list_and_detail_to_negative_net() = runBlocking {
        val model = vm()
        fillCreate(model, "真实收回", "1000")
        onMain { model.save() }
        await(model) { it.committedProjectId != null }
        val id = model.state.value.committedProjectId!!
        onMain { model.consumeCommitted(); model.loadList(); model.openDetail(id) }
        await(model) { it.list.items.size == 1 && it.detail.detail != null }
        onMain { model.startMoney(id, MoneyDirection.RECOVERY); model.changeAmount("1200"); model.changeOccurredOn("2026-01-05"); model.save() }
        await(model) { it.committedProjectId == id }
        await(model) { it.detail.detail?.cost?.netCents == -20000L && it.list.items.singleOrNull()?.cost?.netCents == -20000L }
        assertEquals("-20", model.state.value.detail.detail!!.cost.dailyRate!!.stripTrailingZeros().toPlainString())
        assertEquals(100000L, repository.loadProjectDetail(id, LocalDate.of(2026, 1, 4))!!.cost.netCents)
        assertEquals(2, generation.database.projectDao().listEvents().size)
    }

    @Test fun editing_preserves_events_and_refuses_a_stale_loaded_version() = runBlocking {
        val seed = vm()
        fillCreate(seed, "原名", "1000")
        onMain { seed.save() }
        await(seed) { it.committedProjectId != null }
        val id = seed.state.value.committedProjectId!!
        val events = generation.database.projectDao().listEvents()
        val first = vm()
        val second = vm()
        onMain { first.startEdit(id); second.startEdit(id) }
        await(first) { it.editor?.loading == false }
        await(second) { it.editor?.loading == false }
        onMain { first.changeName("先保存"); first.changeStartedOn("2026-01-02"); first.save() }
        await(first) { it.committedProjectId != null }
        onMain { second.changeName("陈旧覆盖"); second.save() }
        await(second) { it.editor?.message != null && !it.editor!!.busy }
        assertEquals("先保存", repository.loadProjectDetail(id)!!.project.name)
        assertEquals(events, generation.database.projectDao().listEvents())
        assertNull(second.state.value.committedProjectId)
        assertEquals("陈旧覆盖", second.state.value.editor!!.name)
        assertTrue(second.state.value.editor!!.conflict)
        onMain { second.refresh() }
        assertEquals("陈旧覆盖", second.state.value.editor!!.name)
        assertEquals(SubmissionPhase.UNCONFIRMED, second.state.value.editor!!.phase)
        assertTrue(second.state.value.editor!!.conflict)
        onMain { second.retryRead() }
        await(second) { it.editor?.phase == SubmissionPhase.IDLE && it.editor?.name == "先保存" }
        assertEquals("先保存", repository.loadProjectDetail(id)!!.project.name)
        onMain { second.changeName("核对后修改"); second.save() }
        await(second) { it.committedProjectId == id }
        assertEquals("核对后修改", repository.loadProjectDetail(id)!!.project.name)
        assertEquals(events, generation.database.projectDao().listEvents())
    }

    @Test fun real_disk_full_preserves_form_then_retries_the_same_single_event() = runBlocking {
        val model = vm()
        val name = "合成空间不足".repeat(20_000)
        fillCreate(model, name, "1000")
        withContext(Dispatchers.IO) {
            generation.database.withTransaction {
                val sqlite = generation.database.openHelper.writableDatabase
                val pages = sqlite.query("PRAGMA page_count").use { check(it.moveToFirst()); it.getLong(0) }
                sqlite.query("PRAGMA max_page_count = $pages").use { result ->
                    check(result.moveToFirst())
                    assertEquals(pages, result.getLong(0))
                }
            }
        }
        try {
            onMain { model.save() }
            await(model) { it.editor?.phase == SubmissionPhase.UNCONFIRMED }
            assertEquals(name, model.state.value.editor!!.name)
            assertTrue(model.state.value.editor!!.message!!.contains("空间不足"))
            assertTrue(generation.database.projectDao().listProjects().isEmpty())
            assertTrue(generation.database.projectDao().listEvents().isEmpty())
        } finally {
            withContext(Dispatchers.IO) {
                generation.database.withTransaction {
                    generation.database.openHelper.writableDatabase.query("PRAGMA max_page_count = 2147483646").use { result ->
                        check(result.moveToFirst())
                        assertEquals(2147483646L, result.getLong(0))
                    }
                }
            }
        }
        onMain { model.save(); model.save() }
        await(model) { it.committedProjectId != null }
        assertEquals(1, generation.database.projectDao().listProjects().size)
        assertEquals(1, generation.database.projectDao().listEvents().size)
        assertEquals(100000L, generation.database.projectDao().listEvents().single().amountCents)
    }

    @Test fun resumed_read_recalculates_business_day_without_changing_pending_raw_input() = runBlocking {
        var today = day
        val moving = BusinessClock { today }
        val movingRepository = ProjectRepository(generation.database, generation.coordinator, moving)
        val model = vm(movingRepository, businessClock = moving)
        fillCreate(model, "隔日读取", "1100")
        onMain { model.save() }
        await(model) { it.committedProjectId != null }
        val id = model.state.value.committedProjectId!!
        onMain { model.consumeCommitted(); model.loadList(); model.openDetail(id) }
        await(model) { it.detail.detail?.cost?.heldDays == 10L }
        onMain { model.startMoney(id, MoneyDirection.OUTLAY); model.changeAmount("12.345") }
        today = LocalDate.of(2026, 1, 11)
        onMain { model.refresh() }
        await(model) { it.detail.detail?.cost?.heldDays == 11L && it.list.items.singleOrNull()?.cost?.heldDays == 11L }
        assertEquals("100", model.state.value.detail.detail!!.cost.dailyRate!!.stripTrailingZeros().toPlainString())
        assertEquals("12.345", model.state.value.moneyEditor!!.amount)
        assertEquals("2026-01-10", model.state.value.moneyEditor!!.occurredOn)
    }

    @Test fun slow_previous_detail_cannot_appear_while_a_different_project_is_loading() = runBlocking {
        val seedA = vm()
        val seedB = vm()
        fillCreate(seedA, "详情A", "1000")
        onMain { seedA.save() }
        await(seedA) { it.committedProjectId != null }
        fillCreate(seedB, "详情B", "2000")
        onMain { seedB.save() }
        await(seedB) { it.committedProjectId != null }
        val idA = seedA.state.value.committedProjectId!!
        val idB = seedB.state.value.committedProjectId!!
        val startedA = CompletableDeferred<Unit>()
        val startedB = CompletableDeferred<Unit>()
        val releaseA = CompletableDeferred<Unit>()
        val releaseB = CompletableDeferred<Unit>()
        val delayed = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadProjectDetail(id: String, asOf: LocalDate): ProjectDetail? {
                val actual = super.loadProjectDetail(id, asOf)
                if (id == idA) { startedA.complete(Unit); releaseA.await() }
                if (id == idB) { startedB.complete(Unit); releaseB.await() }
                return actual
            }
        }
        val model = vm(delayed)
        try {
            onMain { model.openDetail(idA) }
            assertTrue(withTimeoutOrNull(5_000) { startedA.await(); true } == true)
            onMain { model.openDetail(idB) }
            assertTrue(model.state.value.detail.loading)
            assertNull(model.state.value.detail.detail)
            releaseA.complete(Unit)
            assertTrue(withTimeoutOrNull(5_000) { startedB.await(); true } == true)
            assertNull("A result must not be published into the B route", model.state.value.detail.detail)
            assertTrue("B must stay loading until its own result arrives", model.state.value.detail.loading)
            releaseB.complete(Unit)
            await(model) { it.detail.detail?.project?.id == idB && !it.detail.loading }
            assertEquals("详情B", model.state.value.detail.detail!!.project.name)
        } finally { releaseA.complete(Unit); releaseB.complete(Unit) }
    }

    @Test fun initial_edit_read_restores_its_baseline_without_overwriting_existing_raw_input() = runBlocking {
        val seed = vm()
        fillCreate(seed, "已保存资料", "1000")
        onMain { seed.save() }
        await(seed) { it.committedProjectId != null }
        val id = seed.state.value.committedProjectId!!
        val originalEvents = generation.database.projectDao().listEvents()
        val started = CompletableDeferred<Unit>()
        val neverDelivered = CompletableDeferred<Unit>()
        val delayed = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadProjectDetail(id: String, asOf: LocalDate): ProjectDetail? {
                val actual = super.loadProjectDetail(id, asOf)
                started.complete(Unit)
                neverDelivered.await()
                return actual
            }
        }
        val handle = SavedStateHandle()
        val first = vm(delayed, handle)
        onMain { first.startEdit(id) }
        assertTrue(withTimeoutOrNull(5_000) { started.await(); true } == true)
        clearModel(first)
        val restored = vm(repository, restoredHandle(handle))
        await(restored) { it.editor?.name == "已保存资料" && !it.editor!!.loading }
        onMain { restored.changeName("恢复后正常编辑"); restored.save() }
        await(restored) { it.committedProjectId == id }
        assertEquals(originalEvents, generation.database.projectDao().listEvents())

        val failDelivery = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadProjectDetail(id: String, asOf: LocalDate): ProjectDetail? {
                super.loadProjectDetail(id, asOf)
                throw IOException("Synthetic initial read delivery failure")
            }
        }
        val failedHandle = SavedStateHandle()
        val failed = vm(failDelivery, failedHandle)
        onMain { failed.startEdit(id) }
        await(failed) { it.editor?.message != null && !it.editor!!.loading }
        onMain { failed.changeName("读取失败后保留名称"); failed.changeNote("读取失败后保留备注") }
        clearModel(failed)
        val rawRestored = vm(repository, restoredHandle(failedHandle))
        await(rawRestored) { it.editor?.loading == false && it.editor?.categoryId?.isNotBlank() == true }
        assertEquals("读取失败后保留名称", rawRestored.state.value.editor!!.name)
        assertEquals("读取失败后保留备注", rawRestored.state.value.editor!!.note)
        onMain { rawRestored.save() }
        await(rawRestored) { it.committedProjectId == id }
        assertEquals("读取失败后保留名称", repository.loadProjectDetail(id)!!.project.name)
        assertEquals(originalEvents, generation.database.projectDao().listEvents())
    }

    private fun vm(repo: ProjectRepository = repository, handle: SavedStateHandle = SavedStateHandle(), businessClock: BusinessClock = clock): ProjectViewModel {
        lateinit var model: ProjectViewModel
        onMain {
            model = ProjectViewModel(repo, handle, businessClock) { timestamp }
            ViewModelStore().also { it.put("project", model); stores += it; modelStores[model] = it }
        }
        return model
    }

    private fun fillCreate(model: ProjectViewModel, name: String, amount: String) {
        onMain { model.startCreate(); model.changeName(name); model.changeStartedOn("2026-01-01"); model.changeInitialAmount(amount) }
        assertNotNull("A create form must be opened before saving", model.state.value.editor)
    }

    private fun restoredHandle(source: SavedStateHandle): SavedStateHandle = SavedStateHandle(source.keys().associateWith { key ->
        val value = source.get<Any?>(key)
        if (value is Bundle) Bundle(value) else value
    })

    private suspend fun clearModel(model: ProjectViewModel) {
        val job = model.viewModelScope.coroutineContext[Job]
        onMain { requireNotNull(modelStores[model]).clear() }
        job?.join()
    }

    private suspend fun await(model: ProjectViewModel, predicate: (ProjectUiState) -> Boolean) {
        val result = withTimeoutOrNull(5_000) { model.state.first(predicate) }
        val current = model.state.value
        val status = "phase=${current.editor?.phase ?: current.moneyEditor?.phase}, committed=${current.committedProjectId != null}, readError=${current.detail.error != null}"
        assertNotNull("Expected state was not reached: $status", result)
    }

    private fun onMain(action: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
}
