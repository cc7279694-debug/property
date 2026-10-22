package com.cooper.lithic.data.backup

import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.WRITE
import java.security.MessageDigest

internal object BackupLimits {
    const val MAX_RECORDS = 200_000L
    const val MAX_DATA_BYTES = 128L * 1024 * 1024
    const val MAX_SETTINGS_BYTES = 64L * 1024
    const val MAX_MEDIA_BYTES = 2L * 1024 * 1024
    const val MAX_TOTAL_BYTES = 1024L * 1024 * 1024
    const val MAX_ENTRIES = 10_000
}

/** Internal preparation artifacts, never a completed user backup or a media verification receipt. */
internal data class PreparedBackupCore(
    val directory: Path,
    val entries: List<BackupFileEntry>,
    val recordCounts: Map<String, Long>,
    val recordZoneId: String,
    val requiredMediaPaths: Set<String>,
    private val ownerToken: String,
    private val directoryIdentity: Any? = Files.readAttributes(directory, java.nio.file.attribute.BasicFileAttributes::class.java, NOFOLLOW_LINKS).fileKey(),
) : AutoCloseable {
    /** Delete only this verified internal candidate. Changed/unknown residue is left untouched. */
    override fun close() {
        if (!Files.exists(directory, NOFOLLOW_LINKS)) return
        BackupEntryFiles.requireDirectory(directory)
        val currentKey = Files.readAttributes(directory, java.nio.file.attribute.BasicFileAttributes::class.java, NOFOLLOW_LINKS).fileKey()
        // BasicFileAttributes explicitly permits null fileKey (including this Windows provider).
        // A private random credential and exact payloads remain mandatory on every platform.
        require(directoryIdentity == currentKey) { "Candidate directory was replaced" }
        val owner = directory.resolve(".backup-core-owner")
        require(Files.isRegularFile(owner, NOFOLLOW_LINKS) && Files.size(owner) == ownerToken.toByteArray(Charsets.UTF_8).size.toLong() &&
            Files.readAllBytes(owner).contentEquals(ownerToken.toByteArray(Charsets.UTF_8))) { "Candidate ownership is unknown" }
        val folders = entries.flatMap { entry ->
            val segments = entry.path.split('/')
            (1 until segments.size).map { segments.take(it).joinToString("/") }
        }.toSet()
        val children = Files.walk(directory).use { stream -> stream.iterator().asSequence().filter { it != directory }
            .map { directory.relativize(it).joinToString("/") }.toSet() }
        require(children == entries.map { it.path }.toSet() + folders + ".backup-core-owner") { "Unknown candidate residue; preserve it" }
        folders.forEach { BackupEntryFiles.requireDirectory(directory.resolve(it)) }
        entries.forEach { BackupEntryFiles.verify(directory, it) }
        entries.forEach { Files.delete(directory.resolve(it.path)) }
        folders.sortedByDescending { it.length }.forEach { Files.delete(directory.resolve(it)) }
        Files.delete(owner)
        Files.delete(directory)
    }
}

internal object BackupEntryFiles {
    fun requirePath(name: String) {
        require(name == "data.json" || name == "settings.json" || name.startsWith("media/")) { "Unknown payload entry" }
        require(name.none { it == '\\' || it == ':' || it.code < 32 || it.code == 127 }) { "Invalid entry path" }
        require(name.split('/').none { it.isEmpty() || it == "." || it == ".." }) { "Invalid entry segments" }
    }

    fun requireDirectory(directory: Path) {
        val absolute = directory.toAbsolutePath().normalize()
        require(absolute == directory.toAbsolutePath() && Files.isDirectory(absolute, NOFOLLOW_LINKS)) { "Invalid staging directory" }
        var node: Path? = absolute
        while (node != null) {
            require(!Files.isSymbolicLink(node)) { "Staging ancestors cannot be links" }
            node = node.parent
        }
        require(absolute.toRealPath() == absolute) { "Staging path changed identity" }
    }

    fun write(
        directory: Path,
        name: String,
        exclusiveLimit: Long,
        open: (Path) -> OutputStream = { Files.newOutputStream(it, CREATE_NEW, WRITE) },
        encode: (OutputStream) -> Unit,
    ): BackupFileEntry {
        requirePath(name); requireDirectory(directory)
        require(exclusiveLimit > 0)
        val file = directory.resolve(name)
        require(file.normalize().startsWith(directory) && !Files.exists(file, NOFOLLOW_LINKS)) { "Core output exists or leaves candidate" }
        requireDirectory(file.parent)
        val digest = MessageDigest.getInstance("SHA-256")
        var length = 0L
        open(file).use { output ->
            val checked = object : OutputStream() {
                override fun write(value: Int) = write(byteArrayOf(value.toByte()), 0, 1)
                override fun write(bytes: ByteArray, offset: Int, count: Int) {
                    val next = Math.addExact(length, count.toLong())
                    if (next >= exclusiveLimit) throw IOException("Backup entry reached its resource limit")
                    output.write(bytes, offset, count)
                    digest.update(bytes, offset, count)
                    length = next
                }
                override fun flush() = output.flush()
                // The outer use owns the real stream and observes close failures.
            }
            encode(checked)
            checked.flush()
        }
        val entry = BackupFileEntry(name, length, digest.digest().hex())
        return verify(directory, entry)
    }

    fun verify(directory: Path, expected: BackupFileEntry): BackupFileEntry {
        requireDirectory(directory); requirePath(expected.path)
        val file = directory.resolve(expected.path)
        var node: Path? = file
        while (node != directory) {
            require(node != null && !Files.isSymbolicLink(node)) { "Linked payload" }
            node = node.parent
        }
        require(file.normalize().startsWith(directory.normalize()) && Files.isRegularFile(file, NOFOLLOW_LINKS)) { "Payload is not a regular staged file" }
        val before = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes::class.java, NOFOLLOW_LINKS)
        require(before.size() == expected.byteLength && expected.byteLength >= 0) { "Payload length changed" }
        val digest = MessageDigest.getInstance("SHA-256")
        var count = 0L
        Files.newInputStream(file, NOFOLLOW_LINKS).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                count = Math.addExact(count, read.toLong())
                if (count > expected.byteLength) throw IOException("Payload grew during verification")
                digest.update(buffer, 0, read)
            }
        }
        val after = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes::class.java, NOFOLLOW_LINKS)
        val actual = BackupFileEntry(expected.path, count, digest.digest().hex())
        require(actual == expected && before.fileKey() == after.fileKey() && before.size() == after.size() && before.lastModifiedTime() == after.lastModifiedTime()) { "Staged payload is incomplete or changed" }
        return actual
    }

    internal fun ByteArray.hex(): String = joinToString("") { "%02x".format(it.toInt() and 255) }
}

internal class BackupCoreStager {
    fun prepare(snapshot: BackupSnapshot, parent: Path): PreparedBackupCore {
        BackupEntryFiles.requireDirectory(parent)
        val counts = snapshot.completeRecordCounts()
        require(counts.values.fold(0L, Math::addExact) < BackupLimits.MAX_RECORDS) { "Too many backup records" }
        java.time.ZoneId.of(snapshot.settings.recordZoneId)
        val mediaIds = snapshot.data.mediaAssets.map { it.id }.toSet()
        require((snapshot.data.projects.mapNotNull { it.coverAssetId } + snapshot.data.subscriptions.mapNotNull { it.coverAssetId }).all { it in mediaIds }) { "Unresolved cover reference" }
        val directory = Files.createTempDirectory(parent, "lithic-backup-core-")
        val token = java.util.UUID.randomUUID().toString()
        Files.write(directory.resolve(".backup-core-owner"), token.toByteArray(Charsets.UTF_8), CREATE_NEW, WRITE)
        // This private candidate remains available to its owner on any IO failure; it is never published.
        val data = BackupEntryFiles.write(directory, "data.json", BackupLimits.MAX_DATA_BYTES) { BackupJson.writeData(snapshot.currentData(), it) }
        val settings = BackupEntryFiles.write(directory, "settings.json", BackupLimits.MAX_SETTINGS_BYTES) { BackupJson.writeSettings(snapshot.currentSettings(), it) }
        return PreparedBackupCore(directory, listOf(data, settings), counts.toMap(), snapshot.settings.recordZoneId,
            snapshot.data.mediaAssets.map { it.relativePath }.toSet(), token)
    }
}
