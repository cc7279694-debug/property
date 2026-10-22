package com.cooper.lithic.data.local

import androidx.room.Entity
import com.cooper.lithic.domain.reminder.MilestoneOwnerType
import com.cooper.lithic.domain.reminder.MilestoneWatermark
import java.time.LocalDate

/** Suppression provenance, never proof of a platform notification publication. */
@Entity(tableName = "milestone_watermarks", primaryKeys = ["ownerType", "ownerId"])
data class MilestoneWatermarkEntity(
    val ownerType: MilestoneOwnerType,
    val ownerId: String,
    val cutoffOn: LocalDate,
    val suppressedMask: Int,
) {
    init {
        StoredValueValidation.requireUuid(ownerId)
        require(suppressedMask in 0..63) { "Unknown milestone suppression bits" }
    }
    fun domain(): MilestoneWatermark = MilestoneWatermark(ownerType, ownerId, cutoffOn, suppressedMask)
}
