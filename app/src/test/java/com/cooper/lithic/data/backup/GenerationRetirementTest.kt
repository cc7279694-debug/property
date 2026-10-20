package com.cooper.lithic.data.backup

import com.cooper.lithic.data.repository.LocalWriteCoordinator
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class GenerationRetirementTest {
    @Test fun retirement_waits_for_inflight_write_then_rejects_queued_old_work() = runBlocking {
        val gate=LocalWriteCoordinator(); val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        val write=launch { gate.withWrite { entered.complete(Unit);release.await() } };entered.await()
        var closed=false
        val retire=launch { gate.retire(verify={},dispose={closed=true}) }
        yield();assertFalse(closed);release.complete(Unit);write.join();retire.join();assertTrue(closed)
        try { gate.withWrite { fail("Retired generation must not write") };fail("Expected rejection") } catch (_:IllegalStateException) {}
        try { gate.withSnapshot { fail("Retired generation must not reopen Room") };fail("Expected rejection") } catch (_:IllegalStateException) {}
    }
    @Test fun failed_precommit_verification_keeps_original_coordinator_readable() = runBlocking {
        val gate=LocalWriteCoordinator()
        try { gate.retire(verify={throw java.io.IOException("changed snapshot")},dispose={fail("Must not close")});fail() } catch (_:java.io.IOException) {}
        assertEquals(42,gate.withWrite { 42 })
    }
    @Test fun close_failure_still_fences_all_old_writes() = runBlocking {
        val gate=LocalWriteCoordinator()
        try { gate.retire(verify={},dispose={throw java.io.IOException("close")});fail() } catch (_:java.io.IOException) {}
        try { gate.withWrite { fail() };fail() } catch (_:IllegalStateException) {}
    }
}
