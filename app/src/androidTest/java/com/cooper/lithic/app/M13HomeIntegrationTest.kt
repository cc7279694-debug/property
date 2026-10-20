package com.cooper.lithic.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.backup.BackupExportResult
import com.cooper.lithic.data.backup.BackupExportService
import com.cooper.lithic.data.backup.BackupTarget
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.backup.RestoreCoordinator
import com.cooper.lithic.data.backup.RestoreResult
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.data.repository.AddProjectMoneyEventCommand
import com.cooper.lithic.data.repository.ConfirmRenewalCommand
import com.cooper.lithic.data.repository.CreateProjectCommand
import com.cooper.lithic.data.repository.CreateSubscriptionCommand
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SetSubscriptionStateCommand
import com.cooper.lithic.data.repository.SubscriptionLifecycleResult
import com.cooper.lithic.data.repository.SubscriptionMutationResult
import com.cooper.lithic.data.repository.SubscriptionStateAction
import com.cooper.lithic.data.repository.facts
import com.cooper.lithic.domain.report.OverviewResult
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Room/repositories -> holder -> overview, using synthetic private owners only.
 * Backup uses the real export/restore services with an owned file; this is not a new system SAF test.
 */
@RunWith(AndroidJUnit4::class)
class M13HomeIntegrationTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val zone = ZoneId.of("Asia/Shanghai")
    private val start = LocalDate.of(2026, 1, 1)
    private val observation = AtomicReference(start.plusDays(2))
    private val clock = BusinessClock { observation.get() }
    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")
    private val viewModels = ViewModelStore()
    private val owners = mutableListOf<AppContainer>()
    private lateinit var root: File
    private lateinit var owner: AppContainer
    private lateinit var generation: ManagedGeneration
    private lateinit var holder: LedgerUiStateHolder

    @Before fun before(): Unit = runBlocking {
        assertEquals("Run only with M13IsolatedRunner; the normal ledger Application must never start",
            Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m13-home-${id()}").canonicalFile
        assertTrue(root.mkdir())
        owner = newOwner("primary")
        generation = owner.openStorage()
        holder = withContext(Dispatchers.Main) {
            LedgerUiStateHolder(owner.storageState) { savedZone ->
                assertEquals(zone, savedZone)
                clock
            }.also { viewModels.put("home", it) }
        }
        awaitReady(holder)
    }

    @After fun after(): Unit = runBlocking {
        withContext(Dispatchers.Main) { viewModels.clear() }
        owners.asReversed().forEach { it.close() }
        if (::root.isInitialized) {
            assertEquals(context.noBackupFilesDir.canonicalFile, root.canonicalFile.parentFile)
            assertTrue(root.deleteRecursively())
        }
    }

    @Test fun project_create_and_append_refresh_home_from_real_repository_changes(): Unit = runBlocking {
        val initial = awaitReady(holder)
        val command = projectCommand(30_000)
        assertTrue(initial.repository.createProject(command) is MutationResult.Success)
        val created = awaitRates(project = "100", subscription = "0", total = "100")
        assertEquals(1L, created.projectCount)
        assertEquals(1L, overview(created).activeProjectCount)
        val operation = id()
        val event = ProjectMoneyEventEntity(id(), command.project.id, MoneyDirection.OUTLAY, 15_000,
            observation.get(), role = ProjectMoneyEventRole.REGULAR, operationId = operation)
        assertTrue(created.repository.appendMoneyEvent(AddProjectMoneyEventCommand(operation, event)) is MutationResult.Success)
        val updated = awaitRates(project = "150", subscription = "0", total = "150")
        assertEquals(1L, updated.projectCount)
        assertEquals(2, generation.database.projectDao().eventsForProjects(listOf(command.project.id)).size)
        assertEquals(overview(updated), checkNotNull(updated.reportRepository).loadOverview())
        assertNull(updated.readError)
    }

    @Test fun real_zero_project_is_distinct_from_empty_home_without_demo_records(): Unit = runBlocking {
        val empty = awaitReady(holder)
        assertFalse(overview(empty).hasRecords)
        assertEquals(0L, empty.projectCount)
        assertEquals(0L, empty.subscriptionCount)
        decimal("0", overview(empty).totalRate)
        assertTrue(empty.repository.createProject(projectCommand(0)) is MutationResult.Success)
        val zero = awaitReady(holder) { it.projectCount == 1L }
        assertTrue(overview(zero).hasRecords)
        assertEquals(1L, overview(zero).activeProjectCount)
        decimal("0", overview(zero).totalRate)
        assertEquals(1L, overview(zero).projectCategories.single().contributingCount)
    }

    @Test fun prepaid_renewal_keeps_old_home_price_until_explicit_cross_day_refresh(): Unit = runBlocking {
        val initial = awaitReady(holder)
        val command = subscriptionCommand()
        assertTrue(initial.subscriptionRepository.createWithRenewal(command) is SubscriptionMutationResult.Success)
        val active = awaitRates(project = "0", subscription = "5", total = "5")
        val next = renewal(command.subscription.id, 18_000, start.plusMonths(1), start.plusMonths(2), observation.get())
        assertTrue(active.subscriptionRepository.confirmRenewal(ConfirmRenewalCommand(next.operationId, next)) is SubscriptionMutationResult.Success)
        retry(holder)
        val prepaid = awaitRates(project = "0", subscription = "5", total = "5")
        assertEquals(1L, prepaid.subscriptionCount)
        assertEquals(2L, generation.database.subscriptionDao().countRenewals(command.subscription.id))
        observation.set(start.plusMonths(1))
        retry(holder)
        val effective = awaitRates(project = "0", subscription = "6", total = "6")
        assertEquals(observation.get(), overview(effective).asOf)
        assertEquals(2L, generation.database.subscriptionDao().countRenewals(command.subscription.id))
    }

    @Test fun pause_and_resume_refresh_home_without_fabricating_another_payment(): Unit = runBlocking {
        val initial = awaitReady(holder)
        val command = subscriptionCommand()
        assertTrue(initial.subscriptionRepository.createWithRenewal(command) is SubscriptionMutationResult.Success)
        val active = awaitRates(project = "0", subscription = "5", total = "5")
        val subscriptions = active.subscriptionRepository
        val before = checkNotNull(subscriptions.loadSubscriptionDetail(command.subscription.id)).facts()
        val pause = SubscriptionStateChangeEntity(id(), command.subscription.id, observation.get(),
            SubscriptionOperationState.PAUSED, operationId = id())
        assertTrue(subscriptions.setState(SetSubscriptionStateCommand(pause.operationId, before, pause,
            SubscriptionStateAction.PAUSE)) is SubscriptionLifecycleResult.Saved)
        val paused = awaitRates(project = "0", subscription = "0", total = "0")
        assertTrue(overview(paused).hasRecords)
        assertEquals(0L, overview(paused).activeSubscriptionCount)
        observation.set(observation.get().plusDays(1))
        val pausedFacts = checkNotNull(subscriptions.loadSubscriptionDetail(command.subscription.id)).facts()
        val resume = SubscriptionStateChangeEntity(id(), command.subscription.id, observation.get(),
            SubscriptionOperationState.ENABLED, operationId = id())
        assertTrue(subscriptions.setState(SetSubscriptionStateCommand(resume.operationId, pausedFacts, resume,
            SubscriptionStateAction.RESUME)) is SubscriptionLifecycleResult.Saved)
        val resumed = awaitRates(project = "0", subscription = "5", total = "5")
        assertEquals(1L, overview(resumed).activeSubscriptionCount)
        assertEquals(1L, resumed.subscriptionCount)
        assertEquals(1L, generation.database.subscriptionDao().countRenewals(command.subscription.id))
        assertEquals(before.renewals, checkNotNull(subscriptions.loadSubscriptionDetail(command.subscription.id)).renewals)
    }

    @Test fun explicit_retry_recomputes_project_on_new_business_day_without_changing_facts(): Unit = runBlocking {
        val initial = awaitReady(holder)
        assertTrue(initial.repository.createProject(projectCommand(30_000)) is MutationResult.Success)
        awaitRates(project = "100", subscription = "0", total = "100")
        val before = generation.backupSnapshotSource().capture()
        observation.set(observation.get().plusDays(1))
        retry(holder)
        val refreshed = awaitRates(project = "75", subscription = "0", total = "75")
        assertEquals(observation.get(), overview(refreshed).asOf)
        assertEquals(before, generation.backupSnapshotSource().capture())
        assertEquals(overview(refreshed), checkNotNull(refreshed.reportRepository).loadOverview())
    }

    @Test fun restored_owner_replaces_modified_home_and_recomputes_backup_facts_on_current_day(): Unit = runBlocking {
        val initial = awaitReady(holder)
        val project = projectCommand(30_000)
        assertTrue(initial.repository.createProject(project) is MutationResult.Success)
        assertTrue(initial.subscriptionRepository.createWithRenewal(subscriptionCommand()) is SubscriptionMutationResult.Success)
        val backedUp = awaitRates(project = "100", subscription = "5", total = "105")
        val original = generation.backupSnapshotSource().capture()
        val archive = File(root, "synthetic-core.lithicbackup")
        assertTrue(BackupExportService(context, generation).export(target(archive)) is BackupExportResult.Success)
        val operation = id()
        val extra = ProjectMoneyEventEntity(id(), project.project.id, MoneyDirection.OUTLAY, 30_000,
            observation.get(), role = ProjectMoneyEventRole.REGULAR, operationId = operation)
        assertTrue(backedUp.repository.appendMoneyEvent(AddProjectMoneyEventCommand(operation, extra)) is MutationResult.Success)
        awaitRates(project = "200", subscription = "5", total = "205")
        observation.set(observation.get().plusDays(1))
        val restore = RestoreCoordinator(context, owner)
        val preview = restore.prepare { FileInputStream(archive) }
        assertEquals(RestoreResult.Success, restore.confirm(preview.token))
        val restoredGeneration = owner.openStorage()
        assertNotEquals(generation.id, restoredGeneration.id)
        val restored = awaitReady(holder) { it.generationId == restoredGeneration.id }
        decimal("75", overview(restored).projectRate)
        decimal("5", overview(restored).subscriptionRate)
        decimal("80", overview(restored).totalRate)
        assertEquals(observation.get(), overview(restored).asOf)
        assertEquals(original, restoredGeneration.backupSnapshotSource().capture())
        assertEquals(overview(restored), checkNotNull(restored.reportRepository).loadOverview())
        assertFalse(generation.database.isOpen)
        assertNull(restored.readError)
    }

    @Test fun read_failure_preserves_previous_nonzero_overview_instead_of_showing_zero(): Unit = runBlocking {
        val initial = awaitReady(holder)
        assertTrue(initial.repository.createProject(projectCommand(30_000)) is MutationResult.Success)
        val before = awaitRates(project = "100", subscription = "0", total = "100")
        withContext(Dispatchers.IO) { generation.database.close() }
        retry(holder)
        val failedRead = holder.state.value as LedgerUiState.Ready
        assertNotNull(failedRead.readError)
        assertSame(before.overview, failedRead.overview)
        decimal("100", overview(failedRead).totalRate)
        assertEquals(1L, failedRead.projectCount)
        assertEquals(generation.id, failedRead.generationId)
    }

    @Test fun late_old_generation_retry_cannot_replace_new_home_amount_or_report_repository(): Unit = runBlocking {
        val initial = awaitReady(holder)
        assertTrue(initial.repository.createProject(projectCommand(30_000)) is MutationResult.Success)
        awaitRates(project = "100", subscription = "0", total = "100")
        val secondOwner = newOwner("second")
        val secondGeneration = secondOwner.openStorage()
        val secondRepository = ProjectRepository(secondGeneration.database, secondGeneration.coordinator, clock)
        assertTrue(secondRepository.createProject(projectCommand(60_000)) is MutationResult.Success)
        val states = MutableStateFlow<StorageInitializationState>(StorageInitializationState.Ready(generation))
        val delayedHolder = withContext(Dispatchers.Main) {
            LedgerUiStateHolder(states) { clock }.also { viewModels.put("delayed-home", it) }
        }
        val oldReady = awaitReady(delayedHolder)
        decimal("100", overview(oldReady).totalRate)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val blocker = launch(Dispatchers.IO) {
            generation.coordinator.withSnapshot { entered.complete(Unit); release.await() }
        }
        try {
            withTimeout(10_000) { entered.await() }
            val pending = withContext(Dispatchers.Main) { checkNotNull(delayedHolder.retryRead()) }
            states.value = checkNotNull(secondOwner.storageState.value as? StorageInitializationState.Ready)
            val newReady = awaitReady(delayedHolder) { it.generationId == secondGeneration.id }
            decimal("200", overview(newReady).totalRate)
            assertFalse("The deliberately blocked old read must still be pending", pending.isCompleted)
            release.complete(Unit)
            withTimeout(10_000) { blocker.join(); pending.join() }
            val observed = delayedHolder.state.value as LedgerUiState.Ready
            assertEquals(secondGeneration.id, observed.generationId)
            assertSame(newReady.reportRepository, observed.reportRepository)
            assertSame(newReady.overview, observed.overview)
            decimal("200", overview(observed).totalRate)
        } finally {
            release.complete(Unit)
            withTimeout(10_000) { blocker.join() }
        }
    }

    private suspend fun newOwner(name: String): AppContainer {
        val store = DataGenerationStore(context, File(root, name))
        return AppContainer(store, CoroutineScope(SupervisorJob() + Dispatchers.IO)).also {
            owners += it
            it.start(zone)
            it.openStorage()
        }
    }

    private suspend fun awaitReady(
        target: LedgerUiStateHolder,
        predicate: (LedgerUiState.Ready) -> Boolean = { true },
    ): LedgerUiState.Ready = withTimeout(10_000) {
        target.state.first { it is LedgerUiState.Ready && it.overview != null && predicate(it) } as LedgerUiState.Ready
    }

    private suspend fun awaitRates(project: String, subscription: String, total: String): LedgerUiState.Ready =
        awaitReady(holder) {
            val overview = overview(it)
            overview.projectRate.compareTo(BigDecimal(project)) == 0 &&
                overview.subscriptionRate.compareTo(BigDecimal(subscription)) == 0 &&
                overview.totalRate.compareTo(BigDecimal(total)) == 0
        }

    private suspend fun retry(target: LedgerUiStateHolder) {
        val request = withContext(Dispatchers.Main) { checkNotNull(target.retryRead()) }
        withTimeout(10_000) { request.join() }
    }

    private fun overview(ready: LedgerUiState.Ready): OverviewResult = checkNotNull(ready.overview)

    private fun projectCommand(cents: Long): CreateProjectCommand {
        val project = LongTermProjectEntity(id(), "M13合成项目", LithicDatabase.PROJECT_FALLBACK_ID, start,
            createdAt = createdAt, updatedAt = createdAt)
        val operation = id()
        val initial = ProjectMoneyEventEntity(id(), project.id, MoneyDirection.OUTLAY, cents, start,
            role = ProjectMoneyEventRole.INITIAL, operationId = operation)
        return CreateProjectCommand(operation, project, initial)
    }

    private fun subscriptionCommand(): CreateSubscriptionCommand {
        val subscription = SubscriptionEntity(id(), "M13合成服务", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            createdAt = createdAt, updatedAt = createdAt)
        val initial = renewal(subscription.id, 15_000, start, start.plusMonths(1), start)
        return CreateSubscriptionCommand(initial.operationId, subscription, initial)
    }

    private fun renewal(subscriptionId: String, cents: Long, from: LocalDate, until: LocalDate, paid: LocalDate) =
        SubscriptionRenewalEntity(id(), subscriptionId, paid, from, until, until, cents,
            BillingCycle.MONTHLY, BillingCycle.MONTHLY.standardDays, id())

    private fun target(file: File) = object : BackupTarget {
        override fun openWrite(): OutputStream = FileOutputStream(file)
        override fun openRead(): InputStream = FileInputStream(file)
        override fun deletePartial(): Boolean = !file.exists() || file.delete()
    }

    private fun decimal(expected: String, actual: BigDecimal) = assertEquals(0, BigDecimal(expected).compareTo(actual))
    private fun id(): String = UUID.randomUUID().toString()
}
