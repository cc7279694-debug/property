package com.cooper.lithic.domain.report

import java.math.BigDecimal
import java.time.LocalDate

/** One immutable, complete ledger observation; money facts remain in the existing overview model. */
data class LedgerSnapshot(
    val overview: OverviewSnapshot,
    val events: List<TotalCostEvent> = emptyList(),
)

data class TotalCostEvent(
    val id: String,
    val date: LocalDate,
    val kind: OverviewKind,
    val recordId: String,
    val recordName: String,
    val title: String,
    val description: String,
    val amountCents: Long? = null,
    val nonCash: Boolean = false,
)

data class CostTimelinePoint(
    val date: LocalDate,
    val projectRate: BigDecimal,
    val subscriptionRate: BigDecimal,
    val totalRate: BigDecimal,
    val relatedEventIds: List<String> = emptyList(),
    val projectCount: Long = 0,
    val subscriptionCount: Long = 0,
    val activeProjectCount: Long = 0,
    val activeSubscriptionCount: Long = 0,
    val hasPositiveContribution: Boolean = false,
    val hasNegativeContribution: Boolean = false,
) {
    val hasRecords: Boolean get() = projectCount > 0 || subscriptionCount > 0
    val hasActiveContributions: Boolean get() = activeProjectCount > 0 || activeSubscriptionCount > 0
    val isCancellationZero: Boolean get() = totalRate.signum() == 0 && hasPositiveContribution && hasNegativeContribution
}

data class CostTimeline(
    val asOf: LocalDate,
    val from: LocalDate?,
    val points: List<CostTimelinePoint>,
    val events: List<TotalCostEvent>,
    /** Budget for bucket extrema, excluding the endpoints and all real boundaries/neighbours. */
    val basePointBudget: Int,
    /** Full inclusive natural-day scan; retained display points do not bound computation. */
    val evaluatedDayCount: Long,
    val sampled: Boolean,
)
