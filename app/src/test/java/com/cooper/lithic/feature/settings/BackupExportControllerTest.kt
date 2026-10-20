package com.cooper.lithic.feature.settings

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

class BackupExportControllerTest {
    @Test
    fun one_selection_blocks_repeated_clicks_until_cancelled() {
        val controller = BackupExportController { error("No target has been selected") }
        assertTrue(controller.beginSelection())
        assertFalse(controller.beginSelection())
        assertEquals(BackupUiPhase.CHOOSING, controller.state.value.phase)
        controller.selectionCancelled()
        assertEquals("已取消选择保存位置，未导出备份。", controller.state.value.message)
        assertTrue(controller.beginSelection())
    }

    @Test
    fun success_is_only_shown_after_complete_export_result() = runBlocking {
        val target = FakeTarget()
        var called = 0
        val controller = BackupExportController { received ->
            assertTrue(received === target)
            called += 1
            BackupExportResult.Success(1024, "a".repeat(64))
        }
        controller.beginSelection()
        controller.selected(target)
        assertEquals(1, called)
        assertEquals(BackupUiPhase.IDLE, controller.state.value.phase)
        assertEquals("完整备份已导出并通过校验。", controller.state.value.message)
        assertEquals(1024L, controller.state.value.exportedBytes)
        assertEquals(0, target.deletes)
    }

    @Test
    fun active_export_blocks_second_chooser_and_duplicate_result() = runBlocking {
        val finish = CompletableDeferred<Unit>()
        var calls = 0
        val controller = BackupExportController {
            calls += 1
            finish.await()
            BackupExportResult.Success(128, "b".repeat(64))
        }
        val target = FakeTarget()
        controller.beginSelection()
        val task = async(start = CoroutineStart.UNDISPATCHED) { controller.selected(target) }
        assertEquals(BackupUiPhase.EXPORTING, controller.state.value.phase)
        assertFalse(controller.beginSelection())
        controller.selected(target)
        assertEquals(0, target.deletes)
        finish.complete(Unit)
        task.await()
        assertEquals(1, calls)
    }

    @Test
    fun incomplete_target_failure_is_not_success_and_reports_residual() = runBlocking {
        val controller = BackupExportController {
            BackupExportResult.Failed(BackupFailureReason.INCOMPLETE_TARGET, partialTargetRemaining = true)
        }
        controller.beginSelection()
        controller.selected(FakeTarget())
        assertEquals(BackupUiPhase.IDLE, controller.state.value.phase)
        assertTrue(controller.state.value.message!!.contains("未完成"))
        assertTrue(controller.state.value.message!!.contains("残留"))
        assertEquals(null, controller.state.value.exportedBytes)
    }

    @Test
    fun corrupt_source_has_explicit_failure_without_claiming_partial_backup() = runBlocking {
        val controller = BackupExportController {
            BackupExportResult.Failed(BackupFailureReason.INVALID_MEDIA, partialTargetRemaining = false)
        }
        controller.beginSelection()
        controller.selected(FakeTarget())
        assertTrue(controller.state.value.message!!.contains("图片"))
        assertTrue(controller.state.value.message!!.contains("未完成"))
        assertEquals(null, controller.state.value.exportedBytes)
    }

    @Test
    fun unexpected_exception_deletes_only_selected_target_and_reports_failure() = runBlocking {
        val target = FakeTarget()
        val controller = BackupExportController { throw IOException("synthetic write exception") }
        controller.beginSelection()
        controller.selected(target)
        assertEquals(1, target.deletes)
        assertTrue(controller.state.value.message!!.contains("未完成"))
        assertEquals(null, controller.state.value.exportedBytes)
    }

    @Test
    fun coroutine_cancellation_closes_selected_target_without_success() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val target = FakeTarget()
        val controller = BackupExportController {
            started.complete(Unit)
            CompletableDeferred<Unit>().await()
            error("Cancelled export cannot return success")
        }
        controller.beginSelection()
        val task = async(start = CoroutineStart.UNDISPATCHED) { controller.selected(target) }
        started.await()
        task.cancelAndJoin()
        assertEquals(1, target.deletes)
        assertEquals(BackupUiPhase.IDLE, controller.state.value.phase)
        assertTrue(controller.state.value.message!!.contains("取消"))
        assertEquals(null, controller.state.value.exportedBytes)
    }

    @Test
    fun process_recreation_never_restarts_export_on_late_picker_delivery() = runBlocking {
        var calls = 0
        val target = FakeTarget()
        val controller = BackupExportController(interruptedOnRecreation = true, export = {
            calls += 1
            error("Do not restart an interrupted export")
        })
        controller.selected(target)
        assertEquals(0, calls)
        assertEquals(0, target.deletes)
        assertTrue(controller.state.value.message!!.contains("中断"))
        assertEquals(BackupUiPhase.IDLE, controller.state.value.phase)
    }

    @Test
    fun launch_failure_releases_chooser_state_and_remains_retryable() {
        val controller = BackupExportController { error("Must not export") }
        controller.beginSelection()
        controller.selectionLaunchFailed()
        assertEquals(BackupUiPhase.IDLE, controller.state.value.phase)
        assertTrue(controller.state.value.message!!.contains("无法打开"))
        assertTrue(controller.beginSelection())
    }

    @Test
    fun interrupted_delivery_does_not_delete_unproven_target() = runBlocking {
        val target = FakeTarget(deleteWorks = false)
        val controller = BackupExportController(interruptedOnRecreation = true, export = { error("Must not export") })
        controller.selected(target)
        assertEquals(0, target.deletes)
        assertTrue(controller.state.value.message!!.contains("残留"))
        assertEquals(null, controller.state.value.exportedBytes)
    }

    @Test
    fun duplicate_callback_after_verified_success_does_not_delete_completed_backup() = runBlocking {
        val target = FakeTarget()
        var calls = 0
        val controller = BackupExportController {
            calls += 1
            BackupExportResult.Success(100, "c".repeat(64))
        }
        controller.beginSelection()
        controller.selected(target)
        controller.selected(target)
        assertEquals(1, calls)
        assertEquals(0, target.deletes)
        assertEquals("完整备份已导出并通过校验。", controller.state.value.message)
    }

    @Test
    fun explicit_cancelled_result_preserves_service_cleanup_outcome() = runBlocking {
        val target = FakeTarget()
        val controller = BackupExportController { BackupExportResult.Cancelled(partialTargetRemaining = true) }
        controller.beginSelection()
        controller.selected(target)
        assertEquals(0, target.deletes)
        assertTrue(controller.state.value.message!!.contains("取消"))
        assertTrue(controller.state.value.message!!.contains("残留"))
        assertEquals(null, controller.state.value.exportedBytes)
    }
    @Test
    fun uncertified_registered_media_blocks_complete_export_with_preserved_source_feedback() = runBlocking {
        val controller = BackupExportController {
            BackupExportResult.Failed(BackupFailureReason.MEDIA_NOT_CERTIFIED, partialTargetRemaining = false)
        }
        controller.beginSelection()
        controller.selected(FakeTarget())
        assertTrue(controller.state.value.message!!.contains("无法确认已登记图片的完整性"))
        assertTrue(controller.state.value.message!!.contains("未生成完整备份"))
        assertTrue(controller.state.value.message!!.contains("原记录和图片未改变"))
        assertEquals(null, controller.state.value.exportedBytes)
    }
    @Test
    fun verified_target_remains_success_when_owned_temporary_files_cannot_be_cleaned() = runBlocking {
        val target = FakeTarget()
        val controller = BackupExportController {
            BackupExportResult.Success(512, "d".repeat(64), temporaryFilesRemaining = true)
        }
        controller.beginSelection()
        controller.selected(target)
        assertEquals(512L, controller.state.value.exportedBytes)
        assertTrue(controller.state.value.message!!.contains("完整备份已导出并通过校验"))
        assertTrue(controller.state.value.message!!.contains("内部临时文件暂未清理"))
        assertEquals(0, target.deletes)
    }
    @Test
    fun rejected_picker_uri_reports_possible_residual_without_starting_export() {
        var exports = 0
        val controller = BackupExportController {
            exports += 1
            error("An unverified target cannot start export")
        }
        controller.beginSelection()
        controller.selectionLaunchFailed(possibleResidual = true)
        assertEquals(0, exports)
        assertEquals(BackupUiPhase.IDLE, controller.state.value.phase)
        assertTrue(controller.state.value.message!!.contains("残留"))
        assertEquals(null, controller.state.value.exportedBytes)
    }
    private class FakeTarget(private val deleteWorks: Boolean = true) : BackupTarget {
        var deletes = 0
        override fun openWrite() = ByteArrayOutputStream()
        override fun openRead() = ByteArrayInputStream(byteArrayOf())
        override fun deletePartial(): Boolean { deletes += 1; return deleteWorks }
    }
}
