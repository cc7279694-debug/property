package com.cooper.lithic.feature.media

import android.os.Bundle
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.data.media.CoverBaseline
import com.cooper.lithic.data.repository.SubscriptionFacts
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.time.Instant
import java.time.LocalDate

/** Transient task facts only. The source URI never enters this envelope. */
internal object CoverSavedState {
    fun encodeBaseline(value: CoverBaseline): Bundle = when (value) {
        is CoverBaseline.Project -> Bundle().apply {
            putString("type", "PROJECT"); putBundle("project", encodeProject(value.project))
            putParcelableArrayList("events", ArrayList(value.events.map(::encodeEvent)))
        }
        is CoverBaseline.Subscription -> Bundle().apply {
            putString("type", "SUBSCRIPTION"); putBundle("subscription", encodeSubscription(value.facts.subscription))
            putParcelableArrayList("renewals", ArrayList(value.facts.renewals.map(::encodeRenewal)))
            putParcelableArrayList("states", ArrayList(value.facts.stateChanges.map(::encodeState)))
        }
    }

    fun decodeBaseline(value: Bundle): CoverBaseline = when (value.string("type")) {
        "PROJECT" -> CoverBaseline.Project(decodeProject(value.bundle("project")), bundles(value, "events").map(::decodeEvent))
        "SUBSCRIPTION" -> CoverBaseline.Subscription(SubscriptionFacts(decodeSubscription(value.bundle("subscription")),
            bundles(value, "renewals").map(::decodeRenewal), bundles(value, "states").map(::decodeState)))
        else -> error("Unknown saved cover type")
    }

    fun encodeAsset(value: LocalMediaAssetEntity): Bundle = Bundle().apply {
        putString("id", value.id); putString("path", value.relativePath); putString("mime", value.mime)
        putInt("width", value.width); putInt("height", value.height); putLong("bytes", value.byteLength)
        putString("sha256", value.sha256)
    }
    fun decodeAsset(value: Bundle) = LocalMediaAssetEntity(value.string("id"), value.string("path"), value.string("mime"),
        value.getInt("width"), value.getInt("height"), value.getLong("bytes"), value.string("sha256"))

    private fun encodeProject(value: LongTermProjectEntity) = Bundle().apply {
        putString("id", value.id); putString("name", value.name); putString("categoryId", value.categoryId)
        putString("startedOn", value.startedOn.toString()); putString("endedOn", value.endedOn?.toString())
        putString("endReason", value.endReason); putString("coverAssetId", value.coverAssetId); putString("note", value.note)
        putString("createdAt", value.createdAt.toString()); putString("updatedAt", value.updatedAt.toString())
    }
    private fun decodeProject(value: Bundle) = LongTermProjectEntity(value.string("id"), value.string("name"), value.string("categoryId"),
        LocalDate.parse(value.string("startedOn")), value.getString("endedOn")?.let(LocalDate::parse), value.getString("endReason"),
        value.getString("coverAssetId"), value.getString("note"), Instant.parse(value.string("createdAt")), Instant.parse(value.string("updatedAt")))

    private fun encodeEvent(value: ProjectMoneyEventEntity) = Bundle().apply {
        putString("id", value.id); putString("projectId", value.projectId); putString("direction", value.direction.name)
        putLong("amountCents", value.amountCents); putString("occurredOn", value.occurredOn.toString())
        putString("description", value.description); putString("role", value.role.name); putString("operationId", value.operationId)
    }
    private fun decodeEvent(value: Bundle) = ProjectMoneyEventEntity(value.string("id"), value.string("projectId"),
        MoneyDirection.valueOf(value.string("direction")), value.getLong("amountCents"), LocalDate.parse(value.string("occurredOn")),
        value.getString("description"), ProjectMoneyEventRole.valueOf(value.string("role")), value.string("operationId"))

    private fun encodeSubscription(value: SubscriptionEntity) = Bundle().apply {
        putString("id", value.id); putString("name", value.name); putString("categoryId", value.categoryId)
        putString("coverAssetId", value.coverAssetId); putString("note", value.note)
        putString("createdAt", value.createdAt.toString()); putString("updatedAt", value.updatedAt.toString())
    }
    private fun decodeSubscription(value: Bundle) = SubscriptionEntity(value.string("id"), value.string("name"), value.string("categoryId"),
        value.getString("coverAssetId"), value.getString("note"), Instant.parse(value.string("createdAt")), Instant.parse(value.string("updatedAt")))

    private fun encodeRenewal(value: SubscriptionRenewalEntity) = Bundle().apply {
        putString("id", value.id); putString("subscriptionId", value.subscriptionId); putString("paidOn", value.paidOn.toString())
        putString("effectiveFrom", value.effectiveFrom.toString()); putString("paidUntilExclusive", value.paidUntilExclusive.toString())
        putString("reminderDueOn", value.reminderDueOn.toString()); putLong("amountCents", value.amountCents)
        putString("cycle", value.cycle.name); putInt("standardDays", value.standardDays); putString("operationId", value.operationId)
    }
    private fun decodeRenewal(value: Bundle) = SubscriptionRenewalEntity(value.string("id"), value.string("subscriptionId"),
        LocalDate.parse(value.string("paidOn")), LocalDate.parse(value.string("effectiveFrom")), LocalDate.parse(value.string("paidUntilExclusive")),
        LocalDate.parse(value.string("reminderDueOn")), value.getLong("amountCents"), BillingCycle.valueOf(value.string("cycle")),
        value.getInt("standardDays"), value.string("operationId"))

    private fun encodeState(value: SubscriptionStateChangeEntity) = Bundle().apply {
        putString("id", value.id); putString("subscriptionId", value.subscriptionId); putString("effectiveOn", value.effectiveOn.toString())
        putString("state", value.state.name); putString("reason", value.reason); putString("note", value.note); putString("operationId", value.operationId)
    }
    private fun decodeState(value: Bundle) = SubscriptionStateChangeEntity(value.string("id"), value.string("subscriptionId"),
        LocalDate.parse(value.string("effectiveOn")), SubscriptionOperationState.valueOf(value.string("state")), value.getString("reason"),
        value.getString("note"), value.string("operationId"))

    private fun Bundle.string(key: String): String = requireNotNull(getString(key))
    private fun Bundle.bundle(key: String): Bundle = requireNotNull(getBundle(key))
    @Suppress("DEPRECATION")
    private fun bundles(value: Bundle, key: String): List<Bundle> = requireNotNull(value.getParcelableArrayList<Bundle>(key))
}
