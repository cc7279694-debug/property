package com.cooper.lithic.domain.report

import java.time.LocalDate

object CategoryReportEngine {
    fun evaluate(snapshot: LedgerSnapshot, asOf: LocalDate): List<CategoryReport> {
        val prepared = PreparedReportFacts(snapshot, asOf)
        val current = OverviewEngine.evaluate(snapshot.overview, asOf)
        val rates = (current.projectCategories + current.subscriptionCategories).associateBy { it.categoryId }
        val facts = prepared.facts.groupBy { prepared.categoryFor(it).id }
        val records = prepared.records.groupBy { it.categoryId }
        return snapshot.overview.categories.sortedWith(compareBy<OverviewCategory> { it.kind.ordinal }.thenBy { it.name }.thenBy { it.id })
            .map { category ->
                val rows = facts[category.id].orEmpty()
                val annual = rows.groupBy { it.date.year }.map { (year, values) ->
                    CategoryYearReport(year, summarizeFinancialFacts(values))
                }.sortedByDescending { it.year }
                CategoryReport(category, rates.getValue(category.id).dailyRate, summarizeFinancialFacts(rows),
                    annual, records[category.id].orEmpty())
            }
    }
}
