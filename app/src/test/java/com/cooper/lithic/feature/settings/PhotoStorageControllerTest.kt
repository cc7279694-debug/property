package com.cooper.lithic.feature.settings

import com.cooper.lithic.data.media.PhotoStorageReadFailure
import com.cooper.lithic.data.media.PhotoStorageReadResult
import com.cooper.lithic.data.media.PhotoStorageReader
import com.cooper.lithic.data.media.PhotoStorageSummary
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PhotoStorageControllerTest {
    private val facts = PhotoStorageSummary(3, 1, 2, 600, 350)

    @Test fun idle_does_not_read_or_invent_a_zero_count() {
        val reader = Reader { PhotoStorageReadResult.Ready(facts) }
        val controller = PhotoStorageController(reader)
        assertEquals(PhotoStorageUiState.Idle, controller.state.value)
        assertEquals(0, reader.calls)
    }

    @Test fun only_a_current_success_publishes_the_metadata_summary() = runBlocking {
        val reader = Reader { PhotoStorageReadResult.Ready(facts) }
        val controller = PhotoStorageController(reader)
        controller.refresh()
        assertEquals(1, reader.calls)
        assertEquals(PhotoStorageUiState.Ready(facts), controller.state.value)
    }

    @Test fun unavailable_is_not_rendered_as_zero_or_as_old_success() = runBlocking {
        val reader = Reader { PhotoStorageReadResult.Unavailable(PhotoStorageReadFailure.INCONSISTENT_METADATA) }
        val controller = PhotoStorageController(reader)
        controller.refresh()
        assertEquals(PhotoStorageUiState.Unavailable(PhotoStorageReadFailure.INCONSISTENT_METADATA), controller.state.value)
    }

    @Test fun source_exception_has_no_private_path_or_exception_text_in_presentation() = runBlocking {
        val secret = "/data/user/0/private.app/files/generation/media/secret.png"
        val reader = Reader { throw IOException(secret) }
        val controller = PhotoStorageController(reader)
        controller.refresh()
        assertEquals(PhotoStorageUiState.Unavailable(PhotoStorageReadFailure.READ_FAILED), controller.state.value)
        assertFalse(controller.state.value.toString().contains(secret))
    }

    @Test fun unavailable_owner_is_rejected_before_any_source_read() = runBlocking {
        val reader = Reader { PhotoStorageReadResult.Ready(facts) }.apply { current = false }
        val controller = PhotoStorageController(reader)
        controller.refresh()
        assertEquals(0, reader.calls)
        assertEquals(PhotoStorageUiState.Unavailable(PhotoStorageReadFailure.OWNER_UNAVAILABLE), controller.state.value)
    }

    @Test fun busy_refresh_rejects_repeated_requests_and_has_stable_loading_state() = runBlocking {
        val gate = CompletableDeferred<PhotoStorageReadResult>()
        val reader = Reader { gate.await() }
        val controller = PhotoStorageController(reader)
        val pending = async(start = CoroutineStart.UNDISPATCHED) { controller.refresh() }
        val repeated = async(start = CoroutineStart.UNDISPATCHED) { controller.refresh() }
        try {
            assertEquals(PhotoStorageUiState.Loading, controller.state.value)
            assertEquals(1, reader.calls)
        } finally { gate.complete(PhotoStorageReadResult.Ready(facts)) }
        pending.await()
        repeated.await()
        assertEquals(PhotoStorageUiState.Ready(facts), controller.state.value)
    }

    @Test fun late_result_after_owner_retirement_cannot_publish_numbers() = runBlocking {
        val gate = CompletableDeferred<PhotoStorageReadResult>()
        val reader = Reader { gate.await() }
        val controller = PhotoStorageController(reader)
        val pending = async(start = CoroutineStart.UNDISPATCHED) { controller.refresh() }
        reader.current = false
        gate.complete(PhotoStorageReadResult.Ready(facts))
        pending.await()
        assertEquals(PhotoStorageUiState.Unavailable(PhotoStorageReadFailure.OWNER_UNAVAILABLE), controller.state.value)
    }

    @Test fun same_generation_id_does_not_allow_a_replaced_owner_identity() = runBlocking {
        val gate = CompletableDeferred<PhotoStorageReadResult>()
        val reader = Reader { gate.await() }
        val controller = PhotoStorageController(reader)
        val pending = async(start = CoroutineStart.UNDISPATCHED) { controller.refresh() }
        reader.ownerKey = Any()
        gate.complete(PhotoStorageReadResult.Ready(facts))
        pending.await()
        assertEquals(PhotoStorageUiState.Unavailable(PhotoStorageReadFailure.OWNER_UNAVAILABLE), controller.state.value)
    }

    @Test fun cancellation_is_unknown_without_a_success_or_automatic_retry() = runBlocking {
        val gate = CompletableDeferred<PhotoStorageReadResult>()
        val reader = Reader { gate.await() }
        val controller = PhotoStorageController(reader)
        val pending = async(start = CoroutineStart.UNDISPATCHED) { controller.refresh() }
        pending.cancelAndJoin()
        assertEquals(1, reader.calls)
        assertEquals(PhotoStorageUiState.Unavailable(PhotoStorageReadFailure.READ_FAILED), controller.state.value)
    }

    @Test fun cancellation_does_not_release_busy_until_an_uncancellable_read_finishes() = runBlocking {
        val gate = CompletableDeferred<PhotoStorageReadResult>()
        val reader = Reader { withContext(NonCancellable) { gate.await() } }
        val controller = PhotoStorageController(reader)
        val pending = async(start = CoroutineStart.UNDISPATCHED) { controller.refresh() }
        pending.cancel()
        val repeated = async(start = CoroutineStart.UNDISPATCHED) { controller.refresh() }
        try {
            assertEquals(1, reader.calls)
            assertEquals(PhotoStorageUiState.Loading, controller.state.value)
        } finally { gate.complete(PhotoStorageReadResult.Ready(facts)) }
        pending.join()
        repeated.await()
        assertEquals(PhotoStorageUiState.Unavailable(PhotoStorageReadFailure.READ_FAILED), controller.state.value)
    }

    @Test fun clearing_the_model_fences_late_results_and_future_refreshes() = runBlocking {
        val gate = CompletableDeferred<PhotoStorageReadResult>()
        val reader = Reader { withContext(NonCancellable) { gate.await() } }
        val controller = PhotoStorageController(reader)
        val pending = async(start = CoroutineStart.UNDISPATCHED) { controller.refresh() }
        controller.close()
        gate.complete(PhotoStorageReadResult.Ready(facts))
        pending.await()
        controller.refresh()
        assertEquals(1, reader.calls)
        assertEquals(PhotoStorageUiState.Unavailable(PhotoStorageReadFailure.OWNER_UNAVAILABLE), controller.state.value)
    }

    private class Reader(private val block: suspend () -> PhotoStorageReadResult) : PhotoStorageReader {
        override val generationId = "synthetic-owner"
        override var ownerKey: Any = Any()
        var current = true
        var calls = 0
        override fun isCurrent() = current
        override suspend fun read(): PhotoStorageReadResult {
            calls++
            return block()
        }
    }
}
