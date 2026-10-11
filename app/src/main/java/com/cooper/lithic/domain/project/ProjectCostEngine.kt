package com.cooper.lithic.domain.project

import com.cooper.lithic.core.model.CostUnavailableReason
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectCostResult
import com.cooper.lithic.core.model.ProjectSnapshot
import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object ProjectCostEngine {
    /** Evaluate only known events as of the supplied day. Aggregate overflow fails explicitly. */
    fun evaluate(input: ProjectSnapshot, asOf: LocalDate): ProjectCostResult {
        var outlayCents = 0L
        var recoveryCents = 0L
        for (event in input.moneyEvents) {
            if (event.occurredOn.isAfter(asOf)) continue
            when (event.direction) {
                MoneyDirection.OUTLAY -> outlayCents = Math.addExact(outlayCents, event.amount.cents)
                MoneyDirection.RECOVERY -> recoveryCents = Math.addExact(recoveryCents, event.amount.cents)
            }
        }
        val netCents = Math.subtractExact(outlayCents, recoveryCents)
        val project = input.project
        if (asOf.isBefore(project.startedOn)) {
            return ProjectCostResult(
                heldDays = 0,
                grossOutlayCents = outlayCents,
                recoveryCents = recoveryCents,
                netCents = netCents,
                dailyRate = null,
                contributesToCurrentTotal = false,
                unavailableReason = CostUnavailableReason.PROJECT_NOT_STARTED,
            )
        }

        val endedOn = project.endedOn
        val lastHeldDay = if (endedOn != null && endedOn.isBefore(asOf)) endedOn else asOf
        val heldDays = Math.addExact(ChronoUnit.DAYS.between(project.startedOn, lastHeldDay), 1L)
        return ProjectCostResult(
            heldDays = heldDays,
            grossOutlayCents = outlayCents,
            recoveryCents = recoveryCents,
            netCents = netCents,
            dailyRate = BigDecimal.valueOf(netCents, 2).divide(BigDecimal.valueOf(heldDays), MathContext.DECIMAL128),
            contributesToCurrentTotal = endedOn == null || asOf.isBefore(endedOn),
            unavailableReason = null,
        )
    }
}
