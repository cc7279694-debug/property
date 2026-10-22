package com.cooper.lithic.data.backup

import android.content.Context
import android.os.Process
import android.system.Os
import android.system.OsConstants
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.app.StorageInitializationState
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.WRITE
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One process-local preview. Validation never grants a durable overwrite command. */
internal class RestoreCoordinator(context:Context,private val owner:AppContainer) {
    private val app=context.applicationContext
    private val lifecycle=Mutex()
    private var pending:Pending?=null
    private data class Pending(val token:String,val operation:Path,val operationInode:Long,val credential:ByteArray,val sourceIdentity:String,val manifestIdentity:String,val manifestSha256:String,
        val backup:ValidatedBackup,val expectedGeneration:String,val expectedSnapshot:BackupSnapshot)

    suspend fun prepare(openInput:()->InputStream):RestorePreview=lifecycle.withLock {
        check(pending==null) { "A restore preview is already active" }
        var operation:Path?=null
        var completed:Pending?=null
        try {
            val result=withContext(Dispatchers.IO) {
                val current=owner.openStorage()
                val before=RoomBackupSnapshotSource(current).capture()
                val directory=Files.createTempDirectory(ownedBackupCacheDirectory(app),"lithic-restore-")
                operation=directory
                val credential=UUID.randomUUID().toString().toByteArray(Charsets.UTF_8)
                Files.write(directory.resolve(".restore-owner"),credential,CREATE_NEW,WRITE)
                val source=directory.resolve("source.lithicbackup")
                val ioContext=currentCoroutineContext()
                val checkpoint={ ioContext.ensureActive() }
                openInput().use { input ->
                    java.io.FileOutputStream(source.toFile()).use { output ->
                        val buffer=ByteArray(64*1024);var count=0L
                        while(true) {
                            checkpoint();val n=input.read(buffer)
                            if(n<0) break
                            if(n==0) throw java.io.IOException("Backup reader made no progress")
                            count=Math.addExact(count,n.toLong())
                            require(count<BackupLimits.MAX_TOTAL_BYTES) { "Backup input reached its size limit" }
                            output.write(buffer,0,n)
                        }
                        output.flush();output.fd.sync()
                    }
                }
                val payload=Files.createDirectory(directory.resolve("payload"))
                val bundle=BackupValidator().validate(source,payload,checkpoint)
                // No old database write or ownership switch has occurred.
                require(owner.openStorage().id==current.id) { "Current dataset changed during preview" }
                val token=UUID.randomUUID().toString()
                val record=Pending(token,directory,Os.lstat(directory.toString()).st_ino,credential,fileIdentity(source),fileIdentity(payload.resolve("manifest.json")),hashFile(payload.resolve("manifest.json")),bundle,current.id,before)
                completed=record
                checkpoint()
                record to RestorePreview(token,bundle.snapshot.completeRecordCounts(),before.completeRecordCounts(),
                    bundle.manifest.createdAt,bundle.snapshot.settings.recordZoneId)
            }
            currentCoroutineContext().ensureActive()
            pending=result.first
            result.second
        } catch(failure:Throwable) {
            pending=null
            // Unknown interrupted extraction is retained. Only a fully known bundle is eligible for cleanup.
            completed?.let { withContext(NonCancellable+Dispatchers.IO) { cleanKnown(it) } }
            throw failure
        }
    }

    suspend fun confirm(token:String):RestoreResult=lifecycle.withLock {
        val selected=pending
        if(selected==null || selected.token!=token) return@withLock RestoreResult.Failed("恢复预览已失效，请重新选择备份。",true)
        pending=null // Revoke the preview capability BEFORE the first stateful restore action.
        withContext(NonCancellable+Dispatchers.IO) {
            try {
                verifyOwnership(selected)
                val source=selected.operation.resolve("source.lithicbackup")
                val sourceLength=Files.size(source)
                Files.newInputStream(source,NOFOLLOW_LINKS).use {
                    require(BackupArchiveFiles.hash(it,sourceLength,{})==selected.backup.archiveSha256) { "Selected backup changed" }
                }
                selected.backup.manifest.files.forEach { BackupEntryFiles.verify(selected.backup.directory,it) }
                val candidate=owner.prepareRestoreGeneration(ZoneId.of(selected.backup.snapshot.settings.recordZoneId)) {
                    RestoredGenerationWriter().populate(it,selected.backup)
                }
                owner.activateRestoreGeneration(selected.expectedGeneration,candidate,selected.expectedSnapshot)
                cleanKnown(selected) // Failure here preserves private residue; it never reclassifies a verified commit.
                RestoreResult.Success
            } catch(failure:Exception) {
                val preserved=(owner.storageState.value as? StorageInitializationState.Ready)?.generation?.let { generation ->
                    if(generation.id!=selected.expectedGeneration) false else try {
                        RoomBackupSnapshotSource(generation).capture();true
                    } catch(_:Exception) { false }
                } ?: false
                cleanKnown(selected)
                RestoreResult.Failed(if(preserved) "恢复未完成，原数据集未被替换。请重新选择并核对备份。"
                    else "恢复结果无法确认，已停止使用数据。请保留当前文件并检查错误。",preserved)
            }
        }
    }

    suspend fun discard(token:String)=lifecycle.withLock {
        val selected=pending
        if(selected==null || selected.token!=token) return@withLock
        pending=null
        withContext(NonCancellable+Dispatchers.IO) {
            check(cleanKnown(selected)) { "Interrupted private restore files were preserved" }
        }
    }

    private fun verifyOwnership(selected:Pending) {
        BackupEntryFiles.requireDirectory(selected.operation)
        val stat=Os.lstat(selected.operation.toString())
        require(OsConstants.S_ISDIR(stat.st_mode) && stat.st_uid==Process.myUid() && stat.st_ino==selected.operationInode)
        val marker=selected.operation.resolve(".restore-owner")
        require(Files.isRegularFile(marker,NOFOLLOW_LINKS) && Files.readAllBytes(marker).contentEquals(selected.credential))
        require(selected.backup.directory==selected.operation.resolve("payload"))
        require(fileIdentity(selected.operation.resolve("source.lithicbackup"))==selected.sourceIdentity) { "Restore source identity changed" }
        val manifest=selected.backup.directory.resolve("manifest.json")
        require(fileIdentity(manifest)==selected.manifestIdentity && hashFile(manifest)==selected.manifestSha256) { "Extracted manifest changed" }
    }

    private fun hashFile(path:Path)=Files.newInputStream(path,NOFOLLOW_LINKS).use { BackupArchiveFiles.hash(it,Files.size(path),{}) }
    private fun fileIdentity(path:Path):String {
        val stat=Os.lstat(path.toString())
        require(OsConstants.S_ISREG(stat.st_mode) && stat.st_uid==Process.myUid() && stat.st_nlink==1L)
        return listOf(stat.st_dev,stat.st_ino,stat.st_uid.toLong(),stat.st_size,stat.st_mtime,stat.st_ctime).joinToString(":")
    }
    private fun cleanKnown(selected:Pending):Boolean=try {
        verifyOwnership(selected)
        val source=selected.operation.resolve("source.lithicbackup")
        Files.newInputStream(source,NOFOLLOW_LINKS).use { require(BackupArchiveFiles.hash(it,Files.size(source),{})==selected.backup.archiveSha256) }
        val entries=selected.backup.manifest.files.map { "payload/"+it.path }.toSet()+"payload/manifest.json"
        val dirs=(entries.flatMap { name -> val segments=name.split('/');(1 until segments.size).map { segments.take(it).joinToString("/") } }).toSet()
        val expected=entries+dirs+setOf("source.lithicbackup",".restore-owner")
        val actual=Files.walk(selected.operation).use { paths -> paths.iterator().asSequence().filter { it!=selected.operation }
            .map { selected.operation.relativize(it).joinToString("/") }.toSet() }
        require(actual==expected) { "Unknown restore residue is preserved" }
        dirs.forEach { BackupEntryFiles.requireDirectory(selected.operation.resolve(it)) }
        selected.backup.manifest.files.forEach { BackupEntryFiles.verify(selected.backup.directory,it) }
        entries.forEach { Files.delete(selected.operation.resolve(it)) }
        dirs.sortedByDescending { it.length }.forEach { Files.delete(selected.operation.resolve(it)) }
        Files.delete(selected.operation.resolve("source.lithicbackup"));Files.delete(selected.operation.resolve(".restore-owner"));Files.delete(selected.operation)
        true
    } catch(_:Exception) { false }
}
