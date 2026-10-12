package com.cooper.lithic.data.backup

import android.content.Context
import android.util.AtomicFile
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import com.cooper.lithic.data.settings.SettingsStore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener

class GenerationStorageException(message: String, cause: Throwable? = null) : IOException(message, cause)

class ManagedGeneration internal constructor(
    val id: String,
    val directory: File,
    val database: LithicDatabase,
    val settings: SettingsStore,
    val coordinator: LocalWriteCoordinator,
    private val closeAction: suspend (ManagedGeneration) -> Unit,
) {
    suspend fun close() = closeAction(this)
}

/** Initial generation ownership only. Backup import, switching and rollback belong to M12. */
class DataGenerationStore(
    private val context: Context,
    private val root: File = context.noBackupFilesDir,
    private val coordinator: LocalWriteCoordinator = LocalWriteCoordinator(),
    private val initialize: suspend (ManagedGeneration) -> Unit = {},
) {
    private val lifecycle = Mutex()
    private var active: ManagedGeneration? = null
    private var leasedRoot: String? = null

    suspend fun open(deviceZone: ZoneId = ZoneId.systemDefault()): ManagedGeneration = lifecycle.withLock {
        // Existing active resources are owned by the store and are never closed by a cancelled repeat call.
        active?.let { return@withLock it }
        var acquired: ManagedGeneration? = null
        try {
            val generation = withContext(Dispatchers.IO) {
                val storageRoot = validateRoot()
                acquireRoot(storageRoot.path)
                leasedRoot = storageRoot.path
                try {
                    val pointer = safeChild(storageRoot, "active-generation")
                    val backup = safeChild(storageRoot, "active-generation.bak")
                    val pending = safeChild(storageRoot, "active-generation.new")
                    val generations = safeChild(storageRoot, "generations")
                    if (generations.exists() && !generations.isDirectory) {
                        throw GenerationStorageException("Generations path is not a directory")
                    }
                    val opened = if (pointer.exists() || backup.exists() || pending.exists()) {
                        openExisting(pointer, backup, generations)
                    } else {
                        if (generations.exists() && requireNotNull(generations.listFiles()) {
                                "Cannot read generations directory"
                            }.isNotEmpty()) {
                            throw GenerationStorageException("Existing generations have no active pointer")
                        }
                        createInitial(pointer, generations, deviceZone)
                    }
                    acquired = opened
                    opened
                } catch (failure: Throwable) {
                    releaseRoot()
                    rethrowStorageFailure(failure)
                }
            }
            // Keep the lifecycle lock across the IO return, publishing only after successful delivery.
            active = generation
            generation
        } catch (failure: Throwable) {
            acquired?.let { generation ->
                withContext(NonCancellable + Dispatchers.IO) {
                    disposeAfterFailure(generation.database, generation.settings, failure)
                    releaseRoot()
                }
            }
            rethrowStorageFailure(failure)
        }
    }

    private fun validateRoot(): File {
        val privateRoot = context.noBackupFilesDir.canonicalFile
        val selected = root.canonicalFile
        if (selected != privateRoot && !selected.path.startsWith(privateRoot.path + File.separator)) {
            throw GenerationStorageException("Generation root must remain inside noBackupFilesDir")
        }
        if (!selected.isDirectory && !selected.mkdirs()) {
            throw GenerationStorageException("Cannot create generation root")
        }
        return selected
    }

    private suspend fun createInitial(
        pointer: File,
        generations: File,
        deviceZone: ZoneId,
    ): ManagedGeneration {
        if (!generations.isDirectory && !generations.mkdirs()) {
            throw GenerationStorageException("Cannot create generations directory")
        }
        val id = UUID.randomUUID().toString()
        val directory = safeChild(generations, id)
        if (!directory.mkdir()) throw GenerationStorageException("Cannot create initial generation")
        val paths = generationPaths(directory)
        if (!paths.media.mkdir()) throw GenerationStorageException("Cannot create generation media directory")
        var database: LithicDatabase? = null
        var settings: SettingsStore? = null
        try {
            val openedDatabase = LithicDatabase.open(context, paths.database)
            database = openedDatabase
            coordinator.withWrite {
                openedDatabase.initializeDefaults()
                openedDatabase.requireDefaults()
            }
            val openedSettings = SettingsStore.open(paths.settings, coordinator, deviceZone)
            settings = openedSettings
            val generation = managed(id, directory, openedDatabase, openedSettings)
            // Caller-provided initialization must finish before any readiness or active metadata.
            initialize(generation)
            writeAtomic(paths.ready, JSONObject().put("formatVersion", FORMAT_VERSION).put("generationId", id))
            validateReady(paths.ready, id)
            writeAtomic(pointer, JSONObject().put("formatVersion", FORMAT_VERSION).put("activeId", id)
                .put("previousId", JSONObject.NULL).put("stage", "STABLE"))
            // The generation is complete before the sole persisted activation point above.
            return generation
        } catch (failure: Throwable) {
            disposeAfterFailure(database, settings, failure)
            rethrowStorageFailure(failure)
        }
    }

    private suspend fun openExisting(
        pointer: File,
        backup: File,
        generations: File,
    ): ManagedGeneration {
        // AtomicFile's committed backup wins. Validate before openRead can rename/delete helpers.
        val committed = if (backup.exists()) backup else pointer
        if (!committed.isFile) throw GenerationStorageException("Committed active pointer is missing")
        val metadata = readPointer(committed)
        if (!generations.isDirectory) throw GenerationStorageException("Generations directory is missing")
        val directory = safeChild(generations, metadata.activeId)
        if (!directory.isDirectory) throw GenerationStorageException("Active generation directory is missing")
        val paths = generationPaths(directory)
        if (!paths.database.isFile || paths.database.length() == 0L) {
            throw GenerationStorageException("Existing generation database is missing or empty")
        }
        if (!paths.settings.isFile || !paths.media.isDirectory) {
            throw GenerationStorageException("Existing generation settings or media directory is missing")
        }
        validateReady(paths.ready, metadata.activeId)
        var database: LithicDatabase? = null
        var settings: SettingsStore? = null
        try {
            val openedDatabase = LithicDatabase.open(context, paths.database, requireExisting = true)
            database = openedDatabase
            coordinator.withSnapshot { openedDatabase.requireDefaults() }
            val openedSettings = SettingsStore.open(paths.settings, coordinator, requireExisting = true)
            settings = openedSettings
            if (backup.exists()) {
                val committedBytes = committed.readBytes()
                val recoveredBytes = AtomicFile(pointer).readFully()
                if (!committedBytes.contentEquals(recoveredBytes)) {
                    throw GenerationStorageException("Atomic pointer recovery did not preserve committed metadata")
                }
            }
            return managed(metadata.activeId, directory, openedDatabase, openedSettings)
        } catch (failure: Throwable) {
            disposeAfterFailure(database, settings, failure)
            rethrowStorageFailure(failure)
        }
    }

    private fun managed(id: String, directory: File, database: LithicDatabase, settings: SettingsStore) =
        ManagedGeneration(id, directory, database, settings, coordinator, ::closeGeneration)

    private suspend fun closeGeneration(generation: ManagedGeneration) = withContext(Dispatchers.IO) {
        lifecycle.withLock {
            if (active === generation) {
                withContext(NonCancellable) {
                    try {
                        coordinator.withWrite {
                            try {
                                generation.settings.closeWithinWrite()
                            } finally {
                                generation.database.close()
                            }
                        }
                    } finally {
                        active = null
                        releaseRoot()
                    }
                }
            }
        }
    }

    private suspend fun disposeAfterFailure(
        database: LithicDatabase?,
        settings: SettingsStore?,
        failure: Throwable,
    ) = withContext(NonCancellable) {
        try {
            coordinator.withWrite {
                try {
                    settings?.closeWithinWrite()
                } finally {
                    database?.close()
                }
            }
        } catch (closingFailure: Throwable) {
            failure.addSuppressed(closingFailure)
        }
    }

    private fun generationPaths(directory: File): GenerationPaths {
        val database = safeChild(directory, "data.db")
        listOf("data.db-wal", "data.db-shm", "data.db-journal", "settings.preferences_pb.tmp",
            "ready.json.bak", "ready.json.new").forEach { safeChild(directory, it) }
        return GenerationPaths(database, safeChild(directory, "media"),
            safeChild(directory, "settings.preferences_pb"), safeChild(directory, "ready.json"))
    }

    private fun safeChild(parent: File, name: String): File {
        val child = File(parent, name).absoluteFile
        if (child.canonicalFile != child || child.parentFile != parent) {
            throw GenerationStorageException("Generation path leaves its declared private directory")
        }
        return child
    }

    private fun readPointer(file: File): PointerMetadata {
        val value = readMetadata(file, setOf("formatVersion", "activeId", "previousId", "stage"))
        if (value.get("stage") != "STABLE") {
            throw GenerationStorageException("Generation recovery stage is not supported by M04")
        }
        val id = readId(value.get("activeId"))
        val previous = value.get("previousId")
        if (previous != JSONObject.NULL && readId(previous) == id) {
            throw GenerationStorageException("Previous generation cannot equal active generation")
        }
        return PointerMetadata(id)
    }

    private fun validateReady(file: File, id: String) {
        if (!file.isFile) throw GenerationStorageException("Generation readiness marker is missing")
        val ready = readMetadata(file, setOf("formatVersion", "generationId"))
        if (readId(ready.get("generationId")) != id) {
            throw GenerationStorageException("Readiness marker identifies another generation")
        }
    }

    private fun readMetadata(file: File, fields: Set<String>): JSONObject {
        if (!file.isFile || file.length() !in 1..MAX_METADATA_BYTES) {
            throw GenerationStorageException("Generation metadata is missing or exceeds its size limit")
        }
        try {
            val parser = JSONTokener(file.readText(Charsets.UTF_8))
            val value = parser.nextValue() as? JSONObject
                ?: throw GenerationStorageException("Generation metadata must be a JSON object")
            if (parser.nextClean() != '\u0000') throw GenerationStorageException("Generation metadata has trailing data")
            val keys = value.keys()
            val actual = buildSet { while (keys.hasNext()) add(keys.next()) }
            if (actual != fields || value.get("formatVersion") != FORMAT_VERSION) {
                throw GenerationStorageException("Generation metadata format is unsupported")
            }
            return value
        } catch (failure: org.json.JSONException) {
            throw GenerationStorageException("Generation metadata is corrupt", failure)
        }
    }

    private fun readId(value: Any): String {
        if (value !is String) throw GenerationStorageException("Generation ID is not a string")
        try {
            if (UUID.fromString(value).toString() != value) throw IllegalArgumentException("Noncanonical UUID")
            return value
        } catch (failure: IllegalArgumentException) {
            throw GenerationStorageException("Generation ID is invalid", failure)
        }
    }

    private fun writeAtomic(file: File, value: JSONObject) {
        val bytes = value.toString().toByteArray(Charsets.UTF_8)
        val atomic = AtomicFile(file)
        var output: FileOutputStream? = null
        try {
            output = atomic.startWrite()
            output.write(bytes)
            atomic.finishWrite(output)
            output = null
            if (!file.isFile || !file.readBytes().contentEquals(bytes)) {
                throw GenerationStorageException("Atomic generation metadata was not persisted")
            }
        } catch (failure: Throwable) {
            output?.let { atomic.failWrite(it) }
            throw failure
        }
    }

    private fun releaseRoot() {
        leasedRoot?.let { identity -> synchronized(roots) { roots.remove(identity) } }
        leasedRoot = null
    }

    private fun rethrowStorageFailure(failure: Throwable): Nothing = when (failure) {
        is CancellationException -> throw failure
        is GenerationStorageException -> throw failure
        is Exception -> throw GenerationStorageException("Cannot open or initialize the local generation", failure)
        else -> throw failure
    }

    private data class PointerMetadata(val activeId: String)
    private data class GenerationPaths(val database: File, val media: File, val settings: File, val ready: File)

    companion object {
        private const val FORMAT_VERSION = 1
        private const val MAX_METADATA_BYTES = 16_384L
        private val roots = mutableSetOf<String>()

        private fun acquireRoot(identity: String) {
            synchronized(roots) {
                if (!roots.add(identity)) throw GenerationStorageException("Generation root already has an active owner")
            }
        }
    }
}
