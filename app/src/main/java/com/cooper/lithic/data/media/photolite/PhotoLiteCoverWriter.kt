package com.cooper.lithic.data.media.photolite

import com.cooper.lithic.data.local.LongTermProjectEntity
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** A non-owning capability for this experimental writer's exact live storage owner. */
interface PhotoLiteCoverWriter {
    val generationId: String
    val ownerKey: Any
    fun isCurrent(): Boolean
    suspend fun capture(projectId: String): PhotoLiteCaptureResult
    suspend fun commit(target: PhotoLiteTarget, png: PhotoLitePng): PhotoLiteCommitResult
}

/** One complete loaded row, never a path, database handle, or cross-generation permission. */
class PhotoLiteTarget internal constructor(
    val generationId: String,
    val ownerKey: Any,
    internal val expected: LongTermProjectEntity,
    val nonce: String = UUID.randomUUID().toString(),
) {
    val projectId: String get() = expected.id
    private val claimed = AtomicBoolean(false)
    internal fun claim(): Boolean = claimed.compareAndSet(false, true)
}

sealed interface PhotoLiteCaptureResult {
    data class Ready(val target: PhotoLiteTarget) : PhotoLiteCaptureResult
    data class Rejected(val reason: PhotoLiteFailure) : PhotoLiteCaptureResult
}

sealed interface PhotoLiteCommitResult {
    data class Success(val assetId: String) : PhotoLiteCommitResult
    data class Rejected(val reason: PhotoLiteFailure) : PhotoLiteCommitResult
    /** Persistence or resource finality was not proved. Preserve the file and do not retry. */
    data object Unknown : PhotoLiteCommitResult
}

enum class PhotoLiteFailure {
    OWNER_UNAVAILABLE, STALE_PROJECT, ALREADY_HAS_COVER, INVALID_IMAGE,
    FILE_FAILED, STORAGE_FAILED, DUPLICATE,
}
