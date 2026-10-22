package com.cooper.lithic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cooper.lithic.domain.reminder.MilestoneOwnerType

@Dao
interface NotificationReceiptDao {
    @Query("SELECT * FROM notification_receipts WHERE logicalKey = :logicalKey")
    suspend fun find(logicalKey: String): NotificationReceiptEntity?
    @Query("SELECT * FROM notification_receipts ORDER BY logicalKey")
    suspend fun list(): List<NotificationReceiptEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(value: NotificationReceiptEntity)
    @Query("DELETE FROM notification_receipts WHERE ownerId = :ownerId AND ((:ownerType = 'PROJECT' AND kind = 'PROJECT_MILESTONE') OR (:ownerType = 'SUBSCRIPTION' AND kind = 'SUBSCRIPTION_MILESTONE'))")
    suspend fun deleteMilestones(ownerType: MilestoneOwnerType, ownerId: String)
}
