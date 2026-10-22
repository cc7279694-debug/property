package com.cooper.lithic.feature.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import com.cooper.lithic.data.settings.ReminderPreferences
import com.cooper.lithic.data.settings.SettingsStore
import java.io.File
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real DataStore within a fresh M20 root, without opening any business ledger. */
@RunWith(AndroidJUnit4::class)
class M20NotificationSettingsViewModelTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var root: File
    private val opened = mutableListOf<SettingsStore>()
    private val models = mutableListOf<ViewModelStore>()

    @Before fun before() {
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m20-notification-vm-${UUID.randomUUID()}").canonicalFile
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.mkdir())
    }

    @After fun after(): Unit = runBlocking {
        withContext(Dispatchers.Main) { models.forEach { it.clear() } }
        opened.asReversed().forEach { it.close() }
        if (::root.isInitialized) {
            check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.name.startsWith("m20-notification-vm-"))
            val files = checkNotNull(root.listFiles())
            check(files.all { it.isFile && it.canonicalFile == it.absoluteFile &&
                it.name in setOf("settings.preferences_pb", "settings.preferences_pb.tmp") })
            files.forEach { check(it.delete()) }
            check(root.delete())
        }
    }

    @Test fun both_default_on_switches_persist_independently_and_survive_owner_reopen(): Unit = runBlocking {
        var scheduled = 0
        val first = open()
        val model = model(first) { scheduled++ }
        val initial = await(model) { !it.loading }
        assertEquals(ReminderPreferences(), initial.preferences)
        assertEquals(0, scheduled)
        withContext(Dispatchers.Main) { model.setRenewalEnabled(false) }
        await(model) { !it.saving && it.preferences?.renewalEnabled == false }
        assertTrue(model.state.value.preferences!!.milestoneEnabled)
        withContext(Dispatchers.Main) { model.setMilestoneEnabled(false) }
        await(model) { !it.saving && it.preferences?.milestoneEnabled == false }
        assertEquals(2, scheduled)
        assertEquals(ZoneId.of("Asia/Shanghai"), first.recordZone())
        withContext(Dispatchers.Main) { models.last().clear() }
        first.close()

        val reopened = open(requireExisting = true)
        val restarted = model(reopened) { scheduled++ }
        val readBack = await(restarted) { !it.loading }
        assertEquals(ReminderPreferences(renewalEnabled = false, milestoneEnabled = false), readBack.preferences)
        assertEquals(2, scheduled)
        assertEquals(ZoneId.of("Asia/Shanghai"), reopened.recordZone())
    }

    @Test fun stale_closed_generation_cannot_read_default_preferences_write_or_trigger_schedule(): Unit = runBlocking {
        var scheduled = 0
        val settings = open()
        settings.updateReminderPreferences(renewalEnabled = false)
        settings.close()
        val originalBytes = File(root, "settings.preferences_pb").readBytes()
        val model = model(settings) { scheduled++ }
        val failed = await(model) { !it.loading && it.error != null }
        assertNull(failed.preferences)
        withContext(Dispatchers.Main) { model.setRenewalEnabled(true); model.setMilestoneEnabled(false) }
        assertEquals(0, scheduled)
        assertArrayEquals(originalBytes, File(root, "settings.preferences_pb").readBytes())
    }

    private suspend fun open(requireExisting: Boolean = false) = SettingsStore.open(
        File(root, "settings.preferences_pb"), LocalWriteCoordinator(),
        if (requireExisting) null else ZoneId.of("Asia/Shanghai"), requireExisting).also { opened += it }

    private suspend fun model(settings: SettingsStore, changed: () -> Unit): NotificationSettingsViewModel =
        withContext(Dispatchers.Main) {
            NotificationSettingsViewModel(settings, changed).also { model ->
                ViewModelStore().also { store -> store.put("m20-notifications", model); models += store }
            }
        }

    private suspend fun await(model: NotificationSettingsViewModel, predicate: (NotificationSettingsUiState) -> Boolean): NotificationSettingsUiState {
        val result = withTimeout(10_000) { model.state.first(predicate) }
        withContext(Dispatchers.Main) { /* Observe callbacks from the same UI turn before asserting. */ }
        return result
    }
}
