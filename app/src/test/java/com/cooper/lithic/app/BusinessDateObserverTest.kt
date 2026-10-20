package com.cooper.lithic.app

import com.cooper.lithic.core.time.BusinessClock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BusinessDateObserverTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    @Test fun foreground_midnight_refreshes_once_and_same_day_does_not_repeat() = runBlocking {
        var date = LocalDate.of(2026, 10, 20)
        var ticks = 0
        var refreshes = 0
        val observer = BusinessDateObserver(date)
        try {
            observer.observe(BusinessClock { date }, zone, { refreshes++ }, wait = {
                ticks++
                if (ticks == 1) date = date.plusDays(1)
                if (ticks == 4) throw CancellationException("test stop")
            })
        } catch (_: CancellationException) { }
        assertEquals(1, refreshes)
    }
    @Test fun resume_after_background_rechecks_before_first_wait() = runBlocking {
        var calls = 0
        val old = LocalDate.of(2026, 10, 20)
        val observer = BusinessDateObserver(old)
        try {
            observer.observe(BusinessClock { old.plusDays(2) }, zone, { calls++ }, wait = {
                assertEquals(1, calls)
                throw CancellationException("test stop")
            })
        } catch (_: CancellationException) { }
        assertEquals(1, calls)
    }
    @Test fun scheduling_uses_record_zone_midnight_and_caps_clock_change_detection() {
        val nearMidnight = Instant.parse("2026-10-20T15:59:59.250Z")
        assertEquals(750L, BusinessDateObserver.nextCheckMillis(nearMidnight, zone))
        assertEquals(60_000L, BusinessDateObserver.nextCheckMillis(nearMidnight, ZoneId.of("UTC")))
    }
    @Test fun scheduling_handles_dst_without_changing_business_zone() {
        assertEquals(500L, BusinessDateObserver.nextCheckMillis(Instant.parse("2026-03-08T04:59:59.500Z"), ZoneId.of("America/New_York")))
        assertEquals(60_000L, BusinessDateObserver.nextCheckMillis(Instant.parse("2026-03-08T07:00:00Z"), ZoneId.of("America/New_York")))
    }
}
