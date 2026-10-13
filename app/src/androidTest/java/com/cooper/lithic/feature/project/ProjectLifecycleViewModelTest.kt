package com.cooper.lithic.feature.project

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.*
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Every mutation uses the real Room repository. Decorators interrupt only delivery/reads. */
@RunWith(AndroidJUnit4::class)
class ProjectLifecycleViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val day = LocalDate.of(2026, 1, 10)
    private val clock = FixedBusinessClock(day)
    private val timestamp = Instant.parse("2026-01-10T12:00:00Z")
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var repository: ProjectRepository
    private val stores = mutableMapOf<ProjectViewModel, ViewModelStore>()

    @Before fun before() = runBlocking {
        root = File(context.noBackupFilesDir, "m07-vm-${UUID.randomUUID()}")
        check(root.mkdir())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        repository = ProjectRepository(generation.database, generation.coordinator, clock)
    }

    @After fun after() = runBlocking {
        onMain { stores.values.forEach { it.clear() } }
        generation.close()
        check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
        check(root.deleteRecursively())
    }

    @Test fun end_raw_input_dirty_guard_and_identity_survive_saved_state_without_writing() = runBlocking {
        val baseline = seed()
        val handle = SavedStateHandle()
        val first = vm(handle = handle)
        startEnd(first, baseline.id)
        onMain {
            first.changeEndedOn("2026-01-05")
            first.changeEndReason("真实结束")
            first.changeRecordRecovery(true)
            first.changeRecoveryAmount("12.345")
            first.changeRecoveryOn("2026-01-06")
            first.changeRecoveryDescription("保留原始输入")
        }
        assertEquals(LeaveDecision.CONFIRM_DISCARD, first.leaveDecision())
        val commandId = active(handle).getString("commandId")
        val restoredHandle = restore(handle)
        val restored = vm(handle = restoredHandle)
        assertEquals(first.state.value.endEditor, restored.state.value.endEditor)
        assertEquals(commandId, active(restoredHandle).getString("commandId"))
        assertEquals(baseline, repository.loadProjectDetail(baseline.id)!!.project)
        assertEquals(1, generation.database.projectDao().listEvents().size)
        onMain { restored.discardForm() }
        assertNull(restored.state.value.endEditor)
    }

    @Test fun end_validates_real_recovery_date_and_amount_without_writing() = runBlocking {
        val baseline = seed()
        val model = vm()
        startEnd(model, baseline.id)
        onMain {
            model.changeEndedOn("2025-12-31")
            model.changeEndReason("")
            model.changeRecordRecovery(true)
            model.changeRecoveryAmount("1.001")
            model.changeRecoveryOn("2026-01-11")
            model.save()
        }
        val editor = model.state.value.endEditor!!
        assertTrue(editor.errors.keys.containsAll(listOf("endedOn", "endReason", "recoveryAmount", "recoveryOn")))
        assertEquals(SubmissionPhase.IDLE, editor.phase)
        assertEquals(baseline, repository.loadProjectDetail(baseline.id)!!.project)
        assertEquals(1, generation.database.projectDao().listEvents().size)
    }

    @Test fun zero_end_recovery_commits_once_and_refreshes_current_history_and_detail() = runBlocking {
        val baseline = seed()
        val model = vm()
        onMain { model.loadList(); model.loadHistory(); model.openDetail(baseline.id) }
        await(model) { it.list.items.size == 1 && !it.history.loading && it.detail.detail != null }
        startEnd(model, baseline.id)
        fillEnd(model, recovery = "0")
        onMain { model.save(); model.save() }
        await(model) { it.committedProjectId == baseline.id }
        await(model) { it.list.items.isEmpty() && it.history.items.size == 1 && it.detail.detail?.project?.endedOn == LocalDate.of(2026, 1, 5) }
        val detail = repository.loadProjectDetail(baseline.id)!!
        assertEquals(5L, detail.cost.heldDays)
        assertEquals(2, detail.events.size)
        assertEquals(0L, detail.events.single { it.role == ProjectMoneyEventRole.END_SETTLEMENT }.amountCents)
        assertEquals(LeaveDecision.ALLOW, model.leaveDecision())
    }

    @Test fun update_end_without_settlement_cannot_create_an_implicit_recovery() = runBlocking {
        val baseline = seed(endedOn = LocalDate.of(2026, 1, 5))
        val originalEvents = repository.loadProjectDetail(baseline.id)!!.events
        val model = vm()
        startUpdate(model, baseline.id)
        assertFalse(model.state.value.endEditor!!.hasSettlement)
        onMain {
            model.changeEndedOn("2026-01-06")
            model.changeEndReason("核对后修正")
            model.changeRecordRecovery(true)
            model.changeRecoveryAmount("300")
            model.save()
        }
        await(model) { it.committedProjectId == baseline.id }
        assertEquals(originalEvents, repository.loadProjectDetail(baseline.id)!!.events)
        assertEquals(LocalDate.of(2026, 1, 6), repository.loadProjectDetail(baseline.id)!!.project.endedOn)
    }

    @Test fun initial_money_correction_preserves_all_business_identities_and_updates_facts() = runBlocking {
        val baseline = seed()
        val original = repository.loadProjectDetail(baseline.id)!!.events.single()
        val model = vm()
        startCorrection(model, baseline.id, original.id)
        assertEquals(ProjectMoneyEventRole.INITIAL, model.state.value.moneyCorrection!!.role)
        onMain {
            model.changeAmount("800")
            model.changeOccurredOn("2026-01-02")
            model.changeDescription("原金额录错")
            model.changeDirection(MoneyDirection.RECOVERY)
            model.save()
        }
        await(model) { it.committedProjectId == baseline.id }
        val corrected = repository.loadProjectDetail(baseline.id)!!
        assertEquals(original.copy(amountCents = 80000L, occurredOn = LocalDate.of(2026, 1, 2), description = "原金额录错"), corrected.events.single())
        assertEquals(10L, corrected.cost.heldDays)
        assertEquals(80000L, corrected.cost.netCents)
        assertEquals(baseline.updatedAt.plusNanos(1), corrected.project.updatedAt)
    }

    @Test fun interrupted_end_replays_the_original_project_settlement_and_command_envelope() = runBlocking {
        val baseline = seed()
        val written = CompletableDeferred<EndProjectCommand>()
        val never = CompletableDeferred<Unit>()
        val delayed = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun endProject(command: EndProjectCommand): MutationResult {
                val result = super.endProject(command)
                if (result is MutationResult.LifecycleApplied && !result.replayed) { written.complete(command); never.await() }
                return result
            }
        }
        val handle = SavedStateHandle()
        val first = vm(delayed, handle)
        startEnd(first, baseline.id)
        fillEnd(first, "1200")
        onMain { first.save() }
        val command = delivered(written)
        val persisted = repository.loadProjectDetail(baseline.id)!!
        clear(first)
        var retried: EndProjectCommand? = null
        val replaying = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun endProject(command: EndProjectCommand): MutationResult { retried = command; return super.endProject(command) }
        }
        val restored = vm(replaying, restore(handle))
        assertEquals(SubmissionPhase.UNCONFIRMED, restored.state.value.endEditor!!.phase)
        assertEquals(LeaveDecision.BLOCK_PENDING, restored.leaveDecision())
        onMain { restored.changeRecoveryAmount("999"); restored.save(); restored.save() }
        await(restored) { it.committedProjectId == baseline.id }
        assertEquals(command, retried)
        assertEquals(persisted, repository.loadProjectDetail(baseline.id))
        assertEquals(2, generation.database.projectDao().listEvents().size)
    }

    @Test fun interrupted_update_end_replays_the_same_linked_settlement_identity_and_baseline() = runBlocking {
        val baseline = seed(endedOn = LocalDate.of(2026, 1, 5))
        val settlement = append(baseline.id, ProjectMoneyEventRole.END_SETTLEMENT, "1200")
        val written = CompletableDeferred<UpdateEndCommand>()
        val never = CompletableDeferred<Unit>()
        val delayed = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun updateEnd(command: UpdateEndCommand): MutationResult {
                val result = super.updateEnd(command)
                if (result is MutationResult.LifecycleApplied && !result.replayed) { written.complete(command); never.await() }
                return result
            }
        }
        val handle = SavedStateHandle()
        val first = vm(delayed, handle)
        startUpdate(first, baseline.id)
        onMain { first.changeEndedOn("2026-01-06"); first.changeRecoveryAmount("1300"); first.save() }
        val command = delivered(written)
        val persisted = repository.loadProjectDetail(baseline.id)!!
        clear(first)
        var retried: UpdateEndCommand? = null
        val replaying = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun updateEnd(command: UpdateEndCommand): MutationResult { retried = command; return super.updateEnd(command) }
        }
        val restored = vm(replaying, restore(handle))
        assertEquals(SubmissionPhase.UNCONFIRMED, restored.state.value.endEditor!!.phase)
        onMain { restored.save() }
        await(restored) { it.committedProjectId == baseline.id }
        assertEquals(command, retried)
        assertEquals(persisted, repository.loadProjectDetail(baseline.id))
        val target = persisted.events.single { it.role == ProjectMoneyEventRole.END_SETTLEMENT }
        assertEquals(settlement.id, target.id)
        assertEquals(settlement.operationId, target.operationId)
    }

    @Test fun interrupted_money_correction_replays_original_identity_and_uneditable_raw_input() = runBlocking {
        val baseline = seed()
        val event = repository.loadProjectDetail(baseline.id)!!.events.single()
        val written = CompletableDeferred<CorrectMoneyEventCommand>()
        val never = CompletableDeferred<Unit>()
        val delayed = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun correctMoneyEvent(command: CorrectMoneyEventCommand): MutationResult {
                val result = super.correctMoneyEvent(command)
                if (result is MutationResult.MoneyCorrected && !result.replayed) { written.complete(command); never.await() }
                return result
            }
        }
        val handle = SavedStateHandle()
        val first = vm(delayed, handle)
        startCorrection(first, baseline.id, event.id)
        onMain { first.changeAmount("750"); first.changeDescription("原记录纠错"); first.save() }
        val command = delivered(written)
        val persisted = repository.loadProjectDetail(baseline.id)!!
        clear(first)
        var retried: CorrectMoneyEventCommand? = null
        val replaying = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun correctMoneyEvent(command: CorrectMoneyEventCommand): MutationResult { retried = command; return super.correctMoneyEvent(command) }
        }
        val restored = vm(replaying, restore(handle))
        assertEquals(SubmissionPhase.UNCONFIRMED, restored.state.value.moneyCorrection!!.phase)
        assertEquals(LeaveDecision.BLOCK_PENDING, restored.leaveDecision())
        onMain { restored.changeAmount("100"); restored.changeDescription("不能变更原提交"); restored.save() }
        await(restored) { it.committedProjectId == baseline.id }
        assertEquals(command, retried)
        assertEquals("750", restored.state.value.moneyCorrection!!.amount)
        assertEquals(persisted, repository.loadProjectDetail(baseline.id))
    }

    @Test fun delete_cancel_leaves_facts_and_confirm_removes_project_and_its_events() = runBlocking {
        val baseline = seed()
        val original = repository.loadProjectDetail(baseline.id)!!
        val model = vm()
        startDelete(model, baseline.id)
        onMain { model.discardForm() }
        assertNull(model.state.value.deleteConfirmation)
        assertEquals(original, repository.loadProjectDetail(baseline.id))
        startDelete(model, baseline.id)
        onMain { model.save(); model.save() }
        await(model) { it.deletedProjectId == baseline.id }
        assertNull(repository.loadProjectDetail(baseline.id))
        assertTrue(generation.database.projectDao().listEvents().isEmpty())
        assertNull(model.state.value.committedProjectId)
        onMain { model.consumeDeleted() }
        assertNull(model.state.value.deletedProjectId)
        assertNull(model.state.value.deleteConfirmation)
    }

    @Test fun interrupted_delete_retries_same_snapshot_and_only_claims_current_absence() = runBlocking {
        val baseline = seed()
        val written = CompletableDeferred<DeleteProjectCommand>()
        val never = CompletableDeferred<Unit>()
        val delayed = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun deleteProject(command: DeleteProjectCommand): MutationResult {
                val result = super.deleteProject(command)
                if (result is MutationResult.ProjectDeleted) { written.complete(command); never.await() }
                return result
            }
        }
        val handle = SavedStateHandle()
        val first = vm(delayed, handle)
        startDelete(first, baseline.id)
        onMain { first.save() }
        val command = delivered(written)
        clear(first)
        var retried: DeleteProjectCommand? = null
        val replaying = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun deleteProject(command: DeleteProjectCommand): MutationResult { retried = command; return super.deleteProject(command) }
        }
        val restored = vm(replaying, restore(handle))
        assertEquals(SubmissionPhase.UNCONFIRMED, restored.state.value.deleteConfirmation!!.phase)
        assertEquals(LeaveDecision.BLOCK_PENDING, restored.leaveDecision())
        onMain { restored.discardForm(); restored.save(); restored.save() }
        val completed = requireNotNull(withTimeoutOrNull(5_000) {
            restored.state.first {
                it.deletedProjectId == baseline.id &&
                    it.deleteConfirmation?.phase == SubmissionPhase.COMMITTED &&
                    it.deleteConfirmation.message != null
            }
        }) { "Complete deletion confirmation state was not reached: ${restored.state.value}" }
        assertEquals(command, retried)
        assertTrue(completed.deleteConfirmation!!.message!!.contains("已核对"))
        assertTrue(completed.deleteConfirmation!!.message!!.contains("不存在"))
        assertFalse(completed.deleteConfirmation!!.message!!.contains("重放成功"))
        assertTrue(generation.database.projectDao().listProjects().isEmpty())
        assertTrue(generation.database.projectDao().listEvents().isEmpty())
    }

    @Test fun stale_delete_does_not_automatically_rebase_and_explicit_read_requires_confirmation_again() = runBlocking {
        val baseline = seed()
        val handle = SavedStateHandle()
        val model = vm(handle = handle)
        startDelete(model, baseline.id)
        val oldCommand = active(handle).getString("commandId")
        append(baseline.id, ProjectMoneyEventRole.REGULAR, "100")
        onMain { model.save() }
        await(model) { it.deleteConfirmation?.conflict == true }
        assertEquals(SubmissionPhase.UNCONFIRMED, model.state.value.deleteConfirmation!!.phase)
        onMain { model.refresh() }
        assertEquals(oldCommand, active(handle).getString("commandId"))
        assertTrue(model.state.value.deleteConfirmation!!.conflict)
        assertNotNull(repository.loadProjectDetail(baseline.id))
        onMain { model.retryRead() }
        await(model) { it.deleteConfirmation?.phase == SubmissionPhase.IDLE && it.deleteConfirmation?.loading == false }
        assertNotEquals(oldCommand, active(handle).getString("commandId"))
        assertEquals(2, repository.loadProjectDetail(baseline.id)!!.events.size)
        onMain { model.save() }
        await(model) { it.deletedProjectId == baseline.id }
    }

    @Test fun stale_correction_preserves_raw_values_until_explicit_read_of_current_facts() = runBlocking {
        val baseline = seed()
        val event = repository.loadProjectDetail(baseline.id)!!.events.single()
        val model = vm()
        startCorrection(model, baseline.id, event.id)
        onMain { model.changeAmount("700") }
        val external = repository.correctMoneyEvent(CorrectMoneyEventCommand(uuid(), baseline, event, 90000L, event.occurredOn, "先纠正", timestamp.plusNanos(1)))
        assertTrue(external is MutationResult.MoneyCorrected)
        onMain { model.save() }
        await(model) { it.moneyCorrection?.conflict == true }
        onMain { model.refresh() }
        assertEquals("700", model.state.value.moneyCorrection!!.amount)
        assertEquals(SubmissionPhase.UNCONFIRMED, model.state.value.moneyCorrection!!.phase)
        onMain { model.retryRead() }
        await(model) { it.moneyCorrection?.phase == SubmissionPhase.IDLE && it.moneyCorrection?.amount == "900" }
        assertEquals("先纠正", model.state.value.moneyCorrection!!.description)
        assertNull(model.state.value.committedProjectId)
    }

    @Test fun initial_end_read_interruption_restores_baseline_without_replacing_existing_raw_values() = runBlocking {
        val baseline = seed()
        val started = CompletableDeferred<Unit>()
        val never = CompletableDeferred<Unit>()
        val delayed = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadProjectDetail(id: String, asOf: LocalDate): ProjectDetail? {
                val actual = super.loadProjectDetail(id, asOf)
                started.complete(Unit)
                never.await()
                return actual
            }
        }
        val handle = SavedStateHandle()
        val first = vm(delayed, handle)
        onMain { first.startEnd(baseline.id) }
        assertNotNull(first.state.value.endEditor)
        delivered(started)
        clear(first)
        val raw = active(handle).apply {
            putString("endedOn", "2026-01-04")
            putString("endReason", "恢复前的原输入")
            putString("recoveryAmount", "12.345")
            putBoolean("dirty", true)
        }
        handle[FORM] = raw
        val restored = vm(handle = restore(handle))
        await(restored) { it.endEditor?.loading == false && it.endEditor?.needsInitialRead == false }
        assertEquals("2026-01-04", restored.state.value.endEditor!!.endedOn)
        assertEquals("恢复前的原输入", restored.state.value.endEditor!!.endReason)
        assertEquals("12.345", restored.state.value.endEditor!!.recoveryAmount)
        assertEquals(LeaveDecision.CONFIRM_DISCARD, restored.leaveDecision())
        assertEquals(baseline, repository.loadProjectDetail(baseline.id)!!.project)
    }

    @Test fun delayed_initial_read_cannot_replace_a_new_form_for_the_same_project() = runBlocking {
        val baseline = seed()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var firstRead = true
        val delayed = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadProjectDetail(id: String, asOf: LocalDate): ProjectDetail? {
                val actual = super.loadProjectDetail(id, asOf)
                if (firstRead) { firstRead = false; started.complete(Unit); release.await() }
                return actual
            }
        }
        val model = vm(delayed)
        try {
            onMain { model.startEnd(baseline.id) }
            assertNotNull(model.state.value.endEditor)
            delivered(started)
            onMain { model.discardForm() }
            assertTrue(repository.updateProject(UpdateProjectCommand(uuid(), baseline, "当前资料", baseline.startedOn, baseline.categoryId, baseline.note, timestamp.plusNanos(1))) is MutationResult.ProjectUpdated)
            startEnd(model, baseline.id)
            assertEquals("当前资料", model.state.value.endEditor!!.projectName)
            release.complete(Unit)
            onMain { model.refresh() }
            assertEquals("当前资料", model.state.value.endEditor!!.projectName)
        } finally { release.complete(Unit) }
    }

    @Test fun history_is_database_filtered_bounded_and_refreshes_loaded_pages_after_writes() = runBlocking {
        repeat(55) { seed("历史$it", LocalDate.of(2026, 1, 5)) }
        val future = seed("未来结束", LocalDate.of(2026, 1, 12))
        seed("当前")
        val model = vm()
        onMain { model.loadHistory(); model.loadList() }
        assertTrue(model.state.value.history.loading)
        await(model) { !it.history.loading && !it.list.loading }
        assertEquals(55L, model.state.value.history.totalCount)
        assertEquals(50, model.state.value.history.items.size)
        assertTrue(model.state.value.history.hasMore)
        assertEquals(2, model.state.value.list.items.size)
        assertTrue(model.state.value.list.items.any { it.project.id == future.id })
        onMain { model.loadMoreHistory() }
        await(model) { it.history.items.size == 55 && !it.history.loadingMore }
        assertFalse(model.state.value.history.hasMore)
        assertEquals(55, model.state.value.history.items.map { it.project.id }.distinct().size)
        val target = model.state.value.history.items.first()
        val event = target.events.single()
        assertTrue(repository.correctMoneyEvent(CorrectMoneyEventCommand(uuid(), target.project, event, 70000L, event.occurredOn, "历史纠错", timestamp.plusNanos(1))) is MutationResult.MoneyCorrected)
        await(model) { it.history.items.firstOrNull { item -> item.project.id == target.project.id }?.cost?.netCents == 70000L }
        assertEquals(55, model.state.value.history.items.size)
    }

    @Test fun lifecycle_commit_then_read_failure_stays_committed_without_new_write() = runBlocking {
        val baseline = seed()
        var fail = false
        val unreadable = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadProjectDetail(id: String, asOf: LocalDate): ProjectDetail? {
                if (fail) throw IOException("Synthetic committed read interruption")
                return super.loadProjectDetail(id, asOf)
            }
        }
        val model = vm(unreadable)
        startEnd(model, baseline.id)
        fillEnd(model, "100")
        fail = true
        onMain { model.openDetail(baseline.id); model.save() }
        await(model) { it.committedProjectId == baseline.id && it.detail.error?.contains("已保存") == true }
        assertEquals(SubmissionPhase.COMMITTED, model.state.value.endEditor!!.phase)
        assertTrue(model.state.value.detail.error!!.contains("已保存"))
        onMain { model.save(); model.retryRead() }
        assertEquals(2, generation.database.projectDao().listEvents().size)
        assertFalse(model.state.value.detail.missing)
    }

    @Test fun external_delete_removes_cached_detail_and_missing_is_not_a_read_error() = runBlocking {
        val baseline = seed()
        val model = vm()
        onMain { model.openDetail(baseline.id) }
        await(model) { it.detail.detail != null }
        val original = repository.loadProjectDetail(baseline.id)!!
        assertTrue(repository.deleteProject(DeleteProjectCommand(uuid(), original.project, original.events)) is MutationResult.ProjectDeleted)
        await(model) { it.detail.missing }
        assertNull(model.state.value.detail.detail)
        assertNull(model.state.value.detail.error)
    }

    @Test fun initial_delete_read_failure_and_absence_are_distinct_and_neither_writes() = runBlocking {
        val baseline = seed()
        val unreadable = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadProjectDetail(id: String, asOf: LocalDate): ProjectDetail? = throw IOException("Read delivery failed")
        }
        val failed = vm(unreadable)
        onMain { failed.startDelete(baseline.id) }
        assertNotNull(failed.state.value.deleteConfirmation)
        await(failed) { it.deleteConfirmation?.message != null && it.deleteConfirmation?.loading == false }
        assertTrue(failed.state.value.deleteConfirmation!!.needsInitialRead)
        assertFalse(failed.state.value.deleteConfirmation!!.message!!.contains("不存在"))
        onMain { failed.save() }
        assertEquals(baseline, repository.loadProjectDetail(baseline.id)!!.project)
        val absent = vm()
        onMain { absent.startDelete(uuid()) }
        assertNotNull(absent.state.value.deleteConfirmation)
        await(absent) { it.deleteConfirmation?.message != null && it.deleteConfirmation?.loading == false }
        assertTrue(absent.state.value.deleteConfirmation!!.message!!.contains("不存在"))
        assertNull(absent.state.value.deletedProjectId)
    }

    @Test fun conflicted_delete_explicit_missing_read_reports_only_verified_absence_and_allows_return() = runBlocking {
        val baseline = seed()
        val model = vm()
        startDelete(model, baseline.id)
        append(baseline.id, ProjectMoneyEventRole.REGULAR, "100")
        onMain { model.save() }
        await(model) { it.deleteConfirmation?.conflict == true }
        val current = repository.loadProjectDetail(baseline.id)!!
        assertTrue(repository.deleteProject(DeleteProjectCommand(uuid(), current.project, current.events)) is MutationResult.ProjectDeleted)
        onMain { model.retryRead() }
        await(model) { it.deletedProjectId == baseline.id && it.deleteConfirmation?.loading == false }
        assertEquals(LeaveDecision.ALLOW, model.leaveDecision())
        assertTrue(model.state.value.deleteConfirmation!!.message!!.contains("已核对"))
        assertTrue(model.state.value.deleteConfirmation!!.message!!.contains("不存在"))
        assertFalse(model.state.value.deleteConfirmation!!.message!!.contains("重放成功"))
        assertNull(model.state.value.committedProjectId)
        assertNull(repository.loadProjectDetail(baseline.id))
        onMain { model.consumeDeleted() }
        assertNull(model.state.value.deleteConfirmation)
    }

    @Test fun conflicted_edit_end_update_and_money_explicit_missing_reads_allow_safe_return_without_false_commit() = runBlocking {
        for (kind in listOf("edit", "end", "updateEnd", "correctMoney")) {
            val baseline = seed(name = "外部删除-$kind", endedOn = if (kind == "updateEnd") LocalDate.of(2026, 1, 5) else null)
            val event = repository.loadProjectDetail(baseline.id)!!.events.single()
            val model = vm()
            onMain { model.openDetail(baseline.id) }
            await(model) { it.detail.detail?.project?.id == baseline.id }
            startAndFill(model, kind, baseline.id, event.id)
            assertTrue(repository.updateProject(UpdateProjectCommand(uuid(), baseline, "外部资料修改", baseline.startedOn, baseline.categoryId, baseline.note, timestamp.plusNanos(1))) is MutationResult.ProjectUpdated)
            onMain { model.save() }
            await(model) { hasConflict(it) }
            assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
            val current = repository.loadProjectDetail(baseline.id)!!
            assertTrue(repository.deleteProject(DeleteProjectCommand(uuid(), current.project, current.events)) is MutationResult.ProjectDeleted)
            onMain { model.retryRead() }
            await(model) { !formLoading(it) && formMessage(it)?.contains("不存在") == true && model.leaveDecision() == LeaveDecision.ALLOW }
            assertNull("A verified missing read cannot be reported as a successful save", model.state.value.committedProjectId)
            assertNull("Only the actual delete form emits source-list deletion navigation", model.state.value.deletedProjectId)
            assertTrue(model.state.value.detail.missing)
            assertNull(model.state.value.detail.detail)
            onMain { model.save(); model.discardForm() }
            assertNull(model.state.value.editor)
            assertNull(model.state.value.endEditor)
            assertNull(model.state.value.moneyCorrection)
            assertNull(repository.loadProjectDetail(baseline.id))
        }
    }

    @Test fun conflicted_missing_target_read_exception_keeps_original_envelope_and_blocks_until_verified() = runBlocking {
        val baseline = seed()
        var failRead = false
        val interrupted = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadProjectDetail(id: String, asOf: LocalDate): ProjectDetail? {
                if (failRead) throw IOException("Synthetic explicit missing-read interruption")
                return super.loadProjectDetail(id, asOf)
            }
        }
        val handle = SavedStateHandle()
        val model = vm(interrupted, handle)
        startDelete(model, baseline.id)
        append(baseline.id, ProjectMoneyEventRole.REGULAR, "100")
        onMain { model.save() }
        await(model) { it.deleteConfirmation?.conflict == true }
        val commandId = active(handle).getString("commandId")
        val originalEnvelope = Bundle(requireNotNull(active(handle).getBundle("submitted")))
        val current = repository.loadProjectDetail(baseline.id)!!
        assertTrue(repository.deleteProject(DeleteProjectCommand(uuid(), current.project, current.events)) is MutationResult.ProjectDeleted)
        failRead = true
        onMain { model.retryRead() }
        await(model) { !formLoading(it) && formMessage(it)?.contains("暂时无法读取") == true }
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
        assertEquals(commandId, active(handle).getString("commandId"))
        assertEquals(originalEnvelope.getString("writtenAt"), active(handle).getBundle("submitted")!!.getString("writtenAt"))
        assertTrue(model.state.value.deleteConfirmation!!.conflict)
        assertNull(model.state.value.deletedProjectId)
        assertNull(model.state.value.committedProjectId)
        onMain { model.refresh(); model.discardForm() }
        assertEquals(commandId, active(handle).getString("commandId"))
        assertNotNull(model.state.value.deleteConfirmation)
    }

    @Test fun real_room_lifecycle_state_updates_never_emit_a_gap_in_the_active_form_identity() = runBlocking {
        for (kind in listOf("end", "correctMoney", "delete")) {
            val baseline = seed(name = "连续状态-$kind")
            val event = repository.loadProjectDetail(baseline.id)!!.events.single()
            val model = vm()
            when (kind) {
                "end" -> startEnd(model, baseline.id)
                "correctMoney" -> startCorrection(model, baseline.id, event.id)
                "delete" -> startDelete(model, baseline.id)
            }
            val observed = mutableListOf<ProjectUiState>()
            // Unconfined delivery sees every synchronous publication, like an immediate consumer.
            val observer = CoroutineScope(Dispatchers.Unconfined).launch { model.state.collect { observed += it } }
            try {
                onMain {
                    when (kind) {
                        "end" -> {
                            model.changeEndedOn("2026-01-05")
                            model.changeEndReason("结束状态完整")
                            model.changeRecordRecovery(true)
                            model.changeRecoveryAmount("0")
                        }
                        "correctMoney" -> {
                            model.changeAmount("800")
                            model.changeOccurredOn("2026-01-02")
                            model.changeDescription("纠错状态完整")
                        }
                    }
                    model.save()
                }
                await(model) {
                    if (kind == "delete") it.deletedProjectId == baseline.id && it.deleteConfirmation?.phase == SubmissionPhase.COMMITTED
                    else it.committedProjectId == baseline.id && (it.endEditor?.phase == SubmissionPhase.COMMITTED || it.moneyCorrection?.phase == SubmissionPhase.COMMITTED)
                }
            } finally { observer.cancel(); observer.join() }
            assertTrue(observed.isNotEmpty())
            observed.forEach { emitted ->
                val activeId = when (kind) {
                    "end" -> emitted.endEditor?.projectId
                    "correctMoney" -> emitted.moneyCorrection?.projectId
                    else -> emitted.deleteConfirmation?.projectId
                }
                assertEquals("An input or phase update must not publish an empty/different active $kind form", baseline.id, activeId)
                when (kind) {
                    "end" -> {
                        val editor = requireNotNull(emitted.endEditor)
                        assertEquals(baseline.name, editor.projectName)
                        assertEquals(editor.endedOn, editor.recoveryOn)
                    }
                    "correctMoney" -> {
                        val editor = requireNotNull(emitted.moneyCorrection)
                        assertEquals(event.id, editor.eventId)
                        assertEquals(event.role, editor.role)
                        assertEquals(event.direction, editor.direction)
                        assertTrue(editor.amount.isNotBlank())
                    }
                    else -> assertEquals(baseline.name, emitted.deleteConfirmation!!.projectName)
                }
            }
            when (kind) {
                "end" -> {
                    val persisted = repository.loadProjectDetail(baseline.id)!!
                    assertEquals(LocalDate.of(2026, 1, 5), persisted.project.endedOn)
                    assertEquals(0L, persisted.events.single { it.role == ProjectMoneyEventRole.END_SETTLEMENT }.amountCents)
                }
                "correctMoney" -> assertEquals(80000L, repository.loadProjectDetail(baseline.id)!!.events.single().amountCents)
                else -> assertNull(repository.loadProjectDetail(baseline.id))
            }
        }
    }

    private suspend fun startAndFill(model: ProjectViewModel, kind: String, id: String, eventId: String) {
        when (kind) {
            "edit" -> {
                onMain { model.startEdit(id) }
                await(model) { it.editor?.loading == false && it.editor?.needsInitialRead == false }
                onMain { model.changeName("陈旧编辑") }
            }
            "end" -> { startEnd(model, id); fillEnd(model, "100") }
            "updateEnd" -> { startUpdate(model, id); onMain { model.changeEndedOn("2026-01-06") } }
            "correctMoney" -> { startCorrection(model, id, eventId); onMain { model.changeAmount("700") } }
        }
    }
    private fun hasConflict(state: ProjectUiState): Boolean = state.editor?.conflict == true || state.endEditor?.conflict == true ||
        state.moneyCorrection?.conflict == true || state.deleteConfirmation?.conflict == true
    private fun formLoading(state: ProjectUiState): Boolean = state.editor?.loading == true || state.endEditor?.loading == true ||
        state.moneyCorrection?.loading == true || state.deleteConfirmation?.loading == true
    private fun formMessage(state: ProjectUiState): String? = state.editor?.message ?: state.endEditor?.message ?: state.moneyCorrection?.message ?: state.deleteConfirmation?.message

    private suspend fun seed(name: String = "长期项目", endedOn: LocalDate? = null): LongTermProjectEntity {
        val id = uuid()
        val operation = uuid()
        val project = LongTermProjectEntity(id, name, LithicDatabase.PROJECT_FALLBACK_ID, LocalDate.of(2026, 1, 1), endedOn, if (endedOn == null) null else "既有结束", createdAt = timestamp, updatedAt = timestamp)
        val event = ProjectMoneyEventEntity(uuid(), id, MoneyDirection.OUTLAY, 100000L, project.startedOn, role = ProjectMoneyEventRole.INITIAL, operationId = operation)
        assertTrue(repository.createProject(CreateProjectCommand(operation, project, event)) is MutationResult.Success)
        return project
    }

    private suspend fun append(id: String, role: ProjectMoneyEventRole, amount: String): ProjectMoneyEventEntity {
        val operation = uuid()
        val cents = amount.toLong() * 100
        val event = ProjectMoneyEventEntity(uuid(), id, MoneyDirection.RECOVERY, cents, LocalDate.of(2026, 1, 5), role = role, operationId = operation)
        assertTrue(repository.appendMoneyEvent(AddProjectMoneyEventCommand(operation, event)) is MutationResult.Success)
        return event
    }

    private suspend fun startEnd(model: ProjectViewModel, id: String) {
        onMain { model.startEnd(id) }
        assertNotNull("An end form must exist before its asynchronous read", model.state.value.endEditor)
        await(model) { it.endEditor?.loading == false && it.endEditor?.needsInitialRead == false }
    }

    private suspend fun startUpdate(model: ProjectViewModel, id: String) {
        onMain { model.startUpdateEnd(id) }
        assertNotNull(model.state.value.endEditor)
        await(model) { it.endEditor?.loading == false && it.endEditor?.needsInitialRead == false }
    }

    private suspend fun startCorrection(model: ProjectViewModel, id: String, event: String) {
        onMain { model.startCorrectMoney(id, event) }
        assertNotNull(model.state.value.moneyCorrection)
        await(model) { it.moneyCorrection?.loading == false && it.moneyCorrection?.needsInitialRead == false }
    }

    private suspend fun startDelete(model: ProjectViewModel, id: String) {
        onMain { model.startDelete(id) }
        assertNotNull(model.state.value.deleteConfirmation)
        await(model) { it.deleteConfirmation?.loading == false && it.deleteConfirmation?.needsInitialRead == false }
    }

    private fun fillEnd(model: ProjectViewModel, recovery: String) = onMain {
        model.changeEndedOn("2026-01-05")
        model.changeEndReason("真实结束")
        model.changeRecordRecovery(true)
        model.changeRecoveryAmount(recovery)
        model.changeRecoveryOn("2026-01-05")
    }

    private fun vm(repo: ProjectRepository = repository, handle: SavedStateHandle = SavedStateHandle()): ProjectViewModel {
        lateinit var model: ProjectViewModel
        onMain {
            model = ProjectViewModel(repo, handle, clock) { timestamp }
            ViewModelStore().also { it.put("project", model); stores[model] = it }
        }
        return model
    }

    private fun restore(handle: SavedStateHandle) = SavedStateHandle(handle.keys().associateWith { key ->
        val value = handle.get<Any?>(key)
        if (value is Bundle) Bundle(value) else value
    })
    private fun active(handle: SavedStateHandle): Bundle = requireNotNull(handle.get<Bundle>(FORM))
    private suspend fun clear(model: ProjectViewModel) {
        val job = model.viewModelScope.coroutineContext[Job]
        onMain { requireNotNull(stores[model]).clear() }
        job?.join()
    }
    private suspend fun await(model: ProjectViewModel, predicate: (ProjectUiState) -> Boolean) {
        val reached = withTimeoutOrNull(5_000) { model.state.first(predicate) }
        assertNotNull("Expected lifecycle state was not reached: ${model.state.value}", reached)
    }
    private suspend fun <T> delivered(value: CompletableDeferred<T>): T = requireNotNull(withTimeoutOrNull(5_000) { value.await() }) { "Real transaction/read was not delivered" }
    private fun onMain(action: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
    private fun uuid(): String = UUID.randomUUID().toString()
    companion object { private const val FORM = "lithic.project.activeForm" }
}
