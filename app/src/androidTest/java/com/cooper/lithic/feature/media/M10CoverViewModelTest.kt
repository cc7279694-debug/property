package com.cooper.lithic.feature.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
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
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.media.CoverTarget
import com.cooper.lithic.data.media.CoverTargetType
import com.cooper.lithic.data.media.CoverBaseline
import com.cooper.lithic.data.media.CoverCommand
import com.cooper.lithic.data.media.CoverCommitOutcome
import com.cooper.lithic.data.media.CleanupReport
import com.cooper.lithic.data.media.LocalImageStore
import com.cooper.lithic.data.media.M10ImageSourceFixtures
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.media.MediaResult
import com.cooper.lithic.data.media.PreparedImage
import com.cooper.lithic.data.media.SyntheticImageSource
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.feature.project.LeaveDecision
import com.cooper.lithic.feature.project.SubmissionPhase
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Task-state consumers read actual generation facts and cannot invent a second image owner. */
@RunWith(AndroidJUnit4::class)
class M10CoverViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = FixedBusinessClock(LocalDate.of(2026, 1, 25))
    private val timestamp = Instant.parse("2026-01-25T12:00:00Z")
    private val models = ViewModelStore()
    private val jobs = mutableListOf<Job>()
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var repository: MediaRepository
    private val sources = mutableListOf<SyntheticImageSource>()

    @Before fun before() = runBlocking {
        root = File(context.noBackupFilesDir, "m10-cover-vm-${UUID.randomUUID()}")
        check(root.mkdir())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        repository = MediaRepository(generation.database, generation.coordinator,
            LocalImageStore(generation.directory), clock, context.contentResolver)
    }

    @After fun after() = runBlocking {
        onMain { models.clear() }; jobs.forEach { it.join() }
        sources.forEach { it.delete() }
        generation.close()
        check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
        check(root.deleteRecursively())
    }

    @Test fun project_cover_editor_retains_real_identity_and_history_origin_without_writing() = runBlocking {
        val project = seedProject()
        val model = model()
        onMain { model.start(CoverTarget(CoverTargetType.PROJECT, project.id), "HISTORY") }
        assertNotNull("Opening a real project's cover must produce a recoverable editor", model.state.value.editor)
        val editor = withTimeout(15_000) { model.state.first { it.editor?.loading == false } }.editor!!
        assertEquals(CoverTarget(CoverTargetType.PROJECT, project.id), editor.target)
        assertEquals("HISTORY", editor.origin)
        assertEquals(project.name, editor.name)
        assertEquals(project, generation.database.projectDao().findProject(project.id))
        assertTrue(generation.database.mediaDao().list().isEmpty())
        assertEquals(LeaveDecision.ALLOW, model.leaveDecision())
    }

    @Test fun subscription_cover_editor_retains_identity_and_real_payment_without_writing() = runBlocking {
        val subscription = seedSubscription()
        val before = generation.database.subscriptionDao().renewalsForSubscriptions(listOf(subscription.id))
        val model = model()
        onMain { model.start(CoverTarget(CoverTargetType.SUBSCRIPTION, subscription.id)) }
        assertNotNull("Opening a real subscription's cover must produce a recoverable editor", model.state.value.editor)
        val editor = withTimeout(15_000) { model.state.first { it.editor?.loading == false } }.editor!!
        assertEquals(subscription.id, editor.target.id)
        assertEquals(subscription.name, editor.name)
        assertEquals(subscription, generation.database.subscriptionDao().findSubscription(subscription.id))
        assertEquals(before, generation.database.subscriptionDao().renewalsForSubscriptions(listOf(subscription.id)))
        assertTrue(generation.database.mediaDao().list().isEmpty())
    }

    @Test fun system_picker_cancel_does_not_dirty_or_write_project_or_media() = runBlocking {
        val project = seedProject()
        val beforeEvents = generation.database.projectDao().eventsForProjects(listOf(project.id))
        val model = model()
        onMain { model.start(CoverTarget(CoverTargetType.PROJECT, project.id)) }
        assertNotNull("Picker cancellation must be handled by the formal cover editor", model.state.value.editor)
        withTimeout(15_000) { model.state.first { it.editor?.loading == false } }
        onMain { model.chooseImage(null) }
        assertNull(model.state.value.editor!!.preparedAsset)
        assertEquals(LeaveDecision.ALLOW, model.leaveDecision())
        assertEquals(project, generation.database.projectDao().findProject(project.id))
        assertEquals(beforeEvents, generation.database.projectDao().eventsForProjects(listOf(project.id)))
        assertTrue(generation.database.mediaDao().list().isEmpty())
    }

    @Test fun prepare_keeps_old_reference_until_explicit_save_and_source_uri_is_not_saved() = runBlocking {
        val old = seedStoredImage()
        val project = seedProject(old.id)
        val handle = SavedStateHandle()
        val model = model(handle)
        start(model, project)
        val source = source(png())
        onMain { model.chooseImage(source.uri) }
        val editor = withTimeout(15_000) { model.state.first { it.editor?.preparedAsset != null && !it.editor.preparing } }.editor!!
        assertEquals(old.id, editor.oldAssetId)
        assertEquals(project, generation.database.projectDao().findProject(project.id))
        assertEquals(listOf(old), generation.database.mediaDao().list())
        assertEquals(LeaveDecision.CONFIRM_DISCARD, model.leaveDecision())
        val saved = requireNotNull(handle.get<Bundle>(FORM_KEY))
        assertFalseContainsUri(saved)
        assertTrue(source.delete())
        onMain { model.save() }
        withTimeout(15_000) { model.state.first { it.committedTarget?.id == project.id } }
        val actual = generation.database.projectDao().findProject(project.id)!!
        assertEquals(editor.preparedAsset!!.id, actual.coverAssetId)
        assertEquals(project.note, actual.note)
        assertEquals(project.startedOn, actual.startedOn)
    }

    @Test fun duplicate_save_and_saved_task_recovery_keep_one_asset_and_frozen_operation() = runBlocking {
        val project = seedProject()
        val handle = SavedStateHandle()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val commands = mutableListOf<CoverCommand>()
        val repo = object : MediaRepository(generation.database, generation.coordinator, LocalImageStore(generation.directory), clock, context.contentResolver) {
            override suspend fun commit(command: CoverCommand): MediaResult<CoverCommitOutcome> {
                commands += command; entered.complete(Unit); release.await()
                return super.commit(command)
            }
        }
        val model = model(handle, repo)
        start(model, project)
        onMain { model.chooseImage(source(png()).uri) }
        withTimeout(15_000) { model.state.first { it.editor?.preparedAsset != null && !it.editor.preparing } }
        onMain { model.save(); model.save() }
        withTimeout(15_000) { entered.await() }
        assertEquals(1, commands.size)
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
        val saved = Bundle(requireNotNull(handle.get<Bundle>(FORM_KEY)))
        release.complete(Unit)
        withTimeout(15_000) { model.state.first { it.committedTarget != null } }
        val restored = model(SavedStateHandle(mapOf(FORM_KEY to saved)))
        assertNotNull("A saved in-flight operation must be recovered under the same target", restored.state.value.editor)
        assertEquals(SubmissionPhase.UNCONFIRMED, restored.state.value.editor!!.phase)
        assertEquals(commands.single().newAsset!!.id, restored.state.value.editor!!.preparedAsset!!.id)
        onMain { restored.save() }
        withTimeout(15_000) { restored.state.first { it.committedTarget != null } }
        assertEquals(1, generation.database.mediaDao().list().size)
        assertEquals(commands.single().newAsset!!.id, generation.database.projectDao().findProject(project.id)!!.coverAssetId)
    }

    @Test fun committed_response_loss_replays_same_command_without_a_second_import_or_asset() = runBlocking {
        val project = seedProject()
        val commands = mutableListOf<CoverCommand>()
        var imports = 0
        val repo = object : MediaRepository(generation.database, generation.coordinator, LocalImageStore(generation.directory), clock, context.contentResolver) {
            override suspend fun prepareImport(uri: Uri, assetId: String): MediaResult<PreparedImage> {
                imports += 1; return super.prepareImport(uri, assetId)
            }
            override suspend fun commit(command: CoverCommand): MediaResult<CoverCommitOutcome> {
                commands += command
                val actual = super.commit(command)
                if (commands.size == 1 && actual is MediaResult.Success) throw IOException("Synthetic delivery lost after actual cover commit")
                return actual
            }
        }
        val model = model(repo = repo)
        start(model, project)
        onMain { model.chooseImage(source(png()).uri) }
        withTimeout(15_000) { model.state.first { it.editor?.preparedAsset != null && !it.editor.preparing } }
        onMain { model.save() }
        withTimeout(15_000) { model.state.first { it.editor?.phase == SubmissionPhase.UNCONFIRMED } }
        val persisted = generation.database.projectDao().findProject(project.id)!!
        assertNotNull(persisted.coverAssetId)
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
        onMain { model.save() }
        withTimeout(15_000) { model.state.first { it.committedTarget != null } }
        assertEquals(1, imports)
        assertEquals(2, commands.size)
        assertEquals(commands[0], commands[1])
        assertEquals(persisted, generation.database.projectDao().findProject(project.id))
        assertEquals(1, generation.database.mediaDao().list().size)
    }

    @Test fun committed_saved_envelope_performs_read_only_verification_without_import_or_commit() = runBlocking {
        val asset = seedStoredImage()
        val original = seedProject(asset.id)
        val expected = CoverBaseline.Project(original, generation.database.projectDao().eventsForProjects(listOf(original.id)))
        val writtenAt = timestamp.plusSeconds(1)
        generation.database.openHelper.writableDatabase.execSQL("UPDATE long_term_projects SET updatedAt=? WHERE id=?",
            arrayOf(writtenAt.toString(), original.id))
        val form = savedEnvelope(expected, asset, writtenAt, SubmissionPhase.COMMITTED)
        var commits = 0; var imports = 0; var verifies = 0; var cleanups = 0
        val repo = object : MediaRepository(generation.database, generation.coordinator, LocalImageStore(generation.directory), clock, context.contentResolver) {
            override suspend fun prepareImport(uri: Uri, assetId: String): MediaResult<PreparedImage> {
                imports += 1; return super.prepareImport(uri, assetId)
            }
            override suspend fun commit(command: CoverCommand): MediaResult<CoverCommitOutcome> {
                commits += 1; return super.commit(command)
            }
            override suspend fun verifyCommitted(command: CoverCommand): MediaResult<CoverBaseline> {
                verifies += 1; return super.verifyCommitted(command)
            }
            override suspend fun removeUnreferenced(): MediaResult<CleanupReport> {
                cleanups += 1; return super.removeUnreferenced()
            }
        }
        val model = model(SavedStateHandle(mapOf(FORM_KEY to form)), repo)
        assertNotNull("A committed saved operation needs a read-only recovery state", model.state.value.editor)
        withTimeout(15_000) { model.state.first { it.committedTarget?.id == original.id } }
        onMain { model.save(); model.retryRead() }
        withTimeout(15_000) { model.state.first { it.committedTarget?.id == original.id } }
        withTimeout(15_000) { model.viewModelScope.coroutineContext[Job]!!.children.toList().forEach { it.join() } }
        assertEquals(0, commits)
        assertEquals(0, imports)
        assertEquals("COMMITTED recovery and read retry must not perform media cleanup writes", 0, cleanups)
        assertTrue(verifies >= 1)
        assertEquals(original.copy(updatedAt = writtenAt), generation.database.projectDao().findProject(original.id))
    }

    @Test fun damaged_selected_source_reports_error_and_keeps_old_reference_bytes_and_money() = runBlocking {
        val old = seedStoredImage()
        val project = seedProject(old.id)
        val beforeBytes = File(generation.directory, old.relativePath).readBytes()
        val beforeEvents = generation.database.projectDao().eventsForProjects(listOf(project.id))
        val model = model()
        start(model, project)
        onMain { model.chooseImage(source(byteArrayOf(0, 1, 2, 3), "image/png").uri) }
        withTimeout(15_000) { model.state.first { it.editor?.message != null && !it.editor.preparing } }
        assertEquals(old.id, model.state.value.editor!!.oldAssetId)
        assertNull(model.state.value.editor!!.preparedAsset)
        assertEquals(project, generation.database.projectDao().findProject(project.id))
        org.junit.Assert.assertArrayEquals(beforeBytes, File(generation.directory, old.relativePath).readBytes())
        assertEquals(beforeEvents, generation.database.projectDao().eventsForProjects(listOf(project.id)))
    }

    @Test fun damaged_prepared_private_file_allows_explicit_reload_and_reselect_without_touching_old_cover() = runBlocking {
        val old = seedStoredImage()
        val project = seedProject(old.id)
        val oldBytes = File(generation.directory, old.relativePath).readBytes()
        val events = generation.database.projectDao().eventsForProjects(listOf(project.id))
        val model = model()
        start(model, project)
        onMain { model.chooseImage(source(png()).uri) }
        withTimeout(15_000) { model.state.first { it.editor?.preparedAsset != null && !it.editor.preparing } }
        val prepared = model.state.value.editor!!.preparedAsset!!
        val staged = File(generation.directory, "media/.m10-imports/${prepared.id}/" + if (prepared.mime == "image/png") "image.png" else "image.jpg")
        assertTrue(staged.isFile)
        assertTrue(staged.canonicalPath.startsWith(generation.directory.canonicalPath + File.separator))
        staged.writeBytes(byteArrayOf(0, 1, 2, 3))
        onMain { model.save() }
        withTimeout(15_000) { model.state.first { it.editor?.phase == SubmissionPhase.UNCONFIRMED } }
        assertEquals(project, generation.database.projectDao().findProject(project.id))
        org.junit.Assert.assertArrayEquals(oldBytes, File(generation.directory, old.relativePath).readBytes())
        assertEquals(events, generation.database.projectDao().eventsForProjects(listOf(project.id)))
        onMain { model.retryRead() }
        var editableAgain = false
        try {
            withTimeout(3_000) {
                while (model.state.value.editor?.phase != SubmissionPhase.IDLE || model.state.value.editor?.loading != false) delay(25)
                editableAgain = true
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) { }
        assertTrue("A deterministically damaged staged image must allow explicit fresh read and reselection, not permanent pending lock", editableAgain)
        assertEquals(old.id, model.state.value.editor!!.oldAssetId)
        onMain { model.chooseImage(source(png()).uri) }
        withTimeout(15_000) { model.state.first { it.editor?.preparedAsset != null && !it.editor.preparing } }
        assertTrue(prepared.id != model.state.value.editor!!.preparedAsset!!.id)
        onMain { model.save() }
        withTimeout(15_000) { model.state.first { it.committedTarget?.id == project.id } }
        assertEquals(events, generation.database.projectDao().eventsForProjects(listOf(project.id)))
    }

    @Test fun view_model_clear_after_real_prepare_releases_only_lease_and_preserves_recoverable_bytes() = runBlocking {
        val project = seedProject()
        val preparedBeforeDelivery = CompletableDeferred<PreparedImage>()
        val repo = object : MediaRepository(generation.database, generation.coordinator, LocalImageStore(generation.directory), clock, context.contentResolver) {
            override suspend fun prepareImport(uri: Uri, assetId: String): MediaResult<PreparedImage> {
                val real = super.prepareImport(uri, assetId)
                if (real is MediaResult.Success) {
                    preparedBeforeDelivery.complete(real.value)
                    awaitCancellation()
                }
                return real
            }
        }
        val model = model(repo = repo)
        start(model, project)
        onMain { model.chooseImage(source(png()).uri) }
        val prepared = withTimeout(15_000) { preparedBeforeDelivery.await() }.asset
        val staged = File(generation.directory, "media/.m10-imports/${prepared.id}/" + if (prepared.mime == "image/png") "image.png" else "image.jpg")
        assertTrue(staged.isFile)
        onMain { models.clear() }
        withTimeout(15_000) { jobs.forEach { it.join() } }
        assertTrue("Cancellation lease release must not delete a recoverable prepared image", staged.isFile)
        assertEquals(project, generation.database.projectDao().findProject(project.id))
        assertTrue(generation.database.mediaDao().list().isEmpty())
        val cleanup = repo.removeUnreferenced()
        assertTrue(cleanup is MediaResult.Success)
        assertFalseContainsActive(cleanup, prepared.id)
        assertFalseFileExists(staged)
        assertEquals(project, generation.database.projectDao().findProject(project.id))
    }

    @Test fun view_model_clear_after_prepared_result_is_published_releases_lease_without_deleting_recoverable_bytes() = runBlocking {
        val old = seedStoredImage()
        val project = seedProject(old.id)
        val oldBytes = File(generation.directory, old.relativePath).readBytes()
        val events = generation.database.projectDao().eventsForProjects(listOf(project.id))
        val images = LocalImageStore(generation.directory)
        val repo = MediaRepository(generation.database, generation.coordinator, images, clock, context.contentResolver)
        val model = model(repo = repo)
        start(model, project)
        onMain { model.chooseImage(source(png()).uri) }
        val prepared = withTimeout(15_000) {
            model.state.first { it.editor?.preparedAsset != null && !it.editor.preparing }
        }.editor!!.preparedAsset!!
        val staged = File(generation.directory, "media/.m10-imports/${prepared.id}/" + if (prepared.mime == "image/png") "image.png" else "image.jpg")
        val stagedBytes = staged.readBytes()
        assertTrue("The published preparation must hold its real image-store lease", images.isPreparedActive(prepared.id))
        onMain { models.clear() }
        withTimeout(15_000) { jobs.forEach { it.join() } }
        var released = false
        try {
            withTimeout(3_000) {
                while (images.isPreparedActive(prepared.id)) delay(25)
                released = true
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) { }
        assertTrue("A cleared VM must release the lease even after successful prepare delivery has completed", released)
        assertTrue("Lifecycle cleanup must retain the recoverable preparation until explicit safe collection", staged.isFile)
        org.junit.Assert.assertArrayEquals(stagedBytes, staged.readBytes())
        org.junit.Assert.assertArrayEquals(oldBytes, File(generation.directory, old.relativePath).readBytes())
        assertEquals(project, generation.database.projectDao().findProject(project.id))
        assertEquals(events, generation.database.projectDao().eventsForProjects(listOf(project.id)))
        assertEquals(listOf(old), generation.database.mediaDao().list())
        val cleanup = repo.removeUnreferenced()
        assertTrue(cleanup is MediaResult.Success)
        assertFalseContainsActive(cleanup, prepared.id)
        assertFalseFileExists(staged)
        org.junit.Assert.assertArrayEquals(oldBytes, File(generation.directory, old.relativePath).readBytes())
        assertEquals(project, generation.database.projectDao().findProject(project.id))
        assertEquals(events, generation.database.projectDao().eventsForProjects(listOf(project.id)))
        assertEquals(listOf(old), generation.database.mediaDao().list())
    }

    private suspend fun seedProject(coverId: String? = null): LongTermProjectEntity {
        val project = LongTermProjectEntity(UUID.randomUUID().toString(), "合成项目封面", LithicDatabase.PROJECT_FALLBACK_ID,
            LocalDate.of(2026, 1, 1), coverAssetId = coverId, note = "原项目备注", createdAt = timestamp, updatedAt = timestamp)
        generation.coordinator.withWrite {
            generation.database.withTransaction {
                generation.database.projectDao().insertProject(project)
                generation.database.projectDao().insertEvent(ProjectMoneyEventEntity(UUID.randomUUID().toString(), project.id,
                    MoneyDirection.OUTLAY, 15000, LocalDate.of(2026, 1, 1), "原真实初始付款",
                    ProjectMoneyEventRole.INITIAL, UUID.randomUUID().toString()))
            }
        }
        return project
    }

    private suspend fun seedSubscription(): SubscriptionEntity {
        val subscription = SubscriptionEntity(UUID.randomUUID().toString(), "合成订阅封面", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            note = "原订阅备注", createdAt = timestamp, updatedAt = timestamp)
        generation.coordinator.withWrite {
            generation.database.withTransaction {
                generation.database.subscriptionDao().insertSubscription(subscription)
                generation.database.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(UUID.randomUUID().toString(), subscription.id,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 29),
                    15000, BillingCycle.MONTHLY, 30, UUID.randomUUID().toString()))
            }
        }
        return subscription
    }

    private fun model(handle: SavedStateHandle = SavedStateHandle(), repo: MediaRepository = repository): CoverViewModel {
        lateinit var model: CoverViewModel
        onMain {
            model = CoverViewModel(repo, handle) { timestamp.plusNanos(1) }
            models.put("cover-${UUID.randomUUID()}", model)
            model.viewModelScope.coroutineContext[Job]?.let(jobs::add)
        }
        return model
    }

    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    private suspend fun start(model: CoverViewModel, project: LongTermProjectEntity) {
        onMain { model.start(CoverTarget(CoverTargetType.PROJECT, project.id)) }
        assertNotNull("The formal cover editor is missing", model.state.value.editor)
        withTimeout(15_000) { model.state.first { it.editor?.loading == false } }
    }

    private fun png(): ByteArray = ByteArrayOutputStream().use { output ->
        val bitmap = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(35, 80, 130)); check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); bitmap.recycle()
        output.toByteArray()
    }
    private fun source(bytes: ByteArray, mime: String = "image/png") = M10ImageSourceFixtures.source(bytes, mime).also(sources::add)
    private suspend fun seedStoredImage(): LocalMediaAssetEntity {
        val id = UUID.randomUUID().toString(); val bytes = png()
        val asset = LocalMediaAssetEntity(id, "media/$id.png", "image/png", 32, 24, bytes.size.toLong(),
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) })
        File(generation.directory, asset.relativePath).writeBytes(bytes)
        generation.database.mediaDao().insert(asset)
        return asset
    }

    /** Independently encoded saved contract, not a production encoder mirror assertion. */
    private fun savedEnvelope(expected: CoverBaseline.Project, asset: LocalMediaAssetEntity, writtenAt: Instant, phase: SubmissionPhase) = Bundle().apply {
        putString("targetType", "PROJECT"); putString("targetId", expected.project.id); putString("origin", "CURRENT")
        putString("operationId", UUID.randomUUID().toString()); putString("phase", phase.name)
        putString("writtenAt", writtenAt.toString()); putBoolean("dirty", true)
        putBundle("baseline", Bundle().apply {
            putString("type", "PROJECT")
            putBundle("project", Bundle().apply {
                val p = expected.project
                putString("id", p.id); putString("name", p.name); putString("categoryId", p.categoryId); putString("startedOn", p.startedOn.toString())
                putString("endedOn", p.endedOn?.toString()); putString("endReason", p.endReason); putString("coverAssetId", p.coverAssetId)
                putString("note", p.note); putString("createdAt", p.createdAt.toString()); putString("updatedAt", p.updatedAt.toString())
            })
            putParcelableArrayList("events", ArrayList(expected.events.map { e -> Bundle().apply {
                putString("id", e.id); putString("projectId", e.projectId); putString("direction", e.direction.name)
                putLong("amountCents", e.amountCents); putString("occurredOn", e.occurredOn.toString()); putString("description", e.description)
                putString("role", e.role.name); putString("operationId", e.operationId)
            } }))
        })
        putBundle("asset", Bundle().apply {
            putString("id", asset.id); putString("path", asset.relativePath); putString("mime", asset.mime)
            putInt("width", asset.width); putInt("height", asset.height); putLong("bytes", asset.byteLength); putString("sha256", asset.sha256)
        })
    }
    private fun assertFalseContainsUri(bundle: Bundle) {
        bundle.keySet().forEach { key ->
            val value = bundle.get(key)
            if (value is Bundle) assertFalseContainsUri(value)
            if (value is String) org.junit.Assert.assertFalse("Source URI must not become task or database state", value.startsWith("content://"))
        }
    }
    private fun assertFalseContainsActive(result: MediaResult<CleanupReport>, id: String) {
        val actual = (result as MediaResult.Success<CleanupReport>).value
        org.junit.Assert.assertFalse("A cancelled VM cannot keep a memory lease active forever", id in actual.skippedActiveAssetIds)
    }
    private fun assertFalseFileExists(file: File) = org.junit.Assert.assertFalse("Controlled cleanup should reclaim the now-inactive owned preparation", file.exists())
    private companion object { const val FORM_KEY = "lithic.media.activeForm" }
}
