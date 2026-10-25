package com.cooper.lithic.feature.subscription

import org.junit.Assert.*
import org.junit.Test

class P02SubscriptionBrowseOriginTest {
    @Test fun existing_analysis_routes_can_open_the_existing_forms() {
        listOf("TREND", "CATEGORY", "ANNUAL").forEach { assertTrue(it, isSubscriptionOriginSupported(it)) }
    }
    @Test fun analysis_return_context_survives_a_current_or_stopped_result() {
        listOf("REVIEW", "TREND", "CATEGORY", "ANNUAL").forEach { origin ->
            assertEquals(origin, committedSubscriptionOrigin(origin, false))
            assertEquals(origin, committedSubscriptionOrigin(origin, true))
        }
    }
    @Test fun unknown_or_malformed_context_never_opens_a_form() {
        listOf("SEARCH", "trend", "CATEGORY ", "", "UNKNOWN").forEach { assertFalse(isSubscriptionOriginSupported(it)) }
    }
}
