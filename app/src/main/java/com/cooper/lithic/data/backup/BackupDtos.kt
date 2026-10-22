package com.cooper.lithic.data.backup

import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ReplacementEntity
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import kotlinx.serialization.Serializable
import com.cooper.lithic.data.settings.ReminderPreferences

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

data class BackupSnapshot(
    val data: BackupDataV2,
    val settings: BackupSettingsV1,
    val notificationReceipts: List<BackupNotificationReceiptV3> = emptyList(),
    val reminderPreferences: ReminderPreferences = ReminderPreferences(),
    val milestoneWatermarks: List<BackupMilestoneWatermarkV4> = emptyList(),
) {
    constructor(data:BackupDataV1,settings:BackupSettingsV1):this(data.upgrade(),settings)

    fun completeRecordCounts(): Map<String, Long> = data.recordCounts() +
        ("notification_receipts" to notificationReceipts.size.toLong()) + ("milestone_watermarks" to milestoneWatermarks.size.toLong())
    /** Exact frozen format-3 shape, solely for original archive/proof authentication. */
    internal fun legacyDataV3(): BackupDataV3 {
        require(milestoneWatermarks.isEmpty() && notificationReceipts.all { it.kind == "RENEWAL" }) {
            "Milestone facts cannot disappear behind a format-3 proof or archive"
        }
        return BackupDataV3(data.categories, data.projects, data.projectMoneyEvents,
            data.mediaAssets, data.subscriptions, data.subscriptionRenewals, data.subscriptionStateChanges, data.replacements,
            notificationReceipts).ordered()
    }
    internal fun currentData(): BackupDataV4 = BackupDataV4(data.categories, data.projects, data.projectMoneyEvents,
        data.mediaAssets, data.subscriptions, data.subscriptionRenewals, data.subscriptionStateChanges, data.replacements,
        notificationReceipts, milestoneWatermarks).ordered()
    internal fun currentSettings(): BackupSettingsV3 = BackupSettingsV3(settings.recordZoneId,
        reminderPreferences.renewalEnabled, reminderPreferences.milestoneEnabled, reminderPreferences.importCutoff?.toString())
}

/** No defaults on the wire: strict admission requires all fields, including explicit null cutoff. */
@Serializable
data class BackupSettingsV3(val recordZoneId: String, val renewalEnabled: Boolean, val milestoneEnabled: Boolean, val importCutoff: String?) {
    internal fun preferences(): ReminderPreferences = ReminderPreferences(renewalEnabled, milestoneEnabled,
        importCutoff?.let { value -> java.time.LocalDate.parse(value).also { require(it.toString() == value) } })
}

@Serializable
data class BackupNotificationReceiptV3(val logicalKey: String, val ownerId: String, val kind: String, val boundary: String, val publishedAt: String)

@Serializable
data class BackupDataV3(
    val categories: List<BackupCategoryV1>,
    val projects: List<BackupProjectV1>,
    val projectMoneyEvents: List<BackupProjectMoneyEventV2>,
    val mediaAssets: List<BackupMediaAssetV1>,
    val subscriptions: List<BackupSubscriptionV1>,
    val subscriptionRenewals: List<BackupSubscriptionRenewalV1>,
    val subscriptionStateChanges: List<BackupSubscriptionStateChangeV1>,
    val replacements: List<BackupReplacementV2>,
    val notificationReceipts: List<BackupNotificationReceiptV3>,
) {
    internal fun storedData(): BackupDataV2 = BackupDataV2(categories, projects, projectMoneyEvents, mediaAssets,
        subscriptions, subscriptionRenewals, subscriptionStateChanges, replacements).ordered()
    fun recordCounts(): Map<String, Long> = storedData().recordCounts() + ("notification_receipts" to notificationReceipts.size.toLong())
    internal fun ordered(): BackupDataV3 {
        val old = storedData()
        return copy(categories = old.categories, projects = old.projects, projectMoneyEvents = old.projectMoneyEvents,
            mediaAssets = old.mediaAssets, subscriptions = old.subscriptions, subscriptionRenewals = old.subscriptionRenewals,
            subscriptionStateChanges = old.subscriptionStateChanges, replacements = old.replacements,
            notificationReceipts = notificationReceipts.sortedBy { it.logicalKey })
    }
}

/** Mandatory wire fields; an absent mask/cutoff is never interpreted as a default. */
@Serializable
data class BackupMilestoneWatermarkV4(val ownerType: String, val ownerId: String, val cutoffOn: String, val suppressedMask: Int)

@Serializable
data class BackupDataV4(
    val categories: List<BackupCategoryV1>,
    val projects: List<BackupProjectV1>,
    val projectMoneyEvents: List<BackupProjectMoneyEventV2>,
    val mediaAssets: List<BackupMediaAssetV1>,
    val subscriptions: List<BackupSubscriptionV1>,
    val subscriptionRenewals: List<BackupSubscriptionRenewalV1>,
    val subscriptionStateChanges: List<BackupSubscriptionStateChangeV1>,
    val replacements: List<BackupReplacementV2>,
    val notificationReceipts: List<BackupNotificationReceiptV3>,
    val milestoneWatermarks: List<BackupMilestoneWatermarkV4>,
) {
    internal fun storedData(): BackupDataV2 = BackupDataV2(categories, projects, projectMoneyEvents, mediaAssets,
        subscriptions, subscriptionRenewals, subscriptionStateChanges, replacements).ordered()
    fun recordCounts(): Map<String, Long> = storedData().recordCounts() +
        ("notification_receipts" to notificationReceipts.size.toLong()) + ("milestone_watermarks" to milestoneWatermarks.size.toLong())
    internal fun ordered(): BackupDataV4 {
        val old = storedData()
        return copy(categories = old.categories, projects = old.projects, projectMoneyEvents = old.projectMoneyEvents,
            mediaAssets = old.mediaAssets, subscriptions = old.subscriptions, subscriptionRenewals = old.subscriptionRenewals,
            subscriptionStateChanges = old.subscriptionStateChanges, replacements = old.replacements,
            notificationReceipts = notificationReceipts.sortedBy { it.logicalKey },
            milestoneWatermarks = milestoneWatermarks.sortedWith(compareBy<BackupMilestoneWatermarkV4> { it.ownerType }.thenBy { it.ownerId }))
    }
}

/** Format-v2 stored facts. Nullable fields are still mandatory at strict wire admission. */
@Serializable
data class BackupDataV2(
    val categories:List<BackupCategoryV1>,
    val projects:List<BackupProjectV1>,
    val projectMoneyEvents:List<BackupProjectMoneyEventV2>,
    val mediaAssets:List<BackupMediaAssetV1>,
    val subscriptions:List<BackupSubscriptionV1>,
    val subscriptionRenewals:List<BackupSubscriptionRenewalV1>,
    val subscriptionStateChanges:List<BackupSubscriptionStateChangeV1>,
    val replacements:List<BackupReplacementV2>,
) {
    fun recordCounts():Map<String,Long> = linkedMapOf(
        "categories" to categories.size.toLong(),"long_term_projects" to projects.size.toLong(),
        "project_money_events" to projectMoneyEvents.size.toLong(),"local_media_assets" to mediaAssets.size.toLong(),
        "subscriptions" to subscriptions.size.toLong(),"subscription_renewals" to subscriptionRenewals.size.toLong(),
        "subscription_state_changes" to subscriptionStateChanges.size.toLong(),"replacements" to replacements.size.toLong())
    internal fun ordered():BackupDataV2 = copy(categories=categories.sortedBy { it.id },projects=projects.sortedBy { it.id },
        projectMoneyEvents=projectMoneyEvents.sortedBy { it.id },mediaAssets=mediaAssets.sortedBy { it.id },
        subscriptions=subscriptions.sortedBy { it.id },subscriptionRenewals=subscriptionRenewals.sortedBy { it.id },
        subscriptionStateChanges=subscriptionStateChanges.sortedBy { it.id },replacements=replacements.sortedBy { it.id })

    /** Exact old wire projection, solely for authenticating an existing M12 format-1 proof.
     * New facts may never disappear behind a legacy proof or archive. */
    internal fun legacyProjection():BackupDataV1 {
        require(replacements.isEmpty() && projectMoneyEvents.all { it.basis=="CASH" && it.exchangeGroupId==null }) {
            "New stored facts cannot be authenticated by a legacy restoration proof"
        }
        return BackupDataV1(categories,projects,projectMoneyEvents.map {
            BackupProjectMoneyEventV1(it.id,it.projectId,it.direction,it.amountCents,it.occurredOn,it.description,it.role,it.operationId)
        },mediaAssets,subscriptions,subscriptionRenewals,subscriptionStateChanges).ordered()
    }
}

internal fun BackupDataV1.upgrade():BackupDataV2 = BackupDataV2(categories,projects,projectMoneyEvents.map {
    BackupProjectMoneyEventV2(it.id,it.projectId,it.direction,it.amountCents,it.occurredOn,it.description,it.role,it.operationId,"CASH",null)
},mediaAssets,subscriptions,subscriptionRenewals,subscriptionStateChanges,emptyList()).ordered()

@Serializable
data class BackupProjectMoneyEventV2(val id:String,val projectId:String,val direction:String,val amountCents:Long,
    val occurredOn:String,val description:String?,val role:String,val operationId:String,val basis:String,val exchangeGroupId:String?)

@Serializable
data class BackupReplacementV2(val id:String,val predecessorId:String,val successorId:String,val createdAt:String)

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
        replacements:List<ReplacementEntity> = emptyList(),
    ): BackupDataV2 = BackupDataV2(
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
            BackupProjectMoneyEventV2(
                it.id, it.projectId, it.direction.name, it.amountCents, it.occurredOn.toString(),
                it.description, it.role.name, it.operationId,it.basis.name,it.exchangeGroupId,
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
        replacements=replacements.map { BackupReplacementV2(it.id,it.predecessorId,it.successorId,it.createdAt.toString()) },
    ).ordered()
}
