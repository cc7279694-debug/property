package com.cooper.lithic.app

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

class ReminderOwnerBarrierTest {
    @Test fun repeated_owner_identity_still_gets_a_distinct_lease() {
        val gate = ReminderOwnerBarrier<Any>()
        val owner = Any()
        val first = requireNotNull(gate.activate(owner))
        assertSame(owner, first.owner)
        assertSame(first, gate.current.value)
        gate.revokeForSwitch()
        assertFalse(first.isActive())
        assertNull(first.withActive { "stale" })
        val second = requireNotNull(gate.activate(owner))
        assertNotSame(first, second)
        assertSame(owner, second.owner)
        assertEquals("accepted", second.withActive { "accepted" })
        assertNull(first.withActive { "stale" })
    }

    @Test fun close_permanently_revokes_and_refuses_late_restore_activation() {
        val gate = ReminderOwnerBarrier<Any>()
        val first = requireNotNull(gate.activate(Any()))
        gate.beginClose()
        gate.finishClose()
        assertNull(gate.current.value)
        assertFalse(first.isActive())
        assertNull(first.withActive { "stale" })
        assertNull(gate.activate(Any()))
        gate.revokeForSwitch()
        assertNull(gate.activate(Any()))
    }

    @Test fun revoke_does_not_restore_the_previous_lease_after_failed_switch() {
        val gate = ReminderOwnerBarrier<Any>()
        val owner = Any()
        val first = requireNotNull(gate.activate(owner))
        gate.revokeForSwitch()
        assertNull(gate.current.value)
        val fallback = requireNotNull(gate.activate(owner))
        assertNotSame(first, fallback)
        assertFalse(first.isActive())
        assertTrue(fallback.isActive())
    }

    @Test fun close_fences_new_admissions_before_prior_synchronous_submission_finishes() {
        val gate = ReminderOwnerBarrier<Any>()
        val lease = requireNotNull(gate.activate(Any()))
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(3)
        try {
            val prior = executor.submit<String?> {
                lease.withActive {
                    entered.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                    "accepted before close"
                }
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val closing = executor.submit { gate.beginClose(); gate.finishClose() }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (lease.isActive() && System.nanoTime() < deadline) Thread.yield()
            assertFalse(lease.isActive())
            val late = executor.submit<String?> { lease.withActive { "must not be submitted" } }
            release.countDown()
            assertEquals("accepted before close", prior.get(5, TimeUnit.SECONDS))
            closing.get(5, TimeUnit.SECONDS)
            assertNull(late.get(5, TimeUnit.SECONDS))
            assertNull(gate.current.value)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test fun restore_switch_revokes_before_wait_and_fallback_gets_a_fresh_lease() {
        val gate = ReminderOwnerBarrier<Any>()
        val owner = Any()
        val first = requireNotNull(gate.activate(owner))
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val prior = executor.submit<String?> {
                first.withActive {
                    entered.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                    "submitted before switch"
                }
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val switching = executor.submit { gate.revokeForSwitch() }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (first.isActive() && System.nanoTime() < deadline) Thread.yield()
            assertFalse(first.isActive())
            release.countDown()
            assertEquals("submitted before switch", prior.get(5, TimeUnit.SECONDS))
            switching.get(5, TimeUnit.SECONDS)
            assertNull(gate.current.value)
            val fallback = requireNotNull(gate.activate(owner))
            assertNotSame(first, fallback)
            assertNull(first.withActive { "stale after fallback" })
            assertEquals("current", fallback.withActive { "current" })
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test fun close_must_finish_even_when_synchronous_submission_does_not_return() {
        val gate = ReminderOwnerBarrier<Any>()
        val lease = requireNotNull(gate.activate(Any()))
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val closeStarted = CountDownLatch(1)
        val closeReturned = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            executor.submit {
                lease.withActive {
                    entered.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                    Unit
                }
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            gate.beginClose()
            executor.submit {
                closeStarted.countDown()
                gate.finishClose()
                closeReturned.countDown()
            }
            assertTrue(closeStarted.await(5, TimeUnit.SECONDS))
            assertTrue("Close must not depend on a stuck enqueue", closeReturned.await(500, TimeUnit.MILLISECONDS))
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }
}
