package com.cooper.lithic.core.design

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountFormatterTest {
    @Test fun missing_amount_is_not_a_zero_amount() {
        assertEquals("—", AmountFormatter.formatCny(null))
        assertEquals("—", AmountFormatter.formatDailyRate(null))
    }

    @Test fun real_zero_has_two_decimal_places() {
        assertEquals("¥0.00", AmountFormatter.formatCny(BigDecimal.ZERO))
        assertEquals("¥0.00", AmountFormatter.formatDailyRate(BigDecimal("0.000000")))
    }

    @Test fun ordinary_amount_rounds_half_up_with_both_signs() {
        assertEquals("¥1.24", AmountFormatter.formatCny(BigDecimal("1.235")))
        assertEquals("−¥1.24", AmountFormatter.formatCny(BigDecimal("-1.235")))
        assertEquals("¥1.23", AmountFormatter.formatCny(BigDecimal("1.2349")))
        assertEquals("−¥1.23", AmountFormatter.formatCny(BigDecimal("-1.2349")))
    }

    @Test fun ordinary_half_cent_is_rounded_and_not_a_daily_rate_boundary() {
        assertEquals("¥0.01", AmountFormatter.formatCny(BigDecimal("0.005")))
        assertEquals("−¥0.01", AmountFormatter.formatCny(BigDecimal("-0.005")))
        assertEquals("¥0.00", AmountFormatter.formatCny(BigDecimal("0.0049")))
    }

    @Test fun every_nonzero_daily_rate_below_one_cent_remains_explicitly_nonzero() {
        listOf("0.000000000000000000000000000001", "0.0049", "0.005", "0.00999999999999999999999").forEach {
            assertEquals("<¥0.01", AmountFormatter.formatDailyRate(BigDecimal(it)))
            assertEquals("−<¥0.01", AmountFormatter.formatDailyRate(BigDecimal("-$it")))
        }
    }

    @Test fun exact_one_cent_and_larger_daily_rates_use_normal_rounding() {
        assertEquals("¥0.01", AmountFormatter.formatDailyRate(BigDecimal("0.010000")))
        assertEquals("−¥0.01", AmountFormatter.formatDailyRate(BigDecimal("-0.01")))
        assertEquals("¥0.02", AmountFormatter.formatDailyRate(BigDecimal("0.015")))
        assertEquals("−¥0.02", AmountFormatter.formatDailyRate(BigDecimal("-0.015")))
    }

    @Test fun large_aggregate_is_exact_and_never_uses_scientific_notation() {
        assertEquals("¥92233720368547758.07", AmountFormatter.formatCny(BigDecimal("92233720368547758.07")))
        assertEquals("−¥92233720368547758.07", AmountFormatter.formatDailyRate(BigDecimal("-92233720368547758.07")))
        assertEquals("¥100000000000000000000.00", AmountFormatter.formatCny(BigDecimal("1E+20")))
    }

    @Test fun high_precision_value_is_only_formatted_without_changing_numeric_input() {
        val amount = BigDecimal("1.005000000000000000000000000001")
        assertEquals("¥1.01", AmountFormatter.formatCny(amount))
        assertEquals("¥1.01", AmountFormatter.formatDailyRate(amount))
        assertEquals("1.005000000000000000000000000001", amount.toPlainString())
        assertEquals("<¥0.01", AmountFormatter.formatDailyRate(BigDecimal("1E-30")))
    }
}
