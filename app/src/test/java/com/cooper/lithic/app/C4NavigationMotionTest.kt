package com.cooper.lithic.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.cooper.lithic.feature.project.LeaveDecision
import com.cooper.lithic.feature.settings.RestoreUiPhase
import org.junit.Assert.*
import org.junit.Test

class C4NavigationMotionTest {
    @Test fun the_shared_page_transition_has_no_alpha_or_scale_animation() {
        val transition = c5PageTransition()
        assertEquals(EnterTransition.None, transition.targetContentEnter)
        assertEquals(ExitTransition.None, transition.initialContentExit)
    }

    @Test fun the_scene_wrapper_preserves_detail_and_primary_entry_identity_and_metadata() {
        val detail = ProjectDetail("82000000-0000-0000-0000-000000000001", ProjectOrigin.HOME)
        listOf(Home, Review, Settings, TotalCostTrend, detail, CreateProject).forEach { route ->
            val contentKey = "saved-content-$route"
            val metadata = mapOf("synthetic-source" to route)
            val original = NavEntry<NavKey>(route, contentKey, metadata) { }
            val wrapped = c5SceneEntryProvider { _: NavKey -> original }(route)
            assertEquals(contentKey, wrapped.contentKey)
            assertSame(metadata, wrapped.metadata)
        }
    }

    @Test fun default_content_key_and_empty_scene_metadata_survive_wrapping() {
        val original = NavEntry<NavKey>(TotalCostTrend) { }
        val wrapped = c5SceneEntryProvider { _: NavKey -> original }(TotalCostTrend)
        assertSame(original.contentKey, wrapped.contentKey)
        assertSame(original.metadata, wrapped.metadata)
        assertTrue(wrapped.metadata.isEmpty())
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
