package com.cooper.lithic.feature.settings

import com.cooper.lithic.data.media.PhotoStorageReader
import com.cooper.lithic.data.media.PhotoStorageReadFailure
import com.cooper.lithic.data.media.PhotoStorageReadResult
import com.cooper.lithic.data.media.PhotoStorageSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Presentation contains metadata facts only, never paths, file handles or physical-space claims. */
internal sealed interface PhotoStorageUiState {
    data object Idle : PhotoStorageUiState
    data object Loading : PhotoStorageUiState
    data class Ready(val summary: PhotoStorageSummary) : PhotoStorageUiState
    data class Unavailable(val reason: PhotoStorageReadFailure) : PhotoStorageUiState
}

/** One generation-owned read at a time. Cancelling a coroutine is not evidence that its IO ended. */
internal class PhotoStorageController(private val reader: PhotoStorageReader) {
    private val monitor = Any()
    private val boundOwner = reader.ownerKey
    private val boundGeneration = reader.generationId
    private var closed = false
    private var activeRequest: Any? = null
    private val mutableState = MutableStateFlow<PhotoStorageUiState>(PhotoStorageUiState.Idle)
    val state: StateFlow<PhotoStorageUiState> = mutableState.asStateFlow()

    suspend fun refresh() {
        val request = synchronized(monitor) {
            if (closed || activeRequest != null) return
            if (!ownerAvailable()) {
                mutableState.value = PhotoStorageUiState.Unavailable(PhotoStorageReadFailure.OWNER_UNAVAILABLE)
                return
            }
            Any().also {
                activeRequest = it
                // Old counts are not retained as if they were facts from this new request.
                mutableState.value = PhotoStorageUiState.Loading
            }
        }
        try {
            currentCoroutineContext().ensureActive()
            val result = reader.read()
            currentCoroutineContext().ensureActive()
            publish(request, result)
        } catch (cancelled: CancellationException) {
            publish(request, PhotoStorageReadResult.Unavailable(PhotoStorageReadFailure.READ_FAILED))
            throw cancelled
        } catch (_: Exception) {
            // The source exception may contain a private path; presentation keeps only a safe category.
            publish(request, PhotoStorageReadResult.Unavailable(PhotoStorageReadFailure.READ_FAILED))
        } finally {
            synchronized(monitor) {
                // A cancelled but still executing read holds its request until this actual completion.
                if (activeRequest === request) activeRequest = null
            }
        }
    }

    fun close() = synchronized(monitor) {
        closed = true
        mutableState.value = PhotoStorageUiState.Unavailable(PhotoStorageReadFailure.OWNER_UNAVAILABLE)
        // Do not re-open the source, auto-retry, or claim pending IO has already stopped.
    }

    private fun publish(request: Any, result: PhotoStorageReadResult) = synchronized(monitor) {
        if (closed || activeRequest !== request) return@synchronized
        mutableState.value = if (!ownerAvailable()) {
            PhotoStorageUiState.Unavailable(PhotoStorageReadFailure.OWNER_UNAVAILABLE)
        } else when (result) {
            is PhotoStorageReadResult.Ready -> PhotoStorageUiState.Ready(result.summary)
            is PhotoStorageReadResult.Unavailable -> PhotoStorageUiState.Unavailable(result.reason)
        }
    }

    private fun ownerAvailable(): Boolean = try {
        reader.ownerKey === boundOwner && reader.generationId == boundGeneration && reader.isCurrent()
    } catch (_: Exception) {
        false
    }
}
