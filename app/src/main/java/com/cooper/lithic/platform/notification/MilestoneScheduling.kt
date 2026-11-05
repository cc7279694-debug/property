package com.cooper.lithic.platform.notification

import android.content.Context
import com.cooper.lithic.data.backup.ManagedGeneration
import java.time.Clock

/** V1 core does not schedule or cancel milestone work. */
@Suppress("UNUSED_PARAMETER")
object MilestoneScheduling {
    const val CANDIDATE_TAG = "lithic-milestone-candidate-v1"

    internal suspend fun reconcile(
        context: Context,
        generation: ManagedGeneration,
        isCurrent: () -> Boolean,
        clock: Clock = Clock.systemUTC(),
        publisher: MilestonePublisher? = null,
    ) = Unit
}
