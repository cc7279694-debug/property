package com.cooper.lithic.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** Direct pipeline regression: expose platform IO errors before result mapping can hide the cause. */
@RunWith(AndroidJUnit4::class)
class BackupArchiveAndroidCompatibilityTest {
    @Test fun archive_pipeline_preserves_platform_exception_and_verifies_real_bytes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.noBackupFilesDir, "m11b-platform-${UUID.randomUUID()}")
        check(root.mkdir())
        val generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        var phase = "staging-directory"
        var pathTrace = ""
        try {
            val staging = Files.createTempDirectory(ownedBackupCacheDirectory(context), "m11b-platform-")
            pathTrace = generateSequence(staging) { it.parent }.joinToString("; ") { node ->
                val native = android.system.Os.lstat(node.toString())
                "$node: absolute=${node.isAbsolute}, nioLink=${Files.isSymbolicLink(node)}, mode=${native.st_mode}"
            }
            phase = "capture-and-prepare"
            val core = RoomBackupSnapshotSource(generation).captureInto { BackupCoreStager().prepare(it, staging) }
            phase = "manifest"
            val manifest = BackupManifestV1.complete(core, "synthetic", Instant.parse("2026-01-01T00:00:00Z"))
            phase = "archive-create-and-verify"
            val archive = BackupArchiveFiles.create(core, manifest, staging.resolve("complete.lithicbackup"))
            phase = "closed-archive-verification"
            check(archive == BackupArchiveFiles.verify(core, manifest, archive.path))
            Files.delete(archive.path)
            core.close()
            Files.delete(staging)
        } catch (failure: Exception) {
            throw AssertionError("Backup platform phase=$phase: $failure; trace=$pathTrace", failure)
        } finally {
            generation.close()
            check(root.deleteRecursively())
        }
    }
}
