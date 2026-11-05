package com.cooper.lithic.platform.notification

import android.content.Context
import com.cooper.lithic.data.backup.ManagedGeneration
import java.time.Clock

/** V1 core keeps old call signatures but never changes the WorkManager task graph. */
@Suppress("UNUSED_PARAMETER")
object ReminderScheduling {
    const val CANDIDATE_TAG = "lithic-renewal-candidate-v1"

    fun start(context: Context) = Unit
    fun request(context: Context) = Unit
    internal suspend fun requestMilestones(context: Context) = Unit
    internal suspend fun reconcile(
        context: Context,
        generation: ManagedGeneration,
        isCurrent: () -> Boolean,
        clock: Clock = Clock.systemUTC(),
    ) = Unit
}
