package com.cooper.lithic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertProject(project: LongTermProjectEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertEvent(event: ProjectMoneyEventEntity)
    @Query("SELECT * FROM long_term_projects ORDER BY startedOn, id") suspend fun listProjects(): List<LongTermProjectEntity>
    @Query("SELECT * FROM project_money_events ORDER BY projectId, occurredOn, id") suspend fun listEvents(): List<ProjectMoneyEventEntity>
    @Query("SELECT * FROM long_term_projects WHERE id = :id") suspend fun findProject(id: String): LongTermProjectEntity?
    @Query("SELECT * FROM project_money_events WHERE operationId = :operationId") suspend fun findEventByOperation(operationId: String): ProjectMoneyEventEntity?
}
