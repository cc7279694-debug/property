package com.cooper.lithic.feature.settings

import com.cooper.lithic.data.backup.BackupExportResult
import com.cooper.lithic.data.backup.BackupFailureReason
import com.cooper.lithic.data.backup.BackupTarget
import com.cooper.lithic.data.backup.RestorePreview
import com.cooper.lithic.data.backup.RestoreResult
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercises the actual marker predicates used by both retained ViewModels. */
class BackupRecreationWarningTest {
    @Test fun unknown_restore_result_remains_rebuildable_after_its_operation_returns_to_idle() = runBlocking {
        val controller = restoreController { RestoreResult.Failed("受控结果未知。", currentPreserved = false) }
        controller.beginSelection()
        controller.selected(::input)
        controller.confirmOverwrite()

        assertEquals(RestoreUiPhase.IDLE, controller.state.value.phase)
        assertEquals(RestoreFeedbackKind.OUTCOME_UNKNOWN, controller.state.value.feedback)
        assertTrue(controller.state.value.requiresRecreationWarning())
        val recreated = recreatedRestore(controller.state.value.requiresRecreationWarning())
        assertEquals(RestoreFeedbackKind.OUTCOME_UNKNOWN, recreated.state.value.feedback)
        assertTrue(recreated.state.value.message.orEmpty().contains("请勿清除应用数据"))
    }

    @Test fun confirmation_exception_keeps_a_recreation_warning_without_exposing_the_failure() = runBlocking {
        val controller = restoreController { throw IOException("private-path") }
        controller.beginSelection()
        controller.selected(::input)
        controller.confirmOverwrite()

        assertTrue(controller.state.value.requiresRecreationWarning())
        assertFalse(controller.state.value.message.orEmpty().contains("private-path"))
    }

    @Test fun cancelled_confirmation_keeps_its_unknown_result_rebuildable() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val controller = restoreController {
            entered.complete(Unit)
            CompletableDeferred<Unit>().await()
            RestoreResult.Success
        }
        controller.beginSelection()
        controller.selected(::input)
        val confirming = launch { controller.confirmOverwrite() }
        entered.await()
        confirming.cancelAndJoin()

        assertEquals(RestoreFeedbackKind.OUTCOME_UNKNOWN, controller.state.value.feedback)
        assertTrue(controller.state.value.requiresRecreationWarning())
    }

    @Test fun late_restore_picker_callbacks_do_not_clear_a_recreated_unknown_result() = runBlocking {
        val controller = recreatedRestore(interrupted = true)
        val before = controller.state.value
        controller.selected { error("A late result must not open a backup") }
        controller.selectionCancelled()
        controller.selectionLaunchFailed()
        controller.confirmOverwrite()
        controller.discardPreview()

        assertEquals(before, controller.state.value)
        assertTrue(controller.state.value.requiresRecreationWarning())
        assertEquals(RestoreFeedbackKind.OUTCOME_UNKNOWN,
            recreatedRestore(controller.state.value.requiresRecreationWarning()).state.value.feedback)
    }

    @Test fun late_export_picker_callbacks_do_not_clear_unknown_or_delete_an_unproven_target() = runBlocking {
        val target = Target()
        val controller = BackupExportController(interruptedOnRecreation = true) { error("No automatic export") }
        val before = controller.state.value
        controller.selected(target)
        controller.selectionCancelled()
        controller.selectionLaunchFailed(possibleResidual = true)

        assertEquals(before, controller.state.value)
        assertEquals(0, target.deletes)
        assertTrue(controller.state.value.requiresRecreationWarning())
        val recreated = BackupExportController(controller.state.value.requiresRecreationWarning()) { error("No automatic export") }
        assertEquals(BackupExportFeedback.INTERRUPTED_UNKNOWN, recreated.state.value.feedback)
        assertTrue(recreated.state.value.partialTargetRemaining)
    }

    @Test fun every_busy_phase_keeps_the_existing_recreation_marker() {
        RestoreUiPhase.entries.filter { it != RestoreUiPhase.IDLE }.forEach {
            assertTrue(it.name, BackupRestoreUiState(phase = it).requiresRecreationWarning())
        }
        BackupUiPhase.entries.filter { it != BackupUiPhase.IDLE }.forEach {
            assertTrue(it.name, BackupExportUiState(phase = it).requiresRecreationWarning())
        }
    }

    @Test fun known_restore_outcomes_do_not_turn_into_unknown_on_recreation() = runBlocking {
        for (result in listOf(RestoreResult.Success, RestoreResult.Failed("原集未替换。", currentPreserved = true))) {
            val controller = restoreController { result }
            controller.beginSelection()
            controller.selected(::input)
            controller.confirmOverwrite()
            assertFalse(controller.state.value.requiresRecreationWarning())
            assertEquals(RestoreFeedbackKind.NONE,
                recreatedRestore(controller.state.value.requiresRecreationWarning()).state.value.feedback)
        }
        val controller = restoreController { error("No confirmation") }
        controller.beginSelection()
        controller.selectionCancelled()
        assertFalse(controller.state.value.requiresRecreationWarning())
        controller.beginSelection()
        controller.selected(::input)
        controller.discardPreview()
        assertFalse(controller.state.value.requiresRecreationWarning())
    }

    @Test fun known_export_results_clear_only_the_active_marker_and_keep_their_feedback() = runBlocking {
        for (result in listOf(BackupExportResult.Success(12, "a".repeat(64)),
            BackupExportResult.Cancelled(partialTargetRemaining = true),
            BackupExportResult.Failed(BackupFailureReason.IO, partialTargetRemaining = true))) {
            val controller = BackupExportController { result }
            controller.beginSelection()
            controller.selected(Target())
            assertFalse(controller.state.value.requiresRecreationWarning())
            assertTrue(controller.state.value.message.orEmpty().isNotBlank())
            val recreated = BackupExportController(controller.state.value.requiresRecreationWarning()) { error("No automatic export") }
            assertEquals(BackupExportFeedback.NOT_STARTED, recreated.state.value.feedback)
        }
    }

    @Test fun explicit_new_restore_selection_and_cancellation_keep_the_existing_retry_contract() {
        val controller = recreatedRestore(interrupted = true)
        assertTrue(controller.beginSelection())
        assertEquals(RestoreUiPhase.CHOOSING, controller.state.value.phase)
        assertTrue(controller.state.value.requiresRecreationWarning())
        controller.selectionCancelled()
        assertEquals(RestoreFeedbackKind.SELECTION_CANCELLED, controller.state.value.feedback)
        assertFalse(controller.state.value.requiresRecreationWarning())
    }

    @Test fun explicit_new_export_selection_and_cancellation_keep_the_existing_retry_contract() {
        val controller = BackupExportController(interruptedOnRecreation = true) { error("No selection") }
        assertTrue(controller.beginSelection())
        assertEquals(BackupExportFeedback.CHOOSING, controller.state.value.feedback)
        assertTrue(controller.state.value.requiresRecreationWarning())
        controller.selectionCancelled()
        assertEquals(BackupExportFeedback.CANCELLED, controller.state.value.feedback)
        assertFalse(controller.state.value.requiresRecreationWarning())
    }

    private fun restoreController(confirm: suspend (String) -> RestoreResult) = BackupRestoreController(
        prepare = { preview() }, confirm = confirm, discard = {})

    private fun recreatedRestore(interrupted: Boolean) = BackupRestoreController(interruptedOnRecreation = interrupted,
        prepare = { error("A recreated operation must not prepare automatically") },
        confirm = { error("A recreated operation must not confirm automatically") },
        discard = { error("A recreated operation must not discard an unproven token") })

    private fun input() = ByteArrayInputStream(byteArrayOf(1))
    private fun preview() = RestorePreview("recreation-test-token", mapOf("long_term_projects" to 1L),
        mapOf("long_term_projects" to 0L), "2026-10-20T00:00:00Z", "Asia/Shanghai")

    private class Target : BackupTarget {
        var deletes = 0
        override fun openWrite() = ByteArrayOutputStream()
        override fun openRead() = input()
        override fun deletePartial(): Boolean { deletes += 1; return true }

        private fun input() = ByteArrayInputStream(byteArrayOf())
    }
}
