package com.cooper.lithic.data.backup

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test

class CompleteBackupArchiveTest {
    private val empty = BackupSnapshot(BackupDataV1(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()), BackupSettingsV1("Asia/Shanghai"))
    private val at = Instant.parse("2026-10-20T00:00:00Z")
    private val png = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=")

    private fun mediaCore(): PreparedBackupCore {
        val entry = BackupFileEntry.fromBytes("media/synthetic.png", png)
        val media = BackupMediaAssetV1("synthetic-media", entry.path, "image/png", 1, 1, entry.byteLength, entry.sha256)
        val snapshot = empty.copy(data = empty.data.copy(mediaAssets = listOf(media)))
        val base = BackupCoreStager().prepare(snapshot, Files.createTempDirectory("lithic-archive-test"))
        Files.createDirectory(base.directory.resolve("media"))
        Files.write(base.directory.resolve(entry.path), png)
        return base.copy(entries = base.entries + entry)
    }

    @Test fun complete_archive_includes_exact_nonempty_media_and_is_verified_from_central_directory() {
        val core = mediaCore()
        val manifest = BackupManifestV1.complete(core, "synthetic", at)
        val path = core.directory.parent.resolve("${core.directory.fileName}.lithicbackup")
        val archive = BackupArchiveFiles.create(core, manifest, path)
        assertEquals(archive, BackupArchiveFiles.verify(core, manifest, path))
        assertEquals(Files.size(path), archive.byteLength)
        assertEquals(64, archive.sha256.length)
        java.util.zip.ZipFile(path.toFile()).use { zip ->
            assertEquals(setOf("manifest.json", "data.json", "settings.json", "media/synthetic.png"), zip.entries().asSequence().map { it.name }.toSet())
            assertArrayEquals(png, zip.getInputStream(zip.getEntry("media/synthetic.png")).readBytes())
        }
    }

    @Test fun required_media_must_have_exact_unique_entries_and_counts() {
        val core = mediaCore()
        expectFailure { BackupManifestV1.complete(core.copy(entries = core.entries.filterNot { it.path.startsWith("media/") }), "synthetic", at) }
        expectFailure { BackupManifestV1.complete(core.copy(entries = core.entries + core.entries.last()), "synthetic", at) }
        expectFailure { BackupManifestV1.complete(core.copy(requiredMediaPaths = emptySet()), "synthetic", at) }
        expectFailure { BackupManifestV1.complete(core.copy(recordCounts = core.recordCounts + ("local_media_assets" to 2L)), "synthetic", at) }
    }

    @Test fun missing_changed_and_over_limit_media_cannot_form_manifest() {
        val core = mediaCore()
        val path = core.directory.resolve("media/synthetic.png")
        Files.write(path, byteArrayOf(8, 8, 8, 8))
        expectFailure { BackupManifestV1.complete(core, "synthetic", at) }
        Files.delete(path)
        expectFailure { BackupManifestV1.complete(core, "synthetic", at) }
        val large = core.entries.last().copy(byteLength = BackupLimits.MAX_MEDIA_BYTES)
        expectFailure { BackupManifestV1.complete(core.copy(entries = core.entries.dropLast(1) + large), "synthetic", at) }
    }

    @Test fun truncated_central_directory_extra_entry_and_replaced_manifest_are_rejected() {
        val core = mediaCore()
        val manifest = BackupManifestV1.complete(core, "synthetic", at)
        val path = core.directory.parent.resolve("${core.directory.fileName}.lithicbackup")
        BackupArchiveFiles.create(core, manifest, path)
        val original = Files.readAllBytes(path)
        Files.write(path, original.copyOf(original.size - 22))
        expectFailure { BackupArchiveFiles.verify(core, manifest, path) }
        for (extra in listOf(true, false)) {
            ZipOutputStream(Files.newOutputStream(path)).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json")); zip.write(if (extra) manifest.encode() else "{}".toByteArray()); zip.closeEntry()
                for (entry in manifest.files) {
                    zip.putNextEntry(ZipEntry(entry.path)); zip.write(Files.readAllBytes(core.directory.resolve(entry.path))); zip.closeEntry()
                }
                if (extra) { zip.putNextEntry(ZipEntry("unregistered")); zip.write(9); zip.closeEntry() }
            }
            expectFailure { BackupArchiveFiles.verify(core, manifest, path) }
        }
    }

    @Test fun archive_path_must_be_new_private_sibling_outside_payload_tree() {
        val core = mediaCore()
        val manifest = BackupManifestV1.complete(core, "synthetic", at)
        expectFailure { BackupArchiveFiles.create(core, manifest, core.directory.resolve("output.lithicbackup")) }
        val existing = Files.createTempFile(core.directory.parent, "existing", ".lithicbackup")
        expectFailure { BackupArchiveFiles.create(core, manifest, existing) }
    }

    @Test fun target_copy_verifies_closed_readback_with_exact_whole_archive_hash() {
        val core = mediaCore()
        val manifest = BackupManifestV1.complete(core, "synthetic", at)
        val archive = BackupArchiveFiles.create(core, manifest, core.directory.parent.resolve("${core.directory.fileName}.lithicbackup"))
        var closes = 0
        val bytes = object : ByteArrayOutputStream() { override fun close() { closes++; check(closes == 1) } }
        BackupTargetCopy.copy(archive, { bytes }, { check(closes == 1); bytes.toByteArray().inputStream() })
        assertEquals(1, closes)
        assertArrayEquals(Files.readAllBytes(archive.path), bytes.toByteArray())
    }

    @Test fun target_write_close_readback_failure_or_truncation_never_returns_success() {
        val core = mediaCore()
        val manifest = BackupManifestV1.complete(core, "synthetic", at)
        val archive = BackupArchiveFiles.create(core, manifest, core.directory.parent.resolve("${core.directory.fileName}.lithicbackup"))
        expectFailure { BackupTargetCopy.copy(archive, { object : ByteArrayOutputStream() { override fun write(bytes: ByteArray, off: Int, len: Int) { throw IOException("write") } } }, { byteArrayOf().inputStream() }) }
        expectFailure { BackupTargetCopy.copy(archive, { object : ByteArrayOutputStream() { override fun flush() { throw IOException("flush") } } }, { byteArrayOf().inputStream() }) }
        expectFailure { BackupTargetCopy.copy(archive, { object : ByteArrayOutputStream() { override fun close() { throw IOException("close") } } }, { byteArrayOf().inputStream() }) }
        expectFailure { BackupTargetCopy.copy(archive, { ByteArrayOutputStream() }, { throw IOException("readback") }) }
        expectFailure { BackupTargetCopy.copy(archive, { ByteArrayOutputStream() }, { object : java.io.ByteArrayInputStream(Files.readAllBytes(archive.path)) { override fun close() { throw IOException("readback close") } } }) }
        expectFailure { BackupTargetCopy.copy(archive, { ByteArrayOutputStream() }, { Files.readAllBytes(archive.path).dropLast(1).toByteArray().inputStream() }) }
        expectFailure { BackupTargetCopy.copy(archive, { ByteArrayOutputStream() }, { Files.readAllBytes(archive.path).apply { this[0] = (this[0].toInt() xor 1).toByte() }.inputStream() }) }
    }

    @Test fun source_archive_change_is_rejected_before_target_is_opened() {
        val core = mediaCore()
        val manifest = BackupManifestV1.complete(core, "synthetic", at)
        val archive = BackupArchiveFiles.create(core, manifest, core.directory.parent.resolve("${core.directory.fileName}.lithicbackup"))
        Files.write(archive.path, byteArrayOf(1))
        var opened = false
        expectFailure { BackupTargetCopy.copy(archive, { opened = true; ByteArrayOutputStream() }, { byteArrayOf().inputStream() }) }
        assertFalse(opened)
    }

    @Test fun cancellation_checkpoint_is_observed_during_archive_and_target_streaming() {
        val core = mediaCore()
        val manifest = BackupManifestV1.complete(core, "synthetic", at)
        val path = core.directory.parent.resolve("${core.directory.fileName}.lithicbackup")
        var calls = 0
        expectFailure { BackupArchiveFiles.create(core, manifest, path) { if (++calls >= 3) throw java.util.concurrent.CancellationException("cancel") } }
        assertTrue(calls >= 3)
        val valid = BackupArchiveFiles.create(core, manifest, core.directory.parent.resolve("${core.directory.fileName}-valid.lithicbackup"))
        calls = 0
        expectFailure { BackupTargetCopy.copy(valid, { ByteArrayOutputStream() }, { byteArrayOf().inputStream() }) { if (++calls >= 3) throw java.util.concurrent.CancellationException("cancel") } }
        assertTrue(calls >= 3)
    }

    private fun expectFailure(block: () -> Unit) {
        try { block(); fail("Incomplete archive must not succeed") } catch (expected: Exception) { /* Expected IO or validation rejection. */ }
    }
}
