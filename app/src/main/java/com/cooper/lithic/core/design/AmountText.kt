package com.cooper.lithic.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import java.math.BigDecimal
import java.math.RoundingMode

enum class AmountUnit(val label: String) { YUAN("元"), YUAN_PER_DAY("元/天") }

/** Display only: callers retain their original numeric value for all calculations. */
object AmountFormatter {
    private val oneCent = BigDecimal("0.01")

    fun formatCny(amount: BigDecimal?): String {
        if (amount == null) return "—"
        val rounded = amount.setScale(2, RoundingMode.HALF_UP)
        val sign = if (rounded.signum() < 0) "−" else ""
        return "$sign¥${rounded.abs().toPlainString()}"
    }

    fun formatDailyRate(amount: BigDecimal?): String = when {
        amount == null -> "—"
        isSubcent(amount) -> if (amount.signum() < 0) "−<¥0.01" else "<¥0.01"
        else -> formatCny(amount)
    }

    internal fun description(amount: BigDecimal?, unit: AmountUnit): String {
        if (amount == null) return "暂无数据"
        if (unit == AmountUnit.YUAN_PER_DAY && isSubcent(amount)) {
            return if (amount.signum() < 0) "负值，绝对值小于0.01元/天" else "小于0.01元/天"
        }
        val display = formatCny(amount)
        return if (display.startsWith("−")) "负${display.removePrefix("−¥")}${unit.label}"
            else "${display.removePrefix("¥")}${unit.label}"
    }

    private fun isSubcent(amount: BigDecimal): Boolean = amount.signum() != 0 && amount.abs() < oneCent
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun AmountText(
    amount: BigDecimal?,
    modifier: Modifier = Modifier,
    unit: AmountUnit = AmountUnit.YUAN_PER_DAY,
    style: TextStyle = MaterialTheme.typography.displayLarge,
    showUnit: Boolean = true,
    color: Color = MaterialTheme.colorScheme.onSurface,
    supportingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    // Compatibility only: the former showUnit flag no longer creates a separate unit path.
    // Production callers select a MoneyMetric role directly; all rendering lives there.
    val level = when {
        !style.fontSize.isSp || !style.fontSize.value.isFinite() -> MoneyMetricLevel.HERO
        style.fontSize.value >= 38f -> MoneyMetricLevel.HERO
        style.fontSize.value >= 28f -> MoneyMetricLevel.DETAIL
        style.fontSize.value >= 24f -> MoneyMetricLevel.CARD
        else -> MoneyMetricLevel.ROW
    }
    MoneyMetric(amount, modifier, unit, level, color, supportingColor)
}
