package com.cooper.lithic.data.media.photolite

import com.cooper.lithic.data.local.LongTermProjectEntity
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** A non-owning capability for this experimental writer's exact live storage owner. */
interface PhotoLiteCoverWriter {
    val generationId: String
    val ownerKey: Any
    fun isCurrent(): Boolean
    suspend fun capture(projectId: String, change: PhotoLiteCoverChange = PhotoLiteCoverChange.ADD): PhotoLiteCaptureResult
    suspend fun commit(target: PhotoLiteTarget, png: PhotoLitePng): PhotoLiteCommitResult
    suspend fun remove(target: PhotoLiteTarget): PhotoLiteCommitResult
}

/** Cover-reference intent only; never authorizes physical media deletion or garbage collection. */
enum class PhotoLiteCoverChange {
    ADD, REPLACE, REMOVE;

    fun admissionFailure(coverAssetId: String?): PhotoLiteFailure? = when (this) {
        ADD -> if (coverAssetId == null) null else PhotoLiteFailure.ALREADY_HAS_COVER
        REPLACE, REMOVE -> if (coverAssetId == null) PhotoLiteFailure.NO_COVER else null
    }
}

/** One complete loaded row, never a path, database handle, or cross-generation permission. */
class PhotoLiteTarget internal constructor(
    val generationId: String,
    val ownerKey: Any,
    internal val expected: LongTermProjectEntity,
    val nonce: String = UUID.randomUUID().toString(),
    val change: PhotoLiteCoverChange = PhotoLiteCoverChange.ADD,
) {
    val projectId: String get() = expected.id
    val originalCoverAssetId: String? get() = expected.coverAssetId
    private val claimed = AtomicBoolean(false)
    internal fun claim(): Boolean = claimed.compareAndSet(false, true)
    internal fun matchesProject(current: LongTermProjectEntity?): Boolean =
        current == expected && change.admissionFailure(expected.coverAssetId) == null
}

sealed interface PhotoLiteCaptureResult {
    data class Ready(val target: PhotoLiteTarget) : PhotoLiteCaptureResult
    data class Rejected(val reason: PhotoLiteFailure) : PhotoLiteCaptureResult
}

sealed interface PhotoLiteCommitResult {
    data class Success(val assetId: String) : PhotoLiteCommitResult
    /** Only the cover reference was cleared. Media metadata/files and existing backups remain. */
    data object Cleared : PhotoLiteCommitResult
    data class Rejected(val reason: PhotoLiteFailure) : PhotoLiteCommitResult
    /** Persistence or resource finality was not proved. Preserve the file and do not retry. */
    data object Unknown : PhotoLiteCommitResult
}

enum class PhotoLiteFailure {
    OWNER_UNAVAILABLE, STALE_PROJECT, ALREADY_HAS_COVER, INVALID_IMAGE,
    FILE_FAILED, STORAGE_FAILED, DUPLICATE, NO_COVER, INVALID_OPERATION,
}
