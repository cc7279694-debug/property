package com.cooper.lithic.data.backup

import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.NotificationReceiptIdentity
import com.cooper.lithic.data.settings.ReminderPreferences
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.time.Instant
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.After

class M21BackupFormatTest {
    private val temporaryRoots = mutableListOf<Path>()
    private fun temporaryRoot(purpose: String): Path = Files.createTempDirectory("m21-$purpose").also { temporaryRoots.add(it) }

    @After fun closeOwnedFixtures() {
        val knownFiles = setOf("backup.lithicbackup", "payload/manifest.json", "payload/data.json", "payload/settings.json")
        for (root in temporaryRoots) {
            check(Files.isDirectory(root, NOFOLLOW_LINKS) && !Files.isSymbolicLink(root))
            val nodes = Files.walk(root).use { it.iterator().asSequence().filter { node -> node != root }.toList() }
            // Complete admission precedes the first deletion; unfamiliar residue stays on disk.
            check(nodes.all { path ->
                val name = root.relativize(path).joinToString("/")
                !Files.isSymbolicLink(path) && (if (name == "payload") Files.isDirectory(path, NOFOLLOW_LINKS)
                    else name in knownFiles && Files.isRegularFile(path, NOFOLLOW_LINKS))
            }) { "Unknown fixture residue retained" }
            nodes.sortedByDescending { it.nameCount }.forEach { Files.delete(it) }
            Files.delete(root)
        }
    }

    @Test fun formatFourDeclaresSchemaFiveAndTenTables() {
        val tables = BackupFormats.V2_TABLES + setOf("notification_receipts", "milestone_watermarks")
        val manifest = BackupManifestV1(formatVersion = 4, minReaderFormat = 4, sourceDatabaseSchemaVersion = 5,
            producerAppVersion = "synthetic-m21", createdAt = AT, recordZoneId = "Asia/Shanghai",
            recordCounts = tables.associateWith { 0L }, files = emptyList())
        BackupFormats.requireSupported(manifest)
        assertEquals(tables, BackupFormats.CURRENT_TABLES)
    }

    @Test fun legacyThreeNormalizesMissingWatermarksWithoutInventingReceipts() {
        val legacy = BackupManifestV1(formatVersion = 3, minReaderFormat = 3, sourceDatabaseSchemaVersion = 4,
            producerAppVersion = "synthetic-m20", createdAt = AT, recordZoneId = "Asia/Shanghai",
            recordCounts = (BackupFormats.V2_TABLES + "notification_receipts").associateWith { 0L }, files = emptyList())
        assertEquals(0L, BackupFormats.normalizedCounts(legacy)["milestone_watermarks"])
    }

    @Test fun actualCurrentExportDeclaresFourFourFiveAndTenCounts() {
        BackupCoreStager().prepare(valid(), temporaryRoot("core")).use { core ->
            val manifest = BackupManifestV1.complete(core, "synthetic-m21", Instant.parse(AT))
            assertEquals(4, manifest.formatVersion)
            assertEquals(4, manifest.minReaderFormat)
            assertEquals(5, manifest.sourceDatabaseSchemaVersion)
            assertEquals(10, manifest.recordCounts.size)
            assertEquals(2L, manifest.recordCounts["milestone_watermarks"])
        }
    }

    @Test fun formatFourRoundTripsSameUuidTypedWatermarksAndReceipts() {
        val snapshot = valid()
        val restored = validate(snapshot)
        assertEquals(snapshot, restored.snapshot)
        assertEquals(snapshot.data, restored.snapshot.data)
        assertEquals(2, restored.snapshot.milestoneWatermarks.size)
        assertEquals(3, restored.snapshot.notificationReceipts.size)
    }

    @Test fun oldFormatsNormalizeAllMissingTablesWithoutInventingPublication() {
        for ((format, tables) in listOf(1 to BackupFormats.LEGACY_TABLES, 2 to BackupFormats.V2_TABLES, 3 to BackupFormats.V3_TABLES)) {
            val manifest = BackupManifestV1(format, format, "legacy", format + 1, AT, "Asia/Shanghai", tables.associateWith { 0L }, emptyList())
            assertEquals(BackupFormats.CURRENT_TABLES, BackupFormats.normalizedCounts(manifest).keys)
            assertEquals(0L, BackupFormats.normalizedCounts(manifest)["milestone_watermarks"])
            assertEquals(0L, BackupFormats.normalizedCounts(manifest)["notification_receipts"])
        }
    }

    @Test fun legacyThreeWireAndProofKeepExactlyTheOriginalShape() {
        val old = valid().copy(milestoneWatermarks = emptyList(), notificationReceipts = valid().notificationReceipts.filter { it.kind == "RENEWAL" })
        val original = old.legacyDataV3()
        val bytes = BackupJson.encodeData(original)
        assertFalse(bytes.decodeToString().contains("milestoneWatermarks"))
        val output = ByteArrayOutputStream()
        RestoreProofData.write(old, 3, output)
        assertArrayEquals(bytes, output.toByteArray())
        val read = archive(old, 3, bytes)
        assertEquals(old, read.snapshot)
        assertEquals(9, read.manifest.recordCounts.size)
    }

    @Test fun oldProofsCannotHideNewWatermarksOrMilestoneReceipts() {
        val snapshot = valid()
        for (format in 1..3) {
            rejects { RestoreProofData.write(snapshot, format, ByteArrayOutputStream()) }
            rejects { RestoreProofData.writeSettings(snapshot, format, ByteArrayOutputStream()) }
        }
        val onlyNewReceipt = snapshot.copy(milestoneWatermarks = emptyList(), notificationReceipts = snapshot.notificationReceipts.filter { it.kind != "RENEWAL" })
        rejects { RestoreProofData.write(onlyNewReceipt, 3, ByteArrayOutputStream()) }
    }

    @Test fun formatFourProofAuthenticatesWatermarksAndBothPreferences() {
        val snapshot = valid()
        val data = ByteArrayOutputStream(); val settings = ByteArrayOutputStream()
        RestoreProofData.write(snapshot, 4, data); RestoreProofData.writeSettings(snapshot, 4, settings)
        assertArrayEquals(BackupJson.encodeData(snapshot.currentData()), data.toByteArray())
        assertArrayEquals(BackupJson.encodeSettings(snapshot.currentSettings()), settings.toByteArray())
        assertTrue(data.toString("UTF-8").contains("milestoneWatermarks"))
    }

    @Test fun absentWatermarkCollectionOrAnyMandatoryWatermarkFieldFailsBeforeExtraction() {
        val snapshot = valid()
        val data = Json.parseToJsonElement(BackupJson.encodeData(snapshot.currentData()).decodeToString()).jsonObject
        rejectsArchive(snapshot, JsonObject(data - "milestoneWatermarks").toString().toByteArray())
        for (field in listOf("ownerType", "ownerId", "cutoffOn", "suppressedMask")) {
            val rows = data.getValue("milestoneWatermarks").jsonArray.map { JsonObject(it.jsonObject - field) }
            rejectsArchive(snapshot, JsonObject(data + ("milestoneWatermarks" to JsonArray(rows))).toString().toByteArray())
        }
    }

    @Test fun unknownTypeMaskCanonicalDateOrOrphanWatermarkFails() {
        val source = valid()
        val one = source.milestoneWatermarks.first()
        val other = "00000000-0000-4000-8000-000000000199"
        for (bad in listOf(one.copy(ownerType = "UNKNOWN"), one.copy(suppressedMask = -1), one.copy(suppressedMask = 64),
            one.copy(cutoffOn = "2026-02-30"), one.copy(cutoffOn = "2026-1-01"), one.copy(ownerId = other))) {
            rejects { validate(source.copy(milestoneWatermarks = listOf(bad))) }
        }
    }

    @Test fun duplicateJointIdentityRejectedButSameUuidDifferentTypeAllowed() {
        val source = valid()
        assertEquals(2, validate(source).snapshot.milestoneWatermarks.size)
        rejects { validate(source.copy(milestoneWatermarks = source.milestoneWatermarks + source.milestoneWatermarks.first())) }
    }

    @Test fun wrongTypeOwnerReferenceFailsEvenWithExistingOtherTypeOwner() {
        val source = valid()
        val onlyProjects = source.copy(data = source.data.copy(subscriptions = emptyList(), subscriptionRenewals = emptyList()),
            notificationReceipts = source.notificationReceipts.filter { it.kind == "PROJECT_MILESTONE" })
        rejects { validate(onlyProjects) }
    }

    @Test fun typedReceiptBoundaryKeyAndOwnerCannotMix() {
        val source = valid()
        val project = source.notificationReceipts.first { it.kind == "PROJECT_MILESTONE" }
        for (bad in listOf(project.copy(boundary = "2026-01-01"), project.copy(boundary = "D365"),
            project.copy(kind = "UNKNOWN"), project.copy(logicalKey = "MILESTONE:SUBSCRIPTION:$ID:Y1"))) {
            rejects { validate(source.copy(notificationReceipts = listOf(bad))) }
        }
        NotificationReceiptIdentity.validate(project.logicalKey, project.ownerId, project.kind, project.boundary)
    }

    @Test fun milestonePublicationBeforeRecordedNineIsRejectedWithoutRecomputingHistoricalReachDate() {
        val source = valid()
        val receipt = source.notificationReceipts.first { it.kind == "PROJECT_MILESTONE" }
        rejects { validate(source.copy(notificationReceipts = listOf(receipt.copy(publishedAt = "2026-01-01T00:59:00Z")))) }
    }

    @Test fun legacyThreeRejectsMilestoneKindRatherThanFilteringIt() {
        val source = valid().copy(milestoneWatermarks = emptyList())
        val data = source.currentData()
        val forbidden = BackupDataV3(data.categories, data.projects, data.projectMoneyEvents, data.mediaAssets,
            data.subscriptions, data.subscriptionRenewals, data.subscriptionStateChanges, data.replacements, data.notificationReceipts)
        rejects { archive(source, 3, BackupJson.encodeData(forbidden), forbidden.recordCounts()) }
    }

    @Test fun restorationSuppressesReachedNodesIncludingTodayWithoutInventingReceipt() {
        val original = valid().copy(notificationReceipts = emptyList(), milestoneWatermarks = emptyList())
        val restored = RestoreReminderProtection.snapshot(original, Instant.parse("2026-04-10T02:00:00Z"))
        assertTrue(restored.notificationReceipts.isEmpty())
        assertEquals(original.data, restored.data)
        assertEquals(LocalDate.parse("2026-04-10"), restored.reminderPreferences.importCutoff)
        val marks = restored.milestoneWatermarks.associateBy { it.ownerType }
        assertEquals(5, marks.getValue("PROJECT").suppressedMask) // prior D100 plus Y1
        assertEquals(1, marks.getValue("SUBSCRIPTION").suppressedMask) // elapsed paid D100 today
        assertTrue(restored.milestoneWatermarks.all { it.cutoffOn == "2026-04-10" })
    }

    @Test fun restorationDoesNotRetreatFutureCutoffOrPreviouslySuppressedBits() {
        val original = valid().copy(milestoneWatermarks = valid().milestoneWatermarks.map { it.copy(cutoffOn = "2030-01-01", suppressedMask = 63) },
            reminderPreferences = ReminderPreferences(false, false, LocalDate.parse("2030-01-01")))
        val restored = RestoreReminderProtection.snapshot(original, Instant.parse(AT))
        assertEquals(original, restored)
        assertEquals(restored, RestoreReminderProtection.snapshot(restored, Instant.parse(AT)))
    }

    @Test fun tenTableBudgetIsCountedBeforeMaterialization() {
        assertEquals(0L, requireBackupRecordCounts(List(10) { 0L }, 10))
        rejects { requireBackupRecordCounts(List(9) { 0L }, 10) }
        rejects { requireBackupRecordCounts(List(9) { 0L } + BackupLimits.MAX_RECORDS, 10) }
    }

    private fun valid(): BackupSnapshot {
        val categories = listOf(BackupCategoryV1(LithicDatabase.PROJECT_FALLBACK_ID, "PROJECT", "其他", "其他", true),
            BackupCategoryV1(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, "SUBSCRIPTION", "其他", "其他", true))
        val data = BackupDataV2(categories,
            listOf(BackupProjectV1(ID, "合成项目", LithicDatabase.PROJECT_FALLBACK_ID, "2025-01-01", null, null, null, null, CREATED, CREATED)),
            listOf(BackupProjectMoneyEventV2(ID2, ID, "OUTLAY", 12345, "2025-01-01", null, "INITIAL", ID3, "CASH", null)), emptyList(),
            listOf(BackupSubscriptionV1(ID, "合成订阅", LithicDatabase.SUBSCRIPTION_FALLBACK_ID, null, null, CREATED, CREATED)),
            listOf(BackupSubscriptionRenewalV1(ID4, ID, "2026-01-01", "2026-01-01", "2027-01-01", "2027-01-01", 365000, "YEARLY", 365, ID5)),
            emptyList(), emptyList())
        return BackupSnapshot(data, BackupSettingsV1("Asia/Shanghai"), listOf(
            BackupNotificationReceiptV3("MILESTONE:PROJECT:$ID:Y1", ID, "PROJECT_MILESTONE", "Y1", "2026-01-01T01:00:00Z"),
            BackupNotificationReceiptV3("MILESTONE:SUBSCRIPTION:$ID:D100", ID, "SUBSCRIPTION_MILESTONE", "D100", "2026-04-10T01:00:00Z"),
            BackupNotificationReceiptV3("RENEWAL:$ID:2026-10-25", ID, "RENEWAL", "2026-10-25", AT)),
            ReminderPreferences(false, true, LocalDate.parse("2026-01-01")), listOf(
                BackupMilestoneWatermarkV4("PROJECT", ID, "2026-10-22", 5),
                BackupMilestoneWatermarkV4("SUBSCRIPTION", ID, "2026-10-22", 1)))
    }

    private fun validate(snapshot: BackupSnapshot) = archive(snapshot)
    private fun archive(snapshot: BackupSnapshot, format: Int = 4, dataBytes: ByteArray = BackupJson.encodeData(snapshot.currentData()),
        counts: Map<String, Long> = if (format == 4) snapshot.completeRecordCounts() else snapshot.data.recordCounts() + ("notification_receipts" to snapshot.notificationReceipts.size.toLong())): ValidatedBackup {
        val payload = linkedMapOf("data.json" to dataBytes, "settings.json" to BackupJson.encodeSettings(snapshot.currentSettings()))
        val manifest = BackupManifestV1(format, format, "m21-test", format + 1, AT, snapshot.settings.recordZoneId, counts,
            payload.map { (name, bytes) -> BackupFileEntry.fromBytes(name, bytes) })
        val root = temporaryRoot("wire")
        val zip = root.resolve("backup.lithicbackup")
        ZipOutputStream(Files.newOutputStream(zip)).use { output ->
            for ((name, bytes) in linkedMapOf("manifest.json" to manifest.encode()) + payload) {
                output.putNextEntry(ZipEntry(name)); output.write(bytes); output.closeEntry()
            }
        }
        return BackupValidator().validate(zip, Files.createDirectory(root.resolve("payload")))
    }
    private fun rejectsArchive(snapshot: BackupSnapshot, bytes: ByteArray) { rejects { archive(snapshot, dataBytes = bytes) } }
    private fun rejects(block: () -> Unit) {
        try { block(); fail("Invalid milestone archive/proof admitted") } catch (_: IllegalArgumentException) { }
    }
    private companion object {
        const val AT = "2026-10-22T02:00:00Z"
        const val CREATED = "2025-01-01T00:00:00Z"
        const val ID = "00000000-0000-4000-8000-000000000101"
        const val ID2 = "00000000-0000-4000-8000-000000000102"
        const val ID3 = "00000000-0000-4000-8000-000000000103"
        const val ID4 = "00000000-0000-4000-8000-000000000104"
        const val ID5 = "00000000-0000-4000-8000-000000000105"
    }
}
