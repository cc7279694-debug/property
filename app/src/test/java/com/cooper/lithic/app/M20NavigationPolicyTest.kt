package com.cooper.lithic.app

import com.cooper.lithic.feature.project.LeaveDecision
import org.junit.Assert.assertEquals
import org.junit.Test

class M20NavigationPolicyTest {
    @Test fun notification_preferences_return_through_settings_without_new_bottom_tab() {
        assertEquals(listOf(Home, Settings, NotificationSettings), canonicalBackStack(NotificationSettings))
        assertEquals(canonicalBackStack(NotificationSettings), normalizeBackStack(canonicalBackStack(NotificationSettings)))
    }
    @Test fun notification_preferences_reject_wrong_parent() {
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, Review, NotificationSettings)))
    }
    @Test fun notification_entry_cannot_override_pending_or_unsaved_business_guards() {
        assertEquals(LeaveDecision.BLOCK_PENDING, globalLeaveDecision(LeaveDecision.ALLOW, LeaveDecision.BLOCK_PENDING))
        assertEquals(LeaveDecision.CONFIRM_DISCARD, globalLeaveDecision(LeaveDecision.CONFIRM_DISCARD, LeaveDecision.ALLOW))
    }
}
