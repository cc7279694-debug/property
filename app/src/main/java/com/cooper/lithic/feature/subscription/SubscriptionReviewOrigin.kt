package com.cooper.lithic.feature.subscription

/** UI return context only; never changes persisted subscription state. */
internal fun isSubscriptionOriginSupported(origin: String): Boolean = origin in setOf("CURRENT", "HISTORY", "REVIEW", "TREND", "CATEGORY", "ANNUAL", "HOME", "REVIEW_HOME")
internal fun committedSubscriptionOrigin(origin: String, stopped: Boolean): String =
    if (origin in setOf("REVIEW", "TREND", "CATEGORY", "ANNUAL", "HOME", "REVIEW_HOME")) origin else if (stopped) "HISTORY" else "CURRENT"
