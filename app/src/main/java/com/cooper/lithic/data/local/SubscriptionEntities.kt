package com.cooper.lithic.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = LocalMediaAssetEntity::class, parentColumns = ["id"], childColumns = ["coverAssetId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("categoryId"), Index("coverAssetId")],
)
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String,
    val coverAssetId: String? = null,
    val note: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "subscription_renewals",
    foreignKeys = [ForeignKey(entity = SubscriptionEntity::class, parentColumns = ["id"], childColumns = ["subscriptionId"], onDelete = ForeignKey.RESTRICT)],
    indices = [
        Index(value = ["operationId"], unique = true),
        Index(value = ["subscriptionId", "paidOn", "id"]),
        Index(value = ["subscriptionId", "effectiveFrom", "id"]),
    ],
)
data class SubscriptionRenewalEntity(
    @PrimaryKey val id: String,
    val subscriptionId: String,
    val paidOn: LocalDate,
    val effectiveFrom: LocalDate,
    val paidUntilExclusive: LocalDate,
    val reminderDueOn: LocalDate,
    val amountCents: Long,
    val cycle: BillingCycle,
    val standardDays: Int,
    val operationId: String,
)

@Entity(
    tableName = "subscription_state_changes",
    foreignKeys = [ForeignKey(entity = SubscriptionEntity::class, parentColumns = ["id"], childColumns = ["subscriptionId"], onDelete = ForeignKey.RESTRICT)],
    indices = [
        Index(value = ["operationId"], unique = true),
        Index(value = ["subscriptionId", "effectiveOn"], unique = true),
        Index(value = ["subscriptionId", "effectiveOn", "id"]),
    ],
)
data class SubscriptionStateChangeEntity(
    @PrimaryKey val id: String,
    val subscriptionId: String,
    val effectiveOn: LocalDate,
    val state: SubscriptionOperationState,
    val reason: String? = null,
    val note: String? = null,
    val operationId: String,
)
