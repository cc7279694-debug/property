package com.cooper.lithic.data.media

import android.content.ContentResolver
import android.net.Uri
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import android.system.Os
import android.system.OsConstants
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.channels.Channels
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class ImageStoreStage {
    BEFORE_DECODE, BEFORE_ENCODE, BEFORE_PREPARED_WRITE, BEFORE_FILE_COMMIT,
    AFTER_FINAL_OWNERSHIP, BEFORE_FINAL_SYNC, BEFORE_FILE_REMOVE
}

fun interface ImageStoreFaults {

    fun at(stage: ImageStoreStage)
    companion object {
        val NONE = ImageStoreFaults {
        }
    }
}

/** One generation and immutable files; the repository supplies the outer snapshot/write lock. */

class LocalImageStore(
    directory: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val faults: ImageStoreFaults = ImageStoreFaults.NONE,
) {
    private val files = MediaFileProtocol(directory)
    private val mutex = Mutex()
    private val active = mutableSetOf<String>()
    @Volatile
    private var removalGuard: (suspend (LocalMediaAssetEntity, suspend () -> MediaResult<Boolean>) -> MediaResult<Boolean>)? = null

    @Synchronized
    internal fun bindRemovalGuard(
        guard: suspend (LocalMediaAssetEntity, suspend () -> MediaResult<Boolean>) -> MediaResult<Boolean>,
    ): Boolean {
        if (removalGuard != null) return false
        removalGuard = guard
        return true
    }

    suspend fun importLocalImage(resolver: ContentResolver, uri: Uri, assetId: String): MediaResult<PreparedImage> = prepareImport(resolver, uri, assetId)

    suspend fun prepareImport(resolver: ContentResolver, uri: Uri, assetId: String): MediaResult<PreparedImage> = receivingPreparation(assetId) {
        prepareSource(resolver, uri, assetId)
    }

    private suspend fun prepareSource(resolver: ContentResolver, uri: Uri, assetId: String): MediaResult<PreparedImage> = guarded {
        files.requireId(assetId)
        if (uri.scheme != "content")throw ImageStoreException(MediaFailureKind.PATH, "Select the image with the system image picker")
        val existing = files.stage(assetId)
        if (existing.exists()) {
            val restored = restore(assetId)
            active.add(assetId)
            return@guarded restored
        }
        val stage = files.createStage(assetId)
        active.add(assetId)
        var completed = false
        try {
            val source = files.child(stage, "source.bin")
            val input = resolver.openInputStream(uri) ?: throw ImageStoreException(MediaFailureKind.NOT_FOUND, "The selected image is no longer readable; select it again")
            input.use { stream ->
                FileChannel.open(source.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { channel ->
                    val output = Channels.newOutputStream(channel)
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = stream.read(buffer)
                        if (read<0)break
                        if (read == 0)continue
                        if (total>AndroidImageCodec.SOURCE_LIMIT - read)throw ImageStoreException(MediaFailureKind.TOO_LARGE, "The selected source exceeds 64 MiB")
                        output.write(buffer, 0, read)
                        total += read
                    }
                    output.flush()
                    channel.force(true)
                }
            }
            val work = files.child(stage, ".encoding")
            val info = AndroidImageCodec.normalize(source, work, resolver.getType(uri), faults)
            faults.at(ImageStoreStage.BEFORE_PREPARED_WRITE)
            val prepared = files.child(stage, if (info.mime == "image/png")"image.png" else "image.jpg")
            copyPreparedFile(work, prepared)
            if (!work.delete())throw IOException("Cannot finish the private image preparation")
            files.syncDirectory(stage)
            val bytes = readBounded(prepared)
            AndroidImageCodec.verifyDecode(prepared, info)
            val asset = LocalMediaAssetEntity(assetId, "media/$assetId.${if(info.mime=="image/png")"png" else "jpg"}", info.mime, info.width, info.height, bytes.size.toLong(), sha(bytes))
            files.saveAsset(stage, asset)
            if (!source.delete())throw IOException("Cannot remove the temporary image source")
            files.syncDirectory(stage)
            completed = true
            PreparedImage(asset)
        } finally {
            if (!completed) {
                active.remove(assetId)
                // Only this operation's proven temporary files may be removed. No final path
                // is created during preparation, and an unknown record is left untouched.
                if (files.hasOwner(stage, assetId))runCatching {
                    files.deleteKnownStage(assetId)
                }
            }
        }
    }

    suspend fun restorePrepared(assetId: String): MediaResult<PreparedImage> = receivingPreparation(assetId) {
        guarded {
            restore(assetId).also {
                active.add(assetId)
            }
        }
    }

    private suspend fun receivingPreparation(
        assetId: String,
        operation: suspend () -> MediaResult<PreparedImage>,
    ): MediaResult<PreparedImage> = try {
        operation()
    } catch (cancelled: CancellationException) {
        // A dispatcher may cancel delivery after the durable preparation finished.
        // Revoke its lease, but retain proven bytes for restoration or safe cleanup.
        withContext(NonCancellable) { releasePrepared(assetId) }
        throw cancelled
    }

    suspend fun commitPreparedAsset(prepared: PreparedImage): MediaResult<LocalMediaAssetEntity> = guarded {
        val asset = files.readAsset(prepared.asset.id)
        if (asset != prepared.asset)throw ImageStoreException(MediaFailureKind.CONFLICT, "The prepared image changed; select it again")
        val target = files.finalFile(asset)
        if (target.exists()) {
            if (!files.finalIsOwned(asset))throw ImageStoreException(MediaFailureKind.CONFLICT, "This image identity already belongs to another file")
            persistAndVerifyFinal(asset)
            return@guarded asset
        }
        val source = files.preparedFile(asset)
        verify(source, asset)
        faults.at(ImageStoreStage.BEFORE_FILE_COMMIT)
        // Android app SELinux policies can prohibit hard links. Exclusive creation never
        // overwrites an existing immutable resource, and ownership precedes body writes.
        val descriptor = try {
            Os.open(
                target.path,
                OsConstants.O_WRONLY or OsConstants.O_CREAT or OsConstants.O_EXCL or OsConstants.O_NOFOLLOW,
                384, // 0600: the current app UID only.
            )
        } catch (error: android.system.ErrnoException) {
            if (error.errno == OsConstants.EEXIST) {
                throw ImageStoreException(MediaFailureKind.CONFLICT, "This image identity already belongs to another file")
            }
            throw error
        }
        try {
            val stat = Os.fstat(descriptor)
            files.recordOwnedFinal(asset, stat.st_dev, stat.st_ino)
            faults.at(ImageStoreStage.AFTER_FINAL_OWNERSHIP)
            source.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    if (total > AndroidImageCodec.STORED_LIMIT - count) {
                        throw ImageStoreException(MediaFailureKind.TOO_LARGE, "The private cover exceeds its safe file size")
                    }
                    var offset = 0
                    while (offset < count) {
                        val written = Os.write(descriptor, buffer, offset, count - offset)
                        if (written <= 0) throw IOException("Cannot complete the private image write")
                        offset += written
                    }
                    total += count
                }
            }
        } finally {
            Os.close(descriptor)
        }
        persistAndVerifyFinal(asset)
        asset
    }

    private fun persistAndVerifyFinal(asset: LocalMediaAssetEntity) {
        val target = files.finalFile(asset)
        if (!files.finalIsOwned(asset)) {
            throw ImageStoreException(MediaFailureKind.CONFLICT, "The final image has no matching private ownership record")
        }
        // A previous attempt may have finished its copy without receiving a successful
        // fsync result. Replay must complete that durability step before Room can refer
        // to the image, even though it never rewrites the immutable final bytes.
        val descriptor = Os.open(target.path, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW, 0)
        try {
            val opened = Os.fstat(descriptor)
            val current = Os.lstat(target.path)
            if (!OsConstants.S_ISREG(opened.st_mode) ||
                opened.st_dev != current.st_dev || opened.st_ino != current.st_ino
            ) {
                throw ImageStoreException(MediaFailureKind.PATH, "The final image changed during private synchronization")
            }
            faults.at(ImageStoreStage.BEFORE_FINAL_SYNC)
            Os.fsync(descriptor)
        } finally {
            Os.close(descriptor)
        }
        files.syncDirectory(files.media)
        verify(target, asset)

        // The verified final now supplies restoration; only its redundant, proven
        // prepared payload is removed. All ownership receipts and unknown files remain.
        val prepared = files.preparedFile(asset)
        if (prepared.exists()) {
            verify(prepared, asset)
            if (!prepared.delete()) throw IOException("Cannot remove the redundant private image preparation")
            files.syncDirectory(prepared.parentFile!!)
        }
    }

    suspend fun openAsset(asset: LocalMediaAssetEntity): MediaResult<StoredImage> = guarded {
        StoredImage(asset, verify(files.finalFile(asset), asset))
    }

    suspend fun verifyAsset(asset: LocalMediaAssetEntity): MediaResult<StoredImage> = openAsset(asset)

    suspend fun releasePrepared(assetId: String) {
        mutex.withLock {
            active.remove(assetId)
        }
    }

    suspend fun isPreparedActive(assetId: String): Boolean = mutex.withLock {
        active.contains(assetId)
    }

    suspend fun discardPreparedAsset(assetId: String): MediaResult<Boolean> = guarded {
        files.requireId(assetId)
        active.remove(assetId)
        val stage = files.stage(assetId)
        if (!stage.exists())return@guarded false
        val asset = files.readAsset(assetId)
        if (files.finalFile(asset).exists())return@guarded false
        files.deleteKnownStage(assetId)
    }

    suspend fun managedAssetsForCleanup(): MediaResult<List<LocalMediaAssetEntity>> = guarded {
        for (id in files.incompleteOwnedIds())if (id !in active)files.deleteKnownStage(id)
        files.candidates()
    }

    suspend fun removeUnreferenced(asset: LocalMediaAssetEntity): MediaResult<Boolean> {
        val guardedRemoval = removalGuard
        return if (guardedRemoval == null) removeAfterReferenceCheck(asset)
        else guardedRemoval(asset) { removeAfterReferenceCheck(asset) }
    }

    /** Prove file ownership before a repository deletes its corresponding metadata. */
    internal suspend fun validateOwnedRemoval(asset: LocalMediaAssetEntity): MediaResult<Boolean> = guarded {
        validateRemoval(asset)
    }

    /** The caller must hold the shared coordinator and recheck both reference tables. */
    internal suspend fun removeAfterReferenceCheck(asset: LocalMediaAssetEntity): MediaResult<Boolean> = guarded {
        if (!validateRemoval(asset)) return@guarded false
        val target = files.finalFile(asset)
        faults.at(ImageStoreStage.BEFORE_FILE_REMOVE)
        if (target.exists() && !target.delete())throw IOException("Cannot remove the unreferenced private cover")
        files.syncDirectory(files.media)
        files.deleteKnownStage(asset.id)
    }

    private fun validateRemoval(asset: LocalMediaAssetEntity): Boolean {
        val target = files.finalFile(asset)
        if (asset.id in active)throw ImageStoreException(MediaFailureKind.IN_USE, "This image is still owned by an active edit")
        val stage = files.stage(asset.id)
        if (!stage.exists() && !target.exists())return false
        if (!stage.exists())throw ImageStoreException(MediaFailureKind.PATH, "An unknown final image must be preserved")
        val ownedAsset = try {
            files.readAsset(asset.id)
        } catch (_: IOException) {
            throw ImageStoreException(MediaFailureKind.PATH, "The file has no readable private ownership record")
        } catch (_: org.json.JSONException) {
            throw ImageStoreException(MediaFailureKind.PATH, "The file has no valid private ownership record")
        }
        if (ownedAsset != asset)throw ImageStoreException(MediaFailureKind.PATH, "The file has no matching private ownership record")
        if (target.exists() && !files.finalIsOwned(asset))throw ImageStoreException(MediaFailureKind.PATH, "An unknown final file must be preserved")
        return true
    }

    private fun restore(id: String): PreparedImage {
        files.requireId(id)
        val asset = files.readAsset(id)
        val final = files.finalFile(asset)
        val source = if (final.exists()) {
            if (!files.finalIsOwned(asset))throw ImageStoreException(MediaFailureKind.CONFLICT, "The image identity is occupied by another file")
            final
        } else files.preparedFile(asset)
        verify(source, asset)
        return PreparedImage(asset)
    }

    private fun verify(file: File, asset: LocalMediaAssetEntity): ByteArray {
        if (!file.isFile)throw ImageStoreException(MediaFailureKind.NOT_FOUND, "The saved cover file is missing")
        val bytes = readBounded(file)
        if (bytes.size.toLong() != asset.byteLength || sha(bytes) != asset.sha256)throw ImageStoreException(MediaFailureKind.INVALID_IMAGE, "The saved cover checksum or size does not match")
        val info = AndroidImageCodec.inspect(file, true)
        if (info.mime != asset.mime || info.width != asset.width || info.height != asset.height)throw ImageStoreException(MediaFailureKind.INVALID_IMAGE, "The saved cover dimensions or type do not match")
        AndroidImageCodec.verifyDecode(file, info)
        return bytes
    }

    private fun copyPreparedFile(source: File, target: File) {
        FileChannel.open(target.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { channel ->
            source.inputStream().use { input ->
                val bytes = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val count = input.read(bytes)
                    if (count < 0) break
                    if (count == 0) continue
                    if (total > AndroidImageCodec.STORED_LIMIT - count) {
                        throw ImageStoreException(MediaFailureKind.TOO_LARGE, "The prepared image exceeds its safe limit")
                    }
                    val buffer = ByteBuffer.wrap(bytes, 0, count)
                    while (buffer.hasRemaining()) channel.write(buffer)
                    total += count
                }
            }
            channel.force(true)
        }
    }

    private fun readBounded(file: File): ByteArray {
        if (file.length() !in 1..AndroidImageCodec.STORED_LIMIT)throw ImageStoreException(MediaFailureKind.TOO_LARGE, "The private cover file exceeds its limit")
        return file.inputStream().use { input ->
            val output = ByteArrayOutputStream(minOf(file.length(), 64 * 1024L).toInt())
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read<0)break
                if (read == 0)continue
                if (total>AndroidImageCodec.STORED_LIMIT - read)throw ImageStoreException(MediaFailureKind.TOO_LARGE, "The cover grew beyond its safe limit")
                output.write(buffer, 0, read)
                total += read
            }
            output.toByteArray()
        }
    }

    private fun sha(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
        "%02x".format(it)
    }

    private suspend fun <T> guarded(block: suspend() -> T): MediaResult<T> = withContext(dispatcher) {
        mutex.withLock {
            try {
                MediaResult.Success(block())
            }
            catch (cancelled: CancellationException) {
                throw cancelled
            }
            catch (error: ImageStoreException) {
                MediaResult.Failure(error.kind, error.message ?: "The private image operation failed")
            }
            catch (_: org.json.JSONException) {
                MediaResult.Failure(MediaFailureKind.PATH, "The private image record is invalid")
            }
            catch (_: IOException) {
                MediaResult.Failure(MediaFailureKind.IO, "The private image could not be read or written")
            }
            catch (_: android.system.ErrnoException) {
                MediaResult.Failure(MediaFailureKind.IO, "The private image file operation failed")
            }
            catch (_: SecurityException) {
                MediaResult.Failure(MediaFailureKind.IO, "The selected image or private cover is not accessible")
            }
        }
    }
}
