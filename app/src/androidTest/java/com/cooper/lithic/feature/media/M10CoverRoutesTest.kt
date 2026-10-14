package com.cooper.lithic.feature.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.app.Home
import com.cooper.lithic.app.ProjectCover
import com.cooper.lithic.app.ProjectDetail
import com.cooper.lithic.app.ProjectHistory
import com.cooper.lithic.app.ProjectList
import com.cooper.lithic.app.ProjectOrigin
import com.cooper.lithic.app.Review
import com.cooper.lithic.app.SubscriptionCover
import com.cooper.lithic.app.SubscriptionDetail
import com.cooper.lithic.app.SubscriptionHistory
import com.cooper.lithic.app.SubscriptionList
import com.cooper.lithic.app.SubscriptionOrigin
import com.cooper.lithic.app.canonicalBackStack
import com.cooper.lithic.app.globalLeaveDecision
import com.cooper.lithic.app.normalizeBackStack
import com.cooper.lithic.feature.project.LeaveDecision
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Restored ancestry and the third writer's real leave contract. */
@RunWith(AndroidJUnit4::class)
class M10CoverRoutesTest {
    @Test fun project_cover_retains_current_and_history_detail_parent_chain() {
        val id = "10000000-0000-0000-0000-000000000911"
        val current = ProjectCover(id)
        val history = ProjectCover(id, ProjectOrigin.HISTORY)
        assertEquals(listOf(Home, ProjectList, ProjectDetail(id), current), canonicalBackStack(current))
        val expectedHistory = listOf(Home, Review, ProjectHistory, ProjectDetail(id, ProjectOrigin.HISTORY), history)
        assertEquals(expectedHistory, canonicalBackStack(history))
        assertEquals(expectedHistory, normalizeBackStack(expectedHistory))
        assertEquals(listOf(Home), canonicalBackStack(ProjectCover("../outside")))
    }

    @Test fun subscription_cover_retains_current_and_stopped_history_detail_parent_chain() {
        val id = "10000000-0000-0000-0000-000000000912"
        val current = SubscriptionCover(id)
        val history = SubscriptionCover(id, SubscriptionOrigin.HISTORY)
        assertEquals(listOf(Home, SubscriptionList, SubscriptionDetail(id), current), canonicalBackStack(current))
        val expectedHistory = listOf(Home, Review, SubscriptionHistory, SubscriptionDetail(id, SubscriptionOrigin.HISTORY), history)
        assertEquals(expectedHistory, canonicalBackStack(history))
        assertEquals(expectedHistory, normalizeBackStack(expectedHistory))
        assertEquals(listOf(Home), canonicalBackStack(SubscriptionCover("content://outside")))
    }

    @Test fun pending_cover_cannot_be_discarded_by_either_other_feature() {
        assertEquals(LeaveDecision.BLOCK_PENDING,
            globalLeaveDecision(LeaveDecision.ALLOW, LeaveDecision.ALLOW, LeaveDecision.BLOCK_PENDING))
        assertEquals(LeaveDecision.BLOCK_PENDING,
            globalLeaveDecision(LeaveDecision.CONFIRM_DISCARD, LeaveDecision.ALLOW, LeaveDecision.BLOCK_PENDING))
    }

    @Test fun prepared_cover_requires_explicit_discard_and_another_pending_write_still_blocks() {
        assertEquals(LeaveDecision.CONFIRM_DISCARD,
            globalLeaveDecision(LeaveDecision.ALLOW, LeaveDecision.ALLOW, LeaveDecision.CONFIRM_DISCARD))
        assertEquals(LeaveDecision.BLOCK_PENDING,
            globalLeaveDecision(LeaveDecision.BLOCK_PENDING, LeaveDecision.ALLOW, LeaveDecision.CONFIRM_DISCARD))
        assertEquals(LeaveDecision.ALLOW,
            globalLeaveDecision(LeaveDecision.ALLOW, LeaveDecision.ALLOW, LeaveDecision.ALLOW))
    }
}
