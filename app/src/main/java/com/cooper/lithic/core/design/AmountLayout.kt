package com.cooper.lithic.core.design

internal data class AmountLayout(val fontSp: Float, val horizontalScroll: Boolean)

/** Widths come from TextMeasurer with the actual density/font scale, never digit estimates. */
internal fun amountLayout(preferredSp: Float, availableWidthPx: Int, measuredWidth: (Float) -> Int): AmountLayout {
    require(preferredSp.isFinite() && preferredSp > 0f)
    val floor = minOf(preferredSp, 18f)
    var size = preferredSp
    while (size > floor) {
        if (measuredWidth(size) <= availableWidthPx) return AmountLayout(size, false)
        size = maxOf(floor, size - 2f)
    }
    return AmountLayout(floor, measuredWidth(floor) > availableWidthPx)
}
