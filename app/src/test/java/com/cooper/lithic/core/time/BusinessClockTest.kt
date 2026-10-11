package com.cooper.lithic.core.time

import com.cooper.lithic.core.model.*
import com.cooper.lithic.domain.project.ProjectCostEngine
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class BusinessClockTest {
    @Test fun fixed_business_date_drives_project_evaluation() {
        val clock: BusinessClock = FixedBusinessClock(LocalDate.of(2026, 1, 2))
        val start = LocalDate.of(2026, 1, 1)
        val result = ProjectCostEngine.evaluate(ProjectSnapshot(LongTermProject(start), listOf(
            ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(100_000), start))), clock.today())
        assertEquals(2L, result.heldDays)
        assertEquals(0, BigDecimal("500").compareTo(result.dailyRate))
    }

    @Test fun recorded_zone_uses_its_own_calendar_day() {
        val instant = Instant.parse("2026-01-01T00:30:00Z")
        val shanghai = ZonedBusinessClock(Clock.fixed(instant, ZoneId.of("Asia/Shanghai")))
        val losAngeles = ZonedBusinessClock(Clock.fixed(instant, ZoneId.of("America/Los_Angeles")))
        assertEquals(LocalDate.of(2026, 1, 1), shanghai.today())
        assertEquals(LocalDate.of(2025, 12, 31), losAngeles.today())
    }
}
