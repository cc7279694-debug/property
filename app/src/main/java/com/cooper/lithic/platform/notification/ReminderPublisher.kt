package com.cooper.lithic.platform.notification

import com.cooper.lithic.domain.reminder.RenewalReminderCandidate

/** Success means the platform call returned, never that a person received/read it. */
interface ReminderPublisher {
    fun canPublish(): Boolean
    fun publish(candidate: RenewalReminderCandidate): Boolean
}
