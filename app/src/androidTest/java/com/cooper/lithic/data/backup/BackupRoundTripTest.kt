package com.cooper.lithic.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real export -> archive validation -> independent Room generation -> activation -> export. */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var root: File
    private lateinit var store: DataGenerationStore
    private lateinit var active: ManagedGeneration

    @Before fun before(): Unit = runBlocking {
        root = File(context.noBackupFilesDir, "m12-roundtrip-${UUID.randomUUID()}").canonicalFile
        check(root.mkdir())
        store = DataGenerationStore(context, root)
        active = store.open(ZoneId.of("Asia/Shanghai"))
    }

    @After fun after(): Unit = runBlocking { active.close(); check(root.deleteRecursively()) }

    @Test fun full_archive_round_trip_preserves_all_stored_facts_settings_and_every_original_picture(): Unit = runBlocking {
        val pictures = RestoreSyntheticFixture.insert(active)
        val original = active.backupSnapshotSource().capture()
        val oldId = active.id
        val oldDirectory = active.directory
        val archive = File(root, "first.lithicbackup")
        assertTrue(BackupExportService(context, active).export(RestoreSyntheticFixture.target(archive)) is BackupExportResult.Success)
        val validated = BackupValidator().validate(archive.toPath(), Files.createDirectory(root.toPath().resolve("extract-first")))
        assertEquals(original, validated.snapshot)
        val writer = RestoredGenerationWriter()
        val candidate = store.prepareRestore(ZoneId.of(validated.snapshot.settings.recordZoneId)) { writer.populate(it, validated) }
        assertEquals(oldId, store.open().id)
        active = store.activateRestore(oldId, candidate)
        assertNotEquals(oldId, active.id)
        writer.verify(active, original)
        assertEquals(original, active.backupSnapshotSource().capture())
        assertEquals(ZoneId.of("Asia/Shanghai"), active.settings.recordZone())
        pictures.forEach { (path, bytes) ->
            assertArrayEquals(bytes, File(active.directory, path).readBytes())
            assertArrayEquals(bytes, File(oldDirectory, path).readBytes())
        }
        assertEquals(setOf("media/nested/cover.png", "media/cover.jpg", "media/unreferenced.png"),
            active.backupSnapshotSource().capture().data.mediaAssets.map { it.relativePath }.toSet())
        val second = File(root, "second.lithicbackup")
        assertTrue(BackupExportService(context, active).export(RestoreSyntheticFixture.target(second)) is BackupExportResult.Success)
        val reread = BackupValidator().validate(second.toPath(), Files.createDirectory(root.toPath().resolve("extract-second")))
        assertEquals(original, reread.snapshot)
        pictures.forEach { (path, bytes) -> assertArrayEquals(bytes, Files.readAllBytes(reread.directory.resolve(path))) }
        active.close()
        store = DataGenerationStore(context, root)
        active = store.open(ZoneId.of("America/New_York"))
        assertEquals(candidate.id, active.id)
        writer.verify(active, original)
    }

    @Test fun restored_repositories_recompute_observation_day_without_new_payments_or_changed_timestamps(): Unit = runBlocking {
        RestoreSyntheticFixture.insert(active)
        val original = active.backupSnapshotSource().capture()
        val archive = File(root, "recompute.lithicbackup")
        assertTrue(BackupExportService(context, active).export(RestoreSyntheticFixture.target(archive)) is BackupExportResult.Success)
        val validated = BackupValidator().validate(archive.toPath(), Files.createDirectory(root.toPath().resolve("extract-recompute")))
        val candidate = store.prepareRestore(ZoneId.of(validated.snapshot.settings.recordZoneId)) {
            RestoredGenerationWriter().populate(it, validated)
        }
        active = store.activateRestore(active.id, candidate)
        val projectId = original.data.projects.single { it.name == "合成长存项目" }.id
        val endedId = original.data.projects.single { it.name == "合成已结束项目" }.id
        val subscriptionId = original.data.subscriptions.single().id
        val onJan20 = LocalDate.parse("2026-01-20")
        val projects = ProjectRepository(active.database, active.coordinator, FixedBusinessClock(onJan20))
        val subscriptions = SubscriptionRepository(active.database, active.coordinator, FixedBusinessClock(onJan20))
        val janProject = requireNotNull(projects.loadProjectDetail(projectId))
        assertEquals(20L, janProject.cost.heldDays)
        assertEquals(10000L, janProject.cost.netCents)
        assertEquals(0, requireNotNull(janProject.cost.dailyRate).compareTo(BigDecimal("5")))
        val janSubscription = requireNotNull(subscriptions.loadSubscriptionDetail(subscriptionId))
        assertEquals(SubscriptionDisplayStatus.ACTIVE, janSubscription.cost.displayStatus)
        assertEquals(33000L, janSubscription.cost.confirmedPaidCents)
        assertEquals(60L, janSubscription.cost.standardPurchasedDays)
        assertEquals(0, janSubscription.cost.currentRate.compareTo(BigDecimal("5")))
        val feb = requireNotNull(subscriptions.loadSubscriptionDetail(subscriptionId, LocalDate.parse("2026-02-01")))
        assertEquals(0, feb.cost.currentRate.compareTo(BigDecimal("6")))
        assertEquals(33000L, feb.cost.confirmedPaidCents)
        val ended = requireNotNull(projects.loadProjectDetail(endedId, LocalDate.parse("2026-03-01")))
        assertEquals(10L, ended.cost.heldDays)
        assertEquals(5000L, ended.cost.netCents)
        assertFalse(ended.cost.contributesToCurrentTotal)
        val stopped = requireNotNull(subscriptions.loadSubscriptionDetail(subscriptionId, LocalDate.parse("2026-03-01")))
        assertEquals(SubscriptionDisplayStatus.STOPPED, stopped.cost.displayStatus)
        assertEquals(0, stopped.cost.currentRate.compareTo(BigDecimal.ZERO))
        assertEquals(original, active.backupSnapshotSource().capture())
    }

    @Test fun empty_media_backup_can_replace_an_existing_dataset_without_merging_old_rows(): Unit = runBlocking {
        val original = active.backupSnapshotSource().capture()
        val archive = File(root, "empty.lithicbackup")
        assertTrue(BackupExportService(context, active).export(RestoreSyntheticFixture.target(archive)) is BackupExportResult.Success)
        val validated = BackupValidator().validate(archive.toPath(), Files.createDirectory(root.toPath().resolve("extract-empty")))
        RestoreSyntheticFixture.insert(active)
        assertEquals(2L, active.database.projectDao().countProjects())
        val candidate = store.prepareRestore(ZoneId.of(validated.snapshot.settings.recordZoneId)) {
            RestoredGenerationWriter().populate(it, validated)
        }
        active = store.activateRestore(active.id, candidate)
        assertEquals(original, active.backupSnapshotSource().capture())
        assertEquals(0L, active.database.projectDao().countProjects())
        assertTrue(File(active.directory, "media").listFiles()!!.isEmpty())
    }
}
