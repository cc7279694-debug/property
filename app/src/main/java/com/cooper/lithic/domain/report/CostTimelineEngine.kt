package com.cooper.lithic.domain.report

import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import java.math.BigDecimal
import java.time.LocalDate

object CostTimelineEngine {
    /** Pure historical observations of the same complete facts as the home overview. */
    fun evaluate(snapshot: LedgerSnapshot, dates: List<LocalDate>): List<CostTimelinePoint> {
        val prepared = prepare(snapshot)
        return dates.map { prepared.point(it) }
    }

    /** Exact arbitrary-day queries do not interpolate retained display points. */
    fun point(snapshot: LedgerSnapshot, date: LocalDate): CostTimelinePoint = prepare(snapshot).point(date)

    internal fun point(snapshot: LedgerSnapshot, date: LocalDate, cancellationCheck: () -> Unit): CostTimelinePoint =
        prepare(snapshot, cancellationCheck).point(date, cancellationCheck)

    internal fun prepare(snapshot: LedgerSnapshot, cancellationCheck: () -> Unit = {}): PreparedLedger {
        // An empty observation list/ledger must not bypass the home's category, identity and
        // reference validation. MIN cannot pull a later real payment into an earlier date;
        // actual observations are still evaluated separately with their own business day.
        cancellationCheck()
        OverviewEngine.evaluate(snapshot.overview, LocalDate.MIN, cancellationCheck)
        val projectIds = snapshot.overview.projects.map { it.id }.toSet()
        val subscriptionIds = snapshot.overview.subscriptions.map { it.id }.toSet()
        val eventIds = mutableSetOf<String>()
        snapshot.events.forEach { event ->
            cancellationCheck()
            require(event.id.isNotBlank() && eventIds.add(event.id)) { "Related event identities must be nonblank and unique" }
            require(event.recordName.isNotBlank() && event.title.isNotBlank()) { "A related event needs its record name and description" }
            require(when (event.kind) {
                OverviewKind.PROJECT -> event.recordId in projectIds
                OverviewKind.SUBSCRIPTION -> event.recordId in subscriptionIds
            }) { "A related event must belong to a record of the matching type" }
            require(event.amountCents == null || event.amountCents >= 0L) { "A displayed payment amount is an absolute number of cents" }
        }
        return PreparedLedger(snapshot, snapshot.events.groupBy { cancellationCheck(); it.date }
            .mapValues { (_, events) -> cancellationCheck(); events.map { it.id }.sorted() }, cancellationCheck)
    }

    /** All indexes belong to this single captured ledger. Nothing survives a new prepare(). */
    internal class PreparedLedger(
        private val snapshot: LedgerSnapshot,
        private val relatedByDate: Map<LocalDate, List<String>>,
        cancellationCheck: () -> Unit,
    ) {
        private val subscriptions = snapshot.overview.subscriptions.map { cancellationCheck(); PreparedSubscription(it.snapshot, cancellationCheck) }

        fun point(date: LocalDate, cancellationCheck: () -> Unit = {}): CostTimelinePoint {
            var projectRate = BigDecimal.ZERO
            var subscriptionRate = BigDecimal.ZERO
            var activeProjects = 0L
            var activeSubscriptions = 0L
            var positive = false
            var negative = false
            // Consume each frozen project result once, including inactive results: validation
            // and overflow must not disappear merely because a record contributes no money.
            snapshot.overview.projects.forEach { project ->
                cancellationCheck()
                val cost = ProjectCostEngine.evaluate(project.snapshot, date)
                if (cost.contributesToCurrentTotal) {
                    val rate = requireNotNull(cost.dailyRate)
                    projectRate = projectRate.add(rate)
                    activeProjects = Math.incrementExact(activeProjects)
                    val sign = rate.signum()
                    positive = positive || sign > 0
                    negative = negative || sign < 0
                }
            }
            subscriptions.forEach { subscription ->
                cancellationCheck()
                val contribution = subscription.at(date)
                if (contribution.active) {
                    subscriptionRate = subscriptionRate.add(contribution.rate)
                    activeSubscriptions = Math.incrementExact(activeSubscriptions)
                    positive = positive || contribution.rate.signum() > 0
                    negative = negative || contribution.rate.signum() < 0
                }
            }
            return CostTimelinePoint(date, projectRate, subscriptionRate, projectRate.add(subscriptionRate),
                relatedByDate[date].orEmpty(), snapshot.overview.projects.size.toLong(), snapshot.overview.subscriptions.size.toLong(),
                activeProjects, activeSubscriptions, positive, negative)
        }
    }

    private data class CurrentContribution(val active: Boolean, val rate: BigDecimal)

    /** Only current contribution/status are constant between these exact fact boundaries.
     * Do not cache historicalAverage or elapsedPaidDays here: those have other semantics.
     * The original engine still validates/evaluates every first observation of an interval.
     */
    private class PreparedSubscription(private val snapshot: SubscriptionSnapshot, cancellationCheck: () -> Unit) {
        private val boundaries = (snapshot.renewals.asSequence().flatMap { renewal ->
            cancellationCheck()
            sequenceOf(renewal.paidOn, renewal.effectiveFrom, renewal.paidUntilExclusive)
        } + snapshot.stateChanges.asSequence().map { cancellationCheck(); it.effectiveOn })
            .map(LocalDate::toEpochDay).distinct().sorted().toList().toLongArray()
        private val contributions = mutableMapOf<Int, CurrentContribution>()

        fun at(date: LocalDate): CurrentContribution {
            val position = boundaries.binarySearch(date.toEpochDay())
            val interval = if (position >= 0) position else -position - 2
            return contributions.getOrPut(interval) {
                val cost = SubscriptionCostEngine.evaluate(snapshot, date)
                CurrentContribution(cost.displayStatus == SubscriptionDisplayStatus.ACTIVE, cost.currentRate)
            }
        }
    }
}
