package com.cooper.lithic.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
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
    init {
        require(StoredValueValidation.isUuid(ownerId)) { "Invalid receipt owner" }
        // M21 has not introduced a milestone receipt format. Unknown kinds fail closed.
        require(kind == "RENEWAL") { "Unsupported notification receipt kind" }
        require(LocalDate.parse(boundary).toString() == boundary) { "Noncanonical notification boundary" }
        require(logicalKey == "$kind:$ownerId:$boundary") { "Receipt key differs from its facts" }
    }
}
