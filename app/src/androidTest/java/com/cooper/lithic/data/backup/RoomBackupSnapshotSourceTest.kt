package com.cooper.lithic.data.backup

import android.content.Context
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import com.cooper.lithic.data.settings.SettingsStorageException
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic data only. Device execution requires separate authorization. */
@RunWith(AndroidJUnit4::class)
class RoomBackupSnapshotSourceTest {
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.noBackupFilesDir, "backup-snapshot-test-${UUID.randomUUID()}")
        check(root.mkdirs())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
    }

    @After
    fun tearDown() = runBlocking {
        generation.close()
        check(root.deleteRecursively())
    }

    @Test
    fun captures_all_seven_tables_and_persisted_zone_without_derived_costs() = runBlocking {
        val at = Instant.parse("2026-01-01T03:04:05.123456Z")
        val day = LocalDate.parse("2026-01-01")
        val category = CategoryEntity(uuid(), CategoryType.PROJECT, "合成类别", "合成类别", false)
        val media = LocalMediaAssetEntity(uuid(), "media/fixture.webp", "image/webp", 10, 12, 17, "a".repeat(64))
        val project = LongTermProjectEntity(uuid(), "合成项目", category.id, day,
            endedOn = day.plusDays(12), endReason = "OTHER", coverAssetId = media.id,
            note = "原备注", createdAt = at, updatedAt = at.plusSeconds(1))
        val event = ProjectMoneyEventEntity(uuid(), project.id, MoneyDirection.OUTLAY, 12345,
            day, "合成投入", ProjectMoneyEventRole.INITIAL, uuid())
        val subscription = SubscriptionEntity(uuid(), "合成服务", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            media.id, "服务备注", at, at)
        val renewal = SubscriptionRenewalEntity(uuid(), subscription.id, day, day,
            day.plusMonths(1), day.plusMonths(1), 15000, BillingCycle.MONTHLY, 30, uuid())
        val state = SubscriptionStateChangeEntity(uuid(), subscription.id, day.plusDays(3),
            SubscriptionOperationState.PAUSED, "暂停原因", "状态备注", uuid())
        generation.coordinator.withWrite {
            generation.database.withTransaction {
                generation.database.categoryDao().insert(category)
                generation.database.mediaDao().insert(media)
                generation.database.projectDao().insertProject(project)
                generation.database.projectDao().insertEvent(event)
                generation.database.subscriptionDao().insertSubscription(subscription)
                generation.database.subscriptionDao().insertRenewal(renewal)
                generation.database.subscriptionDao().insertStateChange(state)
            }
        }
        val expected = BackupDtoMapper.fromRows(generation.database.categoryDao().list(),
            listOf(project), listOf(event), listOf(media), listOf(subscription), listOf(renewal), listOf(state))
        val snapshot = generation.backupSnapshotSource().capture()
        assertEquals(expected, snapshot.data)
        assertEquals("Asia/Shanghai", snapshot.settings.recordZoneId)
        val later = project.copy(id = uuid(), name = "后来写入的项目", coverAssetId = null)
        generation.coordinator.withWrite { generation.database.projectDao().insertProject(later) }
        // A delivered snapshot must not be a live database view or reuse a mutable row collection.
        assertEquals(expected, snapshot.data)
        assertEquals(2, generation.backupSnapshotSource().capture().data.projects.size)
    }

    @Test
    fun room_capture_waits_for_coordinated_transaction() = runBlocking {
        val canFinish = CompletableDeferred<Unit>()
        val first = CategoryEntity(uuid(), CategoryType.PROJECT, "先写", "先写", false)
        val second = CategoryEntity(uuid(), CategoryType.PROJECT, "后写", "后写", false)
        val write = async(start = CoroutineStart.UNDISPATCHED) {
            generation.coordinator.withWrite {
                generation.database.withTransaction {
                    generation.database.categoryDao().insert(first)
                    canFinish.await()
                    generation.database.categoryDao().insert(second)
                }
            }
        }
        // Room may schedule the transaction before reaching canFinish. The coordinator is already held.
        val capture = async(start = CoroutineStart.UNDISPATCHED) { generation.backupSnapshotSource().capture() }
        assertFalse(capture.isCompleted)
        canFinish.complete(Unit)
        write.await()
        assertEquals(4, capture.await().data.categories.size)
    }

    @Test
    fun more_than_one_subscription_id_chunk_preserves_all_payments_and_states() = runBlocking {
        val at = Instant.parse("2026-01-01T00:00:00Z")
        val day = LocalDate.parse("2026-01-01")
        val subscriptions = (0..400).map { index ->
            SubscriptionEntity(uuid(), "合成服务$index", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
                createdAt = at, updatedAt = at)
        }
        val renewals = subscriptions.map {
            SubscriptionRenewalEntity(uuid(), it.id, day, day, day.plusMonths(1), day.plusMonths(1),
                15000, BillingCycle.MONTHLY, 30, uuid())
        }
        val states = subscriptions.map {
            SubscriptionStateChangeEntity(uuid(), it.id, day.plusDays(3), SubscriptionOperationState.PAUSED,
                operationId = uuid())
        }
        generation.coordinator.withWrite {
            generation.database.withTransaction {
                val dao = generation.database.subscriptionDao()
                subscriptions.forEach { dao.insertSubscription(it) }
                renewals.forEach { dao.insertRenewal(it) }
                states.forEach { dao.insertStateChange(it) }
            }
        }
        assertEquals(
            BackupDtoMapper.fromRows(generation.database.categoryDao().list(), emptyList(), emptyList(),
                emptyList(), subscriptions, renewals, states),
            generation.backupSnapshotSource().capture().data,
        )
    }

    @Test
    fun capture_after_generation_close_fails_instead_of_exporting_empty_defaults() = runBlocking {
        val source = generation.backupSnapshotSource()
        generation.close()
        try {
            source.capture()
            fail("Closed generation cannot be captured")
        } catch (_: SettingsStorageException) {
            // Settings ownership is validated before any Room transaction can reopen a closed database.
        }
    }

    @Test
    fun settings_backup_rejects_an_unrelated_coordinator() = runBlocking {
        val unrelated = LocalWriteCoordinator()
        try {
            unrelated.withSnapshot { generation.settings.backupRecordZoneWithinSnapshot(unrelated) }
            fail("Backup must share the generation's coordinator")
        } catch (_: SettingsStorageException) {
            assertEquals(ZoneId.of("Asia/Shanghai"), generation.settings.recordZone())
        }
    }

    private fun uuid() = UUID.randomUUID().toString()
}
