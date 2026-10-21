package com.cooper.lithic.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

data class CategoryUsageRow(@Embedded val category: CategoryEntity, val recordCount: Long)

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(category: CategoryEntity)
    @Query("SELECT * FROM categories ORDER BY type, normalizedName, id") suspend fun list(): List<CategoryEntity>
    @Query("SELECT * FROM categories WHERE id = :id") suspend fun find(id: String): CategoryEntity?

    @Query("""
        SELECT c.*, CASE WHEN c.type='PROJECT'
            THEN (SELECT COUNT(*) FROM long_term_projects p WHERE p.categoryId=c.id)
            ELSE (SELECT COUNT(*) FROM subscriptions s WHERE s.categoryId=c.id)
            END AS recordCount
        FROM categories c WHERE c.type=:type ORDER BY c.normalizedName,c.id
    """)
    suspend fun listWithUsage(type: CategoryType): List<CategoryUsageRow>

    @Query("SELECT * FROM categories WHERE type=:type AND normalizedName=:normalizedName")
    suspend fun findByName(type: CategoryType, normalizedName: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM long_term_projects WHERE categoryId=:id")
    suspend fun projectUsage(id: String): Long

    @Query("SELECT COUNT(*) FROM subscriptions WHERE categoryId=:id")
    suspend fun subscriptionUsage(id: String): Long

    @Query("""
        UPDATE categories SET name=:newName,normalizedName=:newNormalizedName
        WHERE id=:id AND type=:type AND name=:expectedName
            AND normalizedName=:expectedNormalizedName AND isFallback=0
    """)
    suspend fun renameIfUnchanged(id: String, type: CategoryType, expectedName: String,
        expectedNormalizedName: String, newName: String, newNormalizedName: String): Int

    @Query("UPDATE long_term_projects SET categoryId=:targetId WHERE categoryId=:sourceId")
    suspend fun moveProjectReferences(sourceId: String, targetId: String): Int

    @Query("UPDATE subscriptions SET categoryId=:targetId WHERE categoryId=:sourceId")
    suspend fun moveSubscriptionReferences(sourceId: String, targetId: String): Int

    @Query("""
        DELETE FROM categories WHERE id=:id AND type=:type AND name=:expectedName
            AND normalizedName=:expectedNormalizedName AND isFallback=0
    """)
    suspend fun deleteIfUnchanged(id: String, type: CategoryType,
        expectedName: String, expectedNormalizedName: String): Int
}
