package com.cooper.lithic.data.backup

import android.content.Context
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Target belongs only to one live manual CreateDocument operation. */
interface BackupTarget {
    fun openWrite(): OutputStream
    fun openRead(): InputStream
    fun deletePartial(): Boolean
}
enum class BackupFailureReason { INVALID_MEDIA, MEDIA_NOT_CERTIFIED, SOURCE_CHANGED, IO, INCOMPLETE_TARGET, INTERNAL }
sealed interface BackupExportResult {
    data class Success(val byteLength: Long, val sha256: String, val temporaryFilesRemaining: Boolean = false) : BackupExportResult
    data class Failed(val reason: BackupFailureReason, val partialTargetRemaining: Boolean) : BackupExportResult
    data class Cancelled(val partialTargetRemaining: Boolean) : BackupExportResult
}

/** Sole writer remains the Host. This service only reads an owned generation and a selected target. */
class BackupExportService(context: Context, private val generation: ManagedGeneration) {
    private val cacheRoot = ownedBackupCacheDirectory(context)
    private val producer = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        ?: throw IllegalStateException("Application version is missing")
    suspend fun export(target: BackupTarget): BackupExportResult {
        // Preserve the result at dispatcher handoff. Cancellation is observed explicitly against
        // caller at bounded IO checkpoints; it cannot discard a verified success and delete it.
        return withBackupIo { checkpoint ->
            var core: PreparedBackupCore? = null
            var capturedSnapshot:BackupSnapshot?=null
            var archive: VerifiedBackupArchive? = null
            var cleanupFailed = false
            var operation: Path? = null
            var result: BackupExportResult
            try {
                checkpoint()
                val staging = Files.createTempDirectory(cacheRoot,"lithic-backup-")
                operation = staging
                core = RoomBackupSnapshotSource(generation).captureInto { snapshot ->
                    capturedSnapshot=snapshot
                    checkpoint()
                    val prepared = BackupCoreStager().prepare(snapshot, staging)
                    core = prepared
                    BackupMediaSnapshot(generation).copy(snapshot, prepared, checkpoint) { core = it }.also { core = it }
                }
                checkpoint()
                val prepared = requireNotNull(core)
                val manifest = BackupManifestV1.complete(prepared, producer, Instant.now())
                BackupBusinessValidation.validate(requireNotNull(capturedSnapshot),manifest,checkpoint)
                archive = BackupArchiveFiles.create(prepared, manifest, staging.resolve("complete.lithicbackup"), checkpoint)
                checkpoint()
                try {
                    BackupTargetCopy.copy(requireNotNull(archive), target::openWrite, target::openRead, checkpoint)
                } catch (failure: IllegalArgumentException) { throw IncompleteBackupTarget(failure) }
                checkpoint()
                result = BackupExportResult.Success(requireNotNull(archive).byteLength,requireNotNull(archive).sha256)
            } catch (_: CancellationException) {
                result = withContext(NonCancellable) { BackupExportResult.Cancelled(!delete(target)) }
            } catch (failure: Exception) {
                val reason = when(failure) {
                    is IncompleteBackupTarget -> BackupFailureReason.INCOMPLETE_TARGET
                    is BackupMediaException -> if (failure.notCertified) BackupFailureReason.MEDIA_NOT_CERTIFIED else BackupFailureReason.INVALID_MEDIA
                    is java.io.IOException -> BackupFailureReason.IO
                    is IllegalArgumentException -> BackupFailureReason.SOURCE_CHANGED
                    else -> BackupFailureReason.INTERNAL
                }
                result = BackupExportResult.Failed(reason,!delete(target))
            } finally {
                // No recursive sweep. Mutation, unknown residue, partial artifacts or uncertain ownership stay.
                withContext(NonCancellable) {
                    try {
                        archive?.let {
                            val before = BackupArchiveFiles.regularArchive(it.path)
                            require(before.size() == it.byteLength)
                            Files.newInputStream(it.path, NOFOLLOW_LINKS).use { input ->
                                require(BackupArchiveFiles.hash(input, it.byteLength, {}) == it.sha256)
                            }
                            require(BackupArchiveFiles.sameIdentity(before, BackupArchiveFiles.regularArchive(it.path)))
                            Files.delete(it.path)
                        }
                        core?.close()
                        operation?.let { Files.delete(it) }
                    } catch (_: Exception) { cleanupFailed = true }
                }
            }
            if (cleanupFailed && result is BackupExportResult.Success)
                result.copy(temporaryFilesRemaining = true)
            else result
        }
    }
    private fun delete(target: BackupTarget) = try { target.deletePartial() } catch (_: Exception) { false }
}

private class IncompleteBackupTarget(cause: Throwable): java.io.IOException("Target did not match verified archive", cause)

/** Android may expose /data/user/0 as a system alias only in the app mount namespace.
 * Normalize only Context's trusted cache root; never relax the payload ancestor/link checks.
 */
internal fun ownedBackupCacheDirectory(context: Context): Path {
    val app = context.applicationContext
    val data = app.dataDir
    val cache = app.cacheDir
    for (directory in listOf(data, cache)) {
        val stat = android.system.Os.lstat(directory.path)
        require(android.system.OsConstants.S_ISDIR(stat.st_mode) && stat.st_uid == android.os.Process.myUid()) {
            "Backup private root ownership is unknown"
        }
    }
    val realData = data.canonicalFile
    val realCache = cache.canonicalFile
    require(cache.absoluteFile.parentFile.canonicalFile == realData && realCache.parentFile == realData) {
        "Backup cache leaves the application data root"
    }
    return realCache.toPath().also(BackupEntryFiles::requireDirectory)
}
