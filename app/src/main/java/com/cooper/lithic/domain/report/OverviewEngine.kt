package com.cooper.lithic.domain.report

import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import java.math.BigDecimal
import java.time.LocalDate

enum class OverviewKind { PROJECT, SUBSCRIPTION }

data class OverviewCategory(val id: String, val name: String, val kind: OverviewKind)
data class OverviewProject(val id: String, val categoryId: String, val snapshot: ProjectSnapshot)
data class OverviewSubscription(val id: String, val categoryId: String, val snapshot: SubscriptionSnapshot)
data class OverviewSnapshot(
    val categories: List<OverviewCategory>,
    val projects: List<OverviewProject>,
    val subscriptions: List<OverviewSubscription>,
)

data class CategoryDailyCost(
    val categoryId: String,
    val name: String,
    val dailyRate: BigDecimal,
    val contributingCount: Long,
)

data class OverviewResult(
    val asOf: LocalDate,
    val totalRate: BigDecimal,
    val projectRate: BigDecimal,
    val subscriptionRate: BigDecimal,
    val projectCount: Long,
    val subscriptionCount: Long,
    val activeProjectCount: Long,
    val activeSubscriptionCount: Long,
    val projectCategories: List<CategoryDailyCost>,
    val subscriptionCategories: List<CategoryDailyCost>,
) {
    val hasRecords: Boolean get() = projectCount > 0L || subscriptionCount > 0L
}

object OverviewEngine {
    /** Frozen engines own cost rules and precision; aggregation adds their values without further rounding. */
    fun evaluate(input: OverviewSnapshot, asOf: LocalDate): OverviewResult {
        validate(input)
        val projectCategories = emptyCategories(input.categories, OverviewKind.PROJECT)
        val subscriptionCategories = emptyCategories(input.categories, OverviewKind.SUBSCRIPTION)
        var activeProjectCount = 0L
        var activeSubscriptionCount = 0L
        for (project in input.projects) {
            val result = ProjectCostEngine.evaluate(project.snapshot, asOf)
            if (result.contributesToCurrentTotal) {
                addContribution(projectCategories, project.categoryId, requireNotNull(result.dailyRate))
                activeProjectCount = Math.incrementExact(activeProjectCount)
            }
        }
        for (subscription in input.subscriptions) {
            val result = SubscriptionCostEngine.evaluate(subscription.snapshot, asOf)
            if (result.displayStatus == SubscriptionDisplayStatus.ACTIVE) {
                addContribution(subscriptionCategories, subscription.categoryId, result.currentRate)
                activeSubscriptionCount = Math.incrementExact(activeSubscriptionCount)
            }
        }
        val projects = sortedCategories(projectCategories.values)
        val subscriptions = sortedCategories(subscriptionCategories.values)
        val projectRate = sumCategories(projects)
        val subscriptionRate = sumCategories(subscriptions)
        return OverviewResult(
            asOf = asOf,
            totalRate = projectRate.add(subscriptionRate),
            projectRate = projectRate,
            subscriptionRate = subscriptionRate,
            projectCount = input.projects.size.toLong(),
            subscriptionCount = input.subscriptions.size.toLong(),
            activeProjectCount = activeProjectCount,
            activeSubscriptionCount = activeSubscriptionCount,
            projectCategories = projects,
            subscriptionCategories = subscriptions,
        )
    }

    /** Combine bounded, disjoint identity batches from one observation and one data generation. */
    fun combine(first: OverviewResult, second: OverviewResult): OverviewResult {
        require(first.asOf == second.asOf) { "Overview batches must share the same observation date" }
        return OverviewResult(
            asOf = first.asOf,
            totalRate = first.totalRate.add(second.totalRate),
            projectRate = first.projectRate.add(second.projectRate),
            subscriptionRate = first.subscriptionRate.add(second.subscriptionRate),
            projectCount = Math.addExact(first.projectCount, second.projectCount),
            subscriptionCount = Math.addExact(first.subscriptionCount, second.subscriptionCount),
            activeProjectCount = Math.addExact(first.activeProjectCount, second.activeProjectCount),
            activeSubscriptionCount = Math.addExact(first.activeSubscriptionCount, second.activeSubscriptionCount),
            projectCategories = combineCategories(first.projectCategories, second.projectCategories),
            subscriptionCategories = combineCategories(first.subscriptionCategories, second.subscriptionCategories),
        )
    }

    private fun validate(input: OverviewSnapshot) {
        require(input.categories.all { it.id.isNotBlank() && it.name.isNotBlank() }) {
            "Overview categories need a nonblank identity and name"
        }
        require(input.categories.map { it.id }.toSet().size == input.categories.size) {
            "Overview category identities must be unique"
        }
        require(input.projects.all { it.id.isNotBlank() } && input.subscriptions.all { it.id.isNotBlank() }) {
            "Overview record identities cannot be blank"
        }
        require(input.projects.map { it.id }.toSet().size == input.projects.size) {
            "Overview project identities must be unique within the project batch"
        }
        require(input.subscriptions.map { it.id }.toSet().size == input.subscriptions.size) {
            "Overview subscription identities must be unique within the subscription batch"
        }
        val categories = input.categories.associateBy { it.id }
        require(input.projects.all { categories[it.categoryId]?.kind == OverviewKind.PROJECT }) {
            "Each overview project must refer to an existing project category"
        }
        require(input.subscriptions.all { categories[it.categoryId]?.kind == OverviewKind.SUBSCRIPTION }) {
            "Each overview subscription must refer to an existing subscription category"
        }
        require(input.subscriptions.all { it.id == it.snapshot.subscription.id }) {
            "Overview subscription identity must match its cost snapshot"
        }
    }

    private fun emptyCategories(categories: List<OverviewCategory>, kind: OverviewKind) = categories
        .filter { it.kind == kind }
        .associate { it.id to CategoryDailyCost(it.id, it.name, BigDecimal.ZERO, 0L) }
        .toMutableMap()

    private fun addContribution(categories: MutableMap<String, CategoryDailyCost>, categoryId: String, rate: BigDecimal) {
        val category = categories.getValue(categoryId)
        categories[categoryId] = category.copy(
            dailyRate = category.dailyRate.add(rate),
            contributingCount = Math.incrementExact(category.contributingCount),
        )
    }

    private fun combineCategories(first: List<CategoryDailyCost>, second: List<CategoryDailyCost>): List<CategoryDailyCost> {
        val categories = mutableMapOf<String, CategoryDailyCost>()
        for (category in first + second) {
            val previous = categories[category.categoryId]
            if (previous == null) {
                categories[category.categoryId] = category
            } else {
                require(previous.name == category.name) { "A category cannot change name between overview batches" }
                categories[category.categoryId] = previous.copy(
                    dailyRate = previous.dailyRate.add(category.dailyRate),
                    contributingCount = Math.addExact(previous.contributingCount, category.contributingCount),
                )
            }
        }
        return sortedCategories(categories.values)
    }

    private fun sortedCategories(categories: Collection<CategoryDailyCost>): List<CategoryDailyCost> =
        categories.sortedWith(compareBy<CategoryDailyCost> { it.name }.thenBy { it.categoryId })

    private fun sumCategories(categories: List<CategoryDailyCost>): BigDecimal =
        categories.fold(BigDecimal.ZERO) { sum, category -> sum.add(category.dailyRate) }
}
