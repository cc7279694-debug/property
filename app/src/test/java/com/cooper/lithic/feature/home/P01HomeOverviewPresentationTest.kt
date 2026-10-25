package com.cooper.lithic.feature.home

import com.cooper.lithic.domain.report.OverviewResult
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class P01HomeOverviewPresentationTest {
    @Test fun unavailable_cost_snapshot_never_invents_a_value() {
        assertEquals(HomeOverviewStatus.UNAVAILABLE, homeOverviewStatus(null))
    }

    @Test fun known_empty_ledger_has_its_own_state() {
        assertEquals(HomeOverviewStatus.EMPTY, homeOverviewStatus(result()))
    }

    @Test fun existing_history_with_no_current_contribution_is_not_empty() {
        assertEquals(HomeOverviewStatus.NO_CURRENT, homeOverviewStatus(result(projects = 2)))
    }

    @Test fun active_zero_cost_is_still_current() {
        assertEquals(HomeOverviewStatus.CURRENT, homeOverviewStatus(result(projects = 1, activeProjects = 1)))
    }

    @Test fun negative_cost_is_preserved_without_changing_state_or_value() {
        val overview = result(total = "-0.005", projects = 1, activeProjects = 1)
        assertEquals(HomeOverviewStatus.CURRENT, homeOverviewStatus(overview))
        assertEquals(BigDecimal("-0.005"), overview.totalRate)
    }

    @Test fun subscription_only_ledger_is_current_without_a_project() {
        assertEquals(HomeOverviewStatus.CURRENT,
            homeOverviewStatus(result(subscriptions = 1, activeSubscriptions = 1)))
    }

    private fun result(total: String = "0", projects: Long = 0, subscriptions: Long = 0,
        activeProjects: Long = 0, activeSubscriptions: Long = 0) = OverviewResult(
        LocalDate.of(2026, 10, 25), BigDecimal(total), BigDecimal.ZERO, BigDecimal(total),
        projects, subscriptions, activeProjects, activeSubscriptions, emptyList(), emptyList(),
    )
}
