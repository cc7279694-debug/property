package com.cooper.lithic.app

import com.cooper.lithic.feature.project.LeaveDecision
import org.junit.Assert.assertEquals
import org.junit.Test

class M21NavigationPolicyTest {
    private val id = "ed09fbca-639b-4e8e-97ed-2dc39ed396a9"
    private val ref = MilestoneSources("50c79a10-bc1e-4a99-9d7a-314a5e757865", "2026-10-22", "a".repeat(64), 51)

    @Test fun bounded_summary_reference_has_its_own_temporary_source_route() {
        assertEquals(listOf(Home, ref), canonicalBackStack(ref))
        assertEquals(listOf(Home, ref), normalizeBackStack(listOf(Home, ref)))
        assertEquals(listOf(Home, InvalidMilestoneNotice), canonicalBackStack(InvalidMilestoneNotice))
    }

    @Test fun unknown_reference_identity_or_fields_cannot_be_restored_as_a_source_page() {
        listOf(ref.copy(generationId = "unknown"), ref.copy(publicationOn = "2026-02-30"),
            ref.copy(digest = "A".repeat(64)), ref.copy(count = 0), ref.copy(count = 1_000_001)).forEach {
            assertEquals(listOf(Home), canonicalBackStack(it))
        }
    }

    @Test fun exact_project_detail_and_editor_keep_a_return_path_to_this_summary() {
        val current = listOf(Home, ref)
        val detail = listOf(Home, ref, ProjectDetail(id))
        assertEquals(detail, milestoneAwareBackStack(current, canonicalBackStack(ProjectDetail(id))))
        val editor = detail + ProjectEdit(id)
        assertEquals(editor, milestoneAwareBackStack(detail, canonicalBackStack(ProjectEdit(id))))
        assertEquals(editor, normalizeBackStack(editor))
    }

    @Test fun exact_subscription_detail_and_editor_keep_the_typed_summary_parent() {
        val detail = listOf(Home, ref, SubscriptionDetail(id))
        assertEquals(detail, milestoneAwareBackStack(listOf(Home, ref), canonicalBackStack(SubscriptionDetail(id))))
        val renewal = detail + ConfirmSubscriptionRenewal(id)
        assertEquals(renewal, milestoneAwareBackStack(detail, canonicalBackStack(ConfirmSubscriptionRenewal(id))))
        assertEquals(renewal, normalizeBackStack(renewal))
    }

    @Test fun changing_bottom_destination_does_not_preserve_a_notice_or_add_a_tab() {
        assertEquals(listOf(Home, Settings), milestoneAwareBackStack(listOf(Home, ref, ProjectDetail(id)), canonicalBackStack(Settings)))
        assertEquals(listOf(Home, Review), milestoneAwareBackStack(listOf(Home, ref), canonicalBackStack(Review)))
    }

    @Test fun source_stack_rejects_wrong_parent_invalid_id_and_unrelated_nested_destinations() {
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, Review, ref)))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, ref, ProjectDetail("unknown"))))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, ref, Settings)))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, ref, SubscriptionDetail(id), ProjectEdit(id))))
    }

    @Test fun notice_click_keeps_existing_pending_and_dirty_priorities() {
        assertEquals(LeaveDecision.BLOCK_PENDING, globalLeaveDecision(LeaveDecision.CONFIRM_DISCARD, LeaveDecision.BLOCK_PENDING))
        assertEquals(LeaveDecision.CONFIRM_DISCARD, globalLeaveDecision(LeaveDecision.ALLOW, LeaveDecision.CONFIRM_DISCARD))
    }

    @Test fun restore_guard_also_applies_to_already_open_navigation_dialogs() {
        com.cooper.lithic.feature.settings.RestoreUiPhase.entries.forEach { phase ->
            assertEquals(phase != com.cooper.lithic.feature.settings.RestoreUiPhase.RESTORING,
                navigationAvailable(phase))
        }
    }
}
