package com.cooper.lithic.data.repository

import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.StoredValueValidation
import java.util.UUID

/** Fixed role identities survive cancelled callers and unknown commit results. */
object TradeInIdentities {
    const val OLD_CREDIT = "old-credit"
    const val NEW_CASH = "new-cash"
    const val NEW_CREDIT = "new-credit"
    fun id(commandId: String, role: String): String =
        UUID.nameUUIDFromBytes("lithic-m16:$commandId:$role".toByteArray(Charsets.UTF_8)).toString()
    fun eventId(commandId: String, role: String): String = id(commandId, "event:$role")
    fun operationId(commandId: String, role: String): String = id(commandId, "operation:$role")
    fun newProjectId(commandId: String): String = id(commandId, "new-project")
    fun linkId(commandId: String): String = id(commandId, "replacement")
}

/** Shared by repository and backup validation; no second set of trade-in rules. */
object TradeInFacts {
    fun validate(projects: List<LongTermProjectEntity>, events: List<ProjectMoneyEventEntity>): String? {
        val byId = projects.associateBy { it.id }
        if (byId.size != projects.size) return "Project identities are duplicated"
        if (events.map { it.id }.distinct().size != events.size || events.map { it.operationId }.distinct().size != events.size) return "Money identities are duplicated"
        if (events.any { it.projectId !in byId }) return "A money event references a missing project"
        if (events.any { it.basis == MoneyBasis.TRADE_IN_CREDIT && it.exchangeGroupId == null }) return "Trade-in credit requires a complete transaction group"
        for ((groupId, rows) in events.filter { it.exchangeGroupId != null }.groupBy { it.exchangeGroupId!! }) {
            if (!StoredValueValidation.isUuid(groupId) || rows.size != 3) return "A trade-in group must retain exactly three facts"
            val old = rows.singleOrNull { it.role == ProjectMoneyEventRole.END_SETTLEMENT && it.direction == MoneyDirection.RECOVERY && it.basis == MoneyBasis.TRADE_IN_CREDIT }
                ?: return "A trade-in group is missing its old-project credit recovery"
            val cash = rows.singleOrNull { it.role == ProjectMoneyEventRole.INITIAL && it.direction == MoneyDirection.OUTLAY && it.basis == MoneyBasis.CASH }
                ?: return "A trade-in group is missing its new-project initial cash payment"
            val credit = rows.singleOrNull { it.role == ProjectMoneyEventRole.REGULAR && it.direction == MoneyDirection.OUTLAY && it.basis == MoneyBasis.TRADE_IN_CREDIT }
                ?: return "A trade-in group is missing its new-project noncash credit"
            if (old.projectId == cash.projectId || cash.projectId != credit.projectId) return "Trade-in credit must join the old and new projects"
            if (cash.projectId != TradeInIdentities.newProjectId(groupId)) return "The new project does not match its transaction identity"
            if (old.amountCents != credit.amountCents) return "The two noncash credit amounts do not match"
            if (old.occurredOn != cash.occurredOn || cash.occurredOn != credit.occurredOn) return "Trade-in facts must share the actual transaction date"
            if (byId.getValue(old.projectId).endedOn != old.occurredOn || byId.getValue(cash.projectId).startedOn != cash.occurredOn) return "The trade-in lifecycle dates do not match its payment facts"
            for ((role, event) in listOf(TradeInIdentities.OLD_CREDIT to old, TradeInIdentities.NEW_CASH to cash, TradeInIdentities.NEW_CREDIT to credit)) {
                if (event.id != TradeInIdentities.eventId(groupId, role) || event.operationId != TradeInIdentities.operationId(groupId, role)) return "Trade-in role identity does not match its transaction"
                try { Money(event.amountCents) } catch (_: IllegalArgumentException) { return "A trade-in amount is outside the supported range" }
            }
            try { Math.addExact(cash.amountCents, credit.amountCents) } catch (_: ArithmeticException) { return "The combined acquisition amount overflows" }
        }
        return null
    }
}
