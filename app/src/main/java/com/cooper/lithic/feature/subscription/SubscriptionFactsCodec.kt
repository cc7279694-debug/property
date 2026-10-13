package com.cooper.lithic.feature.subscription

import android.os.Bundle
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.data.repository.SubscriptionFacts
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.time.Instant
import java.time.LocalDate

/** Saves persistent facts only inside the existing single task form, never a derived snapshot. */
internal object SubscriptionFactsCodec {
    fun encode(facts: SubscriptionFacts) = Bundle().apply {
        putBundle("subscription", Bundle().apply {
            putString("id", facts.subscription.id); putString("name", facts.subscription.name)
            putString("categoryId", facts.subscription.categoryId); putString("coverAssetId", facts.subscription.coverAssetId)
            putString("note", facts.subscription.note); putString("createdAt", facts.subscription.createdAt.toString())
            putString("updatedAt", facts.subscription.updatedAt.toString())
        })
        putParcelableArrayList("renewals", ArrayList(facts.renewals.map(::encodeRenewal)))
        putParcelableArrayList("stateChanges", ArrayList(facts.stateChanges.map(::encodeState)))
    }

    fun decode(bundle: Bundle): SubscriptionFacts {
        val identity = requireNotNull(bundle.getBundle("subscription"))
        val subscription = SubscriptionEntity(uuid(identity, "id"), text(identity, "name"), uuid(identity, "categoryId"),
            identity.getString("coverAssetId")?.also(StoredValueValidation::requireUuid), identity.getString("note"),
            Instant.parse(text(identity, "createdAt")), Instant.parse(text(identity, "updatedAt")))
        val renewals = requireNotNull(bundle.getParcelableArrayList<Bundle>("renewals")).map(::decodeRenewal)
        val states = requireNotNull(bundle.getParcelableArrayList<Bundle>("stateChanges")).map(::decodeState)
        require(renewals.isNotEmpty() && renewals.all { it.subscriptionId == subscription.id })
        require(states.all { it.subscriptionId == subscription.id })
        require(renewals.map { it.id }.distinct().size == renewals.size && renewals.map { it.operationId }.distinct().size == renewals.size)
        require(states.map { it.id }.distinct().size == states.size && states.map { it.operationId }.distinct().size == states.size)
        require(states.map { it.effectiveOn }.distinct().size == states.size)
        return SubscriptionFacts(subscription, renewals, states)
    }

    fun encodeRenewal(row: SubscriptionRenewalEntity) = Bundle().apply {
        putString("id", row.id); putString("subscriptionId", row.subscriptionId)
        putString("paidOn", row.paidOn.toString()); putString("effectiveFrom", row.effectiveFrom.toString())
        putString("paidUntilExclusive", row.paidUntilExclusive.toString()); putString("reminderDueOn", row.reminderDueOn.toString())
        putLong("amountCents", row.amountCents); putString("cycle", row.cycle.name)
        putInt("standardDays", row.standardDays); putString("operationId", row.operationId)
    }

    fun decodeRenewal(bundle: Bundle) = SubscriptionRenewalEntity(uuid(bundle, "id"), uuid(bundle, "subscriptionId"),
        LocalDate.parse(text(bundle, "paidOn")), LocalDate.parse(text(bundle, "effectiveFrom")),
        LocalDate.parse(text(bundle, "paidUntilExclusive")), LocalDate.parse(text(bundle, "reminderDueOn")),
        bundle.getLong("amountCents"), BillingCycle.valueOf(text(bundle, "cycle")), bundle.getInt("standardDays"), uuid(bundle, "operationId"))

    fun encodeState(row: SubscriptionStateChangeEntity) = Bundle().apply {
        putString("id", row.id); putString("subscriptionId", row.subscriptionId)
        putString("effectiveOn", row.effectiveOn.toString()); putString("state", row.state.name)
        putString("reason", row.reason); putString("note", row.note); putString("operationId", row.operationId)
    }

    fun decodeState(bundle: Bundle) = SubscriptionStateChangeEntity(uuid(bundle, "id"), uuid(bundle, "subscriptionId"),
        LocalDate.parse(text(bundle, "effectiveOn")), SubscriptionOperationState.valueOf(text(bundle, "state")),
        bundle.getString("reason"), bundle.getString("note"), uuid(bundle, "operationId"))

    private fun uuid(bundle: Bundle, key: String) = text(bundle, key).also(StoredValueValidation::requireUuid)
    private fun text(bundle: Bundle, key: String) = requireNotNull(bundle.getString(key))
}
