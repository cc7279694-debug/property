package com.cooper.lithic.data.media

import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.repository.SubscriptionFacts
import java.time.Instant

enum class MediaFailureKind {
    PATH, UNSUPPORTED, INVALID_IMAGE, TOO_LARGE, IO, DATABASE, CONFLICT, NOT_FOUND, IN_USE,
    RESOURCE_REJECTED, TIMEOUT, NATIVE_CRASH, LOAD_ERROR, RESULT_UNKNOWN,
}

sealed interface MediaResult<out T> {
    data class Success<T>(val value: T) : MediaResult<T>
    data class Failure(val kind: MediaFailureKind, val message: String) : MediaResult<Nothing>
}

data class PreparedImage(val asset: LocalMediaAssetEntity)
data class StoredImage(val asset: LocalMediaAssetEntity, val bytes: ByteArray)
enum class CoverTargetType { PROJECT, SUBSCRIPTION }
data class CoverTarget(val type: CoverTargetType, val id: String)

/** Complete persisted facts; timeline list order does not define identity. */
sealed interface CoverBaseline {
    data class Project(val project: LongTermProjectEntity, val events: List<ProjectMoneyEventEntity>) : CoverBaseline
    data class Subscription(val facts: SubscriptionFacts) : CoverBaseline

    val target: CoverTarget get() = when (this) {
        is Project -> CoverTarget(CoverTargetType.PROJECT, project.id)
        is Subscription -> CoverTarget(CoverTargetType.SUBSCRIPTION, facts.subscription.id)
    }
    val name: String get() = when (this) {
        is Project -> project.name
        is Subscription -> facts.subscription.name
    }
    val coverAssetId: String? get() = when (this) {
        is Project -> project.coverAssetId
        is Subscription -> facts.subscription.coverAssetId
    }
    val updatedAt: Instant get() = when (this) {
        is Project -> project.updatedAt
        is Subscription -> facts.subscription.updatedAt
    }
}

/** Fixed asset identity and revision also survive uncertain result delivery. */
data class CoverCommand(
    val operationId: String,
    val expected: CoverBaseline,
    val newAsset: LocalMediaAssetEntity?,
    val writtenAt: Instant,
)

data class CoverCommitOutcome(val baseline: CoverBaseline, val replayed: Boolean)
data class CleanupFailure(val assetId: String, val kind: MediaFailureKind, val message: String)
data class CleanupReport(
    val removedAssetIds: List<String>,
    val failures: List<CleanupFailure>,
    val skippedActiveAssetIds: List<String>,
)
