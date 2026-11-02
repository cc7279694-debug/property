package com.cooper.lithic.data.backup

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.CharacterCodingException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

/** Host ZIP/PNG integrity evidence only; never Android SAF, decoding or restore activation. */
class BackupInspectionServiceTest {
    @Test fun a_valid_complete_backup_returns_only_verified_facts_and_cleans_its_own_preparation() = runBlocking {
        val f = fixture()
        val result = f.inspect()
        assertEquals(BackupInspectionStatus.VERIFIED, result.status)
        assertEquals(4, result.summary!!.formatVersion)
        assertEquals(1L, result.summary.mediaCount)
        assertEquals(PNG.size.toLong(), result.summary.mediaBytes)
        assertEquals(1L, result.summary.recordCounts.getValue("long_term_projects"))
        assertEquals(1L, result.summary.recordCounts.getValue("local_media_assets"))
        assertEquals(AT, result.summary.createdAt)
        assertFalse(result.temporaryFilesRetained)
        assertEquals(0L, Files.list(f.cache).use { it.count() })
    }

    @Test fun checked_old_unreferenced_photos_are_included_in_the_complete_backup_summary() = runBlocking {
        val f = fixture(retainedOld = true)
        val result = f.inspect()
        assertEquals(BackupInspectionStatus.VERIFIED, result.status)
        assertEquals(2L, result.summary!!.mediaCount)
        assertEquals(PNG.size.toLong() * 2, result.summary.mediaBytes)
        assertEquals(2L, result.summary.recordCounts.getValue("local_media_assets"))
    }

    @Test fun the_first_check_creates_only_its_missing_single_private_cache_child() = runBlocking {
        val f = fixture()
        val missing = f.parent.resolve("new-inspection-cache")
        assertFalse(Files.exists(missing))
        val service = BackupInspectionService(missing)
        val result = service.inspect({ Files.newInputStream(f.archive) })
        assertEquals(BackupInspectionStatus.VERIFIED, result.status)
        assertTrue(Files.isDirectory(missing))
        assertEquals(0L, Files.list(missing).use { it.count() })
    }

    @Test fun missing_registered_media_is_invalid_and_retains_the_failed_preparation() = runBlocking {
        val f = fixture(); val changed = f.repack(omit = MEDIA_PATH)
        val result = f.inspect(changed)
        assertEquals(BackupInspectionStatus.INVALID, result.status)
        assertNull(result.summary)
        assertTrue(result.temporaryFilesRetained)
        assertEquals(1L, Files.list(f.cache).use { it.count() })
    }

    @Test fun a_wrong_declared_sha_is_invalid_without_changing_the_selected_archive() = runBlocking {
        val f = fixture()
        val changed = f.repack(manifestText = { it.replace(f.manifest.files.single { e -> e.path == MEDIA_PATH }.sha256, "0".repeat(64)) })
        val before = Files.readAllBytes(changed)
        assertEquals(BackupInspectionStatus.INVALID, f.inspect(changed).status)
        assertArrayEquals(before, Files.readAllBytes(changed))
    }

    @Test fun corrupt_zip_bytes_cannot_be_reported_as_verified() = runBlocking {
        val f = fixture()
        val changed = f.parent.resolve("corrupt.zip")
        Files.write(changed, Files.readAllBytes(f.archive).dropLast(22).toByteArray())
        assertEquals(BackupInspectionStatus.INVALID, f.inspect(changed).status)
    }

    @Test fun a_self_consistent_sha_does_not_certify_corrupt_png_content() = runBlocking {
        val bad = PNG.copyOf().also { it[44] = (it[44].toInt() xor 1).toByte() }
        val f = fixture(png = bad)
        assertEquals(BackupInspectionStatus.INVALID, f.inspect().status)
    }

    @Test fun self_consistent_declared_dimensions_must_match_the_actual_media_container() = runBlocking {
        val f = fixture(width = 2)
        assertEquals(BackupInspectionStatus.INVALID, f.inspect().status)
    }

    @Test fun legal_uppercase_media_metadata_sha_keeps_the_existing_case_insensitive_contract() = runBlocking {
        val f = fixture(uppercaseMetadataSha = true)
        val result = f.inspect()
        assertEquals(BackupInspectionStatus.VERIFIED, result.status)
        assertEquals(1L, result.summary!!.mediaCount)
    }

    @Test fun unknown_format_is_unsupported_rather_than_a_success_or_generic_integrity_pass() = runBlocking {
        val f = fixture()
        val future = f.repack(manifestText = { it.replace("\"formatVersion\":4", "\"formatVersion\":5") })
        assertEquals(BackupInspectionStatus.UNSUPPORTED, f.inspect(future).status)
    }

    @Test fun inconsistent_min_reader_or_source_schema_is_unsupported() = runBlocking {
        for (field in listOf("minReaderFormat", "sourceDatabaseSchemaVersion")) {
            val f = fixture()
            val changed = f.repack(manifestText = { it.replace("\"$field\":${if (field == "minReaderFormat") 4 else 5}", "\"$field\":99") })
            assertEquals(BackupInspectionStatus.UNSUPPORTED, f.inspect(changed).status)
        }
    }

    @Test fun a_self_consistent_archive_with_broken_record_relationships_is_invalid() = runBlocking {
        val f = fixture()
        val changed = f.repack(dataText = { it.replace("\"categoryId\":\"${id(1)}\"", "\"categoryId\":\"${id(999)}\"") })
        assertEquals(BackupInspectionStatus.INVALID, f.inspect(changed).status)
    }

    @Test fun inspection_does_not_change_existing_business_fixture_or_cover_bytes() = runBlocking {
        val f = fixture()
        val ledger = Files.write(f.parent.resolve("synthetic-existing-ledger"), byteArrayOf(1, 8, 3, 9))
        val cover = Files.write(f.parent.resolve("synthetic-existing-cover.png"), PNG)
        val source = Files.readAllBytes(f.archive)
        assertEquals(BackupInspectionStatus.VERIFIED, f.inspect().status)
        assertArrayEquals(byteArrayOf(1, 8, 3, 9), Files.readAllBytes(ledger))
        assertArrayEquals(PNG, Files.readAllBytes(cover))
        assertArrayEquals(source, Files.readAllBytes(f.archive))
    }

    @Test fun provider_open_illegal_argument_is_unknown_not_invalid() = runBlocking {
        assertProviderFailure(ProviderStage.OPEN, IllegalArgumentException("synthetic provider open"))
    }

    @Test fun provider_open_zip_exception_is_unknown_not_invalid() = runBlocking {
        assertProviderFailure(ProviderStage.OPEN, ZipException("synthetic provider open"))
    }

    @Test fun provider_open_character_coding_exception_is_unknown_not_invalid() = runBlocking {
        assertProviderFailure(ProviderStage.OPEN, CharacterCodingException())
    }

    @Test fun provider_read_illegal_argument_is_unknown_not_invalid() = runBlocking {
        assertProviderFailure(ProviderStage.READ, IllegalArgumentException("synthetic provider read"))
    }

    @Test fun provider_read_zip_exception_is_unknown_not_invalid() = runBlocking {
        assertProviderFailure(ProviderStage.READ, ZipException("synthetic provider read"))
    }

    @Test fun provider_read_character_coding_exception_is_unknown_not_invalid() = runBlocking {
        assertProviderFailure(ProviderStage.READ, CharacterCodingException())
    }

    @Test fun provider_close_illegal_argument_after_valid_zip_copy_is_unknown_not_invalid() = runBlocking {
        assertProviderFailure(ProviderStage.CLOSE, IllegalArgumentException("synthetic provider close"))
    }

    @Test fun provider_close_zip_exception_after_valid_zip_copy_is_unknown_not_invalid() = runBlocking {
        assertProviderFailure(ProviderStage.CLOSE, ZipException("synthetic provider close"))
    }

    @Test fun provider_close_character_coding_exception_after_valid_zip_copy_is_unknown_not_invalid() = runBlocking {
        assertProviderFailure(ProviderStage.CLOSE, CharacterCodingException())
    }

    @Test fun an_input_provider_read_failure_is_unknown_and_is_not_retried() = runBlocking {
        val f = fixture(); var attempts = 0
        val result = f.service.inspect({ attempts++; throw IOException("private/path/must/not/escape") })
        assertEquals(BackupInspectionStatus.UNKNOWN, result.status)
        assertNull(result.summary)
        assertEquals(1, attempts)
        assertFalse(result.toString().contains("private/path"))
    }

    @Test fun an_input_close_failure_keeps_outcome_unknown() = runBlocking {
        val f = fixture()
        val result = f.service.inspect({ object : java.io.ByteArrayInputStream(Files.readAllBytes(f.archive)) {
            override fun close() { throw IOException("close") }
        } })
        assertEquals(BackupInspectionStatus.UNKNOWN, result.status)
        assertTrue(result.temporaryFilesRetained)
    }

    @Test fun cancellation_preserves_preparation_and_returns_unknown_without_retry() = runBlocking {
        val f = fixture(); var reads = 0
        val result = f.service.inspect({ object : InputStream() {
            override fun read(): Int { reads++; throw CancellationException("cancelled") }
        } })
        assertEquals(BackupInspectionStatus.UNKNOWN, result.status)
        assertTrue(result.temporaryFilesRetained)
        assertEquals(1, reads)
    }

    @Test fun an_old_owner_is_rejected_before_the_document_is_opened() = runBlocking {
        val f = fixture(); var opened = false
        val result = f.service.inspect({ opened = true; Files.newInputStream(f.archive) }, stillCurrent = { false })
        assertEquals(BackupInspectionStatus.UNKNOWN, result.status)
        assertFalse(opened)
        assertFalse(result.temporaryFilesRetained)
    }

    @Test fun owner_change_during_input_cannot_publish_a_verified_old_generation_result() = runBlocking {
        val f = fixture(); var current = true
        val result = f.service.inspect({ object : java.io.ByteArrayInputStream(Files.readAllBytes(f.archive)) {
            override fun read(bytes: ByteArray, off: Int, len: Int): Int = super.read(bytes, off, len).also { current = false }
        } }, stillCurrent = { current })
        assertEquals(BackupInspectionStatus.UNKNOWN, result.status)
        assertNull(result.summary)
        assertTrue(result.temporaryFilesRetained)
    }

    @Test fun three_unknown_residual_sessions_block_a_new_read_without_deleting_residue() = runBlocking {
        val f = fixture()
        repeat(3) { assertEquals(BackupInspectionStatus.INVALID, f.inspect(f.parent.resolve("bad-$it.zip").also { p -> Files.write(p, byteArrayOf(1)) }).status) }
        val before = Files.list(f.cache).use { it.map { p -> p.fileName.toString() }.sorted().toList() }
        var opened = false
        val result = f.service.inspect({ opened = true; Files.newInputStream(f.archive) })
        assertEquals(BackupInspectionStatus.UNKNOWN, result.status)
        assertFalse(opened)
        assertEquals(before, Files.list(f.cache).use { it.map { p -> p.fileName.toString() }.sorted().toList() })
    }

    @Test fun successful_checks_do_not_accumulate_a_permanent_attempt_limit() = runBlocking {
        val f = fixture()
        repeat(4) { assertEquals(BackupInspectionStatus.VERIFIED, f.inspect().status) }
        assertEquals(0L, Files.list(f.cache).use { it.count() })
    }

    @Test fun a_duplicate_request_is_not_opened_or_queued_behind_an_active_document_read() = runBlocking {
        val f = fixture()
        val opened = CompletableDeferred<Unit>()
        val release = java.util.concurrent.CountDownLatch(1)
        val first = async(Dispatchers.Default) { f.service.inspect({
            opened.complete(Unit)
            check(release.await(5, java.util.concurrent.TimeUnit.SECONDS))
            Files.newInputStream(f.archive)
        }) }
        try {
            withTimeout(5_000) { opened.await() }
            var duplicateOpened = false
            val duplicate = f.service.inspect({ duplicateOpened = true; Files.newInputStream(f.archive) })
            assertEquals(BackupInspectionStatus.UNKNOWN, duplicate.status)
            assertFalse(duplicateOpened)
        } finally { release.countDown() }
        assertEquals(BackupInspectionStatus.VERIFIED, first.await().status)
    }

    @Test fun unknown_extra_preparation_children_are_preserved_without_recursive_cleanup() = runBlocking {
        val f = fixture()
        var unknown: Path? = null
        val result = f.service.inspect({ Files.newInputStream(f.archive) }, stillCurrent = {
            val directory = Files.list(f.cache).use { it.findFirst().orElse(null) }
            if (unknown == null && directory != null && Files.exists(directory.resolve("payload/$MEDIA_PATH"))) {
                val child = Files.createDirectory(directory.resolve("unknown-child"))
                unknown = Files.write(child.resolve("do-not-delete"), byteArrayOf(8, 6, 7))
            }
            true
        })
        assertEquals(BackupInspectionStatus.VERIFIED, result.status)
        assertTrue(result.temporaryFilesRetained)
        assertNotNull(unknown)
        assertArrayEquals(byteArrayOf(8, 6, 7), Files.readAllBytes(unknown!!))
        assertTrue(Files.exists(unknown!!.parent.parent.resolve("source.lithicbackup")))
    }

    private enum class ProviderStage { OPEN, READ, CLOSE }

    private suspend fun assertProviderFailure(stage: ProviderStage, failure: Exception) {
        // A fresh fixture avoids reaching the original three-residual-session admission limit.
        val f = fixture()
        val ledger = Files.write(f.parent.resolve("synthetic-existing-ledger"), byteArrayOf(1, 8, 3, 9))
        val cover = Files.write(f.parent.resolve("synthetic-existing-cover.png"), PNG)
        val archiveBefore = Files.readAllBytes(f.archive)
        var opened = 0
        var reads = 0
        var closed = 0
        val result = f.service.inspect({
            opened++
            if (stage == ProviderStage.OPEN) throw failure
            object : java.io.ByteArrayInputStream(archiveBefore) {
                override fun read(bytes: ByteArray, off: Int, len: Int): Int {
                    reads++
                    if (stage == ProviderStage.READ) throw failure
                    return super.read(bytes, off, len)
                }
                override fun close() {
                    closed++
                    if (stage == ProviderStage.CLOSE) throw failure
                    super.close()
                }
            }
        })
        assertEquals(BackupInspectionStatus.UNKNOWN, result.status)
        assertNull(result.summary)
        assertTrue(result.temporaryFilesRetained)
        assertEquals(1, opened)
        assertEquals(if (stage == ProviderStage.OPEN) 0 else 1, closed)
        if (stage == ProviderStage.OPEN) assertEquals(0, reads)
        if (stage == ProviderStage.READ) assertEquals(1, reads)
        val operation = Files.list(f.cache).use { it.toList().single() }
        assertTrue(Files.isRegularFile(operation.resolve(".inspection-owner")))
        assertFalse(Files.exists(operation.resolve("payload")))
        if (stage == ProviderStage.CLOSE) {
            assertTrue(reads >= 2) // Full valid bytes and EOF were read before close failed.
            assertArrayEquals(archiveBefore, Files.readAllBytes(operation.resolve("source.lithicbackup")))
        }
        // Synthetic byte fixtures only: this is not Android Room/SAF execution evidence.
        assertArrayEquals(byteArrayOf(1, 8, 3, 9), Files.readAllBytes(ledger))
        assertArrayEquals(PNG, Files.readAllBytes(cover))
        assertArrayEquals(archiveBefore, Files.readAllBytes(f.archive))
    }

    private class Fixture(val parent: Path, val cache: Path, val archive: Path, val manifest: BackupManifestV1) {
        val service = BackupInspectionService(cache)
        suspend fun inspect(path: Path = archive) = service.inspect({ Files.newInputStream(path) })
        fun repack(omit: String? = null, manifestText: (String) -> String = { it }, dataText: ((String) -> String)? = null): Path {
            val parts = ZipFile(archive.toFile()).use { zip -> zip.entries().asSequence().associate { e -> e.name to zip.getInputStream(e).use { it.readBytes() } }.toMutableMap() }
            var header = manifest
            if (dataText != null) {
                val changed = dataText(parts.getValue("data.json").toString(Charsets.UTF_8)).toByteArray()
                parts["data.json"] = changed
                header = header.copy(files = header.files.map { if (it.path == "data.json") BackupFileEntry.fromBytes(it.path, changed) else it })
            }
            parts["manifest.json"] = manifestText(header.encode().toString(Charsets.UTF_8)).toByteArray()
            if (omit != null) parts.remove(omit)
            val path = Files.createTempFile(parent, "changed-", ".zip")
            ZipOutputStream(Files.newOutputStream(path)).use { zip -> parts.forEach { (name, bytes) -> zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() } }
            return path
        }
    }

    private fun fixture(retainedOld: Boolean = false, png: ByteArray = PNG, width: Int = 1, uppercaseMetadataSha: Boolean = false): Fixture {
        val parent = Files.createTempDirectory("lithic-b1-inspection-test")
        val cache = Files.createDirectory(parent.resolve("inspection-cache"))
        val entry = BackupFileEntry.fromBytes(MEDIA_PATH, png)
        val old = BackupFileEntry.fromBytes("media/retained.png", PNG)
        val assets = listOf(BackupMediaAssetV1(id(20), MEDIA_PATH, "image/png", width, 1, entry.byteLength,
            if (uppercaseMetadataSha) entry.sha256.uppercase(java.util.Locale.ROOT) else entry.sha256)) +
            if (retainedOld) listOf(BackupMediaAssetV1(id(21), old.path, "image/png", 1, 1, old.byteLength, old.sha256)) else emptyList()
        val snapshot = BackupSnapshot(BackupDataV1(
            listOf(BackupCategoryV1(id(1), "PROJECT", "其他", "其他", true), BackupCategoryV1(id(2), "SUBSCRIPTION", "其他", "其他", true)),
            listOf(BackupProjectV1(id(10), "合成物品", id(1), "2026-01-01", null, null, id(20), null, AT, AT)),
            listOf(BackupProjectMoneyEventV1(id(11), id(10), "OUTLAY", 100, "2026-01-01", null, "INITIAL", id(12))),
            assets, emptyList(), emptyList(), emptyList(),
        ), BackupSettingsV1("Asia/Shanghai"))
        val base = BackupCoreStager().prepare(snapshot, parent)
        Files.createDirectory(base.directory.resolve("media")); Files.write(base.directory.resolve(MEDIA_PATH), png)
        if (retainedOld) Files.write(base.directory.resolve(old.path), PNG)
        val core = base.copy(entries = base.entries + entry + if (retainedOld) listOf(old) else emptyList())
        val manifest = BackupManifestV1.complete(core, "B1 synthetic test", java.time.Instant.parse(AT))
        // These test archives intentionally include variants that the new media inspection must reject.
        val archive = parent.resolve("selected.lithicbackup")
        ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
            val entries = mapOf("manifest.json" to manifest.encode()) + manifest.files.associate { it.path to Files.readAllBytes(core.directory.resolve(it.path)) }
            entries.forEach { (name, bytes) -> zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() }
        }
        return Fixture(parent, cache, archive, manifest)
    }

    companion object {
        private const val AT = "2026-10-31T00:00:00Z"
        private const val MEDIA_PATH = "media/registered.png"
        private val PNG = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=")
        private fun id(value: Int) = "00000000-0000-4000-8000-${value.toString().padStart(12, '0')}"
    }
}
