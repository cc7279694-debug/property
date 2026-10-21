package com.cooper.lithic.data.local

import androidx.room.Dao
import androidx.room.Query

data class SearchKey(val type: CategoryType, val id: String)

/** Only names participate. A bound, escaped LIKE value is shared by both identity sets. */
@Dao
interface SearchDao {
    @Query("""
        SELECT type, id FROM (
            SELECT 'PROJECT' AS type, id FROM long_term_projects WHERE name LIKE :pattern ESCAPE '!'
            UNION ALL
            SELECT 'SUBSCRIPTION' AS type, id FROM subscriptions WHERE name LIKE :pattern ESCAPE '!'
        )
        WHERE :afterType IS NULL OR type > :afterType OR (type = :afterType AND id > :afterId)
        ORDER BY type ASC, id ASC LIMIT :limit
    """)
    suspend fun pageNames(pattern: String, afterType: String?, afterId: String?, limit: Int): List<SearchKey>

    @Query("""
        SELECT (SELECT COUNT(*) FROM long_term_projects WHERE name LIKE :pattern ESCAPE '!')
             + (SELECT COUNT(*) FROM subscriptions WHERE name LIKE :pattern ESCAPE '!')
    """)
    suspend fun countNames(pattern: String): Long
}
