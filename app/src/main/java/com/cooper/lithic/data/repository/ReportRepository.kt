package com.cooper.lithic.data.repository

import androidx.room.withTransaction
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.report.OverviewCategory
import com.cooper.lithic.domain.report.OverviewEngine
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.OverviewProject
import com.cooper.lithic.domain.report.OverviewResult
import com.cooper.lithic.domain.report.OverviewSnapshot
import com.cooper.lithic.domain.report.OverviewSubscription
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A non-owning overview reader over the application's existing generation and coordinator. */
class ReportRepository(
    private val db: LithicDatabase,
    private val coordinator: LocalWriteCoordinator,
    private val clock: BusinessClock,
) {
    /** One observation day and one database snapshot for totals, counts and both category lists. */
    suspend fun loadOverview(asOf: LocalDate? = null): OverviewResult = coordinator.withSnapshot {
        val observedOn = asOf ?: clock.today()
        db.withTransaction {
                ReplacementStoredFacts.requireValid(db)
            check(db.projectDao().countOrphanEvents() == 0L) { "A money event refers to a missing project" }
            check(db.subscriptionDao().countOrphanRenewals() == 0L && db.subscriptionDao().countOrphanStates() == 0L) {
                "A payment or state refers to a missing subscription"
            }
            val categoryRows = db.categoryDao().list().associateBy { it.id }
            val categories = categoryRows.values.map {
                OverviewCategory(it.id, it.name, when (it.type) {
                    CategoryType.PROJECT -> OverviewKind.PROJECT
                    CategoryType.SUBSCRIPTION -> OverviewKind.SUBSCRIPTION
                })
            }
            var result = OverviewEngine.evaluate(OverviewSnapshot(categories, emptyList(), emptyList()), observedOn)
            val verifiedMedia = mutableSetOf<String>()
            val projectIds = mutableSetOf<String>()
            var projectCursor: ProjectCursor? = null
            while (true) {
                val rows = db.projectDao().pageProjects(projectCursor?.startedOn, projectCursor?.id, BATCH_SIZE)
                if (rows.isEmpty()) break
                check(rows.all { projectIds.add(it.id) }) { "A project identity was read twice" }
                val events = db.projectDao().eventsForProjects(rows.map { it.id }).groupBy { it.projectId }
                val inputs = rows.map { row ->
                    requireIdentity(row.id, row.name, row.categoryId, row.coverAssetId, row.createdAt, row.updatedAt,
                        CategoryType.PROJECT, categoryRows, verifiedMedia)
                    val input = projectSnapshot(row, events[row.id].orEmpty())
                    if (input.moneyEvents.any { it.occurredOn.isAfter(observedOn) }) {
                        ProjectCostEngine.evaluate(input, LocalDate.MAX)
                    }
                    OverviewProject(row.id, row.categoryId, input)
                }
                result = OverviewEngine.combine(result,
                    OverviewEngine.evaluate(OverviewSnapshot(categories, inputs, emptyList()), observedOn))
                projectCursor = rows.last().let { ProjectCursor(it.startedOn, it.id) }
                if (rows.size < BATCH_SIZE) break
            }
            check(projectIds.size.toLong() == db.projectDao().countProjects()) { "The overview omitted project identities" }

            val subscriptionIds = mutableSetOf<String>()
            var afterSubscriptionId: String? = null
            while (true) {
                val rows = db.subscriptionDao().validationSubscriptions(afterSubscriptionId, BATCH_SIZE)
                if (rows.isEmpty()) break
                check(rows.all { subscriptionIds.add(it.id) }) { "A subscription identity was read twice" }
                val ids = rows.map { it.id }
                val renewals = db.subscriptionDao().renewalsForSubscriptions(ids).groupBy { it.subscriptionId }
                val states = db.subscriptionDao().statesForSubscriptions(ids).groupBy { it.subscriptionId }
                val inputs = rows.map { row ->
                    requireIdentity(row.id, row.name, row.categoryId, row.coverAssetId, row.createdAt, row.updatedAt,
                        CategoryType.SUBSCRIPTION, categoryRows, verifiedMedia)
                    val input = subscriptionSnapshot(row, renewals[row.id].orEmpty(), states[row.id].orEmpty())
                    if (input.renewals.any { it.paidOn.isAfter(observedOn) }) {
                        SubscriptionCostEngine.evaluate(input, LocalDate.MAX)
                    }
                    OverviewSubscription(row.id, row.categoryId, input)
                }
                result = OverviewEngine.combine(result,
                    OverviewEngine.evaluate(OverviewSnapshot(categories, emptyList(), inputs), observedOn))
                afterSubscriptionId = rows.last().id
                if (rows.size < BATCH_SIZE) break
            }
            check(subscriptionIds.size.toLong() == db.subscriptionDao().countSubscriptions()) { "The overview omitted subscription identities" }
            result
        }
    }

    /** Includes the first read and fact/category edits that leave identity counts unchanged. */
    val changes: Flow<Unit> = db.invalidationTracker.createFlow(
        "long_term_projects", "project_money_events", "subscriptions", "subscription_renewals",
        "subscription_state_changes", "categories", "local_media_assets",
    ).map { Unit }

    private suspend fun requireIdentity(
        id: String,
        name: String,
        categoryId: String,
        coverId: String?,
        createdAt: Instant,
        updatedAt: Instant,
        type: CategoryType,
        categories: Map<String, CategoryEntity>,
        verifiedMedia: MutableSet<String>,
    ) {
        listOf(id, categoryId).plus(listOfNotNull(coverId)).forEach(StoredValueValidation::requireUuid)
        check(name.isNotBlank() && !updatedAt.isBefore(createdAt)) { "A stored business identity is invalid" }
        check(categories[categoryId]?.type == type) { "A stored business category is missing or has the wrong type" }
        if (coverId != null && coverId !in verifiedMedia) {
            checkNotNull(db.mediaDao().find(coverId)) { "The referenced cover metadata is missing" }
            verifiedMedia.add(coverId)
        }
    }

    private fun projectSnapshot(row: LongTermProjectEntity, events: List<ProjectMoneyEventEntity>): ProjectSnapshot {
        check((row.endedOn == null) == (row.endReason == null) && (row.endReason == null || row.endReason.isNotBlank())) {
            "The stored project end facts are invalid"
        }
        check(events.count { it.role == ProjectMoneyEventRole.INITIAL } == 1) { "A project must retain its single initial outlay" }
        check(events.count { it.role == ProjectMoneyEventRole.END_SETTLEMENT } <= 1) { "Legacy end settlements are ambiguous" }
        val input = LongTermProject(row.startedOn, row.endedOn)
        val facts = events.map { event ->
            listOf(event.id, event.projectId, event.operationId).forEach(StoredValueValidation::requireUuid)
            check(event.projectId == row.id) { "A money event belongs to another project" }
            when (event.role) {
                ProjectMoneyEventRole.INITIAL -> check(event.direction == MoneyDirection.OUTLAY) { "An initial event is not an outlay" }
                ProjectMoneyEventRole.END_SETTLEMENT -> check(row.endedOn != null && event.direction == MoneyDirection.RECOVERY) {
                    "An end settlement lacks its ended project or recovery direction"
                }
                ProjectMoneyEventRole.REGULAR -> Unit
            }
            ProjectMoneyEvent(event.direction, Money(event.amountCents), event.occurredOn)
        }
        return ProjectSnapshot(input, facts)
    }

    private fun subscriptionSnapshot(
        row: SubscriptionEntity,
        renewals: List<SubscriptionRenewalEntity>,
        states: List<SubscriptionStateChangeEntity>,
    ): SubscriptionSnapshot {
        check(renewals.isNotEmpty()) { "A stored subscription has no confirmed payment" }
        val paid = renewals.map { payment ->
            listOf(payment.id, payment.subscriptionId, payment.operationId).forEach(StoredValueValidation::requireUuid)
            check(payment.subscriptionId == row.id) { "A payment belongs to another subscription" }
            Renewal(Money(payment.amountCents), payment.cycle, payment.paidOn, payment.effectiveFrom,
                payment.paidUntilExclusive, payment.reminderDueOn, payment.standardDays)
        }
        val changes = states.map { state ->
            listOf(state.id, state.subscriptionId, state.operationId).forEach(StoredValueValidation::requireUuid)
            check(state.subscriptionId == row.id) { "A state belongs to another subscription" }
            SubscriptionStateChange(state.effectiveOn, state.state)
        }
        return SubscriptionSnapshot(Subscription(row.id), paid, changes)
    }

    private companion object { const val BATCH_SIZE = 50 }
}
