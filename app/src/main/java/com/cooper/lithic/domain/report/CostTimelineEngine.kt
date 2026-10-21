package com.cooper.lithic.domain.report

import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import java.time.LocalDate

object CostTimelineEngine {
    /** Pure historical observations of the same complete facts as the home overview. */
    fun evaluate(snapshot: LedgerSnapshot, dates: List<LocalDate>): List<CostTimelinePoint> {
        val prepared = prepare(snapshot)
        return dates.map(prepared::point)
    }

    /** Exact arbitrary-day queries do not interpolate retained display points. */
    fun point(snapshot: LedgerSnapshot, date: LocalDate): CostTimelinePoint = prepare(snapshot).point(date)

    internal fun prepare(snapshot: LedgerSnapshot): PreparedLedger {
        // An empty observation list/ledger must not bypass the home's category, identity and
        // reference validation. MIN cannot pull a later real payment into an earlier date;
        // actual observations are still evaluated separately with their own business day.
        OverviewEngine.evaluate(snapshot.overview, LocalDate.MIN)
        val projectIds = snapshot.overview.projects.map { it.id }.toSet()
        val subscriptionIds = snapshot.overview.subscriptions.map { it.id }.toSet()
        val eventIds = mutableSetOf<String>()
        snapshot.events.forEach { event ->
            require(event.id.isNotBlank() && eventIds.add(event.id)) { "Related event identities must be nonblank and unique" }
            require(event.recordName.isNotBlank() && event.title.isNotBlank()) { "A related event needs its record name and description" }
            require(when (event.kind) {
                OverviewKind.PROJECT -> event.recordId in projectIds
                OverviewKind.SUBSCRIPTION -> event.recordId in subscriptionIds
            }) { "A related event must belong to a record of the matching type" }
            require(event.amountCents == null || event.amountCents >= 0L) { "A displayed payment amount is an absolute number of cents" }
        }
        return PreparedLedger(snapshot, snapshot.events.groupBy { it.date }
            .mapValues { (_, events) -> events.map { it.id }.sorted() })
    }

    /** Event indexing is performed once per series scan, not once for every calendar day. */
    internal class PreparedLedger(
        private val snapshot: LedgerSnapshot,
        private val relatedByDate: Map<LocalDate, List<String>>,
    ) {
        fun point(date: LocalDate): CostTimelinePoint {
            val overview = OverviewEngine.evaluate(snapshot.overview, date)
            // These flags explain zero/cancellation; all money and status rules still come
            // from the frozen engines. They are not a second cost calculation formula.
            var positive = false
            var negative = false
            snapshot.overview.projects.forEach { project ->
                val cost = ProjectCostEngine.evaluate(project.snapshot, date)
                if (cost.contributesToCurrentTotal) {
                    val sign = requireNotNull(cost.dailyRate).signum()
                    positive = positive || sign > 0
                    negative = negative || sign < 0
                }
            }
            snapshot.overview.subscriptions.forEach { subscription ->
                val cost = SubscriptionCostEngine.evaluate(subscription.snapshot, date)
                if (cost.displayStatus == SubscriptionDisplayStatus.ACTIVE) {
                    positive = positive || cost.currentRate.signum() > 0
                    negative = negative || cost.currentRate.signum() < 0
                }
            }
            return CostTimelinePoint(date, overview.projectRate, overview.subscriptionRate, overview.totalRate,
                relatedByDate[date].orEmpty(), overview.projectCount, overview.subscriptionCount,
                overview.activeProjectCount, overview.activeSubscriptionCount, positive, negative)
        }
    }
}
