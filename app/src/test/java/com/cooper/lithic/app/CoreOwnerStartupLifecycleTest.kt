package com.cooper.lithic.app

import com.cooper.lithic.data.backup.DataGenerationStore
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.jvm.functions.Function0
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercises the real owner lifecycle with a deliberately failing, non-I/O generation factory. */
class CoreOwnerStartupLifecycleTest {
    @Test fun repeated_start_does_not_open_a_second_owner_and_failure_is_not_hidden() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val attempts = AtomicInteger()
        val failure = IllegalStateException("synthetic generation open failure")
        val factory: () -> DataGenerationStore = {
            attempts.incrementAndGet()
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            throw failure
        }
        val constructor = AppContainer::class.java.getDeclaredConstructor(
            Function0::class.java, CoroutineScope::class.java,
        ).apply { isAccessible = true }
        val owner = constructor.newInstance(factory, CoroutineScope(SupervisorJob() + Dispatchers.IO))
        try {
            owner.start(ZoneId.of("Asia/Shanghai"))
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            owner.start(ZoneId.of("Asia/Shanghai"))
            assertEquals(1, attempts.get())
            release.countDown()
            val state = withTimeout(5_000) { owner.storageState.first { it is StorageInitializationState.Failed } }
            assertSame(failure, (state as StorageInitializationState.Failed).reason)
            assertEquals(1, attempts.get())
        } finally {
            release.countDown()
            owner.close()
        }
    }
}
