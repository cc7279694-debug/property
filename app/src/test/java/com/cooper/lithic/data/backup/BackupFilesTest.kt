package com.cooper.lithic.data.backup

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.time.Instant
import java.util.zip.ZipInputStream
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class BackupFilesTest {
    private val empty = BackupSnapshot(BackupDataV1(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()), BackupSettingsV1("Asia/Shanghai"))
    private val at = Instant.parse("2026-10-20T00:00:00Z")

    @Test fun closed_core_files_have_hashes_of_actual_utf8_bytes_and_no_complete_manifest() {
        val parent = Files.createTempDirectory("lithic-core-test")
        val core = BackupCoreStager().prepare(empty, parent)
        assertEquals(setOf("data.json", "settings.json"), core.entries.map { it.path }.toSet())
        core.entries.forEach { assertEquals(it, BackupEntryFiles.verify(core.directory, it)) }
        assertFalse(Files.exists(core.directory.resolve("manifest.json")))
        assertEquals(empty.completeRecordCounts(), core.recordCounts)
    }

    @Test fun changing_staged_bytes_is_detected_instead_of_trusting_old_metadata() {
        val core = BackupCoreStager().prepare(empty, Files.createTempDirectory("lithic-mutate-test"))
        Files.write(core.directory.resolve("data.json"), "bad".toByteArray())
        expectFailure { BackupEntryFiles.verify(core.directory, core.entries.first { it.path == "data.json" }) }
    }

    @Test fun entry_write_and_close_failure_do_not_return_verified_entries() {
        for (closeFails in listOf(false, true)) {
            val directory = Files.createTempDirectory("lithic-io-test")
            val failing = object : OutputStream() {
                override fun write(b: Int) { if (!closeFails) throw IOException("write failed") }
                override fun close() { if (closeFails) throw IOException("close failed") }
            }
            expectFailure { BackupEntryFiles.write(directory, "data.json", 128, { failing }) { it.write("{}".toByteArray()) } }
        }
    }

    @Test fun path_escape_duplicate_and_limit_are_rejected() {
        val directory = Files.createTempDirectory("lithic-path-test")
        for (bad in listOf("../data.json", "/data.json", "media\\x", "media/../x", "media//x")) {
            expectFailure { BackupEntryFiles.write(directory, bad, 100) { it.write(1) } }
        }
        expectFailure { BackupEntryFiles.write(directory, "data.json", 3) { it.write(byteArrayOf(1, 2, 3)) } }
        BackupEntryFiles.write(directory, "settings.json", 100) { it.write(1) }
        expectFailure { BackupEntryFiles.write(directory, "settings.json", 100) { it.write(2) } }
    }

    @Test fun manifest_v1_has_stable_required_fields_and_payload_hashes() {
        val core = BackupCoreStager().prepare(empty, Files.createTempDirectory("lithic-manifest-test"))
        val manifest = BackupManifestV1.complete(core, "0.1.0-m09", at)
        val json = Json.parseToJsonElement(manifest.encode().decodeToString()).toString()
        for (field in listOf("formatVersion", "minReaderFormat", "producerAppVersion", "sourceDatabaseSchemaVersion", "createdAt", "recordZoneId", "recordCounts", "files")) assertTrue(json.contains("\"$field\""))
        // Retain the historical method identity; M20 explicitly advances current export.
        assertEquals(3, manifest.formatVersion)
        assertEquals(4, manifest.sourceDatabaseSchemaVersion)
        assertEquals(at.toString(), manifest.createdAt)
        assertFalse(manifest.files.any { it.path == "manifest.json" })
    }

    @Test fun archive_bytes_match_manifest_and_target_close_failure_is_failure() {
        val core = BackupCoreStager().prepare(empty, Files.createTempDirectory("lithic-zip-test"))
        val manifest = BackupManifestV1.complete(core, "synthetic-test", at)
        val target = ByteArrayOutputStream()
        BackupArchiveWriter.write(core, manifest, target)
        val payloads = mutableMapOf<String, ByteArray>()
        ZipInputStream(target.toByteArray().inputStream()).use { zip ->
            while (true) { val e = zip.nextEntry ?: break; payloads[e.name] = zip.readBytes() }
        }
        assertEquals(setOf("manifest.json", "data.json", "settings.json"), payloads.keys)
        for (entry in manifest.files) assertEquals(entry, BackupFileEntry.fromBytes(entry.path, payloads.getValue(entry.path)))
        val broken = object : ByteArrayOutputStream() { override fun close() { throw IOException("target close") } }
        expectFailure { BackupArchiveWriter.write(core, manifest, broken) }
    }

    @Test fun archive_owns_one_target_close_and_propagates_target_write_failure() {
        val core = BackupCoreStager().prepare(empty, Files.createTempDirectory("lithic-target-test"))
        val manifest = BackupManifestV1.complete(core, "synthetic-test", at)
        var closed = 0
        val once = object : ByteArrayOutputStream() {
            override fun close() { closed++; check(closed == 1) }
        }
        BackupArchiveWriter.write(core, manifest, once)
        assertEquals(1, closed)
        val broken = object : OutputStream() { override fun write(value: Int) { throw IOException("target write") } }
        expectFailure { BackupArchiveWriter.write(core, manifest, broken) }
    }

    @Test fun media_not_integrated_blocks_complete_export_before_opening_target_even_for_empty_ledger() = runBlocking {
        var opened = 0
        val exporter = BackupExporter(BackupSnapshotSource { empty })
        val result = exporter.exportComplete { opened++; ByteArrayOutputStream() }
        assertEquals(BackupExportReadiness.MEDIA_INTEGRATION_REQUIRED, result)
        assertEquals(0, opened)
    }

    @Test fun media_metadata_cannot_be_mistaken_for_copied_verified_media() {
        val core = BackupCoreStager().prepare(empty, Files.createTempDirectory("lithic-media-test"))
        val incomplete = core.copy(recordCounts = core.recordCounts + ("local_media_assets" to 1L))
        expectFailure { BackupManifestV1.complete(incomplete, "synthetic-test", at) }
    }

    @Test fun successful_internal_candidate_can_release_only_its_owned_files() {
        val core = BackupCoreStager().prepare(empty, Files.createTempDirectory("lithic-release-test"))
        assertTrue("Internal preparation needs an explicit lifetime", AutoCloseable::class.java.isInstance(core))
        (core as Any as AutoCloseable).close()
        assertFalse(Files.exists(core.directory))
    }

    @Test fun unknown_residue_blocks_candidate_cleanup_without_deleting_any_files() {
        val core = BackupCoreStager().prepare(empty, Files.createTempDirectory("lithic-residue-test"))
        Files.write(core.directory.resolve("unknown"), byteArrayOf(9))
        assertTrue("Internal preparation needs an explicit lifetime", AutoCloseable::class.java.isInstance(core))
        expectFailure { (core as Any as AutoCloseable).close() }
        assertTrue(Files.isRegularFile(core.directory.resolve("data.json")))
        assertTrue(Files.isRegularFile(core.directory.resolve("unknown")))
    }

    @Test fun changed_internal_ownership_credential_cannot_delete_payloads() {
        val core = BackupCoreStager().prepare(empty, Files.createTempDirectory("lithic-owner-test"))
        Files.write(core.directory.resolve(".backup-core-owner"), "foreign".toByteArray())
        expectFailure { core.close() }
        assertTrue(Files.isRegularFile(core.directory.resolve("data.json")))
    }

    private fun expectFailure(block: () -> Unit) {
        try { block(); fail("Operation must fail without a success result") } catch (expected: Exception) { /* expected */ }
    }
}
