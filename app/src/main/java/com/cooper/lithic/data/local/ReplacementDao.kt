package com.cooper.lithic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReplacementDao {
    @Query("SELECT * FROM project_money_events WHERE exchangeGroupId IS NOT NULL OR basis != 'CASH' ORDER BY id")
    suspend fun transactionEvents(): List<ProjectMoneyEventEntity>
    @Query("""
        SELECT * FROM long_term_projects WHERE id IN (
            SELECT predecessorId FROM replacements UNION SELECT successorId FROM replacements
            UNION SELECT projectId FROM project_money_events WHERE exchangeGroupId IS NOT NULL OR basis != 'CASH'
        ) ORDER BY id
    """)
    suspend fun referencedProjects(): List<LongTermProjectEntity>
    @Query("SELECT * FROM replacements ORDER BY id") suspend fun listReplacements(): List<ReplacementEntity>
    @Query("SELECT * FROM replacements WHERE id = :id") suspend fun find(id: String): ReplacementEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(value: ReplacementEntity)
    @Query("DELETE FROM replacements WHERE id = :id") suspend fun delete(id: String): Int
    @Query("DELETE FROM replacements WHERE predecessorId = :projectId OR successorId = :projectId") suspend fun deleteForProject(projectId: String): Int
}
