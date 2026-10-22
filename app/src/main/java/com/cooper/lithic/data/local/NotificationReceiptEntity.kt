package com.cooper.lithic.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cooper.lithic.domain.reminder.MilestoneOwnerType
import com.cooper.lithic.domain.reminder.MilestonePlanner
import java.time.Instant
import java.time.LocalDate

/** A successful platform publish call, not delivery or user acknowledgement. */
@Entity(tableName = "notification_receipts", indices = [Index("ownerId")])
data class NotificationReceiptEntity(
    @PrimaryKey val logicalKey: String,
    val ownerId: String,
    val kind: String,
    val boundary: String,
    val publishedAt: Instant,
) {
    init { NotificationReceiptIdentity.validate(logicalKey, ownerId, kind, boundary) }
}

/** The same typed key contract is used at Room, archive and runtime admission. */
object NotificationReceiptIdentity {
    fun validate(logicalKey: String, ownerId: String, kind: String, boundary: String) {
        StoredValueValidation.requireUuid(ownerId)
        if (kind == "RENEWAL") {
            require(LocalDate.parse(boundary).toString() == boundary) { "Noncanonical notification boundary" }
            require(logicalKey == "RENEWAL:$ownerId:$boundary") { "Receipt key differs from its facts" }
        } else {
            val type = milestoneOwnerType(kind)
                ?: throw IllegalArgumentException("Unsupported notification receipt kind")
            MilestonePlanner.validateNode(type, boundary)
            require(logicalKey == MilestonePlanner.logicalKey(type, ownerId, boundary)) { "Receipt key differs from its facts" }
        }
    }

    fun milestoneOwnerType(kind: String): MilestoneOwnerType? = when (kind) {
        "RENEWAL" -> null
        "PROJECT_MILESTONE" -> MilestoneOwnerType.PROJECT
        "SUBSCRIPTION_MILESTONE" -> MilestoneOwnerType.SUBSCRIPTION
        else -> throw IllegalArgumentException("Unsupported notification receipt kind")
    }
}
