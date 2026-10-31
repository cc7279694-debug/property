package com.cooper.lithic.feature.media

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.BuildConfig
import com.cooper.lithic.data.media.photolite.*
import com.cooper.lithic.feature.project.LeaveDecision
import com.cooper.lithic.ui.C2VisualFixtures
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Source assertions only: no chooser launch, files, Room, owner or personal media are used. */
class PhotoLiteCoverControllerTest {
    private val stores = mutableListOf<ViewModelStore>()
    private val project = C2VisualFixtures(true).projects.items.single().project
    private val uri = Uri.parse("content://photolite-test/synthetic")

    @Before fun plain_host_only() {
        assertPhotoLitePlainHost()
    }

    @After fun close_models() = onMain { stores.forEach { it.clear() } }

    @Test fun saved_incomplete_commit_is_unknown_and_cannot_restart() {
        val writer = Writer()
        val saved = SavedStateHandle(mapOf(PhotoLiteCoverController.SAVED_KEY to Bundle().apply {
            putBoolean(PhotoLiteCoverController.INCOMPLETE, true)
            putString(PhotoLiteCoverController.NONCE, "test-nonce")
        }))
        val model = model(writer, saved)
        onMain { model.bindProject(project.id); model.begin(project.id); model.confirm(); model.discard() }
        assertEquals(PhotoLiteCoverPhase.UNKNOWN, model.state.value.phase)
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
        assertEquals(0, writer.captures.get())
        assertEquals(0, writer.commits.get())
    }

    @Test fun unsupported_platform_never_captures_or_requests_a_picker() {
        val writer = Writer()
        val model = model(writer, available = false)
        onMain { model.bindProject(project.id); model.begin(project.id) }
        assertEquals(PhotoLiteCoverPhase.REJECTED, model.state.value.phase)
        assertNull(model.state.value.pickerTicket)
        assertEquals(0, writer.captures.get())
    }

    @Test fun capture_rejection_does_not_open_picker_or_commit() = runBlocking {
        val writer = Writer().apply { rejectCapture = true }
        val model = model(writer)
        onMain { model.bindProject(project.id); model.begin(project.id) }
        await(model, PhotoLiteCoverPhase.REJECTED)
        assertNull(model.state.value.pickerTicket)
        assertEquals(0, writer.commits.get())
    }

    @Test fun double_begin_and_cancelled_choice_do_not_write_or_persist_uri() = runBlocking {
        val writer = Writer()
        val saved = SavedStateHandle()
        val model = model(writer, saved)
        onMain { model.bindProject(project.id); model.begin(project.id); model.begin(project.id) }
        await(model, PhotoLiteCoverPhase.CHOOSING)
        val ticket = model.state.value.pickerTicket!!
        onMain { assertTrue(model.pickerWillLaunch(ticket)); model.pickerResult(ticket, null) }
        assertEquals(PhotoLiteCoverPhase.IDLE, model.state.value.phase)
        assertEquals(1, writer.captures.get())
        assertEquals(0, writer.commits.get())
        assertTrue(saved.keys().isEmpty())
    }

    @Test fun retired_picker_ticket_cannot_rebind_to_new_choice() = runBlocking {
        val writer = Writer()
        val model = model(writer)
        val old = choose(model)
        onMain { model.discard(); model.begin(project.id) }
        await(model, PhotoLiteCoverPhase.CHOOSING)
        val current = model.state.value.pickerTicket
        onMain { model.pickerResult(old, uri) }
        assertEquals(PhotoLiteCoverPhase.CHOOSING, model.state.value.phase)
        assertEquals(current, model.state.value.pickerTicket)
        assertEquals(0, writer.commits.get())
    }

    @Test fun changed_owner_rejects_choice_before_image_processing() = runBlocking {
        val writer = Writer()
        val model = model(writer)
        val ticket = choose(model)
        onMain { writer.current = false; model.pickerResult(ticket, uri) }
        assertEquals(PhotoLiteCoverPhase.REJECTED, model.state.value.phase)
        assertEquals(0, writer.commits.get())
    }

    @Test fun preview_cancel_releases_unpublished_raster_and_does_not_encode() = runBlocking {
        val writer = Writer()
        val preview = preview()
        val encodes = AtomicInteger()
        val model = model(writer, load = { preview }, encode = { encodes.incrementAndGet(); png() })
        val ticket = choose(model)
        onMain { model.pickerResult(ticket, uri) }
        await(model, PhotoLiteCoverPhase.PREVIEW)
        assertEquals(LeaveDecision.CONFIRM_DISCARD, model.leaveDecision())
        onMain { model.discard() }
        assertEquals(PhotoLiteCoverPhase.IDLE, model.state.value.phase)
        assertTrue(preview.bitmap.isRecycled)
        assertEquals(0, encodes.get())
        assertEquals(0, writer.commits.get())
    }

    @Test fun explicit_confirmation_is_single_commit_and_only_success_is_success() = runBlocking {
        val writer = Writer().apply { commitGate = CompletableDeferred() }
        val saved = SavedStateHandle()
        val model = model(writer, saved, load = { preview() }, encode = { png() })
        val ticket = choose(model)
        onMain { model.pickerResult(ticket, uri) }
        await(model, PhotoLiteCoverPhase.PREVIEW)
        onMain { model.confirm(); model.confirm(); model.discard() }
        await(model, PhotoLiteCoverPhase.SAVING)
        withTimeout(3_000) { while (writer.commits.get() == 0) delay(10) }
        assertEquals(1, writer.commits.get())
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
        assertTrue(saved.get<Bundle>(PhotoLiteCoverController.SAVED_KEY)!!.getBoolean(PhotoLiteCoverController.INCOMPLETE))
        writer.commitGate!!.complete(Unit)
        await(model, PhotoLiteCoverPhase.SUCCESS)
        assertFalse(saved.contains(PhotoLiteCoverController.SAVED_KEY))
    }

    @Test fun unknown_result_stays_visible_and_cannot_repeat_or_discard() = runBlocking {
        val writer = Writer().apply { result = PhotoLiteCommitResult.Unknown }
        val model = model(writer, load = { preview() }, encode = { png() })
        val ticket = choose(model)
        onMain { model.pickerResult(ticket, uri) }
        await(model, PhotoLiteCoverPhase.PREVIEW)
        onMain { model.confirm() }
        await(model, PhotoLiteCoverPhase.UNKNOWN)
        onMain { model.confirm(); model.discard(); model.begin(project.id) }
        assertEquals(PhotoLiteCoverPhase.UNKNOWN, model.state.value.phase)
        assertEquals(1, writer.captures.get())
        assertEquals(1, writer.commits.get())
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
    }

    @Test fun load_deadline_revokes_ticket_before_native_late_result() = runBlocking {
        val result = CompletableDeferred<PhotoLitePreview>()
        val writer = Writer()
        val late = preview()
        val model = model(writer, load = { withContext(NonCancellable) { result.await() } }, loadTimeoutMillis = 50)
        val ticket = choose(model)
        try {
            onMain { model.pickerResult(ticket, uri) }
            await(model, PhotoLiteCoverPhase.UNKNOWN)
            result.complete(late)
            withTimeout(3_000) { while (!late.bitmap.isRecycled) delay(10) }
            assertEquals(PhotoLiteCoverPhase.UNKNOWN, model.state.value.phase)
            assertNull(model.state.value.preview)
            assertEquals(0, writer.commits.get())
        } finally { result.complete(late) }
    }

    @Test fun save_deadline_keeps_unknown_marker_after_late_commit_success() = runBlocking {
        val writer = Writer().apply { commitGate = CompletableDeferred(); ignoreCommitCancellation = true }
        val saved = SavedStateHandle()
        val checked = png()
        val model = model(writer, saved, load = { preview() }, encode = { checked }, saveTimeoutMillis = 1_000)
        val ticket = choose(model)
        try {
            onMain { model.pickerResult(ticket, uri) }
            await(model, PhotoLiteCoverPhase.PREVIEW)
            onMain { model.confirm() }
            withTimeout(3_000) { while (writer.commits.get() == 0) delay(10) }
            await(model, PhotoLiteCoverPhase.UNKNOWN)
            val nonce = saved.get<Bundle>(PhotoLiteCoverController.SAVED_KEY)!!.getString(PhotoLiteCoverController.NONCE)
            writer.commitGate!!.complete(Unit)
            withTimeout(3_000) { writer.commitReturned.await() }
            onMain { model.confirm(); model.discard(); model.begin(project.id) }
            assertEquals(PhotoLiteCoverPhase.UNKNOWN, model.state.value.phase)
            val retained = saved.get<Bundle>(PhotoLiteCoverController.SAVED_KEY)!!
            assertTrue(retained.getBoolean(PhotoLiteCoverController.INCOMPLETE))
            assertEquals(nonce, retained.getString(PhotoLiteCoverController.NONCE))
            assertEquals(1, writer.commits.get())
        } finally { writer.commitGate!!.complete(Unit) }
    }

    @Test fun changed_owner_during_commit_cannot_clear_incomplete_marker() = runBlocking {
        val writer = Writer().apply { commitGate = CompletableDeferred() }
        val saved = SavedStateHandle()
        val checked = png()
        val model = model(writer, saved, load = { preview() }, encode = { checked })
        val ticket = choose(model)
        try {
            onMain { model.pickerResult(ticket, uri) }
            await(model, PhotoLiteCoverPhase.PREVIEW)
            onMain { model.confirm() }
            withTimeout(3_000) { while (writer.commits.get() == 0) delay(10) }
            onMain { writer.current = false }
            writer.commitGate!!.complete(Unit)
            await(model, PhotoLiteCoverPhase.UNKNOWN)
            assertTrue(saved.get<Bundle>(PhotoLiteCoverController.SAVED_KEY)!!.getBoolean(PhotoLiteCoverController.INCOMPLETE))
        } finally { writer.commitGate!!.complete(Unit) }
    }

    private suspend fun choose(model: PhotoLiteCoverController): String {
        onMain { model.bindProject(project.id); model.begin(project.id) }
        await(model, PhotoLiteCoverPhase.CHOOSING)
        return requireNotNull(model.state.value.pickerTicket).also { ticket -> onMain { assertTrue(model.pickerWillLaunch(ticket)) } }
    }

    private fun model(writer: Writer, saved: SavedStateHandle = SavedStateHandle(), available: Boolean = true,
        load: suspend (Uri) -> PhotoLitePreview = { error("No image read expected") },
        encode: suspend (PhotoLitePreview) -> PhotoLitePng = { error("No encoding expected") },
        loadTimeoutMillis: Long = 15_000, saveTimeoutMillis: Long = 30_000): PhotoLiteCoverController {
        lateinit var value: PhotoLiteCoverController
        onMain {
            value = PhotoLiteCoverController(writer, saved, { available }, load, encode, loadTimeoutMillis, saveTimeoutMillis)
            stores += ViewModelStore().also { it.put("photo", value) }
        }
        return value
    }

    private suspend fun await(model: PhotoLiteCoverController, phase: PhotoLiteCoverPhase) {
        withTimeout(3_000) { model.state.first { it.phase == phase } }
    }

    private fun preview(): PhotoLitePreview {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        return PhotoLitePreview(bitmap, requireNotNull(CoverPixelBudget(4096).reserve(bitmap.allocationByteCount.toLong())))
    }

    private fun png(): PhotoLitePng {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        return try { PhotoLitePng.checked(ByteArrayOutputStream().also { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }.toByteArray()) }
        finally { bitmap.recycle() }
    }

    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    private inner class Writer : PhotoLiteCoverWriter {
        override val generationId = "photolite-controller-test"
        override val ownerKey = Any()
        @Volatile var current = true
        var rejectCapture = false
        var result: PhotoLiteCommitResult? = null
        var commitGate: CompletableDeferred<Unit>? = null
        var ignoreCommitCancellation = false
        val commitReturned = CompletableDeferred<Unit>()
        val captures = AtomicInteger()
        val commits = AtomicInteger()
        override fun isCurrent() = current
        override suspend fun capture(projectId: String): PhotoLiteCaptureResult {
            captures.incrementAndGet()
            return if (rejectCapture) PhotoLiteCaptureResult.Rejected(PhotoLiteFailure.STALE_PROJECT)
                else PhotoLiteCaptureResult.Ready(PhotoLiteTarget(generationId, ownerKey, project))
        }
        override suspend fun commit(target: PhotoLiteTarget, png: PhotoLitePng): PhotoLiteCommitResult {
            commits.incrementAndGet()
            if (ignoreCommitCancellation) withContext(NonCancellable) { commitGate?.await() } else commitGate?.await()
            commitReturned.complete(Unit)
            return result ?: PhotoLiteCommitResult.Success(target.nonce)
        }
    }
}

/** Runtime refusal only: Gradle must already select this runner before any Application starts. */
internal fun assertPhotoLitePlainHost() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    assertTrue("Only the explicit disposable experiment may run these methods", BuildConfig.PHOTO_LITE_EXPERIMENT)
    assertEquals("com.cooper.lithic.photolite.debug", instrumentation.targetContext.packageName)
    assertEquals("com.cooper.lithic.photolite.debug.test", instrumentation.context.packageName)
    assertEquals("com.cooper.lithic.app.M13IsolatedRunner", instrumentation.javaClass.name)
    assertEquals(Application::class.java, instrumentation.targetContext.applicationContext.javaClass)
}
