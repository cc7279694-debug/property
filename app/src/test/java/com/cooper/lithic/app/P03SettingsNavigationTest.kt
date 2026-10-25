package com.cooper.lithic.app

import com.cooper.lithic.feature.project.LeaveDecision
import com.cooper.lithic.feature.settings.RestoreUiPhase
import org.junit.Assert.*
import org.junit.Test

class P03SettingsNavigationTest {
    @Test fun both_information_pages_return_to_settings() {
        listOf(CalculationExplanation, AboutLithic).forEach {
            assertEquals(listOf(Home, Settings, it), canonicalBackStack(it))
            assertEquals(canonicalBackStack(it), normalizeBackStack(canonicalBackStack(it)))
        }
    }
    @Test fun information_pages_do_not_accept_review_or_search_as_fake_parent() {
        listOf(CalculationExplanation, AboutLithic).forEach {
            assertEquals(listOf(Home), normalizeBackStack(listOf(Home, Review, it)))
            assertEquals(listOf(Home), normalizeBackStack(listOf(Home, NameSearch, it)))
        }
    }
    @Test fun settings_links_do_not_change_pending_or_unsaved_leave_decisions() {
        assertEquals(LeaveDecision.BLOCK_PENDING, globalLeaveDecision(LeaveDecision.ALLOW, LeaveDecision.BLOCK_PENDING))
        assertEquals(LeaveDecision.CONFIRM_DISCARD, globalLeaveDecision(LeaveDecision.CONFIRM_DISCARD, LeaveDecision.ALLOW))
    }
    @Test fun switching_still_blocks_navigation_from_information_links() {
        assertFalse(navigationAvailable(RestoreUiPhase.RESTORING))
        assertTrue(navigationAvailable(RestoreUiPhase.IDLE))
    }
}
