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
import java.io.InputStream
import java.util.UUID
import com.cooper.lithic.data.media.jpegstage.JpegStageV2
import com.cooper.lithic.data.media.jpegstage.JpegRecoveryVerifier
import com.cooper.lithic.data.media.jpegworker.AndroidJpegRecoveryProbe
import com.cooper.lithic.data.media.jpegworker.JpegWorkerClient
import com.cooper.lithic.data.media.jpegworker.JpegWorkerRuntime
import com.cooper.lithic.data.media.jpegworker.JpegWorkerRequest
import com.cooper.lithic.data.media.jpegworker.JpegWorkerOutcome
import com.cooper.lithic.data.media.jpegworker.JpegWorkerResult

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
    private val preparing = mutableSetOf<String>()
    private var closed = false
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

    private suspend fun prepareSource(resolver: ContentResolver, uri: Uri, assetId: String): MediaResult<PreparedImage> = mapped {
        files.requireId(assetId)
        if (uri.scheme != "content") throw ImageStoreException(MediaFailureKind.PATH, "Select the image with the system image picker")
        if (files.stage(assetId).exists()) return@mapped mutex.withLock { restore(assetId).also { active.add(assetId) } }
        val input = resolver.openInputStream(uri) ?: throw ImageStoreException(MediaFailureKind.NOT_FOUND, "Select the unavailable image again")
        input.buffered().use { stream ->
            stream.mark(2)
            val jpeg = stream.read() == 0xff && stream.read() == 0xd8
            stream.reset()
            if (jpeg) {
                val declaredMime = resolver.getType(uri)
                if (declaredMime != null && declaredMime != "image/jpeg") {
                    throw ImageStoreException(MediaFailureKind.INVALID_IMAGE, "The selected image type differs from its bytes")
                }
                prepareJpeg(stream, assetId)
            }
            else mutex.withLock { preparePngSource(stream, resolver.getType(uri), assetId) }
        }
    }

    private suspend fun preparePngSource(input: InputStream, mimeHint: String?, assetId: String): PreparedImage {
        files.requireId(assetId)
        val existing = files.stage(assetId)
        if (existing.exists()) {
            val restored = restore(assetId)
            active.add(assetId)
            return restored
        }
        val stage = files.createStage(assetId)
        active.add(assetId)
        var completed = false
        try {
            val source = files.child(stage, "source.bin")
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
            val info = AndroidImageCodec.normalize(source, work, mimeHint, faults)
            if (info.mime != "image/png") throw ImageStoreException(MediaFailureKind.UNSUPPORTED, "JPEG sources require strict Stage V2")
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
            return PreparedImage(asset)
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

    private suspend fun prepareJpeg(input: InputStream, assetId: String): PreparedImage {
        val protocol = files.jpeg
        val recoveryProbe = AndroidJpegRecoveryProbe(JpegWorkerRuntime.requireHostContext())
        // Only exact old Host/Worker instances that really ended can recover on restart.
        // Active/missing identities, legacy stages and unknown children remain blocked.
        recoverAbandonedJpegStages(recoveryProbe)
        val supervisor = try { recoveryProbe.currentSupervisor() }
        catch (error: IOException) { throw JpegStageV2.Blocked("JPEG supervisor OS identity is unproven", error) }
        val owner = JpegStageV2.Owner(files.generation.name, assetId, UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString())
        // Cooperative checks surround source reads; they cannot interrupt a provider blocked
        // inside read(). Such an attempt keeps its producer ownership until read returns.
        val callerContext = currentCoroutineContext()
        val snapshot = protocol.create(owner, input, supervisor) { callerContext.ensureActive() }
        val guard = protocol.guardFile(owner)
        val guardStat = Os.lstat(guard.path)
        var observed: JpegWorkerResult? = null
        fun cleanupProvedTerminal() {
            val known = protocol.snapshot(owner)
            if (!known.pinned && (known.outcome != null || known.state == JpegStageV2.State.ABANDONED)) protocol.safeCleanup(owner)
        }
        fun closeWithoutStructuredResult(result: JpegWorkerResult): Boolean {
            val worker = result.workerIdentity ?: return false
            if (!result.originalSessionQuiescent || !result.hostFdsClosed || !result.rpcQuiescent ||
                !result.consumerEnded || !result.connectionCleaned || result.outcome == JpegWorkerOutcome.SUCCESS) return false
            val live = JpegRecoveryVerifier.LiveQuiescence(owner, supervisor,
                JpegStageV2.WorkerIdentity(worker.pid, worker.uid, worker.startTicks),
                result.hostFdsClosed, result.rpcQuiescent, result.originalSessionQuiescent,
                result.connectionCleaned, result.binderDead, result.matchingProcessDead, result.guardReleased)
            // The protocol takes and retains its real namespace/consumer lease around
            // OS verification, durable revoke, proof readback and the non-success record.
            protocol.recoverNoResult(owner) { request -> verifyRecoveryClosure(recoveryProbe, request, live) }
            cleanupProvedTerminal()
            return true
        }
        fun abandoned(result: JpegWorkerResult) {
            protocol.revoke(owner, "CALLER_CANCELLED")
            if (protocol.snapshot(owner).state == JpegStageV2.State.ABANDONED) {
                cleanupProvedTerminal()
                return
            }
            val worker = result.workerIdentity
            if (worker != null && result.consumerEnded && result.connectionCleaned && result.resultCount == 1 &&
                result.sourceFdClosed && result.outputFdClosed && result.outcome in setOf(JpegWorkerOutcome.SUCCESS, JpegWorkerOutcome.CANCELLED)) {
                if (protocol.snapshot(owner).outcome == null) {
                    protocol.complete(JpegStageV2.Completion(owner, JpegStageV2.WorkerIdentity(worker.pid, worker.uid, worker.startTicks),
                        JpegStageV2.Outcome.CANCELLED, snapshot.source, resultCount = 1,
                        sourceFdClosed = true, outputFdClosed = true, binderDead = true, matchingProcessDead = true,
                        strictFinished = result.strictFinished, decoderDestroyed = result.decoderDestroyed, connectionCleaned = true))
                }
            } else if (result.outcome != JpegWorkerOutcome.SUCCESS) {
                closeWithoutStructuredResult(result)
            }
            // Cancellation is never evidence of retirement. Only the durable receipt
            // (possibly just proved above) can permit removal of this attempt's bytes.
            cleanupProvedTerminal()
        }
        try {
            val request = JpegWorkerRequest(owner.generation, owner.assetId, owner.attemptId, owner.sessionId, owner.nonce,
                snapshot.source.bytes, snapshot.source.sha256, snapshot.source.device, snapshot.source.inode, guardStat.st_dev, guardStat.st_ino)
            val result = JpegWorkerClient(JpegWorkerRuntime.requireHostContext()).execute(
                protocol.sourceFile(owner), protocol.outputFile(owner), guard, request,
                beforeDelivery = { worker ->
                    protocol.registerConsumer(owner, JpegStageV2.WorkerIdentity(worker.pid, worker.uid, worker.startTicks))
                    protocol.markDelivery(owner)
                }, revoke = { reason -> protocol.revoke(owner, reason) },
                abandonedResult = ::abandoned,
            )
            observed = result
            currentCoroutineContext().ensureActive()
            val worker = result.workerIdentity
            val outcome = when (result.outcome) {
                JpegWorkerOutcome.SUCCESS -> JpegStageV2.Outcome.SUCCESS
                JpegWorkerOutcome.INVALID_IMAGE -> JpegStageV2.Outcome.IMAGE
                JpegWorkerOutcome.RESOURCE_REJECTED -> JpegStageV2.Outcome.RESOURCE
                JpegWorkerOutcome.LOAD_ERROR, JpegWorkerOutcome.UNSUPPORTED -> JpegStageV2.Outcome.LOAD
                JpegWorkerOutcome.CANCELLED -> JpegStageV2.Outcome.CANCELLED
                JpegWorkerOutcome.TIMEOUT -> JpegStageV2.Outcome.TIMEOUT
                JpegWorkerOutcome.NATIVE_CRASH -> JpegStageV2.Outcome.NATIVE_CRASH
                JpegWorkerOutcome.RESULT_UNKNOWN -> JpegStageV2.Outcome.RESULT_UNKNOWN
            }
            if (result.outcome != JpegWorkerOutcome.SUCCESS) protocol.revoke(owner, result.outcome.name)
            var terminalProved = false
            if (worker != null && result.consumerEnded && result.connectionCleaned &&
                result.resultCount == 1 && result.sourceFdClosed && result.outputFdClosed &&
                outcome != JpegStageV2.Outcome.RESULT_UNKNOWN) {
                val output = if (result.canPrepare) files.identity(protocol.outputFile(owner)) else null
                if (output != null && (output.sha256 != result.outputSha256 || output.bytes != result.outputSize || result.mime != "image/jpeg")) {
                    throw ImageStoreException(MediaFailureKind.RESULT_UNKNOWN, "Worker output evidence differs from its private inode")
                }
                protocol.complete(JpegStageV2.Completion(owner, JpegStageV2.WorkerIdentity(worker.pid, worker.uid, worker.startTicks),
                    outcome, snapshot.source, output, result.width.takeIf { result.canPrepare }, result.height.takeIf { result.canPrepare },
                    result.resultCount, result.sourceFdClosed, result.outputFdClosed, result.binderDead, result.matchingProcessDead,
                    result.strictFinished, result.decoderDestroyed, connectionCleaned = result.connectionCleaned))
                terminalProved = true
            } else if (result.outcome != JpegWorkerOutcome.SUCCESS) {
                terminalProved = closeWithoutStructuredResult(result)
            }
            if (!result.canPrepare) {
                // Reclaim only a fully proved terminal attempt. Unknown consumers keep
                // their pin and bytes; a cleanup error remains an unknown resource state.
                if (terminalProved) protocol.safeCleanup(owner)
                val kind = when (result.outcome) {
                    JpegWorkerOutcome.INVALID_IMAGE -> MediaFailureKind.INVALID_IMAGE
                    JpegWorkerOutcome.UNSUPPORTED -> MediaFailureKind.UNSUPPORTED
                    JpegWorkerOutcome.RESOURCE_REJECTED -> MediaFailureKind.RESOURCE_REJECTED
                    JpegWorkerOutcome.TIMEOUT -> MediaFailureKind.TIMEOUT
                    JpegWorkerOutcome.NATIVE_CRASH -> MediaFailureKind.NATIVE_CRASH
                    JpegWorkerOutcome.LOAD_ERROR -> MediaFailureKind.LOAD_ERROR
                    else -> MediaFailureKind.RESULT_UNKNOWN
                }
                val message = when (kind) {
                    MediaFailureKind.INVALID_IMAGE -> "The selected JPEG is incomplete or damaged"
                    MediaFailureKind.UNSUPPORTED -> "This JPEG cannot be processed by the current image validator"
                    MediaFailureKind.RESOURCE_REJECTED -> "The image could not be processed within safe resource limits"
                    MediaFailureKind.TIMEOUT -> "Image validation took too long; the previous cover is unchanged"
                    MediaFailureKind.NATIVE_CRASH -> "Image validation stopped unexpectedly; the previous cover is unchanged"
                    MediaFailureKind.LOAD_ERROR -> "The image validator is unavailable"
                    else -> "Image validation could not confirm a safe result; the previous cover is unchanged"
                }
                throw ImageStoreException(kind, message)
            }
            currentCoroutineContext().ensureActive()
            return mutex.withLock {
                currentCoroutineContext().ensureActive()
                val prepared = protocol.recordPrepared(owner)
                faults.at(ImageStoreStage.BEFORE_PREPARED_WRITE)
                val asset = LocalMediaAssetEntity(assetId, "media/$assetId.jpg", "image/jpeg", prepared.width, prepared.height,
                    prepared.output.bytes, prepared.output.sha256)
                AndroidImageCodec.verifyDecode(prepared.file, ImageContainer(asset.mime, asset.width, asset.height))
                files.saveAsset(files.stage(assetId), asset)
                protocol.releaseSource(owner)
                currentCoroutineContext().ensureActive()
                active.add(assetId)
                PreparedImage(asset)
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable + dispatcher) {
                try {
                    val result = observed
                    if (result != null) abandoned(result) else protocol.revoke(owner, "CALLER_CANCELLED")
                } catch (cleanup: Exception) {
                    cancelled.addSuppressed(cleanup)
                    android.util.Log.w("M10JpegStageV2", "CANCEL_RETIREMENT_UNKNOWN:${cleanup.javaClass.simpleName}")
                }
            }
            throw cancelled
        } catch (error: Exception) {
            // Unknown consumers retain source/pin; no failure fabricates a terminal receipt.
            try {
                protocol.revoke(owner, "HOST_FAILURE")
                cleanupProvedTerminal()
            } catch (cleanup: Exception) {
                throw ImageStoreException(MediaFailureKind.RESULT_UNKNOWN, "JPEG preparation failed with unproven private retirement or cleanup").apply {
                    addSuppressed(error); addSuppressed(cleanup)
                }
            }
            throw error
        }
    }

    /** Never reads business rows or changes a cover. Missing closure evidence stops the whole admission. */
    internal fun recoverAbandonedJpegStages(
        probe: JpegRecoveryVerifier.Probe = AndroidJpegRecoveryProbe(JpegWorkerRuntime.requireHostContext()),
    ) {
        val protocol = files.jpeg
        for (owner in protocol.pendingRecoveryOwners()) {
            protocol.recoverNoResult(owner) { request -> verifyRecoveryClosure(probe, request) }
            protocol.safeCleanup(owner)
        }
        // This also checks legacy/unknown/nonterminal entries that were deliberately
        // not eligible for recovery. No stage is skipped to let a new consumer run.
        protocol.admission()
    }

    private fun verifyRecoveryClosure(
        probe: JpegRecoveryVerifier.Probe,
        request: JpegStageV2.RecoveryRequest,
        live: JpegRecoveryVerifier.LiveQuiescence? = null,
    ): JpegStageV2.ClosureProof = try { JpegRecoveryVerifier(probe).verify(request, live) }
    catch (error: IOException) { throw JpegStageV2.Blocked("JPEG consumer closure is unproven", error) }

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
    ): MediaResult<PreparedImage> {
        val acquired = mutex.withLock {
            if (closed || assetId in preparing) false else { preparing.add(assetId); true }
        }
        if (!acquired) return MediaResult.Failure(MediaFailureKind.IN_USE, "The private image store is closed or this preparation is active")
        return try { operation()
    } catch (cancelled: CancellationException) {
        // A dispatcher may cancel delivery after the durable preparation finished.
        // Revoke its lease, but retain proven bytes for restoration or safe cleanup.
        withContext(NonCancellable) { releasePrepared(assetId) }
        throw cancelled
        } finally { withContext(NonCancellable) { mutex.withLock { preparing.remove(assetId) } } }
    }

    internal suspend fun closeForGeneration() = mutex.withLock {
        if (preparing.isNotEmpty()) throw IOException("Image preparation still owns this generation")
        if (files.hasJpegPreparations) files.jpeg.admission()
        closed = true
    }

    suspend fun commitPreparedAsset(prepared: PreparedImage): MediaResult<LocalMediaAssetEntity> = guarded {
        val asset = files.readAsset(prepared.asset.id)
        requireStrictPreparation(asset)
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
        if (prepared.exists() && !files.isJpegStage(asset.id)) {
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
        for (id in files.incompleteOwnedIds()) if (id !in active && id !in preparing) {
            val source = files.child(files.stage(id), "source.bin")
            val png = runCatching { AndroidImageCodec.inspect(source, false).mime == "image/png" }.getOrDefault(false)
            if (png) files.deleteKnownStage(id)
        }
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
        if (asset.mime == "image/jpeg" && !files.isJpegStage(asset.id)) {
            throw ImageStoreException(MediaFailureKind.IN_USE, "Legacy JPEG resources are preserved until an independently approved cleanup")
        }
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
        requireStrictPreparation(asset)
        val final = files.finalFile(asset)
        val source = if (final.exists()) {
            if (!files.finalIsOwned(asset))throw ImageStoreException(MediaFailureKind.CONFLICT, "The image identity is occupied by another file")
            final
        } else files.preparedFile(asset)
        verify(source, asset)
        return PreparedImage(asset)
    }

    private fun requireStrictPreparation(asset: LocalMediaAssetEntity) {
        if (asset.mime != "image/jpeg") return
        if (files.isJpegStage(asset.id)) {
            val strict = files.jpeg.verifyPrepared(files.jpeg.ownerFor(asset.id))
            if (strict.output.bytes != asset.byteLength || strict.output.sha256 != asset.sha256 ||
                strict.width != asset.width || strict.height != asset.height) throw ImageStoreException(MediaFailureKind.CONFLICT, "The strict JPEG receipt differs")
        } else {
            throw ImageStoreException(MediaFailureKind.UNSUPPORTED, "Reselect the original JPEG; this legacy preparation has no strict source proof")
        }
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

    private suspend fun <T> guarded(block: suspend () -> T): MediaResult<T> = mapped { mutex.withLock { block() } }

    private suspend fun <T> mapped(block: suspend () -> T): MediaResult<T> = withContext(dispatcher) {
        try { MediaResult.Success(block()) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: ImageStoreException) { MediaResult.Failure(error.kind, error.message ?: "The private image operation failed") }
        catch (error: JpegStageV2.SourceLimitExceeded) {
            if (error.suppressed.isNotEmpty()) MediaResult.Failure(MediaFailureKind.RESULT_UNKNOWN, "The oversized JPEG left an unproven private resource; it was preserved")
            else MediaResult.Failure(MediaFailureKind.TOO_LARGE, "The selected source exceeds 64 MiB")
        }
        catch (_: JpegStageV2.Blocked) { MediaResult.Failure(MediaFailureKind.RESULT_UNKNOWN, "The JPEG consumer or private preparation is unproven; resources were preserved") }
        catch (_: org.json.JSONException) { MediaResult.Failure(MediaFailureKind.PATH, "The private image record is invalid") }
        catch (_: IOException) { MediaResult.Failure(MediaFailureKind.IO, "The private image could not be read or written") }
        catch (_: android.system.ErrnoException) { MediaResult.Failure(MediaFailureKind.IO, "The private image file operation failed") }
        catch (_: SecurityException) { MediaResult.Failure(MediaFailureKind.IO, "The selected image or private cover is not accessible") }
    }
}
