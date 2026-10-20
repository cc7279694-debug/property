package com.cooper.lithic.data.backup

import com.cooper.lithic.data.backup.BackupEntryFiles.hex
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.zip.ZipFile

/** A closed, independently read-back archive. This is not a target-provider success receipt. */
internal data class VerifiedBackupArchive(val path: Path, val byteLength: Long, val sha256: String) {
    init {
        require(byteLength > 0 && byteLength < BackupLimits.MAX_TOTAL_BYTES)
        require(Regex("[0-9a-f]{64}").matches(sha256))
    }
}

internal object BackupArchiveFiles {
    /** Never overwrite another operation or put undeclared archive bytes inside the payload tree. */
    fun create(core: PreparedBackupCore, manifest: BackupManifestV1, path: Path, checkpoint: () -> Unit = {}): VerifiedBackupArchive {
        require(path == path.toAbsolutePath().normalize() && path.parent == core.directory.parent) { "Archive must be a private snapshot sibling" }
        BackupEntryFiles.requireDirectory(path.parent)
        require(!Files.exists(path, NOFOLLOW_LINKS)) { "Archive output already exists" }
        checkpoint()
        BackupArchiveWriter.write(core, manifest, Files.newOutputStream(path, CREATE_NEW, WRITE), checkpoint)
        return verify(core, manifest, path, checkpoint)
    }

    /** ZipFile requires a real central directory; local headers alone cannot prove completion. */
    fun verify(core: PreparedBackupCore, manifest: BackupManifestV1, path: Path, checkpoint: () -> Unit = {}): VerifiedBackupArchive {
        checkpoint()
        require(manifest == BackupManifestV1.complete(core, manifest.producerAppVersion, java.time.Instant.parse(manifest.createdAt)))
        val before = regularArchive(path)
        val manifestBytes = manifest.encode()
        val expected = manifest.files.associateBy { it.path }
        require(expected.size == manifest.files.size)
        requireEndDirectory(path, before.size(), expected.size + 1, checkpoint)
        ZipFile(path.toFile()).use { zip ->
            require(zip.comment == null || zip.comment.isEmpty()) { "Unexpected archive comment" }
            val names = mutableSetOf<String>()
            var expanded = 0L
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                checkpoint()
                val entry = entries.nextElement()
                require(!entry.isDirectory && names.add(entry.name)) { "Directory or duplicate ZIP entry" }
                require(names.size < BackupLimits.MAX_ENTRIES)
                val size = if (entry.name == "manifest.json") manifestBytes.size.toLong() else expected[entry.name]?.byteLength
                    ?: throw IOException("Undeclared ZIP payload")
                require(entry.size == size && entry.compressedSize >= 0 && entry.compressedSize <= before.size()) { "Invalid ZIP entry lengths" }
                expanded = Math.addExact(expanded, size)
                require(expanded < BackupLimits.MAX_TOTAL_BYTES)
                zip.getInputStream(entry).use { input ->
                    if (entry.name == "manifest.json") {
                        var offset = 0
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            checkpoint()
                            val read = input.read(buffer)
                            if (read < 0) break
                            if (read == 0) throw IOException("ZIP manifest reader made no progress")
                            if (read > manifestBytes.size - offset) throw IOException("ZIP manifest grew")
                            for (index in 0 until read) require(buffer[index] == manifestBytes[offset + index]) { "ZIP manifest bytes differ" }
                            offset += read
                        }
                        require(offset == manifestBytes.size)
                    } else {
                        val payload = expected.getValue(entry.name)
                        require(hash(input, payload.byteLength, checkpoint) == payload.sha256) { "ZIP payload differs from the snapshot" }
                    }
                }
            }
            require(names == expected.keys + "manifest.json") { "ZIP payload set is incomplete" }
        }
        val digest = Files.newInputStream(path, NOFOLLOW_LINKS).use { hash(it, before.size(), checkpoint) }
        val after = regularArchive(path)
        require(sameIdentity(before, after)) { "Archive changed during verification" }
        checkpoint()
        return VerifiedBackupArchive(path, before.size(), digest)
    }

    private fun requireEndDirectory(path: Path, size: Long, expectedEntries: Int, checkpoint: () -> Unit) {
        require(size >= 22)
        val end = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN)
        FileChannel.open(path, READ, NOFOLLOW_LINKS).use { channel ->
            var offset = size - 22
            while (end.hasRemaining()) {
                checkpoint()
                val count = channel.read(end, offset)
                if (count <= 0) throw IOException("Missing ZIP end directory")
                offset += count
            }
        }
        end.flip()
        require(end.int == 0x06054b50 && end.short.toInt() == 0 && end.short.toInt() == 0) { "ZIP end directory missing or multi-disk" }
        val localCount = end.short.toInt() and 65535
        val totalCount = end.short.toInt() and 65535
        val directorySize = end.int.toLong() and 0xffffffffL
        val directoryOffset = end.int.toLong() and 0xffffffffL
        require(localCount == expectedEntries && totalCount == expectedEntries && end.short.toInt() == 0)
        require(directorySize > 0 && Math.addExact(directoryOffset, directorySize) == size - 22) { "ZIP directory is truncated or has trailing bytes" }
    }

    internal fun regularArchive(path: Path): BasicFileAttributes {
        require(path == path.toAbsolutePath().normalize())
        BackupEntryFiles.requireDirectory(path.parent)
        require(Files.isRegularFile(path, NOFOLLOW_LINKS) && !Files.isSymbolicLink(path)) { "Archive is not an owned regular file" }
        return Files.readAttributes(path, BasicFileAttributes::class.java, NOFOLLOW_LINKS).also {
            require(it.size() > 0 && it.size() < BackupLimits.MAX_TOTAL_BYTES)
        }
    }

    internal fun sameIdentity(before: BasicFileAttributes, after: BasicFileAttributes): Boolean =
        before.fileKey() == after.fileKey() && before.size() == after.size() && before.lastModifiedTime() == after.lastModifiedTime()

    internal fun hash(input: InputStream, expectedLength: Long, checkpoint: () -> Unit, copyTo: OutputStream? = null): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var count = 0L
        val buffer = ByteArray(64 * 1024)
        while (true) {
            checkpoint()
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) throw IOException("Archive reader made no progress")
            count = Math.addExact(count, read.toLong())
            if (count > expectedLength) throw IOException("Archive bytes exceeded the verified length")
            digest.update(buffer, 0, read)
            copyTo?.write(buffer, 0, read)
        }
        require(count == expectedLength) { "Archive bytes are incomplete" }
        return digest.digest().hex()
    }
}

/** A provider result succeeds only after observed close plus exact whole-archive readback. */
internal object BackupTargetCopy {
    fun copy(archive: VerifiedBackupArchive, openWrite: () -> OutputStream, openRead: () -> InputStream, checkpoint: () -> Unit = {}) {
        checkpoint()
        val before = BackupArchiveFiles.regularArchive(archive.path)
        require(before.size() == archive.byteLength)
        Files.newInputStream(archive.path, NOFOLLOW_LINKS).use {
            require(BackupArchiveFiles.hash(it, archive.byteLength, checkpoint) == archive.sha256) { "Internal archive changed before target open" }
        }
        require(BackupArchiveFiles.sameIdentity(before, BackupArchiveFiles.regularArchive(archive.path)))
        openWrite().use { output ->
            Files.newInputStream(archive.path, NOFOLLOW_LINKS).use { input ->
                require(BackupArchiveFiles.hash(input, archive.byteLength, checkpoint, output) == archive.sha256) { "Internal archive changed during copy" }
            }
            output.flush()
        }
        checkpoint()
        openRead().use { input ->
            require(BackupArchiveFiles.hash(input, archive.byteLength, checkpoint) == archive.sha256) { "Closed target readback differs from the verified archive" }
        }
        require(BackupArchiveFiles.sameIdentity(before, BackupArchiveFiles.regularArchive(archive.path)))
        checkpoint()
    }
}
