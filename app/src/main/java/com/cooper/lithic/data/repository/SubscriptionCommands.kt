package com.cooper.lithic.data.repository

import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.domain.subscription.SubscriptionCostResult
import java.time.LocalDate

data class CreateSubscriptionCommand(val commandId: String, val subscription: SubscriptionEntity, val initialRenewal: SubscriptionRenewalEntity)
data class ConfirmRenewalCommand(val commandId: String, val renewal: SubscriptionRenewalEntity)

sealed interface SubscriptionMutationResult {
    data class Success(val subscriptionId: String, val renewalId: String, val replayed: Boolean) : SubscriptionMutationResult
    data class ValidationFailure(val reason: String) : SubscriptionMutationResult
    data class Conflict(val reason: String) : SubscriptionMutationResult
    data class StorageFailure(val kind: StorageFailureKind, val message: String) : SubscriptionMutationResult
}

data class SubscriptionCursor(val lastPaidOn: LocalDate, val id: String)
data class RenewalCursor(val paidOn: LocalDate, val id: String)
data class SubscriptionDetail(
    val subscription: SubscriptionEntity,
    val category: CategoryEntity,
    val renewals: List<SubscriptionRenewalEntity>,
    val stateChanges: List<SubscriptionStateChangeEntity>,
    val cost: SubscriptionCostResult,
    val currentRenewal: SubscriptionRenewalEntity?,
    val asOf: LocalDate,
)
data class SubscriptionPage(val items: List<SubscriptionDetail>, val nextCursor: SubscriptionCursor?, val totalCount: Long, val asOf: LocalDate)
data class RenewalHistoryPage(val items: List<SubscriptionRenewalEntity>, val nextCursor: RenewalCursor?, val totalCount: Long, val asOf: LocalDate)
