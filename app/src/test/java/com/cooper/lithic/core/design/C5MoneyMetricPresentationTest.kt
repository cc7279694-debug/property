package com.cooper.lithic.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class C5MoneyMetricPresentationTest {
    @Test fun measured_daily_text_is_the_same_complete_value_with_a_smaller_baseline_suffix() {
        val text = moneyMetricAnnotatedText(BigDecimal("0.666666"), AmountUnit.YUAN_PER_DAY, Color.Gray)
        assertEquals("¥0.67\u2009/天", text.text)
        assertEquals("\u2009/天", text.text.substring(text.spanStyles.single().start, text.spanStyles.single().end))
        assertEquals(14.sp, text.spanStyles.single().item.fontSize)
        assertEquals(Color.Gray, text.spanStyles.single().item.color)
        assertEquals(null, text.spanStyles.single().item.baselineShift)
    }

    @Test fun cny_and_missing_readouts_have_no_separate_suffix_to_measure_or_read_twice() {
        val cny = moneyMetricAnnotatedText(BigDecimal("2"), AmountUnit.YUAN, Color.Gray)
        assertEquals("¥2.00", cny.text)
        assertEquals(emptyList<Nothing>(), cny.spanStyles)
        val missing = moneyMetricAnnotatedText(null, AmountUnit.YUAN_PER_DAY, Color.Gray)
        assertEquals("—", missing.text)
        assertEquals(emptyList<Nothing>(), missing.spanStyles)
    }

    @Test fun cny_is_one_complete_currency_value_without_a_second_yuan_label() {
        assertEquals("¥2.00", moneyMetricDisplay(BigDecimal("2"), AmountUnit.YUAN))
        assertFalse(moneyMetricDisplay(BigDecimal("2"), AmountUnit.YUAN).contains("元"))
        assertEquals("2.00元", moneyMetricDescription(BigDecimal("2"), AmountUnit.YUAN))
    }

    @Test fun daily_display_and_copy_text_include_the_complete_daily_suffix_once() {
        assertEquals("¥0.67/天", moneyMetricDisplay(BigDecimal("0.666666"), AmountUnit.YUAN_PER_DAY))
        assertEquals("0.67元/天", moneyMetricDescription(BigDecimal("0.666666"), AmountUnit.YUAN_PER_DAY))
    }

    @Test fun missing_zero_and_negative_values_keep_the_existing_distinct_meanings() {
        assertEquals("—", moneyMetricDisplay(null, AmountUnit.YUAN_PER_DAY))
        assertEquals("暂无数据", moneyMetricDescription(null, AmountUnit.YUAN_PER_DAY))
        assertEquals("¥0.00/天", moneyMetricDisplay(BigDecimal.ZERO, AmountUnit.YUAN_PER_DAY))
        assertEquals("−¥2.00/天", moneyMetricDisplay(BigDecimal("-2"), AmountUnit.YUAN_PER_DAY))
        assertEquals("负2.00元/天", moneyMetricDescription(BigDecimal("-2"), AmountUnit.YUAN_PER_DAY))
    }

    @Test fun positive_and_negative_subcent_daily_values_keep_the_original_formatter_contract() {
        assertEquals("<¥0.01/天", moneyMetricDisplay(BigDecimal("0.00001"), AmountUnit.YUAN_PER_DAY))
        assertEquals("−<¥0.01/天", moneyMetricDisplay(BigDecimal("-0.00001"), AmountUnit.YUAN_PER_DAY))
        assertEquals("负值，绝对值小于0.01元/天", moneyMetricDescription(BigDecimal("-0.00001"), AmountUnit.YUAN_PER_DAY))
        assertEquals("¥0.01", moneyMetricDisplay(BigDecimal("0.005"), AmountUnit.YUAN))
    }

    @Test fun metric_label_is_added_once_without_changing_the_value_or_unit_meaning() {
        assertEquals("历史平均日成本，0.67元/天",
            moneyMetricDescription(BigDecimal("0.666666"), AmountUnit.YUAN_PER_DAY, "历史平均日成本"))
        assertEquals("0.67元/天", moneyMetricDescription(BigDecimal("0.666666"), AmountUnit.YUAN_PER_DAY, " "))
    }

    @Test fun full_amount_text_preserves_all_digits_and_the_original_display_precision() {
        val amount = BigDecimal("-123456789012345678901234567890.015")
        assertEquals("−¥123456789012345678901234567890.02/天", moneyMetricDisplay(amount, AmountUnit.YUAN_PER_DAY))
        assertEquals(BigDecimal("-123456789012345678901234567890.015"), amount)
    }
}
