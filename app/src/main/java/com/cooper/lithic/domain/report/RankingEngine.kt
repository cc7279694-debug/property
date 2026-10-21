package com.cooper.lithic.domain.report

import com.cooper.lithic.core.model.ProjectCostResult
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionCostResult
import java.time.LocalDate

enum class ProjectRankOrder { HELD_DAYS, DAILY_RATE }

data class RankedProject(
    val id: String,
    val categoryId: String,
    val cost: ProjectCostResult,
    val isHistory: Boolean,
) {
    val eligible: Boolean get() = cost.unavailableReason == null && cost.heldDays > 0L && cost.dailyRate != null
}

data class RankedSubscription(
    val id: String,
    val categoryId: String,
    val cost: SubscriptionCostResult,
)

object RankingEngine {
    /** Calculate the entire input before any display window; the frozen engine owns cost and date rules. */
    fun projectRanks(
        projects: List<OverviewProject>,
        asOf: LocalDate,
        order: ProjectRankOrder = ProjectRankOrder.HELD_DAYS,
    ): List<RankedProject> = projects.map { input ->
        RankedProject(
            id = input.id,
            categoryId = input.categoryId,
            cost = ProjectCostEngine.evaluate(input.snapshot, asOf),
            isHistory = input.snapshot.project.endedOn?.let { !it.isAfter(asOf) } == true,
        )
    }.sortedWith { left, right ->
        when {
            left.eligible != right.eligible -> if (left.eligible) -1 else 1
            !left.eligible -> left.id.compareTo(right.id)
            else -> {
                val metricOrder = when (order) {
                    ProjectRankOrder.HELD_DAYS -> right.cost.heldDays.compareTo(left.cost.heldDays)
                    ProjectRankOrder.DAILY_RATE -> requireNotNull(left.cost.dailyRate)
                        .compareTo(requireNotNull(right.cost.dailyRate))
                }
                if (metricOrder == 0) left.id.compareTo(right.id) else metricOrder
            }
        }
    }

    /** Purchased standard duration is independent of elapsed coverage and the current contribution. */
    fun subscriptionRanks(
        subscriptions: List<OverviewSubscription>,
        asOf: LocalDate,
    ): List<RankedSubscription> = subscriptions.map { input ->
        RankedSubscription(
            id = input.id,
            categoryId = input.categoryId,
            cost = SubscriptionCostEngine.evaluate(input.snapshot, asOf),
        )
    }.sortedWith { left, right ->
        val metricOrder = right.cost.standardPurchasedDays.compareTo(left.cost.standardPurchasedDays)
        if (metricOrder == 0) left.id.compareTo(right.id) else metricOrder
    }
}
