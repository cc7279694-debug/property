package com.cooper.lithic.data.backup

import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class BackupExportReadiness { MEDIA_INTEGRATION_REQUIRED }

/** No UI or SAF binding: partial internal artifacts cannot become a user-visible complete backup. */
internal class BackupExporter(private val source: BackupSnapshotSource) {
    suspend fun prepareCore(privateStagingParent: Path): PreparedBackupCore = withContext(Dispatchers.IO) {
        BackupCoreStager().prepare(source.capture(), privateStagingParent)
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun exportComplete(openTarget: () -> OutputStream): BackupExportReadiness =
        BackupExportReadiness.MEDIA_INTEGRATION_REQUIRED
}

/** Internal archive IO core, used after the coordinated media snapshot has been verified. */
internal object BackupArchiveWriter {
    fun write(core: PreparedBackupCore, manifest: BackupManifestV1, target: OutputStream, checkpoint: () -> Unit = {}) {
        // Caller gave ownership of target. Even validation/finish/close failures cannot return success.
        target.use { destination ->
            checkpoint()
            val expected = BackupManifestV1.complete(core, manifest.producerAppVersion, java.time.Instant.parse(manifest.createdAt))
            require(manifest == expected) { "Manifest does not describe this snapshot" }
            val bytes = manifest.encode()
            require(core.entries.size + 1 < BackupLimits.MAX_ENTRIES)
            require(core.entries.fold(bytes.size.toLong()) { total, entry -> Math.addExact(total, entry.byteLength) } < BackupLimits.MAX_TOTAL_BYTES)
            val borrowed = object : java.io.FilterOutputStream(destination) {
                private var written = 0L
                override fun write(value: Int) = write(byteArrayOf(value.toByte()), 0, 1)
                override fun write(bytes: ByteArray, offset: Int, length: Int) {
                    val next = Math.addExact(written, length.toLong())
                    if (next >= BackupLimits.MAX_TOTAL_BYTES) throw IOException("Archive reached its output resource limit")
                    out.write(bytes, offset, length)
                    written = next
                }
                override fun close() = flush() // Only target.use owns the real target close.
            }
            ZipOutputStream(borrowed).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json").apply { time = 0 })
                zip.write(bytes); zip.closeEntry()
                for (entry in manifest.files) {
                    checkpoint()
                    BackupEntryFiles.verify(core.directory, entry)
                    zip.putNextEntry(ZipEntry(entry.path).apply { time = 0 })
                    val digest = java.security.MessageDigest.getInstance("SHA-256")
                    var count = 0L
                    Files.newInputStream(core.directory.resolve(entry.path), NOFOLLOW_LINKS).use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            checkpoint()
                            val read = input.read(buffer)
                            if (read < 0) break
                            if (read == 0) throw IOException("Staged reader made no progress")
                            count = Math.addExact(count, read.toLong())
                            if (count > entry.byteLength) throw IOException("Staged entry grew")
                            digest.update(buffer, 0, read); zip.write(buffer, 0, read)
                        }
                    }
                    val actual = BackupFileEntry(entry.path, count, with(BackupEntryFiles) { digest.digest().hex() })
                    require(actual == entry) { "Payload changed while writing target" }
                    zip.closeEntry()
                }
                zip.finish(); zip.flush()
            }
        }
    }
}
