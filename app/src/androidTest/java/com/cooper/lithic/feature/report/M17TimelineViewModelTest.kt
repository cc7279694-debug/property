package com.cooper.lithic.feature.report

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.*
import com.cooper.lithic.data.repository.*
import com.cooper.lithic.domain.report.ProjectTimelineEngine
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real Room inputs. Decorators delay or fail delivery, never invent monetary results. */
@RunWith(AndroidJUnit4::class)
class M17TimelineViewModelTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val firstDay = LocalDate.of(2026, 10, 21)
    private val observedDay = AtomicReference(firstDay)
    private val clock = BusinessClock { observedDay.get() }
    private val time = Instant.parse("2026-10-21T04:00:00Z")
    private val stores = mutableListOf<ViewModelStore>()
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var projects: ProjectRepository
    private lateinit var subscriptions: SubscriptionRepository

    @Before fun before(): Unit = runBlocking {
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m17-vm-${id()}").canonicalFile
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.mkdir())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        projects = ProjectRepository(generation.database, generation.coordinator, clock)
        subscriptions = SubscriptionRepository(generation.database, generation.coordinator, clock)
    }

    @After fun after(): Unit = runBlocking {
        onMain { stores.forEach { it.clear() } }
        generation.close()
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.name.startsWith("m17-vm-"))
        check(root.deleteRecursively())
    }

    @Test fun late_old_request_cannot_replace_new_record_or_its_selected_exact_date(): Unit = runBlocking {
        val old = seed("迟到旧图", 100_000)
        val current = seed("现在的新图", 200_000)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        val repo = object : ItemTimelineRepository(projects, subscriptions, clock) {
            override val changes = emptyFlow<Unit>()
            override suspend fun loadProject(id: String, asOf: LocalDate): ProjectTimelineData? {
                if (id == old.id) return withContext(NonCancellable) {
                    entered.complete(Unit); release.await()
                    super.loadProject(id, asOf).also { returned.complete(Unit) }
                }
                return super.loadProject(id, asOf)
            }
        }
        val model = model(repo)
        onMain { model.openProject(old.id) }
        withTimeout(15_000) { entered.await() }
        try {
            onMain { model.openProject(current.id) }
            await(model) { !it.loading && it.project?.detail?.project?.id == current.id }
            val selected = firstDay.minusDays(7)
            onMain { model.selectDate(selected) }
            await(model) { it.selectedProject?.date == selected }
            assertEquals(selected, model.state.value.selectedDate)
            val input = checkNotNull(model.state.value.project).snapshot
            assertEquals(ProjectTimelineEngine.point(input, firstDay, selected), model.state.value.selectedProject)
            release.complete(Unit)
            // Wait for the real stale query to finish; cancellation alone does not stop in-flight IO.
            withTimeout(15_000) { returned.await() }
            delay(50)
            onMain { }
            assertEquals(current.id, model.state.value.id)
            assertEquals(current.id, model.state.value.project?.detail?.project?.id)
            assertEquals(selected, model.state.value.selectedDate)
        } finally { release.complete(Unit) }
    }

    @Test fun refresh_error_keeps_last_graph_and_explicit_error_while_confirmed_deletion_is_missing(): Unit = runBlocking {
        val project = seed("可恢复读取错误", 100_000)
        val shouldFail = AtomicBoolean(false)
        val repo = object : ItemTimelineRepository(projects, subscriptions, clock) {
            override val changes = emptyFlow<Unit>()
            override suspend fun loadProject(id: String, asOf: LocalDate): ProjectTimelineData? {
                if (shouldFail.get()) throw IOException("Synthetic controlled read failure")
                return super.loadProject(id, asOf)
            }
        }
        val model = model(repo)
        onMain { model.openProject(project.id) }
        await(model) { !it.loading && it.project != null }
        val original = model.state.value.project
        shouldFail.set(true)
        onMain { model.refresh() }
        await(model) { !it.loading && it.error != null }
        assertEquals(original, model.state.value.project)
        assertFalse(model.state.value.missing)
        shouldFail.set(false)
        generation.coordinator.withWrite {
            generation.database.withTransaction {
                generation.database.openHelper.writableDatabase.execSQL("DELETE FROM project_money_events WHERE projectId=?", arrayOf(project.id))
                generation.database.openHelper.writableDatabase.execSQL("DELETE FROM long_term_projects WHERE id=?", arrayOf(project.id))
            }
        }
        onMain { model.refresh() }
        await(model) { !it.loading && it.missing }
        assertNull(model.state.value.error)
        assertNull(model.state.value.project)
        assertNull(model.state.value.selectedProject)
    }

    @Test fun real_write_invalidation_and_business_day_refresh_recompute_without_losing_chosen_history_date(): Unit = runBlocking {
        val project = seed("写入和跨日", 100_000)
        val repo = ItemTimelineRepository(projects, subscriptions, clock)
        val model = model(repo)
        onMain { model.openProject(project.id) }
        await(model) { !it.loading && it.project != null }
        val historyDate = firstDay.minusDays(4)
        onMain { model.selectDate(historyDate) }
        await(model) { it.selectedProject?.date == historyDate }
        val before = generation.backupSnapshotSource().capture()
        val operationId = id()
        generation.coordinator.withWrite {
            generation.database.withTransaction {
                generation.database.projectDao().insertEvent(ProjectMoneyEventEntity(id(), project.id,
                    MoneyDirection.OUTLAY, 200_000, firstDay, "合成今日投入", role = ProjectMoneyEventRole.REGULAR, operationId = operationId))
            }
        }
        await(model) { !it.loading && it.project?.detail?.events?.size == 2 && it.selectedProject?.date == historyDate }
        assertEquals(300_000L, model.state.value.project?.detail?.cost?.netCents)
        assertEquals(historyDate, model.state.value.selectedDate)
        assertEquals(100_000L, model.state.value.selectedProject?.cost?.netCents)
        observedDay.set(firstDay.plusDays(1))
        onMain { model.refresh() }
        await(model) { !it.loading && it.project?.timeline?.asOf == firstDay.plusDays(1) }
        assertEquals(firstDay.plusDays(1), model.state.value.project?.timeline?.holdingPoints?.last()?.date)
        assertEquals(historyDate, model.state.value.selectedDate)
        val after = generation.backupSnapshotSource().capture()
        assertEquals(before.data.projects, after.data.projects)
        assertEquals(before.data.subscriptionRenewals, after.data.subscriptionRenewals)
        assertEquals(before.data.subscriptionStateChanges, after.data.subscriptionStateChanges)
        assertEquals(before.data.projectMoneyEvents.size + 1, after.data.projectMoneyEvents.size)
    }

    @Test fun real_trade_in_event_mapping_keeps_saved_noncash_identity_and_description_without_inventing_payment(): Unit = runBlocking {
        val old = seed("折抵旧机", 900_000)
        val beforeOld = checkNotNull(projects.loadProjectDetail(old.id))
        val operation = id()
        val revision = time.plusSeconds(1)
        val next = LongTermProjectEntity(TradeInIdentities.newProjectId(operation), "折抵新机",
            LithicDatabase.PROJECT_FALLBACK_ID, firstDay, createdAt = revision, updatedAt = revision)
        val outcome = ReplacementRepository(generation.database, generation.coordinator, clock).recordTradeIn(
            TradeInCommand(operation, old, beforeOld.events, emptyList(), next, firstDay, 500_000, 300_000, true, revision))
        assertTrue(outcome is ReplacementMutationResult.TradeInRecorded)
        val before = generation.backupSnapshotSource().capture()
        val model = model(ItemTimelineRepository(projects, subscriptions, clock))
        onMain { model.openProject(next.id) }
        await(model) { !it.loading && it.project != null && it.selectedProject != null }
        val detail = checkNotNull(model.state.value.project).detail
        assertEquals(2, detail.events.size)
        val credit = detail.events.single { it.basis == MoneyBasis.TRADE_IN_CREDIT }
        val cash = detail.events.single { it.basis == MoneyBasis.CASH }
        val chartCredit = model.state.value.events.single { it.id == credit.id }
        assertTrue("Stored trade-in credit must keep its non-cash label", chartCredit.nonCash)
        assertEquals(credit.amountCents, chartCredit.amountCents)
        assertEquals(credit.occurredOn, chartCredit.date)
        assertFalse(model.state.value.events.single { it.id == cash.id }.nonCash)
        assertFalse("Acquisition boundary cannot pretend to be a cash or trade-in payment",
            model.state.value.events.single { it.id == "acquired" }.nonCash)
        assertEquals(2, model.state.value.events.count { it.amountCents != null })
        assertEquals(800_000L, model.state.value.selectedProject?.cost?.netCents)
        assertEquals(before, generation.backupSnapshotSource().capture())
    }

    @Test fun m22_repeated_completed_date_keeps_exact_result_without_restarting_query(): Unit = runBlocking {
        val project = seed("重复自然日", 100_000)
        val model = model(ItemTimelineRepository(projects, subscriptions, clock))
        onMain { model.openProject(project.id) }
        await(model) { !it.loading && it.selectedProject != null }
        onMain {
            val observed = model.state.value
            repeat(20) { model.selectDate(checkNotNull(observed.selectedDate)) }
            assertSame(observed, model.state.value)
            assertNotNull(model.state.value.selectedProject)
        }
    }

    private suspend fun seed(name: String, cents: Long): LongTermProjectEntity {
        val entity = LongTermProjectEntity(id(), name, LithicDatabase.PROJECT_FALLBACK_ID,
            firstDay.minusDays(20), createdAt = time, updatedAt = time)
        generation.coordinator.withWrite {
            generation.database.withTransaction {
                generation.database.projectDao().insertProject(entity)
                generation.database.projectDao().insertEvent(ProjectMoneyEventEntity(id(), entity.id,
                    MoneyDirection.OUTLAY, cents, entity.startedOn, role = ProjectMoneyEventRole.INITIAL, operationId = id()))
            }
        }
        return entity
    }
    private fun model(repository: ItemTimelineRepository): ItemTimelineViewModel = onMainValue {
        ItemTimelineViewModel(repository, clock).also { vm ->
            ViewModelStore().also { it.put(id(), vm); stores += it }
        }
    }
    private suspend fun await(model: ItemTimelineViewModel, predicate: (ItemTimelineUiState) -> Boolean) =
        withTimeout(15_000) { model.state.first(predicate) }
    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    private fun <T> onMainValue(block: () -> T): T {
        var value: T? = null
        onMain { value = block() }
        @Suppress("UNCHECKED_CAST") return value as T
    }
    private fun id() = UUID.randomUUID().toString()
}
