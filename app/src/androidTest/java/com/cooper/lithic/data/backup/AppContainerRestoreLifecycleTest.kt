package com.cooper.lithic.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.app.AppContainer
import java.io.File
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppContainerRestoreLifecycleTest {
    @Test fun close_waits_for_activation_and_releases_the_actual_new_generation_owner() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = File(context.noBackupFilesDir, "m12-owner-close-${UUID.randomUUID()}").canonicalFile
        assertTrue(root.mkdir())
        val beforeSwitch = CompletableDeferred<Unit>()
        val releaseSwitch = CompletableDeferred<Unit>()
        val store = DataGenerationStore(context, root, restoreProbe = { stage ->
            if (stage == "BEFORE_SWITCH") { beforeSwitch.complete(Unit); releaseSwitch.await() }
        })
        val owner = AppContainer(store, CoroutineScope(SupervisorJob() + Dispatchers.IO))
        var activating: Deferred<ManagedGeneration>? = null
        var closing: Deferred<Unit>? = null
        var reopened: ManagedGeneration? = null
        try {
            owner.start(ZoneId.of("UTC"))
            val old = owner.openStorage()
            val expected = old.backupSnapshotSource().capture()
            val candidate = owner.prepareRestoreGeneration(ZoneId.of("Asia/Shanghai")) {
                it.coordinator.withWrite { it.database.initializeDefaults() }
            }
            activating = async(Dispatchers.IO) { owner.activateRestoreGeneration(old.id, candidate, expected) }
            withTimeout(10_000) { beforeSwitch.await() }
            val closeStarted = CompletableDeferred<Unit>()
            val closeReturned = CompletableDeferred<Unit>()
            closing = async(Dispatchers.IO) {
                closeStarted.complete(Unit)
                owner.close()
                closeReturned.complete(Unit)
                Unit
            }
            closeStarted.await()
            val completedWhileSwitchBlocked = withTimeoutOrNull(1_000) { closeReturned.await(); true } ?: false
            assertFalse("close must wait for the active restore instead of returning while storage is Initializing",
                completedWhileSwitchBlocked)
            releaseSwitch.complete(Unit)
            val newGeneration = withTimeout(10_000) { activating.await() }
            withTimeout(10_000) { closing.await() }
            assertFalse("The restored Room owner must be closed before close returns", newGeneration.database.isOpen)
            reopened = withTimeout(10_000) { DataGenerationStore(context, root).open() }
            assertEquals(candidate.id, reopened.id)
            assertTrue("The released root must be usable by one fresh owner", reopened.database.isOpen)
        } finally {
            releaseSwitch.complete(Unit)
            try { activating?.await() } finally {
                try { closing?.await() } finally {
                    owner.close()
                    reopened?.close()
                    assertEquals(context.noBackupFilesDir.canonicalFile, root.parentFile)
                    assertTrue(root.deleteRecursively())
                }
            }
        }
    }
}
