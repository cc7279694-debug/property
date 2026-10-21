package com.cooper.lithic.domain.report

import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.subscription.*
import java.time.LocalDate

internal object ReportEngineFixtures {
    val pc = OverviewCategory("pc", "项目类别", OverviewKind.PROJECT)
    val sc = OverviewCategory("sc", "订阅类别", OverviewKind.SUBSCRIPTION)
    val today: LocalDate = LocalDate.of(2029, 6, 1)
    fun fact(id: String, cents: Long, year: Int = 2026,
        kind: FinancialFactKind = FinancialFactKind.PROJECT_OUTLAY,
        record: String = if (kind == FinancialFactKind.SUBSCRIPTION_PAYMENT) "s" else "p",
        nonCash: Boolean = false, group: String? = null,
    ) = FinancialFact(id, record, kind, LocalDate.of(year, 12, 15), cents, nonCash, "op-$id", group)

    fun ledger(facts: List<FinancialFact>, categories: List<OverviewCategory> = listOf(pc, sc),
        categoryByRecord: Map<String, String> = emptyMap(), endByProject: Map<String, LocalDate> = emptyMap(),
    ): LedgerSnapshot {
        val projects = facts.filter { it.recordKind == OverviewKind.PROJECT }.groupBy { it.recordId }.map { (id, rows) ->
            OverviewProject(id, categoryByRecord[id] ?: pc.id,
                ProjectSnapshot(LongTermProject(LocalDate.of(2026, 1, 1), endByProject[id]), rows.map {
                    ProjectMoneyEvent(if (it.kind == FinancialFactKind.PROJECT_OUTLAY) MoneyDirection.OUTLAY else MoneyDirection.RECOVERY,
                        Money(it.amountCents), it.date)
                }))
        }
        val subscriptions = facts.filter { it.recordKind == OverviewKind.SUBSCRIPTION }.groupBy { it.recordId }.map { (id, rows) ->
            OverviewSubscription(id, categoryByRecord[id] ?: sc.id, SubscriptionSnapshot(Subscription(id), rows.map {
                Renewal(Money(it.amountCents), BillingCycle.YEARLY, it.date, it.date.plusDays(17), it.date.plusYears(1).plusDays(17))
            }))
        }
        val identities = projects.map { ReportRecordIdentity(it.id, OverviewKind.PROJECT, "项目${it.id}", it.categoryId) } +
            subscriptions.map { ReportRecordIdentity(it.id, OverviewKind.SUBSCRIPTION, "订阅${it.id}", it.categoryId) }
        return LedgerSnapshot(OverviewSnapshot(categories, projects, subscriptions), financialFacts = facts, recordIdentities = identities)
    }
}
