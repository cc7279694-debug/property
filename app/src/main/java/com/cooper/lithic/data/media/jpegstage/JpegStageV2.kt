package com.cooper.lithic.data.media.jpegstage

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.LinkOption
import java.nio.file.StandardOpenOption
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean

/** Candidate private JPEG protocol. Immutable receipts never become business facts. */
internal class JpegStageV2(
    private val root: File,
    private val generation: String,
    identity: (File) -> FileIdentity,
    syncDirectory: (File) -> Unit,
) {
    private companion object {
        // POSIX record locks can be dropped by closing ANY same-process FD for that
        // inode. Reject local contention before opening another namespace guard FD.
        // A single bounded gate avoids an unbounded per-path lock registry; the
        // durable FileLock still arbitrates different Android processes.
        val namespaceAdmission = AtomicBoolean(false)
    }
    data class Owner(val generation: String, val assetId: String, val attemptId: String, val sessionId: String, val nonce: String)
    data class FileIdentity(val device: Long, val inode: Long, val bytes: Long, val sha256: String)
    data class WorkerIdentity(val pid: Int, val uid: Int, val startTicks: Long)
    enum class Outcome { SUCCESS, IMAGE, RESOURCE, LOAD, CANCELLED, TIMEOUT, NATIVE_CRASH, RESULT_UNKNOWN }
    enum class State { SOURCE_STAGED, PINNED, DELIVERY_UNKNOWN, REVOKED, TERMINAL, PREPARED }
    data class Snapshot(val owner: Owner, val source: FileIdentity, val state: State, val pinned: Boolean, val worker: WorkerIdentity?, val outcome: Outcome?)
    data class Completion(
        val owner: Owner, val worker: WorkerIdentity, val outcome: Outcome,
        val source: FileIdentity, val output: FileIdentity? = null,
        val width: Int? = null, val height: Int? = null,
        val resultCount: Int = 1, val sourceFdClosed: Boolean,
        val outputFdClosed: Boolean, val binderDead: Boolean, val matchingProcessDead: Boolean,
        val strictFinished: Boolean = false, val decoderDestroyed: Boolean = false,
        val connectionCleaned: Boolean = false,
    )
    data class Prepared(val owner: Owner, val source: FileIdentity, val output: FileIdentity, val width: Int, val height: Int, val file: File)
    class Blocked(message: String, cause: Throwable? = null) : IOException(message, cause)
    class SourceLimitExceeded(message: String) : IOException(message)

    private val files = StageRecords(identity, syncDirectory)
    private val known = setOf("owner.v2", "source.v2", "output.v2", "source.bin", "image.jpg", "consumer.guard",
        "pin.v2", "delivered.v2", "revoked.v2", "terminal.v2", "prepared.v2", "cleanup-intent.v2", "cleaned.v2",
        "source-release-intent.v2", "source-released.v2", "asset.json", "owned-final.json")
    private val decoderPolicy = "libjpeg-turbo-3.2.0-e01-strict-v1"
    private val normalizationPolicy = "m10-jpeg-normalize-v1"
    private val ownerKeys = setOf("protocol", "version", "generation", "assetId", "attemptId", "sessionId", "nonce", "guardDevice", "guardInode", "decoderPolicy", "normalizationPolicy")
    private val abortKeys = ownerKeys + setOf("ownerHash", "sourceDevice", "sourceInode", "sourceBytes", "sourceSha", "reasonHash", "proof")
    private val fileKeys = setOf("ownerHash", "device", "inode", "bytes", "sha256")
    private val pinKeys = setOf("ownerHash", "sourceHash", "pid", "uid", "startTicks")
    private val deliveryKeys = setOf("ownerHash", "pinHash")
    private val revokedKeys = setOf("ownerHash", "reasonHash")
    private val proofKeys = setOf("ownerHash", "terminalHash")
    private val releaseKeys = proofKeys + "sourceHash"
    private val terminalKeys = setOf("ownerHash", "pinHash", "sourceHash", "outcome", "resultCount", "sourceFdClosed", "outputFdClosed",
        "binderDead", "matchingProcessDead", "strictFinished", "decoderDestroyed", "connectionCleaned", "outputDevice", "outputInode", "outputBytes", "outputSha", "width", "height")

    private data class Loaded(val owner: Owner, val directory: File, val source: FileIdentity, val initialOutput: FileIdentity,
        val worker: WorkerIdentity?, val pin: Map<String, String>?, val terminal: Map<String, String>?,
        val revoked: Boolean, val delivered: Boolean, val prepared: Boolean, val cleanup: Boolean)

    private class FreshCreation(
        val owner: Owner, val directory: File, val guardIdentity: FileIdentity,
        val source: File, val sourceChannel: FileChannel, val initialSource: FileIdentity,
        val producerChannel: FileChannel, val producerLease: FileLock,
    ) : java.io.Closeable {
        fun requireProducer() {
            if (!producerChannel.isOpen || !producerLease.isValid) throw Blocked("JPEG producer lease is not held")
        }
        fun closeProducer() {
            var failure: Exception? = null
            try { if (producerLease.isValid) producerLease.release() } catch (error: Exception) { failure = error }
            try { producerChannel.close() } catch (error: Exception) {
                if (failure == null) failure = error else failure.addSuppressed(error)
            }
            if (failure != null) throw failure
        }
        override fun close() {
            var failure: Exception? = null
            try { sourceChannel.close() } catch (error: Exception) { failure = error }
            try { closeProducer() } catch (error: Exception) {
                if (failure == null) failure = error else failure.addSuppressed(error)
            }
            if (failure != null) throw failure
        }
    }

    /**
     * Registration and publication are short namespace transactions. The untrusted
     * provider stream is consumed outside that lock, under this attempt's existing
     * consumer.guard as an exclusive producer lease. Unpublished stages admit no FD
     * delivery, cleanup or restart recovery. A blocking read is not forcibly cancelled.
     */
    fun create(owner: Owner, sourceInput: InputStream, checkActive: () -> Unit = {}): Snapshot {
        try {
            return locked { beginCreation(owner) }.use { fresh ->
                var copyFailure: Exception? = null
                var closeFailure: Exception? = null
                var abortEligible = false
                try {
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = try {
                            checkActive()
                            val read = sourceInput.read(buffer)
                            checkActive()
                            read
                        } catch (failure: Exception) { abortEligible = true; throw failure }
                        if (count < 0) break
                        if (count == 0) { abortEligible = true; blocked("Source stream made no progress") }
                        total = Math.addExact(total, count.toLong())
                        if (total > 64L * 1024 * 1024) { abortEligible = true; throw SourceLimitExceeded("Source exceeds original input limit") }
                        val bytes = ByteBuffer.wrap(buffer, 0, count)
                        while (bytes.hasRemaining()) fresh.sourceChannel.write(bytes)
                    }
                    fresh.sourceChannel.force(true)
                    if (total == 0L) { abortEligible = true; blocked("Empty JPEG source") }
                } catch (failure: Exception) { copyFailure = failure }
                finally {
                    try { fresh.sourceChannel.close() } catch (failure: Exception) { closeFailure = failure }
                }
                // No cancellation callback or external stream code runs while a
                // namespace lock is held, including the final publication check.
                if (copyFailure == null && closeFailure == null) {
                    try { checkActive() } catch (failure: Exception) { abortEligible = true; copyFailure = failure }
                }
                val failure = copyFailure ?: closeFailure
                if (failure != null) {
                    if (closeFailure != null && closeFailure !== failure) failure.addSuppressed(closeFailure)
                    try {
                        locked {
                            try {
                                if (abortEligible && closeFailure == null && !fresh.sourceChannel.isOpen) {
                                    abortFreshCreation(fresh, failure)
                                }
                            } finally { fresh.closeProducer() }
                        }
                    } catch (cleanupFailure: Exception) { failure.addSuppressed(cleanupFailure) }
                    // In particular preserve the original CancellationException.
                    // Unknown cleanup remains attached and the stage stays blocking.
                    throw failure
                }
                locked {
                    try {
                        requireFreshCreation(fresh)
                        // Establish producer FD closure before making any complete
                        // source/output pair visible. A close error leaves this
                        // unpublished stage blocked; it cannot admit a consumer.
                        fresh.closeProducer()
                        files.sync(fresh.directory)
                        files.put(files.child(fresh.directory, "source.v2"), fileValues(fresh.directory, files.fingerprint(fresh.source)))
                        val output = files.child(fresh.directory, "image.jpg")
                        createEmpty(output)
                        files.put(files.child(fresh.directory, "output.v2"), fileValues(fresh.directory, files.fingerprint(output)))
                        asSnapshot(load(owner))
                    } finally {
                        // Publication and producer FD release share the namespace
                        // critical section. A new consumer cannot race a late close.
                        fresh.closeProducer()
                    }
                }
            }
        } catch (failure: CancellationException) { throw failure }
        catch (failure: SourceLimitExceeded) { throw failure }
        catch (failure: Blocked) { throw failure }
        catch (failure: Exception) { throw Blocked("JPEG durable state could not be established", failure) }
    }

    /** Called only under the namespace lock; no provider callbacks are invoked. */
    private fun beginCreation(owner: Owner): FreshCreation {
        validOwner(owner)
        admissionLocked()
        if (files.exists(files.child(root, "abort-${owner.attemptId}.v2"))) blocked("An aborted JPEG attempt cannot regain authority")
        val directory = files.child(root, owner.assetId)
        if (files.exists(directory) || !directory.mkdir()) blocked("JPEG preparation already exists")
        files.sync(root)
        val guard = files.child(directory, "consumer.guard")
        createEmpty(guard)
        val guardIdentity = files.fingerprint(guard)
        files.put(files.child(directory, "owner.v2"), ownerValues(owner) + mapOf("guardDevice" to guardIdentity.device.toString(), "guardInode" to guardIdentity.inode.toString()))
        val source = files.child(directory, "source.bin")
        var producerChannel: FileChannel? = null
        var producerLease: FileLock? = null
        var sourceChannel: FileChannel? = null
        try {
            producerChannel = FileChannel.open(guard.toPath(), StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)
            producerLease = try { producerChannel.tryLock() } catch (_: OverlappingFileLockException) { null }
            if (producerLease == null) blocked("Fresh JPEG producer guard is occupied")
            files.requireIdentity(guard, guardIdentity)
            sourceChannel = FileChannel.open(source.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)
            return FreshCreation(owner, directory, guardIdentity, source, sourceChannel, files.fingerprint(source), producerChannel, producerLease)
        } catch (failure: Throwable) {
            // Registration failures retain their unknown files; only close our FDs.
            for (resource in listOfNotNull(sourceChannel, producerLease, producerChannel)) {
                try { resource.close() } catch (closeFailure: Throwable) { failure.addSuppressed(closeFailure) }
            }
            throw failure
        }
    }

    private fun requireFreshCreation(fresh: FreshCreation) {
        fresh.requireProducer()
        if (fresh.sourceChannel.isOpen || readOwner(fresh.directory) != fresh.owner) blocked("JPEG producer ownership is incomplete")
        if (fresh.directory.listFiles()?.map { it.name }?.toSet() != setOf("owner.v2", "consumer.guard", "source.bin")) blocked("Unpublished JPEG stage changed before publication")
        files.requireInode(fresh.source, fresh.initialSource)
        files.requireIdentity(files.child(fresh.directory, "consumer.guard"), fresh.guardIdentity)
    }

    fun ownerFor(assetId: String): Owner = locked {
        validId(assetId)
        readOwner(files.child(root, assetId))
    }

    fun snapshot(owner: Owner): Snapshot = locked { asSnapshot(load(owner)) }

    /** Must complete durably before the host sends any data FD. */
    fun registerConsumer(owner: Owner, worker: WorkerIdentity): Snapshot = locked {
        val loaded = load(owner)
        if (loaded.revoked || loaded.terminal != null || loaded.delivered) blocked("JPEG attempt no longer admits a consumer")
        validWorker(worker)
        files.put(files.child(loaded.directory, "pin.v2"), mapOf("ownerHash" to ownerHash(loaded.directory), "sourceHash" to loaded.source.sha256,
            "pid" to worker.pid.toString(), "uid" to worker.uid.toString(), "startTicks" to worker.startTicks.toString()))
        asSnapshot(load(owner))
    }

    fun markDelivery(owner: Owner): Snapshot = locked {
        val loaded = load(owner)
        if (loaded.pin == null || loaded.revoked || loaded.terminal != null) blocked("No live JPEG consumer registration")
        files.put(files.child(loaded.directory, "delivered.v2"), mapOf("ownerHash" to ownerHash(loaded.directory), "pinHash" to files.digest(files.child(loaded.directory, "pin.v2"))))
        asSnapshot(load(owner))
    }

    /** First durable revocation wins. A failed write leaves admission blocked by the old pin. */
    fun revoke(owner: Owner, reason: String): Snapshot = locked {
        val loaded = load(owner)
        if (!loaded.revoked) files.put(files.child(loaded.directory, "revoked.v2"), mapOf("ownerHash" to ownerHash(loaded.directory), "reasonHash" to files.digest(reason)))
        asSnapshot(load(owner))
    }

    fun isAuthorized(owner: Owner): Boolean = try {
        val snapshot = snapshot(owner)
        snapshot.pinned && snapshot.state in setOf(State.PINNED, State.DELIVERY_UNKNOWN)
    } catch (_: IOException) { false }

    /** A lock alone never proves retirement. All independent evidence must match first. */
    fun complete(completion: Completion): Snapshot = locked {
        val loaded = load(completion.owner)
        if (loaded.worker != completion.worker || !loaded.delivered || completion.source != loaded.source || completion.outcome == Outcome.RESULT_UNKNOWN ||
            completion.resultCount != 1 || !completion.sourceFdClosed || !completion.outputFdClosed || !completion.binderDead || !completion.matchingProcessDead || !completion.connectionCleaned) blocked("Incomplete or mismatched JPEG termination proof")
        if (completion.outcome == Outcome.SUCCESS && (loaded.revoked || !completion.strictFinished || !completion.decoderDestroyed)) blocked("JPEG success has no current strict authority")
        if (loaded.terminal != null) {
            if (loaded.terminal != terminalValues(loaded, completion)) blocked("Conflicting JPEG completion replay")
            return@locked asSnapshot(loaded)
        }
        consumerLease(loaded) {
            // Identity, file bytes and authority are re-read while holding the same exclusive lease.
            val current = load(completion.owner)
            if (current.revoked && completion.outcome == Outcome.SUCCESS) blocked("JPEG result was revoked")
            if (completion.outcome == Outcome.SUCCESS) {
                val output = completion.output ?: blocked("Successful JPEG has no output")
                if (completion.width == null || completion.height == null || completion.width !in 1..1600 || completion.height !in 1..1600 || output.bytes !in 1..2L * 1024 * 1024) blocked("Normalized JPEG exceeds original limits")
                files.requireIdentity(files.child(current.directory, "image.jpg"), output)
            } else if (!current.revoked) {
                files.put(files.child(current.directory, "revoked.v2"), mapOf("ownerHash" to ownerHash(current.directory), "reasonHash" to files.digest(completion.outcome.name)))
            }
            files.put(files.child(current.directory, "terminal.v2"), terminalValues(current, completion))
            asSnapshot(load(completion.owner))
        }
    }

    fun recordPrepared(owner: Owner): Prepared = locked {
        val loaded = load(owner)
        if (loaded.revoked || loaded.terminal?.get("outcome") != Outcome.SUCCESS.name || loaded.cleanup) blocked("JPEG has no publishable terminal result")
        files.put(files.child(loaded.directory, "prepared.v2"), proofValues(loaded))
        prepared(load(owner))
    }

    fun verifyPrepared(owner: Owner): Prepared = locked { prepared(load(owner)) }

    /** A valid new strict receipt may release its own source; v1 is never upgraded by inference. */
    fun releaseSource(owner: Owner) = locked {
        val loaded = load(owner)
        prepared(loaded)
        consumerLease(loaded) {
            val values = proofValues(loaded) + ("sourceHash" to loaded.source.sha256)
            files.put(files.child(loaded.directory, "source-release-intent.v2"), values)
            val source = files.child(loaded.directory, "source.bin")
            if (files.exists(source)) { files.requireIdentity(source, loaded.source); files.delete(source); files.sync(loaded.directory) }
            files.put(files.child(loaded.directory, "source-released.v2"), values)
        }
    }

    /** Keeps a small terminal tombstone. Unknown children and business metadata are never deleted. */
    fun safeCleanup(owner: Owner) = locked {
        val loaded = load(owner)
        if (loaded.terminal == null) blocked("Cannot clean an unterminated JPEG consumer")
        consumerLease(loaded) {
            val current = load(owner)
            if (!current.revoked) files.put(files.child(current.directory, "revoked.v2"), mapOf("ownerHash" to ownerHash(current.directory), "reasonHash" to files.digest("cleanup")))
            files.put(files.child(current.directory, "cleanup-intent.v2"), proofValues(current))
            for (name in listOf("source.bin", "image.jpg")) {
                val file = files.child(current.directory, name)
                if (files.exists(file)) files.delete(file)
            }
            files.sync(current.directory)
            files.put(files.child(current.directory, "cleaned.v2"), proofValues(current))
        }
    }

    fun admission() = locked { admissionLocked() }
    fun sourceFile(owner: Owner): File = locked { files.child(load(owner).directory, "source.bin") }
    fun outputFile(owner: Owner): File = locked { files.child(load(owner).directory, "image.jpg") }
    fun guardFile(owner: Owner): File = locked { files.child(load(owner).directory, "consumer.guard") }

    private fun admissionLocked() {
        val children = root.listFiles() ?: blocked("Cannot list JPEG namespace")
        for (directory in children.sortedBy { it.name }) {
            if (directory.name == ".namespace.guard") continue
            if (directory.name.startsWith("abort-")) { validateAbort(directory); continue }
            validId(directory.name)
            val owner = readOwner(directory)
            val loaded = load(owner)
            if (loaded.terminal == null) blocked("An old JPEG consumer has no terminal proof")
            consumerLease(loaded) { load(owner) }
        }
    }

    private fun load(owner: Owner): Loaded {
        validOwner(owner)
        val directory = files.child(root, owner.assetId)
        if (readOwner(directory) != owner) blocked("JPEG owner identity mismatch")
        val children = directory.listFiles() ?: blocked("Cannot list JPEG stage")
        if (children.any { it.name !in known || !it.isFile || java.nio.file.Files.isSymbolicLink(it.toPath()) }) blocked("Unknown JPEG stage child must be preserved")
        val ownerRecord = files.read(files.child(directory, "owner.v2"), ownerKeys)
        val guard = files.fingerprint(files.child(directory, "consumer.guard"))
        if (guard.device != number(ownerRecord, "guardDevice") || guard.inode != number(ownerRecord, "guardInode") || guard.bytes != 0L) blocked("JPEG guard identity changed")
        val source = readFileIdentity(directory, "source.v2")
        if (source.bytes !in 1..64L * 1024 * 1024) blocked("Invalid original JPEG source size")
        val initialOutput = readFileIdentity(directory, "output.v2")
        val pin = optional(directory, "pin.v2", pinKeys)?.also {
            requireOwnerHash(directory, it)
            if (it["sourceHash"] != source.sha256) blocked("JPEG consumer source mismatch")
        }
        val worker = pin?.let { WorkerIdentity(integer(it, "pid"), integer(it, "uid"), number(it, "startTicks")).also(::validWorker) }
        val delivered = optional(directory, "delivered.v2", deliveryKeys)?.also {
            requireOwnerHash(directory, it)
            if (pin == null || it["pinHash"] != files.digest(files.child(directory, "pin.v2"))) blocked("JPEG delivery has no matching pin")
        } != null
        val revoked = optional(directory, "revoked.v2", revokedKeys)?.also { requireOwnerHash(directory, it); hash(it.getValue("reasonHash")) } != null
        val terminal = optional(directory, "terminal.v2", terminalKeys)?.also {
            requireOwnerHash(directory, it)
            if (!delivered || pin == null || it["pinHash"] != files.digest(files.child(directory, "pin.v2")) || it["sourceHash"] != source.sha256 || it["resultCount"] != "1" ||
                listOf("sourceFdClosed", "outputFdClosed", "binderDead", "matchingProcessDead", "connectionCleaned").any { key -> it[key] != "true" }) blocked("Invalid durable JPEG termination proof")
            val outcome = outcome(it)
            if (outcome == Outcome.RESULT_UNKNOWN || (outcome != Outcome.SUCCESS && !revoked)) blocked("Unknown or unrevoked JPEG terminal result")
            if (listOf("strictFinished", "decoderDestroyed").any { key -> it[key] !in setOf("true", "false") }) blocked("Malformed JPEG terminal flags")
            val outputFields = listOf("outputDevice", "outputInode", "outputBytes", "outputSha").map(it::getValue)
            if (outputFields.any { value -> value == "none" } && outputFields.any { value -> value != "none" }) blocked("Partial JPEG terminal output identity")
            if (outputFields.first() != "none") terminalOutput(it)
            for (key in listOf("width", "height")) if (it[key] != "none" && integer(it, key) !in 0..1600) blocked("Invalid JPEG terminal dimensions")
            if (outcome == Outcome.SUCCESS && (it["strictFinished"] != "true" || it["decoderDestroyed"] != "true")) blocked("JPEG receipt lacks strict finish")
            if (outcome == Outcome.SUCCESS && (outputFields.first() == "none" || integer(it, "width") !in 1..1600 || integer(it, "height") !in 1..1600 || number(it, "outputBytes") !in 1..2L * 1024 * 1024)) blocked("Invalid successful JPEG terminal output")
        }
        fun requireProof(values: Map<String, String>) {
            if (terminal == null || values["ownerHash"] != ownerHash(directory) || values["terminalHash"] != files.digest(files.child(directory, "terminal.v2"))) blocked("JPEG completion proof mismatch")
        }
        val prepared = optional(directory, "prepared.v2", proofKeys)?.also {
            requireProof(it)
            if (terminal?.get("outcome") != Outcome.SUCCESS.name) blocked("Failed JPEG cannot be prepared")
        } != null
        val cleanupIntent = optional(directory, "cleanup-intent.v2", proofKeys)?.also(::requireProof)
        optional(directory, "cleaned.v2", proofKeys)?.also { requireProof(it); if (cleanupIntent == null) blocked("JPEG cleanup has no durable intent") }
        val releaseIntent = optional(directory, "source-release-intent.v2", releaseKeys)?.also {
            requireProof(it)
            if (!prepared || it["sourceHash"] != source.sha256) blocked("Invalid JPEG source release")
        }
        optional(directory, "source-released.v2", releaseKeys)?.also { requireProof(it); if (releaseIntent != it) blocked("JPEG source release has no matching intent") }
        val sourceFile = files.child(directory, "source.bin")
        if (files.exists(sourceFile)) files.requireIdentity(sourceFile, source)
        else if (cleanupIntent == null && releaseIntent == null) blocked("JPEG original source disappeared")
        val outputFile = files.child(directory, "image.jpg")
        if (files.exists(outputFile)) {
            files.requireInode(outputFile, initialOutput)
            if (terminal?.get("outcome") == Outcome.SUCCESS.name) files.requireIdentity(outputFile, terminalOutput(terminal))
        } else if (cleanupIntent == null) blocked("JPEG output disappeared")
        return Loaded(owner, directory, source, initialOutput, worker, pin, terminal, revoked, delivered, prepared, cleanupIntent != null)
    }

    private fun prepared(loaded: Loaded): Prepared {
        if (!loaded.prepared || loaded.cleanup || loaded.revoked || loaded.terminal?.get("outcome") != Outcome.SUCCESS.name) blocked("JPEG preparation is not publishable")
        val record = loaded.terminal
        val output = terminalOutput(record)
        val width = integer(record, "width")
        val height = integer(record, "height")
        if (width !in 1..1600 || height !in 1..1600 || output.bytes !in 1..2L * 1024 * 1024) blocked("Invalid prepared JPEG limits")
        return Prepared(loaded.owner, loaded.source, output, width, height, files.child(loaded.directory, "image.jpg"))
    }

    private fun asSnapshot(loaded: Loaded): Snapshot {
        val state = when {
            loaded.cleanup -> State.TERMINAL
            loaded.revoked -> State.REVOKED
            loaded.prepared -> State.PREPARED
            loaded.terminal != null -> State.TERMINAL
            loaded.delivered -> State.DELIVERY_UNKNOWN
            loaded.pin != null -> State.PINNED
            else -> State.SOURCE_STAGED
        }
        return Snapshot(loaded.owner, loaded.source, state, loaded.pin != null && loaded.terminal == null, loaded.worker, loaded.terminal?.let(::outcome))
    }

    private fun readOwner(directory: File): Owner {
        val record = files.read(files.child(directory, "owner.v2"), ownerKeys)
        if (record["protocol"] != "lithic-m10-jpeg-stage" || record["version"] != "2" || record["decoderPolicy"] != decoderPolicy || record["normalizationPolicy"] != normalizationPolicy) blocked("Unknown JPEG protocol version or policy")
        return Owner(record.getValue("generation"), record.getValue("assetId"), record.getValue("attemptId"), record.getValue("sessionId"), record.getValue("nonce")).also {
            validOwner(it)
            if (it.assetId != directory.name) blocked("JPEG owner directory mismatch")
        }
    }

    private fun ownerValues(owner: Owner) = mapOf("protocol" to "lithic-m10-jpeg-stage", "version" to "2", "generation" to owner.generation,
        "assetId" to owner.assetId, "attemptId" to owner.attemptId, "sessionId" to owner.sessionId, "nonce" to owner.nonce,
        "decoderPolicy" to decoderPolicy, "normalizationPolicy" to normalizationPolicy)
    private fun ownerHash(directory: File) = files.digest(files.child(directory, "owner.v2"))
    private fun fileValues(directory: File, value: FileIdentity) = mapOf("ownerHash" to ownerHash(directory), "device" to value.device.toString(), "inode" to value.inode.toString(), "bytes" to value.bytes.toString(), "sha256" to value.sha256)
    private fun readFileIdentity(directory: File, name: String): FileIdentity {
        val values = files.read(files.child(directory, name), fileKeys)
        requireOwnerHash(directory, values)
        return FileIdentity(number(values, "device"), number(values, "inode"), number(values, "bytes"), values.getValue("sha256")).also {
            if (it.device < 0 || it.inode <= 0 || it.bytes < 0) blocked("Invalid JPEG file identity")
            hash(it.sha256)
        }
    }
    private fun terminalValues(loaded: Loaded, completion: Completion): Map<String, String> = mapOf("ownerHash" to ownerHash(loaded.directory),
        "pinHash" to files.digest(files.child(loaded.directory, "pin.v2")), "sourceHash" to loaded.source.sha256,
        "outcome" to completion.outcome.name, "resultCount" to completion.resultCount.toString(), "sourceFdClosed" to completion.sourceFdClosed.toString(),
        "outputFdClosed" to completion.outputFdClosed.toString(), "binderDead" to completion.binderDead.toString(), "matchingProcessDead" to completion.matchingProcessDead.toString(),
        "strictFinished" to completion.strictFinished.toString(), "decoderDestroyed" to completion.decoderDestroyed.toString(), "connectionCleaned" to completion.connectionCleaned.toString(),
        "outputDevice" to (completion.output?.device?.toString() ?: "none"), "outputInode" to (completion.output?.inode?.toString() ?: "none"),
        "outputBytes" to (completion.output?.bytes?.toString() ?: "none"), "outputSha" to (completion.output?.sha256 ?: "none"),
        "width" to (completion.width?.toString() ?: "none"), "height" to (completion.height?.toString() ?: "none"))
    private fun terminalOutput(record: Map<String, String>) = FileIdentity(number(record, "outputDevice"), number(record, "outputInode"), number(record, "outputBytes"), record.getValue("outputSha")).also {
        if (it.device < 0 || it.inode <= 0) blocked("Invalid normalized JPEG identity")
        hash(it.sha256)
    }
    private fun proofValues(loaded: Loaded) = mapOf("ownerHash" to ownerHash(loaded.directory), "terminalHash" to files.digest(files.child(loaded.directory, "terminal.v2")))
    private fun optional(directory: File, name: String, keys: Set<String>) = files.child(directory, name).let { if (files.exists(it)) files.confirm(it, keys) else null }
    private fun requireOwnerHash(directory: File, values: Map<String, String>) { if (values["ownerHash"] != ownerHash(directory)) blocked("JPEG record owner mismatch") }
    private fun number(values: Map<String, String>, key: String): Long = values[key]?.toLongOrNull()?.also { if (it.toString() != values[key]) blocked("Noncanonical JPEG number") } ?: blocked("Invalid JPEG number")
    private fun integer(values: Map<String, String>, key: String): Int = number(values, key).also { if (it !in Int.MIN_VALUE..Int.MAX_VALUE) blocked("JPEG number overflow") }.toInt()
    private fun hash(value: String) { if (!value.matches(Regex("[0-9a-f]{64}"))) blocked("Invalid JPEG digest") }
    private fun outcome(record: Map<String, String>) = try { Outcome.valueOf(record.getValue("outcome")) } catch (failure: IllegalArgumentException) { throw Blocked("Unknown JPEG outcome", failure) }
    private fun validWorker(worker: WorkerIdentity) { if (worker.pid <= 0 || worker.uid < 0 || worker.startTicks <= 0) blocked("Unverified JPEG worker identity") }
    private fun validId(value: String) { if (runCatching { UUID.fromString(value).toString() }.getOrNull() != value || value.length != 36) blocked("Invalid canonical JPEG identity") }
    private fun validOwner(owner: Owner) {
        if (owner.generation != generation || !owner.generation.matches(Regex("[a-zA-Z0-9._-]{1,128}"))) blocked("JPEG generation mismatch")
        listOf(owner.assetId, owner.attemptId, owner.sessionId, owner.nonce).forEach(::validId)
    }
    private fun createEmpty(file: File) { FileChannel.open(file.toPath(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS).use { it.force(true) }; files.sync(file.parentFile ?: blocked("JPEG stage file has no parent")) }

    /** Only this still-running create call can issue a NO_CONSUMER proof; restart never does. */
    private fun abortFreshCreation(fresh: FreshCreation, failure: Exception) {
        requireFreshCreation(fresh)
        val owner = fresh.owner
        val directory = fresh.directory
        val guardIdentity = fresh.guardIdentity
        val names = directory.listFiles()?.map { it.name }?.toSet() ?: blocked("Cannot inspect failed creation")
        if (names != setOf("owner.v2", "consumer.guard", "source.bin")) blocked("Failed creation has unknown or delivered state")
        val source = files.child(directory, "source.bin")
        files.requireInode(source, fresh.initialSource)
        files.requireIdentity(files.child(directory, "consumer.guard"), guardIdentity)
        val finalSource = files.fingerprint(source)
        if (finalSource.bytes !in 0..64L * 1024 * 1024) blocked("Failed creation source is outside ownership bounds")
        val ownerRecord = files.confirm(files.child(directory, "owner.v2"), ownerKeys)
        val marker = files.child(root, "abort-${owner.attemptId}.v2")
        val proof = ownerRecord + mapOf("ownerHash" to ownerHash(directory), "sourceDevice" to finalSource.device.toString(), "sourceInode" to finalSource.inode.toString(),
            "sourceBytes" to finalSource.bytes.toString(), "sourceSha" to finalSource.sha256, "reasonHash" to files.digest(failure.javaClass.name), "proof" to "NO_CONSUMER")
        files.put(marker, proof)
        if (files.confirm(marker, abortKeys) != proof) blocked("Failed creation abort proof is not durable")
        // The same exclusive producer lease survived copying and proof creation.
        // Source/output receipts are absent and namespace admission is still held;
        // no consumer registration or FD delivery could have occurred.
        if (directory.listFiles()?.map { it.name }?.toSet() != names) blocked("Failed creation changed before cleanup")
        files.requireIdentity(source, finalSource)
        files.requireIdentity(files.child(directory, "consumer.guard"), guardIdentity)
        if (files.read(files.child(directory, "owner.v2"), ownerKeys) != ownerRecord) blocked("Failed creation owner changed")
        // Windows cannot delete a still-locked guard. Release only here, after the
        // durable NO_CONSUMER proof and all identities are checked, while namespace
        // remains held. This exception NEVER applies to pinned consumer cleanup.
        fresh.closeProducer()
        files.delete(source)
        files.delete(files.child(directory, "consumer.guard"))
        files.delete(files.child(directory, "owner.v2"))
        files.sync(directory)
        if (directory.listFiles()?.isNotEmpty() != false || !directory.delete()) blocked("Failed creation has conservative residuals")
        files.sync(root)
        validateAbort(marker)
    }

    private fun validateAbort(marker: File) {
        val record = files.confirm(marker, abortKeys)
        val owner = Owner(record.getValue("generation"), record.getValue("assetId"), record.getValue("attemptId"), record.getValue("sessionId"), record.getValue("nonce"))
        validOwner(owner)
        if (marker.name != "abort-${owner.attemptId}.v2" || record["proof"] != "NO_CONSUMER") blocked("Unknown JPEG abort ownership")
        val expectedOwner = ownerValues(owner) + mapOf("guardDevice" to record.getValue("guardDevice"), "guardInode" to record.getValue("guardInode"))
        if (record.filterKeys { it in ownerKeys } != expectedOwner || files.digest(String(files.encode(expectedOwner), Charsets.US_ASCII)) != record["ownerHash"]) blocked("JPEG abort owner hash mismatch")
        if (number(record, "guardDevice") < 0 || number(record, "guardInode") <= 0 || number(record, "sourceDevice") < 0 || number(record, "sourceInode") <= 0 || number(record, "sourceBytes") !in 0..64L * 1024 * 1024) blocked("Invalid JPEG abort resource identity")
        hash(record.getValue("sourceSha")); hash(record.getValue("reasonHash"))
        val residual = files.child(root, owner.assetId)
        if (files.exists(residual)) {
            // A new known attempt may reuse its stable assetId only after the old directory
            // was fully removed. Same-attempt partial cleanup is never auto-resumed here.
            if (readOwner(residual).attemptId == owner.attemptId) blocked("JPEG abort cleanup is not complete")
        }
    }

    private fun <T> consumerLease(loaded: Loaded, action: () -> T): T {
        val guard = files.child(loaded.directory, "consumer.guard")
        val before = files.fingerprint(guard)
        return FileChannel.open(guard.toPath(), StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS).use { channel ->
            val lease = try { channel.tryLock() } catch (_: OverlappingFileLockException) { null }
            if (lease == null) blocked("JPEG consumer lease is still occupied")
            lease.use {
                if (files.fingerprint(guard) != before) blocked("JPEG consumer guard replaced during lock acquisition")
                action()
            }
        }
    }

    private fun <T> locked(action: () -> T): T {
        if (!namespaceAdmission.compareAndSet(false, true)) blocked("JPEG protocol is concurrently changing in this process")
        try {
            if (!files.exists(root)) {
                val parent = root.parentFile ?: blocked("JPEG namespace has no parent")
                files.safeDirectory(parent)
                if (!root.mkdir()) blocked("Cannot create JPEG namespace")
                files.sync(parent)
            }
            files.safeDirectory(root)
            val guard = files.child(root, ".namespace.guard")
            if (!files.exists(guard)) {
                try { createEmpty(guard) } catch (failure: java.nio.file.FileAlreadyExistsException) { /* a concurrent creator owns the same guarded namespace */ }
            }
            val before = files.fingerprint(guard)
            if (before.bytes != 0L) blocked("Invalid JPEG namespace guard")
            return FileChannel.open(guard.toPath(), StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS).use { channel ->
                val lease = try { channel.tryLock() } catch (_: OverlappingFileLockException) { null }
                if (lease == null) blocked("JPEG protocol is concurrently changing")
                lease.use {
                    if (files.fingerprint(guard) != before) blocked("JPEG namespace guard was replaced")
                    action()
                }
            }
        } catch (failure: SourceLimitExceeded) { throw failure }
        catch (failure: Blocked) { throw failure }
        catch (failure: Exception) { throw Blocked("JPEG durable state could not be established", failure) }
        finally { namespaceAdmission.set(false) }
    }
    private fun blocked(message: String): Nothing = throw Blocked(message)
}
