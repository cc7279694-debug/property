package com.cooper.lithic.feature.project

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.*
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real Room transactions; the only decorator drops one already-committed result delivery. */
@RunWith(AndroidJUnit4::class)
class M16ReplacementViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = FixedBusinessClock(LocalDate.of(2026, 10, 21))
    private val time = Instant.parse("2026-10-21T04:00:00Z")
    private val stores = mutableListOf<ViewModelStore>()
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var projects: ProjectRepository
    private lateinit var replacements: ReplacementRepository

    @Before fun before(): Unit = runBlocking {
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m16-vm-${UUID.randomUUID()}").canonicalFile
        assertEquals(context.noBackupFilesDir.canonicalFile, root.parentFile)
        check(root.mkdir())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        projects = ProjectRepository(generation.database, generation.coordinator, clock)
        replacements = ReplacementRepository(generation.database, generation.coordinator, clock)
    }
    @After fun after(): Unit = runBlocking {
        onMain { stores.forEach { it.clear() } }
        generation.close()
        assertEquals(context.noBackupFilesDir.canonicalFile, root.canonicalFile.parentFile)
        check(root.deleteRecursively())
    }

    @Test fun dirty_cancel_and_saved_raw_inputs_do_not_write_business_facts(): Unit = runBlocking {
        val old = seed("旧电脑")
        val handle = SavedStateHandle()
        val first = model(handle)
        start(first, old.id)
        onMain { first.changeName("未保存新电脑"); first.changeCash("12.345"); first.changeCredit("3000") }
        val command = handle.get<Bundle>(KEY)!!.getString("commandId")
        val restored = model(copyHandle(handle))
        assertEquals("未保存新电脑", restored.state.value.form!!.name)
        assertEquals("12.345", restored.state.value.form!!.cash)
        assertEquals(LeaveDecision.CONFIRM_DISCARD, restored.leaveDecision())
        onMain { restored.preview() }
        assertTrue(restored.state.value.form!!.errors.containsKey("cash"))
        assertEquals(command, handle.get<Bundle>(KEY)!!.getString("commandId"))
        onMain { restored.discardForm() }
        assertNull(restored.state.value.form)
        assertEquals(1, generation.database.projectDao().listProjects().size)
        assertEquals(1, generation.database.projectDao().listEvents().size)
        assertTrue(replacements.loadLinks().isEmpty())
    }

    @Test fun lost_result_double_click_and_recreated_retry_keep_the_exact_three_facts(): Unit = runBlocking {
        val old = seed("旧手机")
        val firstDelivery = AtomicBoolean(true)
        val calls = AtomicInteger()
        val observedResult = AtomicReference<ReplacementMutationResult>()
        val dropResult = object : ReplacementRepository(generation.database, generation.coordinator, clock) {
            override suspend fun recordTradeIn(command: TradeInCommand): ReplacementMutationResult {
                calls.incrementAndGet()
                val result = super.recordTradeIn(command)
                observedResult.set(result)
                if (firstDelivery.getAndSet(false)) throw IOException("Synthetic result delivery lost after real commit")
                return result
            }
        }
        val handle = SavedStateHandle()
        val first = model(handle, dropResult)
        start(first, old.id)
        onMain { first.changeName("新手机"); first.changeCash("5000"); first.changeCredit("3000"); first.preview() }
        assertTrue(first.state.value.form!!.confirmed)
        onMain { first.save(); first.save() }
        await(first) { it.form?.phase == SubmissionPhase.UNCONFIRMED }
        assertTrue("The real transaction before dropped delivery returned ${observedResult.get()}",
            observedResult.get() is ReplacementMutationResult.TradeInRecorded)
        assertEquals(1, calls.get())
        assertEquals(LeaveDecision.BLOCK_PENDING, first.leaveDecision())
        onMain { first.discardForm(); first.changeCash("9999") }
        assertEquals("5000", first.state.value.form!!.cash)
        val pending = handle.get<Bundle>(KEY)!!
        val fixedRevision = Instant.parse(pending.getString("writtenAt"))
        assertEquals(old.updatedAt.plusNanos(1), fixedRevision)
        val commandId = pending.getString("commandId")!!
        val restored = model(copyHandle(handle), dropResult)
        assertEquals(fixedRevision.toString(), pending.getBundle("submitted")!!.getString("writtenAt"))
        assertEquals(LeaveDecision.BLOCK_PENDING, restored.leaveDecision())
        assertEquals("5000", restored.state.value.form!!.cash)
        onMain { restored.save() }
        await(restored) { it.committedProjectId != null }
        val newId = TradeInIdentities.newProjectId(commandId)
        assertEquals(newId, restored.state.value.committedProjectId)
        val after = projects.loadProjectDetail(newId)!!
        assertEquals(800_000L, after.cost.grossOutlayCents)
        assertEquals(2, after.events.size)
        assertEquals(setOf(MoneyBasis.CASH, MoneyBasis.TRADE_IN_CREDIT), after.events.map { it.basis }.toSet())
        assertTrue(after.events.all { it.exchangeGroupId == commandId })
        assertEquals(4, generation.database.projectDao().listEvents().size)
        assertEquals(2, generation.database.projectDao().listProjects().size)
        assertEquals(1, replacements.loadLinks().size)
        assertEquals(2, calls.get())
    }

    @Test fun actual_project_form_codec_keeps_group_basis_and_blocks_individual_amount_date_changes(): Unit = runBlocking {
        val old = seed("旧相机")
        val oldDetail = projects.loadProjectDetail(old.id)!!
        val command = UUID.randomUUID().toString()
        val tradeAt = time.plusNanos(1)
        val newProject = LongTermProjectEntity(TradeInIdentities.newProjectId(command), "新相机", LithicDatabase.PROJECT_FALLBACK_ID,
            clock.today(), createdAt = tradeAt, updatedAt = tradeAt)
        val tradeResult = replacements.recordTradeIn(TradeInCommand(command, old, oldDetail.events, emptyList(), newProject,
            clock.today(), 0, 300_000, true, tradeAt))
        assertTrue("Expected a real trade-in commit, got $tradeResult", tradeResult is ReplacementMutationResult.TradeInRecorded)
        val detail = projects.loadProjectDetail(newProject.id)!!
        detail.events.forEach { assertEquals(it, ReplacementSavedFacts.event(ReplacementSavedFacts.event(it))) }
        detail.replacementLinks.forEach { assertEquals(it, ReplacementSavedFacts.link(ReplacementSavedFacts.link(it))) }
        val event = detail.events.single { it.role == ProjectMoneyEventRole.INITIAL }
        val handle = SavedStateHandle()
        val projectModel = onMainValue { ProjectViewModel(projects, handle, clock) { time } }.also { retain(it) }
        onMain { projectModel.startCorrectMoney(newProject.id, event.id) }
        withTimeout(15_000) { projectModel.state.first { it.moneyCorrection != null && !it.moneyCorrection!!.needsInitialRead && !it.moneyCorrection!!.loading } }
        assertTrue(projectModel.state.value.moneyCorrection!!.tradeInProtected)
        onMain { projectModel.changeAmount("9999"); projectModel.changeOccurredOn("2020-01-01"); projectModel.changeDescription("说明可更正") }
        assertEquals("0", projectModel.state.value.moneyCorrection!!.amount)
        assertEquals(clock.today().toString(), projectModel.state.value.moneyCorrection!!.occurredOn)
        onMain { projectModel.save() }
        withTimeout(15_000) { projectModel.state.first { it.committedProjectId == newProject.id } }
        val updated = projects.loadProjectDetail(newProject.id)!!.events.single { it.id == event.id }
        assertEquals(event.copy(description = "说明可更正"), updated)
    }

    private suspend fun seed(name: String): LongTermProjectEntity {
        val id = UUID.randomUUID().toString()
        val command = UUID.randomUUID().toString()
        val project = LongTermProjectEntity(id, name, LithicDatabase.PROJECT_FALLBACK_ID, clock.today().minusYears(3), createdAt = time, updatedAt = time)
        assertTrue(projects.createProject(CreateProjectCommand(command, project, ProjectMoneyEventEntity(UUID.randomUUID().toString(), id,
            MoneyDirection.OUTLAY, 900_000, project.startedOn, role = ProjectMoneyEventRole.INITIAL, operationId = command))) is MutationResult.Success)
        return project
    }
    private fun model(handle: SavedStateHandle = SavedStateHandle(), repo: ReplacementRepository = replacements): ReplacementViewModel =
        onMainValue { ReplacementViewModel(repo, projects, handle, clock) { time } }.also { retain(it) }
    private fun retain(model: androidx.lifecycle.ViewModel) = onMain { ViewModelStore().also { it.put(UUID.randomUUID().toString(), model); stores += it } }
    private suspend fun start(model: ReplacementViewModel, id: String) {
        onMain { model.startTradeIn(id) }
        await(model) { it.form?.loading == false && it.form!!.categories.isNotEmpty() }
    }
    private suspend fun await(model: ReplacementViewModel, predicate: (ReplacementUiState) -> Boolean) = withTimeout(15_000) { model.state.first(predicate) }
    private fun copyHandle(source: SavedStateHandle) = SavedStateHandle(mapOf(KEY to Bundle(source.get<Bundle>(KEY)!!)))
    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    private fun <T> onMainValue(block: () -> T): T {
        var value: T? = null
        onMain { value = block() }
        @Suppress("UNCHECKED_CAST") return value as T
    }
    companion object { private const val KEY = "lithic.replacement.activeForm" }
}
