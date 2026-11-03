package com.cooper.lithic.feature.settings

import com.cooper.lithic.platform.notification.EnqueueStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NotificationSchedulingMessageTest {
    @Test fun unknown_save_never_claims_preferences_were_saved() {
        val text = notificationScheduleMessage(EnqueueStatus.UNKNOWN, saveOutcomeUnknown = true)
        assertEquals("提醒偏好保存及后台安排结果暂时无法确认。", text)
        assertFalse(text.contains("已保存"))
    }

    @Test fun known_preferences_with_unknown_enqueue_retain_original_explanation() {
        assertEquals("后台安排结果暂时无法确认；已保存的提醒偏好不变。",
            notificationScheduleMessage(EnqueueStatus.UNKNOWN, saveOutcomeUnknown = false))
    }
}
