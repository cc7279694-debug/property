package com.cooper.lithic.feature.settings

import com.cooper.lithic.core.design.UiMessageTone
import com.cooper.lithic.data.backup.RestorePreview
import com.cooper.lithic.data.backup.RestoreResult
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class P03BackupRestoreFeedbackTest {
    @Test fun recreated_interrupted_restore_reports_unknown_without_promising_old_or_new_dataset() {
        val controller = BackupRestoreController(interruptedOnRecreation = true,
            prepare = { error("A recreated process must not reopen a backup automatically") },
            confirm = { error("A recreated process must not overwrite automatically") }, discard = {})

        val message = controller.state.value.message.orEmpty()
        assertTrue(message.contains("恢复结果暂无法确认"))
        assertTrue(message.contains("请勿清除应用数据"))
        assertFalse(message.contains("当前记录未改变"))
        assertFalse(message.contains("恢复成功"))
        assertEquals(RestoreFeedbackKind.OUTCOME_UNKNOWN, controller.state.value.feedback)
        assertEquals(UiMessageTone.WARNING, controller.state.value.messageTone)
    }

    @Test fun idle_or_raw_success_wording_cannot_authenticate_a_successful_restore() {
        assertEquals(RestoreFeedbackKind.NONE, BackupRestoreUiState().feedback)
        val untyped = BackupRestoreUiState(message = "恢复成功，测试用未认证文案")
        assertEquals(RestoreFeedbackKind.NONE, untyped.feedback)
        assertEquals(UiMessageTone.ERROR, untyped.messageTone)
    }

    @Test fun a_validated_preview_is_not_a_success_until_explicit_confirmation_returns_success() = runBlocking {
        var confirmations = 0
        val controller = controller(confirm = { confirmations += 1; RestoreResult.Success })
        controller.beginSelection()
        controller.selected { input() }
        assertEquals(RestoreUiPhase.PREVIEW, controller.state.value.phase)
        assertEquals(RestoreFeedbackKind.NONE, controller.state.value.feedback)
        assertEquals(0, confirmations)
        controller.confirmOverwrite()
        assertEquals(1, confirmations)
        assertEquals(RestoreFeedbackKind.SUCCESS, controller.state.value.feedback)
        assertEquals(UiMessageTone.INFORMATION, controller.state.value.messageTone)
        assertNull(controller.state.value.preview)
        controller.selected { error("Late callback must not reopen source") }
        controller.confirmOverwrite()
        assertEquals(1, confirmations)
        assertEquals(RestoreFeedbackKind.SUCCESS, controller.state.value.feedback)
    }

    @Test fun file_picker_cancellation_and_launch_failure_are_distinct_from_success() {
        val controller = controller()
        controller.beginSelection()
        controller.selectionCancelled()
        assertEquals(RestoreFeedbackKind.SELECTION_CANCELLED, controller.state.value.feedback)
        assertEquals(UiMessageTone.INFORMATION, controller.state.value.messageTone)
        controller.beginSelection()
        assertEquals(RestoreFeedbackKind.NONE, controller.state.value.feedback)
        controller.selectionLaunchFailed()
        assertEquals(RestoreFeedbackKind.SELECTION_FAILED, controller.state.value.feedback)
        assertEquals(UiMessageTone.ERROR, controller.state.value.messageTone)
    }

    @Test fun invalid_backup_has_known_preconfirmation_preservation_and_no_confirmation() = runBlocking {
        val controller = BackupRestoreController(prepare = { throw IOException("private /data/path") },
            confirm = { error("Invalid backup must not confirm") }, discard = {})
        controller.beginSelection()
        controller.selected { input() }
        assertEquals(RestoreFeedbackKind.VALIDATION_FAILED, controller.state.value.feedback)
        assertEquals(UiMessageTone.ERROR, controller.state.value.messageTone)
        assertTrue(controller.state.value.message.orEmpty().contains("当前记录未改变"))
        assertFalse(controller.state.value.message.orEmpty().contains("private /data/path"))
        assertNull(controller.state.value.preview)
    }

    @Test fun cancelled_validation_remains_preconfirmation_and_does_not_create_success() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val controller = BackupRestoreController(prepare = { entered.complete(Unit); release.await(); preview() },
            confirm = { error("Cancelled validation must not confirm") }, discard = {})
        controller.beginSelection()
        val validation = launch { controller.selected { input() } }
        entered.await()
        assertFalse(controller.beginSelection())
        validation.cancelAndJoin()
        assertEquals(RestoreFeedbackKind.VALIDATION_CANCELLED, controller.state.value.feedback)
        assertEquals(UiMessageTone.INFORMATION, controller.state.value.messageTone)
        assertNull(controller.state.value.preview)
    }

    @Test fun cancelled_preview_with_unremoved_temporary_files_is_a_warning_not_a_restore() = runBlocking {
        val controller = controller(discard = { throw IOException("private temporary path") })
        controller.beginSelection()
        controller.selected { input() }
        controller.discardPreview()
        assertEquals(RestoreFeedbackKind.PREVIEW_CANCELLED_WITH_RESIDUE, controller.state.value.feedback)
        assertEquals(UiMessageTone.WARNING, controller.state.value.messageTone)
        assertTrue(controller.state.value.message.orEmpty().contains("当前记录未改变"))
        assertFalse(controller.state.value.message.orEmpty().contains("private temporary path"))
        assertNull(controller.state.value.preview)
        controller.confirmOverwrite()
        assertEquals(RestoreFeedbackKind.PREVIEW_CANCELLED_WITH_RESIDUE, controller.state.value.feedback)
    }

    @Test fun successfully_discarded_preview_preserves_current_and_revokes_the_confirmation() = runBlocking {
        val discarded = mutableListOf<String>()
        val controller = controller(discard = { discarded += it })
        controller.beginSelection()
        controller.selected { input() }
        controller.discardPreview()
        controller.confirmOverwrite()
        assertEquals(listOf("p03-private-preview-token"), discarded)
        assertEquals(RestoreFeedbackKind.PREVIEW_CANCELLED, controller.state.value.feedback)
        assertEquals(UiMessageTone.INFORMATION, controller.state.value.messageTone)
    }

    @Test fun failed_confirmation_uses_the_structured_preserved_flag_and_never_a_successful_phase() = runBlocking {
        for (preserved in listOf(true, false)) {
            val controller = controller(confirm = { RestoreResult.Failed("受控未完成。", preserved) })
            controller.beginSelection()
            controller.selected { input() }
            controller.confirmOverwrite()
            assertEquals(RestoreUiPhase.IDLE, controller.state.value.phase)
            assertEquals(if (preserved) RestoreFeedbackKind.FAILED_CURRENT_PRESERVED else RestoreFeedbackKind.OUTCOME_UNKNOWN,
                controller.state.value.feedback)
            assertEquals(if (preserved) UiMessageTone.ERROR else UiMessageTone.WARNING, controller.state.value.messageTone)
            assertEquals(preserved, controller.state.value.message.orEmpty().contains("当前记录已保留"))
            assertFalse(controller.state.value.message.orEmpty().contains("恢复成功"))
        }
    }

    @Test fun confirmation_exception_is_unknown_without_promising_the_current_dataset() = runBlocking {
        val controller = controller(confirm = { throw IOException("private /data/path") })
        controller.beginSelection()
        controller.selected { input() }
        controller.confirmOverwrite()
        assertEquals(RestoreFeedbackKind.OUTCOME_UNKNOWN, controller.state.value.feedback)
        assertEquals(UiMessageTone.WARNING, controller.state.value.messageTone)
        assertFalse(controller.state.value.message.orEmpty().contains("当前记录已保留"))
        assertFalse(controller.state.value.message.orEmpty().contains("private /data/path"))
    }

    @Test fun confirmation_cancellation_is_unknown_and_duplicate_requests_cannot_consume_a_second_token() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var confirmations = 0
        val controller = controller(confirm = {
            confirmations += 1
            entered.complete(Unit)
            release.await()
            RestoreResult.Success
        })
        controller.beginSelection()
        controller.selected { input() }
        val confirmation = launch { controller.confirmOverwrite() }
        entered.await()
        assertEquals(RestoreUiPhase.RESTORING, controller.state.value.phase)
        assertFalse(controller.beginSelection())
        controller.confirmOverwrite()
        controller.discardPreview()
        controller.selected { error("Busy duplicate callback must not read") }
        assertEquals(1, confirmations)
        confirmation.cancelAndJoin()
        assertEquals(RestoreFeedbackKind.OUTCOME_UNKNOWN, controller.state.value.feedback)
        assertEquals(UiMessageTone.WARNING, controller.state.value.messageTone)
        assertNull(controller.state.value.preview)
    }

    private fun controller(
        confirm: suspend (String) -> RestoreResult = { error("Unexpected confirmation") },
        discard: suspend (String) -> Unit = {},
    ) = BackupRestoreController(prepare = { preview() }, confirm = confirm, discard = discard)

    private fun input() = ByteArrayInputStream(byteArrayOf(1))
    private fun preview() = RestorePreview("p03-private-preview-token",
        backupCounts = mapOf("long_term_projects" to 3L, "subscriptions" to 2L),
        currentCounts = mapOf("long_term_projects" to 1L, "subscriptions" to 0L),
        createdAt = "2026-10-20T00:00:00Z", recordZoneId = "Asia/Shanghai")
}
