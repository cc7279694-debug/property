package com.cooper.lithic.app

import com.cooper.lithic.platform.notification.LegacyCancellationOperation
import com.cooper.lithic.platform.notification.LegacyCancellationOutcome
import com.cooper.lithic.platform.notification.LegacyReminderCancellationRunner
import com.cooper.lithic.platform.notification.LegacyReminderTarget
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Host tests of the production bootstrap protocol; Android lifecycle execution remains NOT_RUN. */
class CoreOwnerBootstrapTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test fun cold_start_neither_cancels_work_nor_constructs_or_starts_an_owner() {
        var cleanups = 0
        val bootstrap = CoreOwnerBootstrap(
            createOwner = { error("cold start must not create owner") },
            startOwner = { _: Any, _: ZoneId -> error("cold start must not start owner") },
            uiStartAction = { cleanups++ },
        )

        bootstrap.coldStart()

        assertEquals(0, cleanups)
        assertFalse(bootstrap.ownerInitialized)
    }

    @Test fun obtaining_a_worker_provider_does_not_resolve_the_owner() {
        var creates = 0
        val owner = Any()
        val bootstrap = CoreOwnerBootstrap(
            createOwner = { creates++; owner },
            startOwner = { _: Any, _: ZoneId -> error("provider must not start owner") },
            uiStartAction = {},
        )

        val workerProvider = bootstrap.ownerProvider()
        assertFalse(bootstrap.ownerInitialized)
        assertEquals(0, creates)
        assertSame(owner, workerProvider())
        assertTrue(bootstrap.ownerInitialized)
        assertEquals(1, creates)
    }

    @Test fun repeated_ui_entry_starts_the_same_owner_with_the_requested_zone() {
        var creates = 0
        val owner = Any()
        val starts = mutableListOf<Pair<Any, ZoneId>>()
        val bootstrap = CoreOwnerBootstrap(
            createOwner = { creates++; owner },
            startOwner = { instance, observedZone -> starts += instance to observedZone },
            uiStartAction = {},
        )

        assertSame(owner, bootstrap.startForUi(zone))
        assertSame(owner, bootstrap.startForUi(zone))
        assertSame(owner, bootstrap.ownerProvider()())
        assertEquals(1, creates)
        assertEquals(listOf(owner to zone, owner to zone), starts)
    }

    @Test fun concurrent_ui_entry_does_not_create_multiple_owners() {
        val creates = AtomicInteger()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        val owner = Any()
        val bootstrap = CoreOwnerBootstrap(
            createOwner = {
                creates.incrementAndGet()
                started.countDown()
                check(release.await(5, TimeUnit.SECONDS))
                owner
            },
            startOwner = { _: Any, _: ZoneId -> },
            uiStartAction = {},
        )
        try {
            val first = pool.submit<Any> { bootstrap.startForUi(zone) }
            assertTrue(started.await(5, TimeUnit.SECONDS))
            val second = pool.submit<Any> { bootstrap.startForUi(zone) }
            release.countDown()
            assertSame(owner, first.get(5, TimeUnit.SECONDS))
            assertSame(owner, second.get(5, TimeUnit.SECONDS))
            assertEquals(1, creates.get())
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    @Test fun ui_start_failure_is_propagated_without_creating_another_owner() {
        var creates = 0
        val owner = Any()
        val failure = IllegalStateException("synthetic startup failure")
        val bootstrap = CoreOwnerBootstrap(
            createOwner = { creates++; owner },
            startOwner = { _: Any, _: ZoneId -> throw failure },
            uiStartAction = {},
        )

        try {
            bootstrap.startForUi(zone)
            throw AssertionError("startup failure must not be swallowed")
        } catch (caught: IllegalStateException) {
            assertSame(failure, caught)
        }
        assertSame(owner, bootstrap.owner())
        assertEquals(1, creates)
    }

    @Test fun cancellation_start_failure_is_reported_without_blocking_the_owner() {
        val owner = Any()
        val failure = IllegalStateException("synthetic cancellation failure")
        var reported: Exception? = null
        val bootstrap = CoreOwnerBootstrap(
            createOwner = { owner },
            startOwner = { _: Any, _: ZoneId -> },
            uiStartAction = { throw failure },
            onUiActionFailure = { reported = it },
        )

        assertSame(owner, bootstrap.startForUi(zone))
        assertSame(failure, reported)
    }

    @Test fun cancellation_start_failure_is_not_retried_on_later_ui_entry() {
        val owner = Any()
        var attempts = 0
        var unknownReports = 0
        val bootstrap = CoreOwnerBootstrap(
            createOwner = { owner },
            startOwner = { _: Any, _: ZoneId -> },
            uiStartAction = { attempts++; throw IllegalStateException("synthetic uncertain cancellation") },
            onUiActionFailure = { unknownReports++ },
        )

        assertSame(owner, bootstrap.startForUi(zone))
        assertSame(owner, bootstrap.startForUi(zone))
        assertEquals(1, attempts)
        assertEquals(1, unknownReports)
    }

    @Test fun repeated_ui_entry_starts_production_cancellation_runner_only_once() {
        val submitted = mutableListOf<LegacyReminderTarget>()
        val runner = LegacyReminderCancellationRunner(
            dispatch = { it() },
            cancel = { target ->
                submitted += target
                LegacyCancellationOperation { callback -> callback(LegacyCancellationOutcome.CONFIRMED) }
            },
            record = { _, _ -> },
        )
        val bootstrap = CoreOwnerBootstrap(
            createOwner = { Any() },
            startOwner = { _: Any, _: ZoneId -> },
            uiStartAction = runner::start,
        )

        bootstrap.coldStart()
        assertTrue(submitted.isEmpty())
        bootstrap.startForUi(zone)
        bootstrap.startForUi(zone)
        assertEquals(LegacyReminderCancellationRunner.TARGETS, submitted)
    }
}
