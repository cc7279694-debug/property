package com.cooper.lithic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.LocalDate

data class SubscriptionListKey(val id: String, val lastPaidOn: LocalDate?)

@Dao
interface SubscriptionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSubscription(value: SubscriptionEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertRenewal(value: SubscriptionRenewalEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertStateChange(value: SubscriptionStateChangeEntity)
    @Query("SELECT * FROM subscriptions WHERE id=:id") suspend fun findSubscription(id: String): SubscriptionEntity?
    @Query("SELECT * FROM subscriptions WHERE id IN (:ids)") suspend fun subscriptionsForIds(ids: List<String>): List<SubscriptionEntity>
    @Query("SELECT * FROM subscription_renewals WHERE operationId=:operationId") suspend fun findRenewalByOperation(operationId: String): SubscriptionRenewalEntity?
    @Query("SELECT * FROM subscription_renewals WHERE id=:id") suspend fun findRenewal(id: String): SubscriptionRenewalEntity?
    @Query("SELECT COUNT(*) FROM subscriptions") suspend fun countSubscriptions(): Long
    @Query("SELECT COUNT(*) FROM subscriptions s WHERE NOT EXISTS (SELECT 1 FROM subscription_renewals r WHERE r.subscriptionId=s.id)")
    suspend fun countSubscriptionsWithoutRenewals(): Long
    @Query("SELECT COUNT(*) FROM subscription_renewals WHERE subscriptionId=:id") suspend fun countRenewals(id: String): Long

    @Query("""
        SELECT s.id AS id, MAX(r.paidOn) AS lastPaidOn
        FROM subscriptions s LEFT JOIN subscription_renewals r ON r.subscriptionId=s.id
        GROUP BY s.id
        HAVING :afterPaidOn IS NULL OR MAX(r.paidOn)<:afterPaidOn
            OR (MAX(r.paidOn)=:afterPaidOn AND s.id>:afterId)
        ORDER BY lastPaidOn DESC, s.id ASC LIMIT :limit
    """)
    suspend fun pageSubscriptionKeys(afterPaidOn: LocalDate?, afterId: String?, limit: Int): List<SubscriptionListKey>

    @Query("SELECT * FROM subscription_renewals WHERE subscriptionId IN (:ids) ORDER BY subscriptionId,paidOn DESC,id ASC")
    suspend fun renewalsForSubscriptions(ids: List<String>): List<SubscriptionRenewalEntity>
    @Query("SELECT * FROM subscription_state_changes WHERE subscriptionId IN (:ids) ORDER BY subscriptionId,effectiveOn,id")
    suspend fun statesForSubscriptions(ids: List<String>): List<SubscriptionStateChangeEntity>
    @Query("SELECT * FROM categories WHERE id IN (:ids) ORDER BY normalizedName,id")
    suspend fun categoriesForSubscriptions(ids: List<String>): List<CategoryEntity>
    @Query("SELECT * FROM categories WHERE type='SUBSCRIPTION' ORDER BY normalizedName,id")
    suspend fun subscriptionCategories(): List<CategoryEntity>

    @Query("""
        SELECT * FROM subscription_renewals WHERE subscriptionId=:subscriptionId
            AND (:afterPaidOn IS NULL OR paidOn<:afterPaidOn OR (paidOn=:afterPaidOn AND id>:afterId))
        ORDER BY paidOn DESC,id ASC LIMIT :limit
    """)
    suspend fun pageRenewals(subscriptionId: String, afterPaidOn: LocalDate?, afterId: String?, limit: Int): List<SubscriptionRenewalEntity>
}
