package com.cooper.lithic.data.backup

import android.content.Context
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import android.system.Os
import android.system.OsConstants
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.LithicApplication
import com.cooper.lithic.app.MainActivity
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.*
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs only on a separately admitted, newly installed synthetic M11-B device. No owner replacement. */
@RunWith(AndroidJUnit4::class)
class RealSafBackupExportTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var generation: ManagedGeneration
    private lateinit var evidence: File
    private var scenario: ActivityScenario<MainActivity>? = null
    private var originalAutomationFlags: Int? = null

    @Before fun isolatedDeviceOnly() = runBlocking {
        check(InstrumentationRegistry.getArguments().getString("m11bIsolated") == "true") {
            "This test may seed only an independently admitted, empty synthetic M11-B device"
        }
        check(context.packageName == "com.cooper.lithic.debug")
        check(Os.lstat(context.noBackupFilesDir.path).st_uid == Process.myUid())
        val serviceInfo = automation.serviceInfo
        originalAutomationFlags = serviceInfo.flags
        serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        automation.serviceInfo = serviceInfo
        generation = (context.applicationContext as LithicApplication).appContainer.openStorage()
        evidence = File(context.filesDir, "m11b-final-evidence")
        check(evidence.isDirectory || evidence.mkdir())
    }

    @After fun closeOnlyActivity() {
        scenario?.close()
        originalAutomationFlags?.let { flags -> automation.serviceInfo = automation.serviceInfo.apply { this.flags = flags } }
    }

    @Test fun settings_document_picker_cancel_never_reports_success() = runBlocking {
        val before = generation.backupSnapshotSource().capture()
        launchSettingsAndPicker()
        waitForDocumentPicker()
        instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        val feedback = waitForFeedback()
        assertEquals("已取消选择保存位置，未导出备份。", feedback)
        assertFalse(feedback.contains("完整备份已导出"))
        assertEquals(before, generation.backupSnapshotSource().capture())
        writeEvidence("saf-cancel.json", JSONObject().put("result", "PASS")
            .put("realMainActivity", true).put("realSystemPicker", true)
            .put("feedback", feedback).put("businessUnchanged", true)
            .put("generation", generation.id).put("uid", Process.myUid()))
    }

    @Test fun settings_exports_all_seven_tables_settings_and_registered_images_through_system_document_picker() = runBlocking {
        exportAndVerifyThroughActualSaf(seedOnlyEmptySyntheticLedger(), "saf-export.claim")
    }

    @Test fun settings_exports_the_proven_synthetic_ledger_with_a_new_explicit_resume_claim() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val oldClaim = File(evidence, "saf-export.claim")
        check(oldClaim.isFile && oldClaim.length() in 1..4096)
        val bytes = oldClaim.readBytes()
        val claimSha = requireNotNull(arguments.getString("m11bResumeClaimSha256"))
        check(Regex("[0-9a-f]{64}").matches(claimSha) && sha(bytes) == claimSha) {
            "Only the independently observed failed picker claim is admitted; never reset or replace it"
        }
        val claim = JSONObject(bytes.decodeToString())
        check(claim.keys().asSequence().toSet() == setOf("filename", "generation"))
        check(Regex("m11b-synthetic-[a-f0-9-]{36}\\.lithicbackup").matches(claim.getString("filename")))
        check(requireNotNull(arguments.getString("m11bResumeGenerationId")) == generation.id && claim.getString("generation") == generation.id)
        val media = verifyExactPreviouslySeededSyntheticLedger()
        exportAndVerifyThroughActualSaf(media, "saf-export-resume.claim", claimSha)
        assertArrayEquals(bytes, oldClaim.readBytes())
    }

    private suspend fun exportAndVerifyThroughActualSaf(media: Map<String, ByteArray>, claimName: String, resumeOfClaimSha: String? = null) {
        val before = generation.backupSnapshotSource().capture()
        val beforeIdentities = media.keys.associateWith { identity(File(generation.directory, it)) }
        val filename = "m11b-synthetic-${UUID.randomUUID()}.lithicbackup"
        val claim = File(evidence, claimName)
        check(claim.createNewFile()) { "The real SAF export is single-attempt; old evidence must not be overwritten" }
        claim.writeText(JSONObject().put("filename", filename).put("generation", generation.id)
            .apply { resumeOfClaimSha?.let { put("resumeOfClaimSha256", it) } }.toString())
        launchSettingsAndPicker()
        waitForDocumentPicker()
        chooseDownloadsAndSave(filename)
        val feedback = waitForFeedback()
        assertTrue("Actual settings feedback: $feedback", feedback.startsWith("完整备份已导出并通过校验。"))

        // The real production service already closed its SAF output and reopened it through
        // ContentResolver before publishing success. This is a separate, bounded shell read of
        // this test's unique public synthetic file, not a second claimed ContentResolver proof.
        val bytes = readOnlySyntheticDownload(filename)
        val archive = File(evidence, filename)
        check(archive.createNewFile()); archive.writeBytes(bytes)
        val payloadEvidence = JSONArray()
        ZipFile(archive).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toList()
            assertEquals(names.size, names.toSet().size)
            assertEquals(setOf("manifest.json", "data.json", "settings.json") + media.keys, names.toSet())
            val dataBytes = zip.getInputStream(zip.getEntry("data.json")).use { it.readBytes() }
            val settingsBytes = zip.getInputStream(zip.getEntry("settings.json")).use { it.readBytes() }
            assertEquals(before.data, Json.decodeFromString<BackupDataV2>(dataBytes.decodeToString()))
            assertEquals(before.settings, Json.decodeFromString<BackupSettingsV1>(settingsBytes.decodeToString()))
            val manifest = Json.parseToJsonElement(zip.getInputStream(zip.getEntry("manifest.json")).use { it.reader().readText() }).jsonObject
            assertEquals(2, manifest.getValue("formatVersion").jsonPrimitive.int)
            val counts = manifest.getValue("recordCounts").jsonObject
            assertEquals(before.data.recordCounts(), counts.mapValues { it.value.jsonPrimitive.long })
            assertEquals(7, counts.size)
            assertTrue(counts.values.all { it.jsonPrimitive.long > 0 })
            val entries = manifest.getValue("files").jsonArray
            assertEquals(setOf("data.json", "settings.json") + media.keys,
                entries.map { it.jsonObject.getValue("path").jsonPrimitive.content }.toSet())
            entries.forEach { item ->
                val entry = item.jsonObject
                val path = entry.getValue("path").jsonPrimitive.content
                val payload = zip.getInputStream(zip.getEntry(path)).use { it.readBytes() }
                assertEquals(payload.size.toLong(), entry.getValue("byteLength").jsonPrimitive.long)
                assertEquals(sha(payload), entry.getValue("sha256").jsonPrimitive.content)
                media[path]?.let { assertArrayEquals(it, payload) }
                payloadEvidence.put(JSONObject().put("path", path).put("byteLength", payload.size).put("sha256", sha(payload)))
            }
        }
        assertEquals(before, generation.backupSnapshotSource().capture())
        media.forEach { (path, original) ->
            assertArrayEquals(original, File(generation.directory, path).readBytes())
            assertEquals(beforeIdentities.getValue(path), identity(File(generation.directory, path)))
        }
        writeEvidence("saf-export.json", JSONObject().put("result", "PASS")
            .put("realMainActivity", true).put("realSystemPicker", true)
            .put("productionCloseAndContentResolverReadback", "success feedback occurs only after actual service verification")
            .put("independentArchiveRead", "UiAutomation shell; only unique synthetic Downloads file; <= 1 MiB")
            .put("filename", filename).put("byteLength", bytes.size).put("sha256", sha(bytes))
            .put("generation", generation.id).put("uid", Process.myUid())
            .put("claim", claimName).put("resumeOfClaimSha256", resumeOfClaimSha ?: JSONObject.NULL)
            .put("feedback", feedback).put("sevenTablesAndSettingsEqual", true)
            .put("allRegisteredMediaByteIdentical", true).put("sourceIdentityAndBusinessUnchanged", true)
            .put("files", payloadEvidence))
        // The export result remains independently recorded above even if supplementary screenshot
        // capture fails. A capture failure is not swallowed or mislabeled as a backup failure.
        try {
            compose.onNodeWithTag("backup-export-feedback").performScrollTo()
            compose.waitForIdle()
            val screenshot = checkNotNull(automation.takeScreenshot()) { "UiAutomation did not return a screenshot" }
            try {
                val file = File(evidence, "settings-saf-success.png")
                check(file.createNewFile())
                FileOutputStream(file).use { check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally { screenshot.recycle() }
        } catch (failure: Exception) {
            writeEvidence("settings-saf-success-screenshot-error.json", JSONObject().put("exportResult", "PASS")
                .put("screenshotResult", "FAIL").put("error", failure.javaClass.name).put("message", failure.message))
            throw failure
        }
    }

    private fun launchSettingsAndPicker() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(20_000) { compose.onAllNodesWithTag("tab-settings").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("tab-settings").performClick()
        compose.onNodeWithTag("export-complete-backup").performScrollTo().performClick()
    }

    private fun waitForDocumentPicker() {
        waitForNode("System DocumentsUI must actually open") { node ->
            node.packageName?.toString()?.let { it == "com.android.documentsui" || it == "com.google.android.documentsui" } == true &&
                node.className?.toString() == "android.widget.EditText"
        }
    }

    private fun chooseDownloadsAndSave(filename: String) {
        try {
        // Select the actual Downloads root, rather than trusting an inherited picker location.
        val drawer = waitForNode("DocumentsUI root drawer") { node ->
            isDocumentsNode(node) && node.isVisibleToUser && clickableAncestor(node) != null &&
                node.contentDescription?.toString() in setOf("Show roots", "显示根目录", "显示导航栏", "Open navigation drawer", "打开导航抽屉")
        }
        assertTrue(clickNodeOrParent(drawer))
        val downloads = waitForNode("Visible actionable Downloads root, not a toolbar title") {
            isDocumentsNode(it) && it.isVisibleToUser && it.isEnabled && !insideToolbar(it) &&
                it.text?.toString() in setOf("Downloads", "下载") && clickableAncestor(it) != null
        }
        assertTrue(clickNodeOrParent(downloads))
        val edit = waitForNode("Actual create-document filename") { isDocumentsNode(it) && it.isVisibleToUser && it.className?.toString() == "android.widget.EditText" }
        assertTrue(edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, filename)
        }))
        val save = waitForNode("Actual DocumentsUI Save button") {
            isDocumentsNode(it) && it.isVisibleToUser && it.isEnabled && clickableAncestor(it) != null &&
                (it.viewIdResourceName?.endsWith(":id/button1") == true || it.text?.toString() in setOf("SAVE", "Save", "保存"))
        }
        assertTrue(clickNodeOrParent(save))
        } catch (failure: Throwable) {
            try { writePickerFailure(filename, failure) } catch (evidenceFailure: Throwable) { failure.addSuppressed(evidenceFailure) }
            throw failure
        }
    }

    private fun waitForFeedback(): String {
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("backup-export-feedback").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("backup-export-feedback").performScrollTo()
        val node = compose.onNodeWithTag("backup-export-feedback").fetchSemanticsNode()
        return node.config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString("") { it.text }
    }

    private fun waitForNode(label: String, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = android.os.SystemClock.elapsedRealtime() + 15_000
        while (android.os.SystemClock.elapsedRealtime() < deadline) {
            findNode(predicate)?.let { return it }
            Thread.sleep(100)
        }
        throw AssertionError("No complete accessible match for $label; no automatic retry")
    }

    private fun findNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun visit(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (predicate(node)) return node
            for (index in 0 until node.childCount) node.getChild(index)?.let { child -> visit(child)?.let { return it } }
            return null
        }
        return automation.rootInActiveWindow?.let(::visit)
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        return clickableAncestor(node)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
    }

    private fun clickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        repeat(4) { current?.let { if (it.isVisibleToUser && it.isEnabled && it.isClickable) return it; current = it.parent } }
        return null
    }

    private fun isDocumentsNode(node: AccessibilityNodeInfo) = node.packageName?.toString() in setOf("com.android.documentsui", "com.google.android.documentsui")

    private fun insideToolbar(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        repeat(8) { current?.let {
            if (it.viewIdResourceName?.contains("toolbar", ignoreCase = true) == true || it.className?.toString()?.endsWith("Toolbar") == true) return true
            current = it.parent
        } }
        return false
    }

    private fun writePickerFailure(filename: String, failure: Throwable) {
        val nodes = JSONArray()
        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > 16 || nodes.length() >= 128) return
            if (isDocumentsNode(node)) nodes.put(JSONObject().put("id", node.viewIdResourceName).put("class", node.className)
                .put("text", node.text?.toString()?.take(96)).put("description", node.contentDescription?.toString()?.take(96))
                .put("visible", node.isVisibleToUser).put("enabled", node.isEnabled).put("clickable", node.isClickable))
            for (index in 0 until node.childCount) node.getChild(index)?.let { visit(it, depth + 1) }
        }
        automation.rootInActiveWindow?.let { visit(it, 0) }
        writeEvidence("picker-failure-${UUID.randomUUID()}.json", JSONObject().put("filename", filename)
            .put("error", failure.javaClass.name).put("message", failure.message).put("documentsUiNodesOnly", nodes))
    }

    private fun readOnlySyntheticDownload(filename: String): ByteArray {
        check(Regex("m11b-synthetic-[a-f0-9-]{36}\\.lithicbackup").matches(filename))
        if (Build.VERSION.SDK_INT < 34) error("The real SAF evidence read is only admitted on this task's new API36 device")
        val marker = "M11B_READ_${UUID.randomUUID().toString().replace("-", "")}".toByteArray()
        val path = "/sdcard/Download/$filename"
        // Supply the script through stdin. UiAutomation's command tokenizer must not be assumed
        // to implement shell quoting for a compound `sh -c '...'` command string.
        val channels = automation.executeShellCommandRwe("sh")
        check(channels.size == 3)
        try {
        ParcelFileDescriptor.AutoCloseOutputStream(channels[1]).use { input ->
            input.write("cat \"$path\"\nresult=\$?\nprintf '\\n${marker.decodeToString()}:%s\\n' \"\$result\"\nexit \"\$result\"\n".toByteArray())
        }
        val all = ParcelFileDescriptor.AutoCloseInputStream(channels[0]).use { input ->
            val output = ByteArrayOutputStream(); val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer); if (count < 0) break
                check(output.size() + count <= 1024 * 1024) { "Synthetic archive read exceeded its 1 MiB evidence bound" }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val stderr = ParcelFileDescriptor.AutoCloseInputStream(channels[2]).use { error ->
            val bytes = error.readNBytes(4097)
            check(bytes.size <= 4096) { "Synthetic shell stderr exceeded its evidence bound" }
            bytes
        }
        check(stderr.isEmpty()) { "Synthetic archive read emitted stderr: ${stderr.decodeToString()}" }
        val trailer = "\n${marker.decodeToString()}:0\n".toByteArray()
        check(all.size > trailer.size && all.takeLast(trailer.size).toByteArray().contentEquals(trailer)) { "Synthetic shell read failed or lacked complete exit marker" }
        return all.copyOf(all.size - trailer.size)
        } finally {
            channels.forEach { if (it.fileDescriptor.valid()) it.close() }
        }
    }

    private suspend fun seedOnlyEmptySyntheticLedger(): Map<String, ByteArray> {
        val empty = generation.backupSnapshotSource().capture().data
        check(empty.categories.size == 2 && empty.categories.all { it.isFallback } &&
            empty.projects.isEmpty() && empty.projectMoneyEvents.isEmpty() && empty.mediaAssets.isEmpty() &&
            empty.subscriptions.isEmpty() && empty.subscriptionRenewals.isEmpty() && empty.subscriptionStateChanges.isEmpty()) {
            "Refusing to seed an existing ledger"
        }
        val media = syntheticMediaFixtures()
        generation.coordinator.withWrite {
            val at = Instant.parse("2026-01-01T00:00:00Z"); val day = LocalDate.of(2026, 1, 1)
            val assets = media.map { (path, bytes) ->
                val file = File(generation.directory, path); check(file.createNewFile()); file.writeBytes(bytes)
                LocalMediaAssetEntity(id(), path, if (path.endsWith(".jpg")) "image/jpeg" else "image/png",
                    if (path.endsWith(".jpg")) 4 else 11, if (path.endsWith(".jpg")) 3 else 9, bytes.size.toLong(), sha(bytes))
            }
            assets.forEach { generation.database.mediaDao().insert(it) }
            val project = LongTermProjectEntity(id(), "M11-B SAF 合成项目", LithicDatabase.PROJECT_FALLBACK_ID, day,
                coverAssetId = assets[0].id, createdAt = at, updatedAt = at)
            val subscription = SubscriptionEntity(id(), "M11-B SAF 合成订阅", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
                assets[1].id, createdAt = at, updatedAt = at)
            generation.database.projectDao().insertProject(project)
            generation.database.projectDao().insertEvent(ProjectMoneyEventEntity(id(), project.id, MoneyDirection.OUTLAY,
                12345, day, null, ProjectMoneyEventRole.INITIAL, id()))
            generation.database.subscriptionDao().insertSubscription(subscription)
            generation.database.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(id(), subscription.id, day,
                day, day.plusMonths(1), day.plusMonths(1), 18000, BillingCycle.MONTHLY, 30, id()))
            generation.database.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(), subscription.id,
                day, SubscriptionOperationState.ENABLED, operationId = id()))
        }
        return media
    }

    /** Narrow test continuation only: no writes, repairs, deletion, re-seeding or owner replacement. */
    private suspend fun verifyExactPreviouslySeededSyntheticLedger(): Map<String, ByteArray> {
        val before = generation.backupSnapshotSource().capture()
        val data = before.data
        check(data.recordCounts() == mapOf("categories" to 2L, "long_term_projects" to 1L, "project_money_events" to 1L,
            "local_media_assets" to 5L, "subscriptions" to 1L, "subscription_renewals" to 1L, "subscription_state_changes" to 1L))
        check(data.categories.toSet() == setOf(
            BackupCategoryV1(LithicDatabase.PROJECT_FALLBACK_ID, "PROJECT", "其他", "其他", true),
            BackupCategoryV1(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, "SUBSCRIPTION", "其他", "其他", true)))
        val media = syntheticMediaFixtures()
        check(data.mediaAssets.map { it.relativePath }.toSet() == media.keys)
        data.mediaAssets.forEach { asset ->
            val expected = media.getValue(asset.relativePath)
            val jpeg = asset.relativePath.endsWith(".jpg")
            check(asset.mime == if (jpeg) "image/jpeg" else "image/png")
            check(asset.width == if (jpeg) 4 else 11)
            check(asset.height == if (jpeg) 3 else 9)
            check(asset.byteLength == expected.size.toLong() && asset.sha256 == sha(expected))
            val file = File(generation.directory, asset.relativePath)
            check(file.canonicalFile == file.absoluteFile)
            check(file.canonicalFile.parentFile == File(generation.directory, "media").canonicalFile)
            val sourceStat = Os.lstat(file.path)
            check(OsConstants.S_ISREG(sourceStat.st_mode) && sourceStat.st_uid == Process.myUid() && sourceStat.st_nlink == 1L && sourceStat.st_size == asset.byteLength)
            assertArrayEquals(expected, file.readBytes())
            check(identity(file) == listOf(sourceStat.st_dev, sourceStat.st_ino, sourceStat.st_uid.toLong(), sourceStat.st_size, sourceStat.st_mtime, sourceStat.st_ctime).joinToString(":"))
        }
        val timestamp = "2026-01-01T00:00:00Z"
        val project = data.projects.single()
        check(project == BackupProjectV1(project.id, "M11-B SAF 合成项目", LithicDatabase.PROJECT_FALLBACK_ID,
            "2026-01-01", null, null, data.mediaAssets.single { it.relativePath == "media/m11b-jpeg.jpg" }.id,
            null, timestamp, timestamp))
        val event = data.projectMoneyEvents.single()
        check(event == BackupProjectMoneyEventV2(event.id, project.id, "OUTLAY", 12345, "2026-01-01", null, "INITIAL", event.operationId,"CASH",null))
        val subscription = data.subscriptions.single()
        check(subscription == BackupSubscriptionV1(subscription.id, "M11-B SAF 合成订阅", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            data.mediaAssets.single { it.relativePath == "media/m11b-rgba.png" }.id, null, timestamp, timestamp))
        val renewal = data.subscriptionRenewals.single()
        check(renewal == BackupSubscriptionRenewalV1(renewal.id, subscription.id, "2026-01-01", "2026-01-01", "2026-02-01", "2026-02-01",
            18000, "MONTHLY", 30, renewal.operationId))
        val state = data.subscriptionStateChanges.single()
        check(state == BackupSubscriptionStateChangeV1(state.id, subscription.id, "2026-01-01", "ENABLED", null, null, state.operationId))
        listOf(project.id, event.id, event.operationId, subscription.id, renewal.id, renewal.operationId, state.id, requireNotNull(state.operationId))
            .plus(data.mediaAssets.map { it.id }).forEach { check(UUID.fromString(it).toString() == it) }
        check(before.settings.recordZoneId == generation.settings.recordZone().id)
        assertEquals(before, generation.backupSnapshotSource().capture())
        return media
    }

    private fun syntheticMediaFixtures(): Map<String, ByteArray> = linkedMapOf(
        "media/m11b-jpeg.jpg" to jpeg(),
        "media/m11b-rgba.png" to png(6, 8, 0),
        "media/m11b-palette.png" to png(3, 1, 0),
        "media/m11b-gray16.png" to png(0, 16, 0),
        "media/m11b-adam7.png" to png(6, 8, 1),
    )

    private fun jpeg(): ByteArray = ByteArrayOutputStream().also { output ->
        val bitmap = Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888)
        try { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) } finally { bitmap.recycle() }
    }.toByteArray()

    /** Original deterministic, fully framed legal PNG fixtures, not copies of user media. */
    private fun png(color: Int, depth: Int, interlace: Int): ByteArray {
        val output = ByteArrayOutputStream(); output.write(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10))
        fun chunk(type: String, data: ByteArray) {
            val name = type.toByteArray(Charsets.US_ASCII)
            DataOutputStream(output).apply { writeInt(data.size); write(name); write(data); writeInt(CRC32().apply { update(name); update(data) }.value.toInt()) }
        }
        chunk("IHDR", ByteArrayOutputStream().also { DataOutputStream(it).apply {
            writeInt(11); writeInt(9); write(byteArrayOf(depth.toByte(), color.toByte(), 0, 0, interlace.toByte()))
        } }.toByteArray())
        if (color == 3) chunk("PLTE", ByteArray(6))
        val channels = when (color) { 0, 3 -> 1; 2 -> 3; 4 -> 2; else -> 4 }
        val raw = ByteArrayOutputStream()
        val passes = if (interlace == 0) listOf(intArrayOf(0, 0, 1, 1)) else listOf(
            intArrayOf(0, 0, 8, 8), intArrayOf(4, 0, 8, 8), intArrayOf(0, 4, 4, 8), intArrayOf(2, 0, 4, 4),
            intArrayOf(0, 2, 2, 4), intArrayOf(1, 0, 2, 2), intArrayOf(0, 1, 1, 2))
        for (p in passes) {
            val width = if (11 <= p[0]) 0 else (11 - p[0] + p[2] - 1) / p[2]
            val height = if (9 <= p[1]) 0 else (9 - p[1] + p[3] - 1) / p[3]
            if (width > 0 && height > 0) repeat(height) { raw.write(0); raw.write(ByteArray((width * channels * depth + 7) / 8)) }
        }
        chunk("IDAT", ByteArrayOutputStream().also { DeflaterOutputStream(it).use { z -> z.write(raw.toByteArray()) } }.toByteArray())
        chunk("IEND", byteArrayOf()); return output.toByteArray()
    }

    private fun identity(file: File): String = Os.lstat(file.path).let {
        listOf(it.st_dev, it.st_ino, it.st_uid.toLong(), it.st_size, it.st_mtime, it.st_ctime).joinToString(":")
    }
    private fun writeEvidence(name: String, json: JSONObject) { File(evidence, name).also { check(it.createNewFile()); it.writeText(json.toString(2)) } }
    private fun id() = UUID.randomUUID().toString()
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
}
