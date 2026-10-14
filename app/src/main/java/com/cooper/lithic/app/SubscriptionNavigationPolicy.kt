package com.cooper.lithic.app

import com.cooper.lithic.feature.project.LeaveDecision

/** One precedence rule for both features; an uncertain payment can never be discarded. */
internal fun globalLeaveDecision(projectDecision: LeaveDecision, subscriptionDecision: LeaveDecision,
    coverDecision: LeaveDecision = LeaveDecision.ALLOW): LeaveDecision =
    when {
        projectDecision == LeaveDecision.BLOCK_PENDING || subscriptionDecision == LeaveDecision.BLOCK_PENDING || coverDecision == LeaveDecision.BLOCK_PENDING -> LeaveDecision.BLOCK_PENDING
        projectDecision == LeaveDecision.CONFIRM_DISCARD || subscriptionDecision == LeaveDecision.CONFIRM_DISCARD || coverDecision == LeaveDecision.CONFIRM_DISCARD -> LeaveDecision.CONFIRM_DISCARD
        else -> LeaveDecision.ALLOW
    }
