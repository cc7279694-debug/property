package com.cooper.lithic.data.media

import android.os.Process
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import com.cooper.lithic.data.backup.BackupJpegInspection
import com.cooper.lithic.data.backup.BackupLimits
import com.cooper.lithic.data.backup.BackupMediaException
import com.cooper.lithic.data.backup.BackupPngInspection
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.data.settings.SettingsStorageException
import java.io.FileInputStream
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Read-only capability for already registered media; never imports, repairs, writes or decodes. */
class GenerationCoverReader(
    private val generation: ManagedGeneration,
    private val isCurrentOwner: () -> Boolean,
) : CoverReader {
    private val readerIdentity = Any()
    override val generationId: String get() = generation.id
    override val ownerKey: Any get() = readerIdentity

    override fun isCurrent(): Boolean = isCurrentOwner() &&
        !generation.coordinator.isRetired && generation.database.isOpen

    override suspend fun read(reference: CoverReference): CoverReadResult = withContext(Dispatchers.IO) {
        val caller = currentCoroutineContext()
        fun checkpoint() {
            caller.ensureActive()
            if (!isCurrent()) reject(CoverReadFailure.OWNER_UNAVAILABLE)
        }
        try {
            checkpoint()
            if (!StoredValueValidation.isUuid(reference.recordId) ||
                (reference.assetId != null && !StoredValueValidation.isUuid(reference.assetId))) {
                return@withContext CoverReadResult.Unavailable(CoverReadFailure.RECORD_MISMATCH)
            }
            generation.coordinator.withSnapshot {
                checkpoint()
                // Shutdown does not retire the coordinator. Validate the existing settings owner
                // before any DAO access, so an old queued read cannot reopen a closed Room owner.
                generation.settings.backupRecordZoneWithinSnapshot(generation.coordinator)
                checkpoint()
                val cover = when (reference.type) {
                    CoverOwnerType.PROJECT -> generation.database.projectDao().findProject(reference.recordId)
                        ?.let { RecordCover(it.coverAssetId) }
                    CoverOwnerType.SUBSCRIPTION -> generation.database.subscriptionDao().findSubscription(reference.recordId)
                        ?.let { RecordCover(it.coverAssetId) }
                } ?: reject(CoverReadFailure.RECORD_MISMATCH)
                checkpoint()
                if (cover.assetId != reference.assetId) reject(CoverReadFailure.RECORD_MISMATCH)
                if (cover.assetId == null) return@withSnapshot CoverReadResult.NoCover
                val asset = try { generation.database.mediaDao().find(cover.assetId) }
                catch (_: IllegalArgumentException) { reject(CoverReadFailure.INTEGRITY) }
                    ?: reject(CoverReadFailure.MISSING_METADATA)
                if (asset.id != cover.assetId) reject(CoverReadFailure.INTEGRITY)
                val bytes = capture(asset, ::checkpoint)
                checkpoint()
                CoverReadResult.Ready(VerifiedCoverImage(asset.id, asset.mime, asset.width, asset.height, bytes))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: CoverInputFailure) {
            CoverReadResult.Unavailable(failure.reason)
        } catch (_: SettingsStorageException) {
            CoverReadResult.Unavailable(CoverReadFailure.OWNER_UNAVAILABLE)
        } catch (failure: ErrnoException) {
            CoverReadResult.Unavailable(when (failure.errno) {
                OsConstants.ENOENT -> CoverReadFailure.MISSING_FILE
                OsConstants.ELOOP, OsConstants.EACCES -> CoverReadFailure.OWNERSHIP
                else -> CoverReadFailure.READ_FAILED
            })
        } catch (_: Exception) {
            CoverReadResult.Unavailable(if (!isCurrent()) CoverReadFailure.OWNER_UNAVAILABLE else CoverReadFailure.READ_FAILED)
        }
    }

    private fun capture(asset: LocalMediaAssetEntity, checkpoint: () -> Unit): ByteArray {
        checkpoint()
        if (!StoredValueValidation.isMediaPath(asset.relativePath)) reject(CoverReadFailure.OWNERSHIP)
        if (asset.byteLength !in 1 until BackupLimits.MAX_MEDIA_BYTES ||
            asset.width !in 1..1600 || asset.height !in 1..1600) reject(CoverReadFailure.INTEGRITY)
        if (asset.mime !in setOf("image/png", "image/jpeg")) reject(CoverReadFailure.FORMAT)
        val path = generation.directory.toPath().resolve(asset.relativePath)
        val before = ownedSource(path)
        if (before.st_size != asset.byteLength) reject(CoverReadFailure.INTEGRITY)
        // A leaf swapped to a FIFO after lstat must not block before fstat can reject it.
        val descriptor = Os.open(path.toString(), OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or
            OsConstants.O_NONBLOCK or CLOSE_ON_EXEC, 0)
        try {
            if (!same(before, Os.fstat(descriptor))) reject(CoverReadFailure.OWNERSHIP)
            val bytes = FileInputStream(descriptor).use { input ->
                val captured = ByteArray(asset.byteLength.toInt())
                val digest = MessageDigest.getInstance("SHA-256")
                var position = 0
                while (position < captured.size) {
                    checkpoint()
                    val count = input.read(captured, position, minOf(64 * 1024, captured.size - position))
                    if (count <= 0) reject(CoverReadFailure.INTEGRITY)
                    digest.update(captured, position, count)
                    position += count
                }
                checkpoint()
                if (input.read() != -1 || !same(before, Os.fstat(descriptor)) || !same(before, ownedSource(path)))
                    reject(CoverReadFailure.INTEGRITY)
                val sha = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
                if (!sha.equals(asset.sha256, ignoreCase = true)) reject(CoverReadFailure.INTEGRITY)
                captured
            }
            val dimensions = try {
                when (asset.mime) {
                    "image/png" -> BackupPngInspection.inspect(bytes, checkpoint)
                    // Registered JPEG container validation only, never strict entropy/new-import admission.
                    "image/jpeg" -> BackupJpegInspection.inspect(bytes, checkpoint)
                    else -> reject(CoverReadFailure.FORMAT)
                }
            } catch (failure: BackupMediaException) {
                reject(if (failure.notCertified) CoverReadFailure.FORMAT else CoverReadFailure.INTEGRITY)
            }
            if (dimensions != (asset.width to asset.height)) reject(CoverReadFailure.INTEGRITY)
            checkpoint()
            return bytes
        } finally { if (descriptor.valid()) Os.close(descriptor) }
    }

    private fun ownedSource(path: Path): StructStat {
        val root = generation.directory.toPath().toAbsolutePath().normalize()
        if (generation.directory.canonicalFile != generation.directory.absoluteFile ||
            path != path.toAbsolutePath().normalize() || !path.startsWith(root.resolve("media")))
            reject(CoverReadFailure.OWNERSHIP)
        var node = root
        for (part in root.relativize(path)) {
            val directory = Os.lstat(node.toString())
            if (!OsConstants.S_ISDIR(directory.st_mode) || directory.st_uid != Process.myUid())
                reject(CoverReadFailure.OWNERSHIP)
            node = node.resolve(part)
        }
        val file = Os.lstat(path.toString())
        if (!OsConstants.S_ISREG(file.st_mode) || file.st_uid != Process.myUid() || file.st_nlink != 1L)
            reject(CoverReadFailure.OWNERSHIP)
        return file
    }

    private fun same(a: StructStat, b: StructStat): Boolean = a.st_dev == b.st_dev && a.st_ino == b.st_ino &&
        a.st_uid == b.st_uid && a.st_mode == b.st_mode && a.st_size == b.st_size && a.st_nlink == b.st_nlink &&
        a.st_mtime == b.st_mtime && a.st_ctime == b.st_ctime

    private data class RecordCover(val assetId: String?)
    private class CoverInputFailure(val reason: CoverReadFailure) : Exception()
    private fun reject(reason: CoverReadFailure): Nothing = throw CoverInputFailure(reason)

    private companion object {
        // Android Linux O_CLOEXEC; same verified API26-compatible flag as BackupMediaSnapshot.
        const val CLOSE_ON_EXEC = 0x80000
    }
}
