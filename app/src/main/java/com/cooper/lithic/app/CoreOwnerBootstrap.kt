package com.cooper.lithic.app

import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean

/** One lazy application owner; background-only startup never resolves it. */
internal class CoreOwnerBootstrap<T : Any>(
    createOwner: () -> T,
    private val startOwner: (T, ZoneId) -> Unit,
    private val uiStartAction: () -> Unit,
    private val onUiActionFailure: (Exception) -> Unit = {},
) {
    private val ownerDelegate = lazy(createOwner)
    private val uiActionAttempted = AtomicBoolean(false)
    val ownerInitialized: Boolean get() = ownerDelegate.isInitialized()

    // System-started legacy work must not reissue cancellation on every cold process.
    fun coldStart() = Unit
    fun owner(): T = ownerDelegate.value
    fun ownerProvider(): () -> T = { owner() }

    /** The existing owner is responsible for idempotent startup and failure reporting. */
    fun startForUi(zone: ZoneId): T = owner().also {
        startOwner(it, zone)
        if (uiActionAttempted.compareAndSet(false, true)) {
            try { uiStartAction() }
            catch (failure: Exception) {
                // Cancellation is best effort; a synchronous error has an unknown outcome.
                // Neither the optional diagnostic nor cleanup may prevent ledger startup.
                try { onUiActionFailure(failure) } catch (_: Exception) { }
            }
        }
    }
}
