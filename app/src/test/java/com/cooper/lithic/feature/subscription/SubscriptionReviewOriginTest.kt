package com.cooper.lithic.feature.subscription

import org.junit.Assert.*
import org.junit.Test

class SubscriptionReviewOriginTest {
    @Test fun review_origin_can_open_existing_state_and_correction_forms() {
        assertTrue(isSubscriptionOriginSupported("REVIEW"))
    }
    @Test fun review_origin_survives_current_and_stopped_commits() {
        assertEquals("REVIEW", committedSubscriptionOrigin("REVIEW", false))
        assertEquals("REVIEW", committedSubscriptionOrigin("REVIEW", true))
    }
    @Test fun current_and_history_navigation_still_follow_the_real_state() {
        assertEquals("HISTORY", committedSubscriptionOrigin("CURRENT", true))
        assertEquals("CURRENT", committedSubscriptionOrigin("HISTORY", false))
    }
    @Test fun unknown_and_malformed_origins_are_rejected() {
        listOf("", "review", "UNKNOWN", "CURRENT ").forEach { assertFalse(isSubscriptionOriginSupported(it)) }
    }
}
