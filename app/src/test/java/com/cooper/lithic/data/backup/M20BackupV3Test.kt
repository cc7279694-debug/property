package com.cooper.lithic.data.backup

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.settings.ReminderPreferences
import kotlinx.serialization.json.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.fail
import org.junit.Test

class M20BackupV3Test {
    @Test fun currentArchiveDeclaresSchemaFourAndNineTables() {
        // Preserve the actual frozen format-3 shape even though current export is now 4.
        val snapshot = emptySnapshot()
        val manifest = BackupManifestV1(3, 3, "m20-synthetic", 4, AT, snapshot.settings.recordZoneId,
            snapshot.legacyDataV3().recordCounts(), emptyList())
        BackupFormats.requireSupported(manifest)
        assertEquals(3, manifest.formatVersion)
        assertEquals(3, manifest.minReaderFormat)
        assertEquals(4, manifest.sourceDatabaseSchemaVersion)
        assertEquals(9, manifest.recordCounts.size)
        assertEquals(0L, manifest.recordCounts["notification_receipts"])
    }

    @Test fun newReadinessProofUsesExplicitFormatThree() {
        val output = ByteArrayOutputStream()
        RestoreProofData.write(emptySnapshot(), 3, output)
        assertTrue(output.toString("UTF-8").contains("\"notificationReceipts\":[]"))
    }

    @Test fun nineTableArchiveRoundTripsReceiptPreferencesAndCutoffWithoutChangingStoredFacts() {
        val source = validSnapshot()
        val result = validate(archive(source))
        assertEquals(source, result.snapshot)
        assertEquals(9, result.manifest.recordCounts.size)
        assertEquals(1L, result.manifest.recordCounts["notification_receipts"])
    }

    @Test fun realRetainedFormatOneValidatesBeforeUpgradeAndDoesNotInventReceipts() {
        val result = BackupValidator().validate(Paths.get("src/androidTest/assets/m16/legacy-v1.lithicbackup").toAbsolutePath().normalize(),
            Files.createTempDirectory("m20-real-v1"))
        assertEquals(1, result.manifest.formatVersion)
        assertEquals(7, result.manifest.recordCounts.size)
        assertEquals(10, result.snapshot.completeRecordCounts().size)
        assertTrue(result.snapshot.notificationReceipts.isEmpty())
        assertEquals(ReminderPreferences(), result.snapshot.reminderPreferences)
        val media = result.snapshot.data.mediaAssets.single()
        assertEquals(media.sha256, BackupFileEntry.fromBytes(media.relativePath, Files.readAllBytes(result.directory.resolve(media.relativePath))).sha256)
    }

    @Test fun formatTwoAuthenticatesOriginalEightTablesThenAddsOnlyExplicitReminderDefaults() {
        val source = validSnapshot()
        val result = validate(archive(source, format = 2))
        assertEquals(source.data, result.snapshot.data)
        assertEquals(8, result.manifest.recordCounts.size)
        assertTrue(result.snapshot.notificationReceipts.isEmpty())
        assertEquals(ReminderPreferences(), result.snapshot.reminderPreferences)
    }

    @Test fun formatTwoMissingOriginalNullableFieldFailsBeforeUpgrade() {
        val source = validSnapshot()
        val json = Json.parseToJsonElement(BackupJson.encodeData(source.data).decodeToString()).jsonObject
        val malformed = JsonObject(json + ("subscriptions" to JsonArray(json.getValue("subscriptions").jsonArray.map {
            JsonObject(it.jsonObject - "note")
        }))).toString().toByteArray()
        rejected(archive(source, format = 2, dataBytes = malformed))
    }

    @Test fun formatThreeMissingReceiptsFieldIsNotDefaulted() {
        val source = validSnapshot()
        val json = Json.parseToJsonElement(BackupJson.encodeData(source.legacyDataV3()).decodeToString()).jsonObject
        rejected(archive(source, dataBytes = JsonObject(json - "notificationReceipts").toString().toByteArray()))
    }

    @Test fun formatThreeMissingEachPreferenceOrNullableCutoffFails() {
        val source = validSnapshot()
        val json = Json.parseToJsonElement(BackupJson.encodeSettings(source.currentSettings()).decodeToString()).jsonObject
        for (field in listOf("renewalEnabled", "milestoneEnabled", "importCutoff")) {
            rejected(archive(source, settingsBytes = JsonObject(json - field).toString().toByteArray()))
        }
    }

    @Test fun formatThreeMissingReceiptFieldFailsDespiteCorrectLengthAndSha() {
        val source = validSnapshot()
        val json = Json.parseToJsonElement(BackupJson.encodeData(source.legacyDataV3()).decodeToString()).jsonObject
        for (field in listOf("logicalKey", "ownerId", "kind", "boundary", "publishedAt")) {
            val malformed = JsonObject(json + ("notificationReceipts" to JsonArray(json.getValue("notificationReceipts").jsonArray.map {
                JsonObject(it.jsonObject - field)
            }))).toString().toByteArray()
            rejected(archive(source, dataBytes = malformed))
        }
    }

    @Test fun duplicateLogicalKeyUnknownKindAndMissingOwnerFailBeforeExtraction() {
        val source = validSnapshot()
        val receipt = source.notificationReceipts.single()
        for (receipts in listOf(listOf(receipt, receipt), listOf(receipt.copy(kind = "UNKNOWN")),
            listOf(receipt.copy(ownerId = ID2, logicalKey = "RENEWAL:$ID2:2026-10-20")))) {
            rejected(archive(source.copy(notificationReceipts = receipts)))
        }
    }

    @Test fun invalidBoundaryKeyAndPublicationTimesFailBeforeExtraction() {
        val source = validSnapshot()
        val receipt = source.notificationReceipts.single()
        for (bad in listOf(receipt.copy(boundary = "2026-02-30"), receipt.copy(logicalKey = "wrong"),
            receipt.copy(publishedAt = "2026-10-16T23:59:59Z"), receipt.copy(publishedAt = "2026-10-19T16:00:00Z"),
            receipt.copy(publishedAt = "2027-01-01T00:00:00Z"))) {
            rejected(archive(source.copy(notificationReceipts = listOf(bad))))
        }
    }

    @Test fun exactNineTableCountAndSchemaTupleAreRequired() {
        val source = validSnapshot()
        rejected(archive(source, counts = source.legacyDataV3().recordCounts() - "notification_receipts"))
        rejected(archive(source, counts = source.legacyDataV3().recordCounts() + ("notification_receipts" to 2L)))
        rejected(archive(source, tuple = Triple(3, 2, 4)))
        rejected(archive(source, tuple = Triple(3, 3, 3)))
        rejected(archive(source, tuple = Triple(99, 99, 99)))
    }

    @Test fun invalidCutoffAndUnexpectedSettingsFieldAreRejected() {
        val source = validSnapshot()
        val json = Json.parseToJsonElement(BackupJson.encodeSettings(source.currentSettings()).decodeToString()).jsonObject
        rejected(archive(source, settingsBytes = JsonObject(json + ("importCutoff" to JsonPrimitive("2026-02-30"))).toString().toByteArray()))
        rejected(archive(source, settingsBytes = JsonObject(json + ("systemPermissionGranted" to JsonPrimitive(true))).toString().toByteArray()))
    }

    @Test fun legacyProofsKeepTheirOriginalBytesAndCannotHideReminderFacts() {
        val empty = emptySnapshot()
        for (version in 1..2) {
            val output = ByteArrayOutputStream()
            RestoreProofData.writeSettings(empty, version, output)
            assertArrayEquals(BackupJson.encodeSettings(empty.settings), output.toByteArray())
            val changed = empty.copy(reminderPreferences = ReminderPreferences(renewalEnabled = false))
            try { RestoreProofData.write(changed, version, ByteArrayOutputStream()); fail("New reminder setting hidden by old proof") }
            catch (_: IllegalArgumentException) { }
            try { RestoreProofData.write(validSnapshot(), version, ByteArrayOutputStream()); fail("Receipt hidden by old proof") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun currentProofSettingsIncludeBothPreferencesAndExplicitCutoff() {
        val snapshot = validSnapshot()
        val output = ByteArrayOutputStream()
        RestoreProofData.writeSettings(snapshot, 3, output)
        assertArrayEquals(BackupJson.encodeSettings(snapshot.currentSettings()), output.toByteArray())
        assertTrue(output.toString("UTF-8").contains("\"importCutoff\":\"2026-10-16\""))
    }

    @Test fun restorationWatermarkUsesRecordedZoneAndNeverMovesBackwards() {
        val instant = Instant.parse("2026-10-21T17:00:00Z")
        val value = ReminderPreferences(false, true)
        assertEquals(LocalDate.parse("2026-10-22"), RestoreReminderProtection.preferences(value, ZoneId.of("Asia/Shanghai"), instant).importCutoff)
        assertEquals(LocalDate.parse("2026-10-21"), RestoreReminderProtection.preferences(value, ZoneId.of("Pacific/Honolulu"), instant).importCutoff)
        val future = value.copy(importCutoff = LocalDate.parse("2026-12-01"))
        assertEquals(future, RestoreReminderProtection.preferences(future, ZoneId.of("Asia/Shanghai"), instant))
    }

    @Test fun currentRecordBudgetRequiresAllNineTablesBeforeRowsAreRead() {
        assertEquals(0L, requireBackupRecordCounts(List(9) { 0L }, expectedTables = 9))
        try { requireBackupRecordCounts(List(8) { 0L }, expectedTables = 9); fail("Missing receipt table admitted") }
        catch (_: IllegalArgumentException) { }
    }

    private fun validSnapshot(): BackupSnapshot {
        val data = BackupDataV2(
            listOf(BackupCategoryV1(LithicDatabase.PROJECT_FALLBACK_ID, "PROJECT", "其他", "其他", true),
                BackupCategoryV1(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, "SUBSCRIPTION", "其他", "其他", true)),
            emptyList(), emptyList(), emptyList(),
            listOf(BackupSubscriptionV1(ID, "合成订阅", LithicDatabase.SUBSCRIPTION_FALLBACK_ID, null, null, CREATED, CREATED)),
            listOf(BackupSubscriptionRenewalV1(ID2, ID, "2026-10-01", "2026-10-01", "2026-10-20", "2026-10-20", 100, "MONTHLY", 30, ID3)),
            emptyList(), emptyList(),
        )
        return BackupSnapshot(data, BackupSettingsV1("Asia/Shanghai"),
            listOf(BackupNotificationReceiptV3("RENEWAL:$ID:2026-10-20", ID, "RENEWAL", "2026-10-20", "2026-10-17T01:00:00Z")),
            ReminderPreferences(false, false, LocalDate.parse("2026-10-16")))
    }

    private fun archive(snapshot: BackupSnapshot, format: Int = 3, dataBytes: ByteArray? = null, settingsBytes: ByteArray? = null,
        counts: Map<String, Long>? = null, tuple: Triple<Int, Int, Int>? = null): Path {
        val payload = linkedMapOf("data.json" to (dataBytes ?: if (format == 2) BackupJson.encodeData(snapshot.data) else BackupJson.encodeData(BackupDataV3(snapshot.data.categories, snapshot.data.projects, snapshot.data.projectMoneyEvents, snapshot.data.mediaAssets, snapshot.data.subscriptions, snapshot.data.subscriptionRenewals, snapshot.data.subscriptionStateChanges, snapshot.data.replacements, snapshot.notificationReceipts))),
            "settings.json" to (settingsBytes ?: if (format == 2) BackupJson.encodeSettings(snapshot.settings) else BackupJson.encodeSettings(snapshot.currentSettings())))
        val versions = tuple ?: if (format == 2) Triple(2, 2, 3) else Triple(3, 3, 4)
        val manifest = BackupManifestV1(versions.first, versions.second, "m20-test", versions.third, AT, snapshot.settings.recordZoneId,
            counts ?: if (format == 2) snapshot.data.recordCounts() else snapshot.data.recordCounts() + ("notification_receipts" to snapshot.notificationReceipts.size.toLong()), payload.map { (name, bytes) -> BackupFileEntry.fromBytes(name, bytes) })
        val path = Files.createTempFile("m20-archive", ".lithicbackup")
        ZipOutputStream(Files.newOutputStream(path)).use { zip ->
            for ((name, bytes) in linkedMapOf("manifest.json" to manifest.encode()) + payload) {
                zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
            }
        }
        return path
    }
    private fun validate(path: Path) = BackupValidator().validate(path, Files.createTempDirectory("m20-extract"))
    private fun rejected(path: Path) {
        val extraction = Files.createTempDirectory("m20-rejected")
        try { BackupValidator().validate(path, extraction); fail("Malformed backup admitted") }
        catch (_: IllegalArgumentException) { }
        Files.list(extraction).use { assertEquals(0L, it.count()) }
    }

    private fun emptySnapshot() = BackupSnapshot(
        BackupDataV1(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()),
        BackupSettingsV1("Asia/Shanghai"),
    )
    private companion object {
        const val AT = "2026-10-22T02:00:00Z"
        const val CREATED = "2026-10-01T00:00:00Z"
        const val ID = "00000000-0000-4000-8000-000000000101"
        const val ID2 = "00000000-0000-4000-8000-000000000102"
        const val ID3 = "00000000-0000-4000-8000-000000000103"
    }
}
