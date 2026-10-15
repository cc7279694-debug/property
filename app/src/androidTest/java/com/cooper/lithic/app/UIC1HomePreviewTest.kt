package com.cooper.lithic.app

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
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
import com.cooper.lithic.data.repository.ProjectPage
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SubscriptionCursor
import com.cooper.lithic.data.repository.SubscriptionPage
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.feature.subscription.M09SubscriptionFixtures
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real isolated Room generations; only the UI read boundary is delayed in the race cases. */
@RunWith(AndroidJUnit4::class)
class UIC1HomePreviewTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = FixedBusinessClock(M09SubscriptionFixtures.today)
    private val today = M09SubscriptionFixtures.today
    private lateinit var directory: File
    private lateinit var generation: ManagedGeneration
    private val generations = mutableListOf<ManagedGeneration>()
    private val releases = mutableListOf<CompletableDeferred<Unit>>()
    private val viewModels = ViewModelStore()
    private val jobs = mutableListOf<Job>()

    @Before fun before() = runBlocking {
        directory = File(context.noBackupFilesDir, "ui-c1-home-${UUID.randomUUID()}")
        check(directory.mkdir())
        generation = openGeneration("first")
    }

    @After fun after() = runBlocking {
        releases.forEach { it.complete(Unit) }
        onMain { viewModels.clear() }
        jobs.forEach { it.join() }
        generations.forEach { it.close() }
        check(directory.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
        check(directory.deleteRecursively())
    }

    @Test fun full_identity_counts_and_current_previews_use_existing_bounded_interfaces() = runBlocking {
        repeat(55) { seedProject(generation, "当前项目$it", started = today.minusDays(60L - it)) }
        seedProject(generation, "历史项目", ended = today.minusDays(1))
        repeat(5) { seedSubscription(generation, "当前订阅$it") }
        seedSubscription(generation, "已停止订阅", stopped = true)
        val projectCalls = mutableListOf<Pair<Int, LocalDate>>()
        val projectWindowSizes = mutableListOf<Int>()
        val subscriptionCalls = mutableListOf<Triple<Boolean, Int, LocalDate>>()
        val projects = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadCurrentProjectWindow(pageCount: Int, asOf: LocalDate): ProjectPage {
                projectCalls += pageCount to asOf
                return super.loadCurrentProjectWindow(pageCount, asOf).also {
                    projectWindowSizes += it.items.size
                    assertNotNull(it.nextCursor)
                }
            }
        }
        val subscriptions = object : SubscriptionRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadManagementPage(stopped: Boolean, cursor: SubscriptionCursor?, pageSize: Int,
                asOf: LocalDate): SubscriptionPage {
                assertNull(cursor)
                subscriptionCalls += Triple(stopped, pageSize, asOf)
                return super.loadManagementPage(stopped, cursor, pageSize, asOf)
            }
        }
        val result = readHomeOverview(projects, subscriptions, today)
        assertEquals(56L, result.projectCount)
        assertEquals(6L, result.subscriptionCount)
        assertEquals(55L, result.preview.currentProjectCount)
        assertEquals(5L, result.preview.managedSubscriptionCount)
        assertEquals(3, result.preview.projects.size)
        assertEquals(3, result.preview.subscriptions.size)
        assertEquals(listOf(1 to today), projectCalls)
        assertEquals(listOf(50), projectWindowSizes)
        assertEquals(listOf(Triple(false, 3, today)), subscriptionCalls)
        assertTrue(result.preview.projects.all { it.asOf == today && it.project.endedOn == null })
        assertTrue(result.preview.subscriptions.all { it.asOf == today && it.cost.displayStatus != SubscriptionDisplayStatus.STOPPED })
    }

    @Test fun history_only_ledger_retains_identity_counts_with_empty_current_sections() = runBlocking {
        seedProject(generation, "历史项目", ended = today.minusDays(1))
        seedSubscription(generation, "已停止订阅", stopped = true)
        val result = realRead(generation)
        assertEquals(1L, result.projectCount)
        assertEquals(1L, result.subscriptionCount)
        assertEquals(0L, result.preview.currentProjectCount)
        assertEquals(0L, result.preview.managedSubscriptionCount)
        assertTrue(result.preview.projects.isEmpty())
        assertTrue(result.preview.subscriptions.isEmpty())
    }

    @Test fun future_started_project_and_prepaid_subscription_keep_the_shared_observation_day() = runBlocking {
        seedProject(generation, "未来开始", started = today.plusDays(5))
        val facts = M09SubscriptionFixtures.facts("未来生效", futurePayment = true)
        generation.coordinator.withWrite { generation.database.withTransaction {
            generation.database.subscriptionDao().insertSubscription(facts.subscription)
            generation.database.subscriptionDao().insertRenewal(facts.renewals.last())
        } }
        val result = realRead(generation)
        assertEquals(today, result.preview.asOf)
        assertEquals(today, result.preview.projects.single().asOf)
        assertTrue(result.preview.projects.single().cost.unavailableReason != null)
        val subscription = result.preview.subscriptions.single()
        assertEquals(today, subscription.asOf)
        assertEquals(SubscriptionDisplayStatus.EXPIRED, subscription.cost.displayStatus)
        assertEquals(30L, subscription.cost.standardPurchasedDays)
    }

    @Test fun failed_refresh_keeps_last_successful_counts_and_previews() = runBlocking {
        seedProject(generation, "保留项目")
        val holder = holder(MutableStateFlow<StorageInitializationState>(StorageInitializationState.Ready(generation)))
        val before = ready(holder) { !it.isRefreshing }
        generation.database.close()
        onMain { holder.retryRead() }
        val failed = ready(holder) { it.readError != null && !it.isRefreshing }
        assertEquals(before.projectCount, failed.projectCount)
        assertEquals(before.homePreview, failed.homePreview)
        assertSame(before.repository, failed.repository)
        assertFalse(failed.homePreview!!.projects.isEmpty())
    }

    @Test fun late_retry_success_cannot_replace_a_later_change_refresh() = runBlocking {
        overlappingRetry(failOldRead = false)
    }

    @Test fun late_retry_failure_cannot_mark_a_later_success_as_stale() = runBlocking {
        overlappingRetry(failOldRead = true)
    }

    @Test fun late_old_repository_retry_cannot_publish_after_storage_generation_changes() = runBlocking {
        seedProject(generation, "旧代项目")
        val blockNext = AtomicBoolean(false)
        val started = CompletableDeferred<Job>()
        val release = CompletableDeferred<Unit>().also(releases::add)
        val states = MutableStateFlow<StorageInitializationState>(StorageInitializationState.Ready(generation))
        val holder = holder(states) { projects, subscriptions, asOf ->
            val result = readHomeOverview(projects, subscriptions, asOf)
            if (blockNext.compareAndSet(true, false)) {
                started.complete(checkNotNull(currentCoroutineContext()[Job]))
                release.await()
            }
            result
        }
        val old = ready(holder) { !it.isRefreshing }
        blockNext.set(true)
        onMain { holder.retryRead() }
        val oldReadJob = withTimeout(10_000) { started.await() }
        val next = openGeneration("second")
        seedProject(next, "新代项目一")
        seedProject(next, "新代项目二")
        onMain { states.value = StorageInitializationState.Ready(next) }
        val current = ready(holder) { it.repository !== old.repository && !it.isRefreshing }
        assertEquals(2L, current.projectCount)
        assertNotSame(old.mediaRepository, current.mediaRepository)
        release.complete(Unit)
        withTimeout(10_000) { oldReadJob.join() }
        repeat(3) { withContext(Dispatchers.Main) { Unit } }
        val final = holder.state.value as LedgerUiState.Ready
        assertSame(current.repository, final.repository)
        assertSame(current.mediaRepository, final.mediaRepository)
        assertEquals(2L, final.projectCount)
        assertTrue(final.homePreview!!.projects.all { it.project.name.startsWith("新代") })
        assertNull(final.readError)
    }

    private suspend fun overlappingRetry(failOldRead: Boolean) {
        seedProject(generation, "原项目")
        val blockNext = AtomicBoolean(false)
        val started = CompletableDeferred<Job>()
        val release = CompletableDeferred<Unit>().also(releases::add)
        val holder = holder(MutableStateFlow<StorageInitializationState>(StorageInitializationState.Ready(generation))) { projects, subscriptions, asOf ->
            val result = readHomeOverview(projects, subscriptions, asOf)
            if (blockNext.compareAndSet(true, false)) {
                started.complete(checkNotNull(currentCoroutineContext()[Job]))
                release.await()
                if (failOldRead) throw IOException("synthetic delayed read failure")
            }
            result
        }
        ready(holder) { !it.isRefreshing }
        blockNext.set(true)
        onMain { holder.retryRead() }
        val oldReadJob = withTimeout(10_000) { started.await() }
        seedProject(generation, "后来写入")
        val latest = ready(holder) { it.projectCount == 2L && !it.isRefreshing }
        assertNull(latest.readError)
        release.complete(Unit)
        withTimeout(10_000) { oldReadJob.join() }
        repeat(3) { withContext(Dispatchers.Main) { Unit } }
        val final = holder.state.value as LedgerUiState.Ready
        assertEquals(2L, final.projectCount)
        assertSame(latest.homePreview, final.homePreview)
        assertNull(final.readError)
    }

    private fun holder(states: MutableStateFlow<StorageInitializationState>,
        reader: suspend (ProjectRepository, SubscriptionRepository, LocalDate) -> HomeReadResult = ::readHomeOverview): LedgerUiStateHolder {
        lateinit var holder: LedgerUiStateHolder
        onMain {
            holder = LedgerUiStateHolder(states, homeReader = reader, clockFactory = { clock })
            viewModels.put(UUID.randomUUID().toString(), holder)
            jobs += checkNotNull(holder.viewModelScope.coroutineContext[Job])
        }
        return holder
    }

    private suspend fun ready(holder: LedgerUiStateHolder, predicate: (LedgerUiState.Ready) -> Boolean = { true }): LedgerUiState.Ready =
        withTimeout(10_000) { holder.state.filterIsInstance<LedgerUiState.Ready>().first { predicate(it) } }

    private suspend fun realRead(generation: ManagedGeneration): HomeReadResult = readHomeOverview(
        ProjectRepository(generation.database, generation.coordinator, clock),
        SubscriptionRepository(generation.database, generation.coordinator, clock), today)

    private suspend fun openGeneration(name: String): ManagedGeneration {
        val child = File(directory, name)
        check(child.mkdir())
        return DataGenerationStore(context, child).open(ZoneId.of("Asia/Shanghai")).also(generations::add)
    }

    private suspend fun seedProject(generation: ManagedGeneration, name: String,
        started: LocalDate = today.minusDays(10), ended: LocalDate? = null) {
        val project = LongTermProjectEntity(UUID.randomUUID().toString(), name, LithicDatabase.PROJECT_FALLBACK_ID,
            started, ended, endReason = ended?.let { "已核对结束" },
            createdAt = M09SubscriptionFixtures.timestamp, updatedAt = M09SubscriptionFixtures.timestamp)
        val event = ProjectMoneyEventEntity(UUID.randomUUID().toString(), project.id, MoneyDirection.OUTLAY, 100L,
            started, role = ProjectMoneyEventRole.INITIAL, operationId = UUID.randomUUID().toString())
        generation.coordinator.withWrite { generation.database.withTransaction {
            generation.database.projectDao().insertProject(project)
            generation.database.projectDao().insertEvent(event)
        } }
    }

    private suspend fun seedSubscription(generation: ManagedGeneration, name: String, stopped: Boolean = false) {
        val facts = M09SubscriptionFixtures.facts(name, if (stopped) SubscriptionOperationState.STOPPED else null)
        generation.coordinator.withWrite { generation.database.withTransaction {
            generation.database.subscriptionDao().apply {
                insertSubscription(facts.subscription)
                facts.renewals.forEach { insertRenewal(it) }
                facts.stateChanges.forEach { insertStateChange(it) }
            }
        } }
    }

    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
}
