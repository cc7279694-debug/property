package com.cooper.lithic.feature.settings

import org.junit.Assert.assertTrue
import org.junit.Test

class CoreReminderCopyTest {
    @Test fun safety_information_clearly_states_automatic_reminders_are_not_currently_delivered() {
        val copy = productSafetyExplanation.flatMap { it.paragraphs }.joinToString()
        assertTrue(copy.contains("自动续费与里程碑提醒将在后续版本提供"))
        assertTrue(copy.contains("当前不会发送通知"))
        assertTrue(copy.contains("不会自动取消服务商扣费"))
    }

    @Test fun deferred_reminder_information_identifies_v1_1() {
        val copy = productSafetyExplanation.flatMap { it.paragraphs }.joinToString()
        assertTrue(copy.contains("V1.1"))
    }
}
