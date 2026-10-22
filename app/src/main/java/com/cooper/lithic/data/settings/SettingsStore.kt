package com.cooper.lithic.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import java.io.File
import java.io.IOException
import java.time.ZoneId
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SettingsStorageException(message: String, cause: Throwable? = null) : IOException(message, cause)

class SettingsStore private constructor(
    val file: File,
    private val coordinator: LocalWriteCoordinator,
    private val scopeJob: Job,
    private val dataStore: DataStore<Preferences>,
) {
    private val lifecycle = Mutex()
    private var closed = false

    val reminderChanges: Flow<Unit> = dataStore.data.map(::readReminders).distinctUntilChanged().map { Unit }

    suspend fun reminderPreferences(): ReminderPreferences = coordinator.withSnapshot {
        reminderPreferencesWithinSnapshot(coordinator)
    }

    internal suspend fun reminderPreferencesWithinSnapshot(expectedCoordinator: LocalWriteCoordinator): ReminderPreferences {
        if (coordinator !== expectedCoordinator) throw SettingsStorageException("Reminder settings use a different coordinator")
        if (closed) throw SettingsStorageException("Settings store is closed")
        return readReminders(dataStore.data.first())
    }

    suspend fun updateReminderPreferences(renewalEnabled: Boolean? = null, milestoneEnabled: Boolean? = null): ReminderPreferences =
        coordinator.withWrite {
            if (closed) throw SettingsStorageException("Settings store is closed")
            readReminders(dataStore.edit {
                if (renewalEnabled != null) it[RENEWAL_ENABLED] = renewalEnabled
                if (milestoneEnabled != null) it[MILESTONE_ENABLED] = milestoneEnabled
            })
        }

    /** Only a private, unpublished restore candidate may supply an import watermark. */
    internal suspend fun restoreRemindersWithinWrite(expectedCoordinator: LocalWriteCoordinator, value: ReminderPreferences) {
        if (coordinator !== expectedCoordinator || closed) throw SettingsStorageException("Restore settings owner differs or is closed")
        val written = dataStore.edit {
            it[RENEWAL_ENABLED] = value.renewalEnabled
            it[MILESTONE_ENABLED] = value.milestoneEnabled
            if (value.importCutoff == null) it.remove(IMPORT_CUTOFF) else it[IMPORT_CUTOFF] = value.importCutoff.toString()
        }
        check(readReminders(written) == value) { "Restored reminder settings did not persist" }
    }

    /** Immutable business zone, validated from the persisted preferences during open. */
    suspend fun recordZone(): ZoneId = coordinator.withSnapshot {
        backupRecordZoneWithinSnapshot(coordinator)
    }

    /** Backup already owns this non-reentrant coordinator; never acquire it for a second time. */
    internal suspend fun backupRecordZoneWithinSnapshot(expectedCoordinator: LocalWriteCoordinator): ZoneId {
        if (coordinator !== expectedCoordinator) throw SettingsStorageException("Backup uses a different settings coordinator")
        if (closed) throw SettingsStorageException("Settings store is closed")
        return readZone(dataStore.data.first())
    }

    suspend fun close() = coordinator.withWrite { closeWithinWrite() }

    /** Generation shutdown already owns the same non-reentrant write coordinator. */
    internal suspend fun closeWithinWrite() = lifecycle.withLock {
        if (!closed) {
            closed = true
            withContext(NonCancellable) {
                scopeJob.cancelAndJoin()
                release(file.path)
            }
        }
    }

    companion object {
        private val RECORD_ZONE = stringPreferencesKey("record_zone_id")
        private val RENEWAL_ENABLED = booleanPreferencesKey("renewal_reminders_enabled")
        private val MILESTONE_ENABLED = booleanPreferencesKey("milestone_reminders_enabled")
        private val IMPORT_CUTOFF = stringPreferencesKey("reminder_import_cutoff")
        private val owners = mutableSetOf<String>()

        suspend fun open(
            file: File,
            coordinator: LocalWriteCoordinator,
            initialZone: ZoneId? = null,
            requireExisting: Boolean = false,
        ): SettingsStore = open(file, coordinator, initialZone, requireExisting, beforeLease = {})

        /** Observation seam for deterministic ownership scheduling; storage uses the same path. */
        internal suspend fun open(
            file: File,
            coordinator: LocalWriteCoordinator,
            initialZone: ZoneId?,
            requireExisting: Boolean,
            beforeLease: suspend () -> Unit,
        ): SettingsStore {
            var acquired: SettingsStore? = null
            try {
                return withContext(Dispatchers.IO) {
                    val canonical = file.canonicalFile
                    if (!canonical.name.endsWith(".preferences_pb")) {
                        throw SettingsStorageException("Settings file must use Preferences DataStore format")
                    }
                    beforeLease()
                    val identity = canonical.path
                    synchronized(owners) {
                        if (!owners.add(identity)) {
                            throw SettingsStorageException("Settings file already has an active owner")
                        }
                    }
                    val job = SupervisorJob()
                    try {
                        // Existence is observed only while this operation owns the actual file.
                        val existed = canonical.exists()
                        if (requireExisting && !canonical.isFile) {
                            throw SettingsStorageException("Existing settings file is missing")
                        }
                        if (existed && !canonical.isFile) throw SettingsStorageException("Settings path is not a file")
                        if (!existed && initialZone == null) {
                            throw SettingsStorageException("A new settings file requires an explicit record zone")
                        }
                        val parent = canonical.parentFile ?: throw SettingsStorageException("Settings parent is missing")
                        if (!parent.isDirectory && !parent.mkdirs()) {
                            throw SettingsStorageException("Cannot create settings directory")
                        }
                        val temporary = File(parent, "${canonical.name}.tmp")
                        if (temporary.canonicalFile != temporary.absoluteFile) {
                            throw SettingsStorageException("Settings temporary path leaves its directory")
                        }
                        // No corruption handler: damaged preferences must fail without replacement.
                        val dataStore = PreferenceDataStoreFactory.create(
                            scope = CoroutineScope(job + Dispatchers.IO),
                            produceFile = { canonical },
                        )
                        coordinator.withWrite {
                            val current = dataStore.data.first()
                            if (existed) {
                                readZone(current)
                                readReminders(current)
                            } else {
                                val written = dataStore.edit { it[RECORD_ZONE] = requireNotNull(initialZone).id }
                                if (!canonical.isFile || canonical.length() == 0L) {
                                    throw SettingsStorageException("Settings initialization did not persist")
                                }
                                readZone(written)
                            }
                        }
                        SettingsStore(canonical, coordinator, job, dataStore).also { acquired = it }
                    } catch (failure: Throwable) {
                        withContext(NonCancellable) {
                            job.cancelAndJoin()
                            release(identity)
                        }
                        when (failure) {
                            is CancellationException -> throw failure
                            is SettingsStorageException -> throw failure
                            is Exception -> throw SettingsStorageException("Cannot read or initialize settings", failure)
                            else -> throw failure
                        }
                    }
                }
            } catch (failure: Throwable) {
                // withContext may discard a successfully acquired resource on prompt cancellation.
                acquired?.let { store ->
                    withContext(NonCancellable) {
                        try {
                            store.close()
                        } catch (closingFailure: Throwable) {
                            failure.addSuppressed(closingFailure)
                        }
                    }
                }
                throw failure
            }
        }

        private fun readZone(preferences: Preferences): ZoneId {
            val value = preferences[RECORD_ZONE]
                ?: throw SettingsStorageException("Persisted record zone is missing")
            try {
                return ZoneId.of(value)
            } catch (failure: java.time.DateTimeException) {
                throw SettingsStorageException("Persisted record zone is invalid", failure)
            }
        }

        private fun readReminders(preferences: Preferences): ReminderPreferences {
            try {
                val cutoff = preferences[IMPORT_CUTOFF]?.let { raw ->
                    LocalDate.parse(raw).also { require(it.toString() == raw) }
                }
                return ReminderPreferences(preferences[RENEWAL_ENABLED] ?: true, preferences[MILESTONE_ENABLED] ?: true, cutoff)
            } catch (failure: Exception) {
                throw SettingsStorageException("Persisted reminder preferences are invalid", failure)
            }
        }

        private fun release(identity: String) {
            synchronized(owners) { owners.remove(identity) }
        }
    }
}
