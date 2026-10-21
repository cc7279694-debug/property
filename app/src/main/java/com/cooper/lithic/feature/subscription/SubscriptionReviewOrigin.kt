package com.cooper.lithic.feature.subscription

/** UI return context only; never changes persisted subscription state. */
internal fun isSubscriptionOriginSupported(origin: String): Boolean = origin in setOf("CURRENT", "HISTORY", "REVIEW")
internal fun committedSubscriptionOrigin(origin: String, stopped: Boolean): String =
    if (origin == "REVIEW") "REVIEW" else if (stopped) "HISTORY" else "CURRENT"
