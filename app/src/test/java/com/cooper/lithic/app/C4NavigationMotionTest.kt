package com.cooper.lithic.app

import com.cooper.lithic.feature.project.LeaveDecision
import com.cooper.lithic.feature.settings.RestoreUiPhase
import org.junit.Assert.*
import org.junit.Test

class C4NavigationMotionTest {
    @Test fun every_distinct_primary_pair_uses_an_instant_transition() {
        val primary = listOf(Home, Review, Settings)
        primary.forEach { from -> primary.filterNot { it == from }.forEach { to ->
            assertTrue("$from -> $to", c4UseInstantPrimaryTransition(from, to))
        } }
    }

    @Test fun entering_or_leaving_a_detail_does_not_inherit_tab_motion() {
        val detail = ProjectDetail("82000000-0000-0000-0000-000000000001", ProjectOrigin.HOME)
        listOf(Home, Review, Settings).forEach { primary ->
            assertFalse(c4UseInstantPrimaryTransition(primary, detail))
            assertFalse(c4UseInstantPrimaryTransition(detail, primary))
        }
        assertFalse(c4UseInstantPrimaryTransition(detail, CreateProject))
    }

    @Test fun missing_or_unrecognized_scene_metadata_never_matches_a_primary_route() {
        assertFalse(c4UseInstantPrimaryTransition(null, Review))
        assertFalse(c4UseInstantPrimaryTransition(Home, null))
        assertFalse(c4UseInstantPrimaryTransition("Home", "Review"))
        assertFalse(c4UseInstantPrimaryTransition(Home, Home))
    }

    @Test fun switching_tabs_does_not_change_canonical_stacks_or_leave_protection() {
        assertEquals(listOf(Home, Review), canonicalBackStack(Review))
        assertEquals(listOf(Home, Settings), canonicalBackStack(Settings))
        assertEquals(listOf(Home), canonicalBackStack(Home))
        assertEquals(LeaveDecision.CONFIRM_DISCARD,
            globalLeaveDecision(LeaveDecision.CONFIRM_DISCARD, LeaveDecision.ALLOW))
        assertEquals(LeaveDecision.BLOCK_PENDING,
            globalLeaveDecision(LeaveDecision.ALLOW, LeaveDecision.BLOCK_PENDING))
        assertFalse(navigationAvailable(RestoreUiPhase.RESTORING))
    }
}
