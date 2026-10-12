package com.cooper.lithic.domain.project

import java.time.LocalDate

/** Lifecycle validation only; durations and money remain owned by ProjectCostEngine. */
object ProjectLifecycleRules {
    fun validateEnd(startedOn: LocalDate, endedOn: LocalDate, endReason: String): String? = when {
        endedOn.isBefore(startedOn) -> "The end date cannot precede ownership"
        endReason.isBlank() -> "An end reason cannot be blank"
        else -> null
    }

    fun isEnded(endedOn: LocalDate?, asOf: LocalDate): Boolean = endedOn != null && !endedOn.isAfter(asOf)
}
