package com.cooper.lithic.app

import com.cooper.lithic.core.time.BusinessClock
import java.time.Instant
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Foreground-only observer; no business facts are changed by a calendar boundary. */
internal class BusinessDateObserver(initialDate: LocalDate) {
    private var observedDate = initialDate
    suspend fun observe(
        clock: BusinessClock,
        zone: ZoneId,
        onChange: () -> Unit,
        now: () -> Instant = Instant::now,
        wait: suspend (Long) -> Unit = { delay(it) },
    ) {
        while (true) {
            currentCoroutineContext().ensureActive()
            val today = clock.today()
            if (today != observedDate) {
                observedDate = today
                onChange()
            }
            wait(nextCheckMillis(now(), zone))
        }
    }

    companion object {
        fun nextCheckMillis(now: Instant, zone: ZoneId): Long {
            val midnight = now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
            // Wake at recorded-zone midnight; bounded polling also notices foreground clock changes.
            return Duration.between(now, midnight).toMillis().coerceIn(1L, 60_000L)
        }
    }
}
