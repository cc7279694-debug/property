package com.cooper.lithic.data.backup

import java.io.InputStream
import java.io.IOException
import java.nio.charset.CharacterCodingException
import java.nio.file.Files
import java.nio.file.FileAlreadyExistsException
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

internal enum class BackupInspectionStatus { VERIFIED, INVALID, UNSUPPORTED, UNKNOWN }

/** Public-facing facts only. No private path, payload, database handle or restore capability. */
internal data class BackupInspectionSummary(
    val formatVersion: Int,
    val createdAt: String,
    val recordCounts: Map<String, Long>,
    val mediaCount: Long,
    val mediaBytes: Long,
)

internal data class BackupInspectionResult(
    val status: BackupInspectionStatus,
    val summary: BackupInspectionSummary? = null,
    val temporaryFilesRetained: Boolean = false,
)

/**
 * Read-only backup admission. Its dependencies deliberately contain no database, Owner writer,
 * restore coordinator or activation capability. Only this operation's private preparation is written.
 */
internal class BackupInspectionService(private val stagingRoot: Path) {
    private val busy = AtomicBoolean()

    suspend fun inspect(
        openInput: () -> InputStream,
        stillCurrent: () -> Boolean = { true },
    ): BackupInspectionResult {
        // Refuse concurrent work rather than queueing or replaying a selected document.
        if (!busy.compareAndSet(false, true)) return BackupInspectionResult(BackupInspectionStatus.UNKNOWN)
        try {
            return withBackupIo { cancellationCheckpoint ->
                var temporaryFilesRetained = false
                fun checkpoint() {
                    cancellationCheckpoint()
                    val current = try { stillCurrent() } catch (_: Exception) { false }
                    if (!current) throw InspectionUnavailable()
                }
                try {
                    checkpoint()
                    val directory = createOperationDirectory()
                    temporaryFilesRetained = true
                    val record = Operation(directory, attributes(directory).fileKey(), UUID.randomUUID().toString())
                    // A partial marker or copy is retained on every failure.
                    Files.write(directory.resolve(MARKER), record.credential.toByteArray(), CREATE_NEW, WRITE, NOFOLLOW_LINKS)
                    val source = directory.resolve(SOURCE)
                    // Acquire and close the provider first; its exceptions are not content evidence.
                    copySelected(openInput, source, ::checkpoint)
                    val sourceIdentity = BackupArchiveFiles.regularArchive(source)
                    val payload = Files.createDirectory(directory.resolve(PAYLOAD))
                    val payloadKey = attributes(payload).fileKey()
                    val checked = BackupValidator().validate(source, payload, ::checkpoint)
                    inspectMedia(checked, ::checkpoint)
                    checkpoint()
                    val summary = BackupInspectionSummary(
                        checked.manifest.formatVersion, checked.manifest.createdAt,
                        checked.snapshot.completeRecordCounts().toMap(),
                        checked.snapshot.data.mediaAssets.size.toLong(),
                        checked.snapshot.data.mediaAssets.fold(0L) { bytes, asset -> Math.addExact(bytes, asset.byteLength) },
                    )
                    // Only fully authenticated, exact known bytes qualify for this narrow cleanup.
                    val cleaned = cleanKnown(record, sourceIdentity, payloadKey, checked, ::checkpoint)
                    if (cleaned) temporaryFilesRetained = false
                    checkpoint()
                    BackupInspectionResult(BackupInspectionStatus.VERIFIED, summary, !cleaned)
                } catch (_: UnsupportedBackupFormatException) {
                    BackupInspectionResult(BackupInspectionStatus.UNSUPPORTED, temporaryFilesRetained = temporaryFilesRetained)
                } catch (failure: BackupMediaException) {
                    BackupInspectionResult(if (failure.notCertified) BackupInspectionStatus.UNSUPPORTED else BackupInspectionStatus.INVALID,
                        temporaryFilesRetained = temporaryFilesRetained)
                } catch (_: CancellationException) {
                    BackupInspectionResult(BackupInspectionStatus.UNKNOWN, temporaryFilesRetained = temporaryFilesRetained)
                } catch (_: InspectionUnavailable) {
                    BackupInspectionResult(BackupInspectionStatus.UNKNOWN, temporaryFilesRetained = temporaryFilesRetained)
                } catch (_: CharacterCodingException) {
                    BackupInspectionResult(BackupInspectionStatus.INVALID, temporaryFilesRetained = temporaryFilesRetained)
                } catch (_: ZipException) {
                    BackupInspectionResult(BackupInspectionStatus.INVALID, temporaryFilesRetained = temporaryFilesRetained)
                } catch (_: SerializationException) {
                    BackupInspectionResult(BackupInspectionStatus.INVALID, temporaryFilesRetained = temporaryFilesRetained)
                } catch (_: IllegalArgumentException) {
                    BackupInspectionResult(BackupInspectionStatus.INVALID, temporaryFilesRetained = temporaryFilesRetained)
                } catch (_: Exception) {
                    // Provider, disk, ownership, resource and final-state uncertainty are not bad-file proof.
                    BackupInspectionResult(BackupInspectionStatus.UNKNOWN, temporaryFilesRetained = temporaryFilesRetained)
                }
            }
        } finally { busy.set(false) }
    }

    private fun createOperationDirectory(): Path = synchronized(admission) {
        try {
            require(stagingRoot == stagingRoot.toAbsolutePath().normalize())
            val parent = requireNotNull(stagingRoot.parent)
            BackupEntryFiles.requireDirectory(parent)
            if (!Files.exists(stagingRoot, NOFOLLOW_LINKS)) {
                try { Files.createDirectory(stagingRoot) } catch (_: FileAlreadyExistsException) { /* Verify the concurrent entry below. */ }
            }
            BackupEntryFiles.requireDirectory(stagingRoot)
            // Existing partial or unknown sessions are counted, never traversed, reopened or removed.
            val retained = Files.list(stagingRoot).use { entries -> entries.filter { it.fileName.toString().startsWith(PREFIX) }
                .limit(MAX_RESIDUAL_SESSIONS.toLong()).count() }
            if (retained >= MAX_RESIDUAL_SESSIONS) throw InspectionUnavailable()
            // Source and expanded bytes each retain the original <1GiB boundary. Reserve before
            // reading; a later disk failure still returns UNKNOWN and preserves the partial session.
            if (Files.getFileStore(stagingRoot).usableSpace <= BackupLimits.MAX_TOTAL_BYTES * 2 + DISK_RESERVE_BYTES)
                throw InspectionUnavailable()
            Files.createDirectory(stagingRoot.resolve(PREFIX + UUID.randomUUID()))
        } catch (_: Exception) { throw InspectionUnavailable() }
    }

    private fun copySelected(openInput: () -> InputStream, source: Path, checkpoint: () -> Unit): Unit = try {
        checkpoint()
        openInput().use { input ->
            Files.newOutputStream(source, CREATE_NEW, WRITE, NOFOLLOW_LINKS).use { output ->
                val buffer = ByteArray(64 * 1024)
                var length = 0L
                while (true) {
                    checkpoint()
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) throw IOException("Selected document made no progress")
                    length = Math.addExact(length, count.toLong())
                    if (length >= BackupLimits.MAX_TOTAL_BYTES) throw InspectionUnavailable()
                    output.write(buffer, 0, count)
                }
                output.flush()
            }
        }
        checkpoint()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        // Opening, reading, writing the private copy, and provider close must all finish before
        // the validator can classify bytes. Preserve partial preparation on acquisition failure.
        throw InspectionUnavailable()
    }

    private fun inspectMedia(checked: ValidatedBackup, checkpoint: () -> Unit) {
        val entries = checked.manifest.files.associateBy { it.path }
        for (asset in checked.snapshot.data.mediaAssets) {
            checkpoint()
            // The frozen business validator accepts case-insensitive metadata SHA, whereas
            // manifest entries are canonical lower-case. Reuse its authenticated entry.
            val entry = entries.getValue(asset.relativePath)
            verifyEntry(checked.directory, entry, checkpoint)
            val file = checked.directory.resolve(asset.relativePath).toFile()
            val dimensions = when (asset.mime) {
                "image/png" -> BackupPngInspection.inspect(file, checkpoint)
                "image/jpeg" -> BackupJpegInspection.inspect(file, checkpoint)
                else -> throw BackupMediaException("Unsupported stored media", true)
            }
            require(dimensions == (asset.width to asset.height)) { "Media container dimensions differ" }
            // The original validator authenticates format/length/SHA and business relationships;
            // container checks do not certify JPEG entropy, Android decoding or future restoration.
            verifyEntry(checked.directory, entry, checkpoint)
        }
    }

    private fun verifyEntry(directory: Path, entry: BackupFileEntry, checkpoint: () -> Unit) {
        checkpoint()
        BackupEntryFiles.requireDirectory(directory)
        BackupEntryFiles.requirePath(entry.path)
        val file = directory.resolve(entry.path)
        require(file.normalize().startsWith(directory))
        BackupEntryFiles.requireDirectory(file.parent)
        require(Files.isRegularFile(file, NOFOLLOW_LINKS) && !Files.isSymbolicLink(file))
        val before = attributes(file)
        require(before.size() == entry.byteLength)
        Files.newInputStream(file, NOFOLLOW_LINKS).use { require(BackupArchiveFiles.hash(it, entry.byteLength, checkpoint) == entry.sha256) }
        require(BackupArchiveFiles.sameIdentity(before, attributes(file)))
        checkpoint()
    }

    private fun cleanKnown(operation: Operation, sourceIdentity: BasicFileAttributes, payloadKey: Any?, checked: ValidatedBackup, checkpoint: () -> Unit): Boolean = try {
        checkpoint()
        val directory = operation.directory
        require(directory.parent == stagingRoot && directory.fileName.toString().startsWith(PREFIX))
        UUID.fromString(directory.fileName.toString().removePrefix(PREFIX))
        BackupEntryFiles.requireDirectory(directory)
        require(attributes(directory).fileKey() == operation.fileKey)
        val marker = directory.resolve(MARKER)
        require(Files.isRegularFile(marker, NOFOLLOW_LINKS) && !Files.isSymbolicLink(marker))
        val credential = operation.credential.toByteArray()
        require(Files.size(marker) == credential.size.toLong())
        Files.newInputStream(marker, NOFOLLOW_LINKS).use { input ->
            require(credential.indices.all { input.read() == (credential[it].toInt() and 255) } && input.read() == -1)
        }
        val source = directory.resolve(SOURCE)
        require(BackupArchiveFiles.sameIdentity(sourceIdentity, BackupArchiveFiles.regularArchive(source)))
        Files.newInputStream(source, NOFOLLOW_LINKS).use {
            require(BackupArchiveFiles.hash(it, sourceIdentity.size(), checkpoint) == checked.archiveSha256)
        }
        require(checked.directory == directory.resolve(PAYLOAD) && attributes(checked.directory).fileKey() == payloadKey)
        val entries = checked.manifest.files.map { "$PAYLOAD/${it.path}" }.toSet() + "$PAYLOAD/manifest.json"
        val folders = mutableSetOf<String>()
        for (entry in entries) {
            checkpoint()
            val parts = entry.split('/')
            for (length in 1 until parts.size) {
                require(folders.size <= BackupLimits.MAX_ENTRIES * 2) { "Known cleanup path budget reached" }
                folders += parts.take(length).joinToString("/")
            }
        }
        // Only enumerate direct children of already known folders, with one sentinel entry.
        // An unexpected directory is preserved without walking into its subtree.
        val expectedChildren = (entries + folders + setOf(SOURCE, MARKER)).groupBy { it.substringBeforeLast('/', "") }
        for (folder in folders + "") {
            checkpoint()
            val node = if (folder.isEmpty()) directory else directory.resolve(folder)
            BackupEntryFiles.requireDirectory(node)
            val expected = expectedChildren[folder].orEmpty().toSet()
            val actual = Files.list(node).use { paths -> paths.limit(expected.size.toLong() + 1)
                .iterator().asSequence().map { directory.relativize(it).joinToString("/") }.toSet() }
            require(actual == expected) { "Unknown inspection residue is preserved" }
        }
        checked.manifest.files.forEach { verifyEntry(checked.directory, it, checkpoint) }
        val manifest = checked.directory.resolve("manifest.json")
        require(Files.isRegularFile(manifest, NOFOLLOW_LINKS) && !Files.isSymbolicLink(manifest))
        // Preserve original manifest bytes: strict input JSON need not use the writer's whitespace.
        require(Files.size(manifest) < BackupLimits.MAX_DATA_BYTES)
        // Its source ZIP is still independently authenticated; compare the exact extracted bytes.
        java.util.zip.ZipFile(source.toFile()).use { zip ->
            val entry = zip.getEntry("manifest.json")
            require(entry.size == Files.size(manifest))
            zip.getInputStream(entry).use { archived ->
                val originalSha = BackupArchiveFiles.hash(archived, entry.size, checkpoint)
                Files.newInputStream(manifest, NOFOLLOW_LINKS).use { staged ->
                    require(BackupArchiveFiles.hash(staged, entry.size, checkpoint) == originalSha)
                }
            }
        }
        require(BackupArchiveFiles.sameIdentity(sourceIdentity, BackupArchiveFiles.regularArchive(source)))
        checkpoint()
        // Final known-only cleanup: no reads or unbounded traversal remain after this boundary.
        entries.forEach { Files.delete(directory.resolve(it)) }
        folders.sortedByDescending { it.length }.forEach { Files.delete(directory.resolve(it)) }
        Files.delete(source); Files.delete(marker); Files.delete(directory)
        true
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (changed: InspectionUnavailable) { throw changed }
    catch (_: Exception) { false }

    private data class Operation(val directory: Path, val fileKey: Any?, val credential: String)
    private class InspectionUnavailable : RuntimeException()
    private fun attributes(path: Path): BasicFileAttributes = Files.readAttributes(path, BasicFileAttributes::class.java, NOFOLLOW_LINKS)

    private companion object {
        val admission = Any()
        const val PREFIX = "lithic-inspection-"
        const val SOURCE = "source.lithicbackup"
        const val PAYLOAD = "payload"
        const val MARKER = ".inspection-owner"
        const val MAX_RESIDUAL_SESSIONS = 3
        const val DISK_RESERVE_BYTES = 128L * 1024 * 1024
    }
}
