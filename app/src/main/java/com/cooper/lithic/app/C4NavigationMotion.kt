package com.cooper.lithic.app

/** Presentation metadata only: no route, saved-state identity, or leave decision is changed. */
internal const val C4_PRIMARY_DESTINATION = "com.cooper.lithic.primaryDestination"

internal fun c4UseInstantPrimaryTransition(from: Any?, to: Any?): Boolean =
    from != to && from in listOf(Home, Review, Settings) && to in listOf(Home, Review, Settings)
