package com.cooper.lithic.feature.review

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelStore
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
class M19ReportAnalysisViewModelTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val day = LocalDate.of(2026, 10, 21)
    private val observed = AtomicReference(day)
    private val clock = BusinessClock { observed.get() }
    private val time = Instant.parse("2026-10-21T04:00:00Z")
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var categoryId: String
    private val stores = mutableListOf<ViewModelStore>()
    private val reports get() = ReportRepository(generation.database, generation.coordinator, clock)

    @Before fun before(): Unit = runBlocking {
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m19-report-vm-${id()}").canonicalFile
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.mkdir())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        categoryId = id()
        val categories = CategoryRepository(generation.database, generation.coordinator)
        assertTrue(categories.create(CategoryType.PROJECT, "合成类别", categoryId) is CategoryMutationResult.Applied)
        val project = LongTermProjectEntity(id(), "合成年度项目", categoryId, day.minusYears(2), createdAt = time, updatedAt = time)
        generation.database.projectDao().insertProject(project)
        generation.database.projectDao().insertEvent(ProjectMoneyEventEntity(id(), project.id, MoneyDirection.OUTLAY,
            800_000, project.startedOn, role = ProjectMoneyEventRole.INITIAL, operationId = id()))
    }
    @After fun after(): Unit = runBlocking {
        onMain { stores.forEach { it.clear() } }
        generation.close()
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.name.startsWith("m19-report-vm-"))
        check(root.deleteRecursively())
    }

    @Test fun old_noncancellable_read_cannot_overwrite_new_year_category_or_observation(): Unit = runBlocking {
        val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>(); val returned = CompletableDeferred<Unit>()
        val first = AtomicBoolean(true)
        val repository = object : TotalCostTimelineRepository(reports, clock) {
            override val changes = emptyFlow<Unit>()
            override suspend fun loadSnapshot(asOf: LocalDate): LedgerSnapshot {
                val captured = super.loadSnapshot(asOf)
                if (first.compareAndSet(true, false)) return withContext(NonCancellable) {
                    entered.complete(Unit); release.await(); returned.complete(Unit); captured
                }
                return captured
            }
        }
        val model = model(repository)
        onMain { model.open() }; withTimeout(15_000) { entered.await() }
        try {
            observed.set(day.plusDays(1))
            onMain { model.selectYear(2024); model.selectCategory(OverviewKind.PROJECT, categoryId); model.refresh() }
            await(model) { !it.loading && it.asOf == day.plusDays(1) && it.annual?.year == 2024 && it.selectedCategoryId == categoryId }
            val expected = model.state.value
            release.complete(Unit); withTimeout(15_000) { returned.await() }; delay(60); onMain { }
            assertEquals(expected, model.state.value)
            assertEquals(800_000L, expected.annual!!.amounts.projectOutlayCents)
        } finally { release.complete(Unit) }
    }

    @Test fun merge_of_selected_category_clears_only_invalid_selection_with_explicit_notice(): Unit = runBlocking {
        val model = model(TotalCostTimelineRepository(reports, clock))
        onMain { model.selectYear(2024); model.selectCategory(OverviewKind.PROJECT, categoryId); model.open() }
        await(model) { !it.loading && it.selectedCategoryId == categoryId && it.selectedCategory != null }
        val categories = CategoryRepository(generation.database, generation.coordinator)
        val preview = categories.previewMerge(categoryId, LithicDatabase.PROJECT_FALLBACK_ID)
        assertTrue(categories.merge(preview) is CategoryMutationResult.Applied)
        await(model) { !it.loading && it.snapshot != null && it.categories.none { c -> c.category.id == categoryId } }
        assertNull(model.state.value.selectedCategoryId)
        assertEquals(OverviewKind.PROJECT, model.state.value.selectedKind)
        assertEquals(2024, model.state.value.selectedYear)
        assertTrue(checkNotNull(model.state.value.notice).contains("合并或删除"))
        assertEquals(800_000L, model.state.value.annual!!.amounts.projectOutlayCents)
    }

    @Test fun failed_refresh_keeps_matching_old_report_and_never_publishes_fake_empty(): Unit = runBlocking {
        val fail = AtomicBoolean(false)
        val repository = object : TotalCostTimelineRepository(reports, clock) {
            override val changes = emptyFlow<Unit>()
            override suspend fun loadSnapshot(asOf: LocalDate): LedgerSnapshot {
                if (fail.get()) throw IOException("Synthetic controlled failure")
                return super.loadSnapshot(asOf)
            }
        }
        val model = model(repository)
        onMain { model.selectYear(2024); model.open() }
        await(model) { !it.loading && it.annual != null }
        val expected = model.state.value
        fail.set(true); observed.set(day.plusDays(1)); onMain { model.refresh() }
        await(model) { !it.loading && it.error != null }
        assertEquals(expected.annual, model.state.value.annual)
        assertEquals(expected.categories, model.state.value.categories)
        assertEquals(expected.asOf, model.state.value.asOf)
        assertTrue(checkNotNull(model.state.value.error).contains("尚未刷新"))
        val retainedError = model.state.value.error
        onMain { model.selectYear(2025) }
        await(model) { !it.loading && it.annual?.year == 2025 }
        assertEquals(expected.asOf, model.state.value.asOf)
        assertEquals(retainedError, model.state.value.error)
        onMain { model.selectCategory(OverviewKind.PROJECT, categoryId) }
        await(model) { !it.loading && it.selectedCategoryId == categoryId }
        assertEquals(retainedError, model.state.value.error)
        assertSame(expected.snapshot, model.state.value.snapshot)
        fail.set(false); onMain { model.refresh() }
        await(model) { !it.loading && it.asOf == day.plusDays(1) }
        assertNull(model.state.value.error)
    }

    @Test fun write_foreground_and_day_refresh_keep_intent_and_browsing_changes_no_facts(): Unit = runBlocking {
        val model = model(TotalCostTimelineRepository(reports, clock))
        onMain { model.selectYear(2024); model.selectCategory(OverviewKind.PROJECT, categoryId); model.open() }
        await(model) { !it.loading && it.annual != null }
        val before = generation.backupSnapshotSource().capture()
        onMain { model.selectYear(2025) }
        await(model) { !it.loading && it.annual?.year == 2025 }
        assertFalse(model.state.value.annual!!.amounts.hasRecords)
        onMain { model.selectYear(2024) }
        await(model) { !it.loading && it.annual?.year == 2024 }
        assertEquals(before, generation.backupSnapshotSource().capture())
        val project = generation.database.projectDao().listProjects().single()
        generation.coordinator.withWrite {
            generation.database.projectDao().insertEvent(ProjectMoneyEventEntity(id(), project.id, MoneyDirection.RECOVERY,
                900_000, day, "合成真实收回", ProjectMoneyEventRole.REGULAR, id()))
        }
        await(model) { !it.loading && it.selectedCategory?.historical?.projectNetCents == -100_000L }
        observed.set(day.plusDays(1)); onMain { model.pause(); model.open() }
        await(model) { !it.loading && it.asOf == day.plusDays(1) }
        assertEquals(2024, model.state.value.selectedYear)
        assertEquals(categoryId, model.state.value.selectedCategoryId)
    }
    private fun model(repository: TotalCostTimelineRepository): ReportAnalysisViewModel = onMainValue {
        ReportAnalysisViewModel(repository, clock).also { vm -> ViewModelStore().also { it.put(id(), vm); stores += it } }
    }
    private suspend fun await(model: ReportAnalysisViewModel, predicate: (ReportAnalysisUiState) -> Boolean) =
        withTimeout(15_000) { model.state.first(predicate) }
    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    private fun <T> onMainValue(block: () -> T): T { var result: T? = null; onMain { result = block() }; @Suppress("UNCHECKED_CAST") return result as T }
    private fun id() = UUID.randomUUID().toString()
}
