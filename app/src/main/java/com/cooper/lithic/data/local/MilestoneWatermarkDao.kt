package com.cooper.lithic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cooper.lithic.domain.reminder.MilestoneOwnerType

@Dao
interface MilestoneWatermarkDao {
    /** Inspect SQLite's actual values before Room can narrow a 64-bit mask to Int. */
    @Query("SELECT COUNT(*) FROM milestone_watermarks WHERE typeof(ownerType) != 'text' OR ownerType NOT IN ('PROJECT','SUBSCRIPTION') OR typeof(ownerId) != 'text' OR typeof(cutoffOn) != 'integer' OR typeof(suppressedMask) != 'integer' OR suppressedMask NOT BETWEEN 0 AND 63")
    suspend fun countMalformed(): Long
    @Query("SELECT * FROM milestone_watermarks WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun find(ownerType: MilestoneOwnerType, ownerId: String): MilestoneWatermarkEntity?
    @Query("SELECT * FROM milestone_watermarks ORDER BY ownerType,ownerId")
    suspend fun list(): List<MilestoneWatermarkEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: MilestoneWatermarkEntity)
    @Query("DELETE FROM milestone_watermarks WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun delete(ownerType: MilestoneOwnerType, ownerId: String)
}
