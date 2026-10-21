package com.cooper.lithic.app

import com.cooper.lithic.feature.project.LeaveDecision
import com.cooper.lithic.feature.project.ProjectUiState
import com.cooper.lithic.feature.project.ProjectEditorUiState
import com.cooper.lithic.feature.project.ProjectEditorMode
import com.cooper.lithic.feature.project.MoneyEventEditorUiState
import com.cooper.lithic.feature.project.EndProjectUiState
import com.cooper.lithic.feature.project.MoneyCorrectionUiState
import com.cooper.lithic.feature.project.DeleteProjectUiState
import com.cooper.lithic.feature.project.SubmissionPhase
import com.cooper.lithic.core.model.MoneyDirection
import org.junit.Assert.*
import org.junit.Test

class M16NavigationPolicyTest {
    private val a = "00000000-0000-0000-0000-000000000001"
    private val b = "00000000-0000-0000-0000-000000000002"
    @Test fun comparison_keeps_detail_and_original_review_parent() {
        val route = ReplacementComparison(a, ProjectOrigin.REVIEW)
        assertEquals(listOf(Home, Review, LongTermReview, ProjectDetail(a, ProjectOrigin.REVIEW), route), canonicalBackStack(route))
    }
    @Test fun comparison_target_detail_and_editor_keep_the_comparison_return_path() {
        val target = ProjectDetail(b, ProjectOrigin.HISTORY, a)
        val stack = listOf(Home, Review, ProjectHistory, ProjectDetail(a, ProjectOrigin.HISTORY), ReplacementComparison(a, ProjectOrigin.HISTORY), target)
        assertEquals(stack, canonicalBackStack(target))
        assertEquals(stack + ProjectEdit(b, ProjectOrigin.HISTORY, a), canonicalBackStack(ProjectEdit(b, ProjectOrigin.HISTORY, a)))
        assertEquals(stack, normalizeBackStack(stack))
    }
    @Test fun invalid_or_self_comparison_identity_is_rejected() {
        assertEquals(listOf(Home), canonicalBackStack(ProjectDetail(b, comparisonId = "invalid")))
        assertEquals(listOf(Home), canonicalBackStack(ProjectDetail(a, comparisonId = a)))
        assertEquals(listOf(Home), canonicalBackStack(ReplacementComparison("invalid")))
    }
    @Test fun trade_in_is_a_project_editor_without_a_new_bottom_tab() {
        val route = TradeInProject(a, ProjectOrigin.REVIEW)
        assertEquals(listOf(Home, Review, LongTermReview, ProjectDetail(a, ProjectOrigin.REVIEW), route), canonicalBackStack(route))
        assertEquals(ProjectOrigin.REVIEW, projectOrigin(route))
    }
    @Test fun replacement_pending_participates_in_the_existing_global_guard() {
        assertEquals(LeaveDecision.BLOCK_PENDING, globalLeaveDecision(LeaveDecision.ALLOW, LeaveDecision.CONFIRM_DISCARD, LeaveDecision.BLOCK_PENDING))
        assertEquals(LeaveDecision.CONFIRM_DISCARD, globalLeaveDecision(LeaveDecision.ALLOW, LeaveDecision.CONFIRM_DISCARD))
    }
    @Test fun pending_editor_recovery_keeps_the_real_comparison_return_stack() {
        val origin = ProjectOrigin.HISTORY
        val event = "00000000-0000-0000-0000-000000000003"
        val pending = SubmissionPhase.UNCONFIRMED
        val cases = listOf(
            ProjectUiState(editor = ProjectEditorUiState(mode = ProjectEditorMode.EDIT, projectId = b, phase = pending)) to ProjectEdit(b, origin, a),
            ProjectUiState(moneyEditor = MoneyEventEditorUiState(projectId = b, direction = MoneyDirection.RECOVERY, phase = pending)) to ProjectMoneyEntry(b, MoneyDirection.RECOVERY, origin, a),
            ProjectUiState(endEditor = EndProjectUiState(projectId = b, correcting = false, phase = pending)) to ProjectEnd(b, origin, a),
            ProjectUiState(endEditor = EndProjectUiState(projectId = b, correcting = true, phase = pending)) to ProjectUpdateEnd(b, origin, a),
            ProjectUiState(moneyCorrection = MoneyCorrectionUiState(projectId = b, eventId = event, phase = pending)) to ProjectMoneyCorrection(b, event, origin, a),
            ProjectUiState(deleteConfirmation = DeleteProjectUiState(projectId = b, phase = pending)) to ProjectDelete(b, origin, a),
        )
        val comparisonPrefix = listOf(Home, Review, ProjectHistory, ProjectDetail(a, origin), ReplacementComparison(a, origin), ProjectDetail(b, origin, a))
        for ((state, currentPendingRoute) in cases) {
            val recovered = checkNotNull(projectRecoveryRoute(state, origin, a))
            assertEquals("Recovery must recognize the same pending editor without dropping its comparison parent", currentPendingRoute, recovered)
            assertEquals(comparisonPrefix + recovered, canonicalBackStack(recovered))
            assertEquals(comparisonPrefix + recovered, normalizeBackStack(comparisonPrefix + recovered))
        }
        assertEquals(CreateProject, projectRecoveryRoute(ProjectUiState(editor = ProjectEditorUiState(mode = ProjectEditorMode.CREATE, projectId = b, phase = pending)), origin, a))
    }
}
