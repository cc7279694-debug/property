package com.cooper.lithic.app

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import java.io.File
import java.io.IOException
import java.time.ZoneId
import java.util.Collections
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppContainerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var root: File
    private val containers = mutableListOf<AppContainer>()
    private val scopes = mutableListOf<CoroutineScope>()
    private val handles = Collections.synchronizedList(mutableListOf<ManagedGeneration>())

    @Before fun setUp() {
        root = File(context.noBackupFilesDir, "app-startup-test-${UUID.randomUUID()}")
        check(root.mkdirs())
    }

    @After fun tearDown() = runBlocking {
        scopes.forEach { it.cancel() }
        containers.forEach { it.close() }
        handles.toList().asReversed().forEach { it.close() }
        check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
        check(root.deleteRecursively())
    }

    @Test fun start_initializes_complete_storage_without_an_explicit_read() = runBlocking {
        val calls = AtomicInteger()
        val container = container { calls.incrementAndGet() }
        container.start(ZoneId.of("Asia/Shanghai"))
        val state = awaitSettled(container)
        assertTrue("Starting the application must initialize storage without openStorage()", state is StorageInitializationState.Ready)
        val generation = (state as StorageInitializationState.Ready).generation
        assertEquals(1, calls.get())
        assertTrue(File(generation.directory, "data.db").isFile)
        assertTrue(File(generation.directory, "settings.preferences_pb").isFile)
        assertTrue(File(generation.directory, "ready.json").isFile)
        assertEquals(2, generation.database.categoryDao().list().size)
        assertEquals(ZoneId.of("Asia/Shanghai"), generation.settings.recordZone())
        assertEquals(generation.id, JSONObject(File(root, "active-generation").readText()).getString("activeId"))
    }

    @Test fun initialization_failure_is_retained_without_new_generation_or_retry() = runBlocking {
        val calls = AtomicInteger()
        val container = container {
            calls.incrementAndGet()
            throw IOException("Synthetic application startup failure")
        }
        container.start(ZoneId.of("Asia/Shanghai"))
        val state = awaitSettled(container)
        assertTrue("Initialization failure must remain an explicit Failed state", state is StorageInitializationState.Failed)
        val failure = (state as StorageInitializationState.Failed).reason
        assertTrue(failure.hasCauseMessage("Synthetic application startup failure"))
        repeat(2) {
            val rejected = try { container.openStorage(); null } catch (error: Exception) { error }
            assertTrue(rejected?.hasCauseMessage("Synthetic application startup failure") == true)
        }
        container.start(ZoneId.of("America/New_York"))
        delay(50)
        assertSame(failure, (container.storageState.value as StorageInitializationState.Failed).reason)
        assertEquals(1, calls.get())
        assertFalse(File(root, "active-generation").exists())
        val partial = File(root, "generations").listFiles()!!.single()
        assertTrue(File(partial, "data.db").isFile)
        assertFalse(File(partial, "ready.json").exists())
    }

    @Test fun concurrent_readers_wait_for_one_background_start_and_captured_zone() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val calls = AtomicInteger()
        val container = container {
            calls.incrementAndGet()
            assertNotEquals(Looper.getMainLooper(), Looper.myLooper())
            entered.complete(Unit)
            release.await()
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync { container.start(ZoneId.of("Asia/Shanghai")) }
        try {
            assertTrue("start() must launch background work", withTimeoutOrNull(10_000) { entered.await(); true } == true)
            assertTrue(container.storageState.value is StorageInitializationState.Initializing)
            assertFalse(File(root, "active-generation").exists())
            container.start(ZoneId.of("America/New_York"))
            val waiting = (1..8).map { async(Dispatchers.IO) { container.openStorage() } }
            delay(50)
            assertTrue(waiting.none { it.isCompleted })
            release.complete(Unit)
            val opened = waiting.awaitAll()
            opened.forEach { assertSame(opened.first(), it) }
            assertEquals(1, calls.get())
            assertEquals(1, File(root, "generations").listFiles()!!.size)
            assertEquals(ZoneId.of("Asia/Shanghai"), opened.first().settings.recordZone())
            assertSame(opened.first(), (container.storageState.value as StorageInitializationState.Ready).generation)
        } finally { release.complete(Unit) }
    }

    @Test fun cancelled_initialization_propagates_cancellation_and_never_reports_ready() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it }
        val container = container(scope) { entered.complete(Unit); release.await() }
        container.start(ZoneId.of("Asia/Shanghai"))
        try {
            assertTrue(withTimeoutOrNull(10_000) { entered.await(); true } == true)
            scope.cancel(CancellationException("Synthetic startup cancellation"))
            var cancelled = false
            try { container.openStorage() } catch (_: CancellationException) { cancelled = true }
            assertTrue(cancelled)
            assertFalse(container.storageState.value is StorageInitializationState.Ready)
            assertFalse(container.storageState.value is StorageInitializationState.Failed)
            assertFalse(File(root, "active-generation").exists())
        } finally { release.complete(Unit) }
    }

    @Test fun open_before_start_does_not_implicitly_create_storage() = runBlocking {
        val container = container()
        var rejected = false
        try { container.openStorage() } catch (_: IllegalStateException) { rejected = true }
        assertTrue("An unstarted container must not silently choose a later device zone", rejected)
        assertFalse(File(root, "active-generation").exists())
        assertFalse(File(root, "generations").exists())
    }

    @Test fun actual_application_onCreate_starts_its_private_default_storage() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<LithicApplication>()
        val state = awaitSettled(application.appContainer)
        assertTrue("Application.onCreate must schedule startup before any UI or repository read", state is StorageInitializationState.Ready)
        val generation = (state as StorageInitializationState.Ready).generation
        assertEquals(File(context.noBackupFilesDir, "generations/${generation.id}").canonicalFile, generation.directory)
        assertEquals(generation.id, JSONObject(File(context.noBackupFilesDir, "active-generation").readText()).getString("activeId"))
        assertTrue(File(generation.directory, "ready.json").isFile)
        assertEquals(2, generation.database.categoryDao().list().size)
        assertSame(generation, application.appContainer.openStorage())
    }

    private fun container(
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { scopes += it },
        initialize: suspend (ManagedGeneration) -> Unit = {},
    ): AppContainer {
        val generations = DataGenerationStore(context, root, initialize = { generation ->
            handles += generation
            initialize(generation)
        })
        return AppContainer(generations, scope).also { containers += it }
    }

    private suspend fun awaitSettled(container: AppContainer): StorageInitializationState? =
        withTimeoutOrNull(10_000) { container.storageState.first { it !is StorageInitializationState.Initializing } }

    private fun Throwable.hasCauseMessage(message: String): Boolean =
        generateSequence(this) { current -> current.cause.takeIf { it !== current } }.any { it.message == message }
}
