package com.cooper.lithic.app

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.*
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.*
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual LithicApp/Nav3, isolated ComponentActivity/plain Application, synthetic ledger only.
 * LocalDensity provides a 360dp/200% canvas; this does not change system font/density or open MainActivity. */
@RunWith(AndroidJUnit4::class)
class M16ScreenshotsJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val day = LocalDate.of(2026, 10, 21)
    private val clock = FixedBusinessClock(day)
    private val time = Instant.parse("2026-10-21T04:00:00Z")
    private val zone = ZoneId.of("Asia/Shanghai")
    private val runId = UUID.randomUUID().toString()
    private val models = ViewModelStore()
    private val showing = mutableStateOf(true)
    private val fontScale = mutableStateOf(1f)
    private lateinit var root: File
    private lateinit var owner: AppContainer
    private lateinit var holder: LedgerUiStateHolder
    private val ready get() = holder.state.value as LedgerUiState.Ready

    @Before fun before(): Unit = runBlocking {
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m16-ui-$runId").canonicalFile
        assertEquals(context.noBackupFilesDir.canonicalFile, root.parentFile)
        check(root.mkdir())
        val storage = File(root, "target-ledger").apply { check(mkdir()) }
        owner = AppContainer(DataGenerationStore(context, storage), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        owner.start(zone); owner.openStorage()
        withContext(Dispatchers.Main) {
            holder = LedgerUiStateHolder(owner.storageState) { clock }.also { models.put("m16-ledger", it) }
        }
        withTimeout(15_000) { holder.state.first { it is LedgerUiState.Ready } }
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize().testTag("m16-viewport")) {
                val density = constraints.maxWidth / 360f
                val height = minOf(720f, constraints.maxHeight / density).dp
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale.value)) {
                    LithicTheme(darkTheme = false) {
                        Box(Modifier.requiredSize(360.dp, height).testTag("m16-canvas")) {
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
            assertEquals(context.noBackupFilesDir.canonicalFile, root.canonicalFile.parentFile)
            assertEquals("m16-ui-$runId", root.name)
            check(root.deleteRecursively())
        }
    }

    @Test fun chain_comparison_navigation_and_unsaved_guard_with_large_font(): Unit = runBlocking {
        val generation = owner.openStorage()
        val repository = ProjectRepository(generation.database, generation.coordinator, clock)
        val replacement = ReplacementRepository(generation.database, generation.coordinator, clock)
        val a = seed(repository, "第一代·陪伴八年的电脑", day.minusYears(8), 800_000)
        val b = seed(repository, "第二代·工作电脑", day.minusYears(3), 600_000)
        val c = seed(repository, "第三代·随身电脑", day.minusMonths(2), 400_000)
        assertTrue(replacement.link(LinkReplacementCommand(id(), a, b, replacement.loadLinks(), time)) is ReplacementMutationResult.Linked)
        assertTrue(replacement.link(LinkReplacementCommand(id(), b, c, replacement.loadLinks(), time)) is ReplacementMutationResult.Linked)
        val before = generation.backupSnapshotSource().capture()
        openProject(b.id)
        scrollClick("project-detail-content", "project-replacement")
        waitNode("replacement-comparison-screen")
        waitEnabled("replacement-link")
        compose.onNodeWithTag("replacement-comparison-screen").performScrollToNode(hasTestTag("replacement-item-${b.id}"))
        capture("01-chain-vertical-comparison")
        compose.runOnIdle { fontScale.value = 2f }
        compose.onNodeWithTag("replacement-comparison-screen").performScrollToNode(hasTestTag("replacement-open-${c.id}"))
        compose.onNodeWithTag("replacement-open-${c.id}").assertIsDisplayed().assertIsEnabled()
        capture("02-chain-360dp-font200")
        click("replacement-open-${c.id}")
        waitNode("project-detail-name")
        compose.onNodeWithTag("project-detail-name").assertTextEquals(c.name)
        scrollClick("project-detail-content", "project-detail-back")
        waitNode("replacement-comparison-screen")
        compose.runOnIdle { fontScale.value = 1f }
        compose.onNodeWithTag("replacement-comparison-screen").performScrollToNode(hasTestTag("replacement-open-${a.id}"))
        click("replacement-open-${a.id}")
        waitNode("project-detail-name")
        scrollClick("project-detail-content", "project-edit")
        waitNode("project-editor-screen")
        compose.onNodeWithTag("project-name").performTextReplacement("尚未保存")
        click("tab-review")
        waitNode("discard-dialog")
        click("discard-keep")
        compose.onNodeWithTag("project-name").assertTextContains("尚未保存")
        click("project-editor-back")
        waitNode("discard-dialog"); click("discard-confirm")
        waitNode("project-detail-name")
        compose.onNodeWithTag("project-detail-name").assertTextEquals(a.name)
        scrollClick("project-detail-content", "project-detail-back")
        waitNode("replacement-comparison-screen")
        assertEquals(before, generation.backupSnapshotSource().capture())
    }

    @Test fun trade_in_confirmation_saved_facts_home_review_and_restored_generation_refresh(): Unit = runBlocking {
        val generation = owner.openStorage()
        val repository = ProjectRepository(generation.database, generation.coordinator, clock)
        val old = seed(repository, "合成旧手机", day.minusYears(3), 900_000)
        openProject(old.id)
        scrollClick("project-detail-content", "project-trade-in")
        waitNode("trade-in-screen")
        waitEnabled("trade-in-name")
        compose.onNodeWithTag("trade-in-name").performTextReplacement("合成新手机")
        compose.onNodeWithTag("trade-in-cash").performScrollTo().performTextReplacement("5000")
        compose.onNodeWithTag("trade-in-credit").performScrollTo().performTextReplacement("3000")
        compose.onNodeWithTag("trade-in-preview").performScrollTo(); click("trade-in-preview")
        compose.onNodeWithTag("trade-in-confirm-acquisition").performScrollTo().assertIsDisplayed()
        capture("03-trade-in-confirmation")
        compose.runOnIdle { fontScale.value = 2f }
        compose.onNodeWithTag("trade-in-save").performScrollTo().assertIsDisplayed().assertIsEnabled()
        capture("04-trade-in-confirmation-360dp-font200")
        compose.runOnIdle { fontScale.value = 1f }
        compose.onNodeWithTag("trade-in-save").performScrollTo(); click("trade-in-save")
        waitNode("project-detail-name")
        compose.waitUntil(15_000) { compose.onNodeWithTag("project-detail-name").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].any { it.text == "合成新手机" } }
        val new = generation.database.projectDao().listProjects().single { it.name == "合成新手机" }
        val detail = repository.loadProjectDetail(new.id)!!
        val oldDetail = repository.loadProjectDetail(old.id)!!
        assertEquals(800_000L, detail.cost.grossOutlayCents)
        assertEquals(2, detail.events.size)
        assertEquals(300_000L, oldDetail.events.single { it.basis == MoneyBasis.TRADE_IN_CREDIT }.amountCents)
        assertEquals(500_000L, detail.cost.netCents + oldDetail.cost.netCents - 900_000L)
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-detail-outlay"))
        capture("05-trade-in-saved-cost-facts")
        val credit = detail.events.single { it.basis == MoneyBasis.TRADE_IN_CREDIT }
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-event-${credit.id}"))
        compose.onNodeWithTag("project-event-basis-${credit.id}", useUnmergedTree = true).assertTextEquals("非现金折抵")
        capture("06-trade-in-noncash-event")
        click("tab-now"); waitNode("home-screen")
        compose.waitUntil(15_000) { ready.overview?.projectRate?.compareTo(BigDecimal("8000")) == 0 }
        click("tab-review")
        compose.onNodeWithTag("review-screen").performScrollToNode(hasTestTag("review-long-term")); click("review-long-term")
        waitNode("review-ranks-screen")
        compose.onNodeWithTag("review-ranks-screen").performScrollToNode(hasTestTag("review-row-${new.id}"))
        click("review-row-${new.id}"); waitNode("project-detail-name")
        compose.onNodeWithTag("project-detail-name").assertTextEquals(new.name)
        scrollClick("project-detail-content", "project-replacement")
        waitNode("replacement-comparison-screen")
        waitEnabled("replacement-link")
        val expected = generation.backupSnapshotSource().capture()
        val archive = File(root, "m16-ui-controlled-file.lithicbackup")
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
        assertTrue(runCatching { repository.loadProjectDetail(new.id) }.isFailure)
        openProject(new.id)
        scrollClick("project-detail-content", "project-replacement")
        waitNode("replacement-comparison-screen")
        waitEnabled("replacement-link")
        compose.onNodeWithTag("replacement-comparison-screen").performScrollToNode(hasTestTag("replacement-open-${old.id}"))
        compose.onNodeWithTag("replacement-open-${old.id}").assertIsDisplayed()
        capture("07-restored-replacement-chain")
    }

    private suspend fun seed(repo: ProjectRepository, name: String, started: LocalDate, cents: Long): LongTermProjectEntity {
        val project = LongTermProjectEntity(id(), name, LithicDatabase.PROJECT_FALLBACK_ID, started, createdAt = time, updatedAt = time)
        val command = id()
        assertTrue(repo.createProject(CreateProjectCommand(command, project, ProjectMoneyEventEntity(id(), project.id,
            MoneyDirection.OUTLAY, cents, started, role = ProjectMoneyEventRole.INITIAL, operationId = command))) is MutationResult.Success)
        return project
    }
    private fun openProject(id: String) {
        if (compose.onAllNodesWithTag("home-screen").fetchSemanticsNodes().isEmpty()) { click("tab-now"); waitNode("home-screen") }
        compose.onNodeWithTag("open-project-list").performScrollTo(); click("open-project-list")
        waitNode("project-list-content")
        compose.onNodeWithTag("project-list-content").performScrollToNode(hasTestTag("project-row-$id")); click("project-row-$id")
        waitNode("project-detail-name")
    }
    private fun click(tag: String) { Espresso.closeSoftKeyboard(); compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled().performClick() }
    private fun scrollClick(container: String, tag: String) {
        compose.onNodeWithTag(container).performScrollToNode(hasTestTag(tag)); click(tag)
    }
    private fun waitNode(tag: String) { compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() } }
    private fun waitEnabled(tag: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().any {
                !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
            }
        }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        assertEquals(ComponentActivity::class.java, compose.activity.javaClass)
        val files = InstrumentationRegistry.getInstrumentation().targetContext.filesDir.canonicalFile
        val directory = File(files, "m16-evidence")
        assertEquals(files, directory.canonicalFile.parentFile)
        check(directory.isDirectory || directory.mkdir())
        val output = File(directory, "$name-$runId.png")
        check(output.createNewFile())
        val bitmap = compose.onNodeWithTag("m16-canvas").captureToImage().asAndroidBitmap()
        FileOutputStream(output).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)); it.fd.sync() }
        check(output.length() > 0)
        println("M16_COMPONENT_ACTIVITY_SCREENSHOT=${output.absolutePath}")
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply { putString("M16_COMPONENT_ACTIVITY_SCREENSHOT", output.absolutePath) })
    }
    private fun id() = UUID.randomUUID().toString()
}
