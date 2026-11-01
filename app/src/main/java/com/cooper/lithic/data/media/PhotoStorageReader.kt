package com.cooper.lithic.data.media

/** Counts and a metadata byte estimate only; never a physical disk-usage or media-integrity receipt. */
data class PhotoStorageSummary(
    val registeredCount: Long,
    val referencedCount: Long,
    val historicalCount: Long,
    val estimatedRegisteredBytes: Long,
)

enum class PhotoStorageReadFailure { OWNER_UNAVAILABLE, INCONSISTENT_METADATA, READ_FAILED }

sealed interface PhotoStorageReadResult {
    data class Ready(val summary: PhotoStorageSummary) : PhotoStorageReadResult
    data class Unavailable(val reason: PhotoStorageReadFailure) : PhotoStorageReadResult
}

/** Non-owning, read-only capability. Identity is per Reader/Owner instance, not merely a generation ID. */
interface PhotoStorageReader {
    val generationId: String
    val ownerKey: Any
    fun isCurrent(): Boolean
    suspend fun read(): PhotoStorageReadResult
}

/** Validates one coherent set of integer aggregates; no paths or media bytes enter the UI model. */
internal fun photoStorageSummary(
    registeredCount: Long,
    referencedCount: Long,
    estimatedRegisteredBytes: Long,
    missingReferences: Long = 0,
    invalidLengths: Long = 0,
): PhotoStorageSummary {
    require(registeredCount >= 0 && referencedCount >= 0 && estimatedRegisteredBytes >= 0) {
        "Photo metadata aggregates must be nonnegative integers"
    }
    require(referencedCount <= registeredCount && missingReferences == 0L && invalidLengths == 0L) {
        "Photo metadata references or lengths are inconsistent"
    }
    return PhotoStorageSummary(registeredCount, referencedCount,
        Math.subtractExact(registeredCount, referencedCount), estimatedRegisteredBytes)
}
