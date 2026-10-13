package com.cooper.lithic.feature.subscription

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.app.CancelSubscriptionStop
import com.cooper.lithic.app.ChangeSubscriptionState
import com.cooper.lithic.app.CorrectSubscriptionRenewal
import com.cooper.lithic.app.CorrectSubscriptionState
import com.cooper.lithic.app.DeleteSubscriptionRenewal
import com.cooper.lithic.app.Home
import com.cooper.lithic.app.Review
import com.cooper.lithic.app.SubscriptionAction
import com.cooper.lithic.app.SubscriptionDetail
import com.cooper.lithic.app.SubscriptionHistory
import com.cooper.lithic.app.SubscriptionList
import com.cooper.lithic.app.SubscriptionOrigin
import com.cooper.lithic.app.canonicalBackStack
import com.cooper.lithic.app.normalizeBackStack
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class M09SubscriptionRoutesTest {
    private val id = "68dcc0f5-c5cd-4d3d-8a8c-424947fc5c9e"
    private val factId = "1c1c0fe5-afdc-4059-94e4-5aed77b551c2"

    @Test fun stopped_history_and_detail_have_exact_review_parents() {
        assertEquals(listOf(Home, Review, SubscriptionHistory), canonicalBackStack(SubscriptionHistory))
        assertEquals(listOf(Home, Review, SubscriptionHistory, SubscriptionDetail(id, SubscriptionOrigin.HISTORY)),
            canonicalBackStack(SubscriptionDetail(id, SubscriptionOrigin.HISTORY)))
    }

    @Test fun history_reopen_has_five_route_limit_and_restores_exact_source() {
        val route = ChangeSubscriptionState(id, SubscriptionAction.REOPEN, SubscriptionOrigin.HISTORY)
        val expected = listOf(Home, Review, SubscriptionHistory, SubscriptionDetail(id, SubscriptionOrigin.HISTORY), route)
        assertEquals(expected, canonicalBackStack(route))
        assertEquals(expected, normalizeBackStack(expected))
    }

    @Test fun current_state_confirmation_keeps_current_detail_parent() {
        val route = ChangeSubscriptionState(id, SubscriptionAction.PAUSE)
        assertEquals(listOf(Home, SubscriptionList, SubscriptionDetail(id), route), canonicalBackStack(route))
    }

    @Test fun historical_payment_and_state_operations_preserve_target_fact_identity() {
        val parent = listOf(Home, Review, SubscriptionHistory, SubscriptionDetail(id, SubscriptionOrigin.HISTORY))
        listOf(CorrectSubscriptionRenewal(id, factId, SubscriptionOrigin.HISTORY),
            DeleteSubscriptionRenewal(id, factId, SubscriptionOrigin.HISTORY),
            CorrectSubscriptionState(id, factId, SubscriptionOrigin.HISTORY),
            CancelSubscriptionStop(id, factId, SubscriptionOrigin.HISTORY)).forEach { route ->
            assertEquals(parent + route, canonicalBackStack(route))
            assertEquals(parent + route, normalizeBackStack(parent + route))
        }
    }

    @Test fun corrupt_new_route_identity_or_wrong_parent_returns_home() {
        assertEquals(listOf(Home), canonicalBackStack(DeleteSubscriptionRenewal(id, "broken-fact-id")))
        assertEquals(listOf(Home), canonicalBackStack(ChangeSubscriptionState("broken-id", SubscriptionAction.STOP)))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, SubscriptionList, SubscriptionHistory)))
    }
}
