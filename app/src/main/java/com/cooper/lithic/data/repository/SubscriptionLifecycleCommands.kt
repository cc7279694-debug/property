package com.cooper.lithic.data.repository

import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity

/** Complete persisted CAS baseline; list order has no business meaning. */
data class SubscriptionFacts(
    val subscription: SubscriptionEntity,
    val renewals: List<SubscriptionRenewalEntity>,
    val stateChanges: List<SubscriptionStateChangeEntity>,
)

fun SubscriptionDetail.facts() = SubscriptionFacts(subscription, renewals, stateChanges)

enum class SubscriptionStateAction { PAUSE, RESUME, REOPEN, STOP_AT_END, STOP_EARLY }

data class SetSubscriptionStateCommand(
    val commandId: String,
    val expected: SubscriptionFacts,
    val target: SubscriptionStateChangeEntity,
    val action: SubscriptionStateAction,
)

data class UpdateSubscriptionStateCommand(
    val commandId: String,
    val expected: SubscriptionFacts,
    val expectedState: SubscriptionStateChangeEntity,
    val target: SubscriptionStateChangeEntity,
)

data class CancelScheduledStopCommand(
    val commandId: String,
    val expected: SubscriptionFacts,
    val expectedState: SubscriptionStateChangeEntity,
)

data class UpdateSubscriptionRenewalCommand(
    val commandId: String,
    val expected: SubscriptionFacts,
    val expectedRenewal: SubscriptionRenewalEntity,
    val target: SubscriptionRenewalEntity,
)

data class DeleteSubscriptionRenewalCommand(
    val commandId: String,
    val expected: SubscriptionFacts,
    val expectedRenewal: SubscriptionRenewalEntity,
)

data class ReenableSubscriptionWithRenewalCommand(
    val commandId: String,
    val expected: SubscriptionFacts,
    val renewal: SubscriptionRenewalEntity,
    val targetState: SubscriptionStateChangeEntity,
)

sealed interface SubscriptionLifecycleResult {
    data class Saved(
        val subscriptionId: String,
        val stateId: String? = null,
        val renewalId: String? = null,
        val replayed: Boolean,
    ) : SubscriptionLifecycleResult

    /** Current absence only; schema v2 has no permanent deletion operation log. */
    data class CurrentAbsence(val subscriptionId: String, val factId: String) : SubscriptionLifecycleResult
    data class ValidationFailure(val reason: String) : SubscriptionLifecycleResult
    data class Conflict(val reason: String) : SubscriptionLifecycleResult
    data class StorageFailure(val kind: StorageFailureKind, val message: String) : SubscriptionLifecycleResult
}
