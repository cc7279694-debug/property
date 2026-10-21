package com.cooper.lithic.data.repository

import androidx.room.withTransaction
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class SearchStatus { PROJECT_CURRENT, PROJECT_NOT_STARTED, PROJECT_ENDED, ACTIVE, PAUSED, EXPIRED, STOPPED }

data class SearchItem(
    val type: CategoryType,
    val id: String,
    val name: String,
    val categoryName: String,
    val status: SearchStatus,
    val isHistory: Boolean,
)

data class SearchCursor(val query: String, val asOf: LocalDate, val type: CategoryType, val id: String)
data class SearchPage(
    val items: List<SearchItem>,
    val nextCursor: SearchCursor?,
    val totalCount: Long,
    val asOf: LocalDate,
    val query: String,
)

/** Same-generation facade; no owner, derived cache, FTS, or independent cost formula. */
open class SearchRepository(
    private val db: LithicDatabase,
    private val coordinator: LocalWriteCoordinator,
    private val clock: BusinessClock,
) {
    /** Rebuild all requested pages in one snapshot after writes, never append an old query's page. */
    open suspend fun loadWindow(query: String, pageCount: Int, asOf: LocalDate? = null): SearchPage {
        val normalized = SearchNames.query(query)
        require(pageCount > 0) { "A search window must contain at least one page" }
        return coordinator.withSnapshot {
            val observedOn = asOf ?: clock.today()
            db.withTransaction {
                ReplacementStoredFacts.requireValid(db)
                if (normalized.isEmpty()) return@withTransaction empty(normalized, observedOn)
                val pattern = SearchNames.literalPattern(normalized)
                val count = db.searchDao().countNames(pattern)
                val items = mutableListOf<SearchItem>()
                var cursor: SearchCursor? = null
                for (index in 0 until pageCount) {
                    val page = pageInTransaction(normalized, pattern, 50, cursor, count, observedOn)
                    items += page.items
                    cursor = page.nextCursor
                    if (cursor == null) break
                }
                SearchPage(items, cursor, count, observedOn, normalized)
            }
        }
    }

    open suspend fun searchNames(query: String, limit: Int = 50, cursor: SearchCursor? = null, asOf: LocalDate? = null): SearchPage {
        val normalized = SearchNames.query(query)
        require(limit in 1..50) { "A search page contains between 1 and 50 results" }
        cursor?.let { StoredValueValidation.requireUuid(it.id) }
        return coordinator.withSnapshot {
            val observedOn = asOf ?: clock.today()
            require(cursor == null || (cursor.query == normalized && cursor.asOf == observedOn)) {
                "A search cursor belongs to one query and observation day"
            }
            db.withTransaction {
                ReplacementStoredFacts.requireValid(db)
                if (normalized.isEmpty()) return@withTransaction empty(normalized, observedOn)
                val pattern = SearchNames.literalPattern(normalized)
                pageInTransaction(normalized, pattern, limit, cursor, db.searchDao().countNames(pattern), observedOn)
            }
        }
    }

    open val changes: Flow<Unit> = db.invalidationTracker.createFlow(
        "long_term_projects", "project_money_events", "subscriptions", "subscription_renewals",
        "subscription_state_changes", "categories",
    ).map { Unit }

    private suspend fun pageInTransaction(query: String, pattern: String, limit: Int, cursor: SearchCursor?, count: Long, asOf: LocalDate): SearchPage {
        val queried = db.searchDao().pageNames(pattern, cursor?.type?.name, cursor?.id, limit + 1)
        val visible = queried.take(limit)
        if (visible.isEmpty()) return SearchPage(emptyList(), null, count, asOf, query)
        val projectIds = visible.filter { it.type == CategoryType.PROJECT }.map { it.id }
        val subscriptionIds = visible.filter { it.type == CategoryType.SUBSCRIPTION }.map { it.id }
        val projects = if (projectIds.isEmpty()) emptyMap() else db.projectDao().projectsForIds(projectIds).associateBy { it.id }
        val subscriptions = if (subscriptionIds.isEmpty()) emptyMap() else db.subscriptionDao().subscriptionsForIds(subscriptionIds).associateBy { it.id }
        val categoryIds = (projects.values.map { it.categoryId } + subscriptions.values.map { it.categoryId }).distinct()
        val categories = db.projectDao().categoriesForProjects(categoryIds).associateBy { it.id }
        val events = if (projectIds.isEmpty()) emptyMap() else db.projectDao().eventsForProjects(projectIds).groupBy { it.projectId }
        val renewals = if (subscriptionIds.isEmpty()) emptyMap() else db.subscriptionDao().renewalsForSubscriptions(subscriptionIds).groupBy { it.subscriptionId }
        val states = if (subscriptionIds.isEmpty()) emptyMap() else db.subscriptionDao().statesForSubscriptions(subscriptionIds).groupBy { it.subscriptionId }
        val items = visible.map { key ->
            StoredValueValidation.requireUuid(key.id)
            if (key.type == CategoryType.PROJECT) {
                val row = checkNotNull(projects[key.id]) { "A search project disappeared inside its snapshot" }
                val category = checkNotNull(categories[row.categoryId]) { "A search project category is missing" }
                check(category.type == CategoryType.PROJECT && row.name.isNotBlank() && !row.updatedAt.isBefore(row.createdAt)) { "The project identity/category is invalid" }
                val facts = events[row.id].orEmpty()
                facts.forEach { event ->
                    listOf(event.id, event.projectId, event.operationId).forEach(StoredValueValidation::requireUuid)
                    check(event.projectId == row.id) { "A money event belongs to another identity" }
                }
                val cost = ProjectCostEngine.evaluate(ProjectSnapshot(LongTermProject(row.startedOn, row.endedOn),
                    facts.map { ProjectMoneyEvent(it.direction, Money(it.amountCents), it.occurredOn) }), asOf)
                val history = row.endedOn != null && !row.endedOn.isAfter(asOf)
                check(!history || !cost.contributesToCurrentTotal) { "Historical project classification disagrees with its engine" }
                SearchItem(key.type, row.id, row.name, category.name,
                    when {
                        history -> SearchStatus.PROJECT_ENDED
                        cost.unavailableReason == com.cooper.lithic.core.model.CostUnavailableReason.PROJECT_NOT_STARTED -> SearchStatus.PROJECT_NOT_STARTED
                        else -> SearchStatus.PROJECT_CURRENT
                    }, history)
            } else {
                val row = checkNotNull(subscriptions[key.id]) { "A search subscription disappeared inside its snapshot" }
                val category = checkNotNull(categories[row.categoryId]) { "A search subscription category is missing" }
                check(category.type == CategoryType.SUBSCRIPTION && row.name.isNotBlank() && !row.updatedAt.isBefore(row.createdAt)) { "The subscription identity/category is invalid" }
                val paid = renewals[row.id].orEmpty()
                val changes = states[row.id].orEmpty()
                check(paid.isNotEmpty()) { "A stored subscription has no confirmed payment" }
                paid.forEach {
                    listOf(it.id, it.subscriptionId, it.operationId).forEach(StoredValueValidation::requireUuid)
                    check(it.subscriptionId == row.id) { "A payment belongs to another identity" }
                }
                changes.forEach {
                    listOf(it.id, it.subscriptionId, it.operationId).forEach(StoredValueValidation::requireUuid)
                    check(it.subscriptionId == row.id) { "A state belongs to another identity" }
                }
                val cost = SubscriptionCostEngine.evaluate(SubscriptionSnapshot(Subscription(row.id), paid.map {
                    Renewal(Money(it.amountCents), it.cycle, it.paidOn, it.effectiveFrom, it.paidUntilExclusive, it.reminderDueOn, it.standardDays)
                }, changes.map { SubscriptionStateChange(it.effectiveOn, it.state) }), asOf)
                SearchItem(key.type, row.id, row.name, category.name, when (cost.displayStatus) {
                    SubscriptionDisplayStatus.ACTIVE -> SearchStatus.ACTIVE
                    SubscriptionDisplayStatus.PAUSED -> SearchStatus.PAUSED
                    SubscriptionDisplayStatus.EXPIRED -> SearchStatus.EXPIRED
                    SubscriptionDisplayStatus.STOPPED -> SearchStatus.STOPPED
                }, cost.displayStatus == SubscriptionDisplayStatus.STOPPED)
            }
        }
        val next = if (queried.size > limit) visible.last().let { SearchCursor(query, asOf, it.type, it.id) } else null
        return SearchPage(items, next, count, asOf, query)
    }

    private fun empty(query: String, asOf: LocalDate) = SearchPage(emptyList(), null, 0, asOf, query)
}
