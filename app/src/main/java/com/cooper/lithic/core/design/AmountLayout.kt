package com.cooper.lithic.core.design

enum class MoneyMetricLevel { HERO, DETAIL, CARD, ROW }

internal data class AmountLayout(val fontSp: Float, val horizontalScroll: Boolean)

/** Every candidate measures the complete readout with the caller's actual font scale. */
internal fun amountLayout(level: MoneyMetricLevel, availableWidthPx: Int, measuredWidth: (Float) -> Int): AmountLayout {
    val candidates = when (level) {
        MoneyMetricLevel.HERO -> listOf(38f, 32f, 28f)
        MoneyMetricLevel.DETAIL -> listOf(28f, 24f)
        MoneyMetricLevel.CARD -> listOf(24f, 20f)
        MoneyMetricLevel.ROW -> listOf(18f)
    }
    for (size in candidates) {
        if (measuredWidth(size) <= availableWidthPx) return AmountLayout(size, false)
    }
    return AmountLayout(candidates.last(), true)
}
