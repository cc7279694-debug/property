package com.cooper.lithic.data.repository

import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.domain.report.ProjectTimeline
import com.cooper.lithic.domain.report.ProjectTimelineEngine
import com.cooper.lithic.domain.report.SubscriptionTimeline
import com.cooper.lithic.domain.report.SubscriptionTimelineEngine
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.merge

data class ProjectTimelineData(val detail: ProjectDetail, val snapshot: ProjectSnapshot, val timeline: ProjectTimeline)
data class SubscriptionTimelineData(val detail: SubscriptionDetail, val snapshot: SubscriptionSnapshot, val timeline: SubscriptionTimeline)

/** Non-owning read adapter. Existing detail reads hold the generation coordinator and Room
 * transaction, validate all stored facts, and return every associated event, not the first page.
 * Sampling and exact selections subsequently operate only on this immutable captured dataset. */
open class ItemTimelineRepository(
    private val projects: ProjectRepository,
    private val subscriptions: SubscriptionRepository,
    private val clock: BusinessClock,
) {
    open suspend fun loadProject(id: String, asOf: LocalDate = clock.today()): ProjectTimelineData? {
        val detail = projects.loadProjectDetail(id, asOf) ?: return null
        val snapshot = ProjectSnapshot(LongTermProject(detail.project.startedOn, detail.project.endedOn),
            detail.events.map { ProjectMoneyEvent(it.direction, Money(it.amountCents), it.occurredOn) })
        return ProjectTimelineData(detail, snapshot, ProjectTimelineEngine.build(snapshot, asOf))
    }

    open suspend fun loadSubscription(id: String, asOf: LocalDate = clock.today()): SubscriptionTimelineData? {
        val detail = subscriptions.loadSubscriptionDetail(id, asOf) ?: return null
        val snapshot = SubscriptionSnapshot(Subscription(detail.subscription.id), detail.renewals.map {
            Renewal(Money(it.amountCents), it.cycle, it.paidOn, it.effectiveFrom,
                it.paidUntilExclusive, it.reminderDueOn, it.standardDays)
        }, detail.stateChanges.map { SubscriptionStateChange(it.effectiveOn, it.state) })
        return SubscriptionTimelineData(detail, snapshot, SubscriptionTimelineEngine.build(snapshot, asOf))
    }

    open val changes: Flow<Unit> = merge(projects.changes, subscriptions.changes).conflate()
}
