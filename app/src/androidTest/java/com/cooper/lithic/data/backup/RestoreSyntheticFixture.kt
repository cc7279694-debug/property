package com.cooper.lithic.data.backup

import android.graphics.Bitmap
import androidx.room.withTransaction
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
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
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** All facts and pictures are synthetic; never opens the application's normal ledger. */
internal object RestoreSyntheticFixture {
    val day: LocalDate = LocalDate.parse("2026-01-01")
    val at: Instant = Instant.parse("2026-01-01T01:02:03.123456789Z")

    suspend fun insert(generation: ManagedGeneration): Map<String, ByteArray> {
        val png = picture(Bitmap.CompressFormat.PNG, 2, 3)
        val jpeg = picture(Bitmap.CompressFormat.JPEG, 3, 2)
        val unused = picture(Bitmap.CompressFormat.PNG, 1, 1)
        val bytes = linkedMapOf("media/nested/cover.png" to png, "media/cover.jpg" to jpeg,
            "media/unreferenced.png" to unused)
        bytes.forEach { (path, value) ->
            val file = File(generation.directory, path)
            check(file.parentFile?.isDirectory == true || file.parentFile?.mkdirs() == true)
            file.writeBytes(value)
        }
        generation.coordinator.withWrite {
            generation.database.withTransaction {
                val database = generation.database
                val projectCategory = CategoryEntity(id(), CategoryType.PROJECT, "合成设备", "合成设备", false)
                val subscriptionCategory = CategoryEntity(id(), CategoryType.SUBSCRIPTION, "合成服务", "合成服务", false)
                database.categoryDao().insert(projectCategory)
                database.categoryDao().insert(subscriptionCategory)
                val cover = LocalMediaAssetEntity(id(), "media/nested/cover.png", "image/png", 2, 3, png.size.toLong(), sha(png))
                val subCover = LocalMediaAssetEntity(id(), "media/cover.jpg", "image/jpeg", 3, 2, jpeg.size.toLong(), sha(jpeg))
                database.mediaDao().insert(cover)
                database.mediaDao().insert(subCover)
                database.mediaDao().insert(LocalMediaAssetEntity(id(), "media/unreferenced.png", "image/png", 1, 1, unused.size.toLong(), sha(unused)))
                val project = LongTermProjectEntity(id(), "合成长存项目", projectCategory.id, day,
                    coverAssetId = cover.id, note = "原事实\n保留", createdAt = at, updatedAt = at.plusSeconds(1))
                val ended = LongTermProjectEntity(id(), "合成已结束项目", LithicDatabase.PROJECT_FALLBACK_ID,
                    day, day.plusDays(9), "合成结束原因", note = "结束后仍有真实结算", createdAt = at, updatedAt = at.plusSeconds(2))
                database.projectDao().insertProject(project)
                database.projectDao().insertProject(ended)
                database.projectDao().insertEvent(ProjectMoneyEventEntity(id(), project.id, MoneyDirection.OUTLAY, 12345, day, "原始取得", ProjectMoneyEventRole.INITIAL, id()))
                database.projectDao().insertEvent(ProjectMoneyEventEntity(id(), project.id, MoneyDirection.RECOVERY, 2345, day.plusDays(1), "原始收回", ProjectMoneyEventRole.REGULAR, id()))
                database.projectDao().insertEvent(ProjectMoneyEventEntity(id(), ended.id, MoneyDirection.OUTLAY, 6000, day, null, ProjectMoneyEventRole.INITIAL, id()))
                database.projectDao().insertEvent(ProjectMoneyEventEntity(id(), ended.id, MoneyDirection.RECOVERY, 1000, day.plusDays(20), "真实后续结算", ProjectMoneyEventRole.END_SETTLEMENT, id()))
                val subscription = SubscriptionEntity(id(), "合成固定订阅", subscriptionCategory.id,
                    subCover.id, "不产生自动付款", at, at.plusSeconds(3))
                database.subscriptionDao().insertSubscription(subscription)
                database.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(id(), subscription.id,
                    day, day, day.plusMonths(1), day.plusMonths(1), 15000, BillingCycle.MONTHLY, 30, id()))
                database.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(id(), subscription.id,
                    day.plusDays(19), day.plusMonths(1), day.plusMonths(2), day.plusMonths(2), 18000, BillingCycle.MONTHLY, 30, id()))
                database.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(), subscription.id,
                    day.plusDays(4), SubscriptionOperationState.PAUSED, "合成暂停", "保留操作身份", id()))
                database.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(), subscription.id,
                    day.plusDays(6), SubscriptionOperationState.ENABLED, null, "合成恢复", id()))
                database.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(), subscription.id,
                    day.plusMonths(2), SubscriptionOperationState.STOPPED, "合成停止", null, id()))
            }
        }
        return bytes
    }

    fun target(file: File): BackupTarget = object : BackupTarget {
        override fun openWrite(): OutputStream = FileOutputStream(file)
        override fun openRead(): InputStream = FileInputStream(file)
        override fun deletePartial(): Boolean = !file.exists() || file.delete()
    }

    fun sha(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun id(): String = UUID.randomUUID().toString()
    private fun picture(format: Bitmap.CompressFormat, width: Int, height: Int): ByteArray =
        ByteArrayOutputStream().also { output ->
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(android.graphics.Color.rgb(43, 71, 89))
                check(bitmap.compress(format, 91, output))
            } finally { bitmap.recycle() }
        }.toByteArray()
}
