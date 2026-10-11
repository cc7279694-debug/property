package com.cooper.lithic.core.model

import java.math.BigDecimal
import java.time.LocalDate

enum class MoneyDirection { OUTLAY, RECOVERY }

data class LongTermProject(val startedOn: LocalDate, val endedOn: LocalDate? = null) {
    init {
        require(endedOn == null || !endedOn.isBefore(startedOn)) { "The end date cannot precede the start date" }
    }
}

data class ProjectMoneyEvent(val direction: MoneyDirection, val amount: Money, val occurredOn: LocalDate)

data class ProjectSnapshot(val project: LongTermProject, val moneyEvents: List<ProjectMoneyEvent>)

enum class CostUnavailableReason { PROJECT_NOT_STARTED }

data class ProjectCostResult(
    val heldDays: Long,
    val grossOutlayCents: Long,
    val recoveryCents: Long,
    val netCents: Long,
    /** Yuan per natural day, with DECIMAL128 precision; display rounding belongs to the UI. */
    val dailyRate: BigDecimal?,
    val contributesToCurrentTotal: Boolean,
    val unavailableReason: CostUnavailableReason?,
)
