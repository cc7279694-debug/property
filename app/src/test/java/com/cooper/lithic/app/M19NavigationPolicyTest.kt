package com.cooper.lithic.app

import org.junit.Assert.assertEquals
import org.junit.Test

class M19NavigationPolicyTest {
    private val category = "30d2dd9b-77bd-4f93-bd2c-c3561fc43060"

    @Test fun annual_review_has_review_parent_without_new_bottom_tab() {
        assertEquals(listOf(Home, Review, AnnualReview), canonicalBackStack(AnnualReview))
    }
    @Test fun home_category_destination_retains_exact_identity_and_type() {
        val destination = CategoryAnalysis(category, subscription = true)
        assertEquals(listOf(Home, Review, destination), canonicalBackStack(destination))
        assertEquals(canonicalBackStack(destination), normalizeBackStack(canonicalBackStack(destination)))
    }
    @Test fun invalid_category_or_wrong_parent_is_rejected() {
        assertEquals(listOf(Home), canonicalBackStack(CategoryAnalysis("bad")))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, Settings, AnnualReview)))
    }

    @Test fun annual_details_and_editors_preserve_exact_record_and_return_origin() {
        val detail = ProjectDetail(category, ProjectOrigin.ANNUAL)
        val editor = ProjectEdit(category, ProjectOrigin.ANNUAL)
        assertEquals(listOf(Home, Review, AnnualReview, detail, editor), canonicalBackStack(editor))
        assertEquals(ProjectOrigin.ANNUAL, projectOrigin(editor))
        assertEquals(canonicalBackStack(editor), normalizeBackStack(canonicalBackStack(editor)))
        val subscription = SubscriptionDetail(category, SubscriptionOrigin.ANNUAL)
        assertEquals(listOf(Home, Review, AnnualReview, subscription), canonicalBackStack(subscription))
    }

    @Test fun category_subscription_renewal_keeps_guarded_detail_parent() {
        val detail = SubscriptionDetail(category, SubscriptionOrigin.CATEGORY)
        val renewal = ConfirmSubscriptionRenewal(category, SubscriptionOrigin.CATEGORY)
        assertEquals(listOf(Home, Review, CategoryAnalysis(), detail, renewal), canonicalBackStack(renewal))
        assertEquals(SubscriptionOrigin.CATEGORY, subscriptionOrigin(renewal))
        assertEquals(listOf(Home), canonicalBackStack(ProjectDetail("bad", ProjectOrigin.CATEGORY)))
    }
}
