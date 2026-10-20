package com.cooper.lithic.data.backup

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.withTransaction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.LithicApplication
import com.cooper.lithic.app.MainActivity
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryNames
import com.cooper.lithic.data.local.CategoryType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Single admitted continuation of the exact M11 synthetic ledger; never seeds or resets a device. */
@RunWith(AndroidJUnit4::class)
class RealSafRestoreTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private var scenario: ActivityScenario<MainActivity>? = null
    private var originalAutomationFlags: Int? = null

    @After fun closeOnlyActivity() {
        scenario?.close()
        originalAutomationFlags?.let { flags -> automation.serviceInfo = automation.serviceInfo.apply { this.flags = flags } }
    }

    @Test fun actual_system_selection_preview_cancel_and_explicit_overwrite_restore_the_exact_m11_backup() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        check(arguments.getString("m12Isolated") == "true") { "Only the admitted existing M11 synthetic instance may run this test" }
        val resumeArgument = arguments.getString("m12ResumeClaim")
        check(resumeArgument == null || resumeArgument in setOf("true", "false"))
        val resume = resumeArgument == "true"
        val pickerFixArgument = arguments.getString("m12PickerSelectionFix")
        check(pickerFixArgument == null || pickerFixArgument in setOf("true", "false"))
        val pickerFix = pickerFixArgument == "true"
        check(!pickerFix || resume) { "The final picker correction requires the admitted original resume" }
        check(context.packageName == "com.cooper.lithic.debug" && Build.VERSION.SDK_INT == 36)
        check(Os.lstat(context.noBackupFilesDir.path).st_uid == Process.myUid())
        val bootId = shellRead("cat /proc/sys/kernel/random/boot_id", 128).decodeToString().trim()
        check(bootId == EXPECTED_BOOT) { "The admitted M11 emulator instance changed; no writes are permitted" }
        val oldEvidence = File(context.filesDir, "m11b-final-evidence")
        check(oldEvidence.isDirectory)
        val originalFiles = evidenceFiles(oldEvidence)
        val originalProof = File(oldEvidence, "saf-export.json")
        check(originalFiles.getValue("saf-export.json").sha256 == "4aa3d8d2f5a69c955e2e0069a36c879307fb42139a73dd42b50b031ccfd0ba52")
        val proof = JSONObject(readPrivate(originalProof, 64 * 1024).decodeToString())
        check(proof.getString("result") == "PASS" && proof.getString("filename") == FILENAME)
        check(proof.getLong("byteLength") == 3691L && proof.getString("sha256") == ARCHIVE_SHA)
        check(proof.getInt("uid") == Process.myUid() && Process.myUid() == 10216)
        val oldClaim = File(oldEvidence, proof.getString("claim"))
        check(oldClaim.name == "saf-export-resume.claim" && oldClaim.isFile)
        check(originalFiles.getValue(oldClaim.name).sha256 == "78e006232148f08be38bb360bd8a80b241170e49da8a2e6b18fac0c8ce89bf3a")
        val exportedClaim = JSONObject(readPrivate(oldClaim, 4096).decodeToString())
        check(exportedClaim.getString("filename") == FILENAME)
        val sourceArchive = File(oldEvidence, FILENAME)
        val archiveBytes = readPrivate(sourceArchive, 3691)
        check(archiveBytes.size == 3691 && sha(archiveBytes) == ARCHIVE_SHA)
        assertArrayEquals(archiveBytes, readSyntheticDownload())
        val (backup, media) = readKnownBackup(sourceArchive)
        val owner = (context.applicationContext as LithicApplication).appContainer
        val old = owner.openStorage()
        check(old.id == proof.getString("generation") && old.id == exportedClaim.getString("generation"))
        check(old.id == "5886ffc8-317c-4342-a5fe-1ac5f7f85767")
        val initial = old.backupSnapshotSource().capture()
        if (!resume) assertEquals(backup, initial)
        assertEquals(EXPECTED_COUNTS, backup.data.recordCounts())
        assertEquals("GMT", backup.settings.recordZoneId)
        val oldMediaIdentity = verifyMedia(old, media)
        val oldSettingsSha = sha(readPrivate(File(old.directory, "settings.preferences_pb"), 4096))

        // Both paths prove the current ledger before any write. Resume appends one new claim only;
        // the original failed attempt and its one sentinel remain immutable evidence.
        val evidence = File(context.filesDir, "m12-evidence")
        val claim = File(evidence, "saf-restore.claim")
        val sentinelId: String
        val claimBytes: ByteArray
        val claimIdentity: String
        val attemptClaim: File
        val attemptBytes: ByteArray
        val previousResumeClaim = File(evidence, "saf-restore-resume.claim")
        var previousResumeBytes: ByteArray? = null
        var previousResumeIdentity: String? = null
        if (resume) {
            val expectedEvidence = if (pickerFix) setOf("saf-restore.claim", "saf-restore-resume.claim") else setOf("saf-restore.claim")
            check(evidence.isDirectory && checkNotNull(evidence.list()).toSet() == expectedEvidence) {
                "Resume requires only the original failed claim and no prior successful or resumed run"
            }
            check(Os.lstat(claim.path).st_ino == 589844L) { "The independently observed original claim inode changed" }
            claimIdentity = identity(claim)
            claimBytes = readPrivate(claim, 4096)
            check(claimBytes.size == 407 && sha(claimBytes) == RESUME_CLAIM_SHA)
            val admitted = JSONObject(claimBytes.decodeToString())
            check(admitted.keys().asSequence().toSet() == setOf("filename", "archiveSha256", "sourceGeneration",
                "sentinelId", "bootId", "sourceProofSha256"))
            check(admitted.getString("filename") == FILENAME && admitted.getString("archiveSha256") == ARCHIVE_SHA)
            check(admitted.getString("sourceGeneration") == old.id && admitted.getString("bootId") == bootId)
            check(admitted.getString("sourceProofSha256") == originalFiles.getValue("saf-export.json").sha256)
            sentinelId = admitted.getString("sentinelId")
            check(sentinelId == "9ca647fb-2bb7-4ed0-897d-166173985d72" && UUID.fromString(sentinelId).toString() == sentinelId)
            val pointerFile = File(context.noBackupFilesDir, "active-generation")
            val pointerBytes = readPrivate(pointerFile, 4096)
            val pointer = JSONObject(pointerBytes.decodeToString())
            check(pointer.keys().asSequence().toSet() == setOf("formatVersion", "activeId", "previousId", "stage"))
            check(pointer.getInt("formatVersion") == 1 && pointer.getString("activeId") == old.id &&
                pointer.isNull("previousId") && pointer.getString("stage") == "STABLE")
            check(!File(context.noBackupFilesDir, "active-generation.bak").exists() &&
                !File(context.noBackupFilesDir, "active-generation.new").exists())
            assertEquals(backup.data.copy(categories = backup.data.categories + sentinelCategory(sentinelId)).ordered(), initial.data)
            assertEquals(backup.settings, initial.settings)
            assertEquals(3L, initial.data.recordCounts().getValue("categories"))
            assertEquals(claimIdentity, identity(claim))
            if (pickerFix) {
                check(Os.lstat(previousResumeClaim.path).st_ino == 590210L)
                val previousIdentity = identity(previousResumeClaim)
                val previousBytes = readPrivate(previousResumeClaim, 4096)
                check(previousBytes.size == 975 && sha(previousBytes) == PICKER_FIX_RESUME_SHA)
                val previous = JSONObject(previousBytes.decodeToString())
                check(previous.getString("mode") == "RESUME_AFTER_PICKER_HANDOFF_DRIVER_FAILURE")
                check(previous.getString("resumeOfClaimSha256") == RESUME_CLAIM_SHA &&
                    previous.getString("resumeOfClaimIdentity") == claimIdentity && previous.getLong("resumeOfClaimInode") == 589844L)
                check(previous.getString("filename") == FILENAME && previous.getString("archiveSha256") == ARCHIVE_SHA &&
                    previous.getString("sourceGeneration") == old.id && previous.getString("sentinelId") == sentinelId)
                check(previous.getInt("uid") == Process.myUid() && previous.getString("bootId") == bootId &&
                    previous.getString("currentPointerSha256") == sha(pointerBytes))
                check(checkNotNull(ownedBackupCacheDirectory(context).toFile().listFiles()).none { it.name.startsWith("lithic-restore-") }) {
                    "Unexpected restore staging is preserved; do not start another attempt"
                }
                assertEquals(previousIdentity, identity(previousResumeClaim))
                previousResumeBytes = previousBytes
                previousResumeIdentity = previousIdentity
                attemptClaim = File(evidence, "saf-restore-picker-fix.claim")
                attemptBytes = JSONObject().put("mode", "FINAL_EXACT_DOCUMENT_ITEM_SELECTION")
                    .put("originalClaimSha256", RESUME_CLAIM_SHA).put("originalClaimIdentity", claimIdentity)
                    .put("previousResumeSha256", PICKER_FIX_RESUME_SHA).put("previousResumeIdentity", previousIdentity)
                    .put("previousResumeInode", 590210L).put("uid", Process.myUid()).put("bootId", bootId)
                    .put("sourceGeneration", old.id).put("sentinelId", sentinelId).put("filename", FILENAME)
                    .put("archiveSha256", ARCHIVE_SHA).put("currentPointerSha256", sha(pointerBytes))
                    .put("sourceFailures", JSONArray(listOf("final-real-saf-restore.stdout", "final-real-saf-restore-resume.stdout")))
                    .toString().toByteArray()
            } else {
                attemptClaim = previousResumeClaim
                attemptBytes = JSONObject().put("mode", "RESUME_AFTER_PICKER_HANDOFF_DRIVER_FAILURE")
                    .put("resumeOfClaimSha256", RESUME_CLAIM_SHA).put("resumeOfClaimIdentity", claimIdentity)
                    .put("resumeOfClaimInode", 589844L).put("sourceFailure", "final-real-saf-restore.stdout")
                    .put("filename", FILENAME).put("archiveSha256", ARCHIVE_SHA).put("sourceGeneration", old.id)
                    .put("sentinelId", sentinelId).put("uid", Process.myUid()).put("bootId", bootId)
                    .put("sourceProofSha256", originalFiles.getValue("saf-export.json").sha256)
                    .put("currentPointerSha256", sha(pointerBytes)).put("beforeCounts", JSONObject(initial.data.recordCounts()))
                    .toString().toByteArray()
            }
            writeNew(attemptClaim, attemptBytes)
        } else {
            check(evidence.isDirectory || evidence.mkdir())
            sentinelId = UUID.randomUUID().toString()
            claimBytes = JSONObject().put("filename", FILENAME).put("archiveSha256", ARCHIVE_SHA)
                .put("sourceGeneration", old.id).put("sentinelId", sentinelId).put("bootId", bootId)
                .put("sourceProofSha256", originalFiles.getValue("saf-export.json").sha256)
                .toString().toByteArray()
            writeNew(claim, claimBytes)
            claimIdentity = identity(claim)
            attemptClaim = claim
            attemptBytes = claimBytes
        }
        val attemptIdentity = identity(attemptClaim)
        val sentinelName = "M12 SAF 合成哨兵 $sentinelId"
        if (!resume) old.coordinator.withWrite {
            old.database.withTransaction {
                old.database.categoryDao().insert(CategoryEntity(sentinelId, CategoryType.PROJECT,
                    sentinelName, CategoryNames.normalized(sentinelName), false))
            }
        }
        val before = old.backupSnapshotSource().capture()
        assertEquals(backup.data.copy(categories = backup.data.categories + BackupCategoryV1(sentinelId, "PROJECT",
            sentinelName, CategoryNames.normalized(sentinelName), false)).ordered(), before.data)
        assertEquals(3L, before.data.recordCounts().getValue("categories"))
        assertEquals(backup.settings, before.settings)
        val flags = automation.serviceInfo
        originalAutomationFlags = flags.flags
        flags.flags = flags.flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        automation.serviceInfo = flags
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("tab-settings").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.onNodeWithTag("tab-settings").performClick()

        selectActualBackup()
        verifyPreview()
        assertSame(old, owner.openStorage())
        assertEquals(before, old.backupSnapshotSource().capture())
        assertEquals(oldMediaIdentity, verifyMedia(old, media))
        compose.onNodeWithTag("discard-backup-restore").performScrollTo().performClick()
        val cancelled = waitFeedback("已取消恢复")
        assertTrue(cancelled.contains("当前记录未改变"))
        assertFalse(cancelled.contains("暂未清理") || cancelled.contains("可能仍保留"))
        assertSame(old, owner.openStorage())
        assertEquals(before, old.backupSnapshotSource().capture())

        selectActualBackup()
        verifyPreview()
        assertSame(old, owner.openStorage())
        assertEquals(before, old.backupSnapshotSource().capture())
        compose.onNodeWithTag("confirm-backup-restore").performScrollTo().performClick()
        val success = waitFeedback("恢复成功")
        val restored = owner.openStorage()
        assertNotEquals(old.id, restored.id)
        assertEquals(backup, restored.backupSnapshotSource().capture())
        assertNull(restored.database.categoryDao().find(sentinelId))
        assertEquals("GMT", restored.settings.recordZone().id)
        val newMediaIdentity = verifyMedia(restored, media)
        media.keys.forEach { assertNotEquals(oldMediaIdentity.getValue(it), newMediaIdentity.getValue(it)) }
        assertTrue(old.directory.isDirectory)
        verifyKeptOldDatabase(old, sentinelId)
        assertEquals(oldMediaIdentity, verifyMedia(old, media))
        assertEquals(oldSettingsSha, sha(readPrivate(File(old.directory, "settings.preferences_pb"), 4096)))
        assertEquals(originalFiles, evidenceFiles(oldEvidence))
        assertArrayEquals(archiveBytes, readSyntheticDownload())
        assertArrayEquals(claimBytes, readPrivate(claim, 4096))
        assertEquals(claimIdentity, identity(claim))
        assertArrayEquals(attemptBytes, readPrivate(attemptClaim, 4096))
        assertEquals(attemptIdentity, identity(attemptClaim))
        previousResumeBytes?.let { assertArrayEquals(it, readPrivate(previousResumeClaim, 4096)) }
        previousResumeIdentity?.let { assertEquals(it, identity(previousResumeClaim)) }

        val screenshot = checkNotNull(automation.takeScreenshot())
        try {
            val file = File(evidence, when { pickerFix -> "settings-saf-restore-picker-fix-success.png"; resume -> "settings-saf-restore-resume-success.png"; else -> "settings-saf-restore-success.png" })
            check(file.createNewFile())
            FileOutputStream(file).use { check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)); it.fd.sync() }
        } finally { screenshot.recycle() }
        val files = JSONArray()
        media.forEach { (path, bytes) -> files.put(JSONObject().put("path", path).put("byteLength", bytes.size).put("sha256", sha(bytes))) }
        writeNew(File(evidence, when { pickerFix -> "saf-restore-picker-fix.json"; resume -> "saf-restore-resume.json"; else -> "saf-restore.json" }), JSONObject().put("result", "PASS")
            .put("mode", when { pickerFix -> "PICKER_SELECTION_FIX"; resume -> "RESUME"; else -> "ORIGINAL" }).put("claim", attemptClaim.name)
            .put("resumeOfClaimSha256", if (resume) RESUME_CLAIM_SHA else JSONObject.NULL)
            .put("previousResumeSha256", if (pickerFix) PICKER_FIX_RESUME_SHA else JSONObject.NULL)
            .put("sourceFailures", if (pickerFix) JSONArray(listOf("final-real-saf-restore.stdout", "final-real-saf-restore-resume.stdout")) else JSONArray())
            .put("sourceFailure", if (resume) "final-real-saf-restore.stdout" else JSONObject.NULL)
            .put("realMainActivity", true).put("realOpenDocumentPickerSelections", 2)
            .put("filename", FILENAME).put("archiveSha256", ARCHIVE_SHA).put("bootId", bootId)
            .put("uid", Process.myUid()).put("sourceGeneration", old.id).put("restoredGeneration", restored.id)
            .put("sentinelId", sentinelId).put("beforeCounts", JSONObject(before.data.recordCounts()))
            .put("restoredCounts", JSONObject(backup.data.recordCounts())).put("recordZoneId", "GMT")
            .put("cancelFeedback", cancelled).put("successFeedback", success)
            .put("unchangedBeforeConfirmation", true).put("cancellationKeptCurrentAndCleanedKnownPreview", true)
            .put("fullOverwriteWithoutMerge", true).put("allSevenTablesAndSettingsEqual", true)
            .put("allFiveImagesOriginalBytes", true).put("oldGenerationAndSentinelKept", true)
            .put("m11EvidenceAndOriginalClaimsUnchanged", true).put("restoreClaimUnchanged", true)
            .put("files", files).toString(2).toByteArray())
    }

    private fun selectActualBackup() {
        compose.onNodeWithTag("restore-complete-backup").performScrollTo().performClick()
        waitForNode("actual OpenDocument picker") { isDocuments(it) && it.isVisibleToUser }
        val drawer = waitForNode("DocumentsUI roots drawer") {
            isDocuments(it) && clickableAncestor(it) != null && it.contentDescription?.toString() in
                setOf("Show roots", "显示根目录", "显示导航栏", "Open navigation drawer", "打开导航抽屉")
        }
        check(clickableAncestor(drawer)!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        val downloads = waitForNode("actionable Downloads root") {
            isDocuments(it) && !insideToolbar(it) && it.text?.toString() in setOf("Downloads", "下载") && clickableAncestor(it) != null
        }
        check(clickableAncestor(downloads)!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        val source = waitForNode("the exact admitted backup filename") {
            isDocuments(it) && it.viewIdResourceName == "android:id/title" && it.text?.toString() == FILENAME &&
                documentItemForTitle(it) != null
        }
        check(checkNotNull(documentItemForTitle(source)).performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun verifyPreview() {
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("restore-preview").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        compose.onNodeWithTag("restore-count-categories").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("当前 3 · 备份 2").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("完整覆盖当前记录，不合并。当前记录、图片和设置将被备份内容替换。").performScrollTo().assertIsDisplayed()
    }

    private fun waitFeedback(expected: String): String {
        var result = ""
        compose.waitUntil(45_000) {
            result = listOf("backup-restore-feedback", "backup-restore-global-feedback").flatMap { tag ->
                compose.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).map { node ->
                    node.config[SemanticsProperties.Text].joinToString("") { it.text }
                }
            }.firstOrNull { it.contains(expected) }.orEmpty()
            result.isNotEmpty()
        }
        return result
    }

    private fun readKnownBackup(archive: File): Pair<BackupSnapshot, Map<String, ByteArray>> = ZipFile(archive).use { zip ->
        val paths = setOf("media/m11b-jpeg.jpg", "media/m11b-rgba.png", "media/m11b-palette.png", "media/m11b-gray16.png", "media/m11b-adam7.png")
        assertEquals(setOf("manifest.json", "data.json", "settings.json") + paths, zip.entries().asSequence().map { it.name }.toSet())
        val data = Json.decodeFromString<BackupDataV1>(zip.getInputStream(zip.getEntry("data.json")).use { it.reader().readText() })
        val settings = Json.decodeFromString<BackupSettingsV1>(zip.getInputStream(zip.getEntry("settings.json")).use { it.reader().readText() })
        val media = paths.associateWith { zip.getInputStream(zip.getEntry(it)).use { input -> input.readBytes() } }
        data.mediaAssets.forEach { item -> assertEquals(item.byteLength, media.getValue(item.relativePath).size.toLong()); assertEquals(item.sha256, sha(media.getValue(item.relativePath))) }
        BackupSnapshot(data, settings) to media
    }

    private fun sentinelCategory(id: String): BackupCategoryV1 {
        val name = "M12 SAF 合成哨兵 $id"
        return BackupCategoryV1(id, "PROJECT", name, CategoryNames.normalized(name), false)
    }

    private fun verifyMedia(generation: ManagedGeneration, media: Map<String, ByteArray>): Map<String, String> = media.mapValues { (path, bytes) ->
        val file = File(generation.directory, path)
        assertEquals(file.absoluteFile, file.canonicalFile)
        assertEquals(File(generation.directory, "media").canonicalFile, file.canonicalFile.parentFile)
        assertArrayEquals(bytes, readPrivate(file, 2 * 1024 * 1024))
        identity(file)
    }

    private fun verifyKeptOldDatabase(old: ManagedGeneration, sentinel: String) {
        SQLiteDatabase.openDatabase(File(old.directory, "data.db").path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
            database.rawQuery("PRAGMA integrity_check", null).use { assertTrue(it.moveToFirst()); assertEquals("ok", it.getString(0)) }
            database.rawQuery("PRAGMA foreign_key_check", null).use { assertFalse(it.moveToFirst()) }
            EXPECTED_COUNTS.forEach { (table, count) -> database.rawQuery("SELECT COUNT(*) FROM $table", null).use {
                assertTrue(it.moveToFirst()); assertEquals(if (table == "categories") 3L else count, it.getLong(0))
            } }
            database.rawQuery("SELECT COUNT(*) FROM categories WHERE id = ?", arrayOf(sentinel)).use { assertTrue(it.moveToFirst()); assertEquals(1L, it.getLong(0)) }
        }
    }

    private data class EvidenceFile(val size: Long, val sha256: String, val identity: String)
    private fun evidenceFiles(directory: File): Map<String, EvidenceFile> {
        val files = checkNotNull(directory.listFiles())
        check(files.size in 1..32)
        return files.associate { file ->
            val bytes = readPrivate(file, 2 * 1024 * 1024)
            file.name to EvidenceFile(bytes.size.toLong(), sha(bytes), identity(file))
        }
    }

    private fun readPrivate(file: File, limit: Int): ByteArray {
        val before = identity(file)
        check(file.length() in 1..limit.toLong())
        val bytes = file.inputStream().use { it.readNBytes(limit + 1) }
        check(bytes.size <= limit && bytes.size.toLong() == file.length() && before == identity(file))
        return bytes
    }

    private fun identity(file: File): String = Os.lstat(file.path).let {
        check(OsConstants.S_ISREG(it.st_mode) && it.st_uid == Process.myUid() && it.st_nlink == 1L)
        listOf(it.st_dev, it.st_ino, it.st_uid.toLong(), it.st_size, it.st_mtime, it.st_ctime).joinToString(":")
    }

    private fun readSyntheticDownload() = shellRead("cat /sdcard/Download/$FILENAME", 3691).also { check(it.size == 3691 && sha(it) == ARCHIVE_SHA) }
    private fun shellRead(command: String, limit: Int): ByteArray = ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { input ->
        val result = ByteArrayOutputStream(); val buffer = ByteArray(4096)
        while (true) { val count = input.read(buffer); if (count < 0) break; check(count > 0 && result.size() + count <= limit); result.write(buffer, 0, count) }
        result.toByteArray()
    }

    private fun writeNew(file: File, bytes: ByteArray) {
        check(file.createNewFile()) { "A prior attempt exists; do not overwrite claims or automatically retry" }
        FileOutputStream(file).use { it.write(bytes); it.flush(); it.fd.sync() }
    }
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun waitForNode(label: String, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) { findNode(predicate)?.let { return it }; Thread.sleep(100) }
        throw AssertionError("No accessible match for $label; no automatic retry")
    }
    private fun findNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun visit(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (predicate(node)) return node
            for (index in 0 until node.childCount) node.getChild(index)?.let { child -> visit(child)?.let { return it } }
            return null
        }
        return automation.rootInActiveWindow?.let(::visit)
    }
    private fun clickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        repeat(4) { current?.let { if (it.isVisibleToUser && it.isEnabled && it.isClickable) return it; current = it.parent } }
        return null
    }
    /** DocumentsUI's separate preview_icon is actionable too; only item_root selects a document. */
    private fun documentItemForTitle(title: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (!isDocuments(title) || title.viewIdResourceName != "android:id/title" || title.text?.toString() != FILENAME ||
            !title.isVisibleToUser || !title.isEnabled) return null
        var current = title.parent
        repeat(12) {
            val node = current ?: return null
            if (!isDocuments(node) || node.viewIdResourceName?.endsWith(":id/preview_icon") == true) return null
            if (node.viewIdResourceName?.endsWith(":id/item_root") == true) {
                return node.takeIf { it.isVisibleToUser && it.isEnabled && it.isClickable }
            }
            current = node.parent
        }
        return null
    }
    private fun isDocuments(node: AccessibilityNodeInfo) = node.packageName?.toString() in setOf("com.android.documentsui", "com.google.android.documentsui")
    private fun insideToolbar(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        repeat(8) { current?.let { if (it.viewIdResourceName?.contains("toolbar", true) == true || it.className?.toString()?.endsWith("Toolbar") == true) return true; current = it.parent } }
        return false
    }

    companion object {
        private const val EXPECTED_BOOT = "837312dd-125c-41f8-92a9-eb2d96e7795e"
        private const val FILENAME = "m11b-synthetic-0a21510d-ab12-4dda-a226-70848d48865f.lithicbackup"
        private const val ARCHIVE_SHA = "761efcabd55718d0ea6f9bec56f9e4ec490da6766b3dbca0c82a4fba96be5818"
        private const val RESUME_CLAIM_SHA = "43b4a7a283ddc46846c4f090eea40006fab59630ad80305de6d27fab04ffb2a7"
        private const val PICKER_FIX_RESUME_SHA = "f3d8b77eb4d19dbebe684c4be1763f71feda3818a626bb53943c654a7cfd9772"
        private val EXPECTED_COUNTS = mapOf("categories" to 2L, "long_term_projects" to 1L, "project_money_events" to 1L,
            "local_media_assets" to 5L, "subscriptions" to 1L, "subscription_renewals" to 1L, "subscription_state_changes" to 1L)
    }
}
