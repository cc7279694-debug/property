package com.cooper.lithic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.Instant
import java.time.LocalDate

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertProject(project: LongTermProjectEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertEvent(event: ProjectMoneyEventEntity)
    @Query("SELECT * FROM long_term_projects ORDER BY startedOn, id") suspend fun listProjects(): List<LongTermProjectEntity>
    @Query("SELECT * FROM project_money_events ORDER BY projectId, occurredOn, id") suspend fun listEvents(): List<ProjectMoneyEventEntity>
    @Query("SELECT * FROM long_term_projects WHERE id = :id") suspend fun findProject(id: String): LongTermProjectEntity?
    @Query("SELECT * FROM project_money_events WHERE operationId = :operationId") suspend fun findEventByOperation(operationId: String): ProjectMoneyEventEntity?

    @Query("SELECT COUNT(*) FROM long_term_projects") suspend fun countProjects(): Long

    @Query("""
        SELECT * FROM long_term_projects
        WHERE :afterStartedOn IS NULL OR startedOn > :afterStartedOn
            OR (startedOn = :afterStartedOn AND id > :afterId)
        ORDER BY startedOn, id LIMIT :limit
    """)
    suspend fun pageProjects(afterStartedOn: LocalDate?, afterId: String?, limit: Int): List<LongTermProjectEntity>

    @Query("SELECT * FROM project_money_events WHERE projectId IN (:projectIds) ORDER BY projectId, occurredOn, id")
    suspend fun eventsForProjects(projectIds: List<String>): List<ProjectMoneyEventEntity>

    @Query("SELECT * FROM categories WHERE id IN (:categoryIds) ORDER BY normalizedName, id")
    suspend fun categoriesForProjects(categoryIds: List<String>): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE type = 'PROJECT' ORDER BY normalizedName, id")
    suspend fun projectCategories(): List<CategoryEntity>

    /** Only mutable M06 metadata is written; protected identity/lifecycle/media fields stay intact. */
    @Query("""
        UPDATE long_term_projects
        SET name = :name, startedOn = :startedOn, categoryId = :categoryId,
            note = :note, updatedAt = :updatedAt
        WHERE id = :id AND updatedAt = :expectedUpdatedAt
    """)
    suspend fun updateProjectMetadata(
        id: String,
        expectedUpdatedAt: Instant,
        name: String,
        startedOn: LocalDate,
        categoryId: String,
        note: String?,
        updatedAt: Instant,
    ): Int
}
