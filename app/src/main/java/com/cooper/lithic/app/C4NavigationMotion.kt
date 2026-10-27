package com.cooper.lithic.app

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith

/** Retained entry metadata; page motion no longer depends on the source or destination. */
internal const val C4_PRIMARY_DESTINATION = "com.cooper.lithic.primaryDestination"

/** The same presentation contract applies to forward, pop and predictive pop. */
internal fun c5PageTransition(): ContentTransform =
    EnterTransition.None togetherWith ExitTransition.None
