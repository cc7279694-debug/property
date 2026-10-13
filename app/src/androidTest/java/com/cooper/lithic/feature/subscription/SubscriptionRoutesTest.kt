package com.cooper.lithic.feature.subscription

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.app.ConfirmSubscriptionRenewal
import com.cooper.lithic.app.CreateSubscription
import com.cooper.lithic.app.Home
import com.cooper.lithic.app.ProjectDetail
import com.cooper.lithic.app.ProjectEdit
import com.cooper.lithic.app.ProjectHistory
import com.cooper.lithic.app.ProjectList
import com.cooper.lithic.app.ProjectOrigin
import com.cooper.lithic.app.Review
import com.cooper.lithic.app.SubscriptionDetail
import com.cooper.lithic.app.SubscriptionList
import com.cooper.lithic.app.canonicalBackStack
import com.cooper.lithic.app.globalLeaveDecision
import com.cooper.lithic.app.normalizeBackStack
import com.cooper.lithic.feature.project.LeaveDecision
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubscriptionRoutesTest {
    private val id = "1ac0c4d6-dacd-4c95-894f-4f0c6dba5200"

    @Test fun subscription_routes_have_exact_bounded_parents() {
        assertEquals(listOf(Home, SubscriptionList), canonicalBackStack(SubscriptionList))
        assertEquals(listOf(Home, SubscriptionList, SubscriptionDetail(id)), canonicalBackStack(SubscriptionDetail(id)))
        assertEquals(listOf(Home, SubscriptionList, SubscriptionDetail(id), ConfirmSubscriptionRenewal(id)),
            canonicalBackStack(ConfirmSubscriptionRenewal(id)))
    }

    @Test fun restored_create_preserves_its_actual_home_or_list_parent() {
        assertEquals(listOf(Home, CreateSubscription), canonicalBackStack(CreateSubscription))
        assertEquals(listOf(Home, CreateSubscription), normalizeBackStack(listOf(Home, CreateSubscription)))
        assertEquals(listOf(Home, SubscriptionList, CreateSubscription), normalizeBackStack(listOf(Home, SubscriptionList, CreateSubscription)))
    }

    @Test fun pending_submission_from_either_feature_blocks_global_departure() {
        assertEquals(LeaveDecision.BLOCK_PENDING, globalLeaveDecision(LeaveDecision.ALLOW, LeaveDecision.BLOCK_PENDING))
        assertEquals(LeaveDecision.BLOCK_PENDING, globalLeaveDecision(LeaveDecision.BLOCK_PENDING, LeaveDecision.ALLOW))
        assertEquals(LeaveDecision.BLOCK_PENDING, globalLeaveDecision(LeaveDecision.CONFIRM_DISCARD, LeaveDecision.BLOCK_PENDING))
        assertEquals(LeaveDecision.BLOCK_PENDING, globalLeaveDecision(LeaveDecision.BLOCK_PENDING, LeaveDecision.CONFIRM_DISCARD))
    }

    @Test fun dirty_form_from_either_feature_requires_one_global_discard_prompt() {
        assertEquals(LeaveDecision.CONFIRM_DISCARD, globalLeaveDecision(LeaveDecision.ALLOW, LeaveDecision.CONFIRM_DISCARD))
        assertEquals(LeaveDecision.CONFIRM_DISCARD, globalLeaveDecision(LeaveDecision.CONFIRM_DISCARD, LeaveDecision.ALLOW))
        assertEquals(LeaveDecision.ALLOW, globalLeaveDecision(LeaveDecision.ALLOW, LeaveDecision.ALLOW))
    }

    @Test fun invalid_subscription_identity_and_illegal_restored_chain_return_home() {
        assertEquals(listOf(Home), canonicalBackStack(SubscriptionDetail("not-a-uuid")))
        assertEquals(listOf(Home), canonicalBackStack(ConfirmSubscriptionRenewal("not-a-uuid")))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, SubscriptionList, SubscriptionList)))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, Review, SubscriptionDetail(id))))
    }

    @Test fun frozen_project_current_and_history_parent_chains_remain_unchanged() {
        assertEquals(listOf(Home, ProjectList, ProjectDetail(id), ProjectEdit(id)), canonicalBackStack(ProjectEdit(id)))
        val history = listOf(Home, Review, ProjectHistory, ProjectDetail(id, ProjectOrigin.HISTORY), ProjectEdit(id, ProjectOrigin.HISTORY))
        assertEquals(history, canonicalBackStack(ProjectEdit(id, ProjectOrigin.HISTORY)))
        assertEquals(history, normalizeBackStack(history))
    }
}
