package com.cooper.lithic.data.settings

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import java.io.File
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsStoreTest {
    private lateinit var root: File
    private val opened = mutableListOf<SettingsStore>()
    private val coordinator = LocalWriteCoordinator()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        root = File(context.noBackupFilesDir, "settings-test-${UUID.randomUUID()}")
        check(root.mkdirs())
    }

    @After
    fun tearDown() = runBlocking {
        opened.asReversed().forEach { it.close() }
        check(root.deleteRecursively())
    }

    @Test
    fun zone_is_persisted() = runBlocking {
        val file = File(root, "settings.preferences_pb")
        val first = open(file, "Asia/Shanghai")
        assertEquals(ZoneId.of("Asia/Shanghai"), first.recordZone())
        first.close()
        val restarted = open(file, "America/New_York", requireExisting = true)
        assertEquals(ZoneId.of("Asia/Shanghai"), restarted.recordZone())
    }

    @Test
    fun initialization_is_durable_before_open_returns() = runBlocking {
        val file = File(root, "settings.preferences_pb")
        open(file, "UTC")
        assertTrue(file.isFile)
        assertTrue(file.length() > 0L)
    }

    @Test
    fun canonical_file_cannot_have_two_active_owners() = runBlocking {
        val file = File(root, "settings.preferences_pb")
        open(file, "UTC")
        val alias = File(root, "./settings.preferences_pb")
        expectSettingsError { open(alias, "Europe/Paris", requireExisting = true) }
    }

    @Test
    fun closing_releases_the_owner_only_after_datastore_stops() = runBlocking {
        val file = File(root, "settings.preferences_pb")
        repeat(4) {
            val store = open(file, if (it == 0) "UTC" else "Europe/Paris", requireExisting = it > 0)
            assertEquals(ZoneId.of("UTC"), store.recordZone())
            store.close()
        }
    }

    @Test
    fun existing_missing_file_is_not_created() = runBlocking {
        val file = File(root, "missing.preferences_pb")
        expectSettingsError { open(file, "UTC", requireExisting = true) }
        assertTrue(!file.exists())
    }

    @Test
    fun corrupt_settings_remain_unchanged() = runBlocking {
        val file = File(root, "settings.preferences_pb")
        val corrupt = byteArrayOf(0xff.toByte(), 0xff.toByte(), 0xff.toByte())
        file.writeBytes(corrupt)
        expectSettingsError { open(file, "UTC", requireExisting = true) }
        assertArrayEquals(corrupt, file.readBytes())
        expectSettingsError { open(file, "Europe/Paris", requireExisting = true) }
        assertArrayEquals(corrupt, file.readBytes())
    }

    @Test
    fun existing_empty_preferences_are_rejected_without_default_replacement() = runBlocking {
        val file = File(root, "settings.preferences_pb")
        file.writeBytes(byteArrayOf())
        expectSettingsError { open(file, "UTC", requireExisting = true) }
        assertEquals(0L, file.length())
    }

    @Test
    fun new_settings_require_an_explicit_record_zone() = runBlocking {
        val file = File(root, "settings.preferences_pb")
        expectSettingsError {
            opened += SettingsStore.open(file, coordinator)
        }
        assertTrue(!file.exists())
    }

    @Test
    fun valid_nonempty_preferences_without_record_zone_are_not_replaced() = runBlocking {
        val file = File(root, "settings.preferences_pb")
        writePreferencesFixture(file, "theme", "dark")
        val original = file.readBytes()
        assertTrue(original.isNotEmpty())
        expectSettingsError { open(file, "UTC", requireExisting = true) }
        assertArrayEquals(original, file.readBytes())
    }

    @Test
    fun invalid_persisted_record_zone_is_not_replaced() = runBlocking {
        val file = File(root, "settings.preferences_pb")
        writePreferencesFixture(file, "record_zone_id", "not/a-zone")
        val original = file.readBytes()
        expectSettingsError { open(file, "UTC", requireExisting = true) }
        assertArrayEquals(original, file.readBytes())
    }

    @Test
    fun file_existence_is_observed_only_after_the_exclusive_lease() = runBlocking {
        val file = File(root, "settings.preferences_pb")
        val beforeLease = CompletableDeferred<Unit>()
        val resumeLease = CompletableDeferred<Unit>()
        val delayed = async(Dispatchers.IO) {
            SettingsStore.open(file, coordinator, ZoneId.of("America/New_York"), false,
                beforeLease = {
                    beforeLease.complete(Unit)
                    resumeLease.await()
                })
        }
        beforeLease.await()
        val first = open(file, "UTC")
        first.close()
        resumeLease.complete(Unit)
        val second = delayed.await().also { opened += it }
        assertEquals(ZoneId.of("UTC"), second.recordZone())
    }

    @Test
    fun cancellation_before_io_return_releases_the_settings_owner() = runBlocking {
        val file = File(root, "settings.preferences_pb")
        val dispatcher = PausedReturnDispatcher()
        val request = async(dispatcher) { SettingsStore.open(file, coordinator, ZoneId.of("UTC")) }
        dispatcher.awaitTask().run()
        val returnTask = dispatcher.awaitTask()
        assertTrue(file.isFile)
        dispatcher.finishCancelled(request, returnTask)
        request.join()
        var reopenFailure: Exception? = null
        try {
            open(file, "Europe/Paris", requireExisting = true)
        } catch (failure: Exception) {
            reopenFailure = failure
        }
        assertNull("Cancelled resource acquisition must release its owner", reopenFailure)
    }

    @Test
    fun closed_settings_handle_is_rejected() = runBlocking {
        val store = open(File(root, "settings.preferences_pb"), "UTC")
        store.close()
        expectSettingsError { store.recordZone() }
    }

    private suspend fun open(file: File, zone: String, requireExisting: Boolean = false): SettingsStore =
        SettingsStore.open(file, coordinator, ZoneId.of(zone), requireExisting).also { opened += it }

    private suspend fun writePreferencesFixture(file: File, key: String, value: String) {
        val job = SupervisorJob()
        val fixture = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(job + Dispatchers.IO),
            produceFile = { file },
        )
        try {
            fixture.edit { it[stringPreferencesKey(key)] = value }
        } finally {
            job.cancelAndJoin()
        }
    }

    private suspend fun expectSettingsError(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected a settings storage error")
        } catch (_: SettingsStorageException) {
            // A data error must be explicit, rather than default replacement.
        }
    }
}
