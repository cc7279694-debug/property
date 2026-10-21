package com.cooper.lithic.core.design

import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

/** Numeric values stay exact; this projection is only for rendering coordinates. */
data class CostChartPoint(val date: LocalDate, val value: BigDecimal)

internal class ChartGeometry private constructor(
    val start: LocalDate,
    val end: LocalDate,
    val minimum: BigDecimal,
    val maximum: BigDecimal,
) {
    fun x(date: LocalDate): Float = if (start == end) 0.5f else {
        BigDecimal.valueOf(date.toEpochDay() - start.toEpochDay())
            .divide(BigDecimal.valueOf(end.toEpochDay() - start.toEpochDay()), MathContext.DECIMAL128)
            .toFloat().coerceIn(0f, 1f)
    }

    fun y(value: BigDecimal): Float = maximum.subtract(value)
        .divide(maximum.subtract(minimum), MathContext.DECIMAL128).toFloat().coerceIn(0f, 1f)

    val zero: Float get() = y(BigDecimal.ZERO)

    companion object {
        fun forPoints(points: List<CostChartPoint>): ChartGeometry? {
            if (points.isEmpty()) return null
            val minimum = minOf(BigDecimal.ZERO, points.minOf { it.value })
            val maximum = maxOf(BigDecimal.ZERO, points.maxOf { it.value })
            // A zero-only series has a visible centerline; its data is still exactly zero.
            return ChartGeometry(points.minOf { it.date }, points.maxOf { it.date },
                if (minimum.compareTo(maximum) == 0) BigDecimal.ONE.negate() else minimum,
                if (minimum.compareTo(maximum) == 0) BigDecimal.ONE else maximum)
        }
    }
}
