package com.cooper.lithic.data.backup

import com.cooper.lithic.data.repository.LocalWriteCoordinator
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CoordinatedBackupReaderTest {
    @Test
    fun coordinated_write_cannot_mix_business_and_settings_versions() = runBlocking {
        val coordinator = LocalWriteCoordinator()
        var business = "before"
        var settings = "before"
        val reading = CompletableDeferred<Unit>()
        val continueReading = CompletableDeferred<Unit>()
        val reader = CoordinatedBackupReader(coordinator) {
            val first = business
            reading.complete(Unit)
            continueReading.await()
            first to settings
        }
        val capture = async(start = CoroutineStart.UNDISPATCHED) { reader.capture() }
        reading.await()
        val write = async(start = CoroutineStart.UNDISPATCHED) {
            coordinator.withWrite { business = "after"; settings = "after" }
        }
        assertFalse(write.isCompleted)
        continueReading.complete(Unit)
        assertEquals("before" to "before", capture.await())
        write.await()
        assertEquals("after" to "after", business to settings)
    }

    @Test
    fun capture_waits_for_whole_coordinated_write_before_reading() = runBlocking {
        val coordinator = LocalWriteCoordinator()
        var business = 1
        var settings = 1
        val continueWrite = CompletableDeferred<Unit>()
        val write = async(start = CoroutineStart.UNDISPATCHED) {
            coordinator.withWrite {
                business = 2
                continueWrite.await()
                settings = 2
            }
        }
        val capture = async(start = CoroutineStart.UNDISPATCHED) {
            CoordinatedBackupReader(coordinator) { business to settings }.capture()
        }
        assertFalse(capture.isCompleted)
        continueWrite.complete(Unit)
        write.await()
        assertEquals(2 to 2, capture.await())
    }

    @Test
    fun read_failure_returns_no_snapshot_and_releases_coordinator() = runBlocking {
        val coordinator = LocalWriteCoordinator()
        val reader = CoordinatedBackupReader<String>(coordinator) { throw IOException("synthetic read failure") }
        try {
            reader.capture()
            fail("Read failure must not return a snapshot")
        } catch (expected: IOException) {
            assertEquals("synthetic read failure", expected.message)
        }
        var wrote = false
        coordinator.withWrite { wrote = true }
        assertTrue(wrote)
    }

    @Test
    fun cancelled_capture_releases_lock_without_returning_partial_snapshot() = runBlocking {
        val coordinator = LocalWriteCoordinator()
        val started = CompletableDeferred<Unit>()
        val never = CompletableDeferred<Unit>()
        val capture = async(start = CoroutineStart.UNDISPATCHED) {
            CoordinatedBackupReader(coordinator) {
                started.complete(Unit)
                never.await()
                "not published"
            }.capture()
        }
        started.await()
        capture.cancelAndJoin()
        assertTrue(capture.isCancelled)
        assertEquals("next write", coordinator.withWrite { "next write" })
    }
}
