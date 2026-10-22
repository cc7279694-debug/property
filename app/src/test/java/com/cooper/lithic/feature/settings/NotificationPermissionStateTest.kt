package com.cooper.lithic.feature.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionStateTest {
    @Test fun api26_to32_never_requires_a_nonexistent_runtime_grant() {
        assertTrue(NotificationPermissionState(false, false, true).canPublish)
        assertFalse(NotificationPermissionState(false, false, false).canPublish)
    }

    @Test fun api33_runtime_app_and_channel_permissions_are_separate_requirements() {
        assertFalse(NotificationPermissionState(true, false, true).canPublish)
        assertFalse(NotificationPermissionState(true, true, false).canPublish)
        assertFalse(NotificationPermissionState(true, true, true, false).canPublish)
        assertTrue(NotificationPermissionState(true, true, true, true).canPublish)
    }
}
