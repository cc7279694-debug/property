package com.cooper.lithic.feature.review

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
import com.cooper.lithic.domain.report.*
import com.cooper.lithic.domain.report.LedgerSnapshot
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

@RunWith(AndroidJUnit4::class)
class M18TotalCostTrendViewModelTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val day = LocalDate.of(2026, 10, 21)
    private val observed = AtomicReference(day)
    private val clock = BusinessClock { observed.get() }
    private val time = Instant.parse("2026-10-21T04:00:00Z")
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private val stores = mutableListOf<ViewModelStore>()
    private val reports get() = ReportRepository(generation.database, generation.coordinator, clock)

    @Before fun before(): Unit = runBlocking {
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m18-trend-vm-${id()}").canonicalFile
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.mkdir())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        seed()
    }
    @After fun after(): Unit = runBlocking {
        onMain { stores.forEach { it.clear() } }
        generation.close()
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.name.startsWith("m18-trend-vm-"))
        check(root.deleteRecursively())
    }

    @Test fun stale_noncancellable_load_cannot_overwrite_new_snapshot_date_or_split_intent(): Unit = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        val first = AtomicBoolean(true)
        val repo = object : TotalCostTimelineRepository(reports, clock) {
            override val changes = emptyFlow<Unit>()
            override suspend fun loadSnapshot(asOf: LocalDate): LedgerSnapshot {
                val captured = super.loadSnapshot(asOf)
                if (first.compareAndSet(true, false)) return withContext(NonCancellable) {
                    entered.complete(Unit); release.await(); returned.complete(Unit); captured
                }
                return captured
            }
        }
        val model = model(repo)
        onMain { model.open() }
        withTimeout(15_000) { entered.await() }
        try {
            observed.set(day.plusDays(1))
            onMain { model.toggleBreakdown(); model.refresh() }
            await(model) { !it.loading && it.timeline?.asOf == day.plusDays(1) && it.selectedPoint != null }
            val selected = day.minusDays(4)
            onMain { model.selectDate(selected) }
            await(model) { it.selectedPoint?.date == selected }
            val current = model.state.value
            release.complete(Unit)
            withTimeout(15_000) { returned.await() }; delay(60); onMain { }
            assertEquals(current, model.state.value)
            assertTrue(model.state.value.showBreakdown)
            assertEquals(selected, model.state.value.selectedDate)
            assertEquals(CostTimelineEngine.point(checkNotNull(current.snapshot), selected), current.selectedPoint)
        } finally { release.complete(Unit) }
    }

    @Test fun read_failure_keeps_old_timestamped_data_and_never_turns_it_into_empty_or_zero(): Unit = runBlocking {
        val fail = AtomicBoolean(false)
        val repo = object : TotalCostTimelineRepository(reports, clock) {
            override val changes = emptyFlow<Unit>()
            override suspend fun loadSnapshot(asOf: LocalDate): LedgerSnapshot {
                if (fail.get()) throw IOException("Synthetic controlled failure")
                return super.loadSnapshot(asOf)
            }
        }
        val model = model(repo)
        onMain { model.open() }
        await(model) { !it.loading && it.selectedPoint != null }
        val old = model.state.value
        fail.set(true); observed.set(day.plusDays(1))
        onMain { model.refresh() }
        await(model) { !it.loading && it.error != null }
        assertEquals(old.snapshot, model.state.value.snapshot)
        assertEquals(old.timeline, model.state.value.timeline)
        assertEquals(old.selectedPoint, model.state.value.selectedPoint)
        assertTrue(checkNotNull(model.state.value.error).contains("尚未刷新"))
    }

    @Test fun write_day_and_foreground_refresh_publish_matching_snapshot_point_events_and_preserve_selection(): Unit = runBlocking {
        val model = model(TotalCostTimelineRepository(reports, clock))
        onMain { model.open() }
        await(model) { !it.loading && it.selectedPoint != null }
        val date = day.minusDays(4)
        onMain { model.toggleBreakdown(); model.selectDate(date) }
        await(model) { it.selectedPoint?.date == date }
        val project = generation.database.projectDao().listProjects().single()
        generation.coordinator.withWrite { generation.database.withTransaction {
            generation.database.projectDao().insertEvent(ProjectMoneyEventEntity(id(), project.id,
                MoneyDirection.OUTLAY, 200_000, date, "合成历史纠错后真实投入", ProjectMoneyEventRole.REGULAR, id()))
        } }
        await(model) { !it.loading && it.snapshot?.overview?.projects?.single()?.snapshot?.moneyEvents?.size == 2 && it.selectedPoint?.date == date }
        var current = model.state.value
        assertEquals(CostTimelineEngine.point(checkNotNull(current.snapshot), date), current.selectedPoint)
        assertEquals(1, current.events.count { it.date == date && it.amountCents != null })
        observed.set(day.plusDays(1)); onMain { model.pause(); model.open() }
        await(model) { !it.loading && it.timeline?.asOf == day.plusDays(1) && it.selectedPoint?.date == date }
        current = model.state.value
        assertEquals(date, current.selectedDate)
        assertTrue(current.showBreakdown)
        assertEquals(CostTimelineEngine.point(checkNotNull(current.snapshot), date), current.selectedPoint)
    }

    @Test fun future_selection_and_paused_late_result_do_not_publish_mismatched_amounts_or_write_facts(): Unit = runBlocking {
        val model = model(TotalCostTimelineRepository(reports, clock))
        val before = generation.backupSnapshotSource().capture()
        onMain { model.open() }
        await(model) { !it.loading && it.selectedPoint != null }
        val current = model.state.value
        onMain { model.selectDate(day.plusDays(1)) }
        assertEquals(current.selectedDate, model.state.value.selectedDate)
        assertEquals(current.selectedPoint, model.state.value.selectedPoint)
        assertNotNull(model.state.value.notice)
        assertNull(model.state.value.error)
        onMain { model.pause() }
        assertEquals(before, generation.backupSnapshotSource().capture())
    }

    private suspend fun seed() {
        val row = LongTermProjectEntity(id(), "合成趋势", LithicDatabase.PROJECT_FALLBACK_ID,
            day.minusDays(20), createdAt = time, updatedAt = time)
        generation.database.projectDao().insertProject(row)
        generation.database.projectDao().insertEvent(ProjectMoneyEventEntity(id(), row.id, MoneyDirection.OUTLAY,
            100_000, row.startedOn, role = ProjectMoneyEventRole.INITIAL, operationId = id()))
    }
    private fun model(repo: TotalCostTimelineRepository): TotalCostTrendViewModel = onMainValue {
        TotalCostTrendViewModel(repo, clock).also { vm -> ViewModelStore().also { it.put(id(), vm); stores += it } }
    }
    private suspend fun await(model: TotalCostTrendViewModel, predicate: (TotalCostTrendUiState) -> Boolean) =
        withTimeout(15_000) { model.state.first(predicate) }
    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    private fun <T> onMainValue(block: () -> T): T {
        var result: T? = null; onMain { result = block() }
        @Suppress("UNCHECKED_CAST") return result as T
    }
    private fun id() = UUID.randomUUID().toString()
}
