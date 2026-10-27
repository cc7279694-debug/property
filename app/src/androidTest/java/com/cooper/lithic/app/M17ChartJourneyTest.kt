package com.cooper.lithic.app

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.design.AmountFormatter
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.backup.*
import com.cooper.lithic.data.local.*
import com.cooper.lithic.data.repository.*
import com.cooper.lithic.domain.subscription.*
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real LithicApp/Nav3 and real Room, isolated ComponentActivity/plain Application only.
 * Screenshots use a LocalDensity 360dp canvas; 200% is not a system-font or MainActivity claim. */
@RunWith(AndroidJUnit4::class)
class M17ChartJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val today = LocalDate.of(2026, 10, 21)
    private val day = AtomicReference(today)
    private val clock = BusinessClock { day.get() }
    private val time = Instant.parse("2026-10-21T04:00:00Z")
    private val runId = UUID.randomUUID().toString()
    private val showing = mutableStateOf(true)
    private val fontScale = mutableStateOf(1f)
    private val models = ViewModelStore()
    private lateinit var root: File
    private lateinit var owner: AppContainer
    private lateinit var holder: LedgerUiStateHolder
    private val ready get() = holder.state.value as LedgerUiState.Ready

    @Before fun before(): Unit = runBlocking {
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m17-ui-$runId").canonicalFile
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.mkdir())
        owner = AppContainer(DataGenerationStore(context, File(root, "ledger")), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        owner.start(ZoneId.of("Asia/Shanghai")); owner.openStorage()
        withContext(Dispatchers.Main) {
            holder = LedgerUiStateHolder(owner.storageState) { clock }.also { models.put("m17-ledger", it) }
        }
        withTimeout(15_000) { holder.state.first { it is LedgerUiState.Ready } }
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize().testTag("m17-viewport")) {
                val density = constraints.maxWidth / 360f
                val height = minOf(720f, constraints.maxHeight / density).dp
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale.value)) {
                    LithicTheme(darkTheme = false) {
                        Box(Modifier.requiredSize(360.dp, height).testTag("m17-canvas")) {
                            Surface(Modifier.fillMaxSize()) {
                                val ledger by holder.state.collectAsState()
                                if (showing.value) LithicApp(ledger, {}, onRetryLedger = { holder.retryRead() })
                            }
                        }
                    }
                }
            }
        }
        waitNode("home-screen")
    }

    @After fun after(): Unit = runBlocking {
        compose.runOnIdle { showing.value = false; models.clear(); compose.activity.viewModelStore.clear() }
        compose.waitForIdle()
        if (::owner.isInitialized) owner.close()
        if (::root.isInitialized && root.exists()) {
            check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.name == "m17-ui-$runId")
            check(root.deleteRecursively())
        }
    }

    @Test fun real_project_chart_events_negative_values_return_guard_and_foreground_day_refresh(): Unit = runBlocking {
        val generation = owner.openStorage()
        val repo = ready.repository
        val project = seedProject(repo, "合成随身电脑·维修与收回", today.minusDays(99), 10_000)
        val repair = money(project.id, MoneyDirection.OUTLAY, 30_000, today.minusDays(60), "真实合成维修")
        val refundA = money(project.id, MoneyDirection.RECOVERY, 30_000, today.minusDays(4), "同日收回甲")
        val refundB = money(project.id, MoneyDirection.RECOVERY, 20_000, today.minusDays(4), "同日收回乙")
        for (event in listOf(repair, refundA, refundB)) {
            assertTrue(repo.appendMoneyEvent(AddProjectMoneyEventCommand(event.operationId, event)) is MutationResult.Success)
        }
        val before = generation.backupSnapshotSource().capture()
        openProject(project.id)
        val origin = textOf(compose.onNodeWithTag("project-detail-back").fetchSemanticsNode())
        scrollClick("project-detail-content", "project-cost-history")
        waitNode(PROJECT); waitNode("chart-selected-date")
        scrollTo(PROJECT, "chart-selected-value")
        waitText("chart-selected-value", "−")
        capture("01-project-negative-history")
        scrollTo(PROJECT, "cost-chart-canvas"); capture("08-project-complete-curve")
        selectDate(PROJECT, repair.occurredOn.minusDays(1))
        val exactBeforeRepair = checkNotNull(ItemTimelineRepository(ready.repository, ready.subscriptionRepository, clock)
            .loadProject(project.id, today))
        val expected = com.cooper.lithic.domain.report.ProjectTimelineEngine.point(exactBeforeRepair.snapshot, today, repair.occurredOn.minusDays(1))
        waitText("chart-selected-value", AmountFormatter.formatDailyRate(expected.cost.dailyRate))
        selectDate(PROJECT, refundA.occurredOn)
        scrollClick(PROJECT, "chart-events-toggle")
        scrollClick(PROJECT, "chart-show-selected-events")
        scrollTo(PROJECT, "chart-event-${refundA.id}")
        waitText("chart-event-${refundA.id}", "同日收回甲")
        scrollTo(PROJECT, "chart-event-${refundB.id}")
        waitText("chart-event-${refundB.id}", "同日收回乙")
        capture("02-project-same-day-events")
        compose.runOnIdle { fontScale.value = 2f }
        scrollTo(PROJECT, "chart-date-input")
        compose.onNodeWithTag("chart-date-input").assertIsDisplayed().assertIsEnabled()
        capture("03-project-360dp-font200-date-entry")
        scrollTo(PROJECT, "chart-back")
        compose.onNodeWithTag("chart-back").assertIsDisplayed().assertIsEnabled()
        capture("04-project-360dp-font200-back")
        click("chart-back")
        waitText("project-detail-name", project.name)
        compose.onNodeWithTag("project-detail-back").assertTextEquals(origin)
        compose.runOnIdle { fontScale.value = 1f }
        scrollClick("project-detail-content", "project-edit")
        waitNode("project-editor-screen")
        compose.onNodeWithTag("project-name").performTextReplacement("未保存图表旅程草稿")
        click("tab-review"); waitNode("discard-dialog"); click("discard-keep")
        compose.onNodeWithTag("project-name").assertTextContains("未保存图表旅程草稿")
        click("project-editor-back"); waitNode("discard-dialog"); click("discard-confirm")
        waitText("project-detail-name", project.name)
        assertEquals(before, generation.backupSnapshotSource().capture())
        scrollClick("project-detail-content", "project-cost-history")
        waitNode(PROJECT); waitNode("chart-selected-date")
        // An actual committed correction signal refreshes an already open graph, with no route rebuild.
        val followup = money(project.id, MoneyDirection.OUTLAY, 10_000, today, "合成后续真实投入")
        assertTrue(repo.appendMoneyEvent(AddProjectMoneyEventCommand(followup.operationId, followup)) is MutationResult.Success)
        selectDate(PROJECT, today)
        scrollTo(PROJECT, "chart-selected-value")
        waitText("chart-selected-value", AmountFormatter.formatDailyRate(BigDecimal.ZERO))
        assertEquals(0, checkNotNull(repo.loadProjectDetail(project.id)?.cost?.dailyRate).signum())
        day.set(today.plusDays(1))
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(15_000) { compose.onAllNodesWithText("结果截至 ${today.plusDays(1)}").fetchSemanticsNodes().isNotEmpty() }
        selectDate(PROJECT, today.plusDays(1))
        scrollTo(PROJECT, "chart-selected-date")
        waitText("chart-selected-date", today.plusDays(1).toString())
        assertEquals(5, repo.loadProjectDetail(project.id)!!.events.size)
    }

    @Test fun subscription_steps_pause_gap_and_post_end_adjustments_survive_format2_restore(): Unit = runBlocking {
        val generation = owner.openStorage()
        val fixture = insertSubscription(generation)
        val ended = seedProject(ready.repository, "合成已结束相机·Q02", LocalDate.of(2026, 1, 1), 100_000)
        generation.coordinator.withWrite {
            generation.database.withTransaction {
                generation.database.openHelper.writableDatabase.execSQL(
                    "UPDATE long_term_projects SET endedOn=?, endReason=? WHERE id=?",
                    arrayOf<Any?>(LocalDate.of(2026, 1, 10).toEpochDay(), "合成结束", ended.id))
                generation.database.projectDao().insertEvent(money(ended.id, MoneyDirection.RECOVERY, 20_000,
                    LocalDate.of(2026, 1, 20), "结束后真实结算"))
            }
        }
        openSubscription(fixture.id)
        scrollClick("subscription-detail-content", "subscription-price-history")
        waitNode(SUBSCRIPTION); waitNode("chart-selected-date")
        selectDate(SUBSCRIPTION, LocalDate.of(2026, 1, 20))
        scrollTo(SUBSCRIPTION, "chart-selected-value"); waitText("chart-selected-value", "5.00")
        selectDate(SUBSCRIPTION, LocalDate.of(2026, 2, 1))
        scrollTo(SUBSCRIPTION, "chart-selected-value"); waitText("chart-selected-value", "6.00")
        capture("05-subscription-effective-price-step")
        scrollTo(SUBSCRIPTION, "cost-chart-canvas"); capture("09-subscription-complete-step")
        selectDate(SUBSCRIPTION, LocalDate.of(2026, 2, 10))
        scrollTo(SUBSCRIPTION, "chart-selected-status"); waitText("chart-selected-status", "暂停")
        waitText("chart-selected-value", AmountFormatter.formatDailyRate(BigDecimal.ZERO))
        assertEquals(0, com.cooper.lithic.domain.report.SubscriptionTimelineEngine.point(
            checkNotNull(ItemTimelineRepository(ready.repository, ready.subscriptionRepository, clock)
                .loadSubscription(fixture.id, today)).snapshot, today, LocalDate.of(2026, 2, 10)).cost.currentRate.signum())
        capture("06-subscription-paused-zero")
        selectDate(SUBSCRIPTION, LocalDate.of(2026, 3, 2))
        scrollTo(SUBSCRIPTION, "chart-selected-status"); waitText("chart-selected-status", "到期")
        val reader = ItemTimelineRepository(ready.repository, ready.subscriptionRepository, clock)
        val oldChart = checkNotNull(reader.loadSubscription(fixture.id, today))
        assertTrue(oldChart.timeline.points.none { it.date.isAfter(today) })
        val expected = generation.backupSnapshotSource().capture()
        val archive = File(root, "m17-controlled-format2.lithicbackup")
        assertTrue(BackupExportService(context, generation).export(RestoreSyntheticFixture.target(archive)) is BackupExportResult.Success)
        val restorer = RestoreCoordinator(context, owner)
        val preview = restorer.prepare { archive.inputStream() }
        assertEquals(expected.data.recordCounts(), preview.backupCounts)
        assertEquals(RestoreResult.Success, restorer.confirm(preview.token))
        val restored = owner.openStorage()
        assertNotEquals(generation.id, restored.id)
        withTimeout(15_000) { holder.state.first { it is LedgerUiState.Ready && it.generationId == restored.id } }
        waitNode("home-screen")
        assertEquals(expected, restored.backupSnapshotSource().capture())
        assertTrue(runCatching { reader.loadSubscription(fixture.id) }.isFailure)
        assertEquals(oldChart, ItemTimelineRepository(ready.repository, ready.subscriptionRepository, clock).loadSubscription(fixture.id, today))
        openSubscription(fixture.id)
        scrollClick("subscription-detail-content", "subscription-price-history")
        waitNode(SUBSCRIPTION); waitNode("chart-selected-date")
        selectDate(SUBSCRIPTION, LocalDate.of(2026, 2, 12))
        scrollTo(SUBSCRIPTION, "chart-selected-status"); waitText("chart-selected-status", "订阅中")
        scrollTo(SUBSCRIPTION, "chart-selected-value"); waitText("chart-selected-value", "6.00")
        val beforeBrowsing = restored.backupSnapshotSource().capture()
        click("tab-review"); waitNode("review-screen")
        scrollClick("review-screen", "review-project-history"); waitNode("project-history-screen")
        scrollClick("project-history-content", "project-history-row-${ended.id}")
        waitText("project-detail-name", ended.name)
        val back = textOf(compose.onNodeWithTag("project-detail-back").fetchSemanticsNode())
        scrollClick("project-detail-content", "project-cost-history"); waitNode(PROJECT); waitNode("chart-selected-date")
        selectDate(PROJECT, LocalDate.of(2026, 1, 20))
        scrollTo(PROJECT, "chart-selected-value"); waitText("chart-selected-value", "80.00")
        scrollTo(PROJECT, "chart-adjustment-2026-01-20")
        capture("07-ended-project-post-settlement")
        scrollClick(PROJECT, "chart-back"); waitText("project-detail-name", ended.name)
        compose.onNodeWithTag("project-detail-back").assertTextEquals(back)
        assertEquals(beforeBrowsing, restored.backupSnapshotSource().capture())
    }

    private suspend fun seedProject(repository: ProjectRepository, name: String, start: LocalDate, cents: Long): LongTermProjectEntity {
        val project = LongTermProjectEntity(id(), name, LithicDatabase.PROJECT_FALLBACK_ID, start, createdAt = time, updatedAt = time)
        val command = id()
        val event = ProjectMoneyEventEntity(id(), project.id, MoneyDirection.OUTLAY, cents, start,
            role = ProjectMoneyEventRole.INITIAL, operationId = command)
        assertTrue(repository.createProject(CreateProjectCommand(command, project, event)) is MutationResult.Success)
        return project
    }

    private suspend fun insertSubscription(generation: ManagedGeneration): SubscriptionEntity {
        val subscription = SubscriptionEntity(id(), "合成工具·真实调价与暂停", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            createdAt = time, updatedAt = time)
        generation.coordinator.withWrite {
            generation.database.withTransaction {
                val dao = generation.database.subscriptionDao()
                dao.insertSubscription(subscription)
                fun d(value: String) = LocalDate.parse(value)
                suspend fun renewal(paid: String, from: String, until: String, cents: Long, cycle: BillingCycle, days: Int) {
                    dao.insertRenewal(SubscriptionRenewalEntity(id(), subscription.id, d(paid), d(from), d(until), d(until), cents, cycle, days, id()))
                }
                renewal("2026-01-01", "2026-01-01", "2026-02-01", 15_000, BillingCycle.MONTHLY, 30)
                renewal("2026-01-20", "2026-02-01", "2026-03-01", 18_000, BillingCycle.MONTHLY, 30)
                renewal("2026-03-04", "2026-03-04", "2026-04-04", 21_000, BillingCycle.MONTHLY, 30)
                renewal("2026-04-04", "2026-04-04", "2026-05-04", 24_000, BillingCycle.MONTHLY, 30)
                renewal("2026-04-20", "2026-05-04", "2027-05-04", 365_000, BillingCycle.YEARLY, 365)
                for ((date, state) in listOf("2026-02-10" to SubscriptionOperationState.PAUSED,
                    "2026-02-12" to SubscriptionOperationState.ENABLED,
                    "2026-04-08" to SubscriptionOperationState.STOPPED,
                    "2026-04-10" to SubscriptionOperationState.ENABLED)) {
                    dao.insertStateChange(SubscriptionStateChangeEntity(id(), subscription.id, d(date), state, operationId = id()))
                }
                dao.insertStateChange(SubscriptionStateChangeEntity(id(), subscription.id, today.plusDays(2),
                    SubscriptionOperationState.STOPPED, "未来计划不画预测", operationId = id()))
            }
        }
        return subscription
    }
    private fun money(project: String, direction: MoneyDirection, cents: Long, date: LocalDate, note: String) =
        ProjectMoneyEventEntity(id(), project, direction, cents, date, note, role = ProjectMoneyEventRole.REGULAR, operationId = id())
    private fun openProject(id: String) {
        click("tab-now"); waitNode("home-screen")
        compose.onNodeWithTag("open-project-list").performScrollTo(); click("open-project-list"); waitNode("project-list-content")
        scrollClick("project-list-content", "project-row-$id"); waitNode("project-detail-name")
    }
    private fun openSubscription(id: String) {
        click("tab-now"); waitNode("home-screen")
        compose.onNodeWithTag("open-subscription-list").performScrollTo(); click("open-subscription-list"); waitNode("subscription-list-content")
        scrollClick("subscription-list-content", "subscription-row-$id"); waitNode("subscription-detail-name")
    }
    private fun selectDate(container: String, date: LocalDate) {
        scrollTo(container, "chart-date-toggle")
        if (textOf(compose.onNodeWithTag("chart-date-toggle").fetchSemanticsNode()).contains("指定日期")) {
            click("chart-date-toggle")
        }
        scrollTo(container, "chart-date-input")
        compose.onNodeWithTag("chart-date-input").performTextReplacement(date.toString())
        Espresso.closeSoftKeyboard(); scrollClick(container, "chart-date-apply")
        scrollTo(container, "chart-selected-date"); waitText("chart-selected-date", date.toString())
    }
    private fun click(tag: String) {
        Espresso.closeSoftKeyboard(); compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled().performClick()
    }
    private fun scrollTo(container: String, tag: String) = compose.onNodeWithTag(container).performScrollToNode(hasTestTag(tag))
    private fun scrollClick(container: String, tag: String) { scrollTo(container, tag); click(tag) }
    private fun waitNode(tag: String) = compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun waitText(tag: String, value: String) = compose.waitUntil(15_000) {
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().any { textOf(it).contains(value) }
    }
    private fun textOf(node: SemanticsNode): String = buildString {
        if (node.config.contains(SemanticsProperties.Text)) append(node.config[SemanticsProperties.Text].joinToString(" ") { it.text })
        node.children.forEach { append(' '); append(textOf(it)) }
    }
    private fun capture(name: String) {
        compose.waitForIdle(); assertEquals(ComponentActivity::class.java, compose.activity.javaClass)
        val files = InstrumentationRegistry.getInstrumentation().targetContext.filesDir.canonicalFile
        val directory = File(files, "m17-evidence").canonicalFile
        check(directory.parentFile == files && (directory.isDirectory || directory.mkdir()))
        val output = File(directory, "$name-$runId.png")
        check(output.createNewFile())
        val bitmap = compose.onNodeWithTag("m17-canvas").captureToImage().asAndroidBitmap()
        FileOutputStream(output).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)); it.fd.sync() }
        check(output.length() > 0)
        println("M17_COMPONENT_ACTIVITY_SCREENSHOT=${output.absolutePath}")
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply { putString("M17_COMPONENT_ACTIVITY_SCREENSHOT", output.absolutePath) })
    }
    private fun id() = UUID.randomUUID().toString()
    companion object {
        private const val PROJECT = "project-cost-chart-screen"
        private const val SUBSCRIPTION = "subscription-price-chart-screen"
    }
}
