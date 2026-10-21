package com.cooper.lithic.feature.project

import android.os.Bundle
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.ReplacementEntity
import java.time.Instant
import java.time.LocalDate

/** Only fixed identity/fact baselines enter saved UI state; no owner or open resource does. */
internal object ReplacementSavedFacts {
    fun project(value: LongTermProjectEntity) = Bundle().apply {
        putString("id", value.id); putString("name", value.name); putString("categoryId", value.categoryId)
        putString("startedOn", value.startedOn.toString()); putString("endedOn", value.endedOn?.toString())
        putString("endReason", value.endReason); putString("coverAssetId", value.coverAssetId); putString("note", value.note)
        putString("createdAt", value.createdAt.toString()); putString("updatedAt", value.updatedAt.toString())
    }
    fun project(value: Bundle) = LongTermProjectEntity(
        requireNotNull(value.getString("id")), requireNotNull(value.getString("name")), requireNotNull(value.getString("categoryId")),
        LocalDate.parse(value.getString("startedOn")), value.getString("endedOn")?.let(LocalDate::parse),
        value.getString("endReason"), value.getString("coverAssetId"), value.getString("note"),
        Instant.parse(value.getString("createdAt")), Instant.parse(value.getString("updatedAt")),
    )
    fun event(value: ProjectMoneyEventEntity) = Bundle().apply {
        putString("id", value.id); putString("projectId", value.projectId); putString("direction", value.direction.name)
        putLong("amountCents", value.amountCents); putString("occurredOn", value.occurredOn.toString()); putString("description", value.description)
        putString("role", value.role.name); putString("operationId", value.operationId); putString("basis", value.basis.name)
        putString("exchangeGroupId", value.exchangeGroupId)
    }
    fun event(value: Bundle) = ProjectMoneyEventEntity(
        requireNotNull(value.getString("id")), requireNotNull(value.getString("projectId")), MoneyDirection.valueOf(requireNotNull(value.getString("direction"))),
        value.getLong("amountCents"), LocalDate.parse(value.getString("occurredOn")), value.getString("description"),
        ProjectMoneyEventRole.valueOf(requireNotNull(value.getString("role"))), requireNotNull(value.getString("operationId")),
        MoneyBasis.valueOf(value.getString("basis") ?: MoneyBasis.CASH.name), value.getString("exchangeGroupId"),
    )
    fun link(value: ReplacementEntity) = Bundle().apply {
        putString("id", value.id); putString("predecessorId", value.predecessorId); putString("successorId", value.successorId)
        putString("createdAt", value.createdAt.toString())
    }
    fun link(value: Bundle) = ReplacementEntity(requireNotNull(value.getString("id")),
        requireNotNull(value.getString("predecessorId")), requireNotNull(value.getString("successorId")), Instant.parse(value.getString("createdAt")))
    fun links(value: Bundle, key: String = "links"): List<ReplacementEntity> =
        value.getParcelableArrayList<Bundle>(key).orEmpty().map(::link)
    fun events(value: Bundle): List<ProjectMoneyEventEntity> =
        value.getParcelableArrayList<Bundle>("events").orEmpty().map(::event)
}
