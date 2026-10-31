package com.cooper.lithic.data.media.photolite

import android.os.Process
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import androidx.room.withTransaction
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.StoredValueValidation
import java.io.Closeable
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Experimental add-only writer. No picker, decoding, replacement, deletion, or owner creation. */
class GenerationPhotoLiteCoverWriter(
    private val generation: ManagedGeneration,
    private val isCurrentOwner: () -> Boolean,
) : PhotoLiteCoverWriter {
    private val identity = Any()
    override val generationId: String get() = generation.id
    override val ownerKey: Any get() = identity
    override fun isCurrent(): Boolean = isCurrentOwner() &&
        !generation.coordinator.isRetired && generation.database.isOpen

    override suspend fun capture(projectId: String): PhotoLiteCaptureResult = withContext(Dispatchers.IO) {
        if (!StoredValueValidation.isUuid(projectId)) {
            return@withContext PhotoLiteCaptureResult.Rejected(PhotoLiteFailure.STALE_PROJECT)
        }
        try {
            generation.coordinator.withSnapshot {
                currentCoroutineContext().ensureActive()
                checkOwner()
                // This closed-owner check precedes every DAO access: Room must never reopen it.
                generation.settings.backupRecordZoneWithinSnapshot(generation.coordinator)
                checkOwner()
                val project = generation.database.projectDao().findProject(projectId)
                    ?: reject(PhotoLiteFailure.STALE_PROJECT)
                if (project.coverAssetId != null) reject(PhotoLiteFailure.ALREADY_HAS_COVER)
                currentCoroutineContext().ensureActive()
                checkOwner()
                PhotoLiteCaptureResult.Ready(PhotoLiteTarget(generationId, ownerKey, project))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (known: Rejected) {
            PhotoLiteCaptureResult.Rejected(known.reason)
        } catch (_: Exception) {
            PhotoLiteCaptureResult.Rejected(
                if (isCurrent()) PhotoLiteFailure.STORAGE_FAILED else PhotoLiteFailure.OWNER_UNAVAILABLE,
            )
        }
    }

    override suspend fun commit(target: PhotoLiteTarget, png: PhotoLitePng): PhotoLiteCommitResult {
        if (target.ownerKey !== ownerKey || target.generationId != generationId) {
            return PhotoLiteCommitResult.Rejected(PhotoLiteFailure.OWNER_UNAVAILABLE)
        }
        if (!target.claim()) return PhotoLiteCommitResult.Rejected(PhotoLiteFailure.DUPLICATE)
        val caller = currentCoroutineContext()
        // Check the original caller at each bounded IO step. Preserve a known commit result even
        // if cancellation arrives at an IO dispatcher return boundary, as backup export does.
        return withContext(NonCancellable) {
            withContext(Dispatchers.IO) {
                var fileStarted = false
                var transactionEntered = false
                try {
                    val checkpoint = { caller.ensureActive(); checkOwner() }
                    checkpoint()
                    val bytes = try {
                        png.copyBytes().also { validateReceipt(png, it) }
                    } catch (_: IllegalArgumentException) {
                        reject(PhotoLiteFailure.INVALID_IMAGE)
                    }
                    generation.coordinator.withWrite {
                        checkpoint()
                        generation.settings.backupRecordZoneWithinSnapshot(generation.coordinator)
                        checkpoint()
                        if (target.expected.coverAssetId != null) reject(PhotoLiteFailure.ALREADY_HAS_COVER)
                        val dao = generation.database.projectDao()
                        if (dao.findProject(target.projectId) != target.expected) reject(PhotoLiteFailure.STALE_PROJECT)
                        if (!StoredValueValidation.isUuid(target.nonce)) reject(PhotoLiteFailure.INVALID_IMAGE)
                        val relative = "media/${target.nonce}.png"
                        fileStarted = true
                        val authenticated = writeAndAuthenticate(relative, bytes, png, checkpoint)
                        checkpoint()
                        // From here on, an unexpected exception is conservative UNKNOWN. A file
                        // is never removed to guess whether a transaction committed.
                        transactionEntered = true
                        generation.database.withTransaction {
                            checkpoint()
                            authenticated.verify()
                            generation.database.mediaDao().insert(
                                LocalMediaAssetEntity(
                                    target.nonce, relative, "image/png", png.width.toInt(), png.height.toInt(),
                                    png.byteLength, png.sha256,
                                ),
                            )
                            if (addCover(dao = dao, expected = target.expected, assetId = target.nonce) != 1) {
                                throw CompareAndSetConflict()
                            }
                            // Cancellation/owner change before transaction completion rolls back
                            // both rows. File authentication is independently checked again.
                            checkpoint()
                            authenticated.verify()
                        }
                        if (isCurrent()) PhotoLiteCommitResult.Success(target.nonce) else PhotoLiteCommitResult.Unknown
                    }
                } catch (_: CompareAndSetConflict) {
                    PhotoLiteCommitResult.Rejected(PhotoLiteFailure.STALE_PROJECT)
                } catch (_: UnconfirmedFileClose) {
                    PhotoLiteCommitResult.Unknown
                } catch (cancelled: CancellationException) {
                    if (transactionEntered) PhotoLiteCommitResult.Unknown else throw cancelled
                } catch (known: Rejected) {
                    if (transactionEntered) PhotoLiteCommitResult.Unknown else PhotoLiteCommitResult.Rejected(known.reason)
                } catch (_: Exception) {
                    when {
                        transactionEntered -> PhotoLiteCommitResult.Unknown
                        !isCurrent() -> PhotoLiteCommitResult.Rejected(PhotoLiteFailure.OWNER_UNAVAILABLE)
                        fileStarted -> PhotoLiteCommitResult.Rejected(PhotoLiteFailure.FILE_FAILED)
                        else -> PhotoLiteCommitResult.Rejected(PhotoLiteFailure.STORAGE_FAILED)
                    }
                }
            }
        }
    }

    private suspend fun addCover(
        dao: com.cooper.lithic.data.local.ProjectDao,
        expected: LongTermProjectEntity,
        assetId: String,
    ): Int = dao.addPhotoLiteCoverIfUnchanged(
        expected.id, expected.name, expected.categoryId, expected.startedOn, expected.endedOn,
        expected.endReason, expected.coverAssetId, expected.note, expected.createdAt, expected.updatedAt,
        assetId, maxOf(Instant.now(), expected.updatedAt.plusNanos(1)),
    )

    private fun validateReceipt(png: PhotoLitePng, bytes: ByteArray) {
        val checked = PhotoLitePng.checked(bytes)
        require(checked.width == png.width && checked.height == png.height &&
            checked.byteLength == png.byteLength && checked.sha256 == png.sha256)
    }

    private fun checkOwner() {
        if (!isCurrent()) reject(PhotoLiteFailure.OWNER_UNAVAILABLE)
    }

    private fun writeAndAuthenticate(
        relative: String,
        bytes: ByteArray,
        png: PhotoLitePng,
        checkpoint: () -> Unit,
    ): AuthenticatedFile {
        require(StoredValueValidation.isMediaPath(relative))
        val root = generation.directory.absoluteFile
        val media = File(root, "media")
        val rootIdentity = directory(root)
        val mediaIdentity = directory(media)
        require(rootIdentity.st_dev == mediaIdentity.st_dev)
        val destination = File(root, relative).absoluteFile
        require(destination.parentFile == media && destination.canonicalFile == destination)
        val guard = AuthenticatedFile(root, rootIdentity, media, mediaIdentity, destination)
        guard.verifyDirectories()
        checkpoint()
        val writeFd = Os.open(
            destination.path,
            OsConstants.O_WRONLY or OsConstants.O_CREAT or OsConstants.O_EXCL or
                OsConstants.O_NOFOLLOW or CLOSE_ON_EXEC,
            0x180, // 0600; never broaden app-private media permissions.
        )
        withOwnedFd(writeFd) {
            val empty = regular(Os.fstat(writeFd))
            require(empty.st_size == 0L)
            withOwnedStream(FileOutputStream(writeFd)) { output ->
                var offset = 0
                while (offset < bytes.size) {
                    checkpoint()
                    val count = minOf(CHUNK, bytes.size - offset)
                    output.write(bytes, offset, count)
                    offset += count
                }
                output.flush()
                writeFd.sync()
                checkpoint()
                val written = regular(Os.fstat(writeFd))
                require(sameNode(empty, written) && written.st_size == png.byteLength)
                require(sameFile(written, regular(Os.lstat(destination.path))))
                guard.identity = written
                guard.verify()
            }
        }
        // Persist the directory entry before allowing a database reference to this file.
        val directoryFd = Os.open(media.path, OsConstants.O_RDONLY or OsConstants.O_NONBLOCK or
            OsConstants.O_NOFOLLOW or CLOSE_ON_EXEC, 0)
        withOwnedFd(directoryFd) {
            // O_DIRECTORY is not an Android SDK constant. Check the actual opened descriptor
            // before syncing, as the existing restore writer does; NONBLOCK also rejects a swapped FIFO safely.
            val opened = Os.fstat(directoryFd)
            require(OsConstants.S_ISDIR(opened.st_mode) && sameNode(mediaIdentity, opened))
            Os.fsync(directoryFd)
        }
        checkpoint()
        guard.verify()
        val readFd = Os.open(destination.path, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or
            OsConstants.O_NONBLOCK or CLOSE_ON_EXEC, 0)
        withOwnedFd(readFd) {
            val before = regular(Os.fstat(readFd))
            require(sameFile(guard.identity!!, before))
            withOwnedStream(FileInputStream(readFd)) { input ->
                val actual = ByteArray(bytes.size)
                var offset = 0
                while (offset < actual.size) {
                    checkpoint()
                    val count = input.read(actual, offset, minOf(CHUNK, actual.size - offset))
                    if (count <= 0) throw IOException("Incomplete PHOTO-LITE output")
                    offset += count
                }
                require(input.read() == -1)
                checkpoint()
                require(sameFile(before, regular(Os.fstat(readFd))))
                require(actual.contentEquals(bytes))
                require(MessageDigest.getInstance("SHA-256").digest(actual).joinToString("") {
                    "%02x".format(it.toInt() and 0xff)
                } == png.sha256)
                validateReceipt(png, actual)
            }
        }
        guard.verify()
        return guard
    }

    private class AuthenticatedFile(
        private val root: File,
        private val rootIdentity: StructStat,
        private val media: File,
        private val mediaIdentity: StructStat,
        private val destination: File,
    ) {
        var identity: StructStat? = null
        fun verifyDirectories() {
            require(sameNode(rootIdentity, directory(root)))
            require(sameNode(mediaIdentity, directory(media)))
            require(destination.parentFile == media && destination.canonicalFile == destination)
        }
        fun verify() {
            verifyDirectories()
            require(sameFile(requireNotNull(identity), regular(Os.lstat(destination.path))))
        }
    }

    private class Rejected(val reason: PhotoLiteFailure) : RuntimeException()
    private class CompareAndSetConflict : RuntimeException()
    private class UnconfirmedFileClose(cause: Exception) : IOException("PHOTO-LITE FD close not proved", cause)

    private companion object {
        const val CHUNK = 64 * 1024
        const val CLOSE_ON_EXEC = 0x80000
        fun reject(reason: PhotoLiteFailure): Nothing = throw Rejected(reason)
        fun directory(file: File): StructStat {
            require(file.canonicalFile == file.absoluteFile)
            return Os.lstat(file.path).also {
                require(OsConstants.S_ISDIR(it.st_mode) && it.st_uid == Process.myUid())
            }
        }
        fun regular(stat: StructStat): StructStat = stat.also {
            require(OsConstants.S_ISREG(it.st_mode) && it.st_uid == Process.myUid() && it.st_nlink == 1L)
        }
        fun sameNode(a: StructStat, b: StructStat): Boolean = a.st_dev == b.st_dev &&
            a.st_ino == b.st_ino && a.st_uid == b.st_uid && a.st_mode == b.st_mode && a.st_nlink == b.st_nlink
        fun sameFile(a: StructStat, b: StructStat): Boolean = sameNode(a, b) && a.st_size == b.st_size &&
            a.st_mtime == b.st_mtime && a.st_ctime == b.st_ctime
        inline fun <T> withOwnedFd(fd: FileDescriptor, block: () -> T): T {
            var failure: Throwable? = null
            try {
                return block()
            } catch (caught: Throwable) {
                failure = caught
                throw caught
            } finally {
                if (fd.valid()) try {
                    Os.close(fd)
                } catch (unknown: Exception) {
                    throw UnconfirmedFileClose(unknown).also { closing -> failure?.let(closing::addSuppressed) }
                }
            }
        }
        inline fun <S : Closeable, T> withOwnedStream(stream: S, block: (S) -> T): T {
            var failure: Throwable? = null
            try {
                return block(stream)
            } catch (caught: Throwable) {
                failure = caught
                throw caught
            } finally {
                try {
                    stream.close()
                } catch (unknown: Exception) {
                    throw UnconfirmedFileClose(unknown).also { closing -> failure?.let(closing::addSuppressed) }
                }
            }
        }
    }
}
