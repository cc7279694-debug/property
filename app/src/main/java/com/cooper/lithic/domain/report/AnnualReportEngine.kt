package com.cooper.lithic.domain.report

import com.cooper.lithic.core.model.Money
import java.time.LocalDate
import java.time.Year

object AnnualReportEngine {
    fun evaluate(snapshot: LedgerSnapshot, year: Int, asOf: LocalDate = LocalDate.MAX): AnnualReport {
        require(year in Year.MIN_VALUE..Year.MAX_VALUE) { "The selected year is outside the calendar range" }
        val prepared = PreparedReportFacts(snapshot, asOf)
        val facts = prepared.facts.filter { it.date.year == year }
        val categories = facts.groupBy { prepared.categoryFor(it) }.map { (category, rows) ->
            AnnualCategoryReport(category, summarizeFinancialFacts(rows))
        }.sortedWith(compareBy<AnnualCategoryReport> { it.category.kind.ordinal }.thenBy { it.category.name }.thenBy { it.category.id })
        return AnnualReport(year, summarizeFinancialFacts(facts), categories, prepared.recordsFor(facts))
    }
    /** Only real recorded years; a gap does not become an invented zero-payment year. */
    fun years(snapshot: LedgerSnapshot, asOf: LocalDate): List<Int> =
        PreparedReportFacts(snapshot, asOf).facts.map { it.date.year }.distinct().sortedDescending()
}

/** The production aggregate uses this immutable checked step, including every failure path. */
internal fun addFinancialFact(amounts: ReportAmounts, fact: FinancialFact): ReportAmounts {
    Money(fact.amountCents)
    val result = when (fact.kind) {
        FinancialFactKind.PROJECT_OUTLAY -> amounts.copy(
            projectOutlayCents = Math.addExact(amounts.projectOutlayCents, fact.amountCents),
            projectFactCount = Math.incrementExact(amounts.projectFactCount),
            containsNonCash = amounts.containsNonCash || fact.nonCash)
        FinancialFactKind.PROJECT_RECOVERY -> amounts.copy(
            projectRecoveryCents = Math.addExact(amounts.projectRecoveryCents, fact.amountCents),
            projectFactCount = Math.incrementExact(amounts.projectFactCount),
            containsNonCash = amounts.containsNonCash || fact.nonCash)
        FinancialFactKind.SUBSCRIPTION_PAYMENT -> amounts.copy(
            subscriptionPaidCents = Math.addExact(amounts.subscriptionPaidCents, fact.amountCents),
            subscriptionPaymentCount = Math.incrementExact(amounts.subscriptionPaymentCount))
    }
    // Eagerly check the derived difference too; no partially updated result escapes.
    result.projectNetCents
    return result
}
