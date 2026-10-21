package com.cooper.lithic.domain.report

import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import java.time.LocalDate

/** Validates the projection, not a second stored ledger. Room's shared validators still own
 * lifecycle/transaction-group semantics before mapping. Multisets prove no money row was lost
 * or added while projecting, without merging legitimate equal-date/equal-amount facts. */
internal class PreparedReportFacts(snapshot: LedgerSnapshot, val asOf: LocalDate) {
    val categories = snapshot.overview.categories.associateBy { it.id }
    val facts: List<FinancialFact>
    val identities: Map<Pair<OverviewKind, String>, ReportRecordIdentity>
    val records: List<ReportRecord>

    init {
        OverviewEngine.evaluate(snapshot.overview, LocalDate.MIN)
        val allFacts = requireNotNull(snapshot.financialFacts) { "Annual reporting requires the typed, complete monetary projection" }
        val expectedCategories = snapshot.overview.projects.associate { (OverviewKind.PROJECT to it.id) to it.categoryId } +
            snapshot.overview.subscriptions.associate { (OverviewKind.SUBSCRIPTION to it.id) to it.categoryId }
        identities = snapshot.recordIdentities.associateBy { it.kind to it.id }
        require(identities.size == snapshot.recordIdentities.size && identities.keys == expectedCategories.keys) {
            "Report identities must exactly match both complete record sets"
        }
        identities.forEach { (key, row) ->
            require(row.name.isNotBlank() && row.categoryId == expectedCategories[key]) {
                "A report record needs its actual name and current category from the same snapshot"
            }
        }
        require(allFacts.map { it.id }.toSet().size == allFacts.size) { "Financial fact identities must be unique" }
        allFacts.forEach { fact ->
            require(fact.id.isNotBlank() && fact.operationId.isNotBlank()) { "A financial fact needs stable source and operation identities" }
            Money(fact.amountCents)
            require((fact.recordKind to fact.recordId) in identities) { "A monetary fact must reference a record of the matching type" }
            require(fact.exchangeGroupId == null || fact.exchangeGroupId.isNotBlank()) { "An exchange identity cannot be blank" }
            require(!fact.nonCash || fact.exchangeGroupId != null) { "Noncash credit must retain its source transaction identity" }
            if (fact.kind == FinancialFactKind.SUBSCRIPTION_PAYMENT) {
                require(fact.amountCents > 0 && !fact.nonCash && fact.exchangeGroupId == null) {
                    "Subscription facts are positive actual payments, never refunds or trade-in credit"
                }
            }
        }
        // Preserve the structure of the already validated three-row source group. This does
        // not infer roles from display text or regenerate the frozen M16 role identities.
        allFacts.filter { it.exchangeGroupId != null }.groupBy { it.exchangeGroupId!! }.values.forEach { rows ->
            require(rows.size == 3) { "A projected trade-in group is incomplete" }
            val old = rows.singleOrNull { it.nonCash && it.kind == FinancialFactKind.PROJECT_RECOVERY }
            val credit = rows.singleOrNull { it.nonCash && it.kind == FinancialFactKind.PROJECT_OUTLAY }
            val cash = rows.singleOrNull { !it.nonCash && it.kind == FinancialFactKind.PROJECT_OUTLAY }
            require(old != null && credit != null && cash != null && old.recordId != credit.recordId &&
                credit.recordId == cash.recordId && old.amountCents == credit.amountCents &&
                old.date == credit.date && credit.date == cash.date) { "The projected trade-in facts disagree" }
        }
        val actual = allFacts.groupBy { it.recordKind to it.recordId }.mapValues { (_, rows) ->
            rows.groupingBy { Signature(it.kind, it.date, it.amountCents) }.eachCount()
        }
        snapshot.overview.projects.forEach { project ->
            ProjectCostEngine.evaluate(project.snapshot, LocalDate.MAX)
            val expected = project.snapshot.moneyEvents.groupingBy {
                Signature(if (it.direction == MoneyDirection.OUTLAY) FinancialFactKind.PROJECT_OUTLAY else FinancialFactKind.PROJECT_RECOVERY,
                    it.occurredOn, it.amount.cents)
            }.eachCount()
            require(actual[OverviewKind.PROJECT to project.id].orEmpty() == expected) { "A project monetary projection differs from the validated facts" }
        }
        snapshot.overview.subscriptions.forEach { subscription ->
            SubscriptionCostEngine.evaluate(subscription.snapshot, LocalDate.MAX)
            val expected = subscription.snapshot.renewals.groupingBy {
                Signature(FinancialFactKind.SUBSCRIPTION_PAYMENT, it.paidOn, it.amount.cents)
            }.eachCount()
            require(actual[OverviewKind.SUBSCRIPTION to subscription.id].orEmpty() == expected) { "A payment projection differs from the validated renewals" }
        }
        facts = allFacts.filter { !it.date.isAfter(asOf) }
        records = (snapshot.overview.projects.map { row ->
            val identity = identities.getValue(OverviewKind.PROJECT to row.id)
            val cost = ProjectCostEngine.evaluate(row.snapshot, asOf)
            val status = when {
                cost.unavailableReason != null -> ReportRecordStatus.NOT_STARTED
                row.snapshot.project.endedOn?.let { !it.isAfter(asOf) } == true -> ReportRecordStatus.ENDED
                else -> ReportRecordStatus.CURRENT
            }
            ReportRecord(row.id, OverviewKind.PROJECT, identity.name, identity.categoryId, status)
        } + snapshot.overview.subscriptions.map { row ->
            val identity = identities.getValue(OverviewKind.SUBSCRIPTION to row.id)
            val status = SubscriptionCostEngine.evaluate(row.snapshot, asOf).displayStatus
            ReportRecord(row.id, OverviewKind.SUBSCRIPTION, identity.name, identity.categoryId, ReportRecordStatus.valueOf(status.name))
        }).sortedWith(compareBy<ReportRecord> { it.kind.ordinal }.thenBy { it.id })
    }

    fun categoryFor(fact: FinancialFact): OverviewCategory = categories.getValue(identities.getValue(fact.recordKind to fact.recordId).categoryId)
    fun recordsFor(facts: List<FinancialFact>): List<ReportRecord> {
        val keys = facts.map { it.recordKind to it.recordId }.toSet()
        return records.filter { (it.kind to it.id) in keys }
    }

    private data class Signature(val kind: FinancialFactKind, val date: LocalDate, val cents: Long)
}

internal fun summarizeFinancialFacts(facts: List<FinancialFact>): ReportAmounts = facts.fold(ReportAmounts(), ::addFinancialFact)
