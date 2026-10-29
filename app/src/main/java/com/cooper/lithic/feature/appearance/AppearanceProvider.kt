package com.cooper.lithic.feature.appearance

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cooper.lithic.app.LithicApplication
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import okio.FileSystem
import okio.Path.Companion.toOkioPath

internal object AppearanceProvider {
    private var owner: LithicApplication? = null
    private var controller: AppearanceController? = null

    fun get(application: LithicApplication): AppearanceController =
        AppearanceInitialization.mainOnly(application.processRole) {
            synchronized(this) {
                controller?.let {
                    check(owner === application) { "Appearance preferences belong to another application" }
                    return@synchronized it
                }
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                // Context/filesystem failures are surfaced by the controller's read state rather
                // than crashing Activity construction. Kotlin lazy retries a failed creation.
                val storage = object : AppearanceStorage {
                    private val delegate by lazy {
                        val directory = application.noBackupFilesDir.canonicalFile
                        val file = File(directory, "lithic-appearance.preferences_pb")
                        check(file.canonicalFile.parentFile == directory) { "Appearance file leaves its private directory" }
                        DataStoreAppearanceStorage.create(file, scope)
                    }
                    override suspend fun read(): String? = delegate.read()
                    override suspend fun write(storageId: String) = delegate.write(storageId)
                }
                AppearanceController(scope, storage).also { owner = application; controller = it }
            }
        }
}

internal object DataStoreAppearanceStorage {
    private val themeKey = stringPreferencesKey("theme_id")
    private val activeFiles = mutableMapOf<String, Job>()

    fun create(file: File, scope: CoroutineScope): AppearanceStorage {
        val scopeJob = requireNotNull(scope.coroutineContext[Job]) { "Appearance storage needs an owned scope" }
        check(scopeJob.isActive) { "Appearance storage scope is not active" }
        val canonical = file.canonicalFile
        require(canonical.name.endsWith(".preferences_pb")) { "Appearance file must use Preferences DataStore format" }
        val identity = canonical.path
        synchronized(activeFiles) {
            check(activeFiles[identity]?.isCompleted != false) { "Appearance file already has an active instance" }
            activeFiles[identity] = scopeJob
        }
        scopeJob.invokeOnCompletion { release(identity, scopeJob) }
        val dataStore = try {
            // No corruption handler or fallback write: failed reads preserve existing bytes.
            PreferenceDataStoreFactory.create(
                storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer,
                    producePath = { canonical.toOkioPath() }),
                scope = scope,
            )
        } catch (failure: Exception) {
            release(identity, scopeJob)
            throw failure
        }
        return PreferencesStorage(dataStore)
    }

    private fun release(identity: String, job: Job) = synchronized(activeFiles) {
        if (activeFiles[identity] === job) activeFiles.remove(identity)
        Unit
    }

    private class PreferencesStorage(private val dataStore: DataStore<Preferences>) : AppearanceStorage {
        override suspend fun read(): String? = dataStore.data.first()[themeKey]
        override suspend fun write(storageId: String) {
            requireNotNull(LithicAppearance.fromStorage(storageId)) { "Unknown appearance identity" }
            dataStore.edit { it[themeKey] = storageId }
        }
    }
}
