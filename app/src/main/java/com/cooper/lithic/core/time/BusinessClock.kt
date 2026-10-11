package com.cooper.lithic.core.time

import java.time.Clock
import java.time.LocalDate

fun interface BusinessClock {
    fun today(): LocalDate
}

class FixedBusinessClock(private val date: LocalDate) : BusinessClock {
    override fun today(): LocalDate = date
}

/** The injected Clock carries the business zone; this class never chooses the device zone. */
class ZonedBusinessClock(private val clock: Clock) : BusinessClock {
    override fun today(): LocalDate = LocalDate.now(clock)
}
