package com.cooper.lithic.platform.notification

import com.cooper.lithic.data.repository.MilestoneSummary

interface MilestonePublisher {
    fun canPublish(): Boolean
    fun publish(summary: MilestoneSummary): Boolean
}
