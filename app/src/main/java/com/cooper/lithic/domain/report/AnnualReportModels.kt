package com.cooper.lithic.domain.report

import java.math.BigDecimal
import java.time.LocalDate

enum class FinancialFactKind { PROJECT_OUTLAY, PROJECT_RECOVERY, SUBSCRIPTION_PAYMENT }

/** Absolute CNY cents and actual business date, mapped from validated stored rows in one read. */
data class FinancialFact(
    val id: String,
    val recordId: String,
    val kind: FinancialFactKind,
    val date: LocalDate,
    val amountCents: Long,
    val nonCash: Boolean = false,
    val operationId: String = "",
    val exchangeGroupId: String? = null,
) {
    val recordKind: OverviewKind get() = if (kind == FinancialFactKind.SUBSCRIPTION_PAYMENT)
        OverviewKind.SUBSCRIPTION else OverviewKind.PROJECT
}

data class ReportRecordIdentity(val id: String, val kind: OverviewKind, val name: String, val categoryId: String)
enum class ReportRecordStatus { NOT_STARTED, CURRENT, ENDED, ACTIVE, PAUSED, EXPIRED, STOPPED }
data class ReportRecord(val id: String, val kind: OverviewKind, val name: String, val categoryId: String,
    val status: ReportRecordStatus)

/** These are distinct measures, deliberately with no project-plus-subscription cash-total field. */
data class ReportAmounts(
    val projectOutlayCents: Long = 0,
    val projectRecoveryCents: Long = 0,
    val subscriptionPaidCents: Long = 0,
    val projectFactCount: Long = 0,
    val subscriptionPaymentCount: Long = 0,
    val containsNonCash: Boolean = false,
) {
    val projectNetCents: Long get() = Math.subtractExact(projectOutlayCents, projectRecoveryCents)
    val hasRecords: Boolean get() = projectFactCount > 0 || subscriptionPaymentCount > 0
}

data class AnnualCategoryReport(val category: OverviewCategory, val amounts: ReportAmounts)
data class AnnualReport(val year: Int, val amounts: ReportAmounts, val categories: List<AnnualCategoryReport>,
    val records: List<ReportRecord>)
data class CategoryYearReport(val year: Int, val amounts: ReportAmounts)
data class CategoryReport(val category: OverviewCategory, val currentRate: BigDecimal,
    val historical: ReportAmounts, val annual: List<CategoryYearReport>, val records: List<ReportRecord>)
