package com.cooper.lithic.data.backup

import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionIntervalRules
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Restore validates stored facts, not a new write against the importing device's current day. */
internal object BackupBusinessValidation {
    fun validate(snapshot: BackupSnapshot, manifest: BackupManifestV1, checkpoint: () -> Unit) {
        val data = snapshot.data
        ZoneId.of(snapshot.settings.recordZoneId)
        require(snapshot.settings.recordZoneId == manifest.recordZoneId) { "Record zones disagree" }
        require(data.recordCounts() == manifest.recordCounts) { "Record counts disagree" }
        require(data.recordCounts().values.fold(0L, Math::addExact) < BackupLimits.MAX_RECORDS)
        unique(data.categories.map { it.id }); unique(data.projects.map { it.id })
        unique(data.projectMoneyEvents.map { it.id }); unique(data.mediaAssets.map { it.id })
        unique(data.subscriptions.map { it.id }); unique(data.subscriptionRenewals.map { it.id })
        unique(data.subscriptionStateChanges.map { it.id })
        // These mirror the three actual Room unique indices. A reopening may share its
        // command UUID between a real payment and an ENABLED state in different tables.
        unique(data.projectMoneyEvents.map { it.operationId })
        unique(data.subscriptionRenewals.map { it.operationId })
        unique(data.subscriptionStateChanges.map { it.operationId })

        val categories = data.categories.associate { row ->
            checkpoint()
            row.id to CategoryEntity(row.id, CategoryType.valueOf(row.type), row.name, row.normalizedName, row.isFallback)
        }
        require(categories.values.map { it.type to it.normalizedName }.toSet().size == categories.size) { "Duplicate category comparison name" }
        val defaults = listOf(
            CategoryEntity(LithicDatabase.PROJECT_FALLBACK_ID, CategoryType.PROJECT, "其他", "其他", true),
            CategoryEntity(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, CategoryType.SUBSCRIPTION, "其他", "其他", true),
        )
        require(defaults.all { categories[it.id] == it } && categories.values.filter { it.isFallback }.toSet() == defaults.toSet()) {
            "Fixed fallback categories are missing or changed"
        }

        val files = manifest.files.associateBy { it.path }
        require(data.mediaAssets.map { it.relativePath }.toSet().size == data.mediaAssets.size) { "Duplicate media path" }
        data.mediaAssets.forEach { row ->
            checkpoint()
            LocalMediaAssetEntity(row.id, row.relativePath, row.mime, row.width, row.height, row.byteLength, row.sha256)
            require(row.mime in setOf("image/png", "image/jpeg") && row.width in 1..1600 && row.height in 1..1600)
            require(row.byteLength in 1 until BackupLimits.MAX_MEDIA_BYTES)
            val file = files[row.relativePath] ?: throw IllegalArgumentException("Missing registered media payload")
            require(file.byteLength == row.byteLength && file.sha256.equals(row.sha256, ignoreCase = true)) { "Media identity differs from manifest" }
        }
        require(files.keys == setOf("data.json", "settings.json") + data.mediaAssets.map { it.relativePath }) { "Unregistered media payload" }
        val mediaIds = data.mediaAssets.map { it.id }.toSet()
        val projects = data.projects.associateBy { it.id }
        val subscriptions = data.subscriptions.associateBy { it.id }
        data.projectMoneyEvents.forEach {
            checkpoint(); uuid(it.projectId)
            require(it.projectId in projects) { "Orphaned project money event" }
        }
        data.subscriptionRenewals.forEach {
            checkpoint(); uuid(it.subscriptionId)
            require(it.subscriptionId in subscriptions) { "Orphaned subscription renewal" }
        }
        data.subscriptionStateChanges.forEach {
            checkpoint(); uuid(it.subscriptionId)
            require(it.subscriptionId in subscriptions) { "Orphaned subscription state" }
        }
        val events = data.projectMoneyEvents.groupBy { it.projectId }
        val renewals = data.subscriptionRenewals.groupBy { it.subscriptionId }
        val states = data.subscriptionStateChanges.groupBy { it.subscriptionId }
        data.projects.forEach { row ->
            checkpoint()
            requireIdentity(row.name, row.categoryId, row.coverAssetId, row.createdAt, row.updatedAt, CategoryType.PROJECT, categories, mediaIds)
            val project = LongTermProject(date(row.startedOn), row.endedOn?.let(::date))
            require((row.endedOn == null) == (row.endReason == null) && (row.endReason == null || row.endReason.isNotBlank())) { "Invalid project end facts" }
            val rows = events[row.id].orEmpty()
            require(rows.count { it.role == ProjectMoneyEventRole.INITIAL.name } == 1) { "Project must retain its single initial outlay" }
            require(rows.count { it.role == ProjectMoneyEventRole.END_SETTLEMENT.name } <= 1) { "Ambiguous legacy end settlements" }
            val facts = rows.map { event ->
                checkpoint()
                val direction = MoneyDirection.valueOf(event.direction)
                when (ProjectMoneyEventRole.valueOf(event.role)) {
                    ProjectMoneyEventRole.INITIAL -> require(direction == MoneyDirection.OUTLAY)
                    ProjectMoneyEventRole.END_SETTLEMENT -> require(project.endedOn != null && direction == MoneyDirection.RECOVERY)
                    ProjectMoneyEventRole.REGULAR -> Unit
                }
                ProjectMoneyEvent(direction, Money(event.amountCents), date(event.occurredOn))
            }
            // All historical and post-end facts participate. No second implementation of
            // net costs or overflow handling is introduced by the import boundary.
            ProjectCostEngine.evaluate(ProjectSnapshot(project, facts), LocalDate.MAX)
        }
        data.subscriptions.forEach { row ->
            checkpoint()
            requireIdentity(row.name, row.categoryId, row.coverAssetId, row.createdAt, row.updatedAt, CategoryType.SUBSCRIPTION, categories, mediaIds)
            val paid = renewals[row.id].orEmpty()
            require(paid.isNotEmpty()) { "Subscription has no confirmed payment" }
            val input = SubscriptionSnapshot(Subscription(row.id), paid.map {
                checkpoint()
                Renewal(Money(it.amountCents), BillingCycle.valueOf(it.cycle), date(it.paidOn), date(it.effectiveFrom),
                    date(it.paidUntilExclusive), date(it.reminderDueOn), it.standardDays)
            }, states[row.id].orEmpty().map {
                checkpoint()
                SubscriptionStateChange(date(it.effectiveOn), SubscriptionOperationState.valueOf(it.state))
            })
            SubscriptionIntervalRules.validateStructure(input)
            SubscriptionCostEngine.evaluate(input, LocalDate.MAX)
        }
    }

    private fun requireIdentity(name: String, categoryId: String, coverId: String?, createdAt: String, updatedAt: String,
        type: CategoryType, categories: Map<String, CategoryEntity>, mediaIds: Set<String>) {
        uuid(categoryId); coverId?.let(::uuid)
        require(name.isNotBlank() && categories[categoryId]?.type == type) { "Invalid name or category type" }
        require(coverId == null || coverId in mediaIds) { "Missing cover metadata" }
        require(!instant(updatedAt).isBefore(instant(createdAt))) { "Update precedes creation" }
    }

    private fun unique(values: List<String>) {
        require(values.size == values.toSet().size) { "Duplicate database identity or operation" }
        values.forEach(::uuid)
    }
    private fun uuid(value: String) = StoredValueValidation.requireUuid(value)
    private fun date(value: String): LocalDate = LocalDate.parse(value).also { require(it.toString() == value) { "Noncanonical business date" } }
    internal fun instant(value: String): Instant = Instant.parse(value).also { require(it.toString() == value) { "Noncanonical instant" } }
}
