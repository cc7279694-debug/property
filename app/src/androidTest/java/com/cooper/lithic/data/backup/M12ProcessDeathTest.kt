package com.cooper.lithic.data.backup

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Run exactly one method/case per instrumentation invocation with M12ProcessDeathRunner.
 * A killed invocation never reports PASS. Only the separate restart method can write verification.
 * No activities, default storage owner, retry, cleanup, or non-self process termination is used.
 */
@RunWith(AndroidJUnit4::class)
class M12ProcessDeathTest {
    private enum class Case { BEFORE_SWITCH, SWITCHING, STABLE }
    private data class Identity(val pid: Int, val uid: Int, val startTicks: Long, val bootId: String, val cmdline: String) {
        fun json(): JSONObject = JSONObject().put("pid", pid).put("uid", uid).put("startTicks", startTicks)
            .put("bootId", bootId).put("cmdline", cmdline)
    }
    private data class Request(val context: Context, val campaign: String, val case: Case,
        val appSha: String, val testSha: String, val identity: Identity, val root: File)
    private data class Frozen(val backup: ValidatedBackup, val hashes: JSONObject)

    @Test
    fun execute_self_termination(): Unit = runBlocking {
        val request = admit("execute_self_termination") // All environment/APK checks precede the first case write.
        val root = request.root
        val campaignRoot = requireNotNull(root.parentFile)
        if (!Files.exists(campaignRoot.toPath(), NOFOLLOW_LINKS)) makeDirectory(campaignRoot)
        ownedDirectory(campaignRoot)
        check(campaignRoot.listFiles()!!.all { it.name in Case.entries.map(Case::name) }) { "Unknown campaign child" }
        campaignRoot.listFiles()!!.forEach(::ownedDirectory)
        makeDirectory(root) // Exclusive claim: existing case roots are never reused, even after failure.
        val owner = UUID.randomUUID().toString()
        val evidence = File(root, "evidence").also(::makeDirectory)
        val ledger = File(root, "ledger").also(::makeDirectory)
        val source = File(root, "source").also(::makeDirectory)
        var old: ManagedGeneration? = null
        var incoming: ManagedGeneration? = null
        var active: ManagedGeneration? = null
        try {
            val store = DataGenerationStore(request.context, ledger, restoreProbe = { point ->
                val selected = request.case == Case.BEFORE_SWITCH && point == "BEFORE_SWITCH" ||
                    request.case == Case.SWITCHING && point == "AFTER_SWITCH"
                if (selected) checkpointAndTerminate(request, owner, evidence)
            })
            val oldGeneration = store.open(ZoneId.of("UTC")).also { old = it }
            insertFixture(oldGeneration, "old", 100_000L, Color.rgb(190, 32, 61))
            val expectedOld = freeze(oldGeneration, root, "expected-old")
            val sourceStore = DataGenerationStore(request.context, source)
            val sourceGeneration = sourceStore.open(ZoneId.of("Asia/Shanghai")).also { incoming = it }
            insertFixture(sourceGeneration, "new", 230_000L, Color.rgb(17, 87, 203))
            val expectedNew = freeze(sourceGeneration, root, "expected-new")
            sourceGeneration.close()
            check(!sourceGeneration.database.isOpen)
            incoming = null // The source owner is closed before any checkpoint can request a kill.
            requireDistinct(expectedOld.backup.snapshot, expectedNew.backup.snapshot)
            val candidate = store.prepareRestore(ZoneId.of("Asia/Shanghai")) {
                RestoredGenerationWriter().populate(it, expectedNew.backup)
            }
            val caseRecord = common(request, owner).put("oldId", oldGeneration.id).put("newId", candidate.id)
                .put("sourceId", sourceGeneration.id).put("sourceIdentity", request.identity.json())
                .put("oldManifestSha256", sha(File(root, "expected-old/manifest.json")))
                .put("newManifestSha256", sha(File(root, "expected-new/manifest.json")))
                .put("oldExpectedFiles", expectedOld.hashes).put("newExpectedFiles", expectedNew.hashes)
            writeExclusive(File(root, "case.json"), caseRecord)
            requireCaseTree(root)
            assertPointer(request, caseRecord, initial = true)
            val activated = store.activateRestore(oldGeneration.id, candidate, expectedOld.backup.snapshot).also { active = it }
            if (request.case == Case.STABLE) {
                // No production hook: return, capture exact B, then read the durable stable pointer.
                check(activated.backupSnapshotSource().capture() == expectedNew.backup.snapshot)
                assertPointer(request, caseRecord)
                checkpointAndTerminate(request, owner, evidence)
            }
            error("Authorized checkpoint was not reached; no termination attempted")
        } finally {
            // Runs only for a pre-kill failure; process death intentionally bypasses these owners.
            incoming?.close()
            active?.close()
            old?.close()
        }
    }

    @Test
    fun verify_after_restart(): Unit = runBlocking {
        val request = admit("verify_after_restart")
        val root = request.root
        ownedDirectory(root) // Read-only admission: no mkdir and no initialization fallback.
        requireCaseTree(root)
        val caseRecord = readObject(File(root, "case.json"), CASE_FIELDS)
        requireCommon(caseRecord, request)
        val owner = uuid(string(caseRecord, "ownerToken"))
        val sourceIdentity = identity(caseRecord.getJSONObject("sourceIdentity"))
        check(keys(caseRecord.getJSONObject("sourceIdentity")) == IDENTITY_FIELDS)
        check(sourceIdentity.bootId == request.identity.bootId)
        check(sourceIdentity.pid != request.identity.pid || sourceIdentity.startTicks != request.identity.startTicks)
        val evidence = File(root, "evidence")
        requireChildren(evidence, setOf("checkpoint.json", "permit.json", "kill-intent.json"))
        val checkpoint = readObject(File(evidence, "checkpoint.json"), CHECKPOINT_FIELDS)
        requireCommon(checkpoint, request, owner)
        check(identity(checkpoint) == sourceIdentity)
        val checkpointSha = sha(File(evidence, "checkpoint.json"))
        validatePermit(readObject(File(evidence, "permit.json"), PERMIT_FIELDS), request, owner,
            sourceIdentity, checkpointSha)
        val intent = readObject(File(evidence, "kill-intent.json"), PERMIT_FIELDS)
        validatePermit(intent, request, owner, sourceIdentity, checkpointSha)
        val oldProc = File("/proc/${sourceIdentity.pid}/stat")
        val oldPidGone = !oldProc.exists()
        val observedOldTicks = if (oldPidGone) null else startTicks(oldProc.readText())
        check(oldPidGone || observedOldTicks != sourceIdentity.startTicks) { "Source process is still alive" }
        val oldExpected = loadFrozen(root, "expected-old", caseRecord, "old")
        val newExpected = loadFrozen(root, "expected-new", caseRecord, "new")
        requireDistinct(oldExpected.backup.snapshot, newExpected.backup.snapshot)
        check(sha(File(root, "ledger/active-generation")) == string(checkpoint, "pointerSha256"))
        assertPointer(request, caseRecord)
        // Inspect both closed generations/ready/media before the store is allowed to open anything.
        val oldId = uuid(string(caseRecord, "oldId")); val newId = uuid(string(caseRecord, "newId"))
        inspectClosedGeneration(File(root, "ledger/generations/$oldId"), oldId, oldExpected.backup)
        inspectClosedGeneration(File(root, "ledger/generations/$newId"), newId, newExpected.backup, restored = true)
        inspectSource(root, uuid(string(caseRecord, "sourceId")), newExpected.backup)
        val expected = if (request.case == Case.BEFORE_SWITCH) oldExpected.backup else newExpected.backup
        val selectedId = if (request.case == Case.BEFORE_SWITCH) oldId else newId
        val ledger = File(root, "ledger")
        var opened: ManagedGeneration? = null
        try {
            val first = DataGenerationStore(request.context, ledger).open().also { opened = it }
            check(first.id == selectedId)
            verifyCurrent(first, expected)
            requireStablePointer(ledger, selectedId, setOf(oldId, newId))
            first.close(); opened = null
            val second = DataGenerationStore(request.context, ledger).open().also { opened = it }
            check(second.id == selectedId)
            verifyCurrent(second, expected)
            requireStablePointer(ledger, selectedId, setOf(oldId, newId))
            second.close(); opened = null
            // Recheck all immutable expected files after both production opens (including cleanup).
            loadFrozen(root, "expected-old", caseRecord, "old")
            loadFrozen(root, "expected-new", caseRecord, "new")
            check(selfIdentity() == request.identity)
            val result = common(request, owner).put("pid", request.identity.pid).put("uid", request.identity.uid)
                .put("startTicks", request.identity.startTicks).put("bootId", request.identity.bootId)
                .put("cmdline", request.identity.cmdline).put("sourceIdentity", sourceIdentity.json())
                .put("sourcePidAbsent", oldPidGone).put("observedReplacementStartTicks", observedOldTicks ?: JSONObject.NULL)
                .put("checkpointSha256", checkpointSha).put("currentId", selectedId).put("phase", "STABLE")
                .put("allSevenTablesExact", true).put("settingsExact", true).put("originalMediaExact", true)
                .put("integrityCheck", "ok").put("foreignKeyErrors", 0).put("secondOpenExact", true).put("status", "PASS")
            writeExclusive(File(evidence, "verification.json"), result)
        } finally { opened?.close() }
    }

    private fun admit(method: String): Request {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        check(instrumentation.javaClass == M12ProcessDeathRunner::class.java) { "Explicit T04 runner required" }
        val context = instrumentation.targetContext
        check(context.applicationContext.javaClass == Application::class.java) { "Default application is forbidden" }
        check(context.packageName == PACKAGE && context.applicationInfo.packageName == PACKAGE)
        check(context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        check(Build.VERSION.SDK_INT == 36 && Process.myUid() == EXPECTED_UID && context.applicationInfo.uid == EXPECTED_UID)
        val args = InstrumentationRegistry.getArguments()
        check(args.getString("class") == "${M12ProcessDeathTest::class.java.name}#$method") {
            "Select exactly this single T04 method"
        }
        val campaign = uuid(requireNotNull(args.getString("m12T04Campaign")))
        val case = Case.valueOf(requireNotNull(args.getString("m12T04Case")))
        val appSha = requireNotNull(args.getString("m12T04AppSha"))
        val testSha = requireNotNull(args.getString("m12T04TestSha"))
        check(appSha == APP_SHA && SHA.matches(testSha))
        check(sha(File(context.applicationInfo.sourceDir), owned = false) == appSha) { "App APK SHA mismatch" }
        check(instrumentation.context.packageName == "$PACKAGE.test")
        check(sha(File(instrumentation.context.applicationInfo.sourceDir), owned = false) == testSha) { "Test APK SHA mismatch" }
        val identity = selfIdentity()
        check(identity.bootId == BOOT && identity.uid == EXPECTED_UID && identity.cmdline == PACKAGE)
        val privateRoot = context.noBackupFilesDir.canonicalFile.also(::ownedDirectory)
        val root = File(File(privateRoot, "m12-t04-$campaign"), case.name).absoluteFile
        check(root.canonicalFile == root && root.parentFile!!.parentFile == privateRoot)
        return Request(context, campaign, case, appSha, testSha, identity, root)
    }

    private suspend fun checkpointAndTerminate(request: Request, owner: String, evidence: File): Nothing {
        val caseRecord = readObject(File(request.root, "case.json"), CASE_FIELDS)
        requireCommon(caseRecord, request, owner)
        check(selfIdentity() == request.identity)
        assertPointer(request, caseRecord)
        requireChildren(evidence, emptySet())
        val checkpoint = common(request, owner).put("pid", request.identity.pid).put("uid", request.identity.uid)
            .put("startTicks", request.identity.startTicks).put("bootId", request.identity.bootId)
            .put("cmdline", request.identity.cmdline)
            .put("pointerSha256", sha(File(request.root, "ledger/active-generation")))
        // Publish only after the complete checkpoint is closed and synced: the host watches this name.
        val checkpointFile = File(evidence, "checkpoint.json")
        val checkpointTemporary = File(evidence, ".checkpoint-publishing")
        writeExclusive(checkpointTemporary, checkpoint)
        check(!Files.exists(checkpointFile.toPath(), NOFOLLOW_LINKS))
        Files.move(checkpointTemporary.toPath(), checkpointFile.toPath(), ATOMIC_MOVE)
        syncDirectory(evidence)
        val checkpointSha = sha(File(evidence, "checkpoint.json"))
        val permitFile = File(evidence, "permit.json")
        val deadline = SystemClock.elapsedRealtime() + 90_000L
        while (!Files.exists(permitFile.toPath(), NOFOLLOW_LINKS)) {
            check(SystemClock.elapsedRealtime() < deadline) { "Host self-only permit timed out; no kill attempted" }
            delay(100)
        }
        val permit = readObject(permitFile, PERMIT_FIELDS)
        validatePermit(permit, request, owner, request.identity, checkpointSha)
        requireChildren(evidence, setOf("checkpoint.json", "permit.json"))
        check(selfIdentity() == request.identity)
        check(sha(File(evidence, "checkpoint.json")) == checkpointSha)
        assertPointer(request, caseRecord)
        writeExclusive(File(evidence, "kill-intent.json"), permit)
        check(selfIdentity() == request.identity)
        // Sole termination call, always the currently executing process; never a supplied PID.
        Process.killProcess(Process.myPid())
        delay(1_000)
        error("Self termination returned without process death; do not retry")
    }

    private fun validatePermit(value: JSONObject, request: Request, owner: String, expected: Identity, checkpointSha: String) {
        requireCommon(value, request, owner)
        check(integer(value, "pid") == expected.pid.toLong() && integer(value, "uid") == expected.uid.toLong())
        check(integer(value, "startTicks") == expected.startTicks && string(value, "bootId") == expected.bootId)
        check(string(value, "checkpointSha256") == checkpointSha && string(value, "action") == "SELF_ONLY")
    }

    private fun common(request: Request, owner: String): JSONObject = JSONObject().put("campaign", request.campaign)
        .put("case", request.case.name).put("ownerToken", owner).put("appSha256", request.appSha).put("testSha256", request.testSha)

    private fun requireCommon(value: JSONObject, request: Request, owner: String? = null) {
        check(string(value, "campaign") == request.campaign && string(value, "case") == request.case.name)
        check(uuid(string(value, "ownerToken")) == (owner ?: string(value, "ownerToken")))
        check(string(value, "appSha256") == request.appSha && string(value, "testSha256") == request.testSha)
    }

    private fun assertPointer(request: Request, record: JSONObject, initial: Boolean = false) {
        val ledger = File(request.root, "ledger")
        requireChildren(ledger, setOf("active-generation", "generations"))
        val old = uuid(string(record, "oldId")); val new = uuid(string(record, "newId"))
        requireChildren(File(ledger, "generations"), setOf(old, new))
        val pointer = readObject(File(ledger, "active-generation"), POINTER_FIELDS)
        check(integer(pointer, "formatVersion") == 1L)
        val before = initial || request.case == Case.BEFORE_SWITCH
        check(string(pointer, "activeId") == if (before) old else new)
        check(pointer.get("previousId") == if (before) JSONObject.NULL else old)
        check(string(pointer, "stage") == if (!initial && request.case == Case.SWITCHING) "SWITCHING" else "STABLE")
    }

    private fun requireStablePointer(ledger: File, id: String, allowedIds: Set<String>) {
        requireChildren(ledger, setOf("active-generation", "generations"))
        val pointer = readObject(File(ledger, "active-generation"), POINTER_FIELDS)
        check(integer(pointer, "formatVersion") == 1L && string(pointer, "activeId") == id && string(pointer, "stage") == "STABLE")
        check(pointer.get("previousId") == JSONObject.NULL || string(pointer, "previousId") in allowedIds - id)
        val generations = File(ledger, "generations").also(::ownedDirectory)
        check(generations.listFiles()!!.all { it.name in allowedIds })
        ownedDirectory(File(generations, id))
    }

    private suspend fun verifyCurrent(generation: ManagedGeneration, expected: ValidatedBackup) {
        check(generation.backupSnapshotSource().capture() == expected.snapshot) { "Mixed or stale facts/settings" }
        RestoredGenerationWriter().verify(generation, expected.snapshot) // Actual media bytes + integrity + FK.
        expected.snapshot.data.mediaAssets.forEach {
            check(readBytes(File(generation.directory, it.relativePath)).contentEquals(readBytes(expected.directory.resolve(it.relativePath).toFile())))
        }
    }

    private suspend fun freeze(generation: ManagedGeneration, root: File, name: String): Frozen {
        val snapshot = generation.backupSnapshotSource().capture()
        val core = RoomBackupSnapshotSource(generation).captureInto {
            BackupMediaSnapshot(generation).copy(it, BackupCoreStager().prepare(it, root.toPath()))
        }
        val manifest = BackupManifestV1.complete(core, "m12-t04-synthetic", Instant.parse("2026-01-20T00:00:00Z"))
        writeExclusive(core.directory.resolve("manifest.json").toFile(), manifest.encode())
        val destination = File(root, name)
        check(!Files.exists(destination.toPath(), NOFOLLOW_LINKS))
        Files.move(core.directory, destination.toPath(), ATOMIC_MOVE)
        val hashes = JSONObject()
        Files.walk(destination.toPath()).use { stream -> stream.iterator().asSequence().filter { it != destination.toPath() }.forEach { path ->
            val file = path.toFile()
            if (Files.isDirectory(path, NOFOLLOW_LINKS)) ownedDirectory(file) else {
                ownedFile(file); syncFile(file); hashes.put(destination.toPath().relativize(path).toString().replace(File.separatorChar, '/'), sha(file))
                Os.chmod(file.path, 0x100) // 0400: frozen inputs are never rewritten by either method.
            }
        } }
        Files.walk(destination.toPath()).use { stream -> stream.iterator().asSequence()
            .filter { Files.isDirectory(it, NOFOLLOW_LINKS) }.toList().asReversed().forEach { syncDirectory(it.toFile()) } }
        syncDirectory(root)
        val backup = ValidatedBackup(snapshot, manifest, destination.toPath(), sha(File(destination, "manifest.json")))
        BackupBusinessValidation.validate(snapshot, manifest) {}
        return Frozen(backup, hashes)
    }

    private fun loadFrozen(root: File, name: String, record: JSONObject, prefix: String): Frozen {
        val directory = File(root, name).also(::ownedDirectory)
        val hashes = record.getJSONObject("${prefix}ExpectedFiles")
        val expectedFiles = keys(hashes)
        val directories = mutableSetOf<String>()
        val paths = Files.walk(directory.toPath()).use { stream -> stream.iterator().asSequence().filter { it != directory.toPath() }.map {
            if (Files.isDirectory(it, NOFOLLOW_LINKS)) {
                ownedDirectory(it.toFile()); directories += directory.toPath().relativize(it).toString().replace(File.separatorChar, '/'); null
            }
            else { ownedFile(it.toFile()); directory.toPath().relativize(it).toString().replace(File.separatorChar, '/') }
        }.filterNotNull().toSet() }
        check(paths == expectedFiles)
        paths.forEach { check(sha(File(directory, it)) == string(hashes, it)) { "Frozen expected file changed" } }
        check(sha(File(directory, "manifest.json")) == string(record, "${prefix}ManifestSha256"))
        val manifest = JSON.decodeFromString(BackupManifestV1.serializer(), readBytes(File(directory, "manifest.json")).toString(Charsets.UTF_8))
        check(manifest.formatVersion == 1 && manifest.minReaderFormat == 1 && manifest.sourceDatabaseSchemaVersion == 2)
        check(paths == manifest.files.map { it.path }.toSet() + setOf("manifest.json", ".backup-core-owner"))
        check(directories == parentDirectories(paths)) { "Unexpected frozen directory" }
        manifest.files.forEach { BackupEntryFiles.verify(directory.toPath(), it) }
        val snapshot = BackupSnapshot(
            JSON.decodeFromString(BackupDataV1.serializer(), readBytes(File(directory, "data.json")).toString(Charsets.UTF_8)).ordered(),
            JSON.decodeFromString(BackupSettingsV1.serializer(), readBytes(File(directory, "settings.json")).toString(Charsets.UTF_8)))
        BackupBusinessValidation.validate(snapshot, manifest) {}
        return Frozen(ValidatedBackup(snapshot, manifest, directory.toPath(), string(record, "${prefix}ManifestSha256")), hashes)
    }

    private fun inspectSource(root: File, id: String, expected: ValidatedBackup) {
        val source = File(root, "source")
        requireChildren(source, setOf("active-generation", "generations"))
        requireChildren(File(source, "generations"), setOf(id))
        val pointer = readObject(File(source, "active-generation"), POINTER_FIELDS)
        check(integer(pointer, "formatVersion") == 1L && string(pointer, "activeId") == id &&
            pointer.get("previousId") == JSONObject.NULL && string(pointer, "stage") == "STABLE")
        inspectClosedGeneration(File(source, "generations/$id"), id, expected)
    }

    private fun inspectClosedGeneration(directory: File, id: String, expected: ValidatedBackup, restored: Boolean = false) {
        ownedDirectory(directory)
        val allowed = setOf("data.db", "data.db-wal", "data.db-shm", "settings.preferences_pb", "ready.json", "media") +
            if (restored) setOf("restore-proof.json") else emptySet()
        check(directory.listFiles()!!.all { it.name in allowed }) { "Unexpected generation child" }
        listOf("data.db", "settings.preferences_pb", "ready.json").forEach { ownedFile(File(directory, it)) }
        val ready = readObject(File(directory, "ready.json"), setOf("formatVersion", "generationId"))
        check(integer(ready, "formatVersion") == 1L && string(ready, "generationId") == id)
        if (restored) {
            val proof = readObject(File(directory, "restore-proof.json"),
                setOf("formatVersion", "generationId", "nonce", "dataSha256", "settingsSha256"))
            check(integer(proof, "formatVersion") == 1L && string(proof, "generationId") == id)
            uuid(string(proof, "nonce"))
            check(string(proof, "dataSha256") == digest(BackupJson.encodeData(expected.snapshot.data)))
            check(string(proof, "settingsSha256") == digest(BackupJson.encodeSettings(expected.snapshot.settings)))
        }
        val media = File(directory, "media").also(::ownedDirectory)
        val directories = mutableSetOf("media")
        val actual = Files.walk(media.toPath()).use { stream -> stream.iterator().asSequence().filter { it != media.toPath() }.map {
            if (Files.isDirectory(it, NOFOLLOW_LINKS)) {
                ownedDirectory(it.toFile()); directories += directory.toPath().relativize(it).toString().replace(File.separatorChar, '/'); null
            }
            else { ownedFile(it.toFile()); directory.toPath().relativize(it).toString().replace(File.separatorChar, '/') }
        }.filterNotNull().toSet() }
        check(actual == expected.snapshot.data.mediaAssets.map { it.relativePath }.toSet())
        check(directories == parentDirectories(actual)) { "Unexpected media directory" }
        expected.snapshot.data.mediaAssets.forEach { check(sha(File(directory, it.relativePath)) == it.sha256) }
        directory.listFiles()!!.filter { it.name != "media" }.forEach(::ownedFile)
    }

    private suspend fun insertFixture(generation: ManagedGeneration, label: String, amount: Long, color: Int) {
        val png = picture(Bitmap.CompressFormat.PNG, 2, 3, color)
        val jpeg = picture(Bitmap.CompressFormat.JPEG, 3, 2, color)
        writeExclusive(File(generation.directory, "media/$label.png"), png)
        writeExclusive(File(generation.directory, "media/$label.jpg"), jpeg)
        val day = LocalDate.parse("2026-01-01"); val at = Instant.parse("2026-01-01T01:02:03Z")
        generation.coordinator.withWrite { generation.database.withTransaction {
            val db = generation.database
            val projectCategory = CategoryEntity(id(), CategoryType.PROJECT, "$label 合成设备", "$label 合成设备", false)
            val subscriptionCategory = CategoryEntity(id(), CategoryType.SUBSCRIPTION, "$label 合成服务", "$label 合成服务", false)
            db.categoryDao().insert(projectCategory); db.categoryDao().insert(subscriptionCategory)
            val cover = LocalMediaAssetEntity(id(), "media/$label.png", "image/png", 2, 3, png.size.toLong(), digest(png))
            val subCover = LocalMediaAssetEntity(id(), "media/$label.jpg", "image/jpeg", 3, 2, jpeg.size.toLong(), digest(jpeg))
            db.mediaDao().insert(cover); db.mediaDao().insert(subCover)
            val project = LongTermProjectEntity(id(), "$label 合成长存项目", projectCategory.id, day,
                coverAssetId = cover.id, note = "$label 独立事实", createdAt = at, updatedAt = at.plusSeconds(1))
            db.projectDao().insertProject(project)
            db.projectDao().insertEvent(ProjectMoneyEventEntity(id(), project.id, MoneyDirection.OUTLAY, amount,
                day, "$label 初始取得", ProjectMoneyEventRole.INITIAL, id()))
            val subscription = SubscriptionEntity(id(), "$label 固定订阅", subscriptionCategory.id, subCover.id,
                "$label 不自动付款", at, at.plusSeconds(2))
            db.subscriptionDao().insertSubscription(subscription)
            db.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(id(), subscription.id, day, day,
                day.plusMonths(1), day.plusMonths(1), amount + 1_000L, BillingCycle.MONTHLY, 30, id()))
            db.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(), subscription.id,
                day.plusDays(3), SubscriptionOperationState.PAUSED, "$label 暂停", "$label 状态事实", id()))
        } }
    }

    private fun requireDistinct(old: BackupSnapshot, new: BackupSnapshot) {
        check(old.settings.recordZoneId == "UTC" && new.settings.recordZoneId == "Asia/Shanghai")
        check(old.data.recordCounts().values.all { it > 0 } && new.data.recordCounts().values.all { it > 0 })
        check(old.data.projectMoneyEvents.single().amountCents == 100_000L && new.data.projectMoneyEvents.single().amountCents == 230_000L)
        fun ids(data: BackupDataV2): Set<String> = (data.categories.filterNot { it.isFallback }.map { it.id } +
            data.projects.map { it.id } + data.projectMoneyEvents.flatMap { listOf(it.id, it.operationId) } +
            data.mediaAssets.map { it.id } + data.subscriptions.map { it.id } +
            data.subscriptionRenewals.flatMap { listOf(it.id, it.operationId) } +
            data.subscriptionStateChanges.flatMap { listOf(it.id, it.operationId) }).toSet()
        check(ids(old.data).intersect(ids(new.data)).isEmpty())
        check(old.data.projects.single().name != new.data.projects.single().name &&
            old.data.subscriptions.single().name != new.data.subscriptions.single().name)
        check(old.data.mediaAssets.map { it.sha256 }.toSet().intersect(new.data.mediaAssets.map { it.sha256 }.toSet()).isEmpty())
        check(old.data.mediaAssets.map { it.mime }.toSet() == setOf("image/png", "image/jpeg"))
        check(new.data.mediaAssets.map { it.mime }.toSet() == setOf("image/png", "image/jpeg"))
    }

    private fun picture(format: Bitmap.CompressFormat, width: Int, height: Int, color: Int): ByteArray =
        ByteArrayOutputStream().use { output ->
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try { bitmap.eraseColor(color); check(bitmap.compress(format, 91, output)); output.toByteArray() }
            finally { bitmap.recycle() }
        }

    private fun requireCaseTree(root: File) = requireChildren(root,
        setOf("ledger", "source", "evidence", "expected-old", "expected-new", "case.json"))
    private fun parentDirectories(paths: Set<String>): Set<String> = paths.flatMap { path ->
        val segments = path.split('/')
        (1 until segments.size).map { segments.take(it).joinToString("/") }
    }.toSet()
    private fun requireChildren(directory: File, expected: Set<String>) {
        ownedDirectory(directory)
        check(directory.listFiles()!!.map { it.name }.toSet() == expected) { "Unexpected children in ${directory.name}" }
        directory.listFiles()!!.forEach { if (it.isDirectory) ownedDirectory(it) else ownedFile(it) }
    }
    private fun makeDirectory(file: File) {
        ownedDirectory(requireNotNull(file.parentFile))
        check(file.canonicalFile == file.absoluteFile)
        check(!Files.exists(file.toPath(), NOFOLLOW_LINKS)) { "Exclusive directory already exists; never reuse" }
        Files.createDirectory(file.toPath()); ownedDirectory(file); syncDirectory(requireNotNull(file.parentFile))
    }
    private fun ownedDirectory(file: File) {
        check(file.absoluteFile == file.canonicalFile) { "Linked or escaping directory" }
        val stat = Os.lstat(file.path)
        check(OsConstants.S_ISDIR(stat.st_mode) && stat.st_uid == Process.myUid()) { "Directory ownership/type changed" }
    }
    private fun ownedFile(file: File) {
        ownedDirectory(requireNotNull(file.parentFile))
        check(file.absoluteFile == file.canonicalFile)
        val stat = Os.lstat(file.path)
        check(OsConstants.S_ISREG(stat.st_mode) && stat.st_uid == Process.myUid() && stat.st_nlink == 1L)
    }
    private fun writeExclusive(file: File, value: JSONObject) = writeExclusive(file, value.toString().toByteArray(Charsets.UTF_8))
    private fun writeExclusive(file: File, bytes: ByteArray) {
        ownedDirectory(requireNotNull(file.parentFile))
        check(file.absoluteFile == file.canonicalFile)
        val fd = Os.open(file.path, OsConstants.O_WRONLY or OsConstants.O_CREAT or OsConstants.O_EXCL or OsConstants.O_NOFOLLOW, 0x180)
        FileOutputStream(fd).use { output -> output.write(bytes); output.flush(); output.fd.sync() }
        ownedFile(file); check(readBytes(file).contentEquals(bytes)); syncDirectory(requireNotNull(file.parentFile))
    }
    private fun syncFile(file: File) {
        ownedFile(file)
        val fd = Os.open(file.path, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW, 0)
        try { Os.fsync(fd) } finally { Os.close(fd) }
    }
    private fun syncDirectory(directory: File) {
        ownedDirectory(directory)
        val fd = Os.open(directory.path, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW, 0)
        try { Os.fsync(fd) } finally { Os.close(fd) }
    }
    private fun readBytes(file: File): ByteArray {
        ownedFile(file); check(file.length() in 1..(4L * 1024 * 1024))
        val fd = Os.open(file.path, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW, 0)
        return FileInputStream(fd).use { it.readBytes() }
    }
    private fun sha(file: File, owned: Boolean = true): String {
        if (owned) ownedFile(file) else check(file.isFile && file.absoluteFile == file.canonicalFile)
        val md = MessageDigest.getInstance("SHA-256")
        val fd = Os.open(file.path, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW, 0)
        FileInputStream(fd).use { input -> val buffer = ByteArray(64 * 1024); while (true) {
            val read = input.read(buffer); if (read < 0) break; check(read > 0); md.update(buffer, 0, read)
        } }
        return hex(md.digest())
    }
    private fun readObject(file: File, fields: Set<String>): JSONObject {
        val parser = JSONTokener(readBytes(file).toString(Charsets.UTF_8))
        val value = parser.nextValue() as? JSONObject ?: error("Object required")
        check(parser.nextClean() == '\u0000' && keys(value) == fields) { "Unexpected metadata fields/trailing data" }
        return value
    }
    private fun keys(value: JSONObject): Set<String> = value.keys().asSequence().toSet()
    private fun string(value: JSONObject, key: String): String = (value.get(key) as? String) ?: error("$key must be string")
    private fun integer(value: JSONObject, key: String): Long = when (val raw = value.get(key)) {
        is Int -> raw.toLong(); is Long -> raw; else -> error("$key must be JSON integer")
    }
    private fun uuid(value: String): String = value.also { check(UUID.fromString(it).toString() == it) }
    private fun id(): String = UUID.randomUUID().toString()
    private fun digest(value: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(value))
    private fun hex(value: ByteArray): String = value.joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun selfIdentity(): Identity {
        val pid = Process.myPid()
        val ticks = startTicks(File("/proc/self/stat").readText())
        val uidLine = File("/proc/self/status").readLines().single { it.startsWith("Uid:") }.split(Regex("\\s+")).drop(1)
        check(uidLine.size == 4 && uidLine.all { it.toInt() == Process.myUid() })
        return Identity(pid, Process.myUid(), ticks, File("/proc/sys/kernel/random/boot_id").readText().trim(),
            File("/proc/self/cmdline").readBytes().toString(Charsets.UTF_8).trimEnd('\u0000'))
    }
    private fun startTicks(stat: String): Long {
        val close = stat.lastIndexOf(')'); check(close > 0)
        return stat.substring(close + 1).trim().split(Regex("\\s+"))[19].toLong().also { check(it > 0) }
    }
    private fun identity(value: JSONObject): Identity {
        val pid = integer(value, "pid"); val uid = integer(value, "uid"); val ticks = integer(value, "startTicks")
        check(pid in 1..Int.MAX_VALUE.toLong() && uid == EXPECTED_UID.toLong() && ticks > 0)
        return Identity(pid.toInt(), uid.toInt(), ticks, string(value, "bootId"), string(value, "cmdline")).also {
            check(it.bootId == BOOT && it.cmdline == PACKAGE)
        }
    }

    private companion object {
        const val PACKAGE = "com.cooper.lithic.debug"
        const val EXPECTED_UID = 10216
        const val BOOT = "837312dd-125c-41f8-92a9-eb2d96e7795e"
        const val APP_SHA = "3bb0d801300889d5c97f85dd768a14701f3a3f67aacd55dca929aa86e8233107"
        val SHA = Regex("[0-9a-f]{64}")
        val JSON = Json { encodeDefaults = true; explicitNulls = true; ignoreUnknownKeys = false }
        val COMMON_FIELDS = setOf("campaign", "case", "ownerToken", "appSha256", "testSha256")
        val IDENTITY_FIELDS = setOf("pid", "uid", "startTicks", "bootId", "cmdline")
        val CASE_FIELDS = COMMON_FIELDS + setOf("oldId", "newId", "sourceId", "sourceIdentity", "oldManifestSha256",
            "newManifestSha256", "oldExpectedFiles", "newExpectedFiles")
        val CHECKPOINT_FIELDS = COMMON_FIELDS + IDENTITY_FIELDS + "pointerSha256"
        val PERMIT_FIELDS = COMMON_FIELDS + setOf("pid", "uid", "startTicks", "bootId", "checkpointSha256", "action")
        val POINTER_FIELDS = setOf("formatVersion", "activeId", "previousId", "stage")
    }
}
