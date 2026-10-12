package com.cooper.lithic.ui

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.app.LedgerUiState
import com.cooper.lithic.app.LedgerUiStateHolder
import com.cooper.lithic.app.LithicApp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.CreateProjectCommand
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ProjectWriteRepository
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Every Ready fixture comes from actual private Room/DataStore storage, never an empty fallback. */
@RunWith(AndroidJUnit4::class)
class StorageUiStateTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var directory: File
    private lateinit var container: AppContainer
    private val viewModels = ViewModelStore()
    private val releaseInitialization = CompletableDeferred<Unit>()

    @Before fun before() {
        directory = File(context.noBackupFilesDir, "m05-storage-ui-${UUID.randomUUID()}")
        check(directory.mkdir())
    }

    @After fun after() = runBlocking {
        releaseInitialization.complete(Unit)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModels.clear() }
        if (::container.isInitialized) container.close()
        check(directory.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
        check(directory.deleteRecursively())
    }

    @Test fun initializing_shows_loading_without_normal_empty_navigation() = runBlocking {
        start { releaseInitialization.await() }
        val holder = holder()
        show(holder)
        compose.onNodeWithTag("storage_loading").assertIsDisplayed()
        compose.onNodeWithText("暂无记录").assertDoesNotExist()
        compose.onNodeWithTag("primary_navigation").assertDoesNotExist()
        compose.onNodeWithText("¥0.00").assertDoesNotExist()
        releaseInitialization.complete(Unit)
        assertTrue(settled(holder) is LedgerUiState.Ready)
    }

    @Test fun actual_initialization_failure_has_protective_error_and_working_exit() = runBlocking {
        start { throw IOException("Synthetic failure with private /data/hidden/path") }
        val holder = holder()
        assertEquals(LedgerUiState.Failed, settled(holder))
        var exits = 0
        show(holder) { exits++ }
        compose.onNodeWithTag("storage_error").assertIsDisplayed()
        compose.onNodeWithText("本地数据暂时无法读取。").assertIsDisplayed()
        compose.onNodeWithText("Synthetic failure with private /data/hidden/path").assertDoesNotExist()
        compose.onNodeWithText("暂无记录").assertDoesNotExist()
        compose.onNodeWithTag("primary_navigation").assertDoesNotExist()
        compose.onNodeWithTag("storage_exit").performClick()
        assertEquals(1, exits)
        assertFalse(File(directory, "active-generation").exists())
    }

    @Test fun actual_empty_snapshot_is_ready_only_after_repository_and_zone_read() = runBlocking {
        start()
        val holder = holder()
        val state = settled(holder)
        assertTrue("A real successful snapshot must produce Ready", state is LedgerUiState.Ready)
        state as LedgerUiState.Ready
        assertEquals(2, state.snapshot.categories.size)
        assertTrue(state.snapshot.projects.isEmpty())
        assertTrue(state.snapshot.moneyEvents.isEmpty())
        assertEquals(ZoneId.of("Asia/Shanghai"), state.recordZone)
        show(holder)
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("storage_loading").assertDoesNotExist()
        compose.onNodeWithTag("storage_error").assertDoesNotExist()
    }

    @Test fun real_project_snapshot_is_not_replaced_by_empty_data_or_zero_metrics(): Unit = runBlocking {
        start()
        val generation = container.openStorage()
        seedProject(generation)
        val holder = holder()
        val state = settled(holder)
        assertTrue(state is LedgerUiState.Ready)
        state as LedgerUiState.Ready
        assertEquals(listOf("M05 合成项目"), state.snapshot.projects.map { it.name })
        assertEquals(1, state.snapshot.moneyEvents.size)
        show(holder)
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("¥0.00").assertDoesNotExist()
        compose.onNodeWithText("暂无记录").assertDoesNotExist()
        compose.onNodeWithText("已录入 1 条长期项目").assertIsDisplayed()
        compose.onNodeWithTag("open-project-list").performScrollTo().performClick()
        compose.onNodeWithText("M05 合成项目").assertIsDisplayed()
    }

    @Test fun ready_database_read_failure_is_protective_error_instead_of_empty_snapshot() = runBlocking {
        start()
        val generation = container.openStorage()
        generation.database.close()
        val holder = holder()
        assertEquals("Closed real Room database must fail the actual Repository read", LedgerUiState.Failed, settled(holder))
        show(holder)
        compose.onNodeWithTag("storage_error").assertIsDisplayed()
        compose.onNodeWithTag("primary_navigation").assertDoesNotExist()
        compose.onNodeWithText("暂无记录").assertDoesNotExist()
        compose.onNodeWithText("¥0.00").assertDoesNotExist()
    }

    @Test fun clearing_ui_owner_does_not_cancel_or_recreate_the_storage_owner() = runBlocking {
        start { releaseInitialization.await() }
        holder()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModels.clear() }
        releaseInitialization.complete(Unit)
        val generation = container.openStorage()
        assertSame(generation, container.openStorage())
        assertEquals(ZoneId.of("Asia/Shanghai"), generation.settings.recordZone())
        assertEquals(2, generation.database.categoryDao().list().size)
        assertEquals(1, File(directory, "generations").listFiles()!!.size)
    }

    @Test fun ready_settings_read_failure_is_protective_error_with_valid_database() = runBlocking {
        start()
        val generation = container.openStorage()
        generation.settings.close()
        assertEquals(2, generation.database.categoryDao().list().size)
        val holder = holder()
        assertEquals(LedgerUiState.Failed, settled(holder))
        show(holder)
        compose.onNodeWithTag("storage_error").assertIsDisplayed()
        compose.onNodeWithTag("primary_navigation").assertDoesNotExist()
        compose.onNodeWithText("暂无记录").assertDoesNotExist()
    }

    private fun start(initialize: suspend (ManagedGeneration) -> Unit = {}) {
        container = AppContainer(DataGenerationStore(context, directory, initialize = initialize),
            CoroutineScope(SupervisorJob() + Dispatchers.IO))
        container.start(ZoneId.of("Asia/Shanghai"))
    }

    private fun holder(): LedgerUiStateHolder = LedgerUiStateHolder(container.storageState).also {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModels.put("ui", it) }
    }

    private fun show(holder: LedgerUiStateHolder, exit: () -> Unit = {}) {
        compose.setContent {
            val state by holder.state.collectAsState()
            LithicTheme { LithicApp(state, exit) }
        }
    }

    private suspend fun settled(holder: LedgerUiStateHolder): LedgerUiState? =
        withTimeoutOrNull(5_000) { holder.state.first { it != LedgerUiState.Initializing } }

    private suspend fun seedProject(generation: ManagedGeneration) {
        val now = Instant.parse("2026-10-12T00:00:00Z")
        val date = LocalDate.of(2026, 10, 12)
        val project = LongTermProjectEntity(UUID.randomUUID().toString(), "M05 合成项目", LithicDatabase.PROJECT_FALLBACK_ID, date, createdAt = now, updatedAt = now)
        val operation = UUID.randomUUID().toString()
        val event = ProjectMoneyEventEntity(UUID.randomUUID().toString(), project.id, MoneyDirection.OUTLAY, 0L, date, "合成测试", ProjectMoneyEventRole.INITIAL, operation)
        assertTrue(ProjectWriteRepository(generation.database, generation.coordinator, FixedBusinessClock(date))
            .createProject(CreateProjectCommand(operation, project, event)) is MutationResult.Success)
    }
}
