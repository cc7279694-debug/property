package com.cooper.lithic.app

import com.cooper.lithic.feature.project.LeaveDecision
import org.junit.Assert.assertEquals
import org.junit.Test

class M14NavigationPolicyTest {
    @Test fun search_is_a_secondary_route_without_an_extra_tab() {
        assertEquals(listOf(Home, NameSearch), canonicalBackStack(NameSearch))
        assertEquals(listOf(Home, NameSearch), normalizeBackStack(listOf(Home, NameSearch)))
    }

    @Test fun category_management_belongs_to_settings() {
        assertEquals(listOf(Home, Settings, CategoryManagement), canonicalBackStack(CategoryManagement))
        assertEquals(listOf(Home, Settings, CategoryManagement), normalizeBackStack(listOf(Home, Settings, CategoryManagement)))
    }

    @Test fun existing_write_guards_keep_pending_above_discard() {
        assertEquals(LeaveDecision.BLOCK_PENDING, globalLeaveDecision(LeaveDecision.CONFIRM_DISCARD, LeaveDecision.BLOCK_PENDING))
        assertEquals(LeaveDecision.CONFIRM_DISCARD, globalLeaveDecision(LeaveDecision.ALLOW, LeaveDecision.CONFIRM_DISCARD))
    }

    @Test fun incomplete_or_unexpected_stacks_are_rejected() {
        assertEquals(listOf(Home), normalizeBackStack(listOf(NameSearch)))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, CategoryManagement)))
    }

    @Test fun category_pending_write_blocks_both_existing_editors() {
        assertEquals(LeaveDecision.BLOCK_PENDING, globalLeaveDecision(LeaveDecision.ALLOW,
            LeaveDecision.CONFIRM_DISCARD, LeaveDecision.BLOCK_PENDING))
        assertEquals(LeaveDecision.CONFIRM_DISCARD, globalLeaveDecision(LeaveDecision.ALLOW,
            LeaveDecision.ALLOW, LeaveDecision.CONFIRM_DISCARD))
    }
}
