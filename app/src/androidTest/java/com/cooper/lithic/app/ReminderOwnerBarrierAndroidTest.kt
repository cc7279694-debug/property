package com.cooper.lithic.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.data.backup.DataGenerationStore
import java.io.File
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic isolated root only. Failed or unknown fixtures are not deleted. */
@RunWith(AndroidJUnit4::class)
class ReminderOwnerBarrierAndroidTest {
    @Test fun closed_container_cannot_reuse_lease_when_same_generation_reopens() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.noBackupFilesDir, "reminder-owner-${UUID.randomUUID()}")
        check(root.mkdir())
        fun newOwner() = AppContainer(DataGenerationStore(context, root),
            CoroutineScope(SupervisorJob() + Dispatchers.IO))
        val firstContainer = newOwner()
        var reopenedContainer: AppContainer? = null
        try {
            firstContainer.start(ZoneId.of("Asia/Shanghai"))
            val firstGeneration = firstContainer.openStorage()
            val firstLease = requireNotNull(firstContainer.reminderOwnerLease.value)
            assertSame(firstGeneration, firstLease.owner)
            assertTrue(firstLease.isActive())
            firstContainer.close()
            assertFalse(firstLease.isActive())
            assertNull(firstLease.withActive { "stale" })
            assertNull(firstContainer.reminderOwnerLease.value)

            val secondContainer = newOwner().also { reopenedContainer = it }
            secondContainer.start(ZoneId.of("Asia/Shanghai"))
            val secondGeneration = secondContainer.openStorage()
            val secondLease = requireNotNull(secondContainer.reminderOwnerLease.value)
            assertEquals(firstGeneration.id, secondGeneration.id)
            assertNotSame(firstGeneration, secondGeneration)
            assertNotSame(firstLease, secondLease)
            assertSame(secondGeneration, secondLease.owner)
            assertTrue(secondLease.isActive())
            assertNull(firstLease.withActive { "stale after reopen" })
        } finally {
            reopenedContainer?.close()
            firstContainer.close()
        }
    }

    @Test fun close_during_restore_cannot_reauthorize_restored_owner() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.noBackupFilesDir, "reminder-restore-owner-${UUID.randomUUID()}")
        check(root.mkdir())
        val entered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val release = kotlinx.coroutines.CompletableDeferred<Unit>()
        val store = DataGenerationStore(context, root, restoreProbe = { stage ->
            if (stage == "BEFORE_SWITCH") { entered.complete(Unit); release.await() }
        })
        val container = AppContainer(store, CoroutineScope(SupervisorJob() + Dispatchers.IO))
        try {
            container.start(ZoneId.of("UTC"))
            val original = container.openStorage()
            val originalLease = requireNotNull(container.reminderOwnerLease.value)
            val expected = original.backupSnapshotSource().capture()
            val candidate = container.prepareRestoreGeneration(ZoneId.of("Asia/Shanghai")) {
                it.coordinator.withWrite { it.database.initializeDefaults() }
            }
            val activating = async(Dispatchers.IO) {
                container.activateRestoreGeneration(original.id, candidate, expected)
            }
            withTimeout(10_000) { entered.await() }
            assertFalse(originalLease.isActive())
            assertNull(originalLease.withActive { "stale during restore" })
            // Enter close synchronously before releasing the non-cancellable restore.
            val closing = async(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) { container.close() }
            release.complete(Unit)
            val restored = withTimeout(10_000) { activating.await() }
            withTimeout(10_000) { closing.await() }
            assertFalse(restored.database.isOpen)
            assertNull(container.reminderOwnerLease.value)
            assertFalse(originalLease.isActive())
        } finally {
            release.complete(Unit)
            container.close()
        }
    }
}
