package com.cooper.lithic.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.repository.RankingRepository
import com.cooper.lithic.data.repository.ReviewRanking
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Frozen format-v1 controlled-file round trip, isolated from SAF and the default ledger. */
@RunWith(AndroidJUnit4::class)
class M15ReviewBackupRoundTripTest {
    @Test fun restored_generation_recomputes_both_types_and_rejects_retired_review_reads(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.noBackupFilesDir, "m15-review-backup-${UUID.randomUUID()}").canonicalFile
        check(root.mkdir())
        val sourceOwner = AppContainer(DataGenerationStore(context, File(root, "source")),
            CoroutineScope(SupervisorJob() + Dispatchers.IO))
        val targetOwner = AppContainer(DataGenerationStore(context, File(root, "target")),
            CoroutineScope(SupervisorJob() + Dispatchers.IO))
        try {
            sourceOwner.start(ZoneId.of("Asia/Shanghai"))
            targetOwner.start(ZoneId.of("UTC"))
            val source = sourceOwner.openStorage()
            val oldTarget = targetOwner.openStorage()
            val pictures = RestoreSyntheticFixture.insert(source)
            RestoreSyntheticFixture.insert(oldTarget)
            val day = LocalDate.of(2026, 2, 3)
            val sourceRanking = RankingRepository(source.database, source.coordinator, FixedBusinessClock(day))
            val oldRanking = RankingRepository(oldTarget.database, oldTarget.coordinator, FixedBusinessClock(day))
            val before = source.backupSnapshotSource().capture()
            val expected = ReviewRanking.entries.associateWith { sourceRanking.loadWindow(it, 2) }
            val oldIds = oldRanking.loadWindow(ReviewRanking.PROJECT_TIME).items.map { it.id }.toSet()
            assertEquals(2, expected.getValue(ReviewRanking.PROJECT_TIME).items.size)
            assertEquals(1, expected.getValue(ReviewRanking.SUBSCRIPTION_TIME).items.size)
            assertEquals(before, source.backupSnapshotSource().capture())

            val archive = File(root, "review.lithicbackup")
            assertTrue(BackupExportService(context, source).export(RestoreSyntheticFixture.target(archive)) is BackupExportResult.Success)
            val restorer = RestoreCoordinator(context, targetOwner)
            val preview = restorer.prepare { archive.inputStream() }
            assertEquals(before.data.recordCounts(), preview.backupCounts)
            assertEquals(RestoreResult.Success, restorer.confirm(preview.token))
            val restored = targetOwner.openStorage()
            assertNotEquals(oldTarget.id, restored.id)
            assertEquals(before, restored.backupSnapshotSource().capture())
            val restoredRanking = RankingRepository(restored.database, restored.coordinator, FixedBusinessClock(day))
            ReviewRanking.entries.forEach { ranking ->
                assertEquals(expected.getValue(ranking), restoredRanking.loadWindow(ranking, 2))
            }
            assertTrue(restoredRanking.loadWindow(ReviewRanking.PROJECT_TIME).items.none { it.id in oldIds })
            val retiredFailure = try { oldRanking.loadWindow(ReviewRanking.PROJECT_TIME); null } catch (failure: Exception) { failure }
            assertNotNull("A retired generation must not return old review facts", retiredFailure)

            val nextDay = restoredRanking.loadWindow(ReviewRanking.PROJECT_TIME, asOf = day.plusDays(1))
            val todayRows = expected.getValue(ReviewRanking.PROJECT_TIME).items.associateBy { it.id }
            nextDay.items.forEach { item ->
                val prior = checkNotNull(todayRows.getValue(item.id).project)
                val next = checkNotNull(item.project)
                assertEquals(prior.cost.heldDays + if (prior.isHistory) 0L else 1L, next.cost.heldDays)
                assertEquals(prior.cost.netCents, next.cost.netCents)
            }
            assertEquals(before, restored.backupSnapshotSource().capture())
            pictures.forEach { (path, bytes) -> assertArrayEquals(bytes, File(restored.directory, path).readBytes()) }
        } finally {
            sourceOwner.close()
            targetOwner.close()
            check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.name.startsWith("m15-review-backup-"))
            check(root.deleteRecursively())
        }
    }
}
