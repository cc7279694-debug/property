package com.cooper.lithic.feature.settings

import com.cooper.lithic.platform.notification.EnqueueStatus

/** A local save with an unknown outcome is distinct from a known saved preference. */
internal fun notificationScheduleMessage(status: EnqueueStatus, saveOutcomeUnknown: Boolean): String = when (status) {
    EnqueueStatus.PENDING -> "后台安排确认中，提醒是否送达尚未确定。"
    EnqueueStatus.ENQUEUED -> "后台安排已入队，不代表提醒已送达。"
    EnqueueStatus.FAILED -> "后台安排未能入队；已保存的提醒偏好不变。"
    EnqueueStatus.UNKNOWN -> if (saveOutcomeUnknown) "提醒偏好保存及后台安排结果暂时无法确认。"
        else "后台安排结果暂时无法确认；已保存的提醒偏好不变。"
    EnqueueStatus.CANCELLED -> "本地确认已取消，后台安排的实际状态仍未知。"
}
