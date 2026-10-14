package com.cooper.lithic.data.media

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Process
import androidx.test.runner.AndroidJUnitRunner
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.core.time.ZonedBusinessClock
import com.cooper.lithic.app.LithicApplication
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/** Explicit host-driven SIGKILL probe. Not a JUnit PASS or a production component. */
class M10ProcessBoundaryRunner : AndroidJUnitRunner() {
    override fun onStart() {
        if (androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("action") == null) {
            super.onStart()
            return
        }
        try {
            runBlocking { probe() }
            finish(Activity.RESULT_OK, Bundle().apply { putString("m10Result", "PASS") })
        } catch (failure: Exception) {
            finish(Activity.RESULT_CANCELED, Bundle().apply {
                putString("m10Result", "FAIL")
                putString("m10Failure", failure.toString())
            })
        }
    }

    private suspend fun probe() {
        val args = androidx.test.platform.app.InstrumentationRegistry.getArguments()
        if (args.getString("action") == "share-synthetic") {
            shareSynthetic(args)
            return
        }
        val boundary = requireNotNull(args.getString("boundary"))
        require(boundary in setOf("file-before-db", "db-before-cleanup"))
        val action = requireNotNull(args.getString("action"))
        require(action in setOf("kill", "verify"))
        val root = File(targetContext.noBackupFilesDir, "m10-process-$boundary")
        check(root.canonicalFile.parentFile == targetContext.noBackupFilesDir.canonicalFile)
        if (action == "kill") check(!root.exists()) { "Never overwrite a previous probe dataset" }
        val generation = DataGenerationStore(targetContext, root).open(ZoneId.of("UTC"))
        val time = Instant.parse("2026-01-31T00:00:00Z")
        val clock = FixedBusinessClock(LocalDate.of(2026, 1, 31))
        val repository = MediaRepository(generation.database, generation.coordinator,
            generation.images, clock, targetContext.contentResolver)
        val checkpoint = File(root, "probe.json")
        if (action == "kill") {
            val category = generation.database.categoryDao().list().single { it.type == CategoryType.PROJECT }
            val project = LongTermProjectEntity(uuid(), "Synthetic process boundary", category.id,
                LocalDate.of(2026, 1, 1), note = "Keep money and history", createdAt = time, updatedAt = time)
            val event = ProjectMoneyEventEntity(uuid(), project.id, MoneyDirection.OUTLAY, 15_000,
                LocalDate.of(2026, 1, 1), "Synthetic paid fact", ProjectMoneyEventRole.INITIAL, uuid())
            generation.database.projectDao().insertProject(project)
            generation.database.projectDao().insertEvent(event)
            val baseline = CoverBaseline.Project(project, listOf(event))
            val old = prepare(repository, 0xffc47c50.toInt())
            val saved = success(repository.commit(CoverCommand(uuid(), baseline, old.asset, time.plusSeconds(1)))).baseline
            val replacement = prepare(repository, 0xff507cc4.toInt())
            val command = CoverCommand(uuid(), saved, replacement.asset, time.plusSeconds(2))
            if (boundary == "file-before-db") {
                generation.coordinator.withWrite { success(generation.images.commitPreparedAsset(replacement)) }
                check(generation.database.mediaDao().find(replacement.asset.id) == null)
            } else {
                success(repository.commit(command))
                check(generation.database.mediaDao().find(old.asset.id) == old.asset)
            }
            val evidence = JSONObject().put("boundary", boundary).put("pid", Process.myPid())
                .put("generation", generation.id).put("projectId", project.id).put("eventId", event.id)
                .put("old", assetJson(old.asset)).put("new", assetJson(replacement.asset))
                .put("recordZone", generation.settings.recordZone().id)
                .put("checkpoint", "COMPLETE_BEFORE_ACTUAL_SIGKILL")
            FileOutputStream(checkpoint).use { stream ->
                stream.write(evidence.toString(2).toByteArray(Charsets.UTF_8))
                stream.flush()
                stream.fd.sync()
            }
            // Intentionally no close/cleanup: the host must observe this process really die.
            Process.killProcess(Process.myPid())
            error("Process termination unexpectedly returned")
        }
        val before = JSONObject(checkpoint.readText())
        check(before.getString("generation") == generation.id)
        check(before.getString("recordZone") == generation.settings.recordZone().id)
        val old = asset(before.getJSONObject("old"))
        val replacement = asset(before.getJSONObject("new"))
        val project = requireNotNull(generation.database.projectDao().findProject(before.getString("projectId")))
        val events = generation.database.projectDao().eventsForProjects(listOf(project.id))
        check(events.single().id == before.getString("eventId") && events.single().amountCents == 15_000L)
        check(project.note == "Keep money and history" && project.endedOn == null)
        val expected = if (boundary == "file-before-db") old else replacement
        check(project.coverAssetId == expected.id)
        check(success(repository.openAsset(expected.id)).asset == expected)
        check(generation.database.mediaDao().find(old.id) == old)
        if (boundary == "file-before-db") check(generation.database.mediaDao().find(replacement.id) == null)
        else check(generation.database.mediaDao().find(replacement.id) == replacement)
        val cleanup = success(repository.removeUnreferenced())
        check(cleanup.failures.isEmpty())
        val orphan = if (boundary == "file-before-db") replacement else old
        check(orphan.id in cleanup.removedAssetIds)
        check(generation.database.mediaDao().find(orphan.id) == null)
        check(!File(generation.directory, orphan.relativePath).exists())
        check(success(repository.openAsset(expected.id)).asset == expected)
        generation.database.openHelper.readableDatabase.query("PRAGMA integrity_check").use {
            check(it.moveToFirst() && it.getString(0) == "ok")
        }
        generation.database.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { check(!it.moveToFirst()) }
        val result = JSONObject().put("boundary", boundary).put("newProcessPid", Process.myPid())
            .put("oldProcessPid", before.getInt("pid")).put("generation", generation.id)
            .put("coverVerified", expected.id).put("orphanRemoved", orphan.id)
            .put("moneyAndHistoryPreserved", true).put("integrity", "ok").put("foreignKeys", "PASS")
            .put("actualProcessBoundary", "PASS")
        check(result.getInt("newProcessPid") != result.getInt("oldProcessPid"))
        FileOutputStream(File(root, "verified.json")).use {
            it.write(result.toString(2).toByteArray(Charsets.UTF_8)); it.flush(); it.fd.sync()
        }
        generation.close()
    }

    /** Uses the Application's existing owner; only the two explicit M10 QA records are allowed. */
    private suspend fun shareSynthetic(args: Bundle) {
        val generation = (targetContext.applicationContext as LithicApplication).appContainer.openStorage()
        val zone = generation.settings.recordZone()
        val repository = MediaRepository(generation.database, generation.coordinator, generation.images,
            ZonedBusinessClock(Clock.system(zone)), targetContext.contentResolver)
        val project = success(repository.loadTarget(CoverTarget(CoverTargetType.PROJECT,
            requireNotNull(args.getString("projectId"))))) as CoverBaseline.Project
        val subscription = success(repository.loadTarget(CoverTarget(CoverTargetType.SUBSCRIPTION,
            requireNotNull(args.getString("subscriptionId"))))) as CoverBaseline.Subscription
        check(project.name == "M10 QA Project" && subscription.name == "M10 QA Subscription")
        val asset = success(repository.openAsset(requireNotNull(project.coverAssetId))).asset
        val command = CoverCommand(uuid(), subscription, asset, subscription.updatedAt.plusNanos(1))
        val result = success(repository.commit(command)).baseline as CoverBaseline.Subscription
        check(result.facts == subscription.facts.copy(subscription = subscription.facts.subscription.copy(
            coverAssetId = asset.id, updatedAt = command.writtenAt)))
        check(success(repository.loadTarget(project.target)) == project)
        check(success(repository.openAsset(asset.id)).asset == asset)
        val report = JSONObject().put("action", "share-synthetic").put("sameApplicationOwner", true)
            .put("generation", generation.id).put("projectId", project.target.id)
            .put("subscriptionId", subscription.target.id).put("sharedAssetId", asset.id)
            .put("sha256", asset.sha256).put("originalPaymentsAndStatesPreserved", true)
            .put("result", "PASS")
        FileOutputStream(File(targetContext.noBackupFilesDir, "m10-synthetic-share.json")).use {
            it.write(report.toString(2).toByteArray(Charsets.UTF_8)); it.flush(); it.fd.sync()
        }
        // Do not close or replace the Application's shared generation.
    }

    private suspend fun prepare(repository: MediaRepository, color: Int): PreparedImage {
        val bitmap = Bitmap.createBitmap(32, 24, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        val bytes = ByteArrayOutputStream().use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)); out.toByteArray()
        }
        bitmap.recycle()
        val source = M10ImageSourceFixtures.source(bytes)
        return try { success(repository.prepareImport(source.uri, uuid())) } finally { source.delete() }
    }

    private fun assetJson(asset: LocalMediaAssetEntity) = JSONObject().put("id", asset.id)
        .put("relativePath", asset.relativePath).put("mime", asset.mime).put("width", asset.width)
        .put("height", asset.height).put("byteLength", asset.byteLength).put("sha256", asset.sha256)
    private fun asset(json: JSONObject) = LocalMediaAssetEntity(json.getString("id"), json.getString("relativePath"),
        json.getString("mime"), json.getInt("width"), json.getInt("height"), json.getLong("byteLength"), json.getString("sha256"))
    private fun uuid() = UUID.randomUUID().toString()
    private fun <T> success(result: MediaResult<T>): T = when (result) {
        is MediaResult.Success -> result.value
        is MediaResult.Failure -> error("Production operation failed: $result")
    }
}
