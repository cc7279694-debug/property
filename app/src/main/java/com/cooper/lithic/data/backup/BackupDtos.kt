package com.cooper.lithic.data.backup

import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import kotlinx.serialization.Serializable

/** Format v1 is independent of Room's schema number and contains stored facts only. */
@Serializable
data class BackupDataV1(
    val categories: List<BackupCategoryV1>,
    val projects: List<BackupProjectV1>,
    val projectMoneyEvents: List<BackupProjectMoneyEventV1>,
    val mediaAssets: List<BackupMediaAssetV1>,
    val subscriptions: List<BackupSubscriptionV1>,
    val subscriptionRenewals: List<BackupSubscriptionRenewalV1>,
    val subscriptionStateChanges: List<BackupSubscriptionStateChangeV1>,
) {
    fun recordCounts(): Map<String, Long> = linkedMapOf(
        "categories" to categories.size.toLong(),
        "long_term_projects" to projects.size.toLong(),
        "project_money_events" to projectMoneyEvents.size.toLong(),
        "local_media_assets" to mediaAssets.size.toLong(),
        "subscriptions" to subscriptions.size.toLong(),
        "subscription_renewals" to subscriptionRenewals.size.toLong(),
        "subscription_state_changes" to subscriptionStateChanges.size.toLong(),
    )

    /** Stable wire order does not depend on SQLite's unspecified row order. */
    internal fun ordered(): BackupDataV1 = copy(
        categories = categories.sortedBy { it.id },
        projects = projects.sortedBy { it.id },
        projectMoneyEvents = projectMoneyEvents.sortedBy { it.id },
        mediaAssets = mediaAssets.sortedBy { it.id },
        subscriptions = subscriptions.sortedBy { it.id },
        subscriptionRenewals = subscriptionRenewals.sortedBy { it.id },
        subscriptionStateChanges = subscriptionStateChanges.sortedBy { it.id },
    )
}

/** Whitelist of the only application preference implemented at the frozen M09 baseline. */
@Serializable
data class BackupSettingsV1(val recordZoneId: String)

data class BackupSnapshot(val data: BackupDataV1, val settings: BackupSettingsV1)

@Serializable
data class BackupCategoryV1(
    val id: String,
    val type: String,
    val name: String,
    val normalizedName: String,
    val isFallback: Boolean,
)

@Serializable
data class BackupProjectV1(
    val id: String,
    val name: String,
    val categoryId: String,
    val startedOn: String,
    val endedOn: String?,
    val endReason: String?,
    val coverAssetId: String?,
    val note: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class BackupProjectMoneyEventV1(
    val id: String,
    val projectId: String,
    val direction: String,
    val amountCents: Long,
    val occurredOn: String,
    val description: String?,
    val role: String,
    val operationId: String,
)

@Serializable
data class BackupMediaAssetV1(
    val id: String,
    val relativePath: String,
    val mime: String,
    val width: Int,
    val height: Int,
    val byteLength: Long,
    val sha256: String,
) {
    init {
        // Preserve all media names accepted by schema v2; do not apply unfinished M10 rules.
        require(StoredValueValidation.isMediaPath(relativePath)) {
            "Backup media paths must remain relative to the media directory"
        }
    }
}

@Serializable
data class BackupSubscriptionV1(
    val id: String,
    val name: String,
    val categoryId: String,
    val coverAssetId: String?,
    val note: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class BackupSubscriptionRenewalV1(
    val id: String,
    val subscriptionId: String,
    val paidOn: String,
    val effectiveFrom: String,
    val paidUntilExclusive: String,
    val reminderDueOn: String,
    val amountCents: Long,
    val cycle: String,
    val standardDays: Int,
    val operationId: String,
)

@Serializable
data class BackupSubscriptionStateChangeV1(
    val id: String,
    val subscriptionId: String,
    val effectiveOn: String,
    val state: String,
    val reason: String?,
    val note: String?,
    val operationId: String,
)

/** Every schema v2 column is copied; directions and integer cents are never netted or rounded. */
object BackupDtoMapper {
    fun fromRows(
        categories: List<CategoryEntity>,
        projects: List<LongTermProjectEntity>,
        events: List<ProjectMoneyEventEntity>,
        media: List<LocalMediaAssetEntity>,
        subscriptions: List<SubscriptionEntity>,
        renewals: List<SubscriptionRenewalEntity>,
        states: List<SubscriptionStateChangeEntity>,
    ): BackupDataV1 = BackupDataV1(
        categories = categories.map {
            BackupCategoryV1(it.id, it.type.name, it.name, it.normalizedName, it.isFallback)
        },
        projects = projects.map {
            BackupProjectV1(
                it.id, it.name, it.categoryId, it.startedOn.toString(), it.endedOn?.toString(),
                it.endReason, it.coverAssetId, it.note, it.createdAt.toString(), it.updatedAt.toString(),
            )
        },
        projectMoneyEvents = events.map {
            BackupProjectMoneyEventV1(
                it.id, it.projectId, it.direction.name, it.amountCents, it.occurredOn.toString(),
                it.description, it.role.name, it.operationId,
            )
        },
        mediaAssets = media.map {
            BackupMediaAssetV1(it.id, it.relativePath, it.mime, it.width, it.height, it.byteLength, it.sha256)
        },
        subscriptions = subscriptions.map {
            BackupSubscriptionV1(it.id, it.name, it.categoryId, it.coverAssetId, it.note,
                it.createdAt.toString(), it.updatedAt.toString())
        },
        subscriptionRenewals = renewals.map {
            BackupSubscriptionRenewalV1(
                it.id, it.subscriptionId, it.paidOn.toString(), it.effectiveFrom.toString(),
                it.paidUntilExclusive.toString(), it.reminderDueOn.toString(), it.amountCents,
                it.cycle.name, it.standardDays, it.operationId,
            )
        },
        subscriptionStateChanges = states.map {
            BackupSubscriptionStateChangeV1(
                it.id, it.subscriptionId, it.effectiveOn.toString(), it.state.name,
                it.reason, it.note, it.operationId,
            )
        },
    ).ordered()
}
