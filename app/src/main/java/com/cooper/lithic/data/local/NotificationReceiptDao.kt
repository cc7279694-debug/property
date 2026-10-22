package com.cooper.lithic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationReceiptDao {
    @Query("SELECT * FROM notification_receipts WHERE logicalKey = :logicalKey")
    suspend fun find(logicalKey: String): NotificationReceiptEntity?
    @Query("SELECT * FROM notification_receipts ORDER BY logicalKey")
    suspend fun list(): List<NotificationReceiptEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(value: NotificationReceiptEntity)
}
