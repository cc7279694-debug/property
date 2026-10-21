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
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.design.AmountFormatter
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.local.*
import com.cooper.lithic.data.repository.*
import com.cooper.lithic.domain.report.CostTimelineEngine
import java.io.File
import java.io.FileOutputStream
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

/** Real Room and LithicApp/Nav3, in an isolated ComponentActivity/plain Application only.
 * LocalDensity stress and injected business day are not system-font or real-midnight evidence. */
@RunWith(AndroidJUnit4::class)
class M18TotalCostTrendJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val today = LocalDate.of(2026, 10, 21)
    private val day = AtomicReference(today)
    private val clock = BusinessClock { day.get() }
    private val timestamp = Instant.parse("2026-10-21T04:00:00Z")
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
        root = File(context.noBackupFilesDir, "m18-ui-$runId").canonicalFile
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.mkdir())
        owner = AppContainer(DataGenerationStore(context, File(root, "ledger")), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        owner.start(ZoneId.of("Asia/Shanghai")); owner.openStorage()
        withContext(Dispatchers.Main) {
            holder = LedgerUiStateHolder(owner.storageState) { clock }.also { models.put("m18-ledger", it) }
        }
        withTimeout(15_000) { holder.state.first { it is LedgerUiState.Ready } }
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = constraints.maxWidth / 360f
                val height = minOf(720f, constraints.maxHeight / density).dp
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale.value)) {
                    LithicTheme(darkTheme = false) {
                        Box(Modifier.requiredSize(360.dp, height).testTag("m18-canvas")) {
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
            check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.name == "m18-ui-$runId")
            check(root.deleteRecursively())
        }
    }

    @Test fun real_total_trend_twenty_year_spike_events_navigation_guards_and_refresh(): Unit = runBlocking {
        val generation = owner.openStorage()
        val old = seed("合成二十年记录", today.minusYears(20), 100_000)
        seed("合成新增当日尖峰", today.minusYears(9).plusDays(17), 90_000_000)
        val repo = ready.repository
        for (index in 0..50) {
            val event = ProjectMoneyEventEntity(id(), old.id, MoneyDirection.OUTLAY, 100, today,
                "同日说明${index.toString().padStart(2, '0')}", role = ProjectMoneyEventRole.REGULAR, operationId = id())
            assertTrue(repo.appendMoneyEvent(AddProjectMoneyEventCommand(event.operationId, event)) is MutationResult.Success)
        }
        val before = generation.backupSnapshotSource().capture()
        click("tab-review"); waitNode("review-screen")
        scrollClick("review-screen", "review-total-cost-trend")
        waitNode(SCREEN); waitNode("total-trend-selected-date")
        scrollTo(SCREEN, "total-trend-canvas")
        compose.onNodeWithTag("total-trend-canvas").assertIsDisplayed()
        capture("01-total-twenty-year-plot", "total-trend-canvas")
        scrollClick(SCREEN, "total-trend-breakdown")
        scrollTo(SCREEN, "total-trend-canvas")
        compose.onNodeWithTag("total-trend-canvas").assertContentDescriptionContains("共同日期和金额尺度")
        capture("02-component-same-scale-plot", "total-trend-canvas")
        val snapshot = ready.totalCostTimelineRepository!!.loadSnapshot(today)
        val point = CostTimelineEngine.evaluate(snapshot, listOf(today)).single()
        scrollTo(SCREEN, "total-trend-total")
        waitText("total-trend-total", AmountFormatter.formatDailyRate(point.totalRate))
        capture("03-exact-three-amounts")
        scrollTo(SCREEN, "total-trend-events-more"); click("total-trend-events-more")
        val event = snapshot.events.first { it.description.contains("同日说明50") }
        scrollTo(SCREEN, "total-trend-event-${event.id}")
        waitText("total-trend-event-${event.id}", "同日说明50")
        capture("04-same-day-event-after-fifty")
        scrollClick(SCREEN, "total-trend-open-${event.id}")
        waitText("project-detail-name", old.name)
        compose.onNodeWithTag("project-detail-back").assertTextEquals("返回成本趋势")
        scrollClick("project-detail-content", "project-edit")
        waitNode("project-editor-screen")
        compose.onNodeWithTag("project-name").performTextReplacement("未保存趋势来源草稿")
        click("tab-settings"); waitNode("discard-dialog"); click("discard-keep")
        compose.onNodeWithTag("project-name").assertTextContains("未保存趋势来源草稿", substring = true)
        click("project-editor-back"); waitNode("discard-dialog"); click("discard-confirm")
        waitText("project-detail-name", old.name); click("project-detail-back")
        waitNode(SCREEN)
        scrollTo(SCREEN, "total-trend-breakdown")
        compose.onNodeWithTag("total-trend-breakdown").assertTextContains("收起两类贡献", substring = true)
        assertEquals(before, generation.backupSnapshotSource().capture())

        compose.runOnIdle { fontScale.value = 2f }
        scrollTo(SCREEN, "total-trend-date-input")
        compose.onNodeWithTag("total-trend-date-input").assertIsDisplayed()
        capture("05-360dp-font200-date-entry")
        scrollTo(SCREEN, "total-trend-back")
        compose.onNodeWithTag("total-trend-back").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        capture("06-360dp-font200-back")
        compose.runOnIdle { fontScale.value = 1f }

        val extra = ProjectMoneyEventEntity(id(), old.id, MoneyDirection.RECOVERY, 20_000, today,
            "合成写后重算", role = ProjectMoneyEventRole.REGULAR, operationId = id())
        assertTrue(repo.appendMoneyEvent(AddProjectMoneyEventCommand(extra.operationId, extra)) is MutationResult.Success)
        val afterWrite = ready.totalCostTimelineRepository!!.loadSnapshot(today)
        val afterPoint = CostTimelineEngine.evaluate(afterWrite, listOf(today)).single()
        scrollTo(SCREEN, "total-trend-total")
        waitText("total-trend-total", AmountFormatter.formatDailyRate(afterPoint.totalRate))
        day.set(today.plusDays(1))
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(15_000) { compose.onAllNodesWithText("结果截至 ${today.plusDays(1)}").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(53, repo.loadProjectDetail(old.id)!!.events.size)
    }

    private suspend fun seed(name: String, startedOn: LocalDate, cents: Long): LongTermProjectEntity {
        val project = LongTermProjectEntity(id(), name, LithicDatabase.PROJECT_FALLBACK_ID, startedOn,
            createdAt = timestamp, updatedAt = timestamp)
        val command = id()
        val event = ProjectMoneyEventEntity(id(), project.id, MoneyDirection.OUTLAY, cents, startedOn,
            role = ProjectMoneyEventRole.INITIAL, operationId = command)
        assertTrue(ready.repository.createProject(CreateProjectCommand(command, project, event)) is MutationResult.Success)
        return project
    }
    private fun click(tag: String) {
        Espresso.closeSoftKeyboard(); compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled().performClick()
    }
    private fun scrollTo(container: String, tag: String) = compose.onNodeWithTag(container).performScrollToNode(hasTestTag(tag))
    private fun scrollClick(container: String, tag: String) { scrollTo(container, tag); click(tag) }
    private fun waitNode(tag: String) = compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun waitText(tag: String, text: String) = compose.waitUntil(15_000) {
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().any { textOf(it).contains(text) }
    }
    private fun textOf(node: SemanticsNode): String = buildString {
        if (node.config.contains(SemanticsProperties.Text)) append(node.config[SemanticsProperties.Text].joinToString(" ") { it.text })
        node.children.forEach { append(' '); append(textOf(it)) }
    }
    private fun capture(name: String, tag: String = "m18-canvas") {
        compose.waitForIdle(); assertEquals(ComponentActivity::class.java, compose.activity.javaClass)
        val files = InstrumentationRegistry.getInstrumentation().targetContext.filesDir.canonicalFile
        val directory = File(files, "m18-evidence").canonicalFile
        check(directory.parentFile == files && (directory.isDirectory || directory.mkdir()))
        val output = File(directory, "$name-$runId.png"); check(output.createNewFile())
        val bitmap = compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        FileOutputStream(output).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)); it.fd.sync() }
        check(output.length() > 0)
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply { putString("M18_COMPONENT_ACTIVITY_SCREENSHOT", output.absolutePath) })
    }
    private fun id() = UUID.randomUUID().toString()
    private companion object { const val SCREEN = "total-cost-trend-screen" }
}
