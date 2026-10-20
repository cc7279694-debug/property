package com.cooper.lithic.app

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import java.io.File
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LedgerGenerationRefreshTest {
    @Test fun old_generation_retry_completion_cannot_publish_its_repository_after_a_new_generation_is_ready() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.noBackupFilesDir, "m12-ui-refresh-${UUID.randomUUID()}")
        assertTrue(directory.mkdir())
        val oldStore = DataGenerationStore(context, File(directory, "old").also { assertTrue(it.mkdir()) })
        val newStore = DataGenerationStore(context, File(directory, "new").also { assertTrue(it.mkdir()) })
        val viewModels = ViewModelStore()
        val opened = mutableListOf<ManagedGeneration>()
        val releaseOldRead = CompletableDeferred<Unit>()
        try {
            val oldGeneration = oldStore.open(ZoneId.of("Asia/Shanghai")).also(opened::add)
            val newGeneration = newStore.open(ZoneId.of("Asia/Shanghai")).also(opened::add)
            val storage = MutableStateFlow<StorageInitializationState>(StorageInitializationState.Ready(oldGeneration))
            val holder = withContext(Dispatchers.Main) {
                LedgerUiStateHolder(storage).also { viewModels.put("ledger", it) }
            }
            withTimeout(10_000) { holder.state.first { it is LedgerUiState.Ready && it.generationId == oldGeneration.id } }
            val oldReadBlocked = CompletableDeferred<Unit>()
            val blocker = launch(Dispatchers.IO) {
                oldGeneration.coordinator.withSnapshot { oldReadBlocked.complete(Unit); releaseOldRead.await() }
            }
            oldReadBlocked.await()
            // This explicitly requested read is outside collectLatest and must be fenced on completion.
            val retry = withContext(Dispatchers.Main) { checkNotNull(holder.retryRead()) }
            storage.value = StorageInitializationState.Ready(newGeneration)
            val newlyReady = withTimeout(10_000) {
                holder.state.first { it is LedgerUiState.Ready && it.generationId == newGeneration.id }
            } as LedgerUiState.Ready
            releaseOldRead.complete(Unit)
            blocker.join()
            retry.join()
            val observed = holder.state.value as LedgerUiState.Ready
            assertEquals(newGeneration.id, observed.generationId)
            assertSame(newlyReady.repository, observed.repository)
        } finally {
            releaseOldRead.complete(Unit)
            withContext(Dispatchers.Main) { viewModels.clear() }
            opened.asReversed().forEach { it.close() }
            assertEquals(context.noBackupFilesDir.canonicalFile, directory.canonicalFile.parentFile)
            assertTrue(directory.deleteRecursively())
        }
    }
}
