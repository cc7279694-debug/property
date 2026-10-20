package com.cooper.lithic.data.backup

import android.graphics.BitmapFactory
import android.os.Process
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path

/** Reads only already registered files in this owned generation; never normalizes or mutates them. */
internal class BackupMediaSnapshot(private val generation: ManagedGeneration) {
    suspend fun copy(snapshot: BackupSnapshot, core: PreparedBackupCore, checkpoint: () -> Unit = {}, onStaged: (PreparedBackupCore) -> Unit = {}): PreparedBackupCore {
        require(snapshot.data.mediaAssets.map { it.relativePath }.toSet().size == snapshot.data.mediaAssets.size)
        requireMediaSnapshotBudget(core.entries, snapshot.data.mediaAssets)
        val entries = core.entries.toMutableList()
        for (asset in snapshot.data.mediaAssets) {
            checkpoint()
            if (asset.byteLength !in 1 until BackupLimits.MAX_MEDIA_BYTES || asset.width !in 1..1600 || asset.height !in 1..1600)
                throw BackupMediaException("Registered media exceeds stored cover limits")
            // Backup validates already registered original bytes; it is not a new M10 import.
            if (asset.mime !in setOf("image/png", "image/jpeg"))
                throw BackupMediaException("Registered media format is not supported for this backup", true)
            val source = generation.directory.toPath().resolve(asset.relativePath)
            val before = ownedSource(source)
            if (before.st_size != asset.byteLength) throw BackupMediaException("Registered media length changed")
            val destination = core.directory.resolve(asset.relativePath)
            val parts = core.directory.relativize(destination.parent)
            var parent = core.directory
            for (part in parts) {
                parent = parent.resolve(part)
                if (!Files.exists(parent, NOFOLLOW_LINKS)) Files.createDirectory(parent)
                BackupEntryFiles.requireDirectory(parent)
            }
            val fd = Os.open(source.toString(), OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or CLOSE_ON_EXEC, 0)
            try {
                if (!same(before, Os.fstat(fd))) throw BackupMediaException("Source identity changed during open")
                val entry = FileInputStream(fd).use { input ->
                    BackupEntryFiles.write(core.directory, asset.relativePath, BackupLimits.MAX_MEDIA_BYTES) { output ->
                        val bytes = ByteArray(64*1024)
                        while (true) {
                            checkpoint()
                            val n = input.read(bytes)
                            if (n < 0) break
                            output.write(bytes,0,n)
                        }
                        if (!same(before, Os.fstat(fd)) || !same(before, ownedSource(source)))
                            throw BackupMediaException("Source changed while copying")
                    }
                }
                entries.add(entry)
                require(entries.fold(0L) { sum, item -> Math.addExact(sum, item.byteLength) } < BackupLimits.MAX_TOTAL_BYTES)
                onStaged(core.copy(entries = entries.toList()))
                if (entry.byteLength != asset.byteLength || !entry.sha256.equals(asset.sha256, ignoreCase=true))
                    throw BackupMediaException("Registered media checksum does not match its actual bytes")
                val dimensions = when (asset.mime) {
                    "image/png" -> BackupPngInspection.inspect(destination.toFile(), checkpoint)
                    "image/jpeg" -> BackupJpegInspection.inspect(destination.toFile(), checkpoint)
                    else -> throw BackupMediaException("Unsupported registered media format", true)
                }
                if (dimensions != (asset.width to asset.height)) throw BackupMediaException("Registered dimensions changed")
                // Readability and actual raster size only. Android JPEG decode is tolerant and
                // MUST NOT be called a strict entropy-integrity certificate.
                val options = BitmapFactory.Options().apply { inScaled = false }
                val bitmap = BitmapFactory.decodeFile(destination.toString(), options)
                    ?: throw BackupMediaException("Stored image cannot be read")
                try {
                    if (bitmap.width != asset.width || bitmap.height != asset.height || options.outMimeType != asset.mime)
                        throw BackupMediaException("Image decoder disagrees with recorded dimensions or format")
                } finally { bitmap.recycle() }
            } finally { if (fd.valid()) Os.close(fd) }
        }
        return core.copy(entries = entries.toList())
    }

    private fun ownedSource(path: Path): StructStat {
        val root = generation.directory.toPath().toAbsolutePath().normalize()
        if (generation.directory.canonicalFile != generation.directory.absoluteFile || !path.normalize().startsWith(root.resolve("media")))
            throw BackupMediaException("Media source leaves the owned generation")
        var node = root
        for (part in root.relativize(path)) {
            val directory = Os.lstat(node.toString())
            if (!OsConstants.S_ISDIR(directory.st_mode) || directory.st_uid != Process.myUid())
                throw BackupMediaException("Media directory ownership is unknown")
            node = node.resolve(part)
        }
        val stat = Os.lstat(path.toString())
        if (!OsConstants.S_ISREG(stat.st_mode) || stat.st_uid != Process.myUid() || stat.st_nlink != 1L)
            throw BackupMediaException("Media file ownership is unknown")
        return stat
    }
    private fun same(a: StructStat,b: StructStat) = a.st_dev==b.st_dev && a.st_ino==b.st_ino &&
        a.st_uid==b.st_uid && a.st_mode==b.st_mode && a.st_size==b.st_size && a.st_nlink==b.st_nlink &&
        a.st_mtime==b.st_mtime && a.st_ctime==b.st_ctime

    private companion object {
        // Linux Android UAPI: asm-generic/fcntl.h O_CLOEXEC 02000000 (NDK r28c).
        // The public Java constant arrived in API27 and fcntlInt in API30. Pass the
        // verified ABI flag to API21 Os.open, so API26 opens atomically close-on-exec too.
        const val CLOSE_ON_EXEC = 0x80000
    }
}
