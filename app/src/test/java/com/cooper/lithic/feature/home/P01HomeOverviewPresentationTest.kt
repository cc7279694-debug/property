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

    @Test fun no_snapshot_without_explicit_refresh_failure_is_waiting() {
        assertEquals(HomeOverviewReadStatus.WAITING, homeOverviewReadStatus(null, true, null))
        assertEquals(HomeOverviewReadStatus.WAITING, homeOverviewReadStatus(null, true, recoveryWarning))
    }

    @Test fun explicit_refresh_failure_without_snapshot_is_the_controlled_h_case() {
        assertEquals(HomeOverviewReadStatus.FAILED,
            homeOverviewReadStatus(null, true, "本地记录暂时无法刷新。"))
    }

    @Test fun a_successful_snapshot_with_only_a_recovery_warning_is_not_stale() {
        val overview = result(total = "6.48", projects = 8, subscriptions = 5, activeProjects = 2)
        assertEquals(HomeOverviewReadStatus.CURRENT, homeOverviewReadStatus(overview, false, null))
        assertEquals(HomeOverviewReadStatus.UNCONFIRMED, homeOverviewReadStatus(overview, true, null))
        assertEquals(HomeOverviewReadStatus.UNCONFIRMED, homeOverviewReadStatus(overview, true, recoveryWarning))
    }

    @Test fun a_real_refresh_failure_retains_the_supplied_snapshot_with_or_without_a_recovery_warning() {
        val overview = result(total = "-0.005", projects = 8, subscriptions = 5, activeProjects = 2)
        for (message in listOf(refreshError, "$recoveryWarning $refreshError")) {
            assertEquals(HomeOverviewReadStatus.RETAINED, homeOverviewReadStatus(overview, true, message))
        }
        assertEquals(BigDecimal("-0.005"), overview.totalRate)
        assertEquals(LocalDate.of(2026, 10, 25), overview.asOf)
        assertEquals(8L, overview.projectCount)
        assertEquals(5L, overview.subscriptionCount)
    }

    @Test fun empty_snapshot_requires_confirmation_even_when_real_counts_are_zero() {
        val empty = result()
        assertEquals(HomeOverviewReadStatus.CURRENT, homeOverviewReadStatus(empty, false, null))
        assertEquals(HomeOverviewReadStatus.UNCONFIRMED, homeOverviewReadStatus(empty, true, null))
        assertEquals(HomeOverviewReadStatus.RETAINED, homeOverviewReadStatus(empty, true, refreshError))
        assertEquals(true, c2HomeIsEmpty(empty, false))
        assertEquals(false, c2HomeIsEmpty(empty, true))
    }

    private val refreshError = "本地记录暂时无法刷新，当前显示上次读取结果。"
    private val recoveryWarning = "当前数据已打开；未能安全清理的旧资源已保留。"

    private fun result(total: String = "0", projects: Long = 0, subscriptions: Long = 0,
        activeProjects: Long = 0, activeSubscriptions: Long = 0) = OverviewResult(
        LocalDate.of(2026, 10, 25), BigDecimal(total), BigDecimal.ZERO, BigDecimal(total),
        projects, subscriptions, activeProjects, activeSubscriptions, emptyList(), emptyList(),
    )
}
