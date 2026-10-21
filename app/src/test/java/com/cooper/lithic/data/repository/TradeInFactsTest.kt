package com.cooper.lithic.data.repository

import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class TradeInFactsTest {
    private val group = id("group")
    private val date = LocalDate.of(2026, 10, 21)
    private val timestamp = Instant.parse("2026-10-21T00:00:00Z")
    private val old = LongTermProjectEntity(id("old"), "old", LithicDatabase.PROJECT_FALLBACK_ID, date.minusYears(3), date, "以旧换新", createdAt = timestamp, updatedAt = timestamp)
    private val fresh = LongTermProjectEntity(TradeInIdentities.newProjectId(group), "new", LithicDatabase.PROJECT_FALLBACK_ID, date, createdAt = timestamp, updatedAt = timestamp)
    private val projects get() = listOf(old, fresh)
    private fun facts(): List<ProjectMoneyEventEntity> {
        fun row(role: String, projectId: String, direction: MoneyDirection, cents: Long, eventRole: ProjectMoneyEventRole, basis: MoneyBasis) =
            ProjectMoneyEventEntity(TradeInIdentities.eventId(group, role), projectId, direction, cents, date, role = eventRole,
                operationId = TradeInIdentities.operationId(group, role), basis = basis, exchangeGroupId = group)
        return listOf(
            row(TradeInIdentities.OLD_CREDIT, old.id, MoneyDirection.RECOVERY, 300_000, ProjectMoneyEventRole.END_SETTLEMENT, MoneyBasis.TRADE_IN_CREDIT),
            row(TradeInIdentities.NEW_CASH, fresh.id, MoneyDirection.OUTLAY, 500_000, ProjectMoneyEventRole.INITIAL, MoneyBasis.CASH),
            row(TradeInIdentities.NEW_CREDIT, fresh.id, MoneyDirection.OUTLAY, 300_000, ProjectMoneyEventRole.REGULAR, MoneyBasis.TRADE_IN_CREDIT),
        )
    }
    @Test fun canonical_three_fact_group_and_zero_cash_are_valid() {
        assertNull(TradeInFacts.validate(projects, facts()))
        assertNull(TradeInFacts.validate(projects, facts().map { if (it.basis == MoneyBasis.CASH) it.copy(amountCents = 0) else it }))
    }
    @Test fun missing_or_extra_group_fact_is_rejected() {
        assertNotNull(TradeInFacts.validate(projects, facts().dropLast(1)))
        assertNotNull(TradeInFacts.validate(projects, facts() + facts().last().copy(id = id("extra"), operationId = id("extra-op"))))
    }
    @Test fun one_sided_credit_amount_date_and_lifecycle_change_are_rejected() {
        assertNotNull(TradeInFacts.validate(projects, facts().map { if (it.projectId == old.id) it.copy(amountCents = 1) else it }))
        assertNotNull(TradeInFacts.validate(projects, facts().map { if (it.projectId == old.id) it.copy(occurredOn = date.minusDays(1)) else it }))
        assertNotNull(TradeInFacts.validate(listOf(old.copy(endedOn = date.minusDays(1)), fresh), facts()))
        assertNotNull(TradeInFacts.validate(listOf(old, fresh.copy(startedOn = date.minusDays(1))), facts()))
    }
    @Test fun cash_basis_cannot_replace_non_cash_credit() {
        assertNotNull(TradeInFacts.validate(projects, facts().map { if (it.projectId == old.id) it.copy(basis = MoneyBasis.CASH) else it }))
        assertNotNull(TradeInFacts.validate(projects, facts().map { if (it.basis == MoneyBasis.TRADE_IN_CREDIT) it.copy(exchangeGroupId = null) else it }))
    }
    @Test fun wrong_project_or_role_derived_identity_is_rejected() {
        assertNotNull(TradeInFacts.validate(listOf(old), facts()))
        assertNotNull(TradeInFacts.validate(projects, facts().map { if (it.role == ProjectMoneyEventRole.INITIAL) it.copy(operationId = id("forged-op")) else it }))
        assertNotNull(TradeInFacts.validate(projects, facts().map { if (it.role == ProjectMoneyEventRole.INITIAL) it.copy(id = id("forged-event")) else it }))
    }
    @Test fun malformed_group_and_reused_operation_are_rejected() {
        assertNotNull(TradeInFacts.validate(projects, facts().map { it.copy(exchangeGroupId = "unknown") }))
        assertNotNull(TradeInFacts.validate(projects, facts().map { it.copy(operationId = id("same-op")) }))
    }
    @Test fun normal_legacy_cash_does_not_require_a_transaction_group() {
        val initial = ProjectMoneyEventEntity(id("initial"), old.id, MoneyDirection.OUTLAY, 0, old.startedOn,
            role = ProjectMoneyEventRole.INITIAL, operationId = id("normal-op"))
        assertNull(TradeInFacts.validate(projects, facts() + initial))
    }
    private fun id(value: String) = UUID.nameUUIDFromBytes("m16-group-test:$value".toByteArray(Charsets.UTF_8)).toString()
}
