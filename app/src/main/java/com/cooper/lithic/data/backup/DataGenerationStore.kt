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

    /** Logical read only. Full export still requires separately verified media integration. */
    fun backupSnapshotSource(): BackupSnapshotSource = RoomBackupSnapshotSource(this)
}

/** Initial generation ownership only. Backup import, switching and rollback belong to M12. */
class DataGenerationStore(
    private val context: Context,
    private val root: File = context.noBackupFilesDir,
    private val coordinator: LocalWriteCoordinator = LocalWriteCoordinator(),
    private val initialize: suspend (ManagedGeneration) -> Unit = {},
    private val restoreProbe: suspend (String) -> Unit = {},
) {
    private val lifecycle = Mutex()
    private var active: ManagedGeneration? = null
    private var leasedRoot: String? = null
    private val prepared = mutableMapOf<String,String>()
    var lastRecoveryWarning:String? = null
        private set

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
                    disposeAfterFailure(generation.database, generation.settings, failure, generation.coordinator)
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

    private suspend fun openExisting(pointer:File,backup:File,generations:File):ManagedGeneration {
        val committed=if(backup.exists()) backup else pointer
        if(!committed.isFile) throw GenerationStorageException("Committed active pointer is missing")
        val metadata=readPointer(committed)
        if(!generations.isDirectory) throw GenerationStorageException("Generations directory is missing")
        if(backup.exists()) {
            val bytes=committed.readBytes()
            if(!bytes.contentEquals(AtomicFile(pointer).readFully())) throw GenerationStorageException("Atomic pointer recovery changed committed metadata")
        }
        if(metadata.stage in setOf("STABLE","ROLLED_BACK")) {
            if(metadata.stage=="ROLLED_BACK") lastRecoveryWarning="上次恢复的新数据集未通过校验，当前保留原数据集。"
            val opened=openNamed(generations,metadata.activeId)
            try {
                if(metadata.stage=="STABLE" && metadata.previousId!=null) {
                    cleanPreviousAfterStableOpen(pointer,generations,opened,metadata.previousId)
                }
                return opened
            } catch(failure:Throwable) {
                disposeGeneration(opened,failure)
                throw failure
            }
        }
        val previous=requireNotNull(metadata.previousId)
        var candidate:ManagedGeneration?=null
        try {
            candidate=openNamed(generations,metadata.activeId)
            verifyRestoreProof(candidate)
            writePointer(pointer,metadata.activeId,previous,"STABLE")
            return candidate
        } catch(failure:Throwable) {
            candidate?.let { disposeGeneration(it,failure) }
            // The old id is persisted with the switch; never guess from directory dates.
            val old=openNamed(generations,previous)
            try {
                writePointer(pointer,previous,metadata.activeId,"ROLLED_BACK")
                lastRecoveryWarning="恢复的新数据集未通过首次打开校验，已回到原数据集。"
                return old
            } catch(rollback:Throwable) { disposeGeneration(old,rollback);rollback.addSuppressed(failure);throw rollback }
        }
    }

    /** Later stable startup only. Never rolls current user edits back to an old snapshot. */
    private suspend fun cleanPreviousAfterStableOpen(pointer:File,generations:File,current:ManagedGeneration,previousId:String) {
        val currentSnapshot=RoomBackupSnapshotSource(current).capture()
        RestoredGenerationWriter().verify(current,currentSnapshot)
        // Unknown previous data must remain on disk. Failure to prove cleanup ownership
        // does not make an otherwise verified current generation an empty or failed ledger.
        val previousSnapshot=try {
            val previous=openNamed(generations,previousId)
            try { RoomBackupSnapshotSource(previous).capture() }
            finally { closeChecked(previous) }
        } catch(cancelled:CancellationException) { throw cancelled }
        catch(_:Exception) {
            lastRecoveryWarning="当前数据已打开；上次恢复的旧资源归属无法确认，已保守保留。"
            return
        }
        withContext(NonCancellable) {
            // Detach the old id durably BEFORE deletion. An interrupted cleanup can then
            // leave residue, but cannot make a partially deleted directory the fallback.
            writePointer(pointer,current.id,null,"STABLE")
            if(!RetiredGenerationCleanup.clean(safeChild(generations,previousId),previousId,current.id,previousSnapshot)) {
                lastRecoveryWarning="当前数据已打开；未能安全清理的旧资源已保留。"
            }
        }
    }

    private suspend fun openNamed(generations:File,id:String,requireReady:Boolean=true):ManagedGeneration {
        val directory=safeChild(generations,id)
        if(!directory.isDirectory) throw GenerationStorageException("Generation directory is missing")
        val paths=generationPaths(directory)
        if(!paths.database.isFile || paths.database.length()==0L || !paths.settings.isFile || !paths.media.isDirectory)
            throw GenerationStorageException("Generation resources are incomplete")
        if(requireReady) validateReady(paths.ready,id)
        var database:LithicDatabase?=null
        var settings:SettingsStore?=null
        val gate=LocalWriteCoordinator()
        try {
            val db=LithicDatabase.open(context,paths.database,requireExisting=true);database=db
            gate.withSnapshot { db.requireDefaults() }
            val prefs=SettingsStore.open(paths.settings,gate,requireExisting=true);settings=prefs
            return managed(id,directory,db,prefs,gate)
        } catch(failure:Throwable) {
            disposeAfterFailure(database,settings,failure,gate)
            rethrowStorageFailure(failure)
        }
    }

    /** Prepare a new complete dataset without touching the active pointer or closing the old one. */
    internal suspend fun prepareRestore(zone:ZoneId,populate:suspend(ManagedGeneration)->Unit):PreparedGeneration = lifecycle.withLock {
        check(active!=null && leasedRoot!=null) { "Restore requires the current storage owner" }
        withContext(Dispatchers.IO) {
            val generations=safeChild(validateRoot(),"generations")
            val id=UUID.randomUUID().toString();val nonce=UUID.randomUUID().toString()
            val directory=safeChild(generations,id)
            if(!directory.mkdir()) throw GenerationStorageException("Cannot create restore generation")
            val paths=generationPaths(directory)
            if(!paths.media.mkdir()) throw GenerationStorageException("Cannot create restore media directory")
            val gate=LocalWriteCoordinator()
            var db:LithicDatabase?=null;var prefs:SettingsStore?=null
            try {
                val database=LithicDatabase.open(context,paths.database);db=database
                val settings=SettingsStore.open(paths.settings,gate,zone);prefs=settings
                val fresh=managed(id,directory,database,settings,gate)
                populate(fresh)
                val snapshot=RoomBackupSnapshotSource(fresh).capture()
                RestoredGenerationWriter().verify(fresh,snapshot)
                val proof=JSONObject().put("formatVersion",FORMAT_VERSION).put("generationId",id).put("nonce",nonce)
                    .put("dataSha256",restoreDigest { BackupJson.writeData(snapshot.data,it) })
                    .put("settingsSha256",restoreDigest { BackupJson.writeSettings(snapshot.settings,it) })
                // Checkpoint/close all owners before readiness and activation eligibility.
                database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                    check(cursor.moveToFirst() && cursor.getInt(0)==0) { "Restore WAL checkpoint did not complete" }
                }
                gate.withShutdown { settings.closeWithinWrite();database.close() }
                prefs=null;db=null
                syncFile(paths.database);syncFile(paths.settings)
                writeAtomic(safeChild(directory,"restore-proof.json"),proof)
                val reopened=openNamed(generations,id,requireReady=false)
                try { verifyRestoreProof(reopened) } finally { closeChecked(reopened) }
                writeAtomic(paths.ready,JSONObject().put("formatVersion",FORMAT_VERSION).put("generationId",id))
                syncDirectory(directory);syncDirectory(generations)
                restoreProbe("PREPARED")
                prepared[id]=nonce
                PreparedGeneration(id,directory,nonce)
            } catch(failure:Throwable) {
                disposeAfterFailure(db,prefs,failure,gate)
                rethrowStorageFailure(failure) // Unknown/partial private candidate is preserved, never swept.
            }
        }
    }

    /** Restore's sole activation boundary; all old queued writes are retired under their own gate. */
    internal suspend fun activateRestore(expectedActiveId:String,candidate:PreparedGeneration,expectedSnapshot:BackupSnapshot?=null):ManagedGeneration = lifecycle.withLock {
        withContext(NonCancellable+Dispatchers.IO) {
            val old=active ?: throw GenerationStorageException("Current generation is unavailable")
            require(old.id==expectedActiveId) { "Current generation changed after preview" }
            require(prepared[candidate.id]==candidate.nonce) { "Restore capability is stale or unknown" }
            val storageRoot=validateRoot();val generations=safeChild(storageRoot,"generations")
            require(safeChild(generations,candidate.id)==candidate.directory) { "Candidate belongs to another owner" }
            val pointer=safeChild(storageRoot,"active-generation")
            val metadata=committedPointer(pointer)
            require(metadata.stage in setOf("STABLE","ROLLED_BACK") && metadata.activeId==old.id) { "Current persisted activation is uncertain" }
            var next:ManagedGeneration?=null
            var oldSnapshot:BackupSnapshot?=null
            try {
                val checked=openNamed(generations,candidate.id)
                try {
                    val proof=readMetadata(safeChild(candidate.directory,"restore-proof.json"),PROOF_FIELDS)
                    require(proof.getString("nonce")==candidate.nonce)
                    verifyRestoreProof(checked)
                } finally { closeChecked(checked) }
                restoreProbe("BEFORE_SWITCH")
                old.coordinator.retire(verify={
                    val observed=RoomBackupSnapshotSource(old).captureWithinOwnedBoundary(old.coordinator)
                    if(expectedSnapshot!=null) require(observed==expectedSnapshot) { "Records changed after preview; select again" }
                    oldSnapshot=observed
                },dispose={
                    try { old.settings.closeWithinWrite() } finally { old.database.close() }
                })
                active=null
                writePointer(pointer,candidate.id,old.id,"SWITCHING")
                restoreProbe("AFTER_SWITCH")
                next=openNamed(generations,candidate.id)
                restoreProbe("FIRST_OPEN")
                verifyRestoreProof(next)
                writePointer(pointer,candidate.id,old.id,"STABLE")
                active=next;prepared.remove(candidate.id);lastRecoveryWarning=null
                next
            } catch(failure:Throwable) {
                next?.let { disposeGeneration(it,failure) }
                if(!old.coordinator.isRetired) throw failure // Pre-switch/CAS failure: current resources remain live.
                // Only this owned switch may be rolled back. Unknown pointer identity is not overwritten.
                val observed=committedPointer(pointer)
                require(observed.activeId==old.id || (observed.activeId==candidate.id && observed.previousId==old.id)) {
                    "Unknown activation metadata; preserved without guessing"
                }
                disposeGeneration(old,failure)
                val reopened=openNamed(generations,old.id)
                try {
                    oldSnapshot?.let { require(RoomBackupSnapshotSource(reopened).capture()==it) { "Previous facts changed during restore" } }
                    writePointer(pointer,old.id,candidate.id,"ROLLED_BACK")
                    active=reopened
                    lastRecoveryWarning="恢复未完成，原数据集已重新打开。"
                } catch(rollback:Throwable) {
                    disposeGeneration(reopened,rollback);rollback.addSuppressed(failure);active=null;throw rollback
                }
                throw GenerationStorageException("Restore failed; previous generation reopened",failure)
            }
        }
    }

    private suspend fun verifyRestoreProof(generation:ManagedGeneration) {
        val proof=readMetadata(safeChild(generation.directory,"restore-proof.json"),PROOF_FIELDS)
        require(readId(proof.get("generationId"))==generation.id)
        readId(proof.get("nonce"))
        val snapshot=RoomBackupSnapshotSource(generation).capture()
        require(proof.getString("dataSha256")==restoreDigest { BackupJson.writeData(snapshot.data,it) }) { "Restored facts do not match readiness proof" }
        require(proof.getString("settingsSha256")==restoreDigest { BackupJson.writeSettings(snapshot.settings,it) }) { "Restored settings do not match readiness proof" }
        RestoredGenerationWriter().verify(generation,snapshot)
    }

    private fun restoreDigest(write:(java.io.OutputStream)->Unit):String {
        val digest=java.security.MessageDigest.getInstance("SHA-256")
        java.security.DigestOutputStream(object:java.io.OutputStream(){ override fun write(value:Int) {} ;override fun write(bytes:ByteArray,offset:Int,length:Int) {} },digest).use(write)
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    private fun syncFile(file:File) { java.io.RandomAccessFile(file,"rw").use { it.fd.sync() } }
    private fun syncDirectory(file:File) {
        BackupEntryFiles.requireDirectory(file.toPath())
        val before=android.system.Os.lstat(file.path)
        val fd=android.system.Os.open(file.path,android.system.OsConstants.O_RDONLY or android.system.OsConstants.O_NOFOLLOW,0)
        try {
            val held=android.system.Os.fstat(fd)
            require(android.system.OsConstants.S_ISDIR(held.st_mode) && held.st_uid==android.os.Process.myUid() && held.st_dev==before.st_dev && held.st_ino==before.st_ino)
            android.system.Os.fsync(fd)
        } finally { android.system.Os.close(fd) }
    }
    private fun writePointer(file:File,activeId:String,previousId:String?,stage:String) {
        writeAtomic(file,JSONObject().put("formatVersion",FORMAT_VERSION).put("activeId",activeId)
            .put("previousId",previousId ?: JSONObject.NULL).put("stage",stage))
        syncDirectory(requireNotNull(file.parentFile))
    }
    private fun committedPointer(pointer:File):PointerMetadata {
        val backup=safeChild(requireNotNull(pointer.parentFile),"active-generation.bak")
        val selected=if(backup.exists()) backup else pointer
        val metadata=readPointer(selected)
        if(backup.exists()) { val bytes=selected.readBytes();require(bytes.contentEquals(AtomicFile(pointer).readFully())) }
        return metadata
    }
    private suspend fun closeChecked(generation:ManagedGeneration) = withContext(NonCancellable) {
        generation.coordinator.withShutdown {
            try { generation.settings.closeWithinWrite() } finally { generation.database.close() }
        }
    }
    private suspend fun disposeGeneration(generation:ManagedGeneration,failure:Throwable) =
        disposeAfterFailure(generation.database,generation.settings,failure,generation.coordinator)

    private fun managed(id:String,directory:File,database:LithicDatabase,settings:SettingsStore,gate:LocalWriteCoordinator=coordinator) =
        ManagedGeneration(id,directory,database,settings,gate,::closeGeneration)

    private suspend fun closeGeneration(generation: ManagedGeneration) = withContext(Dispatchers.IO) {
        lifecycle.withLock {
            if (active === generation) {
                withContext(NonCancellable) {
                    try {
                        generation.coordinator.withShutdown {
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
        gate:LocalWriteCoordinator=coordinator,
    ) = withContext(NonCancellable) {
        try {
            gate.withShutdown {
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
        val stage=value.get("stage")
        if(stage !is String || stage !in setOf("STABLE","SWITCHING","ROLLED_BACK")) throw GenerationStorageException("Unknown generation recovery stage")
        val id = readId(value.get("activeId"))
        val previous = value.get("previousId")
        if (previous != JSONObject.NULL && readId(previous) == id) {
            throw GenerationStorageException("Previous generation cannot equal active generation")
        }
        val previousId=if(previous==JSONObject.NULL) null else readId(previous)
        if(stage in setOf("SWITCHING","ROLLED_BACK") && previousId==null) throw GenerationStorageException("Switching has no proven previous generation")
        return PointerMetadata(id,previousId,stage)
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
            output.fd.sync()
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

    private data class PointerMetadata(val activeId:String,val previousId:String?,val stage:String)
    private data class GenerationPaths(val database: File, val media: File, val settings: File, val ready: File)

    companion object {
        private val PROOF_FIELDS=setOf("formatVersion","generationId","nonce","dataSha256","settingsSha256")
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
