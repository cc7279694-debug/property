package com.cooper.lithic.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.room.withTransaction
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
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Synthetic records for the actual MainActivity owner of the disposable acceptance AVD only. */
internal object HomeAndroidAcceptanceFixture {
    const val INVESTMENT_CENTS = 6_248_000L
    private const val AVD = "Lithic_Home_Acceptance_01_API36"
    private const val PACKAGE = "com.cooper.lithic.debug"

    data class Seeded(val projectIds: List<String>, val subscriptionIds: List<String>, val investmentCents: Long)

    val seeded = Seeded(listOf(id(1), id(2)), listOf(id(3), id(4)), INVESTMENT_CENTS)

    fun requireDisposableEmulator() {
        val arguments = InstrumentationRegistry.getArguments()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        check(arguments.getString("lithicHomeAcceptance") == "fresh-api36-emulator")
        check(arguments.getString("lithicHomeAcceptanceAvd") == AVD)
        check(BuildConfig.DEBUG && instrumentation.targetContext.packageName == PACKAGE)
        check(Build.VERSION.SDK_INT == 36 && Build.SUPPORTED_ABIS.firstOrNull() == "x86_64")
        fun property(name: String): String = ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("getprop $name"),
        ).bufferedReader().use { it.readLine().orEmpty() }
        check(property("ro.kernel.qemu") == "1" && property("ro.boot.qemu.avd_name") == AVD) {
            "Synthetic fixture requires the named disposable emulator"
        }
    }

    suspend fun seed(application: LithicApplication): Seeded = withContext(Dispatchers.IO) {
        requireDisposableEmulator()
        val generation = application.appContainer.openStorage()
        val db = generation.database
        val day = LocalDate.now(generation.settings.recordZone())
        val now = Instant.now()

        generation.coordinator.withWrite {
            db.withTransaction {
                check(db.projectDao().countProjects() == 0L && db.subscriptionDao().countSubscriptions() == 0L)
                check(db.mediaDao().list().isEmpty())
                val landscape = registerCover(generation, id(10), 600, 360, landscape = true)
                val portrait = registerCover(generation, id(11), 360, 600, landscape = false)
                val missing = LocalMediaAssetEntity(id(12), "media/${id(12)}.png", "image/png", 16, 16, 64,
                    "0".repeat(64))
                check(!File(generation.directory, missing.relativePath).exists())
                listOf(landscape, portrait, missing).forEach { db.mediaDao().insert(it) }

                val cameraStart = day.minusDays(120)
                val bagStart = day.minusDays(59)
                db.projectDao().insertProject(LongTermProjectEntity(seeded.projectIds[0], "合成横向相机",
                    LithicDatabase.PROJECT_FALLBACK_ID, cameraStart, coverAssetId = landscape.id,
                    createdAt = now, updatedAt = now))
                db.projectDao().insertProject(LongTermProjectEntity(seeded.projectIds[1], "合成纵向背包",
                    LithicDatabase.PROJECT_FALLBACK_ID, bagStart, coverAssetId = portrait.id,
                    createdAt = now, updatedAt = now))
                listOf(
                    projectEvent(20, seeded.projectIds[0], MoneyDirection.OUTLAY, 5_000_000, cameraStart,
                        ProjectMoneyEventRole.INITIAL),
                    projectEvent(21, seeded.projectIds[0], MoneyDirection.RECOVERY, 4_879_968, day,
                        ProjectMoneyEventRole.REGULAR),
                    projectEvent(22, seeded.projectIds[1], MoneyDirection.OUTLAY, 1_248_000, bagStart,
                        ProjectMoneyEventRole.INITIAL),
                    projectEvent(23, seeded.projectIds[1], MoneyDirection.RECOVERY, 1_228_080, day,
                        ProjectMoneyEventRole.REGULAR),
                ).forEach { db.projectDao().insertEvent(it) }

                db.subscriptionDao().insertSubscription(SubscriptionEntity(seeded.subscriptionIds[0], "合成阅读服务",
                    LithicDatabase.SUBSCRIPTION_FALLBACK_ID, createdAt = now, updatedAt = now))
                db.subscriptionDao().insertSubscription(SubscriptionEntity(seeded.subscriptionIds[1], "合成云盘服务",
                    LithicDatabase.SUBSCRIPTION_FALLBACK_ID, coverAssetId = missing.id,
                    createdAt = now, updatedAt = now))
                db.subscriptionDao().insertRenewal(renewal(30, seeded.subscriptionIds[0], day, 7_440))
                db.subscriptionDao().insertRenewal(renewal(31, seeded.subscriptionIds[1], day, 2_520))
            }
        }

        val overview = ReportRepository(db, generation.coordinator, FixedBusinessClock(day)).loadOverview()
        check(overview.projectCount == 2L && overview.subscriptionCount == 2L)
        check(overview.totalInvestmentCents == INVESTMENT_CENTS)
        check(overview.totalRate.compareTo(BigDecimal("16.56")) == 0)
        seeded
    }

    private fun projectEvent(number: Int, project: String, direction: MoneyDirection, cents: Long, day: LocalDate,
        role: ProjectMoneyEventRole) = ProjectMoneyEventEntity(id(number), project, direction, cents, day,
        role = role, operationId = id(number + 100))

    private fun renewal(number: Int, subscription: String, day: LocalDate, cents: Long): SubscriptionRenewalEntity {
        val end = day.plusMonths(1)
        return SubscriptionRenewalEntity(id(number), subscription, day, day, end, end, cents,
            BillingCycle.MONTHLY, BillingCycle.MONTHLY.standardDays, id(number + 100))
    }

    private fun registerCover(generation: ManagedGeneration, assetId: String, width: Int, height: Int,
        landscape: Boolean): LocalMediaAssetEntity {
        val relativePath = "media/$assetId.png"
        val file = File(generation.directory, relativePath)
        check(file.absoluteFile == file.canonicalFile && file.parentFile == File(generation.directory, "media"))
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bytes = try {
            drawCover(Canvas(bitmap), landscape)
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally { bitmap.recycle() }
        check(file.createNewFile())
        FileOutputStream(file).use { output -> output.write(bytes); output.fd.sync() }
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        return LocalMediaAssetEntity(assetId, relativePath, "image/png", width, height, bytes.size.toLong(), sha)
    }

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

    private fun id(number: Int) = "484f4d45-0000-4000-8000-${number.toString().padStart(12, '0')}"
}
