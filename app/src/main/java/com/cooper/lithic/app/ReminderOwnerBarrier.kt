package com.cooper.lithic.app

import com.cooper.lithic.platform.notification.EnqueueLease
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A capability belongs to one owner instance and one publication, not a generation ID. */
internal class ReminderOwnerLease<O : Any> internal constructor(
    val owner: O,
    private val barrier: ReminderOwnerBarrier<O>,
) : EnqueueLease {
    override fun isActive(): Boolean = barrier.isActive(this)

    /** The non-suspending block is only the bounded synchronous enqueue submission.
     * Operation completion must be observed outside the barrier. */
    override fun <T : Any> withActive(block: () -> T): T? = barrier.withActive(this, block)
}

/** Separate reminder admission fence; it does not replace storage/restore ownership. */
internal class ReminderOwnerBarrier<O : Any> {
    private val lock = Any()
    @Volatile private var closed = false
    private val switchesInProgress = AtomicInteger(0)
    private val mutableCurrent = MutableStateFlow<ReminderOwnerLease<O>?>(null)
    val current: StateFlow<ReminderOwnerLease<O>?> = mutableCurrent.asStateFlow()

    fun activate(owner: O): ReminderOwnerLease<O>? = synchronized(lock) {
        if (closed || switchesInProgress.get() != 0) return@synchronized null
        ReminderOwnerLease(owner, this).also { mutableCurrent.value = it }
    }

    /** Fence old admissions before waiting for any already-admitted submission. */
    fun revokeForSwitch() {
        switchesInProgress.incrementAndGet()
        try { synchronized(lock) { mutableCurrent.value = null } }
        finally { switchesInProgress.decrementAndGet() }
    }

    /** Synchronously revoke admission without waiting on an in-flight submission. */
    fun beginClose() {
        closed = true
    }

    /** Await any already-admitted synchronous submission on a background dispatcher. */
    fun finishClose() {
        synchronized(lock) { mutableCurrent.value = null }
    }

    // Advisory observation only. Admission must still use withActive under the lock.
    fun isActive(lease: ReminderOwnerLease<O>): Boolean =
        !closed && switchesInProgress.get() == 0 && mutableCurrent.value === lease

    fun <T : Any> withActive(lease: ReminderOwnerLease<O>, block: () -> T): T? = synchronized(lock) {
        if (closed || switchesInProgress.get() != 0 || mutableCurrent.value !== lease) null else block()
    }
}
