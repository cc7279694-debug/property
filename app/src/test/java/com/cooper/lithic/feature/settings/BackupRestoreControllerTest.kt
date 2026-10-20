package com.cooper.lithic.feature.settings

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

class BackupRestoreControllerTest {
    @Test fun selecting_and_validating_a_backup_only_opens_preview_until_explicit_confirmation() = runBlocking {
        val confirmed = mutableListOf<String>()
        val controller = BackupRestoreController(prepare = { preview() }, confirm = {
            confirmed += it
            RestoreResult.Success
        }, discard = {})
        assertTrue(controller.beginSelection())
        controller.selected { ByteArrayInputStream(byteArrayOf(1)) }
        assertEquals(RestoreUiPhase.PREVIEW, controller.state.value.phase)
        assertEquals(3L, controller.state.value.preview?.backupCounts?.get("long_term_projects"))
        assertTrue(confirmed.isEmpty())
        controller.confirmOverwrite()
        assertEquals(listOf("validated-private-token"), confirmed)
        assertEquals(RestoreUiPhase.IDLE, controller.state.value.phase)
        assertNull(controller.state.value.preview)
        assertTrue(controller.state.value.message!!.contains("恢复成功"))
    }

    @Test fun confirm_without_a_live_validated_preview_never_reaches_storage() = runBlocking {
        val controller = BackupRestoreController(prepare = { error("No selection") },
            confirm = { error("An unconfirmed token must not reach storage") }, discard = {})
        controller.confirmOverwrite()
        assertTrue(controller.beginSelection())
        controller.confirmOverwrite()
        assertEquals(RestoreUiPhase.CHOOSING, controller.state.value.phase)
    }

    @Test fun cancelling_selection_and_preview_never_applies_or_reuses_the_prepared_backup() = runBlocking {
        val discarded = mutableListOf<String>()
        val controller = BackupRestoreController(prepare = { preview() },
            confirm = { error("Cancelled restore cannot overwrite") }, discard = { discarded += it })
        controller.beginSelection()
        controller.selectionCancelled()
        assertEquals(RestoreUiPhase.IDLE, controller.state.value.phase)
        assertTrue(controller.state.value.message!!.contains("取消"))
        controller.beginSelection()
        controller.selected { ByteArrayInputStream(byteArrayOf(1)) }
        assertFalse(controller.beginSelection())
        controller.discardPreview()
        controller.confirmOverwrite()
        assertEquals(listOf("validated-private-token"), discarded)
        assertNull(controller.state.value.preview)
        assertTrue(controller.state.value.message!!.contains("未改变"))
    }

    @Test fun validation_failure_keeps_current_data_and_requires_a_new_selection() = runBlocking {
        val controller = BackupRestoreController(prepare = { throw IOException("Private implementation path") },
            confirm = { error("Invalid backup cannot restore") }, discard = {})
        controller.beginSelection()
        controller.selected { ByteArrayInputStream(byteArrayOf(1)) }
        assertEquals(RestoreUiPhase.IDLE, controller.state.value.phase)
        assertNull(controller.state.value.preview)
        assertTrue(controller.state.value.message!!.contains("未改变"))
        assertFalse(controller.state.value.message!!.contains("Private implementation path"))
        assertTrue(controller.beginSelection())
    }

    @Test fun duplicate_picker_callback_and_double_confirmation_cannot_start_a_second_restore() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val entered = CompletableDeferred<Unit>()
        var preparations = 0
        var confirmations = 0
        val controller = BackupRestoreController(prepare = { preparations += 1; preview() }, confirm = {
            confirmations += 1
            entered.complete(Unit)
            release.await()
            RestoreResult.Success
        }, discard = {})
        controller.beginSelection()
        controller.selected { ByteArrayInputStream(byteArrayOf(1)) }
        controller.selected { error("Duplicate callback must not open URI") }
        val first = launch { controller.confirmOverwrite() }
        entered.await()
        assertEquals(RestoreUiPhase.RESTORING, controller.state.value.phase)
        assertFalse(controller.beginSelection())
        controller.confirmOverwrite()
        controller.discardPreview()
        release.complete(Unit)
        first.join()
        assertEquals(1, preparations)
        assertEquals(1, confirmations)
    }

    @Test fun recreated_process_rejects_late_picker_result_and_never_restores_automatically() = runBlocking {
        val controller = BackupRestoreController(interruptedOnRecreation = true,
            prepare = { error("A new process must reselect") }, confirm = { error("No auto overwrite") }, discard = {})
        controller.selected { error("Old URI must not be reopened") }
        controller.confirmOverwrite()
        assertEquals(RestoreUiPhase.IDLE, controller.state.value.phase)
        assertNull(controller.state.value.preview)
        assertTrue(controller.state.value.message!!.contains("重新选择"))
        assertTrue(controller.beginSelection())
    }

    @Test fun failed_confirmation_distinguishes_preserved_current_data_from_unknown_outcome() = runBlocking {
        for (preserved in listOf(true, false)) {
            val controller = BackupRestoreController(prepare = { preview() },
                confirm = { RestoreResult.Failed("受控恢复失败", currentPreserved = preserved) }, discard = {})
            controller.beginSelection()
            controller.selected { ByteArrayInputStream(byteArrayOf(1)) }
            controller.confirmOverwrite()
            assertEquals(RestoreUiPhase.IDLE, controller.state.value.phase)
            assertNull(controller.state.value.preview)
            assertEquals(preserved, controller.state.value.message!!.contains("当前记录已保留"))
            assertFalse(controller.state.value.message!!.contains("恢复成功"))
        }
    }

    @Test fun launch_failure_is_retryable_without_preparing_any_backup() {
        val controller = BackupRestoreController(prepare = { error("Picker failed") },
            confirm = { error("Picker failed") }, discard = {})
        controller.beginSelection()
        controller.selectionLaunchFailed()
        assertEquals(RestoreUiPhase.IDLE, controller.state.value.phase)
        assertTrue(controller.state.value.message!!.contains("无法打开"))
        assertTrue(controller.beginSelection())
    }

    @Test fun cancellation_during_validation_returns_to_safe_manual_selection_without_a_preview() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val controller = BackupRestoreController(prepare = {
            entered.complete(Unit)
            release.await()
            preview()
        }, confirm = { error("Cancelled validation cannot confirm") }, discard = {})
        controller.beginSelection()
        val validation = launch { controller.selected { ByteArrayInputStream(byteArrayOf(1)) } }
        entered.await()
        assertEquals(RestoreUiPhase.VALIDATING, controller.state.value.phase)
        validation.cancelAndJoin()
        assertEquals(RestoreUiPhase.IDLE, controller.state.value.phase)
        assertNull(controller.state.value.preview)
        assertTrue(controller.state.value.message!!.contains("未改变"))
        controller.confirmOverwrite()
    }

    @Test fun unexpected_confirmation_failure_does_not_promise_old_data_preservation_or_expose_paths() = runBlocking {
        val controller = BackupRestoreController(prepare = { preview() },
            confirm = { throw IOException("Private /data/path") }, discard = {})
        controller.beginSelection()
        controller.selected { ByteArrayInputStream(byteArrayOf(1)) }
        controller.confirmOverwrite()
        assertEquals(RestoreUiPhase.IDLE, controller.state.value.phase)
        assertFalse(controller.state.value.message!!.contains("当前记录已保留"))
        assertFalse(controller.state.value.message!!.contains("Private /data/path"))
        assertFalse(controller.state.value.message!!.contains("恢复成功"))
    }

    private fun preview() = RestorePreview("validated-private-token",
        backupCounts = mapOf("long_term_projects" to 3L, "subscriptions" to 2L),
        currentCounts = mapOf("long_term_projects" to 1L, "subscriptions" to 0L),
        createdAt = "2026-10-20T00:00:00Z", recordZoneId = "Asia/Shanghai")
}
