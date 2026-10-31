package com.cooper.lithic.data.media.photolite

import com.cooper.lithic.data.backup.BackupArchiveFiles
import com.cooper.lithic.data.backup.BackupCategoryV1
import com.cooper.lithic.data.backup.BackupCoreStager
import com.cooper.lithic.data.backup.BackupDataV2
import com.cooper.lithic.data.backup.BackupFileEntry
import com.cooper.lithic.data.backup.BackupJson
import com.cooper.lithic.data.backup.BackupManifestV1
import com.cooper.lithic.data.backup.BackupMediaAssetV1
import com.cooper.lithic.data.backup.BackupProjectMoneyEventV2
import com.cooper.lithic.data.backup.BackupProjectV1
import com.cooper.lithic.data.backup.BackupSettingsV1
import com.cooper.lithic.data.backup.BackupSnapshot
import com.cooper.lithic.data.backup.BackupValidator
import com.cooper.lithic.data.local.LithicDatabase
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/**
 * Format4 archive/restore-admission integration only. This uses real backup DTOs, staging, closed
 * archive verification and the validator; it never pretends to run Android BackupMediaSnapshot,
 * Room registration, generation restore, SAF readback or platform decoding on a host JVM.
 * Source prepared while the user has deferred all JVM/Gradle execution.
 */
class PhotoLiteBackup4IntegrationTest {
    @Test fun normalized_cover_and_money_facts_survive_real_format4_archive_and_restore_admission() = ownedFixture { fixture ->
        val archive = BackupArchiveFiles.create(fixture.core, fixture.manifest, fixture.archivePath("complete"))
        fixture.rememberArchive(archive.path)
        assertEquals(archive, BackupArchiveFiles.verify(fixture.core, fixture.manifest, archive.path))
        assertEquals(4, fixture.manifest.formatVersion)
        assertEquals(4, fixture.manifest.minReaderFormat)
        assertEquals(5, fixture.manifest.sourceDatabaseSchemaVersion)

        val restored = BackupValidator().validate(archive.path, fixture.extraction("admitted"))
        assertEquals(archive.sha256, restored.archiveSha256)
        assertEquals(fixture.snapshot, restored.snapshot)
        val media = restored.snapshot.data.mediaAssets.single()
        assertEquals(MEDIA_ID, restored.snapshot.data.projects.single().coverAssetId)
        assertEquals(MEDIA_ID, media.id)
        assertEquals(fixture.photo.width, media.width)
        assertEquals(fixture.photo.height, media.height)
        assertEquals(fixture.photo.byteLength, media.byteLength)
        assertEquals(fixture.photo.sha256, media.sha256)
        assertArrayEquals(fixture.photo.copyBytes(), Files.readAllBytes(restored.directory.resolve(MEDIA_PATH)))
        assertEquals(listOf(12_345L, 23_456L), restored.snapshot.data.projectMoneyEvents.map { it.amountCents })
        assertEquals(listOf("OUTLAY", "RECOVERY"), restored.snapshot.data.projectMoneyEvents.map { it.direction })
        assertEquals(fixture.snapshot.data.projectMoneyEvents, restored.snapshot.data.projectMoneyEvents)
        assertEquals(1L, restored.manifest.recordCounts.getValue("local_media_assets"))
        assertEquals(10, restored.manifest.recordCounts.size)
    }

    @Test fun missing_registered_cover_payload_cannot_pass_closed_archive_or_restore_admission() = ownedFixture { fixture ->
        // The valid core stays unchanged. Only this test-owned archive omits its declared media.
        val missing = fixture.repack("missing", omitMedia = true)
        expectRejected { BackupArchiveFiles.verify(fixture.core, fixture.manifest, missing) }
        expectRejected { BackupValidator().validate(missing, fixture.extraction("missing-admission")) }
        assertArrayEquals(fixture.photo.copyBytes(), Files.readAllBytes(fixture.core.directory.resolve(MEDIA_PATH)))
        assertEquals(MEDIA_ID, fixture.snapshot.data.projects.single().coverAssetId)
    }

    @Test fun damaged_cover_with_unchanged_registered_sha_cannot_pass_archive_or_restore_admission() = ownedFixture { fixture ->
        val damaged = fixture.photo.copyBytes().also { it[44] = (it[44].toInt() xor 1).toByte() }
        expectRejected { PhotoLitePng.checked(damaged) }
        // ZIP's own CRC is recomputed, so rejection must also honor the original registered SHA.
        val archive = fixture.repack("damaged", mediaBytes = damaged)
        expectRejected { BackupArchiveFiles.verify(fixture.core, fixture.manifest, archive) }
        expectRejected { BackupValidator().validate(archive, fixture.extraction("damaged-admission", damaged)) }
        assertArrayEquals(fixture.photo.copyBytes(), Files.readAllBytes(fixture.core.directory.resolve(MEDIA_PATH)))
        assertEquals(fixture.photo.sha256, fixture.snapshot.data.mediaAssets.single().sha256)
    }

    /** No finally-cleanup: an unexpected assertion, IO or validation failure preserves the full fixture. */
    private fun ownedFixture(block: (Fixture) -> Unit) {
        val token = UUID.randomUUID().toString()
        val root = Files.createTempDirectory("photo-lite-backup4-$token-").toRealPath()
        Files.write(root.resolve(OWNER), token.toByteArray(Charsets.UTF_8), CREATE_NEW, WRITE)
        val fixture = Fixture(root, token)
        block(fixture)
        fixture.closeAfterSuccess()
    }

    private class Fixture(val root: Path, private val token: String) {
        private val rootIdentity = Files.readAttributes(root, BasicFileAttributes::class.java, NOFOLLOW_LINKS).fileKey()
        private val allowedDirectories = mutableSetOf(root)
        private val allowedFiles = linkedMapOf(root.resolve(OWNER) to token.toByteArray(Charsets.UTF_8))
        val photo = PhotoLitePng.checked(syntheticPng())
        val snapshot = syntheticSnapshot(photo)
        private val base = BackupCoreStager().prepare(snapshot, root)
        private val mediaDirectory = Files.createDirectory(base.directory.resolve("media"))
        private val mediaEntry = BackupFileEntry.fromBytes(MEDIA_PATH, photo.copyBytes())
        val core = base.copy(entries = base.entries + mediaEntry)
        val manifest: BackupManifestV1

        init {
            Files.write(core.directory.resolve(MEDIA_PATH), photo.copyBytes(), CREATE_NEW, WRITE)
            manifest = BackupManifestV1.complete(core, "photo-lite-backup4-synthetic", Instant.parse(ARCHIVED_AT))
            allowedDirectories.add(core.directory)
            allowedDirectories.add(mediaDirectory)
            allowedFiles[core.directory.resolve(".backup-core-owner")] = Files.readAllBytes(core.directory.resolve(".backup-core-owner"))
            allowedFiles[core.directory.resolve("data.json")] = BackupJson.encodeData(snapshot.currentData())
            allowedFiles[core.directory.resolve("settings.json")] = BackupJson.encodeSettings(snapshot.currentSettings())
            allowedFiles[core.directory.resolve(MEDIA_PATH)] = photo.copyBytes()
        }

        fun archivePath(name: String): Path = root.resolve("$name.lithicbackup")
        fun rememberArchive(path: Path) {
            check(path.parent == root && !allowedFiles.containsKey(path))
            allowedFiles[path] = Files.readAllBytes(path)
        }

        fun extraction(name: String, mediaBytes: ByteArray = photo.copyBytes()): Path {
            val directory = Files.createDirectory(root.resolve(name))
            allowedDirectories.add(directory)
            // Admission can fail after writing a partial owned extraction. Only these known paths
            // may be cleaned if the entire negative test reaches its expected successful endpoint.
            allowedDirectories.add(directory.resolve("media"))
            payload(mediaBytes).forEach { (path, bytes) -> allowedFiles[directory.resolve(path)] = bytes }
            return directory
        }

        fun repack(name: String, omitMedia: Boolean = false, mediaBytes: ByteArray = photo.copyBytes()): Path {
            val path = archivePath(name)
            ZipOutputStream(Files.newOutputStream(path, CREATE_NEW, WRITE)).use { zip ->
                payload(mediaBytes).forEach { (entry, bytes) ->
                    if (!omitMedia || entry != MEDIA_PATH) {
                        zip.putNextEntry(ZipEntry(entry))
                        zip.write(bytes)
                        zip.closeEntry()
                    }
                }
            }
            rememberArchive(path)
            return path
        }

        private fun payload(mediaBytes: ByteArray): Map<String, ByteArray> = linkedMapOf(
            "manifest.json" to manifest.encode(),
            "data.json" to BackupJson.encodeData(snapshot.currentData()),
            "settings.json" to BackupJson.encodeSettings(snapshot.currentSettings()),
            MEDIA_PATH to mediaBytes,
        )

        fun closeAfterSuccess() {
            check(root.fileName.toString().startsWith("photo-lite-backup4-$token-") && root.toRealPath() == root)
            check(Files.readAttributes(root, BasicFileAttributes::class.java, NOFOLLOW_LINKS).fileKey() == rootIdentity)
            val nodes = Files.walk(root).use { it.iterator().asSequence().filter { path -> path != root }.toList() }
            // Complete ownership admission before deleting anything. Unknown nodes/bytes are retained.
            nodes.forEach { path ->
                check(!Files.isSymbolicLink(path) && path.toRealPath() == path && path.startsWith(root))
                if (Files.isDirectory(path, NOFOLLOW_LINKS)) check(path in allowedDirectories)
                else {
                    val expected = requireNotNull(allowedFiles[path]) { "Unknown synthetic fixture residue retained" }
                    check(Files.isRegularFile(path, NOFOLLOW_LINKS) && Files.size(path) == expected.size.toLong())
                    check(Files.readAllBytes(path).contentEquals(expected))
                }
            }
            assertArrayEquals(token.toByteArray(Charsets.UTF_8), Files.readAllBytes(root.resolve(OWNER)))
            core.close() // Frozen token/path/payload checks also have to pass before its first deletion.
            nodes.filter { !it.startsWith(core.directory) && it != root.resolve(OWNER) }
                .sortedByDescending { it.nameCount }.forEach(Files::delete)
            Files.delete(root.resolve(OWNER))
            Files.delete(root)
        }
    }

    private companion object {
        const val OWNER = ".photo-lite-backup4-owner"
        const val PROJECT_ID = "00000000-0000-4000-8000-000000000201"
        const val INITIAL_ID = "00000000-0000-4000-8000-000000000202"
        const val MEDIA_ID = "00000000-0000-4000-8000-000000000203"
        const val INITIAL_OPERATION = "00000000-0000-4000-8000-000000000204"
        const val RECOVERY_ID = "00000000-0000-4000-8000-000000000205"
        const val RECOVERY_OPERATION = "00000000-0000-4000-8000-000000000206"
        const val MEDIA_PATH = "media/$MEDIA_ID.png"
        const val CREATED_AT = "2026-10-01T00:00:00Z"
        const val ARCHIVED_AT = "2026-10-31T00:00:00Z"

        fun syntheticSnapshot(photo: PhotoLitePng): BackupSnapshot {
            val categories = listOf(
                BackupCategoryV1(LithicDatabase.PROJECT_FALLBACK_ID, "PROJECT", "其他", "其他", true),
                BackupCategoryV1(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, "SUBSCRIPTION", "其他", "其他", true),
            )
            val project = BackupProjectV1(PROJECT_ID, "PHOTO-LITE合成归档物品", LithicDatabase.PROJECT_FALLBACK_ID,
                "2026-10-01", null, null, MEDIA_ID, "归档测试，不是Room写入证据", CREATED_AT, CREATED_AT)
            val events = listOf(
                BackupProjectMoneyEventV2(INITIAL_ID, PROJECT_ID, "OUTLAY", 12_345, "2026-10-01", null,
                    "INITIAL", INITIAL_OPERATION, "CASH", null),
                BackupProjectMoneyEventV2(RECOVERY_ID, PROJECT_ID, "RECOVERY", 23_456, "2026-10-02", null,
                    "REGULAR", RECOVERY_OPERATION, "CASH", null),
            )
            val media = BackupMediaAssetV1(MEDIA_ID, MEDIA_PATH, "image/png", photo.width, photo.height, photo.byteLength, photo.sha256)
            return BackupSnapshot(BackupDataV2(categories, listOf(project), events, listOf(media), emptyList(),
                emptyList(), emptyList(), emptyList()), BackupSettingsV1("Asia/Shanghai"))
        }

        fun expectRejected(block: () -> Unit) {
            try { block(); fail("Incomplete registered media must not be admitted") }
            catch (_: IllegalArgumentException) { /* Explicit format/identity rejection. */ }
            catch (_: IOException) { /* Explicit missing/changed archive rejection. */ }
        }

        fun syntheticPng(): ByteArray {
            val output = ByteArrayOutputStream()
            output.write(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10))
            fun chunk(name: String, bytes: ByteArray) {
                val type = name.toByteArray(Charsets.US_ASCII)
                DataOutputStream(output).apply {
                    writeInt(bytes.size); write(type); write(bytes)
                    writeInt(CRC32().apply { update(type); update(bytes) }.value.toInt())
                }
            }
            val header = ByteArrayOutputStream().also { bytes ->
                DataOutputStream(bytes).apply { writeInt(2); writeInt(3); write(byteArrayOf(8, 6, 0, 0, 0)) }
            }.toByteArray()
            chunk("IHDR", header)
            val rows = ByteArrayOutputStream().also { bytes ->
                DeflaterOutputStream(bytes).use { compressed ->
                    repeat(3) {
                        compressed.write(0)
                        repeat(2) { compressed.write(byteArrayOf(24, 76, 53, 255.toByte())) }
                    }
                }
            }.toByteArray()
            chunk("IDAT", rows)
            chunk("IEND", byteArrayOf())
            return output.toByteArray()
        }
    }
}
