package com.cooper.lithic.app

import com.cooper.lithic.core.model.MoneyDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class M15NavigationPolicyTest {
    private val id = "00000000-0000-4000-8000-000000000015"
    private val event = "00000000-0000-4000-8000-000000000016"
    @Test fun long_term_review_retains_review_tab_and_explicit_back_path() {
        assertEquals(listOf(Home, Review, LongTermReview), canonicalBackStack(LongTermReview))
    }
    @Test fun project_review_detail_and_edit_keep_exact_origin() {
        val origin = ProjectOrigin.valueOf("REVIEW")
        assertEquals(listOf(Home, Review, LongTermReview, ProjectDetail(id, origin)), canonicalBackStack(ProjectDetail(id, origin)))
        assertEquals(listOf(Home, Review, LongTermReview, ProjectDetail(id, origin), ProjectEdit(id, origin)), canonicalBackStack(ProjectEdit(id, origin)))
        assertEquals(origin, projectOrigin(ProjectMoneyEntry(id, MoneyDirection.OUTLAY, origin)))
    }
    @Test fun subscription_review_detail_and_renewal_keep_exact_origin() {
        val origin = SubscriptionOrigin.valueOf("REVIEW")
        assertEquals(listOf(Home, Review, LongTermReview, SubscriptionDetail(id, origin), ConfirmSubscriptionRenewal(id, origin)), canonicalBackStack(ConfirmSubscriptionRenewal(id, origin)))
        assertEquals(origin, subscriptionOrigin(CorrectSubscriptionRenewal(id, event, origin)))
    }
    @Test fun valid_review_stack_restores_and_unknown_route_identity_fails_closed() {
        val origin = ProjectOrigin.valueOf("REVIEW")
        val stack = listOf(Home, Review, LongTermReview, ProjectDetail(id, origin))
        assertEquals(stack, normalizeBackStack(stack))
        assertEquals(listOf(Home), canonicalBackStack(ProjectDetail("not-a-uuid", origin)))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, ProjectDetail(id, origin))))
    }
    @Test fun existing_history_parent_stays_unchanged() {
        assertEquals(listOf(Home, Review, ProjectHistory), canonicalBackStack(ProjectHistory))
        assertEquals(listOf(Home, Review, SubscriptionHistory), canonicalBackStack(SubscriptionHistory))
    }
}
