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
    @Query("SELECT * FROM project_money_events WHERE id = :id") suspend fun findEvent(id: String): ProjectMoneyEventEntity?
    @Query("SELECT * FROM project_money_events WHERE projectId = :projectId AND role = 'END_SETTLEMENT' ORDER BY id LIMIT 2")
    suspend fun endSettlementsForProject(projectId: String): List<ProjectMoneyEventEntity>

    @Query("SELECT COUNT(*) FROM long_term_projects") suspend fun countProjects(): Long
    @Query("SELECT COUNT(*) FROM project_money_events e WHERE NOT EXISTS (SELECT 1 FROM long_term_projects p WHERE p.id=e.projectId)")
    suspend fun countOrphanEvents(): Long
    @Query("SELECT COUNT(*) FROM long_term_projects WHERE endedOn IS NULL OR endedOn > :asOf")
    suspend fun countCurrentProjects(asOf: LocalDate): Long
    @Query("SELECT COUNT(*) FROM long_term_projects WHERE endedOn IS NOT NULL AND endedOn <= :asOf")
    suspend fun countHistoryProjects(asOf: LocalDate): Long

    @Query("""
        SELECT * FROM long_term_projects
        WHERE (endedOn IS NULL OR endedOn > :asOf)
            AND (:afterStartedOn IS NULL OR startedOn > :afterStartedOn
                OR (startedOn = :afterStartedOn AND id > :afterId))
        ORDER BY startedOn, id LIMIT :limit
    """)
    suspend fun pageCurrentProjects(asOf: LocalDate, afterStartedOn: LocalDate?, afterId: String?, limit: Int): List<LongTermProjectEntity>

    @Query("""
        SELECT * FROM long_term_projects
        WHERE endedOn IS NOT NULL AND endedOn <= :asOf
            AND (:afterStartedOn IS NULL OR startedOn > :afterStartedOn
                OR (startedOn = :afterStartedOn AND id > :afterId))
        ORDER BY startedOn, id LIMIT :limit
    """)
    suspend fun pageHistoryProjects(asOf: LocalDate, afterStartedOn: LocalDate?, afterId: String?, limit: Int): List<LongTermProjectEntity>

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

    /** Complete loaded-row CAS, including nullable values and immutable creation/media identity. */
    @Query("""
        UPDATE long_term_projects SET name = :name, startedOn = :startedOn,
            categoryId = :categoryId, note = :note, endedOn = :endedOn,
            endReason = :endReason, updatedAt = :updatedAt
        WHERE id = :id AND name = :expectedName AND categoryId = :expectedCategoryId
            AND startedOn = :expectedStartedOn AND endedOn IS :expectedEndedOn
            AND endReason IS :expectedEndReason AND coverAssetId IS :expectedCoverAssetId
            AND note IS :expectedNote AND createdAt = :expectedCreatedAt
            AND updatedAt = :expectedUpdatedAt
    """)
    suspend fun updateProjectIfUnchanged(
        id: String, expectedName: String, expectedCategoryId: String, expectedStartedOn: LocalDate,
        expectedEndedOn: LocalDate?, expectedEndReason: String?, expectedCoverAssetId: String?,
        expectedNote: String?, expectedCreatedAt: Instant, expectedUpdatedAt: Instant,
        name: String, categoryId: String, startedOn: LocalDate, endedOn: LocalDate?,
        endReason: String?, note: String?, updatedAt: Instant,
    ): Int

    @Query("""
        UPDATE project_money_events SET amountCents = :amountCents,
            occurredOn = :occurredOn, description = :description
        WHERE id = :id AND projectId = :projectId AND direction = :direction
            AND amountCents = :expectedAmountCents AND occurredOn = :expectedOccurredOn
            AND description IS :expectedDescription AND role = :role AND operationId = :operationId
    """)
    suspend fun updateEventIfUnchanged(
        id: String, projectId: String, direction: com.cooper.lithic.core.model.MoneyDirection,
        expectedAmountCents: Long, expectedOccurredOn: LocalDate, expectedDescription: String?,
        role: ProjectMoneyEventRole, operationId: String,
        amountCents: Long, occurredOn: LocalDate, description: String?,
    ): Int

    @Query("""
        DELETE FROM project_money_events WHERE id = :id AND projectId = :projectId
            AND direction = :direction AND amountCents = :amountCents AND occurredOn = :occurredOn
            AND description IS :description AND role = :role AND operationId = :operationId
    """)
    suspend fun deleteEventIfUnchanged(
        id: String, projectId: String, direction: com.cooper.lithic.core.model.MoneyDirection,
        amountCents: Long, occurredOn: LocalDate, description: String?,
        role: ProjectMoneyEventRole, operationId: String,
    ): Int

    @Query("""
        DELETE FROM long_term_projects WHERE id = :id AND name = :name AND categoryId = :categoryId
            AND startedOn = :startedOn AND endedOn IS :endedOn AND endReason IS :endReason
            AND coverAssetId IS :coverAssetId AND note IS :note AND createdAt = :createdAt AND updatedAt = :updatedAt
    """)
    suspend fun deleteProjectIfUnchanged(
        id: String, name: String, categoryId: String, startedOn: LocalDate, endedOn: LocalDate?,
        endReason: String?, coverAssetId: String?, note: String?, createdAt: Instant, updatedAt: Instant,
    ): Int
}
