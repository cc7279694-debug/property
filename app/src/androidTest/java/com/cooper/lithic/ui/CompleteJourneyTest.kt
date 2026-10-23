package com.cooper.lithic.ui

import com.cooper.lithic.app.*

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextContains
import android.provider.Settings
import android.graphics.Bitmap
import android.os.Bundle
import androidx.room.withTransaction
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.backup.BackupExportService
import com.cooper.lithic.data.backup.BackupTarget
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.RestoreCoordinator
import com.cooper.lithic.feature.settings.BackupExportViewModel
import com.cooper.lithic.feature.settings.BackupRestoreViewModel
import com.cooper.lithic.feature.settings.RestoreUiPhase
import java.io.File
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import com.cooper.lithic.data.backup.RestoreReminderProtection
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Visible production navigation and writes in a plain ComponentActivity, never MainActivity.
 * Picker results use one synthetic owned file through the real ViewModels/services; this is
 * deliberately not a new system SAF or stable-signed internal-release verification.
 */
@RunWith(AndroidJUnit4::class)
class CompleteJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val zone = ZoneId.of("Asia/Shanghai")
    private val observation = AtomicReference(LocalDate.of(2026, 1, 10))
    private val clock = BusinessClock { observation.get() }
    private val models = ViewModelStore()
    private val showing = mutableStateOf(true)
    private lateinit var root: File
    private lateinit var owner: AppContainer
    private lateinit var holder: LedgerUiStateHolder
    private lateinit var exports: BackupExportViewModel
    private lateinit var restores: BackupRestoreViewModel
    private lateinit var archive: File

    @Before fun setUp(): Unit = runBlocking {
        assertEquals("M13IsolatedRunner must prevent the normal ledger Application from starting",
            Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m22-ui-journey-${UUID.randomUUID()}").canonicalFile
        check(root.mkdir())
        File(root, "owner.txt").writeText("M22 only $root")
        val storage = File(root, "owned-generation").apply { check(mkdir()) }
        owner = AppContainer(DataGenerationStore(context, storage, restoreClock = Clock.fixed(Instant.parse("2026-01-11T04:00:00Z"), zone)), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        owner.start(zone)
        owner.openStorage()
        archive = File(root, "synthetic-selected.lithicbackup")
        withContext(Dispatchers.Main) {
            holder = LedgerUiStateHolder(owner.storageState) { persistedZone -> assertEquals(zone, persistedZone); clock }
                .also { models.put("home", it) }
            exports = BackupExportViewModel(SavedStateHandle()) { target ->
                BackupExportService(context, owner.openStorage()).export(target)
            }.also { models.put("exports", it) }
            restores = BackupRestoreViewModel(SavedStateHandle(), RestoreCoordinator(context, owner))
                .also { models.put("restores", it) }
        }
        withTimeout(15_000) { holder.state.first { it is LedgerUiState.Ready } }
        compose.setContent {
            LithicTheme {
                val ledger by holder.state.collectAsState()
                val backup by exports.state.collectAsState()
                val restore by restores.state.collectAsState()
                if (showing.value) LithicApp(ledger, {}, onRetryLedger = { holder.retryRead() },
                    backupState = backup, onExportBackup = {
                        check(exports.beginSelection())
                        exports.selected(object : BackupTarget {
                            override fun openWrite() = archive.outputStream()
                            override fun openRead() = archive.inputStream()
                            override fun deletePartial() = !archive.exists() || archive.delete()
                        })
                    }, onCancelBackup = exports::cancelExport,
                    restoreState = restore, onRestoreBackup = {
                        check(restores.beginSelection())
                        restores.selected { archive.inputStream() }
                    }, onCancelRestore = restores::cancelValidation,
                    onConfirmRestore = restores::confirmOverwrite, onDiscardRestore = restores::discardPreview)
            }
        }
        waitNode("home-screen")
    }

    @After fun tearDown(): Unit = runBlocking {
        compose.runOnIdle {
            showing.value = false
            models.clear()
            compose.activity.viewModelStore.clear()
        }
        compose.waitForIdle()
        if (::owner.isInitialized) {
            val closing = (owner.storageState.value as? StorageInitializationState.Ready)?.generation
            owner.close()
            assertTrue(closing == null || !closing.database.isOpen)
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
                putString("M22_OWNER_CLOSED", closing?.id ?: "NO_READY_OWNER")
            })
        }
        if (::root.isInitialized) {
            assertEquals(context.noBackupFilesDir.canonicalFile, root.canonicalFile.parentFile)
            check(root.deleteRecursively())
        }
    }

    @Test fun runtime_offline_journey(): Unit = runBlocking {
        // Runtime device evidence is separately captured; fail if the real offline gate is missing.
        assertEquals(1, Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0))
        createHundredYuanProject()
        projectDetailClick("project-add-outlay")
        waitNode("money-editor-screen")
        replace("money-amount", "20")
        replace("money-occurred-on-input", "2026-01-05")
        click("money-save")
        waitNode("project-detail-screen")
        awaitRate("12")
        homeRate("¥12.00")

        click("open-project-list"); waitNode("project-list-screen")
        val projectId = owner.openStorage().database.projectDao().listProjects().single().id
        registerSyntheticCover(projectId)
        waitNode("project-row-$projectId")
        compose.onNodeWithTag("project-list-content").performScrollToNode(hasTestTag("project-row-$projectId"))
        click("project-row-$projectId"); waitNode("project-detail-screen")
        projectDetailClick("project-add-recovery"); waitNode("money-editor-screen")
        replace("money-amount", "10"); replace("money-occurred-on-input", "2026-01-07")
        click("money-save"); waitNode("project-detail-screen"); awaitRate("11")
        projectDetailClick("project-end"); waitNode("end-editor-screen")
        replace("end-ended-on-input", "2026-01-08")
        replace("end-reason", "合成项目已完成使用")
        click("end-save")
        waitNode("project-detail-screen"); awaitRate("0")
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-detail-daily"))
        compose.onNodeWithTag("project-detail-daily").assertTextContains("13.75", substring = true)
        capture("01-ended-project-result")
        projectDetailClick("project-cost-history"); waitNode("project-cost-chart-screen")
        queryDate("project-cost-chart-screen", "chart-date-input", "chart-date-apply", "2026-01-07")
        compose.onNodeWithTag("project-cost-chart-screen").performScrollToNode(hasTestTag("chart-selected-value"))
        compose.onNodeWithTag("chart-selected-value").assertTextContains("15.71", substring = true)
        capture("02-project-exact-date")
        click("chart-back"); waitNode("project-detail-screen"); homeRate("¥0.00")

        click("home-create-subscription")
        waitNode("subscription-paid-on-input")
        replace("subscription-name", "M22离线合成订阅")
        replace("subscription-amount", "150")
        click("subscription-cycle-MONTHLY")
        replace("subscription-paid-on-input", "2026-01-01")
        replace("subscription-effective-from-input", "2026-01-01")
        replace("subscription-paid-until-input", "2026-02-01")
        replace("subscription-reminder-due-input", "2026-01-29")
        click("subscription-confirm-payment")
        waitNode("subscription-detail-screen")
        awaitRate("5")
        homeRate("¥5.00")

        click("open-subscription-list"); waitNode("subscription-list-screen")
        val serviceId = owner.openStorage().database.subscriptionDao().validationSubscriptions(null, 2).single().id
        waitNode("subscription-row-$serviceId")
        compose.onNodeWithTag("subscription-list-content").performScrollToNode(hasTestTag("subscription-row-$serviceId"))
        click("subscription-row-$serviceId"); waitNode("subscription-detail-screen")
        subscriptionDetailClick("subscription-detail-renewal"); waitNode("renewal-editor-screen")
        replace("renewal-amount", "180"); replace("renewal-paid-on-input", "2026-01-10")
        replace("renewal-effective-from-input", "2026-02-01"); replace("renewal-paid-until-input", "2026-03-01")
        replace("renewal-reminder-due-input", "2026-02-26"); click("renewal-confirm-payment")
        waitNode("subscription-detail-screen"); awaitRate("5")
        subscriptionDetailClick("subscription-price-history"); waitNode("subscription-price-chart-screen")
        queryDate("subscription-price-chart-screen", "chart-date-input", "chart-date-apply", "2026-01-10")
        click("chart-back"); waitNode("subscription-detail-screen")
        // Bottom navigation is fixed outside the page's scroll container.
        compose.onNodeWithTag("tab-review").assertIsDisplayed().performClick()
        waitNode("review-screen"); click("review-total-cost-trend")
        waitNode("total-cost-trend-screen")
        queryDate("total-cost-trend-screen", "total-trend-date-input", "total-trend-date-apply", "2026-01-10")
        compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag("total-trend-total"))
        compose.onNodeWithTag("total-trend-total").assertTextContains("5.00", substring = true)
        capture("03-total-exact-date")
        val backedUp = owner.openStorage().backupSnapshotSource().capture()

        compose.onNodeWithTag("tab-settings").performClick()
        click("export-complete-backup")
        compose.waitUntil(20_000) { exports.state.value.exportedBytes?.let { it > 0L } == true }
        compose.onNodeWithTag("backup-export-feedback").performScrollTo().assertIsDisplayed()
        assertTrue(archive.isFile && archive.length() == exports.state.value.exportedBytes)
        val previousGeneration = owner.openStorage().id

        compose.onNodeWithTag("tab-now").performClick()
        click("open-subscription-list")
        waitNode("subscription-list-screen")
        val subscriptionId = owner.openStorage().database.subscriptionDao().validationSubscriptions(null, 2).single().id
        waitNode("subscription-list-count")
        waitNode("subscription-row-$subscriptionId")
        compose.onNodeWithTag("subscription-list-content").performScrollToNode(hasTestTag("subscription-row-$subscriptionId"))
        compose.onNodeWithTag("subscription-row-$subscriptionId").performClick()
        waitNode("subscription-detail-screen")
        subscriptionDetailClick("subscription-detail-pause")
        waitNode("subscription-state-confirm")
        click("subscription-state-confirm")
        awaitRate("0")
        homeRate("¥0.00")

        compose.onNodeWithTag("tab-settings").performClick()
        click("restore-complete-backup")
        compose.waitUntil(20_000) { restores.state.value.phase == RestoreUiPhase.PREVIEW }
        compose.onNodeWithTag("restore-count-subscription_state_changes").performScrollTo().assertIsDisplayed()
        assertEquals(1L, restores.state.value.preview!!.currentCounts["subscription_state_changes"])
        assertEquals(0L, restores.state.value.preview!!.backupCounts["subscription_state_changes"])
        assertEquals(previousGeneration, owner.openStorage().id)
        observation.set(LocalDate.of(2026, 1, 11))
        click("confirm-backup-restore")
        compose.waitUntil(30_000) {
            (holder.state.value as? LedgerUiState.Ready)?.let { ready ->
                ready.generationId != previousGeneration && ready.overview?.asOf == observation.get()
            } == true && restores.state.value.message == "恢复成功，已重新读取恢复后的本地记录。"
        }
        val restored = owner.openStorage()
        assertNotEquals(previousGeneration, restored.id)
        assertEquals(3, restored.database.projectDao().listEvents().size)
        assertEquals(2L, restored.database.subscriptionDao().countRenewals(subscriptionId))
        assertTrue(restored.database.subscriptionDao().statesForSubscriptions(listOf(subscriptionId)).isEmpty())
        assertEquals(zone, restored.settings.recordZone())
        val after = restored.backupSnapshotSource().capture()
        assertEquals(backedUp.data, after.data)
        assertEquals(backedUp.settings, after.settings)
        assertEquals(backedUp.notificationReceipts, after.notificationReceipts)
        assertEquals(1, after.data.mediaAssets.size)
        val media = after.data.mediaAssets.single()
        val restoredBytes = File(restored.directory, media.relativePath).readBytes()
        assertEquals(media.byteLength, restoredBytes.size.toLong())
        assertEquals(media.sha256, MessageDigest.getInstance("SHA-256").digest(restoredBytes).joinToString("") { "%02x".format(it) })
        assertEquals(backedUp.data.mediaAssets.single(), media)
        assertEquals(backedUp.reminderPreferences.renewalEnabled, after.reminderPreferences.renewalEnabled)
        assertEquals(backedUp.reminderPreferences.milestoneEnabled, after.reminderPreferences.milestoneEnabled)
        assertEquals(observation.get(), after.reminderPreferences.importCutoff)
        // Import suppresses historical milestones; this expected cutoff merge is distinct from monetary equality.
        assertEquals(RestoreReminderProtection.snapshot(backedUp, Instant.parse("2026-01-11T04:00:00Z")).milestoneWatermarks, after.milestoneWatermarks)
        capture("04-restored-offline-home")
        waitNode("home-screen")
        compose.onNodeWithTag("home-total-rate").performScrollTo().assert(hasText("¥5.00"))
        compose.onNodeWithTag("home-project-rate").performScrollTo().assert(hasText("¥0.00"))
        compose.onNodeWithTag("home-subscription-rate").performScrollTo().assert(hasText("¥5.00"))
    }

    @Test fun injected_day_with_real_foreground_observer_and_resume_refresh(): Unit = runBlocking {
        createHundredYuanProject()
        homeRate("¥10.00")
        val initial = holder.state.value as LedgerUiState.Ready
        observation.set(LocalDate.of(2026, 1, 11))
        // The production observer retains its real <=60s wait. No UI action or retry causes this read.
        compose.waitUntil(70_000) {
            (holder.state.value as? LedgerUiState.Ready)?.overview?.asOf == observation.get()
        }
        compose.onNodeWithTag("home-total-rate").performScrollTo().assert(hasText("¥9.09"))

        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        observation.set(LocalDate.of(2026, 1, 12))
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(15_000) {
            (holder.state.value as? LedgerUiState.Ready)?.overview?.asOf == observation.get()
        }
        compose.onNodeWithTag("home-total-rate").performScrollTo().assert(hasText("¥8.33"))
        val resumed = holder.state.value as LedgerUiState.Ready
        assertEquals(initial.generationId, resumed.generationId)
        assertTrue("Activity stop/resume must keep the same storage facade", initial.repository === resumed.repository)
        assertEquals(initial.generationId, owner.openStorage().id)
    }

    private suspend fun createHundredYuanProject() {
        click("home-create-project")
        waitNode("project-started-on-input")
        replace("project-name", "M22离线合成项目")
        replace("project-started-on-input", "2026-01-01")
        replace("project-initial-amount", "100")
        // Invalid precision keeps the entered name and does not create partial facts.
        replace("project-initial-amount", "1.234"); click("project-save")
        compose.onNodeWithTag("project-name").assertTextContains("M22离线合成项目", substring = true)
        assertTrue(owner.openStorage().database.projectDao().listProjects().isEmpty())
        capture("00-form-error-input-retained")
        replace("project-initial-amount", "100"); click("project-save")
        waitNode("project-detail-screen")
        awaitRate("10")
    }

    /** Existing registered synthetic cover, not an import path or M10 decoder experiment. */
    private suspend fun registerSyntheticCover(projectId: String) {
        val generation = owner.openStorage()
        val bytes = ByteArrayOutputStream().use { output ->
            val bitmap = Bitmap.createBitmap(2, 3, Bitmap.Config.ARGB_8888)
            try { bitmap.eraseColor(android.graphics.Color.BLUE); check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); output.toByteArray() }
            finally { bitmap.recycle() }
        }
        val mediaId = UUID.randomUUID().toString()
        val path = "media/$mediaId.png"
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        generation.coordinator.withWrite {
            val file = File(generation.directory, path).canonicalFile
            check(file.parentFile == File(generation.directory, "media").canonicalFile && file.createNewFile())
            FileOutputStream(file).use { it.write(bytes); it.fd.sync() }
            generation.database.withTransaction {
                generation.database.mediaDao().insert(LocalMediaAssetEntity(mediaId, path, "image/png", 2, 3, bytes.size.toLong(), digest))
                generation.database.openHelper.writableDatabase.compileStatement(
                    "UPDATE long_term_projects SET coverAssetId=? WHERE id=? AND coverAssetId IS NULL").use {
                    it.bindString(1, mediaId); it.bindString(2, projectId); check(it.executeUpdateDelete() == 1)
                }
            }
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendStatus(0, Bundle().apply {
            putString("M22_REGISTERED_SYNTHETIC_PNG", "$mediaId:${bytes.size}:$digest")
        })
    }

    private fun queryDate(screen: String, input: String, apply: String, date: String) {
        compose.onNodeWithTag(screen).performScrollToNode(hasTestTag(input))
        replace(input, date); click(apply)
        val tag = if (screen == "total-cost-trend-screen") "total-trend-selected-date" else "chart-selected-date"
        compose.onNodeWithTag(screen).performScrollToNode(hasTestTag(tag))
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasTestTag(tag) and hasText("所选日期：$date")).fetchSemanticsNodes().isNotEmpty()
        }
        waitNode(if (screen == "total-cost-trend-screen") "total-trend-total" else "chart-selected-value")
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val run = root.name.removePrefix("m22-ui-journey-")
        val directory = File(context.cacheDir, "m22-captures-$run").apply { mkdirs() }
        File(directory, "owner.txt").writeText("M22 $run production LithicApp isolated ComponentActivity")
        val image = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val file = File(directory, "$name.png")
        file.outputStream().use { check(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        val sha = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
        File(directory, "$name.json").writeText("""{"source":"real API36 screenshot; isolated ComponentActivity with production LithicApp/Nav3", "fontScale":${context.resources.configuration.fontScale},"width":${image.width},"height":${image.height},"sha256":"$sha"}""")
        image.recycle()
    }

    private fun replace(tag: String, text: String) {
        compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag(tag) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(tag).performScrollTo().assertIsEnabled().performTextReplacement(text)
        hideKeyboardAndVerify()
        compose.waitForIdle()
    }
    private fun click(tag: String) {
        hideKeyboardAndVerify()
        compose.waitUntil(15_000) { compose.onAllNodes(hasTestTag(tag) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(tag).performScrollTo().assertIsEnabled().performClick()
    }
    private fun waitNode(tag: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        val detail = when (tag) {
            "project-detail-screen" -> "project-detail-content" to "project-detail-name"
            "subscription-detail-screen" -> "subscription-detail-content" to "subscription-detail-name"
            else -> null
        }
        detail?.let { (list, name) ->
            // The screen container also exists while its real ViewModel is loading.
            compose.onNodeWithTag(list).performScrollToIndex(0)
            compose.waitUntil(15_000) { compose.onAllNodesWithTag(name).fetchSemanticsNodes().isNotEmpty() }
        }
    }
    private fun hideKeyboardAndVerify() {
        val decor = compose.activity.window.decorView
        checkNotNull(ViewCompat.getRootWindowInsets(decor))
        compose.runOnUiThread {
            WindowCompat.getInsetsController(compose.activity.window, decor).hide(WindowInsetsCompat.Type.ime())
        }
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(decor)?.isVisible(WindowInsetsCompat.Type.ime()) == false
        }
    }
    private fun projectDetailClick(tag: String) {
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performClick()
    }
    private fun subscriptionDetailClick(tag: String) {
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performClick()
    }
    private suspend fun awaitRate(expected: String) {
        withTimeout(15_000) { holder.state.first { state ->
            (state as? LedgerUiState.Ready)?.overview?.totalRate?.compareTo(BigDecimal(expected)) == 0
        } }
    }
    private fun homeRate(expected: String) {
        compose.onNodeWithTag("tab-now").performClick()
        waitNode("home-screen")
        compose.onNodeWithTag("home-total-rate").performScrollTo().assert(hasText(expected))
    }
}
