package com.cooper.lithic.data.backup

import java.nio.file.Files
import java.nio.file.Path
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption.WRITE
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

/** Real format-1 archives; malformed variants keep correct checksums unless that is the defect. */
class BackupValidatorTest {
    private val at = "2026-01-01T00:00:00Z"
    private val png = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=")
    private val mediaEntry = BackupFileEntry.fromBytes("media/existing-safe-name.png", png)
    private val data = BackupDataV1(
        listOf(BackupCategoryV1(id(1), "PROJECT", "其他", "其他", true), BackupCategoryV1(id(2), "SUBSCRIPTION", "其他", "其他", true)),
        listOf(BackupProjectV1(id(10), "相机", id(1), "2025-01-01", null, null, id(20), "原始备注\n", at, at)),
        listOf(BackupProjectMoneyEventV1(id(11), id(10), "OUTLAY", 0, "2025-01-01", null, "INITIAL", id(12))),
        listOf(BackupMediaAssetV1(id(20), mediaEntry.path, "image/png", 1, 1, mediaEntry.byteLength, mediaEntry.sha256)),
        listOf(BackupSubscriptionV1(id(30), "服务", id(2), id(20), null, at, at)),
        listOf(BackupSubscriptionRenewalV1(id(31), id(30), "2025-12-01", "2026-01-01", "2026-02-01", "2026-01-29", 15000, "MONTHLY", 30, id(12))),
        // Operation IDs are unique per table, not across three unrelated unique indices.
        listOf(BackupSubscriptionStateChangeV1(id(32), id(30), "2026-01-10", "PAUSED", null, null, id(12))),
    )

    @Test fun restores_all_facts_shared_media_zero_initial_and_cross_table_operation_ids() {
        val archive = archive()
        val directory = Files.createTempDirectory("lithic-import")
        val validated = validate(archive, directory)
        assertEquals(BackupSnapshot(data.ordered(), BackupSettingsV1("Asia/Shanghai")), validated.snapshot)
        assertEquals(directory, validated.directory)
        assertArrayEquals(png, Files.readAllBytes(directory.resolve(mediaEntry.path)))
        assertEquals(64, validated.archiveSha256.length)
    }

    @Test fun validates_archive_created_by_existing_m11_writer() {
        val parent = Files.createTempDirectory("lithic-m11-roundtrip")
        val core = BackupCoreStager().prepare(BackupSnapshot(data, BackupSettingsV1("Asia/Shanghai")), parent)
        Files.createDirectories(core.directory.resolve("media"))
        Files.write(core.directory.resolve(mediaEntry.path), png)
        val complete = core.copy(entries = core.entries + mediaEntry)
        val manifest = BackupManifestV1.complete(complete, "synthetic-m11", Instant.parse(at))
        val zip = BackupArchiveFiles.create(complete, manifest, parent.resolve("complete.lithicbackup"))
        val result = validate(zip.path, Files.createTempDirectory("lithic-import"))
        assertEquals(zip.sha256, result.archiveSha256)
    }

    @Test fun accepts_arbitrary_table_row_order_and_returns_canonical_facts_for_room_comparison() {
        val zip = archive(dataText = { text ->
            val root = Json.parseToJsonElement(text).jsonObject
            JsonObject(root + ("categories" to JsonArray(root.getValue("categories").jsonArray.reversed()))).toString()
        })
        val result = validate(zip, Files.createTempDirectory("lithic-import"))
        assertEquals(listOf(id(1), id(2)), result.snapshot.data.categories.map { it.id })
        assertEquals(data.ordered(), result.snapshot.data)
    }

    @Test fun refuses_new_format_reader_schema_and_missing_defaulted_manifest_fields() {
        listOf(
            { s: String -> s.replace("\"formatVersion\":1", "\"formatVersion\":2") },
            { s: String -> s.replace("\"minReaderFormat\":1", "\"minReaderFormat\":2") },
            { s: String -> s.replace("\"sourceDatabaseSchemaVersion\":2", "\"sourceDatabaseSchemaVersion\":3") },
            { s: String -> s.replace("\"formatVersion\":1,", "") },
            { s: String -> s.replace("\"minReaderFormat\":1,", "") },
        ).forEach { change -> rejected(archive(manifestText = change)) }
    }

    @Test fun rejects_duplicate_json_keys_unknown_fields_missing_nullable_fields_and_invalid_utf8() {
        rejected(archive(dataText = { it.replace("\"amountCents\":0", "\"amountCents\":0,\"amountCents\":1") }))
        rejected(archive(dataText = { it.replace("\"projects\":", "\"extra\":true,\"projects\":") }))
        rejected(archive(dataText = { it.replace("\"endReason\":null,", "") }))
        rejected(archive(settingsText = { "{\"recordZoneId\":\"Asia/Shanghai\",\"recordZoneId\":\"UTC\"}" }))
        rejected(archive(settingsBytes = byteArrayOf(0xc3.toByte(), 0x28)))
    }

    @Test fun rejects_unsafe_unknown_and_duplicate_zip_paths_before_extraction() {
        listOf("../outside", "/absolute", "media\\bad.png", "media/../escape", "media//bad.png", "media/./bad.png", "C:/bad", "extra.json").forEach { name ->
            val directory = Files.createTempDirectory("lithic-import")
            rejected(archive(extra = name to byteArrayOf(1)), directory)
            Files.list(directory).use { assertEquals(0L, it.count()) }
        }
        val zip = archive(extra = "dats.json" to byteArrayOf(1))
        val bytes = Files.readAllBytes(zip)
        replaceAscii(bytes, "dats.json", "data.json")
        Files.write(zip, bytes)
        rejected(zip)
    }

    @Test fun long_deep_and_multibyte_zip_names_fail_early_path_resource_admission() {
        val paths = listOf(
            "media/" + "a/".repeat(2050) + "z", // 4107 UTF-8 bytes; short components but excessive whole path.
            "media/" + "a".repeat(256), // One component exceeds Android's 255-byte boundary.
            "media/" + "界".repeat(86), // 86 characters are 258 UTF-8 bytes, rather than 86 bytes.
        )
        paths.forEach { name ->
            // Exactly four entries. Existing media is omitted so the current implementation
            // eventually rejects the manifest; that late rejection cannot satisfy this test.
            earlyResourceRejection(archive(extra = name to byteArrayOf(1), omit = mediaEntry.path), "ZIP path resource limit")
        }
    }

    @Test fun oversized_central_directory_fails_early_metadata_budget_without_large_fixture_allocation() {
        val original = Files.readAllBytes(archive()) // The real fixture is only a few KiB.
        val end = original.copyOfRange(original.size - 22, original.size)
        val centralStart = u32(end, 16)
        val declaredMetadata = 16L * 1024 * 1024 + 1
        put32(end, 12, declaredMetadata)
        val sparse = Files.createTempFile("lithic-metadata-budget", ".zip")
        FileChannel.open(sparse, WRITE).use { channel ->
            val prefix = ByteBuffer.wrap(original, 0, original.size - 22)
            while (prefix.hasRemaining()) channel.write(prefix)
            // Seek, rather than allocating or filling a sixteen-MiB buffer.
            channel.position(centralStart + declaredMetadata)
            val ending = ByteBuffer.wrap(end)
            while (ending.hasRemaining()) channel.write(ending)
        }
        earlyResourceRejection(sparse, "ZIP metadata resource limit")
    }

    @Test fun rejects_symlink_central_attributes_and_local_central_name_disagreement() {
        val link = archive()
        val bytes = Files.readAllBytes(link)
        val central = signature(bytes, 0x02014b50)
        bytes[central + 5] = 3 // Unix producer.
        put32(bytes, central + 38, 0xa1ff0000L) // Symbolic link, mode 0120777.
        Files.write(link, bytes)
        rejected(link)
        val mismatch = archive()
        val changed = Files.readAllBytes(mismatch)
        changed[30] = 'x'.code.toByte()
        Files.write(mismatch, changed)
        rejected(mismatch)
    }

    @Test fun rejects_truncated_directory_trailing_bytes_crc_and_size_lies() {
        val original = Files.readAllBytes(archive())
        listOf(original.copyOf(original.size - 22), original + byteArrayOf(0)).forEach { bytes ->
            val path = Files.createTempFile("lithic-bad", ".zip")
            Files.write(path, bytes); rejected(path)
        }
        for (offset in listOf(16, 24)) {
            val zip = archive()
            val bytes = Files.readAllBytes(zip)
            put32(bytes, signature(bytes, 0x02014b50) + offset, if (offset == 24) 0xffffffffL else 7L)
            Files.write(zip, bytes); rejected(zip)
        }
    }

    @Test fun rejects_hidden_bytes_after_deflate_end_even_when_zip_headers_and_crc_agree() {
        val zip = archive()
        val original = Files.readAllBytes(zip)
        val firstCentral = signature(original, 0x02014b50)
        val compressed = u32(original, firstCentral + 20).toInt()
        val insertion = 30 + u16(original, 26) + u16(original, 28) + compressed
        val changed = original.copyOfRange(0, insertion) + byteArrayOf(0) + original.copyOfRange(insertion, original.size)
        // Preserve every ZIP range, CRC, local name and descriptor. Only the deflate
        // stream itself ends one byte before its declared compressed boundary.
        put32(changed, insertion + 1 + 8, compressed + 1L)
        var central = firstCentral + 1
        repeat(4) { index ->
            if (index == 0) put32(changed, central + 20, compressed + 1L)
            else put32(changed, central + 42, u32(changed, central + 42) + 1L)
            central += 46 + u16(changed, central + 28) + u16(changed, central + 30) + u16(changed, central + 32)
        }
        put32(changed, changed.size - 22 + 16, firstCentral + 1L)
        Files.write(zip, changed)
        rejected(zip)
    }

    @Test fun rejects_missing_extra_payload_and_wrong_manifest_hash_length_or_record_counts() {
        rejected(archive(omit = mediaEntry.path))
        rejected(archive(manifestText = { it.replace(mediaEntry.sha256, "0".repeat(64)) }))
        rejected(archive(manifestText = { it.replace("\"byteLength\":${png.size}", "\"byteLength\":${png.size + 1}") }))
        rejected(archive(manifestText = { it.replace("\"long_term_projects\":1", "\"long_term_projects\":2") }))
        rejected(archive(manifestText = { it.replace("\"categories\":2", "\"categories\":9223372036854775807") }))
    }

    @Test fun rejects_invalid_zone_disagreement_and_malformed_dates_instants_uuid_and_enums() {
        rejected(archive(settingsText = { "{\"recordZoneId\":\"Unknown/Zone\"}" }))
        rejected(archive(settingsText = { "{\"recordZoneId\":\"UTC\"}" }))
        rejected(archive(data.copy(projects = listOf(data.projects.single().copy(startedOn = "2025-02-30")))))
        rejected(archive(data.copy(projects = listOf(data.projects.single().copy(updatedAt = "invalid")))))
        rejected(archive(data.copy(projects = listOf(data.projects.single().copy(id = "not-a-uuid")))))
        rejected(archive(data.copy(projectMoneyEvents = listOf(data.projectMoneyEvents.single().copy(direction = "INVALID")))))
    }

    @Test fun rejects_broken_relations_wrong_category_type_and_changed_or_missing_defaults() {
        rejected(archive(data.copy(projects = listOf(data.projects.single().copy(categoryId = id(2))))))
        rejected(archive(data.copy(projects = listOf(data.projects.single().copy(coverAssetId = id(999))))))
        rejected(archive(data.copy(projectMoneyEvents = listOf(data.projectMoneyEvents.single().copy(projectId = id(999))))))
        rejected(archive(data.copy(subscriptionRenewals = listOf(data.subscriptionRenewals.single().copy(subscriptionId = id(999))))))
        rejected(archive(data.copy(categories = data.categories.drop(1))))
        rejected(archive(data.copy(categories = listOf(data.categories.first().copy(name = "改名", normalizedName = "改名"), data.categories.last()))))
    }

    @Test fun rejects_duplicate_ids_operations_category_keys_media_paths_and_state_dates() {
        rejected(archive(data.copy(projects = data.projects + data.projects.single())))
        rejected(archive(data.copy(projectMoneyEvents = data.projectMoneyEvents + data.projectMoneyEvents.single().copy(id = id(19), role = "REGULAR"))))
        rejected(archive(data.copy(categories = data.categories + data.categories.first().copy(id = id(5), isFallback = false))))
        rejected(archive(data.copy(mediaAssets = data.mediaAssets + data.mediaAssets.single().copy(id = id(21)))))
        rejected(archive(data.copy(subscriptionStateChanges = data.subscriptionStateChanges + data.subscriptionStateChanges.single().copy(id = id(33), operationId = id(34)))))
    }

    @Test fun rejects_money_out_of_range_nonpositive_renewal_overlap_and_wrong_standard_days() {
        listOf(-1L, 1_000_000_000_000L, Long.MAX_VALUE).forEach { amount ->
            rejected(archive(data.copy(projectMoneyEvents = listOf(data.projectMoneyEvents.single().copy(amountCents = amount)))))
        }
        rejected(archive(data.copy(subscriptionRenewals = listOf(data.subscriptionRenewals.single().copy(amountCents = 0)))))
        rejected(archive(data.copy(subscriptionRenewals = listOf(data.subscriptionRenewals.single().copy(standardDays = 31)))))
        rejected(archive(data.copy(subscriptionRenewals = data.subscriptionRenewals + data.subscriptionRenewals.single().copy(id = id(40), operationId = id(41), effectiveFrom = "2026-01-15"))))
    }

    @Test fun rejects_invalid_project_lifecycle_or_missing_real_subscription_payment() {
        val project = data.projects.single()
        rejected(archive(data.copy(projects = listOf(project.copy(name = "  ")))))
        rejected(archive(data.copy(projects = listOf(project.copy(endedOn = "2025-01-02")))))
        rejected(archive(data.copy(projects = listOf(project.copy(endedOn = "2024-01-02", endReason = "结束")))))
        rejected(archive(data.copy(projects = listOf(project.copy(updatedAt = "2025-01-01T00:00:00Z")))))
        rejected(archive(data.copy(projectMoneyEvents = emptyList())))
        rejected(archive(data.copy(projectMoneyEvents = listOf(data.projectMoneyEvents.single().copy(direction = "RECOVERY")))))
        rejected(archive(data.copy(subscriptionRenewals = emptyList())))
        rejected(archive(data.copy(subscriptionStateChanges = listOf(data.subscriptionStateChanges.single().copy(state = "UNKNOWN")))))
    }

    @Test fun refuses_inconsistent_media_identity_declared_dimensions_and_resource_budget() {
        rejected(archive(data.copy(mediaAssets = listOf(data.mediaAssets.single().copy(width = 1601)))))
        rejected(archive(data.copy(mediaAssets = listOf(data.mediaAssets.single().copy(mime = "image/gif")))))
        rejected(archive(data.copy(mediaAssets = listOf(data.mediaAssets.single().copy(byteLength = 1)))))
        rejected(archive(data.copy(mediaAssets = listOf(data.mediaAssets.single().copy(sha256 = "0".repeat(64))))))
    }

    @Test fun refuses_nonempty_extraction_root_without_touching_existing_files() {
        val directory = Files.createTempDirectory("lithic-import")
        Files.write(directory.resolve("keep"), byteArrayOf(7))
        rejected(archive(), directory)
        assertArrayEquals(byteArrayOf(7), Files.readAllBytes(directory.resolve("keep")))
    }

    @Test fun propagates_cancellation_during_validation() {
        var calls = 0
        try {
            validate(archive(), Files.createTempDirectory("lithic-import")) {
                if (++calls >= 3) throw java.util.concurrent.CancellationException("cancel")
            }
            fail("Cancellation must not become successful validation")
        } catch (_: java.util.concurrent.CancellationException) { assertTrue(calls >= 3) }
    }

    private fun archive(
        input: BackupDataV1 = data,
        dataText: (String) -> String = { it },
        settingsText: (String) -> String = { it },
        manifestText: (String) -> String = { it },
        settingsBytes: ByteArray? = null,
        extra: Pair<String, ByteArray>? = null,
        omit: String? = null,
    ): Path {
        val payload = linkedMapOf("data.json" to dataText(BackupJson.encodeData(input).decodeToString()).toByteArray(),
            "settings.json" to (settingsBytes ?: settingsText(BackupJson.encodeSettings(BackupSettingsV1("Asia/Shanghai")).decodeToString()).toByteArray()),
            mediaEntry.path to png)
        val manifest = BackupManifestV1(producerAppVersion = "synthetic", createdAt = at, recordZoneId = "Asia/Shanghai",
            recordCounts = input.recordCounts(), files = payload.map { (path, bytes) -> BackupFileEntry.fromBytes(path, bytes) })
        val path = Files.createTempFile("lithic-validate", ".zip")
        ZipOutputStream(Files.newOutputStream(path)).use { zip ->
            val entries = linkedMapOf("manifest.json" to manifestText(manifest.encode().decodeToString()).toByteArray()) + payload + listOfNotNull(extra)
            for ((name, bytes) in entries) if (name != omit) {
                zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
            }
        }
        return path
    }

    private fun validate(archive: Path, root: Path, checkpoint: () -> Unit = {}): ValidatedBackup =
        BackupValidator().validate(archive, root, checkpoint)

    private fun rejected(archive: Path, root: Path = Files.createTempDirectory("lithic-import")) {
        try { validate(archive, root); fail("Unsafe archive was accepted") }
        catch (_: IllegalArgumentException) { }
        catch (_: java.io.IOException) { }
    }
    private fun earlyResourceRejection(archive: Path, reason: String) {
        val root = Files.createTempDirectory("lithic-resource-admission")
        try { validate(archive, root); fail("Oversized ZIP metadata was admitted") }
        catch (invalid: IllegalArgumentException) {
            assertTrue("The resource admission pass must reject before later format/manifest checks: ${invalid.message}",
                invalid.message?.startsWith(reason) == true)
        }
        Files.list(root).use { assertEquals(0L, it.count()) }
    }
    private fun id(n: Int) = "00000000-0000-4000-8000-${n.toString().padStart(12, '0')}"
    private fun replaceAscii(bytes: ByteArray, from: String, to: String) {
        val pattern = from.toByteArray(); val replacement = to.toByteArray()
        for (i in 0..bytes.size - pattern.size) if (pattern.indices.all { bytes[i + it] == pattern[it] })
            replacement.copyInto(bytes, i)
    }
    private fun signature(bytes: ByteArray, value: Int): Int = (0 until bytes.size - 3).first { offset ->
        (0..3).all { bytes[offset + it].toInt() and 255 == (value ushr (it * 8) and 255) }
    }
    private fun put32(bytes: ByteArray, offset: Int, value: Long) {
        for (i in 0..3) bytes[offset + i] = (value ushr (i * 8)).toByte()
    }
    private fun u16(bytes: ByteArray, offset: Int): Int = (bytes[offset].toInt() and 255) or ((bytes[offset + 1].toInt() and 255) shl 8)
    private fun u32(bytes: ByteArray, offset: Int): Long = (0..3).fold(0L) { value, i -> value or ((bytes[offset + i].toLong() and 255L) shl (i * 8)) }
}
