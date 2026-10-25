package com.cooper.lithic.feature.settings

import com.cooper.lithic.core.design.UiMessageTone
import com.cooper.lithic.data.backup.BackupExportResult
import com.cooper.lithic.data.backup.BackupFailureReason
import com.cooper.lithic.data.backup.BackupTarget
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class P03BackupExportFeedbackTest {
    @Test
    fun recreated_export_explicitly_keeps_previous_result_unknown() {
        val controller = BackupExportController(interruptedOnRecreation = true) { error("No automatic export") }
        assertTrue(controller.state.value.message!!.contains("中断"))
        assertTrue(controller.state.value.message!!.contains("无法确认"))
        assertEquals(null, controller.state.value.exportedBytes)
        assertEquals(BackupExportFeedback.INTERRUPTED_UNKNOWN, controller.state.value.feedback)
        assertTrue(controller.state.value.partialTargetRemaining)
        assertEquals(UiMessageTone.WARNING, controller.state.value.messageTone)
    }

    @Test
    fun late_recreated_picker_result_never_turns_unknown_into_failure_or_success() = runBlocking {
        var calls = 0
        val target = Target()
        val controller = BackupExportController(interruptedOnRecreation = true) {
            calls++
            BackupExportResult.Success(20, "a".repeat(64))
        }
        controller.selected(target)
        assertEquals(0, calls)
        assertEquals(0, target.deletes)
        assertTrue(controller.state.value.message!!.contains("无法确认"))
        assertTrue(controller.state.value.message!!.contains("请勿将其用于恢复"))
        assertEquals(BackupExportFeedback.INTERRUPTED_UNKNOWN, controller.state.value.feedback)
    }

    @Test
    fun idle_phase_message_and_bytes_cannot_manufacture_verified_success() {
        val state = BackupExportUiState(message = "备份成功", exportedBytes = 100)
        assertEquals(BackupUiPhase.IDLE, state.phase)
        assertEquals(BackupExportFeedback.NOT_STARTED, state.feedback)
    }

    @Test
    fun choosing_and_running_are_explicit_and_do_not_inherit_previous_outcome() = runBlocking {
        val finish = CompletableDeferred<Unit>()
        val controller = BackupExportController {
            finish.await()
            BackupExportResult.Success(10, "b".repeat(64))
        }
        assertEquals(BackupExportFeedback.NOT_STARTED, controller.state.value.feedback)
        assertTrue(controller.beginSelection())
        assertEquals(BackupExportFeedback.CHOOSING, controller.state.value.feedback)
        assertFalse(controller.beginSelection())
        val running = async(start = CoroutineStart.UNDISPATCHED) { controller.selected(Target()) }
        assertEquals(BackupExportFeedback.EXPORTING, controller.state.value.feedback)
        assertEquals(null, controller.state.value.exportedBytes)
        assertFalse(controller.beginSelection())
        finish.complete(Unit)
        running.await()
        assertEquals(BackupExportFeedback.VERIFIED_SUCCESS, controller.state.value.feedback)
    }

    @Test
    fun picker_cancellation_is_information_without_export_or_success() {
        var calls = 0
        val controller = BackupExportController { calls++; error("No selection") }
        controller.beginSelection()
        controller.selectionCancelled()
        assertEquals(0, calls)
        assertEquals(BackupExportFeedback.CANCELLED, controller.state.value.feedback)
        assertEquals(UiMessageTone.INFORMATION, controller.state.value.messageTone)
        assertFalse(controller.state.value.partialTargetRemaining)
        assertEquals(null, controller.state.value.exportedBytes)
    }

    @Test
    fun picker_failure_preserves_its_possible_target_residue_and_error_severity() {
        val controller = BackupExportController { error("No verified target") }
        controller.beginSelection()
        controller.selectionLaunchFailed(possibleResidual = true)
        assertEquals(BackupExportFeedback.PICKER_FAILED, controller.state.value.feedback)
        assertEquals(UiMessageTone.ERROR, controller.state.value.messageTone)
        assertTrue(controller.state.value.partialTargetRemaining)
        assertEquals(null, controller.state.value.failureReason)
        assertTrue(controller.state.value.message!!.contains("请勿将其用于恢复"))
    }

    @Test
    fun actual_success_is_the_only_result_that_publishes_verified_success_and_bytes() = runBlocking {
        val controller = BackupExportController { BackupExportResult.Success(4096, "c".repeat(64)) }
        controller.beginSelection()
        controller.selected(Target())
        val state = controller.state.value
        assertEquals(BackupExportFeedback.VERIFIED_SUCCESS, state.feedback)
        assertEquals(4096L, state.exportedBytes)
        assertEquals(UiMessageTone.INFORMATION, state.messageTone)
        assertFalse(state.partialTargetRemaining)
        assertFalse(state.temporaryFilesRemaining)
        assertEquals(null, state.failureReason)
    }

    @Test
    fun temporary_cleanup_failure_warns_without_invalidating_verified_backup() = runBlocking {
        val target = Target()
        val controller = BackupExportController {
            BackupExportResult.Success(2048, "d".repeat(64), temporaryFilesRemaining = true)
        }
        controller.beginSelection()
        controller.selected(target)
        val state = controller.state.value
        assertEquals(BackupExportFeedback.VERIFIED_SUCCESS, state.feedback)
        assertEquals(2048L, state.exportedBytes)
        assertTrue(state.temporaryFilesRemaining)
        assertFalse(state.partialTargetRemaining)
        assertEquals(UiMessageTone.WARNING, state.messageTone)
        assertTrue(state.message!!.contains("已校验的备份文件仍可使用"))
        assertEquals(0, target.deletes)
    }

    @Test
    fun every_real_failure_keeps_exact_reason_without_claiming_success_or_dropping_source_warning() = runBlocking {
        for (reason in BackupFailureReason.entries) {
            val controller = BackupExportController { BackupExportResult.Failed(reason, false) }
            controller.beginSelection()
            controller.selected(Target())
            assertEquals(reason.name, BackupExportFeedback.FAILED, controller.state.value.feedback)
            assertEquals(reason, controller.state.value.failureReason)
            assertEquals(UiMessageTone.ERROR, controller.state.value.messageTone)
            assertEquals(null, controller.state.value.exportedBytes)
            assertFalse(controller.state.value.partialTargetRemaining)
            if (reason in setOf(BackupFailureReason.INVALID_MEDIA, BackupFailureReason.MEDIA_NOT_CERTIFIED,
                    BackupFailureReason.SOURCE_CHANGED, BackupFailureReason.INTERNAL)) {
                assertTrue(controller.state.value.message!!.contains("原记录和图片未改变"))
            }
        }
    }

    @Test
    fun failed_target_verification_keeps_residue_warning_and_does_not_delete_again() = runBlocking {
        val target = Target()
        val controller = BackupExportController { BackupExportResult.Failed(BackupFailureReason.INCOMPLETE_TARGET, true) }
        controller.beginSelection()
        controller.selected(target)
        assertEquals(BackupExportFeedback.FAILED, controller.state.value.feedback)
        assertEquals(BackupFailureReason.INCOMPLETE_TARGET, controller.state.value.failureReason)
        assertTrue(controller.state.value.partialTargetRemaining)
        assertTrue(controller.state.value.message!!.contains("请勿将其用于恢复"))
        assertEquals(0, target.deletes)
    }

    @Test
    fun explicit_cancelled_results_keep_service_cleanup_outcome_and_do_not_delete_again() = runBlocking {
        for (remaining in listOf(false, true)) {
            val target = Target()
            val controller = BackupExportController { BackupExportResult.Cancelled(remaining) }
            controller.beginSelection()
            controller.selected(target)
            assertEquals(BackupExportFeedback.CANCELLED, controller.state.value.feedback)
            assertEquals(remaining, controller.state.value.partialTargetRemaining)
            assertEquals(if (remaining) UiMessageTone.WARNING else UiMessageTone.INFORMATION, controller.state.value.messageTone)
            assertEquals(null, controller.state.value.exportedBytes)
            assertEquals(0, target.deletes)
        }
    }

    @Test
    fun unexpected_io_exception_keeps_failed_cleanup_as_unknown_residue() = runBlocking {
        val target = Target(deleteWorks = false)
        val controller = BackupExportController { throw IOException("synthetic IO exception") }
        controller.beginSelection()
        controller.selected(target)
        assertEquals(BackupExportFeedback.FAILED, controller.state.value.feedback)
        assertEquals(BackupFailureReason.IO, controller.state.value.failureReason)
        assertTrue(controller.state.value.partialTargetRemaining)
        assertEquals(1, target.deletes)
    }

    @Test
    fun coroutine_cancellation_preserves_unknown_cleanup_before_returning_to_cancelled_caller() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val target = Target(deleteWorks = false)
        val controller = BackupExportController {
            started.complete(Unit)
            CompletableDeferred<Unit>().await()
            error("Cancelled export cannot return success")
        }
        controller.beginSelection()
        val task = async(start = CoroutineStart.UNDISPATCHED) { controller.selected(target) }
        started.await()
        task.cancelAndJoin()
        assertEquals(BackupExportFeedback.CANCELLED, controller.state.value.feedback)
        assertTrue(controller.state.value.partialTargetRemaining)
        assertEquals(UiMessageTone.WARNING, controller.state.value.messageTone)
        assertEquals(null, controller.state.value.exportedBytes)
        assertEquals(1, target.deletes)
    }

    @Test
    fun duplicate_success_callback_does_not_overwrite_outcome_or_delete_verified_file() = runBlocking {
        var calls = 0
        val target = Target()
        val controller = BackupExportController { calls++; BackupExportResult.Success(22, "e".repeat(64)) }
        controller.beginSelection()
        controller.selected(target)
        val verified = controller.state.value
        controller.selected(target)
        controller.selectionCancelled()
        controller.selectionLaunchFailed(possibleResidual = true)
        assertEquals(verified, controller.state.value)
        assertEquals(1, calls)
        assertEquals(0, target.deletes)
    }

    @Test
    fun explicit_new_selection_clears_only_rebuildable_feedback_and_never_deletes_unknown_target() {
        val controller = BackupExportController(interruptedOnRecreation = true) { error("Not selected") }
        assertTrue(controller.state.value.partialTargetRemaining)
        assertTrue(controller.beginSelection())
        assertEquals(BackupExportFeedback.CHOOSING, controller.state.value.feedback)
        assertEquals(null, controller.state.value.message)
        assertFalse(controller.state.value.partialTargetRemaining)
        assertEquals(null, controller.state.value.failureReason)
    }

    private class Target(private val deleteWorks: Boolean = true) : BackupTarget {
        var deletes = 0
        override fun openWrite() = ByteArrayOutputStream()
        override fun openRead() = ByteArrayInputStream(byteArrayOf())
        override fun deletePartial(): Boolean { deletes++; return deleteWorks }
    }
}
