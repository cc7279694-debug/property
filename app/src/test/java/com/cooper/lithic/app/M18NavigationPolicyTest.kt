package com.cooper.lithic.app

import org.junit.Assert.assertEquals
import org.junit.Test

class M18NavigationPolicyTest {
    private val id = "66d1d59a-8508-4712-b48e-7fe33d57038c"
    @Test fun total_trend_has_review_parent_without_new_bottom_tab() {
        assertEquals(listOf(Home, Review, TotalCostTrend), canonicalBackStack(TotalCostTrend))
    }

    @Test fun saved_total_trend_stack_keeps_read_only_return_path() {
        val stack = listOf(Home, Review, TotalCostTrend)
        assertEquals(stack, normalizeBackStack(stack))
    }

    @Test fun malformed_total_trend_stack_returns_home() {
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, Settings, TotalCostTrend)))
    }

    @Test fun event_project_detail_and_editor_keep_trend_origin_and_guard_stack() {
        val detail = ProjectDetail(id, ProjectOrigin.TREND)
        val edit = ProjectEdit(id, ProjectOrigin.TREND)
        assertEquals(listOf(Home, Review, TotalCostTrend, detail), canonicalBackStack(detail))
        assertEquals(listOf(Home, Review, TotalCostTrend, detail, edit), canonicalBackStack(edit))
        assertEquals(ProjectOrigin.TREND, projectOrigin(edit))
        assertEquals(canonicalBackStack(edit), normalizeBackStack(canonicalBackStack(edit)))
    }

    @Test fun event_subscription_detail_and_renewal_keep_trend_return_path() {
        val detail = SubscriptionDetail(id, SubscriptionOrigin.TREND)
        val renewal = ConfirmSubscriptionRenewal(id, SubscriptionOrigin.TREND)
        assertEquals(listOf(Home, Review, TotalCostTrend, detail), canonicalBackStack(detail))
        assertEquals(listOf(Home, Review, TotalCostTrend, detail, renewal), canonicalBackStack(renewal))
        assertEquals(SubscriptionOrigin.TREND, subscriptionOrigin(renewal))
    }

    @Test fun invalid_event_identity_cannot_create_detail_route() {
        assertEquals(listOf(Home), canonicalBackStack(ProjectDetail("bad", ProjectOrigin.TREND)))
        assertEquals(listOf(Home), canonicalBackStack(SubscriptionDetail("bad", SubscriptionOrigin.TREND)))
    }
}
