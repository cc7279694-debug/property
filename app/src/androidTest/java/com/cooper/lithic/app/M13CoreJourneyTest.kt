package com.cooper.lithic.app

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
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
class M13CoreJourneyTest {
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
        root = File(context.noBackupFilesDir, "m13-ui-journey-${UUID.randomUUID()}").canonicalFile
        check(root.mkdir())
        val storage = File(root, "owned-generation").apply { check(mkdir()) }
        owner = AppContainer(DataGenerationStore(context, storage), CoroutineScope(SupervisorJob() + Dispatchers.IO))
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
        if (::owner.isInitialized) owner.close()
        if (::root.isInitialized) {
            assertEquals(context.noBackupFilesDir.canonicalFile, root.canonicalFile.parentFile)
            check(root.deleteRecursively())
        }
    }

    @Test fun visible_core_writes_home_backup_confirmation_and_restored_generation_recompute(): Unit = runBlocking {
        createHundredYuanProject()
        projectDetailClick("project-add-outlay")
        waitNode("money-editor-screen")
        replace("money-amount", "20")
        replace("money-occurred-on-input", "2026-01-05")
        click("money-save")
        waitNode("project-detail-screen")
        awaitRate("12")
        homeRate("¥12.00")

        click("home-create-subscription")
        waitNode("subscription-paid-on-input")
        replace("subscription-name", "合成核心旅程订阅")
        replace("subscription-amount", "150")
        click("subscription-cycle-MONTHLY")
        replace("subscription-paid-on-input", "2026-01-01")
        replace("subscription-effective-from-input", "2026-01-01")
        replace("subscription-paid-until-input", "2026-02-01")
        replace("subscription-reminder-due-input", "2026-01-29")
        click("subscription-confirm-payment")
        waitNode("subscription-detail-screen")
        awaitRate("17")
        homeRate("¥17.00")

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
        compose.onNodeWithTag("subscription-list-content").performScrollToNode(hasTestTag("subscription-row-$subscriptionId"))
        compose.onNodeWithTag("subscription-row-$subscriptionId").performClick()
        waitNode("subscription-detail-screen")
        subscriptionDetailClick("subscription-detail-pause")
        waitNode("subscription-state-confirm")
        click("subscription-state-confirm")
        awaitRate("12")
        homeRate("¥12.00")

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
        assertEquals(2, restored.database.projectDao().listEvents().size)
        assertEquals(1L, restored.database.subscriptionDao().countRenewals(subscriptionId))
        assertTrue(restored.database.subscriptionDao().statesForSubscriptions(listOf(subscriptionId)).isEmpty())
        assertEquals(zone, restored.settings.recordZone())
        waitNode("home-screen")
        compose.onNodeWithTag("home-total-rate").performScrollTo().assert(hasText("¥15.91"))
        compose.onNodeWithTag("home-project-rate").performScrollTo().assert(hasText("¥10.91"))
        compose.onNodeWithTag("home-subscription-rate").performScrollTo().assert(hasText("¥5.00"))
    }

    @Test fun real_foreground_date_observer_and_activity_resume_refresh_without_manual_retry(): Unit = runBlocking {
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
        replace("project-name", "合成核心旅程项目")
        replace("project-started-on-input", "2026-01-01")
        replace("project-initial-amount", "100")
        click("project-save")
        waitNode("project-detail-screen")
        awaitRate("10")
    }

    private fun replace(tag: String, text: String) {
        compose.onNodeWithTag(tag).performScrollTo().assertIsEnabled().performTextReplacement(text)
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
    }
    private fun click(tag: String) {
        Espresso.closeSoftKeyboard()
        if (tag in setOf("money-save", "end-save", "project-save", "subscription-save", "renewal-confirm-payment", "subscription-state-confirm", "subscription-detail-renew", "money-back", "end-back", "subscription-editor-back", "renewal-back", "subscription-state-back")) {
            compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled().performClick()
        } else {
            compose.onNodeWithTag(tag).performScrollTo().assertIsEnabled().performClick()
        }
    }
    private fun waitNode(tag: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
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
