package com.cooper.lithic.feature.settings

import com.cooper.lithic.data.backup.BackupInspectionResult
import com.cooper.lithic.data.backup.BackupInspectionStatus
import com.cooper.lithic.data.backup.BackupInspectionSummary
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BackupInspectionControllerTest {
    private val summary = BackupInspectionSummary(4, "2026-11-01T00:00:00Z", mapOf("projects" to 1L), 0, 0)
    private fun result(status: BackupInspectionStatus) = BackupInspectionResult(status,
        summary.takeIf { status == BackupInspectionStatus.VERIFIED })
    private fun input() = ByteArrayInputStream(byteArrayOf(1))

    @Test fun only_real_results_can_produce_each_typed_outcome() = runBlocking {
        for (status in BackupInspectionStatus.entries) {
            val owner = Any()
            val c = BackupInspectionController(currentOwner = { owner }) { _, current ->
                assertTrue(current()); result(status)
            }
            val id = requireNotNull(c.beginSelection())
            c.selected(id, ::input)
            assertEquals(status, c.state.value.result?.status)
            assertEquals(BackupInspectionPhase.IDLE, c.state.value.phase)
            assertFalse(c.state.value.message.orEmpty().contains("恢复成功"))
        }
    }

    @Test fun selection_cancel_is_read_only_and_stale_callback_is_ignored() = runBlocking {
        val owner = Any(); var calls = 0
        val c = BackupInspectionController(currentOwner = { owner }) { _, _ -> calls++; result(BackupInspectionStatus.VERIFIED) }
        val first = requireNotNull(c.beginSelection()); c.selectionCancelled(first)
        val second = requireNotNull(c.beginSelection()); assertNotEquals(first, second)
        c.selected(first, ::input)
        assertEquals(0, calls); assertEquals(second, c.state.value.requestId)
        c.selectionCancelled(second); assertNull(c.state.value.result)
    }

    @Test fun duplicate_clicks_and_callbacks_do_not_start_two_checks() = runBlocking {
        val owner = Any(); var calls = 0; val finish = CompletableDeferred<Unit>()
        val c = BackupInspectionController(currentOwner = { owner }) { _, _ ->
            calls++; finish.await(); result(BackupInspectionStatus.VERIFIED)
        }
        val id = requireNotNull(c.beginSelection()); assertNull(c.beginSelection())
        val work = async(start = CoroutineStart.UNDISPATCHED) { c.selected(id, ::input) }
        c.selected(id, ::input); assertNull(c.beginSelection()); assertEquals(1, calls)
        finish.complete(Unit); work.await(); assertEquals(BackupInspectionStatus.VERIFIED, c.state.value.result?.status)
    }

    @Test fun cancel_retains_busy_until_inflight_read_really_returns_and_rejects_late_success() = runBlocking {
        val owner = Any(); val finish = CompletableDeferred<Unit>()
        val c = BackupInspectionController(currentOwner = { owner }) { _, _ -> finish.await(); result(BackupInspectionStatus.VERIFIED) }
        val work = async(start = CoroutineStart.UNDISPATCHED) { c.selected(requireNotNull(c.beginSelection()), ::input) }
        assertTrue(c.requestCancellation()); assertTrue(c.state.value.busy); assertNull(c.beginSelection())
        finish.complete(Unit); work.await()
        assertFalse(c.state.value.busy); assertEquals(BackupInspectionStatus.UNKNOWN, c.state.value.result?.status)
    }

    @Test fun equal_generation_names_do_not_allow_a_different_owner_instance_to_publish() = runBlocking {
        data class Owner(val id: String)
        var owner: Any? = Owner("same-generation"); val finish = CompletableDeferred<Unit>()
        val c = BackupInspectionController(currentOwner = { owner }) { _, current ->
            finish.await(); assertFalse(current()); result(BackupInspectionStatus.VERIFIED)
        }
        val work = async(start = CoroutineStart.UNDISPATCHED) { c.selected(requireNotNull(c.beginSelection()), ::input) }
        owner = Owner("same-generation"); assertTrue(c.ownerChanged())
        assertTrue(c.state.value.busy); finish.complete(Unit); work.await()
        assertEquals(BackupInspectionStatus.UNKNOWN, c.state.value.result?.status)
    }

    @Test fun switching_owner_in_chooser_does_not_open_the_selected_document() = runBlocking {
        var owner: Any? = Any(); var opens = 0
        val c = BackupInspectionController(currentOwner = { owner }) { open, _ -> open(); result(BackupInspectionStatus.VERIFIED) }
        val id = requireNotNull(c.beginSelection()); owner = Any(); c.ownerChanged()
        assertTrue(c.state.value.busy); assertNull(c.beginSelection())
        c.selected(id) { opens++; input() }
        assertEquals(0, opens); assertEquals(BackupInspectionStatus.UNKNOWN, c.state.value.result?.status)
        assertFalse(c.state.value.busy)
    }

    @Test fun missing_owner_cannot_start_a_check_or_forge_zero_data() {
        val c = BackupInspectionController(currentOwner = { null }) { _, _ -> error("must not run") }
        assertNull(c.beginSelection()); assertEquals(BackupInspectionStatus.UNKNOWN, c.state.value.result?.status)
        assertNull(c.state.value.result?.summary)
    }

    @Test fun recreation_preserves_unknown_without_replaying_input_or_result() {
        val c = BackupInspectionController(true, { Any() }) { _, _ -> error("no replay") }
        assertEquals(BackupInspectionStatus.UNKNOWN, c.state.value.result?.status)
        assertTrue(c.state.value.requiresRecreationWarning()); assertNull(c.state.value.requestId)
    }

    @Test fun exceptions_and_picker_failure_never_leak_private_paths() = runBlocking {
        val owner = Any(); val secret = "/data/user/0/synthetic-only/private.zip"
        val c = BackupInspectionController(currentOwner = { owner }) { _, _ -> throw IOException(secret) }
        c.selected(requireNotNull(c.beginSelection()), ::input)
        assertEquals(BackupInspectionStatus.UNKNOWN, c.state.value.result?.status)
        assertTrue(c.state.value.result!!.temporaryFilesRetained)
        assertFalse(c.state.value.message.orEmpty().contains(secret))
        c.selectionLaunchFailed(requireNotNull(c.beginSelection()))
        assertEquals(BackupInspectionStatus.UNKNOWN, c.state.value.result?.status)
        assertFalse(c.state.value.message.orEmpty().contains(secret))
    }

    @Test fun process_recreation_does_not_reuse_a_picker_request_identity() = runBlocking {
        val owner = Any(); var opens = 0
        val before = BackupInspectionController(currentOwner = { owner }) { _, _ -> error("no result") }
        val old = requireNotNull(before.beginSelection())
        val after = BackupInspectionController(true, { owner }) { open, _ -> open(); result(BackupInspectionStatus.VERIFIED) }
        val current = requireNotNull(after.beginSelection())
        assertNotEquals(old, current)
        after.selected(old) { opens++; input() }
        assertEquals(0, opens)
        assertEquals(current, after.state.value.requestId)
        after.selectionCancelled(current)
    }

    @Test fun verified_without_a_summary_is_unknown() = runBlocking {
        val owner = Any(); val c = BackupInspectionController(currentOwner = { owner }) { _, _ -> BackupInspectionResult(BackupInspectionStatus.VERIFIED) }
        c.selected(requireNotNull(c.beginSelection()), ::input)
        assertEquals(BackupInspectionStatus.UNKNOWN, c.state.value.result?.status)
    }

    @Test fun a_restored_picker_nonce_can_only_be_drained_and_cannot_open_a_document() = runBlocking {
        val owner = Any(); var opens = 0
        val c = BackupInspectionController(true, { owner }) { open, _ -> open(); result(BackupInspectionStatus.VERIFIED) }
        c.retainPendingSelection("old-process-nonce")
        assertTrue(c.state.value.busy); assertNull(c.beginSelection())
        c.selected("old-process-nonce") { opens++; input() }
        assertEquals(0, opens); assertFalse(c.state.value.busy)
        assertEquals(BackupInspectionStatus.UNKNOWN, c.state.value.result?.status)
        assertNotNull(c.beginSelection())
    }
}
