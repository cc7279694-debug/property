package com.cooper.lithic.app

import com.cooper.lithic.feature.project.LeaveDecision

/** One precedence rule for every editor; an uncertain write can never be discarded. */
internal fun globalLeaveDecision(vararg decisions: LeaveDecision): LeaveDecision =
    when {
        LeaveDecision.BLOCK_PENDING in decisions -> LeaveDecision.BLOCK_PENDING
        LeaveDecision.CONFIRM_DISCARD in decisions -> LeaveDecision.CONFIRM_DISCARD
        else -> LeaveDecision.ALLOW
    }
