package com.cooper.lithic.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.CategoryMutationResult
import com.cooper.lithic.data.repository.CategoryRepository
import com.cooper.lithic.data.repository.ReportRepository
import com.cooper.lithic.data.repository.SearchRepository
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

/** M14 uses the frozen complete exporter/restorer; format v1 and all seven table facts stay intact. */
@RunWith(AndroidJUnit4::class)
class M14CategoryBackupRoundTripTest {
    @Test fun renamed_and_merged_categories_round_trip_with_exact_facts_settings_and_media(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.noBackupFilesDir, "m14-category-backup-${UUID.randomUUID()}").canonicalFile
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
            val oldRepository = CategoryRepository(oldTarget.database, oldTarget.coordinator)
            val pictures = RestoreSyntheticFixture.insert(source)
            val repository = CategoryRepository(source.database, source.coordinator)
            val customProject = repository.list(CategoryType.PROJECT).single { !it.category.isFallback }.category
            val customSubscription = repository.list(CategoryType.SUBSCRIPTION).single { !it.category.isFallback }.category
            assertTrue(repository.rename(customSubscription, "长期服务") is CategoryMutationResult.Applied)
            assertTrue(repository.create(CategoryType.PROJECT, "随身设备") is CategoryMutationResult.Applied)
            val destination = repository.list(CategoryType.PROJECT).single { it.category.name == "随身设备" }.category
            assertTrue(repository.merge(repository.previewMerge(customProject.id, destination.id)) is CategoryMutationResult.Applied)
            val expected = source.backupSnapshotSource().capture()
            val asOf = LocalDate.of(2026, 2, 3)
            val overview = ReportRepository(source.database, source.coordinator, FixedBusinessClock(asOf)).loadOverview()
            val search = SearchRepository(source.database, source.coordinator, FixedBusinessClock(asOf)).loadWindow("合成", 1)
            assertEquals(setOf("合成长存项目", "合成已结束项目", "合成固定订阅"), search.items.map { it.name }.toSet())
            val archive = File(root, "categories.lithicbackup")
            assertTrue(BackupExportService(context, source).export(RestoreSyntheticFixture.target(archive)) is BackupExportResult.Success)
            val restorer = RestoreCoordinator(context, targetOwner)
            val preview = restorer.prepare { archive.inputStream() }
            assertEquals(expected.data.recordCounts(), preview.backupCounts)
            assertEquals(RestoreResult.Success, restorer.confirm(preview.token))
            val restored = targetOwner.openStorage()
            assertNotEquals(oldTarget.id, restored.id)
            assertEquals(expected, restored.backupSnapshotSource().capture())
            val newRepository = CategoryRepository(restored.database, restored.coordinator)
            assertEquals(repository.list(CategoryType.PROJECT), newRepository.list(CategoryType.PROJECT))
            assertEquals(repository.list(CategoryType.SUBSCRIPTION), newRepository.list(CategoryType.SUBSCRIPTION))
            assertEquals(overview, ReportRepository(restored.database, restored.coordinator, FixedBusinessClock(asOf)).loadOverview())
            assertEquals(search, SearchRepository(restored.database, restored.coordinator, FixedBusinessClock(asOf)).loadWindow("合成", 1))
            pictures.forEach { (path, bytes) -> assertArrayEquals(bytes, File(restored.directory, path).readBytes()) }
            assertFalse(oldRepository.create(CategoryType.PROJECT, "旧代迟到类别") is CategoryMutationResult.Applied)
            assertEquals(expected, restored.backupSnapshotSource().capture())
        } finally {
            sourceOwner.close()
            targetOwner.close()
            check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.name.startsWith("m14-category-backup-"))
            check(root.deleteRecursively())
        }
    }
}
