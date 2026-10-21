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
import com.cooper.lithic.domain.report.OverviewProject
import com.cooper.lithic.domain.report.OverviewSubscription
import com.cooper.lithic.domain.report.ProjectRankOrder
import com.cooper.lithic.domain.report.RankedProject
import com.cooper.lithic.domain.report.RankedSubscription
import com.cooper.lithic.domain.report.RankingEngine
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ReviewRanking { PROJECT_TIME, PROJECT_COST, SUBSCRIPTION_TIME }

data class ReviewRankItem(
    val type: CategoryType,
    val id: String,
    val name: String,
    val categoryName: String,
    val project: RankedProject? = null,
    val subscription: RankedSubscription? = null,
)

data class ReviewPage(
    val items: List<ReviewRankItem>,
    val totalCount: Long,
    val rankableCount: Long,
    val asOf: LocalDate,
    val ranking: ReviewRanking,
    val hasMore: Boolean,
)

/** Non-owning, read-only review facade over one existing generation and its write boundary. */
open class RankingRepository(
    private val db: LithicDatabase,
    private val coordinator: LocalWriteCoordinator,
    private val clock: BusinessClock,
) {
    /**
     * Read every relevant identity and associated fact before global ranking. Fifty is the display
     * page size, not a bound on calculation work: each fresh window computes the full type's list.
     * Neither ranks nor derived cost/time values are written back to the ledger.
     */
    open suspend fun loadWindow(
        ranking: ReviewRanking,
        pageCount: Int = 1,
        asOf: LocalDate? = null,
    ): ReviewPage {
        require(pageCount > 0) { "A review window must contain at least one page" }
        val limit = Math.multiplyExact(pageCount, BATCH_SIZE)
        return coordinator.withSnapshot {
            val observedOn = asOf ?: clock.today()
            db.withTransaction {
                ReplacementStoredFacts.requireValid(db)
                db.requireDefaults()
                val categories = db.categoryDao().list().associateBy { it.id }
                val verifiedMedia = mutableSetOf<String>()
                val all = when (ranking) {
                    ReviewRanking.PROJECT_TIME, ReviewRanking.PROJECT_COST ->
                        projectRanks(ranking, observedOn, categories, verifiedMedia)
                    ReviewRanking.SUBSCRIPTION_TIME -> subscriptionRanks(observedOn, categories, verifiedMedia)
                }
                val available = all.count { row ->
                    row.project?.eligible ?: (checkNotNull(row.subscription).cost.standardPurchasedDays > 0L)
                }
                ReviewPage(all.take(limit), all.size.toLong(), available.toLong(), observedOn, ranking, all.size > limit)
            }
        }
    }

    /** Identity counts alone miss corrections, same-day status edits and category remapping. */
    open val changes: Flow<Unit> = db.invalidationTracker.createFlow(
        "long_term_projects", "project_money_events", "subscriptions", "subscription_renewals",
        "subscription_state_changes", "categories", "local_media_assets",
    ).map { Unit }

    private suspend fun projectRanks(
        ranking: ReviewRanking,
        observedOn: LocalDate,
        categories: Map<String, CategoryEntity>,
        verifiedMedia: MutableSet<String>,
    ): List<ReviewRankItem> {
        check(db.projectDao().countOrphanEvents() == 0L) { "A money event refers to a missing project" }
        val identities = linkedMapOf<String, LongTermProjectEntity>()
        val inputs = mutableListOf<OverviewProject>()
        var afterStartedOn: LocalDate? = null
        var afterId: String? = null
        while (true) {
            val rows = db.projectDao().pageProjects(afterStartedOn, afterId, BATCH_SIZE)
            if (rows.isEmpty()) break
            val events = db.projectDao().eventsForProjects(rows.map { it.id }).groupBy { it.projectId }
            rows.forEach { row ->
                check(identities.put(row.id, row) == null) { "A review project identity was read twice" }
                requireIdentity(row.id, row.name, row.categoryId, row.coverAssetId, row.createdAt, row.updatedAt,
                    CategoryType.PROJECT, categories, verifiedMedia)
                val input = projectSnapshot(row, events[row.id].orEmpty())
                // Viewing the past must not skip validation of already persisted later facts.
                if (input.moneyEvents.any { it.occurredOn.isAfter(observedOn) }) {
                    ProjectCostEngine.evaluate(input, LocalDate.MAX)
                }
                inputs += OverviewProject(row.id, row.categoryId, input)
            }
            afterStartedOn = rows.last().startedOn
            afterId = rows.last().id
            if (rows.size < BATCH_SIZE) break
        }
        check(identities.size.toLong() == db.projectDao().countProjects()) { "The review omitted project identities" }
        val order = if (ranking == ReviewRanking.PROJECT_TIME) ProjectRankOrder.HELD_DAYS else ProjectRankOrder.DAILY_RATE
        return RankingEngine.projectRanks(inputs, observedOn, order).map { result ->
            val identity = identities.getValue(result.id)
            ReviewRankItem(CategoryType.PROJECT, result.id, identity.name,
                categories.getValue(identity.categoryId).name, project = result)
        }
    }

    private suspend fun subscriptionRanks(
        observedOn: LocalDate,
        categories: Map<String, CategoryEntity>,
        verifiedMedia: MutableSet<String>,
    ): List<ReviewRankItem> {
        check(db.subscriptionDao().countOrphanRenewals() == 0L && db.subscriptionDao().countOrphanStates() == 0L) {
            "A payment or state refers to a missing subscription"
        }
        val identities = linkedMapOf<String, SubscriptionEntity>()
        val inputs = mutableListOf<OverviewSubscription>()
        var afterId: String? = null
        while (true) {
            val rows = db.subscriptionDao().validationSubscriptions(afterId, BATCH_SIZE)
            if (rows.isEmpty()) break
            val ids = rows.map { it.id }
            val renewals = db.subscriptionDao().renewalsForSubscriptions(ids).groupBy { it.subscriptionId }
            val states = db.subscriptionDao().statesForSubscriptions(ids).groupBy { it.subscriptionId }
            rows.forEach { row ->
                check(identities.put(row.id, row) == null) { "A review subscription identity was read twice" }
                requireIdentity(row.id, row.name, row.categoryId, row.coverAssetId, row.createdAt, row.updatedAt,
                    CategoryType.SUBSCRIPTION, categories, verifiedMedia)
                val input = subscriptionSnapshot(row, renewals[row.id].orEmpty(), states[row.id].orEmpty())
                if (input.renewals.any { it.paidOn.isAfter(observedOn) }) {
                    SubscriptionCostEngine.evaluate(input, LocalDate.MAX)
                }
                inputs += OverviewSubscription(row.id, row.categoryId, input)
            }
            afterId = rows.last().id
            if (rows.size < BATCH_SIZE) break
        }
        check(identities.size.toLong() == db.subscriptionDao().countSubscriptions()) { "The review omitted subscription identities" }
        return RankingEngine.subscriptionRanks(inputs, observedOn).map { result ->
            val identity = identities.getValue(result.id)
            ReviewRankItem(CategoryType.SUBSCRIPTION, result.id, identity.name,
                categories.getValue(identity.categoryId).name, subscription = result)
        }
    }

    private suspend fun requireIdentity(
        id: String, name: String, categoryId: String, coverId: String?, createdAt: Instant, updatedAt: Instant,
        type: CategoryType, categories: Map<String, CategoryEntity>, verifiedMedia: MutableSet<String>,
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
        return ProjectSnapshot(LongTermProject(row.startedOn, row.endedOn), facts)
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
