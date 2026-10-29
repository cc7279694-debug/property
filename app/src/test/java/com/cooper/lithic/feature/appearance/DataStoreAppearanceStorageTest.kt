package com.cooper.lithic.feature.appearance

import androidx.datastore.core.CorruptionException
import java.io.File
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreAppearanceStorageTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun one_preferences_file_cannot_have_competing_active_instances() = runBlocking {
        val file = File(temporary.newFolder("one-store"), "appearance.preferences_pb")
        val firstJob = SupervisorJob()
        val secondJob = SupervisorJob()
        try {
            val first = DataStoreAppearanceStorage.create(file, CoroutineScope(firstJob + Dispatchers.IO))
            first.write("charcoal")
            try {
                DataStoreAppearanceStorage.create(file, CoroutineScope(secondJob + Dispatchers.IO))
                fail("A second active instance acquired the same preference file")
            } catch (_: IllegalStateException) { }
            assertEquals("charcoal", first.read())
        } finally { firstJob.cancelAndJoin(); secondJob.cancelAndJoin() }
    }

    @Test fun actual_preferences_file_can_be_saved_closed_and_reopened() = runBlocking {
        val file = File(temporary.newFolder("appearance"), "appearance.preferences_pb")
        var job = SupervisorJob()
        try {
            val first = DataStoreAppearanceStorage.create(file, CoroutineScope(job + Dispatchers.IO))
            assertNull(first.read())
            first.write("charcoal")
            assertEquals("charcoal", first.read())
            job.cancelAndJoin()
            assertTrue(file.isFile)
            assertTrue(file.length() > 0)
            job = SupervisorJob()
            val reopened = DataStoreAppearanceStorage.create(file, CoroutineScope(job + Dispatchers.IO))
            assertEquals("charcoal", reopened.read())
            reopened.write("pure_white")
            assertEquals("pure_white", reopened.read())
        } finally { job.cancelAndJoin() }
    }

    @Test fun corrupt_preferences_are_not_replaced_by_read_or_failed_save() = runBlocking {
        val file = File(temporary.newFolder("corrupt"), "appearance.preferences_pb")
        val original = byteArrayOf(0x0a, 0x7f, 0x01)
        file.writeBytes(original)
        val job = SupervisorJob()
        try {
            val storage = DataStoreAppearanceStorage.create(file, CoroutineScope(job + Dispatchers.IO))
            assertTrue(runCatching { storage.read() }.exceptionOrNull() is CorruptionException)
            assertArrayEquals(original, file.readBytes())
            assertTrue(runCatching { storage.write("pure_white") }.exceptionOrNull() is CorruptionException)
            assertArrayEquals(original, file.readBytes())
        } finally { job.cancelAndJoin() }
    }

}
