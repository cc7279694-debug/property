package com.cooper.lithic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(asset: LocalMediaAssetEntity)
    @Query("SELECT * FROM local_media_assets ORDER BY id") suspend fun list(): List<LocalMediaAssetEntity>
    @Query("SELECT * FROM local_media_assets WHERE id = :id") suspend fun find(id: String): LocalMediaAssetEntity?
}
