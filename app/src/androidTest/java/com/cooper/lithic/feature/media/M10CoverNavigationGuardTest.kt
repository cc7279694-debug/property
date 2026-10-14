package com.cooper.lithic.feature.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToString
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.LedgerUiState
import com.cooper.lithic.app.LedgerUiStateHolder
import com.cooper.lithic.app.LithicApp
import com.cooper.lithic.app.StorageInitializationState
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.media.CoverTarget
import com.cooper.lithic.data.media.CoverTargetType
import com.cooper.lithic.data.media.CoverBaseline
import com.cooper.lithic.data.media.CoverCommand
import com.cooper.lithic.data.media.MediaResult
import com.cooper.lithic.data.media.PreparedImage
import com.cooper.lithic.data.media.LocalImageStore
import com.cooper.lithic.data.media.M10ImageSourceFixtures
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.media.SyntheticImageSource
import com.cooper.lithic.feature.project.ProjectViewModel
import com.cooper.lithic.feature.project.SubmissionPhase
import com.cooper.lithic.feature.subscription.SubscriptionViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Nav3/ComponentActivity behavior; hidden task state must remain reachable. */
@RunWith(AndroidJUnit4::class)
class M10CoverNavigationGuardTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = FixedBusinessClock(LocalDate.of(2026, 1, 25))
    private val timestamp = Instant.parse("2026-01-25T12:00:00Z")
    private val models = ViewModelStore()
    private val jobs = mutableListOf<Job>()
    private val sources = mutableListOf<SyntheticImageSource>()
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var ready: LedgerUiState.Ready
    private lateinit var projectModel: ProjectViewModel
    private lateinit var subscriptionModel: SubscriptionViewModel
    private lateinit var repository: MediaRepository
    private lateinit var project: LongTermProjectEntity
    private lateinit var event: ProjectMoneyEventEntity

    @Before fun before() = runBlocking {
        root = File(context.noBackupFilesDir, "m10-cover-nav-${UUID.randomUUID()}")
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        project = LongTermProjectEntity(UUID.randomUUID().toString(), "合成封面导航项目", LithicDatabase.PROJECT_FALLBACK_ID,
            LocalDate.of(2026, 1, 1), note = "原备注", createdAt = timestamp, updatedAt = timestamp)
        event = ProjectMoneyEventEntity(UUID.randomUUID().toString(), project.id, MoneyDirection.OUTLAY, 15000,
            LocalDate.of(2026, 1, 1), "真实初始款", ProjectMoneyEventRole.INITIAL, UUID.randomUUID().toString())
        generation.database.projectDao().insertProject(project); generation.database.projectDao().insertEvent(event)
        repository = MediaRepository(generation.database, generation.coordinator, LocalImageStore(generation.directory), clock, context.contentResolver)
        val storage = MutableStateFlow<StorageInitializationState>(StorageInitializationState.Ready(generation))
        lateinit var holder: LedgerUiStateHolder
        onMain { holder = LedgerUiStateHolder(storage) { clock }; retain("ledger", holder) }
        ready = withTimeout(15_000) { holder.state.filterIsInstance<LedgerUiState.Ready>().first() }
        onMain {
            projectModel = ProjectViewModel(ready.repository, SavedStateHandle(), clock); retain("project", projectModel)
            subscriptionModel = SubscriptionViewModel(ready.subscriptionRepository, SavedStateHandle(), clock); retain("subscription", subscriptionModel)
        }
    }

    @After fun after() = runBlocking {
        onMain { models.clear() }; jobs.forEach { it.join() }; sources.forEach { it.delete() }
        generation.close()
        check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
        check(root.deleteRecursively())
    }

    @Test fun home_real_system_back_preserves_hidden_pending_cover_and_has_reachable_recovery_route() = runBlocking {
        val handle = SavedStateHandle(mapOf(FORM_KEY to pendingForm()))
        val cover = model(handle)
        assertEquals(SubmissionPhase.UNCONFIRMED, cover.state.value.editor!!.phase)
        render(cover)
        val host = compose.activity
        onMain { assertEquals(Lifecycle.State.RESUMED, host.lifecycle.currentState) }
        Espresso.pressBackUnconditionally()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        onMain {
            assertFalse("Home cannot finish while a cover commit remains unconfirmed", host.isFinishing)
            assertFalse(host.isDestroyed)
            assertEquals(Lifecycle.State.RESUMED, host.lifecycle.currentState)
        }
        compose.onNodeWithTag("pending-submit-dialog").assertIsDisplayed()
        compose.onNodeWithTag("pending-submit-return-to-form").performClick()
        compose.onNodeWithTag("cover-editor-screen").assertIsDisplayed()
        compose.onNodeWithTag("cover-save").performScrollTo().assertTextContains("核对并重试同一封面保存")
        assertNotNull(handle.get<Bundle>(FORM_KEY))
        assertEquals(project, generation.database.projectDao().findProject(project.id))
        assertEquals(listOf(event), generation.database.projectDao().eventsForProjects(listOf(project.id)))
        assertEquals(0, generation.database.mediaDao().list().size)
    }

    @Test fun prepared_cover_guards_all_tabs_and_explicit_discard_never_updates_the_business_reference() = runBlocking {
        val cover = model()
        onMain { cover.start(CoverTarget(CoverTargetType.PROJECT, project.id)) }
        withTimeout(15_000) { cover.state.first { it.editor?.loading == false } }
        val source = M10ImageSourceFixtures.source(png()).also(sources::add)
        onMain { cover.chooseImage(source.uri) }
        withTimeout(15_000) { cover.state.first { it.editor?.preparedAsset != null && !it.editor.preparing } }
        val assetId = cover.state.value.editor!!.preparedAsset!!.id
        render(cover)
        for (tag in listOf("tab-now", "tab-review", "tab-settings")) {
            compose.onNodeWithTag(tag).performClick()
            compose.onNodeWithTag("discard-dialog").assertIsDisplayed()
            compose.onNodeWithTag("discard-keep").performClick()
            assertEquals(assetId, cover.state.value.editor!!.preparedAsset!!.id)
        }
        compose.onNodeWithTag("tab-settings").performClick()
        compose.onNodeWithTag("discard-confirm").performClick()
        compose.onNodeWithTag("settings-screen").assertIsDisplayed()
        assertEquals(project, generation.database.projectDao().findProject(project.id))
        assertEquals(listOf(event), generation.database.projectDao().eventsForProjects(listOf(project.id)))
        assertEquals(0, generation.database.mediaDao().list().size)
    }

    @Test fun project_delete_commit_triggers_independent_safe_media_cleanup_without_changing_delete_result() = runBlocking {
        val source = M10ImageSourceFixtures.source(png()).also(sources::add)
        val prepared = repository.prepareImport(source.uri, UUID.randomUUID().toString())
        assertTrue("The real import must succeed before deletion cleanup is tested", prepared is MediaResult.Success)
        val asset = (prepared as MediaResult.Success<PreparedImage>).value.asset
        val result = repository.commit(CoverCommand(UUID.randomUUID().toString(), CoverBaseline.Project(project, listOf(event)), asset,
            timestamp.plusSeconds(1)))
        assertTrue(result is MediaResult.Success)
        assertTrue(File(generation.directory, asset.relativePath).isFile)
        ready = ready.copy(mediaRepository = repository)
        onMain { projectModel.startDelete(project.id) }
        withTimeout(15_000) { projectModel.state.first { it.deleteConfirmation?.loading == false } }
        onMain { projectModel.save() }
        withTimeout(15_000) { projectModel.state.first { it.deletedProjectId == project.id } }
        assertEquals(null, generation.database.projectDao().findProject(project.id))
        assertNotNull("The original project delete transaction must leave media for independent cleanup", generation.database.mediaDao().find(asset.id))
        render(model())
        var completed = false
        try {
            withTimeout(8_000) {
                while (generation.database.mediaDao().find(asset.id) != null || File(generation.directory, asset.relativePath).exists()) delay(25)
                completed = true
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) { }
        assertTrue("Successful project deletion must request safe media cleanup after its Room transaction", completed)
        // Storage cleanup can finish before the Nav3 transition and list read have settled.
        try {
            compose.waitUntil(10_000) { projectModel.state.value.deletedProjectId == null && !projectModel.state.value.list.loading }
            compose.waitUntil(10_000) { compose.onNodeWithTag("project-list-screen").isDisplayed() }
        } catch (failure: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("Deleted marker=${projectModel.state.value.deletedProjectId}; list=${projectModel.state.value.list}\n" +
                compose.onRoot(useUnmergedTree = true).printToString(), failure)
        }
        compose.onNodeWithTag("project-list-screen").assertIsDisplayed()
        assertEquals(null, generation.database.projectDao().findProject(project.id))
        assertEquals(0, generation.database.projectDao().eventsForProjects(listOf(project.id)).size)
    }

    private fun model(handle: SavedStateHandle = SavedStateHandle()): CoverViewModel {
        lateinit var model: CoverViewModel
        onMain { model = CoverViewModel(repository, handle) { timestamp.plusSeconds(1) }; retain("cover-${UUID.randomUUID()}", model) }
        return model
    }
    private fun render(cover: CoverViewModel) {
        compose.setContent { LithicTheme { LithicApp(ready, {}, projectViewModel = projectModel,
            subscriptionViewModel = subscriptionModel, coverViewModel = cover) } }
        compose.waitForIdle()
    }
    private fun retain(key: String, model: ViewModel) {
        models.put(key, model); model.viewModelScope.coroutineContext[Job]?.let(jobs::add)
    }
    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    private fun png() = ByteArrayOutputStream().use { output ->
        val image = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888); image.eraseColor(Color.BLUE)
        check(image.compress(Bitmap.CompressFormat.PNG, 100, output)); image.recycle(); output.toByteArray()
    }

    /** No source URI and no DB side effect is needed to recover an uncertain original command. */
    private fun pendingForm() = Bundle().apply {
        putString("targetType", "PROJECT"); putString("targetId", project.id); putString("origin", "CURRENT")
        putString("operationId", UUID.randomUUID().toString()); putString("phase", SubmissionPhase.IN_FLIGHT.name)
        putString("writtenAt", timestamp.plusSeconds(1).toString()); putBoolean("dirty", true)
        putBundle("baseline", Bundle().apply {
            putString("type", "PROJECT")
            putBundle("project", Bundle().apply {
                putString("id", project.id); putString("name", project.name); putString("categoryId", project.categoryId)
                putString("startedOn", project.startedOn.toString()); putString("endedOn", null); putString("endReason", null)
                putString("coverAssetId", null); putString("note", project.note)
                putString("createdAt", timestamp.toString()); putString("updatedAt", timestamp.toString())
            })
            putParcelableArrayList("events", arrayListOf(Bundle().apply {
                putString("id", event.id); putString("projectId", project.id); putString("direction", "OUTLAY"); putLong("amountCents", 15000)
                putString("occurredOn", "2026-01-01"); putString("description", event.description); putString("role", "INITIAL")
                putString("operationId", event.operationId)
            }))
        })
        val assetId = UUID.randomUUID().toString()
        putBundle("asset", Bundle().apply {
            putString("id", assetId); putString("path", "media/$assetId.png"); putString("mime", "image/png")
            putInt("width", 32); putInt("height", 24); putLong("bytes", 100); putString("sha256", "0".repeat(64))
        })
    }
    private companion object { const val FORM_KEY = "lithic.media.activeForm" }
}
