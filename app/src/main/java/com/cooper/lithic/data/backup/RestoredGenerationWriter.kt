package com.cooper.lithic.data.backup

import android.graphics.BitmapFactory
import android.os.Process
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import androidx.room.withTransaction
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ReplacementEntity
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Populates only an unpublished, empty generation. Activation and ownership stay with its store. */
internal class RestoredGenerationWriter {
    suspend fun populate(generation: ManagedGeneration, backup: ValidatedBackup) = withContext(Dispatchers.IO) {
        val caller = currentCoroutineContext()
        val checkpoint = { caller.ensureActive() }
        val snapshot = backup.snapshot
        BackupFormats.requireSupported(backup.manifest)
        if(backup.manifest.formatVersion==1) snapshot.data.legacyProjection()
        require(backup.manifest.recordZoneId == snapshot.settings.recordZoneId &&
            BackupFormats.normalizedCounts(backup.manifest) == snapshot.completeRecordCounts()) { "Restore manifest disagrees with its validated facts" }
        val media = mediaEntries(snapshot)
        val declared = backup.manifest.files.associateBy { it.path }
        require(declared.size == backup.manifest.files.size &&
            declared.keys == media.keys + setOf("data.json", "settings.json")) { "Restore payload set changed" }
        media.forEach { (path, entry) -> require(declared.getValue(path) == entry) { "Media metadata disagrees with manifest" } }
        requireMediaSnapshotBudget(backup.manifest.files.filter { it.path in setOf("data.json", "settings.json") }, snapshot.data.mediaAssets)
        generation.coordinator.withWrite {
            checkpoint()
            val root = generation.directory.toPath()
            val rootIdentity = ownedDirectory(root)
            val destinationMedia = root.resolve("media")
            ownedDirectory(destinationMedia)
            require(Files.list(destinationMedia).use { !it.findAny().isPresent }) { "Restore destination media is not empty" }
            val sqlite = generation.database.openHelper.writableDatabase
            check(sqlite.version == 5) { "Restore requires Room schema 5" }
            for (table in TABLES) sqlite.query("SELECT COUNT(*) FROM $table").use { cursor ->
                check(cursor.moveToFirst() && cursor.getLong(0) == 0L && !cursor.moveToNext()) {
                    "Restore may populate only an empty database"
                }
            }
            check(generation.settings.backupRecordZoneWithinSnapshot(generation.coordinator).id == snapshot.settings.recordZoneId) {
                "Restore settings were initialized with a different record zone"
            }
            val sourceRoot = backup.directory
            ownedDirectory(sourceRoot)
            require(sourceRoot != root && !sourceRoot.startsWith(root) && !root.startsWith(sourceRoot)) {
                "Restore input and destination must be independent directories"
            }
            // Admission precedes the first destination write. Recheck each source during the copy too.
            snapshot.data.mediaAssets.forEach { asset -> verifyMedia(sourceRoot, asset, checkpoint) }
            for (asset in snapshot.data.mediaAssets) {
                checkpoint()
                copyMedia(sourceRoot, root, asset, checkpoint)
            }
            check(sameDirectory(rootIdentity, ownedDirectory(root))) { "Restore generation identity changed" }
            generation.database.withTransaction {
                val database = generation.database
                snapshot.data.categories.forEach { value -> database.categoryDao().insert(CategoryEntity(
                    value.id, CategoryType.valueOf(value.type), value.name, value.normalizedName, value.isFallback)) }
                snapshot.data.mediaAssets.forEach { value -> database.mediaDao().insert(LocalMediaAssetEntity(
                    value.id, value.relativePath, value.mime, value.width, value.height, value.byteLength, value.sha256)) }
                snapshot.data.projects.forEach { value -> database.projectDao().insertProject(LongTermProjectEntity(
                    value.id, value.name, value.categoryId, LocalDate.parse(value.startedOn), value.endedOn?.let(LocalDate::parse),
                    value.endReason, value.coverAssetId, value.note, Instant.parse(value.createdAt), Instant.parse(value.updatedAt))) }
                snapshot.data.projectMoneyEvents.forEach { value -> database.projectDao().insertEvent(ProjectMoneyEventEntity(
                    value.id, value.projectId, MoneyDirection.valueOf(value.direction), value.amountCents,
                    LocalDate.parse(value.occurredOn), value.description, ProjectMoneyEventRole.valueOf(value.role), value.operationId,
                    MoneyBasis.valueOf(value.basis),value.exchangeGroupId)) }
                snapshot.data.replacements.forEach { value -> database.replacementDao().insert(ReplacementEntity(
                    value.id,value.predecessorId,value.successorId,Instant.parse(value.createdAt))) }
                snapshot.data.subscriptions.forEach { value -> database.subscriptionDao().insertSubscription(SubscriptionEntity(
                    value.id, value.name, value.categoryId, value.coverAssetId, value.note, Instant.parse(value.createdAt), Instant.parse(value.updatedAt))) }
                snapshot.data.subscriptionRenewals.forEach { value -> database.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(
                    value.id, value.subscriptionId, LocalDate.parse(value.paidOn), LocalDate.parse(value.effectiveFrom),
                    LocalDate.parse(value.paidUntilExclusive), LocalDate.parse(value.reminderDueOn), value.amountCents,
                    BillingCycle.valueOf(value.cycle), value.standardDays, value.operationId)) }
                snapshot.data.subscriptionStateChanges.forEach { value -> database.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(
                    value.id, value.subscriptionId, LocalDate.parse(value.effectiveOn), SubscriptionOperationState.valueOf(value.state),
                    value.reason, value.note, value.operationId)) }
                snapshot.notificationReceipts.forEach { value -> database.notificationReceiptDao().insert(
                    com.cooper.lithic.data.local.NotificationReceiptEntity(value.logicalKey, value.ownerId, value.kind,
                        value.boundary, Instant.parse(value.publishedAt))) }
                snapshot.milestoneWatermarks.forEach { value -> database.milestoneWatermarkDao().upsert(
                    com.cooper.lithic.data.local.MilestoneWatermarkEntity(
                        com.cooper.lithic.domain.reminder.MilestoneOwnerType.valueOf(value.ownerType), value.ownerId,
                        LocalDate.parse(value.cutoffOn), value.suppressedMask)) }
                checkpoint()
            }
            // Still private and unpublished. A failure here cannot activate an incomplete settings/data pair.
            generation.settings.restoreRemindersWithinWrite(generation.coordinator, snapshot.reminderPreferences)
        }
        // The candidate remains private. Do not re-enter its non-reentrant coordinator above.
        verify(generation, snapshot)
    }

    /** Reused after close/reopen and during the first new-generation open, before declaring it stable. */
    suspend fun verify(generation: ManagedGeneration, expected: BackupSnapshot) = withContext(Dispatchers.IO) {
        val caller = currentCoroutineContext()
        val checkpoint = { caller.ensureActive() }
        RoomBackupSnapshotSource(generation).captureInto { actual ->
            checkpoint()
            check(actual == expected) { "Restored facts or settings differ from the validated backup" }
            generation.database.requireDefaults()
            val sqlite = generation.database.openHelper.readableDatabase
            check(sqlite.version == 5) { "Restored database schema changed" }
            sqlite.query("PRAGMA integrity_check").use { cursor ->
                check(cursor.moveToFirst() && cursor.getString(0) == "ok" && !cursor.moveToNext()) {
                    "Restored SQLite integrity validation failed"
                }
            }
            sqlite.query("PRAGMA foreign_key_check").use { cursor ->
                check(!cursor.moveToFirst()) { "Restored database contains broken relationships" }
            }
            val root = generation.directory.toPath()
            val identity = ownedDirectory(root)
            val expectedMedia = mediaEntries(expected)
            requireExactMediaTree(root, expectedMedia.keys)
            expected.data.mediaAssets.forEach { asset -> verifyMedia(root, asset, checkpoint) }
            check(sameDirectory(identity, ownedDirectory(root))) { "Restored generation changed during verification" }
        }
    }

    private fun mediaEntries(snapshot: BackupSnapshot): Map<String, BackupFileEntry> {
        val entries = snapshot.data.mediaAssets.map { asset ->
            require(asset.mime in setOf("image/jpeg", "image/png") && asset.width in 1..1600 && asset.height in 1..1600 &&
                asset.byteLength in 1 until BackupLimits.MAX_MEDIA_BYTES) { "Restored media exceeds supported cover limits" }
            BackupFileEntry(asset.relativePath, asset.byteLength, asset.sha256.lowercase(java.util.Locale.ROOT))
        }
        require(entries.map { it.path }.toSet().size == entries.size) { "Duplicate restored media path" }
        return entries.associateBy { it.path }
    }

    private fun copyMedia(sourceRoot: Path, destinationRoot: Path, asset: BackupMediaAssetV1, checkpoint: () -> Unit) {
        val source = sourceRoot.resolve(asset.relativePath)
        val before = ownedFile(sourceRoot, source)
        val destination = destinationRoot.resolve(asset.relativePath)
        var parent = destinationRoot
        for (part in destinationRoot.relativize(destination.parent)) {
            val parentIdentity = ownedDirectory(parent)
            parent = parent.resolve(part)
            if (!Files.exists(parent, NOFOLLOW_LINKS)) Files.createDirectory(parent)
            ownedDirectory(parent)
            check(sameDirectory(parentIdentity, ownedDirectory(parent.parent))) { "Restore media parent changed" }
        }
        val parentIdentity = ownedDirectory(destination.parent)
        val sourceFd = Os.open(source.toString(), OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or CLOSE_ON_EXEC, 0)
        try {
            check(sameFile(before, Os.fstat(sourceFd))) { "Restore source changed during open" }
            val outputFd = Os.open(destination.toString(), OsConstants.O_WRONLY or OsConstants.O_CREAT or
                OsConstants.O_EXCL or OsConstants.O_NOFOLLOW or CLOSE_ON_EXEC, OWNER_READ_WRITE)
            try {
                val outputIdentity = Os.fstat(outputFd)
                requireRegularOwned(outputIdentity)
                val digest = MessageDigest.getInstance("SHA-256")
                var count = 0L
                FileInputStream(sourceFd).use { input ->
                    FileOutputStream(outputFd).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            checkpoint()
                            val read = input.read(buffer)
                            if (read < 0) break
                            if (read == 0) throw IOException("Restore media reader made no progress")
                            count = Math.addExact(count, read.toLong())
                            if (count > asset.byteLength || count >= BackupLimits.MAX_MEDIA_BYTES)
                                throw IOException("Restore media grew during copy")
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                        }
                        check(count == asset.byteLength && sha(digest).equals(asset.sha256, ignoreCase = true)) {
                            "Restore media bytes changed during copy"
                        }
                        check(sameFile(before, Os.fstat(sourceFd)) && sameFile(before, ownedFile(sourceRoot, source))) {
                            "Restore source identity changed during copy"
                        }
                        output.flush()
                        output.fd.sync()
                        val after = Os.fstat(outputFd)
                        requireRegularOwned(after)
                        check(sameIdentity(outputIdentity, after) && sameFile(after, ownedFile(destinationRoot, destination))) {
                            "Restore destination identity changed during copy"
                        }
                    }
                }
            } finally { if (outputFd.valid()) Os.close(outputFd) }
        } finally { if (sourceFd.valid()) Os.close(sourceFd) }
        check(sameDirectory(parentIdentity, ownedDirectory(destination.parent))) { "Restore destination directory changed" }
        verifyMedia(destinationRoot, asset, checkpoint)
        // Persist both the file entry and any nested directories created for an accepted v2 path.
        var sync = destination.parent
        while (true) {
            syncDirectory(sync)
            if (sync == destinationRoot) break
            sync = sync.parent
        }
    }

    private fun verifyMedia(root: Path, asset: BackupMediaAssetV1, checkpoint: () -> Unit) {
        BackupEntryFiles.requirePath(asset.relativePath)
        val path = root.resolve(asset.relativePath)
        val before = ownedFile(root, path)
        check(before.st_size == asset.byteLength) { "Restored media length differs" }
        val fd = Os.open(path.toString(), OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or CLOSE_ON_EXEC, 0)
        try {
            check(sameFile(before, Os.fstat(fd))) { "Restore media identity changed during open" }
            val digest = MessageDigest.getInstance("SHA-256")
            var count = 0L
            FileInputStream(fd).use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    checkpoint()
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) throw IOException("Restore verifier made no progress")
                    count = Math.addExact(count, read.toLong())
                    if (count > asset.byteLength) throw IOException("Restored media grew during verification")
                    digest.update(buffer, 0, read)
                }
                check(count == asset.byteLength && sha(digest).equals(asset.sha256, ignoreCase = true)) { "Restored media checksum differs" }
                val dimensions = when (asset.mime) {
                    "image/png" -> BackupPngInspection.inspect(path.toFile(), checkpoint)
                    "image/jpeg" -> BackupJpegInspection.inspect(path.toFile(), checkpoint)
                    else -> throw BackupMediaException("Restore image format is unsupported", true)
                }
                check(dimensions == (asset.width to asset.height)) { "Restored image container dimensions differ" }
                // Bounds are checked before allocating a full raster. JPEG readability is not a
                // strict entropy-integrity certificate; original length/SHA and container checks remain mandatory.
                Os.lseek(fd, 0, OsConstants.SEEK_SET)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
                BitmapFactory.decodeFileDescriptor(fd, null, bounds)
                check(bounds.outWidth == asset.width && bounds.outHeight == asset.height && bounds.outMimeType == asset.mime) {
                    "Restored image decoder bounds disagree"
                }
                checkpoint()
                Os.lseek(fd, 0, OsConstants.SEEK_SET)
                val options = BitmapFactory.Options().apply { inScaled = false }
                val bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options)
                    ?: throw BackupMediaException("Restored image is not Android-readable")
                try {
                    check(bitmap.width == asset.width && bitmap.height == asset.height && options.outMimeType == asset.mime) {
                        "Restored image raster differs from recorded dimensions or format"
                    }
                } finally { bitmap.recycle() }
                check(sameFile(before, Os.fstat(fd)) && sameFile(before, ownedFile(root, path))) {
                    "Restored image identity changed during verification"
                }
            }
        } finally { if (fd.valid()) Os.close(fd) }
    }

    private fun requireExactMediaTree(root: Path, files: Set<String>) {
        val media = root.resolve("media")
        ownedDirectory(media)
        val folders = files.flatMap { path ->
            val parts = path.split('/')
            (2 until parts.size).map { parts.take(it).joinToString("/") }
        }.toSet()
        val actual = Files.walk(media).use { stream ->
            val result = mutableSetOf<String>()
            val iterator = stream.iterator()
            while (iterator.hasNext()) {
                val node = iterator.next()
                if (node == media) continue
                check(result.size < BackupLimits.MAX_ENTRIES) { "Restored media tree exceeds its resource limit" }
                val relative = root.relativize(node).joinToString("/")
                if (relative in folders) ownedDirectory(node) else ownedFile(root, node)
                result += relative
            }
            result
        }
        check(actual == files + folders) { "Restored media contains missing or unknown resources" }
    }

    private fun ownedDirectory(path: Path): StructStat {
        BackupEntryFiles.requireDirectory(path)
        return Os.lstat(path.toString()).also {
            check(OsConstants.S_ISDIR(it.st_mode) && it.st_uid == Process.myUid()) { "Restore directory ownership is unknown" }
        }
    }

    private fun ownedFile(root: Path, path: Path): StructStat {
        ownedDirectory(root)
        require(path.toAbsolutePath().normalize() == path && path.startsWith(root.resolve("media"))) {
            "Restore media leaves its declared private directory"
        }
        var node = root
        for (part in root.relativize(path.parent)) {
            node = node.resolve(part)
            ownedDirectory(node)
        }
        return Os.lstat(path.toString()).also(::requireRegularOwned)
    }

    private fun requireRegularOwned(stat: StructStat) {
        check(OsConstants.S_ISREG(stat.st_mode) && stat.st_uid == Process.myUid() && stat.st_nlink == 1L) {
            "Restore payload is not a uniquely owned regular file"
        }
    }

    private fun syncDirectory(path: Path) {
        val before = ownedDirectory(path)
        val fd = Os.open(path.toString(), OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or CLOSE_ON_EXEC, 0)
        try {
            val opened = Os.fstat(fd)
            check(OsConstants.S_ISDIR(opened.st_mode) && opened.st_uid == Process.myUid() && sameDirectory(before, opened)) {
                "Restore directory changed before sync"
            }
            Os.fsync(fd)
            check(sameDirectory(before, ownedDirectory(path))) { "Restore directory changed during sync" }
        } finally { Os.close(fd) }
    }

    private fun sha(digest: MessageDigest): String = with(BackupEntryFiles) { digest.digest().hex() }
    private fun sameIdentity(a: StructStat, b: StructStat): Boolean = a.st_dev == b.st_dev && a.st_ino == b.st_ino &&
        a.st_uid == b.st_uid && a.st_mode == b.st_mode && a.st_nlink == b.st_nlink
    private fun sameDirectory(a: StructStat, b: StructStat): Boolean = a.st_dev == b.st_dev && a.st_ino == b.st_ino &&
        a.st_uid == b.st_uid && a.st_mode == b.st_mode
    private fun sameFile(a: StructStat, b: StructStat): Boolean = sameIdentity(a, b) && a.st_size == b.st_size &&
        a.st_mtime == b.st_mtime && a.st_ctime == b.st_ctime

    private companion object {
        const val CLOSE_ON_EXEC = 0x80000 // Linux Android UAPI O_CLOEXEC; available on API26.
        const val OWNER_READ_WRITE = 0x180 // 0600.
        val TABLES = listOf("categories", "long_term_projects", "project_money_events", "local_media_assets",
            "subscriptions", "subscription_renewals", "subscription_state_changes","replacements","notification_receipts","milestone_watermarks")
    }
}
