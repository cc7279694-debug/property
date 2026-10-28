package com.cooper.lithic.data.media

/** A reference is interpreted only in its reader's current generation and owner scope. */
enum class CoverOwnerType { PROJECT, SUBSCRIPTION }

data class CoverReference(val type: CoverOwnerType, val recordId: String, val assetId: String?)

interface CoverReader {
    val generationId: String
    val ownerKey: Any
    fun isCurrent(): Boolean
    suspend fun read(reference: CoverReference): CoverReadResult
}

sealed interface CoverReadResult {
    data object NoCover : CoverReadResult
    data class Ready(val image: VerifiedCoverImage) : CoverReadResult
    data class Unavailable(val reason: CoverReadFailure) : CoverReadResult
}

enum class CoverReadFailure {
    OWNER_UNAVAILABLE, RECORD_MISMATCH, MISSING_METADATA, MISSING_FILE,
    OWNERSHIP, INTEGRITY, FORMAT, READ_FAILED,
}

/** Captured registered bytes only: no private path, live FD, Bitmap or new-import certificate. */
class VerifiedCoverImage internal constructor(
    val assetId: String,
    val mime: String,
    val width: Int,
    val height: Int,
    val bytes: ByteArray,
)
