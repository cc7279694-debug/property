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

    @Query("""
        SELECT EXISTS(SELECT 1 FROM long_term_projects WHERE coverAssetId = :id)
            OR EXISTS(SELECT 1 FROM subscriptions WHERE coverAssetId = :id)
    """)
    suspend fun isReferenced(id: String): Boolean

    /** Exact immutable metadata CAS and both business owners are checked inside the transaction. */
    @Query("""
        DELETE FROM local_media_assets WHERE id = :id AND relativePath = :relativePath
            AND mime = :mime AND width = :width AND height = :height
            AND byteLength = :byteLength AND sha256 = :sha256
            AND NOT EXISTS(SELECT 1 FROM long_term_projects WHERE coverAssetId = :id)
            AND NOT EXISTS(SELECT 1 FROM subscriptions WHERE coverAssetId = :id)
    """)
    suspend fun deleteIfUnreferenced(id: String, relativePath: String, mime: String,
        width: Int, height: Int, byteLength: Long, sha256: String): Int
}
