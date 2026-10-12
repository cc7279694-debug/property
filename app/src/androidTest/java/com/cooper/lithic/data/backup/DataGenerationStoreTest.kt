package com.cooper.lithic.data.backup

import android.content.Context
import android.system.Os
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.data.settings.PausedReturnDispatcher
import java.io.File
import java.io.IOException
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataGenerationStoreTest {
    private lateinit var context: Context
    private lateinit var root: File
    private val handles = mutableListOf<ManagedGeneration>()
    private val extraRoots = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        root = File(context.noBackupFilesDir, "generation-test-${UUID.randomUUID()}")
        check(root.mkdirs())
    }

    @After
    fun tearDown() = runBlocking {
        handles.asReversed().forEach { it.close() }
        check(root.deleteRecursively())
        extraRoots.forEach { check(it.deleteRecursively()) }
    }

    @Test
    fun generation_is_self_contained() = runBlocking {
        val generation = open(DataGenerationStore(context, root))
        assertEquals(generation.id, UUID.fromString(generation.id).toString())
        assertEquals(File(root, "generations/${generation.id}").canonicalFile, generation.directory)
        assertTrue(File(generation.directory, "data.db").isFile)
        assertTrue(File(generation.directory, "media").isDirectory)
        assertEquals(File(generation.directory, "settings.preferences_pb"), generation.settings.file)
        assertTrue(generation.settings.file.isFile)
        assertEquals(2, generation.database.categoryDao().list().size)
        val ready = JSONObject(File(generation.directory, "ready.json").readText())
        assertEquals(generation.id, ready.getString("generationId"))
        val pointer = JSONObject(File(root, "active-generation").readText())
        assertEquals(generation.id, pointer.getString("activeId"))
        assertEquals("STABLE", pointer.getString("stage"))
        assertTrue(pointer.isNull("previousId"))
    }

    @Test
    fun restart_opens_the_same_generation_and_record_zone() = runBlocking {
        val first = open(DataGenerationStore(context, root), "Asia/Shanghai")
        val identity = first.id
        assertTrue(File(root, "active-generation").isFile)
        val pointerBytes = File(root, "active-generation").readBytes()
        first.close()
        val restarted = open(DataGenerationStore(context, root), "America/New_York")
        assertEquals(identity, restarted.id)
        assertEquals(ZoneId.of("Asia/Shanghai"), restarted.settings.recordZone())
        assertArrayEquals(pointerBytes, File(root, "active-generation").readBytes())
        assertEquals(1, File(root, "generations").listFiles()!!.size)
    }

    @Test
    fun repeated_open_on_one_owner_returns_the_same_handle() = runBlocking {
        val owner = DataGenerationStore(context, root)
        val first = open(owner)
        assertSame(first, open(owner, "Europe/Paris"))
    }

    @Test
    fun same_canonical_root_cannot_have_two_active_owners() = runBlocking {
        open(DataGenerationStore(context, root))
        expectGenerationError { open(DataGenerationStore(context, File(root, "."))) }
    }

    @Test
    fun old_generation_directories_are_retained_on_reopen() = runBlocking {
        val first = open(DataGenerationStore(context, root))
        first.close()
        val old = File(root, "generations/${UUID.randomUUID()}")
        check(old.mkdirs())
        val sentinel = File(old, "untouched.txt").apply { writeText("historical synthetic data") }
        val restarted = open(DataGenerationStore(context, root))
        assertEquals(first.id, restarted.id)
        assertEquals("historical synthetic data", sentinel.readText())
    }

    @Test
    fun failed_initialization_never_activates_a_partial_generation() = runBlocking {
        val owner = DataGenerationStore(context, root, initialize = { generation ->
            assertTrue(File(generation.directory, "data.db").isFile)
            assertTrue(generation.settings.file.isFile)
            assertEquals(2, generation.database.categoryDao().list().size)
            throw IOException("Synthetic failure before ready and activation")
        })
        expectGenerationError { open(owner) }
        assertTrue(!File(root, "active-generation").exists())
        val incomplete = File(root, "generations").listFiles()!!.single()
        assertTrue(File(incomplete, "data.db").isFile)
        assertTrue(File(incomplete, "settings.preferences_pb").isFile)
        assertTrue(!File(incomplete, "ready.json").exists())
        expectGenerationError { open(DataGenerationStore(context, root)) }
        assertEquals(1, File(root, "generations").listFiles()!!.size)
    }

    @Test
    fun missing_pointer_with_existing_generation_does_not_reset() = runBlocking {
        val generation = open(DataGenerationStore(context, root))
        generation.close()
        assertTrue(File(root, "active-generation").isFile)
        check(File(root, "active-generation").delete())
        expectGenerationError { open(DataGenerationStore(context, root)) }
        assertTrue(File(generation.directory, "data.db").isFile)
        assertEquals(1, File(root, "generations").listFiles()!!.size)
        assertTrue(!File(root, "active-generation").exists())
    }

    @Test
    fun corrupt_pointer_remains_unchanged_and_does_not_reset() = runBlocking {
        val generation = open(DataGenerationStore(context, root))
        generation.close()
        val corrupt = "broken pointer".toByteArray()
        File(root, "active-generation").writeBytes(corrupt)
        expectGenerationError { open(DataGenerationStore(context, root)) }
        assertArrayEquals(corrupt, File(root, "active-generation").readBytes())
        assertEquals(1, File(root, "generations").listFiles()!!.size)
        assertTrue(File(generation.directory, "data.db").isFile)
    }

    @Test
    fun pointer_new_file_evidence_is_not_treated_as_fresh_install() = runBlocking {
        val evidence = File(root, "active-generation.new").apply { writeText("interrupted write") }
        expectGenerationError { open(DataGenerationStore(context, root)) }
        assertEquals("interrupted write", evidence.readText())
        assertTrue(!File(root, "active-generation").exists())
        assertTrue(!File(root, "generations").exists())
    }

    @Test
    fun corrupt_pointer_backup_evidence_is_preserved() = runBlocking {
        val evidence = File(root, "active-generation.bak").apply { writeText("broken backup") }
        expectGenerationError { open(DataGenerationStore(context, root)) }
        assertEquals("broken backup", evidence.readText())
        assertTrue(!File(root, "active-generation").exists())
    }

    @Test
    fun atomic_file_backup_can_recover_a_valid_committed_pointer() = runBlocking {
        val generation = open(DataGenerationStore(context, root))
        generation.close()
        val active = File(root, "active-generation")
        assertTrue(active.isFile)
        check(active.renameTo(File(root, "active-generation.bak")))
        val restarted = open(DataGenerationStore(context, root))
        assertEquals(generation.id, restarted.id)
        assertTrue(File(generation.directory, "data.db").isFile)
    }

    @Test
    fun missing_generation_directory_is_not_recreated() = runBlocking {
        val generation = open(DataGenerationStore(context, root))
        generation.close()
        val retained = File(root, "retained-directory")
        check(generation.directory.renameTo(retained))
        expectGenerationError { open(DataGenerationStore(context, root)) }
        assertTrue(!generation.directory.exists())
        assertTrue(File(retained, "data.db").isFile)
    }

    @Test
    fun missing_database_is_not_recreated() = runBlocking {
        assertMissingFileFailsWithoutReplacement("data.db")
    }

    @Test
    fun missing_settings_are_not_recreated() = runBlocking {
        assertMissingFileFailsWithoutReplacement("settings.preferences_pb")
    }

    @Test
    fun missing_ready_marker_is_not_recreated() = runBlocking {
        assertMissingFileFailsWithoutReplacement("ready.json")
    }

    @Test
    fun corrupt_database_is_preserved_without_empty_replacement() = runBlocking {
        val generation = open(DataGenerationStore(context, root))
        generation.close()
        val database = File(generation.directory, "data.db")
        val corrupt = "not a SQLite database".toByteArray()
        database.writeBytes(corrupt)
        expectGenerationError { open(DataGenerationStore(context, root)) }
        assertArrayEquals(corrupt, database.readBytes())
        assertEquals(1, File(root, "generations").listFiles()!!.size)
    }

    @Test
    fun corrupt_settings_are_preserved_without_default_replacement() = runBlocking {
        val generation = open(DataGenerationStore(context, root))
        generation.close()
        val corrupt = byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte())
        generation.settings.file.writeBytes(corrupt)
        expectGenerationError { open(DataGenerationStore(context, root)) }
        assertArrayEquals(corrupt, generation.settings.file.readBytes())
    }

    @Test
    fun ready_marker_identity_must_match_the_active_generation() = runBlocking {
        val generation = open(DataGenerationStore(context, root))
        generation.close()
        val ready = File(generation.directory, "ready.json")
        ready.writeText(JSONObject().put("formatVersion", 1).put("generationId", UUID.randomUUID()).toString())
        val bytes = ready.readBytes()
        expectGenerationError { open(DataGenerationStore(context, root)) }
        assertArrayEquals(bytes, ready.readBytes())
    }

    @Test
    fun unsupported_pointer_format_and_stage_fail_explicitly() = runBlocking {
        val generation = open(DataGenerationStore(context, root))
        generation.close()
        val pointer = File(root, "active-generation")
        assertTrue(pointer.isFile)
        val original = pointer.readText()
        pointer.writeText(JSONObject(original).put("formatVersion", 999).toString())
        expectGenerationError { open(DataGenerationStore(context, root)) }
        pointer.writeText(JSONObject(original).put("stage", "PENDING_VALIDATION").toString())
        expectGenerationError { open(DataGenerationStore(context, root)) }
        assertEquals(1, File(root, "generations").listFiles()!!.size)
    }

    @Test
    fun pointer_id_cannot_escape_the_generations_directory() = runBlocking {
        val pointer = File(root, "active-generation")
        pointer.writeText(JSONObject().put("formatVersion", 1).put("activeId", "../../elsewhere")
            .put("previousId", JSONObject.NULL).put("stage", "STABLE").toString())
        val bytes = pointer.readBytes()
        expectGenerationError { open(DataGenerationStore(context, root)) }
        assertArrayEquals(bytes, pointer.readBytes())
        assertTrue(!File(root, "generations").exists())
    }

    @Test
    fun symlinked_generations_cannot_escape_the_private_root() = runBlocking {
        val outside = File(context.noBackupFilesDir, "outside-test-${UUID.randomUUID()}")
        check(outside.mkdirs())
        extraRoots += outside
        File(outside, "sentinel").writeText("preserved")
        Os.symlink(outside.path, File(root, "generations").path)
        expectGenerationError { open(DataGenerationStore(context, root)) }
        assertEquals(listOf("sentinel"), outside.list()!!.toList())
        assertEquals("preserved", File(outside, "sentinel").readText())
    }

    @Test
    fun database_wal_and_shm_stay_in_the_same_generation() = runBlocking {
        val generation = open(DataGenerationStore(context, root))
        generation.database.categoryDao().list()
        val databasePath = requireNotNull(generation.database.openHelper.writableDatabase.path)
        assertEquals(File(generation.directory, "data.db").canonicalPath, File(databasePath).canonicalPath)
        assertTrue(File(generation.directory, "data.db-wal").isFile)
        assertTrue(File(generation.directory, "data.db-shm").isFile)
        assertTrue(!File(root, "data.db-wal").exists())
        assertTrue(!File(root, "data.db-shm").exists())
    }

    @Test
    fun cancellation_before_io_return_releases_the_generation_owner() = runBlocking {
        val dispatcher = PausedReturnDispatcher()
        val request = async(dispatcher) { DataGenerationStore(context, root).open(ZoneId.of("UTC")) }
        dispatcher.awaitTask().run()
        val returnTask = dispatcher.awaitTask()
        val pointer = File(root, "active-generation")
        assertTrue(pointer.isFile)
        val pointerBytes = pointer.readBytes()
        dispatcher.finishCancelled(request, returnTask)
        request.join()
        var reopenFailure: Exception? = null
        try {
            open(DataGenerationStore(context, root), "Europe/Paris")
        } catch (failure: Exception) {
            reopenFailure = failure
        }
        assertNull("Cancelled resource acquisition must close stores and release its root", reopenFailure)
        assertArrayEquals(pointerBytes, pointer.readBytes())
    }

    @Test
    fun cancelled_repeat_open_preserves_the_already_active_handle() = runBlocking {
        val owner = DataGenerationStore(context, root)
        val active = open(owner)
        val dispatcher = PausedReturnDispatcher()
        val request = async(dispatcher) { owner.open(ZoneId.of("Europe/Paris")) }
        dispatcher.awaitTask().run()
        if (request.isCompleted) {
            request.cancel()
        } else {
            dispatcher.finishCancelled(request, dispatcher.awaitTask())
        }
        request.join()
        assertSame(active, open(owner))
        assertEquals(ZoneId.of("UTC"), active.settings.recordZone())
        assertEquals(2, active.database.categoryDao().list().size)
    }

    private suspend fun assertMissingFileFailsWithoutReplacement(name: String) {
        val generation = open(DataGenerationStore(context, root))
        generation.close()
        val target = File(generation.directory, name)
        assertTrue(target.isFile)
        check(target.delete())
        expectGenerationError { open(DataGenerationStore(context, root)) }
        assertTrue(!target.exists())
        assertEquals(1, File(root, "generations").listFiles()!!.size)
    }

    private suspend fun open(owner: DataGenerationStore, zone: String = "UTC"): ManagedGeneration =
        owner.open(ZoneId.of(zone)).also { handles += it }

    private suspend fun expectGenerationError(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected a generation storage error")
        } catch (_: GenerationStorageException) {
            // A data error must be explicit, rather than a new empty generation.
        }
    }
}
