package com.cooper.lithic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import android.system.Os
import android.system.OsConstants
import android.system.StructStat
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.BuildConfig
import com.cooper.lithic.app.LithicApplication
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LocalMediaAssetEntity
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.repository.ReportRepository
import com.cooper.lithic.domain.subscription.BillingCycle
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Synthetic records for the actual MainActivity owner of the disposable acceptance AVD only. */
internal object HomeAndroidAcceptanceFixture {
    const val INVESTMENT_CENTS = 6_248_000L
    private const val AVD = "Lithic_Home_Acceptance_Repair05_API36"
    private const val PACKAGE = "com.cooper.lithic.debug"
    private const val TEST_PACKAGE = "$PACKAGE.test"
    private const val RUNNER = "androidx.test.runner.AndroidJUnitRunner"

    data class Seeded(val projectIds: List<String>, val subscriptionIds: List<String>, val investmentCents: Long)

    fun requireDisposableEmulator() {
        val arguments = InstrumentationRegistry.getArguments()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        check(arguments.getString("lithicHomeAcceptance") == "fresh-api36-emulator")
        check(arguments.getString("lithicHomeAcceptanceAvd") == AVD)
        check(BuildConfig.DEBUG && instrumentation.targetContext.packageName == PACKAGE)
        check(instrumentation.context.packageName == TEST_PACKAGE && instrumentation.javaClass.name == RUNNER)
        check(Build.VERSION.SDK_INT == 36 && Build.SUPPORTED_ABIS.firstOrNull() == "x86_64")
        fun property(name: String): String = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("getprop $name"),
        ).bufferedReader().use { it.readLine().orEmpty() }
        check(property("ro.kernel.qemu") == "1" && property("ro.boot.qemu.avd_name") == AVD) {
            "Synthetic fixture requires the named disposable emulator"
        }
    }

    suspend fun prepare(application: LithicApplication, methodRunId: String): Session = withContext(Dispatchers.IO) {
        requireDisposableEmulator()
        require(UUID.fromString(methodRunId).toString() == methodRunId)
        val generation = application.appContainer.openStorage()
        generation.coordinator.withSnapshot {
            generation.database.withTransaction { requireOwnedLedger(generation, Rows()) }
        }
        Session(application, generation, methodRunId)
    }

    /** Each method holds its own seed receipt. An abandoned or modified fixture requires inspection. */
    class Session internal constructor(
        private val application: LithicApplication,
        private val generation: ManagedGeneration,
        private val methodRunId: String,
    ) {
        private var rows: Rows? = null

        suspend fun seed(): Seeded = withContext(Dispatchers.IO) {
            requireCurrentOwner()
            check(rows == null) { "This acceptance method has already seeded its ledger" }
            val db = generation.database
            val day = LocalDate.now(generation.settings.recordZone())
            val now = Instant.now()
            val cameraStart = day.minusDays(120)
            val bagStart = day.minusDays(59)
            val projectIds = listOf(id(1), id(2))
            val subscriptionIds = listOf(id(3), id(4))
            val landscape = buildCover(id(10), 600, 360, landscape = true)
            val portrait = buildCover(id(11), 360, 600, landscape = false)
            val missing = LocalMediaAssetEntity(id(12), "media/${id(12)}.png", "image/png", 16, 16, 64,
                "0".repeat(64))
            val owned = Rows(
                projects = listOf(
                    LongTermProjectEntity(projectIds[0], "合成横向相机", LithicDatabase.PROJECT_FALLBACK_ID,
                        cameraStart, coverAssetId = landscape.asset.id, createdAt = now, updatedAt = now),
                    LongTermProjectEntity(projectIds[1], "合成纵向背包", LithicDatabase.PROJECT_FALLBACK_ID,
                        bagStart, coverAssetId = portrait.asset.id, createdAt = now, updatedAt = now),
                ),
                events = listOf(
                    projectEvent(20, projectIds[0], MoneyDirection.OUTLAY, 5_000_000, cameraStart,
                        ProjectMoneyEventRole.INITIAL),
                    projectEvent(21, projectIds[0], MoneyDirection.RECOVERY, 4_879_968, day,
                        ProjectMoneyEventRole.REGULAR),
                    projectEvent(22, projectIds[1], MoneyDirection.OUTLAY, 1_248_000, bagStart,
                        ProjectMoneyEventRole.INITIAL),
                    projectEvent(23, projectIds[1], MoneyDirection.RECOVERY, 1_228_080, day,
                        ProjectMoneyEventRole.REGULAR),
                ),
                subscriptions = listOf(
                    SubscriptionEntity(subscriptionIds[0], "合成阅读服务", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
                        createdAt = now, updatedAt = now),
                    SubscriptionEntity(subscriptionIds[1], "合成云盘服务", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
                        coverAssetId = missing.id, createdAt = now, updatedAt = now),
                ),
                renewals = listOf(renewal(30, subscriptionIds[0], day, 7_440), renewal(31, subscriptionIds[1], day, 2_520)),
                media = listOf(landscape.asset, portrait.asset, missing),
            )
            generation.coordinator.withWrite {
                rows = db.withTransaction {
                    requireOwnedLedger(generation, Rows())
                    val coverFiles = listOf(landscape, portrait).map { cover ->
                        val file = coverFile(generation, cover.asset)
                        check(file.createNewFile()) { "Refusing to overwrite an existing cover" }
                        FileOutputStream(file).use { output -> output.write(cover.bytes); output.fd.sync() }
                        val stat = Os.lstat(file.path)
                        OwnedCover(cover.asset, stat.st_dev, stat.st_ino)
                    }
                    owned.media.forEach { db.mediaDao().insert(it) }
                    owned.projects.forEach { db.projectDao().insertProject(it) }
                    owned.events.forEach { db.projectDao().insertEvent(it) }
                    owned.subscriptions.forEach { db.subscriptionDao().insertSubscription(it) }
                    owned.renewals.forEach { db.subscriptionDao().insertRenewal(it) }
                    owned.copy(coverFiles = coverFiles)
                }
            }

            val overview = ReportRepository(db, generation.coordinator, FixedBusinessClock(day)).loadOverview()
            check(overview.projectCount == 2L && overview.subscriptionCount == 2L)
            check(overview.totalInvestmentCents == INVESTMENT_CENTS)
            check(overview.totalRate.compareTo(BigDecimal("16.56")) == 0)
            Seeded(projectIds, subscriptionIds, INVESTMENT_CENTS)
        }

        suspend fun close() = withContext(Dispatchers.IO + NonCancellable) {
            requireCurrentOwner()
            val db = generation.database
            generation.coordinator.withWrite {
                val owned = rows ?: Rows()
                db.withTransaction {
                    requireOwnedLedger(generation, owned)
                    val sqlite = db.openHelper.writableDatabase
                    owned.events.forEach { sqlite.deleteOwnedRow("project_money_events", it.id) }
                    owned.renewals.forEach { sqlite.deleteOwnedRow("subscription_renewals", it.id) }
                    owned.projects.forEach { sqlite.deleteOwnedRow("long_term_projects", it.id) }
                    owned.subscriptions.forEach { sqlite.deleteOwnedRow("subscriptions", it.id) }
                    owned.media.forEach { sqlite.deleteOwnedRow("local_media_assets", it.id) }
                }
                owned.coverFiles.forEach { cover ->
                    val file = verifiedCoverFile(generation, cover)
                    check(file.delete()) { "Unable to remove this acceptance method's cover" }
                }
                db.withTransaction { requireOwnedLedger(generation, Rows()) }
                rows = null
            }
        }

        private suspend fun requireCurrentOwner() {
            requireDisposableEmulator()
            check(application.appContainer.openStorage() === generation) { "The acceptance generation changed" }
        }

        private fun id(number: Int) = UUID.nameUUIDFromBytes("$methodRunId:$number".toByteArray(Charsets.UTF_8)).toString()

        private fun projectEvent(number: Int, project: String, direction: MoneyDirection, cents: Long, day: LocalDate,
            role: ProjectMoneyEventRole) = ProjectMoneyEventEntity(id(number), project, direction, cents, day,
            role = role, operationId = id(number + 100))

        private fun renewal(number: Int, subscription: String, day: LocalDate, cents: Long): SubscriptionRenewalEntity {
            val end = day.plusMonths(1)
            return SubscriptionRenewalEntity(id(number), subscription, day, day, end, end, cents,
                BillingCycle.MONTHLY, BillingCycle.MONTHLY.standardDays, id(number + 100))
        }
    }

    private data class Rows(
        val projects: List<LongTermProjectEntity> = emptyList(),
        val events: List<ProjectMoneyEventEntity> = emptyList(),
        val subscriptions: List<SubscriptionEntity> = emptyList(),
        val renewals: List<SubscriptionRenewalEntity> = emptyList(),
        val media: List<LocalMediaAssetEntity> = emptyList(),
        val coverFiles: List<OwnedCover> = emptyList(),
    )

    private suspend fun requireOwnedLedger(generation: ManagedGeneration, expected: Rows) {
        val db = generation.database
        val sqlite = db.openHelper.writableDatabase
        val counts = mapOf(
            "categories" to 2,
            "long_term_projects" to expected.projects.size,
            "project_money_events" to expected.events.size,
            "subscriptions" to expected.subscriptions.size,
            "subscription_renewals" to expected.renewals.size,
            "local_media_assets" to expected.media.size,
            "subscription_state_changes" to 0,
            "replacements" to 0,
            "notification_receipts" to 0,
            "milestone_watermarks" to 0,
        )
        counts.forEach { (table, count) ->
            sqlite.query("SELECT COUNT(*) FROM $table").use { cursor ->
                check(cursor.moveToFirst() && cursor.getLong(0) == count.toLong()) { "Unexpected rows in $table" }
            }
        }
        db.requireDefaults()
        check(expected.projects.all { db.projectDao().findProject(it.id) == it }) { "Unexpected project rows" }
        check(expected.events.all { db.projectDao().findEvent(it.id) == it }) { "Unexpected project amount rows" }
        check(expected.subscriptions.all { db.subscriptionDao().findSubscription(it.id) == it }) { "Unexpected subscription rows" }
        check(expected.renewals.all { db.subscriptionDao().findRenewal(it.id) == it }) { "Unexpected payment rows" }
        check(expected.media.all { db.mediaDao().find(it.id) == it }) { "Unexpected media rows" }

        val files = checkNotNull(mediaDirectory(generation).list()) { "Cannot enumerate acceptance media" }
        check(files.toSet() == expected.coverFiles.map { "${it.asset.id}.png" }.toSet()) { "Unexpected media files" }
        expected.coverFiles.forEach { verifiedCoverFile(generation, it) }
    }

    private fun SupportSQLiteDatabase.deleteOwnedRow(table: String, id: String) {
        compileStatement("DELETE FROM $table WHERE id = ?").use { statement ->
            statement.bindString(1, id)
            check(statement.executeUpdateDelete() == 1) { "Ownership changed during fixture cleanup in $table" }
        }
    }

    private fun mediaDirectory(generation: ManagedGeneration): File = File(generation.directory, "media").also { directory ->
        val stat = Os.lstat(directory.path)
        check(directory.absoluteFile == directory.canonicalFile && OsConstants.S_ISDIR(stat.st_mode) &&
            stat.st_uid == Process.myUid()) { "Acceptance media directory is not private and canonical" }
    }

    private fun coverFile(generation: ManagedGeneration, asset: LocalMediaAssetEntity): File {
        check(asset.relativePath == "media/${asset.id}.png") { "Unexpected acceptance media path" }
        return File(mediaDirectory(generation), "${asset.id}.png")
    }

    private data class OwnedCover(val asset: LocalMediaAssetEntity, val device: Long, val inode: Long)

    private fun verifiedCoverFile(generation: ManagedGeneration, cover: OwnedCover): File {
        val file = coverFile(generation, cover.asset)
        check(file.absoluteFile == file.canonicalFile) { "Acceptance cover path changed" }
        fun requireSameFile(stat: StructStat) {
            check(OsConstants.S_ISREG(stat.st_mode) && stat.st_uid == Process.myUid() && stat.st_nlink == 1L &&
                stat.st_dev == cover.device && stat.st_ino == cover.inode && stat.st_size == cover.asset.byteLength) {
                "Acceptance cover identity changed"
            }
        }
        requireSameFile(Os.lstat(file.path))
        val descriptor = Os.open(file.path, OsConstants.O_RDONLY or OsConstants.O_NOFOLLOW or OsConstants.O_NONBLOCK, 0)
        try {
            DataInputStream(FileInputStream(descriptor)).use { input ->
                requireSameFile(Os.fstat(descriptor))
                val bytes = ByteArray(cover.asset.byteLength.toInt())
                input.readFully(bytes)
                check(input.read() == -1 && sha256(bytes) == cover.asset.sha256) { "Acceptance cover content changed" }
                requireSameFile(Os.fstat(descriptor))
            }
        } finally {
            if (descriptor.valid()) Os.close(descriptor)
        }
        requireSameFile(Os.lstat(file.path))
        return file
    }

    private data class Cover(val asset: LocalMediaAssetEntity, val bytes: ByteArray)

    private fun buildCover(assetId: String, width: Int, height: Int, landscape: Boolean): Cover {
        val relativePath = "media/$assetId.png"
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bytes = try {
            drawCover(Canvas(bitmap), landscape)
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally { bitmap.recycle() }
        return Cover(LocalMediaAssetEntity(assetId, relativePath, "image/png", width, height,
            bytes.size.toLong(), sha256(bytes)), bytes)
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun drawCover(canvas: Canvas, landscape: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        fun shape(left: Float, top: Float, right: Float, bottom: Float, radius: Float, color: Int) {
            paint.color = color
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
        }
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        if (landscape) {
            canvas.drawColor(Color.rgb(221, 205, 180))
            shape(120f, 80f, 480f, 275f, 20f, Color.rgb(47, 51, 50))
            shape(155f, 64f, 248f, 110f, 7f, Color.rgb(70, 75, 70))
            paint.color = Color.rgb(144, 164, 154)
            canvas.drawCircle(width / 2f, height / 2f, 83f, paint)
            paint.color = Color.rgb(49, 79, 80)
            canvas.drawCircle(width / 2f, height / 2f, 59f, paint)
            shape(398f, 104f, 450f, 130f, 6f, Color.rgb(229, 212, 184))
        } else {
            canvas.drawColor(Color.rgb(207, 222, 229))
            shape(134f, 190f, 226f, 243f, 18f, Color.rgb(85, 96, 105))
            shape(80f, 216f, 280f, 415f, 38f, Color.rgb(100, 114, 123))
            shape(105f, 315f, 255f, 393f, 15f, Color.rgb(74, 86, 96))
            shape(140f, 326f, 220f, 333f, 3f, Color.rgb(204, 181, 147))
        }
    }
}
