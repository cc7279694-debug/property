package com.cooper.lithic.app

import com.cooper.lithic.feature.project.LeaveDecision
import org.junit.Assert.*
import org.junit.Test

class M17NavigationPolicyTest {
    private val a = "00000000-0000-0000-0000-000000000001"
    private val b = "00000000-0000-0000-0000-000000000002"
    @Test fun project_chart_preserves_current_history_review_and_comparison_return() {
        ProjectOrigin.entries.forEach { origin ->
            val detail = ProjectDetail(b, origin, a)
            val route = ProjectCostHistory(b, origin, a)
            val stack = canonicalBackStack(detail) + route
            assertEquals(stack, canonicalBackStack(route))
            assertEquals(stack, normalizeBackStack(stack))
            assertEquals(origin, projectOrigin(route))
            assertEquals(a, projectComparisonId(route))
            assertEquals(detail, stack.dropLast(1).last())
        }
    }
    @Test fun subscription_chart_keeps_exact_origin_detail_and_saved_stack() {
        SubscriptionOrigin.entries.forEach { origin ->
            val route = SubscriptionPriceHistory(a, origin)
            val stack = canonicalBackStack(SubscriptionDetail(a, origin)) + route
            assertEquals(stack, canonicalBackStack(route))
            assertEquals(stack, normalizeBackStack(stack))
            assertEquals(origin, subscriptionOrigin(route))
        }
    }
    @Test fun invalid_chart_identity_and_injected_stack_are_rejected() {
        assertEquals(listOf(Home), canonicalBackStack(ProjectCostHistory("bad")))
        assertEquals(listOf(Home), canonicalBackStack(ProjectCostHistory(a, comparisonId = a)))
        assertEquals(listOf(Home), canonicalBackStack(SubscriptionPriceHistory("bad")))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, ProjectCostHistory(a))))
    }
    @Test fun chart_request_obeys_existing_dirty_pending_and_restore_navigation_contract() {
        assertEquals(LeaveDecision.CONFIRM_DISCARD, globalLeaveDecision(LeaveDecision.ALLOW, LeaveDecision.CONFIRM_DISCARD))
        assertEquals(LeaveDecision.BLOCK_PENDING, globalLeaveDecision(LeaveDecision.CONFIRM_DISCARD, LeaveDecision.BLOCK_PENDING))
        // Chart destinations carry identities only, never a mutable form or a storage owner.
        assertEquals(ProjectCostHistory(a), canonicalBackStack(ProjectCostHistory(a)).last())
    }
}
