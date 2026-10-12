package com.cooper.lithic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(category: CategoryEntity)
    @Query("SELECT * FROM categories ORDER BY type, normalizedName, id") suspend fun list(): List<CategoryEntity>
    @Query("SELECT * FROM categories WHERE id = :id") suspend fun find(id: String): CategoryEntity?
}
