package com.cooper.lithic.core.design

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
fun AmountText(
    amount: BigDecimal?,
    modifier: Modifier = Modifier,
    unit: AmountUnit = AmountUnit.YUAN_PER_DAY,
    style: TextStyle = MaterialTheme.typography.displayLarge,
) {
    val display = when (unit) {
        AmountUnit.YUAN -> AmountFormatter.formatCny(amount)
        AmountUnit.YUAN_PER_DAY -> AmountFormatter.formatDailyRate(amount)
    }
    Column(modifier.semantics(mergeDescendants = true) {
        contentDescription = AmountFormatter.description(amount, unit)
    }) {
        Text(display, style = style, color = MaterialTheme.colorScheme.onSurface, softWrap = true)
        Text(unit.label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = true)
    }
}
