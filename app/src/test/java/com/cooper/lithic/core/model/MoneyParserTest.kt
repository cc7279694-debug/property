package com.cooper.lithic.core.model

import com.cooper.lithic.domain.project.ProjectCostEngine
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class MoneyParserTest {
    @Test fun money_precision_and_zero() {
        val date = LocalDate.of(2026, 1, 1)
        val input = ProjectSnapshot(LongTermProject(date), listOf(
            ProjectMoneyEvent(MoneyDirection.OUTLAY, parsed("0.10"), date),
            ProjectMoneyEvent(MoneyDirection.OUTLAY, parsed("0.20"), date),
        ))
        val result = ProjectCostEngine.evaluate(input, date)
        assertEquals(30L, result.grossOutlayCents)
        assertEquals(0, BigDecimal("0.30").compareTo(result.dailyRate))
        assertEquals(0L, parsed("0").cents)
        rejected("0.001", MoneyParseError.EXCESS_PRECISION)
        rejected("10000000000", MoneyParseError.OUT_OF_RANGE)
    }

    @Test fun parses_exact_non_negative_cny() {
        for ((text, expected) in listOf("0" to 0L, "1" to 100L,
            "0.1" to 10L, "0.01" to 1L, "001.20" to 120L, " 2.30 " to 230L)) {
            assertEquals(text, expected, parsed(text).cents)
        }
    }

    @Test fun accepts_largest_single_amount() {
        assertEquals(999_999_999_999L, parsed("9999999999.99").cents)
    }

    @Test fun rejects_excess_precision_without_rounding() {
        for (text in listOf("1.001", "1.000", "9999999999.999")) {
            rejected(text, MoneyParseError.EXCESS_PRECISION)
        }
    }

    @Test fun rejects_out_of_range_numbers() {
        for (text in listOf("10000000000", "92233720368547758.08", "9".repeat(4096))) {
            rejected(text, MoneyParseError.OUT_OF_RANGE)
        }
    }

    @Test fun rejects_non_decimal_and_signed_input() {
        for (text in listOf("", " ", "-1", "-0", "+1", "1e3", "NaN", "Infinity",
            "1,000", "¥1", ".5", "1.", "1 2", "abc", "１２")) {
            rejected(text, MoneyParseError.INVALID_FORMAT)
        }
    }

    @Test fun direct_money_construction_cannot_bypass_amount_limits() {
        assertThrows(IllegalArgumentException::class.java) { Money(-1) }
        assertThrows(IllegalArgumentException::class.java) { Money(1_000_000_000_000L) }
    }

    private fun parsed(text: String): Money {
        val result = MoneyParser.parseCny(text)
        assertTrue("Expected accepted amount for '$text', got $result", result is MoneyParseResult.Success)
        return (result as MoneyParseResult.Success).money
    }

    private fun rejected(text: String, reason: MoneyParseError) {
        assertEquals(text, MoneyParseResult.Failure(reason), MoneyParser.parseCny(text))
    }
}
